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
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.client.gui.panel.FreeColDialog;


/**
 * A FreeColDialog with input field/s.
 */
public abstract class FreeColInputDialog<T> extends FreeColDialog<T> {

    private static final Logger logger = Logger.getLogger(FreeColInputDialog.class.getName());


    /**
     * {@inheritDoc}
     */
    protected FreeColInputDialog(FreeColClient freeColClient) {
        super(freeColClient);
    }

    /**
     * Create a new <code>FreeColInputDialog</code> with an object and
     * a ok/cancel option.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param obj The object containing the input fields and
     *     explanation to the user.
     * @param icon An optional icon to display.
     * @param okText The text displayed on the "ok"-button.
     * @param cancelText The text displayed on the optional "cancel"-button.
     * @return The <code>FreeColDialog</code> created.
     */
    public FreeColInputDialog(final FreeColClient freeColClient,
                              Object obj, ImageIcon icon,
                              String okText, String cancelText) {
        this(freeColClient);

        initialize(obj, icon, okText, cancelText);
    }


    /**
     * Initialize this input dialog.
     *
     * @param obj The object containing the input fields and
     *     explanation to the user.
     * @param icon An optional icon to display.
     * @param okText The text displayed on the "ok"-button.
     * @param cancelText The text displayed on the optional "cancel"-button.
     */
    protected void initialize(Object obj, ImageIcon icon,
                              String okText, String cancelText) {
        List<ChoiceItem<T>> c = choices();
        c.add(new ChoiceItem<T>(Messages.message(okText),
                (T)null).okOption());
        if (cancelText != null) {
            c.add(new ChoiceItem<T>(Messages.message(cancelText),
                    (T)null).cancelOption().defaultOption());
        }
        initialize(DialogType.QUESTION, true, obj, icon, c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getResponse() {
        Object value = getValue();
        return (value == this.options.get(0)) ? getInputValue() : (T)null;
    }


    /**
     * Extract the input value from the input field/s.
     * To be supplied by implementations.
     *
     * @return The value of the input field/s.
     */
    abstract protected T getInputValue();
}
