package me.nov.threadtear.analysis.rewriter.value.values;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;

import me.nov.threadtear.analysis.rewriter.value.CodeReferenceValue;

public class MemberAccessValue extends CodeReferenceValue {

  private CodeReferenceValue ownerRef;
  public String owner;
  public String name;
  public String desc;

  public MemberAccessValue(BasicValue type, CodeReferenceValue ownerReference, AbstractInsnNode node, String owner,
                           String name, String desc) {
    super(type, node);
    if (!(node instanceof MethodInsnNode || node instanceof FieldInsnNode)) {
      throw new IllegalArgumentException();
    }
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
    if (node != other.node)
      return false;
    if (owner == null) {
      return other.owner == null;
    } else
      return owner.equals(other.owner);
  }

  @Override
  public InsnList cloneInstructions() {
    InsnList list = new InsnList();
    if (ownerRef != null) {
      list.add(ownerRef.cloneInstructions());
    }
    switch (node.getOpcode()) {
      case GETSTATIC:
      case PUTSTATIC:
      case GETFIELD:
      case PUTFIELD:
        list.add(new FieldInsnNode(node.getOpcode(), owner, name, desc));
        break;
      case INVOKEINTERFACE:
      case INVOKESTATIC:
      case INVOKEVIRTUAL:
      case INVOKESPECIAL:
        list.add(new MethodInsnNode(node.getOpcode(), owner, name, desc));
        break;
      default:
        throw new IllegalArgumentException();
    }
    return list;
  }

  @Override
  public List<AbstractInsnNode> getInstructions() {
    List<AbstractInsnNode> list = new ArrayList<>();
    if (ownerRef != null) {
      list.addAll(ownerRef.getInstructions());
    }
    list.add(node);
    return list;
  }

  @Override
  public Object getStackValueOrNull() {
    return null;
  }
}
