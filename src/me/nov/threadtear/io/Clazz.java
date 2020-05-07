package me.nov.threadtear.io;

import java.io.*;
import java.util.jar.*;

import org.objectweb.asm.tree.ClassNode;

public class Clazz {

  public boolean transform = true;
  public final ClassNode node;
  public final JarEntry oldEntry;
  public final JarFile jar;

  public Clazz(ClassNode node, JarEntry oldEntry, JarFile jar) {
    super();
    this.node = node;
    this.oldEntry = oldEntry;
    this.jar = jar;
  }

  public InputStream streamOriginal() throws IOException {
    JarFile jf = new JarFile(jar.getName());
    return jf.getInputStream(jf.getEntry(oldEntry.getName()));
  }
}
