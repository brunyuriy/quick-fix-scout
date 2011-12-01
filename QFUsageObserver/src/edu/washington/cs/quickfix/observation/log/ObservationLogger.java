package edu.washington.cs.quickfix.observation.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import edu.washington.cs.quickfix.bridge.BridgeActionManager;
import edu.washington.cs.quickfix.bridge.ISpeculatorObserverBridge;
import edu.washington.cs.quickfix.observation.gui.ObservationPreferencePage;
import edu.washington.cs.quickfix.observation.log.internal.QFSession;
import edu.washington.cs.util.eclipse.SharedConstants;

public class ObservationLogger
{
    public static final String LOG_DIRECTORY_PATH = SharedConstants.DEBUG_LOG_DIR;
    public static final String LOG_PATH;
    static
    {
        LOG_PATH = LOG_DIRECTORY_PATH + "qf_usage_" + SharedConstants.UNIQUE_TIME_STAMP + ".txt";
    }
    private static final ObservationLogger instance_ = new ObservationLogger();
    private static final Logger logger = Logger.getLogger(ObservationLogger.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
        logger.fine("Logging quick fix usage information to file = " + LOG_PATH);
    }
    private Formatter logWriter_;
    private QFSession currentSession_ = null; // new QFUsageSession();
    private QFSession previousSession_ = null;

    // singleton
    private ObservationLogger()
    {}

    public void startLogging()
    {
        try
        {
            File file = new File(LOG_PATH);
            file.getParentFile().mkdirs();
            logWriter_ = new Formatter(new File(LOG_PATH));
        }
        catch (FileNotFoundException e)
        {
            logger.log(Level.SEVERE, "Cannot open a file to log usage statistics!", e);
        }
    }

    public synchronized static ObservationLogger getLogger()
    {
        return instance_;
    }

    public void log(QFSession session)
    {
        String log = session.toString();
        log(log);
    }

    private void log(String log)
    {
        logger.finer("Logging session:\n" + log);
        logWriter_.format("%s%n", log);
        logWriter_.flush();
    }

    public synchronized void logUndo(String undo)
    {
        if (!ObservationPreferencePage.getInstance().isActivated())
            return;
        String ls = System.getProperty("line.separator");
        log(QFSession.UNDO_STRING + undo + ls);
    }

    public synchronized void closeLogWriter()
    {
        logWriter_.close();
    }

    public synchronized void logPopupCreated()
    {
        if (currentSession_ == null)
            currentSession_ = new QFSession();
        logger.info("Communication: QF popup created.");
        currentSession_.logPopupCreated();
    }

    public synchronized void logChangePerformed(String name)
    {
        if (previousSession_ != null)
            previousSession_.logChangePerformed(name);
        else
            logger.fine("Change perform is detected but previous sessions is null!");
    }

    public synchronized void logPopupClosed()
    {
        if (currentSession_ != null)
        {
            logger.info("Communication: Popup close message received.");
            ISpeculatorObserverBridge bridge = BridgeActionManager.getInstance().getSpeculatorObserverBridge();
            if (bridge != null)
            {
                IProblemLocation [] locations = currentSession_.getLocations();
                if (locations != null)
                {
                    String [] calculatedProposals = bridge.getCalculatedProposals(locations);
                    Date analysisCompletionTime = bridge.getAnalysisCompletionTime();
                    Date localSpeculationCompletionTime = bridge.getLocalSpeculationCompletionTime();
                    currentSession_.setSpeculationProposals(calculatedProposals);
                    currentSession_.setAnalysisCompletionTime(analysisCompletionTime);
                    currentSession_.setLocalSpeculationCompletionTime(localSpeculationCompletionTime);
                }
            }
//            Observer.getUsageObserver().getCurrentTaskWorker().unblock();
            currentSession_.logPopupClosed();
            previousSession_ = currentSession_;
            currentSession_ = null;
        }
        else
            logger.fine("Popup close detected but current session is null!");
    }

    public synchronized QFSession logProposalSelected(ICompletionProposal proposal)
    {
        if (currentSession_.isInvalid())
        {
            logger.info("Communication: Session is invalidated, selected proposal is ignored.");
            return null;
        }
        else
        {
            logger.info("Communication: Selected proposal is set.");
            currentSession_.logProposalSelected(proposal);
            return currentSession_;
        }
    }

    public synchronized void invalidateCurrentSession()
    {
        if (currentSession_ != null)
            currentSession_.invalidate();
    }

    public synchronized QFSession getCurrentSession()
    {
        if (currentSession_ == null)
            currentSession_ = new QFSession();
        return currentSession_;
    }
}
