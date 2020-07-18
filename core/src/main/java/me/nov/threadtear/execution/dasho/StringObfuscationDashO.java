package me.nov.threadtear.execution.dasho;

import me.nov.threadtear.analysis.stack.BasicReferenceHandler;
import me.nov.threadtear.analysis.stack.ConstantValue;
import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import me.nov.threadtear.util.asm.InstructionModifier;
import me.nov.threadtear.util.format.Strings;
import me.nov.threadtear.vm.IVMReferenceHandler;
import me.nov.threadtear.vm.Sandbox;
import me.nov.threadtear.vm.VM;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Frame;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class StringObfuscationDashO extends Execution implements IVMReferenceHandler {


  private static final List<String> DESCS = Arrays.asList("(ILjava/lang/String;)Ljava/lang/String;",
    "(Ljava/lang/String;I)Ljava/lang/String;", "(Ljava/lang/String;II)Ljava/lang/String;");
  private Map<String, Clazz> classes;
  private int encrypted;
  private int decrypted;
  private boolean verbose;
  private ClassNode fakeInvocationClone;

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
      InstructionModifier modifier = new InstructionModifier();
      loopConstantFrames(cn, m, new BasicReferenceHandler(),
        (ain, frame) -> tryReplaceMethods(cn, m, modifier, ain, frame));
      modifier.apply(m);
    });
  }

  private void tryReplaceMethods(ClassNode cn, MethodNode m, InstructionModifier modifier, AbstractInsnNode ain,
                                 Frame<ConstantValue> frame) {
    if (ain.getOpcode() == INVOKESTATIC) {
      MethodInsnNode min = (MethodInsnNode) ain;
      if (DESCS.contains(min.desc)) {
        try {
          encrypted++;
          Type[] args = Type.getArgumentTypes(min.desc);
          for (int i = 0; i < args.length; i++)
            if (!frame.getStack(frame.getStackSize() - 1 - i).isKnown()) {
              if (verbose)
                logger.error("Failed to decrypt string in {}", referenceString(cn, m));
              return;
            }
          // strings are not high utf and no high sdev,
          // don't check
          String realString = invokeProxy(cn, m, min, args, frame);
          if (realString != null) {
            if (Strings.isHighUTF(realString)) {
              logger.warning("String may have not decrypted correctly in {}", referenceString(cn, m));
            }
            this.decrypted++;

            InsnList il = new InsnList();
            for (Type arg : args) {
              // pop off remaining decryption values
              il.add(new InsnNode(arg.getSize() > 1 ? POP2 : POP));
            }
            modifier.prepend(min, il);
            modifier.replace(min, new LdcInsnNode(realString));
          } else {
            logger.error("Failed to decrypt string in {}", referenceString(cn, m));
          }
        } catch (Throwable e) {
          if (verbose) {
            logger.error("Throwable", e);
          }
          logger.error("Failed to decrypt string in {}: {}", referenceString(cn, m), shortStacktrace(e));
        }
      }
    }
  }

  private String invokeProxy(ClassNode cn, MethodNode m, MethodInsnNode min, Type[] args, Frame<ConstantValue> frame)
    throws Exception {
    VM vm = VM.constructNonInitializingVM(this);
    createFakeClone(cn, m, min, args, frame); // create a
    // duplicate of the current class,
    // we need this because dashO checks for
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
      loadedClone.getDeclaredConstructor().newInstance(); // special case:
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

  private void createFakeClone(ClassNode cn, MethodNode m, MethodInsnNode min, Type[] args,
                               Frame<ConstantValue> frame) {
    ClassNode node = Sandbox.createClassProxy(cn.name);
    InsnList instructions = new InsnList();
    for (int i = 0; i < args.length; i++)
      instructions.insert(new LdcInsnNode(frame.getStack(frame.getStackSize() - 1 - i).getValue()));

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

  @Override
  public ClassNode tryClassLoad(String name) {
    if (name.equals(fakeInvocationClone.name)) {
      return fakeInvocationClone;
    }
    return classes.containsKey(name) ? classes.get(name).node : null;
  }
}
