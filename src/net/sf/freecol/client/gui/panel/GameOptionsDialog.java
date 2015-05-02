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

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.option.BooleanOptionUI;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.option.OptionGroup;


/**
 * Dialog for changing the {@link net.sf.freecol.common.model.GameOptions}.
 */
public final class GameOptionsDialog extends OptionsDialog {


    /**
     * Creates a game options dialog.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     * @param editable Whether the game options can be modified.
     * @param custom Whether to load custom options.
     */
    public GameOptionsDialog(FreeColClient freeColClient, JFrame frame,
            boolean editable, boolean custom) {
        super(freeColClient, frame, editable,
            freeColClient.getGame().getGameOptions(),
            GameOptions.getXMLElementTagName(),
            FreeColDirectories.GAME_OPTIONS_FILE_NAME,
            GameOptions.getXMLElementTagName());

        if (isEditable()) loadDefaultOptions();

        // Set special cases
        // Disable victory option "All humans defeated"
        // when playing single player
        if (isEditable() && freeColClient.isSinglePlayer()) {
            BooleanOptionUI comp = (BooleanOptionUI) getOptionUI()
                .getOptionUI(GameOptions.VICTORY_DEFEAT_HUMANS);
            if (comp != null) {
                comp.setValue(false);
                comp.getComponent().setEnabled(false);
            }
        }

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
            if (isEditable()) saveDefaultOptions();
        }
        return value;
    }
}
