package me.nov.threadtear.execution.zkm;

import me.nov.threadtear.analysis.stack.ConstantValue;
import me.nov.threadtear.analysis.stack.IConstantReferenceHandler;
import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import me.nov.threadtear.execution.generic.inliner.ArgumentInfer;
import me.nov.threadtear.util.asm.Access;
import me.nov.threadtear.util.asm.InstructionModifier;
import me.nov.threadtear.util.asm.Instructions;
import me.nov.threadtear.util.asm.References;
import me.nov.threadtear.util.asm.method.MethodContext;
import me.nov.threadtear.util.format.Strings;
import me.nov.threadtear.vm.IVMReferenceHandler;
import me.nov.threadtear.vm.Sandbox;
import me.nov.threadtear.vm.VM;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class StringObfuscationZKM extends Execution implements IVMReferenceHandler, IConstantReferenceHandler {

  private static final String ENCHANCED_MODE_METHOD_DESC = "\\(II+\\)Ljava/lang/String;";
  private static final String ALLOWED_CALLS = "(java/lang/String).*";
  private int decrypted;
  private boolean verbose;
  private ArgumentInfer argumentInfer;

  /*
   * TODO: String encryption using DES Cipher (probably
   *  only in combination with reflection obfuscation
   */
  private FieldInsnNode decryptedArrayField;
  private String[] decryptedFieldValue;

  public StringObfuscationZKM() {
    super(ExecutionCategory.ZKM, "String obfuscation removal",
      "Tested on ZKM 5 - 11, could work on newer versions too.<br><i>String " +
        "encryption using DES Cipher is currently <b>NOT</b> supported.</i>",
      ExecutionTag.RUNNABLE, ExecutionTag.POSSIBLY_MALICIOUS);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    this.verbose = verbose;
    final List<ClassNode> classNodes = classes.values().stream().map(c -> c.node).collect(Collectors.toList());
    argumentInfer = new ArgumentInfer(classNodes);
    classes.values().stream().filter(this::hasZKMBlock).forEach(this::decrypt);
    logger.info("Decrypted {} strings successfully.", decrypted);
    return decrypted > 0;
  }

  private boolean hasZKMBlock(Clazz c) {
    ClassNode cn = c.node;
    if (Access.isInterface(cn.access)) // TODO maybe
      // interfaces get string encrypted too, but proxy
      // would not be
      // working because static methods in interfaces are
      // not allowed
      return false;
    MethodNode mn = getStaticInitializer(cn);
    if (mn == null)
      return false;
    return StreamSupport.stream(mn.instructions.spliterator(), false).anyMatch(
      ain -> ain.getType() == AbstractInsnNode.LDC_INSN &&
        Strings.isHighSDev(((LdcInsnNode) ain).cst.toString()));
  }

  private void decrypt(Clazz c) {
    ClassNode cn = c.node;
    logger.collectErrors(c);
    MethodNode clinit = getStaticInitializer(cn);
    if (clinit == null)
      return;
    if (clinit.instructions.size() > 2000) {
      logger.error("Static initializer too huge to decrypt in {}", referenceString(cn, null));
      return;
    }
    ClassNode proxyClass = Sandbox.createClassProxy("ProxyClass");
    MethodNode callMethod = Sandbox.copyMethod(clinit);
    callMethod.name = "clinitProxy";
    callMethod.access = ACC_PUBLIC | ACC_STATIC;
    Instructions.isolateCallsThatMatch(callMethod, (name, desc) -> !name.matches(ALLOWED_CALLS),
      (name, desc) -> !name.equals(cn.name) ||
        (!desc.equals("[Ljava/lang/String;") && !desc.equals("Ljava/lang/String;")));
    proxyClass.methods.add(callMethod);

    // add decryption methods
    cn.methods.stream().filter(m -> m.desc.matches(ENCHANCED_MODE_METHOD_DESC)).forEach(m -> {
      MethodNode copy = Sandbox.copyMethod(m);
      proxyClass.methods.add(copy);
      Instructions.isolateCallsThatMatch(copy, (name, desc) -> !name.matches(ALLOWED_CALLS),
        (name, desc) -> !name.equals(cn.name) ||
          (!desc.equals("[Ljava/lang/String;") && !desc.equals("Ljava/lang/String;")));
    });
    cn.fields.stream().filter(m -> m.desc.equals("[Ljava/lang/String;") || m.desc.equals("Ljava/lang/String;"))
      .forEach(f -> proxyClass.fields.add(f));
    Map<String, String> singleMap = Collections.singletonMap(cn.name, proxyClass.name);
    proxyClass.methods.stream().map(m -> m.instructions.toArray()).flatMap(Arrays::stream)
      .forEach(ain -> References.remapClassRefs(singleMap, ain));
    try {
      invokeVMAndReplace(proxyClass, cn);
    } catch (Throwable e) {
      if (verbose)
        logger.error("Throwable", e);
      logger.error("Failed to run proxy in {}, {} ", referenceString(cn, null), shortStacktrace(e));
    }
  }

  /**
   * Creates a VM with the same class, but with a new
   * method called "clinitProxy" that is a cutout of the
   * original static initializer with the decryption ONLY.
   */
  private void invokeVMAndReplace(ClassNode proxy, ClassNode realClass) throws Throwable {
    VM vm = VM.constructNonInitializingVM(this);
    vm.explicitlyPreload(proxy, true);
    vm.explicitlyPreload(realClass, true);
    Class<?> callProxy = vm.loadClass("ProxyClass");
    try {
      callProxy.getMethod("clinitProxy").invoke(null); // invoke cut clinit, fields
      // in original class in vm get set
    } catch (InvocationTargetException e) {
      if (!(e.getCause() instanceof NullPointerException)) {
        // only ignore NPE from instruction removal
        throw e;
      } else {
        logger.info("NPE in " + realClass.name);
      }
    }
    realClass.methods.forEach(m -> {
      argumentInfer.inline(new MethodContext(realClass, m));
      decryptedArrayField = null;
      m.instructions.forEach(ain -> {
        if (isLocalField(realClass, ain) && ((FieldInsnNode) ain).desc.equals("[Ljava/lang/String;")) {
          decryptedArrayField = (FieldInsnNode) ain;
          try {
            decryptedFieldValue = (String[]) callProxy.getField(((FieldInsnNode) ain).name).get(null);
          } catch (Exception e) {
            logger.error("Failed to get decrypted field value in {}", referenceString(realClass, m));
          }
        }
      });

      InstructionModifier modifier = new InstructionModifier();

      loopConstantFrames(realClass, m, this, (ain, frame) -> {
        if (isZKMMethod(realClass, ain)) {
          decryptMethodsAndRewrite(realClass, callProxy, m, (MethodInsnNode) ain, frame, modifier);
        } else {
          tryReplaceFieldLoads(realClass, callProxy, m, ain, frame, modifier);
        }
      });

      modifier.apply(m);
    });
  }

  private boolean isLocalField(ClassNode cn, AbstractInsnNode ain) {
    if (ain.getOpcode() != GETSTATIC)
      return false;
    FieldInsnNode fin = ((FieldInsnNode) ain);
    // could be either array or normal string field, two
    // cases
    return fin.owner.equals(cn.name);
  }

  /**
   * Is ain a method call to enhanced method decryption?
   */
  private boolean isZKMMethod(ClassNode cn, AbstractInsnNode ain) {
    if (ain.getOpcode() != INVOKESTATIC)
      return false;
    MethodInsnNode min = ((MethodInsnNode) ain);
    return min.owner.equals(cn.name) && min.desc.matches(ENCHANCED_MODE_METHOD_DESC);
  }

  /**
   * Replace decryption methods that take ints as
   * argument and returns the decrypted String. This does
   * only occur sometimes!
   */
  private void decryptMethodsAndRewrite(ClassNode cn, Class<?> callProxy, MethodNode m, MethodInsnNode min,
                                        Frame<ConstantValue> frame, InstructionModifier modifier) {
    try {
      int argCount = (int) Arrays.stream(Type.getArgumentTypes(min.desc))
        .filter(t -> t.getClassName().equals(int.class.getName())).count();

      ConstantValue[] argValues = new ConstantValue[argCount];
      for (int i = 0; i < argValues.length; i++) {
        argValues[i] = frame.getStack(frame.getStackSize() - argValues.length + i);
      }

      if (Arrays.stream(argValues).allMatch(v -> v.isInteger() && v.isKnown())) {
        final Object[] invokeArgs = new Object[argCount];
        final Class<?>[] invokeArgTypes = new Class[argCount];
        final InsnList newInsns = new InsnList();
        for (int i = 0; i < argCount; i++) {
          invokeArgs[i] = argValues[i].getAsInteger();
          invokeArgTypes[i] = int.class;
          newInsns.add(new InsnNode(POP));
        }

        String decryptedLDC = (String) callProxy.getDeclaredMethod(min.name, invokeArgTypes).invoke(null, invokeArgs);
        if (!Strings.isHighUTF(decryptedLDC)) {
          newInsns.add(new LdcInsnNode(decryptedLDC));
          modifier.replace(min, newInsns);
          decrypted++;
        } else if (verbose) {
          logger.error("Failed decrypting {}", referenceString(cn, m));
        }
      } else if (verbose) {
        logger.warning("Failed to find arguments for {}", referenceString(cn, m));
      }
    } catch (Throwable t) {
      if (verbose) {
        t.printStackTrace();
      }
      logger.error("General failure in {}, {}", referenceString(cn, m), shortStacktrace(t));
    }
  }

  /**
   * Replace decrypted String[] and String fields in the
   * code. This is the hardest part
   */
  private void tryReplaceFieldLoads(ClassNode cn, Class<?> callProxy, MethodNode m, AbstractInsnNode ain,
                                    Frame<ConstantValue> frame, InstructionModifier modifier) {
    try {
      if (ain.getOpcode() == GETSTATIC) {
        FieldInsnNode fin = (FieldInsnNode) ain;
        if (isLocalField(cn, fin) && fin.desc.equals("Ljava/lang/String;")) {
          String decrypedString = (String) callProxy.getDeclaredField(fin.name).get(null);
          if (decrypedString == null) {
            // could be false call, not the decrypted string
            logger.warning("Possible false call in {} or failed decryption, single field is null: {}",
              referenceString(cn, m), fin.name);
            return;
          } else {
            // i don't know why we need NOP, but it only
            // works that way :confusion:
            modifier.replace(ain, new LdcInsnNode(decrypedString), new InsnNode(NOP));
            decrypted++;
          }
        }
      } else if (ain.getOpcode() == AALOAD) {
        ConstantValue previous = frame.getStack(frame.getStackSize() - 1);
        ConstantValue prePrevious = frame.getStack(frame.getStackSize() - 2);

        if (previous.getValue() != null) {
          int arrayIndex = previous.getAsInteger();
          Object reference = prePrevious.getValue();
          if (reference instanceof String[]) {
            String[] ref = (String[]) reference;
            String decryptedString = ref[arrayIndex];
            if (Strings.isHighUTF(decryptedString)) {
              logger.warning("String decryption in {} may have failed", referenceString(cn, m));
            }
            modifier.replace(ain, new InsnNode(POP2), new LdcInsnNode(decryptedString));
            decrypted++;
          }
        }
      }
    } catch (Throwable t) {
      if (verbose) {
        logger.error("Throwable", t);
      }
      logger.error("General failure in {}, {}", referenceString(cn, m), shortStacktrace(t));
    }
  }

  @Override
  public Object getFieldValueOrNull(BasicValue v, String owner, String name, String desc) {
    return decryptedArrayField != null && decryptedArrayField.owner.equals(owner) &&
      decryptedArrayField.name.equals(name) && decryptedArrayField.desc.equals(desc) ? decryptedFieldValue : null;
  }

  @Override
  public Object getMethodReturnOrNull(BasicValue v, String owner, String name, String desc,
                                      List<? extends ConstantValue> values) {
    return null;
  }

  @Override
  public ClassNode tryClassLoad(String name) {
    return null;
  }
}
