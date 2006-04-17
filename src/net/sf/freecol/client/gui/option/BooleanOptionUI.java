
package net.sf.freecol.client.gui.option;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.Option;



/**
* This class provides visualization for an {@link BooleanOption}. In order to
* enable values to be both seen and changed.
*/
public final class BooleanOptionUI extends JPanel implements OptionUpdater {
    private static final Logger logger = Logger.getLogger(BooleanOptionUI.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private final BooleanOption option;
    private final JCheckBox checkBox;
    

    /**
    * Creates a new <code>BooleanOptionUI</code> for the given <code>BooleanOption</code>.
    * @param option The <code>BooleanOption</code> to make a user interface for.
    */
    public BooleanOptionUI(BooleanOption option) {
        super(new FlowLayout(FlowLayout.LEFT));

        this.option = option;

        checkBox = new JCheckBox(option.getName(), option.getValue());
        checkBox.setToolTipText(option.getShortDescription());
        
        add(checkBox);
        setBorder(null);
        setOpaque(false);
    }

    /**
     * {@inheritDoc}
     */
    public Dimension getPreferredSize() {
        return new Dimension(getParent().getWidth()/2 - getParent().getInsets().left - getParent().getInsets().right - (OptionGroupUI.H_GAP*3)/2, super.getPreferredSize().height);
    }
    
    /**
     * {@inheritDoc}
     */
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    /**
    * Updates the value of the {@link Option} this object keeps.
    */
    public void updateOption() {
        option.setValue(checkBox.isSelected());
    }

}
