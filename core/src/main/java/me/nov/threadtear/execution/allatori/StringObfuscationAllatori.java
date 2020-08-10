package me.nov.threadtear.execution.allatori;

import java.lang.reflect.Method;
import java.util.*;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import me.nov.threadtear.analysis.stack.*;
import me.nov.threadtear.execution.*;
import me.nov.threadtear.util.asm.Instructions;
import me.nov.threadtear.util.format.Strings;
import me.nov.threadtear.vm.*;

public class StringObfuscationAllatori extends Execution implements IVMReferenceHandler, IConstantReferenceHandler {

  private static final String ALLATORI_DECRPYTION_METHOD_DESC = "(Ljava/lang/String;)Ljava/lang/String;";
  private Map<String, Clazz> classes;
  private int encrypted;
  private int decrypted;
  private boolean verbose;

  public StringObfuscationAllatori() {
    super(ExecutionCategory.ALLATORI, "String obfuscation removal",
            "Tested on version 7.3, should work for older versions too.", ExecutionTag.RUNNABLE,
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
      logger.error("No strings matching Allatori 7.3 string obfuscation have been found!");
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
      loopConstantFrames(cn, m, this, (ain, frame) -> {
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
      if (min.desc.equals(ALLATORI_DECRPYTION_METHOD_DESC)) {
        try {
          encrypted++;
          ConstantValue top = frame.getStack(frame.getStackSize() - 1);
          if (top.isKnown() && top.isString()) {
            String encryptedString = (String) top.getValue();
            // strings are not high utf and no high sdev,
            // don't check
            String realString = invokeProxy(cn, m, min, encryptedString);
            if (realString != null) {
              if (Strings.isHighUTF(realString)) {
                logger.warning("String may have not decrypted correctly in " + cn.name + "." + m.name + m.desc);
              }
              this.decrypted++;
              return new AbstractInsnNode[]{new InsnNode(POP), new LdcInsnNode(realString)};
            } else {
              logger.error("Failed to decrypt string in " + cn.name + "." + m.name + m.desc);
            }
          } else if (verbose) {
            logger.warning("Unknown top stack value in " + cn.name + "." + m.name + m.desc + ", skipping");
          }
        } catch (Throwable e) {
          if (verbose) {
            logger.error("Throwable", e);
          }
          logger.error(
                  "Failed to decrypt string in " + cn.name + "." + m.name + m.desc + ": " + e.getClass().getName() +
                          ", " + e.getMessage());
        }
      }
    }
    return new AbstractInsnNode[]{ain};
  }

  private String invokeProxy(ClassNode cn, MethodNode m, MethodInsnNode min, String encrypted) throws Exception {
    VM vm = VM.constructNonInitializingVM(this);
    createFakeClone(cn, m, min, encrypted); // create a
    // duplicate of the current class,
    // we need this because stringer checks for
    // stacktrace method name and class

    final Clazz owner = classes.get(min.owner);
    if (owner == null) {
      logger.error("Could not find owner class in class list");
      return null;
    }
    ClassNode decryptionMethodOwner = owner.node;
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

  private void createFakeClone(ClassNode cn, MethodNode m, MethodInsnNode min, String encrypted) {
    ClassNode node = Sandbox.createClassProxy(cn.name);
    InsnList instructions = new InsnList();
    instructions.add(new LdcInsnNode(encrypted));
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

  @Override
  public Object getFieldValueOrNull(BasicValue v, String owner, String name, String desc) {
    return null;
  }

  @Override
  public Object getMethodReturnOrNull(BasicValue v, String owner, String name, String desc,
                                      List<? extends ConstantValue> values) {
    return null;
  }
}
