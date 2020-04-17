package me.nov.threadtear.analysis.full;

import java.util.List;

import org.objectweb.asm.tree.analysis.BasicValue;

import me.nov.threadtear.analysis.full.value.CodeReferenceValue;

public interface ICodeReferenceHandler {

	Object getFieldValueOrNull(BasicValue v, String owner, String name, String desc);

	Object getMethodReturnOrNull(BasicValue v, String owner, String name, String desc, List<? extends CodeReferenceValue> values);

}
