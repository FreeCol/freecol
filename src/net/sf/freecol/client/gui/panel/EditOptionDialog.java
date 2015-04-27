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

import javax.swing.JFrame;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.option.OptionUI;
import net.sf.freecol.common.option.Option;


/**
 * Dialog to edit options with.
 */
public class EditOptionDialog extends FreeColConfirmDialog {

    private final OptionUI ui;


    /**
     * Create an EditOptionDialog.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     * @param option The <code>Option</code> to operate on.
     */
    public EditOptionDialog(FreeColClient freeColClient, JFrame frame, Option option) {
        super(freeColClient, frame);

        ui = OptionUI.getOptionUI(getGUI(), option, true);

        MigPanel panel = new MigPanel(new MigLayout());
        if (ui.getJLabel() == null) panel.add(ui.getJLabel(), "split 2");
        panel.add(ui.getComponent());

        initializeConfirmDialog(frame, true, panel, null, "ok", "cancel");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getResponse() {
        Boolean result = super.getResponse();
        if (result && ui != null) {
            ui.updateOption();
        }
        return result;
    }
}
