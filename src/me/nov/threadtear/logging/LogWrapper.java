package me.nov.threadtear.logging;

import java.util.regex.Matcher;

import org.slf4j.*;

/**
 * @author Col-E
 */
public class LogWrapper {
  private static final Logger logfile = LoggerFactory.getLogger("logfile");
  private static final Logger console = LoggerFactory.getLogger("console");
  private static final Logger form = LoggerFactory.getLogger("form");

  public void info(String format, Object... args) {
    String msg = compile(format, args);
    console.info(msg);
    logfile.info(msg);
    form.info(msg);
  }

  public void warning(String format, Object... args) {
    String msg = compile(format, args);
    console.warn(msg);
    logfile.warn(msg);
    form.warn(msg);
  }

  public void error(String format, Object... args) {
    String msg = compile(format, args);
    console.error(msg);
    logfile.error(msg);
    form.error(msg);
  }

  public void error(String format, Throwable t, Object... args) {
    String msg = compile(format, args);
    console.error(msg, t);
    logfile.error(msg, t);
    form.error(msg, t);
  }

  /**
   * Compiles message with "{}" arg patterns.
   *
   * @param msg  Message pattern.
   * @param args Values to pass.
   * @return Compiled message with inlined arg values.
   */
  private static String compile(String msg, Object[] args) {
    int c = 0;
    try {
      while (msg.contains("{}")) {
        Object arg = args[c];
        String argStr = arg == null ? "null" : arg.toString();
        msg = msg.replaceFirst("\\{}", Matcher.quoteReplacement(argStr));
        c++;
      }
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    return msg;
  }
}