package net.sf.freecol.client.gui.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.panel.MapControls;

/**
 * An action for displaying the map controls.
 * 
 * @see MapControls
 */
public class MapControlsAction extends SelectableAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MapControlsAction.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public static final String ID = "mapControlsAction";

    private MapControls mapControls;


    /**
     * Creates this action.
     * 
     * @param freeColClient The main controller object for the client.
     */
    MapControlsAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.view.mapControls", null, KeyStroke.getKeyStroke('M', Toolkit.getDefaultToolkit()
                .getMenuShortcutKeyMask()));

        setSelected(true);
    }

    /**
     * Updates the "enabled"-status and calls {@link #showMapControls(boolean)}.
     */
    public void update() {
        super.update();

        showMapControls(enabled && isSelected());
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return "mapControlsAction"
     */
    public String getId() {
        return ID;
    }

    /**
     * Returns The MapControls object.
     * 
     * @return The MapControls object.
     */
    public MapControls getMapControls() {
        return mapControls;
    }

    /**
     * Applies this action.
     * 
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        selected = ((AbstractButton) e.getSource()).isSelected();
        showMapControls(enabled && selected);
    }

    private void showMapControls(boolean value) {
        if (value && getFreeColClient().getGUI().isInGame()) {
            if (mapControls == null) {
                mapControls = new MapControls(getFreeColClient(), getFreeColClient().getGUI());
            }
            mapControls.update();
        }
        if (mapControls != null) {
            if (value) {
                if (!mapControls.isShowing()) {
                    mapControls.addToComponent(getFreeColClient().getCanvas());
                }
                mapControls.update();
            } else {
                if (mapControls.isShowing()) {
                    mapControls.removeFromComponent(getFreeColClient().getCanvas());
                }
            }
        }
    }
}
