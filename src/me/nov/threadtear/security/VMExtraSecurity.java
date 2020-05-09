package me.nov.threadtear.security;

import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import me.nov.threadtear.Threadtear;
import me.nov.threadtear.io.Conversion;

public class VMExtraSecurity implements Opcodes {
  public static InsnList rewriteSetAccessible0(String blockRegex) {
    InsnList il = new InsnList();
    LabelNode jump = new LabelNode();
    il.add(new VarInsnNode(ALOAD, 0));
    il.add(new TypeInsnNode(INSTANCEOF, "java/lang/reflect/Member"));
    il.add(new JumpInsnNode(IFEQ, jump));
    il.add(new VarInsnNode(ALOAD, 0));
    il.add(new TypeInsnNode(CHECKCAST, "java/lang/reflect/Member"));
    il.add(new MethodInsnNode(INVOKEINTERFACE, "java/lang/reflect/Member", "getName", "()Ljava/lang/String;"));
    il.add(new LdcInsnNode(blockRegex));
    il.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "matches", "(Ljava/lang/String;)Z"));
    il.add(new JumpInsnNode(IFEQ, jump));
    il.add(new InsnNode(RETURN));
    il.add(jump);
    il.add(new FrameNode(F_NEW, 2, new Object[] { "java/lang/reflect/AccessibleObject", INTEGER }, 0, null));
    il.add(new VarInsnNode(ALOAD, 0));
    il.add(new VarInsnNode(ILOAD, 1));
    il.add(new FieldInsnNode(PUTFIELD, "java/lang/reflect/AccessibleObject", "override", "Z"));
    il.add(new InsnNode(RETURN));
    return il;
  }

  public static ClassNode rewriteAccessibleObject() {
    String classFile = "/java/lang/reflect/AccessibleObject.class";
    URL url = VMExtraSecurity.class.getResource(classFile);
    try {
      ClassNode loadedNode = Conversion.toNode(IOUtils.toByteArray(url));
      MethodNode setAccessible0 = loadedNode.methods.stream().filter(m -> m.name.equals("setAccessible0")).findFirst().get();
      setAccessible0.instructions = rewriteSetAccessible0("(me\\.nov|java\\.lang|sun\\.).*");
      setAccessible0.maxStack = 2;
      setAccessible0.maxLocals = 2;
      setAccessible0.tryCatchBlocks = null;
      setAccessible0.localVariables = null;
      return loadedNode;
    } catch (Exception e) {
      Threadtear.logger.error("Couldn't rewrite AccessibleObject, {}", e.toString());
      return null;
    }
  }
}
