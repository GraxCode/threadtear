package me.nov.threadtear.analysis.full.value.values;

import java.util.Objects;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;

import me.nov.threadtear.analysis.full.value.CodeReferenceValue;

public class StringValue extends CodeReferenceValue {

	private String value; // leave this object so we don't have problems with java.lang.Character or
												// java.lang.Boolean

	public StringValue(BasicValue type, String value) {
		super(type);
		this.value = Objects.requireNonNull(value);
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
		if (obj instanceof StringValue) {
			return ((StringValue) obj).value.equals(value);
		}
		return false;
	}

	@Override
	public InsnList toInstructions() {
		InsnList list = new InsnList();
		list.add(new LdcInsnNode(value));
		return list;
	}

	@Override
	public Object getStackValueOrNull() {
		return value;
	}
}
