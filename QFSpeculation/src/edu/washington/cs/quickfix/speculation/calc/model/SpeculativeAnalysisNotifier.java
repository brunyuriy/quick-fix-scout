package edu.washington.cs.quickfix.speculation.calc.model;

public interface SpeculativeAnalysisNotifier
{
    void signalSpeculativeAnalysisRoundComplete();

    void signalSpeculativeAnalysisStart();

    void signalSpeculativeAnalysisComplete();

    void addListener(SpeculativeAnalysisListener listener);

    void removeListener(SpeculativeAnalysisListener listener);
}
