package me.nov.threadtear.execution.analysis;

import java.util.*;

import me.nov.threadtear.execution.*;

public class RemoveTCBs extends Execution {

  public RemoveTCBs() {
    super(ExecutionCategory.ANALYSIS, "Remove try catch blocks", "Removes all try catch blocks",
            ExecutionTag.POSSIBLE_DAMAGE, ExecutionTag.BETTER_DECOMPILE);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    classes.values().stream().map(c -> c.node.methods).flatMap(List::stream).forEach(m -> m.tryCatchBlocks.clear());
    logger.info("Removed all try catch blocks");
    return true;
  }
}
