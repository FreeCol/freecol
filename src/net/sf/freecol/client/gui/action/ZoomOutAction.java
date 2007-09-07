package net.sf.freecol.client.gui.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;
import javax.swing.KeyStroke;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;

/**
 * An action for chosing the next unit as the active unit.
 */
public class ZoomOutAction extends FreeColAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ZoomOutAction.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision: 1945 $";

    public static final String ID = "zoomOutAction";


    /**
     * Creates a new <code>ZoomOutAction</code>.
     * 
     * @param freeColClient The main controller object for the client.
     */
    ZoomOutAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.view.zoomOut", null, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0));
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if the mapboard is selected
     *      and can be zoomed onto.
     */
    protected boolean shouldBeEnabled() {
        if (!super.shouldBeEnabled()) {
            return false;
        } else {
            float oldScaling = getFreeColClient().getGUI().getImageLibrary().getScalingFactor();
            return ((oldScaling - 1/8f) * 8 > 1);
        }
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return "zoomInAction"
     */
    public String getId() {
        return ID;
    }

    /**
     * Applies this action.
     * 
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        float oldScaling = getFreeColClient().getGUI().getImageLibrary().getScalingFactor();
        ImageLibrary im = getFreeColClient().getImageLibrary().getScaledImageLibrary(oldScaling - 1/8f);
        getFreeColClient().getGUI().setImageLibrary(im);
        getFreeColClient().getGUI().forceReposition();
        getFreeColClient().getCanvas().refresh();
        
        update();
        freeColClient.getActionManager().getFreeColAction(ZoomInAction.ID).update();
    }
}
