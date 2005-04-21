
package net.sf.freecol.client.gui.option;

import net.sf.freecol.common.option.*;

import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;



/**
* This class provides visualization for an {@link OptionGroup}. In order to
* enable values to be both seen and changed.
*/
public final class OptionGroupUI extends JPanel implements OptionUpdater {
    private static final Logger logger = Logger.getLogger(OptionGroupUI.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private final OptionGroup option;
    private final OptionUpdater[] optionUpdaters;
    

    /**
    * Creates a new <code>OptionGroupUI</code> for the given <code>OptionGroup</code>.
    * @param option The <code>OptionGroup</code> to make a user interface for.
    */
    public OptionGroupUI(OptionGroup option) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.option = option;

        List ou = new ArrayList();
        Iterator it = option.iterator();
        while (it.hasNext()) {
            Option o = (Option) it.next();

            if (o instanceof OptionGroup) {
                JComponent c = new OptionGroupUI((OptionGroup) o);
                add(c);
                ou.add((OptionUpdater) c);
            } else if (o instanceof BooleanOption) {
                JComponent c = new BooleanOptionUI((BooleanOption) o);
                add(c);
                ou.add((OptionUpdater) c);
            } else if (o instanceof IntegerOption) {
                JComponent c = new IntegerOptionUI((IntegerOption) o);
                add(c);
                ou.add((OptionUpdater) c);
            } else {
                logger.warning("Unknown option.");
            }
        }
        optionUpdaters = (OptionUpdater[]) ou.toArray(new OptionUpdater[0]);

        setBorder(BorderFactory.createTitledBorder(option.getName()));
        setOpaque(false);
    }


    /**
    * Updates the value of the {@link Option} this object keeps.
    */
    public void updateOption() {
        for (int i=0; i<optionUpdaters.length; i++) {
            optionUpdaters[i].updateOption();
        }

    }

}
