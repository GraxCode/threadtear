package me.nov.threadtear.swing.frame;

import java.awt.*;

import javax.swing.*;

import com.github.weisj.darklaf.icons.IconLoader;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.swing.Utils;
import me.nov.threadtear.swing.panel.*;

public class AnalysisFrame extends JFrame {
  private static final long serialVersionUID = 1L;

  public AnalysisFrame(Clazz clazz) {
    setTitle(clazz.node.name);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setBounds(100, 100, 1000, 600);
    setMinimumSize(new Dimension(900, 540));
    setLayout(new BorderLayout());
    setIconImage(Utils.iconToImage(IconLoader.get().loadSVGIcon("res/decompile.svg", 64, 64, false)));
    setAlwaysOnTop(true);
    JPanel cp = new JPanel(new BorderLayout());
    cp.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Decompiler", IconLoader.get().loadSVGIcon("res/decompile.svg", false), new DecompilerPanel(clazz));
    tabbedPane.addTab("Bytecode", IconLoader.get().loadSVGIcon("res/bytecode.svg", false), new BytecodePanel(clazz.node));

    cp.add(tabbedPane, BorderLayout.CENTER);
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
