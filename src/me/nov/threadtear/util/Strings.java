package me.nov.threadtear.util;

public class Strings {
	public static boolean seemsEncrypted(String cst) {
		int unicodes = 0;
		for (char c : cst.toCharArray()) {
			if (c > 127) {
				unicodes++;
			}
		}
		return (unicodes / (float) cst.length()) >= 0.5;
	}
}
