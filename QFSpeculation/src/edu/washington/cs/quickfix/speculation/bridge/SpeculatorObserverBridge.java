package edu.washington.cs.quickfix.speculation.bridge;

import java.util.Date;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import edu.washington.cs.hack.Hack;
import edu.washington.cs.quickfix.bridge.ISpeculatorObserverBridge;
import edu.washington.cs.quickfix.speculation.Speculator;
import edu.washington.cs.quickfix.speculation.calc.SpeculationCalculator;
import edu.washington.cs.quickfix.speculation.gui.SpeculationPreferencePage;
import edu.washington.cs.quickfix.speculation.hack.QuickFixDialogCoordinator;

public class SpeculatorObserverBridge extends Hack implements ISpeculatorObserverBridge
{
    @Override
    public String [] getCalculatedProposals(IProblemLocation [] locations)
    {
        SpeculationCalculator currentCalculator = Speculator.getSpeculator().getCurrentCalculator();
        if (currentCalculator == null)
            return null;
        else
            return QuickFixDialogCoordinator.getCoordinator().getCalculatedProposals();
    }

    @Override
    public boolean isSpeculationRunning()
    {
        return SpeculationPreferencePage.getInstance().isActivated();
    }

    @Override
    public Date getAnalysisCompletionTime()
    {
        SpeculationCalculator currentCalculator = Speculator.getSpeculator().getCurrentCalculator();
        if (currentCalculator == null)
            return null;
        else
            return currentCalculator.getAnalysisCompletionTime();
    }

    @Override
    public Date getLocalSpeculationCompletionTime()
    {
        SpeculationCalculator currentCalculator = Speculator.getSpeculator().getCurrentCalculator();
        if (currentCalculator == null)
            return null;
        else
            return currentCalculator.getLocalSpeculationCompletionTime();
    }
}
