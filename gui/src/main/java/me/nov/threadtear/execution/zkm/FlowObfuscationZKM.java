package me.nov.threadtear.execution.zkm;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;
import java.util.function.Predicate;

public class FlowObfuscationZKM extends Execution {

  public FlowObfuscationZKM() {
    super(ExecutionCategory.ZKM, "Flow obfuscation " + "removal",
            "Tested on ZKM 14, could work on " + "newer " + "versions too.", ExecutionTag.POSSIBLE_DAMAGE,
            ExecutionTag.BETTER_DECOMPILE);
  }

  private int replaced;

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    replaced = 0;
    logger.info("Removing all garbage jumps");
    classes.values().stream().map(c -> c.node).forEach(c -> c.methods.forEach(this::removeZKMJumps));
    logger.info("Removed {} jumps matching ZKM pattern in" + " total", replaced);
    return replaced > 0;
  }

  private static final Predicate<Integer> singleJump =
          op -> (op >= IFEQ && op <= IFLE) || op == IFNULL || op == IFNONNULL;

  public void removeZKMJumps(MethodNode mn) {
    for (AbstractInsnNode ain : mn.instructions.toArray()) {
      if (ain.getPrevious() != null && singleJump.test(ain.getOpcode())) {
        AbstractInsnNode previous = ain.getPrevious();
        if (previous.getOpcode() == IFNULL || previous.getOpcode() == IFNONNULL) {
          AbstractInsnNode previousPrevious = previous.getPrevious();
          if (previousPrevious != null && previousPrevious.getOpcode() == ALOAD) {
            mn.instructions.set(previous, new InsnNode(POP));
            replaced++;
          }
        }
      }
    }
  }
}
