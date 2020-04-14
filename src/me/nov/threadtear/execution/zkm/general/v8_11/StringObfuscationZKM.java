package me.nov.threadtear.execution.zkm.general.v8_11;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.nov.threadtear.asm.Clazz;
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

	public StringObfuscationZKM() {
		super(ExecutionCategory.ZKM8_11, "Remove string obfuscation by ZKM 8 - 11",
				"Works for ZKM 8 - 11, but could work for older or newer versions too.<br><b>Can possibly run dangerous code.</b><br><b>UNFINISHED</b>");
	}

	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose, boolean ignoreErr) {
		this.classes = classes;
		this.verbose = verbose;
		classes.stream().map(c -> c.node).filter(this::hasZKMBlock).forEach(this::decrypt);
		logger.info("Decrypted " + decrypted + " strings successfully, failed on " + notDecrypted + " strings.");
		return decrypted > 0;
	}

	public boolean hasZKMBlock(ClassNode cn) {
		MethodNode mn = getStaticInitializer(cn);
		if (mn == null)
			return false;
		return StreamSupport.stream(mn.instructions.spliterator(), false)
				.anyMatch(ain -> ain.getType() == AbstractInsnNode.LDC_INSN
						&& Strings.isHighSDev(((LdcInsnNode) ain).cst.toString()));
	}

	public void decrypt(ClassNode cn) {
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

	private void invokeVMAndReplace(ClassNode cn) throws Throwable {
		VM vm = VM.constructVM(this);
		Class<?> callProxy = vm.loadClass(cn.name);
		callProxy.getMethod("clinitProxy").invoke(null); // invoke cut clinit
		cn.methods.forEach(m -> {
			StreamSupport.stream(m.instructions.spliterator(), false).filter(ain -> isZKMField(cn, ain))
					.collect(Collectors.toSet()).forEach(ain -> {
						if (tryReplaceFieldLoads(cn, callProxy, m, (FieldInsnNode) ain)) {
							decrypted++;
						} else {
							notDecrypted++;
						}
					});
			// TODO methods with two int arg here
		});
	}

	private boolean isZKMField(ClassNode cn, AbstractInsnNode ain) {
		if (ain.getOpcode() != GETSTATIC)
			return false;
		FieldInsnNode fin = ((FieldInsnNode) ain);
		// could be either array or normal string field, two cases
		return fin.owner.equals(cn.name) && fin.desc.endsWith("Ljava/lang/String;");
	}

	private boolean tryReplaceFieldLoads(ClassNode cn, Class<?> callProxy, MethodNode m, FieldInsnNode fin) {
		try {
			if (fin.desc.equals("[Ljava/lang/String;")) {
				String[] decryptedArray = (String[]) callProxy.getField(fin.name).get(null);
				if (decryptedArray == null) {
					if (verbose)
						logger.warning("Possible false call in " + cn.name + " or failed decryption");
					// could be false call, not the decrypted array
					return false;
				}
				AbstractInsnNode next = Instructions.getRealNext(fin);
				if (!Instructions.isInteger(next)) {
					// TODO: in this case ZKM stores the decrypted array in a local variable -> next
					// = ASTORE X -> before int push and AALOAD is ALOAD X
					if (verbose)
						logger.severe("Failed replacement in " + cn.name + ", case currently unimplemented");
					return false;
				}
				int arrayPos = Instructions.getIntValue(next);
				if (arrayPos < decryptedArray.length && !Strings.isHighUTF(decryptedArray[arrayPos])) {
					m.instructions.remove(Instructions.getRealNext(next)); // remove aaload
					m.instructions.remove(next); // remove int
					m.instructions.set(fin, new LdcInsnNode(decryptedArray[arrayPos]));
					return true;
				} else if (verbose) {
					logger.severe("Failed string array decryption in " + cn.name);
				}
			} else if (fin.desc.equals("Ljava/lang/String;")) {
				String decrypedString = (String) callProxy.getField(fin.name).get(null);
				if (decrypedString == null) {
					// could be false call, not the decrypted string
					return false;
				}
				if (!Strings.isHighUTF(decrypedString)) {
					m.instructions.set(fin, new LdcInsnNode(decrypedString));
					return true;
				} else if (verbose) {
					logger.severe("Failed single string decryption in " + cn.name);
				}
			} else if (verbose) {
				logger.warning("Ignoring multi array in " + cn.name);
			}
		} catch (Throwable t) {
			t.printStackTrace();
			logger.severe("Failure in " + cn.name);
		}
		return false;
	}

	private InsnList modifyClinitForProxy(MethodNode clinit) {
		return Instructions.copy(clinit.instructions);
	}

	@Override
	public ClassNode tryClassLoad(String name) {
		return classes.stream().map(c -> c.node).filter(c -> c.name.equals(name)).findFirst().orElse(null);
	}

}
