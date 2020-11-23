package me.nov.threadtear.swing.panel;

import com.github.weisj.darklaf.components.OverlayScrollPane;
import com.github.weisj.darklaf.components.border.DarkBorders;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxCellRenderer;
import me.nov.threadtear.graph.Block;
import me.nov.threadtear.graph.CFGraph;
import me.nov.threadtear.graph.CFGraph.CFGComponent;
import me.nov.threadtear.graph.Converter;
import me.nov.threadtear.graph.layout.PatchedHierarchicalLayout;
import me.nov.threadtear.graph.vertex.BlockVertex;
import me.nov.threadtear.swing.SwingUtils;
import me.nov.threadtear.swing.button.ReloadButton;
import me.nov.threadtear.swing.component.AutoCompletion;
import me.nov.threadtear.util.Images;
import me.nov.threadtear.util.format.Strings;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CFGPanel extends JPanel {
  private static final long serialVersionUID = 1L;
  private final ArrayList<Block> blocks = new ArrayList<>();
  private final CFGraph graph;
  private final CFGComponent graphComponent;
  private final Map<Block, mxCell> existing = new HashMap<>();
  public boolean useTreeLayout = false;
  private JScrollPane scrollPane;
  private MethodNode mn;

  public CFGPanel(ClassNode cn) {
    this.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    this.setLayout(new BorderLayout(4, 4));
    this.graph = new CFGraph();
    JPanel leftActionPanel = new JPanel();
    leftActionPanel.setLayout(new GridBagLayout());
    JPanel rightActionPanel = new JPanel();
    rightActionPanel.setLayout(new GridBagLayout());

    leftActionPanel.add(new JLabel("Control flow graph"));
    JComboBox<Object> methodSelection = new JComboBox<>(cn.methods.stream().map(m -> m.name + m.desc).toArray());
    AutoCompletion.enable(methodSelection);
    methodSelection.setPreferredSize(new Dimension(Math.min(400, methodSelection.getPreferredSize().width),
      methodSelection.getPreferredSize().height));
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(0, 4, 0, 0);
    leftActionPanel.add(methodSelection, gbc);
    methodSelection.addActionListener(l -> {
      String item = (String) methodSelection.getSelectedItem();
      mn = cn.methods.stream().filter(m -> (m.name + m.desc).equals(item)).findAny().get();
      clear();
      generateGraph();
      SwingUtilities.invokeLater(() -> {
        Rectangle bounds = scrollPane.getViewport().getViewRect();
        JScrollBar horizontal = scrollPane.getHorizontalScrollBar();
        horizontal.setValue((horizontal.getMaximum() - bounds.width) / 2);
      });
    });

    JPanel rightActions = new JPanel();
    rightActions.setLayout(new GridBagLayout());
    JComboBox<String> layout = new JComboBox<>(new String[]{"Hierarchial layout", "Compact layout"});
    layout.addActionListener(a -> {
      useTreeLayout = layout.getSelectedIndex() == 1;
      generateGraph();
    });
    rightActions.add(layout);
    JButton save = new JButton("Save as image");
    save.setIcon(SwingUtils.getIcon("save.svg", true));
    save.addActionListener(l -> {
      File parentDir = FileSystemView.getFileSystemView().getHomeDirectory();
      JFileChooser jfc = new JFileChooser(parentDir);
      jfc.setAcceptAllFileFilterUsed(false);
      jfc.setFileFilter(new FileNameExtensionFilter("Portable Network Graphics (.png)", "png"));
      jfc.addChoosableFileFilter(new FileNameExtensionFilter("Bitmap image file (.bmp)", "bmp"));
      if (mn.name.length() < 32) {
        jfc.setSelectedFile(new File(parentDir, mn.name.replaceAll("[^a-zA-Z0-9-_.]", "_") + ".png"));
      } else {
        jfc.setSelectedFile(new File(parentDir, "method-" + (mn.name + mn.desc).hashCode() + ".png"));
      }
      int result = jfc.showSaveDialog(CFGPanel.this);
      if (result == JFileChooser.APPROVE_OPTION) {
        File output = jfc.getSelectedFile();
        String type = ((FileNameExtensionFilter) jfc.getFileFilter()).getExtensions()[0];
        BufferedImage image = mxCellRenderer.createBufferedImage(graph, null, 2, null, true, null);
        try {
          ImageIO.write(Images.watermark(image), type, output);
        } catch (IOException ioex) {
          ioex.printStackTrace();
        }
      }
    });
    rightActions.add(save);
    JButton reload = new ReloadButton();
    reload.addActionListener(l -> generateGraph());
    rightActions.add(reload);
    rightActionPanel.add(rightActions);
    JPanel topPanel = new JPanel();
    topPanel.setBorder(new EmptyBorder(3, 5, 0, 1));
    topPanel.setLayout(new BorderLayout());
    topPanel.add(leftActionPanel, BorderLayout.WEST);
    topPanel.add(rightActionPanel, BorderLayout.EAST);
    this.add(topPanel, BorderLayout.NORTH);
    graphComponent = graph.getComponent();
    JPanel inner = new JPanel() {
      @Override
      public Color getBackground() {
        return graphComponent.getBackground();
      }
    };
    inner.setBorder(new EmptyBorder(30, 30, 30, 30));
    inner.setLayout(new BorderLayout(0, 0));
    inner.add(graphComponent, BorderLayout.CENTER);
    OverlayScrollPane overlayScrollPane = new OverlayScrollPane(inner);
    scrollPane = overlayScrollPane.getScrollPane();
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    scrollPane.setBorder(DarkBorders.createLineBorder(1, 1, 1, 1));
    graphComponent.scp = scrollPane;
    addActionBar();
    this.add(overlayScrollPane, BorderLayout.CENTER);
    SwingUtilities.invokeLater(() -> {
      if (mn == null && !cn.methods.isEmpty()) {
        mn = cn.methods.get(0);
        this.generateGraph();
      }
    });
  }

  private void addActionBar() {
    JPanel actionPanel = new JPanel();
    actionPanel.setLayout(new BorderLayout());
    JPanel rightActions = new JPanel();
    rightActions.setLayout(new GridBagLayout());

    GridBagConstraints c = new GridBagConstraints();
    c.weightx = 1.0;
    c.weighty = 1.0;
    c.anchor = GridBagConstraints.EAST;

    rightActions.add(SwingUtils
        .createSlimButton(SwingUtils.getIcon("zoom_reset.svg", true),
          e -> graphComponent.resetZoom()),
      c);
    rightActions.add(SwingUtils
      .createSlimButton(SwingUtils.getIcon("zoom_in.svg", true), e -> graphComponent.zoomIn()), c);
    rightActions.add(SwingUtils
      .createSlimButton(SwingUtils.getIcon("zoom_out.svg", true), e -> graphComponent.zoomOut()), c);
    rightActions.setBorder(new EmptyBorder(2, 0, 0, 0));

    actionPanel.add(rightActions, BorderLayout.EAST);
    actionPanel.setBorder(new EmptyBorder(1, 5, 0, 5));

    this.add(actionPanel, BorderLayout.PAGE_END);
  }

  @Override
  public void updateUI() {
    super.updateUI();
    if (blocks != null) {
      generateGraph();
    }
  }

  public void generateGraph() {
    blocks.clear();
    if (mn.instructions.size() == 0) {
      this.clear();
      return;
    }
    graphComponent.scp = scrollPane;
    Converter c = new Converter(mn);
    try {
      blocks.addAll(c.convert(true, true, true, 2));
    } catch (Exception e) {
      e.printStackTrace();
      this.clear();
      return;
    }
    Object parent = graph.getDefaultParent();
    graph.getModel().beginUpdate();
    try {
      graph.removeCells(graph.getChildCells(graph.getDefaultParent(), true, true));
      existing.clear();
      if (!blocks.isEmpty()) {
        boolean first = true;
        for (Block b : blocks) {
          if (b.getInput().isEmpty() || first) {
            addBlock((mxCell) parent, b, null);
            first = false;
          }
        }
      }
      graph.getView().setScale(1);
      if (useTreeLayout) {
        mxCompactTreeLayout layout = new mxCompactTreeLayout(graph);
        layout.setResetEdges(true);
        layout.setEdgeRouting(true);
        layout.setHorizontal(false);
        layout.setMoveTree(true);
        layout.setUseBoundingBox(true);
        layout.execute(graph.getDefaultParent());
      } else {
        PatchedHierarchicalLayout layout = new PatchedHierarchicalLayout(graph);
        layout.setFineTuning(true);
        layout.setIntraCellSpacing(20d);
        layout.setInterRankCellSpacing(50d);
        layout.setDisableEdgeStyle(true);
        layout.setParallelEdgeSpacing(100d);
        layout.setUseBoundingBox(true);
        layout.execute(graph.getDefaultParent());
      }
    } finally {
      graph.getModel().endUpdate();
    }
    this.revalidate();
    this.repaint();
  }

  private mxCell addBlock(mxCell parent, Block b, BlockVertex input) {
    if (existing.containsKey(b)) {
      mxCell cached = existing.get(b);
      if (input != null) {
        ((BlockVertex) cached.getValue()).addInput(input);
      }
      return cached;
    }
    BlockVertex vertex = new BlockVertex(mn, b, b.getNodes(), b.getLabel(),
      mn.instructions.indexOf(b.getNodes().get(0)));
    if (input != null) {
      vertex.addInput(input);
    }
    mxCell cell = (mxCell) graph.insertVertex(parent, null, vertex, 150, 10, 80, 40,
      String.format("fillColor=%s;fontColor=%s;strokeColor=%s", Strings.hexColor(getBackground().brighter()),
        Strings.hexColor(getForeground().brighter()), Strings.hexColor(getBackground().brighter().brighter())));
    graph.updateCellSize(cell); // resize cell
    existing.put(b, cell);
    assert (cell != null);
    List<Block> next = b.getOutput();
    for (int i = 0; i < next.size(); i++) {
      Block out = next.get(i);
      if (out.equals(b)) {
        graph.insertEdge(parent, null, "Infinite loop", cell, cell,
          "strokeColor=" + getEdgeColor(b, i) + ";fontColor=" + Strings.hexColor(getForeground().brighter()));
      } else {
        mxCell vertexOut = addBlock(parent, out, vertex);
        graph.insertEdge(parent, null, null, cell, vertexOut, "strokeColor=" + getEdgeColor(b, i) + ";");
      }
    }
    return cell;
  }

  private String getEdgeColor(Block b, int i) {
    if (b.endsWithJump()) {
      if (b.getOutput().size() > 1) {
        if (i == 0) {
          return "#009432";
        }
        return "#EA2027";
      }
      return "#FFC312";
    }
    if (b.endsWithSwitch()) {
      if (i == 0) {
        return "#12CBC4";
      }
      return "#9980FA";
    }
    return Strings.hexColor(getForeground().darker());
  }

  public void clear() {
    graph.getModel().beginUpdate();
    graph.removeCells(graph.getChildCells(graph.getDefaultParent(), true, true));
    graph.getModel().endUpdate();
  }
}
