package edu.washington.cs.quickfix.observation.log;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;

import edu.washington.cs.quickfix.observation.Observer;
import edu.washington.cs.quickfix.observation.log.internal.QFSession;
import edu.washington.cs.synchronization.sync.task.internal.TaskWorker;
import edu.washington.cs.util.eclipse.BuilderUtility;

public class ObservationCompilationErrorLogger extends Thread
{
    private final TaskWorker worker_;
    private boolean notInitialized_;
    
    private static final Logger logger = Logger.getLogger(ObservationCompilationErrorLogger.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }

    public static enum Type
    {
        AFTER, BEFORE
    };

    private final Type type_;
    private final QFSession session_;
    
    private int noCompilationErrors_;

    public ObservationCompilationErrorLogger(QFSession session, Type type)
    {
        session_ = session;
        notInitialized_ = false;
        type_ = type;
        worker_ = Observer.getUsageObserver().getCurrentTaskWorker();
        noCompilationErrors_ = -1;
        if (worker_ == null)
            notInitialized_ = true;
    }
    
    public int getNoCompilationErrors()
    {
        return noCompilationErrors_;
    }

    public void run()
    {
        if (notInitialized_)
            return;
        // Check is the session is still valid (i.e., if we are observing the current project or not).
        if (session_.isInvalid())
            return;
        worker_.block();
        worker_.waitUntilSynchronization();
        IProject shadow = Observer.getUsageObserver().getCurrentSynchronizer().getShadowProject();
        boolean isAutoBuilding = BuilderUtility.isAutoBuilding();
        BuilderUtility.setAutoBuilding(false);
        BuilderUtility.build(shadow);
        noCompilationErrors_ = BuilderUtility.getNumberOfCompilationErrors(shadow);
        if (isAutoBuilding)
            BuilderUtility.setAutoBuilding(true);
        if (type_ == Type.BEFORE)
        {
            logger.info("Communication: Setting the number of errors before a proposal has selected.");
            session_.logNumberOfErrorsBefore(noCompilationErrors_);
        }
        else if (type_ == Type.AFTER)
        {
            logger.info("Communication: Setting the number of errors after a proposal has selected.");
            session_.logNumberOfErrorsAfter(noCompilationErrors_);
        }
        worker_.unblock();
    }
}
