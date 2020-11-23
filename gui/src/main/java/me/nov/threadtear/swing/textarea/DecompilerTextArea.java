package me.nov.threadtear.swing.textarea;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.extensions.rsyntaxarea.DarklafRSyntaxTheme;
import com.github.weisj.darklaf.theme.event.ThemeInstalledListener;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;

public class DecompilerTextArea extends RSyntaxTextArea {
  private static final long serialVersionUID = 1L;

  public DecompilerTextArea() {
    this.setSyntaxEditingStyle("text/java");
    this.setCodeFoldingEnabled(true);
    this.setAntiAliasingEnabled(true);
    this.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    this.setEditable(false);
    updateSyntaxTheme();
    LafManager.addThemeChangeListener((ThemeInstalledListener) e -> updateSyntaxTheme());
    addHierarchyListener(e -> {
      if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
        SwingUtilities.invokeLater(this::updateSyntaxTheme);
      }
    });
  }

  private void updateSyntaxTheme() {
    new DarklafRSyntaxTheme(this).apply(this);
  }
}
