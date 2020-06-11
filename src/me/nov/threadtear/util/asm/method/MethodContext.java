package me.nov.threadtear.util.asm.method;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * A bundle of {@link MethodNode} and its owner
 * {@link ClassNode}.
 */
public class MethodContext {

  private final ClassNode owner;
  private final MethodNode method;
  private final MethodSignature signature;

  public MethodContext(ClassNode owner, MethodNode method) {
    this.owner = owner;
    this.method = method;
    signature = new MethodSignature(owner, method);
  }

  public ClassNode getOwner() {
    return owner;
  }

  public MethodNode getMethod() {
    return method;
  }

  public MethodSignature getSignature() {
    return signature;
  }
}
