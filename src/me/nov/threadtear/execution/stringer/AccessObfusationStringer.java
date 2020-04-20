package me.nov.threadtear.execution.stringer;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Map;

import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import me.nov.threadtear.util.DynamicReflection;
import me.nov.threadtear.vm.IVMReferenceHandler;
import me.nov.threadtear.vm.VM;

public class AccessObfusationStringer extends Execution implements IVMReferenceHandler {

	private static final String STRINGER_INVOKEDYNAMIC_HANDLE_DESC = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
	private Map<String, Clazz> classes;
	private int encrypted;
	private int decrypted;
	private boolean verbose;

	public AccessObfusationStringer() {
		super(ExecutionCategory.STRINGER, "Access obfuscation removal", "Works for version 3 - 9.<br>Only works with invokedynamic obfuscation for now.", ExecutionTag.RUNNABLE,
				ExecutionTag.POSSIBLY_MALICIOUS);
	}

	@Override
	public boolean execute(Map<String, Clazz> classes, boolean verbose) {
		this.verbose = verbose;
		this.classes = classes;
		this.encrypted = 0;
		this.decrypted = 0;
		logger.info("Decrypting all invokedynamic references, this could take some time!");
		classes.values().stream().map(c -> c.node).forEach(this::decrypt);
		if (encrypted == 0) {
			logger.severe("No access obfuscation matching stringer has been found!");
			return false;
		}
		float decryptionRatio = Math.round((decrypted / (float) encrypted) * 100);
		logger.info("Of a total " + encrypted + " encrypted references, " + (decryptionRatio) + "% were successfully decrypted");
		return decryptionRatio > 0.25;
	}

	private void decrypt(ClassNode cn) {
		MethodNode clinit = getStaticInitializer(cn);
		if (clinit != null)
			cn.methods.remove(clinit);
		VM vm = VM.constructVM(this);
		cn.methods.forEach(m -> {
			for (int i = 0; i < m.instructions.size(); i++) {
				AbstractInsnNode ain = m.instructions.get(i);
				if (ain.getOpcode() == INVOKEDYNAMIC) {
					InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) ain;
					if (idin.bsm != null) {
						Handle bsm = idin.bsm;
						if (bsm.getOwner().equals(cn.name) && bsm.getDesc().equals(STRINGER_INVOKEDYNAMIC_HANDLE_DESC)) {
							encrypted++;
							try {
								CallSite callsite = loadCallSiteFromVM(vm, cn, idin, bsm);
								MethodHandleInfo methodInfo = DynamicReflection.revealMethodInfo(callsite.getTarget());
								m.instructions.set(ain, DynamicReflection.getInstructionFromHandleInfo(methodInfo));
								decrypted++;
							} catch (Throwable t) {
								logger.severe("Failed to get callsite using classloader in " + cn.name + "." + m.name + m.desc + ": " + t.getClass().getName() + ", " + t.getMessage());
							}
						} else if (verbose) {
							logger.warning("Other bootstrap type in " + cn.name + ": " + bsm);
						}
					}
				}
			}
		});
		if (clinit != null)
			cn.methods.add(clinit);
	}

	private CallSite loadCallSiteFromVM(VM vm, ClassNode cn, InvokeDynamicInsnNode idin, Handle bsm) throws Throwable {
		Class<?> proxyClass = vm.loadClass(cn.name.replace('/', '.'), true);
		Method bootstrap = proxyClass.getDeclaredMethod(bsm.getName(), Object.class, Object.class, Object.class);
		CallSite callsite = (CallSite) bootstrap.invoke(null, MethodHandles.lookup(), idin.name, MethodType.fromMethodDescriptorString(idin.desc, vm));
		return callsite;
	}

	@Override
	public ClassNode tryClassLoad(String name) {
		return classes.containsKey(name) ? classes.get(name).node : null;
	}

}