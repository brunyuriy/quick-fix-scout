package edu.washington.cs.quickfix.speculation.calc.internal;

public interface SpeculativeAnalysisListener
{
    void speculativeAnalysisRoundCompleted();

    void speculativeAnalysisStarted();

    void speculativeAnalysisCompleted();
}
