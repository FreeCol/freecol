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
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.common.i18n.Messages;


/**
 * A FreeColDialog with input field/s.
 */
public abstract class FreeColInputDialog<T> extends FreeColDialog<T> {

    private static final Logger logger = Logger.getLogger(FreeColInputDialog.class.getName());


    /**
     * Internal constructor.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     */
    protected FreeColInputDialog(FreeColClient freeColClient, JFrame frame) {
        super(freeColClient, frame);
    }

    /**
     * Create a new <code>FreeColInputDialog</code> with an object and
     * a ok/cancel option.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     * @param modal True if this dialog should be modal.
     * @param obj The object containing the input fields and
     *     explanation to the user.
     * @param icon An optional icon to display.
     * @param okKey The key displayed on the "ok"-button.
     * @param cancelKey The key displayed on the optional "cancel"-button.
     */
    public FreeColInputDialog(final FreeColClient freeColClient, JFrame frame,
            boolean modal, Object obj, ImageIcon icon,
            String okKey, String cancelKey) {
        this(freeColClient, frame);

        initializeInputDialog(frame, modal, obj, icon, okKey, cancelKey);
    }


    /**
     * Initialize this input dialog.
     *
     * @param frame The owner frame.
     * @param modal True if this dialog should be modal.
     * @param obj The object containing the input fields and
     *     explanation to the user.
     * @param icon An optional icon to display.
     * @param okKey The key displayed on the "ok"-button.
     * @param cancelKey The key displayed on the optional "cancel"-button.
     */
    protected final void initializeInputDialog(JFrame frame, boolean modal,
            Object obj, ImageIcon icon, String okKey, String cancelKey) {
        List<ChoiceItem<T>> c = choices();
        c.add(new ChoiceItem<>(Messages.message(okKey), (T)null).okOption());
        if (cancelKey != null) {
            c.add(new ChoiceItem<>(Messages.message(cancelKey), (T)null)
                .cancelOption().defaultOption());
        }
        initializeDialog(frame, DialogType.QUESTION, modal, obj, icon, c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getResponse() {
        if (responded()) {
            Object value = getValue();
            if (value == this.options.get(0)) return getInputValue();
        }
        return null;
    }


    /**
     * Extract the input value from the input field/s.
     * To be supplied by implementations.
     *
     * @return The value of the input field/s.
     */
    abstract protected T getInputValue();
}
