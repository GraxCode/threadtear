package me.nov.threadtear.swing.analysis;

import java.util.List;
import java.util.stream.StreamSupport;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import me.nov.threadtear.Threadtear;
import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.util.*;
import me.nov.threadtear.util.format.Strings;

public class InstructionAnalysis implements Opcodes {
  public static void analyze(List<Clazz> classes) {
    Threadtear.logger.debug("Analyzing all instructions...");
    Threadtear.logger.debug("Jumps in proportion to references -> ");
    double jumpPercentage = classes.stream().map(c -> c.node.methods).flatMap(List::stream).mapToDouble(m -> Counting
            .percentOf(AbstractInsnNode.JUMP_INSN, m.instructions, AbstractInsnNode.METHOD_INSN,
                    AbstractInsnNode.FIELD_INSN, AbstractInsnNode.TYPE_INSN))
            .average().orElse(Double.NaN);
    Threadtear.logger.debug(Math.round(jumpPercentage * 10000) / 100.0 + "%");
    Threadtear.logger.debug("Normal proportion is about 11%.");
    Threadtear.logger.debug("A higher value indicates flow " + "obfuscation.");
    Threadtear.logger.debug("----------------------------------------------");

    Threadtear.logger.debug("Average invokedynamics per method -> ");
    double invokedynamics = classes.stream().map(c -> c.node.methods).flatMap(List::stream)
            .mapToDouble(m -> Counting.count(m.instructions, AbstractInsnNode.INVOKE_DYNAMIC_INSN)).average()
            .orElse(Double.NaN);
    Threadtear.logger.debug(Math.round(invokedynamics * 100) / 100.0 + "");
    Threadtear.logger.debug("Normally about 0.0 - 0.4.");
    Threadtear.logger.debug("A higher value indicates reference " + "obfuscation.");
    Threadtear.logger.debug("----------------------------------------------");

    Threadtear.logger.debug("Rare stack operations averagely per " + "method -> ");
    double stackop = classes.stream().map(c -> c.node.methods).flatMap(List::stream)
            .mapToDouble(m -> Counting.countOp(m.instructions, POP2, DUP2, DUP_X1, DUP_X2, DUP2_X1, DUP2_X2, SWAP))
            .average().orElse(Double.NaN);
    Threadtear.logger.debug(Math.round(stackop * 100) / 100.0 + "");
    Threadtear.logger.debug("Normally about 0.0 - 0.1.");
    Threadtear.logger.debug("A higher value indicates flow " + "obfuscation.");
    Threadtear.logger.debug("----------------------------------------------");

    Threadtear.logger.debug("Average standard deviation of letters" + " in strings -> ");
    double sdev = classes.stream().map(c -> c.node.methods).flatMap(List::stream).map(m -> m.instructions.spliterator())
            .flatMap(insns -> StreamSupport.stream(insns, false)).filter(ain -> ain
                    .getOpcode() == LDC && ((LdcInsnNode) ain).cst instanceof String && ((LdcInsnNode) ain).cst
                    .toString().length() > 2).mapToDouble(ain -> Strings.calcSdev(((LdcInsnNode) ain).cst.toString()))
            .average().orElse(Double.NaN);
    Threadtear.logger.debug(Math.round(sdev * 100) / 100.0 + "");
    Threadtear.logger.debug("Normally around 15 - 40.");
    Threadtear.logger.debug("A higher value could indicate string " + "obfuscation.");
    Threadtear.logger.debug("----------------------------------------------");

    Threadtear.logger.debug("Percentage of high character value " + "strings -> ");
    double highutf = classes.stream().map(c -> c.node.methods).flatMap(List::stream)
            .map(m -> m.instructions.spliterator()).flatMap(insns -> StreamSupport.stream(insns, false))
            .filter(ain -> ain
                    .getOpcode() == LDC && ((LdcInsnNode) ain).cst instanceof String && ((LdcInsnNode) ain).cst
                    .toString().length() > 2)
            .mapToDouble(ain -> Strings.isHighUTF(((LdcInsnNode) ain).cst.toString()) ? 1 : 0).average()
            .orElse(Double.NaN);
    Threadtear.logger.debug(Math.round(highutf * 10000) / 100.0 + "");
    Threadtear.logger.debug("Normally around 0% - 1%.");
    Threadtear.logger.debug("A higher value could indicate string obfuscation.");
    Threadtear.logger.debug("----------------------------------------------");

    Threadtear.logger.debug("NOP instructions averagely per method -> ");
    double nops = classes.stream().map(c -> c.node.methods).flatMap(List::stream)
            .mapToDouble(m -> Counting.countOp(m.instructions, NOP)).average().orElse(Double.NaN);
    Threadtear.logger.debug(Math.round(nops * 100) / 100.0 + "");
    Threadtear.logger.debug("Normally about 0.0 - 0.1.");
    Threadtear.logger.debug("A higher value indicates unoptimized code.");
    Threadtear.logger.debug("----------------------------------------------");
  }
}
