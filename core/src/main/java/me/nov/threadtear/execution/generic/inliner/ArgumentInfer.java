package me.nov.threadtear.execution.generic.inliner;

import me.nov.threadtear.analysis.stack.ConstantTracker;
import me.nov.threadtear.analysis.stack.ConstantValue;
import me.nov.threadtear.analysis.stack.IConstantReferenceHandler;
import me.nov.threadtear.logging.LogWrapper;
import me.nov.threadtear.util.asm.Access;
import me.nov.threadtear.util.asm.Mapping;
import me.nov.threadtear.util.asm.method.MethodContext;
import me.nov.threadtear.util.asm.method.MethodSignature;
import me.nov.threadtear.vm.Sandbox;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Infers method arguments from caller methods, allowing
 * you to inline those that are constant.
 *
 * @author ViRb3
 */
public class ArgumentInfer implements IConstantReferenceHandler, Opcodes {
  private final Map<MethodNode, List<MethodContext>> references;
  private final Map<MethodNode, List<ConstantValue>> cache;
  private HashSet<MethodNode> trace;

  public ArgumentInfer(List<ClassNode> classes) {
    this.references = Mapping.getMethodReferences(classes);
    this.cache = new HashMap<>();
  }

  /**
   * Infers arguments for {@code methodCtx} and then
   * inlines those that are constant.
   * <p>
   * This will modify {@code methodCtx}.
   *
   * @return Was anything changed.
   */
  public boolean inline(MethodContext methodCtx) {
    trace = new HashSet<>();
    final List<ConstantValue> args = infer(methodCtx);
    return injectArgs(methodCtx.getMethod(), args);
  }

  /**
   * Infers arguments for {@code methodCtx}.
   *
   * @return Inferred arguments.
   */
  public List<ConstantValue> analyze(MethodContext methodCtx) {
    trace = new HashSet<>();
    return infer(methodCtx);
  }

  private List<ConstantValue> infer(MethodContext methodCtx) {
    final MethodNode method = methodCtx.getMethod();

    final List<ConstantValue> cachedResult = cache.get(method);
    if (cachedResult != null) {
      return cachedResult;
    }

    // prevent infinite recursion into callers
    if (trace.contains(method)) {
      return new ArrayList<>();
    }
    trace.add(method);

    final List<MethodContext> callerCtxs = references.get(method);
    if (callerCtxs == null) {
      return new ArrayList<>();
    }
    Map<MethodNode, List<ConstantValue>> result = new HashMap<>();
    for (MethodContext callerCtx : callerCtxs) {
      result.put(callerCtx.getMethod(), infer(methodCtx, callerCtx));
    }

    final List<ConstantValue> args = getCommonArgs(result);
    cache.put(method, args);
    return args;
  }

  private List<ConstantValue> infer(MethodContext methodCtx, MethodContext callerCtx) {
    final List<ConstantValue> cachedResult = cache.get(methodCtx.getMethod());
    if (cachedResult != null) {
      return cachedResult;
    }

    final List<ConstantValue> inferredArgs = infer(callerCtx);

    final MethodNode newCaller = Sandbox.copyMethod(callerCtx.getMethod());
    injectArgs(newCaller, inferredArgs);
    callerCtx = new MethodContext(callerCtx.getOwner(), newCaller);
    final Frame<ConstantValue>[] frames = calculateFrames(callerCtx);

    final List<List<ConstantValue>> args = getMatchingArgs(methodCtx, callerCtx, frames);
    return getCommonArgs(args);
  }

  private List<ConstantValue> getCommonArgs(Map<MethodNode, List<ConstantValue>> inferredArgs) {
    return getCommonArgs(inferredArgs.values());
  }

  /**
   * Combines arguments from multiple callers so that
   * only the constant arguments found in all collections
   * remain.
   * <p>
   * This is a naive implementation - it will skip
   * Analyzer failures.
   * It will only stop if it is guaranteed that more than
   * two values exist for the same argument.
   */
  private List<ConstantValue> getCommonArgs(Collection<List<ConstantValue>> inferredArgs) {
    final List<ConstantValue> result =
            inferredArgs.stream().max(Comparator.comparing(List::size)).orElse(new ArrayList<>());

    for (int i = 0; i < result.size(); i++) {
      final int I = i;
      final Set<ConstantValue> args =
              inferredArgs.stream().filter(a -> a.size() > I).map(a -> a.get(I)).collect(Collectors.toSet());
      if (args.size() == 1) {
        result.set(i, args.iterator().next());
      } else {
        ConstantValue unknown = result.get(i);
        unknown.setValue(null);
        result.set(i, unknown);
      }
    }
    return result;
  }

  /**
   * Scans {@code callerCtx} for calls to {@code
   * methodCtx} and builds a list of all matching arguments.
   */
  private List<List<ConstantValue>> getMatchingArgs(MethodContext methodCtx, MethodContext callerCtx,
                                                    Frame<ConstantValue>[] frames) {
    final Type[] methodArgs = Type.getArgumentTypes(methodCtx.getMethod().desc);
    final List<List<ConstantValue>> results = new ArrayList<>();

    for (int i = 0; i < frames.length; i++) {
      AbstractInsnNode instr = callerCtx.getMethod().instructions.get(i);
      if (instr instanceof MethodInsnNode) {
        final MethodInsnNode methodInsnNode = (MethodInsnNode) instr;
        final Frame<ConstantValue> frame = frames[i];
        if (frame == null) {
          break;
        }
        if (methodCtx.getSignature().equals(new MethodSignature(methodInsnNode))) {
          results.add(IntStream.range(0, methodArgs.length)
                  .mapToObj(u -> frame.getStack(frame.getStackSize() - methodArgs.length + u))
                  .collect(Collectors.toList()));
        }
      }
    }
    return results;
  }

  /**
   * Injects arguments by prepending LDC+STORE
   * instructions to {@code method}.
   * This change is used by analyzers and decompilers.
   * <p>
   * This will modify {@code method}, create a copy with
   * {@link Sandbox#copyMethod(MethodNode)} if you wish
   * to preserve it.
   *
   * @return Was anything changed.
   */
  private static boolean injectArgs(MethodNode method, List<ConstantValue> args) {
    int oldLen = method.instructions.size();
    int varIndex = Access.isStatic(method.access) ? 0 : 1;
    for (ConstantValue arg : args) {
      if (arg.isKnown()) {
        if (arg.isInteger()) {
          method.instructions.insert(new VarInsnNode(ISTORE, varIndex));
          method.instructions.insert(new LdcInsnNode(arg.getAsInteger())); // make sure shorts
          // or bytes are also ints
        } else {
          if (arg.isNull()) {
            method.instructions.insert(new VarInsnNode(ASTORE, varIndex));
            method.instructions.insert(new InsnNode(ACONST_NULL));
          } else {
            Type type = arg.getType().getType();
            if (type != null && !Access.isArray(type.getDescriptor())) {
              method.instructions.insert(new VarInsnNode(type.getOpcode(ISTORE), varIndex));
              method.instructions.insert(new LdcInsnNode(arg.getValue()));
            }
          }
        }
      }
      varIndex += arg.getType().getSize();
    }
    return method.instructions.size() > oldLen;
  }

  private Frame<ConstantValue>[] calculateFrames(MethodContext methodCtx) {
    final ClassNode owner = methodCtx.getOwner();
    final MethodNode method = methodCtx.getMethod();

    Analyzer<ConstantValue> analyzer = new Analyzer<>(
            new ConstantTracker(this, Access.isStatic(method.access), method.maxLocals, method.desc, new Object[0]));
    try {
      analyzer.analyze(owner.name, method);
    } catch (AnalyzerException e) {
      LogWrapper.logger.error("Failed analysis in {}#{}{}", e, owner.name, method.name, method.desc);
      // for debugging purposes:
      // BytecodeDebugger.show(method, e);
    }
    return analyzer.getFrames();
  }

  @Override
  public Object getFieldValueOrNull(BasicValue v, String owner, String name, String desc) {
    return null;
  }

  @Override
  public Object getMethodReturnOrNull(BasicValue v, String owner, String name, String desc,
                                      List<? extends ConstantValue> values) {
    return null;
  }
}
