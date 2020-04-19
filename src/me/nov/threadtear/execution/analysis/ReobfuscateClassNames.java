package me.nov.threadtear.execution.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.util.References;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import me.nov.threadtear.util.Strings;

public class ReobfuscateClassNames extends Execution {

	public ReobfuscateClassNames() {
		super(ExecutionCategory.ANALYSIS, "Reobfuscate class names", "Reobfuscate class names for easier analysis.<br>Gets rid of annoying class names like IlIllIlI.", ExecutionTag.BETTER_DECOMPILE,
				ExecutionTag.POSSIBLE_DAMAGE);
	}

	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose) {
		logger.info("Generating random names");
		Queue<String> words = Strings.generateWordQueue(classes.size());
		Map<String, String> map = classes.stream().collect(Collectors.toMap(c -> c.node.name, c -> words.poll()));
		if (verbose) {
			logger.info("Generated " + map.size() + " unique easy-to-remember strings");
			logger.info("Renaming classes and source files to original names");
		}
		classes.stream().forEach(c -> {
			c.node.sourceFile = c.node.name; // to have a connection with original file
			c.node.name = map.getOrDefault(c.node.name, c.node.name);
		});
		logger.info("Updating code references");
		int refs = classes.stream().map(c -> c.node.methods).flatMap(List::stream).map(m -> m.instructions.toArray()).flatMap(Arrays::stream).mapToInt(ain -> References.remapInstructionDescs(map, ain))
				.sum();
		logger.info(refs + " code references updated successfully!");
		classes.stream().map(c -> c.node.methods).flatMap(List::stream).forEach(m -> References.remapMethodType(map, m));
		classes.stream().map(c -> c.node.fields).flatMap(List::stream).forEach(f -> References.remapFieldType(map, f));
		classes.stream().map(c -> c.node).forEach(c -> References.remapClassType(map, c));
		logger.info("Updated remaining references successfully!");
		return true;
	}
}
