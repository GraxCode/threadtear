package me.nov.threadtear.execution.zkm;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import me.nov.threadtear.analysis.stack.ConstantTracker;
import me.nov.threadtear.analysis.stack.ConstantValue;
import me.nov.threadtear.analysis.stack.IConstantReferenceHandler;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import me.nov.threadtear.io.Clazz;
import me.nov.threadtear.io.Conversion;
import me.nov.threadtear.util.Strings;
import me.nov.threadtear.util.asm.Access;
import me.nov.threadtear.util.asm.Instructions;
import me.nov.threadtear.util.asm.References;
import me.nov.threadtear.vm.IVMReferenceHandler;
import me.nov.threadtear.vm.Sandbox;
import me.nov.threadtear.vm.VM;

public class StringObfuscationZKM extends Execution implements IVMReferenceHandler, IConstantReferenceHandler {

	private int decrypted;

	private boolean verbose;

	private static final String ENCHANCED_MODE_METHOD_DESC = "(II)Ljava/lang/String;";

	public StringObfuscationZKM() {
		super(ExecutionCategory.ZKM, "String obfuscation removal",
				"Tested on ZKM 5 - 11, could work on newer versions too.<br>" + "<i>String encryption using DES Cipher is currently <b>NOT</b> supported.</i>", ExecutionTag.RUNNABLE,
				ExecutionTag.POSSIBLY_MALICIOUS);
	}

	/*
	 * TODO: String encryption using DES Cipher (probably only in combination with reflection obfuscation
	 */

	@Override
	public boolean execute(Map<String, Clazz> classes, boolean verbose) {
		this.verbose = verbose;
		classes.values().stream().map(c -> c.node).filter(this::hasZKMBlock).forEach(this::decrypt);
		logger.info("Decrypted " + decrypted + " strings successfully.");
		return decrypted > 0;
	}

	private boolean hasZKMBlock(ClassNode cn) {
		if (Access.isInterface(cn.access)) // TODO maybe interfaces get string encrypted too, but proxy would not be
																				// working because static methods in interfaces are not allowed
			return false;
		MethodNode mn = getStaticInitializer(cn);
		if (mn == null)
			return false;
		return StreamSupport.stream(mn.instructions.spliterator(), false).anyMatch(ain -> ain.getType() == AbstractInsnNode.LDC_INSN && Strings.isHighSDev(((LdcInsnNode) ain).cst.toString()));
	}

	private static final String ALLOWED_CALLS = "(java/lang/String).*";

	private void decrypt(ClassNode cn) {
		MethodNode clinit = getStaticInitializer(cn);
		if (clinit == null)
			return;
		if (clinit.instructions.size() > 2000) {
			logger.severe("Static initializer too huge to decrypt in " + cn.name);
			return;
		}
		ClassNode proxyClass = Sandbox.createClassProxy("ProxyClass");
		MethodNode callMethod = Sandbox.copyMethod(clinit);
		callMethod.name = "clinitProxy";
		callMethod.access = ACC_PUBLIC | ACC_STATIC;
		Instructions.isolateCallsThatMatch(callMethod, (name, desc) -> !name.matches(ALLOWED_CALLS),
				(name, desc) -> !name.equals(cn.name) || (!desc.equals("[Ljava/lang/String;") && !desc.equals("Ljava/lang/String;")));
		proxyClass.methods.add(callMethod);

		// add decryption methods
		cn.methods.stream().filter(m -> m.desc.equals(ENCHANCED_MODE_METHOD_DESC)).forEach(m -> {
			MethodNode copy = Sandbox.copyMethod(m);
			proxyClass.methods.add(copy);
			Instructions.isolateCallsThatMatch(copy, (name, desc) -> !name.matches(ALLOWED_CALLS),
					(name, desc) -> !name.equals(cn.name) || (!desc.equals("[Ljava/lang/String;") && !desc.equals("Ljava/lang/String;")));
		});
		cn.fields.stream().filter(m -> m.desc.equals("[Ljava/lang/String;") || m.desc.equals("Ljava/lang/String;")).forEach(f -> proxyClass.fields.add(f));
		Map<String, String> singleMap = Collections.singletonMap(cn.name, proxyClass.name);
		proxyClass.methods.stream().map(m -> m.instructions.toArray()).flatMap(Arrays::stream).forEach(ain -> References.remapInstructionDescs(singleMap, ain));
		try {
			invokeVMAndReplace(proxyClass, cn);
		} catch (Throwable e) {
			if (verbose)
				e.printStackTrace();
			logger.severe("Failed to run proxy in " + cn.name + ": " + e.getMessage());
		}
	}

	/**
	 * Creates a VM with the same class, but with a new method called "clinitProxy" that is a cutout of the original static initializer with the decryption ONLY.
	 */
	private void invokeVMAndReplace(ClassNode proxy, ClassNode realClass) throws Throwable {
		VM vm = VM.constructNonInitializingVM(this);
		vm.explicitlyPreloadNoClinit(proxy);
		vm.explicitlyPreloadNoClinit(realClass);
		Class<?> callProxy = vm.loadClass("ProxyClass");
		try {
			callProxy.getMethod("clinitProxy").invoke(null); // invoke cut clinit, fields in original class in vm get set
		} catch (InvocationTargetException e) {
			if (!(e.getCause() instanceof NullPointerException)) {
				// only ignore NPE from instruction removal
				throw e;
			} else {
				logger.info("NPE in " + realClass.name);
			}
		}
		realClass.methods.stream().forEach(m -> {
			decryptedArrayField = null;
			m.instructions.forEach(ain -> {
				if (isLocalField(realClass, ain) && ((FieldInsnNode) ain).desc.equals("[Ljava/lang/String;")) {
					decryptedArrayField = (FieldInsnNode) ain;
					try {
						decryptedFieldValue = (String[]) callProxy.getField(((FieldInsnNode) ain).name).get(null);
					} catch (Exception e) {
					}
				}
			});

			Analyzer<ConstantValue> a = new Analyzer<ConstantValue>(new ConstantTracker(this, Access.isStatic(m.access), m.maxLocals, m.desc, new Object[0]));
			try {
				a.analyze(realClass.name, m);
			} catch (AnalyzerException e) {
				logger.severe("Failed stack analysis in " + realClass.name + "." + m.name + ":" + e.getMessage());
				return;
			}
			Frame<ConstantValue>[] frames = a.getFrames();
			InsnList rewrittenCode = new InsnList();
			Map<LabelNode, LabelNode> labels = Instructions.cloneLabels(m.instructions);

			// as we can't add instructions because frame index and instruction index
			// wouldn't fit together anymore we have to do it this way
			for (int i = 0; i < m.instructions.size(); i++) {
				AbstractInsnNode ain = m.instructions.get(i);
				Frame<ConstantValue> frame = frames[i];
				if (isZKMMethod(realClass, ain)) {
					for (AbstractInsnNode newInstr : decryptMethodsAndRewrite(realClass, callProxy, m, (MethodInsnNode) ain, frame)) {
						rewrittenCode.add(newInstr.clone(labels));
					}
				} else {
					for (AbstractInsnNode newInstr : tryReplaceFieldLoads(realClass, callProxy, m, ain, frame, proxy)) {
						rewrittenCode.add(newInstr.clone(labels));
					}
				}
			}
			Instructions.updateInstructions(m, labels, rewrittenCode);
		});
	}

	private boolean isLocalField(ClassNode cn, AbstractInsnNode ain) {
		if (ain.getOpcode() != GETSTATIC)
			return false;
		FieldInsnNode fin = ((FieldInsnNode) ain);
		// could be either array or normal string field, two cases
		return fin.owner.equals(cn.name);
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
	 * Replace decryption methods that take two ints as argument and returns the decrypted String. This does only occur sometimes!
	 * 
	 * @param frame
	 * @return new instructions
	 */
	private AbstractInsnNode[] decryptMethodsAndRewrite(ClassNode cn, Class<?> callProxy, MethodNode m, MethodInsnNode min, Frame<ConstantValue> frame) {
		try {
			ConstantValue previous = frame.getStack(frame.getStackSize() - 1);
			ConstantValue prePrevious = frame.getStack(frame.getStackSize() - 2);
			if (previous.isInteger() && prePrevious.isInteger()) {
				String decryptedLDC = (String) callProxy.getDeclaredMethod(min.name, int.class, int.class).invoke(null, prePrevious.getInteger(), previous.getInteger());
				if (!Strings.isHighUTF(decryptedLDC)) {
					// avoid concurrent modification
					decrypted++;
					return new AbstractInsnNode[] { new InsnNode(POP2), new LdcInsnNode(decryptedLDC) };
				} else if (verbose) {
					logger.severe("Failed string array decryption in " + cn.name);
				}
			} else if (verbose) {
				logger.warning("Unexpected case, method is not feeded two ints " + cn.name + "." + m.name);
			}
		} catch (Throwable t) {
			logger.severe("Failure in " + cn.name + ": " + t.getClass().getName() + "-" + t.getMessage());
		}
		return new AbstractInsnNode[] { min };
	}

	/**
	 * Replace decrypted String[] and String fields in the code. This is the hardest part
	 * 
	 * @param proxy
	 */
	private AbstractInsnNode[] tryReplaceFieldLoads(ClassNode cn, Class<?> callProxy, MethodNode m, AbstractInsnNode ain, Frame<ConstantValue> frame, ClassNode proxy) {
		try {
			if (ain.getOpcode() == GETSTATIC) {
				FieldInsnNode fin = (FieldInsnNode) ain;
				if (isLocalField(cn, fin) && fin.desc.equals("Ljava/lang/String;")) {
					String decrypedString = (String) callProxy.getDeclaredField(fin.name).get(null);
					if (decrypedString == null) {
						logger.warning("Possible false call in " + cn.name + " or failed decryption, single field is null (" + fin.name + ")");
						// could be false call, not the decrypted string
						return new AbstractInsnNode[] { ain };
					} else {
						decrypted++;
						// i don't know why we need NOP, but it only works that way :confusion:
						return new AbstractInsnNode[] { new LdcInsnNode(decrypedString), new InsnNode(NOP) };
					}
				}
			} else if (ain.getOpcode() == AALOAD) {
				ConstantValue previous = frame.getStack(frame.getStackSize() - 1);
				ConstantValue prePrevious = frame.getStack(frame.getStackSize() - 2);

				if (previous.getValue() != null) {
					int arrayIndex = previous.getInteger();
					Object reference = prePrevious.getValue();
					if (reference != null && reference instanceof String[]) {
						String[] ref = (String[]) reference;
						String decryptedString = ref[arrayIndex];
						if (Strings.isHighUTF(decryptedString)) {
							logger.warning("String decryption in " + cn.name + "." + m.name + " may have failed");
						}
						decrypted++;
						return new AbstractInsnNode[] { new InsnNode(POP2), new LdcInsnNode(decryptedString) };
					}
				}
			}
		} catch (Throwable t) {
			if (verbose) {
				t.printStackTrace();
			}
			logger.severe("Failure in " + cn.name + ": " + t.getClass().getName() + "-" + t.getMessage());
		}
		return new AbstractInsnNode[] { ain };
	}

	private FieldInsnNode decryptedArrayField;
	private String[] decryptedFieldValue;

	@Override
	public Object getFieldValueOrNull(BasicValue v, String owner, String name, String desc) {
		return decryptedArrayField != null && decryptedArrayField.owner.equals(owner) && decryptedArrayField.name.equals(name) && decryptedArrayField.desc.equals(desc) ? decryptedFieldValue : null;
	}

	@Override
	public Object getMethodReturnOrNull(BasicValue v, String owner, String name, String desc, List<? extends ConstantValue> values) {
		return null;
	}

	@Override
	public ClassNode tryClassLoad(String name) {
		return null;
	}
}
