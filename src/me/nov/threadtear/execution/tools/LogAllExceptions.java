package me.nov.threadtear.execution.tools;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import me.nov.threadtear.io.Clazz;
import me.nov.threadtear.util.asm.Instructions;

public class LogAllExceptions extends Execution {

	public LogAllExceptions() {
		super(ExecutionCategory.TOOLS, "Log all exceptions", "Adds .printStackTrace() in every try catch block handler.", ExecutionTag.RUNNABLE);
	}

	@Override
	public boolean execute(Map<String, Clazz> classes, boolean verbose) {
		classes.values().stream().map(c -> c.node.methods).flatMap(List::stream).forEach(m -> {
			if (m.tryCatchBlocks == null)
				return;
			m.tryCatchBlocks.forEach(tcb -> {
				AbstractInsnNode handler = Instructions.getRealNext(tcb.handler);
				m.instructions.insertBefore(handler, new InsnNode(DUP));
				m.instructions.insertBefore(handler, new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V"));
			});
			m.maxStack = Math.max(m.maxStack, 2);
		});
		logger.info("Inserted .printStackTrace() in every catch block!");
		return true;
	}
}