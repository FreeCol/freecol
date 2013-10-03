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

import java.awt.event.ActionEvent;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.option.OptionUI;
import net.sf.freecol.client.gui.panel.MigPanel;

import net.sf.freecol.common.option.Option;

import net.miginfocom.swing.MigLayout;


/**
 * Dialog to edit options with.
 */
public class EditOptionDialog extends FreeColDialog<Boolean> {

    private OptionUI ui;


    /**
     * Create an EditOptionDialog.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param option The <code>Option</code> to operate on.
     */
    public EditOptionDialog(FreeColClient freeColClient, Option option) {
        super(freeColClient);

        ui = OptionUI.getOptionUI(getGUI(), option, true);

        MigPanel panel = new MigPanel(new MigLayout());
        if (ui.getLabel() == null) panel.add(ui.getLabel(), "split 2");
        panel.add(ui.getComponent());

        initialize(DialogType.QUESTION, true, panel, null, new String[] {
                Messages.message("ok"),
                Messages.message("cancel")
            });
    }


    /**
     * {@inheritDoc}
     */
    public Boolean getResponse() {
        Object value = getValue();
        if (value == options[0]) {
            ui.updateOption();
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
}
