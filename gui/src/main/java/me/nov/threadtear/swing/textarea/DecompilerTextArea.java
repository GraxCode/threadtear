package me.nov.threadtear.swing.textarea;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.components.tooltip.ToolTipStyle;
import com.github.weisj.darklaf.theme.event.ThemeInstalledListener;
import com.github.weisj.darklaf.ui.tooltip.DarkToolTipUI;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.Gutter;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;

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
      Theme syntaxTheme = Theme.load(Objects.requireNonNull(getClass().getResourceAsStream(themeName)));
      syntaxTheme.apply(this);
    } catch (IOException e) {
      e.printStackTrace();
    }
    setBackground(UIManager.getColor("textBackgroundSecondary"));
    setCurrentLineHighlightColor(UIManager.getColor("textSelectionBackgroundSecondary"));
    Gutter gutter = RSyntaxUtilities.getGutter(this);
    if (gutter != null) {
      Color gutterBg = UIManager.getColor("textBackgroundSecondaryInactive");
      gutter.setBackground(gutterBg);
      gutter.setLineNumberColor(UIManager.getColor("textForegroundSecondary"));
      Color lineColor = UIManager.getColor("borderSecondary");
      gutter.setBorderColor(lineColor);
      gutter.setFoldIndicatorForeground(lineColor);
      gutter.setFoldBackground(gutterBg);
      gutter.setShowCollapsedRegionToolTips(true);
      gutter.setArmedFoldBackground(getCurrentLineHighlightColor());
      for (Component c : gutter.getComponents()) {
        ((JComponent) c).putClientProperty(DarkToolTipUI.KEY_STYLE, ToolTipStyle.PLAIN);
      }
    }
  }
}
