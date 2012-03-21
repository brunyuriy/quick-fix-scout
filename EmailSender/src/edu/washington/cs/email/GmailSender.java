package edu.washington.cs.email;

import java.util.Properties;

public class GmailSender extends SecureEmailSender
{
    private static final String HOST = "smtp.gmail.com";
    private static final String START_TLS_IDENTIFIER = "mail.smtp.starttls.enable";
    private static final int PORT = 587;
    private static final String PROTOCOL = "smtps";

    public GmailSender(String username, String password, String from, String to, String subject, String text)
    {
        super(username, password, PROTOCOL, from, to, subject, text, HOST, PORT);
    }

    @Override
    public Properties createProperties()
    {
        Properties result = super.createProperties();
        result.put(START_TLS_IDENTIFIER, "true");
        return result;
    }

    public static void main(String [] args) throws Exception
    {
        GmailSender gmail = new GmailSender("kivancmuslu", "asdfgh", "kivancmuslu@gmail.com",
                "kivancmuslu@gmail.com", "Deneme", "Deneme");
        // gmail.addAttachment(new File("/Users/kivanc/Desktop/Resume - Kivanc Muslu.pdf"));
        // gmail.addAttachment(new File("/Users/kivanc/Desktop/Screen shot 2011-03-23 at 5.18.51 PM.png"));
        gmail.sendEmail();
    }

    @Override
    protected long getMaxAttachmentSize()
    {
        // 24 MB, just to be sure.
        return 1024 * 1024 * 24;
    }
}
