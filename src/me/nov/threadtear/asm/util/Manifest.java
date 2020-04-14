package me.nov.threadtear.asm.util;

import java.io.UnsupportedEncodingException;

public class Manifest {
	public static byte[] patchManifest(byte[] manifestBytes) throws UnsupportedEncodingException {
		String manifest = new String(manifestBytes, "UTF-8");
		StringBuilder patchedManifest = new StringBuilder();
		for (String line : manifest.split("\n")) {
			if (line.length() > 1 && !line.startsWith("JAR-Signature:") && !line.startsWith("Name:") && !line.startsWith("SHA-256-Digest:")) {
				patchedManifest.append(line);
				patchedManifest.append('\n');
			}
		}
		patchedManifest.append('\n'); // need a new line because without it won't recognize it
		return patchedManifest.toString().getBytes("UTF-8");
	}
}
