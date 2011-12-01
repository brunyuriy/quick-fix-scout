package edu.washington.cs.synchronization.sync.task.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.washington.cs.synchronization.sync.internal.ProjectModificationListener;
import edu.washington.cs.synchronization.sync.internal.ProjectModificationNotifier;
import edu.washington.cs.threading.BlockableMortalThread;

/**
 * A blockable mortal thread implementation for handling a queue of tasks. <br>
 * Task worker will try to do all the tasks in its queue in every cycle of its life as long as it is not blocked. <br>
 * After completing a cycle, it will wait until new tasks are added to its queue, in which it is awaken and start its
 * new cycle.
 * <p>
 * Task worker is also a project modifier notifier. <br>
 * It will notify its listeners whenever a change will be applied to the project by the user.
 * </p>
 * 
 * @author Kivanc Muslu
 * @see Task
 */
public class TaskWorker extends BlockableMortalThread implements ProjectModificationNotifier
{
    /** logger for debugging. */
    private static final Logger logger = Logger.getLogger(TaskWorker.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }
    /** The list of tasks. */
    private final LinkedList <Task> tasks_;
    /** A separate set of tasks used for efficient lookup. */
    private final HashSet <Task> tasksCopy_;
    /** List of tasks of secondary importance. */
    private final HashSet <SaveTask> saveTasks_;
    private final ReentrantLock saveTaskLock_;
    private final ReentrantLock taskLock_;
    private final Condition taskCondition_;
    /** boolean variable that indicates if the typing session should be bypassed or not. */
    private volatile boolean bypassTypingSessionCheck_ = false;
    /** a list of listeners. */
    private final ArrayList <ProjectModificationListener> listenerList_;
    /** lock object for list of listeners. */
    private final ReentrantLock listenerListLock_;
    /** last modification date (i.e., the moment when the last task is added. */
    private volatile Date lastModificationDate_ = new Date();
    
    public static volatile boolean blockAddingTasks = false;

    // private static final long BREAK_TIME = 500;
    /**
     * Creates a task worker that will sleep the given time between its cycle of work.
     * 
     * @param breakTime The time that 'this' will sleep between each cycle of work.
     */
    public TaskWorker(long breakTime)
    {
        super(breakTime, "Project Synchronizer Task Worker");
        tasks_ = new LinkedList <Task>();
        saveTasks_ = new HashSet <SaveTask>();
        tasksCopy_ = new HashSet <Task>();
        listenerList_ = new ArrayList <ProjectModificationListener>();
        listenerListLock_ = new ReentrantLock();
        saveTaskLock_ = new ReentrantLock();
        taskLock_ = new ReentrantLock();
        taskCondition_ = taskLock_.newCondition();
    }

    /**
     * Creates a task worker that won't sleep between its cycle of work.
     */
    public TaskWorker()
    {
        this(0);
    }
    
    public synchronized Date getLastModificationDate()
    {
        Date result = lastModificationDate_;
        lastModificationDate_ = new Date();
        return result;
    }
    
    @Override
    protected void postWaitUntilSynchronization()
    {
        super.postWaitUntilSynchronization();
        doSaveTasks();
    }
    
    private void addSaveTask(SaveTask task)
    {
        if (task == null)
            return;
        
        saveTaskLock_.lock();
        saveTasks_.add(task);
        saveTaskLock_.unlock();
    }
    
    private void doSaveTasks()
    {
        taskLock_.lock();
        saveTaskLock_.lock();
        for (Task task: tasks_)
        {
            SaveTask save = task.doTask();
            if (save != null)
                saveTasks_.add(save);
        }
        tasks_.clear();
        taskLock_.unlock();
        logger.fine("Saving files..");
        for (SaveTask task: saveTasks_)
            task.doTask();
        saveTasks_.clear();
        logger.fine("Saved files..");
        saveTaskLock_.unlock();
    }

    /**
     * Adds a new task to the worklist. <br>
     * The task is added iff it does not exist in one of the worklists already. <br>
     * The task is added to {@link #tasks_} if it has {@link Task#LOWEST_PRIORITY}, or added to {@link #tasksCopy_}
     * otherwise. <br>
     * This method also notifies 'this' since a new task is added to the worklist.
     * <p>
     * This method is synchronized over 'this'.
     * </p>
     * 
     * @param task Task to be added to the worklist.
     * @see #preDoWork()
     */
    public void addTask(Task task)
    {
        addTask(task, false);
    }
    
    private void addTask(Task task, boolean internal)
    {
        assert task != null;
        taskLock_.lock();
        if (!internal)
            lastModificationDate_ = new Date();
        boolean newTask = tasksCopy_.add(task);
        if (newTask)
        {
            logger.fine("Adding new task = " + task);
            tasks_.add(task);
//            if (super.isSynched())
//            {
                signalProjectIsAboutToBeModified();
                taskCondition_.signalAll();
//            }
        }
        else
            logger.fine("Ignored duplicate task: " + task);
        taskLock_.unlock();
    }
    
    /**
     * Returns the next task that needs to be done from the worklist. <br>
     * This method tries to return the top task from {@link #tasks_} list first and then tries the
     * {@link #saveTasks_} list. <br>
     * If both lists are empty, this method returns <code>null</code> indicating that there is no more task to process.
     * <p>
     * This method is synchronized over 'this'.
     * </p>
     * 
     * @return The next task that needs to be done from the worklist, <code>null</code> if there is no such task.
     */
    public Task removeTopTask()
    {
        Task result = null;
        taskLock_.lock();
        if (!tasks_.isEmpty())
            result = tasks_.remove();
//        else if (!secondaryTasks_.isEmpty())
//            result = secondaryTasks_.remove();
        // If we got a task, remove it from the duplicate set.
        if (result != null)
            tasksCopy_.remove(result);
        taskLock_.unlock();
        return result;
    }

    /**
     * Returns <code>true</code> if the worklist is empty, <code>false</code> otherwise.
     * <p>
     * This method is synchronized over 'this'.
     * </p>
     * 
     * @return <code>true</code> if the worklist is empty, <code>false</code> otherwise.
     */
    public boolean isEmpty()
    {
        return getTaskSize() == 0;
    }

    /**
     * This method tells the notifier daemon to bypass typing session check since a change done by the user is
     * guaranteed to effect the number of compilation errors in the project. <br>
     * Called indirectly by Eclipse.
     */
    public void bypassTypingSessionCheck()
    {
        bypassTypingSessionCheck_ = true;
    }

    /**
     * Returns <code>true</code> if current daemon should bypass typing session (shouldn't wait at all) before signaling
     * a possible project modification, <code>false</code> otherwise. <br>
     * Calling this method also clears the {@link #bypassTypingSessionCheck_} value.
     * 
     * @return <code>true</code> if current daemon should bypass typing session, <code>false</code> otherwise.
     */
    private boolean shouldBypassTypingSessionCheck()
    {
        boolean result = bypassTypingSessionCheck_;
        bypassTypingSessionCheck_ = false;
        return result;
    }

    /**
     * Returns the number of tasks present in the worklist.
     * <p>
     * This method is synchronized over 'this'.
     * </p>
     * 
     * @return The number of tasks present in the worklist.
     */
    private int getTaskSize()
    {
        taskLock_.lock();
        int result = tasks_.size(); //+ secondaryTasks_.size();
        taskLock_.unlock();
        return result;
    }

    /****************************************
     * BlockableMortalThread IMPLEMENTATION *
     ***************************************/
    /**
     * {@inheritDoc}
     * <p>
     * This implementation only checks the blocked condition if the worklist is empty. <br>
     * This means that the task worker will work until all the current tasks are completed even if it is blocked in the
     * middle. <br>
     * This method blocks even if 'this' is not blocked since there is no reason to attempt for a task worker to try
     * doing a task when there is none. <br>
     * If there is no task, it waits on 'this'. <br>
     * When I new task is added to the worklist, 'this' is notified.
     * </p>
     * 
     * @see #addTask(Task)
     */
    @Override
    protected void preDoWork() throws InterruptedException
    {
        if (isDead())
            return;
        waitUntilTaskCame();
    }
    
    private void waitUntilTaskCame() throws InterruptedException
    {
        while (!isDead() && isEmpty())
        {
            super.preDoWork();
            taskLock_.lock();
            taskCondition_.await();
            taskLock_.unlock();
        }
    }
    
    /** reference to the previous daemon. */
    private TaskWorkerNotifierDaemon oldDaemon_ = null;

    /**
     * {@inheritDoc}
     * <p>
     * A cycle of work for a task worker is defined as:
     * <ol>
     * <li>If there is an old notifier daemon, invalidate it.</li>
     * <li>Do all the tasks in the worklist until it is empty.</li>
     * <li>Create a new notifier daemon, which will mimic typing session (i.e., signal a project modification if there
     * is no other task comes).</li>
     * </ol>
     * </p>
     * 
     * @see TaskWorkerNotifierDaemon
     */
    @Override
    protected void doWork() throws InterruptedException
    {
        if (isDead())
            return;
        if (oldDaemon_ != null)
            oldDaemon_.skipIt();
        Task current;
        while ((current = removeTopTask()) != null)
        {
            SaveTask result = current.doTask();
            addSaveTask(result);
        }
        TaskWorkerNotifierDaemon daemon = new TaskWorkerNotifierDaemon(2000);
        daemon.start();
        oldDaemon_ = daemon;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 'this' is also notified as a part of task worker implementation in this method.
     * </p>
     */
    @Override
    public void killAndJoin()
    {
        kill();
        logger.finer("Killed the worker.");
        unblock();
        logger.finer("Unblocked worker.");
        taskLock_.lock();
        taskCondition_.signalAll();
        taskLock_.unlock();
        logger.finer("Notified the worker.");
        try
        {
            join();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        logger.finer("Joined the worker.");
    }

    /**********************************************
     * ProjectModificationNotifier IMPLEMENTATION *
     *********************************************/
    /**
     * {@inheritDoc}
     * <p>
     * This method is synchronized over {@link #listenerListLock_}.
     * </p>
     */
    @Override
    public void addProjectChangeListener(ProjectModificationListener listener)
    {
        listenerListLock_.lock();
        listenerList_.add(listener);
        listenerListLock_.unlock();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is synchronized over {@link #listenerListLock_}.
     * </p>
     */
    @Override
    public void signalProjectModification()
    {
        listenerListLock_.lock();
        for (ProjectModificationListener listener: listenerList_)
            listener.projectModified();
        listenerListLock_.unlock();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is synchronized over {@link #listenerListLock_}.
     * </p>
     */
    @Override
    public void signalProjectIsAboutToBeModified()
    {
        listenerListLock_.lock();
        for (ProjectModificationListener listener: listenerList_)
            listener.projectIsAboutToBeModified();
        listenerListLock_.unlock();
    }

    /**
     * This is a daemon thread implementation that is used by {@link TaskWorker}. <br>
     * Since task worker cannot wait until the current typing session ends, we need another thread running in parallel
     * that will check this property. <br>
     * The whole purpose of this implementation is to signal different signals about project modification.
     * 
     * @author Kivanc Muslu
     */
    private class TaskWorkerNotifierDaemon extends Thread
    {
        /** variable that represents whether the project modification should be signaled or not. */
        private volatile boolean shouldNotify_;
        private final int waitTime_;

        /**
         * Creates a task worker daemon.
         */
        public TaskWorkerNotifierDaemon(int waitTime)
        {
            shouldNotify_ = true;
            waitTime_ = waitTime;
        }

        /**
         * Tells the daemon to skip project notification.
         */
        private void skipIt()
        {
            shouldNotify_ = false;
        }

        /**
         * Returns <code>true</code> if 'this' should signal a project modification, <code>false</code> otherwise. <br>
         * 
         * @return <code>true</code> if 'this' should signal a project modification, <code>false</code> otherwise.
         */
        private boolean shouldNotify()
        {
            return shouldNotify_;
        }

        /**
         * {@inheritDoc}
         * 
         * @see #runInternal()
         */
        @Override
        public void run()
        {
            try
            {
                runInternal();
            }
            catch (Exception e)
            {
                logger.log(Level.SEVERE, "Calculator notifier thread threw an exception.", e);
            }
        }

        /**
         * Represents the actual work done by the daemon. <br>
         * It first sleeps for the amount of typing session length, checks the preconditions to signal a modification,
         * and signals the project modification if the preconditions are met.
         * 
         * @throws InterruptedException If 'this' is interrupted during its sleep.
         */
        private void runInternal() throws InterruptedException
        {
            boolean bypassCheck = shouldBypassTypingSessionCheck();
            if (!bypassCheck)
                Thread.sleep(waitTime_/* Control.getControl().getTypingSessionLength() */);
            if (bypassCheck || shouldNotify())
                signalProjectModification();
        }
    }

    public void clear()
    {
        taskLock_.lock();
        tasks_.clear();
        taskLock_.unlock();
    }
}
