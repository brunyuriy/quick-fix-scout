package edu.washington.cs.synchronization.sync.task.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.BufferChangedEvent;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import edu.washington.cs.synchronization.ProjectSynchronizer;
import edu.washington.cs.synchronization.sync.SynchronizerBufferChangedListener;

/**
 * An implementation of a buffer changed task. <br>
 * Represents that one of the buffers that are being listened has an underlying change.
 * 
 * @author Kivanc Muslu
 * @see SynchronizerBufferChangedListener
 */
public class BufferChangedTask implements Task
{
    /** logger for debugging. */
    private final static Logger logger = Logger.getLogger(BufferChangedTask.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }
    /** synchronizer that will manage the buffer change in the shadow buffer. */
    private final ProjectSynchronizer synchronizer_;
    /** The event that represents the change. */
    private final BufferChangedEvent event_;

    /**
     * Creates a buffer changed task.
     * 
     * @param event Event that represents the change in the buffer.
     * @param synchronizer Synchronizer that will manage the same change in the shadow buffer.
     */
    public BufferChangedTask(BufferChangedEvent event, ProjectSynchronizer synchronizer)
    {
        event_ = event;
        synchronizer_ = synchronizer;
    }

    /**
     * {@inheritDoc}
     * <p>
     * A buffer changed task is completed when the same change applied to the listened buffer is also applied to the
     * shadow buffer, which will be retrieved by the {@link #synchronizer_}. <br>
     * Returns a buffer save task for the same buffer if the change is applied with success, <code>null</code>
     * otherwise.
     * </p>
     * 
     * @return a buffer save task for the same buffer if the change is applied with success, <code>null</code>
     *         otherwise.
     */
    @Override
    public SaveTask doTask()
    {
        SaveTask result = null;
        IProject shadowProject = synchronizer_.getShadowProject();
        IFile shadowFile = shadowProject.getFile(getProjectRelativePath());
        if (!shadowFile.exists())
            return result;
        ICompilationUnit shadowUnit = JavaCore.createCompilationUnitFrom(shadowFile);
        try
        {
            IBuffer shadowBuffer = shadowUnit.getBuffer();
            String text = event_.getText();
            int offset = event_.getOffset();
            int length = event_.getLength();
            if (text == null)
                shadowBuffer.replace(offset, length, "");
            else
                shadowBuffer.replace(offset, length, text);
            logger.finer("Applied buffer changed event to file = " + shadowBuffer.getUnderlyingResource().toString());
            result = new BufferSaveTask(shadowBuffer);
        }
        catch (JavaModelException e)
        {
            logger.log(Level.SEVERE, "Cannot get buffer for file = " + shadowFile.toString(), e);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[BufferChangedTask: text = " + event_.getText() + ", offset = " + event_.getOffset()
                + ", length = " + event_.getLength());
        return buffer.toString();
    }

    private IPath getProjectRelativePath()
    {
        return event_.getBuffer().getUnderlyingResource().getProjectRelativePath();
    }
}
