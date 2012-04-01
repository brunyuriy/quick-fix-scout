package edu.washington.cs.quickfix.observation.log.internal;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;
import java.util.Stack;

import com.kivancmuslu.www.time.Dates;

import edu.washington.cs.quickfix.observation.log.internal.QFSession.QFSessionType;

public class QFSessionLogParser
{
    private File file_;
    private Stack<QFSession> completedSessions_ = new Stack <QFSession>();
    public static int missingUndoes_ = 0;
    
    public EclipseSessionLog parse(File file) throws Exception
    {
        file_ = file;
        EclipseSessionLog log = new EclipseSessionLog();
        Scanner reader = new Scanner(file_);
        while (reader.hasNext())
        {
            String line = reader.nextLine().trim();
            if (line.equals(""))
                continue;
            else if (isUsageSessionLog(line))
            {
                QFSession usageSessionEvent = parseUsageSession(line, reader);
                if (usageSessionEvent != null)
                {
                    if (usageSessionEvent.getSelectedProposalString() != null)
                        completedSessions_.push(usageSessionEvent);
                    log.addLogEvent(usageSessionEvent);
                }
            }
            else if (isUndoLog(line))
            {
                String undoEvent = parseUndo(line);
                QFSession lastSession = completedSessions_.peek();
                if (equalUndoEvent(lastSession.getSelectedProposalString(), undoEvent))
//                if (lastSession.getSelectedProposalString().equals(undoEvent))
                {
                    lastSession.undone();
                    completedSessions_.pop();
                }
                else
                {
//                    QFSession match = null;
//                    for (QFSession completedSession: completedSessions_)
//                    {
//                        if (completedSession.getSelectedProposalString().equals(undoEvent))
//                            match = completedSession;
//                    }
//                    if (match != null)
//                    {
//                        match.undone();
//                        completedSessions_.remove(match);
//                    }
//                    else
                    {
                        System.err
                        .println("Undone event and last session selected proposal does not match: selected proposal = "
                                + lastSession.getSelectedProposalString() + ", undone proposal = " + undoEvent);
                        missingUndoes_ ++;
                    }
                }
                log.addLogEvent(undoEvent);
            }
        }
        reader.close();
        // System.out.println("Parsing the log completed with success.");
        // System.out.println("\nPrinting the parsed log: ");
        // System.out.println(log);
        return log;
    }

    private boolean equalUndoEvent(String selectedProposalString, String undoEvent)
    {
        if (selectedProposalString.equals("Surround with try/catch") && undoEvent.equals("Surround with try/catch Block"))
            return true;
        return selectedProposalString.equals(undoEvent);
    }

    private boolean isUsageSessionLog(String line)
    {
        return line.startsWith(QFSession.SESSION_START_STRING);
    }

    private boolean isUndoLog(String line)
    {
        return line.startsWith(QFSession.UNDO_STRING);
    }

    private String parseUndo(String undoLine)
    {
        String undoProposal = undoLine.split(QFSession.UNDO_STRING)[1].trim();
        return undoProposal;
    }
    
    private long stringToTime(String input)
    {
        String [] parts = input.split(":");
        return ((Integer.parseInt(parts[0]) * 60) + Integer.parseInt(parts[1])) * 1000 + Integer.parseInt(parts[2]);
    }

    private QFSession parseUsageSession(String initialLine, Scanner reader) throws Exception
    {
        String currentLine = initialLine;
        QFSessionType sessionType = QFSessionType.DIALOG;
        if (checkContains(currentLine, QFSession.SESSION_TYPE_STRING))
        {
            String sessionTypeString = currentLine.split(QFSession.SESSION_TYPE_STRING)[1].trim();
            if (sessionTypeString.equals(QFSessionType.HOVER))
                sessionType = QFSessionType.HOVER;
            currentLine = reader.nextLine().trim();
        }
        
        Date startDate = parseDate(currentLine, QFSession.SESSION_START_STRING, 1);
        currentLine = reader.nextLine().trim();
        Date delayDate = new Date(0);
        if (checkContains(currentLine, QFSession.SESSION_DELAY_STRING))
        {
            String parseInput = currentLine.split(QFSession.SESSION_DELAY_STRING)[1].split(QFSession.SESSION_DELAY_STRING_SEPERATOR)[1].trim();
            delayDate = new Date(stringToTime(parseInput));
            currentLine = reader.nextLine().trim();
        }
        Date localComputationLength = new Date(0);
        if (checkContains(currentLine, QFSession.LOCAL_COMPUTATION_DELAY_STRING))
        {
            String parseInput = currentLine.split(QFSession.LOCAL_COMPUTATION_DELAY_STRING)[1].split(QFSession.SESSION_DELAY_STRING_SEPERATOR)[1].trim();
            // Normalization done due to conversion bug in TimeUtility.
            long delay = stringToTime(parseInput);
            if (delay == 0)
                delay = 1000;
            else if (delay < 100)
                delay *= 1000;
            delayDate = new Date(delay);
            currentLine = reader.nextLine().trim();
        }
        Date analysisLength = new Date(0);
        if (checkContains(currentLine, QFSession.GLOBAL_COMPUTATION_DELAY_STRING))
        {
            String parseInput = currentLine.split(QFSession.GLOBAL_COMPUTATION_DELAY_STRING)[1].split(QFSession.SESSION_DELAY_STRING_SEPERATOR)[1].trim();
            delayDate = new Date(stringToTime(parseInput));
            currentLine = reader.nextLine().trim();
        }
        Boolean speculationRunning = null;
        if (checkContains(currentLine, QFSession.SPECULATION_RUNNING_STRING))
        {
            speculationRunning = Boolean.parseBoolean(currentLine.split(QFSession.SPECULATION_RUNNING_STRING)[1].trim());
            currentLine = reader.nextLine();
        }
        assertEquality(currentLine, QFSession.ECLIPSE_PROPOSALS_STRING);
        ArrayList <String> eclipseProposals = new ArrayList <String>();
        do
        {
            currentLine = reader.nextLine().trim();
            String splitString = QFSession.PROPOSAL_SPLIT_STRING.replace(")", "\\)");
            if (checkContains(currentLine, QFSession.PROPOSAL_SPLIT_STRING))
            {
                String proposal = currentLine.split(splitString)[1].trim();
                eclipseProposals.add(proposal);
            }
        } while (!checkEquality(currentLine, QFSession.SPECULATION_PROPOSALS_STRING)
                && !checkPrefix(currentLine, QFSession.BEFORE_COMPILATION_ERROR_STRING));
        ArrayList <String> speculationProposals = null;
        if (checkEquality(currentLine, QFSession.SPECULATION_PROPOSALS_STRING))
        {
            speculationProposals = new ArrayList <String>();
            do
            {
                currentLine = reader.nextLine().trim();
                String splitString = QFSession.PROPOSAL_SPLIT_STRING.replace(")", "\\)");
                if (checkContains(currentLine, QFSession.PROPOSAL_SPLIT_STRING))
                {
                    String proposal = currentLine.split(splitString)[1].trim();
                    speculationProposals.add(proposal);
                }
            } while (!checkPrefix(currentLine, QFSession.BEFORE_COMPILATION_ERROR_STRING));
            // Why did I need this?
//            speculationProposals.remove(speculationProposals.size() - 1);
        }
        assertPrefix(currentLine, QFSession.BEFORE_COMPILATION_ERROR_STRING);
        int errorsBefore = parseInt(currentLine, QFSession.BEFORE_COMPILATION_ERROR_STRING, 1);
        assertNonNegativeInteger(errorsBefore, "Errors before should be a positive integer! ");
        currentLine = reader.nextLine();
        String selectedProposal = null;
        int errorsAfter = QFSession.INVALID_ERRORS;
        if (checkPrefix(currentLine, QFSession.USER_SELECTED_STRING))
        {
            selectedProposal = currentLine.split(QFSession.USER_SELECTED_STRING)[1].trim();
            currentLine = reader.nextLine();
            assertPrefix(currentLine, QFSession.AFTER_COMPILATION_ERROR_STRING);
            errorsAfter = parseInt(currentLine, QFSession.AFTER_COMPILATION_ERROR_STRING, 1);
        }
        else
            assertEquality(currentLine, QFSession.TERMINATION_WITHOUT_SELECTION_STRING);
        currentLine = reader.nextLine();
        assertPrefix(currentLine, QFSession.SESSION_END_STRING);
        // Cannot parse the end date from the printed information since the formatted date loses ms information.
//        Date endDate = parseDate(currentLine, QFSession.SESSION_END_STRING, 1);
        currentLine = reader.nextLine();
        assertPrefix(currentLine, QFSession.SESSION_LENGTH_STRING);
        String sessionLengthInput = currentLine.split(QFSession.SESSION_LENGTH_STRING)[1].trim();
        long sessionLength = stringToTime(sessionLengthInput);
        // Normalization done due to conversion bug in TimeUtility.
        if (sessionLength == 0)
            sessionLength = 1000;
        else if (sessionLength < 100)
            sessionLength *= 1000;
//        assert sessionLength > 0: sessionLengthInput;
        Date endDate = Dates.add(startDate, new Date(sessionLength));
        
        // Make sure that Eclipse proposals are ordered with respect to relevance.
        Proposal [] eclipseProps = new Proposal[eclipseProposals.size()];
        for (int a = 0; a < eclipseProps.length; a++)
            eclipseProps[a] = new Proposal(eclipseProposals.get(a));
        Arrays.sort(eclipseProps);
        eclipseProposals.clear();
        for (Proposal eclipseProp: eclipseProps)
            eclipseProposals.add(eclipseProp.getDisplayString());
        
//        speculationProposals = QFSession.getGlobalBestProposals(speculationProposals, eclipseProposals);
        if (speculationProposals != null)
        {
            // Trick to also sort speculation proposals (as they were ordered)
            SpeculationProposal [] speculationProps = new SpeculationProposal[speculationProposals.size()];
            for (int a = 0; a < speculationProps.length; a++)
                speculationProps[a] = new SpeculationProposal(speculationProposals.get(a));
            Arrays.sort(speculationProps);
            speculationProposals.clear();
//            System.out.println("Speculation proposals...");
            for (Proposal speculationProp: speculationProps)
            {
                speculationProposals.add(speculationProp.getDisplayString());
//                System.out.println(speculationProp.getDisplayString());
            }
        }
        
        return new QFSession(sessionType, startDate, delayDate, speculationRunning, eclipseProposals.toArray(new String [eclipseProposals.size()]),
                speculationProposals == null ? null : speculationProposals.toArray(new String [speculationProposals
                        .size()]), errorsBefore, selectedProposal, errorsAfter, endDate, localComputationLength, analysisLength);
    }

    private void assertNonNegativeInteger(int value, String explanation)
    {
        if (value < 0)
            System.err.println(explanation + "Value = " + value + ". Happened for file = " + file_.getAbsolutePath());
    }

    private boolean checkContains(String actual, String expected)
    {
        return actual.contains(expected);
    }

    private Date parseDate(String current, String splitString, int splitIndex) throws Exception
    {
        String dateString = current.split(splitString)[splitIndex].trim();
        return DateFormat.getInstance().parse(dateString);
    }

    private int parseInt(String current, String splitString, int splitIndex) throws NumberFormatException
    {
        return Integer.parseInt(current.split(splitString)[splitIndex].trim());
    }

    private boolean checkPrefix(String actual, String expectedPrefix)
    {
        return actual.startsWith(expectedPrefix);
    }

    private boolean checkEquality(String actual, String expected)
    {
        return actual.equals(expected);
    }

    private void assertPrefix(String actual, String expectedPrefix) throws Exception
    {
        if (!checkPrefix(actual, expectedPrefix))
            throw new Exception("Expected prefix = " + expectedPrefix + ", received = " + actual);
    }

    private void assertEquality(String actual, String expected) throws Exception
    {
        if (!checkEquality(actual, expected))
            throw new Exception("Expected = " + expected + ", received = " + actual);
    }

    public EclipseSessionLog parse(String path) throws Exception
    {
        return parse(new File(path));
    }
}
