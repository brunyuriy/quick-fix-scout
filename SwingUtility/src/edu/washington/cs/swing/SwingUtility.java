package edu.washington.cs.swing;

public class SwingUtility
{
    public static final String PLUG_IN_ID = "edu.washington.cs.swing";

    private SwingUtility()
    {}
    
    /**
     * Creates an String in HTML format with the given text and width.
     * 
     * @param text text to be formatted.
     * @param width HTML division width.
     * @return
     */
    static String makeHTML(String text, int width)
    {
        if (text.startsWith("<html>"))
            return text;
        else
            return "<html><div align=justify width=" + width + ">" + text + "</div></html>";
    }
}
