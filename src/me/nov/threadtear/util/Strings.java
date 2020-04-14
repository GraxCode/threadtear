package me.nov.threadtear.util;

public class Strings {
	public static boolean isHighUTF(String cst) {
		int unicodes = 0;
		for (char c : cst.toCharArray()) {
			if (c > 127) {
				unicodes++;
			}
		}
		return (unicodes / (float) cst.length()) >= 0.5;
	}

	public static boolean isHighSDev(String cst) {
		if (cst.length() < 2)
			return false;
		double sum = 0;
		char[] ccst = cst.toCharArray();
		for (char c : ccst)
			sum += c;
		double mean = sum / cst.length();
		double sdev = 0.0;
		for (int i = 1; i < ccst.length; i++)
			sdev += (ccst[i] - mean) * (ccst[i] - mean);
		sdev = Math.sqrt(sdev / (ccst.length - 1.0));
		if (sdev > 30) {
			return true;
		}
		return false;
	}
}
