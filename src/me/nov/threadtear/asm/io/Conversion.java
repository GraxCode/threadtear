package me.nov.threadtear.asm.io;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class Conversion {
	public static byte[] toBytecode(ClassNode cn, boolean useMaxs) {
		ClassWriter cw = new ClassWriter(useMaxs ? ClassWriter.COMPUTE_MAXS : ClassWriter.COMPUTE_FRAMES);
		cn.accept(cw);
		byte[] b = cw.toByteArray();
		return b;
	}

	public static byte[] toBytecode0(ClassNode cn) {
		ClassWriter cw = new ClassWriter(0);
		cn.accept(cw);
		byte[] b = cw.toByteArray();
		return b;
	}

	public static ClassNode toNode(final byte[] bytez) {
		ClassReader cr = new ClassReader(bytez);
		ClassNode cn = new ClassNode();
		try {
			cr.accept(cn, ClassReader.EXPAND_FRAMES);
		} catch (Exception e) {
			try {
				cr.accept(cn, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
			} catch (Exception e2) {
				// e2.printStackTrace();
			}
		}
		cr = null;
		return cn;
	}
}
