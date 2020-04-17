package me.nov.threadtear.analysis.full;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

import me.nov.threadtear.analysis.SuperInterpreter;
import me.nov.threadtear.analysis.full.value.CodeReferenceValue;
import me.nov.threadtear.analysis.full.value.values.BinaryOpValue;
import me.nov.threadtear.analysis.full.value.values.NumberValue;
import me.nov.threadtear.analysis.full.value.values.StringValue;
import me.nov.threadtear.analysis.full.value.values.UnaryOpValue;
import me.nov.threadtear.analysis.full.value.values.UnknownInstructionValue;

public class CodeTracker extends Interpreter<CodeReferenceValue> implements Opcodes {

	SuperInterpreter basic = new SuperInterpreter();

	private CodeReferenceValue[] presetArgs;
	private Type[] desc;

	private ICodeReferenceHandler referenceHandler;

	public CodeTracker(ICodeReferenceHandler referenceHandler) {
		super(ASM8);
		this.referenceHandler = referenceHandler;
	}

	public CodeTracker(ICodeReferenceHandler referenceHandler, boolean isStatic, int localVariables, String descr, CodeReferenceValue[] args) {
		super(ASM8);
		this.referenceHandler = referenceHandler;
		this.desc = Type.getArgumentTypes(descr);
		ArrayList<CodeReferenceValue> reformatted = new ArrayList<>();
		if (!isStatic) {
			reformatted.add(null); // this reference
		}
		for (int i = 0; i < desc.length; ++i) {
			reformatted.add(i >= args.length ? null : args[i]);
			if (desc[i].getSize() == 2) {
				reformatted.add(null); // placeholder for long and double
			}
		}
		if (reformatted.size() > localVariables) {
			// this shouldn't happen...
			// throw new IllegalArgumentException();
		}
		while (reformatted.size() < localVariables) {
			// add placeholder for remaining local variables
			reformatted.add(null);
		}
		this.presetArgs = reformatted.toArray(new CodeReferenceValue[0]);
	}

	@Override
	public CodeReferenceValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
		BasicValue v = basic.newOperation(insn);
		switch (insn.getOpcode()) {
		case ACONST_NULL:
			return new UnknownInstructionValue(v, insn);
		case ICONST_M1:
		case ICONST_0:
		case ICONST_1:
		case ICONST_2:
		case ICONST_3:
		case ICONST_4:
		case ICONST_5:
			return new NumberValue(BasicValue.INT_VALUE, insn.getOpcode() - ICONST_0);
		case LCONST_0:
		case LCONST_1:
			return new NumberValue(BasicValue.LONG_VALUE, (long) (insn.getOpcode() - LCONST_0));
		case FCONST_0:
		case FCONST_1:
		case FCONST_2:
			return new NumberValue(BasicValue.FLOAT_VALUE, (float) (insn.getOpcode() - FCONST_0));
		case DCONST_0:
		case DCONST_1:
			return new NumberValue(BasicValue.DOUBLE_VALUE, (double) (insn.getOpcode() - DCONST_0));
		case BIPUSH:
		case SIPUSH:
			return new NumberValue(BasicValue.INT_VALUE, ((IntInsnNode) insn).operand);
		case LDC:
			Object cst = ((LdcInsnNode) insn).cst;
			if (cst instanceof String) {
				return new StringValue(v, cst.toString());
			} else if (cst instanceof Number) {
				return new NumberValue(v, cst);
			} else {
				return new UnknownInstructionValue(v, insn);
			}
		case GETSTATIC:
			FieldInsnNode fin = (FieldInsnNode) insn;
			return fieldReference(v, fin);
		default:
			return new UnknownInstructionValue(v, insn);
		}
	}

	@Override
	public CodeReferenceValue copyOperation(AbstractInsnNode insn, CodeReferenceValue value) throws AnalyzerException {
		switch (insn.getOpcode()) {
		case ALOAD:
		case ILOAD:
		case LLOAD:
		case DLOAD:
		case FLOAD:
			CodeReferenceValue var = presetArgs != null ? presetArgs[((VarInsnNode) insn).var] : null;
			return var == null ? new UnknownInstructionValue(value.getType(), insn) : var.setType(value.getType());
		case ASTORE:
		case ISTORE:
		case LSTORE:
		case DSTORE:
		case FSTORE:
			if (presetArgs != null) {
				presetArgs[((VarInsnNode) insn).var] = value;
			}
		default:
			break;
		}
		return value; // stack ops
	}

	@Override
	public CodeReferenceValue newValue(Type type) {
		BasicValue v = basic.newValue(type);
		return v == null ? null : new UnknownInstructionValue(v, new InsnNode(ACONST_NULL));
	}

	@Override
	public CodeReferenceValue newExceptionValue(TryCatchBlockNode tryCatchBlockNode, Frame<CodeReferenceValue> handlerFrame, Type exceptionType) {
		BasicValue v = basic.newValue(exceptionType);
		return new UnknownInstructionValue(v, new InsnNode(ACONST_NULL)); // TODO make AlreadyOnStackInstruction
	}

	@Override
	public CodeReferenceValue newEmptyValue(int local) {
		return new UnknownInstructionValue(BasicValue.UNINITIALIZED_VALUE, new InsnNode(ACONST_NULL));
	}

	@Override
	public CodeReferenceValue unaryOperation(AbstractInsnNode insn, CodeReferenceValue value) throws AnalyzerException {
		BasicValue v = basic.unaryOperation(insn, value.getType());
		switch (insn.getOpcode()) {
		case GETFIELD:
			FieldInsnNode fin = (FieldInsnNode) insn;
			return fieldReference(v, fin);
		case INEG:
		case FNEG:
		case LNEG:
		case DNEG:
		case L2I:
		case F2I:
		case D2I:
		case I2B:
		case I2C:
		case I2S:
		case I2F:
		case L2F:
		case D2F:
		case I2L:
		case F2L:
		case D2L:
		case I2D:
		case L2D:
		case F2D:
			return new UnaryOpValue(v, insn.getOpcode(), value);
		default:
			return v == null ? null : new UnknownInstructionValue(v, insn);
		}
	}

	@Override
	public CodeReferenceValue binaryOperation(AbstractInsnNode insn, CodeReferenceValue a, CodeReferenceValue b) throws AnalyzerException {
		BasicValue v = basic.binaryOperation(insn, a.getType(), b.getType());
		switch (insn.getOpcode()) {
		case BALOAD:
		case CALOAD:
		case SALOAD:
		case IALOAD:
		case FALOAD:
		case DALOAD:
		case LALOAD:
		case AALOAD:
			return new UnknownInstructionValue(v, insn);
		case IADD:
		case ISUB:
		case IMUL:
		case IDIV:
		case IREM:
		case ISHL:
		case ISHR:
		case IUSHR:
		case IAND:
		case IOR:
		case IXOR:
		case FADD:
		case FSUB:
		case FMUL:
		case FDIV:
		case FREM:
		case LADD:
		case LSUB:
		case LMUL:
		case LDIV:
		case LREM:
		case LSHL:
		case LSHR:
		case LUSHR:
		case LAND:
		case LOR:
		case LXOR:
		case DADD:
		case DSUB:
		case DMUL:
		case DDIV:
		case DREM:
			return new BinaryOpValue(v, insn.getOpcode(), a, b);
		}
		return v == null ? null : new UnknownInstructionValue(v, insn);
	}

	@Override
	public CodeReferenceValue ternaryOperation(AbstractInsnNode insn, CodeReferenceValue a, CodeReferenceValue b, CodeReferenceValue c) throws AnalyzerException {
		// no stack push here, we don't care
		return null;
	}

	@Override
	public CodeReferenceValue naryOperation(AbstractInsnNode insn, List<? extends CodeReferenceValue> values) throws AnalyzerException {
		BasicValue v = basic.naryOperation(insn, null); // values unused by BasicInterpreter
		switch (insn.getOpcode()) {
		case INVOKEVIRTUAL:
		case INVOKESTATIC:
		case INVOKESPECIAL:
		case INVOKEINTERFACE:
			MethodInsnNode min = (MethodInsnNode) insn;
			return v == null ? null : methodReference(v, min, values);
		default:
			return v == null ? null : new UnknownInstructionValue(v, insn);
		}
	}

	@Override
	public void returnOperation(AbstractInsnNode insn, CodeReferenceValue value, CodeReferenceValue expected) {
	}

	@Override
	public CodeReferenceValue merge(CodeReferenceValue a, CodeReferenceValue b) {
		if (a.equals(b))
			return a;
		BasicValue t = basic.merge(a.getType(), b.getType());
		// TODO make this work
		a.setType(t);
		return a;
//		return t.equals(a.getType()) ? a : t.equals(b.getType()) ? b : a;
		// TODO check if this works with null
//		return t.equals(a.getType()) && (a.value == null && a != NULL || a.value != null && a.value.equals(b.value)) ? a : t.equals(b.getType()) && b.value == null && b != NULL ? b : new ConstantValue(t, null);
	}

//
	private CodeReferenceValue fieldReference(BasicValue v, FieldInsnNode fin) {
		Object o = referenceHandler.getFieldValueOrNull(v, fin.owner, fin.name, fin.desc);
		if (o != null) {
			if (o instanceof String) {
				return new StringValue(v, o.toString());
			} else if (o instanceof Number || o instanceof Character || o instanceof Boolean) {
				return new NumberValue(v, o);
			}
		}
		return new UnknownInstructionValue(v, fin);
//		return new ReferenceValue(v, fin.getOpcode(), fin.owner, fin.name, fin.desc);
	}

	private CodeReferenceValue methodReference(BasicValue v, MethodInsnNode min, List<? extends CodeReferenceValue> values) {
		Object o = referenceHandler.getMethodReturnOrNull(v, min.owner, min.name, min.desc, values);
		if (o != null) {
			if (o instanceof String) {
				return new StringValue(v, o.toString());
			} else if (o instanceof Number || o instanceof Character || o instanceof Boolean) {
				return new NumberValue(v, o);
			}
		}
		return new UnknownInstructionValue(v, min);
//		return new ReferenceValue(v, min.getOpcode(), min.owner, min.name, min.desc);
	}
}