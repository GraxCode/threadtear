package me.nov.threadtear.execution.cleanup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.StreamSupport;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.util.Access;
import me.nov.threadtear.asm.util.Instructions;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;

public class InlineMethods extends Execution {

	public InlineMethods() {
		super(ExecutionCategory.CLEANING, "Inline static methods without invocation",
				"Inline static methods that only return or throw.<br>Can be useful for deobfuscating try catch block obfuscation.", ExecutionTag.RUNNABLE);
	}

	public int inlines;

	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose, boolean ignoreErr) {
		HashMap<String, MethodNode> map = new HashMap<>();
		classes.stream().map(c -> c.node).forEach(c -> {
			c.methods.stream().filter(this::isUnnecessary).forEach(m -> map.put(c.name + "." + m.name + m.desc, m));
		});
		logger.info(map.size() + " unnecessary methods found that could be inlined");
		inlines = 0;
		classes.stream().map(c -> c.node.methods).flatMap(List::stream).forEach(m -> {
			m.instructions.forEach(ain -> {
				if (ain.getOpcode() == INVOKESTATIC) { // can't inline invokevirtual / special as object could only be superclass and real overrides
					MethodInsnNode min = (MethodInsnNode) ain;
					String key = min.owner + "." + min.name + min.desc;
					if (map.containsKey(key)) {
						inlineMethod(m, min, map.get(key));
						inlines++;
					}
				}
			});
		});
		logger.info("Inlined " + inlines + " references!");
		return true;
	}

	private void inlineMethod(MethodNode m, MethodInsnNode min, MethodNode method) {
		InsnList copy = Instructions.copy(method.instructions);
		StreamSupport.stream(copy.spliterator(), false)
				.filter(ain -> ain.getType() == AbstractInsnNode.LINE || ain.getType() == AbstractInsnNode.FRAME)
				.forEach(copy::remove);
		removeReturn(copy);

		InsnList fakeVarList = createFakeVarList(method);
		copy.insert(fakeVarList);

		StreamSupport.stream(copy.spliterator(), false).filter(ain -> ain.getType() == AbstractInsnNode.VAR_INSN)
				.map(ain -> (VarInsnNode) ain).forEach(v -> v.var += m.maxLocals + 4); // offset local variables to not collide with existing ones
		m.instructions.insert(min, copy);
		m.instructions.remove(min);
	}

	private InsnList createFakeVarList(MethodNode m) {
		InsnList fakeVarList = new InsnList();

		LinkedHashMap<Integer, Integer> varTypes = getVarsAndTypesForDesc(m.desc.substring(1, m.desc.indexOf(')')));
		for (int var : varTypes.keySet()) {
			fakeVarList.insert(new VarInsnNode(varTypes.get(var), var)); // make sure its reversed
		}
		/**
		 * pop object here for non static invoke: fakeVarList.add(new InsnNode(POP));
		 */
		return fakeVarList;
	}

	public static LinkedHashMap<Integer, Integer> getVarsAndTypesForDesc(String rawType) {
		LinkedHashMap<Integer, Integer> map = new LinkedHashMap<>();
		int var = 0; // would be 1 on non-static methods

		boolean object = false;
		boolean array = false;
		for (char c : rawType.toCharArray()) {
			if (!object) {
				if (array && c != 'L') {
					map.put(var, ASTORE); // array type is astore
					var++;
					array = false;
					continue;
				}
				switch (c) {
				case 'L':
					array = false;
					map.put(var, ASTORE);
					object = true;
					var++;
					break;
				case 'I':
					map.put(var, ISTORE);
					var++;
					break;
				case 'D':
					map.put(var, DSTORE);
					var += 2;
					break;
				case 'F':
					map.put(var, FSTORE);
					var++;
					break;
				case 'J':
					map.put(var, LSTORE);
					var += 2;
					break;
				case '[':
					array = true;
					break;
				}
			} else if (c == ';') {
				object = false;
			}
		}
		return map;
	}

	private void removeReturn(InsnList copy) {
		int i = copy.size() - 1;
		while (i >= 0) {
			AbstractInsnNode ain = copy.get(i);

			if (ain.getOpcode() == ATHROW) {
				// keep athrow, as it would still be in code
				return;
			}
			copy.remove(ain);
			switch (ain.getOpcode()) {
			case RETURN:
			case ARETURN:
			case DRETURN:
			case FRETURN:
			case IRETURN:
			case LRETURN:
				return;
			default:
			}
			i--;
		}
		throw new RuntimeException("no return found to remove, invalid method?");
	}

	public boolean isUnnecessary(MethodNode m) {
		if (!Access.isStatic(m.access)) {
			return false;
		} else if (m.instructions.size() > 32) {
			// do not inline huge methods
			return false;
		} else if (m.instructions.size() < 2) {
			// abstract methods or similar
			return false;
		}
		return !StreamSupport.stream(m.instructions.spliterator(), false).anyMatch(this::isInvocationOrJump);
	}

	public boolean isInvocationOrJump(AbstractInsnNode ain) {
		switch (ain.getType()) {
		case AbstractInsnNode.METHOD_INSN:
		case AbstractInsnNode.FIELD_INSN:
		case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
		case AbstractInsnNode.TYPE_INSN:
		case AbstractInsnNode.JUMP_INSN: // TODO replace return (if not last, inside loop) with goto to last insn, so jumps are supported too
			return true;
		}
		return false;
	}
}
