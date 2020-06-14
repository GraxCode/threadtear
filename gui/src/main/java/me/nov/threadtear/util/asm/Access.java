package me.nov.threadtear.util.asm;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author <a href="https://github.com/notaudrey/">Audrey</a>
 * @since 4/30/15
 */
public final class Access {
  private Access() {
  }

  /**
   * Tells whether a given access modifier is public
   *
   * @param mod The access modifier to check
   * @return True if the access modifier is public, false
   * otherwise
   */
  public static boolean isPublic(int mod) {
    return (mod & ACC_PUBLIC) != 0;
  }

  /**
   * Tells whether a given access modifier is protected
   *
   * @param mod The access modifier to check
   * @return True if the access modifier is protected,
   * false otherwise
   */
  public static boolean isProtected(int mod) {
    return (mod & ACC_PROTECTED) != 0;
  }

  /**
   * Tells whether a given access modifier is private
   *
   * @param mod The access modifier to check
   * @return True if the access modifier is private,
   * false otherwise
   */
  public static boolean isPrivate(int mod) {
    return (mod & ACC_PRIVATE) != 0;
  }

  /**
   * Tells whether a given access modifier is static
   *
   * @param mod The access modifier to check
   * @return True if the access modifier is static, false
   * otherwise
   */
  public static boolean isStatic(int mod) {
    return (mod & ACC_STATIC) != 0;
  }

  /**
   * Tells whether a given access modifier is native
   *
   * @param mod The access modifier to check
   * @return True if the access modifier is native, false
   * otherwise
   */
  public static boolean isNative(int mod) {
    return (mod & ACC_NATIVE) != 0;
  }

  /**
   * Tells whether a given access modifier is abstract
   *
   * @param mod The access modifier to check
   * @return True if the access modifier is abstract,
   * false otherwise
   */
  public static boolean isAbstract(int mod) {
    return (mod & ACC_ABSTRACT) != 0;
  }

  /**
   * Tells whether a given access modifier is final
   *
   * @param mod The access modifier to check
   * @return True if the access modifier is final, false
   * otherwise
   */
  public static boolean isFinal(int mod) {
    return (mod & ACC_FINAL) != 0;
  }

  /**
   * Tells whether a given access modifier is synthetic.
   * A modifier is synthetic if it is marked with the
   * ACC_SYNTHETIC flag (0x1000), as specified in JLS8, 4
   * .6 <tt>Methods</tt>.
   *
   * @param mod The access modifier to check
   * @return True if the access modifier is synthetic,
   * false otherwise
   */
  public static boolean isSynthetic(int mod) {
    return (mod & ACC_SYNTHETIC) != 0;
  }

  /**
   * Tells whether a given access modifier is volatile
   *
   * @param mod The access modifier to check
   * @return True if the access modifier is volatile,
   * false otherwise
   */
  public static boolean isVolatile(int mod) {
    return (mod & ACC_VOLATILE) != 0;
  }

  /**
   * Tells whether a given access modifier is bridge. A
   * modifier is bridge if it is marked with the
   * ACC_BRIDGE flag (0x0040), as specified in JLS8, 4.6
   * <tt>Methods</tt>.
   *
   * @param mod The access modifier to check
   * @return True if the access modifier is bridge, false
   * otherwise
   */
  public static boolean isBridge(int mod) {
    return (mod & ACC_BRIDGE) != 0;
  }

  /**
   * Tells whether a given access modifier is
   * synchronized. A modifier is synchronized if it is
   * marked with the ACC_SYNCHRONIZED flag (0x0020), as
   * specified in JLS8, 4.6
   * <tt>Methods</tt> and JLS8, 2.11.10
   * <tt>Synchronization</tt>
   *
   * @param mod The access modifier to check
   * @return True if the access modifier is synchronized,
   * false otherwise
   */
  public static boolean isSynchronized(int mod) {
    return (mod & ACC_SYNCHRONIZED) != 0;
  }

  /**
   * Tells whether a given access modifier is interface
   *
   * @param mod The access modifier to check
   * @return True if the access modifier is interface,
   * false otherwise
   */
  public static boolean isInterface(int mod) {
    return (mod & ACC_INTERFACE) != 0;
  }

  /**
   * Tells whether a given access modifier is enum
   *
   * @param mod The access modifier to check
   * @return True if the access modifier is enum, false
   * otherwise
   */
  public static boolean isEnum(int mod) {
    return (mod & ACC_ENUM) != 0;
  }

  /**
   * Tells whether a given access modifier is annotation
   * (@interface)
   *
   * @param mod The access modifier to check
   * @return True if the access modifier is annotation,
   * false otherwise
   */
  public static boolean isAnnotation(int mod) {
    return (mod & ACC_ANNOTATION) != 0;
  }

  /**
   * Tells whether a given access modifier is deprecated
   *
   * @param mod The access modifier to check
   * @return True if the access modifier is deprecated,
   * false otherwise
   */
  public static boolean isDeprecated(int mod) {
    return (mod & ACC_DEPRECATED) != 0;
  }

  /**
   * Tells whether a given type is a void
   *
   * @param desc The type description to check
   * @return True if the type is a void, false otherwise
   */
  public static boolean isVoid(String desc) {
    return desc.endsWith("V");
  }

  /**
   * Tells whether a given type is a boolean
   *
   * @param desc The type description to check
   * @return True if the type is a boolean, false otherwise
   */
  public static boolean isBoolean(String desc) {
    return desc.endsWith("Z");
  }

  /**
   * Tells whether a given type is a char
   *
   * @param desc The type description to check
   * @return True if the type is a char, false otherwise
   */
  public static boolean isChar(String desc) {
    return desc.endsWith("C");
  }

  /**
   * Tells whether a given type is a byte
   *
   * @param desc The type description to check
   * @return True if the type is a byte, false otherwise
   */
  public static boolean isByte(String desc) {
    return desc.endsWith("B");
  }

  /**
   * Tells whether a given type is a short
   *
   * @param desc The type description to check
   * @return True if the type is a short, false otherwise
   */
  public static boolean isShort(String desc) {
    return desc.endsWith("S");
  }

  /**
   * Tells whether a given type is an int
   *
   * @param desc The type description to check
   * @return True if the type is an int, false otherwise
   */
  public static boolean isInt(String desc) {
    return desc.endsWith("I");
  }

  /**
   * Tells whether a given type is a float
   *
   * @param desc The type description to check
   * @return True if the type is a float, false otherwise
   */
  public static boolean isFloat(String desc) {
    return desc.endsWith("F");
  }

  /**
   * Tells whether a given type is a long
   *
   * @param desc The type description to check
   * @return True if the type is a long, false otherwise
   */
  public static boolean isLong(String desc) {
    return desc.endsWith("J");
  }

  /**
   * Tells whether a given type is a double
   *
   * @param desc The type description to check
   * @return True if the type is a double, false otherwise
   */
  public static boolean isDouble(String desc) {
    return desc.endsWith("D");
  }

  /**
   * Tells whether a given type is an array
   *
   * @param desc The type description to check
   * @return True if the type is an array, false otherwise
   */
  public static boolean isArray(String desc) {
    return desc.startsWith("[");
  }

  /**
   * Tells whether a given type is an Object
   *
   * @param desc The type description to check
   * @return True if the type is an Object, false otherwise
   */
  public static boolean isObject(String desc) {
    return desc.endsWith(";");
  }

  /**
   * Tells whether the given method signature is generic.
   * The method is considered generic if its signature
   * ends with something along the lines of ")TV;"
   *
   * @param desc The method signature to check
   * @return True if the method signature is generic,
   * false otherwise
   */
  public static boolean isMethodReturnTypeGeneric(String desc) {
    return desc.contains(")T");
  }

  /**
   * Tells whether the given field description+signature
   * is generic.
   *
   * @param desc      Description of the field
   * @param signature Signature of the field
   * @return True if the field is generic, false otherwise
   */
  public static boolean isFieldGeneric(String desc, String signature) {
    return signature != null && desc != null && signature.startsWith("T") && signature.endsWith(";") &&
            Character.isUpperCase(signature.charAt(1)) && desc.contains("java/lang/Object");
  }

  public static int removeAccess(int access, int... remove) {
    for (int r : remove) {
      access &= ~r;
    }
    return access;
  }

  public static int makePublic(int access) {
    if (Access.isPrivate(access)) {
      access = removeAccess(access, ACC_PRIVATE);
    }
    if (Access.isProtected(access)) {
      access = removeAccess(access, ACC_PROTECTED);
    }
    if (!Access.isPublic(access)) {
      access |= ACC_PUBLIC;
    }
    return access;
  }
}