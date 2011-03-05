/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.awt.event.ActionListener;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.option.BooleanOptionUI;
import net.sf.freecol.common.model.GameOptions;



/**
 * Dialog for changing the {@link net.sf.freecol.common.model.GameOptions}.
 */
public final class GameOptionsDialog extends OptionsDialog implements ActionListener {

    public static final String OPTION_GROUP_ID = "gameOptions";

    /**
     * The constructor that will add the items to this panel.
     *
     * @param parent The parent of this panel.
     */
    public GameOptionsDialog(Canvas parent, boolean editable) {
        super(parent, editable);
        initialize(getSpecification().getOptionGroup(OPTION_GROUP_ID),
                   Messages.message(OPTION_GROUP_ID), null);

        // Set special cases
        // Disable victory option "All humans defeated"
        //when playing single player
        if (editable && getClient().isSingleplayer()) {
            BooleanOptionUI comp = (BooleanOptionUI) getOptionUI().getOptionUI(GameOptions.VICTORY_DEFEAT_HUMANS);

            comp.setValue(false);
            comp.setEnabled(false);
        }
    }

    public String getDefaultFileName() {
        return "game_options.xml";
    }

    public String getOptionGroupId() {
        return OPTION_GROUP_ID;
    }

}
