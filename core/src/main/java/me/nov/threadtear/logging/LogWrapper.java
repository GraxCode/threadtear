package me.nov.threadtear.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;

import org.slf4j.*;

import me.nov.threadtear.execution.Clazz;

/**
 * @author Col-E
 */
public class LogWrapper {

  public static final LogWrapper logger = new LogWrapper();

  private final List<Logger> loggers;

  private Clazz currentErrorCollector;

  public void collectErrors(Clazz c) {
    currentErrorCollector = c;
  }

  public LogWrapper() {
    this.loggers = new ArrayList<>();
  }

  public void addLogger(Logger logger) {
    if (logger != null) loggers.add(logger);
  }

  public void removeLogger(Logger logger) {
    loggers.remove(logger);
  }

  public void info(String format, Object... args) {
    String msg = compile(format, args);
    logMessage(msg, Logger::info);
  }

  public void errorIf(String format, boolean error, Object... args) {
    if (error) {
      error(format, args);
    } else {
      info(format, args);
    }
  }

  public void warning(String format, Object... args) {
    String msg = compile(format, args);
    logMessage(msg, Logger::warn);
  }

  public void error(String format, Object... args) {
    String msg = compile(format, args);
    if (currentErrorCollector != null) {
      currentErrorCollector.addFail(msg);
    }
    logMessage(msg, Logger::error);
  }

  public void error(String format, Throwable t, Object... args) {
    String msg = compile(format, args);
    msg += " (" + t.toString() + ")";
    if (currentErrorCollector != null) {
      currentErrorCollector.addFail(t);
    }
    logMessage(msg, Logger::error);
  }

  public void debug(String format, Object... args) {
    String msg = compile(format, args);
    logMessage(msg, Logger::debug);
  }

  public void trace(String format, Object... args) {
    String msg = compile(format, args);
    logMessage(msg, Logger::trace);
  }

  private void logMessage(String msg, BiConsumer<Logger, String> logFunction) {
    for (Logger logger : loggers) {
      logFunction.accept(logger, msg);
    }
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
