package me.nov.threadtear.swing.component.panel;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import me.nov.threadtear.Threadtear;

public class ConfigurationPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private Threadtear main;
	private JCheckBox verbose;
	private JCheckBox computeFrames;
	private JCheckBox ignoreErrors;
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
		panel.add(computeFrames = new JCheckBox("Compute Frames"));
		panel.add(ignoreErrors = new JCheckBox("Ignore Errors"));
		panel.add(removeSignature = new JCheckBox("Remove Signature"));
		return panel;
	}

	private JPanel createBottomButtons() {
		JPanel panel = new JPanel(new GridLayout(1, 4, 16, 16));
		panel.add(new JButton("Load Config"));
		panel.add(new JButton("Save Config"));
		panel.add(new JButton("Show Log"));
		JButton run = new JButton("Run");
		run.addActionListener(l -> {
			main.run(verbose.isSelected(), computeFrames.isSelected(), ignoreErrors.isSelected(), removeSignature.isSelected());
		});
		panel.add(run);
		return panel;
	}
}
