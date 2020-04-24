package me.nov.threadtear.execution;

import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import me.nov.threadtear.Threadtear;
import me.nov.threadtear.io.Clazz;

public abstract class Execution implements Opcodes {
	public final String name;
	public final ExecutionCategory type;
	public final String description;
	public final ExecutionTag[] tags;

	protected static final Logger logger = Threadtear.logger;
	protected static final Random random = new Random();

	public Execution(ExecutionCategory type, String name, String description, ExecutionTag... tags) {
		this.type = type;
		this.name = name;
		this.description = description;
		this.tags = tags;
	}

	@Override
	public String toString() {
		return name;
	}

	public abstract boolean execute(Map<String, Clazz> map, boolean verbose);

	protected MethodNode getMethod(ClassNode node, String name, String desc) {
		if (node == null)
			return null;
		return node.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(desc)).findFirst().orElse(null);
	}

	protected MethodNode getStaticInitializer(ClassNode node) {
		return node.methods.stream().filter(m -> m.name.equals("<clinit>")).findFirst().orElse(null);
	}
}
