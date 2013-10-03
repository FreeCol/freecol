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

import javax.swing.ImageIcon;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.FreeColDialog;


/**
 * A simple modal yes/no or ok/cancel dialog.
 */
public class FreeColConfirmDialog extends FreeColDialog<Boolean> {

    private static final Boolean values[] = { Boolean.TRUE, Boolean.FALSE };


    /**
     * Create a new <code>FreeColConfirmDialog</code> with a text and a
     * ok/cancel option.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param text The text that explains the choice for the user.
     * @param icon An optional icon to display.
     * @param okText The text displayed on the "ok"-button.
     * @param cancelText The text displayed on the "cancel"-button.
     * @return The <code>FreeColDialog</code> created.
     */
    public FreeColConfirmDialog(final FreeColClient freeColClient,
                                String text, ImageIcon icon,
                                String okText, String cancelText) {
        super(freeColClient);

        initialize(DialogType.QUESTION, true, text, icon, new String[] {
                Messages.message(okText),
                Messages.message(cancelText)
            });
    }


    /**
     * {@inheritDoc}
     */
    public Boolean getResponse() {
        Object value = getValue();
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) return values[i];
        }
        return null;
    }
}
