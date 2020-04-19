package me.nov.threadtear.execution.stringer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
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
import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.util.Access;
import me.nov.threadtear.asm.util.Instructions;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import me.nov.threadtear.util.Casts;
import me.nov.threadtear.util.Descriptor;
import me.nov.threadtear.util.Strings;
import me.nov.threadtear.vm.IVMReferenceHandler;
import me.nov.threadtear.vm.Sandbox;
import me.nov.threadtear.vm.VM;

public class StringObfuscationStringer extends Execution implements IVMReferenceHandler, IConstantReferenceHandler {

	private static final String STRINGER_DECRPYTION_METHOD_DESC_REGEX = "\\(Ljava/lang/Object;.?.?.?\\)Ljava/lang/String;";
	private ArrayList<Clazz> classes;
	private int encrypted;
	private int decrypted;
	private boolean verbose;

	public StringObfuscationStringer() {
		super(ExecutionCategory.STRINGER, "String obfuscation removal", "Works for version 3 - 9.<br>Make sure to decrypt access obfuscation first.", ExecutionTag.RUNNABLE, ExecutionTag.POSSIBLY_MALICIOUS);
	}

	/*
	 * this works as following:
	 * 
	 * find decryption method via regex
	 * 
	 * create proxy class with same method name and same class name
	 * 
	 * in method, invoke decryption method and set result to field in class
	 * 
	 * method parameters are loaded with fields (getstatic) but from another class,
	 * because reflection calls <clinit> when setting a field (fuck you, java) (and
	 * it's impossible to change method names, stringer forces us to)
	 * 
	 * method is run -> replace in code
	 * 
	 * 
	 */

	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose) {
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
		logger.info("Of a total " + encrypted + " encrypted strings, " + (decryptionRatio) + "% were successfully decrypted");
		return decryptionRatio > 0.25;
	}

	private void decrypt(ClassNode cn) {

		cn.methods.forEach(m -> {

			Analyzer<ConstantValue> a = new Analyzer<ConstantValue>(new ConstantTracker(this, Access.isStatic(m.access), m.maxLocals, m.desc, new Object[0]));
			try {
				a.analyze(cn.name, m);
			} catch (AnalyzerException e) {
				if (verbose) {
					e.printStackTrace();
				}
				logger.severe("Failed stack analysis in " + cn.name + "." + m.name + ":" + e.getMessage());
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
				for (AbstractInsnNode newInstr : tryReplaceMethods(cn, m, ain, frame)) {
					rewrittenCode.add(newInstr.clone(labels));
				}
			}
			Instructions.updateInstructions(m, labels, rewrittenCode);
		});
	}

	private AbstractInsnNode[] tryReplaceMethods(ClassNode cn, MethodNode m, AbstractInsnNode ain, Frame<ConstantValue> frame) {
		if (ain.getOpcode() == INVOKESTATIC) {
			MethodInsnNode min = (MethodInsnNode) ain;
			if (min.desc.matches(STRINGER_DECRPYTION_METHOD_DESC_REGEX)) {
				try {
					encrypted++;
					String realString = invokeProxy(cn, m, min, frame);
					if (realString != null) {
						if (Strings.isHighUTF(realString)) {
							logger.warning("String may have not decrypted correctly in " + cn.name + "." + m.name + m.desc);
						} else {
							this.decrypted++;
						}
						return new AbstractInsnNode[] { min, new InsnNode(POP), new LdcInsnNode(realString) };
					} else {
						logger.severe("Failed to decrypt string in " + cn.name + "." + m.name + m.desc);
					}
				} catch (Throwable e) {
					e.printStackTrace();
					logger.severe("Failed to decrypt string in " + cn.name + "." + m.name + m.desc + ": " + e.getClass().getName() + ", " + e.getMessage());
				}
			}
		}
		return new AbstractInsnNode[] { ain };
	}

	private String invokeProxy(ClassNode cn, MethodNode m, MethodInsnNode min, Frame<ConstantValue> frame) throws Exception {
		VM vm = VM.constructVM(this);
		createFakeCloneAndFieldGetter(cn, m, min, frame); // create a duplicate of the current class,
		// we need this because stringer checks for stacktrace method name and class

		Class<?> proxyFieldClass = vm.loadClass(invocationFieldClass.name.replace('/', '.'), true);
		// set proxyFields to stack values
		ArrayList<String> args = Descriptor.splitArguments(min.desc.substring(1, min.desc.lastIndexOf(')')));
		if (frame == null) {
			if (verbose) {
				logger.severe("Unvisited frame in " + cn.name + "." + m.name + ": " + frame);
			}
			return null;
		}
		if (args.size() > frame.getStackSize()) {
			if (verbose) {
				logger.severe("Stack has not enough values in " + cn.name + "." + m.name + ": " + frame);
			}
			return null;
		}
		for (int i = 0; i < args.size(); i++) {
			Field proxyField = proxyFieldClass.getDeclaredField("proxyField_" + i);
			ConstantValue stackValue = frame.getStack(frame.getStackSize() - args.size() + i);
			if (!stackValue.isKnown()) {
				if (verbose) {
					logger.severe("Stack index " + i + " is unknown in " + cn.name + "." + m.name + ": field type: " + proxyField.getType().getName() + ", stack type: " + stackValue.getType());
				}
				return null;
			}
			proxyField.set(null, Casts.castWithPrimitives(proxyField.getType(), stackValue.getValue()));
		}

		vm.loadClass(min.owner.replace('/', '.'), true); // load decryption class, this class will load another class (some type of map)
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
		return (String) loadedClone.getDeclaredField("proxyReturn").get(null);
	}

	private void createFakeCloneAndFieldGetter(ClassNode cn, MethodNode m, MethodInsnNode min, Frame<ConstantValue> frame) {
		ClassNode node = Sandbox.createClassProxy(cn.name);
		ClassNode fieldClass = Sandbox.createClassProxy("ProxyFields"); // we can't put the fields in the same class, as setting them via reflection
																																		// would run <clinit>
		InsnList instructions = new InsnList();
		ArrayList<String> args = Descriptor.splitArguments(min.desc.substring(1, min.desc.lastIndexOf(')')));
		for (int i = 0; i < args.size(); i++) {
			// make fields as stack placeholder, that's the easiest way of transferring
			// stack to method
			String desc = args.get(i);
			fieldClass.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, "proxyField_" + i, desc, null, null));
			instructions.add(new FieldInsnNode(GETSTATIC, fieldClass.name, "proxyField_" + i, desc));
		}
		instructions.add(min.clone(null)); // we can clone original method here
		instructions.add(new FieldInsnNode(PUTSTATIC, node.name, "proxyReturn", "Ljava/lang/String;"));
		instructions.add(new InsnNode(RETURN));

		node.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, "proxyReturn", "Ljava/lang/String;", null, null));
		node.methods.add(Sandbox.createMethodProxy(instructions, m.name, "()V")); // method should return real string
		fakeInvocationClone = node;
		invocationFieldClass = fieldClass;
	}

	private ClassNode fakeInvocationClone;
	private ClassNode invocationFieldClass;

	@Override
	public ClassNode tryClassLoad(String name) {
		if (name.equals(fakeInvocationClone.name)) {
			return fakeInvocationClone;
		}
		if (name.equals(invocationFieldClass.name)) {
			return invocationFieldClass;
		}
		// try to find decryption class in loaded jar file
		return classes.stream().map(c -> c.node).filter(c -> c.name.equals(name)).findFirst().orElse(null);
	}

	@Override
	public Object getFieldValueOrNull(BasicValue v, String owner, String name, String desc) {
		return null;
	}

	@Override
	public Object getMethodReturnOrNull(BasicValue v, String owner, String name, String desc, List<? extends ConstantValue> values) {
		if (name.equals("toCharArray") && owner.equals("java/lang/String")) {
			if (!values.get(0).isKnown()) {
				if (verbose) {
					logger.severe("String that should be converted to char[] is unknown");
				}
				return null;
			}
			// allow char array method
			return ((String) values.get(0).getValue()).toCharArray();
		}
		return null;
	}
}