/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

package net.sf.freecol.client.gui.dialog;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.io.FreeColDirectories;


/**
 * Dialog for changing the {@link net.sf.freecol.client.ClientOptions}.
 */
public final class ClientOptionsDialog extends OptionsDialog {

    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param frame The owner frame.
     */
    public ClientOptionsDialog(FreeColClient freeColClient, JFrame frame, boolean editable) {
        super(freeColClient, freeColClient.getClientOptions(),
              freeColClient.getClientOptions().getId(),
              FreeColDirectories.CLIENT_OPTIONS_FILE_NAME,
              ClientOptions.TAG, editable);
        
        final List<JButton> buttons = new ArrayList<>();
        if (editable) {
            final JButton resetButton = Utility.localizedButton("reset");
            resetButton.addActionListener(e -> {
                getOptionUI().reset();
            });
            buttons.add(resetButton);
            
            final JButton applyDefaults = Utility.localizedButton("revertToDefaults");
            applyDefaults.addActionListener(e -> {
                load(FreeColDirectories.getBaseClientOptionsFile());
            });
            buttons.add(applyDefaults);
        }
        
        initialize(frame, buttons);
    }
}
