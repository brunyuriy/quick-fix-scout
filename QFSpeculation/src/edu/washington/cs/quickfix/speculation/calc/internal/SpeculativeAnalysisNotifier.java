package edu.washington.cs.quickfix.speculation.calc.internal;

public interface SpeculativeAnalysisNotifier
{
    void signalSpeculativeAnalysisRoundComplete();

    void signalSpeculativeAnalysisStart();

    void signalSpeculativeAnalysisComplete();

    void addListener(SpeculativeAnalysisListener listener);

    void removeListener(SpeculativeAnalysisListener listener);
}
