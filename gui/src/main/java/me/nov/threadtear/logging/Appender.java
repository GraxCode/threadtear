package me.nov.threadtear.logging;

import java.awt.Color;

import javax.swing.*;
import javax.swing.text.*;

import ch.qos.logback.classic.*;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import me.nov.threadtear.Threadtear;

/**
 * @author Rodrigo Garcia Lima (email:
 * rodgarcialima@gmail.com | github: rodgarcialima)
 */
public class Appender extends AppenderBase<ILoggingEvent> {

  private PatternLayout patternLayout;
  public static final SimpleAttributeSet ERROR_ATT, WARN_ATT, INFO_ATT, DEBUG_ATT, TRACE_ATT, RESTO_ATT;

  static {
    ERROR_ATT = new SimpleAttributeSet();
    ERROR_ATT.addAttribute(StyleConstants.CharacterConstants.Bold, Boolean.TRUE);
    ERROR_ATT.addAttribute(StyleConstants.CharacterConstants.Italic, Boolean.FALSE);
    ERROR_ATT.addAttribute(StyleConstants.CharacterConstants.Foreground, new Color(255, 107, 107));

    WARN_ATT = new SimpleAttributeSet();
    WARN_ATT.addAttribute(StyleConstants.CharacterConstants.Bold, Boolean.FALSE);
    WARN_ATT.addAttribute(StyleConstants.CharacterConstants.Italic, Boolean.FALSE);
    WARN_ATT.addAttribute(StyleConstants.CharacterConstants.Foreground, new Color(254, 202, 87));

    INFO_ATT = new SimpleAttributeSet();
    INFO_ATT.addAttribute(StyleConstants.CharacterConstants.Bold, Boolean.FALSE);
    INFO_ATT.addAttribute(StyleConstants.CharacterConstants.Italic, Boolean.FALSE);
    INFO_ATT.addAttribute(StyleConstants.CharacterConstants.Foreground, new Color(72, 219, 251));

    DEBUG_ATT = new SimpleAttributeSet();
    DEBUG_ATT.addAttribute(StyleConstants.CharacterConstants.Bold, Boolean.FALSE);
    DEBUG_ATT.addAttribute(StyleConstants.CharacterConstants.Italic, Boolean.TRUE);
    DEBUG_ATT.addAttribute(StyleConstants.CharacterConstants.Foreground, new Color(29, 209, 161));

    TRACE_ATT = new SimpleAttributeSet();
    TRACE_ATT.addAttribute(StyleConstants.CharacterConstants.Bold, Boolean.FALSE);
    TRACE_ATT.addAttribute(StyleConstants.CharacterConstants.Italic, Boolean.TRUE);
    TRACE_ATT.addAttribute(StyleConstants.CharacterConstants.Foreground, new Color(255, 159, 243));

    RESTO_ATT = new SimpleAttributeSet();
    RESTO_ATT.addAttribute(StyleConstants.CharacterConstants.Bold, Boolean.FALSE);
    RESTO_ATT.addAttribute(StyleConstants.CharacterConstants.Italic, Boolean.TRUE);
    RESTO_ATT.addAttribute(StyleConstants.CharacterConstants.Foreground, new Color(200, 214, 229));
  }

  @Override
  public void start() {
    patternLayout = new PatternLayout();
    patternLayout.setContext(getContext());
    patternLayout.setPattern("%d{HH:mm:ss.SSS} %-5level: %msg %ex{2}%nopex%n");
    patternLayout.start();

    super.start();
  }

  @Override
  protected void append(ILoggingEvent event) {
    String formattedMsg = patternLayout.doLayout(event);

    SwingUtilities.invokeLater(() -> {
      JTextPane textPane = Threadtear.getInstance().logFrame.getTextArea();
      if (textPane == null) {
        return;
      }
      synchronized (textPane.getDocument()) {
        try {
          int limite = 800;
          int apaga = 200;
          if (textPane.getDocument().getDefaultRootElement().getElementCount() > limite) {
            int end = getLineEndOffset(textPane, apaga);
            replaceRange(textPane, null, 0, end);
          }

          if (event.getLevel() == Level.ERROR)
            textPane.getDocument().insertString(textPane.getDocument().getLength(), formattedMsg, ERROR_ATT);
          else if (event.getLevel() == Level.WARN)
            textPane.getDocument().insertString(textPane.getDocument().getLength(), formattedMsg, WARN_ATT);
          else if (event.getLevel() == Level.INFO)
            textPane.getDocument().insertString(textPane.getDocument().getLength(), formattedMsg, INFO_ATT);
          else if (event.getLevel() == Level.DEBUG)
            textPane.getDocument().insertString(textPane.getDocument().getLength(), formattedMsg, DEBUG_ATT);
          else if (event.getLevel() == Level.TRACE)
            textPane.getDocument().insertString(textPane.getDocument().getLength(), formattedMsg, TRACE_ATT);
          else
            textPane.getDocument().insertString(textPane.getDocument().getLength(), formattedMsg, RESTO_ATT);

        } catch (BadLocationException e) {
        }

        textPane.setCaretPosition(textPane.getDocument().getLength());
      }
    });
  }

  private int getLineCount(JTextPane textPane) {
    return textPane.getDocument().getDefaultRootElement().getElementCount();
  }

  private int getLineEndOffset(JTextPane textPane, int line) throws BadLocationException {
    int lineCount = getLineCount(textPane);
    if (line < 0) {
      throw new BadLocationException("Negative line", -1);
    } else if (line >= lineCount) {
      throw new BadLocationException("No such line", textPane.getDocument().getLength() + 1);
    } else {
      Element map = textPane.getDocument().getDefaultRootElement();
      Element lineElem = map.getElement(line);
      int endOffset = lineElem.getEndOffset();
      // hide the implicit break at the end of the document
      return ((line == lineCount - 1) ? (endOffset - 1) : endOffset);
    }
  }

  private void replaceRange(JTextPane textPane, String str, int start, int end) throws IllegalArgumentException {
    if (end < start) {
      throw new IllegalArgumentException("end before start");
    }
    Document doc = textPane.getDocument();
    if (doc != null) {
      try {
        if (doc instanceof AbstractDocument) {
          ((AbstractDocument) doc).replace(start, end - start, str, null);
        } else {
          doc.remove(start, end - start);
          doc.insertString(start, str, null);
        }
      } catch (BadLocationException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }
  }
}
