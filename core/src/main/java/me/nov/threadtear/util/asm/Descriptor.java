package me.nov.threadtear.util.asm;

import java.util.Map;

import org.objectweb.asm.Type;

public final class Descriptor {
  private Descriptor() {
  }

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
    String x = description.replaceAll("(?=([L;()/\\[IDFJBZV]))", "");
    if (x.isEmpty()) {
      return true;
    } else if (x.equals("Z") || x.equals("J") || x.equals("I") || x.equals("F") || x.equals("D") || x.equals("C") ||
      x.equals("T") || x.equals("G")) {
      return true;
    }
    return false;
  }

  public static String generalize(Type methodType) {
    StringBuilder sb = new StringBuilder("(");
    for (Type arg : methodType.getArgumentTypes())
      generalize(sb, arg);
    sb.append(")");
    generalize(sb, methodType.getReturnType());
    return sb.toString();
  }

  private static void generalize(StringBuilder sb, Type type) {
    if (type.getSort() == Type.OBJECT) {
      sb.append(type.getInternalName().startsWith("java.") ? type.getDescriptor() : "Ljava/lang/Object;");
    } else if (type.getSort() == Type.ARRAY) {
      if (type.getElementType().getInternalName().startsWith("java.")) {
        sb.append(type.getDescriptor());
      } else {
        for (int i = 0; i < type.getDimensions(); i++)
          sb.append("[");
        sb.append("Ljava/lang/Object;");
      }
    } else {
      sb.append(type.getDescriptor());
    }
  }

  public static boolean matchesParameters(Class<?>[] classes, String desc) {
    Type[] params = Type.getArgumentTypes(desc);
    if (classes.length != params.length)
      return false;
    for (int i = 0; i < classes.length; i++) {
      if (!classes[i].getName().equals(toClassName(params[i].getDescriptor()))) {
        return false;
      }
    }
    return true;
  }

  public static String toClassName(String descriptor) {
    Type t = Type.getType(descriptor);
    if (t.getSort() == Type.ARRAY) {
      // why java, why?!
      return descriptor.replace('/', '.');
    }
    return t.getClassName();
  }
}
