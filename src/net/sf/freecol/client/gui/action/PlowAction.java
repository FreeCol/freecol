package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;

/**
 * An action for using the active unit to plow/clear a forest.
 */
public class PlowAction extends MapboardAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(PlowAction.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public static final String ID = "plowAction";

    private boolean plow;


    /**
     * Creates this action.
     * 
     * @param freeColClient The main controller object for the client.
     */
    PlowAction(FreeColClient freeColClient) {
        super(freeColClient, "unit.state.5", null, KeyStroke.getKeyStroke('P', 0));

        plow = false;
        updateValues(true);
    }

    /**
     * Updates this action to be either a "plow" or a "clear forest".
     * 
     * @param p <code>true</code> if this action should be "clear forest".
     */
    private void updateValues(boolean p) {
        if (plow == p) {
            return;
        }
        plow = p;

        if (plow) {
            putValue(BUTTON_IMAGE, getFreeColClient().getImageLibrary().getUnitButtonImageIcon(
                    ImageLibrary.UNIT_BUTTON_PLOW, 0));
            putValue(BUTTON_ROLLOVER_IMAGE, getFreeColClient().getImageLibrary().getUnitButtonImageIcon(
                    ImageLibrary.UNIT_BUTTON_PLOW, 1));
            putValue(BUTTON_PRESSED_IMAGE, getFreeColClient().getImageLibrary().getUnitButtonImageIcon(
                    ImageLibrary.UNIT_BUTTON_PLOW, 2));
            putValue(BUTTON_DISABLED_IMAGE, getFreeColClient().getImageLibrary().getUnitButtonImageIcon(
                    ImageLibrary.UNIT_BUTTON_PLOW, 3));
            putValue(NAME, Messages.message("unit.state.5"));
        } else {
            putValue(BUTTON_IMAGE, getFreeColClient().getImageLibrary().getUnitButtonImageIcon(
                    ImageLibrary.UNIT_BUTTON_CLEAR, 0));
            putValue(BUTTON_ROLLOVER_IMAGE, getFreeColClient().getImageLibrary().getUnitButtonImageIcon(
                    ImageLibrary.UNIT_BUTTON_CLEAR, 1));
            putValue(BUTTON_PRESSED_IMAGE, getFreeColClient().getImageLibrary().getUnitButtonImageIcon(
                    ImageLibrary.UNIT_BUTTON_CLEAR, 2));
            putValue(BUTTON_DISABLED_IMAGE, getFreeColClient().getImageLibrary().getUnitButtonImageIcon(
                    ImageLibrary.UNIT_BUTTON_CLEAR, 3));
            putValue(NAME, Messages.message("unit.state.4"));
        }
    }

    /**
     * Updates the "enabled"-status with the value returned by
     * {@link #shouldBeEnabled} and updates the name of the action.
     */
    public void update() {
        super.update();

        GUI gui = getFreeColClient().getGUI();
        if (gui != null) {
            Unit selectedOne = getFreeColClient().getGUI().getActiveUnit();
            if (enabled && selectedOne != null && selectedOne.getTile() != null) {
                Tile tile = selectedOne.getTile();
                if (selectedOne.canPlow()) {
                    updateValues(!tile.isForested());
                } else {
                    updateValues(true);
                }
            } else {
                updateValues(true);
            }
        }
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>false</code> if there is no active unit or if the unit
     *         cannot plow/clear forest.
     */
    protected boolean shouldBeEnabled() {
        if (!super.shouldBeEnabled()) {
            return false;
        }

        GUI gui = getFreeColClient().getGUI();
        if (gui == null)
            return false;

        Unit selectedOne = getFreeColClient().getGUI().getActiveUnit();
        return selectedOne != null && selectedOne.getTile() != null && selectedOne.canPlow() && selectedOne.isPioneer()
                && selectedOne.checkSetState(Unit.PLOW);
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return "plowAction"
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
        getFreeColClient().getInGameController().changeState(getFreeColClient().getGUI().getActiveUnit(), Unit.PLOW);
    }
}
