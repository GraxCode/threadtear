package me.nov.threadtear.execution.tools;

import java.util.Map;

import me.nov.threadtear.execution.*;
import me.nov.threadtear.util.asm.Instructions;

public class IsolatePossiblyMalicious extends Execution {

  private static final String POSSIBLY_MALICIOUS_REGEX =
          "(java/lang/runtime/Runtime|java/lang/reflect/|com/sun/jna|java/nio/file|java/io" +
                  "/File|sun/misc/Unsafe|java/io/.*Writer|java/io/.*Reader|org/apache/commons/io|io" +
                  "/netty/channel).*";
  private int changed;

  public IsolatePossiblyMalicious() {
    super(ExecutionCategory.TOOLS, "Isolate dangerous calls",
            "Isolate runtime, reflection and IO calls, so no damaging code can be executed" +
                    ".<br><p style=\"color:#e74c3c\"><b>This <i>DOESN'T</i> protect you fully against " +
                    "malicious code!</b></p><br>Libraries can be used to bypass the regex.",
            ExecutionTag.POSSIBLE_DAMAGE);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    this.changed = 0;
    logger.info("Isolating all {} classes", classes.size());
    classes.values().stream().map(c -> c.node).forEach(c -> c.methods.forEach(m -> {
      int oldSize = m.instructions.size();
      Instructions.isolateCallsThatMatch(m, (name, desc) -> name.matches(POSSIBLY_MALICIOUS_REGEX),
              (name, desc) -> name.matches(POSSIBLY_MALICIOUS_REGEX));
      if (oldSize != m.instructions.size()) {
        changed++;
        if (verbose) {
          logger.info("Removed calls in {}", referenceString(c, m));
        }
      }
    }));
    logger.info("{} methods containing calls were isolated", changed);
    return changed > 0;
  }
}
