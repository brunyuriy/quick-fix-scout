package edu.washington.cs.quickfix.observation.log.internal;

public class Proposal implements Comparable <Proposal>
{
    private final ProposalType type_;
    private final int relevance_;
    private final String displayString_;
    
    public static final int WARNING_RELEVANCE = -1;
    public static final int UNKNOWN_RELEVANCE = -10;
    public static final Proposal NONE = new Proposal(ProposalType.NOT_AVAILABLE, Integer.MIN_VALUE);
    
    public Proposal(String displayString)
    {
        displayString_ = displayString;
        type_ = ProposalType.parse(displayString);
        relevance_ = determineRelevance(displayString);
    }
    
    private Proposal(ProposalType type, int relevance)
    {
        displayString_ = null;
        type_ = type;
        relevance_ = relevance;
    }

    private int determineRelevance(String displayString)
    {
        if (displayString.startsWith("("))
            displayString = displayString.substring(displayString.indexOf(")") + 2);
        
        if (displayString.startsWith("Import ") && displayString.contains("(") && displayString.contains(")"))
            return 103;
        if (displayString.contains("Import ") && displayString.contains("(") && displayString.contains(")"))
            // gbp version
            return 103;
        if (displayString.startsWith("Change to ") && displayString.contains("(") && displayString.contains(")"))
            // for imports.
            return 3;
        if (displayString.contains("Change ") && displayString.contains(" to ") && displayString.contains("(") && displayString.contains(")"))
            // for imports, gbp version.
            return 3;
        if (displayString.startsWith("Change to ") && !displayString.contains("(") && !displayString.contains(")"))
            // for variables
            return 5;
        if (displayString.startsWith("Create class "))
            return 6;
        if (displayString.startsWith("Create annotation "))
            return 4;
        if (displayString.startsWith("Create interface "))
            return 5;
        if (displayString.startsWith("Create enum "))
            return 3;
        if (displayString.startsWith("Add type parameter ") && displayString.contains(" to ") && !displayString.contains("(") && !displayString.contains(")"))
            // For class parameters
            return 1;
        if (displayString.startsWith("Add type parameter ") && displayString.contains(" to ") && displayString.contains("(") && displayString.contains(")"))
            // For method parameters
            return 0;
        if (displayString.contains("Add type parameter ") && displayString.contains(" to ") && displayString.contains("(") && displayString.contains(")"))
            // For method parameters, gbp version
            return 0;
        if (displayString.equals("Fix project setup..."))
            return -10;
        if (displayString.startsWith("Create field "))
            return 6;
        if (displayString.contains("Create field "))
            // gbp version
            return 6;
        if (displayString.startsWith("Create parameter "))
            return 5;
        if (displayString.startsWith("Create constant "))
            return 4;
        if (displayString.startsWith("Create local variable "))
            return 7;
        if (displayString.contains("Create local variable "))
            // gbp version
            return 7;
        if (displayString.startsWith("Remove assignment "))
            return 4;
        if (displayString.startsWith("Add argument to match ") && displayString.contains("(") && displayString.contains(")"))
            // Constructor and methods.
            return 8;
        if (displayString.contains("Add argument ") && displayString.contains(" to match ") && displayString.contains("(") && displayString.contains(")"))
            // gbp version
            // Constructor and methods.
            return 8;
        if (displayString.startsWith("Add arguments to match ") && displayString.contains("(") && displayString.contains(")"))
            // Constructor and methods.
            return 8;
        if (displayString.startsWith("Remove argument to match ") && displayString.contains("(") && displayString.contains(")"))
            // Constructor and methods.
            return 8;
        if (displayString.contains("Remove argument ") && displayString.contains(" to match ") && displayString.contains("(") && displayString.contains(")"))
        	// gbp version
        	// Constructor and methods.
            return 8;
        if (displayString.startsWith("Remove arguments to match ") && displayString.contains("(") && displayString.contains(")"))
            // Constructor and methods.
            return 8;
        if (displayString.startsWith("Change constructor ") && displayString.contains("(") && displayString.contains(")") && displayString.contains(" Add parameter "))
            return 5;
        if (displayString.startsWith("Change constructor ") && displayString.contains("(") && displayString.contains(")") && displayString.contains(" Add parameters "))
            return 5;
        if (displayString.startsWith("Change constructor ") && displayString.contains("(") && displayString.contains(")") && displayString.contains(" Remove parameter "))
            return 5;
        if (displayString.startsWith("Change constructor ") && displayString.contains("(") && displayString.contains(")") && displayString.contains(" Remove parameters "))
            return 5;
        if (displayString.startsWith("Change method ") && displayString.contains("(") && displayString.contains(")") && displayString.contains(" Remove parameter "))
            return 5;
        if (displayString.startsWith("Change method ") && displayString.contains("(") && displayString.contains(")") && displayString.contains(" Remove parameters "))
            return 5;
        if (displayString.startsWith("Create constructor ") && displayString.contains("(") && displayString.contains(")"))
            return 5;
        if (displayString.startsWith("Add constructor ") && displayString.contains("(") && displayString.contains(")"))
            return 5;
        if (displayString.contains("Add constructor ") && displayString.contains("(") && displayString.contains(")"))
            // gbp version
        	return 5;
        if (displayString.startsWith("Create method ") && displayString.contains("(") && displayString.contains(")"))
            return 5;
        if (displayString.contains("Create method ") && displayString.contains("(") && displayString.contains(")"))
        	// gbp version
            return 5;
        if (displayString.equals("Replace catch clause with throws"))
            return 4;
        if (displayString.equals("Remove catch clause"))
            return 4;
        if (displayString.equals("Remove invalid modifiers"))
            return 5;
        if (displayString.startsWith("Remove exceptions from "))
            return 8;
        if (displayString.startsWith("Add exceptions to "))
            return 7;
        if (displayString.contains("Add exceptions to "))
            // GBP version
            return 7;
        if (displayString.equals("Change return type to 'void'"))
            return 5;
        if (displayString.equals("Set method return type to 'void'"))
            // TODO Make sure that this is correct.
            return 5;
        if (displayString.startsWith("Set method return type to ") && !displayString.contains("'void'"))
            // TODO Make sure that this is correct.
            return 6;
        if (displayString.startsWith("Change method return type to ") && !displayString.contains("'void'"))
            return 6;
        if (displayString.startsWith("Change method ") && displayString.contains(": Add parameter "))
            return 5;
        if (displayString.equals("Change to 'return'"))
            return 5;
        if (displayString.equals("Add return statment"))
            return 6;
        if (displayString.startsWith("Change return type to ") && !displayString.endsWith("'void'"))
            return 6;
        if (displayString.startsWith("Change return type of ") && displayString.contains("(..)"))
            // Same for void and other types.
            return 8;
        if (displayString.contains("Change return type of ") && displayString.contains("(..)"))
            // Same for void and other types, gbp version.
            return 8;
        if (displayString.startsWith("Change return type of overridden ") && displayString.contains("(..)"))
            return 7;
        if (displayString.startsWith("Change modifier of ") && displayString.endsWith(" to 'static'"))
            return 5;
        if (displayString.startsWith("Remove 'static' modifier of ") && displayString.contains("(") && displayString.contains(")"))
            return 5;
        if (displayString.equals("Add throws declaration"))
            return 8;
        if (displayString.contains("Add throws declaration"))
            // gbp version
            return 8;
        if (displayString.equals("Remove type arguments"))
            return 6;
        if (displayString.equals("Add catch clause to surrounding try"))
            return 7;
        if (displayString.equals("Surround with try/catch"))
            return 6;
        if (displayString.contains("Surround with try/catch"))
            // gbp version
            return 6;
        if (displayString.startsWith("Change type of ") && displayString.contains(" to "))
            return 6;
        if (displayString.contains("Change type of ") && displayString.contains(" to "))
            // gbp version
            return 6;
        if (displayString.startsWith("Add cast to "))
            return 7;
        if (displayString.startsWith("Rename ") && displayString.contains("(Ctrl+2, R)"))
            return 8;
        if (displayString.startsWith("Rename ") && displayString.contains("(?2 R)"))
            return 8;
        if (displayString.equals("Remove assignment"))
            return 4;
        if (displayString.equals("Add unimplemented methods"))
            return 10;
        if (displayString.contains("Add unimplemented methods"))
            // gbp version
            return 10;
        if (displayString.equals("Add return statement"))
            return 6;
        if (displayString.startsWith("Make type ") && displayString.endsWith(" abstract"))
            return 5;
        if (displayString.startsWith("Change modifier of ") && displayString.endsWith(" to final"))
            return 5;
        if (displayString.startsWith("Remove 'final' modifier of "))
            return 9;
        if (displayString.startsWith("Change package declaration to "))
            return 5;
        if (displayString.startsWith("Let ") && displayString.contains(" implement "))
            return 4;
        if (displayString.equals("Organize imports"))
            return 5;
        if (displayString.equals("Insert missing quote"))
            return 0;
        if (displayString.equals("Remove method body"))
            return 5;
        if (displayString.equals("Remove 'abstract' modifier"))
            return 6;
        if (displayString.equals("Add body"))
            return 9;
        if (displayString.equals("Initialize variable"))
            return 6;
        if (displayString.equals("Add 'abstract' modifier"))
            return 8;
        if (displayString.startsWith("Rename type to "))
            return 5;
        if (displayString.contains("Rename ") && displayString.contains(" to "))
            // gbp version.
            return 5;
        if (displayString.startsWith("Rename compilation unit to "))
            return 5;
        if (displayString.equals("Remove unused import"))
            return 5;
        if (displayString.equals("Change 'extends' to 'implements'"))
            return 6;
        if (displayString.startsWith("Change ") && displayString.endsWith(" to interface"))
            return 3;
        if (displayString.equals("Remove '@Override' annotation"))
            return 6;
        if (displayString.startsWith("Create ") && displayString.contains(" in super type "))
            return 6;
        if (displayString.startsWith("Remove package declaration "))
            return 5;
        if (displayString.startsWith("Swap arguments") && displayString.contains(" and "))
            return 8;
        if (displayString.startsWith("Change method ") && displayString.contains(": Swap parameters "))
            return 5;
        if (displayString.startsWith("Move ") && displayString.contains(" to package "))
            return 6;
        if (displayString.startsWith("Add package declaration "))
            return 7;
        if (displayString.startsWith("Move ") && displayString.contains(" to the default package"))
            return 6;
        if (displayString.startsWith("Change visibility of ") && displayString.contains(" to 'default'"))
            return 10;
        if (displayString.startsWith("Change visibility of ") && displayString.contains(" to 'public'"))
            return 10;
        if (displayString.startsWith("Create getter and setter for "))
            return 9;
        if (displayString.equals("Extract to local variable (replace all occurrences)"))
            return UNKNOWN_RELEVANCE;
        if (displayString.equals("Configure build path..."))
            return UNKNOWN_RELEVANCE;
        if (displayString.equals("Assign statement to new local variable"))
            return UNKNOWN_RELEVANCE;
        if (displayString.startsWith("Change type to "))
            return UNKNOWN_RELEVANCE;
        if (displayString.startsWith("Remove ") && displayString.endsWith(" token"))
            return UNKNOWN_RELEVANCE;
        if (displayString.startsWith ("Cast argument ") && displayString.contains(" to "))
            return UNKNOWN_RELEVANCE;
        if (displayString.equals("Add JUnit 4 library to the build path"))
            return UNKNOWN_RELEVANCE;
        if (displayString.startsWith("Remove ") && displayString.contains(", keep side-effect assignments"))
            return UNKNOWN_RELEVANCE;
        if (displayString.startsWith("Remove ") && displayString.contains(" and all assignments"))
            return UNKNOWN_RELEVANCE;
        if (displayString.equals("Add default serial version ID"))
            return WARNING_RELEVANCE;
        if (displayString.startsWith("Create getter and setter for "))
            return WARNING_RELEVANCE;
        if (displayString.startsWith("Remove method "))
            return WARNING_RELEVANCE;
        if (displayString.startsWith("Remove type "))
            return WARNING_RELEVANCE;
        if (displayString.equals("Infer Generic Type Arguments..."))
            return WARNING_RELEVANCE;
        if (displayString.startsWith("Add type arguments to "))
            return WARNING_RELEVANCE;
        if (displayString.startsWith("Remove constructor "))
            return WARNING_RELEVANCE;
        if (displayString.equals("Add generated serial version ID"))
            return WARNING_RELEVANCE;
        if (displayString.contains("Add @SuppressWarnings ") && displayString.contains(" to "))
            return WARNING_RELEVANCE;

        
        assert false: displayString;
        return UNKNOWN_RELEVANCE;
    }

    public ProposalType getType()
    {
        return type_;
    }
    

    public int getRelevance()
    {
        return relevance_;
    }
    
    public static void main(String [] args)
    {
        String test = "(1) Add type parameter 'Label' to 'HardDriveArchieverGUI'";
        System.out.println(new SpeculationProposal(test).getRelevance());
    }

    @Override
    public int compareTo(Proposal other)
    {
        int difference = other.relevance_ - relevance_;
        if (difference == 0)
            return displayString_.compareTo(other.displayString_);
        return difference;
    }

    public String getDisplayString()
    {
        return displayString_;
    }
}
