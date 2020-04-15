package me.nov.threadtear.execution.cleanup.remove;

import java.util.ArrayList;
import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import me.nov.threadtear.Threadtear;
import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.analysis.ConstantTracker;
import me.nov.threadtear.asm.analysis.ConstantValue;
import me.nov.threadtear.asm.analysis.IReferenceHandler;
import me.nov.threadtear.asm.analysis.hack.AnalyzerHack;
import me.nov.threadtear.asm.util.Access;
import me.nov.threadtear.asm.util.Instructions;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;

public class RemoveUnnecessary extends Execution implements IReferenceHandler {

	public RemoveUnnecessary() {
		super(ExecutionCategory.CLEANING, "Remove unnecessary instructions", "Remove unnecessary instructions that can be optimized.<br>This could include number or flow obfuscation.<br><b>Do not run this, it is unfinished!</b>");
	}

	/*
	 * TODO Nothing done here yet, this class should simulate stack and
	 * simultaneously rewrite the code.
	 * 
	 * eg. ICONST_4 ICONST_1 IADD INVOKESTATIC ...
	 * 
	 * would be turned into
	 * 
	 * ICONST_5 INVOKESTATIC ...
	 * 
	 */

	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose, boolean ignoreErr) {
		logger.info("Simulating stack for every method!");
		classes.stream().map(c -> c.node).forEach(this::optimize);
		return false;
	}

	private void optimize(ClassNode cn) {
		cn.methods.forEach(m -> {
			m.instructions = simulateAndRewrite(cn, m, m.instructions);
		});
	}

	private InsnList simulateAndRewrite(ClassNode cn, MethodNode m, InsnList instructions) {
		AnalyzerHack<ConstantValue> a = new AnalyzerHack<>(new ConstantTracker(this, Access.isStatic(m.access), m.maxLocals, m.desc, new Object[0]));
		try {
			a.analyze(cn.name, m);
		} catch (AnalyzerException e) {
			e.printStackTrace();
			Threadtear.logger.severe("Failed stack analysis in " + cn.name + "." + m.name + ":" + e.getMessage());
			return m.instructions;
		}
		Frame<ConstantValue>[] frames = a.getFrames();
		InsnList rewrittenCode = new InsnList();
		Map<LabelNode, LabelNode> labels = Instructions.cloneLabels(m.instructions);
		Threadtear.logger.info(frames.length + " " + m.instructions.size());
		for (int i = 0; i < m.instructions.size(); i++) {
			AbstractInsnNode ain = m.instructions.get(i);
			Frame<ConstantValue> frame = frames[i];
			if (frame != null) {
				if (frame.getStackSize() > 0) {
					Threadtear.logger.info(i + ": " + frame.getStack(frame.getStackSize() - 1) + " op: " + ain.getOpcode());
				} else {
					Threadtear.logger.info(i + ": empty stack");
				}
			}
			rewrittenCode.add(ain.clone(labels));
		}
		// TODO very much to do here...
		return rewrittenCode;
	}

	@Override
	public Object getFieldValueOrNull(BasicValue v, String owner, String name, String desc) {
		Threadtear.logger.info("request for field " + name);
		return name.equals("x") ? 12 : null;
	}

	@Override
	public Object getMethodReturnOrNull(BasicValue v, String owner, String name, String desc) {
		return null;
	}
}