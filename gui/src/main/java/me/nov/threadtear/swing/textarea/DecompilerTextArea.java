package me.nov.threadtear.swing.textarea;

import java.awt.Font;
import java.io.IOException;
import java.util.Objects;

import com.github.weisj.darklaf.theme.event.ThemeInstalledListener;
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
    reloadSyntaxTheme();
    LafManager.addThemeChangeListener((ThemeInstalledListener) e -> updateSyntaxTheme(e.getNewTheme()));
  }

  public void reloadSyntaxTheme() {
    updateSyntaxTheme(LafManager.getTheme());
  }

  private void updateSyntaxTheme(com.github.weisj.darklaf.theme.Theme theme) {
    String themeName = com.github.weisj.darklaf.theme.Theme.isDark(theme) ? "rsta-theme.xml" :
                       "/org/fife/ui/rsyntaxtextarea/themes/eclipse.xml";
    try {
      Theme syntaxTheme = Theme.load(
        Objects.requireNonNull(getClass().getResourceAsStream(themeName))
      );
      syntaxTheme.apply(this);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
