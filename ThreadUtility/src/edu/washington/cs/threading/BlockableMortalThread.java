package edu.washington.cs.threading;

/**
 * Blockable mortal thread is an abstract implementation of a mortal thread that can be blocked and unblocked on a
 * condition during its life time. The life cycle of a blockable thread is as following:
 * 
 * <pre>
 * while (alive)
 * {
 *     sleep();
 *     while (blocked)
 *         blockCondition.wait();
 *     preDoWork();
 *     doWork();
 *     signalSycnronization();
 * }
 * </pre> {@link #block()} and {@link #unblock()} methods can be used to control the blocking of this thread. <br>
 * Note that the thread will not be blocked immediately, if it is working, it will complete its current cycle, so if
 * some other thread wants to be sure that the current cycle of this thread is complete it should call
 * {@link #waitUntilSynchronization()}. This method blocks the caller thread, if 'this' is working and the caller thread
 * will be automatically notified before 'this' blocks itself
 * 
 * @author Kivanc Muslu
 * @see MortalThread
 */
public abstract class BlockableMortalThread extends MortalThread
{
    /** internal object used for block condition. */
    private final Object blockCondition_ = new Object();
    /** boolean that indicates if 'this' is blocked or not. */
    private boolean blocked_;

    /**
     * Creates a blockable mortal thread.
     * 
     * @param breakTime The amount of time that 'this' should be sleeping between each cycle of work.
     * @param name Name of the thread, used for debugging.
     * @see MortalThread#MortalThread(long)
     */
    public BlockableMortalThread(long breakTime, String name)
    {
        super(breakTime, name);
        blocked_ = false;
    }

    /**
     * Creates a blockable mortal thread with no waiting time between the cycles of work.
     * @param name Name of the thread, used for debugging.
     */
    public BlockableMortalThread(String name)
    {
        this(0, name);
    }

    /**************
     * PUBLIC API *
     *************/
    /**
     * {@inheritDoc} <br>
     * <br>
     * 'this' is unblocked (as an extra) in this method.
     */
    public void killAndJoin()
    {
        kill();
        unblock();
        try
        {
            join();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    /*****************
     * PROTECTED API *
     ****************/
    /**
     * If {@link #blocked_} is set to <code>true</code>, waits on {@link #blockCondition_} until notified. <br>
     * This method is called at the beginning of a new cycle of work.
     */
    @Override
    protected void preDoWork() throws InterruptedException
    {
        while (!isDead() && isBlocked())
        {
            synchronized (blockCondition_)
            {
//                System.err.println("Blocking itself...");
                blockCondition_.wait();
            }
        }
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * The synchronization point for a task worker is reached whenever it is not working and its task list is empty.
     */
    // TODO Why do I need isBlocked() to be true to get in sync?
    @Override
    protected boolean isSynched()
    {
        boolean result = isBlocked() && super.isSynched();
        return result;
    }

    /***************************
     * GETTER & SETTER METHODS *
     ***************************/
    /**
     * Blocks 'this'. Calls {@link #setBlock(boolean)} with <code>true</code>.
     */
    public void block()
    {
        setBlock(true);
    }

    /**
     * Unblocks 'this'. Calls {@link #setBlock(boolean)} with <code>false</code>.
     */
    public void unblock()
    {
        setBlock(false);
    }

    /**
     * Returns true if 'this' is blocked, false otherwise. This method is synchronized over {@link #blockCondition_}
     * 
     * @return true if 'this' is blocked, fasle otherwise.
     */
    public boolean isBlocked()
    {
        synchronized (blockCondition_)
        {
            return blocked_;
        }
    }

    /**
     * Utility method to set the value of {@link #blocked_}. <br>
     * If the thread is unblocked, it also wakes up the thread. <br>
     * This method is synchronized over {@link #blockCondition_}.
     * 
     * @param value boolean value that will be set for {@link #blocked_}
     */
    private void setBlock(boolean value)
    {
        synchronized (blockCondition_)
        {
            blocked_ = value;
            if (!value)
                blockCondition_.notifyAll();
        }
    }
}
