package me.nov.threadtear.swing.frame;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.objectweb.asm.tree.ClassNode;

import me.nov.threadtear.swing.panel.DecompilerPanel;

public class DecompilerFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	public DecompilerFrame(ClassNode cn) {
		setTitle("Decompiler: " + cn.name);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 1000, 600);
		setLayout(new BorderLayout());
		setAlwaysOnTop(true);
		JPanel cp = new JPanel(new BorderLayout());
		cp.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		cp.add(new DecompilerPanel(cn), BorderLayout.CENTER);
		this.add(cp, BorderLayout.CENTER);
		JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
		JButton close = new JButton("Close");
		close.addActionListener(e -> {
			dispose();
		});
		buttons.add(close);
		getContentPane().add(buttons, BorderLayout.SOUTH);

	}
}
