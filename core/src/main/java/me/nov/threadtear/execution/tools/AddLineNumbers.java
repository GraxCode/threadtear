package me.nov.threadtear.execution.tools;

import java.util.Map;

import org.objectweb.asm.tree.*;

import me.nov.threadtear.execution.*;

public class AddLineNumbers extends Execution {

  private int line;
  private int method;

  public AddLineNumbers() {
    super(ExecutionCategory.TOOLS, "Add debug line numbers",
            "Adds line numbers to the code to find out where exactly exceptions happen.",
            ExecutionTag.RUNNABLE);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    classes.values().stream().map(c -> c.node).forEach(c -> {
      if (c.sourceFile == null) {
        c.sourceFile = c.name.hashCode() + ".java";
      }
      method = 0;
      c.methods.forEach(m -> {
        m.instructions.forEach(ain -> {
          if (ain.getType() == AbstractInsnNode.LINE) {
            m.instructions.remove(ain);
          }
        });
        line = method * 10000 + 1;
        m.instructions.forEach(ain -> {
          if (couldThrow(ain)) {
            LabelNode start = new LabelNode();
            m.instructions.insertBefore(ain, start);
            m.instructions.insertBefore(ain, new LineNumberNode(line++, start));
          }
        });
        method++;
      });
    });

    logger.info("Created fake line numbers for debugging purposes!");
    return true;
  }

  private boolean couldThrow(AbstractInsnNode ain) {
    if (ain.getType() == AbstractInsnNode.METHOD_INSN)
      return true;
    switch (ain.getOpcode()) {
      case IASTORE:
      case LASTORE:
      case FASTORE:
      case DASTORE:
      case AASTORE:
      case BASTORE:
      case CASTORE:
      case SASTORE:
      case IALOAD:
      case LALOAD:
      case FALOAD:
      case DALOAD:
      case AALOAD:
      case BALOAD:
      case CALOAD:
      case SALOAD:
      case ATHROW:
        return true;
    }
    return false;
  }
}
