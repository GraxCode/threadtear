package me.nov.threadtear.execution.allatori;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import me.nov.threadtear.util.asm.InstructionModifier;
import org.objectweb.asm.tree.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class JunkRemoverAllatori extends Execution {
  public JunkRemoverAllatori() {
    super(ExecutionCategory.ALLATORI, "Junk instruction remover",
            "Removes junk instructions that create a lot of boolean variables when " +
                    "decompiled with Fernflower.", ExecutionTag.BETTER_DECOMPILE);
  }

  @Override
  public boolean execute(Map<String, Clazz> map, boolean verbose) {
    int methodTotal = 0;
    int methodModified = 0;
    int removedTotal = 0;
    final List<ClassNode> classNodes = map.values().stream().map(c -> c.node).collect(Collectors.toList());
    for (ClassNode clazz : classNodes) {
      for (MethodNode method : clazz.methods) {
        methodTotal++;
        int removed = processMethod(method);
        removedTotal += removed;
        if (removed > 0) {
          methodModified++;
        }
      }
    }
    logger.info("Removed {} junk instructions from {}/{} methods.", removedTotal, methodModified, methodTotal);
    return true;
  }

  private int processMethod(MethodNode method) {
    AtomicInteger removed = new AtomicInteger();
    InstructionModifier modifier = new InstructionModifier();
    StreamSupport.stream(method.instructions.spliterator(), false)
            .filter(i -> i.getOpcode() == ICONST_1 && i.getNext() != null && i.getNext().getOpcode() == DUP &&
                    i.getNext().getNext() != null && i.getNext().getNext().getOpcode() == POP2).map(i -> (InsnNode) i)
            .forEach(i -> {
              removed.getAndIncrement();
              modifier.remove(i);
              modifier.remove(i.getNext());
              modifier.remove(i.getNext().getNext());
            });
    modifier.apply(method);
    return removed.get();
  }

  @Override
  public String getAuthor() {
    return "ViRb3";
  }
}
