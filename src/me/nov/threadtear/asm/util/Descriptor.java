package me.nov.threadtear.asm.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Descriptor {
	public static String fixDesc(String description, Map<String, String> map) {
		if (description == null || description.isEmpty() || isPrimitive(description)) {
			return description;
		}
		if (description.contains("L") && description.contains(";")) {
			if (description.startsWith("(")
					|| (description.startsWith("L") || description.startsWith("[")) && description.endsWith(";")) {
				String regex = "(?<=[L])[^;]*(?=;)";
				Pattern p = Pattern.compile(regex);
				Matcher m = p.matcher(Pattern.quote(description));
				StringBuffer sb = new StringBuffer(description.length());
				while (m.find()) {
					m.appendReplacement(sb, Matcher.quoteReplacement(fixDesc(m.group(), map)));
				}
				m.appendTail(sb);
				String result = sb.toString();
				return result.substring(2, result.length() - 2); // remove Pattern.quote
			}
		} else {
			return map.getOrDefault(description, description);
		}
		return description;
	}

	public static boolean isPrimitive(String description) {
		String x = description.replaceAll("(?=([L;()\\/\\[IDFJBZV]))", "");
		if (x.isEmpty()) {
			return true;
		} else if (x.equals("Z") || x.equals("J") || x.equals("I") || x.equals("F") || x.equals("D") || x.equals("C")
				|| x.equals("T") || x.equals("G")) {
			return true;
		}
		return false;
	}

	public static ArrayList<Integer> calculateAmountArguments(String desc) {
		ArrayList<Integer> sizes = new ArrayList<>();
		boolean inObject = false;
		boolean nextIsObject = false;
		for (char c : desc.toCharArray()) {
			if (inObject) {
				if (c == ';') {
					inObject = false;
				}
				continue;
			}
			if (c == 'J') {
				inObject = true;
			}
			if (c == '[') {
				nextIsObject = true;
				continue;
			}
			sizes.add(nextIsObject ? 1 : getSize(c));
			nextIsObject = false;
		}
		return sizes;

	}

	public static int getSize(char type) {
		if (type == 'J' || type == 'D') {
			return 2;
		}
		return 1;
	}
}
