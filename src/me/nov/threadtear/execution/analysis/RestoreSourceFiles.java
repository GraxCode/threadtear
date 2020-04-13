package me.nov.threadtear.execution.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.util.References;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;

public class RestoreSourceFiles extends Execution {

	public RestoreSourceFiles() {
		super(ExecutionCategory.ANALYSIS, "Restore names by source file",
				"Restore class names by their source file attribute, if it isn't null.<br>Could reverse obfuscation with bad configuration.");
	}

	private Map<String, String> map;

	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose, boolean ignoreErr) {
		logger.info("Generating mappings for source file attributes");
		map = classes.stream().filter(c -> c.node.sourceFile != null && c.node.sourceFile.endsWith(".java")).collect(
				Collectors.toMap(c -> c.node.name, c -> c.node.sourceFile.substring(0, c.node.sourceFile.length() - 5)));
		boolean duplicateFound = false;
		if (map.size() < classes.size()) {
			logger.warning(map.size() + " classes of " + classes.size() + " have a valid source file attribute.");
			if (map.isEmpty()) {
				logger.severe("No source file attribute found, nothing to do, returning!");
				return false;
			}
		}
		// check for duplicates
		for (Entry<String, String> entry : map.entrySet()) {
			long count = map.values().stream().filter(sf -> sf.equalsIgnoreCase(entry.getValue())).count();
			if (count > 1) {
				if (!duplicateFound) {
					logger.warning("Duplicate mapping was found! Numbering classes with multiple occurrences!");
					duplicateFound = true;
				}
				if (verbose) {
					logger.warning(entry.getValue() + " exists " + count + " times! Renaming...");
				}
				// rename duplicate
				entry.setValue(entry.getValue() + count);
			}
		}
		logger.info("Updating class names");
		classes.stream().forEach(c -> c.node.name = map.getOrDefault(c.node.name, c.node.name));
		logger.info("Updating code references");
		int refs = classes.stream().map(c -> c.node.methods).flatMap(List::stream).map(m -> m.instructions.toArray())
				.flatMap(Arrays::stream).mapToInt(ain -> References.remapInstruction(map, ain)).sum();
		logger.info(refs + " code references updated successfully!");
		classes.stream().map(c -> c.node.methods).flatMap(List::stream).forEach(m -> References.remapMethodType(map, m));
		classes.stream().map(c -> c.node.fields).flatMap(List::stream).forEach(f -> References.remapFieldType(map, f));
		classes.stream().map(c -> c.node).forEach(c -> References.remapClassType(map, c));
		logger.info("Successfully updated remaining references");
		return true;
	}
}
