package me.nov.threadtear.analysis.rewriter.value.values;

import java.util.Objects;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;

import me.nov.threadtear.analysis.rewriter.value.CodeReferenceValue;

public class BinaryOpValue extends CodeReferenceValue {

  public CodeReferenceValue left;
  public CodeReferenceValue right;

  public BinaryOpValue(BasicValue type, InsnNode node, CodeReferenceValue left, CodeReferenceValue right) {
    super(type, node);
    this.left = Objects.requireNonNull(left);
    this.right = Objects.requireNonNull(right);
  }

  @Override
  public boolean isKnownValue() {
    return left.isKnownValue() && right.isKnownValue();
  }

  @Override
  public boolean isRequiredInCode() {
    // TODO check for idiv 0 and similar things
    return left.isRequiredInCode() || right.isRequiredInCode();
  }

  @Override
  public CodeReferenceValue combine() {
    if (!isKnownValue()) {
      return this;
    }
    Object stack = getStackValueOrNull();
    return new NumberValue(type, new LdcInsnNode(stack), stack);
  }

  @Override
  public boolean equalsWith(CodeReferenceValue obj) {
    if (getClass() != obj.getClass())
      return false;
    BinaryOpValue other = (BinaryOpValue) obj;
    if (left == null) {
      if (other.left != null)
        return false;
    } else if (!left.equals(other.left))
      return false;
    if (node.getOpcode() != other.node.getOpcode())
      return false;
    if (right == null) {
      if (other.right != null)
        return false;
    } else if (!right.equals(other.right))
      return false;
    return true;
  }

  @Override
  public InsnList cloneInstructions() {
    InsnList list = new InsnList();
    list.add(left.cloneInstructions());
    list.add(right.cloneInstructions());
    list.add(new InsnNode(node.getOpcode()));
    return list;
  }

  @Override
  public InsnList getInstructions() {
    InsnList list = new InsnList();
    list.add(left.getInstructions());
    list.add(right.getInstructions());
    list.add(node);
    return list;
  }

  @Override
  public Object getStackValueOrNull() {
    if (!isKnownValue()) {
      return null;
    }
    Number num1 = (Number) left.getStackValueOrNull();
    Number num2 = (Number) right.getStackValueOrNull();
    switch (node.getOpcode()) {
    case IADD:
      return num1.intValue() + num2.intValue();
    case ISUB:
      return num1.intValue() - num2.intValue();
    case IMUL:
      return num1.intValue() * num2.intValue();
    case IDIV:
      return num1.intValue() / num2.intValue();
    case IREM:
      return num1.intValue() % num2.intValue();
    case ISHL:
      return num1.intValue() << num2.intValue();
    case ISHR:
      return num1.intValue() >> num2.intValue();
    case IUSHR:
      return num1.intValue() >>> num2.intValue();
    case IAND:
      return num1.intValue() & num2.intValue();
    case IOR:
      return num1.intValue() | num2.intValue();
    case IXOR:
      return num1.intValue() ^ num2.intValue();
    case FADD:
      return num1.floatValue() + num2.floatValue();
    case FSUB:
      return num1.floatValue() - num2.floatValue();
    case FMUL:
      return num1.floatValue() * num2.floatValue();
    case FDIV:
      return num1.floatValue() / num2.floatValue();
    case FREM:
      return num1.floatValue() % num2.floatValue();
    case LADD:
      return num1.longValue() + num2.longValue();
    case LSUB:
      return num1.longValue() - num2.longValue();
    case LMUL:
      return num1.longValue() * num2.longValue();
    case LDIV:
      return num1.longValue() / num2.longValue();
    case LREM:
      return num1.longValue() % num2.longValue();
    case LSHL:
      return num1.longValue() << num2.longValue();
    case LSHR:
      return num1.longValue() >> num2.longValue();
    case LUSHR:
      return num1.longValue() >>> num2.longValue();
    case LAND:
      return num1.longValue() & num2.longValue();
    case LOR:
      return num1.longValue() | num2.longValue();
    case LXOR:
      return num1.longValue() ^ num2.longValue();
    case DADD:
      return num1.doubleValue() + num2.doubleValue();
    case DSUB:
      return num1.doubleValue() - num2.doubleValue();
    case DMUL:
      return num1.doubleValue() * num2.doubleValue();
    case DDIV:
      return num1.doubleValue() / num2.doubleValue();
    case DREM:
      return num1.doubleValue() % num2.doubleValue();

    // compare instructions not tested, could return wrong result
    case LCMP:
      return Long.compare(num1.longValue(), num2.longValue());
    case FCMPL:
    case FCMPG:
      // no NaN handling, could affect results
      return Float.compare(num1.longValue(), num2.longValue());
    case DCMPL:
    case DCMPG:
      // no NaN handling, could affect results
      return Double.compare(num1.longValue(), num2.longValue());
    default:
      return null;
    }
  }
}