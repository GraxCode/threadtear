package me.nov.threadtear.execution.generic.inliner;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;

public class JSRInliner extends Execution {

  public JSRInliner() {
    super(ExecutionCategory.GENERIC, "JSR opcode inliner",
      "Inlines JSR opcodes", ExecutionTag.POSSIBLE_VERIFY_ERR,
      ExecutionTag.BETTER_DECOMPILE);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    classes.values().forEach(c -> {
      for (int i = 0; i < c.node.methods.size(); i++) {
        MethodNode mn = c.node.methods.get(i);
        try {
          JSRInlinerAdapter adapter = new JSRInlinerAdapter(mn, mn.access, mn.name, mn.desc, mn.signature, mn.exceptions.toArray(new String[0]));
          mn.accept(adapter);
          c.node.methods.set(i, mn);
        } catch (Throwable t) {
          logger.error("Failed to inline JSRs in {}", t, referenceString(c.node, mn));
        }
      }
    });
    logger.info("Inlined all JSR instructions");
    return true;
  }
}
