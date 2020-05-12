package me.nov.threadtear.execution.zkm;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.*;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import me.nov.threadtear.Threadtear;
import me.nov.threadtear.analysis.stack.*;
import me.nov.threadtear.execution.*;
import me.nov.threadtear.util.asm.Instructions;
import me.nov.threadtear.util.reflection.DynamicReflection;
import me.nov.threadtear.vm.*;

public class AccessObfusationZKM extends Execution implements IVMReferenceHandler, IConstantReferenceHandler {

  private static final String ZKM_INVOKEDYNAMIC_HANDLE_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";

  /**
   * The method that returns the real MethodHandle
   */
  private static final String ZKM_INVOKEDYNAMIC_REAL_BOOTSTRAP_DESC_REGEX = Pattern
      .quote("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;Ljava/lang/String;Ljava/lang/invoke/MethodType;") + "[JI]+" + Pattern.quote(")Ljava/lang/invoke/MethodHandle;");

  private Map<String, Clazz> classes;
  private int encrypted;
  private int decrypted;
  private boolean verbose;
  private VM vm;

  public AccessObfusationZKM() {
    super(ExecutionCategory.ZKM, "Access obfuscation removal", "Tested on ZKM 8 - 11, could work on newer versions too.<br>Only works with invokedynamic obfuscation for now.", ExecutionTag.RUNNABLE,
        ExecutionTag.POSSIBLY_MALICIOUS);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    this.verbose = verbose;
    this.classes = classes;
    this.encrypted = 0;
    this.decrypted = 0;
    logger.info("Decrypting all invokedynamic references, this could take some time!");
    logger.warning("Make sure all required libraries or dynamic classes are in the jar itself, or else some invokedynamics cannot be deobfuscated!");
    this.vm = VM.constructNonInitializingVM(this);
    this.vm.setDummyLoading(true);
    classes.values().stream().forEach(this::decrypt);
    if (encrypted == 0) {
      logger.error("No access obfuscation matching ZKM has been found!");
      return false;
    }
    float decryptionRatio = Math.round((decrypted / (float) encrypted) * 100);
    logger.errorIf("Of a total {} encrypted references, {}% were successfully decrypted", decryptionRatio <= 0.25, encrypted, decryptionRatio);
    return decryptionRatio > 0.25;
  }

  private void decrypt(Clazz cz) {
    // TODO if invokedynamic points to ordinal() of enum, the invokedynamic cannot be decrypted, as ordinal() does not exist at bytecode level, only runtime.
    ClassNode cn = cz.node;
    logger.collectErrors(cz);
    cn.methods.forEach(m -> {
      InsnList rewrittenCode = new InsnList();
      Map<LabelNode, LabelNode> labels = Instructions.cloneLabels(m.instructions);
      loopConstantFrames(cn, m, this, (ain, frame) -> {
        if (ain.getOpcode() == INVOKEDYNAMIC && frame != null) {
          InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) ain;
          if (idin.bsm != null) {
            Handle bsm = idin.bsm;
            if (bsm.getDesc().equals(ZKM_INVOKEDYNAMIC_HANDLE_DESC) && classes.values().stream().map(c -> c.node).anyMatch(node -> node.name.equals(bsm.getOwner()))) {
              encrypted++;
              try {
                allowReflection(true);
                MethodHandle handle = loadZKMBuriedHandleFromVM(classes.values().stream().map(c -> c.node).filter(node -> node.name.equals(bsm.getOwner())).findFirst().get(), idin, bsm, frame);
                if (handle != null) {
                  MethodHandleInfo methodInfo = DynamicReflection.revealMethodInfo(handle);
                  for (Type t : Type.getArgumentTypes("(" + new StringBuilder(matchZKMIdynParams(idin.desc)).reverse().toString() + ")V")) { // create a fake desc if you are too lazy
                    rewrittenCode.add(new InsnNode(t.getSize() > 1 ? POP2 : POP));
                  }
                  rewrittenCode.add(DynamicReflection.getInstructionFromHandleInfo(methodInfo));
                  decrypted++;
                  return;
                }
                allowReflection(false);
              } catch (Throwable t) {
                if (verbose) {
                  logger.error("Throwable", t);
                }
                logger.error("Failed to get callsite using classloader in {}, {}", referenceString(cn, m), shortStacktrace(t));
              }
            } else if (verbose) {
              logger.warning("Other bootstrap type in {}: {}", referenceString(cn, m), bsm);
            }
          }
        }
        rewrittenCode.add(ain.clone(labels));
      });
      if (rewrittenCode.size() > 0) {
        Instructions.updateInstructions(m, labels, rewrittenCode);
      }
    });
  }

  private MethodHandle loadZKMBuriedHandleFromVM(ClassNode cn, InvokeDynamicInsnNode idin, Handle bsm, Frame<ConstantValue> frame) throws Throwable {
    if (!vm.isLoaded(cn.name.replace('/', '.'))) {
      cn.methods.forEach(
          mn -> Instructions.isolateCallsThatMatch(mn, (name, desc) -> !name.equals(cn.name) && !name.matches("java/lang/.*"), (name, desc) -> !name.equals(cn.name) && !name.matches("java/lang/.*")));
      vm.explicitlyPreload(cn); // make sure bootstrap class class has <clinit>
    }
    Class<?> proxyClass = vm.loadClass(cn.name.replace('/', '.'), true);
    Method bootstrap = null;
    for (Method m : proxyClass.getDeclaredMethods()) {
      if (Type.getMethodDescriptor(m).matches(ZKM_INVOKEDYNAMIC_REAL_BOOTSTRAP_DESC_REGEX)) {
        bootstrap = m;
        break;
      }
    }
    if (bootstrap == null) {
      logger.warning("Failed to find real bootstrap method in {}: {}", referenceString(cn, null), bsm);
      return null;
    }
    if (idin.desc.matches("\\(.*[JI]+\\).*")) {
      try {
        int invokedynamicParams = matchZKMIdynParams(idin.desc).length();
        List<Object> args = new ArrayList<>(
            Arrays.asList(DynamicReflection.getTrustedLookup(), null /* MutableCallSite, unused in method */, idin.name, MethodType.fromMethodDescriptorString(idin.desc, vm)));
        for (int i = 0; i < invokedynamicParams; i++) {
          ConstantValue stack = frame.getStack(frame.getStackSize() - invokedynamicParams + i);
          if (!stack.isKnown()) {
            Threadtear.logger.warning("Stack value depth {} is unknown in {}, could be decryption class itself", i, referenceString(cn, null));
            return null;
          }
          args.add(stack.getValue());
        }
        return (MethodHandle) bootstrap.invoke(null, args.toArray());
      } catch (IllegalArgumentException e) {
        Threadtear.logger.error("IllegalArgumentException: One or more classes not in jar file or stack not matching desc: {}, cannot decrypt!", idin.desc);
      }
      return null;
    } else {
      logger.warning("Unimplemented or other dynamic desc variant in {}: {}", referenceString(cn, null), idin.desc);
      return null;
    }
  }

  private String matchZKMIdynParams(String desc) {

    Pattern p = Pattern.compile("[JI]+\\)");
    Matcher m = p.matcher(desc);
    if (!m.find()) {
      Threadtear.logger.error("Pattern matching failed for {}", desc);
      return "";
    }
    String group = m.group();
    return group.substring(0, group.length() - 1); // remove ")"
  }

  @Override
  public ClassNode tryClassLoad(String name) {
    return classes.containsKey(name.replace('.', '/')) ? classes.get(name).node : null;
  }

  @Override
  public Object getFieldValueOrNull(BasicValue v, String owner, String name, String desc) {
    return null;
  }

  @Override
  public Object getMethodReturnOrNull(BasicValue v, String owner, String name, String desc, List<? extends ConstantValue> values) {
    return null;
  }

}