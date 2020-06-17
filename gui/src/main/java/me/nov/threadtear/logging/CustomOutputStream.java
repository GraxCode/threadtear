package me.nov.threadtear.logging;

import java.io.*;
import java.util.logging.*;

public class CustomOutputStream extends OutputStream {
  Logger logger;
  Level level;
  StringBuilder stringBuilder;

  public CustomOutputStream(Logger logger, Level level) {
    this.logger = logger;
    this.level = level;
    stringBuilder = new StringBuilder();
  }

  @Override
  public final void write(int i) throws IOException {
    char c = (char) i;
    if (c == '\r' || c == '\n') {
      if (stringBuilder.length() > 0) {
        logger.log(level, stringBuilder.toString());
        stringBuilder = new StringBuilder();
      }
    } else
      stringBuilder.append(c);
  }
}