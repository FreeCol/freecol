/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */



package net.sf.freecol.client.gui.action;

import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;


/**
* Super class for all actions that should be disabled when the mapboard is not selected.
*/
public abstract class MapboardAction extends FreeColAction {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MapboardAction.class.getName());


    /**
     * Creates a new <code>MapboardAction</code>.
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
    protected MapboardAction(FreeColClient freeColClient, String id) {
        super(freeColClient, id);
    }

    protected MapboardAction(FreeColClient freeColClient, String name, String shortDescription, int mnemonic, KeyStroke accelerator) {
        super(freeColClient, name, shortDescription, mnemonic, accelerator);
    }
    
    protected MapboardAction(FreeColClient freeColClient, String name, String shortDescription, int mnemonic) {
        super(freeColClient, name, shortDescription, mnemonic);
    }

    protected MapboardAction(FreeColClient freeColClient, String name, String shortDescription, KeyStroke accelerator) {
        super(freeColClient, name, shortDescription, accelerator);
    }
    
    protected MapboardAction(FreeColClient freeColClient, String name, String shortDescription) {
        super(freeColClient, name, shortDescription);
    }
    
    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if the mapboard is selected.
     */
    protected boolean shouldBeEnabled() { 
        return super.shouldBeEnabled()  
            && getFreeColClient().getCanvas() != null
            && getFreeColClient().getCanvas().isMapboardActionsEnabled()
            && (getFreeColClient().getGame() == null
                || getFreeColClient().getGame().getCurrentPlayer() == getFreeColClient().getMyPlayer());
    }
}
