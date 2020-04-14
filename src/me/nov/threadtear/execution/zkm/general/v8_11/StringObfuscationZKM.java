package me.nov.threadtear.execution.zkm.general.v8_11;

import java.util.ArrayList;
import java.util.stream.StreamSupport;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.util.Access;
import me.nov.threadtear.asm.util.Instructions;
import me.nov.threadtear.asm.vm.IVMReferenceHandler;
import me.nov.threadtear.asm.vm.Sandbox;
import me.nov.threadtear.asm.vm.VM;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.util.Strings;

public class StringObfuscationZKM extends Execution implements IVMReferenceHandler {

	private ArrayList<Clazz> classes;
	private int decrypted;
	private int notDecrypted;

	private boolean verbose;

	private static final String ENCHANCED_MODE_METHOD_DESC = "(II)Ljava/lang/String;";

	public StringObfuscationZKM() {
		super(ExecutionCategory.ZKM8_11, "Remove string obfuscation by ZKM 8 - 11",
				"Works for ZKM 8 - 11, but could work for older or newer versions too.<br><b>Can possibly run dangerous code.</b><br><b>UNFINISHED</b>");
	}

	/*
	 * TODO: instead of checking through bytecode, stack analysis should be made, because ZKM often abuses stack and pushes ints or getfields not in the place where they normally are!
	 */
	
	
	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose, boolean ignoreErr) {
		this.classes = classes;
		this.verbose = verbose;
		classes.stream().map(c -> c.node).filter(this::hasZKMBlock).forEach(this::decrypt);
		logger.info("Decrypted " + decrypted + " strings successfully, failed on " + notDecrypted + " strings.");
		return decrypted > 0;
	}

	private boolean hasZKMBlock(ClassNode cn) {
		if (Access.isInterface(cn.access)) // TODO maybe interfaces get string encrypted too, but proxy is not working
											// because static methods in interfaces are not allowed
			return false;
		MethodNode mn = getStaticInitializer(cn);
		if (mn == null)
			return false;
		return StreamSupport.stream(mn.instructions.spliterator(), false)
				.anyMatch(ain -> ain.getType() == AbstractInsnNode.LDC_INSN
						&& Strings.isHighSDev(((LdcInsnNode) ain).cst.toString()));
	}

	private void decrypt(ClassNode cn) {
		MethodNode clinit = getStaticInitializer(cn);
		MethodNode callMethod = Sandbox.createMethodProxy(modifyClinitForProxy(clinit), "clinitProxy", "()V");
		cn.methods.add(callMethod);
		try {
			invokeVMAndReplace(cn);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		cn.methods.remove(callMethod);
	}

	/**
	 * Creates a VM with the same class, but with a new method called "clinitProxy"
	 * that is a cutout of the original static initializer with the decryption ONLY.
	 */
	private void invokeVMAndReplace(ClassNode cn) throws Throwable {
		VM vm = VM.constructVM(this);
		Class<?> callProxy = vm.loadClass(cn.name);
		callProxy.getMethod("clinitProxy").invoke(null); // invoke cut clinit

		// clinitProxy should not be handled, but <clinit> SHOULD as there could be
		// encrypted strings too
		cn.methods.stream().filter(m -> !m.name.equals("clinitProxy")).forEach(m -> {
			for (int i = 0; i < m.instructions.size(); i++) {
				AbstractInsnNode ain = m.instructions.get(i);
				if (isZKMField(cn, ain)) {
					int status = tryReplaceFieldLoads(cn, callProxy, m, (FieldInsnNode) ain);
					if (status == 1) {
						decrypted++;
					} else if (status == -1) {
						notDecrypted++;
					}
				}
				if (isZKMMethod(cn, ain)) {
					int status = tryReplaceDecryptionMethods(cn, callProxy, m, (MethodInsnNode) ain);
					if (status == 1) {
						decrypted++;
					} else if (status == -1) {
						notDecrypted++;
					}
				}
			}
		});
	}

	/**
	 * Is ain a field call to a decrypted field?
	 */
	private boolean isZKMField(ClassNode cn, AbstractInsnNode ain) {
		if (ain.getOpcode() != GETSTATIC)
			return false;
		FieldInsnNode fin = ((FieldInsnNode) ain);
		// could be either array or normal string field, two cases
		return fin.owner.equals(cn.name) && fin.desc.endsWith("Ljava/lang/String;");
	}

	/**
	 * Is ain a method call to enchanced method decryption?
	 */
	private boolean isZKMMethod(ClassNode cn, AbstractInsnNode ain) {
		if (ain.getOpcode() != INVOKESTATIC)
			return false;
		MethodInsnNode min = ((MethodInsnNode) ain);
		return min.owner.equals(cn.name) && min.desc.equals(ENCHANCED_MODE_METHOD_DESC);
	}

	/**
	 * Replace decryption methods that take two ints as argument and returns the
	 * decrypted String. This does only occur sometimes!
	 */
	private int tryReplaceDecryptionMethods(ClassNode cn, Class<?> callProxy, MethodNode m, MethodInsnNode min) {

		try {
			AbstractInsnNode previous = Instructions.getRealPrevious(min);
			AbstractInsnNode prePrevious = Instructions.getRealPrevious(previous);
			if (Instructions.isInteger(previous) && Instructions.isInteger(prePrevious)) {
				String decrypted = (String) callProxy.getDeclaredMethod(min.name, int.class, int.class).invoke(null,
						Instructions.getIntValue(prePrevious), Instructions.getIntValue(previous));
				if (!Strings.isHighUTF(decrypted)) {
					// avoid concurrent modification
					m.instructions.set(prePrevious, new InsnNode(NOP)); // remove aaload
					m.instructions.set(previous, new InsnNode(NOP)); // remove int
					m.instructions.set(min, new LdcInsnNode(decrypted));
					return 1;
				} else if (verbose) {
					logger.severe("Failed string array decryption in " + cn.name);
				}
			} else if (verbose) {
				logger.warning("Unexpected case, method is not feeded two ints " + cn.name + "." + m.name);
			}
		} catch (Throwable t) {
			logger.severe("Failure in " + cn.name + ": " + t.getClass().getName() + "-" + t.getMessage());
		}
		return -1;
	}

	/**
	 * Replace decrypted String[] and String fields in the code. This is the hardest
	 * part
	 */
	private int tryReplaceFieldLoads(ClassNode cn, Class<?> callProxy, MethodNode m, FieldInsnNode fin) {
		try {
			if (fin.desc.equals("[Ljava/lang/String;")) {
				String[] decryptedArray = (String[]) callProxy.getField(fin.name).get(null);
				if (decryptedArray == null) {
					if (verbose)
						logger.warning("Possible false call in " + cn.name + " or failed decryption, array is null");
					// could be false call, not the decrypted array
					return 0;
				}
				AbstractInsnNode next = Instructions.getRealNext(fin);
				if (!Instructions.isInteger(next)) {
					if (next.getType() == AbstractInsnNode.VAR_INSN) {
						return handleLocalVariableLoad(decryptedArray, cn, m, fin, (VarInsnNode) next);
					} else {
						if (verbose)
							logger.severe("Failed replacement in " + cn.name + ", unexpected case");
						return 0;
					}
				}
				int arrayPos = Instructions.getIntValue(next);
				if (arrayPos < decryptedArray.length && !Strings.isHighUTF(decryptedArray[arrayPos])) {
					// avoid concurrent modification
					m.instructions.set(Instructions.getRealNext(next), new InsnNode(NOP)); // remove aaload
					m.instructions.set(next, new InsnNode(NOP)); // remove int
					m.instructions.set(fin, new LdcInsnNode(decryptedArray[arrayPos]));
					return 1;
				} else if (verbose) {
					logger.severe("Failed string array decryption in " + cn.name);
				}
			} else if (fin.desc.equals("Ljava/lang/String;")) {
				String decrypedString = (String) callProxy.getField(fin.name).get(null);
				if (decrypedString == null) {
					// could be false call, not the decrypted string
					return 0;
				}
				if (!Strings.isHighUTF(decrypedString)) {
					m.instructions.set(fin, new LdcInsnNode(decrypedString));
					return 1;
				} else if (verbose) {
					logger.severe("Failed single string decryption in " + cn.name);
				}
			} else if (verbose) {
				logger.warning("Ignoring multi array in " + cn.name);
			}
		} catch (Throwable t) {
			logger.severe("Failure in " + cn.name + ": " + t.getClass().getName() + "-" + t.getMessage());
		}
		return -1;
	}

	/**
	 * This case is only when ZKM stores the decrypted String[] in a local variable
	 * instead of GETSTATIC everytime
	 */
	private int handleLocalVariableLoad(String[] decryptedArray, ClassNode cn, MethodNode m, FieldInsnNode fin,
			VarInsnNode vin) {
		int replaces = 0;
		for (int i = 0; i < m.instructions.size(); i++) {
			AbstractInsnNode ain = m.instructions.get(i);
			if (ain.getOpcode() == ALOAD) {
				VarInsnNode load = (VarInsnNode) ain;
				if (load.var != vin.var)
					continue;
				AbstractInsnNode next = Instructions.getRealNext(ain);
				if (Instructions.isInteger(next)) {
					int arrayPos = Instructions.getIntValue(next);
					if (arrayPos < decryptedArray.length && !Strings.isHighUTF(decryptedArray[arrayPos])) {
						// avoid concurrent modification
						m.instructions.set(Instructions.getRealNext(next), new InsnNode(NOP)); // remove aaload
						m.instructions.set(next, new InsnNode(NOP)); // remove int
						m.instructions.set(ain, new LdcInsnNode(decryptedArray[arrayPos]));
						replaces++;
					} else if (verbose) {
						logger.severe("Failed string array decryption in " + cn.name + " " + m.name);
					}
				}
			}
		}
		return replaces > 1 ? 1 : -1;
	}

	/**
	 * Only cut out decryption part
	 */
	private InsnList modifyClinitForProxy(MethodNode clinit) {
		return Instructions.copy(clinit.instructions);
	}

	@Override
	public ClassNode tryClassLoad(String name) {
		return classes.stream().map(c -> c.node).filter(c -> c.name.equals(name)).findFirst().orElse(null);
	}

}
