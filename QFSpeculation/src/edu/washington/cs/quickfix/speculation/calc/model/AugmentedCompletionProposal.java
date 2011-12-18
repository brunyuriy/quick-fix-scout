package edu.washington.cs.quickfix.speculation.calc.model;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.TableItem;

import edu.washington.cs.quickfix.speculation.model.SpeculationUtility;
import edu.washington.cs.util.eclipse.model.Squiggly;
import edu.washington.cs.util.eclipse.model.CompilationErrorDetails;

public class AugmentedCompletionProposal implements Comparable <AugmentedCompletionProposal>
{
    private ICompletionProposal proposal_;
    private final int errorBefore_;
    private final Squiggly compilationError_;
    public static final int NOT_AVAILABLE = -1;
    private final Squiggly [] errorsAfter_;

    public AugmentedCompletionProposal(ICompletionProposal proposal, Squiggly compilationError, Squiggly [] errorsAfter, int errorBefore)
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
    
    public Squiggly getCompilationError()
    {
        return compilationError_;
    }
    
    public Squiggly [] getRemainingErrors()
    {
        return errorsAfter_;
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
        if (errorsAfter_ == Squiggly.NOT_COMPUTED)
            result = null;
        else if (errorsAfter_ == Squiggly.UNKNOWN)
            result = null;
        else
        {
            int noErrorsAfter = errorsAfter_.length;
            // Things are worse.
            if (noErrorsAfter > errorBefore_)
                result = new Color(item.getDisplay(), 200, 0, 0);
            // Things are better.
            else if (noErrorsAfter < errorBefore_)
                result = new Color(item.getDisplay(), 0, 200, 0);
            // No change.
            else
                result = null;
        }
        return result;
    }

    public ICompletionProposal getProposal()
    {
        return proposal_;
    }

    @Override
    public int compareTo(AugmentedCompletionProposal other)
    {
        if (isResultAvaliable())
        {
            if (other.isResultAvaliable())
                return errorsAfter_.length - other.errorsAfter_.length;
            else
                /*
                 * Here min value is returned so that comparison never swaps elements (i.e., leaves the proposal that has no
                 * information at the end).
                 */
                return Integer.MIN_VALUE;
        }
        else
        {
            if (other.isResultAvaliable())
                /*
                 * Here max value is returned so that comparison always swaps elements (i.e., puts the proposal that has no
                 * information to the end).
                 */
                return Integer.MAX_VALUE;
            else
                return 0;
        }
    }

    public String toString()
    {
        return "[AugmentedCompletionProposal: proposal = "
                + (proposal_ == null ? "null" : proposal_.getDisplayString()) + ", number of errors = " + resolveErrorsAfter() + "]";
    }
    
    public String getFinalDisplayString()
    {
        return getFinalDisplayString(false);
    }

    public String getFinalDisplayString(boolean gbp)
    {
        Squiggly ce = getCompilationError();
        CompilationErrorDetails ced = (ce == null ? null : getCompilationError().computeDetails());
        String gbpInformation = gbp ? ((ced == null ? "!" : ced.toString()) + ": ") : "";
        String prefix = "(" + resolveErrorsAfter() + ") ";
        String text = prefix + gbpInformation + (proposal_ == null ? "null" : proposal_.getDisplayString());
//        if (text.contains("Change to 'String'"))
//            text = text.replace("Change to 'String'", "Change 'string' to 'String'");
        return text;
    }
    
    public boolean isResultAvaliable()
    {
        return errorsAfter_ != Squiggly.NOT_COMPUTED && errorsAfter_ != Squiggly.UNKNOWN; 
    }
    
    private String resolveErrorsAfter()
    {
        if (errorsAfter_ == Squiggly.UNKNOWN)
            return "N/A";
        else if (errorsAfter_ == Squiggly.NOT_COMPUTED)
            return "?";
        
        return errorsAfter_.length + "";
    }

    public boolean canFix(Squiggly compilationError)
    {
        for(Squiggly errorAfter: errorsAfter_)
        {
            // Here, I cannot use the exact offset information since after the application of the proposal, the line
            // that the current proposal applied to changes. So the errors coming after that line change offset.
            if (SpeculationUtility.areOnTheSameLine(errorAfter, compilationError))
                return false;
        }
        return true;
    }
}
