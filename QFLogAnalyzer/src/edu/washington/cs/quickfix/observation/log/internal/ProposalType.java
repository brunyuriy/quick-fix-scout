package edu.washington.cs.quickfix.observation.log.internal;

public enum ProposalType
{
    PACKAGE_DECLARATION("Package"),
    IMPORT("Import"),
    TYPE("Type"),
    CONSTRUCTOR("Constructor"),
    METHOD("Method"),
    FIELD_AND_VARIABLE("Field & Variable"),
    EXCEPTION_HANDLING("Exception"),
    BUILD_PATH_PROBLEM("Build Path"),
    OTHER("Other"),
    UNKNOWN("Unknown"),
    NOT_AVAILABLE("N/A");

    private final String description_;
    
    private ProposalType(String description)
    {
        description_ = description;
    }
    
    public String toString()
    {
        return description_;
    }
    
    static ProposalType parse(String value)
    {
        if (value.startsWith("("))
            value = value.substring(value.indexOf(")") + 2);
        
        ProposalType result = null;
        result = parsePackageDeclaration(value);
        result = result != null ? result : parseImport(value);
        result = result != null ? result : parseType(value);
        result = result != null ? result : parseConstructor(value);
        result = result != null ? result : parseMethod(value);
        result = result != null ? result : parseExceptionHandling(value);
        result = result != null ? result : parseBuildPathProblem(value);
        result = result != null ? result : parseOther(value);
        result = result != null ? result : parseFieldAndVariable(value);
        result = result != null ? result : parseUnknown(value);
        
        assert result != null: "Cannot parse proposals type for value = " + value;
        return result;
    }
    
    private static ProposalType parseUnknown(String value)
    {
        if (value.startsWith("("))
            value = value.substring(value.indexOf(")") + 2);
        
        if (value.equals("Remove"))
            return UNKNOWN;
        if (value.startsWith("Remove ") && value.contains(" annotation"))
            return UNKNOWN;
        if (value.contains("Remove ") && value.contains(" annotation"))
            // gbp version
        	return UNKNOWN;
        if (value.equals("Remove assignment"))
            return UNKNOWN;
        if (value.startsWith("Rename method ") && value.contains("(?2 R)"))
            return UNKNOWN;
        if (value.startsWith("Rename method ") && value.contains("(Ctrl+2 R)"))
            return UNKNOWN;
        if (value.startsWith("Rename field ") && value.contains("(?2 R)"))
            return UNKNOWN;
        if (value.startsWith("Rename field ") && value.contains("(Ctrl+2 R)"))
            return UNKNOWN;
        if (value.startsWith("Remove ") && value.contains(" and all assignments"))
            return UNKNOWN;
        if (value.equals("Add default serial version ID"))
            return UNKNOWN;
        if (value.equals("Add generated serial version ID"))
            return UNKNOWN;
        if (value.equals("Infer Generic Type Arguments..."))
            return UNKNOWN;
        if (value.startsWith("Remove exceptions from "))
            return UNKNOWN;
        if (value.startsWith("Add exceptions to "))
            return UNKNOWN;
        if (value.contains("Add exceptions to "))
            // gbp version.
            return UNKNOWN;
        if (value.equals("Remove invalid modifiers"))
            return UNKNOWN;
        if (value.startsWith("Remove ") && value.endsWith(" token"))
            return UNKNOWN;
        if (value.startsWith("Change access to static using ") && value.endsWith("(declaring type)"))
            return UNKNOWN;
        if (value.equals("Insert missing quote"))
            return UNKNOWN;
        if (value.startsWith("Rename ") && value.endsWith(" (Ctrl+2, R)"))
            return UNKNOWN;
        if (value.startsWith("Let") && value.contains(" implement "))
            return UNKNOWN;
        if (value.contains("Let") && value.contains(" implement "))
            return UNKNOWN;
        if (value.startsWith("Cast argument") && value.contains(" to "))
            return UNKNOWN;
        if (value.startsWith("Remove 'final' modifier of "))
            return UNKNOWN;
        if (value.contains("Remove 'final' modifier of "))
            // gbp version
        	return UNKNOWN;
        if (value.equals("Remove 'abstract' modifier"))
            return UNKNOWN;
        if (value.startsWith("Remove 'static' modifier of "))
            return UNKNOWN;
        if (value.contains("Remove 'static' modifier of "))
        	// gbp version
            return UNKNOWN;

        return null;
    }

    private static ProposalType parseImport(String value)
    {
        if (value.equals("Remove unused import"))
            return IMPORT;
        if (value.equals("Organize imports"))
            return IMPORT;
        // TODO Auto-generated method stub
        return null;
    }

    private static ProposalType parseOther(String value)
    {
        if (value.startsWith("Add @SuppressWarnings ") && value.contains(" to "))
            return OTHER;
        if (value.startsWith("Add cast to "))
            return OTHER;
        if (value.contains("Add cast to "))
        	// gbp version
            return OTHER;
        if (value.startsWith("Add type arguments to "))
            return OTHER;
        // TODO Auto-generated method stub
        return null;
    }

    private static ProposalType parseBuildPathProblem(String value)
    {
        if (value.startsWith("("))
            value = value.substring(value.indexOf(")") + 2);
        
        if (value.contains("Fix project setup..."))
            return BUILD_PATH_PROBLEM;
        if (value.equals("Configure build path..."))
            return BUILD_PATH_PROBLEM;
        if (value.contains("Add ") && value.endsWith(" to the build path"))
            return BUILD_PATH_PROBLEM;
        // TODO Auto-generated method stub
        return null;
    }

    private static ProposalType parseExceptionHandling(String value)
    {
        if (value.startsWith("("))
            value = value.substring(value.indexOf(")") + 2);
        
        if (value.equals("Surround with try/catch"))
            return EXCEPTION_HANDLING;
        if (value.contains("Surround with try/catch"))
            // gbp version
            return EXCEPTION_HANDLING;
        if (value.equals("Add throws declaration"))
            return EXCEPTION_HANDLING;
        if (value.contains("Add throws declaration"))
            // gbp version
            return EXCEPTION_HANDLING;
        if (value.equals("Add catch clause to surrounding try"))
            return EXCEPTION_HANDLING;
        if (value.equals("Replace catch clause with throws"))
            return EXCEPTION_HANDLING;
        if (value.equals("Remove catch clause"))
            return EXCEPTION_HANDLING;
        // TODO Auto-generated method stub
        return null;
    }

    private static ProposalType parseFieldAndVariable(String value)
    {
        if (value.startsWith("("))
            value = value.substring(value.indexOf(")") + 2);
        
        if (value.startsWith("Create getter and setter for "))
            return FIELD_AND_VARIABLE;
        if (value.contains("Change ") && value.contains(" to "))
            return FIELD_AND_VARIABLE;
        if (value.startsWith("Create field "))
            return FIELD_AND_VARIABLE;
        if (value.contains("Create field "))
            // gbp version
            return FIELD_AND_VARIABLE;
        if (value.startsWith("Create parameter "))
            return FIELD_AND_VARIABLE;
        if (value.startsWith("Create local variable "))
            return FIELD_AND_VARIABLE;
        if (value.contains("Create local variable "))
            // gbp version
            return FIELD_AND_VARIABLE;
        if (value.startsWith("Create constant "))
            return FIELD_AND_VARIABLE;
        if (value.startsWith("Change type of ") && value.contains(" to "))
            return FIELD_AND_VARIABLE;
        if (value.startsWith("Change type to "))
            return FIELD_AND_VARIABLE;
        if (value.startsWith("Change modifier of ") && value.contains(" to "))
            return FIELD_AND_VARIABLE;
        if (value.equals("Initialize variable"))
            return FIELD_AND_VARIABLE;
        if (value.contains("Initialize variable"))
        	// gbp version
            return FIELD_AND_VARIABLE;
        if (value.equals("Assign statement to new local variable"))
        	return FIELD_AND_VARIABLE;
        if (value.equals("Extract to local variable (replace all occurrences)"))
        	return FIELD_AND_VARIABLE;
        
        // TODO Auto-generated method stub
        return null;
    }

    private static ProposalType parseMethod(String value)
    {
        if (value.startsWith("Remove ") && value.contains(", keep side-effect assignments"))
            return METHOD;
        if (value.startsWith("Create method "))
            return METHOD;
        if (value.contains("Create method "))
            // gbp version
        	return METHOD;
        if (value.startsWith("Create ") && value.contains(" in super type "))
            return METHOD;
        if (value.startsWith("Change to ") && value.contains("(..)"))
            return METHOD;
        if (value.startsWith("Remove argument to match "))
        {
            String remaining = value.substring("Remove argument to match ".length());
            if (Character.isLowerCase(remaining.charAt(1)))
                return METHOD;
        }
        if (value.contains("Remove argument ") && value.contains(" to match "))
        {
        	// gbp version
            String remaining = value.split(" to match ")[1];
            if (Character.isLowerCase(remaining.charAt(1)))
                return METHOD;
        }
        if (value.startsWith("Remove arguments to match "))
        {
            String remaining = value.substring("Remove arguments to match ".length());
            if (Character.isLowerCase(remaining.charAt(1)))
                return METHOD;
        }
        if (value.startsWith("Add argument to match "))
        {
            String remaining = value.substring("Add argument to match ".length());
            if (Character.isLowerCase(remaining.charAt(1)))
                return METHOD;
        }
        if (value.contains("Add argument ") && value.contains(" to match "))
        {
            // gbp version
            String remaining = value.split(" to match ")[1];
            if (Character.isLowerCase(remaining.charAt(1)))
                return METHOD;
        }
        if (value.startsWith("Add arguments to match "))
        {
            String remaining = value.substring("Add arguments to match ".length());
            if (Character.isLowerCase(remaining.charAt(1)))
                return METHOD;
        }
        if (value.startsWith("Change return type of "))
            return METHOD;
        if (value.contains("Change return type of "))
            // gbp version.
            return METHOD;
        if (value.startsWith("Change method ") && value.contains(" to "))
            return METHOD;
        if (value.startsWith("Change method ") && value.contains(": Add parameter "))
            return METHOD;
        if (value.startsWith("Change method ") && value.contains(": Remove parameter "))
            return METHOD;
        if (value.startsWith("Change method ") && value.contains(": Remove parameters "))
            return METHOD;
        if (value.startsWith("Change method ") && value.contains(": Swap parameters "))
            return METHOD;
        if (value.equals("Add return statement"))
            return METHOD;
        if (value.contains("Add return statement"))
        	// gbp version
            return METHOD;
        if (value.startsWith("Change return type to "))
            return METHOD;
        if (value.startsWith("Change visibility of ") && value.contains("(") && value.contains(")'") && value.contains(" to "))
            return METHOD;
        if (value.startsWith("Swap arguments ") && value.contains(" and "))
            return METHOD;
        if (value.startsWith("Remove method "))
            return METHOD;
        if (value.equals("Add body"))
            return METHOD;
        if (value.equals("Add 'abstract' modifier"))
            return METHOD;
        if (value.startsWith("Remove 'static' modifier of ") && value.contains("(") & value.endsWith(")'"))
            return METHOD;
        if (value.startsWith("Set method return type to "))
            return METHOD;
        // TODO Auto-generated method stub
        return null;
    }

    private static ProposalType parseConstructor(String value)
    {
        if (value.startsWith("("))
            value = value.substring(value.indexOf(")") + 2);
        
        if (value.startsWith("Remove argument to match "))
        {
            String remaining = value.substring("Remove argument to match ".length());
            if (Character.isUpperCase(remaining.charAt(1)))
                return CONSTRUCTOR;
        }
        if (value.startsWith("Remove arguments to match "))
        {
            String remaining = value.substring("Remove arguments to match ".length());
            if (Character.isUpperCase(remaining.charAt(1)))
                return CONSTRUCTOR;
        }
        if (value.startsWith("Add argument to match "))
        {
            String remaining = value.substring("Add argument to match ".length());
            if (Character.isUpperCase(remaining.charAt(1)))
                return CONSTRUCTOR;
        }
        if (value.contains("Add argument ") && value.contains(" to match "))
        {
            // gbp version
            String remaining = value.split(" to match ")[1];
            if (Character.isUpperCase(remaining.charAt(1)))
                return CONSTRUCTOR;
        }
        if (value.startsWith("Add arguments to match "))
        {
            String remaining = value.substring("Add arguments to match ".length());
            if (Character.isUpperCase(remaining.charAt(1)))
                return CONSTRUCTOR;
        }
        if (value.startsWith("Create constructor "))
            return CONSTRUCTOR;
        if (value.equals("Add unimplemented methods"))
            return CONSTRUCTOR;
        if (value.contains("Add unimplemented methods"))
            // gbp version
            return CONSTRUCTOR;
        if (value.startsWith("Make type ") && value.endsWith(" abstract"))
            return CONSTRUCTOR;
        if (value.startsWith("Change constructor ") && value.contains(" to "))
            return CONSTRUCTOR;
        if (value.startsWith("Change constructor ") && value.contains(": Remove parameters "))
            return CONSTRUCTOR;
        if (value.startsWith("Add constructor "))
            return CONSTRUCTOR;
        if (value.contains("Add constructor "))
        	// gbp version
            return CONSTRUCTOR;
        if (value.startsWith("Change constructor ") && value.contains(": Remove parameter "))
            return CONSTRUCTOR;
        if (value.startsWith("Change constructor ") && value.contains(": Add parameter "))
            return CONSTRUCTOR;
        if (value.startsWith("Change constructor ") && value.contains(": Add parameters "))
            return CONSTRUCTOR;
        if (value.startsWith("Remove constructor "))
            return CONSTRUCTOR;
        // TODO Auto-generated method stub
        return null;
    }

    private static ProposalType parseType(String value)
    {
        if (value.startsWith("("))
            value = value.substring(value.indexOf(")") + 2);
        
        if (value.startsWith("Create class "))
            return TYPE;
        if (value.startsWith("Create interface "))
            return TYPE;
        if (value.startsWith("Create enum "))
            return TYPE;
        if (value.startsWith("Create annotation "))
            return TYPE;
        if (value.startsWith("Change to ") && value.contains(" (") &&  value.contains(")"))
            return TYPE;
        if (value.contains("Import ") && value.contains(" (") &&  value.contains(")"))
            return TYPE;
        if (value.contains("Add type parameter ") && value.contains(" to "))
            return TYPE;
        if (value.startsWith("Rename type to "))
            return TYPE;
        if (value.contains("Rename ") && value.contains(" to "))
            // gbp version
            return TYPE;
        if (value.startsWith("Rename compilation unit to "))
            return TYPE;
        if (value.startsWith("Remove type "))
            return TYPE;

        // TODO Auto-generated method stub
        return null;
    }

    private static ProposalType parsePackageDeclaration(String value)
    {
        if (value.startsWith("("))
            value = value.substring(value.indexOf(")") + 2);
        
        if (value.startsWith("Remove package declaration "))
            return PACKAGE_DECLARATION;
        if (value.startsWith("Add package declaration "))
            return PACKAGE_DECLARATION;
        if (value.startsWith("Move ") && value.contains(" to package "))
            return PACKAGE_DECLARATION;
        if (value.startsWith("Move ") && value.endsWith(" to the default package"))
            return PACKAGE_DECLARATION;
        if (value.startsWith("Change package declaration to "))
            return PACKAGE_DECLARATION;
        
        return null;
    }

}
