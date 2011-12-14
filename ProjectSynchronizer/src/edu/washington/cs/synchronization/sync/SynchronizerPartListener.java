package edu.washington.cs.synchronization.sync;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;

import edu.washington.cs.swing.KDialog;
import edu.washington.cs.synchronization.ProjectSynchronizer;
import edu.washington.cs.synchronization.sync.internal.ActiveFileChangedListener;
import edu.washington.cs.synchronization.sync.internal.ActiveFileChangedNotifier;
import edu.washington.cs.util.eclipse.EclipseUIUtility;
import edu.washington.cs.util.eclipse.ResourceUtility;
import edu.washington.cs.util.exception.NotInitializedException;

/**
 * Synchronizer part listener is the implementation of an {@link IPartListener2} that reports the files that are
 * selected in the Eclipse editor to the related synchronizers so that projects will be in sync with the minimal effort
 * required. <br>
 * <br>
 * Synchronization part listener also acts as an {@link ActiveFileChangedNotifier}. Whenever the active project is
 * changed, it notifies all the listeners with this information.
 * 
 * @author Kivanc Muslu
 * 
 * @deprecated Use {@link SynchronizerCursorListener} instead.
 */
public class SynchronizerPartListener implements IPartListener2, ActiveFileChangedNotifier
{
    /** singleton instance. */
    private static SynchronizerPartListener instance_;
    /** logger for debugging. */
    private static final Logger logger = Logger.getLogger(SynchronizerPartListener.class.getName());
    static
    {
        logger.setLevel(Level.FINE);
    }
    /** listener list. */
    private final ArrayList <ActiveFileChangedListener> listeners_ = new ArrayList <ActiveFileChangedListener>();

    // singleton?
    /**
     * Creates a synchronization part listener and assigns the {@link #instance_}.
     */
    public SynchronizerPartListener()
    {
        synchronized (SynchronizerPartListener.class)
        {
            instance_ = this;
        }
    }

    /**
     * Returns the current part listener.
     * <p>
     * This method is synchronized over {@link SynchronizerPartListener}.
     * </p>
     * 
     * @return The current part listener.
     */
    public static synchronized SynchronizerPartListener getInstance()
    {
        return instance_;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Retrieves the selected file, and signals an active file changed according to the file selected.
     * </p>
     */
    @Override
    public void partActivated(IWorkbenchPartReference partRef)
    {
//        IEditorPart editor = getEditorPart(partRef);
//        if (editor != null && editor instanceof ITextEditor)
//        {
//            ITextEditor textEditor = (ITextEditor) editor;
//            updateCursorListener(textEditor.getSelectionProvider());
//        }
//        else
//            System.out.println("Unknown editor type = " + editor.getClass());
        
        // activation is the last thing to happen in a part listener. However even during the activation I cannot close the current file.
        debugPartActivity("Part activated", partRef);
        final IFile file = getCurrentFile(partRef);
        if (file != null)
        {
            signalActiveProjectChange(file);
            Thread closer = new Thread()
            {
                private volatile boolean completed_ = false;
                private volatile boolean shadow_ = false;
                
                public void run()
                {
                    Display.getDefault().syncExec(new Thread()
                    {
                        public void run()
                        {
                            IProject project = file.getProject();
                            if (project.getName().startsWith(ProjectSynchronizer.SHADOW_PREFIX))
                            {
                                shadow_ = true;
                                try
                                {
                                    EclipseUIUtility.closeActiveEditor();
                                    // Strip off DO_NOT_DELETE_
                                    String longName = project.getName().split(ProjectSynchronizer.SHADOW_PREFIX)[1];
                                    String [] parts = longName.split("_");
                                    String originalName = "";
                                    assert parts.length >= 3;
                                    if (parts.length >= 3)
                                    {
                                        // We don't care the first prefix (Observation or Speculation) and the last one (hash code)
                                        for (int a = 1; a < parts.length-1; a++)
                                            originalName += parts[a] + "_";
                                        originalName = originalName.substring(0, originalName.length()-1);
//                                        int hashCode = Integer.parseInt(parts[parts.length-1]);
//                                        System.out.println(hashCode);
//                                        assert originalName.hashCode() == hashCode;
//                                        if (originalName.hashCode() == hashCode)
                                        IProject original = ResourceUtility.getProject(originalName);
                                        IFile originalFile = original.getFile(file.getProjectRelativePath());
                                        EclipseUIUtility.openFileInEditor(originalFile);
                                        completed_ = true;
                                            
                                    }
                                }
                                catch (NotInitializedException e)
                                {
                                    assert false: "At this point the page must be initialized.";
                                }
                                catch (PartInitException e)
                                {
                                    logger.log(Level.SEVERE, "Cannot jump back to the original file.", e);
                                }
                            }
                        }
                    });
                    if (shadow_)
                    {
                        String text = "Quick Fix Scout plug-in detected that you have switched to one of the shadow files (projects)<br>";
                        if (completed_)
                            text += "The shadow file is closed and returned back to the original file.";
                        else
                            text += "The shadow file is closed.";
                        KDialog.showInformation(null, text, "Shadow File Detected", 400);
                    }
                }
            };
            closer.start();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Retrieves the selected file, and signals an active file changed according to the file selected.
     * </p>
     */
    @Override
    public void partInputChanged(IWorkbenchPartReference partRef)
    {
//        IEditorPart editor = getEditorPart(partRef);
//        if (editor != null && editor instanceof ITextEditor)
//        {
//            ITextEditor textEditor = (ITextEditor) editor;
//            updateCursorListener(textEditor.getSelectionProvider());
//        }
        
        debugPartActivity("Part input changed", partRef);
        IFile file = getCurrentFile(partRef);
        if (file != null)
            signalActiveProjectChange(file);
    }

    /**
     * {@inheritDoc} <br>
     * <br>
     * This method makes sure that there is nothing that is not saved at the moment the user closed the part. <br>
     * If there is anything to be saved, but it is not saved (i.e., discarded) then the shadow files are synced again
     * with the current version of the file.
     */
    @Override
    public void partClosed(IWorkbenchPartReference partRef)
    {
        debugPartActivity("Part closed", partRef);
        /*
         * TODO I am not sure if this is too much work for UI thread. If that is the case, I might need to move this to
         * a background thread (after the selected file is retrieved).
         */
        IFile file = getCurrentFile(partRef);
        if (file != null)
        {
            // FIXME This might be a redundant check when I have buffer listeners for all opened
            // files.
            // Make sure that the selected file has a buffer.
            if (ResourceUtility.isJavaLike(file) && ResourceUtility.isOnClassPath(file))
            {
                ICompilationUnit compilationUnit = JavaCore.createCompilationUnitFrom(file);
                try
                {
                    IBuffer buffer = compilationUnit.getBuffer();
                    if (buffer.hasUnsavedChanges())
                    {
                        IProject project = file.getProject();
                        ProjectSynchronizer [] synchronizers = ProjectSynchronizer.getSynchronizers(project);
                        for (ProjectSynchronizer synchronizer: synchronizers)
                        {
                            IProject shadow = synchronizer.getShadowProject();
                            IFile shadowFile = shadow.getFile(file.getProjectRelativePath());
                            /*
                             * Here we don't want a buffer level sync, since the extra unsaved content in
                             */
                            try
                            {
                                shadowFile.setContents(file.getContents(), true, false, null);
                            }
                            catch (CoreException e)
                            {
                                // This should not fail!
                                logger.log(Level.SEVERE,
                                        "Cannot change the contents of file = " + shadow.getProjectRelativePath(), e);
                            }
                        }
                    }
                }
                catch (JavaModelException e)
                {
                    // This should not fail!
                    logger.log(Level.SEVERE, "Cannot get the buffer for file = " + file.getProjectRelativePath(), e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void addActiveFileChangedListener(ActiveFileChangedListener listener)
    {
        listeners_.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void signalActiveProjectChange(IFile file)
    {
        for (ActiveFileChangedListener listener: listeners_)
            listener.activeProjectChanged(file);
    }

    /**
     * {@inheritDoc} <br>
     * <br>
     * Default implementation does nothing.
     */
    @Override
    public void partHidden(IWorkbenchPartReference partRef)
    {
        debugPartActivity("Part hidden", partRef);
    }

    /**
     * {@inheritDoc} <br>
     * <br>
     * Default implementation does nothing.
     */
    @Override
    public void partVisible(IWorkbenchPartReference partRef)
    {
        debugPartActivity("Part visible", partRef);
    }

    /**
     * {@inheritDoc} <br>
     * <br>
     * Default implementation does nothing.
     */
    @Override
    public void partDeactivated(IWorkbenchPartReference partRef)
    {
        debugPartActivity("Part deactivated", partRef);
    }

    /**
     * {@inheritDoc} <br>
     * <br>
     * Default implementation does nothing.
     */
    @Override
    public void partBroughtToTop(IWorkbenchPartReference partRef)
    {
        debugPartActivity("Part brought to top", partRef);
    }

    /**
     * {@inheritDoc} <br>
     * <br>
     * Default implementation does nothing.
     */
    @Override
    public void partOpened(IWorkbenchPartReference partRef)
    {
        debugPartActivity("Part opened", partRef);
    }

    /**
     * Given a part reference that is instance of {@link IEditorPart}, returns the file that lies inside the given part.
     * 
     * @param partRef The reference to the part that holds the file inside it.
     * @return The file that lies inside the given part.
     */
    private IFile getCurrentFile(IWorkbenchPartReference partRef)
    {
        IWorkbenchPart part = partRef.getPart(false);
        IFile result = null;
        if (part instanceof IEditorPart)
        {
            IEditorPart editorPart = (IEditorPart) part;
            String path = "/" + editorPart.getTitleToolTip();
            result = ResourceUtility.getFile(new Path(path));
        }
        return result;
    }
    
    private void debugPartActivity(String explanation, IWorkbenchPartReference partRef)
    {
        IFile file = getCurrentFile(partRef);
        if (file != null)
            logger.fine(explanation + ": " + file.getProjectRelativePath() + " in " + file.getProject().getName());
    }
}
