package edu.washington.cs.util.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Implementation of common loggers for debug purposes.
 * 
 * @author Kivanc Muslu
 */
public class LogHandlers
{
    public static final String PLUG_IN_ID = "edu.washington.cs.logging";
    /**
     * The path which the log will be written if {@link LogHandlers#logAlsoToFile_} is <code>true</code>.
     */
    public static String logPath_ = null;
    /**
     * Formatter object which will write the log to a file if {@link LogHandlers#logAlsoToFile_} is <code>true</code>.
     */
    private static Formatter logWriter_;
    
    private static boolean initialized_ = false;
    
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    
    public static void init(String path)
    {
        if (initialized_)
            return;
        logPath_ = path;
    }

    /**
     * A console handler implementation for changing the default's of the {@link Logger} handlers. <br>
     * Current log format is: <br>
     * "log_level": "log_message" in ("log_class"#"log_method") <br>
     * [optional "exception_thrown"]
     * 
     * @author Kivanc Muslu
     */
    public static class LoggerConsoleHandler extends Handler
    {
        /** print steam that will be replaced for the default {@link Logger}'s handler. */
        private final PrintStream ps_;

        /**
         * Creates a console handler for logging purposes with the given print stream. <br>
         * Currently used print streams are the following: <br>
         * Use {@link System#out} to create normal console output. <br>
         * Use {@link System#err} to create error console output. <br>
         * 
         * @param ps Print stream that the log will be written to.
         */
        public LoggerConsoleHandler(PrintStream ps)
        {
            if (logPath_ != null)
            {
                try
                {
                    logWriter_ = new Formatter(new File(logPath_));
                }
                catch (FileNotFoundException e)
                {
                    ps.println("Cannot create log writer! Log path = " + logPath_);
                }
            }
            ps_ = ps;
        }

        /**
         * {@inheritDoc}
         * <p>
         * If the log is also written to a file, this method also closes the formatters that are writing the log to
         * files.
         * </p>
         */
        @Override
        public void close() throws SecurityException
        {
            if (logWriter_ != null)
                logWriter_.close();
        }

        /**
         * {@inheritDoc}
         * <p>
         * If the log is also written to a file, this method also flushes the formatters that are writing the log to
         * files.
         * </p>
         */
        @Override
        public void flush()
        {
            if (logWriter_ != null)
                logWriter_.flush();
        }

        /**
         * {@inheritDoc}
         * <p>
         * Current log format is: <br>
         * "log_level": "log_message" in ("log_class"#"log_method") <br>
         * [optional "exception_thrown"]
         * </p>
         */
        @Override
        public void publish(LogRecord record)
        {
            StringBuffer result = new StringBuffer();
            if (!record.getMessage().equals(""))
            {
                result.append(record.getLevel().toString() + ": " + record.getMessage() + " in ("
                        + record.getSourceClassName() + "#" + record.getSourceMethodName() + ")");
                Throwable exception = record.getThrown();
                if (exception != null)
                {
                    result.append(LINE_SEPARATOR + exception.getMessage() + LINE_SEPARATOR);
                    for (StackTraceElement ste: exception.getStackTrace())
                        result.append("\t" + ste.toString() + LINE_SEPARATOR);
                    result.deleteCharAt(result.length() - 1);
                }
            }
            if (logWriter_ != null)
                logWriter_.format("%s" + LINE_SEPARATOR, result.toString());
            ps_.println(result.toString());
            flush();
        }
    }

    /**
     * This method overrides the output handler of {@link Logger}'s default implementation with a new
     * {@link LoggerConsoleHandler} constructed with the given print stream argument. <br>
     * This way the log format is modified and log is also written to a file if {@link #logAlsoToFile_} is set to
     * <code>true</code>.
     * 
     * @param ps Print stream that will be used to construct LoggerConsoleHandler.
     */
    public static void setMainHandler(PrintStream ps)
    {
        if (initialized_)
            return;
        Logger root = Logger.getLogger("");
        for (Handler handler: root.getHandlers())
            root.removeHandler(handler);
        root.addHandler(new LogHandlers.LoggerConsoleHandler(ps));
    }
}
