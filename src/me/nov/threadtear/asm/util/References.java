package me.nov.threadtear.asm.util;

import java.util.Map;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import me.nov.threadtear.util.Descriptor;

public class References {

	public static int remapInstruction(Map<String, String> map, AbstractInsnNode ain) {
		if (ain instanceof MethodInsnNode) {
			MethodInsnNode min = (MethodInsnNode) ain;
			min.owner = Descriptor.fixDesc(min.owner, map);
			min.desc = Descriptor.fixDesc(min.desc, map);
		} else if (ain instanceof FieldInsnNode) {
			FieldInsnNode fin = (FieldInsnNode) ain;
			fin.owner = Descriptor.fixDesc(fin.owner, map);
			fin.desc = Descriptor.fixDesc(fin.desc, map);
		} else if (ain instanceof TypeInsnNode) {
			TypeInsnNode tin = (TypeInsnNode) ain;
			tin.desc = Descriptor.fixDesc(tin.desc, map);
		} else if (ain instanceof InvokeDynamicInsnNode) {
			InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) ain;
			idin.desc = Descriptor.fixDesc(idin.desc, map);
			for (int i = 0; i < idin.bsmArgs.length; i++) {
				Object o = idin.bsmArgs[i];
				if (o instanceof Handle) {
					Handle handle = (Handle) o;
					idin.bsmArgs[i] = new Handle(handle.getTag(), Descriptor.fixDesc(handle.getOwner(), map), handle.getName(), Descriptor.fixDesc(handle.getDesc(), map), handle.isInterface());
				} else if (o instanceof Type) {
					Type type = (Type) o;
					idin.bsmArgs[i] = Type.getType(Descriptor.fixDesc(type.getDescriptor(), map));
				}
			}
		} else if (ain instanceof LdcInsnNode) {
			LdcInsnNode lin = (LdcInsnNode) ain;
			if (lin.cst instanceof Type) {
				Type t = (Type) lin.cst;
				lin.cst = Type.getType(Descriptor.fixDesc(t.getDescriptor(), map));
			}
		} else {
			return 0;
		}
		return 1;
	}

	// cn.attrs.forEach(at -> at.type = Descriptor.fixDesc(at.type, map)); don't
	// know
	public static void remapMethodType(Map<String, String> map, MethodNode mn) {
		mn.desc = Descriptor.fixDesc(mn.desc, map);
		for (int i = 0; i < mn.exceptions.size(); i++) {
			mn.exceptions.set(i, Descriptor.fixDesc(mn.exceptions.get(i), map));
		}
		mn.tryCatchBlocks.forEach(tcb -> tcb.type = Descriptor.fixDesc(tcb.type, map));
		if (mn.localVariables != null)
			mn.localVariables.forEach(lv -> lv.desc = Descriptor.fixDesc(lv.desc, map));
		if (mn.invisibleAnnotations != null)
			mn.invisibleAnnotations.forEach(a -> a.desc = Descriptor.fixDesc(a.desc, map));
		if (mn.visibleAnnotations != null)
			mn.visibleAnnotations.forEach(a -> a.desc = Descriptor.fixDesc(a.desc, map));
		if (mn.invisibleTypeAnnotations != null)
			mn.invisibleTypeAnnotations.forEach(a -> a.desc = Descriptor.fixDesc(a.desc, map));
		if (mn.visibleTypeAnnotations != null)
			mn.visibleTypeAnnotations.forEach(a -> a.desc = Descriptor.fixDesc(a.desc, map));
		mn.signature = null;
		// TODO remap signature, is actually not necessary. other annotations, etc..
	}

	public static void remapClassType(Map<String, String> map, ClassNode cn) {
		for (int i = 0; i < cn.interfaces.size(); i++) {
			cn.interfaces.set(i, Descriptor.fixDesc(cn.interfaces.get(i), map));
		}
		cn.superName = Descriptor.fixDesc(cn.superName, map);
		if (cn.invisibleAnnotations != null)
			cn.invisibleAnnotations.forEach(a -> a.desc = Descriptor.fixDesc(a.desc, map));
		if (cn.visibleAnnotations != null)
			cn.visibleAnnotations.forEach(a -> a.desc = Descriptor.fixDesc(a.desc, map));
		if (cn.invisibleTypeAnnotations != null)
			cn.invisibleTypeAnnotations.forEach(a -> a.desc = Descriptor.fixDesc(a.desc, map));
		if (cn.visibleTypeAnnotations != null)
			cn.visibleTypeAnnotations.forEach(a -> a.desc = Descriptor.fixDesc(a.desc, map));
		cn.signature = null;
		// TODO more cases
	}

	public static void remapFieldType(Map<String, String> map, FieldNode fn) {
		fn.desc = Descriptor.fixDesc(fn.desc, map);
		if (fn.invisibleAnnotations != null)
			fn.invisibleAnnotations.forEach(a -> a.desc = Descriptor.fixDesc(a.desc, map));
		if (fn.visibleAnnotations != null)
			fn.visibleAnnotations.forEach(a -> a.desc = Descriptor.fixDesc(a.desc, map));
		if (fn.invisibleTypeAnnotations != null)
			fn.invisibleTypeAnnotations.forEach(a -> a.desc = Descriptor.fixDesc(a.desc, map));
		if (fn.visibleTypeAnnotations != null)
			fn.visibleTypeAnnotations.forEach(a -> a.desc = Descriptor.fixDesc(a.desc, map));
		fn.signature = null;
	}
}
