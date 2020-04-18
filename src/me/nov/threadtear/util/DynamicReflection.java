package me.nov.threadtear.util;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class DynamicReflection implements Opcodes {

	private static final String BMHL = "java.lang.invoke.BoundMethodHandle$Species_L";

	/**
	 * This probably only works for java 8
	 */
	public static MethodHandleInfo revealMethodInfo(CallSite callsite) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		MethodHandle handle = callsite.getTarget();
		if (handle.getClass().getName().startsWith(BMHL)) {
			Field original = handle.getClass().getDeclaredField("argL0");
			original.setAccessible(true);
			handle = (MethodHandle) original.get(handle);
		}
		Field impl = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
		impl.setAccessible(true);
		MethodHandles.Lookup trustedLookup = (Lookup) impl.get(null);
		// MethodHandles.lookup() also works but isn't working always
		return trustedLookup.revealDirect(handle);
	}

	public static AbstractInsnNode getInstructionFromHandleInfo(MethodHandleInfo direct) throws Exception {
		Class<?> declaringClass = direct.getDeclaringClass();
		String name = direct.getName();
		MethodType methodType = direct.getMethodType();
		int op = bootstrapTagToOp(direct.getReferenceKind());
		if (op <= H_PUTSTATIC) {
			if (op <= H_GETSTATIC) {
				// method handle treats field retrieving as a method ()X
				return new FieldInsnNode(op, declaringClass.getName().replace('.', '/'), name, methodType.toMethodDescriptorString().substring(2));
			} else {
				// method handle treats field putting as a method (returning void) -> (X)V
				String mds = methodType.toMethodDescriptorString();
				return new FieldInsnNode(op, declaringClass.getName().replace('.', '/'), name, mds.substring(1, mds.lastIndexOf(')')));
			}
		} else {
			return new MethodInsnNode(op, declaringClass.getName().replace('.', '/'), name, methodType.toMethodDescriptorString());
		}
	}

	public static int bootstrapTagToOp(int tag) {
		switch (tag) {
		case H_GETFIELD:
			return GETFIELD;
		case H_GETSTATIC:
			return GETSTATIC;
		case H_PUTFIELD:
			return PUTFIELD;
		case H_PUTSTATIC:
			return PUTSTATIC;
		case H_INVOKEVIRTUAL:
			return INVOKEVIRTUAL;
		case H_INVOKESTATIC:
			return INVOKESTATIC;
		case H_INVOKESPECIAL:
		case H_NEWINVOKESPECIAL:
			return INVOKESPECIAL;
		case H_INVOKEINTERFACE:
			return INVOKEINTERFACE;
		}
		throw new IllegalArgumentException("not a bootstrap tag: " + tag);
	}
}
