package edu.washington.cs.quickfix.speculation.calc.model;

public interface SpeculativeAnalysisListener
{
    void speculativeAnalysisRoundCompleted();

    void speculativeAnalysisStarted();

    void speculativeAnalysisCompleted();
}
