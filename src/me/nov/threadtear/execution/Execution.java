package me.nov.threadtear.execution;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import me.nov.threadtear.Threadtear;
import me.nov.threadtear.analysis.stack.ConstantTracker;
import me.nov.threadtear.analysis.stack.ConstantValue;
import me.nov.threadtear.analysis.stack.IConstantReferenceHandler;
import me.nov.threadtear.io.Clazz;
import me.nov.threadtear.util.asm.Access;

public abstract class Execution implements Opcodes {
	public final String name;
	public final ExecutionCategory type;
	public final String description;
	public final ExecutionTag[] tags;

	protected static final Logger logger = Threadtear.logger;
	protected static final Random random = new Random();

	public Execution(ExecutionCategory type, String name, String description, ExecutionTag... tags) {
		this.type = type;
		this.name = name;
		this.description = description;
		this.tags = tags;
	}

	@Override
	public String toString() {
		return name;
	}

	public abstract boolean execute(Map<String, Clazz> map, boolean verbose);

	protected MethodNode getMethod(ClassNode node, String name, String desc) {
		if (node == null)
			return null;
		return node.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(desc)).findFirst().orElse(null);
	}

	protected MethodNode getStaticInitializer(ClassNode node) {
		return node.methods.stream().filter(m -> m.name.equals("<clinit>")).findFirst().orElse(null);
	}

	protected Frame<ConstantValue>[] getConstantFrames(ClassNode c, MethodNode m, IConstantReferenceHandler handler) {
		Analyzer<ConstantValue> a = new Analyzer<ConstantValue>(new ConstantTracker(handler, Access.isStatic(m.access), m.maxLocals, m.desc, new Object[0]));
		try {
			a.analyze(c.name, m);
		} catch (AnalyzerException e) {
			logger.severe("Failed stack analysis in " + c.name + "." + m.name + ":" + e.getMessage());
			return null;
		}
		return a.getFrames();
	}

	protected void loopConstantFrames(ClassNode c, MethodNode m, IConstantReferenceHandler handler, BiConsumer<AbstractInsnNode, Frame<ConstantValue>> consumer) {
		Frame<ConstantValue>[] frames = getConstantFrames(c, m, handler);
		if (frames == null)
			return;
		AbstractInsnNode[] insns = m.instructions.toArray();
		for (int i = 0; i < insns.length; i++) {
			consumer.accept(insns[i], frames[i]);
		}
	}

	protected HashMap<LabelNode, Frame<ConstantValue>> collectLabels(ClassNode c, MethodNode m, IConstantReferenceHandler handler) {
		HashMap<LabelNode, Frame<ConstantValue>> map = new HashMap<>();
		loopConstantFrames(c, m, handler, (ain, frame) -> {
			if (ain.getType() == AbstractInsnNode.LABEL) {
				map.put((LabelNode) ain, frame);
			}
		});
		return map;
	}
}
