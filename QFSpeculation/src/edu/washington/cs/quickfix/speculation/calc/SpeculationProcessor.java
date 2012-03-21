package edu.washington.cs.quickfix.speculation.calc;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;

import edu.washington.cs.quickfix.speculation.Speculator;

public class SpeculationProcessor implements IQuickFixProcessor
{
    /*
     * (non-Javadoc)
     * @see IAssistProcessor#getCorrections(org.eclipse.jdt.internal.ui.text.correction.IAssistContext,
     * org.eclipse.jdt.internal.ui.text.correction.IProblemLocation[])
     */
    public IJavaCompletionProposal [] getCorrections(IInvocationContext context, IProblemLocation [] locations)
            throws CoreException
    {
        Speculator.getSpeculator().quickFixInvoked();
        Thread thread = new SpeculationGrabber(context, locations);
        thread.start();
        return new IJavaCompletionProposal [0];
    }

    @Override
    public boolean hasCorrections(ICompilationUnit unit, int problemId)
    {
        return false;
    }
}
