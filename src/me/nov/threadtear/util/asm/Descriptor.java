package me.nov.threadtear.util.asm;

import java.util.Map;

import org.objectweb.asm.Type;

public class Descriptor {
	public static String fixMethodDesc(String desc, Map<String, String> map) {
		assert (desc.contains("(") && desc.contains(")"));
		StringBuilder newDesc = new StringBuilder("(");
		for (Type t : Type.getArgumentTypes(desc)) {
			appendRemappedType(map, newDesc, t);
		}
		newDesc.append(")");
		appendRemappedType(map, newDesc, Type.getReturnType(desc));
		return newDesc.toString();
	}

	public static String fixTypeDesc(String type, Map<String, String> map) {
		StringBuilder newDesc = new StringBuilder();
		appendRemappedType(map, newDesc, Type.getType(type));
		return newDesc.toString();
	}

	public static Type fixType(Type type, Map<String, String> map) {
		StringBuilder newDesc = new StringBuilder();
		appendRemappedType(map, newDesc, type);
		return Type.getType(newDesc.toString());
	}

	private static void appendRemappedType(Map<String, String> map, StringBuilder sb, Type t) {
		if (t.getSort() == Type.OBJECT) {
			String internalName = t.getInternalName();
			sb.append("L");
			sb.append(map.getOrDefault(internalName, internalName));
			sb.append(";");
		} else if (t.getSort() == Type.ARRAY && t.getElementType().getSort() == Type.OBJECT) {
			for (int i = 0; i < t.getDimensions(); i++)
				sb.append("[");
			String internalName = t.getElementType().getInternalName();
			sb.append("L");
			sb.append(map.getOrDefault(internalName, internalName));
			sb.append(";");
		} else {
			sb.append(t.getDescriptor());
		}
	}

	public static boolean isPrimitive(String description) {
		String x = description.replaceAll("(?=([L;()\\/\\[IDFJBZV]))", "");
		if (x.isEmpty()) {
			return true;
		} else if (x.equals("Z") || x.equals("J") || x.equals("I") || x.equals("F") || x.equals("D") || x.equals("C") || x.equals("T") || x.equals("G")) {
			return true;
		}
		return false;
	}
}
