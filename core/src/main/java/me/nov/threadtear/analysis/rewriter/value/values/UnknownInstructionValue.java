package me.nov.threadtear.analysis.rewriter.value.values;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;

import me.nov.threadtear.analysis.rewriter.value.CodeReferenceValue;
import me.nov.threadtear.util.asm.Instructions;

import java.util.Collections;
import java.util.List;

public class UnknownInstructionValue extends CodeReferenceValue {

  public UnknownInstructionValue(BasicValue type, AbstractInsnNode node) {
    super(type, node);
    if (node.getType() == AbstractInsnNode.LABEL || node.getType() == AbstractInsnNode.JUMP_INSN)
      throw new IllegalArgumentException();
  }

  @Override
  public boolean isKnownValue() {
    return false;
  }

  @Override
  public boolean isRequiredInCode() {
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
    if (getClass() != obj.getClass())
      return false;
    UnknownInstructionValue other = (UnknownInstructionValue) obj;
    if (!node.equals(other.node))
      return false;
    return true;
  }

  @Override
  public InsnList cloneInstructions() {
    return Instructions.singleton(node.clone(null));
  }

  @Override
  public List<AbstractInsnNode> getInstructions() {
    return Collections.singletonList(node);
  }

  @Override
  public Object getStackValueOrNull() {
    return null;
  }
}
