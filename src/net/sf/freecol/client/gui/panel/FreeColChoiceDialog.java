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

    /**
     * {@inheritDoc}
     */
    protected FreeColChoiceDialog(FreeColClient freeColClient) {
        super(freeColClient);
    }

    /**
     * Create a new <code>FreeColChoiceDialog</code> with a text and a
     * ok/cancel option.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param text The text that explains the choice for the user.
     * @param icon An optional icon to display.
     * @param cancelText Optional text for a cancel option that returns null.
     * @param choices A list of <code>ChoiceItem</code>s to create buttons for.
     * @return The <code>FreeColChoiceDialog</code> created.
     */
    public FreeColChoiceDialog(final FreeColClient freeColClient,
                               String text, ImageIcon icon, String cancelText,
                               List<ChoiceItem<T>> choices) {
        this(freeColClient);

        initialize(text, icon, cancelText, choices);
    }


    /**
     * @param text An object that explains the choice for the user.
     * @param icon An optional icon to display.
     * @param cancelText Optional text for a cancel option that returns null.
     * @param choices A list of <code>ChoiceItem</code>s to create buttons for.
     */
    protected void initialize(Object text, ImageIcon icon, String cancelText,
                              List<ChoiceItem<T>> choices) {
        if (cancelText != null) {
            choices.add(new ChoiceItem<T>(Messages.message(cancelText),
                    (T)null).cancelOption().defaultOption());
        }
        initialize(DialogType.PLAIN, true, text, icon, choices);
    }
}
