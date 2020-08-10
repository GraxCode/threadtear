package me.nov.threadtear.execution.stringer;

import java.lang.reflect.*;
import java.util.*;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import me.nov.threadtear.analysis.stack.*;
import me.nov.threadtear.execution.*;
import me.nov.threadtear.util.asm.Instructions;
import me.nov.threadtear.util.format.Strings;
import me.nov.threadtear.util.reflection.Casts;
import me.nov.threadtear.vm.*;

public class StringObfuscationStringer extends Execution implements IVMReferenceHandler, IConstantReferenceHandler {

  private static final String STRINGER_DECRPYTION_METHOD_DESC_REGEX =
          "\\(Ljava/lang/Object;[^\\[L]?[^\\[L]?[^\\[L]?[^\\[L]?\\)Ljava/lang/String;";
  private Map<String, Clazz> classes;
  private int encrypted;
  private int decrypted;
  private boolean verbose;

  public StringObfuscationStringer() {
    super(ExecutionCategory.STRINGER, "String obfuscation removal",
            "Works for version 3 - 9.<br>Make sure to decrypt access obfuscation first.",
            ExecutionTag.RUNNABLE, ExecutionTag.POSSIBLY_MALICIOUS);
  }

  /*
   * this works as following:
   *
   * find decryption method via regex
   *
   * create proxy class with same method name and same
   * class name
   *
   * in method, invoke decryption method and set result
   * to field in class
   *
   * method parameters are loaded with fields (getstatic)
   *  but from another class, because reflection calls
   * <clinit> when setting a field (fuck you, java) (and
   * it's impossible to
   * change method names, stringer forces us to)
   *
   * method is run -> replace in code
   *
   *
   */

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    this.verbose = verbose;
    this.classes = classes;
    this.encrypted = 0;
    this.decrypted = 0;
    if (classes.values().stream().anyMatch(c -> c.oldEntry.getExtra() != null && c.oldEntry.getExtra().length > 0)) {
      logger.warning("The file has a stringer signature, please patch first!");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }
    classes.values().forEach(this::decrypt);
    if (encrypted == 0) {
      logger.error("No strings matching stringer 9 string obfuscation have been found!");
      return false;
    }
    float decryptionRatio = Math.round((decrypted / (float) encrypted) * 100);
    logger.info("Of a total {} encrypted strings, {}% were successfully decrypted", encrypted, decryptionRatio);
    return decryptionRatio > 0.25;
  }

  private void decrypt(Clazz c) {
    logger.collectErrors(c);
    ClassNode cn = c.node;
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
      if (min.desc.matches(STRINGER_DECRPYTION_METHOD_DESC_REGEX)) {
        try {
          encrypted++;
          allowReflection(true);
          String realString = invokeProxy(cn, m, min, frame);
          allowReflection(false);
          if (realString != null) {
            if (Strings.isHighUTF(realString)) {
              logger.warning("String may have not decrypted correctly in {}", referenceString(cn, m));
            }
            this.decrypted++;
            return new AbstractInsnNode[]{min, new InsnNode(POP), new LdcInsnNode(realString)};
          } else {
            logger.error("Failed to decrypt string or false call in {}", referenceString(cn, m));
          }
        } catch (Throwable e) {
          if (verbose) {
            logger.error("Throwable", e);
          }
          logger.error("Failed to decrypt string in {}, {}", referenceString(cn, m), shortStacktrace(e));
        }
      }
    }
    return new AbstractInsnNode[]{ain};
  }

  private String invokeProxy(ClassNode cn, MethodNode m, MethodInsnNode min, Frame<ConstantValue> frame)
          throws Exception {
    if (frame == null) {
      if (verbose) {
        logger.error("Unvisited frame in {}: {}", referenceString(cn, m), frame);
      }
      return null;
    }
    VM vm = VM.constructVM(this);
    createFakeCloneAndFieldGetter(cn, m, min, frame); //
    // create a duplicate of the current class,
    // we need this because stringer checks for
    // stacktrace method name and class

    Class<?> proxyFieldClass = vm.loadClass(invocationFieldClass.name.replace('/', '.'), true);
    // set proxyFields to stack values
    int arguments = Type.getArgumentTypes(min.desc).length;
    if (arguments > frame.getStackSize()) {
      if (verbose) {
        logger.error("Stack has not enough values in {}", referenceString(cn, m), frame);
      }
      return null;
    }
    for (int i = 0; i < arguments; i++) {
      Field proxyField = proxyFieldClass.getDeclaredField("proxyField_" + i);
      ConstantValue stackValue = frame.getStack(frame.getStackSize() - arguments + i);
      if (!stackValue.isKnown()) {
        if (verbose) {
          logger.error("Stack index " + i + " is unknown in " + cn.name + "." + m.name + ": field type: " +
                  proxyField.getType().getName() + ", stack type: " + stackValue.getType());
        }
        return null;
      }
      proxyField.set(null, Casts.castWithPrimitives(proxyField.getType(), stackValue.getValue()));
    }

    vm.loadClass(min.owner.replace('/', '.'), true); // load decryption
    // class, this class will load another class (some
    // type of map)
    Class<?> loadedClone = vm.loadClass(fakeInvocationClone.name.replace('/', '.'), true); // load dupe

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

  private void createFakeCloneAndFieldGetter(ClassNode cn, MethodNode m, MethodInsnNode min,
                                             Frame<ConstantValue> frame) {
    ClassNode node = Sandbox.createClassProxy(cn.name);
    // we can't put the fields in the same class, as
    // setting them via reflection
    // would execute <clinit>
    ClassNode fieldClass = Sandbox.createClassProxy("ProxyFields");
    InsnList instructions = new InsnList();
    Type[] types = Type.getArgumentTypes(min.desc);
    for (int i = 0; i < types.length; i++) {
      // make fields as stack placeholder, that's the
      // easiest way of transferring
      // stack to method
      String desc = types[i].getDescriptor();
      fieldClass.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, "proxyField_" + i, desc, null, null));
      instructions.add(new FieldInsnNode(GETSTATIC, fieldClass.name, "proxyField_" + i, desc));
    }
    instructions.add(min.clone(null)); // we can clone
    // original method here
    instructions.add(new FieldInsnNode(PUTSTATIC, node.name, "proxyReturn", "Ljava/lang/String;"));
    instructions.add(new InsnNode(RETURN));

    node.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, "proxyReturn", "Ljava/lang/String;", null, null));
    node.methods.add(Sandbox.createMethodProxy(instructions, m.name, "()V")); // method should return real
    // string
    fakeInvocationClone = node;
    invocationFieldClass = fieldClass;
  }

  private ClassNode fakeInvocationClone;
  private ClassNode invocationFieldClass;

  @Override
  public ClassNode tryClassLoad(String name) {
    if (name.equals(fakeInvocationClone.name)) {
      return fakeInvocationClone;
    }
    if (name.equals(invocationFieldClass.name)) {
      return invocationFieldClass;
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
    if (name.equals("toCharArray") && owner.equals("java/lang/String")) {
      if (!values.get(0).isKnown()) {
        if (verbose) {
          logger.error("String that should be converted to char[] is unknown");
        }
        return null;
      }
      // allow char array method
      return ((String) values.get(0).getValue()).toCharArray();
    }
    return null;
  }
}
