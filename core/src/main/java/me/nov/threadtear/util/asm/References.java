package me.nov.threadtear.util.asm;

import me.nov.threadtear.execution.analysis.ReobfuscateMembers.MappedMember;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class References {
  private References() {
  }

  /**
   * Remap class references, if ain has one
   *
   * @param map Map<old class name, new class name>
   * @param ain instruction
   * @return 1 if updated, 0 if not
   */
  public static int remapClassRefs(Map<String, String> map, AbstractInsnNode ain) {
    if (ain instanceof MethodInsnNode) {
      MethodInsnNode min = (MethodInsnNode) ain;
      min.owner = map.getOrDefault(min.owner, min.owner);
      min.desc = Descriptor.fixMethodDesc(min.desc, map);
    } else if (ain instanceof FieldInsnNode) {
      FieldInsnNode fin = (FieldInsnNode) ain;
      fin.owner = map.getOrDefault(fin.owner, fin.owner);
      fin.desc = Descriptor.fixTypeDesc(fin.desc, map);
    } else if (ain instanceof TypeInsnNode) {
      TypeInsnNode tin = (TypeInsnNode) ain;
      String desc = tin.desc;
      if (desc.startsWith("[")) {
        String substring = desc.substring(1);
        tin.desc = "[" + map.getOrDefault(substring, substring);
      } else {
        tin.desc = map.getOrDefault(desc, desc);
      }
    } else if (ain instanceof InvokeDynamicInsnNode) {
      InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) ain;
      idin.desc = Descriptor.fixMethodDesc(idin.desc, map);
      for (int i = 0; i < idin.bsmArgs.length; i++) {
        Object o = idin.bsmArgs[i];
        if (o instanceof Handle) {
          Handle handle = (Handle) o;
          idin.bsmArgs[i] = new Handle(handle.getTag(), map.getOrDefault(handle.getOwner(), handle.getOwner()),
            handle.getName(), Descriptor.fixMethodDesc(handle.getDesc(), map), handle.isInterface());
        } else if (o instanceof Type) {
          Type type = (Type) o;
          idin.bsmArgs[i] = Descriptor.fixType(type, map);
        }
      }
      if (idin.bsm != null) {
        idin.bsm = new Handle(idin.bsm.getTag(), map.getOrDefault(idin.bsm.getOwner(), idin.bsm.getOwner()),
          idin.bsm.getName(), Descriptor.fixMethodDesc(idin.bsm.getDesc(), map), idin.bsm.isInterface());
      }
    } else if (ain instanceof LdcInsnNode) {
      LdcInsnNode lin = (LdcInsnNode) ain;
      if (lin.cst instanceof Type) {
        lin.cst = Descriptor.fixType((Type) lin.cst, map);
      } else if (lin.cst instanceof Handle) {
        Handle handle = (Handle) lin.cst;
        lin.cst = new Handle(handle.getTag(), map.getOrDefault(handle.getOwner(), handle.getOwner()), handle.getName(),
          Descriptor.fixMethodDesc(handle.getDesc(), map), handle.isInterface());
      }
    } else if (ain instanceof FrameNode) {
      FrameNode fn = (FrameNode) ain;
      Function<String, String> namingFunction = s -> {
        //TODO: optimize by using Type class
        String newName;
        if (s.startsWith("[")) {
          String substring;
          if (s.endsWith(";")) {
            substring = s.substring(s.indexOf("L") + 1, s.indexOf(";"));
            newName = "[L" + map.getOrDefault(substring, substring) + ";";
          } else {
            substring = s.substring(1);
            newName = "[" + map.getOrDefault(substring, substring);
          }
        } else {
          newName = map.getOrDefault(s, s);
        }
        return newName;
      };
      for (int i = 0; i < fn.stack.size(); i++) {
        Object o = fn.stack.get(i);
        if (o instanceof String) {
          String element = namingFunction.apply((String) o);
          fn.stack.set(i, element);
        }
      }
      for (int i = 0; i < fn.local.size(); i++) {
        Object o = fn.local.get(i);
        if (o instanceof String) {
          String element = namingFunction.apply((String) o);
          fn.local.set(i, element);
        }
      }
    } else {
      return 0;
    }
    return 1;
  }

  public static int remapMethodRefs(Map<String, ? extends List<MappedMember>> methods, AbstractInsnNode ain) {
    if (ain instanceof MethodInsnNode) {
      MethodInsnNode min = (MethodInsnNode) ain;
      if (!methods.containsKey(min.owner))
        return 0;
      MappedMember newMapping = methods.get(min.owner).stream()
        .filter(mapped -> mapped.oldName.equals(min.name) && mapped.oldDesc.equals(min.desc)).findFirst()
        .orElse(null);
      if (newMapping == null) {
        // this shouldn't happen, only if the code is
        // referencing a library
        return 0;
      }
      min.name = newMapping.newName;
    } else if (ain instanceof InvokeDynamicInsnNode) {
      InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) ain;
      for (int i = 0; i < idin.bsmArgs.length; i++) {
        Object o = idin.bsmArgs[i];
        if (o instanceof Handle) {
          Handle handle = (Handle) o;
          String owner = handle.getOwner();
          String name = handle.getName();
          String desc = handle.getDesc();
          idin.bsmArgs[i] = new Handle(handle.getTag(), owner, methods.containsKey(owner) ? methods.get(owner).stream()
            .filter(mapped -> mapped.oldName.equals(name) && mapped.oldDesc.equals(desc)).findFirst()
            .get().newName : name, desc, handle.isInterface());
        }
      }
      if (idin.bsm != null) {
        String owner = idin.bsm.getOwner();
        String name = idin.bsm.getName();
        String desc = idin.bsm.getDesc();
        idin.bsm = new Handle(idin.bsm.getTag(), owner, methods.containsKey(owner) ?
          methods.get(owner).stream().filter(mapped -> mapped.oldName.equals(name) && mapped.oldDesc.equals(desc))
            .findFirst().get().newName : name, desc, idin.bsm.isInterface());
      }
    } else {
      return 0;
    }
    return 1;
  }

  public static int remapFieldRefs(Map<String, ? extends List<MappedMember>> fields, AbstractInsnNode ain) {
    if (ain instanceof FieldInsnNode) {
      FieldInsnNode fin = (FieldInsnNode) ain;
      if (!fields.containsKey(fin.owner))
        return 0;
      MappedMember newMapping = fields.get(fin.owner).stream()
        .filter(mapped -> mapped.oldName.equals(fin.name) && mapped.oldDesc.equals(fin.desc)).findFirst()
        .orElse(null);
      if (newMapping == null) {
        // this shouldn't happen, only if the code is
        // referencing a library
        return 0;
      }
      fin.name = newMapping.newName;
      return 1;
    }
    return 0;
  }

  public static void remapMethodType(Map<String, String> map, MethodNode mn) {
    mn.desc = Descriptor.fixMethodDesc(mn.desc, map);
    for (int i = 0; i < mn.exceptions.size(); i++) {
      String ex = mn.exceptions.get(i);
      mn.exceptions.set(i, map.getOrDefault(ex, ex));
    }
    mn.tryCatchBlocks.forEach(tcb -> tcb.type = map.getOrDefault(tcb.type, tcb.type));
    if (mn.localVariables != null)
      mn.localVariables.forEach(lv -> lv.desc = Descriptor.fixTypeDesc(lv.desc, map));
    if (mn.invisibleAnnotations != null)
      mn.invisibleAnnotations.forEach(a -> a.desc = Descriptor.fixTypeDesc(a.desc, map));
    if (mn.visibleAnnotations != null)
      mn.visibleAnnotations.forEach(a -> a.desc = Descriptor.fixTypeDesc(a.desc, map));
    if (mn.invisibleTypeAnnotations != null)
      mn.invisibleTypeAnnotations.forEach(a -> a.desc = Descriptor.fixTypeDesc(a.desc, map));
    if (mn.visibleTypeAnnotations != null)
      mn.visibleTypeAnnotations.forEach(a -> a.desc = Descriptor.fixTypeDesc(a.desc, map));
    mn.signature = null;
    // cn.attrs.forEach(at -> at.type = Descriptor
    // .fixDesc(at.type, map));
    // TODO remap signature, is actually not necessary.
    //  other annotations, etc..
  }

  public static void remapClassType(Map<String, String> map, ClassNode cn) {
    for (int i = 0; i < cn.interfaces.size(); i++) {
      String itf = cn.interfaces.get(i);
      cn.interfaces.set(i, map.getOrDefault(itf, itf));
    }
    cn.superName = map.getOrDefault(cn.superName, cn.superName);
    if (cn.invisibleAnnotations != null)
      cn.invisibleAnnotations.forEach(a -> a.desc = Descriptor.fixTypeDesc(a.desc, map));
    if (cn.visibleAnnotations != null)
      cn.visibleAnnotations.forEach(a -> a.desc = Descriptor.fixTypeDesc(a.desc, map));
    if (cn.invisibleTypeAnnotations != null)
      cn.invisibleTypeAnnotations.forEach(a -> a.desc = Descriptor.fixTypeDesc(a.desc, map));
    if (cn.visibleTypeAnnotations != null)
      cn.visibleTypeAnnotations.forEach(a -> a.desc = Descriptor.fixTypeDesc(a.desc, map));
    cn.signature = null;
    if (cn.innerClasses != null)
      cn.innerClasses.forEach(i -> {
        i.innerName = map.getOrDefault(i.innerName, i.innerName);
        i.outerName = map.getOrDefault(i.outerName, i.outerName);
      });
    // TODO more cases
  }

  public static void remapFieldType(Map<String, String> map, FieldNode fn) {
    fn.desc = Descriptor.fixTypeDesc(fn.desc, map);
    if (fn.invisibleAnnotations != null)
      fn.invisibleAnnotations.forEach(a -> a.desc = Descriptor.fixTypeDesc(a.desc, map));
    if (fn.visibleAnnotations != null)
      fn.visibleAnnotations.forEach(a -> a.desc = Descriptor.fixTypeDesc(a.desc, map));
    if (fn.invisibleTypeAnnotations != null)
      fn.invisibleTypeAnnotations.forEach(a -> a.desc = Descriptor.fixTypeDesc(a.desc, map));
    if (fn.visibleTypeAnnotations != null)
      fn.visibleTypeAnnotations.forEach(a -> a.desc = Descriptor.fixTypeDesc(a.desc, map));
    fn.signature = null;
  }
}
