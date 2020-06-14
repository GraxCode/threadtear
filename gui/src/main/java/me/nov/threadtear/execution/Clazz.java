package me.nov.threadtear.execution;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.jar.*;
import java.util.stream.*;

import me.nov.threadtear.util.format.Strings;
import org.objectweb.asm.tree.ClassNode;

public class Clazz {

  public boolean transform = true;
  public final ClassNode node;
  public final JarEntry oldEntry;
  public final Object inputFile;
  public final ArrayList<String> failures = new ArrayList<>();

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

  public void addFail(Throwable t) {
    addFail(Stream.of(t.getStackTrace()).map(StackTraceElement::toString).limit(4).collect(Collectors.joining("<br>")));
  }

  public void addFail(String fail) {
    if (!failures.contains(fail))
      failures.add(fail);
  }

  private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy hh:mm");

  public String getMetadataString() {
    if (oldEntry != null) {
      return Strings.formatBytes(oldEntry.getSize()) + ", " + dateFormat.format(oldEntry.getTime());
    }
    return "";
  }
}
