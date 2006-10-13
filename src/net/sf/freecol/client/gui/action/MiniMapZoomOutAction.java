

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;


/**
 * An action for zooming out on the minimap.
 */
public class MiniMapZoomOutAction extends MapboardAction {
    private static final Logger logger = Logger.getLogger(MiniMapZoomOutAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "miniMapZoomOutAction";


    /**
     * Creates a new <code>MiniMapZoomOutAction</code>.
     * @param freeColClient The main controller object for the client.
     */
    MiniMapZoomOutAction(FreeColClient freeColClient) {
        super(freeColClient, "unit.state.10", null, KeyEvent.VK_PLUS, KeyStroke.getKeyStroke('+', 0));
        putValue(BUTTON_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_ZOOM_OUT, 0));
        putValue(BUTTON_ROLLOVER_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_ZOOM_OUT, 1));
        putValue(BUTTON_PRESSED_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_ZOOM_OUT, 2));
        putValue(BUTTON_DISABLED_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_ZOOM_OUT, 3));
    }
    
    
    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if the minimap can be zoomed out.
     */
    protected boolean shouldBeEnabled() {
        MapControlsAction mca = (MapControlsAction) getFreeColClient().getActionManager().getFreeColAction(MapControlsAction.ID);
        return super.shouldBeEnabled()
                && mca.getMapControls() != null
                && mca.getMapControls().canZoomOut();
    }      
    
    /**
    * Returns the id of this <code>Option</code>.
    * @return "miniMapZoomOutAction"
    */
    public String getId() {
        return ID;
    }

    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */    
    public void actionPerformed(ActionEvent e) {
        MapControlsAction mca = (MapControlsAction) getFreeColClient().getActionManager().getFreeColAction(MapControlsAction.ID);
        mca.getMapControls().zoomOut();
        update();
        getFreeColClient().getActionManager().getFreeColAction(MiniMapZoomInAction.ID).update();
    }
}
