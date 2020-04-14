package me.nov.threadtear.execution.stringer.v3_9;

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
import me.nov.threadtear.util.Strings;

public class StringObfuscationStringer extends Execution implements IVMReferenceHandler {

	private static final String STRINGER_DECRPYTION_METHOD_DESC = "(Ljava/lang/Object;)Ljava/lang/String;";
	private ArrayList<Clazz> classes;
	private int encrypted;
	private int decrypted;
	private boolean verbose;

	public StringObfuscationStringer() {
		super(ExecutionCategory.STRINGER3_9, "Remove string obfuscation by Stringer",
				"Should work for version 3 - 9.<br><b>Can possibly run dangerous code.</b>");
	}

	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose, boolean ignoreErr) {
		this.verbose = verbose;
		this.classes = classes;
		this.encrypted = 0;
		this.decrypted = 0;

		classes.stream().map(c -> c.node).forEach(this::decrypt);
		if (encrypted == 0) {
			logger.severe("No strings matching stringer 3 - 9 string obfuscation have been found!");
			return false;
		}
		float decryptionRatio = Math.round((decrypted / (float) encrypted) * 100);
		logger.info(
				"Of a total of " + encrypted + " encrypted strings, " + (decryptionRatio) + "% were successfully decrypted");
		return decryptionRatio > 0.25;
	}

	private void decrypt(ClassNode cn) {
		cn.methods.forEach(m -> m.instructions.forEach(ain -> check(cn, m, ain)));
	}

	private void check(ClassNode cn, MethodNode m, AbstractInsnNode ain) {
		if (ain.getType() == AbstractInsnNode.LDC_INSN) {
			LdcInsnNode lin = (LdcInsnNode) ain;
			if (lin.cst instanceof String && Strings.seemsEncrypted((String) lin.cst)) {
				AbstractInsnNode next = Instructions.getRealNext(lin);
				if (next.getOpcode() == INVOKESTATIC) {
					MethodInsnNode min = (MethodInsnNode) next;
					if (min.desc.equals(STRINGER_DECRPYTION_METHOD_DESC)) {
						this.encrypted++;
						// decryption method found!
						try {
							String realString = invokeProxy(cn, m, min, (String) lin.cst);
							if (realString != null) {
								if (Strings.seemsEncrypted(realString)) {
									logger.warning("String may have not decrypted correctly in " + cn.name + "." + m.name + m.desc);
								} else {
									this.decrypted++;
								}
								lin.cst = realString;
								// Can't call m.instructions.remove(min); as it would mess with the loop, turn it into String.valueOf instead
								min.owner = "java/lang/String";
								min.name = "valueOf";
								min.desc = "(Ljava/lang/Object;)Ljava/lang/String;";
							} else {
								logger.warning("Failed to decrypt string in " + cn.name + "." + m.name + m.desc);
							}
						} catch (Exception e) {
							e.printStackTrace();
							logger.warning("Failed to decrypt string in " + cn.name + "." + m.name + m.desc + ": "
									+ e.getClass().getName() + ", " + e.getMessage());
						}
					} else if (verbose) {
						logger.warning("Wrong desc in " + cn.name + "." + m.name + m.desc);
					}
				} else if (verbose) {
					logger.warning("No invokestatic in " + cn.name + "." + m.name + m.desc + ": op=" + next.getOpcode()
							+ ", possibly decryption class itself");
				}
			}
		}
	}

	private ClassNode fakeInvocationClone;

	private String invokeProxy(ClassNode cn, MethodNode m, MethodInsnNode min, String encryptedString) throws Exception {
		VM vm = VM.constructVM(this);
		fakeInvocationClone = createFakeClone(cn, m, min, encryptedString); // create a duplicate of the current class, we need this because stringer checks for stacktrace method name and class
		vm.loadClass(min.owner.replace('/', '.'), true); // load decryption class, this class will load another class (some type of list / map)
		Class<?> loadedClone = vm.loadClass(fakeInvocationClone.name.replace('/', '.'), true); // load dupe
		if (m.name.equals("<init>")) {
			loadedClone.newInstance(); // special case: constructors have to be invoked by newInstance. Sandbox.createMethodProxy automatically handles access and super call
		} else {
			loadedClone.getMethod(m.name).invoke(null);
		}
		return (String) loadedClone.getFields()[0].get(null);
	}

	private ClassNode createFakeClone(ClassNode cn, MethodNode m, MethodInsnNode min, String encryptedString) {
		ClassNode node = Sandbox.createClassProxy(cn.name);

		InsnList instructions = new InsnList();
		instructions.add(new LdcInsnNode(encryptedString));
		instructions.add(min.clone(null)); // we can clone original method here
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