/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;

import static net.sf.freecol.common.util.StringUtils.*;


/**
 * Display text over tiles.
 */
public final class DisplayTileTextAction extends SelectableAction {

    public static final String id = "displayTileTextAction.";

    // FIXME: make ClientOptions use enum
    public static enum DisplayText {
        EMPTY, NAMES, OWNERS, REGIONS;

        public String getKey() {
            return getEnumKey(this);
        }
    };

    private static final int[] accelerators = {
        KeyEvent.VK_E,
        KeyEvent.VK_N,
        KeyEvent.VK_O,
        KeyEvent.VK_R
    };

    private DisplayText display = null;


    /**
     * Creates this action
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param type a {@code DisplayText} value
     */
    public DisplayTileTextAction(FreeColClient freeColClient,
                                 DisplayText type) {
        super(freeColClient, id + type.getKey(),
              ClientOptions.DISPLAY_TILE_TEXT);
        display = type;
        setAccelerator(KeyStroke.getKeyStroke(accelerators[type.ordinal()],
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        setSelected(shouldBeSelected());
    }


    // Override SelectableAction

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shouldBeSelected() {
        return display != null
            && freeColClient.getClientOptions() != null
            && freeColClient.getClientOptions().getDisplayTileText()
                == display.ordinal()
            && super.shouldBeEnabled();
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        if (((JRadioButtonMenuItem)ae.getSource()).isSelected()) {
            freeColClient.getClientOptions()
                .setInteger(ClientOptions.DISPLAY_TILE_TEXT, display.ordinal());
            getGUI().refresh();
        }
    }
}
