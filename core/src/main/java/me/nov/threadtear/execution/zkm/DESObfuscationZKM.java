package me.nov.threadtear.execution.zkm;

import me.nov.threadtear.analysis.stack.ConstantValue;
import me.nov.threadtear.analysis.stack.IConstantReferenceHandler;
import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import me.nov.threadtear.util.asm.Access;
import me.nov.threadtear.util.asm.InstructionModifier;
import me.nov.threadtear.util.asm.Instructions;
import me.nov.threadtear.util.asm.References;
import me.nov.threadtear.util.reflection.DynamicReflection;
import me.nov.threadtear.vm.IVMReferenceHandler;
import me.nov.threadtear.vm.Sandbox;
import me.nov.threadtear.vm.VM;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import javax.crypto.BadPaddingException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DESObfuscationZKM extends Execution implements IVMReferenceHandler, IConstantReferenceHandler {
  private static final String ZKM_INVOKEDYNAMIC_HANDLE_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;" +
    "Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";
  private static final String ZKM_STRING_INVOKEDYNAMIC_DESC = "\\([IJ]+\\)Ljava/lang/String;";
  private static final String ZKM_INVOKEDYNAMIC_REAL_BOOTSTRAP_DESC_REGEX = "\\(Ljava/lang/invoke/MethodHandles" +
    "\\$Lookup;Ljava/lang/invoke/MutableCallSite;Ljava/lang/String;Ljava/lang/invoke/MethodType;[JI]+\\)" +
    "Ljava/lang/invoke/MethodHandle;";
  private static final String ZKM_REFERENCE_DESC_REGEX = "\\((?:L.*;)?J+\\)(?:\\[?(?:I|J|(?:L.*;)))";

  private boolean verbose;
  private int strings, encryptedStrings;
  private int references, encryptedReferences;
  private Map<String, Clazz> classes;

  public DESObfuscationZKM() {
    super(ExecutionCategory.ZKM, "ZKM DES case deobfuscator (WIP, unstable)",
      "Deobfuscates string / access obfuscation with DES cipher." +
        "<br>Tested on ZKM 14, could work on newer versions too.", ExecutionTag.POSSIBLE_DAMAGE,
      ExecutionTag.POSSIBLY_MALICIOUS);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    this.verbose = verbose;
    this.references = 0;
    this.encryptedReferences = 0;
    this.strings = 0;
    this.encryptedStrings = 0;
    this.classes = classes;
//    final List<ClassNode> classNodes = classes.values().stream().map(c -> c.node).collect(Collectors.toList());
    Collection<Clazz> values = classes.values();
//    values.forEach(this::fixInterface);
    logger.info("Decrypting references...");
//    String s = "constantpool/";
    values.stream()
//      .filter(this::hasDESEncryption)
//      .filter(clazz -> clazz.node.name.contains(s))
      .forEach(this::decryptReferences);
//    logger.info("Decrypting strings...");
//    values.stream()
////      .filter(clazz -> clazz.node.name.endsWith(s))
//      .forEach(this::decryptStrings);
    int stringDecryptionRate = Math.round((this.strings / (float) this.encryptedStrings) * 100);
    int referenceDecryptionRate = Math.round((this.references / (float) this.encryptedReferences) * 100);
    logger.info("Decrypted {} strings ({}%) and {} references ({}%) successfully.",
      strings, stringDecryptionRate, references, referenceDecryptionRate);
    return references > 0 || strings > 0;
  }

//  private void decryptStrings(Clazz clazz) {
//    ClassNode classNode = clazz.node;
//    for (MethodNode methodNode : classNode.methods) {
//      InsnList instructions = methodNode.instructions;
//      Set<InvokeDynamicInsnNode> nodes = this.getInvokeDynamicInstructions(
//        methodNode, node -> node.getPrevious() != null
//      );
//      if (nodes.isEmpty()) {
//        logger.debug("Skipping method {} without obfuscated strings...", referenceString(classNode, methodNode));
//        continue;
//      }
//      encryptedStrings += nodes.size();
//      long key = 0;
//      for (InvokeDynamicInsnNode node : nodes) {
//        Handle bsm = node.bsm;
//        try {
//          if (key == 0) {
//            key = this.getFieldKey(classNode, node, instructions);
//          }
//          if (key == -1) {
//            logger.warning("Failed to get key in {}", referenceString(classNode, methodNode));
//            break;
//          }
//          Class<?> aClass = this.vm.loadClass(bsm.getOwner().replace("/", "."));
//          Method stringDecryptionMethod = Arrays.stream(aClass.getDeclaredMethods())
//            .filter(method -> method.getParameterCount() == 2)
//            .filter(method -> Arrays.equals(method.getParameterTypes(), new Class[]{int.class, long.class})
//              || Arrays.equals(method.getParameterTypes(), new Class[]{long.class, long.class}))
//            .findFirst()
//            .orElse(null);
//          if (stringDecryptionMethod == null) {
//            logger.warning("String decryption method in class {} is null?!", classNode.name);
//            break;
//          }
//          stringDecryptionMethod.setAccessible(true);
//          IntInsnNode sipush = (IntInsnNode) this.findFirstInstruction(node, SIPUSH);
//          if (sipush == null) {
//            logger.warning("Unable to find first key in {}", referenceString(classNode, methodNode));
//            continue;
//          }
//          LdcInsnNode ldcInsnNode = (LdcInsnNode) sipush.getNext();
//          long anotherNumber = (long) ldcInsnNode.cst;
//
//          int first = sipush.operand;
//          long second = anotherNumber ^ key;
//          String decryptedString = (String) stringDecryptionMethod.invoke(null, first, second);
//          instructions.insertBefore(node, new InsnNode(POP2));
//          instructions.insertBefore(node, new InsnNode(POP));
//          instructions.set(node, new LdcInsnNode(decryptedString));
//          strings++;
//        } catch (IncompatibleClassChangeError ignored) {
//        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
//          if (verbose)
//            logger.error("Error", error);
//          logger.error("An exception was thrown while initializing class {}", error, classNode.name);
//        } catch (Exception e) {
//          e.printStackTrace();
//        }
//      }
//    }
//  }

  private void decryptReferences(Clazz clazz) {
    logger.collectErrors(clazz);
    ClassNode classNode = clazz.node;
    logger.info("Decrypting references in class {}...", classNode.name);
    MethodNode clinit = super.getStaticInitializer(classNode);
    if (clinit != null) {
      BiPredicate<String, String> predicate = (owner, desc) -> !owner.equals(classNode.name)
        && !owner.matches("javax?/(lang|util|crypto)/.*")
        && !desc.matches("\\[?Ljava/lang/String;|J")
        && !desc.matches("\\(JJLjava/lang/Object;\\)L.+;")
        && !desc.equals("(J)J")
        && !desc.matches(ZKM_REFERENCE_DESC_REGEX);
      Instructions.isolateCallsThatMatch(clinit, predicate, predicate);
    }
    if (clinit == null)
      return;
    ClassNode proxyNode = this.getProxy(classNode, clinit);
    if (proxyNode == null)
      return;
    Map<String, String> singleMap = Collections.singletonMap(classNode.name, proxyNode.name);
    proxyNode.methods.stream()
      .map(m -> m.instructions.toArray())
      .flatMap(Arrays::stream)
      .forEach(ain -> References.remapClassRefs(singleMap, ain));
    proxyNode.fields.forEach(fieldNode -> References.remapFieldType(singleMap, fieldNode));

    VM vm = VM.constructVM(this);
    try {
      this.invokeVM(classNode, proxyNode, vm);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof BadPaddingException) {
//        logger.warning("Skipping class {} because the key could not be calculated correctly...",
//          referenceString(classNode, null));
//        return;
      }
      e.printStackTrace();
//      else if (e.getCause() instanceof RuntimeException && e.getCause().getMessage().contains("NoSuchMethodException")) {
//
//      }
    } catch (Throwable e) {
      logger.error("Failed to invoke proxy in {}, {}", referenceString(classNode, null), shortStacktrace(e));
      e.printStackTrace();
      return;
    }
    for (MethodNode methodNode : classNode.methods) {
      if (methodNode.name.equals("clinitProxy"))
        methodNode.name = "<clinit>";
//      if (!methodNode.name.equals("<clinit>"))
//        continue;
      Set<InvokeDynamicInsnNode> nodes = this.invokeDynamicsWithoutStrings(methodNode);
      if (nodes.isEmpty()) {
        logger.debug("Skipping method {} without obfuscated references...", methodNode.name);
        continue;
      }
      InsnList instructions = methodNode.instructions;
      InstructionModifier modifier = new InstructionModifier();
      encryptedReferences += nodes.size();
      Frame<ConstantValue>[] frames = getConstantFrames(classNode, methodNode, this);
      long key = 0;
      nodes:
      for (InvokeDynamicInsnNode node : nodes) {
        Handle bsm = node.bsm;
        try {
          if (key == 0) {
            key = this.getFieldKey(proxyNode, node, instructions, vm);
          }
          if (key == -1) {
            logger.warning("Failed to get key in {}", referenceString(classNode, methodNode));
            break;
          }
          Class<?> bootstrapClass = vm.loadClass(bsm.getOwner().replace("/", "."));
          Method bootstrapMethod = Arrays.stream(bootstrapClass.getDeclaredMethods())
            .filter(method -> Type.getMethodDescriptor(method)
              .matches(ZKM_INVOKEDYNAMIC_REAL_BOOTSTRAP_DESC_REGEX))
            .findFirst()
            .orElse(null);
          if (bootstrapMethod == null) {
            logger.error("Failed to find bootstrap method to get method handle.");
            break;
          }
          bootstrapMethod.setAccessible(true);

          List<Object> args = new ArrayList<>(Arrays.asList(
            DynamicReflection.getTrustedLookup(), null, node.name,
            MethodType.fromMethodDescriptorString(node.desc, vm)
          ));

          Frame<ConstantValue> frame = frames[instructions.indexOf(node)];
          int parameterCount = Type.getArgumentTypes(Type.getMethodDescriptor(bootstrapMethod)).length - 4;
          for (int i = 0; i < parameterCount - 1; i++) {
            ConstantValue constantValue = frame.getStack(frame.getStackSize() - parameterCount + i);
            if (!constantValue.isKnown()) {
              logger.warning("Stack value depth {} is unknown in {}, could be decryption class itself", i,
                referenceString(classNode, null));
              break nodes;
            }
            Object value = constantValue.getValue();
            args.add(value);
          }
          args.add(key);

          MethodHandle methodHandle;
          try {
            methodHandle = (MethodHandle) bootstrapMethod.invoke(null, args.toArray());
          } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (verbose)
              logger.error("Exception", e);
            if (cause instanceof ArrayIndexOutOfBoundsException) {
              logger.warning("Something went wrong while invoking a bootstrap method", shortStacktrace(cause));
              return;
            }
            logger.error("Failed to get MethodHandle in {}, {}", referenceString(classNode, methodNode),
              shortStacktrace(cause));
//            e.printStackTrace();
            continue;
          }
          MethodHandleInfo methodHandleInfo = DynamicReflection.revealMethodInfo(methodHandle);
          AbstractInsnNode instruction = DynamicReflection.getInstructionFromHandleInfo(methodHandleInfo);

//          Type[] decryptionTypes = Type.getArgumentTypes(methodHandle.type().toMethodDescriptorString());
//          Type[] realTypes = Type.getArgumentTypes(methodHandleInfo.getMethodType().toMethodDescriptorString());
//          int extraArgs = decryptionTypes.length - realTypes.length; // difference equals extra count
//          if (instruction.getOpcode() != INVOKESTATIC && instruction.getOpcode() != GETSTATIC
//            && instruction.getOpcode() != PUTSTATIC) {
//            // object reference is an argument on the handle, we do not want to pop it
//            extraArgs--;
//          }
//          for (int i = 0; i < extraArgs; i++) {
//            Type t = decryptionTypes[decryptionTypes.length - 1 - i];
//            instructions.insertBefore(node, new InsnNode(t.getSize() > 1 ? POP2 : POP));
//            // pop off remaining decryption values
//          }
//          instructions.insertBefore(node, new InsnNode(POP2));
//          instructions.insertBefore(node, new InsnNode(POP2));
//          instructions.set(node, instruction);
          modifier.replace(node, new InsnNode(POP2), new InsnNode(POP2), instruction);

          references++;
        } catch (IncompatibleClassChangeError ignored) {
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
          if (verbose)
            logger.error("Error", error);
          logger.error("An exception was thrown while initializing class {}", error, classNode.name);
          error.printStackTrace();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      modifier.apply(methodNode);
    }
  }

  private void invokeVM(ClassNode classNode, ClassNode proxyNode, VM vm) throws Exception {
//    vm.explicitlyPreload(proxyNode, true);
    Class<?> clazz = vm.loadClass(classNode.name.replace("/", "."));
    try {
      clazz.getMethod("clinitProxy").invoke(null);
    } catch (InvocationTargetException e) {
      if (!(e.getCause() instanceof NullPointerException)) {
        throw e;
      } else {
        logger.info("NPE in " + classNode.name);
      }
    }
  }

  private ClassNode getProxy(ClassNode classNode, MethodNode clinit) {
    if (clinit == null)
      return null;
    ClassNode proxyClass = Sandbox.createClassProxy(classNode.name);
    if (Access.isEnum(classNode.access)) {
      proxyClass.access |= ACC_ENUM | ACC_FINAL;
      proxyClass.superName = "java/lang/Enum";
    }
    if ((classNode.access & ACC_SUPER) != 0) {
      proxyClass.access |= ACC_SUPER;
    }
    if (!classNode.superName.equals("java/lang/Object")) {
      proxyClass.superName = classNode.superName;
    }
//    Instructions.isolateCallsThatMatch(clinit,
//      (owner, desc) -> !owner.equals(classNode.name) && !owner.matches("javax?/(lang|crypto)/.*"),
//      (owner, desc) -> !owner.equals(classNode.name) && !owner.matches("javax?/(lang|crypto)/.*")
//        || !desc.matches("\\[?Ljava/lang/String;")
//    );

    List<MethodNode> methods = classNode.methods;
    int clinitIndex = methods.indexOf(clinit);
    Set<MethodNode> copiedMethods = new HashSet<>(methods.subList(clinitIndex + 1, methods.size()));

    Set<MethodNode> methodInvocations = Arrays.stream(clinit.instructions.toArray())
      .filter(node -> node instanceof MethodInsnNode)
      .map(node -> (MethodInsnNode) node)
      .filter(node -> node.owner.equals(classNode.name))
      .flatMap(node -> classNode.methods.stream()
        .filter(methodNode -> methodNode.name.equals(node.name) && methodNode.desc.equals(node.desc)))
      .collect(Collectors.toSet());

    Set<MethodNode> proxyMethods = new HashSet<>();
    proxyMethods.addAll(copiedMethods);
    proxyMethods.addAll(methodInvocations);
    proxyClass.methods.addAll(proxyMethods);
    clinit.name = "clinitProxy";
    proxyClass.methods.add(clinit);

//    Set<FieldNode> fieldNodes;
//    if (Access.isEnum(classNode.access)) {
//      fieldNodes = new HashSet<>(classNode.fields);
//    } else {
//      fieldNodes = classNode.fields.stream()
//        .filter(it -> Access.isStatic(it.access))
//        .filter(it -> it.desc.equals("J") || it.desc.equals("Ljava/util/Map;")
//          || it.desc.matches("\\[?Ljava/lang/String;"))
//        .collect(Collectors.toSet());
//    }
//    if (fieldNodes.isEmpty())
//      return null;
//    proxyClass.fields.addAll(fieldNodes);
    proxyClass.fields.addAll(classNode.fields);
    return proxyClass;
  }

  private void fixInterface(Clazz clazz) {
    ClassNode classNode = clazz.node;
    if (!Access.isInterface(classNode.access)) {
      return;
    }
    MethodNode clinit = this.getStaticInitializer(classNode);
    if (clinit == null || !this.hasDESEncryption(clazz)) {
      return;
    }
//    classNode.access &= ~ACC_INTERFACE;
    classNode.methods.clear();
  }

  private long getFieldKey(
    ClassNode classNode,
    InvokeDynamicInsnNode node,
    InsnList instructions,
    VM vm
  ) throws Exception {
    VarInsnNode varInsnNode;
    AbstractInsnNode previous = node.getPrevious();
    if (previous instanceof VarInsnNode) {
      varInsnNode = (VarInsnNode) previous;
    } else if (previous.getPrevious() instanceof VarInsnNode) {
      varInsnNode = (VarInsnNode) previous.getPrevious();
    } else {
      return -1;
    }
    int variableIndex = varInsnNode.var;
    long secondKey = this.searchForSecondKey(variableIndex, instructions);
    if (secondKey == -1)
      return -1;
    Class<?> clazz = vm.loadClass(classNode.name.replace("/", "."));
    if (clazz == null)
      throw new IllegalStateException("Could not find or load class " + classNode.name);
    Field field = this.findField(clazz, classNode);
    if (field == null) {
      return -1;
    }
    field.setAccessible(true);
    long key = (Long) field.get(null);
    return key ^ secondKey;
  }

  private Field findField(Class<?> clazz, ClassNode classNode) throws NoSuchFieldException {
    MethodNode clinit = super.getMethod(classNode, "clinitProxy", "()V");
    if (clinit == null)
      return null;
    FieldInsnNode fieldInsnNode = this.findFirstFieldInstruction(clinit);
    if (fieldInsnNode == null)
      return null;
    return clazz.getDeclaredField(fieldInsnNode.name);
  }

  private FieldInsnNode findFirstFieldInstruction(MethodNode clinit) {
    for (AbstractInsnNode node : clinit.instructions) {
      if (node.getOpcode() != PUTSTATIC)
        continue;
      FieldInsnNode fieldInsnNode = (FieldInsnNode) node;
      if (!fieldInsnNode.desc.equals("J"))
        break;
      return fieldInsnNode;
    }
    return null;
  }

  private AbstractInsnNode findFirstInstruction(AbstractInsnNode start, int opcode) {
    for (; start != null; start = start.getPrevious()) {
      if (start.getOpcode() == opcode) {
        return start;
      }
    }
    return null;
  }

  /*
    getstatic <key_field>
    ldc <second_key>
    lxor
    lstore <variable_index>
   */
  private long searchForSecondKey(int variableIndex, InsnList instructions) {
    for (AbstractInsnNode node : instructions.toArray()) {
      if (node.getOpcode() != LSTORE)
        continue;
      VarInsnNode varInsnNode = (VarInsnNode) node;
      if (varInsnNode.var != variableIndex)
        continue;
      AbstractInsnNode previous = varInsnNode.getPrevious();
      if (previous == null || previous.getPrevious() == null || !(previous.getPrevious() instanceof LdcInsnNode))
        continue;
      //TODO: optimize/replace with ConstantValue on the stack
      LdcInsnNode ldcInsnNode = (LdcInsnNode) previous.getPrevious();
      return (long) ldcInsnNode.cst;
    }
    return -1;
  }

  private Set<InvokeDynamicInsnNode> invokeDynamicsWithoutStrings(MethodNode methodNode) {
    return this.getInvokeDynamicInstructions(methodNode)
      .stream()
      .filter(node -> !node.desc.matches(ZKM_STRING_INVOKEDYNAMIC_DESC))
      .collect(Collectors.toSet());
  }

  private Set<InvokeDynamicInsnNode> getInvokeDynamicInstructions(MethodNode methodNode) {
    return this.getInvokeDynamicInstructions(methodNode,
      node -> node.bsm.getDesc().equals(ZKM_INVOKEDYNAMIC_HANDLE_DESC)
    );
  }

  private Set<InvokeDynamicInsnNode> getInvokeDynamicInstructions(
    MethodNode methodNode, Predicate<InvokeDynamicInsnNode> predicate
  ) {
    if (predicate == null)
      predicate = __ -> true;
    return Arrays.stream(methodNode.instructions.toArray())
      .filter(node -> node.getOpcode() == INVOKEDYNAMIC)
      .map(node -> (InvokeDynamicInsnNode) node)
      .filter(node -> node.bsm != null)
      .filter(node -> !node.bsm.getName().equals("metafactory"))
      .filter(predicate)
      .collect(Collectors.toSet());
  }

  private boolean matchesPattern(AbstractInsnNode node) {
    AbstractInsnNode previous = node.getPrevious();
    return previous != null && previous.getOpcode() == LLOAD
      && previous.getPrevious() != null && previous.getPrevious().getOpcode() == LDC;
  }

  //TODO: implement better DES encryption check for classes without static initializer but obfuscation
  private boolean hasDESEncryption(Clazz c) {
    ClassNode cn = c.node;
    if (Access.isInterface(cn.access))
      return false;
    MethodNode mn = getStaticInitializer(cn);
    if (mn == null)
      return false;
    return StreamSupport.stream(mn.instructions.spliterator(), false)
      .anyMatch(ain -> ain.getType() == AbstractInsnNode.LDC_INSN &&
        "DES/CBC/PKCS5Padding".equals(((LdcInsnNode) ain).cst));
  }

  @Override
  public String getAuthor() {
    return "iamkyaru";
  }

  @Override
  public ClassNode tryClassLoad(String name) {
    return this.classes.containsKey(name) ? this.classes.get(name).node : null;
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
