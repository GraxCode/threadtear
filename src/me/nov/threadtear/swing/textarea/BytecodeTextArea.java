package me.nov.threadtear.swing.textarea;

import java.awt.Font;
import java.io.IOException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;

public class BytecodeTextArea extends RSyntaxTextArea {
  private static final long serialVersionUID = 1L;

  public BytecodeTextArea() {
    this.setSyntaxEditingStyle(SYNTAX_STYLE_CPLUSPLUS);
    this.setCodeFoldingEnabled(true);
    this.setAntiAliasingEnabled(true);
    this.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    this.setEditable(false);

    try {
      Theme theme = Theme.load(getClass().getResourceAsStream("/res/rsta-theme.xml"));
      theme.apply(this);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
