package me.nov.threadtear.analysis.rewriter;

import java.util.List;

import org.objectweb.asm.tree.analysis.BasicValue;

import me.nov.threadtear.analysis.rewriter.value.CodeReferenceValue;

/**
 * Same as {@link me.nov.threadtear.analysis.stack.IConstantReferenceHandler} for CodeRewriter
 */
public interface ICRReferenceHandler {

  Object getFieldValueOrNull(BasicValue v, String owner, String name, String desc);

  Object getMethodReturnOrNull(BasicValue v, String owner, String name, String desc,
                               List<? extends CodeReferenceValue> values);

}
