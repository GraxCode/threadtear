package me.nov.threadtear.execution.tools;

import java.util.Map;

import me.nov.threadtear.execution.*;

public class Java8Compatibility extends Execution {

  public Java8Compatibility() {
    super(ExecutionCategory.TOOLS, "Make Java 8 compatible",
            "Only works when no Java 9+ specific methods are present.", ExecutionTag.POSSIBLE_DAMAGE);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    logger.info("Changing compability to Java 8+. Only works when no Java 9+ specific methods are present.");
    classes.values().stream().map(c -> c.node).forEach(c -> c.version = 52);
    // TODO check if bytecode is compatible
    logger.info("Compatibility changed to Java 8+");
    return true;
  }
}
