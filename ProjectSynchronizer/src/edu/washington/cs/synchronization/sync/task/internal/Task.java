package edu.washington.cs.synchronization.sync.task.internal;

/**
 * Abstract task implementation for worker-task pattern. <br>
 * A concrete task class should implement the {@link #doTask()} method to represent a single piece of work.
 * 
 * @author Kivanc Muslu
 * @see TaskWorker
 */
public interface Task
{
    /**
     * Represents a single piece of work that needs to be completed by a {@link TaskWorker}.
     * 
     * @return A new {@link Task} if completing 'this' creates a new task, <code>null</code> otherwise.
     */
    SaveTask doTask();
}
