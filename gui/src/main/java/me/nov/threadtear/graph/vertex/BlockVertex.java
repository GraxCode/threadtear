package me.nov.threadtear.graph.vertex;

import java.io.Serializable;
import java.util.*;

import org.objectweb.asm.tree.*;

import me.nov.threadtear.graph.Block;
import me.nov.threadtear.util.format.*;

public class BlockVertex implements Serializable {
  private static final long serialVersionUID = -5186897790572902164L;
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
        TryCatchBlockNode tcb = block.getTCB();
        String type = tcb.type;
        sb.append("<html>");
        sb.append(Html.mono(
                "tcb-handler " + OpFormat.labelToString(tcb.start) + " to " + OpFormat.labelToString(tcb.end) + " - N" +
                        block.getTCBIndex()));
        sb.append(": (");
        sb.append((type == null || type.equals("java/lang/Throwable") ? "catch all" : type.replace('/', '.')));
        sb.append(")");
        sb.append("\n");
      }
      if (block.getNodes().get(0).getPrevious() == null) {
        sb.append(Html.mono("entry-point:"));
        sb.append("\n");
      }
      for (AbstractInsnNode ain : nodes) {
        String str = OpFormat.toHtmlString(ain);
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
