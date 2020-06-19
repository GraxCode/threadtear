package me.nov.threadtear.util.asm.method;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Objects;

/**
 * A comparable method signature that includes its owner
 * class's name.
 */
public class MethodSignature {
  private final String owner;
  private final String name;
  private final String desc;

  public MethodSignature(MethodInsnNode min) {
    this.owner = min.owner;
    this.name = min.name;
    this.desc = min.desc;
  }

  public MethodSignature(String owner, String name, String desc) {
    this.owner = owner;
    this.name = name;
    this.desc = desc;
  }

  public MethodSignature(ClassNode clazz, MethodNode method) {
    this.owner = clazz.name;
    this.name = method.name;
    this.desc = method.desc;
  }

  public MethodSignature(String owner, MethodNode method) {
    this.owner = owner;
    this.name = method.name;
    this.desc = method.desc;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    MethodSignature that = (MethodSignature) o;
    return Objects.equals(owner, that.owner) && Objects.equals(name, that.name) && Objects.equals(desc, that.desc);
  }

  @Override
  public String toString() {
    return "MethodSignature{owner='" + owner + '\'' + ", name='" + name + '\'' + ", desc='" + desc + '\'' + '}';
  }

  @Override
  public int hashCode() {
    return Objects.hash(owner, name, desc);
  }
}
