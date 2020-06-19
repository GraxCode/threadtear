package me.nov.threadtear.execution.generic.inliner;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import me.nov.threadtear.util.asm.method.MethodContext;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArgumentInliner extends Execution {

  public ArgumentInliner() {
    super(ExecutionCategory.GENERIC, "Argument inliner", "Inlines constant arguments from caller methods",
            ExecutionTag.BETTER_DECOMPILE, ExecutionTag.BETTER_DEOBFUSCATE, ExecutionTag.POSSIBLE_DAMAGE);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    final List<ClassNode> classNodes = classes.values().stream().map(c -> c.node).collect(Collectors.toList());
    ArgumentInfer argumentInfer = new ArgumentInfer(classNodes);
    int inlined = 0;
    int total = 0;
    for (ClassNode clazz : classNodes) {
      for (MethodNode method : clazz.methods) {
        total++;
        if (argumentInfer.inline(new MethodContext(clazz, method))) {
          inlined++;
        }
      }
    }
    logger.info("Inlined {}/{} methods.", inlined, total);
    return inlined > 0;
  }

  @Override
  public String getAuthor() {
    return "ViRb3";
  }
}
