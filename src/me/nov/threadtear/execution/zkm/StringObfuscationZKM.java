package me.nov.threadtear.execution.zkm;

import java.util.ArrayList;
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

import me.nov.threadtear.Threadtear;
import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.analysis.ConstantTracker;
import me.nov.threadtear.asm.analysis.ConstantValue;
import me.nov.threadtear.asm.analysis.IReferenceHandler;
import me.nov.threadtear.asm.util.Access;
import me.nov.threadtear.asm.util.Instructions;
import me.nov.threadtear.asm.vm.IVMReferenceHandler;
import me.nov.threadtear.asm.vm.Sandbox;
import me.nov.threadtear.asm.vm.VM;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import me.nov.threadtear.util.Strings;

public class StringObfuscationZKM extends Execution implements IVMReferenceHandler, IReferenceHandler {

	private ArrayList<Clazz> classes;
	private int decrypted;

	private boolean verbose;

	private static final String ENCHANCED_MODE_METHOD_DESC = "(II)Ljava/lang/String;";

	public StringObfuscationZKM() {
		super(ExecutionCategory.ZKM, "String obfuscation removal", "Tested on ZKM 5 - 11, could work on newer versions too.<br>" + "<i>String encryption using DES Cipher is currently <b>NOT</b> supported.</i>", ExecutionTag.RUNNABLE,
				ExecutionTag.POSSIBLY_MALICIOUS);
	}

	/*
	 * TODO: instead of checking through bytecode, stack analysis should be made,
	 * because ZKM often abuses stack and pushes ints or getfields not in the place
	 * where they normally are!
	 * 
	 * TODO: String encryption using DES Cipher (probably only in combination with
	 * reflection obfuscation
	 */

	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose) {
		this.classes = classes;
		this.verbose = verbose;
		classes.stream().map(c -> c.node).filter(this::hasZKMBlock).forEach(this::decrypt);
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
		// cut out decryption part and make proxy
		MethodNode callMethod = Sandbox.createMethodProxy(Instructions.isolateCallsThatMatch(cn, clinit, (s) -> !s.equals(cn.name) && !s.matches(ALLOWED_CALLS)), "clinitProxy", "()V");
		cn.methods.add(callMethod);
		try {
			invokeVMAndReplace(cn);
		} catch (Throwable e) {
			Threadtear.logger.severe("Failed to run proxy in " + cn.name + ":" + e.getMessage());
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

			decryptedField = null;
			m.instructions.forEach(ain -> {
				if (isLocalField(cn, ain) && ((FieldInsnNode) ain).desc.equals("[Ljava/lang/String;")) {
					decryptedField = (FieldInsnNode) ain;
					try {
						decryptedFieldValue = (String[]) callProxy.getField(((FieldInsnNode) ain).name).get(null);
					} catch (Exception e) {
					}
				}
			});

			Analyzer<ConstantValue> a = new Analyzer<ConstantValue>(new ConstantTracker(this, Access.isStatic(m.access), m.maxLocals, m.desc, new Object[0]));
			try {
				a.analyze(cn.name, m);
			} catch (AnalyzerException e) {
				Threadtear.logger.severe("Failed stack analysis in " + cn.name + "." + m.name + ":" + e.getMessage());
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
				if (isZKMMethod(cn, ain)) {
					for (AbstractInsnNode newInstr : decryptMethodsAndRewrite(cn, callProxy, m, (MethodInsnNode) ain, frame)) {
						rewrittenCode.add(newInstr.clone(labels));
					}
				} else {
					for (AbstractInsnNode newInstr : tryReplaceFieldLoads(cn, callProxy, m, ain, frame)) {
						rewrittenCode.add(newInstr.clone(labels));
					}
				}
			}
			m.instructions = rewrittenCode;
			m.tryCatchBlocks.forEach(tcb -> {
				tcb.start = labels.get(tcb.start);
				tcb.end = labels.get(tcb.end);
				tcb.handler = labels.get(tcb.handler);
			});
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
	 * Replace decryption methods that take two ints as argument and returns the
	 * decrypted String. This does only occur sometimes!
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
	 * Replace decrypted String[] and String fields in the code. This is the hardest
	 * part
	 */
	private AbstractInsnNode[] tryReplaceFieldLoads(ClassNode cn, Class<?> callProxy, MethodNode m, AbstractInsnNode ain, Frame<ConstantValue> frame) {
		try {
			if (ain.getOpcode() == GETSTATIC) {
				FieldInsnNode fin = (FieldInsnNode) ain;
				if (isLocalField(cn, fin) && fin.desc.equals("Ljava/lang/String;")) {
					String decrypedString = (String) callProxy.getField(fin.name).get(null);
					if (decrypedString == null) {
						logger.warning("Possible false call in " + cn.name + " or failed decryption, single field is null");
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

	private FieldInsnNode decryptedField;
	private String[] decryptedFieldValue;

	@Override
	public Object getFieldValueOrNull(BasicValue v, String owner, String name, String desc) {
		return decryptedField != null && decryptedField.owner.equals(owner) && decryptedField.name.equals(name) && decryptedField.desc.equals(desc) ? decryptedFieldValue : null;
	}

	@Override
	public Object getMethodReturnOrNull(BasicValue v, String owner, String name, String desc, List<? extends ConstantValue> values) {
		return null;
	}

	@Override
	public ClassNode tryClassLoad(String name) {
		return classes.stream().map(c -> c.node).filter(c -> c.name.equals(name)).findFirst().orElse(null);
	}
}
