package edu.washington.cs.email;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public abstract class EmailSender
{
    public static final String PLUG_IN_ID = "edu.washington.cs.email"; 
    private final static Logger logger = Logger.getLogger(EmailSender.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }
    private final static String HOST_IDENTIFIER = "mail.smtp.host";
    private final static String PORT_IDENTIFIER = "mail.smtp.port";
    private final static String USER_IDENTIFIER = "mail.smtp.user";
    private Session session_;
    private final String text_;
    private final ArrayList <File> attachments_;
    private final String from_;
    private final String to_;
    private final String subject_;
    private final String host_;
    private final int port_;

    public EmailSender(String from, String to, String subject, String text, String host, int port)
    {
        text_ = text;
        from_ = from;
        to_ = to;
        subject_ = subject;
        host_ = host;
        port_ = port;
        attachments_ = new ArrayList <File>();
    }
    
    protected abstract long getMaxAttachmentSize();

    protected Session getSession()
    {
        return session_;
    }

    protected String getHost()
    {
        return host_;
    }

    public void addAttachment(File file)
    {
        attachments_.add(file);
    }

    protected Properties createProperties()
    {
        Properties result = new Properties();
        result.put(HOST_IDENTIFIER, host_);
        result.put(USER_IDENTIFIER, from_);
        result.put(PORT_IDENTIFIER, port_);
        return result;
    }

    private MimeMessage createMessage()
    {
        Properties properties = createProperties();
        session_ = Session.getDefaultInstance(properties, null);
        MimeMessage message = new MimeMessage(session_);
        try
        {
            message.setFrom(new InternetAddress(from_));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to_));
            message.setSubject(subject_);
            message.setSentDate(new Date());
        }
        catch (AddressException e)
        {
            e.printStackTrace();
        }
        catch (MessagingException e)
        {
            e.printStackTrace();
        }
        return message;
    }

    private void completeMessage(MimeMessage message) throws MessagingException
    {
        Multipart multipart = new MimeMultipart();
        // Set the email message text.
        MimeBodyPart messagePart = new MimeBodyPart();
        messagePart.setText(text_);
        multipart.addBodyPart(messagePart);
        for (File file: attachments_)
        {
            // Set the email attachment file
            MimeBodyPart attachmentPart = new MimeBodyPart();
            FileDataSource fileDataSource = new FileDataSource(file.getAbsolutePath())
            {
                @Override
                public String getContentType()
                {
                    return "application/octet-stream";
                }
            };
            attachmentPart.setDataHandler(new DataHandler(fileDataSource));
            attachmentPart.setFileName(file.getName());
            multipart.addBodyPart(attachmentPart);
        }
        message.setContent(multipart);
    }

    public void sendEmail() throws Exception
    {
        long totalAttachmentSize = 0;
        for (File attachment: attachments_)
            totalAttachmentSize += attachment.length();
        if (totalAttachmentSize > getMaxAttachmentSize())
            throw new AttachmentTooBigException();
        
        /*
         * These cannot be done in a daemon thread since if an exception happens during the e-mail sending, it might
         * create problem.
         */
        MimeMessage message = createMessage();
        completeMessage(message);
        doSendEmail(message, session_);
    }

    protected void doSendEmail(MimeMessage message, Session session) throws MessagingException
    {
        Transport.send(message);
    }
}
