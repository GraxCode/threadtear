package me.nov.threadtear.asm.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.tree.ClassNode;

import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.util.Manifest;

public class JarIO {

	public static ArrayList<Clazz> loadClasses(File jarFile) throws IOException {
		ArrayList<Clazz> classes = new ArrayList<Clazz>();
		JarFile jar = new JarFile(jarFile);
		Stream<JarEntry> str = jar.stream();
		str.forEach(z -> readEntry(jar, z, classes));
		jar.close();
		return classes;
	}

	private static ArrayList<Clazz> readEntry(JarFile jar, JarEntry en, ArrayList<Clazz> classes) {
		String name = en.getName();
		try (InputStream jis = jar.getInputStream(en)) {
			if (name.endsWith(".class")) {
				byte[] bytes = IOUtils.toByteArray(jis);
				if (String.format("%02X%02X%02X%02X", bytes[0], bytes[1], bytes[2], bytes[3]).equals("CAFEBABE")) {
					try {
						final ClassNode cn = Conversion.toNode(bytes);
						if (cn != null && (cn.superName != null || cn.name.equals("java/lang/Object"))) {
							classes.add(new Clazz(cn, en));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return classes;
	}

	public static void saveAsJar(File original, File output, ArrayList<Clazz> classes, boolean noSign) {
		try {
			JarOutputStream out = new JarOutputStream(new FileOutputStream(output));
			JarFile jar = new JarFile(original);
			Stream<JarEntry> str = jar.stream();
			str.forEach(z -> {
				try {
					String name = z.getName();
					if (name.endsWith(".class")) {
						Clazz clazz = classes.stream().filter(c -> c.oldEntry.getName().equals(name)).findFirst().orElse(null);
						if (clazz != null) {
							out.putNextEntry(cloneOldEntry(clazz.oldEntry, clazz.node.name + ".class"));
							out.write(Conversion.toBytecode(clazz.node, true));
							out.closeEntry();
							return;
						}
					}
					if(name.startsWith("META-INF/") && noSign) {
						if(name.startsWith("META-INF/CERT.")) {
							//export no certificates
							return;
						}
						if(name.equals("META-INF/MANIFEST.MF")) {
							out.putNextEntry(cloneOldEntry(z, z.getName()));
							out.write(Manifest.patchManifest(IOUtils.toByteArray(jar.getInputStream(z))));
							out.closeEntry();
							return;
						}
					}
					out.putNextEntry(cloneOldEntry(z, z.getName()));
					out.write(IOUtils.toByteArray(jar.getInputStream(z)));
					out.closeEntry();
				} catch (Exception e) {
					throw new RuntimeException("Failed at entry " + z.getName(), e);
				}
			});
			jar.close();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static JarEntry cloneOldEntry(JarEntry old, String name) {
		JarEntry entry = new JarEntry(name);
		// entry.setCreationTime(old.getCreationTime());
		entry.setExtra(old.getExtra());
		entry.setComment(old.getComment());
		// entry.setLastAccessTime(old.getLastAccessTime());
		// entry.setLastModifiedTime(old.getLastModifiedTime());
		return entry;
	}
}
