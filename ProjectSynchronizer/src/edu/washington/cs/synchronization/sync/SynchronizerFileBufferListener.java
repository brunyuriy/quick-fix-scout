package edu.washington.cs.synchronization.sync;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.filebuffers.IFileBuffer;
import org.eclipse.core.filebuffers.IFileBufferListener;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IBufferChangedListener;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import edu.washington.cs.synchronization.ProjectSynchronizer;
import edu.washington.cs.util.eclipse.ResourceUtility;

/**
 * As an implementation of {@link IFileBufferListener}, this class keeps track of the changes done to all file buffers
 * in the workspace. <br>
 * Provides help for synchronization at buffer level.
 * 
 * @author Kivanc Muslu
 */
public class SynchronizerFileBufferListener implements IFileBufferListener
{

    private static final Logger logger = Logger.getLogger(SynchronizerFileBufferListener.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }

    public static void attachListenerToBuffer(IFileBuffer buffer)
    {
        IPath location = buffer.getLocation();
        IFile file = ResourceUtility.getFile(location);
        if (buffer instanceof ITextFileBuffer)
        {
            IProject project = file.getProject();
            ITextFileBuffer textBuffer = (ITextFileBuffer) buffer;
            if (!ProjectSynchronizer.isShadowProject(project))
                textBuffer.getDocument().addDocumentListener(new SynchronizerDocumentListener(buffer));
        }
    }

    /**
     * Installs a new buffer listener to the given file. <br>
     * If the buffer of the file cannot be retrieved (i.e., does not exists), no listener is attached.
     * 
     * @param file File to install the buffer listener.
     */
    @SuppressWarnings("unused")
    private static boolean attachBufferListenerToFile(IFile file)
    {
        // Check is the file is java-like. If not, it cannot generate a compilation unit, and has no
        // buffer.
        if (!ResourceUtility.isJavaLike(file) || !ResourceUtility.isOnClassPath(file))
            return false;
        // Cannot fail.
        ICompilationUnit unit = JavaCore.createCompilationUnitFrom(file);
        try
        {
            IBufferChangedListener bufferListener = new SynchronizerBufferChangedListener();
            unit.getBuffer().addBufferChangedListener(bufferListener);
            logger.finer("Added buffer changed listener for compilation unit = " + file.getName());
        }
        catch (JavaModelException e)
        {
            logger.log(Level.SEVERE, "For compilation unit = " + unit.getResource().getFullPath()
                    + " cannot get the buffer or add the listener...", e);
            return false;
        }
        return true;
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * If the file that the buffer is created for is not in a shadow project (i.e., is in one of the user's project), a
     * new {@link SynchronizerDocumentListener} is created and added to that file to keep track of the changes done from
     * this moment on.
     * </p>
     */
    @Override
    public void bufferCreated(IFileBuffer buffer)
    {
        attachListenerToBuffer(buffer);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Default implementation is empty.
     * </p>
     */
    @Override
    public void bufferDisposed(IFileBuffer buffer)
    {}

    /**
     * {@inheritDoc}
     * <p>
     * Default implementation is empty.
     * </p>
     */
    @Override
    public void bufferContentAboutToBeReplaced(IFileBuffer buffer)
    {}

    /**
     * {@inheritDoc}
     * <p>
     * Default implementation is empty.
     * </p>
     */
    @Override
    public void bufferContentReplaced(IFileBuffer buffer)
    {}

    /**
     * {@inheritDoc}
     * <p>
     * Default implementation is empty.
     * </p>
     */
    @Override
    public void stateChanging(IFileBuffer buffer)
    {}

    /**
     * {@inheritDoc}
     * <p>
     * Default implementation is empty.
     * </p>
     */
    @Override
    public void dirtyStateChanged(IFileBuffer buffer, boolean isDirty)
    {}

    /**
     * {@inheritDoc}
     * <p>
     * Default implementation is empty.
     * </p>
     */
    @Override
    public void stateValidationChanged(IFileBuffer buffer, boolean isStateValidated)
    {}

    /**
     * {@inheritDoc}
     * <p>
     * Default implementation is empty.
     * </p>
     */
    @Override
    public void underlyingFileMoved(IFileBuffer buffer, IPath path)
    {}

    /**
     * {@inheritDoc}
     * <p>
     * Default implementation is empty.
     * </p>
     */
    @Override
    public void underlyingFileDeleted(IFileBuffer buffer)
    {}

    /**
     * {@inheritDoc}
     * <p>
     * Default implementation is empty.
     * </p>
     */
    @Override
    public void stateChangeFailed(IFileBuffer buffer)
    {}
}
