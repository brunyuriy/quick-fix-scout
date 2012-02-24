package edu.cs.washington.quickfix.speculation.converter;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameCompilationUnitChange;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;

@SuppressWarnings("restriction")
public class ChangeConverter extends EclipseObjectConverter
{
    private final static Logger logger = Logger.getLogger(ChangeConverter.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }

    public ChangeConverter(IProject original)
    {
        super(original, "Change");
    }

    protected Change convert(Change shadowChange)
    {
        Change result = shadowChange;
        if (shadowChange instanceof RenameCompilationUnitChange)
            result = convert((RenameCompilationUnitChange) shadowChange);
        else if (shadowChange instanceof NullChange)
            // There is no conversion needed for NullChange.
            result = shadowChange;
        logUnknownObjectType(shadowChange);
        return result;
    }

    private RenameCompilationUnitChange convert(RenameCompilationUnitChange shadowChange)
    {
        Exception thrown = null;
        try
        {
            Object resourcePath = getFieldValue("fResourcePath", shadowChange, shadowChange.getClass().getSuperclass());
            // Object stampToRestore = getFieldValue("fStampToRestore", shadowChange,
            // shadowChange.getClass().getSuperclass());
            boolean resourcePathCorrect = resourcePath == null || resourcePath instanceof IPath;
            // boolean stampToRestoreCorrect = stampToRestore == null || stampToRestore instanceof Long;
            if (resourcePathCorrect)
            {
                IPath path = (IPath) resourcePath;
                // substring(1) removes the starting '/' from the string.
                String pathString = path.toString().substring(1);
                // TODO This is hacky stuff. Try to improve later on.
                IPath relativePath = new Path(pathString.substring(pathString.indexOf("/") + 1));
                IFile originalFile = getOriginalProject().getFile(relativePath);
                ICompilationUnit originalCompilationUnit = JavaCore.createCompilationUnitFrom(originalFile);
                if (originalCompilationUnit == null)
                    logger.severe("Couldn't create original compilation unit for rename compilation unit change. Relative path = "
                            + relativePath.toString());
                RenameCompilationUnitChange originalChange = new RenameCompilationUnitChange(originalCompilationUnit,
                        shadowChange.getNewName());
                return originalChange;
            }
            else
            {
                logEclipseAPIChange(shadowChange);
                checkFieldName(resourcePathCorrect, "Resource path", resourcePath);
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
        logWrongUsageOfReflection(shadowChange, thrown);
        return shadowChange;
    }
}
