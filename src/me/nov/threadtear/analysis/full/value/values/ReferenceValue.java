package me.nov.threadtear.analysis.full.value.values;

//package me.nov.threadtear.analysis.full.holder.holders;
//
//import org.objectweb.asm.tree.InsnList;
//import org.objectweb.asm.tree.analysis.BasicValue;
//
//import me.nov.threadtear.analysis.full.holder.RealValueHolder;
//
//public class ReferenceValue extends RealValueHolder {
//
//	public int op;
//	public String owner;
//	public String name;
//	public String desc;
//
//	public ReferenceValue(BasicValue type, int op, String owner, String name, String desc) {
//		super(type);
//		this.op = op;
//		this.owner = owner;
//		this.name = name;
//		this.desc = desc;
//	}
//
//	@Override
//	public boolean isKnownValue() {
//		return false;
//	}
//
//	@Override
//	public RealValueHolder combine() {
//		return this;
//	}
//
//	@Override
//	public boolean equalsWith(RealValueHolder obj) {
//		if (this == obj)
//			return true;
//		ReferenceValue other = (ReferenceValue) obj;
//		if (desc == null) {
//			if (other.desc != null)
//				return false;
//		} else if (!desc.equals(other.desc))
//			return false;
//		if (name == null) {
//			if (other.name != null)
//				return false;
//		} else if (!name.equals(other.name))
//			return false;
//		if (op != other.op)
//			return false;
//		if (owner == null) {
//			if (other.owner != null)
//				return false;
//		} else if (!owner.equals(other.owner))
//			return false;
//		return true;
//	}
//
//	@Override
//	public InsnList toInstructions() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Object getStackValueOrNull() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//}
