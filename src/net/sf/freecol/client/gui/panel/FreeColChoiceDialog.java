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

import java.util.List;

import javax.swing.ImageIcon;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.client.gui.panel.FreeColDialog;

/**
 * A simple modal choice dialog.
 */
public class FreeColChoiceDialog<T> extends FreeColDialog<T> {

    private final List<ChoiceItem<T>> choices;


    /**
     * Create a new <code>FreeColChoiceDialog</code> with a text and a
     * ok/cancel option.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param text The text that explains the choice for the user.
     * @param icon An optional icon to display.
     * @param cancelText Optional text for a cancel option that returns null.
     * @param choices The <code>List</code> containing the ChoiceItems to
     *            create buttons for.
     * @return The <code>FreeColChoiceDialog</code> created.
     */
    public FreeColChoiceDialog(final FreeColClient freeColClient,
                                String text, ImageIcon icon, String cancelText,
                                List<ChoiceItem<T>> choices) {
        super(freeColClient);

        this.choices = choices;

        int len = choices.size() + ((cancelText == null) ? 0 : 1);
        String[] options = new String[len];
        for (int i = 0; i < choices.size(); i++) {
            options[i] = choices.get(i).toString();
        }
        if (cancelText != null) {
            options[len-1] = Messages.message(cancelText);
        }
        initialize(DialogType.PLAIN, true, text, icon, options);
    }


    /**
     * {@inheritDoc}
     */
    public T getResponse() {
        Object value = getValue();
        for (int i = 0; i < options.length; i++) {
            ChoiceItem<T> c = choices.get(i);
            if (c.toString().equals(value)) return c.getObject();
        }
        return null;
    }
}
