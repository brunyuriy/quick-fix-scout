package edu.washington.cs.quickfix.speculation.calc.model;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.TableItem;

import edu.washington.cs.quickfix.speculation.model.SpeculationUtility;
import edu.washington.cs.util.eclipse.model.CompilationError;
import edu.washington.cs.util.eclipse.model.CompilationErrorDetails;

public class AugmentedCompletionProposal implements Comparable <AugmentedCompletionProposal>
{
    private ICompletionProposal proposal_;
    private final int errorBefore_;
    private final CompilationError compilationError_;
    public static final int NOT_AVAILABLE = -1;
    private final CompilationError [] errorsAfter_;

    public AugmentedCompletionProposal(ICompletionProposal proposal, CompilationError compilationError, CompilationError [] errorsAfter, int errorBefore)
    {
        proposal_ = proposal;
        errorBefore_ = errorBefore;
        compilationError_ = compilationError;
        errorsAfter_ = errorsAfter;
    }
    
    public void setProposal(ICompletionProposal proposal)
    {
        proposal_ = proposal;
    }
    
    public CompilationError getCompilationError()
    {
        return compilationError_;
    }
    
    public CompilationError [] getRemainingErrors()
    {
        return errorsAfter_;
    }
    
    public int getErrorAfter()
    {
        return errorsAfter_ == CompilationError.UNKNOWN ? NOT_AVAILABLE : errorsAfter_.length;
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
    
    public int hashCode()
    {
        return getDisplayString().hashCode();
    }
    
    public boolean equals(Object other)
    {
        if (other instanceof AugmentedCompletionProposal)
            return equals((AugmentedCompletionProposal) other);
        return false;
    }
    
    public boolean equals(AugmentedCompletionProposal other)
    {
        return getDisplayString().equals(other.getDisplayString());
    }

    /**
     * Table item must be 'nonnull'. The caller thread must be a UI thread (has access to change table item
     * information).
     */
    public void setYourselfAsTableItem(TableItem item, boolean gbp)
    {
        String text = getFinalDisplayString(gbp);
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
        if (getErrorAfter() > errorBefore_)
            result = new Color(item.getDisplay(), 200, 0, 0);
        else if (getErrorAfter() < errorBefore_)
            result = new Color(item.getDisplay(), 0, 200, 0);
        // Should override the previous decision if any, so there is no 'else'.
        if (getErrorAfter() == NOT_AVAILABLE)
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
        if (getErrorAfter() == NOT_AVAILABLE)
        {
            if (other.getErrorAfter() == NOT_AVAILABLE)
                return 0;
            /*
             * Here max value is returned so that comparison always swaps elements (i.e., puts the proposal that has no
             * information to the end).
             */
            return Integer.MAX_VALUE;
        }
        else if (other.getErrorAfter() == NOT_AVAILABLE)
            /*
             * Here min value is returned so that comparison never swaps elements (i.e., leaves the proposal that has no
             * information at the end).
             */
            return Integer.MIN_VALUE;
        else
            return getErrorAfter() - other.getErrorAfter();
    }

    public String toString()
    {
        return "[AugmentedCompletionProposal: proposal = "
                + (proposal_ == null ? "null" : proposal_.getDisplayString()) + ", number of errors = " + getErrorAfter() + "]";
    }
    
    public String getFinalDisplayString()
    {
        return getFinalDisplayString(false);
    }

    public String getFinalDisplayString(boolean gbp)
    {
        CompilationError ce = getCompilationError();
        CompilationErrorDetails ced = (ce == null ? null : getCompilationError().computeDetails());
        String gbpInformation = gbp ? ((ced == null ? "!" : ced.toString()) + ": ") : "";
        String prefix = (getErrorAfter() == NOT_AVAILABLE ? "(N/A)" : ("(" + getErrorAfter() + ")")) + " ";
        String text = prefix + gbpInformation + (proposal_ == null ? "null" : proposal_.getDisplayString());
//        if (text.contains("Change to 'String'"))
//            text = text.replace("Change to 'String'", "Change 'string' to 'String'");
        return text;
    }

    public boolean canFix(CompilationError compilationError)
    {
        for(CompilationError errorAfter: errorsAfter_)
        {
            // Here, I cannot use the exact offset information since after the application of the proposal, the line
            // that the current proposal applied to changes. So the errors coming after that line change offset.
            if (SpeculationUtility.areOnTheSameLine(errorAfter, compilationError))
                return false;
        }
        return true;
    }
}
