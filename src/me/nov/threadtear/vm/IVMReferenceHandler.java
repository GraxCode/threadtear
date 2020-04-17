package me.nov.threadtear.vm;

import org.objectweb.asm.tree.ClassNode;

public interface IVMReferenceHandler {
	public ClassNode tryClassLoad(String name);
}
