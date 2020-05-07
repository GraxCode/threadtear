package me.nov.threadtear.swing.tree.component;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import me.nov.threadtear.io.Clazz;

public class SortedTreeClassNode extends DefaultMutableTreeNode implements Comparator<SortedTreeClassNode> {
  private static final long serialVersionUID = 1L;

  public Clazz member;
  private String text;

  public SortedTreeClassNode(Clazz clazz) {
    this.member = clazz;
    updateClassName();
  }

  public SortedTreeClassNode(String pckg) {
    this.member = null;
    this.text = pckg;
  }

  public void updateClassName() {
    if (member != null) {
      String[] split = member.node.name.split("/");
      if (member.transform) {
        this.text = split[split.length - 1];
      } else {
        this.text = "<html><strike>" + split[split.length - 1];
      }
    }
  }

  @SuppressWarnings("unchecked")
  public void sort() {
    if (children != null)
      Collections.sort(children, this);
  }

  @Override
  public String toString() {
    return text;
  }

  @SuppressWarnings("unchecked")
  public void combinePackage(SortedTreeClassNode pckg) {
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
    new ArrayList<>(pckg.children).forEach(m -> this.add((SortedTreeClassNode) m));
  }

  @Override
  public int compare(SortedTreeClassNode node1, SortedTreeClassNode node2) {
    boolean leaf1 = node1.member != null;
    boolean leaf2 = node2.member != null;

    if (leaf1 != leaf2) {
      return leaf1 ? 1 : -1;
    }
    return node1.text.compareTo(node2.text);
  }
}