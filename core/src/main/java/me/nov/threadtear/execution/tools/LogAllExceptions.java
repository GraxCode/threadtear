package me.nov.threadtear.execution.tools;

import java.util.*;

import org.objectweb.asm.tree.*;

import me.nov.threadtear.execution.*;
import me.nov.threadtear.util.asm.Instructions;

public class LogAllExceptions extends Execution {

  public LogAllExceptions() {
    super(ExecutionCategory.TOOLS, "Log all exceptions",
            "Adds .printStackTrace() in every try catch block handler.", ExecutionTag.RUNNABLE);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    classes.values().stream().map(c -> c.node.methods).flatMap(List::stream).forEach(m -> {
      if (m.tryCatchBlocks == null)
        return;
      m.tryCatchBlocks.forEach(tcb -> {
        AbstractInsnNode firstInstructionAfterHandler = Instructions.getRealNext(tcb.handler);
        if (!printsAlready(firstInstructionAfterHandler)) {
          m.instructions.insertBefore(firstInstructionAfterHandler, new InsnNode(DUP));
          m.instructions.insertBefore(firstInstructionAfterHandler,
                  new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V"));
        }
      });
      m.maxStack = Math.max(m.maxStack, 2);
    });
    logger.info("Inserted .printStackTrace() in every catch block!");
    return true;
  }

  private boolean printsAlready(AbstractInsnNode ain) {
    if (ain == null || ain.getType() == AbstractInsnNode.JUMP_INSN || Instructions.isCodeEnd(ain)) {
      return false;
    }
    if (ain.getOpcode() == INVOKEVIRTUAL) {
      MethodInsnNode min = (MethodInsnNode) ain;
      if (min.name.equals("printStackTrace") && min.desc.equals("()V")) {
        return true;
      }
    }
    return printsAlready(Instructions.getRealNext(ain));
  }
}
