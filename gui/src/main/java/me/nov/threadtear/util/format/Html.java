package me.nov.threadtear.util.format;

public final class Html {
  private Html() {
  }

  public static String color(String color, String text) {
    return "<font color=\"" + color + "\">" + text + "</font>";
  }

  public static String bold(String text) {
    return "<b>" + text + "</b>";
  }

  public static String italics(String text) {
    return "<i>" + text + "</i>";
  }

  public static String underline(String text) {
    return "<u>" + text + "</u>";
  }

  public static String mono(String text) {
    return "<tt>" + text + "</tt>";
  }

  public static String escape(String s) {
    StringBuilder out = new StringBuilder(Math.max(16, s.length()));
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c > 127 || c == '"' || c == '\'' || c == '<' || c == '>' || c == '&') {
        out.append("&#");
        out.append((int) c);
        out.append(';');
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }
}
