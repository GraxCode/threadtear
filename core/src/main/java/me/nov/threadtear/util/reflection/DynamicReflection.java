package me.nov.threadtear.util.reflection;

import me.nov.threadtear.logging.LogWrapper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class DynamicReflection implements Opcodes {
  private DynamicReflection() {
  }

  private static final String BMHL = "java.lang.invoke.BoundMethodHandle$Species_L";

  /**
   * This probably only works for java 8
   */
  public static MethodHandleInfo revealMethodInfo(MethodHandle handle)
          throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
    Class<? extends MethodHandle> clazz = handle.getClass();
    if (clazz.getName().startsWith(BMHL)) {
      Field original = clazz.getDeclaredField("argL0");
      original.setAccessible(true);
      handle = (MethodHandle) original.get(handle);
    } else if (clazz.getName().contains("BruteArgumentMoverHandle")) {
      LogWrapper.logger.warning("Wrong java version! Please use Java 8 to decrypt MethodHandles properly.");
    }
    return revealTrusted(handle);
  }

  public static MethodHandleInfo revealTrusted(MethodHandle handle)
          throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
    return getTrustedLookup().revealDirect(handle);
  }

  public static MethodHandles.Lookup getTrustedLookup()
          throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
    Field impl = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
    impl.setAccessible(true);
    return (Lookup) impl.get(null);
  }

  public static AbstractInsnNode getInstructionFromHandleInfo(MethodHandleInfo direct) throws Exception {
    Class<?> declaringClass = direct.getDeclaringClass();
    String name = direct.getName();
    MethodType methodType = direct.getMethodType();
    int refKind = direct.getReferenceKind();
    int op = bootstrapTagToOp(refKind);
    if (refKind <= H_PUTSTATIC) {
      if (refKind <= H_GETSTATIC) {
        // method handle treats field retrieving as a
        // method ()X
        return new FieldInsnNode(op, declaringClass.getName().replace('.', '/'), name,
                methodType.toMethodDescriptorString().substring(2));
      } else {
        // method handle treats field putting as a method
        // (returning void) -> (X)V
        String mds = methodType.toMethodDescriptorString();
        return new FieldInsnNode(op, declaringClass.getName().replace('.', '/'), name,
                mds.substring(1, mds.lastIndexOf(')')));
      }
    } else {
      return new MethodInsnNode(op, declaringClass.getName().replace('.', '/'), name,
              methodType.toMethodDescriptorString());
    }
  }

  public static int bootstrapTagToOp(int tag) {
    switch (tag) {
      case H_GETFIELD:
        return GETFIELD;
      case H_GETSTATIC:
        return GETSTATIC;
      case H_PUTFIELD:
        return PUTFIELD;
      case H_PUTSTATIC:
        return PUTSTATIC;
      case H_INVOKEVIRTUAL:
        return INVOKEVIRTUAL;
      case H_INVOKESTATIC:
        return INVOKESTATIC;
      case H_INVOKESPECIAL:
      case H_NEWINVOKESPECIAL:
        return INVOKESPECIAL;
      case H_INVOKEINTERFACE:
        return INVOKEINTERFACE;
    }
    throw new IllegalArgumentException("not a bootstrap tag: " + tag);
  }

  @Deprecated
  public static void setFinalStatic(Field field, Object newValue) throws Exception {
    field.setAccessible(true);

    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

    field.set(null, newValue);
  }
}
