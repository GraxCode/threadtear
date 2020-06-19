package me.nov.threadtear.swing.tree.component;

import java.util.*;

import javax.swing.tree.*;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.util.format.Html;
import me.nov.threadtear.util.format.Strings;

public class ClassTreeNode extends DefaultMutableTreeNode implements Comparator<TreeNode> {
  private static final long serialVersionUID = 1L;

  public Clazz member;
  private String text;

  public ClassTreeNode(Clazz clazz) {
    this.member = clazz;
    updateClassName();
  }

  public ClassTreeNode(String pckg) {
    this.member = null;
    this.text = pckg;
  }

  public void updateClassName() {
    if (member != null) {
      String topName = getTopName();
      if (member.transform) {
        this.text = "<html>" + topName;
      } else {
        this.text = "<html><strike>" + topName;
      }
    }
  }

  private String getTopName() {
    String[] split = member.node.name.split("/");
    String topName = Strings.min(split[split.length - 1], 50);
    topName += " <font size=-2>" + Html.color("#666666", member.getMetadataString());
    return topName;
  }

  public void sort() {
    if (children != null)
      children.sort(this);
  }

  @Override
  public String toString() {
    return text;
  }

  public void combinePackage(ClassTreeNode pckg) {
    if (pckg.member != null)
      throw new IllegalArgumentException("cannot merge package with file");
    if (pckg == this)
      throw new IllegalArgumentException("cannot merge itself");
    if (!children.contains(pckg))
      throw new IllegalArgumentException("package is not a child");
    if (this.getChildCount() != 1)
      throw new IllegalArgumentException("child count over 1");
    text += "." + pckg.text; // combine package names
    this.removeAllChildren(); // remove old package

    // to avoid dirty OOB exceptions
    new ArrayList<>(pckg.children).forEach(m -> this.add((ClassTreeNode) m));
  }

  @Override
  public int compare(TreeNode node1, TreeNode node2) {
    boolean leaf1 = ((ClassTreeNode) node1).member != null;
    boolean leaf2 = ((ClassTreeNode) node2).member != null;

    if (leaf1 != leaf2) {
      return leaf1 ? 1 : -1;
    }
    return ((ClassTreeNode) node1).text.compareTo(((ClassTreeNode) node2).text);
  }
}
