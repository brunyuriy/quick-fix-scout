package edu.washington.cs.swing;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class KDialog
{
    private KDialog()
    {}

    /**
     * Shows the Error Dialog with the given message and title.
     * 
     * @param parent Parent component of the Error Dialog
     * @param message Error message
     * @param title Dialog title
     */
    public static void showError(Component parent, String message, String title, int width)
    {
        showDialog(parent, message, title, JOptionPane.ERROR_MESSAGE, width);
    }

    public static void showInformation(Component parent, String message, String title, int width)
    {
        showDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE, width);
    }
    
    public static Object showOptionDialog(Component parent, String message, String title, int width, Object ... options)
    {
        JOptionPane pane = new JOptionPane(createEditorPane(message, width), JOptionPane.DEFAULT_OPTION);
        pane.setOptions(options);
        JDialog dialog = pane.createDialog(parent, title);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
        return pane.getValue();
    }
    
    // Taken and modified from: http://stackoverflow.com/questions/8348063/clickable-links-in-joptionpane
    private static class JOptionPaneHyperlinkListener implements HyperlinkListener
    {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent hle)
        {
            if (hle.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
            {
                if (Desktop.isDesktopSupported())
                    try
                    {
                        Desktop.getDesktop().browse(hle.getURL().toURI());
                    }
                    catch (IOException e)
                    {
                        showError(null, "Cannot open hyperlink due to IO exception", "Cannot open hyperlink", 500);
                    }
                    catch (URISyntaxException e)
                    {
                        showError(null, "Cannot open hyperlink due to URI Sytax exception", "Cannot open hyperlink", 500);
                    }
            }
        }
    }
    // Taken and modified from: http://stackoverflow.com/questions/8348063/clickable-links-in-joptionpane
    
    /**
     * Creates, formats and sets visible to the a dialog. This function is called by showErrorDialog and showAboutDialog
     * functions to create the desired dialog.
     * 
     * @param parent Parent component of the created Dialog.
     * @param message Message of the Dialog
     * @param title Title of the Dialog
     * @param style the type of message to be displayed: ERROR_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE,
     *            QUESTION_MESSAGE, or PLAIN_MESSAGE
     */
    public static void showDialog(Component parent, String message, String title, int style, int width)
    {
        JOptionPane pane = new JOptionPane(createEditorPane(message, width), style);
        JDialog dialog = pane.createDialog(parent, title);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }
    
    private static JEditorPane createEditorPane(String message, int width)
    {
        // Taken and modified from: http://stackoverflow.com/questions/8348063/clickable-links-in-joptionpane
        // for copying style
        JLabel label = new JLabel();
        Font font = label.getFont();

        // create some css from the label's font
        StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
        style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
        style.append("font-size:" + font.getSize() + "pt;");

        message = SwingUtility.makeHTML(message, style.toString(), width);
        JEditorPane ep = new JEditorPane("text/html", message);
        // handle link events
        ep.addHyperlinkListener(new JOptionPaneHyperlinkListener());
        ep.setEditable(false);
        ep.setBackground(label.getBackground());
        // Taken and modified from: http://stackoverflow.com/questions/8348063/clickable-links-in-joptionpane
        return ep;
    }
}
