package edu.washington.cs.synchronization.sync.task.internal;

import java.nio.charset.UnmappableCharacterException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * A task implementation that represents a file should be saved (so that the underlying buffer will be in sync with the
 * file system).
 * 
 * @author Kivanc Muslu
 */
public class DocumentSaveTask implements SaveTask
{
    /** Logger for debugging. */
    private static final Logger logger = Logger.getLogger(DocumentSaveTask.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }
    /** Document provider that the document is connected to. */
    private final IDocumentProvider provider_;
    /** File that will be saved. */
    private final IFile file_;

    /**
     * Creates a document save task with the given file, document and document provider.
     * 
     * @param provider Document provider that the document is connected to. This connection is created when this task is
     *            created (i.e., in {@link DocumentChangeTask}).
     * @param document Document that the file is connected to.
     * @param file File that will be saved.
     */
    public DocumentSaveTask(IDocumentProvider provider, IFile file)
    {
        provider_ = provider;
        file_ = file;
    }

    /**
     * {@inheritDoc}
     * <p>
     * A document save task is completed when the file that is connected to that document is saved. <br>
     * Upon success, this method returns <code>null</code>
     * </p>
     * 
     * @return <code>null</code> upon success.
     */
    @Override
    public void doTask()
    {
        try
        {
            if (provider_.canSaveDocument(file_))
                provider_.saveDocument(null, file_, provider_.getDocument(file_), false);
            provider_.disconnect(file_);
        }
        catch (CoreException e)
        {
            boolean knownReasons = false;
            Throwable throwable = e.getCause();
            /*
             * This is a known exception that is raised when the document that we are trying to save has non unicode
             * characters (i.e., characters that cannot be represented with the file's current character encoding) that
             * are just added to the document. Normally, if the user tried the same thing, Eclipse would also raise an
             * error telling him that he cannot do that. In this case the change is already applied to the document, we
             * just don't save it (since user cannot also, and he will eventually delete the characters that are entered
             * wrongly). The document will be saved probably with the next change.
             */
            if (throwable instanceof UnmappableCharacterException)
                knownReasons = true;
            if (!knownReasons)
                logger.log(Level.SEVERE,
                        "Cannot save the document represented for the file = " + file_.getProjectRelativePath()
                                + " due to an exception.", e);
        }
    }

    public String toString()
    {
        return "[DocumentSaveTask: file = " + file_.getProjectRelativePath().toString() + " in "
                + file_.getProject().getName() + "]";
    }

    @Override
    public int hashCode()
    {
        return file_.getProjectRelativePath().toString().hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof DocumentSaveTask)
            return equals((DocumentSaveTask) obj);
        return false;
    }

    public boolean equals(DocumentSaveTask other)
    {
        return file_.getProjectRelativePath().toString().equals(other.file_.getProjectRelativePath().toString());
    }
}
