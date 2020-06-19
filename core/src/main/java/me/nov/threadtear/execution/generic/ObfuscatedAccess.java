package me.nov.threadtear.execution.generic;

import java.util.*;

import org.objectweb.asm.tree.*;

import me.nov.threadtear.execution.*;
import me.nov.threadtear.util.asm.Access;

public class ObfuscatedAccess extends Execution {

  public ObfuscatedAccess() {
    super(ExecutionCategory.GENERIC, "Fix obfuscated access",
            "Fixes obfuscated access like synthetic or bridge.<br>Can break some " +
                    "decompilers, but mostly improves readability", ExecutionTag.POSSIBLE_VERIFY_ERR,
            ExecutionTag.BETTER_DECOMPILE);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    classes.values().stream().map(c -> c.node.methods).flatMap(List::stream).filter(this::shouldRemove)
            .forEach(m -> m.access = Access.removeAccess(m.access, ACC_SYNTHETIC, ACC_BRIDGE, ACC_DEPRECATED));
    classes.values().stream().map(c -> c.node.fields).flatMap(List::stream).filter(this::shouldRemove)
            .forEach(f -> f.access = Access.removeAccess(f.access, ACC_SYNTHETIC, ACC_BRIDGE, ACC_DEPRECATED));
    logger.info("Removed every synthetic, bridge and deprecated access");
    return true;
  }

  public boolean shouldRemove(FieldNode fn) {
    return !(Access.isFinal(fn.access) && fn.name.matches("(val\\$|this\\$).*"));
  }

  public boolean shouldRemove(MethodNode mn) {
    return !(Access.isStatic(mn.access) && mn.name.matches("(access\\$|lambda\\$).*"));
  }
}
