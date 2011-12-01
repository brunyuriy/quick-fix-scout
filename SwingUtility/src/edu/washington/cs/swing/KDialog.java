package edu.washington.cs.swing;

import java.awt.Component;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

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
        message = SwingUtility.makeHTML(message, width);
        JOptionPane pane = new JOptionPane(message, style);
        JDialog dialog = pane.createDialog(parent, title);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }
}
