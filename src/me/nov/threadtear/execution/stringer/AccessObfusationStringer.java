package me.nov.threadtear.execution.stringer;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.Map;

import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.*;

import me.nov.threadtear.execution.*;
import me.nov.threadtear.io.Clazz;
import me.nov.threadtear.util.reflection.DynamicReflection;
import me.nov.threadtear.vm.*;

public class AccessObfusationStringer extends Execution implements IVMReferenceHandler {

  private static final String STRINGER_INVOKEDYNAMIC_HANDLE_DESC = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
  private Map<String, Clazz> classes;
  private int encrypted;
  private int decrypted;
  private boolean verbose;
  private VM vm;

  public AccessObfusationStringer() {
    super(ExecutionCategory.STRINGER, "Access obfuscation removal", "Works for version 3 - 9.<br>Only works with invokedynamic obfuscation for now.", ExecutionTag.RUNNABLE,
        ExecutionTag.POSSIBLY_MALICIOUS);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    this.verbose = verbose;
    this.classes = classes;
    this.encrypted = 0;
    this.decrypted = 0;
    logger.info("Decrypting all invokedynamic references");
    this.vm = VM.constructVM(this); // can't use non-initializing as decryption class needs <clinit>
    classes.values().stream().map(c -> c.node).forEach(this::decrypt);
    if (encrypted == 0) {
      logger.severe("No access obfuscation matching stringer 3 - 9 has been found!");
      return false;
    }
    float decryptionRatio = Math.round((decrypted / (float) encrypted) * 100);
    logger.info("Of a total " + encrypted + " encrypted references, " + (decryptionRatio) + "% were successfully decrypted");
    return decryptionRatio > 0.25;
  }

  private void decrypt(ClassNode cn) {
    try {
      ClassNode proxy = Sandbox.createClassProxy(String.valueOf(cn.name.hashCode())); // can't use real class name here
      proxy.sourceFile = cn.name + ".java";
      cn.methods.stream().filter(m -> m.desc.equals(STRINGER_INVOKEDYNAMIC_HANDLE_DESC)).forEach(m -> {
        proxy.methods.add(m);
      });
      vm.explicitlyPreloadNoClinit(proxy);
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
                  CallSite callsite = loadCallSiteFromVM(proxyClass, cn, idin, bsm);
                  MethodHandleInfo methodInfo = DynamicReflection.revealMethodInfo(callsite.getTarget());
                  m.instructions.set(ain, DynamicReflection.getInstructionFromHandleInfo(methodInfo));
                  decrypted++;
                } catch (Throwable t) {
                  if (verbose) {
                    logger.error("Throwable", t);
                  }
                  logger.severe("Failed to get callsite using classloader in " + cn.name + "." + m.name + m.desc + ": " + t.getClass().getName() + ", " + t.getMessage());
                }
              } else if (verbose) {
                logger.warning("Other bootstrap type in " + cn.name + ": " + bsm + " " + bsm.getOwner().equals(cn.name) + " " + bsm.getDesc().equals(STRINGER_INVOKEDYNAMIC_HANDLE_DESC));
              }
            }
          }
        }
      });
    } catch (Throwable t) {
      if (verbose) {
        logger.error("Throwable", t);
      }
      logger.severe("Failed load proxy for " + cn.name + t.getClass().getName() + ", " + t.getMessage());
    }
  }

  private CallSite loadCallSiteFromVM(Class<?> proxyClass, ClassNode cn, InvokeDynamicInsnNode idin, Handle bsm) throws Throwable {
    Method bootstrap = proxyClass.getDeclaredMethod(bsm.getName(), Object.class, Object.class, Object.class);
    CallSite callsite = (CallSite) bootstrap.invoke(null, MethodHandles.lookup(), idin.name, MethodType.fromMethodDescriptorString(idin.desc, vm));
    return callsite;
  }

  private boolean keepInitializer(ClassNode node) {
    // TODO this can probably be solved in a better way
    if (node.methods.stream().anyMatch(m -> m.desc.equals("(I)Ljava/lang/Class;")) && node.methods.stream().anyMatch(m -> m.desc.equals("(I)Ljava/lang/reflect/Method;"))
        && node.methods.stream().anyMatch(m -> m.desc.equals("(I)Ljava/lang/reflect/Field;"))) {
      // decryption class
      return true;
    }
    if (node.methods.stream().anyMatch(m -> m.desc.startsWith("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType"))) {
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
      logger.warning("Unresolved: " + name + ", decryption might fail");
    return null;
  }

}