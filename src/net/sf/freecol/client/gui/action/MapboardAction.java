

package net.sf.freecol.client.gui.action;

import net.sf.freecol.client.FreeColClient;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import java.util.logging.Logger;


/**
* Super class for all actions that should be disabled when the mapboard is not selected.
*/
public abstract class MapboardAction extends FreeColAction {
    private static final Logger logger = Logger.getLogger(MapboardAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    /**
    * Creates a new <code>MapboardAction</code>.
    */
    protected MapboardAction(FreeColClient freeColClient, String name, String shortDescription, int mnemonic, KeyStroke accelerator) {
        super(freeColClient, name, shortDescription, mnemonic, accelerator);
    }
    

    
    /**
    * Disables this option if the mapboard is not selected and otherwise enables this option.
    */
    public void update() {
        super.update();
        
        if (enabled) {
            setEnabled(getFreeColClient().getCanvas() != null
                    && !getFreeColClient().getCanvas().getColonyPanel().isShowing()
                    && !getFreeColClient().getCanvas().getEuropePanel().isShowing()
                    && !getFreeColClient().getCanvas().getChooseFoundingFatherDialog().isShowing()
                    && !getFreeColClient().getCanvas().getEventPanel().isShowing());
        }

/*
        if (enabled) {
            setEnabled(getFreeColClient().getCanvas() != null && !getFreeColClient().getCanvas().isShowingSubPanel());
        }*/
    }
}
