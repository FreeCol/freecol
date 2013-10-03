/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.option.OptionGroup;


/**
 * Dialog for changing the {@link net.sf.freecol.client.ClientOptions}.
 */
public final class ClientOptionsDialog extends OptionsDialog  {

    private static final Logger logger = Logger.getLogger(ClientOptionsDialog.class.getName());


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ClientOptionsDialog(FreeColClient freeColClient) {
        super(freeColClient, true, freeColClient.getClientOptions(),
            freeColClient.getClientOptions().getName(),
            "options.xml", "clientOptions");

        initialize();
    }


    /**
     * {@inheritDoc}
     */
    public OptionGroup getResponse() {
        Object value = getValue();
        if (options[0].equals(value)) {
            File file = FreeColDirectories.getClientOptionsFile();
            OptionGroup group = getGroup();
            try {
                group.save(file);
            } catch (FileNotFoundException e) {
                logger.log(Level.WARNING, "Save failure", e);
            }
            return group;
        }
        return null;
    }
}
