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

import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.common.i18n.Messages;


/**
 * A simple modal ok/cancel dialog.
 */
public class FreeColConfirmDialog extends FreeColDialog<Boolean> {

    /**
     * Internal constructor.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     */
    protected FreeColConfirmDialog(FreeColClient freeColClient, JFrame frame) {
        super(freeColClient, frame);
    }

    /**
     * Create a new <code>FreeColConfirmDialog</code> with a text and a
     * ok/cancel option.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     * @param modal True if this dialog should be modal.
     * @param obj An object that explains the choice for the user.
     * @param icon An optional icon to display.
     * @param okKey A key for the text displayed on the "ok"-button.
     * @param cancelKey A key for the text displayed on the "cancel"-button.
     */
    public FreeColConfirmDialog(final FreeColClient freeColClient, JFrame frame,
            boolean modal, Object obj, ImageIcon icon,
            String okKey, String cancelKey) {
        this(freeColClient, frame);

        initializeConfirmDialog(frame, modal, obj, icon, okKey, cancelKey);
    }


    /**
     * Initialize this confirm dialog.
     *
     * @param frame The owner frame.
     * @param modal True if this dialog should be modal.
     * @param obj The object that explains the choice for the user.
     * @param icon An optional icon to display.
     * @param okKey The text displayed on the "ok"-button.
     * @param cancelKey The text displayed on the "cancel"-button.
     */
    protected final void initializeConfirmDialog(JFrame frame, boolean modal,
            Object obj, ImageIcon icon, String okKey, String cancelKey) {
        List<ChoiceItem<Boolean>> c = choices();
        c.add(new ChoiceItem<>(Messages.message(okKey), Boolean.TRUE)
            .okOption());
        c.add(new ChoiceItem<>(Messages.message(cancelKey), Boolean.FALSE)
            .cancelOption().defaultOption());
        initializeDialog(frame, DialogType.QUESTION, modal, obj, icon, c);
    }
}
