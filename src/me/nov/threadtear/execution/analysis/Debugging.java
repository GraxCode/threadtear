package me.nov.threadtear.execution.analysis;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import me.nov.threadtear.analysis.stack.ConstantAnalyzer;
import me.nov.threadtear.analysis.stack.ConstantTracker;
import me.nov.threadtear.analysis.stack.ConstantValue;
import me.nov.threadtear.analysis.stack.IConstantReferenceHandler;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.io.Clazz;
import me.nov.threadtear.util.asm.Access;
import me.nov.threadtear.util.asm.Instructions;

public class Debugging extends Execution implements IConstantReferenceHandler {

	public Debugging() {
		super(ExecutionCategory.CLEANING, "Debug Analysis", "<b>Do not run this</b>");
	}

	@Override
	public boolean execute(Map<String, Clazz> classes, boolean verbose) {
		logger.info("Simulating stack for every method!");
		classes.values().stream().map(c -> c.node).forEach(this::optimize);
		return false;
	}

	private void optimize(ClassNode cn) {
		cn.methods.forEach(m -> {
			m.instructions = simulateAndRewrite(cn, m);
		});
	}

	// XXX this is currently used for debugging purposes
	private InsnList simulateAndRewrite(ClassNode cn, MethodNode m) {
		ConstantAnalyzer a = new ConstantAnalyzer(new ConstantTracker(this, Access.isStatic(m.access), m.maxLocals, m.desc, new Object[0]));
		try {
			a.analyze(cn.name, m);
		} catch (AnalyzerException e) {
			e.printStackTrace();
			logger.severe("Failed stack analysis in " + cn.name + "." + m.name + ":" + e.getMessage());
			return m.instructions;
		}
		Frame<ConstantValue>[] frames = a.getFrames();
		InsnList rewrittenCode = new InsnList();
		Map<LabelNode, LabelNode> labels = Instructions.cloneLabels(m.instructions);
		logger.info(frames.length + " " + m.instructions.size() + "-----------" + m.name);
		for (int i = 0; i < m.instructions.size(); i++) {
			AbstractInsnNode ain = m.instructions.get(i);
			Frame<ConstantValue> frame = frames[i];
			if (frame != null) {
				if (frame.getStackSize() > 0) {
					ConstantValue top = frame.getStack(frame.getStackSize() - 1);
					logger.info(i + ": " + top + " op: " + ain.getOpcode());
					if (top.isKnown()) {
						logger.info("val type: " + top.getValue().getClass());
					}
					logger.info("Full: " + frame);

				} else {
					logger.info(i + ": empty stack");
				}
			}
			rewrittenCode.add(ain.clone(labels));
		}
		return rewrittenCode;
	}

	@Override
	public Object getFieldValueOrNull(BasicValue v, String owner, String name, String desc) {
		return null;
	}

	@Override
	public Object getMethodReturnOrNull(BasicValue v, String owner, String name, String desc, List<? extends ConstantValue> values) {
		return null;
	}
}