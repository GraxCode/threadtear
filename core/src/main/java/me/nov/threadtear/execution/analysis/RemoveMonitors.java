package me.nov.threadtear.execution.analysis;

import java.util.Map;

import org.objectweb.asm.tree.*;

import me.nov.threadtear.execution.*;

public class RemoveMonitors extends Execution {

  public RemoveMonitors() {
    super(ExecutionCategory.ANALYSIS, "Remove synchronized blocks",
            "Removes all monitorenter and monitorexit instructions", ExecutionTag.POSSIBLE_DAMAGE,
            ExecutionTag.BETTER_DECOMPILE);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    classes.values().stream().map(c -> c.node).forEach(c -> c.methods.forEach(m -> {
      for (AbstractInsnNode ain : m.instructions.toArray())
        if (ain.getOpcode() == MONITORENTER || ain.getOpcode() == MONITOREXIT)
          m.instructions.set(ain, new InsnNode(POP));
    }));
    logger.info("Removed all synchronized blocks");
    return true;
  }
}
