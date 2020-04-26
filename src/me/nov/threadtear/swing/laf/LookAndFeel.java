package me.nov.threadtear.swing.laf;

import java.awt.Color;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.ColorUIResource;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.OneDarkTheme;
import com.github.weisj.darklaf.theme.Theme;

public class LookAndFeel {
	public static void setLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			for (LookAndFeelInfo lafi : UIManager.getInstalledLookAndFeels()) {
				if (lafi.getName().equals("Nimbus")) {
					try {
						UIManager.setLookAndFeel(lafi.getClassName());
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				}
			}
		}
		LafManager.enableLogging(true);
		LafManager.registerDefaultsAdjustmentTask((t, d) -> {
			if (Theme.isDark(t)) {
				Object p = d.get("backgroundContainer");
				if (p instanceof Color) {
					d.put("backgroundContainer", new ColorUIResource(((Color) p).darker()));
				}
			}
		});
		LafManager.install(new OneDarkTheme());
	}
}
