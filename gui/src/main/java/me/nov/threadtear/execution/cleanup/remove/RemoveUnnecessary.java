package me.nov.threadtear.execution.cleanup.remove;

import java.util.*;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import me.nov.threadtear.analysis.rewriter.*;
import me.nov.threadtear.analysis.rewriter.value.CodeReferenceValue;
import me.nov.threadtear.execution.*;
import me.nov.threadtear.util.asm.*;

public class RemoveUnnecessary extends Execution implements ICRReferenceHandler {

  public RemoveUnnecessary() {
    super(ExecutionCategory.CLEANING, "<html><s>Remove " + "unnecessary instructions</s>",
            "Remove " + "unnecessary " + "instructions or flow obfuscation" + " that can be optimized.<br>This could " +
                    "include number or flow " + "obfuscation.<br><b>Do" + " not run this, it is unfinished!</b>");
  }

  /*
   * TODO Nothing done here yet, this class should
   *  simulate stack and simultaneously rewrite the code.
   *
   * eg. ICONST_4 ICONST_1 IADD INVOKESTATIC ...
   *
   * would be turned into
   *
   * ICONST_5 INVOKESTATIC ...
   *
   */

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    logger.info("Simulating stack for every method!");
    classes.values().stream().map(c -> c.node).forEach(this::optimize);
    return false;
  }

  private void optimize(ClassNode cn) {
    cn.methods.forEach(m -> m.instructions = simulateAndRewrite(cn, m));
  }

  private InsnList simulateAndRewrite(ClassNode cn, MethodNode m) {
    m.tryCatchBlocks.clear();
    if (m.localVariables != null)
      m.localVariables.clear();
    CodeAnalyzer a = new CodeAnalyzer(new CodeRewriter(this, Access.isStatic(m.access), m.maxLocals, m.desc));
    try {
      a.analyze(cn.name, m);
    } catch (AnalyzerException e) {
      logger.error("Failed stack analysis in " + cn.name + "." + m.name + ":" + e.getMessage());
      return m.instructions;
    }
    // TODO rewrite this whole thing without analyzer
    Frame<CodeReferenceValue>[] frames = a.getFrames();
    InsnList rewrittenCode = new InsnList();
    Map<LabelNode, LabelNode> labels = Instructions.cloneLabels(m.instructions);
    for (int i = 0; i < m.instructions.size(); i++) {
      if (rewrittenCode.size() > Math.max(25, m.instructions.size() * 4)) {
        logger.error("Code got too huge in " + cn.name + "." + m.name + " -> returning old code");
        return m.instructions;
      }
      AbstractInsnNode ain = m.instructions.get(i);
      Frame<CodeReferenceValue> frame = frames[i];
      if (frame == null) {
        //        rewrittenCode.add(ain.clone(labels));
        continue;
      }
      int stackSize = frame.getStackSize();
      if (ain.getType() == AbstractInsnNode.LABEL) {
        rewrittenCode.add(ain.clone(labels));
      } else {
        // all instructions that remove from the stack
        // take their arguments and rewrite
        switch (ain.getOpcode()) {
          case ATHROW:
          case ARETURN:
          case DRETURN:
          case FRETURN:
          case IRETURN:
          case LRETURN:
            rewrittenCode.add(frame.getStack(stackSize - 1).combine().cloneInstructions());
          case RETURN:
          case GOTO:
            rewrittenCode.add(ain.clone(labels));
            break;
          case MONITORENTER:
          case MONITOREXIT:
          case LOOKUPSWITCH:
          case TABLESWITCH:
          case ISTORE:
          case LSTORE:
          case FSTORE:
          case DSTORE:
          case ASTORE:
          case IFEQ:
          case IFNE:
          case IFNULL:
          case IFNONNULL:
          case IFGT:
          case IFGE:
          case IFLT:
          case IFLE:
            rewrittenCode.add(frame.getStack(stackSize - 1).combine().cloneInstructions());
            rewrittenCode.add(ain.clone(labels));
            break;
          case IASTORE:
          case LASTORE:
          case FASTORE:
          case DASTORE:
          case AASTORE:
          case BASTORE:
          case CASTORE:
          case SASTORE:
            rewrittenCode.add(frame.getStack(stackSize - 3).combine().cloneInstructions()); // array
            rewrittenCode.add(frame.getStack(stackSize - 2).combine().cloneInstructions()); // index
            rewrittenCode.add(frame.getStack(stackSize - 1).combine().cloneInstructions()); // value
            rewrittenCode.add(ain.clone(labels));
            break;
          case PUTFIELD:
            rewrittenCode.add(frame.getStack(stackSize - 2).combine().cloneInstructions()); // object
            // reference
          case PUTSTATIC:
            rewrittenCode.add(frame.getStack(stackSize - 1).combine().cloneInstructions()); // value
            rewrittenCode.add(ain.clone(labels));
            break;
          case POP:
            CodeReferenceValue top = frame.getStack(stackSize - 1);
            if (top.isRequiredInCode()) {
              rewrittenCode.add(top.combine().cloneInstructions());
              rewrittenCode.add(ain.clone(labels));
            }
            break;
          case POP2:
            CodeReferenceValue first = frame.getStack(stackSize - 1);
            if (first.getSize() == 1) {
              CodeReferenceValue second = frame.getStack(stackSize - 2);
              if (second.isRequiredInCode() && first.isRequiredInCode()) {
                rewrittenCode.add(second.combine().cloneInstructions());
                rewrittenCode.add(first.combine().cloneInstructions());
                rewrittenCode.add(ain.clone(labels));
              } else if (second.isRequiredInCode()) {
                rewrittenCode.add(second.combine().cloneInstructions());
                rewrittenCode.add(new InsnNode(POP));
              } else if (first.isRequiredInCode()) {
                rewrittenCode.add(first.combine().cloneInstructions());
                rewrittenCode.add(new InsnNode(POP));
              }
            } else if (first.isRequiredInCode()) {
              rewrittenCode.add(first.combine().cloneInstructions());
              rewrittenCode.add(ain.clone(null));
            }
            break;
          case IF_ICMPEQ:
          case IF_ICMPNE:
          case IF_ICMPLT:
          case IF_ICMPGE:
          case IF_ICMPGT:
          case IF_ICMPLE:
          case IF_ACMPEQ:
          case IF_ACMPNE:
            rewrittenCode.add(frame.getStack(stackSize - 2).combine().cloneInstructions());
            rewrittenCode.add(frame.getStack(stackSize - 1).combine().cloneInstructions());
            rewrittenCode.add(ain.clone(labels));
            break;
          case INVOKEVIRTUAL:
          case INVOKESPECIAL:
          case INVOKEINTERFACE:
            String desc1 = ((MethodInsnNode) ain).desc;
            if (desc1.endsWith(")V")) {
              int parameters = Type.getArgumentTypes(desc1).length + 1; // one for reference
              while (parameters > 0) {
                rewrittenCode.add(frame.getStack(stackSize - (parameters--)).combine().cloneInstructions());
              }
              rewrittenCode.add(ain.clone(labels));
            }
            break;
          case INVOKESTATIC:
            String desc2 = ((MethodInsnNode) ain).desc;
            if (desc2.endsWith(")V")) {
              int parameters = Type.getArgumentTypes(desc2).length;
              while (parameters > 0) {
                rewrittenCode.add(frame.getStack(stackSize - (parameters--)).combine().cloneInstructions());

              }
              rewrittenCode.add(ain.clone(labels));
            }
            break;
          case INVOKEDYNAMIC:
            String desc3 = ((InvokeDynamicInsnNode) ain).desc;
            if (desc3.endsWith(")V")) {
              int parameters = Type.getArgumentTypes(desc3).length;
              while (parameters > 0) {
                rewrittenCode.add(frame.getStack(stackSize - (parameters--)).combine().cloneInstructions());
              }
              rewrittenCode.add(ain.clone(labels));
            }
            break;
        }
      }
    }
    m.tryCatchBlocks.clear();
    // TODO rewrite try catch blocks
    return rewrittenCode;
  }

  public String toString(Frame<CodeReferenceValue> f) {
    StringBuilder stringBuilder = new StringBuilder(" LOCALS: (");
    for (int i = 0; i < f.getLocals(); ++i) {
      stringBuilder.append(f.getLocal(i));
      stringBuilder.append('|');
    }
    stringBuilder.append(") STACK: (");
    for (int i = 0; i < f.getStackSize(); ++i) {
      CodeReferenceValue combined = f.getStack(i).combine();
      stringBuilder
              .append(combined.getStackValueOrNull() != null ? combined.getStackValueOrNull() : combined.toString());
      stringBuilder.append('|');
    }
    stringBuilder.append(')');
    return stringBuilder.toString();
  }

  @Override
  public Object getFieldValueOrNull(BasicValue v, String owner, String name, String desc) {
    return null;
  }

  @Override
  public Object getMethodReturnOrNull(BasicValue v, String owner, String name, String desc,
                                      List<? extends CodeReferenceValue> values) {
    if (name.equals("toCharArray") && owner.equals("java/lang/String")) {
    }
    return null;
  }
}