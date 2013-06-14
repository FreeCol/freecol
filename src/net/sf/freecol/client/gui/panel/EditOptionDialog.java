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

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.option.OptionUI;
import net.sf.freecol.common.option.Option;


/**
 * Dialog to edit options with.
 */
public class EditOptionDialog extends FreeColDialog<Boolean> {

    private OptionUI ui;


    public EditOptionDialog(FreeColClient freeColClient, Option option) {
        super(freeColClient);

        setLayout(new MigLayout()); 
        ui = OptionUI.getOptionUI(getGUI(), option, editable);
        if (ui.getLabel() == null) {
            add(ui.getLabel(), "split 2");
        }
        add(ui.getComponent());

        add(okButton, "newline, split 2, tag ok");
        add(cancelButton, "tag cancel");
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            ui.updateOption();
            setResponse(true);
        } else {
            setResponse(false);
        }
    }

}
