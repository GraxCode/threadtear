package me.nov.threadtear.analysis.full.value;

import java.util.Objects;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Value;

public abstract class CodeReferenceValue implements Value, Opcodes {
	public abstract boolean isKnownValue();

	public abstract CodeReferenceValue combine();

	public abstract boolean equalsWith(CodeReferenceValue obj);

	public abstract InsnList toInstructions();

	public abstract Object getStackValueOrNull();

	protected BasicValue type;

	protected CodeReferenceValue(BasicValue type) {
		this.type = Objects.requireNonNull(type);
	}

	@Override
	public int getSize() {
		return getType().getSize();
	}

	@Override
	public String toString() {
		Type t = getType().getType();
		if (t == null)
			return "uninitialized";
		return this.getClass().getName() + " (" + getType().toString() + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CodeReferenceValue) {
			return equalsWith((CodeReferenceValue) obj);
		}
		return false;
	}

	public BasicValue getType() {
		return type;
	}

	public CodeReferenceValue setType(BasicValue type) {
		this.type = Objects.requireNonNull(type);
		return this;
	}

}
