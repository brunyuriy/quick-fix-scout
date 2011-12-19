package edu.washington.cs.quickfix.observation.log;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.commands.operations.TriggeredOperations;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.internal.core.refactoring.UndoableOperation2ChangeAdapter;

@SuppressWarnings("restriction")
public class ObservationOperationHistoryListener implements IOperationHistoryListener
{
    private static final Logger logger = Logger.getLogger(ObservationOperationHistoryListener.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }

    public void historyNotification(OperationHistoryEvent event)
    {
        IUndoableOperation op = event.getOperation();
        if (op instanceof TriggeredOperations)
            op = ((TriggeredOperations) op).getTriggeringOperation();
        UndoableOperation2ChangeAdapter changeOperation = null;
        if (op instanceof UndoableOperation2ChangeAdapter)
            changeOperation = (UndoableOperation2ChangeAdapter) op;
        if (changeOperation == null)
            return;
        Change change = changeOperation.getChange();
        if (change == null)
            return;
        String changeName = change.getName();
        switch (event.getEventType())
        {
            case OperationHistoryEvent.ABOUT_TO_EXECUTE:
                break;
            case OperationHistoryEvent.ABOUT_TO_UNDO:
                break;
            case OperationHistoryEvent.ABOUT_TO_REDO:
                break;
            case OperationHistoryEvent.DONE:
                logger.fine("Change executed = " + changeName);
                ObservationLogger.getLogger().logChangePerformed(changeName);
                break;
            case OperationHistoryEvent.UNDONE:
                logger.fine("Undo executed = " + changeName);
                ObservationLogger.getLogger().logUndo(changeName);
                break;
            case OperationHistoryEvent.REDONE:
                // System.out.println("Redo executed = " + changeName);
                break;
            case OperationHistoryEvent.OPERATION_NOT_OK:
                break;
            case OperationHistoryEvent.OPERATION_ADDED:
                // System.out.println("Operation added...");
                break;
            case OperationHistoryEvent.OPERATION_REMOVED:
                // System.out.println("Operation removed...");
                break;
        }
    }
}
