package me.nov.threadtear.execution.generic;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.util.Instructions;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;

public class ConvertCompareInstructions extends Execution {

	private int count;

	public ConvertCompareInstructions() {
		super(ExecutionCategory.GENERIC, "Remove abnormal compare instructions",
				"Changes double-, float- and long-compare instructions to real invocations.<br>This is used by some obfuscators to trick decompilers.<br>Could slightly affect code behavior, as NaN handling is different.",
				ExecutionTag.RUNNABLE, ExecutionTag.BETTER_DECOMPILE);
	}

	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose) {
		this.count = 0;
		classes.stream().map(c -> c.node.methods).flatMap(List::stream).forEach(m -> {
			for (AbstractInsnNode ain : m.instructions) {
				AbstractInsnNode next = Instructions.getRealNext(ain);
				// keep them if they are jvm generated (for loops)
				if (next != null && next.getType() != AbstractInsnNode.JUMP_INSN) {
					switch (ain.getOpcode()) {
					case LCMP:
						m.instructions.set(ain, new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "compare", "(JJ)I"));
						count++;
						continue;
					case FCMPL:
					case FCMPG:
						m.instructions.set(ain, new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "compare", "(FF)I"));
						count++;
						continue;
					case DCMPL:
					case DCMPG:
						m.instructions.set(ain, new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "compare", "(DD)I"));
						count++;
						continue;
					}
				}
			}
		});

		logger.info("Removed " + count + " abnormal dcmp, fcmp and lcmp instructions");
		return count > 0;
	}
}