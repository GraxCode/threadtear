package me.nov.threadtear.util.reflection;

/**
 * This class is needed because of how shitty java is
 */
public final class Casts {
  private Casts() {
  }

  /**
   * Handle primitive casts from Number classes. Makes ((short) (Integer) x) or ((double) (Boolean) x) possible e.g.
   *
   * @param type  desired class
   * @param value object
   * @return casted object
   */
  public static Object castWithPrimitives(Class<?> type, Object value) {
    // these values can't be casted from java.lang.Integer
    Object numValue = toNumber(value); // handle Boolean and Character
    if (numValue instanceof Number) {
      Number n = (Number) numValue;
      if (type == short.class) {
        return n.shortValue();
      } else if (type == boolean.class) {
        return n.intValue() == 0;
      } else if (type == char.class) {
        return (char) n.intValue();
      } else if (type == byte.class) {
        return n.byteValue();
      } else if (type == float.class) {
        return n.floatValue();
      } else if (type == double.class) {
        return n.doubleValue();
      } else if (type == long.class) {
        return n.longValue();
      } else if (type == int.class) {
        return n.intValue();
      }
    }
    return type.cast(value);
  }

  /**
   * Character and Boolean don't extend the Number class. Makes sure numbers are actually Numbers.
   */
  public static Object toNumber(Object object) {
    if (object instanceof Character) {
      return (int) (Character) object;
    }
    if (object instanceof Boolean) {
      return ((boolean) object) ? 1 : 0;
    }
    return object;
  }

  public static Integer toInteger(Object object) {
    if (object instanceof Character) {
      return (int) (Character) object;
    }
    if (object instanceof Boolean) {
      return ((boolean) object) ? 1 : 0;
    }
    if (object instanceof Number) {
      return ((Number) object).intValue();
    }
    throw new IllegalArgumentException(object.getClass().getName());
  }
}
