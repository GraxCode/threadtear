package me.nov.threadtear.analysis.rewriter;

import java.util.*;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.objectweb.asm.tree.analysis.Frame;

import me.nov.threadtear.analysis.SuperInterpreter;
import me.nov.threadtear.analysis.rewriter.value.CodeReferenceValue;
import me.nov.threadtear.analysis.rewriter.value.values.*;

public class CodeRewriter extends Interpreter<CodeReferenceValue> implements Opcodes {

  SuperInterpreter basic = new SuperInterpreter();

  private CodeReferenceValue[] presetArgs;
  private ICRReferenceHandler referenceHandler;

  public InsnList rewritten = new InsnList();

  public CodeRewriter(ICRReferenceHandler referenceHandler) {
    super(ASM8);
    this.referenceHandler = referenceHandler;
  }

  public CodeRewriter(ICRReferenceHandler referenceHandler, boolean isStatic, int localVariables, String descr) {
    super(ASM8);
    this.referenceHandler = referenceHandler;
    Type.getArgumentTypes(descr);
    ArrayList<CodeReferenceValue> args = new ArrayList<>();
    while (args.size() < localVariables) {
      // add placeholder for remaining local variables
      args.add(null);
    }
    this.presetArgs = args.toArray(new CodeReferenceValue[0]);
  }

  @Override
  public CodeReferenceValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
    BasicValue v = basic.newOperation(insn);
    switch (insn.getOpcode()) {
      case ICONST_M1:
      case ICONST_0:
      case ICONST_1:
      case ICONST_2:
      case ICONST_3:
      case ICONST_4:
      case ICONST_5:
        return new NumberValue(BasicValue.INT_VALUE, insn, insn.getOpcode() - ICONST_0);
      case LCONST_0:
      case LCONST_1:
        return new NumberValue(BasicValue.LONG_VALUE, insn, (long) (insn.getOpcode() - LCONST_0));
      case FCONST_0:
      case FCONST_1:
      case FCONST_2:
        return new NumberValue(BasicValue.FLOAT_VALUE, insn, (float) (insn.getOpcode() - FCONST_0));
      case DCONST_0:
      case DCONST_1:
        return new NumberValue(BasicValue.DOUBLE_VALUE, insn, (double) (insn.getOpcode() - DCONST_0));
      case BIPUSH:
      case SIPUSH:
        return new NumberValue(BasicValue.INT_VALUE, insn, ((IntInsnNode) insn).operand);
      case LDC:
        Object cst = ((LdcInsnNode) insn).cst;
        if (cst instanceof String) {
          return new StringValue(v, insn, cst.toString());
        } else if (cst instanceof Number) {
          return new NumberValue(v, insn, cst);
        } else {
          return new UnknownInstructionValue(v, insn);
        }
      case GETSTATIC:
        FieldInsnNode fin = (FieldInsnNode) insn;
        return fieldReference(v, null, fin);
      case ACONST_NULL:
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
        return new UnknownInstructionValue(value.getType(), insn);
        //CodeReferenceValue var = presetArgs != null ? presetArgs[((VarInsnNode) insn).var] : null;
        //return var == null ? new UnknownInstructionValue(value.getType(), insn) : var.setType(value.getType());
      case ASTORE:
      case ISTORE:
      case LSTORE:
      case DSTORE:
      case FSTORE:
        if (presetArgs != null) {
          presetArgs[((VarInsnNode) insn).var] = value;
        }
      case DUP:
      case DUP_X1:
      case DUP_X2:
      case DUP2:
      case DUP2_X1:
      case DUP2_X2:
      case SWAP:
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
  public CodeReferenceValue newExceptionValue(TryCatchBlockNode tryCatchBlockNode,
                                              Frame<CodeReferenceValue> handlerFrame, Type exceptionType) {
    BasicValue v = basic.newValue(exceptionType);
    return new UnknownInstructionValue(v, new InsnNode(ACONST_NULL)); // TODO make
    // AlreadyOnStackInstruction so that aconst_null
    // doesn't end up in code
  }

  @Override
  public CodeReferenceValue newEmptyValue(int local) {
    return new UnknownInstructionValue(BasicValue.UNINITIALIZED_VALUE, new InsnNode(ACONST_NULL));
  }

  @Override
  public CodeReferenceValue unaryOperation(AbstractInsnNode insn, CodeReferenceValue value) throws AnalyzerException {
    // TODO checkcast, instanceof
    BasicValue v = basic.unaryOperation(insn, value.getType());
    switch (insn.getOpcode()) {
      case GETFIELD:
        FieldInsnNode fin = (FieldInsnNode) insn;
        return fieldReference(v, value, fin);
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
        return new UnaryOpValue(v, (InsnNode) insn, value);
      case IFEQ:
      case IFNE:
      case IFLT:
      case IFGE:
      case IFGT:
      case IFLE:
      case TABLESWITCH:
      case LOOKUPSWITCH:
      case MONITORENTER:
      case MONITOREXIT:
      case PUTSTATIC:
      default:
        return v == null ? null : new UnknownInstructionValue(v, insn);
    }
  }

  @Override
  public CodeReferenceValue binaryOperation(AbstractInsnNode insn, CodeReferenceValue a, CodeReferenceValue b)
          throws AnalyzerException {
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
        return new BinaryOpValue(v, (InsnNode) insn, a, b);
    }
    return v == null ? null : new UnknownInstructionValue(v, insn);
  }

  @Override
  public CodeReferenceValue ternaryOperation(AbstractInsnNode insn, CodeReferenceValue a, CodeReferenceValue b,
                                             CodeReferenceValue c) throws AnalyzerException {
    // add to code

    // no stack push here, we don't care
    return null;
  }

  @Override
  public CodeReferenceValue naryOperation(AbstractInsnNode insn, List<? extends CodeReferenceValue> values)
          throws AnalyzerException {
    BasicValue v = basic.naryOperation(insn, null); // values unused
    // by BasicInterpreter
    switch (insn.getOpcode()) {
      case INVOKEVIRTUAL:
      case INVOKESPECIAL:
      case INVOKEINTERFACE:
        MethodInsnNode min = (MethodInsnNode) insn;
        return v == null ? null : methodReference(v, values.remove(0), min, values);
      case INVOKESTATIC:
        MethodInsnNode staticMin = (MethodInsnNode) insn;
        return v == null ? null : methodReference(v, null, staticMin, values);
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
    // FIXME fix try catch block merges
    a.setType(t);
    return a;
  }

  private CodeReferenceValue fieldReference(BasicValue v, CodeReferenceValue reference, FieldInsnNode fin) {
    Object o = referenceHandler.getFieldValueOrNull(v, fin.owner, fin.name, fin.desc);
    if (o != null) {
      if (o instanceof String) {
        return new StringValue(v, fin, o.toString());
      } else if (o instanceof Number || o instanceof Character || o instanceof Boolean) {
        return new NumberValue(v, fin, o);
      }
    }
    return new MemberAccessValue(v, reference, fin, fin.owner, fin.name, fin.desc);
  }

  private CodeReferenceValue methodReference(BasicValue v, CodeReferenceValue reference, MethodInsnNode min,
                                             List<? extends CodeReferenceValue> values) {
    Object o = referenceHandler.getMethodReturnOrNull(v, min.owner, min.name, min.desc, values);
    if (o != null) {
      if (o instanceof String) {
        return new StringValue(v, min, o.toString());
      } else if (o instanceof Number || o instanceof Character || o instanceof Boolean) {
        return new NumberValue(v, min, o);
      }
    }
    return new MemberAccessValue(v, reference, min, min.owner, min.name, min.desc);
  }
}
