/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import javax.swing.JFrame;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.option.OptionGroup;


/**
 * Dialog for changing the {@link net.sf.freecol.client.ClientOptions}.
 */
public final class ClientOptionsDialog extends OptionsDialog  {

    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     */
    public ClientOptionsDialog(FreeColClient freeColClient, JFrame frame) {
        super(freeColClient, frame, true, freeColClient.getClientOptions(),
              freeColClient.getClientOptions().getId(),
              FreeColDirectories.CLIENT_OPTIONS_FILE_NAME,
              ClientOptions.getXMLElementTagName());

        // Client options are loaded early

        initialize(frame);
    }


    // Override OptionsDialog

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionGroup getResponse() {
        OptionGroup value = super.getResponse();
        if (value != null) {
            saveDefaultOptions();
        }
        return value;
    }
}
