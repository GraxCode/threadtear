package me.nov.threadtear.execution.zkm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import me.nov.threadtear.analysis.stack.ConstantTracker;
import me.nov.threadtear.analysis.stack.ConstantValue;
import me.nov.threadtear.analysis.stack.IConstantReferenceHandler;
import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.util.Access;
import me.nov.threadtear.asm.util.Instructions;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import me.nov.threadtear.util.DynamicReflection;
import me.nov.threadtear.vm.IVMReferenceHandler;
import me.nov.threadtear.vm.VM;

public class AccessObfusationZKM extends Execution implements IVMReferenceHandler, IConstantReferenceHandler {

	private static final String ZKM_INVOKEDYNAMIC_HANDLE_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";

	/**
	 * The method that returns the real MethodHandle
	 */
	private static final String ZKM_INVOKEDYNAMIC_REAL_BOOTSTRAP_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;Ljava/lang/String;Ljava/lang/invoke/MethodType;J)Ljava/lang/invoke/MethodHandle;";

	private ArrayList<Clazz> classes;
	private int encrypted;
	private int decrypted;
	private boolean verbose;
	private VM vm;

	public AccessObfusationZKM() {
		super(ExecutionCategory.ZKM, "Access obfuscation removal", "Tested on ZKM 8 - 11, could work on newer versions too.<br>Only works with invokedynamic obfuscation for now.", ExecutionTag.RUNNABLE, ExecutionTag.POSSIBLY_MALICIOUS);
	}

	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose) {
		this.verbose = verbose;
		this.classes = classes;
		this.encrypted = 0;
		this.decrypted = 0;
		logger.info("Decrypting all invokedynamic references, this could take some time!");
		this.vm = VM.constructNonInitializingVM(this);
		classes.stream().map(c -> c.node).forEach(this::decrypt);
		if (encrypted == 0) {
			logger.severe("No access obfuscation matching ZKM has been found!");
			return false;
		}
		float decryptionRatio = Math.round((decrypted / (float) encrypted) * 100);
		logger.info("Of a total " + encrypted + " encrypted references, " + (decryptionRatio) + "% were successfully decrypted");
		logger.info("For better results use \"-noverify\" as JVM argument!");
		return decryptionRatio > 0.25;
	}

	private void decrypt(ClassNode cn) {
		cn.methods.forEach(m -> {
			Analyzer<ConstantValue> a = new Analyzer<ConstantValue>(new ConstantTracker(this, Access.isStatic(m.access), m.maxLocals, m.desc, new Object[0]));
			try {
				a.analyze(cn.name, m);
			} catch (AnalyzerException e) {
				logger.severe("Failed stack analysis in " + cn.name + "." + m.name + ":" + e.getMessage());
				return;
			}
			Frame<ConstantValue>[] frames = a.getFrames();
			InsnList rewrittenCode = new InsnList();
			Map<LabelNode, LabelNode> labels = Instructions.cloneLabels(m.instructions);
			for (int i = 0; i < m.instructions.size(); i++) {
				AbstractInsnNode ain = m.instructions.get(i);
				Frame<ConstantValue> frame = frames[i];
				if (ain.getOpcode() == INVOKEDYNAMIC && frame != null) {
					InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) ain;
					if (idin.bsm != null) {
						Handle bsm = idin.bsm;
						if (bsm.getDesc().equals(ZKM_INVOKEDYNAMIC_HANDLE_DESC) && classes.stream().map(c -> c.node).anyMatch(node -> node.name.equals(bsm.getOwner()))) {
							encrypted++;
							try {
								ConstantValue top = frame.getStack(frame.getStackSize() - 1);
								if (top.isKnown()) {
									MethodHandle handle = loadZKMBuriedHandleFromVM(classes.stream().map(c -> c.node).filter(node -> node.name.equals(bsm.getOwner())).findFirst().get(), idin, bsm, (long) top.getValue());
									if (handle != null) {
										MethodHandleInfo methodInfo = DynamicReflection.revealMethodInfo(handle);
										rewrittenCode.add(new InsnNode(POP2)); // pop the long
										rewrittenCode.add(DynamicReflection.getInstructionFromHandleInfo(methodInfo));
										decrypted++;
										continue;
									}
								}
							} catch (Throwable t) {
								if (verbose) {
									t.printStackTrace();
								}
								logger.severe("Failed to get callsite using classloader in " + cn.name + "." + m.name + m.desc + ": " + t.getClass().getName() + ", " + t.getMessage());
							}
						} else if (verbose) {
							logger.warning("Other bootstrap type in " + cn.name + ": " + bsm);
						}
					}
				}
				rewrittenCode.add(ain.clone(labels));
			}
			Instructions.updateInstructions(m, labels, rewrittenCode);
		});
		System.out.println();
	}

	private MethodHandle loadZKMBuriedHandleFromVM(ClassNode cn, InvokeDynamicInsnNode idin, Handle bsm, long decryptionLong) throws Throwable {
		if (!vm.isLoaded(cn.name.replace('/', '.'))) {
			cn.methods.forEach(mn -> Instructions.isolateCallsThatMatch(mn, (name) -> !name.equals(cn.name) && !name.matches("java/lang/.*")));
			vm.explicitlyPreloadWithClinit(cn); // make sure bootstrap class class has <clinit>
		}
		Class<?> proxyClass = vm.loadClass(cn.name.replace('/', '.'), true);
		Method bootstrap = null;
		for (Method m : proxyClass.getDeclaredMethods()) {
			if (Type.getMethodDescriptor(m).equals(ZKM_INVOKEDYNAMIC_REAL_BOOTSTRAP_DESC)) {
				bootstrap = m;
				break;
			}
		}
		if (bootstrap == null) {
			logger.warning("Failed to find real bootstrap method in " + cn.name + ": " + bsm);
			return null;
		}
		return (MethodHandle) bootstrap.invoke(null, DynamicReflection.getTrustedLookup(), null /* MutableCallSide, unused in method */, idin.name, MethodType.fromMethodDescriptorString(idin.desc, vm), decryptionLong);
	}

	@Override
	public ClassNode tryClassLoad(String name) {
		return classes.stream().map(c -> c.node).filter(c -> c.name.equals(name)).findFirst().orElse(null);
	}

	@Override
	public Object getFieldValueOrNull(BasicValue v, String owner, String name, String desc) {
		return null;
	}

	@Override
	public Object getMethodReturnOrNull(BasicValue v, String owner, String name, String desc, List<? extends ConstantValue> values) {
		return null;
	}

}