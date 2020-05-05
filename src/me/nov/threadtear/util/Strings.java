package me.nov.threadtear.util;

import java.io.*;
import java.util.*;

import javax.lang.model.SourceVersion;

import org.apache.commons.io.IOUtils;

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
    return cst.length() >= 2 ? (calcSdev(cst) > 30) : false;
  }

  public static double calcSdev(String cst) {
    double sum = 0;
    char[] ccst = cst.toCharArray();
    for (char c : ccst)
      sum += c;
    double mean = sum / cst.length();
    double sdev = 0.0;
    for (int i = 1; i < ccst.length; i++)
      sdev += (ccst[i] - mean) * (ccst[i] - mean);
    return Math.sqrt(sdev / (ccst.length - 1.0));
  }

  public static Queue<String> generateWordQueue(int amount, InputStream wordList) {
    Queue<String> queue = new LinkedList<>();
    try {
      String list = IOUtils.toString(wordList, "UTF-8");
      List<String> words = Arrays.asList(list.split("\n"));
      Collections.shuffle(words);
      int i = 0;
      while (queue.size() < amount) {
        String word = i >= words.size() ? generateWord(8) : words.get(i);
        if (SourceVersion.isName(word)) {
          queue.add(word);
        }
        i++;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return queue;
  }

  private static final String goodConsonants = "bcdfglmnprstvyz";
  private static final String vocals = "aeiou";
  private static final Random random = new Random();

  public static String generateWord(int len) {
    boolean vocal = random.nextBoolean();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < len; i++) {
      if (vocal) {
        sb.append(goodConsonants.charAt(random.nextInt(goodConsonants.length())));
      } else {
        sb.append(vocals.charAt(random.nextInt(vocals.length())));
      }
      vocal = !vocal;
    }
    return sb.toString();
  }
}
