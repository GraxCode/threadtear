package me.nov.threadtear.execution.paramorphism;

import java.util.*;

import me.nov.threadtear.execution.*;

public class BadAttributeRemover extends Execution {

  public BadAttributeRemover() {
    super(ExecutionCategory.PARAMORPHISM, "Remove bad attributes",
            "Removes all inner and outer class attributes.", ExecutionTag.POSSIBLE_DAMAGE);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    classes.values().stream().map(c -> c.node).forEach(c -> {
      c.innerClasses = new ArrayList<>();
      c.outerClass = null;
      c.outerMethod = null;
      c.outerMethodDesc = null;
    });
    // TODO check if bytecode is compatible
    logger.info("Removed all inner and outer class attributes.");
    return true;
  }
}
