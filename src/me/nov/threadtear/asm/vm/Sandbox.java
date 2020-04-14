package me.nov.threadtear.asm.vm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

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
}
