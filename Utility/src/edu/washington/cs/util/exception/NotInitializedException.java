package edu.washington.cs.util.exception;

/**
 * This exception indicates that an element that is being accessed (possibly related to internal Eclipse) is not
 * initialized yet.
 * 
 * @author Kivanc Muslu
 */
public class NotInitializedException extends Exception
{
    private static final long serialVersionUID = -5015005055234652922L;

    /**
     * Creates the exception.
     * 
     * @param message Error message that explains the non-initialized element.
     */
    public NotInitializedException(String message)
    {
        super(message);
    }
}
