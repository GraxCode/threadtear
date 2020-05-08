package me.nov.threadtear.execution.zkm;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.*;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;

import me.nov.threadtear.analysis.stack.*;
import me.nov.threadtear.execution.*;
import me.nov.threadtear.io.Clazz;
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
    this.vm = VM.constructNonInitializingVM(this);
    classes.values().stream().map(c -> c.node).forEach(this::decrypt);
    if (encrypted == 0) {
      logger.severe("No access obfuscation matching ZKM has been found!");
      return false;
    }
    float decryptionRatio = Math.round((decrypted / (float) encrypted) * 100);
    logger.info("Of a total {} encrypted references, {}% were successfully decrypted", encrypted, decryptionRatio);
    return decryptionRatio > 0.25;
  }

  private void decrypt(ClassNode cn) {
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
                ConstantValue top = frame.getStack(frame.getStackSize() - 1);
                if (top.isKnown()) {
                  MethodHandle handle = loadZKMBuriedHandleFromVM(classes.values().stream().map(c -> c.node).filter(node -> node.name.equals(bsm.getOwner())).findFirst().get(), idin, bsm,
                      (long) top.getValue());
                  if (handle != null) {
                    MethodHandleInfo methodInfo = DynamicReflection.revealMethodInfo(handle);
                    rewrittenCode.add(new InsnNode(POP2)); // pop the long
                    rewrittenCode.add(DynamicReflection.getInstructionFromHandleInfo(methodInfo));
                    decrypted++;
                    return;
                  }
                }
              } catch (Throwable t) {
                if (verbose) {
                  logger.error("Throwable", t);
                }
                logger.severe("Failed to get callsite using classloader in ", referenceString(cn, m), shortStacktrace(t));
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
    System.out.println();
  }

  private MethodHandle loadZKMBuriedHandleFromVM(ClassNode cn, InvokeDynamicInsnNode idin, Handle bsm, long decryptionLong) throws Throwable {
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
    return (MethodHandle) bootstrap.invoke(null, DynamicReflection.getTrustedLookup(), null /* MutableCallSide, unused in method */, idin.name, MethodType.fromMethodDescriptorString(idin.desc, vm),
        decryptionLong);
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