package me.nov.threadtear.execution.generic;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;

import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.util.Instructions;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;

public class InlineUnchangedFields extends Execution {

	public InlineUnchangedFields() {
		super(ExecutionCategory.CLEANING, "Inline unchanged fields", "Inline fields that are not set anywhere in the code.<br>Can be useful for ZKM deobfuscation.", ExecutionTag.RUNNABLE,
				ExecutionTag.BETTER_DECOMPILE, ExecutionTag.BETTER_DEOBFUSCATE);
	}

	public int inlines;
	private ArrayList<Clazz> classes;

	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose) {
		this.classes = classes;
		this.inlines = 0;
		// TODO filter out static initializer
		// TODO takes too long
		classes.stream().map(c -> c.node).forEach(c -> {
			c.fields.stream().filter(f -> isReferenced(c, f)).forEach(f -> inline(c, f));
		});
		logger.info("Inlined " + inlines + " field references!");
		return true;
	}

	private boolean isReferenced(ClassNode cn, FieldNode f) {
		return classes.stream().map(c -> c.node.methods).flatMap(List::stream).map(m -> m.instructions.spliterator()).flatMap(insns -> StreamSupport.stream(insns, false))
				.filter(ain -> ain.getType() == AbstractInsnNode.FIELD_INSN).map(ain -> (FieldInsnNode) ain).allMatch(fin -> !isPutReferenceTo(cn, fin, f));
	}

	public void inline(ClassNode cn, FieldNode fn) {
		classes.stream().map(c -> c.node).forEach(c -> {
			c.methods.forEach(m -> {
				for (AbstractInsnNode ain : m.instructions) {
					if (ain.getType() == AbstractInsnNode.FIELD_INSN) {
						FieldInsnNode fin = (FieldInsnNode) ain;
						if (isGetReferenceTo(cn, fin, fn)) {
							m.instructions.set(ain, Instructions.createNullPushForType(fn.desc));
							inlines++;
						}
					}
				}
			});
		});
	}

	private boolean isPutReferenceTo(ClassNode cn, FieldInsnNode fin, FieldNode fn) {
		return (fin.getOpcode() == PUTSTATIC || fin.getOpcode() == PUTFIELD) && fin.owner.equals(cn.name) && fin.name.equals(fn.name) && fin.desc.equals(fn.desc);
	}

	private boolean isGetReferenceTo(ClassNode cn, FieldInsnNode fin, FieldNode fn) {
		return (fin.getOpcode() == GETSTATIC || fin.getOpcode() == GETFIELD) && fin.owner.equals(cn.name) && fin.name.equals(fn.name) && fin.desc.equals(fn.desc);
	}
}
