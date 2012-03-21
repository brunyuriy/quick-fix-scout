package edu.washington.cs.quickfix.observation.log.internal;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeCorrectionProposal;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import com.kivancmuslu.www.time.Dates;

import edu.washington.cs.quickfix.bridge.BridgeActionManager;
import edu.washington.cs.quickfix.bridge.ISpeculatorObserverBridge;
import edu.washington.cs.quickfix.observation.Observer;
import edu.washington.cs.quickfix.observation.log.ObservationCompilationErrorLogger;
import edu.washington.cs.quickfix.observation.log.ObservationLogger;
import edu.washington.cs.quickfix.observation.log.ObservationCompilationErrorLogger.Type;
import edu.washington.cs.synchronization.sync.task.internal.TaskWorker;
import edu.washington.cs.util.eclipse.QuickFixUtility;
import edu.washington.cs.util.log.CommonLoggers;

/**
 * Data structure abstraction that represents a quick fix session.
 * 
 * @author Kivanc Muslu
 */
@SuppressWarnings("restriction")
public class QFSession
{
    /** Constant that represents the prefix used for the session type of a Quick Fix session. */
    static final String SESSION_TYPE_STRING = "Logging Quick Fix session of type = ";
    /** Constant that represents whether the session is marked as invalid (getting out of sync) or not. */
    static final String SESSION_VALIDITY_STRING = "Session is valid: ";
    /** Constant that represents the prefix used for the start of a quick fix session. */
    static final String SESSION_START_STRING = "Quick Fix session started. Current time = ";
    /** Constant that represents the prefix used for the delay before a quick fix session. */
    static final String SESSION_DELAY_STRING = "Delay before the current session = ";
    static final String SESSION_DELAY_STRING_SEPERATOR = " => ";
    static final String LOCAL_COMPUTATION_DELAY_STRING = "Delay before the local speculation completed = ";
    static final String GLOBAL_COMPUTATION_DELAY_STRING = "Delay before the analysis completion = ";
    static final String SPECULATION_RUNNING_STRING = "Speculative analysis is running: ";
    /** Constant that represents the prefix used for the proposals that are offered by Eclipse. */
    static final String ECLIPSE_PROPOSALS_STRING = "The following proposals are offered by the Eclipse:";
    /** Constant that represents the string that will be used in split(...) method to get the proposal information. */
    static final String PROPOSAL_SPLIT_STRING = "-) ";
    /** Constant that represents the prefix used for the proposals that are offered by the speculative analysis. */
    static final String SPECULATION_PROPOSALS_STRING = "The following proposals are offered by the speculative analysis:";
    /**
     * Constant that represents the prefix used for the number of compilation errors in the project at the time quick
     * fix icon is clicked.
     */
    static final String BEFORE_COMPILATION_ERROR_STRING = "Number of compilation errors before the proposal selected = ";
    /** Constant that represents the prefix used for the proposal that user has selected. */
    static final String USER_SELECTED_STRING = "User selected proposal = ";
    /**
     * Constant that represents the prefix used for the number of compilation errors in the project at the time after
     * the proposal that is selected is applied to the project.
     */
    static final String AFTER_COMPILATION_ERROR_STRING = "Number of compilation errors after the proposal selected = ";
    /** Constant that represents the prefix used when the user closes the quick fix popup without selecting a proposal. */
    static final String TERMINATION_WITHOUT_SELECTION_STRING = "User closed popup window without selecting a proposal.";
    /** Constant that represents the prefix used for the end of a quick fix session. */
    static final String SESSION_END_STRING = "Quick Fix session is completed. Current time = ";
    /** Constant that represents the prefix used for the length of a quick fix session. */
    static final String SESSION_LENGTH_STRING = "Quick Fix session lasted for = ";
    /** Constant that represents the prefix used for the proposal that user undid. */
    public static final String UNDO_STRING = "User undid proposal = ";
    /** Constant that represents invalid time. */
    private static final Date INVALID_TIME = new Date();
    /** Constant that represents invalid errors. */
    static final int INVALID_ERRORS = -1;
    /** Logger for debugging. */
    private static final Logger logger = Logger.getLogger(QFSession.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }
    /** Complete log that represents this quick fix session. */
    private StringBuffer log_;
    /** Time between the last edit to the project and the quick fix dialog is created. */
    private Date delayTime_;
    /** Time when the session started as in {@link System#nanoTime()}. */
    private Date sessionStartTime_;
    /** Time when the session ended as in {@link System#nanoTime()}. */
    private Date sessionEndTime_;
    /** The proposals that are offered by Eclipse for this session. */
    private String [] availableProposals_;
    /** The proposals that are offered by the speculative analysis at the time the quick fix popup is generated. */
    private String [] speculationProposals_;
    /** Number of compilation errors in the project at the time the quick fix popup is created. */
    private int errorsBefore_;
    /** Number of compilation errors in the project after the proposal selected by user is applied. */
    private int errorsAfter_;
    /** String representation of the proposal that is selected by the user. */
    private String selectedProposalString_;
    /** Actual proposal that is selected by the user. */
    private ICompletionProposal selectedProposal_;
    /** Variable that indicates the validity of this session. */
    private boolean invalid_;
    /** Variable that indicates if the log that represents this session is constructed or not. */
    private boolean logConstructed_;
    // Locations are needed so that later on I can retrieve speculation proposals.
    private IProblemLocation [] locations_;
    
    private Boolean isSpeculationRunning_;
    private Date localSpeculationCompletionTime_;
    private Date analysisCompletionTime_;
    
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public static enum QFSessionType { HOVER, DIALOG }
    private QFSessionType sessionType_;
    
    private boolean markedInvalid_ = false;
    
    private boolean undone_ = false;
    
    public QFSession()
    {
        this(null, INVALID_TIME, INVALID_TIME, null, null, null, INVALID_ERRORS, null, INVALID_ERRORS, INVALID_TIME,
                INVALID_TIME, INVALID_TIME);
    }
    
    QFSession(QFSessionType sessionType, Date sessionStartTime, Date delayTime, Boolean speculationRunning,
            String [] availableProposals, String [] speculationProposals, int errorsBefore, String selectedProposal,
            int errorsAfter, Date sessionEndTime, Date localComputationLength, Date analysisLength)
    {
        sessionType_ = sessionType;
        log_ = new StringBuffer();
        sessionStartTime_ = sessionStartTime;
        delayTime_ = delayTime;
        isSpeculationRunning_ = speculationRunning;
        availableProposals_ = availableProposals;
        speculationProposals_ = speculationProposals;
        errorsBefore_ = errorsBefore;
        selectedProposalString_ = selectedProposal;
        errorsAfter_ = errorsAfter;
        sessionEndTime_ = sessionEndTime;
        invalid_ = false;
        logConstructed_ = false;
        selectedProposal_ = null;
        locations_ = null;
        
        long difference = sessionEndTime.getTime() - sessionStartTime_.getTime();
        long million = (long)Math.pow(10, 6);
        if (difference > million*100)
        {
            sessionStartTime_ = new Date(sessionStartTime_.getTime() / million);
            sessionEndTime_ = new Date(sessionEndTime_.getTime() / million);
        }
        if (delayTime.getTime() > million*100)
            delayTime_ = new Date(delayTime_.getTime() / million);
        
        localSpeculationCompletionTime_ = localComputationLength;
        analysisCompletionTime_ = analysisLength;
    }
    
    private void log(String log)
    {
        logger.finer("Logging: " + log);
        log_.append(log);
        log_.append(LINE_SEPARATOR);
    }

    public synchronized void invalidate()
    {
        invalid_ = true;
    }

    public synchronized void setSpeculationProposals(String [] proposals)
    {
        speculationProposals_ = proposals;
    }

    public synchronized boolean isInvalid()
    {
        return invalid_;
    }

    public synchronized void logPopupCreated()
    {
        Date currentTime = new Date();
        TaskWorker currentWorker = Observer.getUsageObserver().getCurrentTaskWorker();
        if (currentWorker != null)
        {
            Date lastModificationDate = currentWorker.getLastModificationDate();
            delayTime_ = Dates.subtract(currentTime, lastModificationDate);
        }
        else
            delayTime_ = INVALID_TIME;
        sessionStartTime_ = currentTime;    
        
        // See if speculative analysis is running.
        ISpeculatorObserverBridge bridge = BridgeActionManager.getInstance().getSpeculatorObserverBridge();
        isSpeculationRunning_ = bridge == null ? null : bridge.isSpeculationRunning();
    }

    public synchronized void setLocations(IProblemLocation [] locations)
    {
        locations_ = locations;
    }

    public synchronized IProblemLocation [] getLocations()
    {
        return locations_;
    }

    /**
     * Caller of this method must be sure that the session is valid!
     */
    public synchronized void logNumberOfErrorsBefore(int errors)
    {
        errorsBefore_ = errors;
        logger.fine("Number of compilation errors before the selection is computed.");
        notifyAll();
    }

    /**
     * Caller of this method must be sure that the session is valid!
     * 
     * @param proposal
     */
    public synchronized void logProposalSelected(ICompletionProposal proposal)
    {
        selectedProposal_ = proposal;
        selectedProposalString_ = selectedProposal_ == null ? "" : selectedProposal_.getDisplayString();
        notifyAll();
    }

    /**
     * Caller of this method must be sure that the session is valid!
     * 
     * @param errors
     */
    public synchronized void logNumberOfErrorsAfter(int errors)
    {
        errorsAfter_ = errors;
        notifyAll();
    }
    
    public synchronized void logPopupClosed()
    {
        if (isInvalid())
        {
            CommonLoggers.getCommunicationLogger().info("Session is invalid, close message is discarded.");
            return;
        }
        Date currentTime = new Date();
        sessionEndTime_ = currentTime;
        if (sessionStartTime_ == INVALID_TIME)
        {
            CommonLoggers.getCommunicationLogger().info("Popup close detected without a popup is created, ignoring event.");
            return;
        }
        Thread thread = new LogFinalizer();
        thread.start();
    }

    /**
     * The caller of this method must be sure that the current session is valid!
     * 
     * @param proposals
     */
    public synchronized void setProposals(IJavaCompletionProposal [] proposals)
    {
        if (proposals == null)
            availableProposals_ = new String [0];
        else
        {
            Arrays.sort(proposals, new QuickFixUtility.EclipseProposalSorter());
            availableProposals_ = new String [proposals.length];
            for (int a = 0; a < availableProposals_.length; a++)
                availableProposals_[a] = proposals[a].getDisplayString();
        }
        logger.fine("Avaible proposals are set.");
        notifyAll();
    }
    
    public String toString()
    {
        if (!logConstructed_)
            createLog();
        return log_.toString();
    }

    public synchronized void logChangePerformed(String changeName)
    {
        if (isInvalid())
            return;
        boolean proposalsMatch_ = false;
        if (selectedProposal_ instanceof ChangeCorrectionProposal)
        {
            ChangeCorrectionProposal correctionProposal = (ChangeCorrectionProposal) selectedProposal_;
            try
            {
                if (correctionProposal.getChange().getName().equals(changeName))
                    proposalsMatch_ = true;
            }
            catch (CoreException e)
            {}
        }
        if (selectedProposalString_ != null && selectedProposalString_.equals(changeName))
            proposalsMatch_ = true;
        if (proposalsMatch_)
        {
            Thread compilationErrorLogger = new ObservationCompilationErrorLogger(this, Type.AFTER);
            compilationErrorLogger.start();
        }
        else
            logger.warning("Selected proposal and change performed does not match! Selected proposal = "
                    + selectedProposalString_ + ", change performed = " + changeName);
    }
    
    public void setSessionType(QFSessionType sessionType)
    {
        sessionType_ = sessionType;
    }

    private String makeString(Date date)
    {
        return DateFormat.getInstance().format(date);
    }

    private synchronized void createLog()
    {
        log(SESSION_TYPE_STRING + sessionType_.toString());
        log(SESSION_VALIDITY_STRING + !markedInvalid_);
        log(SESSION_START_STRING + makeString(sessionStartTime_));
        log(SESSION_DELAY_STRING + makeString(delayTime_) + SESSION_DELAY_STRING_SEPERATOR
                + Dates.toReadableString(delayTime_));
        Date instant = new Date(0);
        if (localSpeculationCompletionTime_ != null)
        {
            Date localDelay = Dates.subtract(localSpeculationCompletionTime_, sessionStartTime_);
            if (localDelay.before(instant))
                localDelay = instant;
            log(LOCAL_COMPUTATION_DELAY_STRING + makeString(localDelay) + SESSION_DELAY_STRING_SEPERATOR + Dates.toReadableString(localDelay));
        }
        if (analysisCompletionTime_ != null)
        {
            Date globalDelay = Dates.subtract(analysisCompletionTime_, sessionStartTime_);
            if (globalDelay.before(instant))
                globalDelay = instant;
            log(GLOBAL_COMPUTATION_DELAY_STRING + makeString(globalDelay) + SESSION_DELAY_STRING_SEPERATOR + Dates.toReadableString(globalDelay));
        }
        log(SPECULATION_RUNNING_STRING + isSpeculationRunning_);
        log(ECLIPSE_PROPOSALS_STRING);
        for (int a = 0; a < availableProposals_.length; a++)
        {
            int index = a + 1;
            String indexS = (availableProposals_.length >= 10 && index < 10 ? "0" : "") + index;
            log(indexS + PROPOSAL_SPLIT_STRING + availableProposals_[a]);
        }
        if (speculationProposals_ != null)
        {
            log(SPECULATION_PROPOSALS_STRING);
            for (int a = 0; a < speculationProposals_.length; a++)
            {
                int index = a + 1;
                String indexS = (speculationProposals_.length >= 10 && index < 10 ? "0" : "") + index;
                log(indexS + PROPOSAL_SPLIT_STRING + speculationProposals_[a]);
            }
        }
        log(BEFORE_COMPILATION_ERROR_STRING + errorsBefore_);
        if (selectedProposalString_ != null)
        {
            log(USER_SELECTED_STRING + selectedProposalString_);
            log(AFTER_COMPILATION_ERROR_STRING + errorsAfter_);
        }
        else
            log(TERMINATION_WITHOUT_SELECTION_STRING);
        log(SESSION_END_STRING + makeString(sessionEndTime_));
        log(SESSION_LENGTH_STRING + Dates.toReadableString(Dates.subtract(sessionEndTime_, sessionStartTime_)));
        logConstructed_ = true;
    }

    private synchronized boolean isLogCompleted()
    {
        StringBuffer log = new StringBuffer();
        boolean sessionTypeSet = sessionType_ != null;
        boolean availableProposalsSet = availableProposals_ != null;
        boolean startTimeSet = sessionStartTime_ != INVALID_TIME;
        boolean endTimeSet = sessionEndTime_ != INVALID_TIME;
        boolean errorsBeforeSet = errorsBefore_ != INVALID_ERRORS;
        boolean selectedProposalSet = selectedProposal_ != null;
        boolean errorsAfterSet = errorsAfter_ != INVALID_ERRORS;
        log.append("sessionType != null ==> " + sessionTypeSet + LINE_SEPARATOR);
        log.append("availableProposals != null ==> " + availableProposalsSet + LINE_SEPARATOR);
        log.append("sessionStartTime != invalidTime ==> " + startTimeSet + LINE_SEPARATOR);
        log.append("sessionEndTime != invalidTime ==> " + endTimeSet + LINE_SEPARATOR);
        log.append("errorsBefore != invalidErrors ==> " + errorsBeforeSet + LINE_SEPARATOR);
        log.append("selectedProposal != null ==> " + selectedProposalSet + LINE_SEPARATOR);
        log.append("errorsAfter != invalidErrors ==> " + errorsAfterSet + LINE_SEPARATOR);
        logger.finer(log.toString());
        return sessionTypeSet && availableProposalsSet && startTimeSet && endTimeSet && errorsBeforeSet
                && (selectedProposalString_ == null || errorsAfterSet);
    }

    private class LogFinalizer extends Thread
    {
        public void run()
        {
            CommonLoggers.getCommunicationLogger().info("Log Finalizer is running.");
            while (!isLogCompleted())
            {
                try
                {
                    synchronized (QFSession.this)
                    {
                        QFSession.this.wait();
                    }
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
            createLog();
            ObservationLogger.getLogger().log(QFSession.this);
            CommonLoggers.getCommunicationLogger().info("Session logged with success.");
        }
    }

    public void setLocalSpeculationCompletionTime(Date time)
    {
        localSpeculationCompletionTime_ = time;
    }

    public void setAnalysisCompletionTime(Date time)
    {
        analysisCompletionTime_ = time;
    }
    
    /** JUST ADDED FOR LOG ANALYSES, NOT USED IN THE PLUG-IN */
    
    /*******************************
     ************ GETTERS **********
     *******************************/
    
    void undone()
    {
        undone_ = true;
    }
    
    boolean isUndone()
    {
        return undone_;
    }
    
    Action isGlobalBestProposalSelected()
    {
        // If the session is not completed, then no meaning to compute this.
        if (!isSessionCompleted())
            return Action.NOT_AVAILABLE;
        if (isSpeculationRunning_ == null)
        {
            // If the speculative analysis information is not available and speculation proposals
            // are not recorded, then we assume that we could not record this information.
            if (speculationProposals_ == null)
                return Action.NOT_AVAILABLE;
        }
        else if (!isSpeculationRunning_)
            // if the speculation was not running, then this cannot be computed.
            return Action.NOT_AVAILABLE;
        // At this moment, the speculation is guaranteed to be running (i.e., either the flag is set
        // or speculative proposals are set).
        if (speculationProposals_ == null)
            // if speculative proposals are not set, it means the speculation was running, however the
            // user closed the dialog without waiting for the speculation results.
            return Action.FALSE;
        ArrayList <String> globalBestProposals = getGlobalBestProposals();
        for (String gbp: globalBestProposals)
        {
            if(containsAllWords(gbp, selectedProposalString_))
                return Action.TRUE;
        }
        return Action.FALSE;
    }

    private static boolean containsAllWords(String source, String target)
    {
        ArrayList<String> words = new ArrayList<String>(Arrays.asList(target.split(" ")));
        ArrayList<String> sourceWords = new ArrayList<String>(Arrays.asList(source.split(" ")));
        for (String word: words)
        {
            if (!sourceWords.contains(word))
                return false;
            else
                sourceWords.remove(word);
        }
        return true;
    }

    Action isBestProposalSelected()
    {
        // if global best proposal is selected or first proposal is selected, then we know
        // it is guaranteed that the selected proposal is the best proposal. We also know that
        // if there is no speculation information, and the first proposal is not selected, then
        // there is no way to know.
        Action result = isGlobalBestProposalSelected();// .or(isFirstProposalSelected());
        if (result == Action.FALSE && speculationProposals_ != null)
        {
            // However, if there is speculation information, we could still determine whether it is
            // the best or not.
            int compilationError = -1;
            int bestCompilationError = Integer.MAX_VALUE;
            ArrayList <String> globalBestProposals = getGlobalBestProposals();
            for (String speculationProposal: speculationProposals_)
            {
//                System.out.println(speculationProposal);
                int currentCompilationError = getCompilationErrorPart(speculationProposal);
                String proposalPart = getProposalPart(speculationProposal);
                if (!globalBestProposals.contains(proposalPart))
                {
                    // We have to make sure that we are not including global best proposal's compilation
                    // error information to our calculation.
                    if (proposalPart.equals(selectedProposalString_))
                        compilationError = currentCompilationError;
                    bestCompilationError = Math.min(bestCompilationError, currentCompilationError);
                }
            }
            assert compilationError == -1 || bestCompilationError == Integer.MAX_VALUE || compilationError >= bestCompilationError;
//            System.out.println("Selected proposal = " + selectedProposalString_ + ", compilation error = " + compilationError + ", best compilation error = " + bestCompilationError );
            if (compilationError != -1 && compilationError == bestCompilationError)
                // If the number of compilation errors for the selected proposal is equal to the number of lowest compilation errors
                // then, we return true.
                result = Action.TRUE;
        }
        return result;
    }
    
    Action isFirstProposalSelected()
    {
        // If session is not completed, there is no way to measure this.
        if (!isSessionCompleted())
            return Action.NOT_AVAILABLE;
        
        // If no proposals are offered, there is no way to compute this.
        if (availableProposals_ == null || availableProposals_.length == 0)
            return Action.NOT_AVAILABLE;
        
        // Look whether the speculation analysis is running and/or we have speculation results. If so, look if the
        // selected proposal is the same as the first speculation proposal.
        if (speculationProposals_ != null && speculationProposals_.length > 0)
        {
            if (selectedProposalString_.equals(speculationProposals_[0]))
                return Action.TRUE;
        }
        
        // If there is no speculation information, then look whether the first selected proposal is equal to the first
        // Eclipse proposal or not.
        return Action.FromBoolean(selectedProposalString_.equals(availableProposals_[0]));
    }

    Action isOtherProposalSelected()
    {
        for (int a = 1; a < availableProposals_.length; a++)
        {
            if (availableProposals_[a].equals(selectedProposalString_))
                return Action.TRUE;
        }
        return Action.FALSE;
//        return isFirstProposalSelected().negate();
    }
    
    boolean isGlobalBestProposalGenerated()
    {
//        System.out.println("Global Best Proposals: ");
//        for (String gbp: getGlobalBestProposals())
//            System.out.println(gbp);
        
        return getGlobalBestProposals().size() != 0;
    }

    String getSelectedProposalString()
    {
        return selectedProposalString_;
    }

    Date getStartTime()
    {
        return sessionStartTime_;
    }

    Date getCompletionTime()
    {
        return sessionEndTime_;
    }

    Date getDelayTime()
    {
        return delayTime_;
    }

    Date getLength()
    {
        return Dates.subtract(sessionEndTime_, sessionStartTime_);
    }
    
    int getNumberOfProposalsOfferedByEclipse()
    {
        return availableProposals_.length;
    }

    int getErrorsBefore()
    {
        return errorsBefore_;
    }

    int getErrorsAfter()
    {
        return errorsAfter_;
    }
    
    Boolean isSpeculationRunning()
    {
        return isSpeculationRunning_;
    }
    
    String [] getEclipseProposals()
    {
        return availableProposals_;
    }
    
    static ArrayList<String> getGlobalBestProposals(ArrayList<String> speculationProposals, ArrayList<String> eclipseProposals)
    {
        if (speculationProposals == null)
            return null;
        
        ArrayList<String> result = new ArrayList <String>();
        for (String speculationProposal: speculationProposals)
        {
            if (speculationProposal.contains(".java:"))
                result.add(speculationProposal);
            else
            {
                for (String eclipseProposal: eclipseProposals)
                {
                    if (containsAllWords(speculationProposal, eclipseProposal))
                    {
                        result.add(speculationProposal);
                        break;
                    }
                }
            }
        }
        return result;
    }
    
    String [] getSpeculationProposals()
    {
        if (speculationProposals_ == null)
            return null;
        ArrayList<String> result = new ArrayList <String>();
        for (String speculationProposal: speculationProposals_)
        {
            if (speculationProposal.contains(".java:"))
                result.add(speculationProposal);
            else
            {
                for (String eclipseProposal: availableProposals_)
                {
                    if (containsAllWords(speculationProposal, eclipseProposal))
                    {
                        result.add(speculationProposal);
                        break;
                    }
                }
            }
        }
        return result.toArray(new String [result.size()]);
    }
    
    boolean isSessionCompleted()
    {
        return selectedProposalString_ != null || selectedProposal_ != null;
    }
    
    /********************************
     ********* INTERNAL API *********
     *******************************/
    
    private String getProposalPart(String speculationProposal)
    {
        return speculationProposal.substring(speculationProposal.indexOf(')') + 1).trim();
    }
    
    private int getCompilationErrorPart(String speculationProposal)
    {
        String representation = speculationProposal.substring(1,speculationProposal.indexOf(')')).trim();
        if (representation.equals("?"))
            return Integer.MAX_VALUE;
        return Integer.parseInt(representation);
    }
    
    private ArrayList <String> getGlobalBestProposals()
    {
        if (speculationProposals_ == null)
            return new ArrayList <String>();
        
        ArrayList <String> result = new ArrayList <String>();
        HashSet <String> eclipseProposals = new HashSet <String>(Arrays.asList(availableProposals_));
        for (String speculationProposal: speculationProposals_)
        {
            String proposalPart = getProposalPart(speculationProposal);
            if (speculationProposal.contains(".java:") && !eclipseProposals.contains(proposalPart))
                result.add(proposalPart);
        }
        return result;
    }
    
    static enum Action
    {
        TRUE("TRUE"),
        FALSE("FALSE"),
        NOT_AVAILABLE("N/A");
        
        private String value_;
        
        private Action(String value)
        {
            value_ = value;
        }
        
        public String toString()
        {
            return value_;
        }
        
        public Action or(Action other)
        {
            if (this == NOT_AVAILABLE || other == NOT_AVAILABLE)
                return NOT_AVAILABLE;
            if (this == TRUE || other == TRUE)
                return TRUE;
            return FALSE;
        }
        
        public Action negate()
        {
            return this == Action.TRUE ? Action.FALSE : (this == Action.FALSE ? Action.TRUE : Action.NOT_AVAILABLE);
        }
        
        public static Action FromBoolean(boolean value)
        {
            return value ? Action.TRUE : Action.FALSE;
        }
        
        public Boolean toBoolean()
        {
            if (this == Action.FALSE)
                return false;
            if (this == Action.TRUE)
                return true;
            return null;
        }
    }

    void assertProposalSelection()
    {
        StringBuilder log = new StringBuilder();
        String ls = System.getProperty("line.separator");
        log.append("Selected proposal: " + selectedProposalString_ + ls);
        log.append("Eclipse proposals:" + ls);
        for (String proposal: availableProposals_)
            log.append("\t" + proposal + ls);
        if (speculationProposals_ != null)
        {
            log.append("Speculation proposals:" + ls);
            for (String proposal: speculationProposals_)
                log.append("\t" + proposal + ls);
        }
        System.err.println(log);
    }

    public void markInvalidated()
    {
        markedInvalid_ = true;
        
    }
    
}
