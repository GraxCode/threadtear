package me.nov.threadtear.analysis.stack;

import java.util.List;

import org.objectweb.asm.tree.analysis.BasicValue;

/**
 * Use values of referenced fields or methods during stack calculation
 */
public interface IConstantReferenceHandler {

  /**
   * Return a value that is used while calculating stack.
   *
   * @param v     Value with the type
   * @param owner Owner of the field
   * @param name  Name of the field
   * @param desc  Desc of the field
   * @return preferred value or null if unknown
   */
  Object getFieldValueOrNull(BasicValue v, String owner, String name, String desc);

  /**
   * Return a value that is used while calculating stack. This is useful if e.g. string length functions are used.
   *
   * @param v      Value with the type
   * @param owner  Owner of the field
   * @param name   Name of the field
   * @param desc   Desc of the field
   * @param values Arguments that are passed, including the object reference as first entry
   * @return preferred value or null if unknown
   */
  Object getMethodReturnOrNull(BasicValue v, String owner, String name, String desc,
                               List<? extends ConstantValue> values);

}
