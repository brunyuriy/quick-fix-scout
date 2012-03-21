package edu.washington.cs.synchronization.sync.task.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

/**
 * A task implementation that represents a change in a document (i.e., file buffer).
 * 
 * @author Kivanc Muslu
 */
public class DocumentChangeTask implements Task
{
    /** Logger for debugging. */
    private static final Logger logger = Logger.getLogger(DocumentChangeTask.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }
    /** File that the change will be applied to. */
    private final IFile file_;
    /** The event that represents the change. */
    private final DocumentEvent event_;

    /**
     * Creates a document change task using the file as the document that will be changed and event to represent the
     * change.
     * 
     * @param file File that will be changed (to sustain the synchronization).
     * @param event Event that represents the change done to the file.
     */
    public DocumentChangeTask(IFile file, DocumentEvent event)
    {
        file_ = file;
        event_ = event;
    }

    /**
     * {@inheritDoc}
     * <p>
     * A document change task is completed when the change represented by the event is applied to the file given. <br>
     * A {@link DocumentSaveTask} that would save the same file is returned upon success. Returns <code>null</code> if
     * something goes wrong.
     * </p>
     * 
     * @return A document save task that will save the currently modified document if the modification is done with
     *         success, <code>null</code> otherwise.
     */
    @Override
    public SaveTask doTask()
    {
        try
        {
            final IDocumentProvider provider = new TextFileDocumentProvider();
            provider.connect(file_);
            final IDocument document = provider.getDocument(file_);
            if (document != null)
            {
                final int offset = event_.getOffset();
                final int length = event_.getLength();
                final String text = event_.getText();
                /*
                 * This must be done with Eclipse UI Thread. Because for some reason that I don't quite know, if the file
                 * that will be replaced (the shadow file) is open in the background, then a change fire causes one of the
                 * Eclipse widgets to call checkWidget(), which throws an InvalidThreadAccessException if the caller thread
                 * of document.replace(...) is not the Eclipse UI thread. If the file that is supposed to change is not open
                 * in the Eclipse editor, normally calling this with a normal thread does not throw an Exception. Also the
                 * thrown exception, doesn't cause any failure but it is just ugly.
                 */
                Display.getDefault().syncExec(new Thread()
                {
                    public void run()
                    {
                        try
                        {
                            document.replace(offset, length, text);
                        }
                        catch (BadLocationException e)
                        {
                            e.printStackTrace();
                        }
                    }
                });
                return new DocumentSaveTask(provider, file_);
            }
        }
        catch (CoreException e1)
        {
            logger.log(Level.SEVERE, "Cannot apply the document change to file = " + file_.getProjectRelativePath()
                    + " due to an exception." + e1);
        }
        return null;
    }

    @Override
    public String toString()
    {
        return "[DocumentChangeTask: text = " + event_.getText() + ", offset = " + event_.getOffset() + ", length = "
                + event_.getLength() + ", file = " + file_.getProjectRelativePath().toString() + " in " + file_.getProject().getName() + "]";
    }
}
