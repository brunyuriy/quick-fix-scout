package edu.washington.cs.synchronization.sync;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.BufferChangedEvent;
import org.eclipse.jdt.core.IBufferChangedListener;

import edu.washington.cs.synchronization.ProjectSynchronizer;
import edu.washington.cs.synchronization.sync.task.internal.BufferChangedTask;
import edu.washington.cs.synchronization.sync.task.internal.Task;
import edu.washington.cs.synchronization.sync.task.internal.TaskWorker;

/**
 * An implementation of {@link IBufferChangedListener} to sync two files at buffer level.
 * 
 * @author Kivanc Muslu
 */
public class SynchronizerBufferChangedListener implements IBufferChangedListener
{
    /** logger for debugging. */
    private static final Logger logger = Logger.getLogger(SynchronizerBufferChangedListener.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }

    /**
     * Creates a buffer listener that will synchronize a file in the given synchronizer's project.
     */
    public SynchronizerBufferChangedListener()
    {
    }

    /**
     * {@inheritDoc} <br>
     * <br>
     * This implementation first checks if the current event is a buffer close event. If <code>true</code>, it returns. <br>
     * If not, it logs the event and adds the event to the task worker.
     * 
     * @see TaskWorker#addTask(Task)
     */
    @Override
    public void bufferChanged(BufferChangedEvent event)
    {
        // We handle this case with part listener, and generates a lot of non necessary tasks.
        if (isBufferClosedTask(event))
            return;
        logBufferChangedEvent(event);
        IProject original = event.getBuffer().getUnderlyingResource().getProject();
        for (ProjectSynchronizer synchronizer: ProjectSynchronizer.getSynchronizers(original))
        {
            Task bufferChangedTask = new BufferChangedTask(event, synchronizer);
            synchronizer.getTaskWorker().addTask(bufferChangedTask);
        }
    }

    /**
     * Returns <code>true</code> if the given event is a buffer close event, <code>false</code> otherwise.
     * 
     * @param event The event that is analyzed.
     * @return <code>true</code> if the given event is a buffer close event, <code>false</code> otherwise.
     */
    private boolean isBufferClosedTask(BufferChangedEvent event)
    {
        return event.getText() == null && event.getOffset() == 0 && event.getLength() == 0;
    }

    /**
     * Logs the given event in a detailed form.
     * 
     * @param event The event that will be logged.
     */
    private void logBufferChangedEvent(BufferChangedEvent event)
    {
        logger.fine("event.resource = " + event.getBuffer().getUnderlyingResource().getProjectRelativePath());
        String text = event.getText();
        if (text != null && !text.trim().equals(""))
            logger.fine("event.text = " + event.getText());
    }
}