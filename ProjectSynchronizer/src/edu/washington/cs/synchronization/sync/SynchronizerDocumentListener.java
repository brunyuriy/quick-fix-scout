package edu.washington.cs.synchronization.sync;

import org.eclipse.core.filebuffers.IFileBuffer;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;

import edu.washington.cs.synchronization.ProjectSynchronizer;
import edu.washington.cs.synchronization.sync.task.internal.DocumentChangeTask;
import edu.washington.cs.synchronization.sync.task.internal.DocumentSaveTask;
import edu.washington.cs.synchronization.sync.task.internal.Task;
import edu.washington.cs.util.eclipse.ResourceUtility;

/**
 * Synchronizer document listener provides methods to keep track of the changes done to a document at buffer level so
 * that the same changes can also be applied to copy files. <br>
 * Provides synchronization at buffer level.
 * 
 * @author Kivanc Muslu
 */
public class SynchronizerDocumentListener implements IDocumentListener
{
    /** File that would represent the document. */
    private final IFile file_;
    /** Active synchronizers that are interested in changes done to this document. */
    private ProjectSynchronizer [] synchronizers_ = null;

    /**
     * Creates a document listener that keeps track of the changes that is done to the given file buffer.
     * 
     * @param buffer The buffer that the changes will be tracked.
     */
    public SynchronizerDocumentListener(IFileBuffer buffer)
    {
        file_ = ResourceUtility.getFile(buffer.getLocation());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Default implementation is empty.
     * </p>
     */
    @Override
    public void documentAboutToBeChanged(DocumentEvent event)
    {}

    /**
     * {@inheritDoc}
     * <p>
     * For the synchronizers that are interested in the changes that are done to this document, creates a
     * {@link DocumentSaveTask} and adds this task to their worklist.
     * </p>
     */
    @Override
    public void documentChanged(final DocumentEvent event)
    {
        Thread worker = new Thread()
        {
            public void run()
            {
                /*
                 * We have check for this every time since it seems that IDocumentListeners are notified before IPartListener2.
                 * This means that the moment we created this object, it might be the case that no synchronizer is created to
                 * watch this document. However, when the user makes a change (this method is called), a synchronizer that is
                 * interested in this document might have been created.
                 */
                if (synchronizers_ == null)
                    synchronizers_ = ProjectSynchronizer.getSynchronizers(file_.getProject());
                for (ProjectSynchronizer synchronizer: synchronizers_)
                {
                    IFile shadowFile = synchronizer.getShadowProject().getFile(file_.getProjectRelativePath());
                    Task documentChangeTask = new DocumentChangeTask(shadowFile, event);
                    synchronizer.getTaskWorker().addTask(documentChangeTask);
                }
            }
        };
        worker.start();
    }
}
