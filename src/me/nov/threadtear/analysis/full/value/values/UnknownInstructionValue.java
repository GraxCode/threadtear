package me.nov.threadtear.analysis.full.value.values;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.analysis.BasicValue;

import me.nov.threadtear.analysis.full.value.CodeReferenceValue;

public class UnknownInstructionValue extends CodeReferenceValue {

	public AbstractInsnNode ain;

	public UnknownInstructionValue(BasicValue type, AbstractInsnNode ain) {
		super(type);
		this.ain = ain;
	}

	@Override
	public boolean isKnownValue() {
		return false;
	}

	@Override
	public CodeReferenceValue combine() {
		return this;
	}

	@Override
	public boolean equalsWith(CodeReferenceValue obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		UnknownInstructionValue other = (UnknownInstructionValue) obj;
		if (ain == null) {
			if (other.ain != null)
				return false;
		} else if (!ain.equals(other.ain))
			return false;
		return true;
	}

	@Override
	public InsnList toInstructions() {
		InsnList list = new InsnList();
		if(ain.getOpcode() == NOP) {
//			throw new IllegalArgumentException(type.toString());
		}
		list.add(ain);
		return list;
	}

	@Override
	public Object getStackValueOrNull() {
		return null;
	}
}
