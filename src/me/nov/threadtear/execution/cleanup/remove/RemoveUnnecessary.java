package me.nov.threadtear.execution.cleanup.remove;

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

import me.nov.threadtear.analysis.full.CodeAnalyzer;
import me.nov.threadtear.analysis.full.CodeTracker;
import me.nov.threadtear.analysis.full.ICodeReferenceHandler;
import me.nov.threadtear.analysis.full.value.CodeReferenceValue;
import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.util.Access;
import me.nov.threadtear.asm.util.Instructions;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;

public class RemoveUnnecessary extends Execution implements ICodeReferenceHandler {

	public RemoveUnnecessary() {
		super(ExecutionCategory.CLEANING, "Convert to readable instructions",
				"Remove unnecessary instructions or flow obfuscation that can be optimized.<br>This could include number or flow obfuscation.<br><b>Do not run this, it is unfinished!</b>");
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

	private InsnList simulateAndRewrite(ClassNode cn, MethodNode m) {
		m.tryCatchBlocks.clear();
		if (m.localVariables != null)
			m.localVariables.clear();
		CodeAnalyzer a = new CodeAnalyzer(new CodeTracker(this, Access.isStatic(m.access), m.maxLocals, m.desc, new CodeReferenceValue[0]));
		try {
			a.analyze(cn.name, m);
		} catch (AnalyzerException e) {
			e.printStackTrace();
			logger.severe("Failed stack analysis in " + cn.name + "." + m.name + ":" + e.getMessage());
			return m.instructions;
		}
		// TODO rewrite this whole thing without analyzer
		Frame<CodeReferenceValue>[] frames = a.getFrames();
		InsnList rewrittenCode = new InsnList();
		Map<LabelNode, LabelNode> labels = Instructions.cloneLabels(m.instructions);
		for (int i = 0; i < m.instructions.size(); i++) {
			AbstractInsnNode ain = m.instructions.get(i);
			Frame<CodeReferenceValue> frame = frames[i];
			if (frame == null) {
				rewrittenCode.add(ain.clone(labels));
			} else if (ain.getType() == AbstractInsnNode.LABEL) {
				rewrittenCode.add(ain.clone(labels));
			} else if (ain.getType() == AbstractInsnNode.METHOD_INSN || ain.getType() == AbstractInsnNode.JUMP_INSN || isObligatory(ain.getOpcode()) || Instructions.isCodeEnd(ain)) {
				for (int j = 0; j < frame.getStackSize(); j++) {
					CodeReferenceValue stack = frame.getStack(j);
					stack = stack.combine();
					rewrittenCode.add(stack.cloneInstructions());
				}
				rewrittenCode.add(ain.clone(labels));
			}
			logger.info(i + ": " + (frame == null ? "null" : toString(frame)));
		}
		logger.info(rewrittenCode.size() + " final size");
		return rewrittenCode;
	}

	private boolean isObligatory(int opcode) {
		switch (opcode) {
		case MONITORENTER:
		case MONITOREXIT:
		case LOOKUPSWITCH:
		case TABLESWITCH:
		case INVOKEDYNAMIC:
		case IASTORE:
		case LASTORE:
		case FASTORE:
		case DASTORE:
		case AASTORE:
		case BASTORE:
		case CASTORE:
		case SASTORE:
		case PUTFIELD:
		case PUTSTATIC:
		case ISTORE:
		case LSTORE:
		case FSTORE:
		case DSTORE:
		case ASTORE:
			return true;
		}
		return false;
	}

	public String toString(Frame<CodeReferenceValue> f) {
		StringBuilder stringBuilder = new StringBuilder(" LOCALS: (");
		for (int i = 0; i < f.getLocals(); ++i) {
			stringBuilder.append(f.getLocal(i));
			stringBuilder.append('|');
		}
		stringBuilder.append(") STACK: (");
		for (int i = 0; i < f.getStackSize(); ++i) {
			CodeReferenceValue combined = f.getStack(i).combine();
			stringBuilder.append(combined.getStackValueOrNull() != null ? combined.getStackValueOrNull() : combined.toString());
			stringBuilder.append('|');
		}
		stringBuilder.append(')');
		return stringBuilder.toString();
	}

	@Override
	public Object getFieldValueOrNull(BasicValue v, String owner, String name, String desc) {
		return null;
	}

	@Override
	public Object getMethodReturnOrNull(BasicValue v, String owner, String name, String desc, List<? extends CodeReferenceValue> values) {
		if (name.equals("toCharArray") && owner.equals("java/lang/String")) {
		}
		return null;
	}
}