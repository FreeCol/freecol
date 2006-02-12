
package net.sf.freecol.client.gui.option;

import java.awt.FlowLayout;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComboBox;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.option.SelectOption;
import net.sf.freecol.common.option.Option;



/**
* This class provides visualization for an {@link SelectOption}. In order to
* enable values to be both seen and changed.
*/
public final class SelectOptionUI extends JPanel implements OptionUpdater {
    private static final Logger logger = Logger.getLogger(SelectOptionUI.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private final SelectOption option;
    
    private final JComboBox comboBox;


    /**
    * Creates a new <code>SelectOptionUI</code> for the given <code>SelectOption</code>.
    * @param option The <code>SelectOption</code> to make a user interface for.
    */
    public SelectOptionUI(SelectOption option) {
        super(new FlowLayout(FlowLayout.LEFT));

        this.option = option;

        JLabel label = new JLabel(option.getName(), JLabel.LEFT);
        label.setToolTipText(option.getShortDescription());
        add(label);

        String[] strings = option.getOptions();
        String[] localized = new String[strings.length];
        for (int i = 0; i < strings.length; i++) {
            localized[i] = Messages.message(strings[i]);
        }

        comboBox = new JComboBox(localized);
        comboBox.setSelectedIndex(option.getValue());
        add(comboBox);
        
        setOpaque(false);
    }


    /**
    * Updates the value of the {@link Option} this object keeps.
    */
    public void updateOption() {
        option.setValue(comboBox.getSelectedIndex());
    }

}
