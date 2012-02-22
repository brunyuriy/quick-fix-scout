package edu.washington.cs.email;

import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

public abstract class SecureEmailSender extends EmailSender
{
    private static final String PASSWORD_IDENTIFIER = "mail.smtp.password";
    private static final String AUTHENTICATION_IDENTIFIER = "mail.smtp.auth";
    private final String username_;
    private final String password_;
    private final String protocol_;

    public SecureEmailSender(String username, String password, String protocol, String from, String to, String subject,
            String text, String host, int port)
    {
        super(from, to, subject, text, host, port);
        protocol_ = protocol;
        username_ = username;
        password_ = password;
    }

    @Override
    protected Properties createProperties()
    {
        Properties result = super.createProperties();
        result.put(PASSWORD_IDENTIFIER, password_);
        result.put(AUTHENTICATION_IDENTIFIER, "true");
        return result;
    }

    @Override
    protected void doSendEmail(MimeMessage message, Session session) throws MessagingException
    {
        Transport transport = session.getTransport(protocol_);
        transport.connect("smtp.gmail.com", username_, password_);
        transport.sendMessage(message, message.getAllRecipients());
    }
}