package me.nov.threadtear.io;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.stream.Stream;
import java.util.zip.ZipException;

import me.nov.threadtear.logging.LogWrapper;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.tree.ClassNode;

import me.nov.threadtear.execution.Clazz;

public final class JarIO {
  private JarIO() {
  }

  public static ArrayList<Clazz> loadClasses(File jarFile) throws IOException {
    ArrayList<Clazz> classes = new ArrayList<>();
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
            classes.add(new Clazz(cn, en, jar));
          }
        } catch (Exception e) {
          LogWrapper.logger.error("Failed to load file {}", e, name);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return classes;
  }

  public static final String CERT_REGEX = "META-INF/.+(\\.SF|\\.RSA|\\.DSA)";

  public static void saveAsJar(File original, File output, List<Clazz> classes, boolean noSignature,
                               boolean watermark) {
    try {
      JarOutputStream out = new JarOutputStream(new FileOutputStream(output));
      Rewriting:
      {
        JarFile jar;
        try {
          jar = new JarFile(original);
        } catch (ZipException e) {
          // not a zip file, has to be a class
          break Rewriting;
        }
        Stream<JarEntry> str = jar.stream();
        str.forEach(z -> {
          try {
            if (classes.stream().anyMatch(c -> c.oldEntry.getName().equals(z.getName()))) {
              // ignore old class files
              return;
            }
            String name = z.getName();
            if (noSignature && name.matches(CERT_REGEX)) {
              // export no certificates
              return;
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
            LogWrapper.logger
                    .error("Failed at entry " + z.getName() + " " + e.getClass().getName() + " " + e.getMessage());
          }
        });
        jar.close();
      }
      for (Clazz c : classes) {
        try {
          // add updated classes
          out.putNextEntry(cloneOldEntry(c.oldEntry, c.node.name + ".class"));
          out.write(Conversion.toBytecode0(c.node));
          out.closeEntry();
        } catch (Exception e) {
          LogWrapper.logger
                  .error("Failed at class entry " + c.node.name + " " + e.getClass().getName() + " " + e.getMessage());
        }
      }
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static File writeTempJar(String name, byte[] clazz) {
    try {
      File temp = File.createTempFile("temp-jar", ".jar");
      JarOutputStream out = new JarOutputStream(new FileOutputStream(temp));
      out.putNextEntry(new JarEntry(name + ".class"));
      out.write(clazz);
      out.closeEntry();
      out.close();
      return temp;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean isClassFile(byte[] bytes) {
    return bytes.length >= 4 &&
            String.format("%02X%02X%02X%02X", bytes[0], bytes[1], bytes[2], bytes[3]).equals("CAFEBABE");
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
