package edu.washington.cs.quickfix.observation.log;

import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.IUndoManagerListener;

/**
 * @author Kivanc Muslu
 * @ deprecated Not used anymore.
 * @see ObservationOperationHistoryListener
 */
public class ObservationUndoManagerListener implements IUndoManagerListener
{
    private static final Logger logger = Logger.getLogger(ObservationUndoManagerListener.class.getName());
    static
    {
        logger.setLevel(Level.FINE);
    }
    private final Stack <String> undoStrings_;
    private boolean possibleUndoExecuted_;

    public ObservationUndoManagerListener()
    {
        undoStrings_ = new Stack <String>();
        possibleUndoExecuted_ = false;
    }

    @Override
    public void undoStackChanged(IUndoManager manager)
    {
        String undoName = manager.peekUndoName();
        logger.fine("undoStackChanged = " + undoName);
        if (undoStrings_.isEmpty())
        {
            if (undoName != null)
            {
                undoStrings_.push(undoName);
                logger.info("Detected new change = " + undoName);
            }
        }
        else
        {
            if (undoName == null)
            {
                // problem
            }
            else if (!undoStrings_.peek().equals(undoName))
            {
                undoStrings_.push(undoName);
                logger.info("Detected new change = " + undoName);
            }
        }
    }

    @Override
    public void redoStackChanged(IUndoManager manager)
    {
        System.out.println("Redo stack changed!");
    }

    @Override
    public void aboutToPerformChange(IUndoManager manager, Change change)
    {
        String undoName = manager.peekUndoName();
        logger.fine("aboutToPerformChange = " + undoName);
        possibleUndoExecuted_ = !undoStrings_.isEmpty() && undoStrings_.peek().equals(undoName);
    }

    @Override
    public void changePerformed(IUndoManager manager, Change change)
    {
        boolean undoChange = false;
        String undoName = manager.peekUndoName();
        System.out.println("Change performed = " + change.getName());
        logger.fine("changePerformed = " + undoName);
        if (possibleUndoExecuted_)
        {
            String undoCandidate = undoStrings_.pop();
            possibleUndoExecuted_ = (undoStrings_.isEmpty() && undoName == null)
                    || (!undoStrings_.isEmpty() && undoStrings_.peek().equals(undoName));
            if (possibleUndoExecuted_)
            {
                undoChange = true;
                logger.info("User undid proposal = " + undoCandidate);
                ObservationLogger.getLogger().logUndo(undoCandidate);
            }
            else
                undoStrings_.push(undoCandidate);
        }
        if (!undoChange)
        {
            logger.info("Detected a normal change (proposal application) = " + undoName);
            // TODO match this name with the proposal that is applied.
            ObservationLogger.getLogger().logChangePerformed(undoName);
        }
    }
}
