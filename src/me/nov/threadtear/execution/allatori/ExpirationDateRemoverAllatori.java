package me.nov.threadtear.execution.allatori;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;

import me.nov.threadtear.analysis.stack.ConstantValue;
import me.nov.threadtear.analysis.stack.IConstantReferenceHandler;
import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.ExecutionTag;

public class ExpirationDateRemoverAllatori extends Execution implements IConstantReferenceHandler {

	public ExpirationDateRemoverAllatori() {
		super(ExecutionCategory.ALLATORI, "Remove expiry date", "Allatori adds expiration dates to the code<br>that stop the obfuscated jar file from running after being passed.<br>They can be removed easily.",
				ExecutionTag.POSSIBLE_DAMAGE);
	}

	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose) {
		logger.info("Finding most common long ldc cst");
		long mostCommon = classes.stream().map(c -> c.node.methods).flatMap(List::stream).map(m -> m.instructions.spliterator()).flatMap(insns -> StreamSupport.stream(insns, false))
				.filter(ain -> ain.getOpcode() == LDC && ((LdcInsnNode) ain).cst instanceof Long).map(ain -> (LdcInsnNode) ain)
				.filter(ldc -> Math.abs((long) ldc.cst - System.currentTimeMillis()) < 157784760000L).collect(Collectors.groupingBy(ldc -> (long) ldc.cst, Collectors.counting())).entrySet().stream()
				.max(Comparator.comparing(Entry::getValue)).map(Entry::getKey).orElseThrow(RuntimeException::new);
		logger.info("Expiration date is " + new Date(mostCommon).toString() + ", replacing");
		classes.stream().map(c -> c.node.methods).flatMap(List::stream).map(m -> m.instructions.spliterator()).flatMap(insns -> StreamSupport.stream(insns, false))
				.filter(ain -> ain.getOpcode() == LDC && ((LdcInsnNode) ain).cst.equals(mostCommon)).map(ain -> (LdcInsnNode) ain).forEach(ldc -> ldc.cst = 1337133713371337L);
		return true;
	}

	public static <T> Collector<T, ?, Long> counting() {
		return Collectors.reducing(0L, (var0) -> {
			return 1L;
		}, Long::sum);
	}

	@Override
	public Object getFieldValueOrNull(BasicValue v, String owner, String name, String desc) {
		return null;
	}

	@Override
	public Object getMethodReturnOrNull(BasicValue v, String owner, String name, String desc, List<? extends ConstantValue> values) {
		return null;
	}
}