package me.nov.threadtear.execution.dasho;

import java.lang.reflect.Method;
import java.util.*;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import me.nov.threadtear.analysis.stack.*;
import me.nov.threadtear.execution.*;
import me.nov.threadtear.util.asm.Instructions;
import me.nov.threadtear.util.format.Strings;
import me.nov.threadtear.vm.*;

public class StringObfuscationDashO extends Execution implements IVMReferenceHandler {

  private static final String DASHO_DECRPYTION_METHOD_DESC1 = "(ILjava/lang/String;)Ljava/lang/String;";
  private static final String DASHO_DECRPYTION_METHOD_DESC2 = "(Ljava/lang/String;I)Ljava/lang/String;";

  private Map<String, Clazz> classes;
  private int encrypted;
  private int decrypted;
  private boolean verbose;

  public StringObfuscationDashO() {
    super(ExecutionCategory.DASHO, "String obfuscation removal",
      "Tested on version 10.3, should work for older versions too.", ExecutionTag.RUNNABLE,
      ExecutionTag.POSSIBLY_MALICIOUS);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    this.verbose = verbose;
    this.classes = classes;
    this.encrypted = 0;
    this.decrypted = 0;

    classes.values().forEach(this::decrypt);
    if (encrypted == 0) {
      logger.error("No strings matching DashO 7.3 string obfuscation have been found!");
      return false;
    }
    float decryptionRatio = Math.round((decrypted / (float) encrypted) * 100);
    logger.info("Of a total " + encrypted + " encrypted strings, " + (decryptionRatio) + "% were " +
      "successfully decrypted");
    return decryptionRatio > 0.25;
  }

  private void decrypt(Clazz c) {
    ClassNode cn = c.node;
    logger.collectErrors(c);
    cn.methods.forEach(m -> {
      InsnList rewrittenCode = new InsnList();
      Map<LabelNode, LabelNode> labels = Instructions.cloneLabels(m.instructions);

      // as we can't add instructions because frame index
      // and instruction index
      // wouldn't fit together anymore we have to do it
      // this way
      loopConstantFrames(cn, m, new BasicReferenceHandler(), (ain, frame) -> {
        for (AbstractInsnNode newInstr : tryReplaceMethods(cn, m, ain, frame)) {
          rewrittenCode.add(newInstr.clone(labels));
        }
      });
      if (rewrittenCode.size() > 0) {
        Instructions.updateInstructions(m, labels, rewrittenCode);
      }
    });
  }

  private AbstractInsnNode[] tryReplaceMethods(ClassNode cn, MethodNode m, AbstractInsnNode ain,
                                               Frame<ConstantValue> frame) {
    if (ain.getOpcode() == INVOKESTATIC) {
      MethodInsnNode min = (MethodInsnNode) ain;
      if (min.desc.equals(DASHO_DECRPYTION_METHOD_DESC1) || min.desc.equals(DASHO_DECRPYTION_METHOD_DESC2)) {
        try {
          encrypted++;
          ConstantValue second = frame.getStack(frame.getStackSize() - 2);
          ConstantValue top = frame.getStack(frame.getStackSize() - 1);
          if (top.isKnown() && second.isKnown()) {
            // strings are not high utf and no high sdev,
            // don't check
            String realString = invokeProxy(cn, m, min, top, second);
            if (realString != null) {
              if (Strings.isHighUTF(realString)) {
                logger.warning("String may have not decrypted correctly in {}", referenceString(cn, m));
              }
              this.decrypted++;
              return new AbstractInsnNode[]{new InsnNode(POP2), new LdcInsnNode(realString)};
            } else {
              logger.error("Failed to decrypt string in {}", referenceString(cn, m));
            }
          } else if (verbose) {
            logger.warning("Unknown top stack value in {}, skipping", referenceString(cn, m));
          }
        } catch (Throwable e) {
          if (verbose) {
            logger.error("Throwable", e);
          }
          logger.error("Failed to decrypt string in {}: {}", referenceString(cn, m), shortStacktrace(e));
        }
      }
    }
    return new AbstractInsnNode[]{ain};
  }

  private String invokeProxy(ClassNode cn, MethodNode m, MethodInsnNode min, ConstantValue top, ConstantValue second)
    throws Exception {
    VM vm = VM.constructNonInitializingVM(this);
    createFakeClone(cn, m, min, top, second); // create a
    // duplicate of the current class,
    // we need this because stringer checks for
    // stacktrace method name and class

    ClassNode decryptionMethodOwner = classes.get(min.owner).node;
    if (decryptionMethodOwner == null)
      return null;
    vm.explicitlyPreload(fakeInvocationClone); // proxy
    // class can't contain code in clinit other than the
    // one we want to run
    if (!vm.isLoaded(decryptionMethodOwner.name.replace('/', '.'))) // decryption class
      // could be the same class
      vm.explicitlyPreload(decryptionMethodOwner, true, (name, desc) -> !name.matches("java/lang/.*"));
    Class<?> loadedClone = vm.loadClass(fakeInvocationClone.name.replace('/', '.'), true); // load
    // dupe class

    if (m.name.equals("<init>")) {
      loadedClone.newInstance(); // special case:
      // constructors have to be invoked by newInstance.
      // Sandbox.createMethodProxy automatically handles
      // access and super call
    } else {
      for (Method reflectionMethod : loadedClone.getMethods()) {
        if (reflectionMethod.getName().equals(m.name)) {
          reflectionMethod.invoke(null);
          break;
        }
      }
    }
    return (String) loadedClone.getDeclaredField("proxyReturn").get(null);
  }

  private void createFakeClone(ClassNode cn, MethodNode m, MethodInsnNode min, ConstantValue top,
                               ConstantValue second) {
    ClassNode node = Sandbox.createClassProxy(cn.name);
    InsnList instructions = new InsnList();
    instructions.add(new LdcInsnNode(second.getValue()));
    instructions.add(new LdcInsnNode(top.getValue()));
    instructions.add(min.clone(null)); // we can clone
    // original method here
    instructions.add(new FieldInsnNode(PUTSTATIC, node.name, "proxyReturn", "Ljava/lang/String;"));
    instructions.add(new InsnNode(RETURN));

    node.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, "proxyReturn", "Ljava/lang/String;", null, null));
    node.methods.add(Sandbox.createMethodProxy(instructions, m.name, "()V")); // method should return real
    // string
    if (min.owner.equals(cn.name)) {
      // decryption method is in own class
      node.methods.add(Sandbox.copyMethod(getMethod(classes.get(min.owner).node, min.name, min.desc)));
    }
    fakeInvocationClone = node;
  }

  private ClassNode fakeInvocationClone;

  @Override
  public ClassNode tryClassLoad(String name) {
    if (name.equals(fakeInvocationClone.name)) {
      return fakeInvocationClone;
    }
    return classes.containsKey(name) ? classes.get(name).node : null;
  }
}
