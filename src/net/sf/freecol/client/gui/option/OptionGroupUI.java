
package net.sf.freecol.client.gui.option;

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import net.sf.freecol.client.gui.action.FreeColAction;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.SelectOption;



/**
* This class provides visualization for an {@link OptionGroup}. In order to
* enable values to be both seen and changed.
*/
public final class OptionGroupUI extends JPanel implements OptionUpdater {
    private static final Logger logger = Logger.getLogger(OptionGroupUI.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** The horisontal gap between components in this <code>OptionGroupUI</code>. */
    public static final int H_GAP = 20;

    private final OptionGroup option;
    private final OptionUpdater[] optionUpdaters;
    

    /**
    * Creates a new <code>OptionGroupUI</code> for the given <code>OptionGroup</code>.
    * @param option The <code>OptionGroup</code> to make a user interface for.
    */
    public OptionGroupUI(OptionGroup option, boolean editable) {
        setLayout(new FlowLayout(FlowLayout.LEFT, H_GAP, 5));

        this.option = option;

        List ou = new ArrayList();
        Iterator it = option.iterator();
        while (it.hasNext()) {
            Option o = (Option) it.next();

            JComponent c = null;
            if (o instanceof OptionGroup) {
                c = new OptionGroupUI((OptionGroup) o, editable);
            } else if (o instanceof BooleanOption) {
                c = new BooleanOptionUI((BooleanOption) o, editable);
            } else if (o instanceof IntegerOption) {
                c = new IntegerOptionUI((IntegerOption) o, editable);
            } else if (o instanceof SelectOption) {
                c = new SelectOptionUI((SelectOption) o, editable);
            } else if (o instanceof FreeColAction) {
                c = new FreeColActionUI((FreeColAction) o, this);
            } else {
                logger.warning("Unknown option.");
            }
            if (c != null) {
                add(c);
                ou.add(c);
            }
        }
        optionUpdaters = (OptionUpdater[]) ou.toArray(new OptionUpdater[0]);

        setBorder(BorderFactory.createTitledBorder(option.getName()));
        setOpaque(false);
    }


    /**
     * Unregister <code>PropertyChangeListener</code>s.
     */
    public void unregister() {
        for (int i=0; i<optionUpdaters.length; i++) {
            optionUpdaters[i].updateOption();
        }    
    }
    
    /**
    * Updates the value of the {@link Option} this object keeps.
    */
    public void updateOption() {
        for (int i=0; i<optionUpdaters.length; i++) {
            optionUpdaters[i].updateOption();
        }
    }
    
    
    /**
    * Removes the given <code>KeyStroke</code> from all of this
    * <code>OptionGroupUI</code>'s children.
    * 
    * @param keyStroke The <code>KeyStroke</code> to be removed.
    */
    public void removeKeyStroke(KeyStroke keyStroke) {
        for (int i=0; i<optionUpdaters.length; i++) {
            if (optionUpdaters[i] instanceof FreeColActionUI) {
                ((FreeColActionUI) optionUpdaters[i]).removeKeyStroke(keyStroke);
            }
        }
    }

}
