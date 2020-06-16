package me.nov.threadtear.execution.tools;

import java.util.*;

import me.nov.threadtear.execution.*;

public class RemoveMaxs extends Execution {

  public RemoveMaxs() {
    super(ExecutionCategory.TOOLS, "Remove maxs", "Removes max local and stack limit", ExecutionTag.RUNNABLE);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    classes.values().stream().map(c -> c.node.methods).flatMap(List::stream)
            .forEach(m -> m.maxLocals = m.maxStack = 1337);
    logger.info("Removed frame limits");
    return true;
  }
}