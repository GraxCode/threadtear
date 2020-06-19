package me.nov.threadtear.execution.zkm;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class FlowObfuscationZKM extends Execution {

  private static final Predicate<Integer> singleJump =
    op -> (op >= IFEQ && op <= IFLE) || op == IFNULL || op == IFNONNULL;
  private int replaced;

  public FlowObfuscationZKM() {
    super(ExecutionCategory.ZKM, "Flow obfuscation removal",
          "Tested on ZKM 14, could work on newer versions too.", ExecutionTag.POSSIBLE_DAMAGE,
          ExecutionTag.BETTER_DECOMPILE);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    replaced = 0;
    logger.info("Removing all garbage jumps");
    classes.values().stream().map(c -> c.node).forEach(c -> c.methods.forEach(this::removeZKMJumps));
    logger.info("Removed {} jumps matching ZKM pattern in total", replaced);
    return replaced > 0;
  }

  public void removeZKMJumps(MethodNode mn) {
    for (AbstractInsnNode ain : mn.instructions.toArray()) {
      if (ain.getPrevious() != null && singleJump.test(ain.getOpcode())) {
        AbstractInsnNode previous = ain.getPrevious();
        boolean shouldPop = false;
        if (ain.getOpcode() == IFNULL || ain.getOpcode() == IFNONNULL) { //first case flow obfuscation scenario
          if (previous.getOpcode() == ALOAD) {
            shouldPop = true;
          }
        } else if (ain.getOpcode() >= IFEQ && ain.getOpcode() <= IFLE) { //second case
          if (previous.getOpcode() == ILOAD) {
            shouldPop = true;
          }
        }
        if (shouldPop) {
          mn.instructions.set(ain, new InsnNode(POP));
          replaced++;
        }
      }
    }
  }
}
