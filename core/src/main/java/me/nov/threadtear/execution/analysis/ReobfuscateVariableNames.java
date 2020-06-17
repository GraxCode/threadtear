package me.nov.threadtear.execution.analysis;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import me.nov.threadtear.util.asm.Access;
import me.nov.threadtear.util.asm.Instructions;
import me.nov.threadtear.util.format.Strings;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ReobfuscateVariableNames extends Execution {
  public ReobfuscateVariableNames() {
    super(ExecutionCategory.ANALYSIS, "Reobfuscate variable names",
      "Reobfuscate method local variable names for easier analysis." +
        "<br>" +
        "Gets rid of default names like a, a2, ... and obfuscated names like 恼人的名字.",
      ExecutionTag.BETTER_DECOMPILE, ExecutionTag.POSSIBLE_DAMAGE);
  }

  @Override
  public String getAuthor() {
    return "ViRb3";
  }

  private int getVariableCount(MethodNode method) {
    final List<VarInsnNode> varInstrs = StreamSupport.stream(method.instructions.spliterator(), false)
      .filter(i -> i.getType() == AbstractInsnNode.VAR_INSN)
      .map(i -> (VarInsnNode) i)
      .collect(Collectors.toList());

    HashSet<Integer> loadVars = new HashSet<>();
    if (!Access.isStatic(method.access)) {
      loadVars.add(0);
    }
    varInstrs.stream().filter(Instructions::isLoadVarInsn).map(i -> i.var).forEach(loadVars::add);
    return loadVars.size();
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    Map<MethodNode, Integer> methodVariableCountMap = new HashMap<>();
    classes.values().stream()
      .map(c -> c.node.methods).flatMap(Collection::stream)
      .filter(m -> m.instructions != null && m.instructions.size() > 0)
      .forEach(m -> methodVariableCountMap.put(m, getVariableCount(m)));

    logger.info("Generating random names");
    Queue<String> words =
      Strings.generateWordQueue(methodVariableCountMap.values().stream().mapToInt(Integer::intValue).sum(),
        Objects.requireNonNull(ReobfuscateVariableNames.class.getResourceAsStream("english-words.txt")));
    int count = words.size();

    logger.info("Renaming variables");
    methodVariableCountMap.forEach((k, v) -> {
      if (v < 1) {
        return;
      }
      k.localVariables = new ArrayList<>();
      final LabelNode start = new LabelNode();
      final LabelNode end = new LabelNode();
      k.instructions.insertBefore(k.instructions.getFirst(), start);
      k.instructions.add(end);
      for (int i = 0; i < v; i++) {
        k.localVariables.add(
          new LocalVariableNode(words.poll(), "java/lang/Object", null, start, end, i));
      }
    });

    logger.info(count + " variables renamed successfully!");
    return count > 0;
  }
}
