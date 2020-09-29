package me.nov.threadtear.execution.paramorphism;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.*;

import me.nov.threadtear.logging.LogWrapper;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import me.nov.threadtear.execution.*;
import me.nov.threadtear.util.asm.Instructions;
import me.nov.threadtear.util.reflection.DynamicReflection;
import me.nov.threadtear.vm.*;

public class AccessObfuscationParamorphism extends Execution implements IVMReferenceHandler {

  private static final String PARAMORPHISM_INVOKEDYNAMIC_HANDLE_DESC = "\\(Ljava/lang/invoke/MethodHandles\\$Lookup;" +
          "Ljava/lang/String;Ljava/lang/invoke/MethodType;[JI]+\\)Ljava/lang/invoke/CallSite;";
  private static final String DEPTH_TEST_METHOD = "([Ljava/lang/StackTraceElement;I)I";
  private Map<String, Clazz> classes;
  private int encrypted;
  private int decrypted;
  private boolean verbose;
  private VM vm;

  public AccessObfuscationParamorphism() {
    super(ExecutionCategory.PARAMORPHISM, "Access obfuscation removal",
            "Tested on version 2.1.<br>This is unfinished: Doesn't work on " +
                    "constructors and static initializers.", ExecutionTag.RUNNABLE, ExecutionTag.POSSIBLY_MALICIOUS);
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
    classes.values().forEach(this::patchThrowableDepth);
    logger.info("Make sure to remove bad attributes first!");
    logger.info("Starting decryption, this could take some time!");
    classes.values().forEach(this::decrypt);
    if (encrypted == 0) {
      logger.error("No access obfuscation matching Paramorphism 2.1 have been found!");
      return false;
    }
    float decryptionRatio = Math.round((decrypted / (float) encrypted) * 100);
    logger.errorIf("Of a total {} encrypted references, {}% were successfully decrypted", decryptionRatio <= 0.25,
            encrypted, decryptionRatio);
    return decryptionRatio > 0.25;
  }

  private void patchThrowableDepth(Clazz c) {
    c.node.methods.forEach(m -> {
      if (m.desc.equals(DEPTH_TEST_METHOD)) {
        InsnList il = new InsnList();
        il.add(new InsnNode(ICONST_2));
        il.add(new InsnNode(IRETURN));
        Instructions.updateInstructions(m, null, il);
        logger.info("Patched depth test method in {}", c.node.name);
      }
    });
  }

  private void decrypt(Clazz c) {
    logger.collectErrors(c);
    ClassNode cn = c.node;
    try {
      cn.methods.forEach(m -> {
        for (int i = 0; i < m.instructions.size(); i++) {
          AbstractInsnNode ain = m.instructions.get(i);
          if (ain.getOpcode() == INVOKEDYNAMIC) {
            // we need an own VM for each invokedynamic.
            // this slows down everything but is the only
            // option.
            this.vm = VM.constructVM(this);
            vm.setDummyLoading(true);
            InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) ain;
            if (idin.bsm != null) {
              Handle bsm = idin.bsm;
              if (bsm.getDesc().matches(PARAMORPHISM_INVOKEDYNAMIC_HANDLE_DESC)) {
                if (!classes.containsKey(bsm.getOwner())) {
                  logger.error("Missing decryption class: {}", bsm.getOwner());
                  continue;
                }
                encrypted++;
                try {
                  allowReflection(true);
                  if (!vm.isLoaded(bsm.getOwner().replace('/', '.')))
                    vm.explicitlyPreload(classes.get(bsm.getOwner()).node, false); // WITH clinit
                  CallSite callsite = loadCallSiteFromVM(cn, m, idin, bsm);
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
                                bsm.getDesc().equals(PARAMORPHISM_INVOKEDYNAMIC_HANDLE_DESC));
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

  // TODO seems to throw some exception sometimes, but
  //  works 90%
  private CallSite loadCallSiteFromVM(ClassNode cn, MethodNode m, InvokeDynamicInsnNode idin, Handle bsm)
          throws Throwable {
    ClassNode proxy = Sandbox.createClassProxy(cn.name); // paramorphism
    // checks for method name and class name

    InsnList invoker = new InsnList();
    Type[] types = Type.getArgumentTypes(bsm.getDesc());
    int var = 0;
    for (Type type : types) {
      invoker.add(new VarInsnNode(type.getOpcode(ILOAD), var));
      var += type.getSize();
    }
    invoker.add(new MethodInsnNode(INVOKESTATIC, bsm.getOwner(), bsm.getName(), bsm.getDesc())); // invokedynamic fake
    invoker.add(new InsnNode(ARETURN)); // return callsite
    String name = m.name.startsWith("<") ? '\0' + m.name : m.name;
    proxy.methods.add(Sandbox.createMethodProxy(invoker, name, bsm.getDesc())); // same desc
    vm.explicitlyPreload(proxy);
    Class<?> loadedProxy = vm.loadClass(proxy.name.replace('/', '.'));
    try {
      List<Object> args = new ArrayList<>(Arrays.asList(DynamicReflection.getTrustedLookup(), idin.name,
              MethodType.fromMethodDescriptorString(idin.desc, vm)));
      // extra arguments
      // paramorphism stores those extra parameters in
      // bsmArgs
      args.addAll(Arrays.asList(idin.bsmArgs));
      Method bootstrapBridge = loadedProxy.getDeclaredMethods()[0];
      return (CallSite) bootstrapBridge.invoke(null, args.toArray());
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
      LogWrapper.logger.error("One or more classes not in jar file: {}, cannot decrypt!", idin.desc);
    } catch (Throwable e) {
      if (verbose)
        LogWrapper.logger.error("CallSite exception", e);
    }
    return null;
  }

  @Override
  public ClassNode tryClassLoad(String name) {
    if (classes.containsKey(name)) {
      return classes.get(name).node;
    }
    if (verbose)
      logger.warning("Unresolved: {}, decryption might fail", name);
    return null;
  }

}
