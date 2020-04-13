package me.nov.threadtear.execution;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Logger;

import org.objectweb.asm.Opcodes;

import me.nov.threadtear.Threadtear;
import me.nov.threadtear.asm.Clazz;

public abstract class Execution implements Opcodes {
	public String name;
	public ExecutionType type;
	public String description;
	protected static final Logger logger = Threadtear.logger;
	protected static final Random random = new Random();
	public Execution(ExecutionType type, String name, String description) {
		this.type = type;
		this.name = name;
		this.description = description;
	}

	@Override
	public String toString() {
		return name;
	}

	public abstract boolean execute(ArrayList<Clazz> classes, boolean verbose, boolean ignoreErr);
}
