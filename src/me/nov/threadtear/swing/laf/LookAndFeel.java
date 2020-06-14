package me.nov.threadtear.swing.laf;

import java.awt.Color;

import javax.swing.plaf.ColorUIResource;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.*;
import com.github.weisj.darklaf.theme.info.DefaultThemeProvider;

public class LookAndFeel {

  public static void init() {
    LafManager.setThemeProvider(new DefaultThemeProvider(
      new IntelliJTheme(),
      new OneDarkTheme(),
      new HighContrastLightTheme(),
      new HighContrastDarkTheme()
    ));
  }

  public static void setLookAndFeel() {
    LafManager.enableLogging(true);
    LafManager.registerDefaultsAdjustmentTask((t, d) -> {
      if (Theme.isDark(t)) {
        Object p = d.get("backgroundContainer");
        if (p instanceof Color) {
          d.put("backgroundContainer", new ColorUIResource(((Color) p).darker()));
        }
      }
    });
    LafManager.installTheme(LafManager.getPreferredThemeStyle());
  }
}
