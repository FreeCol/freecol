

package net.sf.freecol.client.gui.action;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Tile;

import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import java.util.logging.Logger;


/**
* An action for using the active unit to plow/clear a forest.
*/
public class PlowAction extends MapboardAction {
    private static final Logger logger = Logger.getLogger(PlowAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    
    public static final String ID = "plowAction";


    /**
    * Creates a new <code>PlowAction</code>.
    */
    PlowAction(FreeColClient freeColClient) {
        super(freeColClient, "unit.state.5", null, KeyEvent.VK_P, KeyStroke.getKeyStroke('P', 0));
    }
    
    

    /**
    * Updates this action. If there is no active unit or the active unit cannot plow/clear
    * a forest then <code>setEnabled(false)</code> gets called.
    */
    public void update() {
        super.update();

        Unit selectedOne = getFreeColClient().getGUI().getActiveUnit();
        if (enabled && selectedOne != null && selectedOne.getTile() != null) {
            Tile tile = selectedOne.getTile();
            if(tile.isLand() && tile.isForested()) {
                putValue(NAME, Messages.message("unit.state.4"));
                setEnabled(selectedOne.isPioneer());
            } else if (tile.isLand() && !tile.isForested() && !tile.isPlowed()) {
                putValue(NAME, Messages.message("unit.state.5"));
                setEnabled(selectedOne.isPioneer());
            } else {
                putValue(NAME, Messages.message("unit.state.5"));
                setEnabled(false);
            }
        } else {
            putValue(NAME, Messages.message("unit.state.5"));
            setEnabled(false);
        }
    }

    
    /**
    * Returns the id of this <code>Option</code>.
    * @return "plowAction"
    */
    public String getId() {
        return ID;
    }



    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getInGameController().changeState(getFreeColClient().getGUI().getActiveUnit(), Unit.PLOW);
    }
}
