package me.nov.threadtear.execution.zkm;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;
import me.nov.threadtear.util.asm.Access;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DESObfuscationZKM extends Execution {
  private boolean verbose;
  private int decrypted;

  public DESObfuscationZKM() {
    super(ExecutionCategory.ZKM, "ZKM DES case deobfuscator",
      "Deobfuscates string / access obfuscation with DES cipher." +
        "<br>Tested on ZKM 14, could work on newer versions too.", ExecutionTag.POSSIBLE_DAMAGE,
      ExecutionTag.POSSIBLY_MALICIOUS);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    this.verbose = verbose;
    this.decrypted = 0;
    final List<ClassNode> classNodes = classes.values().stream().map(c -> c.node).collect(Collectors.toList());
    classes.values().stream().filter(this::hasDESEncryption).forEach(this::decrypt);
    logger.info("Decrypted {} strings successfully.", decrypted);
    return decrypted > 0;
  }

  private void decrypt(Clazz clazz) {
    // TODO
    // decrypt DES key first, then decrypt strings and references.
  }

  private boolean hasDESEncryption(Clazz c) {
    ClassNode cn = c.node;
    if (Access.isInterface(cn.access))
      return false;
    MethodNode mn = getStaticInitializer(cn);
    if (mn == null)
      return false;
    return StreamSupport.stream(mn.instructions.spliterator(), false).anyMatch(
      ain -> ain.getType() == AbstractInsnNode.LDC_INSN &&
        "DES/CBC/PKCS5Padding".equals(((LdcInsnNode) ain).cst));
  }
}
