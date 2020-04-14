package me.nov.threadtear.execution;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Logger;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import me.nov.threadtear.Threadtear;
import me.nov.threadtear.asm.Clazz;

public abstract class Execution implements Opcodes {
	public String name;
	public ExecutionCategory type;
	public String description;
	protected static final Logger logger = Threadtear.logger;
	protected static final Random random = new Random();

	public Execution(ExecutionCategory type, String name, String description) {
		this.type = type;
		this.name = name;
		this.description = description;
	}

	@Override
	public String toString() {
		return name;
	}

	public abstract boolean execute(ArrayList<Clazz> classes, boolean verbose, boolean ignoreErr);

	protected Clazz getClass(ArrayList<Clazz> classes, String name) {
		return classes.stream().filter(c -> c.node.name.equals(name)).findFirst().orElse(null);
	}

	protected MethodNode getMethod(ClassNode node, String name, String desc) {
		if(node == null)
			return null;
		return node.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(desc)).findFirst().orElse(null);
	}
	
	protected MethodNode getStaticInitializer(ClassNode node) {
		return node.methods.stream().filter(m -> m.name.equals("<clinit>")).findFirst().orElse(null);
	}
}
