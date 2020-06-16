package me.nov.threadtear.util;

import java.util.stream.IntStream;

import org.objectweb.asm.tree.*;

public final class Counting {
  private Counting() {
  }

  public static double percentOf(int nodeType, InsnList instructions, int... searchFor) {
    AbstractInsnNode insn = instructions.getFirst();
    int count = 0;
    int typeCount = 0;
    while (insn != null) {
      int type = insn.getType();
      if (type == nodeType) {
        typeCount++;
      } else if (IntStream.of(searchFor).anyMatch(x -> x == type)) {
        count++;
      }
      insn = insn.getNext();
    }
    return count > 0 ? (typeCount / (double) count) : 0.0;
  }

  public static double count(InsnList instructions, int... searchFor) {
    AbstractInsnNode insn = instructions.getFirst();
    int count = 0;
    while (insn != null) {
      int type = insn.getType();
      if (IntStream.of(searchFor).anyMatch(x -> x == type)) {
        count++;
      }
      insn = insn.getNext();
    }
    return count;
  }

  public static double countOp(InsnList instructions, int... searchFor) {
    AbstractInsnNode insn = instructions.getFirst();
    int count = 0;
    while (insn != null) {
      int op = insn.getOpcode();
      if (IntStream.of(searchFor).anyMatch(x -> x == op)) {
        count++;
      }
      insn = insn.getNext();
    }
    return count;
  }

  public static boolean hasOp(InsnList instructions, int... ops) {
    AbstractInsnNode insn = instructions.getFirst();
    while (insn != null) {
      int op = insn.getOpcode();
      if (IntStream.of(ops).anyMatch(x -> x == op)) {
        return true;
      }
      insn = insn.getNext();
    }
    return false;
  }
}
