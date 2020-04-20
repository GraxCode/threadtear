package me.nov.threadtear.asm.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.Frame;

import me.nov.threadtear.util.Descriptor;

public class Instructions implements Opcodes {
	public static InsnList copy(InsnList insnList) {
		InsnList copy = new InsnList();
		Map<LabelNode, LabelNode> labels = cloneLabels(insnList);
		for (AbstractInsnNode ain : insnList) {
			copy.add(ain.clone(labels));
		}
		return copy;
	}

	public static Map<LabelNode, LabelNode> cloneLabels(InsnList insns) {
		HashMap<LabelNode, LabelNode> labelMap = new HashMap<LabelNode, LabelNode>();
		for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
			if (insn.getType() == AbstractInsnNode.LABEL) {
				labelMap.put((LabelNode) insn, new LabelNode());
			}
		}
		return labelMap;
	}

	public static boolean computable(AbstractInsnNode ain) {
		switch (ain.getType()) {
		case AbstractInsnNode.METHOD_INSN:
		case AbstractInsnNode.FIELD_INSN:
		case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
		case AbstractInsnNode.VAR_INSN:
		case AbstractInsnNode.JUMP_INSN:
			return false;
		default:
			return !isCodeEnd(ain);
		}
	}

	public static boolean isCodeEnd(AbstractInsnNode ain) {
		switch (ain.getOpcode()) {
		case ATHROW:
		case RETURN:
		case ARETURN:
		case DRETURN:
		case FRETURN:
		case IRETURN:
		case LRETURN:
			return true;
		default:
			return false;
		}
	}

	public static boolean unnecessaryToStack(AbstractInsnNode ain) {
		switch (ain.getType()) {
		case AbstractInsnNode.LINE:
		case AbstractInsnNode.FIELD_INSN:
		case AbstractInsnNode.LABEL:
			return false;
		default:
			return true;
		}
	}

	public static boolean removeDeadCode(ClassNode cn, MethodNode mn) {
		Analyzer<?> analyzer = new Analyzer<>(new BasicInterpreter());
		try {
			analyzer.analyze(cn.name, mn);
		} catch (AnalyzerException e) {
			return false;
		}
		Frame<?>[] frames = analyzer.getFrames();
		AbstractInsnNode[] insns = mn.instructions.toArray();
		for (int i = 0; i < frames.length; i++) {
			AbstractInsnNode insn = insns[i];
			if (frames[i] == null && insn.getType() != AbstractInsnNode.LABEL) {
				mn.instructions.remove(insn);
				insns[i] = null;
			}
		}
		return true;
	}

	public static AbstractInsnNode getRealNext(AbstractInsnNode ain) {
		do {
			// skip labels, frames and line numbers
			ain = ain.getNext();
		} while (ain != null && ain.getOpcode() == -1);
		return ain;
	}

	public static AbstractInsnNode getRealPrevious(AbstractInsnNode ain) {
		do {
			// skip labels, frames and line numbers
			ain = ain.getPrevious();
		} while (ain != null && ain.getOpcode() == -1);
		return ain;
	}

	public static boolean isInteger(AbstractInsnNode ain) {
		int op = ain.getOpcode();

		switch (op) {
		case BIPUSH:
		case SIPUSH:
		case ICONST_M1:
		case ICONST_0:
		case ICONST_1:
		case ICONST_2:
		case ICONST_3:
		case ICONST_4:
		case ICONST_5:
			return true;
		}
		if (ain.getType() == AbstractInsnNode.LDC_INSN) {
			return ((LdcInsnNode) ain).cst instanceof Integer;
		}
		return false;
	}

	public static int getIntValue(AbstractInsnNode node) {
		if (node.getOpcode() >= ICONST_M1 && node.getOpcode() <= ICONST_5) {
			return node.getOpcode() - 3; // simple but effective
		}
		if (node.getOpcode() == SIPUSH || node.getOpcode() == BIPUSH) {
			return ((IntInsnNode) node).operand;
		}
		if (node.getType() == AbstractInsnNode.LDC_INSN) {
			return (Integer) ((LdcInsnNode) node).cst;
		}
		throw new IllegalArgumentException("not an int: " + node.getClass().getName());
	}

	public static void isolateCallsThatMatch(MethodNode mn, Predicate<String> p) {
		for (int i = 0; i < mn.instructions.size(); i++) {
			AbstractInsnNode ain = mn.instructions.get(i);
			if (ain.getType() == AbstractInsnNode.METHOD_INSN) {
				MethodInsnNode min = (MethodInsnNode) ain;
				if (p.test(min.owner)) {
					String inner = min.desc.substring(1, min.desc.lastIndexOf(')'));
					String outer = min.desc.substring(min.desc.lastIndexOf(')') + 1);
					// TODO use ASM Type class instead
					for (int size : Descriptor.calculateAmountArguments(inner)) {
						mn.instructions.insertBefore(min, new InsnNode(size > 1 ? POP2 : POP));
						i += 1;
					}
					if (min.getOpcode() != INVOKESTATIC) {
						mn.instructions.insertBefore(min, new InsnNode(POP)); // pop reference
					}
					mn.instructions.set(min, createNullPushForType(outer));
				}
			} else if (ain.getType() == AbstractInsnNode.FIELD_INSN) {
				FieldInsnNode fin = (FieldInsnNode) ain;
				if (p.test(fin.owner)) {
					switch (fin.getOpcode()) {
					case GETFIELD:
						mn.instructions.insertBefore(fin, new InsnNode(POP)); // pop reference
						i += 1;
					case GETSTATIC:
						mn.instructions.set(fin, createNullPushForType(fin.desc));
						break;
					case PUTFIELD:
						mn.instructions.insertBefore(fin, new InsnNode(POP)); // pop reference
						mn.instructions.insertBefore(fin, new InsnNode(Descriptor.getStackSize(fin.desc.charAt(0)) > 1 ? POP2 : POP));
						mn.instructions.set(fin, createNullPushForType(fin.desc));
						i += 2;
						break;
					case PUTSTATIC:
						mn.instructions.insertBefore(fin, new InsnNode(Descriptor.getStackSize(fin.desc.charAt(0)) > 1 ? POP2 : POP));
						mn.instructions.set(fin, createNullPushForType(fin.desc));
						i += 1;
						break;
					}
				}
			} else if (ain.getType() == AbstractInsnNode.TYPE_INSN) {
				TypeInsnNode tin = (TypeInsnNode) ain;
				if (p.test(tin.desc)) {
					switch (tin.getOpcode()) {
					case NEW:
						mn.instructions.set(tin, new InsnNode(ACONST_NULL));
						break;
					case INSTANCEOF:
						mn.instructions.insertBefore(tin, new InsnNode(POP));
						mn.instructions.set(tin, new InsnNode(ICONST_0));
						i += 1;
						break;
					case CHECKCAST:
						mn.instructions.set(tin, new InsnNode(NOP));
						break;
					}
				}
			} else if (ain.getType() == AbstractInsnNode.MULTIANEWARRAY_INSN) {
				MultiANewArrayInsnNode marr = (MultiANewArrayInsnNode) ain;
				if (p.test(marr.desc)) {
					for (int j = 0; j < marr.dims; j++) {
						mn.instructions.insertBefore(marr, new InsnNode(POP));
						i += 1;
					}
					mn.instructions.set(marr, new InsnNode(ACONST_NULL));
				}
			} else if (ain.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN) {
				// TODO fix invokedynamic here
			}
		}
	}

	public static AbstractInsnNode createNullPushForType(String desc) {
		if (desc.length() > 1) {
			// arrays and objects: [* or L*;
			return new InsnNode(ACONST_NULL);
		}
		switch (desc.charAt(0)) {
		case 'V':
			return new InsnNode(NOP);
		case 'D':
			return new InsnNode(DCONST_0);
		case 'F':
			return new InsnNode(FCONST_0);
		case 'J':
			return new InsnNode(LCONST_0);
		default:
			return new InsnNode(ICONST_0);
		}
	}

	public static boolean matchOpcodes(InsnList list1, InsnList list2) {
		if (list1.size() != list2.size())
			return false;
		for (int i = 0; i < list1.size(); i++) {
			AbstractInsnNode ain1 = list1.get(i);
			AbstractInsnNode ain2 = list2.get(i);
			if (ain1.getOpcode() != ain2.getOpcode()) {
				return false;
			}
		}
		return true;
	}

	public static void updateInstructions(MethodNode m, Map<LabelNode, LabelNode> labels, InsnList rewrittenCode) {
		m.instructions.clear();
		m.instructions = rewrittenCode;
		m.tryCatchBlocks.forEach(tcb -> {
			tcb.start = labels.get(tcb.start);
			tcb.end = labels.get(tcb.end);
			tcb.handler = labels.get(tcb.handler);
		});
		if (m.localVariables != null) {
			m.localVariables.forEach(lv -> {
				lv.start = labels.get(lv.start);
				lv.end = labels.get(lv.end);
			});
		}
		m.visibleLocalVariableAnnotations = null;
		m.invisibleLocalVariableAnnotations = null;
	}

	public static InsnList singleton(AbstractInsnNode ain) {
		InsnList list = new InsnList();
		list.add(ain);
		return list;
	}
}
