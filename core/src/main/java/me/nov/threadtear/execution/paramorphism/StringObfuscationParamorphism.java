package me.nov.threadtear.execution.paramorphism;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import me.nov.threadtear.logging.LogWrapper;
import me.nov.threadtear.util.format.Strings;
import me.nov.threadtear.vm.IVMReferenceHandler;
import me.nov.threadtear.vm.Sandbox;
import me.nov.threadtear.vm.VM;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Method;
import java.util.Map;

public class StringObfuscationParamorphism extends Execution implements IVMReferenceHandler {
  private boolean verbose;
  private Map<String, Clazz> classes;
  private int encrypted;
  private int decrypted;
  private VM vm;

  public StringObfuscationParamorphism() {
    super(ExecutionCategory.PARAMORPHISM, "String obfuscation removal",
            "Tested on version 2.1<br>Make sure to decrypt access obfuscation first.",
            ExecutionTag.RUNNABLE, ExecutionTag.POSSIBLY_MALICIOUS);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    this.verbose = verbose;
    this.classes = classes;
    this.encrypted = 0;
    this.decrypted = 0;
    this.vm = VM.constructVM(this);
    classes.values().forEach(this::decrypt);
    if (encrypted == 0) {
      logger.error("No strings matching Paramorphism 2.1 string obfuscation have been found!");
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
      for (int i = 0; i < m.instructions.size(); i++) {
        AbstractInsnNode ain = m.instructions.get(i);
        if (ain.getOpcode() == INVOKESTATIC) {
          MethodInsnNode min = (MethodInsnNode) ain;
          if (min.desc.equals("()Ljava/lang/String;") && classes.containsKey(min.owner)) {
            if (classes.get(min.owner).node.fields.stream().filter(f -> f.desc.equals("Ljava/util/Map;")).count() > 5) {
              encrypted++;
              String string = invokeVM(cn, m, min);
              if (string != null) {
                if (Strings.isHighUTF(string)) {
                  logger.warning("String may have not decrypted correctly in {}", referenceString(cn, m));
                }
                this.decrypted++;
                m.instructions.set(ain, new LdcInsnNode(string));
              }
            }
          }
        }
      }
    });
  }

  private String invokeVM(ClassNode cn, MethodNode m, MethodInsnNode min) {
    ClassNode proxy = Sandbox.createClassProxy(cn.name); // paramorphism
    // checks for method name and class name

    InsnList invoker = new InsnList();
    invoker.add(min.clone(null)); // clone method
    invoker.add(new InsnNode(ARETURN)); // return callsite
    String name = m.name.startsWith("<") ? '\0' + m.name : m.name;
    proxy.methods.add(Sandbox.createMethodProxy(invoker, name, min.desc)); // same desc
    if (!vm.isLoaded(proxy.name.replace('/', '.')))
      vm.explicitlyPreload(proxy, true); // we need no
    // clinit here
    try {
      Class<?> loadedProxy = vm.loadClass(proxy.name.replace('/', '.'));
      Method stringGetterBridge = loadedProxy.getDeclaredMethods()[0];
      return (String) stringGetterBridge.invoke(null);
    } catch (Throwable e) {
      if (verbose)
        LogWrapper.logger.error("Throwable", e);
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
