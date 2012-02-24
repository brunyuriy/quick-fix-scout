package edu.cs.washington.quickfix.speculation.converter;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.corext.fix.IProposableFix;
import org.eclipse.jdt.internal.corext.fix.UnimplementedCodeFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.UnusedCodeFix;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

@SuppressWarnings("restriction")
public class IProposableFixConverter extends EclipseObjectConverter
{
    private final CompilationUnitRewriteOperationConverter rewriteOperationConverter_;

    public IProposableFixConverter(IProject original)
    {
        super(original, "fix");
        rewriteOperationConverter_ = new CompilationUnitRewriteOperationConverter(original);
    }

    public IProposableFix convert(IProposableFix shadowFix, FixCorrectionProposal shadowProposal,
            IProblemLocation shadowLocation)
    {
        IProposableFix result = shadowFix;
        if (shadowFix instanceof UnimplementedCodeFix)
            // Also an instance of CompilationUnitRewriteOperationsFix
            result = convert((UnimplementedCodeFix) shadowFix, shadowProposal, shadowLocation);
        else if (shadowFix instanceof UnusedCodeFix)
            // Also an instance of CompilationUnitRewriteOperationsFix
            result = convert((UnusedCodeFix) shadowFix, shadowProposal, shadowLocation);
        else
            logUnknownObjectType(shadowFix);
        return result;
    }

    @SuppressWarnings("rawtypes")
    private UnusedCodeFix convert(UnusedCodeFix shadowFix, FixCorrectionProposal shadowProposal,
            IProblemLocation shadowLocation)
    {
        Exception thrown = null;
        try
        {
            Object operations = getFieldValue("fOperations", shadowFix, shadowFix.getClass().getSuperclass());
            Object cleanUpOptions = getFieldValue("fCleanUpOptions", shadowFix);
            boolean operationsCorrect = operations == null || operations instanceof CompilationUnitRewriteOperation [];
            boolean cleanUpOptionsCorrect = cleanUpOptions == null || cleanUpOptions instanceof Map;
            if (operationsCorrect && cleanUpOptionsCorrect)
            {
                ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
                CompilationUnit originalASTCompilationUnit = createCompilationUnitFrom(originalCompilationUnit);
                CompilationUnitRewriteOperation [] rewriteOperations = (CompilationUnitRewriteOperation []) operations;
                CompilationUnitRewriteOperation [] originalRewriteOperations = new CompilationUnitRewriteOperation [rewriteOperations.length];
                for (int a = 0; a < rewriteOperations.length; a++)
                    originalRewriteOperations[a] = rewriteOperationConverter_.convert(rewriteOperations[a],
                            originalASTCompilationUnit, shadowLocation);
                UnusedCodeFix originalFix;
                if (cleanUpOptions == null)
                    originalFix = constructObject(UnusedCodeFix.class, new Class <?> [] {String.class,
                            CompilationUnit.class, new CompilationUnitRewriteOperation [0].getClass()}, new Object [] {
                            shadowFix.getDisplayString(), originalASTCompilationUnit, originalRewriteOperations});
                else
                    originalFix = constructObject(UnusedCodeFix.class, new Class <?> [] {String.class,
                            CompilationUnit.class, new CompilationUnitRewriteOperation [0].getClass(), Map.class},
                            new Object [] {shadowFix.getDisplayString(), originalASTCompilationUnit,
                                    originalRewriteOperations, (Map) cleanUpOptions});
                return originalFix;
            }
            else
            {
                logEclipseAPIChange(shadowFix);
                checkFieldName(operationsCorrect, "Operations", operations);
                checkFieldName(cleanUpOptionsCorrect, "Clean up options", cleanUpOptions);
            }
        }
        catch (NoSuchFieldException e)
        {
            thrown = e;
        }
        catch (IllegalAccessException e)
        {
            thrown = e;
        }
        catch (SecurityException e)
        {
            thrown = e;
        }
        catch (IllegalArgumentException e)
        {
            thrown = e;
        }
        catch (NoSuchMethodException e)
        {
            thrown = e;
        }
        catch (InstantiationException e)
        {
            thrown = e;
        }
        catch (InvocationTargetException e)
        {
            thrown = e;
        }
        logWrongUsageOfReflection(shadowFix, thrown);
        return shadowFix;
    }

    private UnimplementedCodeFix convert(UnimplementedCodeFix shadowFix, FixCorrectionProposal shadowProposal,
            IProblemLocation shadowLocation)
    {
        Exception thrown = null;
        try
        {
            Object operations = getFieldValue("fOperations", shadowFix, shadowFix.getClass().getSuperclass());
            boolean operationsCorrect = operations == null || operations instanceof CompilationUnitRewriteOperation [];
            if (operationsCorrect)
            {
                ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
                CompilationUnit originalASTCompilationUnit = createCompilationUnitFrom(originalCompilationUnit);
                CompilationUnitRewriteOperation [] rewriteOperations = (CompilationUnitRewriteOperation []) operations;
                CompilationUnitRewriteOperation [] originalRewriteOperations = new CompilationUnitRewriteOperation [rewriteOperations.length];
                for (int a = 0; a < rewriteOperations.length; a++)
                    originalRewriteOperations[a] = rewriteOperationConverter_.convert(rewriteOperations[a],
                            originalASTCompilationUnit, shadowLocation);
                UnimplementedCodeFix originalFix = new UnimplementedCodeFix(shadowFix.getDisplayString(),
                        originalASTCompilationUnit, originalRewriteOperations);
                return originalFix;
            }
            else
            {
                logEclipseAPIChange(shadowFix);
                checkFieldName(operationsCorrect, "Operations", operations);
            }
        }
        catch (NoSuchFieldException e)
        {
            thrown = e;
        }
        catch (IllegalAccessException e)
        {
            thrown = e;
        }
        logWrongUsageOfReflection(shadowFix, thrown);
        return shadowFix;
    }
}
