package me.nov.threadtear.util.reflection;

public final class Primitives {
  private Primitives() {
  }

  public static Class<?> objectiveClass(Class<?> klass) {
    if (klass.isPrimitive()) {
      if (klass == char.class)
        return Character.class;
      if (klass == int.class)
        return Integer.class;
      if (klass == boolean.class)
        return Boolean.class;
      if (klass == byte.class)
        return Byte.class;
      if (klass == double.class)
        return Double.class;
      if (klass == float.class)
        return Float.class;
      if (klass == long.class)
        return Long.class;
      if (klass == short.class)
        return Short.class;
    } else {
      throw new IllegalArgumentException(klass.getName());
    }
    return klass;
  }

  public static Class<?> primitiveClass(Class<?> klass) {
    if (klass == Character.class)
      return char.class;
    if (klass == Integer.class)
      return int.class;
    if (klass == Boolean.class)
      return boolean.class;
    if (klass == Byte.class)
      return byte.class;
    if (klass == Double.class)
      return double.class;
    if (klass == Float.class)
      return float.class;
    if (klass == Long.class)
      return long.class;
    if (klass == Short.class)
      return short.class;

    throw new IllegalArgumentException(klass.getName());
  }
}
