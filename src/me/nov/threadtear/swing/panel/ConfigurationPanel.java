package me.nov.threadtear.swing.panel;

import java.awt.GridLayout;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.github.weisj.darklaf.icons.IconLoader;

import me.nov.threadtear.Threadtear;
import me.nov.threadtear.io.JarIO;

public class ConfigurationPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private Threadtear main;
	private JCheckBox verbose;
	private JCheckBox computeFrames;
	private JCheckBox disableSecurity;
	private JCheckBox removeSignature;

	public ConfigurationPanel(Threadtear main) {
		this.main = main;
		this.setLayout(new GridLayout(2, 1, 16, 16));
		this.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
		this.add(createCheckboxes());
		this.add(createBottomButtons());

	}

	private JPanel createCheckboxes() {
		JPanel panel = new JPanel(new GridLayout(2, 2));
		panel.add(verbose = new JCheckBox("Verbose logging"));
		verbose.setToolTipText("Log more information and print full stack traces.");
		panel.add(computeFrames = new JCheckBox("Compute frames"));
		computeFrames.setEnabled(false);
		panel.add(disableSecurity = new JCheckBox("<html>Disable <tt>SecurityManager</tt> protection"));
		disableSecurity.setToolTipText("Remove the protection agains unwanted executions. Could improve deobfuscation.");
		panel.add(removeSignature = new JCheckBox("Remove manifest signature"));
		removeSignature.setToolTipText("Remove the signature from the manifest file, if available.");
		return panel;
	}

	private JPanel createBottomButtons() {
		JPanel panel = new JPanel(new GridLayout(1, 4, 16, 16));
		JButton loadCfg = new JButton("Load config", IconLoader.get().loadSVGIcon("res/load_config.svg", false));
		loadCfg.setEnabled(false);
		panel.add(loadCfg);
		JButton saveCfg = new JButton("Save config", IconLoader.get().loadSVGIcon("res/save_config.svg", false));
		saveCfg.setEnabled(false);
		panel.add(saveCfg);
		JButton save = new JButton("Save as jar file", IconLoader.get().loadSVGIcon("res/save.svg", false));
		save.addActionListener(l -> {
			File inputFile = main.listPanel.classList.inputFile;
			if (inputFile == null) {
				JOptionPane.showMessageDialog(this, "You have to load a jar file first.");
				return;
			}
			JFileChooser jfc = new JFileChooser(inputFile.getParentFile());
			jfc.setAcceptAllFileFilterUsed(false);
			jfc.setSelectedFile(inputFile);
			jfc.setDialogTitle("Save transformed jar archive");
			jfc.setFileFilter(new FileNameExtensionFilter("Java Package (*.jar)", "jar"));
			int result = jfc.showSaveDialog(this);
			if (result == JFileChooser.APPROVE_OPTION) {
				File output = jfc.getSelectedFile();
				JarIO.saveAsJar(inputFile, output, main.listPanel.classList.classes, removeSignature.isSelected());
				Threadtear.logger.info("Saved to " + output.getAbsolutePath());
			}
		});
		panel.add(save);
		JButton run = new JButton("Run", IconLoader.get().loadSVGIcon("res/run.svg", false));
		run.addActionListener(l -> {
			main.run(verbose.isSelected(), computeFrames.isSelected(), disableSecurity.isSelected());
		});
		panel.add(run);
		return panel;
	}
}
