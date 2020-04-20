package me.nov.threadtear.swing.panel;

import java.awt.GridLayout;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import me.nov.threadtear.Threadtear;
import me.nov.threadtear.asm.io.JarIO;

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
		panel.add(verbose = new JCheckBox("Verbose"));
		panel.add(computeFrames = new JCheckBox("Compute frames"));
		panel.add(disableSecurity = new JCheckBox("Disable SecurityManager protection"));
		panel.add(removeSignature = new JCheckBox("Remove signature"));
		return panel;
	}

	private JPanel createBottomButtons() {
		JPanel panel = new JPanel(new GridLayout(1, 4, 16, 16));
		JButton loadCfg = new JButton("Load config");
		loadCfg.setEnabled(false);
		panel.add(loadCfg);
		JButton saveCfg = new JButton("Save config");
		saveCfg.setEnabled(false);
		panel.add(saveCfg);
		JButton save = new JButton("Save as jar file");
		save.addActionListener(l -> {
			File inputFile = main.listPanel.classList.inputFile;
			if (inputFile == null)
				return;
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
		JButton run = new JButton("Run");
		run.addActionListener(l -> {
			main.run(verbose.isSelected(), computeFrames.isSelected(), disableSecurity.isSelected());
		});
		panel.add(run);
		return panel;
	}
}
