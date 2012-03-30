package edu.washington.cs.quickfix.observation.log.internal;

// represents QF analysis for one users with multiple sessions and possibly groups.
public class QFSessionAnalysis
{
//    private QFSessionGroup speculation_;
//    private QFSessionGroup observation_;
//    private QFSessionGroup unknownSpecAvailable_;
//    private QFSessionGroup unknownSpecNotAvailable_;
    
    private QFSessionGroup speculationTrue_;
    private QFSessionGroup speculationFalse_;
    private QFSessionGroup speculationObservationTrue_;
    private QFSessionGroup speculationObservationFalse_;
    private QFSessionGroup observationTrue_;
    private QFSessionGroup observationFalse_;
    
    private int totalUndos_;
    
    public QFSessionAnalysis()
    {
//        speculation_ = new QFSessionGroup();
//        observation_ = new QFSessionGroup();
//        unknownSpecAvailable_ = new QFSessionGroup();
//        unknownSpecNotAvailable_ = new QFSessionGroup();
        
        speculationTrue_ = new QFSessionGroup();
        speculationFalse_ = new QFSessionGroup();
        speculationObservationTrue_ = new QFSessionGroup();
        speculationObservationFalse_ = new QFSessionGroup();
        observationTrue_ = new QFSessionGroup();
        observationFalse_ = new QFSessionGroup();
        
        totalUndos_ = 0;
    }
    
    void analyzeUndo(String undoString)
    {
        totalUndos_++;
    }
    
    void analyzeQFSession(QFSession session)
    {
        Boolean speculationRunning = session.isSpeculationRunning();
        boolean sessionCompleted = session.isSessionCompleted();
        if (speculationRunning == null)
        {
            String [] speculationProposals = session.getSpeculationProposals();
            if (speculationProposals == null)
            {
                if (sessionCompleted)
                {
                    ObserverLogAnalyzer.writeToCSVFile(session, ObserverLogAnalyzer.OBSERVATION_TRUE);
                    observationTrue_.analyzeQFSession(session);
                }
                else
                {
                    ObserverLogAnalyzer.writeToCSVFile(session, ObserverLogAnalyzer.OBSERVATION_FALSE);
                    observationFalse_.analyzeQFSession(session);
                }
            }
            else
            {
                if (sessionCompleted)
                {
                    ObserverLogAnalyzer.writeToCSVFile(session, ObserverLogAnalyzer.SPECULATION_TRUE);
                    speculationTrue_.analyzeQFSession(session);
                }
                else
                {
                    ObserverLogAnalyzer.writeToCSVFile(session, ObserverLogAnalyzer.SPECULATION_FALSE);
                    speculationFalse_.analyzeQFSession(session);
                } 
            }
        }
        else if (speculationRunning == false)
        {
            if (sessionCompleted)
            {
                ObserverLogAnalyzer.writeToCSVFile(session, ObserverLogAnalyzer.OBSERVATION_TRUE);
                observationTrue_.analyzeQFSession(session);
            }
            else
            {
                ObserverLogAnalyzer.writeToCSVFile(session, ObserverLogAnalyzer.OBSERVATION_FALSE);
                observationFalse_.analyzeQFSession(session);
            }
        }
        else if (speculationRunning == true)
        {
            String [] speculationProposals = session.getSpeculationProposals();
            if (speculationProposals == null)
            {
                if (sessionCompleted)
                {
                    ObserverLogAnalyzer.writeToCSVFile(session, ObserverLogAnalyzer.SPECULATION_OBSERVATION_TRUE);
                    speculationObservationTrue_.analyzeQFSession(session);
                }
                else
                {
                    ObserverLogAnalyzer.writeToCSVFile(session, ObserverLogAnalyzer.SPECULATION_OBSERVATION_FALSE);
                    speculationObservationFalse_.analyzeQFSession(session);
                }
            }
            else
            {
                if (sessionCompleted)
                {
                    ObserverLogAnalyzer.writeToCSVFile(session, ObserverLogAnalyzer.SPECULATION_TRUE);
                    speculationTrue_.analyzeQFSession(session);
                }
                else
                {
                    ObserverLogAnalyzer.writeToCSVFile(session, ObserverLogAnalyzer.SPECULATION_FALSE);
                    speculationFalse_.analyzeQFSession(session);
                } 
            }
        }
    }
    
    static QFSessionAnalysis sum(QFSessionAnalysis ... analyses)
    {
        QFSessionAnalysis result = new QFSessionAnalysis();
        for (QFSessionAnalysis analysis: analyses)
        {
            result.speculationTrue_ = QFSessionGroup.sum(result.speculationTrue_, analysis.speculationTrue_);
            result.speculationFalse_ = QFSessionGroup.sum(result.speculationFalse_, analysis.speculationFalse_);
            result.speculationObservationTrue_ = QFSessionGroup.sum(result.speculationObservationTrue_, analysis.speculationObservationTrue_);
            result.speculationObservationFalse_ = QFSessionGroup.sum(result.speculationObservationFalse_, analysis.speculationObservationFalse_);
            result.observationTrue_ = QFSessionGroup.sum(result.observationTrue_, analysis.observationTrue_);
            result.observationFalse_ = QFSessionGroup.sum(result.observationFalse_, analysis.observationFalse_);
            result.totalUndos_ += analysis.totalUndos_;
        }
        return result;
    }

    void printResults(String username)
    {
        System.out.println("=== Analysis Results for user = " + username + "===");
        speculationTrue_.printResults("Speculation Completed");
        speculationFalse_.printResults("Speculation not Completed");
        speculationObservationTrue_.printResults("Speculation Observation Completed");
        speculationObservationFalse_.printResults("Speculation Observation not Completed");
        observationTrue_.printResults("Observation Completed");
        observationFalse_.printResults("Observation not Completed");

        String total = normalizeString("\tTotal");
        System.out.println("# of Undos:");
        System.out.println(total + " = " + totalUndos_);
        
        System.out.println("=========================================");
        System.out.println();        
    }
    
    private String normalizeString(String str)
    {
        return String.format("%-15s", str);
    }
}
