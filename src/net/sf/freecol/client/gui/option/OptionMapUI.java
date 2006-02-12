
package net.sf.freecol.client.gui.option;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.OptionMap;
import net.sf.freecol.common.option.SelectOption;



/**
* This class provides visualization for an {@link OptionMap}. In order to
* enable values to be both seen and changed.
*/
public final class OptionMapUI extends JPanel implements OptionUpdater {
    private static final Logger logger = Logger.getLogger(OptionMapUI.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private final OptionMap option;
    private final OptionUpdater[] optionUpdaters;
    private final JTabbedPane tb;


    /**
    * Creates a new <code>OptionMapUI</code> for the given <code>OptionMap</code>.
    * @param option The <code>OptionMap</code> to make a user interface for.
    */
    public OptionMapUI(OptionMap option) {
        super(new BorderLayout());
        this.option = option;

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.setOpaque(false);

        tb = new JTabbedPane(JTabbedPane.TOP);
        tb.setOpaque(false);

        List ou = new ArrayList();
        Iterator it = option.iterator();
        while (it.hasNext()) {
            Option o = (Option) it.next();

            if (o instanceof OptionGroup) {
                JComponent c = new OptionGroupUI((OptionGroup) o);
                c.setBorder(BorderFactory.createEmptyBorder(OptionGroupUI.H_GAP-5,0,0,0));
                c.setOpaque(true);
                tb.addTab(o.getName(), null, c, o.getShortDescription());
                ou.add(c);
            } else if (o instanceof BooleanOption) {
                JComponent c = new BooleanOptionUI((BooleanOption) o);
                northPanel.add(c);
                ou.add(c);
            } else if (o instanceof IntegerOption) {
                JComponent c = new IntegerOptionUI((IntegerOption) o);
                northPanel.add(c);
                ou.add(c);
            } else if (o instanceof SelectOption) {
                JComponent c = new SelectOptionUI((SelectOption) o);
                northPanel.add(c);
                ou.add(c);
            } else {
                logger.warning("Unknown option.");
            }
        }
        optionUpdaters = (OptionUpdater[]) ou.toArray(new OptionUpdater[0]);

        add(northPanel, BorderLayout.NORTH);
        add(tb, BorderLayout.CENTER);

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
