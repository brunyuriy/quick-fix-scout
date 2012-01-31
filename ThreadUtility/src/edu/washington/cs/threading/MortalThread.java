package edu.washington.cs.threading;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mortal Thread is an abstract implementation of a thread that can be killed, or will a particular work in its each
 * cycle of work. <br>
 * The default implementation for mortal thread is as follows:
 * 
 * <pre>
 * while (alive)
 * {
 *     sleep();
 *     preDoWork();
 *     doWork();
 *     signalSynchronization();
 * }
 * </pre>
 * 
 * preDoWork method is provided for the concrete classes that need to implement extra internal handling before actually
 * starting doing the work. <br>
 * After each cycle of work is done it is assumed that a synchronization point is reached and this is signaled.
 * 
 * @author Kivanc Muslu
 */
public abstract class MortalThread extends Thread
{
    public static final String PLUG_IN_ID = "edu.washington.cs.threading";
    /**
     * boolean that indicates if 'this' is working or not, used for synchronization.
     */
    private volatile boolean working_;
    /**
     * boolean that indicates if 'this' is alive or not.
     */
    private volatile boolean alive_;
    /** The amount of time that 'this' will be sleeping at the start of each cycle of work. */
    private final long breakTime_;
    /** internal object used for other threads who want to wait for synchronization. */
    private final Object synchronizationQueue_;
    /** Name of the thread, used for debugging. */
    private final String name_;
    /** Logger for debugging. */
    private static final Logger logger = Logger.getLogger(MortalThread.class.getName());
    static
    {
        // FINE => debug waits on synchronization queue.
        logger.setLevel(Level.INFO);
    }

    /**
     * Creates a mortal thread that will sleep the given amount of time at the start of each cycle of work.
     * 
     * @param breakTime The amount of time that 'this' will be sleeping at the start of each cycle of work.
     * @param name Name of the thread, used for debugging.
     */
    public MortalThread(long breakTime, String name)
    {
        working_ = false;
        alive_ = true;
        name_ = name;
        breakTime_ = breakTime;
        synchronizationQueue_ = new Object();
    }

    /**
     * Creates a mortal thread that won't be sleeping at all at the start of each cycle of work.
     * @param name Name of the thread, used for debugging.
     */
    public MortalThread(String name)
    {
        this(0, name);
    }

    /**
     * {@inheritDoc}
     */
    public void run()
    {
        while (!isDead())
        {
            try
            {
                Thread.sleep(breakTime_);
                preDoWork();
                working_ = true;
                doWork();
            }
            catch (Exception e)
            {
                logger.log(Level.SEVERE, "Due to some exception thread = \"" + name_ + "\" couldn't complete its current work.", e);
            }
            finally
            {
                working_ = false;
                signalSynchronization();
            }
        }
        logger.info("Thread \"" + name_ + "\" completed its life cycle.");
    }

    /**************
     * PUBLIC API *
     *************/
    /**
     * Convenient method to kill this thread and waits until it is completely dead (i.e., finish its last cycle of
     * work). <br>
     * This method is supposed to be called when the current worker is no longer necessary or the program is closed.
     */
    public void killAndJoin()
    {
        kill();
        try
        {
            join();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Waits in the synchronization queue of this thread until 'this' does a cycle of its work. <br>
     * If 'this' is already waiting of not in the middle of a work, then this method returns immediately, otherwise this
     * is a blocking. <br>
     * If 'this' is blocked, then to prevent a possible deadlock, this method first unblocks it, waits until the next
     * synchronization point is reached and blocks 'this' again at that moment. <br>
     * This method is synchronized over {@link #synchronizationQueue_}.
     */
    public void waitUntilSynchronization()
    {
        preWaitUntilSynchronization();
        while (!isSynched())
        {
            synchronized (synchronizationQueue_)
            {
                try
                {
                    logger.fine("Waiting for synchronization...");
                    synchronizationQueue_.wait();
                    logger.fine("Woken up...");
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
        postWaitUntilSynchronization();
    }

    /*****************
     * PROTECTED API *
     ****************/
    /**
     * Signals that the current cycle of work is done and a synchronization point is reached. <br>
     * This method is provided as 'protected' so that the concrete threads that implement {@link BlockableMortalThread}
     * can use it, it should not be used from outside.
     * <p>
     * This method is synchronized over {@link #synchronizationQueue_}.
     * </p>
     */
    protected void signalSynchronization()
    {
        logger.fine("Signalling synchronization...");
        synchronized (synchronizationQueue_)
        {
            synchronizationQueue_.notifyAll();
        }
    }

    /**
     * This method is provided for the concrete classes so that they can do their extra work before actually starting
     * their current cycle. <br>
     * Extra work is generally done for specialized workers, restoring the state, waiting on an extra condition etc. <br>
     * The default implementation on calls {@link #signalSynchronization()}.
     * 
     * @throws InterruptedException If 'this' is interrupted during this method.
     */
    protected abstract void preDoWork() throws InterruptedException;

    /**
     * This method represents a cycle of work for 'this'. This is where the actual work is done.
     * 
     * @throws InterruptedException If 'this' is interrupted during work.
     */
    protected abstract void doWork() throws InterruptedException;

    /**
     * This method is provided for the concrete classes to implement extra handling before the caller thread of
     * {@link #waitUntilSynchronization()} starts its current waiting. <br>
     * The default implementation does nothing.
     */
    protected void preWaitUntilSynchronization()
    {}

    /**
     * This method is provided for the concrete classes to implement extra handling after the caller thread of
     * {@link #waitUntilSynchronization()} starts its current waiting. <br>
     * The default implementation does nothing.
     */
    protected void postWaitUntilSynchronization()
    {}

    /***************************
     * GETTER & SETTER METHODS *
     ***************************/
    /**
     * Returns <code>true</code> if 'this' has reached a synchronization point, <code>false</code> otherwise. <br>
     * This method provides the default implementation, which should be overridden as required. <br>
     * The default implementation returns <code>true</code> if 'this' is not working.
     * 
     * @return <code>true</code> if 'this' has reached a synchronization point, <code>false</code> otherwise.
     */
    protected boolean isSynched()
    {
        return !working_;
    }

    /**
     * <p>
     * Returns <code>true</code> if this is dead, <code>false</code> otherwise.
     * </p>
     * This method should only be called by 'this' and sub-classes.
     * 
     * @return <code>true</code> if this is dead, <code>false</code> otherwise.
     */
    protected boolean isDead()
    {
        return !alive_;
    }

    /**
     * Kills 'this'.
     */
    protected void kill()
    {
        alive_ = false;
    }

    /**
     * Indicates that the thread stopped working.
     */
    protected void stopWorking()
    {
        working_ = false;
    }
}
