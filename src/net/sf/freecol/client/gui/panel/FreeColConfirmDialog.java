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

import java.util.List;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.client.gui.panel.FreeColDialog;


/**
 * A simple modal ok/cancel dialog.
 */
public class FreeColConfirmDialog extends FreeColDialog<Boolean> {

    /**
     * {@inheritDoc}
     */
    protected FreeColConfirmDialog(FreeColClient freeColClient) {
        super(freeColClient);
    }

    /**
     * Create a new <code>FreeColConfirmDialog</code> with a text and a
     * ok/cancel option.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param modal True if this dialog should be modal.
     * @param text The text that explains the choice for the user.
     * @param icon An optional icon to display.
     * @param okKey A key for the text displayed on the "ok"-button.
     * @param cancelKey A key for the text displayed on the "cancel"-button.
     * @return The <code>FreeColDialog</code> created.
     */
    public FreeColConfirmDialog(final FreeColClient freeColClient,
                                boolean modal, String text, ImageIcon icon,
                                String okKey, String cancelKey) {
        this(freeColClient);

        initialize(modal, text, icon, okKey, cancelKey);
    }


    /**
     * Initialize this confirm dialog.
     *
     * @param modal True if this dialog should be modal.
     * @param obj The object that explains the choice for the user.
     * @param icon An optional icon to display.
     * @param okKey The text displayed on the "ok"-button.
     * @param cancelKey The text displayed on the "cancel"-button.
     */
    protected void initialize(boolean modal, Object obj, ImageIcon icon,
                              String okKey, String cancelKey) {
        List<ChoiceItem<Boolean>> c = choices();
        c.add(new ChoiceItem<Boolean>(Messages.message(okKey),
                Boolean.TRUE).okOption());
        c.add(new ChoiceItem<Boolean>(Messages.message(cancelKey),
                Boolean.FALSE).cancelOption().defaultOption());
        initialize(DialogType.QUESTION, modal, obj, icon, c);
    }
}
