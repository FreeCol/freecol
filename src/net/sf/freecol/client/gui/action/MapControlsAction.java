

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.panel.MapControls;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;


/**
* An action for displaying the map controls.
* @see MapControls
*/
public class MapControlsAction extends MapboardAction {
    private static final Logger logger = Logger.getLogger(MapControlsAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "mapControlsAction";

    private MapControls mapControls;
    private boolean selected = true;



    /**
     * Creates a new <code>MapControlsAction</code>.
     * @param freeColClient The main controller object for the client.
     */
    MapControlsAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.view.mapControls", null, KeyEvent.VK_M, KeyStroke.getKeyStroke('M', InputEvent.CTRL_MASK));
    }

    /**
     * Updates the "enabled"-status and
     * calls {@link #showMapControls(boolean)}.
     */
    public void update() {
        super.update();
        
        final Game game = getFreeColClient().getGame();
        final Player p = getFreeColClient().getMyPlayer();
        if (game != null && p != null && game.getModelMessageIterator(p).hasNext()) {
            enabled = false;
        }
        showMapControls(enabled && selected);
    }

    /**
    * Returns the id of this <code>Option</code>.
    * @return "mapControlsAction"
    */
    public String getId() {
        return ID;
    }


    /**
    * Returns The MapControls object.
    * @return The MapControls object.
    */
    public MapControls getMapControls() {
        return mapControls;
    }


    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */    
    public void actionPerformed(ActionEvent e) {
        selected = ((AbstractButton) e.getSource()).isSelected();
        showMapControls(selected);
    }


    /**
     * Checks if the map controls is selcted.
     * @return <code>true</code> if the map controls is selected.
     */
    public boolean isSelected() {
        return selected;
    }


    private void showMapControls(boolean value) {
        if (value && getFreeColClient().getGUI().isInGame()) {
            if (mapControls == null ) {
                mapControls = new MapControls(getFreeColClient(), getFreeColClient().getGUI());
            }
            mapControls.update();
        }
        if (mapControls != null) {
            if (value && !mapControls.isShowing()) {
                mapControls.addToComponent(getFreeColClient().getCanvas());                
            } else if (!value && mapControls.isShowing()) {
                mapControls.removeFromComponent(getFreeColClient().getCanvas());
            }
        }
    }
}
