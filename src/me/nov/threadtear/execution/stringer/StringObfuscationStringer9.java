package me.nov.threadtear.execution.stringer;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.util.Instructions;
import me.nov.threadtear.asm.vm.IVMReferenceHandler;
import me.nov.threadtear.asm.vm.Sandbox;
import me.nov.threadtear.asm.vm.VM;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import me.nov.threadtear.util.Descriptor;
import me.nov.threadtear.util.Strings;

public class StringObfuscationStringer9 extends Execution implements IVMReferenceHandler {

	private static final String STRINGER_DECRPYTION_METHOD_DESC_REGEX = "\\(Ljava/lang/Object;...?\\)Ljava/lang/String;";
	private ArrayList<Clazz> classes;
	private int encrypted;
	private int decrypted;
	private boolean verbose;

	public StringObfuscationStringer9() {
		super(ExecutionCategory.STRINGER, "String obfuscation removal targeting Stringer 9", "Works for version 9 only.<br>Make sure to decrypt access obfuscation first.", ExecutionTag.RUNNABLE, ExecutionTag.POSSIBLY_MALICIOUS);
	}

	/*
	 * TODO: combine String Obfuscation classes and solve pre-decryption calculation using ConstantAnalyzer
	 */
	
	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose, boolean ignoreErr) {
		this.verbose = verbose;
		this.classes = classes;
		this.encrypted = 0;
		this.decrypted = 0;

		classes.stream().map(c -> c.node).forEach(this::decrypt);
		if (encrypted == 0) {
			logger.severe("No strings matching stringer 9 string obfuscation have been found!");
			return false;
		}
		float decryptionRatio = Math.round((decrypted / (float) encrypted) * 100);
		logger.info("Of a total of " + encrypted + " encrypted strings, " + (decryptionRatio) + "% were successfully decrypted");
		return decryptionRatio > 0.25;
	}

	private void decrypt(ClassNode cn) {
		cn.methods.forEach(m -> {
			for (int i = 0; i < m.instructions.size(); i++) {
				check(cn, m, m.instructions.get(i));
			}
		});
	}

	private void check(ClassNode cn, MethodNode m, AbstractInsnNode ain) {
		if (ain.getType() == AbstractInsnNode.LDC_INSN) {
			LdcInsnNode lin = (LdcInsnNode) ain;
			if (lin.cst instanceof String && Strings.isHighUTF((String) lin.cst)) {
				InsnList proxyExecution = new InsnList();

				AbstractInsnNode next = lin;
				do {
					proxyExecution.add(next.clone(null));
					next = Instructions.getRealNext(next);
				} while (next != null && next.getOpcode() != INVOKESTATIC && next.getType() != AbstractInsnNode.JUMP_INSN);
				if (next != null && next.getOpcode() == INVOKESTATIC) {
					MethodInsnNode min = (MethodInsnNode) next;
					proxyExecution.add(min.clone(null));
					if (min.desc.matches(STRINGER_DECRPYTION_METHOD_DESC_REGEX)) {
						try {
							encrypted++;
							String realString = invokeProxy(cn, m, min, proxyExecution);
							if (realString != null) {
								if (Strings.isHighUTF(realString)) {
									logger.warning("String may have not decrypted correctly in " + cn.name + "." + m.name + m.desc);
								} else {
									this.decrypted++;
								}

								String inner = min.desc.substring(1, min.desc.lastIndexOf(')'));
								for (int size : Descriptor.calculateAmountArguments(inner)) {
									m.instructions.insertBefore(min, new InsnNode(size > 1 ? POP2 : POP));
								}
								m.instructions.set(min, new LdcInsnNode(realString));
								lin.cst = ""; // clean encrypted ldc to save bytes
							} else {
								logger.warning("Failed to decrypt string in " + cn.name + "." + m.name + m.desc);
							}
						} catch (Exception e) {
							e.printStackTrace();
							logger.warning("Failed to decrypt string in " + cn.name + "." + m.name + m.desc + ": " + e.getClass().getName() + ", " + e.getMessage());
						}
					} else if (verbose) {
						logger.warning("Wrong desc in " + cn.name + "." + m.name + m.desc + ": " + min.desc);
					}
				}
			}
		}
	}

	private ClassNode fakeInvocationClone;

	private String invokeProxy(ClassNode cn, MethodNode m, MethodInsnNode min, InsnList proxyExecution) throws Exception {
		VM vm = VM.constructVM(this);
		fakeInvocationClone = createFakeClone(cn, m, proxyExecution); // create a duplicate of the current class,
		// we need this because stringer checks for
		// stacktrace method name and class
		vm.loadClass(min.owner.replace('/', '.'), true); // load decryption class, this class will load another class
		// (some type of list / map)
		Class<?> loadedClone = vm.loadClass(fakeInvocationClone.name.replace('/', '.'), true); // load dupe
		if (m.name.equals("<init>")) {
			loadedClone.newInstance(); // special case: constructors have to be invoked by newInstance.
			// Sandbox.createMethodProxy automatically handles access and super call
		} else {
			for (Method reflectionMethod : loadedClone.getMethods()) {
				if (reflectionMethod.getName().equals(m.name)) {
					reflectionMethod.invoke(null);
					break;
				}
			}
		}
		return (String) loadedClone.getFields()[0].get(null);
	}

	private ClassNode createFakeClone(ClassNode cn, MethodNode m, InsnList proxyExecution) {
		ClassNode node = Sandbox.createClassProxy(cn.name);

		InsnList instructions = new InsnList();
		instructions.add(proxyExecution);
		instructions.add(new FieldInsnNode(PUTSTATIC, node.name, "proxyReturn", "Ljava/lang/String;"));
		instructions.add(new InsnNode(RETURN));

		node.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, "proxyReturn", "Ljava/lang/String;", null, null));
		node.methods.add(Sandbox.createMethodProxy(instructions, m.name, "()V")); // method should return real string
		return node;
	}

	@Override
	public ClassNode tryClassLoad(String name) {
		if (name.equals(fakeInvocationClone.name)) {
			return fakeInvocationClone;
		}
		// try to find class in loaded jar file
		return classes.stream().map(c -> c.node).filter(c -> c.name.equals(name)).findFirst().orElse(null);
	}
}