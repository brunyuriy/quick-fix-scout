package edu.washington.cs.util.log;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CommonLoggers
{
    private CommonLoggers() {}
    
    private static final Logger communicationLogger_ = Logger.getLogger(CommonLoggers.class.getName() + ".communication");
    static
    {
        communicationLogger_.setLevel(Level.WARNING);
    }

    public static Logger getCommunicationLogger()
    {
        return communicationLogger_;
    }
    
}
