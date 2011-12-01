package edu.washington.cs.quickfix.speculation.calc.internal;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.TableItem;

import edu.washington.cs.util.eclipse.model.CompilationError;

public class AugmentedCompletionProposal implements Comparable <AugmentedCompletionProposal>
{
    private final ICompletionProposal proposal_;
    private final int error_;
    private final int errorBefore_;
    private String displayString_;
    private final CompilationError compilationError_;
    public static final int NOT_AVAILABLE = -1;

    public AugmentedCompletionProposal(ICompletionProposal proposal, CompilationError compilationError, int error, int errorBefore)
    {
        proposal_ = proposal;
        error_ = error;
        errorBefore_ = errorBefore;
        compilationError_ = compilationError;
    }
    
    public IFile getFile()
    {
        return (IFile) compilationError_.getResource();
    }
    
    public IProblemLocation getLocation()
    {
        return compilationError_.getLocation();
    }

    public int getError()
    {
        return error_;
    }

    public int getErrorBefore()
    {
        return errorBefore_;
    }

    public String getDisplayString()
    {
        if (proposal_ == null)
            return "";
        return proposal_.getDisplayString();
    }

    /**
     * Table item must be 'nonnull'. The caller thread must be a UI thread (has access to change table item
     * information).
     */
    public void setYourselfAsTableItem(TableItem item)
    {
        String text = getFinalDisplayString();
        item.setData(proposal_);
        item.setImage(proposal_.getImage());
        item.setText(text);
        Color foregroundColor = decideColor(item);
        if (foregroundColor != null)
            item.setForeground(foregroundColor);
    }

    private Color decideColor(TableItem item)
    {
        Color result = null;
        if (error_ > errorBefore_)
            result = new Color(item.getDisplay(), 200, 0, 0);
        else if (error_ < errorBefore_)
            result = new Color(item.getDisplay(), 0, 200, 0);
        // Should override the previous decision if any, so there is no 'else'.
        if (error_ == NOT_AVAILABLE)
            result = null;
        return result;
    }

    public ICompletionProposal getProposal()
    {
        return proposal_;
    }

    @Override
    public int compareTo(AugmentedCompletionProposal other)
    {
        if (error_ == NOT_AVAILABLE)
        {
            if (other.error_ == NOT_AVAILABLE)
                return 0;
            /*
             * Here max value is returned so that comparison always swaps elements (i.e., puts the proposal that has no
             * information to the end).
             */
            return Integer.MAX_VALUE;
        }
        else if (other.error_ == NOT_AVAILABLE)
            /*
             * Here min value is returned so that comparison never swaps elements (i.e., leaves the proposal that has no
             * information at the end).
             */
            return Integer.MIN_VALUE;
        else
            return error_ - other.error_;
    }

    public String toString()
    {
        return "[AugmentedCompletionProposal: proposal = "
                + (proposal_ == null ? "null" : proposal_.getDisplayString()) + ", number of errors = " + error_ + "]";
    }

    public String getFinalDisplayString()
    {
        String prefix = (error_ == NOT_AVAILABLE ? "(N/A)" : ("(" + error_ + ")")) + " ";
        String text = prefix + (proposal_ == null ? "null" : proposal_.getDisplayString());
        return text;
    }

    public String toDebugString()
    {
        return "[AugmentedCompletionProposal: proposal = " + displayString_ + ", number of errors = " + error_ + "]";
    }
}
