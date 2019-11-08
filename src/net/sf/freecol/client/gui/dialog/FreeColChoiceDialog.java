/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingConstants;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.StringTemplate;


/**
 * A simple modal choice dialog.
 */
public class FreeColChoiceDialog<T> extends FreeColDialog<T> {

    /**
     * Internal constructor.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param frame The owner frame.
     */
    protected FreeColChoiceDialog(FreeColClient freeColClient, JFrame frame) {
        super(freeColClient, frame);
    }

    /**
     * Create a new {@code FreeColChoiceDialog} with a text and a
     * ok/cancel option.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param frame The owner frame.
     * @param modal True if this dialog should be modal.
     * @param obj An object that explains the choice for the user.
     * @param icon An optional icon to display.
     * @param cancelKey Key for the text of the optional cancel button.
     * @param choices A list of {@code ChoiceItem}s to create buttons for.
     */
    public FreeColChoiceDialog(final FreeColClient freeColClient, JFrame frame,
                               boolean modal, StringTemplate tmpl,
                               ImageIcon icon, String cancelKey,
                               List<ChoiceItem<T>> choices) {
        this(freeColClient, frame);

        initializeChoiceDialog(frame, modal,
            Utility.localizedLabel(tmpl, icon, SwingConstants.LEFT),
            null, cancelKey, choices);
    }


    /**
     * @param frame The owner frame.
     * @param modal True if this dialog should be modal.
     * @param jc An object that explains the choice for the user.
     * @param icon An optional icon to display.
     * @param cancelKey Key for the text of the optional cancel button.
     * @param choices A list of {@code ChoiceItem}s to create buttons for.
     */
    protected final void initializeChoiceDialog(JFrame frame, boolean modal,
                                                JComponent jc, ImageIcon icon,
                                                String cancelKey,
                                                List<ChoiceItem<T>> choices) {
        if (cancelKey != null) {
            choices.add(new ChoiceItem<>(Messages.message(cancelKey), (T)null)
                .cancelOption().defaultOption());
        }
        initializeDialog(frame, DialogType.PLAIN, modal, jc, icon, choices);
    }
}
