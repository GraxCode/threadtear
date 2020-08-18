package me.nov.threadtear.vm;

import me.nov.threadtear.util.asm.Instructions;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Map;
import java.util.stream.Collectors;

public final class Sandbox implements Opcodes {
  private Sandbox() {
  }

  public static MethodNode createMethodProxy(InsnList code, String name, String desc) {
    boolean isConstructor = name.equals("<init>");
    MethodNode proxy = new MethodNode(isConstructor ? ACC_PUBLIC : ACC_PUBLIC | ACC_STATIC, name, desc, null, null);
    if (isConstructor) {
      proxy.instructions.add(generateConstructorSuperCall());
    }
    proxy.instructions.add(code);
    proxy.maxStack = 1337;
    proxy.maxLocals = 1337;
    return proxy;
  }

  private static InsnList generateConstructorSuperCall() {
    InsnList list = new InsnList();
    list.add(new VarInsnNode(ALOAD, 0));
    list.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V"));
    return list;
  }

  public static ClassNode createClassProxy(String name) {
    ClassNode proxy = new ClassNode();
    proxy.access = ACC_PUBLIC;
    proxy.version = 52;
    proxy.name = name;
    proxy.superName = "java/lang/Object";
    return proxy;
  }

  public static MethodNode copyMethod(MethodNode original) {
    MethodNode mn = new MethodNode(original.access, original.name, original.desc, original.signature,
      original.exceptions.toArray(new String[0]));
    InsnList copy = new InsnList();
    Map<LabelNode, LabelNode> labels = Instructions.cloneLabels(original.instructions);
    for (AbstractInsnNode ain : original.instructions) {
      copy.add(ain.clone(labels));
    }
    if (original.tryCatchBlocks != null)
      mn.tryCatchBlocks = original.tryCatchBlocks.stream()
        .map(tcb -> new TryCatchBlockNode(tcb.start, tcb.end, tcb.handler, tcb.type))
        .collect(Collectors.toList());
    if (original.localVariables != null)
      mn.localVariables = original.localVariables.stream()
        .map(lv -> new LocalVariableNode(lv.name, lv.desc, lv.signature, lv.start, lv.end, lv.index))
        .collect(Collectors.toList());
    Instructions.updateInstructions(mn, labels, copy);
    mn.maxStack = 1337;
    mn.maxLocals = 1337;
    return mn;
  }

  public static ClassNode fullClassProxy(ClassNode classNode) {
    return fullClassProxy(classNode, false);
  }

  public static ClassNode fullClassProxy(ClassNode cn, boolean keepSuperClass) {
    ClassNode clone = new ClassNode();
    clone.access = cn.access;
    clone.version = 52;
    clone.name = cn.name;
    clone.sourceFile = cn.sourceFile;
    if (!cn.superName.equals("java/lang/Object") && keepSuperClass)
      clone.superName = cn.superName;
    else
      clone.superName = "java/lang/Object";
    cn.fields.forEach(f -> clone.fields.add(new FieldNode(f.access, f.name, f.desc, f.signature, f.value)));
    cn.methods.forEach(m -> clone.methods.add(copyMethod(m)));
    return clone;
  }

}
