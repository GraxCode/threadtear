package me.nov.threadtear.analysis.stack;

import me.nov.threadtear.logging.LogWrapper;
import me.nov.threadtear.util.asm.Descriptor;
import me.nov.threadtear.util.reflection.Casts;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

/**
 * A basic IConstantReferenceHandler that handles simple calls like Integer.parseInt or Float.valueOf
 */
public class BasicReferenceHandler implements IConstantReferenceHandler {

  @Override
  public Object getFieldValueOrNull(BasicValue v, String owner, String name, String desc) {
    return null;
  }

  protected static final List<String> PERMITTED_SIMULATION = Arrays.asList("java/lang/Integer", "java/lang/Long",
    "java/lang/Short", "java/lang/Byte", "java/lang/Boolean", "java/lang/String", "java/lang/Float", "java/lang/Double",
    "java/lang/StringBuilder", "java/lang/StringBuffer");

  @Override
  public Object getMethodReturnOrNull(BasicValue v, String owner, String name, String desc,
                                      List<? extends ConstantValue> values) {
    if (PERMITTED_SIMULATION.contains(owner)) {
      if (!values.stream().allMatch(ConstantValue::isKnown)) {
        return null;
      }
      try {
        Method method = Arrays.stream(Class.forName(owner.replace('/', '.')).getDeclaredMethods())
          .filter(m -> m.getName().equals(name) && Descriptor.matchesParameters(m.getParameterTypes(), desc))
          .findFirst().orElse(null);
        if (method != null) {
          if (Modifier.isStatic(method.getModifiers())) {
            return method.invoke(null, toExactObjects(method.getParameterTypes(), values));
          } else {
            return method.invoke(values.get(0).getValue(), toExactObjects(method.getParameterTypes(),
              values.subList(1, values.size())));
          }
        }
      } catch (InvocationTargetException e) {
        // ignore invocation exceptions
      } catch (Throwable t) {
        LogWrapper.logger.error("Couldn't bridge reference to runtime method {}.{}", t, owner, name);
      }
    }
    return null;
  }

  /**
   * Make sure int types are converted to match the parameters
   */
  protected Object[] toExactObjects(Class<?>[] parameterTypes, List<? extends ConstantValue> values) {
    Object[] params = new Object[parameterTypes.length];
    for (int i = 0; i < parameterTypes.length; i++) {
      Class<?> param = parameterTypes[i];
      Object value = values.get(i).getValue();

      params[i] = Casts.castWithPrimitives(param, value);
    }
    return params;
  }
}
