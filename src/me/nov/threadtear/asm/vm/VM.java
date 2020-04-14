package me.nov.threadtear.asm.vm;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.HashMap;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import me.nov.threadtear.asm.io.Conversion;
import me.nov.threadtear.asm.util.Access;

public class VM extends ClassLoader implements Opcodes {
	HashMap<String, Class<?>> loaded = new HashMap<>();

	public static final String RT = "(com\\.oracle\\.|com\\.sun\\.|java\\.|javax\\.|jdk\\.|sun\\.).*";
	private IVMReferenceHandler handler;

	private VM(IVMReferenceHandler handler, ClassLoader parent) {
		super(parent);
		this.handler = handler;
	}

	public Class<?> bytesToClass(String name, byte[] bytes) {
		if (loaded.containsKey(name))
			throw new RuntimeException();
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

	@Override
	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (name.matches(RT)) {
			return super.loadClass(name, resolve);
		}
		if (loaded.containsKey(name)) {
			return loaded.get(name);
		}
		byte[] clazz = convert(handler.tryClassLoad(name.replace('.', '/')));
		if (clazz == null) {
			return null;
		}
		Class<?> loadedClass = bytesToClass(name, clazz);
		loaded.put(name, loadedClass);
		return loadedClass;
	}

	private byte[] convert(ClassNode node) {
		if (node == null)
			return null;
		node.methods.forEach(m -> m.access = Access.makePublic(m.access));
		node.fields.forEach(f -> f.access = Access.makePublic(f.access));
		node.access = Access.makePublic(node.access);
		byte[] bytes = Conversion.toBytecode0(node);
		return bytes;
	}

	public static VM constructVM(IVMReferenceHandler ivm) {
		return new VM(ivm, ClassLoader.getSystemClassLoader());
	}
}