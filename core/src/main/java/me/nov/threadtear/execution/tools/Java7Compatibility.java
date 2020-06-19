package me.nov.threadtear.execution.tools;

import java.util.Map;
import java.util.stream.StreamSupport;

import me.nov.threadtear.execution.*;

public class Java7Compatibility extends Execution {

  private boolean success;

  public Java7Compatibility() {
    super(ExecutionCategory.TOOLS, "Make Java 7 compatible",
            "Only works when no lambda expressions and no Java 8 specific methods are present.",
            ExecutionTag.POSSIBLE_DAMAGE);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    classes.values().stream().map(c -> c.node).forEach(c -> c.version = 51);
    success = true;
    classes.values().stream().map(c -> c.node).forEach(c -> c.methods.forEach(m -> {
      if (StreamSupport.stream(m.instructions.spliterator(), false).anyMatch(ain -> ain.getOpcode() == INVOKEDYNAMIC)) {
        logger.error("{} contains instructions that are not supported by Java 7.", referenceString(c, m));
        success = false;
      }
    }));
    logger.info("Compatibility changed to Java 7+");
    return success;
  }
}
