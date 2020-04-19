package me.nov.threadtear.vm;

import java.util.Map;
import java.util.stream.Collectors;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import me.nov.threadtear.asm.util.Instructions;

public class Sandbox implements Opcodes {

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
		MethodNode mn = new MethodNode(original.access, original.name, original.desc, original.signature, original.exceptions.toArray(new String[0]));
		InsnList copy = new InsnList();
		Map<LabelNode, LabelNode> labels = Instructions.cloneLabels(original.instructions);
		for (AbstractInsnNode ain : original.instructions) {
			copy.add(ain.clone(labels));
		}
		mn.tryCatchBlocks = original.tryCatchBlocks.stream().map(tcb -> new TryCatchBlockNode(tcb.start, tcb.end, tcb.handler, tcb.type)).collect(Collectors.toList());
		mn.localVariables = original.localVariables.stream().map(lv -> new LocalVariableNode(lv.name, lv.desc, lv.signature, lv.start, lv.end, lv.index)).collect(Collectors.toList());
		Instructions.updateInstructions(mn, labels, copy);
		mn.maxStack = 1337;
		mn.maxLocals = 1337;
		return mn;
	}
}
