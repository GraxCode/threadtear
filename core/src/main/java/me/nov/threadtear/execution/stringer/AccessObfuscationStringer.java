package me.nov.threadtear.execution.stringer;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import me.nov.threadtear.logging.LogWrapper;
import me.nov.threadtear.util.reflection.DynamicReflection;
import me.nov.threadtear.vm.IVMReferenceHandler;
import me.nov.threadtear.vm.Sandbox;
import me.nov.threadtear.vm.VM;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Map;

public class AccessObfuscationStringer extends Execution implements IVMReferenceHandler {

  private static final String STRINGER_INVOKEDYNAMIC_HANDLE_DESC =
          "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
  private Map<String, Clazz> classes;
  private int encrypted;
  private int decrypted;
  private boolean verbose;
  private VM vm;

  public AccessObfuscationStringer() {
    super(ExecutionCategory.STRINGER, "Access obfuscation removal",
            "Works for version 3 - 9.<br>Only works with invokedynamic obfuscation for now.",
            ExecutionTag.RUNNABLE, ExecutionTag.POSSIBLY_MALICIOUS);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    this.verbose = verbose;
    this.classes = classes;
    this.encrypted = 0;
    this.decrypted = 0;
    logger.info("Decrypting all invokedynamic references");
    logger.warning("Make sure all required libraries or dynamic classes are in the jar itself, or else some" +
            " invokedynamics cannot be deobfuscated!");

    this.vm = VM.constructVM(this); // can't use
    // non-initializing as decryption class needs <clinit>
//    vm.setDummyLoading(true);
    classes.values().forEach(this::decrypt);
    if (encrypted == 0) {
      logger.error("No access obfuscation matching stringer 3 - 9 has been found!");
      return false;
    }
    float decryptionRatio = Math.round((decrypted / (float) encrypted) * 100);
    logger.errorIf("Of a total {} encrypted references, {}% were successfully decrypted", decryptionRatio <= 0.25,
            encrypted, decryptionRatio);
    return decryptionRatio > 0.25;
  }

  private void decrypt(Clazz c) {
    logger.collectErrors(c);
    ClassNode cn = c.node;
    try {
      ClassNode proxy = Sandbox.createClassProxy(String.valueOf(cn.name.hashCode())); // can't use real
      // class name here
      proxy.sourceFile = cn.name + ".java";
      cn.methods.stream().filter(m -> m.desc.equals(STRINGER_INVOKEDYNAMIC_HANDLE_DESC))
              .forEach(m -> proxy.methods.add(m));
      vm.explicitlyPreload(proxy, true);
      Class<?> proxyClass = vm.loadClass(proxy.name, true);
      cn.methods.forEach(m -> {
        for (int i = 0; i < m.instructions.size(); i++) {
          AbstractInsnNode ain = m.instructions.get(i);
          if (ain.getOpcode() == INVOKEDYNAMIC) {
            InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) ain;
            if (idin.bsm != null) {
              Handle bsm = idin.bsm;
              if (bsm.getOwner().equals(cn.name) && bsm.getDesc().equals(STRINGER_INVOKEDYNAMIC_HANDLE_DESC)) {
                encrypted++;
                try {
                  allowReflection(true);
                  CallSite callsite = loadCallSiteFromVM(proxyClass, idin, bsm);
                  if (callsite != null) {
                    MethodHandleInfo methodInfo = DynamicReflection.revealMethodInfo(callsite.getTarget());
                    m.instructions.set(ain, DynamicReflection.getInstructionFromHandleInfo(methodInfo));
                    decrypted++;
                  }
                  allowReflection(false);
                } catch (Throwable t) {
                  if (verbose) {
                    logger.error("Throwable", t);
                  }
                  logger.error("Failed to get callsite using classloader in {}, {}", referenceString(cn, m),
                          shortStacktrace(t));
                }
              } else if (verbose) {
                logger.warning(
                        "Other bootstrap type in " + cn.name + ": " + bsm + " " + bsm.getOwner().equals(cn.name) + " " +
                                bsm.getDesc().equals(STRINGER_INVOKEDYNAMIC_HANDLE_DESC));
              }
            }
          }
        }
      });
    } catch (Throwable t) {
      if (verbose) {
        logger.error("Throwable", t);
      }
      logger.error("Failed load proxy for {}, {}", referenceString(cn, null), shortStacktrace(t));
    }
  }

  private CallSite loadCallSiteFromVM(Class<?> proxyClass, InvokeDynamicInsnNode idin, Handle bsm) throws Throwable {
    Method bootstrap = proxyClass.getDeclaredMethod(bsm.getName(), Object.class, Object.class, Object.class);
    try {
      return (CallSite) bootstrap
              .invoke(null, MethodHandles.lookup(), idin.name, MethodType.fromMethodDescriptorString(idin.desc, vm));
    } catch (IllegalArgumentException e) {
      LogWrapper.logger.error("One or more classes not in jar file: {}, cannot decrypt!", idin.desc);
    } catch (Exception e) {
      if (verbose)
        LogWrapper.logger.error("CallSite exception", e);
    }
    return null;
  }

  private boolean keepInitializer(ClassNode node) {
    // TODO this can probably be solved in a better way
    if (node.methods.stream().anyMatch(m -> m.desc.equals("(I)Ljava/lang/Class;")) &&
            node.methods.stream().anyMatch(m -> m.desc.equals("(I)Ljava/lang/reflect/Method;")) &&
            node.methods.stream().anyMatch(m -> m.desc.equals("(I)Ljava/lang/reflect/Field;"))) {
      // decryption class
      return true;
    }
    if (node.methods.stream().anyMatch(m -> m.desc.startsWith(
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke" +
                    "/MethodType"))) {
      // other decryption class
      return true;
    }
    return false;
  }

  @Override
  public ClassNode tryClassLoad(String name) {
    if (classes.containsKey(name)) {
      ClassNode node = classes.get(name).node;
      if (keepInitializer(node)) {
        return node;
      }
      // this is necessary because bootstrap class initializes the classes with Class.forName
      ClassNode clazz = Sandbox.fullClassProxy(node);
      clazz.methods.removeIf(m -> m.name.equals("<clinit>"));
      return clazz;
    }
    if (verbose)
      logger.warning("Unresolved: {}, decryption might fail", name);
    return null;
  }

}
