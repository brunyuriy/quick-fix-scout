package edu.washington.cs.quickfix.observation.log;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;

public class ObservationProcessor implements IQuickFixProcessor
{
    /*
     * (non-Javadoc)
     * @see IAssistProcessor#getCorrections(org.eclipse.jdt.internal.ui.text.correction.IAssistContext,
     * org.eclipse.jdt.internal.ui.text.correction.IProblemLocation[])
     */
    public IJavaCompletionProposal [] getCorrections(final IInvocationContext context,
            final IProblemLocation [] locations) throws CoreException
    {
        Thread thread = new ObservationGrabber(context, locations);
        thread.start();
        return new IJavaCompletionProposal [0];
    }

    @Override
    public boolean hasCorrections(ICompilationUnit unit, int problemId)
    {
        return false;
    }
}
