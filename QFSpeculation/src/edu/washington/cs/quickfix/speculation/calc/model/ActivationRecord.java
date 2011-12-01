package edu.washington.cs.quickfix.speculation.calc.model;

public class ActivationRecord
{
    private boolean active_;
    private boolean valid_;
    
    public ActivationRecord()
    {
        this(false, true);
    }
    
    public ActivationRecord(boolean active, boolean valid)
    {
        active_ = active;
        valid_ = valid;
    }
    
    public synchronized void activate()
    {
        active_ = true;
        notifyAll();
    }
    
    public synchronized void invalidate()
    {
        valid_ = false;
    }
    
    public synchronized void deactivate()
    {
        if (valid_)
            active_ = false;
    }
    
    public synchronized boolean isActive()
    {
        return active_;
    }
    
    public synchronized boolean isValid()
    {
        return valid_;
    }
    
    public boolean isInvalid()
    {
        return !isValid();
    }
    
    public synchronized void waitUntilActivated()
    {
        while (!active_)
        {
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
            }
        }
    }


}
