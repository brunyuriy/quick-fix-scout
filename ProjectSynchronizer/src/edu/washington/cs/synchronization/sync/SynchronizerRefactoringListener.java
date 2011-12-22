package edu.washington.cs.synchronization.sync;

import org.eclipse.ltk.core.refactoring.history.IRefactoringExecutionListener;
import org.eclipse.ltk.core.refactoring.history.RefactoringExecutionEvent;

import edu.washington.cs.synchronization.sync.task.internal.TaskWorker;

public class SynchronizerRefactoringListener implements IRefactoringExecutionListener
{
    @Override
    public void executionNotification(RefactoringExecutionEvent event)
    {
        switch(event.getEventType())
        {
            case RefactoringExecutionEvent.ABOUT_TO_PERFORM:
                System.out.println("About to perform = " + event);
                System.out.println(event.getDescriptor());
                System.out.println(event.getDescriptor().getProject());
                TaskWorker.blockAddingTasks = true;
                break;
            case RefactoringExecutionEvent.PERFORMED:
                TaskWorker.blockAddingTasks = false;
                break;
        }
    }
}
