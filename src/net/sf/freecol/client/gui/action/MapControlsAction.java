

package net.sf.freecol.client.gui.action;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.panel.MapControls;

import javax.swing.AbstractButton;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import java.util.logging.Logger;


/**
* An action for displaying the map controls.
* @see MapControls
*/
public class MapControlsAction extends MapboardAction {
    private static final Logger logger = Logger.getLogger(MapControlsAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "mapControlsAction";

    private MapControls mapControls;
    private boolean selected = true;



    /**
    * Creates a new <code>MapControlsAction</code>
    */
    MapControlsAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.view.mapControls", null, KeyEvent.VK_M, KeyStroke.getKeyStroke('M', InputEvent.CTRL_MASK));
    }


    /**
    * Displays the map controls if the mapboard is selected.
    * @see MapControls
    */
    public void update() {
        super.update();
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


    public void actionPerformed(ActionEvent e) {
        selected = ((AbstractButton) e.getSource()).isSelected();
        showMapControls(selected);
    }


    public boolean isSelected() {
        return selected;
    }


    private void showMapControls(boolean selected) {
        if (selected && getFreeColClient().getGUI().isInGame()) {
            if (mapControls == null ) {
                mapControls = new MapControls(getFreeColClient(), getFreeColClient().getGUI());
            }
            mapControls.update();
        }
        if (mapControls != null) {
            if (selected && !mapControls.isShowing()) {
                mapControls.addToComponent(getFreeColClient().getCanvas());
            } else if (!selected && mapControls.isShowing()) {
                mapControls.removeFromComponent(getFreeColClient().getCanvas());
            }
        }
    }
}
