package me.nov.threadtear.execution.generic;

import java.util.ArrayList;
import java.util.List;

import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.util.Access;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;

public class FixAccess extends Execution {

	public FixAccess() {
		super(ExecutionCategory.GENERIC, "Fix obfuscated access", "Fixes obfuscated access like synthetic or bridge.",
				ExecutionTag.POSSIBLE_VERIFY_ERR, ExecutionTag.BETTER_DECOMPILE);
	}

	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose, boolean ignoreErr) {
		classes.stream().map(c -> c.node.methods).flatMap(List::stream)
				.forEach(m -> m.access = Access.removeAccess(m.access, ACC_SYNTHETIC, ACC_BRIDGE, ACC_DEPRECATED));
		classes.stream().map(c -> c.node.fields).flatMap(List::stream)
				.forEach(f -> f.access = Access.removeAccess(f.access, ACC_SYNTHETIC, ACC_BRIDGE, ACC_DEPRECATED));
		logger.info("Removed every synthetic, bridge and deprecated access");
		return true;
	}

}