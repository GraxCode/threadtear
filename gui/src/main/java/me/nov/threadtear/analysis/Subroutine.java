package me.nov.threadtear.analysis;

import java.util.*;

import org.objectweb.asm.tree.*;

/**
 * A method subroutine (corresponds to a JSR instruction).
 *
 * @author Eric Bruneton
 */
public final class Subroutine {

  /**
   * The start of this subroutine.
   */
  public final LabelNode start;

  /**
   * The local variables that are read or written by this
   * subroutine. The i-th element is true if and only if
   * the local variable at index i is read or written by
   * this subroutine.
   */
  public final boolean[] localsUsed;

  /**
   * The JSR instructions that jump to this subroutine.
   */
  public final List<JumpInsnNode> callers;

  /**
   * Constructs a new {@link Subroutine}.
   *
   * @param start     the start of this subroutine.
   * @param maxLocals the local variables that are read
   *                  or written by this subroutine.
   * @param caller    a JSR instruction that jump to this
   *                  subroutine.
   */
  public Subroutine(final LabelNode start, final int maxLocals, final JumpInsnNode caller) {
    this.start = start;
    this.localsUsed = new boolean[maxLocals];
    this.callers = new ArrayList<>();
    callers.add(caller);
  }

  /**
   * Constructs a copy of the given {@link Subroutine}.
   *
   * @param subroutine the subroutine to copy.
   */
  public Subroutine(final Subroutine subroutine) {
    this.start = subroutine.start;
    this.localsUsed = subroutine.localsUsed.clone();
    this.callers = new ArrayList<>(subroutine.callers);
  }

  /**
   * Merges the given subroutine into this subroutine.
   * The local variables read or written by the given
   * subroutine are marked as read or written by this
   * one, and the callers of the
   * given subroutine are added as callers of this one
   * (if both have the same start).
   *
   * @param subroutine another subroutine. This
   *                   subroutine is left unchanged by
   *                   this method.
   * @return whether this subroutine has been modified by
   * this method.
   */
  public boolean merge(final Subroutine subroutine) {
    boolean changed = false;
    for (int i = 0; i < localsUsed.length; ++i) {
      if (subroutine.localsUsed[i] && !localsUsed[i]) {
        localsUsed[i] = true;
        changed = true;
      }
    }
    if (subroutine.start == start) {
      for (int i = 0; i < subroutine.callers.size(); ++i) {
        JumpInsnNode caller = subroutine.callers.get(i);
        if (!callers.contains(caller)) {
          callers.add(caller);
          changed = true;
        }
      }
    }
    return changed;
  }
}
