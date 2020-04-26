package me.nov.threadtear.io;

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

import me.nov.threadtear.Threadtear;

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
			byte[] bytes = IOUtils.toByteArray(jis);
			if (isClassFile(bytes)) {
				try {
					final ClassNode cn = Conversion.toNode(bytes);
					if (cn != null && (cn.superName != null || (cn.name != null && cn.name.equals("java/lang/Object")))) {
						classes.add(new Clazz(cn, en));
					}
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("Failed to load file " + name);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return classes;
	}

	public static void saveAsJar(File original, File output, ArrayList<Clazz> classes, boolean noSignature, boolean watermark) {
		try {
			JarOutputStream out = new JarOutputStream(new FileOutputStream(output));
			JarFile jar = new JarFile(original);
			Stream<JarEntry> str = jar.stream();
			str.forEach(z -> {
				try {
					if (classes.stream().anyMatch(c -> c.oldEntry.getName().equals(z.getName()))) {
						// ignore old class files
						return;
					}
					String name = z.getName();
					if (noSignature && name.startsWith("META-INF/")) {
						if (name.startsWith("META-INF/CERT.")) {
							// export no certificates
							return;
						}
					}
					if (name.equals("META-INF/MANIFEST.MF")) {
						byte[] manifest = IOUtils.toByteArray(jar.getInputStream(z));
						if (noSignature) {
							manifest = Manifest.patchManifest(manifest);
						}
						if (watermark) {
							manifest = Manifest.watermark(manifest);
						}
						out.putNextEntry(cloneOldEntry(z, z.getName()));
						out.write(manifest);
						out.closeEntry();
						return;
					}
					// export resources
					out.putNextEntry(cloneOldEntry(z, z.getName()));
					out.write(IOUtils.toByteArray(jar.getInputStream(z)));
					out.closeEntry();
				} catch (Exception e) {
					Threadtear.logger.severe("Failed at entry " + z.getName() + " " + e.getClass().getName() + " " + e.getMessage());
				}
			});
			for (Clazz c : classes) {
				try {
					// add updated classes
					out.putNextEntry(cloneOldEntry(c.oldEntry, c.node.name + ".class"));
					out.write(Conversion.toBytecode(c.node, true));
					out.closeEntry();
				} catch (Exception e) {
					Threadtear.logger.severe("Failed at class entry " + c.node.name + " " + e.getClass().getName() + " " + e.getMessage());
				}
			}
			jar.close();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean isClassFile(byte[] bytes) {
		return bytes.length >= 4 && String.format("%02X%02X%02X%02X", bytes[0], bytes[1], bytes[2], bytes[3]).equals("CAFEBABE");
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
