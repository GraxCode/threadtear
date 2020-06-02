package me.nov.threadtear.graph.vertex;

import java.util.*;

import org.objectweb.asm.tree.*;

import me.nov.threadtear.graph.Block;
import me.nov.threadtear.util.format.*;

public class BlockVertex {
  public MethodNode mn;
  public List<AbstractInsnNode> nodes;

  public LabelNode label;
  public int listIndex;

  public Block block;
  public List<BlockVertex> inputBlocks = new ArrayList<>();

  public BlockVertex(MethodNode mn, Block block, List<AbstractInsnNode> nodes, LabelNode label, int listIndex) {
    super();
    this.mn = mn;
    this.block = block;
    this.nodes = nodes;
    this.label = label;
    this.listIndex = listIndex;
  }

  public void addInput(BlockVertex v) {
    if (!inputBlocks.contains(v)) {
      this.inputBlocks.add(v);
    }
  }

  private String text = null;

  @Override
  public String toString() {
    if (text == null) {
      StringBuilder sb = new StringBuilder();
      if (block.getTCB() != null) {
        String type = block.getTCB().type;
        sb.append("<html>");
        sb.append(Html.mono("tcb-handler " + block.getTCBIndex()) + ": (" + (type == null || type.equals("java/lang/Throwable") ? "all throwables" : type.replace('/', '.')) + ")");
        sb.append("\n");
      }
      for (AbstractInsnNode ain : nodes) {
        String str = OpFormat.toString(ain);
        if (!str.trim().isEmpty()) {
          sb.append(str);
          sb.append("\n");
        }
      }
      text = sb.toString();
    }
    return text;
  }
}