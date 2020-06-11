package me.nov.threadtear.swing.textarea;

import java.awt.Font;
import java.io.IOException;

import org.fife.ui.rsyntaxtextarea.*;

import com.github.weisj.darklaf.LafManager;

public class DecompilerTextArea extends RSyntaxTextArea {
  private static final long serialVersionUID = 1L;

  public DecompilerTextArea() {
    this.setSyntaxEditingStyle("text/java");
    this.setCodeFoldingEnabled(true);
    this.setAntiAliasingEnabled(true);
    this.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    this.setEditable(false);
    String themeName = com.github.weisj.darklaf.theme.Theme.isDark(LafManager.getTheme()) ? "/res/rsta-theme.xml" :
            "/org/fife/ui/rsyntaxtextarea/themes" + "/eclipse.xml";
    try {
      Theme theme = Theme.load(getClass().getResourceAsStream(themeName));
      theme.apply(this);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
