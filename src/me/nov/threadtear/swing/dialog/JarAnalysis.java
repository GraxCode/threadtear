package me.nov.threadtear.swing.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.util.Counting;
import me.nov.threadtear.util.Strings;

public class JarAnalysis extends JDialog implements Opcodes {
	private static final long serialVersionUID = 1L;
	private JTextArea area;

	public JarAnalysis(ArrayList<Clazz> classes) {
		setModalityType(ModalityType.APPLICATION_MODAL);
		setTitle("Obfuscation Analysis");
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		JPanel cp = new JPanel();
		cp.setBorder(new EmptyBorder(10, 10, 10, 10));
		getContentPane().add(cp, BorderLayout.CENTER);
		cp.setLayout(new BorderLayout(0, 0));
		JPanel treePanel = new JPanel(new BorderLayout());
		treePanel.setBorder(BorderFactory.createLoweredBevelBorder());
		area = new JTextArea();
		area.setMargin(new Insets(8, 8, 8, 8));
		area.setFont(new Font("Consolas", Font.PLAIN, 11));
		treePanel.add(new JScrollPane(area), BorderLayout.CENTER);
		cp.add(treePanel);
		JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttons, BorderLayout.SOUTH);
		JButton close = new JButton("Close");
		close.addActionListener(e -> {
			dispose();
		});
		close.setActionCommand("Close");
		buttons.add(close);
		getRootPane().setDefaultButton(close);
		SwingUtilities.invokeLater(() -> {
			analyze(classes);
		});
	}

	private void analyze(ArrayList<Clazz> classes) {
		if (classes == null) {
			print("Open a jar file first.");
			return;
		}
		print("Analyzing instructions...\n\n");
		print("Jumps in proportion to references -> ");
		double jumpPercentage = classes.stream().map(c -> c.node.methods).flatMap(List::stream)
				.mapToDouble(m -> Counting.percentOf(AbstractInsnNode.JUMP_INSN, m.instructions, AbstractInsnNode.METHOD_INSN, AbstractInsnNode.FIELD_INSN, AbstractInsnNode.TYPE_INSN)).average().orElse(Double.NaN);
		print(Math.round(jumpPercentage * 10000) / 100.0 + "%\n");
		print("Normal proportion is about 11%.\n");
		print("A higher value indicates flow obfuscation.\n");
		print("----------------------------------------------\n");

		print("Average invokedynamics per method -> ");
		double invokedynamics = classes.stream().map(c -> c.node.methods).flatMap(List::stream).mapToDouble(m -> Counting.count(m.instructions, AbstractInsnNode.INVOKE_DYNAMIC_INSN)).average().orElse(Double.NaN);
		print(Math.round(invokedynamics * 100) / 100.0 + "\n");
		print("Normally about 0.0 - 0.4.\n");
		print("A higher value indicates reference obfuscation.\n");
		print("----------------------------------------------\n");

		print("Normally rare stack operations per method -> ");
		double stackop = classes.stream().map(c -> c.node.methods).flatMap(List::stream).mapToDouble(m -> Counting.countOp(m.instructions, POP2, DUP2, DUP_X1, DUP_X2, DUP2_X1, DUP2_X2, SWAP)).average().orElse(Double.NaN);
		print(Math.round(stackop * 100) / 100.0 + "\n");
		print("Normally about 0.0 - 0.1.\n");
		print("A higher value indicates flow obfuscation.\n");
		print("----------------------------------------------\n");

		print("Average standard deviation of letters in strings -> ");
		double sdev = classes.stream().map(c -> c.node.methods).flatMap(List::stream).map(m -> m.instructions.spliterator()).flatMap(insns -> StreamSupport.stream(insns, false))
				.filter(ain -> ain.getOpcode() == LDC && ((LdcInsnNode) ain).cst instanceof String && ((LdcInsnNode) ain).cst.toString().length() > 2).mapToDouble(ain -> Strings.calcSdev(((LdcInsnNode) ain).cst.toString())).average()
				.orElse(Double.NaN);
		print(Math.round(sdev * 100) / 100.0 + "\n");
		print("Normally around 15 - 40.\n");
		print("A higher value could indicate string obfuscation.\n");
		print("----------------------------------------------\n");

		print("Percentage of high character value strings -> ");
		double highutf = classes.stream().map(c -> c.node.methods).flatMap(List::stream).map(m -> m.instructions.spliterator()).flatMap(insns -> StreamSupport.stream(insns, false))
				.filter(ain -> ain.getOpcode() == LDC && ((LdcInsnNode) ain).cst instanceof String && ((LdcInsnNode) ain).cst.toString().length() > 2).mapToDouble(ain -> Strings.isHighUTF(((LdcInsnNode) ain).cst.toString()) ? 1 : 0).average()
				.orElse(Double.NaN);
		print(Math.round(highutf * 10000) / 100.0 + "\n");
		print("Normally around 0% - 1%.\n");
		print("A higher value could indicate string obfuscation.\n");
		print("----------------------------------------------\n");

		print("NOP instructions per method -> ");
		double nops = classes.stream().map(c -> c.node.methods).flatMap(List::stream).mapToDouble(m -> Counting.countOp(m.instructions, NOP)).average().orElse(Double.NaN);
		print(Math.round(nops * 100) / 100.0 + "\n");
		print("Normally about 0.0 - 0.1.\n");
		print("A higher value indicates unoptimization.\n");
		print("----------------------------------------------\n");

	}

	private void print(String string) {
		area.append(string);
		area.setCaretPosition(area.getDocument().getLength());
	}
}
