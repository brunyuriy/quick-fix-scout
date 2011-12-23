package edu.washington.cs.quickfix.speculation.calc.model;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.TableItem;

import edu.washington.cs.quickfix.speculation.model.SpeculationUtility;
import edu.washington.cs.util.eclipse.model.Squiggly;
import edu.washington.cs.util.eclipse.model.SquigglyDetails;

public class AugmentedCompletionProposal implements Comparable <AugmentedCompletionProposal>
{
    private ICompletionProposal proposal_;
    private final int errorBefore_;
    private final Squiggly compilationError_;
    public static final int NOT_AVAILABLE = -1;
    private final Squiggly [] errorsAfter_;
    
    private final static Logger logger_ = Logger.getLogger(AugmentedCompletionProposal.class.getName());
    static
    {
        logger_.setLevel(Level.INFO);
    }

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
        String gbpInformation = getGBPInformation(gbp);
        String prefix = getPrefix();
        String displayString = getDisplayStringWithContext(gbp);
        String text = prefix + gbpInformation + displayString;
        return text;
    }
    
    private String getGBPInformation(boolean gbp)
    {
        Squiggly ce = getCompilationError();
        SquigglyDetails ced = (ce == null ? null : getCompilationError().computeDetails());
        String result = gbp ? ((ced == null ? "!" : ced.toString()) + ": ") : "";
        return result;
    }

    private String getPrefix()
    {
        return "(" + resolveErrorsAfter() + ") ";
    }

    private String getDisplayStringWithContext(boolean gbp)
    {
        if (proposal_ == null)
            return "null";
        
        String result = proposal_.getDisplayString();
        if (gbp)
        {
            try
            {
                result = contexify(result);
            }
            catch (JavaModelException e)
            {
                logger_.log(Level.SEVERE, "Cannot contexify proposal string for = " + result + " due to a java model exception.", e);
            }
            catch (BadLocationException e)
            {
                logger_.log(Level.SEVERE, "Cannot contexify proposal string for = " + result + " due to a bad location exception.", e);
            }
        }
        return result;
    }

    private String contexify(String result) throws JavaModelException, BadLocationException
    {
        logger_.info("Contexifying global best proposal: " + result);
        
        // The proposals that does not need extra context first...
        // 'package' proposals
        if (result.startsWith("Remove package declaration "))
            return result;
        if (result.startsWith("Add package declaration "))
            return result;
        if (result.startsWith("Move ") && result.contains(".java' to "))
            return result;
        if (result.startsWith("Move ") && result.endsWith(".java' to the default package"))
            return result;
        if (result.startsWith("Change package declaration to "))
            return result;

        // 'import' proposals
        if (result.equals("Organize imports"))
            return result;
        
        // 'type' proposals
        if (result.startsWith("Create class "))
            return result;
        if (result.startsWith("Create interface "))
            return result;
        if (result.startsWith("Create enum "))
            return result;
        if (result.startsWith("Create annotation "))
            return result;
        if (result.startsWith("Import "))
            return result;
        if (result.startsWith("Add type parameter ") && result.contains(" to "))
            return result;
        if (result.startsWith("Remove type "))
            return result;

        // 'constructor' proposals
        if (result.startsWith("Create constructor "))
            return result;
        if (result.startsWith("Make type ") && result.endsWith(" abstract"))
            return result;
        if (result.startsWith("Change constructor ") && result.contains(" to "))
            return result;
        if (result.startsWith("Change constructor ") && result.contains(": Remove parameter "))
            return result;
        if (result.startsWith("Change constructor ") && result.contains(": Remove parameters "))
            return result;
        if (result.startsWith("Add constructor "))
            return result;
        if (result.startsWith("Remove constructor "))
            return result;
        
        // 'method' proposals
        if (result.startsWith("Remove ") && result.contains(", keep side-effect assignments"))
            return result;
        if (result.startsWith("Create method "))
            return result;
        if (result.startsWith("Change method ") && result.contains(" to "))
            return result;
        if (result.startsWith("Change method ") && result.contains(": Remove parameter "))
            return result;
        if (result.startsWith("Change method ") && result.contains(": Add parameter "))
            return result;
        if (result.startsWith("Change method ") && result.contains(": Swap parameters "))
            return result;
        if (result.startsWith("Change visibility of ") && result.contains(" to "))
            return result;
        if (result.startsWith("Remove method "))
            return result;
        if (result.startsWith("Remove 'static' modifier of "))
            return result;
        
        // 'build path problem' proposals
        if (result.equals("Fix project setup..."))
            return result;
        
        // 'other' proposals
        if (result.startsWith("Add @SuppressWarnings") && result.contains(" to "))
            return result;
        if (result.startsWith("Add cast to "))
            return result;
        if (result.startsWith("Add type arguments to "))
            return result;
        
        // 'field and variable' proposals
        if (result.startsWith("Create getter and setter for "))
            return result;
        if (result.startsWith("Create field "))
            return result;
        if (result.startsWith("Create constant "))
            return result;
        if (result.startsWith("Initialize variable "))
            return result;
        
        // 'unknown' proposals
        if (result.startsWith("Rename method "))
            return result;
        if (result.startsWith("Rename field "))
            return result;
        if (result.startsWith("Remove ") && result.endsWith(" and all assignments"))
            return result;
        if (result.equals("Add default serial version ID"))
            return result;
        if (result.equals("Add generated serial version ID"))
            return result;
        if (result.startsWith("Remove exceptions from "))
            return result;
        if (result.startsWith("Add exceptions to "))
            return result;
        
        // The proposals that I am not sure what to do. For now, I am not adding context to them.
        // 'exception handling' proposals
        if (result.equals("Surround with try/catch"))
            return result;
        if (result.equals("Add catch clause to surrounding try"))
            return result;
        if (result.equals("Replace catch clause with throws"))
            return result;
        if (result.equals("Remove catch clause"))
            return result;
        
        // 'unknown' proposals
        if (result.equals("Remove assignment"))
            return result;
        if (result.equals("Infer Generic Type Arguments..."))
            return result;
        if (result.startsWith("Remove ") && result.endsWith(" token"))
            return result;
        if (result.startsWith("Change access to using static ") && result.endsWith(" (declaring type)"))
            return result;
        
        // The proposals that need context information.
        String context = compilationError_.getContext();
        String cuName = compilationError_.getResource().getName();
        // 'import' proposals
        if (result.equals("Remove unused import"))
            return result + " '" + context + "'";
        
        // 'type' proposals
        // This one also applies to 'field and variable' proposals.
        if (result.startsWith("Change to "))
        {
            // This one also applied to 'method' proposals (method calls)
            if (result.contains("(..)"))
                return result.replace("Change to ", "Change '" + context + "(..)' to ");
            else
                return result.replace("Change to ", "Change '" + context + "' to ");
        }
        if (result.startsWith("Rename type to "))
            return result.replace("Rename type to ", "Rename '" + context + "' to ");
        if (result.startsWith("Rename compilation unit to "))
            return result.replace("Rename compilation unit to ", "Rename '" + cuName + ".java' to ");
        
        // 'constructor' proposals (the first four proposals are also applicable to method proposals).
        if (result.startsWith("Remove argument to match "))
            return result.replace("Remove argument to match ", "Remove argument from '" + context + "(..)' to match ");
        if (result.startsWith("Remove arguments to match "))
            return result.replace("Remove arguments to match ", "Remove arguments from '" + context + "(..)' to match ");
        if (result.startsWith("Add argument to match "))
            return result.replace("Add argument to match ", "Add argument from '" + context + "(..)' to match ");
        if (result.startsWith("Add arguments to match "))
            return result.replace("Add arguments to match ", "Add arguments from '" + context + "(..)' to match ");
        if (result.equals("Add unimplemented methods"))
            return result + " to '" + context + "'";
        
        // 'method' proposals
        if (result.startsWith("Swap arguments ") && result.contains(" and "))
            return "For method call: '" + context + "(..)' " + result.replace("Swap arguments ", "swap arguments ");
        if (result.equals("Add body"))
            return result + " to '" + context + "'";
        if (result.equals("Add abstract modifier"))
            return result + " to '" + context + "'";
        if (result.equals("Add return statement"))
            return result + " to '" + context + "'";
        if (result.startsWith("Change return type to "))
            return result.replace("Change return type to ", "Change return type of '" + context + "' to");

        // 'exception handling' proposals
            
        // 'field and variable' proposals
        
        // 'unknown' proposals
        if (result.startsWith("Remove ") && result.endsWith(" annotation"))
            return result + " from '" + context + "'"; 
        if (result.equals("Remove invalid modifiers"))
            return result + " from '" + context + "'";
        
        // The proposals I don't know yet.
        logger_.warning("Unknown proposal for contexifying: " + result);
        return result;
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
