package me.nov.threadtear.execution.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import javax.lang.model.SourceVersion;

import org.apache.commons.io.IOUtils;

import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.util.References;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;

public class ReobfuscateClassNames extends Execution {

	public ReobfuscateClassNames() {
		super(ExecutionCategory.ANALYSIS, "Reobfuscate class names", "Reobfuscate class names for easier analysis");
	}

	private Map<String, String> map;

	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose, boolean ignoreErr) {
		Queue<String> words = generateWordQueue(classes.size());
		logger.info("Generating random names");
		map = classes.stream().collect(Collectors.toMap(c -> c.node.name, c -> words.poll()));
		if (verbose) {
			logger.info("Generated " + map.size() + " unique easy-to-remember strings");
			logger.info("Renaming classes and source files to original names");
		}
		classes.stream().forEach(c -> {
			c.node.sourceFile = c.node.name; // to have a connection with original file
			c.node.name = map.getOrDefault(c.node.name, c.node.name);
		});
		logger.info("Updating code references");
		int refs = classes.stream().map(c -> c.node.methods).flatMap(List::stream).map(m -> m.instructions.toArray())
				.flatMap(Arrays::stream).mapToInt(ain -> References.remapInstruction(map, ain)).sum();
		logger.info(refs + " code references updated successfully!");
		classes.stream().map(c -> c.node.methods).flatMap(List::stream).forEach(m -> References.remapMethodType(map, m));
		classes.stream().map(c -> c.node.fields).flatMap(List::stream).forEach(f -> References.remapFieldType(map, f));
		classes.stream().map(c -> c.node).forEach(c -> References.remapClassType(map, c));
		logger.info("Updated remaining references successfully!");
		return true;
	}

	private Queue<String> generateWordQueue(int amount) {
		Queue<String> queue = new LinkedList<>();
		try {
			String nouns = IOUtils.toString(ReobfuscateClassNames.class.getResourceAsStream("/res/english-nouns.txt"),
					"UTF-8");
			String[] words = nouns.split("\n");
			int i = 0;
			while (queue.size() < amount) {
				String word = i >= words.length ? generateWord(6) : words[i];
				if (SourceVersion.isName(word)) {
					queue.add(word);
				}
				i++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return queue;
	}

	private static final String goodConsonants = "bcdfglmnprstvyz";
	private static final String vocals = "aeiou";

	public String generateWord(int len) {
		boolean vocal = random.nextBoolean();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < len; i++) {
			if (vocal) {
				sb.append(goodConsonants.charAt(random.nextInt(goodConsonants.length())));
			} else {
				sb.append(vocals.charAt(random.nextInt(vocals.length())));
			}
			vocal = !vocal;
		}
		return sb.toString();
	}
}
