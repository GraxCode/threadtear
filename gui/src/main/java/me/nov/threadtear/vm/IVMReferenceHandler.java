package me.nov.threadtear.vm;

import org.objectweb.asm.tree.ClassNode;

public interface IVMReferenceHandler {
  ClassNode tryClassLoad(String name);
}
