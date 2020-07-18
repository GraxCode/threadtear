package me.nov.threadtear.execution.analysis;

import java.net.URL;
import java.util.*;

import me.nov.threadtear.logging.LogWrapper;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.tree.*;

import me.nov.threadtear.execution.*;
import me.nov.threadtear.io.Conversion;
import me.nov.threadtear.util.asm.*;
import me.nov.threadtear.util.format.Strings;

public class ReobfuscateMembers extends Execution {
  private Map<String, Clazz> classes;
  private Queue<String> words;
  private boolean verbose;

  public ReobfuscateMembers() {
    super(ExecutionCategory.ANALYSIS, "Reobfuscate methods and fields",
      "Reobfuscate method and field names for easier analysis.<br>Gets " +
        "rid of annoying method names like 恼人的名字.", ExecutionTag.BETTER_DECOMPILE,
      ExecutionTag.POSSIBLE_DAMAGE);
  }

  @Override
  public boolean execute(Map<String, Clazz> classes, boolean verbose) {
    this.classes = classes;
    this.verbose = verbose;

    logger.info("Generating random names");
    this.words = Strings.generateWordQueue(
      (int) (classes.values().stream().map(c -> c.node.fields).mapToLong(List::size).sum() +
        classes.values().stream().map(c -> c.node.methods).mapToLong(List::size).sum()),
      Objects.requireNonNull(ReobfuscateMembers.class.getResourceAsStream("english-words.txt")));

    logger.info("Making method mappings");
    classes.values().stream().map(c -> c.node).forEach(this::makeMethodMappings);
    logger.info(methods.size() + " method mappings created for classes and superclasses");

    logger.info("Renaming methods");
    classes.values().stream().map(c -> c.node).forEach(c -> c.methods.forEach(m -> m.name =
      methods.get(c.name).stream().filter(mapped -> mapped.equalsMethod(m)).findFirst().get().newName));

    logger.info("Updating method references in code");
    int mrefs =
      classes.values().stream().map(c -> c.node.methods).flatMap(List::stream).map(m -> m.instructions.toArray())
        .flatMap(Arrays::stream).mapToInt(ain -> References.remapMethodRefs(methods, ain)).sum();
    logger.info(mrefs + " method references updated successfully!");

    logger.info("Making field mappings");
    classes.values().stream().map(c -> c.node).forEach(this::makeFieldMappings);

    logger.info("Renaming fields");
    classes.values().stream().map(c -> c.node).forEach(c -> c.fields.forEach(f -> f.name =
      fields.get(c.name).stream().filter(mapped -> mapped.equalsField(f)).findFirst().get().newName));

    logger.info("Updating field references in code");
    int frefs =
      classes.values().stream().map(c -> c.node.methods).flatMap(List::stream).map(m -> m.instructions.toArray())
        .flatMap(Arrays::stream).mapToInt(ain -> References.remapFieldRefs(fields, ain)).sum();
    logger.info(frefs + " field references updated successfully!");
    return frefs > 0 && mrefs > 0;
  }

  private HashMap<String, ArrayList<MappedMember>> fields = new HashMap<>();
  private HashMap<String, ArrayList<MappedMember>> methods = new HashMap<>();

  private void makeFieldMappings(ClassNode c) {
    ArrayList<MappedMember> list = new ArrayList<>();
    c.fields.forEach(f -> list.add(new MappedMember(f.name, f.desc, words.poll() + "$"))); // add a dollar
    // sign to avoid collisions
    fields.put(c.name, list);
  }

  private void makeMethodMappings(ClassNode c) {
    if (methods.containsKey(c.name))
      return;
    boolean isLocal = classes.values().stream().anyMatch(clazz -> clazz.node.equals(c));
    ArrayList<ClassNode> parents = new ArrayList<>();
    // first remap parents
    if (c.superName != null) {
      ClassNode superClass = findClass(c.superName);
      if (superClass != null) {
        parents.add(superClass);
        makeMethodMappings(superClass);
      } else {
        isLocal = false;
      }
    }
    for (String itf : c.interfaces) {
      ClassNode interfaze = findClass(itf);
      if (interfaze != null) {
        parents.add(interfaze);
        makeMethodMappings(interfaze);
      } else {
        isLocal = false;
      }
    }
    ArrayList<MappedMember> list = new ArrayList<>();
    parents.forEach(p -> list.addAll(methods.get(p.name))); // add parent methods. there
    // are better solutions in terms of performance but
    // this is the simplest one
    final boolean local = isLocal;
    c.methods.forEach(
      m -> list.add(new MappedMember(m.name, m.desc, local && isChangeable(m) ? makeName(parents, m) : m.name)));
    methods.put(c.name, list);
  }

  private String makeName(ArrayList<ClassNode> parents, MethodNode m) {
    MappedMember overriddenMethod =
      parents.stream().map(c -> c.name).filter(methods::containsKey).map(methods::get).flatMap(List::stream)
        .filter(mapped -> mapped.equalsMethod(m)).findFirst().orElse(null);
    if (overriddenMethod != null) {
      // return parent name
      return overriddenMethod.newName;
    }
    return "_" + words.poll(); // add an underscore to avoid
    // collisions
  }

  private static final List<String> ignore = Arrays.asList("valueOf", "values", "ordinal", "toString", "hashCode");
  // save some time

  private boolean isChangeable(MethodNode m) {
    if (Access.isNative(m.access))
      return false;
    if (m.name.startsWith("<"))
      return false;
    if (m.name.equals("main") && m.desc.equals("([Ljava/lang/String;)V"))
      return false;
    if (ignore.contains(m.name))
      return false;
    return true;
  }

  private ClassNode findClass(String name) {
    Clazz node = classes.get(name);
    return node == null ? findInRT(name) : node.node;
  }

  private final HashMap<String, ClassNode> loadedRuntimeClasses = new HashMap<>();

  private ClassNode findInRT(String name) {
    if (loadedRuntimeClasses.containsKey(name)) {
      return loadedRuntimeClasses.get(name);
    }
    String classFile = "/" + name + ".class";
    URL url = ReobfuscateMembers.class.getResource(classFile);
    try {
      ClassNode loadedNode = Conversion.toNode(IOUtils.toByteArray(url));
      loadedRuntimeClasses.put(name, loadedNode);
      return loadedNode;
    } catch (Exception e) {
      if (verbose)
        LogWrapper.logger.warning("Couldn't find or resolve {}, {}", name, shortStacktrace(e));
      return null;
    }
  }

  public static class MappedMember {
    public String oldName;
    public String oldDesc;
    public String newName;

    public MappedMember(String oldName, String oldDesc, String newName) {
      super();
      this.oldName = oldName;
      this.oldDesc = oldDesc;
      this.newName = newName;
    }

    public boolean equalsMethod(MethodNode mn) {
      return oldName.equals(mn.name) && oldDesc.equals(mn.desc);
    }

    public boolean equalsField(FieldNode fn) {
      return oldName.equals(fn.name) && oldDesc.equals(fn.desc);
    }

    @Override
    public String toString() {
      return oldName + oldDesc + " -> " + newName;
    }
  }
}
