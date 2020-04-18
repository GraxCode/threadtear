package me.nov.threadtear.execution.tools;

import java.util.ArrayList;

import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;

public class Java8Compatibility extends Execution {

	public Java8Compatibility() {
		super(ExecutionCategory.TOOLS, "Make java 8 compatible", "Only works when no java 9+ specific methods are present.", ExecutionTag.POSSIBLE_DAMAGE);
	}

	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose) {
		classes.stream().map(c -> c.node).forEach(c -> c.version = 52);
		logger.info("Compatibility changed to java 8+");
		return true;
	}
}