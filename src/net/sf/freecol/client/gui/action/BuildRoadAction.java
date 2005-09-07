

package net.sf.freecol.client.gui.action;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Tile;

import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import java.util.logging.Logger;


/**
* An action for using the active unit to build a road.
*/
public class BuildRoadAction extends MapboardAction {
    private static final Logger logger = Logger.getLogger(BuildRoadAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "buildRoadAction";

    /**
    * Creates a new <code>BuildRoadAction</code>.
    */
    BuildRoadAction(FreeColClient freeColClient) {
        super(freeColClient, "unit.state.6", null, KeyEvent.VK_R, KeyStroke.getKeyStroke('R', 0));
    }
    
    
    /**
    * Updates this action. If there is no active unit or the active unit cannot build a road,
    * then <code>setEnabled(false)</code> gets called.
    */
    public void update() {
        super.update();

        Unit selectedOne = getFreeColClient().getGUI().getActiveUnit();
        if (enabled && selectedOne != null && selectedOne.getTile() != null && selectedOne.isPioneer()) {
            Tile tile = selectedOne.getTile();
            setEnabled(tile.isLand() && !tile.hasRoad() && selectedOne.checkSetState(Unit.BUILD_ROAD));
        } else {
            setEnabled(false);
        }
    }
    
    
    /**
    * Returns the id of this <code>Option</code>.
    * @return "buildRoadAction"
    */
    public String getId() {
        return ID;
    }


    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getInGameController().changeState(getFreeColClient().getGUI().getActiveUnit(), Unit.BUILD_ROAD);
    }
}
