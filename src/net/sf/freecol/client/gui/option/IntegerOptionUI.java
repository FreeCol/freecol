
package net.sf.freecol.client.gui.option;

import java.awt.FlowLayout;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.Option;



/**
* This class provides visualization for an {@link IntegerOption}. In order to
* enable values to be both seen and changed.
*/
public final class IntegerOptionUI extends JPanel implements OptionUpdater {
    private static final Logger logger = Logger.getLogger(IntegerOptionUI.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private final IntegerOption option;
    
    private final JSpinner spinner;


    /**
    * Creates a new <code>IntegerOptionUI</code> for the given <code>IntegerOption</code>.
    * @param option The <code>IntegerOption</code> to make a user interface for.
    */
    public IntegerOptionUI(IntegerOption option) {
        super(new FlowLayout(FlowLayout.LEFT));

        this.option = option;

        JLabel label = new JLabel(option.getName(), JLabel.LEFT);
        label.setToolTipText(option.getShortDescription());
        add(label);

        int stepSize = Math.min((option.getMaximumValue() - option.getMinimumValue()) / 10, 1000);
        spinner = new JSpinner(new SpinnerNumberModel(option.getValue(), option.getMinimumValue(), option.getMaximumValue(), stepSize));
        spinner.setToolTipText(option.getShortDescription());
        add(spinner);
        
        setOpaque(false);
    }


    /**
    * Updates the value of the {@link Option} this object keeps.
    */
    public void updateOption() {
        option.setValue(((Integer) spinner.getValue()).intValue());
    }

}
