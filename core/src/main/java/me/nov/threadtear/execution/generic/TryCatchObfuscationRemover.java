package me.nov.threadtear.execution.generic;

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
    super(ExecutionCategory.GENERIC, "Remove unnecessary try catch blocks",
      "Remove try catch block flow obfuscation.<br>Makes decompiling a lot easier.",
      ExecutionTag.RUNNABLE, ExecutionTag.BETTER_DECOMPILE);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    this.verbose = verbose;
    this.classes = classes;
    logger.info("Removing redundant try catch blocks");
    // TODO: recursive scan to check if catch type is ever thrown
    long tcbs = getAmountBlocks();
    classes.values().stream().map(c -> c.node).forEach(c -> checkTCBs(c, c.methods));
    long amount = (tcbs - getAmountBlocks());
    logger.info("Finished, removed {} blocks of {} total blocks!", amount, tcbs);
    return amount > 0;
  }

  private long getAmountBlocks() {
    return classes.values().stream().map(c -> c.node.methods).flatMap(List::stream).map(m -> m.tryCatchBlocks)
      .mapToLong(List::size).sum();
  }

  public void checkTCBs(ClassNode c, List<MethodNode> methods) {
    methods.forEach(m -> {
      m.tryCatchBlocks.removeIf(this::isFake);
      m.tryCatchBlocks.removeIf(tcb -> isNonsense(m, tcb));
      Instructions.removeDeadCode(c, m);
    });
  }

  private boolean isNonsense(MethodNode mn, TryCatchBlockNode tcbn) {
    return tcbn.start == tcbn.end || mn.instructions.indexOf(tcbn.start) >= mn.instructions.indexOf(tcbn.end);
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
          logger.warning("Class {} not found, possibly library", min.owner);
        return false;
      }
      MethodNode getter = getMethod(clazz.node, min.name, min.desc);
      if (getter == null) {
        if (verbose)
          logger.warning("Getter {} not found, possibly library", min.owner + "." + min.name + min.desc);
        return false;
      }
      if ((getter.access & ACC_NATIVE) != 0) {
        if (verbose)
          logger.warning("Getter {} is a native method, skipping", min.owner + "." + min.name + min.desc);
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
