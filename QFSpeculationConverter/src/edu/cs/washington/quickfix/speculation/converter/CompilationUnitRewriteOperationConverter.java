package edu.cs.washington.quickfix.speculation.converter;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.AddUnimplementedMethodsOperation;
import org.eclipse.jdt.internal.corext.fix.UnimplementedCodeFix;
import org.eclipse.jdt.internal.corext.fix.UnusedCodeFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

@SuppressWarnings("restriction")
public class CompilationUnitRewriteOperationConverter extends EclipseObjectConverter
{
    private static final String REMOVE_IMPORT_OPERATION_CLASS = "class org.eclipse.jdt.internal.corext.fix.UnusedCodeFix$RemoveImportOperation";

    public CompilationUnitRewriteOperationConverter(IProject original)
    {
        super(original, "rewrite operation");
    }

    public CompilationUnitRewriteOperation convert(CompilationUnitRewriteOperation shadowRewriteOperation,
            CompilationUnit originalASTCompilationUnit, IProblemLocation shadowLocation)
    {
        CompilationUnitRewriteOperation result = shadowRewriteOperation;
        if (shadowRewriteOperation instanceof UnimplementedCodeFix.MakeTypeAbstractOperation)
            result = convert((UnimplementedCodeFix.MakeTypeAbstractOperation) shadowRewriteOperation,
                    originalASTCompilationUnit, shadowLocation);
        else if (shadowRewriteOperation.getClass().toString().equals(REMOVE_IMPORT_OPERATION_CLASS))
            // Going extreme hack! I have to do this since the class is a private class.
            result = convertRemoveImportOperation(shadowRewriteOperation, originalASTCompilationUnit, shadowLocation);
        else if (shadowRewriteOperation instanceof AddUnimplementedMethodsOperation)
            result = convert((AddUnimplementedMethodsOperation) shadowRewriteOperation, originalASTCompilationUnit, shadowLocation);
        else
            logUnknownObjectType(shadowRewriteOperation);
        return result;
    }

    private AddUnimplementedMethodsOperation convert(AddUnimplementedMethodsOperation shadowRewriteOperation, CompilationUnit originalASTCompilationUnit, IProblemLocation shadowLocation)
    {
        ASTNode typeNode = UnimplementedCodeFix.getSelectedTypeNode(originalASTCompilationUnit, shadowLocation);
        AddUnimplementedMethodsOperation originalRewriteOperation = new AddUnimplementedMethodsOperation(typeNode);
        return originalRewriteOperation;
    }

    private CompilationUnitRewriteOperation convertRemoveImportOperation(
            CompilationUnitRewriteOperation shadowRewriteOperation, CompilationUnit origianlASTCompilationUnit,
            IProblemLocation shadowLocation)
    {
        Exception thrown = null;
        try
        {
            Class <?> removeImportOperationClass = getInnerClass(UnusedCodeFix.class, REMOVE_IMPORT_OPERATION_CLASS);
            ImportDeclaration originalImportDeclaration = getImportDeclaration(shadowLocation,
                    origianlASTCompilationUnit);
            Object object = constructObject(removeImportOperationClass, ImportDeclaration.class,
                    originalImportDeclaration);
            boolean objectCorrect = object == null || object instanceof CompilationUnitRewriteOperation;
            if (objectCorrect)
            {
                CompilationUnitRewriteOperation originalRewriteOperation = (CompilationUnitRewriteOperation) object;
                return originalRewriteOperation;
            }
            else
            {
                logEclipseAPIChange(shadowRewriteOperation);
                checkFieldName(objectCorrect, "constructed object", object);
            }
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
        catch (IllegalAccessException e)
        {
            thrown = e;
        }
        catch (InvocationTargetException e)
        {
            thrown = e;
        }
        logWrongUsageOfReflection(shadowRewriteOperation, thrown);
        return shadowRewriteOperation;
    }

    private UnimplementedCodeFix.MakeTypeAbstractOperation convert(
            UnimplementedCodeFix.MakeTypeAbstractOperation shadowRewriteOperation,
            CompilationUnit originalASTCompilationUnit, IProblemLocation shadowLocation)
    {
        ASTNode typeDeclaration = UnimplementedCodeFix.getSelectedTypeNode(originalASTCompilationUnit, shadowLocation);
        boolean typeDeclarationCorrect = typeDeclaration == null || typeDeclaration instanceof TypeDeclaration;
        if (typeDeclarationCorrect)
        {
            TypeDeclaration originalTypeDeclaration = (TypeDeclaration) typeDeclaration;
            UnimplementedCodeFix.MakeTypeAbstractOperation originalRewriteOperation = new UnimplementedCodeFix.MakeTypeAbstractOperation(
                    originalTypeDeclaration);
            return originalRewriteOperation;
        }
        else
        {
            logEclipseAPIChange(shadowRewriteOperation);
            checkFieldName(typeDeclarationCorrect, "Type declaration", typeDeclaration);
        }
        return shadowRewriteOperation;
    }

    // This code is copied from UnusedCodeFix.java
    private static ImportDeclaration getImportDeclaration(IProblemLocation problem, CompilationUnit compilationUnit)
    {
        ASTNode selectedNode = problem.getCoveringNode(compilationUnit);
        if (selectedNode != null)
        {
            ASTNode node = ASTNodes.getParent(selectedNode, ASTNode.IMPORT_DECLARATION);
            if (node instanceof ImportDeclaration) { return (ImportDeclaration) node; }
        }
        return null;
    }
}
