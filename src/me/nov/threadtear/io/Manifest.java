package me.nov.threadtear.io;

import java.io.UnsupportedEncodingException;

import me.nov.threadtear.swing.Utils;

public class Manifest {
  private static String lineSeparator = "\r\n";

  public static byte[] patchManifest(byte[] manifestBytes) throws UnsupportedEncodingException {
    String manifest = new String(manifestBytes, "UTF-8");
    StringBuilder patchedManifest = new StringBuilder();
    for (String line : manifest.split(lineSeparator)) {
      if (line.length() > 1 && !line.startsWith("JAR-Signature:") && !line.startsWith("Name:") && !line.startsWith("SHA-256-Digest:")) {
        patchedManifest.append(line);
        patchedManifest.append(lineSeparator);
      }
    }
    patchedManifest.append(lineSeparator); // need a new line because without it won't recognize it
    return patchedManifest.toString().getBytes("UTF-8");
  }

  public static byte[] watermark(byte[] manifestBytes) throws UnsupportedEncodingException {
    String manifest = new String(manifestBytes, "UTF-8");
    if (!manifest.contains("Deobfuscated-By: ")) {
      manifest = manifest.substring(0, manifest.length() - lineSeparator.length()); // remove new line
      manifest += "Deobfuscated-By: Threadtear " + Utils.getVersion() + lineSeparator + lineSeparator;
    }
    return manifest.getBytes("UTF-8");
  }
}
