package me.nov.threadtear.vm;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.function.BiPredicate;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import me.nov.threadtear.Threadtear;
import me.nov.threadtear.io.Conversion;
import me.nov.threadtear.util.asm.*;

public class VM extends ClassLoader implements Opcodes {
  public HashMap<String, Class<?>> loaded = new HashMap<>();

  public static final String RT = "(com\\.oracle\\.|com\\.sun\\.|java\\.|javax\\.|jdk\\.|sun\\.).*";
  private IVMReferenceHandler handler;

  public boolean noInitialization;

  private VM(IVMReferenceHandler handler, ClassLoader parent, boolean clinit) {
    super(parent);
    this.handler = handler;
    this.noInitialization = clinit;
  }

  public Class<?> bytesToClass(String name, byte[] bytes) {
    if (loaded.containsKey(name))
      throw new RuntimeException("class " + name + " is already defined");
    if (isForbiddenName(name))
      throw new RuntimeException(name + " is not an allowed class name");
    try {
      Method define = ClassLoader.class.getDeclaredMethod("defineClass0", String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
      define.setAccessible(true);
      Class<?> c = (Class<?>) define.invoke(this, name, bytes, 0, bytes.length, null);
      resolveClass(c);
      return c;
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      Class<?> c = defineClass(name, bytes, 0, bytes.length);
      resolveClass(c);
      return c;
    } catch (Throwable t) {
      t.printStackTrace();
      return null;
    }
  }

  private boolean isForbiddenName(String name) {
    return name.startsWith(Threadtear.class.getPackage().getName()) || name.matches(RT);
  }

  @Override
  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    if (name.matches(RT)) {
      return super.loadClass(name, resolve);
    }
    if (name.contains("/"))
      throw new IllegalArgumentException();
    if (loaded.containsKey(name)) {
      return loaded.get(name);
    }
    byte[] clazz = convert(handler.tryClassLoad(name.replace('.', '/')), noInitialization, null);
    if (clazz == null) {
      return null;
    }
    Class<?> loadedClass = bytesToClass(name, clazz);
    loaded.put(name, loadedClass);
    return loadedClass;
  }

  private byte[] convert(ClassNode node, boolean noInitialization, BiPredicate<String, String> removalPredicate) {
    if (node == null)
      return null;
    ClassNode vmnode = Conversion.toNode(Conversion.toBytecode0(node)); // clone ClassNode the easy way
    vmnode.methods.forEach(m -> m.access = fixAccess(m.access));
    vmnode.fields.forEach(f -> f.access = fixAccess(f.access));
    vmnode.access = fixAccess(node.access);
    if (noInitialization) {
      vmnode.methods.stream().filter(m -> m.name.equals("<clinit>")).forEach(m -> {
        m.instructions.clear();
        m.instructions.add(new InsnNode(RETURN));
        m.tryCatchBlocks.clear();
        m.localVariables = null;
      });
      vmnode.superName = "java/lang/Object";
      vmnode.interfaces = new ArrayList<>();
    }
    if (removalPredicate != null) {
      vmnode.methods.forEach(m -> Instructions.isolateCallsThatMatch(m, removalPredicate, removalPredicate));
    }
    return Conversion.toBytecode0(vmnode);
  }

  private int fixAccess(int access) {
    if (Access.isStatic(access)) {
      return ACC_PUBLIC | ACC_STATIC;
    }
    return ACC_PUBLIC;
  }

  public void explicitlyPreloadWithClinit(ClassNode node) {
    String name = node.name.replace('/', '.');
    byte[] clazz = convert(node, false, null);
    Class<?> loadedClass = bytesToClass(name, clazz);
    loaded.put(name, loadedClass);
  }

  public void explicitlyPreloadNoClinit(ClassNode node) {
    String name = node.name.replace('/', '.');
    byte[] clazz = convert(node, true, null);
    Class<?> loadedClass = bytesToClass(name, clazz);
    loaded.put(name, loadedClass);
  }

  public void explicitlyPreloadNoClinitAndIsolate(ClassNode node, BiPredicate<String, String> p) {
    String name = node.name.replace('/', '.');
    byte[] clazz = convert(node, true, p);
    Class<?> loadedClass = bytesToClass(name, clazz);
    loaded.put(name, loadedClass);
  }

  public boolean isLoaded(String name) {
    if (name.contains("/"))
      throw new IllegalArgumentException();
    return loaded.containsKey(name);
  }

  public static VM constructVM(IVMReferenceHandler ivm) {
    return new VM(ivm, ClassLoader.getSystemClassLoader(), false);
  }

  public static VM constructNonInitializingVM(IVMReferenceHandler ivm) {
    return new VM(ivm, ClassLoader.getSystemClassLoader(), true);
  }
}