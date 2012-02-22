package edu.washington.cs.quickfix.observation.log;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.kivancmuslu.www.time.Calendars;
import com.kivancmuslu.www.zip.ZipException;
import com.kivancmuslu.www.zip.ZipStatus;
import com.kivancmuslu.www.zip.Zipper;

import edu.washington.cs.email.AttachmentTooBigException;
import edu.washington.cs.email.EmailSender;
import edu.washington.cs.email.GmailSender;
import edu.washington.cs.swing.KDialog;
import edu.washington.cs.util.eclipse.SharedConstants;
import edu.washington.cs.util.log.LogHandlers;

public class ObservationLogSender
{
    private static final long MB = 1024*1024;
    private static final long ZIP_LIMIT = 20 * MB;
    // Just for internal testing, don't forget to comment.
//    private static final long ZIP_LIMIT = 25 * 1024;
    private static final Logger logger = Logger.getLogger(ObservationLogSender.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }
    private boolean sendingLogs_;

    public ObservationLogSender()
    {
        sendingLogs_ = false;
    }

    public void waitUntilLogsSent()
    {
        while (isSendingLogs())
        {
            try
            {
                synchronized (this)
                {
                    wait();
                }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    public synchronized boolean isSendingLogs()
    {
        return sendingLogs_;
    }

    private ZipStatus createZip(File directory)
    {
        String zipName = Calendars.nowToString(".", "-", ".");
        File result = new File(directory.getParentFile(), zipName + ".zip");
        Zipper zipCreator = new Zipper(result.getAbsolutePath(), ZIP_LIMIT);
        try
        {
            zipCreator.addFolder(directory, new File(SharedConstants.DEBUG_LOG_PATH), new File(ObservationLogger.LOG_PATH));
            zipCreator.close();
        }
        catch (ZipException e)
        {
            logger.log(Level.SEVERE, "Cannot create the zip file: " + zipName, e);
        }
        return zipCreator.getStatus();
    }

    public void sendLogs()
    {
        synchronized (this)
        {
            sendingLogs_ = true;
        }
        Thread logSender = new Thread()
        {
            public void run()
            {
                File logDirectory = new File(ObservationLogger.LOG_DIRECTORY_PATH);
                logger.info("LogSender: Creating zip file.");
                ZipStatus status = createZip(logDirectory);
                // Delete all the excluded files (since we won't be able to zip them anyways).
                for (File file: status.getExcludedFiles())
                {
                    logger.info("Deleting log file: " + file.getName() + " since the zipped version is too big.");
                    file.delete();
                }
                boolean allOK = true;
                for (File zipFile: status.getCreatedZips())
                {
                    logger.info("LogSender: Sending zip file: " + zipFile.getName());
                    boolean result = sendEmail(zipFile);
                    allOK = allOK && result;
                    if (result)
                        zipFile.delete();
                }
                if (allOK)
                {
                    logger.info("LogSender: Logs sent, deleting logs.");
                    deleteContents(logDirectory, 0);
                    logger.info("LogSender: Logs deleted.");
                }
                synchronized (ObservationLogSender.this)
                {
                    sendingLogs_ = false;
                    ObservationLogSender.this.notifyAll();
                }
            }
        };
        logSender.setDaemon(false);
        logSender.start();
    }

    private boolean sendEmail(File attachment)
    {
        boolean result = true;
        try
        {
            String subject = "'" + System.getProperty("user.name") + "' quick fix observer logs";
            EmailSender emailSender = new GmailSender("quickfix.speculation", "quick.fix.speculation",
                    "quickfix.speculation@gmail.com", "kivancmuslu@gmail.com", subject, "Attached\n\n");
            emailSender.addAttachment(attachment);
            emailSender.sendEmail();
        }
        catch (AttachmentTooBigException e)
        {
            logger.log(Level.SEVERE, "Cannot send e-mail due to log size being too much." + e);
            result = false;
            EmailSender emailSender = new GmailSender("quickfix.speculation", "quick.fix.speculation",
                    "quickfix.speculation@gmail.com", "kivancmuslu@gmail.com", "Sending Failure: '"
                            + System.getProperty("user.name") + "' quick fix observer logs",
                    "Sending failed, probably big log size!\n\n");
            KDialog.showError(null, "Sending logs automatically failed possibly due to size of the created zip file (> 20 mb.)" +
            		"<br>Please get rid of the big log files (either delete, or copy them somewhere else) in your ~HOME/Quick_Fix_Usage" +
            		" directory and try again.<br><br>Thank you for your patience.", "Cannot Sends Logs Automatically", 500); 
            try
            {
                emailSender.sendEmail();
            }
            catch (Exception e1)
            {
                logger.log(Level.SEVERE, "Cannot send e-mail due to exception." + e);
            }
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "Cannot send e-mail due to exception." + e);
        }
        return result;
    }

    private void deleteContents(File directory, int level)
    {
        for (File file: directory.listFiles())
        {
            if (file.isDirectory())
                deleteContents(file, level + 1);
            else
            {
                String filePath = file.getAbsolutePath();
                if (!filePath.equals(ObservationLogger.LOG_PATH) && !filePath.equals(LogHandlers.logPath_))
                    file.delete();
            }
        }
        if (level != 0)
            directory.delete();
    }

    public static void main(String [] args)
    {
        ObservationLogSender logSender = new ObservationLogSender();
        logSender.sendLogs();
    }
}
