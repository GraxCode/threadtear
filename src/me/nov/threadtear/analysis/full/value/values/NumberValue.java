package me.nov.threadtear.analysis.full.value.values;

import java.util.Objects;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;

import me.nov.threadtear.analysis.full.value.CodeReferenceValue;

public class NumberValue extends CodeReferenceValue {

	private Object value; // leave this object so we don't have problems with java.lang.Character or
												// java.lang.Boolean

	public NumberValue(BasicValue type, Object value) {
		super(type);
		if (value instanceof Number || value instanceof Character || value instanceof Boolean) {
			this.value = Objects.requireNonNull(value);
		} else {
			throw new IllegalArgumentException("not a number");
		}
	}

	@Override
	public boolean isKnownValue() {
		return true;
	}

	@Override
	public CodeReferenceValue combine() {
		return this;
	}

	@Override
	public boolean equalsWith(CodeReferenceValue obj) {
		if (this == obj)
			return true;
		if (obj instanceof NumberValue) {
			return ((NumberValue) obj).value.equals(value);
		}
		return false;
	}

	@Override
	public InsnList toInstructions() {
		InsnList list = new InsnList();
		list.add(new LdcInsnNode(getStackValueOrNull()));
		return list;
	}

	@Override
	public Object getStackValueOrNull() {
		if (value instanceof Character) {
			return (int) ((Character) value).charValue();
		}
		if (value instanceof Boolean) {
			return ((Boolean) value).booleanValue() ? 1 : 0;
		}
		return value;
	}
}
