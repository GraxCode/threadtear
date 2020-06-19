package me.nov.threadtear.execution.cleanup.remove;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import me.nov.threadtear.util.asm.Access;
import me.nov.threadtear.util.asm.InstructionModifier;
import me.nov.threadtear.util.asm.Instructions;
import org.objectweb.asm.tree.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


public class RemoveUnusedVariables extends Execution {
  public RemoveUnusedVariables() {
    super(ExecutionCategory.CLEANING, "Remove unused variables", "Removes unused variables",
            ExecutionTag.BETTER_DECOMPILE);
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
    logger.info("Removed {} unused variables from {}/{} methods.", removedTotal, methodModified, methodTotal);
    return true;
  }

  private int processMethod(MethodNode method) {
    final List<VarInsnNode> varInstrs = StreamSupport.stream(method.instructions.spliterator(), false)
            .filter(i -> i.getType() == AbstractInsnNode.VAR_INSN).map(i -> (VarInsnNode) i)
            .collect(Collectors.toList());

    HashSet<Integer> loadVars = new HashSet<>();
    if (!Access.isStatic(method.access)) {
      loadVars.add(0);
    }
    varInstrs.stream().filter(Instructions::isLoadVarInsn).map(i -> i.var).forEach(loadVars::add);

    AtomicInteger removed = new AtomicInteger();
    InstructionModifier modifier = new InstructionModifier();
    varInstrs.stream().filter(i -> Instructions.isStoreVarInsn(i) && !loadVars.contains(i.var)).forEach(i -> {
      removed.getAndIncrement();
      if (Instructions.isWideVarInsn(i))
        modifier.replace(i, new InsnNode(POP2));
      else
        modifier.replace(i, new InsnNode(POP));
    });
    modifier.apply(method);
    return removed.get();
  }
}
