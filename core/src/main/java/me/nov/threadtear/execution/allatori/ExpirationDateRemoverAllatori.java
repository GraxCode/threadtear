package me.nov.threadtear.execution.allatori;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.*;

import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;

import me.nov.threadtear.analysis.stack.*;
import me.nov.threadtear.execution.*;

public class ExpirationDateRemoverAllatori extends Execution implements IConstantReferenceHandler {

  public ExpirationDateRemoverAllatori() {
    super(ExecutionCategory.ALLATORI, "Remove expiry date",
            "Allatori adds expiration dates to the code<br>that stop the obfuscated jar " +
                    "file from running after being passed.<br>They can be removed easily.",
            ExecutionTag.POSSIBLE_DAMAGE);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    try {
      logger.info("Finding most common long ldc cst");
      long mostCommon = classes.values().stream().map(c -> c.node.methods).flatMap(List::stream)
              .map(m -> m.instructions.spliterator()).flatMap(insns -> StreamSupport.stream(insns, false))
              .filter(ain -> ain.getOpcode() == LDC && ((LdcInsnNode) ain).cst instanceof Long)
              .map(ain -> (LdcInsnNode) ain)
              .filter(ldc -> Math.abs((long) ldc.cst - System.currentTimeMillis()) < 157784760000L)
              .collect(Collectors.groupingBy(ldc -> (long) ldc.cst, Collectors.counting())).entrySet().stream()
              .max(Entry.comparingByValue()).map(Entry::getKey).orElseThrow(RuntimeException::new);
      logger.info("Expiration date is " + new Date(mostCommon).toString() + ", replacing");
      classes.values().stream().map(c -> c.node.methods).flatMap(List::stream).map(m -> m.instructions.spliterator())
              .flatMap(insns -> StreamSupport.stream(insns, false))
              .filter(ain -> ain.getOpcode() == LDC && ((LdcInsnNode) ain).cst.equals(mostCommon))
              .map(ain -> (LdcInsnNode) ain).forEach(ldc -> ldc.cst = 1337133713371337L);
      return true;
    } catch (Exception e) {
      logger.error("Failure", e);
      return false;
    }
  }

  @Override
  public Object getFieldValueOrNull(BasicValue v, String owner, String name, String desc) {
    return null;
  }

  @Override
  public Object getMethodReturnOrNull(BasicValue v, String owner, String name, String desc,
                                      List<? extends ConstantValue> values) {
    return null;
  }
}
