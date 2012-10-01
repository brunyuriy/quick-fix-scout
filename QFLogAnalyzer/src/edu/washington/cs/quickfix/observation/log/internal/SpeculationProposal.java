package edu.washington.cs.quickfix.observation.log.internal;

public class SpeculationProposal extends Proposal
{
    private final int remainingErrors_;
    
    public static final int NOT_AVAILABLE = -10;
    public static final int NOT_COMPUTED = -1;
    
    public SpeculationProposal(String displayString)
    {
        super(displayString);
        remainingErrors_ = parseRemainingErrors(displayString);
    }
    
    private int parseRemainingErrors(String displayString)
    {
        String errors = displayString.split("\\(")[1].split("\\)")[0];
        if (errors.equals("?"))
            return NOT_COMPUTED;
        if (errors.equals("N/A"))
            return NOT_AVAILABLE;
        return Integer.parseInt(errors);
    }
    
    private boolean hasRemainingErrors()
    {
        return remainingErrors_ >= 0;
    }
    
    public int getRemainingErrors()
    {
        return remainingErrors_;
    }
    
    public int compareTo(Proposal other)
    {
        if (other instanceof SpeculationProposal)
            return compareTo((SpeculationProposal) other);
        return super.compareTo(other);
    }

    public int compareTo(SpeculationProposal other)
    {
//        if (true)
//            return -1;
        
        if (hasRemainingErrors())
        {
            if (other.hasRemainingErrors())
            {
                int difference = remainingErrors_ - other.remainingErrors_;
                if (difference == 0)
                    return super.compareTo(other);
                return difference;
            }
            else
                return -1;
        }
        else
        {
            if (other.hasRemainingErrors())
                return 1;
            else
                return super.compareTo(other);
        }
    }
}
