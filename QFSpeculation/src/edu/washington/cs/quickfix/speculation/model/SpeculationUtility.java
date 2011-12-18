package edu.washington.cs.quickfix.speculation.model;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import edu.washington.cs.quickfix.speculation.calc.SpeculationCalculator;
import edu.washington.cs.util.eclipse.ResourceUtility;
import edu.washington.cs.util.eclipse.model.Squiggly;
import edu.washington.cs.util.eclipse.model.CompilationErrorDetails;
import edu.washington.cs.util.exception.NotInitializedException;

public class SpeculationUtility
{
    // Logger.
    private static final Logger logger = Logger.getLogger(SpeculationUtility.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }

    /**
     * This class cannot be instantiated.
     */
    private SpeculationUtility()
    {}

    /**************
     * PUBLIC API *
     *************/
    
    public static void printProblemLocation(IProblemLocation location)
    {
        logger.info("");
        logger.info("Problem ID = " + Integer.toHexString(location.getProblemId()));
        logger.info("Length = " + location.getLength());
        logger.info("Offset = " + location.getOffset());
        logger.info("Args: ");
        for (int a = 0; a < location.getProblemArguments().length; a++)
            logger.info((a+1) + "-) " + location.getProblemArguments()[a]);
        logger.info("");
    }
    
    public static boolean areOnTheSameLine(Squiggly ce1, Squiggly ce2)
    {
        CompilationErrorDetails d1 = ce1.computeDetails();
        CompilationErrorDetails d2 = ce2.computeDetails();
        return d1.getFile().getName().equals(d2.getFile().getName()) && d1.getLine() == d2.getLine();
    }
    
    // Flagged proposals are the proposals that force getting out of sync (after undo application)
    // That is why they are not computed during the speculation and represented with (?) instead of
    // N/A in Quick Fix Dialog.
    public static boolean isFlaggedProposal(IJavaCompletionProposal proposal)
    {
        String displayString = proposal.getDisplayString();
        if (displayString.startsWith("Rename compilation unit to '") && displayString.endsWith(".java'"))
            return true;
        return false;
    }
    
    public static boolean sameProblemLocationContent(IProblemLocation location1, IProblemLocation location2)
    {
//        printProblemLocation(location1);
//        printProblemLocation(location2);
        if (location1.getProblemId() != location2.getProblemId())
            return false;
        if (location1.getLength() != location2.getLength())
            return false;
         if (location1.getOffset() != location2.getOffset())
             return false;
        String [] args1 = location1.getProblemArguments();
        String [] args2 = location2.getProblemArguments();
        if (args1.length != args2.length)
            return false;
        // TODO need to highly justify this, seems to work with the comparison of arguments at the moment.
        for (int a = 0; a < args1.length; a++)
        {
            String arg1 = args1[a];
            String arg2 = args2[a];
            // first check whether the arguments are the same or not. 
            if (!arg1.equals(arg2))
            {
                // if they are not the same, then this might be due to different paths in the shadow and original projects.
                boolean valid = false;
                try
                {
                    IFile file1 = ResourceUtility.getFile(new Path(arg1));
                    IFile file2 = ResourceUtility.getFile(new Path(arg2));
                    if (file1.getProjectRelativePath().toString().equals(file2.getProjectRelativePath().toString()))
                        valid = true;
                }
                catch (Exception e)
                {
                    // If an exception is thrown, this means that the arguments were not representing files, so we just assume that
                    // our assumption was wrong. That is why, there is no need to do anything in this case.
                }
                if (!valid)
                {
                    logger.fine("Problem locations differ on args => arg1 = " + arg1 + ", arg2 = " + arg2);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Compares two problem locations and decides if they contain the same information. Returns {@code true} if they
     * have the same information, {@code false} otherwise. <br>
     * <br>
     * Quick fix calculator {@link SpeculationCalculator} caches the problem locations that were calculated in the last
     * successful calculation so that it does not recalculate the quick fix results every time there is a change in the
     * project.
     * 
     * @param location1 First location to be compared.
     * @param location2 Second location to be compared.
     * @return {@code true} if the locations contain the same information, false otherwise.
     */
    public static boolean sameProblemLocationContentWeak(IProblemLocation location1, IProblemLocation location2)
    {
        // System.out.println("location1.id = " + location1.getProblemId() + ", location2.id = " +
        // location2.getProblemId());
        // System.out.println("location1.length = " + location1.getLength() + ", location2.length = " +
        // location2.getLength());
        // System.out.println("location1.markerType = " + location1.getMarkerType() + ", location2.markerType = " +
        // location2.getMarkerType());
        // System.out.println("location1.offset = " + location1.getOffset() + ", location2.offset = " +
        // location2.getOffset());
        // String [] a1 = location1.getProblemArguments();
        // String [] a2 = location2.getProblemArguments();
        // System.out.println("location1.arguments:");
        // for (String arg: a1)
        // System.out.println(arg);
        // System.out.println("location2.arguments:");
        // for (String arg: a2)
        // System.out.println(arg);
        if (location1.getProblemId() != location2.getProblemId())
            return false;
        if (location1.getLength() != location2.getLength())
            return false;
        String [] args1 = location1.getProblemArguments();
        String [] args2 = location2.getProblemArguments();
        if (args1.length != args2.length)
            return false;
        boolean result = true;
        /*
         * I cannot use this because e.g., for WrongClassName.java, the problem argument depends on the project that
         * generates it. So the argument for the original project and the copy project does not match.
         */
        //@formatter:off
        //        for (int a = 0; a < args1.length; a++)
        //        {
        //            if (!args1[a].equals(args2[a]))
        //            {
        //                result = false;
        //                logger.finest("Locations differ for problem arguments:");
        //                logger.finest(args1[a]);
        //                logger.finest(args2[a]);
        //            }
        //        }
        //@formatter:on
        return result;
    }

    /**
     * This method applies the change, and saves the underlying resource (buffer) if the change has any modified
     * elements.
     * 
     * @param change {@link Change} to be applied.
     * @return The undo provided by the change.
     * @throws CoreException If there is a problem during change performing.
     */
    public static Change performChangeAndSave(Change change) throws CoreException
    {
        /*
         * This is the workaround for opening the file in an editor and closing it :-)) Works for (almost) all of the
         * changes I am interested in for now.
         */
        // XXX Workaround!
        if (change instanceof TextFileChange)
        {
            TextFileChange textFileChange = (TextFileChange) change;
            textFileChange.setSaveMode(TextFileChange.FORCE_SAVE);
        }
        IProgressMonitor dummy = new NullProgressMonitor();
        Change result = null;
        try
        {
            change.initializeValidationData(dummy);
            if (!change.isEnabled())
            {
                logger.severe("Change = " + change.getName() + " is not enabled.");
                return result;
            }
            RefactoringStatus valid = change.isValid(new SubProgressMonitor(dummy, 1));
            if (valid.hasFatalError())
            {
                logger.severe("Change = " + change.getName() + " is not valid.");
                return result;
            }
            result = change.perform(new SubProgressMonitor(dummy, 1));
            Object modifiedObject = change.getModifiedElement();
            if (modifiedObject instanceof IOpenable)
            {
                IOpenable openable = (IOpenable) modifiedObject;
                saveFileBuffer(openable);
            }
        }
        finally
        {
            change.dispose();
        }
        return result;
    }

    /**
     * Needed by {@link #openAndSwitchBackInUIThread(IFile)}.
     */
    private static IEditorPart editor = null;

    /**
     * Opens the given file in a new eclipse editor, switches back to the current editor and returns the new editor. <br>
     * <br>
     * In Eclipse API for {@link Change}s to be applied correctly, the file that the change will effect must be opened
     * and at least selected once in the Eclipse editor.
     * 
     * @param file File to be opened.
     * @return The editor created (or already existed) for the file.
     */
    @SuppressWarnings("unused")
    private static IEditorPart openAndSwitchBackInUIThread(final IFile file)
    {
        editor = null;
        Display.getDefault().syncExec(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    editor = openAndSwitchBack(file);
                }
                catch (NotInitializedException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        return editor;
    }

    /**
     * Closes the eclipse editor (also the file associated with it).
     * 
     * @param editor Editor to be closed.
     */
    @SuppressWarnings("unused")
    private static void closeEditorInUIThread(final IEditorPart editor)
    {
        Display.getDefault().syncExec(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    closeEditor(editor);
                }
                catch (NotInitializedException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }

    /**********************
     * END OF PUBLIC API *
     *********************/
    /***************
     * PRIVATE API *
     **************/
    private static IWorkbenchWindow getWindow() throws NotInitializedException
    {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null)
            throw new NotInitializedException("Workbench window is not initialized. Cannot open an editor!");
        return window;
    }

    private static IEditorPart openAndSwitchBack(IFile file) throws NotInitializedException
    {
        IWorkbenchWindow window = getWindow();
        IWorkbenchPage page = window.getActivePage();
        IEditorPart currentEditor = page.getActiveEditor();
        IEditorPart result = openEditor(file);
        page.activate(currentEditor);
        return result;
    }

    private static void saveFileBuffer(IOpenable openable)
    {
        IBuffer buffer = null;
        try
        {
            buffer = openable.getBuffer();
        }
        catch (JavaModelException e)
        {
            logger.log(Level.SEVERE, "Cannot get buffer for openable = " + openable, e);
        }
        // if (buffer != null)
        if (buffer != null && buffer.hasUnsavedChanges())
        {
            try
            {
                buffer.save(null, true);
            }
            catch (JavaModelException e)
            {
                logger.log(Level.SEVERE, "Cannot save buffer for openable = " + openable, e);
            }
        }
    }

    private static void closeEditor(IEditorPart editor) throws NotInitializedException
    {
        IWorkbenchWindow window = getWindow();
        // We don't care saving...
        window.getActivePage().closeEditor(editor, false);
    }

    private static IEditorPart openEditor(IFile file) throws NotInitializedException
    {
        IJavaElement javaElement = JavaCore.create(file);
        IEditorPart result = null;
        try
        {
            result = JavaUI.openInEditor(javaElement);
        }
        catch (PartInitException e1)
        {
            logger.log(Level.SEVERE, "Cannot initialize editor.", e1);
        }
        catch (JavaModelException e1)
        {
            logger.log(Level.SEVERE, "Cannot open file = " + file.getProjectRelativePath() + " in an editor.", e1);
        }
        return result;
    }
    /**********************
     * END OF PRIVATE API *
     *********************/
}
