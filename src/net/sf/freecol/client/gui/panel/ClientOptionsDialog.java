/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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


package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.action.MapControlsAction;
import net.sf.freecol.common.model.StringTemplate;


/**
 * Dialog for changing the {@link net.sf.freecol.client.ClientOptions}.
 */
public final class ClientOptionsDialog extends OptionsDialog  {

    private static final Logger logger = Logger.getLogger(ClientOptionsDialog.class.getName());

    public static final String OPTION_GROUP_ID = "clientOptions";

    /**
     * The constructor that will add the items to this panel.
     * @param freeColClient 
     * @param parent The parent of this panel.
     */
    public ClientOptionsDialog(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui, true);
        getButtons().clear();
        initialize(getClientOptions(), getClientOptions().getName(), null);
    }

    /**
     * {@inheritDoc}
     */
    public String getDefaultFileName() {
        return "options.xml";
    }

    /**
     * {@inheritDoc}
     */
    public String getOptionGroupId() {
        return OPTION_GROUP_ID;
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent event) {
        super.actionPerformed(event);
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            File file = new File(FreeCol.getOptionsDirectory(), getDefaultFileName());
            try {
                getGroup().save(file);
                getFreeColClient().getActionManager().update();
                getGUI().resetMenuBar();
                // Immediately redraw the minimap if that was updated.
                MapControlsAction mca = (MapControlsAction) getFreeColClient()
                    .getActionManager().getFreeColAction(MapControlsAction.id);
                if (mca.getMapControls() != null) {
                    mca.getMapControls().update();
                }
            } catch(FileNotFoundException e) {
                logger.warning(e.toString());
                StringTemplate t = StringTemplate.template("failedToSave")
                    .addName("%name%", file.getPath());
                getGUI().showInformationMessage(t);
            }
        }
    }
}
