package me.nov.threadtear.execution;

import java.util.*;
import java.util.function.BiConsumer;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import me.nov.threadtear.analysis.stack.*;
import me.nov.threadtear.logging.LogWrapper;
import me.nov.threadtear.security.VMSecurityManager;
import me.nov.threadtear.util.asm.Access;

public abstract class Execution implements Opcodes {
  public final String name;
  public final ExecutionCategory type;
  public final String description;
  public final ExecutionTag[] tags;

  protected static final LogWrapper logger = LogWrapper.logger;
  protected static final Random random = new Random();

  public Execution(ExecutionCategory type, String name, String description, ExecutionTag... tags) {
    this.type = type;
    this.name = name;
    this.description = description;
    this.tags = tags;
  }

  /**
   * Add your name here, if you are planning to make a
   * pull request
   *
   * @return null by default
   */
  public String getAuthor() {
    return null;
  }

  @Override
  public String toString() {
    return name;
  }

  /**
   * Run the execution
   *
   * @param map     a map of all loaded classes, while
   *                the key is the internal name
   *                (java/foo/bar)
   * @param verbose true when the user wants more logging
   *                output
   * @return success (true) or failure (false)
   */
  public abstract boolean execute(Map<String, Clazz> map, boolean verbose);

  /**
   * Get a method from a class node
   */
  protected MethodNode getMethod(ClassNode node, String name, String desc) {
    if (node == null)
      return null;
    return node.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(desc)).findFirst().orElse(null);
  }

  /**
   * Get the static initializer from a class node
   */
  protected MethodNode getStaticInitializer(ClassNode node) {
    return node.methods.stream().filter(m -> m.name.equals("<clinit>")).findFirst().orElse(null);
  }

  /**
   * Run the constant analyzer and get the frames The
   * frames will have more information than normal ASM
   * ones and pre-compute easy calculations.
   *
   * @param handler a reference handler to integrate more
   *                methods or fields in the computation
   * @return array of Frames, while the array index
   * corresponds to the instruction index
   */
  protected Frame<ConstantValue>[] getConstantFrames(ClassNode c, MethodNode m, IConstantReferenceHandler handler) {
    Analyzer<ConstantValue> a =
            new Analyzer<>(new ConstantTracker(handler, Access.isStatic(m.access), m.maxLocals, m.desc, new Object[0]));
    try {
      a.analyze(c.name, m);
    } catch (Throwable e) {
      logger.error("Failed stack analysis in {}, {}", e, referenceString(c, m), shortStacktrace(e));
      // for debugging
      // BytecodeDebugger.show(c, (Exception) e);
      return null;
    }
    return a.getFrames();
  }

  /**
   * see
   * {@link #getConstantFrames(ClassNode c, MethodNode m, IConstantReferenceHandler handler) getConstantFrames}
   */
  protected void loopConstantFrames(ClassNode c, MethodNode m, IConstantReferenceHandler handler,
                                    BiConsumer<AbstractInsnNode, Frame<ConstantValue>> consumer) {
    Frame<ConstantValue>[] frames = getConstantFrames(c, m, handler);
    if (frames == null)
      return;
    AbstractInsnNode[] insns = m.instructions.toArray();
    for (int i = 0; i < insns.length; i++) {
      consumer.accept(insns[i], frames[i]);
    }
  }

  /**
   * @return all labels with a corresponding
   * (ConstantValue) frame, see
   * {@link #getConstantFrames(ClassNode c, MethodNode m, IConstantReferenceHandler handler) getConstantFrames}
   */
  protected HashMap<LabelNode, Frame<ConstantValue>> collectLabels(ClassNode c, MethodNode m,
                                                                   IConstantReferenceHandler handler) {
    HashMap<LabelNode, Frame<ConstantValue>> map = new HashMap<>();
    loopConstantFrames(c, m, handler, (ain, frame) -> {
      if (ain.getType() == AbstractInsnNode.LABEL) {
        map.put((LabelNode) ain, frame);
      }
    });
    return map;
  }

  /**
   * For logging
   */
  protected String shortStacktrace(Throwable e) {
    StringBuilder sb = new StringBuilder();
    sb.append(e.getClass().getName());
    if (e.getMessage() != null) {
      sb.append(": ");
      sb.append(e.getMessage().replace("{}", "{ }"));
    }
    if (e.getCause() != null) {
      sb.append(", Cause: [");
      sb.append(shortStacktrace(e.getCause()));
      sb.append("]");
    }
    return sb.toString();
  }

  /**
   * For logging
   */
  protected String referenceString(ClassNode cn, MethodNode m) {
    StringBuilder sb = new StringBuilder();
    if (cn != null)
      sb.append(cn.name.replace('/', '.'));
    if (m != null) {
      sb.append(" ");
      sb.append(m.name);
      sb.append(m.desc);
    }
    return sb.toString();
  }

  protected void allowReflection(boolean allow) {
    SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      ((VMSecurityManager) sm).allowReflection(allow);
    }
  }
}
