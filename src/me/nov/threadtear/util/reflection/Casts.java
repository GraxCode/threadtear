package me.nov.threadtear.util.reflection;

/**
 * This class is needed because of how shitty java is
 */
public class Casts {
  public static Object castWithPrimitives(Class<?> type, Object value) {
    // these values can't be casted from java.lang.Integer
    if (type == short.class) {
      return ((Integer) value).shortValue();
    } else if (type == boolean.class) {
      return ((Integer) value).intValue() == 0 ? true : false;
    } else if (type == char.class) {
      return (char) ((Integer) value).intValue();
    } else if (type == byte.class) {
      return ((Integer) value).byteValue();
    } else if (type == float.class) {
      return ((Integer) value).floatValue();
    } else if (type == double.class) {
      return ((Integer) value).doubleValue();
    } else if (type == long.class) {
      return ((Integer) value).longValue();
    } else if (type == int.class) {
      return ((Integer) value).intValue();
    }
    return type.cast(value);
  }

  /**
   * Character and Boolean don't extend Number, fuck you Java!
   */
  public static Object toNumber(Object object) {
    if (object instanceof Character) {
      return (int) ((Character) object).charValue();
    }
    if (object instanceof Boolean) {
      return ((Boolean) object) ? 1 : 0;
    }

    return object;
  }

  public static Integer toInteger(Object object) {
    if (object instanceof Character) {
      return (int) ((Character) object).charValue();
    }
    if (object instanceof Boolean) {
      return ((Boolean) object) ? 1 : 0;
    }
    if (object instanceof Number) {
      return ((Number) object).intValue();
    }
    throw new IllegalArgumentException(object.getClass().getName());
  }
}
