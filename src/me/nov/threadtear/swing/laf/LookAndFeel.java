package me.nov.threadtear.swing.laf;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;

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
		LafManager.enableLogging(false);
		LafManager.install(new DarculaTheme());
		UIManager.put("Tree.background", UIManager.getColor("Tree.background").darker());
	}
}
