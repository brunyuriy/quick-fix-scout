package edu.washington.cs.quickfix.bridge;

import java.util.Date;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import edu.washington.cs.hack.IHack;

public interface ISpeculatorObserverBridge extends IHack
{
    Date getAnalysisCompletionTime();
    Date getLocalSpeculationCompletionTime();
    String [] getCalculatedProposals(IProblemLocation [] location);
    Boolean isSpeculationRunning();
}
