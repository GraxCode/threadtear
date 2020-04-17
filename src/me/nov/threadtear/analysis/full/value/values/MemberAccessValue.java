package me.nov.threadtear.analysis.full.value.values;

import java.util.Objects;

import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;

import me.nov.threadtear.analysis.full.value.CodeReferenceValue;

public class MemberAccessValue extends CodeReferenceValue {

	public int op;

	private CodeReferenceValue ownerRef;
	public String owner;
	public String name;
	public String desc;

	public MemberAccessValue(BasicValue type, CodeReferenceValue ownerReference, int op, String owner, String name, String desc) {
		super(type);
		this.op = op;
		this.ownerRef = ownerReference;
		this.owner = Objects.requireNonNull(owner);
		this.name = Objects.requireNonNull(name);
		this.desc = Objects.requireNonNull(desc);
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
		MemberAccessValue other = (MemberAccessValue) obj;
		if (desc == null) {
			if (other.desc != null)
				return false;
		} else if (!desc.equals(other.desc))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (op != other.op)
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		return true;
	}

	@Override
	public InsnList toInstructions() {
		InsnList list = new InsnList();
		if (ownerRef != null) {
			list.add(ownerRef.toInstructions());
		}
		switch (op) {
		case GETSTATIC:
		case PUTSTATIC:
		case GETFIELD:
		case PUTFIELD:
			list.add(new FieldInsnNode(op, owner, name, desc));
			break;
		case INVOKEINTERFACE:
		case INVOKESTATIC:
		case INVOKEVIRTUAL:
		case INVOKESPECIAL:
			list.add(new MethodInsnNode(op, owner, name, desc));
			break;
		default:
			throw new IllegalArgumentException();
		}
		return list;
	}

	@Override
	public Object getStackValueOrNull() {
		return null;
	}
}
