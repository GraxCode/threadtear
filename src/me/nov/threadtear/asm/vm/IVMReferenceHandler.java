package me.nov.threadtear.asm.vm;

import org.objectweb.asm.tree.ClassNode;

public interface IVMReferenceHandler {
	public ClassNode tryClassLoad(String name);
}
