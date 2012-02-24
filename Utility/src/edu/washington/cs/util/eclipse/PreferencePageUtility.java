package edu.washington.cs.util.eclipse;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

@SuppressWarnings("unused")
public class PreferencePageUtility
{
    private PreferencePageUtility()
    {}

    public static void makeControlVisible(Control control)
    {
        control.pack();
        control.setVisible(true);
    }

    public static Group createVerticalGroup(Composite parent, String groupName)
    {
        Group result = new Group(parent, SWT.NONE);
        // result.setFont(new Font(parent.getDisplay(), "Times New Roman", 14, SWT.NORMAL));
        GridLayout resultLayout = new GridLayout(1, false);
        result.setLayout(resultLayout);
        result.setText(groupName);
        return result;
    }
    
    public static Button createButton(Composite parent, String name)
    {
        return createButton(parent, name, SWT.PUSH);
    }
    
    public static Button createCheckBox(Composite parent, String name)
    {
        return createButton(parent, name, SWT.CHECK);
    }
    
    private static Button createButton(Composite parent, String name, int style)
    {
        Button result = new Button(parent, style);
        result.setText(name);
        makeControlVisible(result);
        return result;
    }
    
    public static Button createRadioButton(Composite parent, String name)
    {
        return createButton(parent, name, SWT.RADIO);
    }
    
    public static Label createLabel(Composite parent, String labelString)
    {
        Label result = new Label(parent, SWT.LEFT);
        result.setText(labelString);
        makeControlVisible(result);
        return result;
    }

    public static Text createText(Composite parent, String text, boolean editable)
    {
        Text result = new Text(parent, SWT.LEFT);
        result.setEditable(editable);
        result.setText(text);
        return result;
    }

    public static void makeHorizontalComposite(Composite composite, Control... controls)
    {
        composite.setLayout(new GridLayout(controls.length, false));
        for (Control control: controls)
            makeControlVisible(control);
        makeControlVisible(composite);
    }
}
