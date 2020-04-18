package me.nov.threadtear.execution.other;

import java.util.ArrayList;

import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;

public class Java7Compatibility extends Execution {

	public Java7Compatibility() {
		super(ExecutionCategory.OTHER, "Make java 7 compatible", "Only works when no lambda expressions and no java 8 specific methods are present.", ExecutionTag.POSSIBLE_DAMAGE);
	}

	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose) {
		classes.stream().map(c -> c.node).forEach(c -> c.version = 51);
		logger.info("Compatibility changed to java 7+");
		return true;
	}
}