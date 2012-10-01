package edu.washington.cs.quickfix.observation.log.internal;

import java.util.ArrayList;
import java.util.List;

public class EclipseSessionLog
{
    private final ArrayList <Object> logEvents_;

    public EclipseSessionLog()
    {
        logEvents_ = new ArrayList <Object>();
    }

    public void addLogEvent(Object event)
    {
        logEvents_.add(event);
    }

    public List <Object> getEvents()
    {
        return logEvents_;
    }

    public String toString()
    {
        StringBuffer result = new StringBuffer();
        result.append("[Quick fix usage log with the following events:\n");
        for (Object event: logEvents_)
            result.append(event + "\n");
        result.delete(result.length() - 2, result.length());
        result.append(" ]\n");
        return result.toString();
    }
}
