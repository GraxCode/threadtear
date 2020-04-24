package me.nov.threadtear.io;

import java.util.jar.JarEntry;

import org.objectweb.asm.tree.ClassNode;

public class Clazz {

	public boolean transform = true;
	public ClassNode node;
	public JarEntry oldEntry;

	public Clazz(ClassNode node, JarEntry oldEntry) {
		super();
		this.node = node;
		this.oldEntry = oldEntry;
	}
}
