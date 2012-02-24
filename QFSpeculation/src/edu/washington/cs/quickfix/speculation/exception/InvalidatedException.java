package edu.washington.cs.quickfix.speculation.exception;

public class InvalidatedException extends Exception
{
    /**
     * 
     */
    private static final long serialVersionUID = -7632283394105006105L;
    
    public InvalidatedException(String message)
    {
        super(message);
    }
    
    public InvalidatedException()
    {
        this("Speculative analysis invalidated.");
    }
}
