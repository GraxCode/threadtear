package me.nov.threadtear.analysis.rewriter;

import java.util.*;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.objectweb.asm.tree.analysis.Frame;

import me.nov.threadtear.analysis.Subroutine;
import me.nov.threadtear.analysis.rewriter.value.CodeReferenceValue;
import me.nov.threadtear.analysis.stack.ConstantTracker;

/**
 * Analyzer that uses known stack values to handle known
 * jumps without losing stack values. Only usable with
 * CodeReferenceValue.
 * <p>
 * FIXME sometimes predicting jumps that shouldn't be
 * predicted (sample: org.benf.cfr.reader.bytecode
 * .analysis.parse.expression.AbstractExpression
 * .class:dumpWithOuterPrecedence)
 */
public class CodeAnalyzer implements Opcodes {

  private final Interpreter<CodeReferenceValue> interpreter;

  private InsnList insnList;

  private int insnListSize;

  private List<TryCatchBlockNode>[] handlers;

  private Frame<CodeReferenceValue>[] frames;

  private Subroutine[] subroutines;

  private boolean[] inInstructionsToProcess;

  private int[] instructionsToProcess;

  private int numInstructionsToProcess;

  public CodeAnalyzer(final Interpreter<CodeReferenceValue> interpreter) {
    this.interpreter = interpreter;
  }

  @SuppressWarnings("unchecked")
  public Frame<CodeReferenceValue>[] analyze(final String owner, final MethodNode method) throws AnalyzerException {
    if ((method.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0) {
      frames = (Frame<CodeReferenceValue>[]) new Frame<?>[0];
      return frames;
    }
    insnList = method.instructions;
    insnListSize = insnList.size();
    handlers = (List<TryCatchBlockNode>[]) new List<?>[insnListSize];
    frames = (Frame<CodeReferenceValue>[]) new Frame<?>[insnListSize];
    subroutines = new Subroutine[insnListSize];
    inInstructionsToProcess = new boolean[insnListSize];
    instructionsToProcess = new int[insnListSize];
    numInstructionsToProcess = 0;

    // For each exception handler, and each instruction
    // within its range, record in
    // 'handlers' the
    // fact that execution can flow from this instruction
    // to the exception handler.
    for (int i = 0; i < method.tryCatchBlocks.size(); ++i) {
      TryCatchBlockNode tryCatchBlock = method.tryCatchBlocks.get(i);
      int startIndex = insnList.indexOf(tryCatchBlock.start);
      int endIndex = insnList.indexOf(tryCatchBlock.end);
      for (int j = startIndex; j < endIndex; ++j) {
        List<TryCatchBlockNode> insnHandlers = handlers[j];
        if (insnHandlers == null) {
          insnHandlers = new ArrayList<>();
          handlers[j] = insnHandlers;
        }
        insnHandlers.add(tryCatchBlock);
      }
    }

    // For each instruction, compute the subroutine to
    // which it belongs.
    // Follow the main 'subroutine', and collect the jsr
    // instructions to nested
    // subroutines.
    Subroutine main = new Subroutine(null, method.maxLocals, null);
    List<AbstractInsnNode> jsrInsns = new ArrayList<>();
    findSubroutine(0, main, jsrInsns);
    // Follow the nested subroutines, and collect their
    // own nested subroutines,
    // until all
    // subroutines are found.
    Map<LabelNode, Subroutine> jsrSubroutines = new HashMap<>();
    while (!jsrInsns.isEmpty()) {
      JumpInsnNode jsrInsn = (JumpInsnNode) jsrInsns.remove(0);
      Subroutine subroutine = jsrSubroutines.get(jsrInsn.label);
      if (subroutine == null) {
        subroutine = new Subroutine(jsrInsn.label, method.maxLocals, jsrInsn);
        jsrSubroutines.put(jsrInsn.label, subroutine);
        findSubroutine(insnList.indexOf(jsrInsn.label), subroutine, jsrInsns);
      } else {
        subroutine.callers.add(jsrInsn);
      }
    }
    // Clear the main 'subroutine', which is not a real
    // subroutine (and was used
    // only as an
    // intermediate step above to find the real ones).
    for (int i = 0; i < insnListSize; ++i) {
      if (subroutines[i] != null && subroutines[i].start == null) {
        subroutines[i] = null;
      }
    }

    // Initializes the data structures for the control
    // flow analysis.
    Frame<CodeReferenceValue> currentFrame = computeInitialFrame(owner, method);
    merge(0, currentFrame, null);
    init(owner, method);

    // Control flow analysis.
    while (numInstructionsToProcess > 0) {
      // Get and remove one instruction from the list of
      // instructions to process.
      int insnIndex = instructionsToProcess[--numInstructionsToProcess];
      Frame<CodeReferenceValue> oldFrame = frames[insnIndex];
      Subroutine subroutine = subroutines[insnIndex];
      inInstructionsToProcess[insnIndex] = false;

      // Simulate the execution of this instruction.
      AbstractInsnNode insnNode = null;
      try {
        insnNode = method.instructions.get(insnIndex);
        int insnOpcode = insnNode.getOpcode();
        int insnType = insnNode.getType();

        if (insnType == AbstractInsnNode.LABEL || insnType == AbstractInsnNode.LINE ||
                insnType == AbstractInsnNode.FRAME) {
          merge(insnIndex + 1, oldFrame, subroutine);
          newControlFlowEdge(insnIndex, insnIndex + 1);
        } else {
          // XXX here the hack happens
          int jump = predictJump(currentFrame, insnNode.getOpcode()); // predict possible jump
          // outcome before popping off stack in frame
          // execution
          currentFrame.init(oldFrame).execute(insnNode, interpreter);
          subroutine = subroutine == null ? null : new Subroutine(subroutine);

          if (insnNode instanceof JumpInsnNode) {
            JumpInsnNode jumpInsn = (JumpInsnNode) insnNode;
            if (jump == 1) {
              // jump predicted
              int jumpInsnIndex = insnList.indexOf(jumpInsn.label);
              currentFrame.initJumpTarget(insnOpcode, jumpInsn.label);
              merge(jumpInsnIndex, currentFrame, subroutine);
              newControlFlowEdge(insnIndex, jumpInsnIndex);
            } else if (jump == -1) {
              // no jump predicted, move on with next node
              currentFrame.initJumpTarget(insnOpcode, null);
              merge(insnIndex + 1, currentFrame, subroutine);
              newControlFlowEdge(insnIndex, insnIndex + 1);
            } else {
              // no known outcome, move on normally
              if (insnOpcode != GOTO && insnOpcode != JSR) {
                currentFrame.initJumpTarget(insnOpcode, null);
                merge(insnIndex + 1, currentFrame, subroutine);
                newControlFlowEdge(insnIndex, insnIndex + 1);
              }
              int jumpInsnIndex = insnList.indexOf(jumpInsn.label);
              currentFrame.initJumpTarget(insnOpcode, jumpInsn.label);
              if (insnOpcode == JSR) {
                merge(jumpInsnIndex, currentFrame, new Subroutine(jumpInsn.label, method.maxLocals, jumpInsn));
              } else {
                merge(jumpInsnIndex, currentFrame, subroutine);
              }
              newControlFlowEdge(insnIndex, jumpInsnIndex);
            }
          } else if (insnNode instanceof LookupSwitchInsnNode) {
            LookupSwitchInsnNode lookupSwitchInsn = (LookupSwitchInsnNode) insnNode;
            int targetInsnIndex = insnList.indexOf(lookupSwitchInsn.dflt);
            currentFrame.initJumpTarget(insnOpcode, lookupSwitchInsn.dflt);
            merge(targetInsnIndex, currentFrame, subroutine);
            newControlFlowEdge(insnIndex, targetInsnIndex);
            for (int i = 0; i < lookupSwitchInsn.labels.size(); ++i) {
              LabelNode label = lookupSwitchInsn.labels.get(i);
              targetInsnIndex = insnList.indexOf(label);
              currentFrame.initJumpTarget(insnOpcode, label);
              merge(targetInsnIndex, currentFrame, subroutine);
              newControlFlowEdge(insnIndex, targetInsnIndex);
            }
          } else if (insnNode instanceof TableSwitchInsnNode) {
            TableSwitchInsnNode tableSwitchInsn = (TableSwitchInsnNode) insnNode;
            int targetInsnIndex = insnList.indexOf(tableSwitchInsn.dflt);
            currentFrame.initJumpTarget(insnOpcode, tableSwitchInsn.dflt);
            merge(targetInsnIndex, currentFrame, subroutine);
            newControlFlowEdge(insnIndex, targetInsnIndex);
            for (int i = 0; i < tableSwitchInsn.labels.size(); ++i) {
              LabelNode label = tableSwitchInsn.labels.get(i);
              currentFrame.initJumpTarget(insnOpcode, label);
              targetInsnIndex = insnList.indexOf(label);
              merge(targetInsnIndex, currentFrame, subroutine);
              newControlFlowEdge(insnIndex, targetInsnIndex);
            }
          } else if (insnOpcode == RET) {
            if (subroutine == null) {
              throw new AnalyzerException(insnNode, "RET instruction outside of a subroutine");
            }
            for (int i = 0; i < subroutine.callers.size(); ++i) {
              JumpInsnNode caller = subroutine.callers.get(i);
              int jsrInsnIndex = insnList.indexOf(caller);
              if (frames[jsrInsnIndex] != null) {
                merge(jsrInsnIndex + 1, frames[jsrInsnIndex], currentFrame, subroutines[jsrInsnIndex],
                        subroutine.localsUsed);
                newControlFlowEdge(insnIndex, jsrInsnIndex + 1);
              }
            }
          } else if (insnOpcode != ATHROW && (insnOpcode < IRETURN || insnOpcode > RETURN)) {
            if (subroutine != null) {
              if (insnNode instanceof VarInsnNode) {
                int var = ((VarInsnNode) insnNode).var;
                subroutine.localsUsed[var] = true;
                if (insnOpcode == LLOAD || insnOpcode == DLOAD || insnOpcode == LSTORE || insnOpcode == DSTORE) {
                  subroutine.localsUsed[var + 1] = true;
                }
              } else if (insnNode instanceof IincInsnNode) {
                int var = ((IincInsnNode) insnNode).var;
                subroutine.localsUsed[var] = true;
              }
            }
            merge(insnIndex + 1, currentFrame, subroutine);
            newControlFlowEdge(insnIndex, insnIndex + 1);
          }
        }

        List<TryCatchBlockNode> insnHandlers = handlers[insnIndex];
        if (insnHandlers != null) {
          for (TryCatchBlockNode tryCatchBlock : insnHandlers) {
            Type catchType;
            if (tryCatchBlock.type == null) {
              catchType = Type.getObjectType("java/lang/Throwable");
            } else {
              catchType = Type.getObjectType(tryCatchBlock.type);
            }
            if (newControlFlowExceptionEdge(insnIndex, tryCatchBlock)) {
              Frame<CodeReferenceValue> handler = newFrame(oldFrame);
              handler.clearStack();
              handler.push(interpreter.newExceptionValue(tryCatchBlock, handler, catchType));
              merge(insnList.indexOf(tryCatchBlock.handler), handler, subroutine);
            }
          }
        }
      } catch (AnalyzerException e) {
        throw new AnalyzerException(e.node, "Error at instruction " + insnIndex + ": " + e.getMessage(), e);
      } catch (RuntimeException e) {
        // DontCheck(IllegalCatch): can't be fixed, for
        // backward compatibility.
        throw new AnalyzerException(insnNode, "Error at instruction " + insnIndex + ": " + e.getMessage(), e);
      }
    }

    return frames;
  }

  /**
   * XXX this predicts the outcome of a jump by knowing
   * top frame values
   *
   * @param frame
   * @param op
   * @return
   */
  private int predictJump(Frame<CodeReferenceValue> frame, int op) {
    if (frame.getStackSize() == 0)
      return 0;
    CodeReferenceValue up = frame.getStack(frame.getStackSize() - 1);
    if (!up.isKnownValue())
      return 0;
    Object upperVal = up.getStackValueOrNull();
    switch (op) {
      case IFEQ:
        return ((Integer) upperVal) == 0 ? 1 : -1;
      case IFNE:
        return ((Integer) upperVal) != 0 ? 1 : -1;
      case IFNULL:
        return upperVal.equals(ConstantTracker.NULL) ? 1 : -1;
      case IFNONNULL:
        return !upperVal.equals(ConstantTracker.NULL) ? 1 : -1;
      case IFGT:
        return ((Integer) upperVal) > 0 ? 1 : -1;
      case IFGE:
        return ((Integer) upperVal) >= 0 ? 1 : -1;
      case IFLT:
        return ((Integer) upperVal) < 0 ? 1 : -1;
      case IFLE:
        return ((Integer) upperVal) <= 0 ? 1 : -1;
    }
    if (frame.getStackSize() >= 2) {
      CodeReferenceValue low = frame.getStack(frame.getStackSize() - 2);
      if (!low.isKnownValue())
        return 0;
      Object lowerVal = low.getStackValueOrNull();
      switch (op) {
        case IF_ICMPEQ:
          return upperVal.equals(lowerVal) ? 1 : -1;
        case IF_ICMPNE:
          return upperVal.equals(lowerVal) ? -1 : 1;
        case IF_ICMPLT:
          return (Integer) lowerVal < (Integer) upperVal ? 1 : -1;
        case IF_ICMPGE:
          return (Integer) lowerVal >= (Integer) upperVal ? 1 : -1;
        case IF_ICMPGT:
          return (Integer) lowerVal > (Integer) upperVal ? 1 : -1;
        case IF_ICMPLE:
          return (Integer) lowerVal <= (Integer) upperVal ? 1 : -1;
        case IF_ACMPEQ:
          return up.equals(low) ? 1 : -1;
        case IF_ACMPNE:
          return !up.equals(low) ? 1 : -1;
      }
    }
    return 0;

  }

  private void findSubroutine(final int insnIndex, final Subroutine subroutine, final List<AbstractInsnNode> jsrInsns)
          throws AnalyzerException {
    ArrayList<Integer> instructionIndicesToProcess = new ArrayList<>();
    instructionIndicesToProcess.add(insnIndex);
    while (!instructionIndicesToProcess.isEmpty()) {
      int currentInsnIndex = instructionIndicesToProcess.remove(instructionIndicesToProcess.size() - 1);
      if (currentInsnIndex < 0 || currentInsnIndex >= insnListSize) {
        throw new AnalyzerException(null, "Execution can fall off the end of the code");
      }
      if (subroutines[currentInsnIndex] != null) {
        continue;
      }
      subroutines[currentInsnIndex] = new Subroutine(subroutine);
      AbstractInsnNode currentInsn = insnList.get(currentInsnIndex);

      // Push the normal successors of currentInsn onto
      // instructionIndicesToProcess.
      if (currentInsn instanceof JumpInsnNode) {
        if (currentInsn.getOpcode() == JSR) {
          // Do not follow a jsr, it leads to another
          // subroutine!
          jsrInsns.add(currentInsn);
        } else {
          JumpInsnNode jumpInsn = (JumpInsnNode) currentInsn;
          instructionIndicesToProcess.add(insnList.indexOf(jumpInsn.label));
        }
      } else if (currentInsn instanceof TableSwitchInsnNode) {
        TableSwitchInsnNode tableSwitchInsn = (TableSwitchInsnNode) currentInsn;
        findSubroutine(insnList.indexOf(tableSwitchInsn.dflt), subroutine, jsrInsns);
        for (int i = tableSwitchInsn.labels.size() - 1; i >= 0; --i) {
          LabelNode labelNode = tableSwitchInsn.labels.get(i);
          instructionIndicesToProcess.add(insnList.indexOf(labelNode));
        }
      } else if (currentInsn instanceof LookupSwitchInsnNode) {
        LookupSwitchInsnNode lookupSwitchInsn = (LookupSwitchInsnNode) currentInsn;
        findSubroutine(insnList.indexOf(lookupSwitchInsn.dflt), subroutine, jsrInsns);
        for (int i = lookupSwitchInsn.labels.size() - 1; i >= 0; --i) {
          LabelNode labelNode = lookupSwitchInsn.labels.get(i);
          instructionIndicesToProcess.add(insnList.indexOf(labelNode));
        }
      }

      // Push the exception handler successors of
      // currentInsn onto
      // instructionIndicesToProcess.
      List<TryCatchBlockNode> insnHandlers = handlers[currentInsnIndex];
      if (insnHandlers != null) {
        for (TryCatchBlockNode tryCatchBlock : insnHandlers) {
          instructionIndicesToProcess.add(insnList.indexOf(tryCatchBlock.handler));
        }
      }

      // Push the next instruction, if the control flow
      // can go from currentInsn to the
      // next.
      switch (currentInsn.getOpcode()) {
        case GOTO:
        case RET:
        case TABLESWITCH:
        case LOOKUPSWITCH:
        case IRETURN:
        case LRETURN:
        case FRETURN:
        case DRETURN:
        case ARETURN:
        case RETURN:
        case ATHROW:
          break;
        default:
          instructionIndicesToProcess.add(currentInsnIndex + 1);
          break;
      }
    }
  }

  private Frame<CodeReferenceValue> computeInitialFrame(final String owner, final MethodNode method) {
    Frame<CodeReferenceValue> frame = newFrame(method.maxLocals, method.maxStack);
    int currentLocal = 0;
    boolean isInstanceMethod = (method.access & ACC_STATIC) == 0;
    if (isInstanceMethod) {
      Type ownerType = Type.getObjectType(owner);
      frame.setLocal(currentLocal, interpreter.newParameterValue(isInstanceMethod, currentLocal, ownerType));
      currentLocal++;
    }
    Type[] argumentTypes = Type.getArgumentTypes(method.desc);
    for (Type argumentType : argumentTypes) {
      frame.setLocal(currentLocal, interpreter.newParameterValue(isInstanceMethod, currentLocal, argumentType));
      currentLocal++;
      if (argumentType.getSize() == 2) {
        frame.setLocal(currentLocal, interpreter.newEmptyValue(currentLocal));
        currentLocal++;
      }
    }
    while (currentLocal < method.maxLocals) {
      frame.setLocal(currentLocal, interpreter.newEmptyValue(currentLocal));
      currentLocal++;
    }
    frame.setReturn(interpreter.newReturnTypeValue(Type.getReturnType(method.desc)));
    return frame;
  }

  public Frame<CodeReferenceValue>[] getFrames() {
    return frames;
  }

  public List<TryCatchBlockNode> getHandlers(final int insnIndex) {
    return handlers[insnIndex];
  }

  protected void init(final String owner, final MethodNode method) throws AnalyzerException {
    // Nothing to do.
  }

  protected Frame<CodeReferenceValue> newFrame(final int numLocals, final int numStack) {
    return new Frame<>(numLocals, numStack);
  }

  protected Frame<CodeReferenceValue> newFrame(final Frame<CodeReferenceValue> frame) {
    return new Frame<>(frame);
  }

  protected void newControlFlowEdge(final int insnIndex, final int successorIndex) {
    // Nothing to do.
  }

  protected boolean newControlFlowExceptionEdge(final int insnIndex, final int successorIndex) {
    return true;
  }

  protected boolean newControlFlowExceptionEdge(final int insnIndex, final TryCatchBlockNode tryCatchBlock) {
    return newControlFlowExceptionEdge(insnIndex, insnList.indexOf(tryCatchBlock.handler));
  }

  // -----------------------------------------------------------------------------------------------

  private void merge(final int insnIndex, final Frame<CodeReferenceValue> frame, final Subroutine subroutine)
          throws AnalyzerException {
    boolean changed;
    Frame<CodeReferenceValue> oldFrame = frames[insnIndex];
    if (oldFrame == null) {
      frames[insnIndex] = newFrame(frame);
      changed = true;
    } else {
      changed = oldFrame.merge(frame, interpreter);
    }
    Subroutine oldSubroutine = subroutines[insnIndex];
    if (oldSubroutine == null) {
      if (subroutine != null) {
        subroutines[insnIndex] = new Subroutine(subroutine);
        changed = true;
      }
    } else {
      if (subroutine != null) {
        changed |= oldSubroutine.merge(subroutine);
      }
    }
    if (changed && !inInstructionsToProcess[insnIndex]) {
      inInstructionsToProcess[insnIndex] = true;
      instructionsToProcess[numInstructionsToProcess++] = insnIndex;
    }
  }

  private void merge(final int insnIndex, final Frame<CodeReferenceValue> frameBeforeJsr,
                     final Frame<CodeReferenceValue> frameAfterRet, final Subroutine subroutineBeforeJsr,
                     final boolean[] localsUsed) throws AnalyzerException {
    frameAfterRet.merge(frameBeforeJsr, localsUsed);

    boolean changed;
    Frame<CodeReferenceValue> oldFrame = frames[insnIndex];
    if (oldFrame == null) {
      frames[insnIndex] = newFrame(frameAfterRet);
      changed = true;
    } else {
      changed = oldFrame.merge(frameAfterRet, interpreter);
    }
    Subroutine oldSubroutine = subroutines[insnIndex];
    if (oldSubroutine != null && subroutineBeforeJsr != null) {
      changed |= oldSubroutine.merge(subroutineBeforeJsr);
    }
    if (changed && !inInstructionsToProcess[insnIndex]) {
      inInstructionsToProcess[insnIndex] = true;
      instructionsToProcess[numInstructionsToProcess++] = insnIndex;
    }
  }
}
