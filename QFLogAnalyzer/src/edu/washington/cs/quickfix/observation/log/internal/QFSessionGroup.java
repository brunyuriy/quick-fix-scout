package edu.washington.cs.quickfix.observation.log.internal;

import edu.washington.cs.quickfix.observation.log.internal.QFSession.Action;

// Represents a couple of QF sessions for the same user and for the same kind of usage
// (i.e., either with speculative analysis running, not running, or unknown)
public class QFSessionGroup
{
 // add max, min, average number of proposal offered by eclipse.
    // delay length.
    private long minDelayLength_ = Integer.MAX_VALUE;
    private long maxDelayLength_ = Integer.MIN_VALUE;
    private long totalDelayLength_ = 0;
    // session length.
    private long minSessionLength_ = Integer.MAX_VALUE;
    private long maxSessionLength_ = Integer.MIN_VALUE;
    private long totalSessionLength_ = 0;
    // completed session length.
    private long minCompletedSessionLength_ = Integer.MAX_VALUE;
    private long maxCompletedSessionLength_ = Integer.MIN_VALUE;
    private long totalCompletedSessionLength_ = 0;
    // errors before
    private int minErrorsBefore_ = Integer.MAX_VALUE;
    private int maxErrorsBefore_ = Integer.MIN_VALUE;
    private int totalErrorsBefore_ = 0;
    private int minErrorsBeforeCompleted_ = Integer.MAX_VALUE;
    private int maxErrorsBeforeCompleted_ = Integer.MIN_VALUE;
    private int totalErrorsBeforeCompleted_ = 0;
    // errors after
    private int minErrorsAfter_ = Integer.MAX_VALUE;
    private int maxErrorsAfter_ = Integer.MIN_VALUE;
    private int totalErrorsAfter_ = 0;
    // error change
    private int maxErrorsDecrease_ = Integer.MIN_VALUE;
    private int maxErrorsIncrease_ = Integer.MIN_VALUE;
    private int netErrorGain_ = 0;
    // offered proposals
    private int minEclipseProposals_ = Integer.MAX_VALUE;
    private int maxEclipseProposals_ = Integer.MIN_VALUE;
    private int totalEclipseProposals_ = 0;     // number of proposals offered by Eclipse.
    private int fpSelected_ = 0;
    private int bpSelected_ = 0;
    private int gbpSelected_ = 0;
    private int opSelected_ = 0; // other proposal.
    private int gbpGenerated_ = 0;
    // general statistics
    private int totalSessions_ = 0;
    private int sessionLengthValidSessions_ = 0;
    private int completedSessionLengthValidSessions_ = 0;
    private int delayLengthValidSessions_ = 0;
    private int totalCompletedSessions_ = 0;
    private int totalCanceledSessions_ = 0;
    private int totalPositiveSessions_ = 0;
    private int totalNegativeSessions_ = 0;
    
    void analyzeQFSession(QFSession session)
    {
        totalSessions_++;
        // session length
        long sessionLength = session.getLength().getTime();
        assert sessionLength >= 0: "\nSession length for qf session = " + session + "is not positive.";
        if (sessionLength > 0)
        {
            totalSessionLength_ += sessionLength;
            maxSessionLength_ = Math.max(maxSessionLength_, sessionLength);
            minSessionLength_ = Math.min(minSessionLength_, sessionLength);
            sessionLengthValidSessions_++;
        }
        // delay length
        long delayLength = session.getDelayTime().getTime();
        assert delayLength >= 0: "\nDelay length for qf session = " + delayLength + "is not positive.";
        if (delayLength > 0)
        {
            minDelayLength_ = Math.min(minDelayLength_, delayLength);
            maxDelayLength_ = Math.max(maxDelayLength_, delayLength);
            delayLengthValidSessions_ ++;
            totalDelayLength_ += delayLength;
        }
        // errors before
        int errorsBefore = session.getErrorsBefore();
        minErrorsBefore_ = Math.min(minErrorsBefore_, errorsBefore);
        maxErrorsBefore_ = Math.max(maxErrorsBefore_, errorsBefore);
        totalErrorsBefore_ += errorsBefore;
        // eclipse offered proposals
        int numberOfEclipseOfferedProposals = session.getNumberOfProposalsOfferedByEclipse();
        minEclipseProposals_ = Math.min(minEclipseProposals_,
                numberOfEclipseOfferedProposals);
        maxEclipseProposals_ = Math.max(maxEclipseProposals_,
                numberOfEclipseOfferedProposals);
        totalEclipseProposals_ += numberOfEclipseOfferedProposals;
        int errorsAfter = session.getErrorsAfter();
        if (errorsAfter == QFSession.INVALID_ERRORS)
            totalCanceledSessions_++;
        else
        {
            totalCompletedSessions_++;
            if (sessionLength > 0)
            {
                // completed session length
                maxCompletedSessionLength_ = Math.max(maxCompletedSessionLength_, sessionLength);
                minCompletedSessionLength_ = Math.min(minCompletedSessionLength_, sessionLength);
                totalCompletedSessionLength_ += sessionLength;
                completedSessionLengthValidSessions_ ++;
            }
            Action gbpSelected = session.isGlobalBestProposalSelected();
            Action opSelected = session.isOtherProposalSelected();
            Action fpSelected = session.isFirstProposalSelected();
            // selected proposal
            if (session.isBestProposalSelected() == Action.TRUE)
                bpSelected_++;
            if (fpSelected == Action.TRUE)
                fpSelected_++;
            if (opSelected == Action.TRUE)
                opSelected_++;
            if (gbpSelected == Action.TRUE)
                gbpSelected_++;
            if (!(gbpSelected == Action.TRUE || opSelected == Action.TRUE || fpSelected == Action.TRUE))
                session.assertProposalSelection();
            if (session.isGlobalBestProposalGenerated())
                gbpGenerated_++;
            // errors after
            minErrorsAfter_ = Math.min(minErrorsAfter_, errorsAfter);
            maxErrorsAfter_ = Math.max(maxErrorsAfter_, errorsAfter);
            totalErrorsAfter_ += errorsAfter;
            // errors before completed
            minErrorsBeforeCompleted_ = Math.min(minErrorsBeforeCompleted_, errorsBefore);
            maxErrorsBeforeCompleted_ = Math.max(maxErrorsBeforeCompleted_, errorsBefore);
            totalErrorsBeforeCompleted_ += errorsBefore;
            // new change in errors
            int netGain = errorsBefore - errorsAfter;
            netErrorGain_ += netGain;
            if (netGain > 0)
            {
                totalPositiveSessions_++;
                maxErrorsDecrease_ = Math.max(maxErrorsDecrease_, netGain);
            }
            else
            {
                totalNegativeSessions_++;
                maxErrorsIncrease_ = Math.max(maxErrorsIncrease_, -netGain);
            }
        }
    }
    
    public static QFSessionGroup sum(QFSessionGroup g1, QFSessionGroup g2)
    {
        // general statistics.
        QFSessionGroup result = new QFSessionGroup();
        result.totalSessions_ = g1.totalSessions_ + g2.totalSessions_;
        result.delayLengthValidSessions_ = g1.delayLengthValidSessions_ + g2.delayLengthValidSessions_;
        result.sessionLengthValidSessions_ = g1.sessionLengthValidSessions_ + g2.sessionLengthValidSessions_;
        result.completedSessionLengthValidSessions_ = g1.completedSessionLengthValidSessions_ + g2.completedSessionLengthValidSessions_;
        result.totalCanceledSessions_ = g1.totalCanceledSessions_ + g2.totalCanceledSessions_;
        result.totalCompletedSessions_ = g1.totalCompletedSessions_ + g2.totalCompletedSessions_;
        result.totalPositiveSessions_ = g1.totalPositiveSessions_ + g2.totalPositiveSessions_;
        result.totalNegativeSessions_ = g1.totalNegativeSessions_ + g2.totalNegativeSessions_;
        // delay length
        result.totalDelayLength_ = g1.totalDelayLength_ + g2.totalDelayLength_;
        result.minDelayLength_ = Math.min(g1.minDelayLength_, g2.minDelayLength_);
        result.maxDelayLength_ = Math.max(g1.maxDelayLength_, g2.maxDelayLength_);
        // session length
        result.minSessionLength_ = Math.min(g1.minSessionLength_, g2.minSessionLength_);
        result.maxSessionLength_ = Math.max(g1.maxSessionLength_, g2.maxSessionLength_);
        result.totalSessionLength_ = g1.totalSessionLength_ + g2.totalSessionLength_;
        // completed session length
        result.maxCompletedSessionLength_ = Math.max(g1.maxCompletedSessionLength_, g2.maxCompletedSessionLength_);
        result.minCompletedSessionLength_ = Math.min(g1.minCompletedSessionLength_, g2.minCompletedSessionLength_);
        result.totalCompletedSessionLength_ = g1.totalCompletedSessionLength_ + g2.totalCompletedSessionLength_;
        // errors before
        result.minErrorsBefore_ = Math.min(g1.minErrorsBefore_, g2.minErrorsBefore_);
        result.maxErrorsBefore_ = Math.max(g1.maxErrorsBefore_, g2.maxErrorsBefore_);
        result.totalErrorsBefore_ = g1.totalErrorsBefore_ + g2.totalErrorsBefore_;
        result.minErrorsBeforeCompleted_ = Math.min(g1.minErrorsBeforeCompleted_, g2.minErrorsBeforeCompleted_);
        result.maxErrorsBeforeCompleted_ = Math.max(g1.maxErrorsBeforeCompleted_, g2.maxErrorsBeforeCompleted_);
        result.totalErrorsBeforeCompleted_ = g1.totalErrorsBeforeCompleted_ + g2.totalErrorsBeforeCompleted_;
        // errors after
        result.minErrorsAfter_ = Math.min(g1.minErrorsAfter_, g2.minErrorsAfter_);
        result.maxErrorsAfter_ = Math.max(g1.maxErrorsAfter_, g2.maxErrorsAfter_);
        result.totalErrorsAfter_ = g1.totalErrorsAfter_ + g2.totalErrorsAfter_;
        // errors change
        result.maxErrorsDecrease_ = Math.max(g1.maxErrorsDecrease_, g2.maxErrorsDecrease_);
        result.maxErrorsIncrease_ = Math.max(g1.maxErrorsIncrease_, g2.maxErrorsIncrease_);
        result.netErrorGain_ = g1.netErrorGain_ + g2.netErrorGain_;
        // offered proposal
        result.minEclipseProposals_ = Math.min(g1.minEclipseProposals_, g2.minEclipseProposals_);
        result.maxEclipseProposals_ = Math.max(g1.minEclipseProposals_, g2.minEclipseProposals_);
        result.totalEclipseProposals_ = g1.totalEclipseProposals_ + g2.totalEclipseProposals_;
        result.bpSelected_ = g1.bpSelected_ + g2.bpSelected_;
        result.fpSelected_ = g1.fpSelected_ + g2.fpSelected_;
        result.opSelected_ = g1.opSelected_ + g2.opSelected_;
        result.gbpSelected_ = g1.gbpSelected_ + g2.gbpSelected_;
        result.gbpGenerated_ = g1.gbpGenerated_ + g2.gbpGenerated_;
        
        return result;
    }

    void printResults(String sessionName)
    {
        String twoTabs = "\t\t";
        String oneTab = "\t";
        String seperator = " = ";
        if (totalSessions_ != 0)
        {
            System.out.println("= " + sessionName + " sessions =");
            String max = normalizeString(twoTabs + "Max");
            String maxIncrease = normalizeString(twoTabs + "Max Increase");
            String maxDecrease = normalizeString(twoTabs + "Max Decrease");
            String totalNet = normalizeString(twoTabs + "Total Net");
            String min = normalizeString(twoTabs + "Min");
            String average = normalizeString(twoTabs + "Average");
            String total = normalizeString(twoTabs + "Total");
            String canceled = normalizeString(twoTabs + "Canceled");
            String completed = normalizeString(twoTabs + "Completed");
            String fpSelected = normalizeString(twoTabs + "First Proposal selected");
            String opSelected = normalizeString(twoTabs + "Other Proposal selected");
            String bpSelected = normalizeString(twoTabs + "Best Proposal selected");
            String gbpSelected = normalizeString(twoTabs + "Global Best Proposal selected");
            String gbpGenerated = normalizeString(twoTabs + "Global Best Proposal generated");
            String positive = normalizeString(twoTabs + "Positive");
            String negative = normalizeString(twoTabs + "Negative");
            // session info
            System.out.println(oneTab + "# of Sessions");
            System.out.println(total + seperator + totalSessions_);
            System.out.println(canceled + seperator + totalCanceledSessions_);
            System.out.println(completed + seperator + totalCompletedSessions_);
            // session length
            System.out.println(oneTab + "Session Length:");
            System.out.println(max + seperator + convertToHumanReadableTime(maxSessionLength_));
            System.out.println(min + seperator + convertToHumanReadableTime(minSessionLength_));
            System.out.println(average + seperator + convertToHumanReadableTime(getAverageSessionLength()));
            // delay length
            System.out.println(oneTab + "Delay Length:");
            System.out.println(max + seperator + convertToHumanReadableTime(maxDelayLength_));
            System.out.println(min + seperator + convertToHumanReadableTime(minDelayLength_));
            System.out.println(average + seperator + convertToHumanReadableTime(getAverageDelayLength()));
            // errors before
            System.out.println(oneTab + "# of Errors Before:");
            System.out.println(max + seperator + maxErrorsBefore_);
            System.out.println(min + seperator + minErrorsBefore_);
            System.out.println(average + seperator + String.format("%.2f", getAverageErrorsBefore()));
            // eclipse proposals (# of)
            System.out.println(oneTab + "# of Eclipse Proposals Offered:");
            System.out.println(max + seperator + maxEclipseProposals_);
            System.out.println(min + seperator + minEclipseProposals_);
            System.out.println(average + seperator + String.format("%.2f", getAverageEclipseProposals()));
            if (totalCompletedSessions_ != 0)
            {
                // completed session length
                System.out.println(oneTab + "Completed Session Length:");
                System.out.println(max + seperator + convertToHumanReadableTime(maxCompletedSessionLength_));
                System.out.println(min + seperator + convertToHumanReadableTime(minCompletedSessionLength_));
                System.out.println(average + seperator + convertToHumanReadableTime(getAverageCompletedSessionLength()));
                // errors before completed
                System.out.println(oneTab + "# of Errors Before (for Completed Sessions):");
                System.out.println(max + seperator + maxErrorsBeforeCompleted_);
                System.out.println(min + seperator + minErrorsBeforeCompleted_);
                System.out.println(average + seperator + String.format("%.2f", getAverageErrorsBeforeCompleted()));
                // errors after
                System.out.println(oneTab + "# of Errors After:");
                System.out.println(max + seperator + maxErrorsAfter_);
                System.out.println(min + seperator + minErrorsAfter_);
                System.out.println(average + seperator + String.format("%.2f", getAverageErrorsAfter()));
                // errors change status
                System.out.println(oneTab + "# of Errors Change in a Session:");
                System.out.println(maxDecrease + seperator + maxErrorsDecrease_);
                System.out.println(maxIncrease + seperator + maxErrorsIncrease_);
                System.out.println(totalNet + seperator + netErrorGain_);
                // positive-negative session information
                System.out.println(oneTab + "Positive-Negative Session Information:");
                System.out.println(positive + seperator + totalPositiveSessions_);
                System.out.println(negative + seperator + totalNegativeSessions_);
                // proposal information
                System.out.println(oneTab + "Proposal information: (# of)");
                System.out.println(fpSelected + seperator + fpSelected_);
                System.out.println(opSelected + seperator + opSelected_);
                System.out.println(bpSelected + seperator + bpSelected_);
                System.out.println(gbpSelected + seperator + gbpSelected_);
                System.out.println(gbpGenerated + seperator + gbpGenerated_);
            }
        }
    }
    
    private String normalizeString(String str)
    {
        return String.format("%-15s", str);
    }
    
    private static String convertToHumanReadableTime(long milliseconds)
    {
        long seconds = milliseconds / 1000;
        milliseconds %= 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        // Not sure if I need to check hours
        return padWithZero(minutes, 2) + ":" + padWithZero(seconds, 2) + ":" + padWithZero(milliseconds, 3);
    }
    
    private static String padWithZero(long time, int maxLength)
    {
        String result = time + "";
        while (result.length() < maxLength)
            result = "0" + result;
        return result;
    }
    
    private double getAverageEclipseProposals()
    {
        return totalEclipseProposals_ * 1.0 / totalSessions_;
    }

    private long getAverageDelayLength()
    {
        if (delayLengthValidSessions_ == 0)
            return -1;
        return totalDelayLength_ / delayLengthValidSessions_;
    }

    public double getAverageErrorsAfter()
    {
        return totalErrorsAfter_ * 1.0 / totalCompletedSessions_;
    }

    public double getAverageErrorsBefore()
    {
        return totalErrorsBefore_ * 1.0 / totalSessions_;
    }

    public double getAverageErrorsBeforeCompleted()
    {
        return totalErrorsBeforeCompleted_ * 1.0 / totalCompletedSessions_;
    }

    private long getAverageSessionLength()
    {
        return totalSessionLength_ / sessionLengthValidSessions_;
    }

    private long getAverageCompletedSessionLength()
    {
        return totalCompletedSessionLength_ / completedSessionLengthValidSessions_;
    }
}
