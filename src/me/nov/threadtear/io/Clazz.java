package me.nov.threadtear.io;

import java.io.*;
import java.util.jar.*;

import org.objectweb.asm.tree.ClassNode;

public class Clazz {

  public boolean transform = true;
  public final ClassNode node;
  public final JarEntry oldEntry;
  public final Object inputFile;

  public Clazz(ClassNode node, JarEntry oldEntry, Object inputFile) {
    super();
    this.node = node;
    this.oldEntry = oldEntry;
    this.inputFile = inputFile;
  }

  public InputStream streamOriginal() throws IOException {
    if (inputFile instanceof JarFile) {
      JarFile jf = new JarFile(((JarFile) inputFile).getName());
      return jf.getInputStream(jf.getEntry(oldEntry.getName()));
    }
    return new FileInputStream((File) inputFile);
  }
}
