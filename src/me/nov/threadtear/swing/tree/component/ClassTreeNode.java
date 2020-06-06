package me.nov.threadtear.swing.tree.component;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.util.format.Strings;

public class ClassTreeNode extends DefaultMutableTreeNode implements Comparator<ClassTreeNode> {
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
      String[] split = member.node.name.split("/");
      if (member.transform) {
        this.text = Strings.min(split[split.length - 1], 50);
      } else {
        this.text = "<html><strike>" + Strings.min(split[split.length - 1], 50);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public void sort() {
    if (children != null)
      children.sort(this);
  }

  @Override
  public String toString() {
    return text;
  }

  @SuppressWarnings("unchecked")
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
  public int compare(ClassTreeNode node1, ClassTreeNode node2) {
    boolean leaf1 = node1.member != null;
    boolean leaf2 = node2.member != null;

    if (leaf1 != leaf2) {
      return leaf1 ? 1 : -1;
    }
    return node1.text.compareTo(node2.text);
  }
}