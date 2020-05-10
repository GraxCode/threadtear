package me.nov.threadtear.execution.zkm;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.*;

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
  private static final String ZKM_INVOKEDYNAMIC_REAL_BOOTSTRAP_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;Ljava/lang/String;Ljava/lang/invoke/MethodType;J)Ljava/lang/invoke/MethodHandle;";

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
                  rewrittenCode.add(new InsnNode(POP2)); // pop the long
                  if (idin.desc.startsWith("(IJ)")) {
                    rewrittenCode.add(new InsnNode(POP)); // pop the int, if the desc contains one
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
      vm.explicitlyPreloadWithClinit(cn); // make sure bootstrap class class has <clinit>
    }
    Class<?> proxyClass = vm.loadClass(cn.name.replace('/', '.'), true);
    Method bootstrap = null;
    for (Method m : proxyClass.getDeclaredMethods()) {
      if (Type.getMethodDescriptor(m).equals(ZKM_INVOKEDYNAMIC_REAL_BOOTSTRAP_DESC)) {
        bootstrap = m;
        break;
      }
    }
    if (bootstrap == null) {
      logger.warning("Failed to find real bootstrap method in {}: {}", referenceString(cn, null), bsm);
      return null;
    }
    if (idin.desc.startsWith("(J)") || idin.desc.startsWith("(IJ)")) {
      // they use the same decryption method with only an extra long as argument
      ConstantValue top = frame.getStack(frame.getStackSize() - 1);
      if (top.isKnown()) {
        logger.warning("Decryption long unknown in {}: {}", referenceString(cn, null), idin.desc);
        return null;
      }
      try {
        return (MethodHandle) bootstrap.invoke(null, DynamicReflection.getTrustedLookup(), null /* MutableCallSide, unused in method */, idin.name,
            MethodType.fromMethodDescriptorString(idin.desc, vm), (long) top.getValue());
      } catch (IllegalArgumentException e) {
        Threadtear.logger.error("One or more classes not in jar file: {}, cannot decrypt!", idin.desc);
      }
      return null;
    } else {
      logger.warning("Unimplemented or other dynamic desc variant in {}: {}", referenceString(cn, null), idin.desc);
      return null;
    }
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