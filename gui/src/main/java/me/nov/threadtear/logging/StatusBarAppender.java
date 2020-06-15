package me.nov.threadtear.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import me.nov.threadtear.Threadtear;

public class StatusBarAppender extends AppenderBase<ILoggingEvent> {

  @Override
  protected void append(ILoggingEvent eventObject) {
    Threadtear.getInstance().statusBar.setMessage(eventObject.getMessage());
  }
}
