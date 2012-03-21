package edu.cs.washington.quickfix.speculation.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.internal.corext.fix.IProposableFix;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.ReorgCorrectionsSubProcessor;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.AddArgumentCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.AddImportCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.AddTypeParameterProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CUCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CastCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CorrectMainTypeNameProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedNamesAssistProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.MissingReturnTypeCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ModifierChangeCorrectionProposal;
// I need to implement this, but I don't have an example yet.
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewVariableCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.TypeChangeCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposal.ChangeDescription;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CorrectPackageDeclarationProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewCUUsingWizardProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewMethodCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.RenameNodeCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ReplaceCorrectionProposal;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.ltk.core.refactoring.Change;

@SuppressWarnings({"restriction"})
public class IJavaCompletionProposalConverter extends EclipseObjectConverter
{
    private static final Logger logger = Logger.getLogger(IJavaCompletionProposalConverter.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }
    
    private final IProposableFixConverter fixConverter_;
    private final ChangeConverter changeConverter_;

    public IJavaCompletionProposalConverter(IProject original)
    {
        super(original, "proposal");
        fixConverter_ = new IProposableFixConverter(original);
        changeConverter_ = new ChangeConverter(original);
    }

    public IJavaCompletionProposal convert(IJavaCompletionProposal shadowProposal, IProblemLocation shadowLocation)
    {
        // Implement those:
        // MissingAnnotationAttributesProposal
        // NewDefiningMethodProposal
        
        IJavaCompletionProposal result = shadowProposal;
        if (shadowProposal instanceof NewMethodCorrectionProposal)
            // also a AbstractMethodCorrectionProposal, LinkedCorrectionProposal, ASTRewriteCorrectionProposal,
            // CUCorrectionProposal, ChangeCorrectionProposal
            result = convert((NewMethodCorrectionProposal) shadowProposal);
        else if (shadowProposal instanceof MissingReturnTypeCorrectionProposal)
            // also a LinkedCorrectionProposal, ASTRewriteCorrectionProposal, CUCorrectionProposal,
            // ChangeCorrectionProposal
            result = convert((MissingReturnTypeCorrectionProposal) shadowProposal);
        else if (shadowProposal instanceof TypeChangeCorrectionProposal)
            // also a LinkedCorrectionProposal, ASTRewriteCorrectionProposal, CUCorrectionProposal,
            // ChangeCorrectionProposal
            result = convert((TypeChangeCorrectionProposal) shadowProposal);
        else if (shadowProposal instanceof ModifierChangeCorrectionProposal)
            // also a LinkedCorrectionProposal, ASTRewriteCorrectionProposal, CUCorrectionProposal,
            // ChangeCorrectionProposal
            result = convert((ModifierChangeCorrectionProposal) shadowProposal);
        else if (shadowProposal instanceof CastCorrectionProposal)
            // also a LinkedCorrectionProposal, ASTRewriteCorrectionProposal, CUCorrectionProposal,
            // ChangeCorrectionProposal
            result = convert((CastCorrectionProposal) shadowProposal);
        else if (shadowProposal instanceof AddArgumentCorrectionProposal)
            // also a LinkedCorrectionProposal, ASTRewriteCorrectionProposal, CUCorrectionProposal,
            // ChangeCorrectionProposal
            result = convert((AddArgumentCorrectionProposal) shadowProposal);
        else if (shadowProposal instanceof ChangeMethodSignatureProposal)
            // also a LinkedCorrectionProposal, ASTRewriteCorrectionProposal, CUCorrectionProposal,
            // ChangeCorrectionProposal
            result = convert((ChangeMethodSignatureProposal) shadowProposal);
        else if (shadowProposal instanceof AddTypeParameterProposal)
            // also a LinkedCorrectionProposal, ASTRewriteCorrectionProposal, CUCorrectionProposal,
            // ChangeCorrectionProposal
            result = convert((AddTypeParameterProposal) shadowProposal);
        else if (shadowProposal instanceof NewVariableCorrectionProposal)
            // also a LinkedCorrectionProposal, ASTRewriteCorrectionProposal, CUCorrectionProposal,
            // ChangeCorrectionProposal
            result = convert((NewVariableCorrectionProposal) shadowProposal);
        else if (shadowProposal instanceof LinkedCorrectionProposal)
            // also an ASTRewriteCorrectionProposal, CUCorrectionProposal, ChangeCorrectionProposal
            result = convert((LinkedCorrectionProposal) shadowProposal);
        else if (shadowProposal instanceof CorrectMainTypeNameProposal)
            // also an ASTRewriteCorrectionProposal, CUCorrectionProposal, ChangeCorrectionProposal
            result = convert((CorrectMainTypeNameProposal) shadowProposal, shadowLocation);
        else if (shadowProposal instanceof AddImportCorrectionProposal)
            // also an ASTRewriteCorrectionProposal, CUCorrectionProposal, ChangeCorrectionProposal
            result = convert((AddImportCorrectionProposal) shadowProposal, shadowLocation);
        else if (shadowProposal instanceof ReorgCorrectionsSubProcessor.ClasspathFixCorrectionProposal)
            // also a CUCorrectionProposal, ChangeCorrectionProposal
            result = convert((ReorgCorrectionsSubProcessor.ClasspathFixCorrectionProposal) shadowProposal);
        else if (shadowProposal instanceof CorrectPackageDeclarationProposal)
            // also a CUCorrectionProposal, ChangeCorrectionProposal
            result = convert((CorrectPackageDeclarationProposal) shadowProposal);
        else if (shadowProposal instanceof RenameNodeCorrectionProposal)
            // also a CUCorrectionProposal, ChangeCorrectionProposal
            result = convert((RenameNodeCorrectionProposal) shadowProposal);
        else if (shadowProposal instanceof ReplaceCorrectionProposal)
            // also a CUCorrectionProposal, ChangeCorrectionProposal
            result = convert((ReplaceCorrectionProposal) shadowProposal);
        else if (shadowProposal instanceof FixCorrectionProposal)
            // also a CUCorrectionProposal, ChangeCorrectionProposal
            result = convert((FixCorrectionProposal) shadowProposal, shadowLocation);
        else if (shadowProposal instanceof ASTRewriteCorrectionProposal)
            // also a CUCorrectionProposal, ChangeCorrectionProposal
            result = convert((ASTRewriteCorrectionProposal) shadowProposal);
        else if (shadowProposal instanceof CUCorrectionProposal)
            // also a ChangeCorrectionProposal
            result = convert((CUCorrectionProposal)shadowProposal, shadowLocation);
        else if (shadowProposal instanceof NewCUUsingWizardProposal)
            // also a ChangeCorrectionProposal
            result = convert((NewCUUsingWizardProposal) shadowProposal);
        else if (shadowProposal instanceof ChangeCorrectionProposal)
            result = convert((ChangeCorrectionProposal) shadowProposal);
        else if (shadowProposal instanceof LinkedNamesAssistProposal)
            result = convert((LinkedNamesAssistProposal) shadowProposal);
        else
            logUnknownObjectType(shadowProposal);
        return result;
    }
    
    private CorrectMainTypeNameProposal convert(CorrectMainTypeNameProposal shadowProposal, IProblemLocation shadowLocation)
    {
        Exception thrown = null;
        try
        {
            Object oldName = getFieldValue("fOldName", shadowProposal);
            Object newName = getFieldValue("fNewName", shadowProposal);
            boolean oldNameCorrect = oldName == null || oldName instanceof String;
            boolean newNameCorrect = newName == null || newName instanceof String;
            if (newNameCorrect && oldNameCorrect)
            {
                ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
                IInvocationContext originalContext = new AssistContext(originalCompilationUnit, shadowLocation.getOffset(), shadowLocation.getLength());
                CorrectMainTypeNameProposal originalProposal = new CorrectMainTypeNameProposal(originalCompilationUnit, originalContext, (String) oldName, (String) newName, shadowProposal.getRelevance());
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(newNameCorrect, "Old name", newName);
                checkFieldName(oldNameCorrect, "New name", oldName);
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
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }

    // FIXME This failed once, I don't know when yet.
    // XXX This is kind of extreme hack and power.
    private CUCorrectionProposal convert(CUCorrectionProposal shadowProposal, IProblemLocation shadowLocation)
    {
        Exception thrown = null;
        ArrayList <CUCorrectionProposal> result = new ArrayList <CUCorrectionProposal>();
        ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
        @SuppressWarnings("unused")
        IInvocationContext context = new AssistContext(originalCompilationUnit, shadowLocation.getOffset(), shadowLocation.getLength());
//        try
//        {
//            LocalCorrectionsSubProcessor.addUncaughtExceptionProposals(context, shadowLocation, result);
//        }
//        catch (CoreException e)
//        {
//            thrown = e;
//        }
        ArrayList <CUCorrectionProposal> prunedResult = new ArrayList <CUCorrectionProposal>();
        for (CUCorrectionProposal proposal: result)
        {
            if (proposal.getDisplayString().equals(shadowProposal.getDisplayString()))
                prunedResult.add(proposal);
        }
        if (prunedResult.size() == 1)
            return result.get(0);
        else
        {
            logEclipseAPIChange(shadowProposal, thrown);
            logger.severe("result.size() = " + result.size());
            for (CUCorrectionProposal proposal: result)
                logger.severe(proposal.getDisplayString());
            logger.severe("prunedResult.size() = " + prunedResult.size());
            for (CUCorrectionProposal proposal: prunedResult)
                logger.severe(proposal.getDisplayString());
            return shadowProposal;
        }

        // Below does not work, but I am still keeping it as reference...
        //        Exception thrown = null;
        //        try
        //        {
        //            ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
        //            Object linkedProposalModel = getFieldValue("fLinkedProposalModel", shadowProposal);
        //            boolean linkedProposalModelCorrect = linkedProposalModel == null || linkedProposalModel instanceof LinkedProposalModel;
        //            TextChange textChange = null;
        //            CompilationUnitChange originalChange = null;
        //            try
        //            {
        //                textChange = shadowProposal.getTextChange();
        //                originalChange = new CompilationUnitChange(textChange.getName(), originalCompilationUnit);
        //                originalChange.setEdit(textChange.getEdit());
        ////                originalChange.setDescriptor(textChange.getDescriptor());
        ////                originalChange.setKeepPreviewEdits(true);
        ////                originalChange.getPreviewContent(null);
        //                for (TextEditChangeGroup group: textChange.getTextEditChangeGroups())
        //                {
        ////                    try
        ////                    {
        ////                        originalChange.addEdit(group.getTextChange().getEdit());
        ////                    }
        ////                    catch (MalformedTreeException e)
        ////                    {
        ////                        // do nothing.
        ////                    }
        //                    originalChange.addTextEditChangeGroup(group);
        //                }
        ////                originalChange.setSaveMode(textChange)
        ////                System.out.println(originalChange.getPreviewDocument(null).get());
        //            }
        //            catch (CoreException e)
        //            {
        //                e.printStackTrace();
        //                // XXX This should not be really important I hope.
        //            }
        //            if (linkedProposalModelCorrect)
        //            {
        //                LinkedProposalModel originalLinkedProposalModel = (LinkedProposalModel) linkedProposalModel;
        //                if (originalLinkedProposalModel == null)
        //                    originalLinkedProposalModel = new LinkedProposalModel();
        //                CUCorrectionProposal originalProposal = new CUCorrectionProposal(shadowProposal.getName(), originalCompilationUnit, originalChange, shadowProposal.getRelevance(), shadowProposal.getImage());
        //                originalProposal.setLinkedProposalModel(originalLinkedProposalModel);
        //                Object info = originalProposal.getAdditionalProposalInfo(null);
        //                return originalProposal;
        //            }
        //            else
        //            {
        //                logEclipseAPIChange(shadowProposal);
        //                checkFieldName(linkedProposalModelCorrect, "Linked proposal model", linkedProposalModel);
        //            }
        //        }
        //        catch (NoSuchFieldException e)
        //        {
        //            thrown = e;
        //        }
        //        catch (IllegalAccessException e)
        //        {
        //            thrown = e;
        //        }
        //        logWrongUsageOfReflection(shadowProposal, thrown);
        //        return shadowProposal;
    }

    private LinkedNamesAssistProposal convert(LinkedNamesAssistProposal shadowProposal)
    {
        Exception thrown = null;
        try
        {
            Object label = getFieldValue("fLabel", shadowProposal);
            Object context = getFieldValue("fContext", shadowProposal);
            Object node = getFieldValue("fNode", shadowProposal);
            Object valueSuggestion = getFieldValue("fValueSuggestion", shadowProposal);
            boolean labelCorrect = label == null || label instanceof String;
            boolean contextCorrect = context == null || context instanceof IInvocationContext;
            boolean nodeCorrect = node == null || node instanceof SimpleName;
            boolean valueSuggestionCorrect = valueSuggestion == null || valueSuggestion instanceof String;
            if (contextCorrect && labelCorrect && nodeCorrect && valueSuggestionCorrect)
            {
                IInvocationContext shadowContext = (IInvocationContext) context;
                ICompilationUnit originalCompilationUnit = convert(shadowContext.getCompilationUnit());
                IInvocationContext originalContext = new AssistContext(originalCompilationUnit,
                        shadowContext.getSelectionOffset(), shadowContext.getSelectionLength());
                /*
                 * XXX I haven't expected node to work directly (since they are still connected to the shadow
                 * compilation unit's AST), however they work at the moment.
                 */
                LinkedNamesAssistProposal originalProposal = new LinkedNamesAssistProposal((String) label,
                        originalContext, (SimpleName) node, (String) valueSuggestion);
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(contextCorrect, "Context", context);
                checkFieldName(labelCorrect, "Label", label);
                checkFieldName(nodeCorrect, "Node", node);
                checkFieldName(valueSuggestionCorrect, "Value suggestion", valueSuggestion);
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
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }

    private MissingReturnTypeCorrectionProposal convert(MissingReturnTypeCorrectionProposal shadowProposal)
    {
        Exception thrown = null;
        try
        {
            Object methodDeclaration = getFieldValue("fMethodDecl", shadowProposal);
            Object existingReturn = getFieldValue("fExistingReturn", shadowProposal);
            boolean methodDeclarationCorrect = methodDeclaration == null
            || methodDeclaration instanceof MethodDeclaration;
            boolean existingReturnCorrect = existingReturn == null || existingReturn instanceof ReturnStatement;
            if (existingReturnCorrect && methodDeclarationCorrect)
            {
                ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
                /*
                 * XXX I haven't expected methodDeclaration and existingReturn fields to work directly (since they are
                 * still connected to the shadow compilation unit's AST), however they work at the moment.
                 */
                MissingReturnTypeCorrectionProposal originalProposal = new MissingReturnTypeCorrectionProposal(
                        originalCompilationUnit, (MethodDeclaration) methodDeclaration,
                        (ReturnStatement) existingReturn, shadowProposal.getRelevance());
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(existingReturnCorrect, "Method declaration", existingReturn);
                checkFieldName(methodDeclarationCorrect, "Existing return", methodDeclaration);
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
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }

    private TypeChangeCorrectionProposal convert(TypeChangeCorrectionProposal shadowProposal)
    {
        Exception thrown = null;
        try
        {
            Object binding = getFieldValue("fBinding", shadowProposal);
            Object newType = getFieldValue("fNewType", shadowProposal);
            Object offerSuperTypeProposals = getFieldValue("fOfferSuperTypeProposals", shadowProposal);
            boolean bindingCorrect = binding == null || binding instanceof IBinding;
            boolean newTypeCorrect = newType == null || newType instanceof ITypeBinding;
            boolean offerSuperTypeProposalsCorrect = offerSuperTypeProposals == null
            || offerSuperTypeProposals instanceof Boolean;
            if (newTypeCorrect && bindingCorrect && offerSuperTypeProposalsCorrect)
            {
                ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
                CompilationUnit originalASTCompilationUnit = createCompilationUnitFrom(originalCompilationUnit);
                TypeChangeCorrectionProposal originalProposal = new TypeChangeCorrectionProposal(
                        originalCompilationUnit, (IBinding) binding, originalASTCompilationUnit,
                        (ITypeBinding) newType, (Boolean) offerSuperTypeProposals, shadowProposal.getRelevance());
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(newTypeCorrect, "Node", newType);
                checkFieldName(bindingCorrect, "New type", binding);
                checkFieldName(offerSuperTypeProposalsCorrect, "Offer super type proposals", offerSuperTypeProposals);
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
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }

    private ModifierChangeCorrectionProposal convert(ModifierChangeCorrectionProposal shadowProposal)
    {
        Exception thrown = null;
        try
        {
            Object node = getFieldValue("fNode", shadowProposal);
            Object binding = getFieldValue("fBinding", shadowProposal);
            Object includedModifiers = getFieldValue("fIncludedModifiers", shadowProposal);
            Object excludedModifiers = getFieldValue("fExcludedModifiers", shadowProposal);
            boolean nodeCorrect = node == null || node instanceof ASTNode;
            boolean bindingCorrect = binding == null || binding instanceof IBinding;
            boolean includedModifiersCorrect = includedModifiers == null || includedModifiers instanceof Integer;
            boolean excludedModifiersCorrect = excludedModifiers == null || excludedModifiers instanceof Integer;
            if (nodeCorrect && bindingCorrect && includedModifiersCorrect && excludedModifiersCorrect)
            {
                ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
                /*
                 * XXX I didn't actually expected the cast of node would directly work here. I was expecting it to
                 * require the same ASTNode node from the original file. If a problem occurs later on, this is the place
                 * that needs to be looked. It works fine at the moment.
                 */
                ModifierChangeCorrectionProposal originalProposal = new ModifierChangeCorrectionProposal(
                        shadowProposal.getName(), originalCompilationUnit, (IBinding) binding, (ASTNode) node,
                        (Integer) includedModifiers, (Integer) excludedModifiers, shadowProposal.getRelevance(),
                        shadowProposal.getImage());
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(nodeCorrect, "Node", node);
                checkFieldName(bindingCorrect, "Binding", binding);
                checkFieldName(includedModifiersCorrect, "Included modifiers", includedModifiers);
                checkFieldName(excludedModifiersCorrect, "Excluded modifiers", excludedModifiers);
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
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }

    private CastCorrectionProposal convert(CastCorrectionProposal shadowProposal)
    {
        Exception thrown = null;
        try
        {
            Object nodeToCast = getFieldValue("fNodeToCast", shadowProposal);
            Object castType = getFieldValue("fCastType", shadowProposal);
            boolean nodeToCastCorrect = nodeToCast == null || nodeToCast instanceof Expression;
            boolean castTypeCorrect = castType == null || castType instanceof ITypeBinding;
            if (nodeToCastCorrect && castTypeCorrect)
            {
                ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
                /*
                 * XXX I didn't actually expected the cast of nodeToCast would directly work here. I was expecting it to
                 * require the same Expression node from the original file. If a problem occurs later on, this is the
                 * place that needs to be looked. It works fine at the moment.
                 */
                CastCorrectionProposal originalProposal = new CastCorrectionProposal(shadowProposal.getName(),
                        originalCompilationUnit, (Expression) nodeToCast, (ITypeBinding) castType,
                        shadowProposal.getRelevance());
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(nodeToCastCorrect, "Node to cast", nodeToCast);
                checkFieldName(castTypeCorrect, "Cast type", castType);
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
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }

    private NewVariableCorrectionProposal convert(NewVariableCorrectionProposal shadowProposal)
    {
        Exception thrown = null;
        try
        {
            Object originalNode = getFieldValue("fOriginalNode", shadowProposal);
            Object senderBinding = getFieldValue("fSenderBinding", shadowProposal);
            boolean originalNodeCorrect = originalNode == null || originalNode instanceof SimpleName;
            boolean senderBindingCorrect = senderBinding == null || senderBinding instanceof ITypeBinding;
            if (originalNodeCorrect && senderBindingCorrect)
            {
                ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
                NewVariableCorrectionProposal originalProposal = new NewVariableCorrectionProposal(
                        shadowProposal.getName(), originalCompilationUnit, shadowProposal.getVariableKind(),
                        (SimpleName) originalNode, (ITypeBinding) senderBinding, shadowProposal.getRelevance(),
                        shadowProposal.getImage());
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(originalNodeCorrect, "Original node", originalNode);
                checkFieldName(senderBindingCorrect, "Sender binding", senderBinding);
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
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }

    private LinkedCorrectionProposal convert(LinkedCorrectionProposal shadowProposal)
    {
        Exception thrown = null;
        try
        {
            /*
             * field 'fRewrite' comes from the super class of LinkedCorrectionProposal: ASTRewriteCorrectionProposal.
             * That is why we need to pass the super class' Class argument to get(...)
             */
            Object rewrite = getFieldValue("fRewrite", shadowProposal, shadowProposal.getClass().getSuperclass());
            boolean rewriteCorrect = rewrite == null || rewrite instanceof ASTRewrite;
            if (rewriteCorrect)
            {
                ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
                LinkedCorrectionProposal originalProposal = new LinkedCorrectionProposal(shadowProposal.getName(),
                        originalCompilationUnit, (ASTRewrite) rewrite, shadowProposal.getRelevance(),
                        shadowProposal.getImage());
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(rewriteCorrect, "Rewrite", rewrite);
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
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }

    private AddArgumentCorrectionProposal convert(AddArgumentCorrectionProposal shadowProposal)
    {
        Exception thrown = null;
        try
        {
            Object callerNode = getFieldValue("fCallerNode", shadowProposal);
            Object insertIndexes = getFieldValue("fInsertIndexes", shadowProposal);
            Object parameterTypes = getFieldValue("fParamTypes", shadowProposal);
            boolean callerNodeCorrect = callerNode == null || callerNode instanceof ASTNode;
            boolean insertIndexesCorrect = insertIndexes == null || insertIndexes instanceof int [];
            boolean parameterTypesCorrect = parameterTypes == null || parameterTypes instanceof ITypeBinding [];
            if (callerNodeCorrect && insertIndexesCorrect && parameterTypesCorrect)
            {
                ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
                AddArgumentCorrectionProposal originalProposal = new AddArgumentCorrectionProposal(
                        shadowProposal.getName(), originalCompilationUnit, (ASTNode) callerNode,
                        (int []) insertIndexes, (ITypeBinding []) parameterTypes, shadowProposal.getRelevance());
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(callerNodeCorrect, "Caller Node", callerNode);
                checkFieldName(insertIndexesCorrect, "Insert indexes", insertIndexes);
                checkFieldName(parameterTypesCorrect, "Parameter types", parameterTypes);
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
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }

    private ChangeMethodSignatureProposal convert(ChangeMethodSignatureProposal shadowProposal)
    {
        Exception thrown = null;
        try
        {
            Object invocationNode = getFieldValue("fInvocationNode", shadowProposal);
            Object parameterChanges = getFieldValue("fParameterChanges", shadowProposal);
            Object exceptionChanges = getFieldValue("fExceptionChanges", shadowProposal);
            Object binding = getFieldValue("fSenderBinding", shadowProposal);
            boolean invocationNodeCorrect = invocationNode == null || invocationNode instanceof ASTNode;
            boolean parameterChangesCorrect = parameterChanges == null
            || parameterChanges instanceof ChangeDescription [];
            boolean exceptionChangesCorrect = exceptionChanges == null
            || exceptionChanges instanceof ChangeDescription [];
            boolean bindingCorrect = binding == null || binding instanceof IMethodBinding;
            if (invocationNodeCorrect && parameterChangesCorrect && exceptionChangesCorrect && bindingCorrect)
            {
                ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
                ChangeMethodSignatureProposal originalProposal = new ChangeMethodSignatureProposal(
                        shadowProposal.getName(), originalCompilationUnit, (ASTNode) invocationNode,
                        (IMethodBinding) binding, (ChangeDescription []) parameterChanges,
                        (ChangeDescription []) exceptionChanges, shadowProposal.getRelevance(),
                        shadowProposal.getImage());
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(invocationNodeCorrect, "Invocation Node", invocationNode);
                checkFieldName(parameterChangesCorrect, "Parameter changes", parameterChanges);
                checkFieldName(exceptionChangesCorrect, "Exception changes", exceptionChanges);
                checkFieldName(bindingCorrect, "Binding", binding);
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
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }

    @SuppressWarnings("unchecked")
    private NewMethodCorrectionProposal convert(NewMethodCorrectionProposal shadowProposal)
    {
        Exception thrown = null;
        try
        {
            // 'fNode' field is coming from AbstractMethodCorrectionProposal, which is super class of
            // NewMethodCorrectionProposal
            Object invocationNode = getFieldValue("fNode", shadowProposal, shadowProposal.getClass().getSuperclass());
            Object arguments = getFieldValue("fArguments", shadowProposal);
            // 'fSenderBinding' field is coming from AbstractMethodCorrectionProposal, which is super class of
            // NewMethodCorrectionProposal
            Object binding = getFieldValue("fSenderBinding", shadowProposal, shadowProposal.getClass().getSuperclass());
            boolean invocationNodeCorrect = invocationNode == null || invocationNode instanceof ASTNode;
            boolean argumentsCorrect = arguments == null || arguments instanceof List;
            boolean bindingCorrect = binding == null || binding instanceof ITypeBinding;
            if (invocationNodeCorrect && argumentsCorrect && bindingCorrect)
            {
                ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
                @SuppressWarnings("rawtypes") NewMethodCorrectionProposal originalProposal = new NewMethodCorrectionProposal(
                        shadowProposal.getName(), originalCompilationUnit, (ASTNode) invocationNode, (List) arguments,
                        (ITypeBinding) binding, shadowProposal.getRelevance(), shadowProposal.getImage());
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(invocationNodeCorrect, "Invocation Node", invocationNode);
                checkFieldName(argumentsCorrect, "Arguments", arguments);
                checkFieldName(bindingCorrect, "Binding", binding);
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
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }

    private ChangeCorrectionProposal convert(ChangeCorrectionProposal shadowProposal)
    {
        System.out.println(shadowProposal.getDisplayString());
        Exception thrown = null;
        try
        {
            Change shadowChange = shadowProposal.getChange();
            Change originalChange = changeConverter_.convert(shadowChange);
            ChangeCorrectionProposal originalProposal = new ChangeCorrectionProposal(shadowProposal.getName(), originalChange, shadowProposal.getRelevance(), shadowProposal.getImage());
            return originalProposal;
        }
        catch (CoreException e)
        {
            thrown = e;
        }
        logWrongUsageOfReflection(shadowProposal, thrown);
        // It seems that there is nothing project specific implemented inside the change correction proposal.
        return shadowProposal;
    }

    private FixCorrectionProposal convert(FixCorrectionProposal shadowProposal, IProblemLocation shadowLocation)
    {
        Exception thrown = null;
        try
        {
            Object fix = getFieldValue("fFix", shadowProposal);
            Object cleanUp = getFieldValue("fCleanUp", shadowProposal);
            boolean fixCorrect = fix == null || fix instanceof IProposableFix;
            boolean cleanUpCorrect = cleanUp == null || cleanUp instanceof ICleanUp;
            if (fixCorrect && cleanUpCorrect)
            {
                ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
                IInvocationContext originalInvocationContext = new AssistContext(originalCompilationUnit,
                        shadowLocation.getOffset(), shadowLocation.getLength());
                IProposableFix shadowFix = (IProposableFix) fix;
                IProposableFix originalFix = shadowFix;
                if (shadowFix != null)
                    originalFix = fixConverter_.convert(shadowFix, shadowProposal, shadowLocation);
                ICleanUp shadowCleanUp = (ICleanUp) cleanUp;
                ICleanUp originalCleanUp = shadowCleanUp;
                FixCorrectionProposal originalProposal = new FixCorrectionProposal(originalFix, originalCleanUp,
                        shadowProposal.getRelevance(), shadowProposal.getImage(), originalInvocationContext);
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(fixCorrect, "Fix", fix);
                checkFieldName(cleanUpCorrect, "Clean up", cleanUp);
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
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }

    private ReplaceCorrectionProposal convert(ReplaceCorrectionProposal shadowProposal)
    {
        Exception thrown = null;
        try
        {
            Object offset = getFieldValue("fOffset", shadowProposal);
            Object length = getFieldValue("fLength", shadowProposal);
            Object replacementString = getFieldValue("fReplacementString", shadowProposal);
            boolean offsetCorrect = offset == null || offset instanceof Integer;
            boolean lengthCorrect = length == null || length instanceof Integer;
            boolean replacementStringCorrect = replacementString == null || replacementString instanceof String;
            if (offsetCorrect && lengthCorrect && replacementStringCorrect)
            {
                ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
                ReplaceCorrectionProposal originalProposal = new ReplaceCorrectionProposal(shadowProposal.getName(),
                        originalCompilationUnit, (Integer) offset, (Integer) length, (String) replacementString,
                        shadowProposal.getRelevance());
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(offsetCorrect, "Offset", offset);
                checkFieldName(lengthCorrect, "Length", length);
                checkFieldName(replacementStringCorrect, "Replacement String", replacementString);
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
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }

    private RenameNodeCorrectionProposal convert(RenameNodeCorrectionProposal shadowProposal)
    {
        Exception thrown = null;
        try
        {
            Object offset = getFieldValue("fOffset", shadowProposal);
            Object length = getFieldValue("fLength", shadowProposal);
            Object newName = getFieldValue("fNewName", shadowProposal);
            boolean offsetCorrect = offset == null || offset instanceof Integer;
            boolean lengthCorrect = length == null || length instanceof Integer;
            boolean newNameCorrect = newName == null || newName instanceof String;
            if (offsetCorrect && lengthCorrect && newNameCorrect)
            {
                ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
                RenameNodeCorrectionProposal originalProposal = new RenameNodeCorrectionProposal(
                        shadowProposal.getName(), originalCompilationUnit, (Integer) offset, (Integer) length,
                        (String) newName, shadowProposal.getRelevance());
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(offsetCorrect, "Offset", offset);
                checkFieldName(lengthCorrect, "Length", length);
                checkFieldName(newNameCorrect, "New name", newName);
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
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }

    private CorrectPackageDeclarationProposal convert(CorrectPackageDeclarationProposal shadowProposal)
    {
        Exception thrown = null;
        try
        {
            Object location = getFieldValue("fLocation", shadowProposal);
            boolean locationCorrect = location == null || location instanceof IProblemLocation;
            if (locationCorrect)
            {
                ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
                CorrectPackageDeclarationProposal originalProposal = new CorrectPackageDeclarationProposal(
                        originalCompilationUnit, (IProblemLocation) location, shadowProposal.getRelevance());
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(locationCorrect, "Location", location);
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
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }

    private ReorgCorrectionsSubProcessor.ClasspathFixCorrectionProposal convert(
            ReorgCorrectionsSubProcessor.ClasspathFixCorrectionProposal shadowProposal)
    {
        Exception thrown = null;
        try
        {
            Object offset = getFieldValue("fOffset", shadowProposal);
            Object length = getFieldValue("fLength", shadowProposal);
            Object missingType = getFieldValue("fMissingType", shadowProposal);
            boolean offsetCorrect = offset == null || offset instanceof Integer;
            boolean lengthCorrect = length == null || length instanceof Integer;
            boolean missingTypeCorrect = missingType == null || missingType instanceof String;
            if (offsetCorrect && lengthCorrect && missingTypeCorrect)
            {
                ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
                ReorgCorrectionsSubProcessor.ClasspathFixCorrectionProposal originalProposal = new ReorgCorrectionsSubProcessor.ClasspathFixCorrectionProposal(
                        originalCompilationUnit, (Integer) offset, (Integer) length, (String) missingType);
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(offsetCorrect, "Offset", offset);
                checkFieldName(lengthCorrect, "Length", length);
                checkFieldName(missingTypeCorrect, "Missing type", missingType);
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
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }

    private AddImportCorrectionProposal convert(AddImportCorrectionProposal shadowProposal,
            IProblemLocation shadowLocation)
    {
        Exception thrown = null;
        try
        {
            Object qualifierName = getFieldValue("fQualifierName", shadowProposal);
            Object typeName = getFieldValue("fTypeName", shadowProposal);
            ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
            CompilationUnit originalASTCompilationUnit = createCompilationUnitFrom(originalCompilationUnit);
            ASTNode node = shadowLocation.getCoveringNode(originalASTCompilationUnit);
            boolean qualifierNameCorrect = qualifierName == null || qualifierName instanceof String;
            boolean typeNameCorrect = typeName == null || typeName instanceof String;
            boolean nodeCorrect = node == null || node instanceof SimpleName;
            if (qualifierNameCorrect && typeNameCorrect && nodeCorrect)
            {
                AddImportCorrectionProposal originalProposal = new AddImportCorrectionProposal(
                        shadowProposal.getName(), originalCompilationUnit, shadowProposal.getRelevance(),
                        shadowProposal.getImage(), (String) qualifierName, (String) typeName, (SimpleName) node);
                // Below is adapted from UnresolvedElementsSubProcessor#createTypeRefChangeProposal(ICompilationUnit,
                // String, Name, int, int)
                originalProposal.setCommandId(shadowProposal.getCommandId());
                ImportRewrite importRewrite = shadowProposal.getImportRewrite();
                if (importRewrite != null)
                    originalProposal.setImportRewrite(importRewrite);
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(qualifierNameCorrect, "Qualifier name", qualifierName);
                checkFieldName(typeNameCorrect, "Type name", typeName);
                checkFieldName(nodeCorrect, "Node", node);
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
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }

    private AddTypeParameterProposal convert(AddTypeParameterProposal shadowProposal)
    {
        Exception thrown = null;
        try
        {
            Object binding = getFieldValue("fBinding", shadowProposal);
            Object typeParameterName = getFieldValue("fTypeParamName", shadowProposal);
            Object bounds = getFieldValue("fBounds", shadowProposal);
            boolean bindingCorrect = binding == null || binding instanceof IBinding;
            boolean typeParameterNameCorrect = typeParameterName == null || typeParameterName instanceof String;
            boolean boundsCorrect = bounds == null || bounds instanceof ITypeBinding [];
            if (bindingCorrect && typeParameterNameCorrect && boundsCorrect)
            {
                ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
                CompilationUnit originalASTCompilationUnit = createCompilationUnitFrom(originalCompilationUnit);
                AddTypeParameterProposal originalProposal = new AddTypeParameterProposal(originalCompilationUnit,
                        (IBinding) binding, originalASTCompilationUnit, (String) typeParameterName,
                        (ITypeBinding []) bounds, shadowProposal.getRelevance());
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(boundsCorrect, "Bounds", bounds);
                checkFieldName(bindingCorrect, "Binding", binding);
                checkFieldName(typeParameterNameCorrect, "Type parameter name", typeParameterName);
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
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }

    private NewCUUsingWizardProposal convert(NewCUUsingWizardProposal shadowProposal)
    {
        Exception thrown = null;
        try
        {
            Object typeContainer = getFieldValue("fTypeContainer", shadowProposal);
            Object compilationUnit = getFieldValue("fCompilationUnit", shadowProposal);
            Object node = getFieldValue("fNode", shadowProposal);
            /*
             * type container for the original is not created yet, therefore we need to use the exact one coming from
             * the shadow proposal. It works.
             */
            boolean typeContainerCorrect = typeContainer == null || typeContainer instanceof IJavaElement;
            boolean compilationUnitCorrect = compilationUnit == null || compilationUnit instanceof ICompilationUnit;
            boolean nodeCorrect = node == null || node instanceof Name;
            if (typeContainerCorrect && compilationUnitCorrect && nodeCorrect)
            {
                ICompilationUnit originalcompilationUnit = convert((ICompilationUnit) compilationUnit);
                NewCUUsingWizardProposal originalProposal = new NewCUUsingWizardProposal(originalcompilationUnit,
                        (Name) node, shadowProposal.getTypeKind(), (IJavaElement) typeContainer,
                        shadowProposal.getRelevance());
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(typeContainerCorrect, "Type container", typeContainer);
                checkFieldName(compilationUnitCorrect, "Compilation unit", compilationUnit);
                checkFieldName(nodeCorrect, "Node", node);
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
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }

    private ASTRewriteCorrectionProposal convert(ASTRewriteCorrectionProposal shadowProposal)
    {
        Exception thrown = null;
        try
        {
            ICompilationUnit originalCompilationUnit = convert(shadowProposal.getCompilationUnit());
            Object rewrite = getFieldValue("fRewrite", shadowProposal);
            boolean rewriteCorrect = rewrite == null || rewrite instanceof ASTRewrite;
            if (rewriteCorrect)
            {
                ASTRewriteCorrectionProposal originalProposal = new ASTRewriteCorrectionProposal(
                        shadowProposal.getName(), originalCompilationUnit, (ASTRewrite) rewrite,
                        shadowProposal.getRelevance(), shadowProposal.getImage());
                return originalProposal;
            }
            else
            {
                logEclipseAPIChange(shadowProposal);
                checkFieldName(rewriteCorrect, "Rewrite", rewrite);
            }
        }
        catch (IllegalAccessException e)
        {
            thrown = e;
        }
        catch (NoSuchFieldException e)
        {
            thrown = e;
        }
        logWrongUsageOfReflection(shadowProposal, thrown);
        return shadowProposal;
    }
}
