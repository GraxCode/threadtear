package me.nov.threadtear.execution.zkm;

import java.util.*;
import java.util.stream.Collectors;

import org.objectweb.asm.tree.*;

import me.nov.threadtear.execution.*;
import me.nov.threadtear.util.asm.Instructions;

/**
 * Works with ZKM 8 - 11, newer versions remain untested
 */
public class TryCatchObfuscationRemover extends Execution {

  private Map<String, Clazz> classes;
  private boolean verbose;

  public TryCatchObfuscationRemover() {
    super(ExecutionCategory.ZKM, "Remove unnecessary try " + "catch blocks", "Remove try catch block flow " +
            "obfuscation by ZKM.<br>Makes decompiling a " + "lot easier.", ExecutionTag.RUNNABLE,
            ExecutionTag.BETTER_DECOMPILE);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    this.verbose = verbose;
    this.classes = classes;
    logger.info("Removing redundant try catch blocks by " + "ZKM");
    long tcbs = getAmountBlocks();
    classes.values().stream().map(c -> c.node).forEach(c -> checkTCBs(c, c.methods));
    long amount = (tcbs - getAmountBlocks());
    logger.info("Finished, removed {} blocks of {} total " + "blocks!", amount, tcbs);
    return amount > 0;
  }

  private long getAmountBlocks() {
    return classes.values().stream().map(c -> c.node.methods).flatMap(List::stream).map(m -> m.tryCatchBlocks)
            .mapToLong(List::size).sum();
  }

  public void checkTCBs(ClassNode c, List<MethodNode> methods) {
    methods.forEach(m -> {
      if (m.tryCatchBlocks.stream().anyMatch(this::isFake)) {
        m.tryCatchBlocks.stream().filter(this::isFake).collect(Collectors.toSet())
                .forEach(tcb -> m.tryCatchBlocks.remove(tcb));
        Instructions.removeDeadCode(c, m);
      }
    });
  }

  public boolean isFake(TryCatchBlockNode tcbn) {
    AbstractInsnNode ain = Instructions.getRealNext(tcbn.handler);
    if (ain == null || ain.getOpcode() == ATHROW) {
      return true;
    } else if (ain.getType() == AbstractInsnNode.METHOD_INSN && ain.getNext().getOpcode() == ATHROW) {
      MethodInsnNode min = (MethodInsnNode) ain;
      Clazz clazz = classes.get(min.owner);
      if (clazz == null) {
        if (verbose)
          logger.warning("Class {} not found, possibly " + "library", min.owner);
        return false;
      }
      MethodNode getter = getMethod(clazz.node, min.name, min.desc);
      if (getter == null) {
        if (verbose)
          logger.warning("Getter {} not found, possibly " + "library", min.owner + "." + min.name + min.desc);
        return false;
      }
      AbstractInsnNode getterFirst = getter.instructions.getFirst();
      while (getterFirst.getOpcode() == -1) {
        getterFirst = ain.getNext();
      }
      if (getterFirst instanceof VarInsnNode && getterFirst.getNext().getOpcode() == ARETURN) {
        return ((VarInsnNode) getterFirst).var == 0;
      }
    }
    // method not matching zkm fake athrow getter
    return false;
  }
}