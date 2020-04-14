package me.nov.threadtear.execution.zkm.general;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.util.Instructions;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;

/**
 * Works with ZKM 8 - 11, newer versions remain untested
 */
public class TryCatchObfuscationRemover extends Execution {

	private ArrayList<Clazz> classes;
	private boolean verbose;

	public TryCatchObfuscationRemover() {
		super(ExecutionCategory.ZKM, "Remove unnecessary try catch blocks",
				"Remove try catch block flow obfuscation by ZKM.<br>Makes decompiling a lot easier.", 
				ExecutionTag.RUNNABLE,
				ExecutionTag.BETTER_DECOMPILE);
	}

	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose, boolean ignoreErr) {
		this.verbose = verbose;
		this.classes = classes;
		logger.info("Removing redundant try catch blocks by ZKM");
		long tcbs = getAmountBlocks();
		classes.stream().map(c -> c.node).forEach(c -> checkTCBs(c, c.methods));
		long amount = (tcbs - getAmountBlocks());
		logger.info("Finished, removed " + amount + " blocks of " + tcbs + " total blocks!");
		return amount > 0;
	}

	private long getAmountBlocks() {
		return classes.stream().map(c -> c.node.methods).flatMap(List::stream).map(m -> m.tryCatchBlocks)
				.flatMap(List::stream).count();
	}

	public void checkTCBs(ClassNode c, List<MethodNode> methods) {
		methods.forEach(m -> {
			if (m.tryCatchBlocks.stream().anyMatch(this::isFake)) {
				m.tryCatchBlocks.stream().filter(this::isFake).collect(Collectors.toSet()).forEach(tcb -> {
					m.tryCatchBlocks.remove(tcb);
				});
				Instructions.removeDeadCode(c, m);
			}
		});
	}

	public boolean isFake(TryCatchBlockNode tcbn) {
		AbstractInsnNode ain = Instructions.getRealNext(tcbn.handler);
		if (ain.getOpcode() == ATHROW) {
			return true;
		} else if (ain.getType() == AbstractInsnNode.METHOD_INSN && ain.getNext().getOpcode() == ATHROW) {
			MethodInsnNode min = (MethodInsnNode) ain;
			Clazz clazz = getClass(classes, min.owner);
			if (clazz == null) {
				if (verbose)
					logger.warning("Class " + min.owner + " not found, possibly library");
				return false;
			}
			MethodNode getter = getMethod(clazz.node, min.name, min.desc);
			if (getter == null) {
				if (verbose)
					logger.warning("Getter " + min.owner + "." + min.name + min.desc + " not found, possibly library");
				return false;
			}
			AbstractInsnNode getterFirst = getter.instructions.getFirst();
			while (getterFirst.getOpcode() == -1) {
				getterFirst = ain.getNext();
			}
			if (getterFirst instanceof VarInsnNode && getterFirst.getNext().getOpcode() == ARETURN) {
				if (((VarInsnNode) getterFirst).var == 0) {
					return true;
				}
			}
		}
		// method not matching zkm fake athrow getter
		return false;
	}
}