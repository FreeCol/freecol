

package net.sf.freecol.client.gui.action;

import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.panel.MapControls;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;

/**
* An action for displaying the map controls.
* @see MapControls
*/
public abstract class SelectableAction extends MapboardAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(SelectableAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "selectableAction";

    protected boolean selected = false;

    /**
     * Creates this action.
     * @param freeColClient The main controller object for the client
     * @param name An i18n-key to identify the name of this action.
     * @param shortDescription An i18n-key to identify a short 
     *      description of this action. This value can be set to
     *      <code>null</code> if the action does not have a
     *      description.
     * @param mnemonic A mnemonic to be used for selecting this action
     *      when the action is displaying on a menu etc.
     * @param accelerator The keyboard accelerator to be used for
     *      selecting this action or <code>null</code> if this action
     *      does not have an accelerator.
     */
    protected SelectableAction(FreeColClient freeColClient, String name, String shortDescription, int mnemonic, KeyStroke accelerator) {
        super(freeColClient, name, shortDescription, mnemonic, accelerator);
    }
    
    protected SelectableAction(FreeColClient freeColClient, String name, String shortDescription, int mnemonic) {
        super(freeColClient, name, shortDescription, mnemonic);
    }

    protected SelectableAction(FreeColClient freeColClient, String name, String shortDescription, KeyStroke accelerator) {
        super(freeColClient, name, shortDescription, accelerator);
    }
    
    protected SelectableAction(FreeColClient freeColClient, String name, String shortDescription) {
        super(freeColClient, name, shortDescription);
    }

    /**
     * Updates the "enabled"-status 
     */
    public void update() {
        super.update();
        
        final Game game = getFreeColClient().getGame();
        final Player p = getFreeColClient().getMyPlayer();
        if (game != null && p != null && game.getModelMessageIterator(p).hasNext()) {
            enabled = false;
        }
    }

    /**
    * Returns the id of this <code>Option</code>.
    * @return "mapControlsAction"
    */
    public String getId() {
        return ID;
    }

    /**
     * Checks if the map controls is selcted.
     * @return <code>true</code> if the map controls is selected.
     */
    public boolean isSelected() {
        return selected;
    }
    
    public void setSelected(boolean b) {
        this.selected = b;
    }
}
