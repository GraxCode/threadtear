package me.nov.threadtear.execution.zkm;

import me.nov.threadtear.analysis.stack.ConstantValue;
import me.nov.threadtear.analysis.stack.IConstantReferenceHandler;
import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import me.nov.threadtear.util.asm.Access;
import me.nov.threadtear.util.reflection.DynamicReflection;
import me.nov.threadtear.vm.IVMReferenceHandler;
import me.nov.threadtear.vm.VM;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DESObfuscationZKM extends Execution implements IVMReferenceHandler, IConstantReferenceHandler {
  private boolean verbose;
  private int strings, encryptedStrings;
  private int references, encryptedReferences;
  private Map<String, Clazz> classes;
  private VM vm;
//  private List<ClassNode>

  public DESObfuscationZKM() {
    super(ExecutionCategory.ZKM, "ZKM DES case deobfuscator",
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
    this.vm = VM.constructVM(this);
//    final List<ClassNode> classNodes = classes.values().stream().map(c -> c.node).collect(Collectors.toList());
    Collection<Clazz> values = classes.values();
//    values.forEach(this::fixInterface);
    logger.info("Decrypting references...");
//    String s = "JVMInstr";
    values.stream()
//      .filter(this::hasDESEncryption)
//      .filter(clazz -> clazz.node.name.endsWith(s))
      .forEach(this::decryptReferences);
    logger.info("Decrypting strings...");
    values.stream()
//      .filter(clazz -> clazz.node.name.endsWith(s))
      .forEach(this::decryptStrings);
    int stringDecryptionRate = Math.round((this.strings / (float) this.encryptedStrings) * 100);
    int referenceDecryptionRate = Math.round((this.references / (float) this.encryptedReferences) * 100);
    logger.info("Decrypted {} strings ({}%) and {} references ({}%) successfully.",
      strings, stringDecryptionRate, references, referenceDecryptionRate);
    return references > 0 || strings > 0;
  }

  private void decryptStrings(Clazz clazz) {
    ClassNode classNode = clazz.node;
    for (MethodNode methodNode : classNode.methods) {
      InsnList instructions = methodNode.instructions;
      Set<InvokeDynamicInsnNode> nodes = this.getInvokeDynamicInstructions(
        methodNode, node -> node.getPrevious() != null && node.getPrevious().getOpcode() == LXOR
      );
      if (nodes.isEmpty()) {
        logger.debug("Skipping method {} without obfuscated strings...", referenceString(classNode, methodNode));
        continue;
      }
      encryptedStrings += nodes.size();
      long key = 0;
      for (InvokeDynamicInsnNode node : nodes) {
        Handle bsm = node.bsm;
        try {
          if (key == 0) {
            key = this.getFieldKey(classNode, node, instructions);
          }
          if (key == -1) {
            logger.warning("Failed to get key in {}", referenceString(classNode, methodNode));
            break;
          }
          Class<?> aClass = this.vm.loadClass(bsm.getOwner().replace("/", "."));
          Method stringDecryptionMethod = Arrays.stream(aClass.getDeclaredMethods())
            .filter(method -> method.getParameterCount() == 2)
            .filter(method -> Arrays.equals(method.getParameterTypes(), new Class[]{int.class, long.class}))
            .findFirst()
            .orElse(null);
          if (stringDecryptionMethod == null) {
            logger.warning("String decryption method in class {} is null?!", classNode.name);
            break;
          }
          stringDecryptionMethod.setAccessible(true);
          IntInsnNode sipush = (IntInsnNode) this.findFirstInstruction(node, SIPUSH);
          if (sipush == null) {
            logger.warning("Unable to find first key in {}", referenceString(classNode, methodNode));
            continue;
          }
          LdcInsnNode ldcInsnNode = (LdcInsnNode) sipush.getNext();
          long anotherNumber = (long) ldcInsnNode.cst;

          int first = sipush.operand;
          long second = anotherNumber ^ key;
          String decryptedString = (String) stringDecryptionMethod.invoke(null, first, second);
          instructions.insertBefore(node, new InsnNode(POP2));
          instructions.insertBefore(node, new InsnNode(POP));
          instructions.set(node, new LdcInsnNode(decryptedString));
          strings++;
        } catch (IncompatibleClassChangeError ignored) {
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
          if (verbose)
            logger.error("Error", error);
          logger.error("An exception was thrown while initializing class {}", error, classNode.name);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void decryptReferences(Clazz clazz) {
    ClassNode classNode = clazz.node;
    logger.info("Decrypting references in class {}...", classNode.name);
    MethodNode clinit = this.getStaticInitializer(classNode);
    if (clinit != null) {
//      BiPredicate<String, String> predicate = (name, desc) -> !name.equals(classNode.name)
//        && !name.matches("javax?/(lang|crypto)/.*");
//      Instructions.isolateCallsThatMatch(clinit, predicate, predicate);
    }
    for (MethodNode methodNode : classNode.methods) {
      InsnList instructions = methodNode.instructions;
      Set<InvokeDynamicInsnNode> nodes = this.getInvokeDynamicInstructions(methodNode, this::matchesPattern);
      if (nodes.isEmpty()) {
        logger.debug("Skipping method {} without obfuscated references...", methodNode.name);
        continue;
      }
      encryptedReferences += nodes.size();
      long key = 0;
      for (InvokeDynamicInsnNode node : nodes) {
        Handle bsm = node.bsm;
        try {
          if (key == 0) {
            key = this.getFieldKey(classNode, node, instructions);
          }
          if (key == -1) {
            logger.warning("Failed to get key in {}", referenceString(classNode, methodNode));
            break;
          }
          Class<?> bootstrapClass = this.vm.loadClass(bsm.getOwner().replace("/", "."));
          Method bootstrapMethod = bootstrapClass.getDeclaredMethod(bsm.getName(), MethodHandles.Lookup.class,
            MutableCallSite.class, String.class, MethodType.class, long.class, long.class);
          bootstrapMethod.setAccessible(true);
          LdcInsnNode ldcInsnNode = (LdcInsnNode) node.getPrevious().getPrevious();
          long anotherKey = (long) ldcInsnNode.cst;

          List<Object> args = new ArrayList<>(Arrays.asList(
            DynamicReflection.getTrustedLookup(), null, node.name,
            MethodType.fromMethodDescriptorString(node.desc, this.vm)
          ));
          args.add(anotherKey);
          args.add(key);

          MethodHandle methodHandle;
          try {
            methodHandle = (MethodHandle) bootstrapMethod.invoke(null, args.toArray());
          } catch (InvocationTargetException e) {
            if (verbose)
              logger.error("Exception", e);
            logger.error("Failed to get MethodHandle in {}, {}", referenceString(classNode, methodNode),
              shortStacktrace(e));
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
          instructions.remove(node.getPrevious().getPrevious());
          instructions.remove(node.getPrevious());
          instructions.set(node, instruction);

          references++;
        } catch (IncompatibleClassChangeError ignored) {
        } catch (ExceptionInInitializerError | NoClassDefFoundError error) {
          if (verbose)
            logger.error("Error", error);
          logger.error("An exception was thrown while initializing class {}", error, classNode.name);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
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

  private long getFieldKey(ClassNode classNode, InvokeDynamicInsnNode node, InsnList instructions) throws Exception {
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
      throw new IllegalStateException();
    Field field = this.findField(clazz, classNode);
    if (field == null) {
      return -1;
    }
    field.setAccessible(true);
    long key = (Long) field.get(null);
    return key ^ secondKey;
  }

  private Field findField(Class<?> clazz, ClassNode classNode) throws NoSuchFieldException {
    MethodNode clinit = this.getStaticInitializer(classNode);
    if (clinit == null)
      return null;
    FieldInsnNode fieldInsnNode = this.findFirstFieldInstruction(clinit);
    if (fieldInsnNode == null)
      return null;
    return clazz.getDeclaredField(fieldInsnNode.name);
  }

  private AbstractInsnNode findFirstInstruction(AbstractInsnNode start, int opcode) {
    for (; start != null; start = start.getPrevious()) {
      if (start.getOpcode() == opcode) {
        return start;
      }
    }
    return null;
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
      LdcInsnNode ldcInsnNode = (LdcInsnNode) previous.getPrevious();
      return (long) ldcInsnNode.cst;
    }
    return -1;
  }

  private Set<InvokeDynamicInsnNode> getInvokeDynamicInstructions(MethodNode methodNode,
                                                                  Predicate<InvokeDynamicInsnNode> predicate) {
    return Arrays.stream(methodNode.instructions.toArray())
      .filter(node -> node.getOpcode() == INVOKEDYNAMIC)
      .map(node -> (InvokeDynamicInsnNode) node)
      .filter(predicate)
      .filter(node -> node.bsm != null)
      .filter(node -> !node.bsm.getName().equals("metafactory"))
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
    return classes.containsKey(name) ? classes.get(name).node : null;
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
