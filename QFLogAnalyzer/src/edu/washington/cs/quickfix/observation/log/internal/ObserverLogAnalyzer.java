package edu.washington.cs.quickfix.observation.log.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;

public class ObserverLogAnalyzer
{
    private static HashMap<String, Formatter> writers_ = new HashMap <String, Formatter>();
    private static HashMap <String, Formatter> userWriters_ = new HashMap <String, Formatter>();
    private static String currentUser_ = null;
    
    static final String OBSERVATION_TRUE = "_observation_completed";
    static final String OBSERVATION_FALSE = "_observation_not_completed";
    static final String SPECULATION_TRUE = "_speculation_completed";
    static final String SPECULATION_FALSE = "_speculation_not_completed";
    static final String SPECULATION_OBSERVATION_TRUE = "_speculation_observation_completed";
    static final String SPECULATION_OBSERVATION_FALSE = "_speculation_observation_not_completed";
    
//    static final String [] ALL_TYPES = new String []{OBSERVATION_TRUE, OBSERVATION_FALSE, SPECULATION_TRUE, SPECULATION_FALSE, SPECULATION_OBSERVATION_TRUE, SPECULATION_OBSERVATION_FALSE};
    static final String [] ALL_TYPES = new String []{"_all"};
    
    private final static File root = new File("results");
    
    private final static boolean GENERATE_FOR_SQL = false;
    private final static String SEPERATION = GENERATE_FOR_SQL ? "!S!" : ",";
    private final static String NOT_AVAILABLE = GENERATE_FOR_SQL ? "" : "N/A";
    
    public static void main(String [] args) throws ParseException, FileNotFoundException
    {
        System.out.println("Number of arguments = " + args.length);
        for (String arg: args)
            System.out.println(arg);
//        args = analyzeCaseStudy();
        args = analyzeExperiment();
//        if (args.length != 1)
//            printUsage();
        if (GENERATE_FOR_SQL)
        {
            String identifier = "";
            Formatter writer = new Formatter(new File(root, "all" + identifier + ".csv"));
            writeOpening(writer);
            writers_.put(identifier, writer);
        }
        else
        {
            for (String identifier: ALL_TYPES)
            {
                Formatter writer = new Formatter(new File(root, "all" + identifier + ".csv"));
                writeOpening(writer);
                writers_.put(identifier, writer);
            }
        }
        ObserverLogAnalyzer logAnalyzer = new ObserverLogAnalyzer();
        for (String arg: args)
            logAnalyzer.analyzeDirectory(arg);
        logAnalyzer.printAnalysisResults();
        for (Formatter writer: userWriters_.values())
            writer.close();
        for (Formatter writer: writers_.values())
            writer.close();
        
        System.out.println("Could not map " + QFSessionLogParser.missingUndoes_ + " undoes.");
    }

    private static String [] analyzeExperiment()
    {
        String fs = File.separator;
        String home = System.getProperty("user.home");
        // C:\Users\Kivanc\Dropbox\Cloud\Research-Papers\Quick_Fix_Scout\QuickFixScout\Controlled Study\Experiment Data
        File logDir = new File(home + fs + "Dropbox" + fs + "Cloud" + fs + "Research-Papers" + fs + "Quick_Fix_Scout" + fs + "QuickFixScout" + fs + "Controlled Experiment" + fs + "Experiment Data");
        String [] result = new String[1];
        result[0] = logDir.getAbsolutePath();
//        result[0] = logDir.getAbsolutePath() + fs + "2012.03.05 - Colin";
        return result;
    }

    private static String [] analyzeCaseStudy()
    {
        String fs = File.separator;
        String home = System.getProperty("user.home");
        File logDir = new File(home + fs + "Dropbox" + fs + "Cloud" + fs + "Research-Papers" + fs + "Quick_Fix_Scout" + fs + "QuickFixScout" + fs + "Case Study");
        String [] result = new String[3];
        result[0] = logDir.getAbsolutePath() + fs +  "2012.01.31_QF_Logs_Processed";
        result[1] = logDir.getAbsolutePath() + fs +  "QF_Logs_Old_Processed";
        result[2] = logDir.getAbsolutePath() + fs +  "2012.03.31_QF_Logs_Processed";
        
//        result = new String[1];
//        result[0] = "test/";
//        result[0] = "/Users/kivanc/Dropbox/Cloud/Research-Papers/Quick_Fix_Scout/QuickFixScout/log_data/2012.01.31_QF_Logs_Processed/Kivanc"; 
        return result;
    }

    public static void printUsage()
    {
        System.err.println("Usage: java -jar observer-log-analyzer.jar <directory that contains logs>");
        System.err.println("Analysis is done recursively in the given directory.");
        System.exit(-1);
    }

    static void initCSVFile(String username) throws FileNotFoundException
    {
        if (!userWriters_.containsKey(username))
        {
            if (!GENERATE_FOR_SQL)
//            {
//                String identifier = "";
//                Formatter writer = new Formatter(new File(root, username + identifier + ".csv"));
//                writeOpening(writer);
//                userWriters_.put(username + identifier, writer);
//            }
//            else
            {
                for (String identifier: ALL_TYPES)
                {
                    Formatter writer = new Formatter(new File(root, username + identifier + ".csv"));
                    writeOpening(writer);
                    userWriters_.put(username + identifier, writer);
                }
            }
        }
        currentUser_ = username;
    }

    private static void writeOpening(Formatter writer)
    {
        if (!GENERATE_FOR_SQL)
        {
            //@formatter:off
            String header = 
                    "Username" + SEPERATION + 
                    "Delay (MS)" + SEPERATION + 
                    "Session Length (MS)" + SEPERATION + 
                    "QF Project" + SEPERATION + 
                    "QF File" + SEPERATION + 
                    "# of Proposals Offered" + SEPERATION + 
                    "Selected Proposal" + SEPERATION + 
                    "Selected Proposal Type" + SEPERATION + 
                    "# of CE Before" + SEPERATION + 
                    "# of CE after" + SEPERATION + 
                    "Session Completed" + SEPERATION + 
                    "Speculation Running" + SEPERATION + 
                    "FP Selected" + SEPERATION + 
                    "BP Selected" + SEPERATION +
                    "GBP Selected" + SEPERATION +
                    "GBP Generated" + SEPERATION + 
                    "Undone";
            //@formatter:on
            int limit = 5;
            int selectionLimit = 5;
            for (int a = 0; a < limit; a++)
                header += SEPERATION + (a + 1) + "th EP";
            for (int a = 0; a < limit; a++)
                header += SEPERATION + (a + 1) + "th EP Type";
            for (int a = 0; a < selectionLimit; a++)
                header += SEPERATION + (a+1) + "th EP Selected";
            for (int a = 0; a < limit; a++)
                header += SEPERATION + (a + 1) + "th SP";
            for (int a = 0; a < limit; a++)
                header += SEPERATION + (a + 1) + "th SP Type";
            for (int a = 0; a < selectionLimit; a++)
                header += SEPERATION + (a+1) + "th SP Selected";
            
            header += SEPERATION + "Treatment" + SEPERATION + "Task";
            
            write(header, writer);
        }
    }

    private static void write(String message, Formatter writer)
    {
        writer.format("%s%n", message);
    }

    /*
     * Data format:
     * Username
     * Delay (MS), Session Length (MS)
     * # of proposals offered (by Eclipse)
     * Selected proposal, Selected proposal type
     * # of compilation errors before, of compilation errors after
     * Session Completed
     * Speculation Running
     * FP Selected, BP Selected, GBP selected, GBP Generated
     * Undone
     */
    static void writeToCSVFile(QFSession session, String identifier)
    {
        String username = currentUser_;
        String [] parts = currentUser_.split("-");
        String treatment = "";
        String taskType = "";
        if (parts.length == 3)
        {
            username = parts[0];
            treatment = parts[1];
            taskType = parts[2];
        }
        
        Date delay = session.getDelayTime();
        int compilationBefore = session.getErrorsBefore();
        boolean completed = session.isSessionCompleted();
        String selectedProposal = NOT_AVAILABLE;
        Proposal selected = Proposal.NONE;
        String compilationAfterS = NOT_AVAILABLE;
        if (completed)
        {
            selectedProposal = session.getSelectedProposalString();
            selected = new Proposal(selectedProposal);
            compilationAfterS = session.getErrorsAfter() + "";
        }
        String fpString = representBoolean(session.isFirstProposalSelected().toBoolean());
        String [] eclipseProposals = session.getEclipseProposals();
        int numberOfProposalsOffered = eclipseProposals.length;
        Proposal [] eclipseProps = computeProposal(eclipseProposals);
        String [] speculationProposals = session.getSpeculationProposals();
        SpeculationProposal [] speculationProps = null;
        String bpString = NOT_AVAILABLE;
        String gbpString = NOT_AVAILABLE;
        String gbpGenerated = NOT_AVAILABLE;
        if (speculationProposals != null && speculationProposals.length != 0)
        {
            speculationProps = computeSpeculationProposal(speculationProposals);
            speculationProposals = filterSpeculationText(speculationProposals);
            bpString = representBoolean(session.isBestProposalSelected().toBoolean());
            gbpString = representBoolean(session.isGlobalBestProposalSelected().toBoolean());
            gbpGenerated = representBoolean(session.isGlobalBestProposalGenerated()); 
        }
//        System.out.println("gbpString = " + gbpString);
        Boolean speculationRunning = session.isSpeculationRunning();
        String speculationRunningS = representBoolean(speculationRunning);
        Date length = session.getLength();
        String delayS = "" + delay.getTime();
        if (delay.getTime() < 100)
            delayS = "";
        String lengthS = "" + length.getTime();
        if (length.getTime() < 100)
            lengthS = "";
        String completedS = representBoolean(completed);
        
        String qfProject = session.getQFProject();
        String qfFile = session.getQFFile();
        String qfProjectS = qfProject == null ? "N/A" : qfProject;
        String qfFileS = qfFile == null ? "N/A" : qfFile;
        
        //@formatter:off
        String data = 
                username + SEPERATION + 
                delayS + SEPERATION + 
                lengthS + SEPERATION +
                qfProjectS + SEPERATION + 
                qfFileS + SEPERATION + 
                numberOfProposalsOffered + SEPERATION + 
                escape(selectedProposal) + SEPERATION +
                escape(selected.getType().toString()) + SEPERATION + 
                compilationBefore + SEPERATION + 
                compilationAfterS + SEPERATION + 
                completedS + SEPERATION + 
                speculationRunningS + SEPERATION + 
                fpString + SEPERATION + 
                bpString + SEPERATION + 
                gbpString + SEPERATION + 
                gbpGenerated + SEPERATION + 
                toBit(session.isUndone());
        //@formatter:on
        int limit = GENERATE_FOR_SQL ? 5 : 5;
        int selectionLimit = GENERATE_FOR_SQL ? 5 : 5;
        for (int a = 0; a < limit; a++)
        {
            String eclipseProposal = eclipseProposals.length > a ? eclipseProposals[a] : NOT_AVAILABLE;
            data += SEPERATION + escape(eclipseProposal);
        }
        for (int a = 0; a < limit; a++)
        {
            String eclipseProposalType = eclipseProps.length > a ? eclipseProps[a].getType().toString() : NOT_AVAILABLE;
            data += SEPERATION + escape(eclipseProposalType);
        }
        for (int a = 0; a < selectionLimit; a++)
        {
            Boolean result = eclipseProposals.length > a ? (eclipseProposals[a].equals(selectedProposal)) : null;
            String cellValue = representBoolean(result);
            data += SEPERATION + cellValue;
        }
        for (int a = 0; a < limit; a++)
        {
            String speculationProposal = (speculationProposals != null && speculationProposals.length > a) ? speculationProposals[a]
                    : NOT_AVAILABLE;
            data += SEPERATION + escape(speculationProposal);
        }
        for (int a = 0; a < limit; a++)
        {
            String speculationProposalType = (speculationProps != null && speculationProps.length > a) ? speculationProps[a].getType()
                    .toString() : NOT_AVAILABLE;
            data += SEPERATION + escape(speculationProposalType);
        }
        for (int a = 0; a < selectionLimit; a++)
        {
            Boolean result = (speculationProposals != null && speculationProposals.length > a) ? containsProposal(speculationProposals[a], selectedProposal) : null;
            String cellValue = representBoolean(result);
            data += SEPERATION + cellValue;
        }
//        for (int a = 0; a < selectionLimit; a++)
//        {
//            Integer remainingErrors = (speculationProps != null && speculationProps.length > a) ? speculationProps[a].getRemainingErrors(): null;
//            String cellValue = representInteger(remainingErrors);
//            data += SEPERATION + cellValue;
//        }
//        for (int a = 0; a < selectionLimit; a++)
//        {
//            Integer relevance = (speculationProps != null && speculationProps.length > a) ? speculationProps[a].getRelevance(): null;
//            String cellValue = representInteger(relevance);
//            data += SEPERATION + cellValue;
//        }
        
        if (parts.length == 3)
        {
            data += SEPERATION + treatment;
            data += SEPERATION + taskType;
        }
        
        identifier = GENERATE_FOR_SQL ? "" : identifier;
        if (!GENERATE_FOR_SQL)
            write(data, getCurrentWriter(identifier));
        write(data, getAllWriter(identifier));
        
    }
    
    private static String representInteger(Integer value)
    {
        return value == null ? NOT_AVAILABLE : value + "";
    }

    private static SpeculationProposal [] computeSpeculationProposal(String [] speculationProposals)
    {
        SpeculationProposal [] result = new SpeculationProposal[speculationProposals.length];
        for (int a = 0; a < speculationProposals.length; a++)
            result[a] = new SpeculationProposal(speculationProposals[a]);
        return result;
    }

    private static boolean containsProposal(String source, String target)
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
    
    private static String representBoolean(Boolean value)
    {
        return value == null ? NOT_AVAILABLE : (GENERATE_FOR_SQL ? toBit(value) + "" : value + ""); 
    }

    private static String toBit(boolean value)
    {
    	if (GENERATE_FOR_SQL)
    		return value ? "1" : "0";
    	return "" + value;
    }

    private static Formatter getAllWriter(String identifier)
    {
    	if (ALL_TYPES.length == 1)
    		identifier = "_all";
    	
        return writers_.get(identifier);
    }

    private static Formatter getCurrentWriter(String identifier)
    {
    	if (ALL_TYPES.length == 1)
    		identifier = "_all";
    	
        Formatter result = userWriters_.get(currentUser_ + identifier);
        assert result != null: identifier;
        return result;
    }

    private static String [] filterSpeculationText(String [] proposals)
    {
        String [] result = new String [proposals.length];
        for (int a = 0; a < proposals.length; a++)
        {
            String proposal = proposals[a];
            int index = proposal.indexOf(")") + 1;
            result[a] = proposal.substring(index).trim();
        }
        return result;
    }

    private static Proposal [] computeProposal(String [] eclipseProposals)
    {
        Proposal [] result = new Proposal[eclipseProposals.length];
        for (int a = 0; a < eclipseProposals.length; a++)
            result[a] = new Proposal(eclipseProposals[a]);
        return result;
    }

    private static ProposalType determineType(String proposal)
    {
        return ProposalType.parse(proposal);
    }

    private static String escape(String input)
    {
        if (GENERATE_FOR_SQL)
            return input;
        return "\"" + input + "\"";
        // return input.replace(",", "\\,");
    }

    private final QFSessionLogParser parser_;
    private final HashMap <String, ArrayList <QFSessionAnalysis>> analyses_;
    private File topDir_;

    public ObserverLogAnalyzer()
    {
        parser_ = new QFSessionLogParser();
        analyses_ = new HashMap <String, ArrayList <QFSessionAnalysis>>();
    }

    public void analyzeDirectory(String dirPath) throws FileNotFoundException
    {
        File directory = new File(dirPath);
        topDir_ = directory;
        analyzeDirectory(directory);
    }

    private void analyzeDirectory(File directory) throws FileNotFoundException
    {
         System.out.println("Analyzing directory = " + directory.getAbsolutePath());
        for (File file: directory.listFiles())
            analyze(file, findParent(directory));
    }

    private File findParent(File directory)
    {
        if (directory.getAbsolutePath().equals(topDir_.getAbsolutePath()))
            return directory;
        if (!directory.getName().equals("Quick_Fix_Usage") && !(directory.getName().split("\\.").length >= 2))
            return directory; 
//        if (directory.getParentFile().getAbsolutePath().equals(topDir_.getAbsolutePath()))
//            return directory;
        else
            return findParent(directory.getParentFile());
    }

    private void analyze(File file, File parent) throws FileNotFoundException
    {
        if (file.isDirectory())
            analyzeDirectory(file);
        else
        {
            String fileName = file.getName();
            if (fileName.contains("usage") && fileName.endsWith(".txt"))
            {
                if (!parent.getName().equals(currentUser_))
                    initCSVFile(parent.getName());
                System.out.println("Parsing file = " + file.getAbsolutePath());
                try
                {
                    EclipseSessionLog currentLog = parser_.parse(file);
                    System.out.println("File parsed with success...\n");
                    QFSessionAnalysis analysis = convertSessionLogToAnalysis(currentLog);
                    addAnalysis(parent.getName(), analysis);
                }
                catch (Exception e)
                {
                    System.out.println("Cannot parse file, check the formatting.");
                    System.out.println(e.getMessage());
                    for (StackTraceElement traceElement: e.getStackTrace())
                        System.out.println(traceElement.toString());
                    System.out.println();
                }
            }
            // else
            // System.out.println("Skipping file = " + file.getAbsolutePath() + " since it is not a usage log." );
        }
    }

    private void addAnalysis(String user, QFSessionAnalysis analysis)
    {
        ArrayList <QFSessionAnalysis> analyses = analyses_.get(user);
        if (analyses == null)
        {
            analyses = new ArrayList <QFSessionAnalysis>();
            analyses_.put(user, analyses);
        }
        analyses.add(analysis);
    }
    
    private void printAnalysisResults()
    {
        HashMap <String, QFSessionAnalysis> analysesFlattened = new HashMap <String, QFSessionAnalysis>();
        for (String user: analyses_.keySet())
        {
            ArrayList <QFSessionAnalysis> analyses = analyses_.get(user);
            analysesFlattened.put(user, QFSessionAnalysis.sum(analyses.toArray(new QFSessionAnalysis[analyses.size()])));
            analysesFlattened.get(user).printResults(user);
        }
        Collection <QFSessionAnalysis> userAnalyses = analysesFlattened.values();
        QFSessionAnalysis all = QFSessionAnalysis.sum(userAnalyses.toArray(new QFSessionAnalysis[userAnalyses.size()]));
        all.printResults("All");
    }

    private QFSessionAnalysis convertSessionLogToAnalysis(EclipseSessionLog sessionLog)
    {
        QFSessionAnalysis result = new QFSessionAnalysis();
        for (Object logEvent: sessionLog.getEvents())
        {
            if (logEvent instanceof QFSession)
                result.analyzeQFSession((QFSession) logEvent);
            else if (logEvent instanceof String)
                result.analyzeUndo((String) logEvent);
        }
        return result;
    }
}
