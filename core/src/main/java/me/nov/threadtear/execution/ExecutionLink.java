package me.nov.threadtear.execution;

import me.nov.threadtear.execution.allatori.ExpirationDateRemoverAllatori;
import me.nov.threadtear.execution.allatori.JunkRemoverAllatori;
import me.nov.threadtear.execution.allatori.StringObfuscationAllatori;
import me.nov.threadtear.execution.analysis.*;
import me.nov.threadtear.execution.cleanup.InlineMethods;
import me.nov.threadtear.execution.cleanup.InlineUnchangedFields;
import me.nov.threadtear.execution.cleanup.remove.RemoveAttributes;
import me.nov.threadtear.execution.cleanup.remove.RemoveUnnecessary;
import me.nov.threadtear.execution.cleanup.remove.RemoveUnusedVariables;
import me.nov.threadtear.execution.dasho.StringObfuscationDashO;
import me.nov.threadtear.execution.generic.ConvertCompareInstructions;
import me.nov.threadtear.execution.generic.KnownConditionalJumps;
import me.nov.threadtear.execution.generic.ObfuscatedAccess;
import me.nov.threadtear.execution.generic.TryCatchObfuscationRemover;
import me.nov.threadtear.execution.generic.inliner.ArgumentInliner;
import me.nov.threadtear.execution.generic.inliner.JSRInliner;
import me.nov.threadtear.execution.paramorphism.AccessObfuscationParamorphism;
import me.nov.threadtear.execution.paramorphism.BadAttributeRemover;
import me.nov.threadtear.execution.paramorphism.StringObfuscationParamorphism;
import me.nov.threadtear.execution.stringer.AccessObfuscationStringer;
import me.nov.threadtear.execution.stringer.StringObfuscationStringer;
import me.nov.threadtear.execution.tools.*;
import me.nov.threadtear.execution.zkm.*;

import java.util.ArrayList;
import java.util.List;

public class ExecutionLink {
  public static final List<Class<? extends Execution>> executions = new ArrayList<Class<? extends Execution>>() {{
    add(InlineMethods.class);
    add(InlineUnchangedFields.class);
    add(RemoveUnnecessary.class);
    add(RemoveUnusedVariables.class);
    add(RemoveAttributes.class);

    add(ArgumentInliner.class);
    add(JSRInliner.class);
    add(ObfuscatedAccess.class);
    add(KnownConditionalJumps.class);
    add(ConvertCompareInstructions.class);

    add(RestoreSourceFiles.class);
    add(ReobfuscateClassNames.class);
    add(ReobfuscateMembers.class);
    add(ReobfuscateVariableNames.class);
    add(RemoveMonitors.class);
    add(RemoveTCBs.class);

    add(StringObfuscationStringer.class);
    add(AccessObfuscationStringer.class);

    add(TryCatchObfuscationRemover.class);
    add(StringObfuscationZKM.class);
    add(AccessObfuscationZKM.class);
    add(FlowObfuscationZKM.class);
    add(DESObfuscationZKM.class);

    add(StringObfuscationAllatori.class);
    add(ExpirationDateRemoverAllatori.class);
    add(JunkRemoverAllatori.class);

    add(StringObfuscationDashO.class);

    add(BadAttributeRemover.class);
    add(StringObfuscationParamorphism.class);
    add(AccessObfuscationParamorphism.class);

    add(Java7Compatibility.class);
    add(Java8Compatibility.class);
    add(IsolatePossiblyMalicious.class);
    add(AddLineNumbers.class);
    add(LogAllExceptions.class);
    add(RemoveMaxs.class);
  }};
}
