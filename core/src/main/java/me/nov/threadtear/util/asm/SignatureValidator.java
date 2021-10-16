package me.nov.threadtear.util.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

public final class SignatureValidator {

  private SignatureValidator() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated.");
  }

  public static void validateSignatures(ClassNode classNode) {
    parseOr(classNode.signature, () -> classNode.signature = null);

    for(MethodNode methodNode : classNode.methods) {
      parseOr(methodNode.signature, () -> methodNode.signature = null);

      for(LocalVariableNode localVariableNode : methodNode.localVariables) {
        parseOr(localVariableNode.signature, () -> localVariableNode.signature = null);
      }
    }

    for(FieldNode fieldNode : classNode.fields) {
      parseOr(fieldNode.signature, () -> fieldNode.signature = null);
    }
  }

  private static void parseOr(String signature, Runnable runnable) {
    SignatureReader signatureReader = new SignatureReader(signature);
    try {
      signatureReader.accept(new SignatureVisitor(Opcodes.ASM9) {});
    } catch (Exception ignored) {
      runnable.run();
    }
  }
}
