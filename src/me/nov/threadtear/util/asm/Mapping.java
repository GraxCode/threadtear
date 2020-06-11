package me.nov.threadtear.util.asm;

import me.nov.threadtear.util.asm.method.MethodContext;
import me.nov.threadtear.util.asm.method.MethodSignature;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Provides various mappings between nodes.
 */
public class Mapping {

  /**
   * @return a map of every method and its callers. This
   * will not account for Reflection.
   */
  public static HashMap<MethodNode, List<MethodContext>> getMethodReferences(List<ClassNode> classes) {
    HashMap<MethodSignature, MethodContext> methodMap = getMethodSignatures(classes);
    HashMap<MethodNode, List<MethodContext>> referenceMap = new HashMap<>();

    for (ClassNode clazz : classes) {
      for (MethodNode method : clazz.methods) {
        final MethodContext callerCtx = new MethodContext(clazz, method);
        for (AbstractInsnNode instr : method.instructions) {
          if (instr instanceof MethodInsnNode) {
            MethodInsnNode min = (MethodInsnNode) instr;
            final MethodContext calleeCtx = methodMap.get(new MethodSignature(min));
            //TODO: Method calling itself is still
            // technically valid
            if (calleeCtx != null && calleeCtx.getMethod() != method) {
              referenceMap.computeIfAbsent(calleeCtx.getMethod(), k -> new ArrayList<>()).add(callerCtx);
            }
          }
        }
      }
    }

    return referenceMap;
  }

  /**
   * @return A map of every method's signature and its
   * {@link MethodContext}.
   */
  public static HashMap<MethodSignature, MethodContext> getMethodSignatures(List<ClassNode> classes) {
    HashMap<MethodSignature, MethodContext> methodMap = new HashMap<>();
    for (ClassNode clazz : classes) {
      for (MethodNode method : clazz.methods) {
        methodMap.put(new MethodSignature(clazz, method), new MethodContext(clazz, method));
      }
    }
    return methodMap;
  }
}
