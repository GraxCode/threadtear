package me.nov.threadtear.analysis.rewriter.value.values;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;

import me.nov.threadtear.analysis.rewriter.value.CodeReferenceValue;
import me.nov.threadtear.util.asm.Instructions;

public class StringValue extends CodeReferenceValue {

  private String value;

  public StringValue(BasicValue type, AbstractInsnNode node, String value) {
    super(type, node);
    this.value = Objects.requireNonNull(value);
  }

  @Override
  public boolean isKnownValue() {
    return true;
  }

  @Override
  public boolean isRequiredInCode() {
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
    if (obj instanceof StringValue) {
      return ((StringValue) obj).value.equals(value);
    }
    return false;
  }

  @Override
  public InsnList cloneInstructions() {
    return Instructions.singleton(new LdcInsnNode(value));
  }

  @Override
  public List<AbstractInsnNode> getInstructions() {
    return Collections.singletonList(node);
  }

  @Override
  public Object getStackValueOrNull() {
    return value;
  }
}
