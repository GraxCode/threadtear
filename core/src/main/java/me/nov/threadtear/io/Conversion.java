package me.nov.threadtear.io;

import java.io.*;
import java.nio.file.Files;

import me.nov.threadtear.logging.LogWrapper;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

public final class Conversion {
  private Conversion() {
  }

  public static byte[] toBytecode(ClassNode cn, boolean useMaxs) {
    try {
      ClassWriter cw = new ClassWriter(useMaxs ? ClassWriter.COMPUTE_MAXS : ClassWriter.COMPUTE_FRAMES);
      cn.accept(cw);
      return cw.toByteArray();
    } catch (Exception e) {
      return toBytecode0(cn);
    }
  }

  public static byte[] toBytecode0(ClassNode cn) {
    ClassWriter cw = new ClassWriter(0);
    cn.accept(cw);
    return cw.toByteArray();
  }

  public static ClassNode toNode(final byte[] bytez) {
    ClassReader cr = new ClassReader(bytez);
    ClassNode cn = new ClassNode();
    try {
      cr.accept(cn, ClassReader.EXPAND_FRAMES);
    } catch (Exception e) {
      try {
        cr.accept(cn, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
      } catch (Exception e2) {
        e2.printStackTrace();
        LogWrapper.logger.error("Failed to load class ", e2);
      }
    }
    return cn;
  }

  public static String textify(ClassNode cn) {
    try {
      StringWriter out = new StringWriter();
      new ClassReader(toBytecode0(cn)).accept(new TraceClassVisitor(new PrintWriter(out)), ClassReader.SKIP_DEBUG);
      return out.toString();
    } catch (Throwable t) {
      t.printStackTrace();
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      t.printStackTrace(pw);
      return sw.toString();
    }
  }

  public static void saveDebugFile(ClassNode cn) {
    try {
      Files.write(new File(cn.name.replace('/', '.') + "-debug.class").toPath(), Conversion.toBytecode0(cn));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
