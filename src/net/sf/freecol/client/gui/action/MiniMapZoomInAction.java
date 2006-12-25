

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;


/**
 * An action for zooming in on the minimap.
 */
public class MiniMapZoomInAction extends MapboardAction {
    private static final Logger logger = Logger.getLogger(MiniMapZoomInAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = MiniMapZoomInAction.class.toString();


    /**
     * Creates a new <code>MiniMapZoomInAction</code>.
     * @param freeColClient The main controller object for the client.
     */
    MiniMapZoomInAction(FreeColClient freeColClient) {
        super(freeColClient, "unit.state.9", null, KeyEvent.VK_PLUS, KeyStroke.getKeyStroke('+', 0));
        putValue(BUTTON_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_ZOOM_IN, 0));
        putValue(BUTTON_ROLLOVER_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_ZOOM_IN, 1));
        putValue(BUTTON_PRESSED_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_ZOOM_IN, 2));
        putValue(BUTTON_DISABLED_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_ZOOM_IN, 3));
    }
    
    
    /**
    * Returns the id of this <code>Option</code>.
    * @return "miniMapZoomInAction"
    */
    public String getId() {
        return ID;
    }
    

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if the minimap can be zoomed in.
     */
    protected boolean shouldBeEnabled() {
        MapControlsAction mca = (MapControlsAction) getFreeColClient().getActionManager().getFreeColAction(MapControlsAction.ID);
        return super.shouldBeEnabled()
                && mca.getMapControls() != null
                && mca.getMapControls().canZoomIn();
    }  
    
    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */    
    public void actionPerformed(ActionEvent e) {
        MapControlsAction mca = (MapControlsAction) getFreeColClient().getActionManager().getFreeColAction(MapControlsAction.ID);
        mca.getMapControls().zoomIn();
        update();
        getFreeColClient().getActionManager().getFreeColAction(MiniMapZoomOutAction.ID).update();
    }
}
