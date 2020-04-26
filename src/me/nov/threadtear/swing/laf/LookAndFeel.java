package me.nov.threadtear.swing.laf;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

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
		LafManager.registerInitTask(LookAndFeel::applyCustomChanges);
		LafManager.install(new OneDarkTheme());
	}

	public static void applyCustomChanges(Theme t, UIDefaults d) {
		if (Theme.isDark(t)) {
			d.put("Tree.background", d.getColor("Tree.background").darker());
		}
	}
}
