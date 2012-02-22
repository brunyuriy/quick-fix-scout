package edu.washington.cs.quickfix.observation.log;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.commands.operations.IOperationHistoryListener;
//import org.eclipse.core.commands.operations.IUndoContext;
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
//        if (event.getEventType() == OperationHistoryEvent.UNDONE)
//        {
//            IUndoableOperation op = event.getOperation();
//            while (op instanceof TriggeredOperations)
//            {
//                System.out.println("Operation is a triggered operation.");
//                op = ((TriggeredOperations) op).getTriggeringOperation();
//            }
//            System.out.println("Label = " + op.getLabel());
//            System.out.println(op.getClass());
//            System.out.println("Op = " + op);
//            if (op instanceof UndoableOperation2ChangeAdapter)
//            {
//                System.out.println("Operation is undoable operation 2 change adapter.");
//            }
//        }

        IUndoableOperation op = event.getOperation();
        if (op instanceof TriggeredOperations)
            op = ((TriggeredOperations) op).getTriggeringOperation();
        UndoableOperation2ChangeAdapter changeOperation = null;
        if (op instanceof UndoableOperation2ChangeAdapter)
            changeOperation = (UndoableOperation2ChangeAdapter) op;
        if (changeOperation == null)
            return;
        else
            System.out.println("Event has the following change operation = " + changeOperation);
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
                logger.info("Change executed = " + changeName);
                ObservationLogger.getLogger().logChangePerformed(changeName);
                break;
            case OperationHistoryEvent.UNDONE:
                logger.info("Undo executed = " + changeName);
                ObservationLogger.getLogger().logUndo(changeName);
                break;
            case OperationHistoryEvent.REDONE:
                break;
            case OperationHistoryEvent.OPERATION_NOT_OK:
                break;
            case OperationHistoryEvent.OPERATION_ADDED:
                break;
            case OperationHistoryEvent.OPERATION_REMOVED:
                break;
        }
    }
}
