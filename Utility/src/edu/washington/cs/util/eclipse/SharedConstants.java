package edu.washington.cs.util.eclipse;

import java.util.Calendar;

public class SharedConstants
{
    public static final String DEBUG_LOG_PATH;
    public static final String DEBUG_LOG_DIR;
    public static final String UNIQUE_TIME_STAMP;
    private static final String FILE_SEPARATOR = System.getProperty("file.separator");
    static
    {
        /*
         * Creates a log file on the disk. Current implement creates the file in
         * ~/Quick_Fix_Usage/qf_log_<current_time>.txt
         */
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        DEBUG_LOG_DIR = System.getProperty("user.home") + FILE_SEPARATOR + "Quick_Fix_Usage" + FILE_SEPARATOR;
        UNIQUE_TIME_STAMP = makeString(month) + "." + makeString(day) + "." + year + "_" + makeString(hour) + "."
                + makeString(minute) + "." + makeString(second);
        DEBUG_LOG_PATH = DEBUG_LOG_DIR + "qf_log_" + UNIQUE_TIME_STAMP + ".txt";
    }

    /**
     * Appends '0' to the beginning of the number given if the input is less than 10.
     * 
     * @param value The number that will be converted to a string.
     * @return The input if it is greater than 10, "0"+input otherwise.
     */
    public static String makeString(long value)
    {
        return (value < 10 ? "0" : "") + value;
    }

    private SharedConstants()
    {}
}
