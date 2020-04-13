package me.nov.threadtear.asm.util;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.HashMap;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

public class Sandbox implements Opcodes {

	public static Object compute(InsnList code) {
		ClassNode cn = createClassProxy();
		MethodNode mn = createMethodProxy(code);
		cn.methods.add(mn);
		try {
			Class<?> clazz = load(cn);
			return clazz.getDeclaredMethods()[0].invoke(null);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}

	private static MethodNode createMethodProxy(InsnList code) {
		MethodNode proxy = new MethodNode(ACC_PUBLIC | ACC_STATIC, "proxyMethod" + System.currentTimeMillis() % 10000,
				"()Ljava/lang/Object;", null, null);
		proxy.instructions.add(code);
		proxy.maxStack = 10;
		proxy.maxLocals = 20;
		return proxy;
	}

	private static ClassNode createClassProxy() {
		ClassNode proxy = new ClassNode();
		proxy.access = ACC_PUBLIC;
		proxy.version = 52;
		proxy.name = "ProxyClass" + System.currentTimeMillis() % 10000;
		proxy.superName = "java/lang/Object";
		return null;
	}

	public static Class<?> load(ClassNode cn) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		return new ClassDefiner(ClassLoader.getSystemClassLoader()).get(cn.name.replace("/", "."), cw.toByteArray());
	}

	public static class ClassDefiner extends ClassLoader {
		HashMap<String, Class<?>> loaded = new HashMap<>();
		HashMap<String, byte[]> local = new HashMap<>();

		public ClassDefiner(ClassLoader parent) {
			super(parent);
		}

		public void predefine(String name, byte[] bytes) {
			if (!local.containsKey(name))
				local.put(name, bytes);
		}

		public Class<?> get(String name, byte[] bytes) {
			if (loaded.containsKey(name))
				throw new RuntimeException();
			try {
				Method define = ClassLoader.class.getDeclaredMethod("defineClass0", String.class, byte[].class, int.class,
						int.class, ProtectionDomain.class);
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
			if (loaded.containsKey(name)) {
				return loaded.get(name);
			}
			if (local.containsKey(name)) {
				return get(name, local.remove(name));
			}
			return super.loadClass(name, resolve);
		}

	}
}
