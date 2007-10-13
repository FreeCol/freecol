
package net.sf.freecol.client.gui.option;

import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.LanguageOption;
import net.sf.freecol.common.option.LanguageOption.Language;



/**
* This class provides visualization for an {@link LanguageOption}. In order to
* enable values to be both seen and changed.
*/
public final class LanguageOptionUI extends JPanel implements OptionUpdater, PropertyChangeListener {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(LanguageOptionUI.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private final LanguageOption option;
    
    private final JComboBox comboBox;


    /**
    * Creates a new <code>LanguageOptionUI</code> for the given <code>LanguageOption</code>.
    * @param option The <code>LanguageOption</code> to make a user interface for.
    */
    public LanguageOptionUI(LanguageOption option, boolean editable) {
        super(new FlowLayout(FlowLayout.LEFT));

        this.option = option;

        String name = option.getName();
        String description = option.getShortDescription();
        JLabel label = new JLabel(name, JLabel.LEFT);
        label.setToolTipText((description != null) ? description : name);
        add(label);

        Language[] languages = option.getOptions();

        comboBox = new JComboBox(languages);
        comboBox.setSelectedIndex(0);
        add(comboBox);
        
        comboBox.setEnabled(editable);

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
            comboBox.setSelectedIndex(((Integer) event.getNewValue()).intValue());
        }
    }
    
    /**
     * Updates the value of the {@link Option} this object keeps.
     */
    public void updateOption() {
        option.setValue((Language) comboBox.getSelectedItem());
    }

    /**
     * Reset with the value from the option.
     */
    public void reset() {
        comboBox.setSelectedItem(option.getValue());
    }
}
