package me.nov.threadtear.swing.laf;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.OneDarkTheme;

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
		LafManager.install(new OneDarkTheme());
		applyCustomChanges();
	}

	public static void applyCustomChanges() {
		UIManager.put("Tree.background", UIManager.getColor("Tree.background").darker());
	}
}
