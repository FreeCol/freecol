
package net.sf.freecol.client.gui.option;

import net.sf.freecol.common.option.*;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.util.Iterator;
import java.util.logging.Logger;



/**
* This class provides visualization for an {@link BooleanOption}. In order to
* enable values to be both seen and changed.
*/
public final class BooleanOptionUI extends JPanel implements OptionUpdater {
    private static final Logger logger = Logger.getLogger(BooleanOptionUI.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
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
    * Updates the value of the {@link Option} this object keeps.
    */
    public void updateOption() {
        option.setValue(checkBox.isSelected());
    }

}
