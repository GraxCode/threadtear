package me.nov.threadtear.execution.analysis;

import java.util.*;
import java.util.stream.Collectors;

import me.nov.threadtear.execution.*;
import me.nov.threadtear.util.asm.References;
import me.nov.threadtear.util.format.Strings;

public class ReobfuscateClassNames extends Execution {

  public ReobfuscateClassNames() {
    super(ExecutionCategory.ANALYSIS, "Reobfuscate class names",
            "Reobfuscate class names for easier analysis.<br>Gets rid of annoying class names like IlIllIlI.",
            ExecutionTag.BETTER_DECOMPILE, ExecutionTag.POSSIBLE_DAMAGE);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    logger.info("Generating random names");
    Queue<String> words = Strings.generateWordQueue(classes.size(),
            Objects.requireNonNull(ReobfuscateClassNames.class.getResourceAsStream("names.txt")));
    Map<String, String> map = classes.values().stream().collect(Collectors.toMap(c -> c.node.name, c -> words.poll()));
    if (verbose) {
      logger.info("Generated {} unique easy-to-remember strings", map.size());
      logger.info("Renaming classes and source files to original names");
    }
    classes.values().forEach(c -> {
      c.node.sourceFile = c.node.name; // to have a
      // connection with original file
      c.node.name = map.getOrDefault(c.node.name, c.node.name);
    });
    logger.info("Updating code references");
    int refs =
            classes.values().stream().map(c -> c.node.methods).flatMap(List::stream).map(m -> m.instructions.toArray())
                    .flatMap(Arrays::stream).mapToInt(ain -> References.remapClassRefs(map, ain)).sum();
    logger.info(refs + " code references updated successfully!");
    classes.values().stream().map(c -> c.node.methods).flatMap(List::stream)
            .forEach(m -> References.remapMethodType(map, m));
    classes.values().stream().map(c -> c.node.fields).flatMap(List::stream)
            .forEach(f -> References.remapFieldType(map, f));
    classes.values().stream().map(c -> c.node).forEach(c -> References.remapClassType(map, c));
    logger.info("Updated remaining references successfully!");
    return true;
  }
}
