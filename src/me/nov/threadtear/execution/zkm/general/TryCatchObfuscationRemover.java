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

/**
 * Works with ZKM 8 - 11, newer versions remain untested
 */
public class TryCatchObfuscationRemover extends Execution {

	private ArrayList<Clazz> classes;

	public TryCatchObfuscationRemover() {
		super(ExecutionCategory.ZKM, "Remove unnecessary try catch blocks",
				"Remove try catch block flow obfuscation by ZKM.<br>Makes decompiling a lot easier.");
	}

	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose, boolean ignoreErr) {
		this.classes = classes;
		logger.info("Removing redundant try catch blocks by ZKM");
		long tcbs = getAmountBlocks();
		classes.stream().map(c -> c.node).forEach(c -> checkTCBs(c, c.methods));
		long amount = (tcbs - getAmountBlocks());
		logger.info("Finished, removed " + amount + " blocks in total!");
		return amount > 0;
	}

	private long getAmountBlocks() {
		return classes.stream().map(c -> c.node.methods).flatMap(List::stream).map(m -> m.tryCatchBlocks)
				.flatMap(List::stream).count();
	}

	public void checkTCBs(ClassNode c, List<MethodNode> methods) {
		methods.forEach(m -> {
			logger.info("m");
			if (m.tryCatchBlocks.stream().anyMatch(this::isFake)) {
				m.tryCatchBlocks.stream().filter(this::isFake).collect(Collectors.toSet()).forEach(tcb -> {
					logger.info("remov");
					m.tryCatchBlocks.remove(tcb);
				});
				Instructions.removeDeadCode(c, m);
			}
		});
	}

	public boolean isFake(TryCatchBlockNode tcbn) {
		AbstractInsnNode ain = tcbn.handler;
		while (ain.getOpcode() == -1) { // skip labels and frames
			ain = ain.getNext();
		}
		if (ain.getOpcode() == ATHROW) {
			return true;
		} else if (ain instanceof MethodInsnNode && ain.getNext().getOpcode() == ATHROW) {
			MethodInsnNode min = (MethodInsnNode) ain;
			ClassNode node = getClass(classes, min.owner).node;
			MethodNode getter = getMethod(node, min.name, min.desc);
			if (getter == null) {
				logger.warning("Getter " + min.owner + "." + min.name + min.desc + " not found, is library?");
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