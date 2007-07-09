
package net.sf.freecol.client.gui.option;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.option.FileOption;
import net.sf.freecol.common.option.Option;



/**
 * This class provides visualization for an {@link FileOption}. In order to
 * enable values to be both seen and changed.
*/
public final class FileOptionUI extends JPanel implements OptionUpdater, PropertyChangeListener {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FileOptionUI.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private final FileOption option;
    
    private final JTextField fileField;


    /**
    * Creates a new <code>FileOptionUI</code> for the given
    * <code>FileOption</code>.
    * 
    * @param option The <code>FileOption</code> to make a user interface for.
    */
    public FileOptionUI(final FileOption option, boolean editable) {
        super(new FlowLayout(FlowLayout.LEFT));

        this.option = option;

        String name = option.getName();
        String description = option.getShortDescription();
        JLabel label = new JLabel(name, JLabel.LEFT);
        label.setToolTipText((description != null) ? description : name);
        add(label);

        final String value = (option.getValue() != null) 
                ? option.getValue().getAbsolutePath()
                        : "";
        fileField = new JTextField(value, 10);
        add(fileField);
        
        JButton browse = new JButton(Messages.message("file.browse"));
        if (editable) {
            browse.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                   final Canvas canvas = FreeCol.getFreeColClient().getCanvas();
                   File file = canvas.showLoadDialog(FreeCol.getSaveDirectory());

                   if (file == null) {
                       return;
                   }

                   if (!file.isFile()) {
                       canvas.errorMessage("fileNotFound");
                       return;
                   }

                   fileField.setText(file.getAbsolutePath());
               }
            });
        }
        add(browse);
        
        JButton remove = new JButton(Messages.message("option.remove"));
        if (editable) {
            remove.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                   fileField.setText("");
               }
            });
        }
        add(remove);
        
        browse.setEnabled(editable);
        remove.setEnabled(editable);
        fileField.setEnabled(false);
        label.setLabelFor(fileField);
        
        option.addPropertyChangeListener(this);
        
        setOpaque(false);
    }


    /**
     * Unregister <code>PropertyChangeListener</code>s.
     */
    public void unregister() {
        option.removePropertyChangeListener(this);    
    }
    
    /**
     * Updates this UI with the new data from the option.
     * @param event The event.
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals("value")) {
            fileField.setText(((File) event.getNewValue()).getAbsolutePath());
        }
    }
    
    /**
    * Updates the value of the {@link Option} this object keeps.
    */
    public void updateOption() {
        if (fileField.getText().equals("")) {
            option.setValue(null);
        } else {
            option.setValue(new File(fileField.getText()));
        }
    }
    
    /**
     * Reset with the value from the option.
     */
    public void reset() {
        setValue(option.getValue());
    }
    
    /**
     * Sets the value of this component.
     */
    public void setValue(File f) {
        if (f != null) { 
            fileField.setText(f.getAbsolutePath());
        } else {
            fileField.setText("");
        }
    }
}
