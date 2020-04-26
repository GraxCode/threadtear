package me.nov.threadtear.analysis.rewriter;

import java.util.List;

import org.objectweb.asm.tree.analysis.BasicValue;

import me.nov.threadtear.analysis.rewriter.value.CodeReferenceValue;

public interface ICRReferenceHandler {

	Object getFieldValueOrNull(BasicValue v, String owner, String name, String desc);

	Object getMethodReturnOrNull(BasicValue v, String owner, String name, String desc, List<? extends CodeReferenceValue> values);

}
