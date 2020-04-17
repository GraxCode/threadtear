package me.nov.threadtear.analysis.full.value.values;

import java.util.Objects;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;

import me.nov.threadtear.analysis.full.value.CodeReferenceValue;

public class UnaryOpValue extends CodeReferenceValue {

	public int opcode;
	public CodeReferenceValue value;

	public UnaryOpValue(BasicValue type, int opcode, CodeReferenceValue value) {
		super(type);
		this.opcode = opcode;
		this.value = Objects.requireNonNull(value);
	}

	@Override
	public boolean isKnownValue() {
		return value.isKnownValue();
	}

	@Override
	public CodeReferenceValue combine() {
		if (!isKnownValue()) {
			return this;
		}
		return new NumberValue(type, getStackValueOrNull());
	}

	@Override
	public boolean equalsWith(CodeReferenceValue obj) {
		if (getClass() != obj.getClass())
			return false;
		UnaryOpValue other = (UnaryOpValue) obj;
		if (opcode != other.opcode)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public InsnList toInstructions() {
		InsnList list = new InsnList();
		list.add(value.toInstructions());
		list.add(new InsnNode(opcode));
		return list;
	}

	@Override
	public Object getStackValueOrNull() {
		Number num = (Number) value.getStackValueOrNull();
		switch (opcode) {
		case INEG:
			return -((int) num.intValue());
		case FNEG:
			return -((float) num.floatValue());
		case LNEG:
			return -((long) num.longValue());
		case DNEG:
			return -((double) num.doubleValue());
		case L2I:
		case F2I:
		case D2I:
			return num.intValue();
		case I2B:
			return num.byteValue();
		case I2C:
			return num.intValue() & 0x0000FFFF;
		case I2S:
			return num.shortValue();
		case I2F:
		case L2F:
		case D2F:
			return num.floatValue();
		case I2L:
		case F2L:
		case D2L:
			return num.longValue();
		case I2D:
		case L2D:
		case F2D:
			return num.doubleValue();
		default:
			throw new IllegalArgumentException();
		}
	}

}