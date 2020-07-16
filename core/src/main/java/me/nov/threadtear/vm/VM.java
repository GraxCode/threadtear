package me.nov.threadtear.vm;

import me.nov.threadtear.ThreadtearCore;
import me.nov.threadtear.io.Conversion;
import me.nov.threadtear.logging.LogWrapper;
import me.nov.threadtear.util.asm.Access;
import me.nov.threadtear.util.asm.Instructions;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

public class VM extends ClassLoader implements Opcodes {
  public static final String RT_REGEX = "((?:com\\.(?:oracle|sun)|j(?:avax?|dk)|sun)\\.).*";
  public static final String threadtearPkg = ThreadtearCore.class.getPackage().getName();
  public Map<String, Class<?>> loaded = new HashMap<>();
  public boolean noInitialization;
  private IVMReferenceHandler handler;
  private boolean dummyLoading;

  private VM(IVMReferenceHandler handler, ClassLoader parent, boolean clinit) {
    super(parent);
    this.handler = handler;
    this.noInitialization = clinit;
    this.dummyLoading = false;
  }

  public static VM constructVM(IVMReferenceHandler ivm) {
    return new VM(ivm, ClassLoader.getSystemClassLoader(), false);
  }

  public static VM constructNonInitializingVM(IVMReferenceHandler ivm) {
    return new VM(ivm, ClassLoader.getSystemClassLoader(), true);
  }

  private Class<?> bytesToClass(String name, byte[] bytes) {
    if (loaded.containsKey(name))
      throw new RuntimeException("class " + name + " is already defined");
    if (isForbiddenName(name))
      throw new RuntimeException(name + " is not an allowed class name");
    try {
      Method define = ClassLoader.class
        .getDeclaredMethod("defineClass0", String.class, byte[].class, int.class, int.class,
          ProtectionDomain.class);
      define.setAccessible(true);
      Class<?> c = (Class<?>) define.invoke(this, name, bytes, 0, bytes.length, null);
      resolveClass(c);
      return c;
    } catch (Exception e) {
    }
    try {
      Class<?> c = defineClass(name, bytes, 0, bytes.length);
      resolveClass(c);
      return c;
    } catch (Throwable t) {
      LogWrapper.logger.error("Failed to resolve class using defineClass", t);
      return null;
    }
  }

  private boolean isForbiddenName(String name) {
    return name.startsWith(threadtearPkg) || name.matches(RT_REGEX);
  }

  @Override
  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    if (name.contains("/"))
      throw new IllegalArgumentException();
    if (name.startsWith(threadtearPkg)) {
      LogWrapper.logger.warning("Dynamic class tried to access a threadtear package!");
      return null;
    }
    if (loaded.containsKey(name)) {
      return loaded.get(name);
    }
    if (name.matches(RT_REGEX)) {
      return super.loadClass(name, resolve);
    }
    // unloaded class, convert ClassNode to bytes
    byte[] clazz = convert(name, handler.tryClassLoad(name.replace('.', '/')), noInitialization, null);
    if (clazz == null) {
      return null;
    }
    Class<?> loadedClass = bytesToClass(name, clazz);
    loaded.put(name, loadedClass);
    return loadedClass;
  }

  private byte[] convert(String name, ClassNode node, boolean noInitialization,
                         BiPredicate<String, String> removalPredicate) {
    if (node == null) {
      if (dummyLoading) {
        ClassNode dummy = new ClassNode();
        dummy.name = name.replace('.', '/');
        dummy.superName = "java/lang/Object";
        dummy.version = 52;
        return Conversion.toBytecode0(dummy);
      }
      return null;
    }
    ClassNode vmnode = Conversion.toNode(Conversion.toBytecode0(node)); // clone ClassNode the
    // easy way
    vmnode.methods.forEach(m -> m.access = fixAccess(m.access));
    vmnode.fields.forEach(f -> f.access = fixAccess(f.access));
    vmnode.access = fixAccess(node.access);
    if (noInitialization) {
      vmnode.methods.stream().filter(m -> m.name.equals("<clinit>")).findFirst().ifPresent(m -> {
        m.instructions.clear();
        m.instructions.add(new InsnNode(RETURN));
        m.tryCatchBlocks.clear();
        m.localVariables = null;
      });
      if (Access.isEnum(vmnode.access))
        vmnode.superName = "java/lang/Enum";
      else
        vmnode.superName = "java/lang/Object";
      vmnode.interfaces = new ArrayList<>();
    }
    if (removalPredicate != null) {
      vmnode.methods.forEach(m -> Instructions.isolateCallsThatMatch(m, removalPredicate, removalPredicate));
    }
    return Conversion.toBytecode0(vmnode);
  }

  private int fixAccess(int access) {
    int newAccess = ACC_PUBLIC;
    if (Access.isStatic(access)) {
      newAccess |= ACC_STATIC;
    }
    if (Access.isInterface(access)) {
      newAccess |= ACC_INTERFACE;
    }
    if (Access.isEnum(access)) {
      newAccess |= ACC_ENUM;
    }
    if (Access.isAbstract(access)) {
      newAccess |= ACC_ABSTRACT;
    }
    return newAccess;
  }

  public void explicitlyPreload(ClassNode node) {
    this.explicitlyPreload(node, false);
  }

  public void explicitlyPreload(ClassNode node, boolean removeClinit) {
    this.explicitlyPreload(node, removeClinit, null);
  }

  public void explicitlyPreload(ClassNode node, boolean removeClinit, BiPredicate<String, String> p) {
    String name = node.name.replace('/', '.');
    byte[] clazz = convert(name, node, removeClinit, p);
    Class<?> loadedClass = bytesToClass(name, clazz);
    loaded.put(name, loadedClass);
  }

  public boolean isLoaded(String name) {
    if (name.contains("/"))
      throw new IllegalArgumentException();
    return loaded.containsKey(name);
  }

  public void setDummyLoading(boolean dummyLoad) {
    this.dummyLoading = dummyLoad;
  }
}
