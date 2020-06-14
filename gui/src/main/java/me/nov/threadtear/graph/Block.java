package me.nov.threadtear.graph;

import java.io.Serializable;
import java.util.*;

import org.objectweb.asm.tree.*;

public class Block implements Serializable {
  private static final long serialVersionUID = -5027761519661700375L;

  private List<Block> output = new ArrayList<>();
  private List<Block> input = new ArrayList<>();

  private AbstractInsnNode endNode;
  private List<AbstractInsnNode> nodes = new ArrayList<>();

  private LabelNode label;

  private List<Block> surroundingBlocks = new ArrayList<>();
  private int depth;

  private TryCatchBlockNode tcb;
  private int tcbIndex = -1;

  public Block() {
    super();
  }

  public AbstractInsnNode getEndNode() {
    return endNode;
  }

  public void setEndNode(AbstractInsnNode endNode) {
    this.endNode = endNode;
  }

  public List<Block> getOutput() {
    return output;
  }

  public void setOutput(List<Block> output) {
    this.output = output;
  }

  public List<Block> getInput() {
    return input;
  }

  public void setInput(List<Block> input) {
    this.input = input;
  }

  public LabelNode getLabel() {
    return label;
  }

  public void setLabel(LabelNode label) {
    this.label = label;
  }

  public List<AbstractInsnNode> getNodes() {
    return nodes;
  }

  public void setNodes(List<AbstractInsnNode> nodes) {
    this.nodes = nodes;
  }

  public List<Block> getSurroundingBlocks() {
    return surroundingBlocks;
  }

  public void setSurroundingBlocks(List<Block> surroundingBlocks) {
    this.surroundingBlocks = surroundingBlocks;
  }

  public int getDepth() {
    return depth;
  }

  public void setDepth(int depth) {
    this.depth = depth;
  }

  public boolean endsWithJump() {
    return endNode instanceof JumpInsnNode;
  }

  public boolean endsWithSwitch() {
    return endNode instanceof TableSwitchInsnNode || endNode instanceof LookupSwitchInsnNode;
  }

  public void setTCB(TryCatchBlockNode tcb, int index) {
    this.tcb = tcb;
    this.tcbIndex = index;
  }

  public TryCatchBlockNode getTCB() {
    return tcb;
  }

  public int getTCBIndex() {
    return tcbIndex;
  }
}
