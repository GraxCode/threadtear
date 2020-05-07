package me.nov.threadtear.swing.frame;

import java.awt.*;

import javax.swing.*;

import org.objectweb.asm.tree.ClassNode;

import com.github.weisj.darklaf.icons.IconLoader;

import me.nov.threadtear.swing.Utils;
import me.nov.threadtear.swing.panel.BytecodePanel;

public class BytecodeFrame extends JFrame {
  private static final long serialVersionUID = 1L;

  public BytecodeFrame(ClassNode cn) {
    setTitle("Bytecode: " + cn.name);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setBounds(100, 100, 1000, 600);
    setMinimumSize(new Dimension(500, 300));
    setLayout(new BorderLayout());
    setIconImage(Utils.iconToImage(IconLoader.get().loadSVGIcon("res/bytecode.svg", 64, 64, false)));
    setAlwaysOnTop(true);
    JPanel cp = new JPanel(new BorderLayout());
    cp.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    cp.add(new BytecodePanel(cn), BorderLayout.CENTER);
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
