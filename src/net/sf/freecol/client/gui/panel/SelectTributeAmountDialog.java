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

import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StringTemplate;

import net.miginfocom.swing.MigLayout;


/**
 * The panel that allows a choice of tribute amount.
 */
public final class SelectTributeAmountDialog extends FreeColInputDialog<Integer> {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(SelectTributeAmountDialog.class.getName());

    private final int COLUMNS = 5;

    /** The field to contain the input. */
    private JTextField input;

    /** The maxumum amount allowed. */
    private final int maximum;


    /**
     * The constructor to use.
     *
     * @param freeColClient The enclosing <code>FreeColClient</code>.
     * @param question A <code>StringTemplate</code> describing the
     *     input required.
     * @param maximum The inclusive maximum integer input value.
     */
    public SelectTributeAmountDialog(FreeColClient freeColClient,
                                     StringTemplate question,
                                     int maximum) {
        super(freeColClient);

        this.maximum = maximum;
        this.input = new JTextField(Integer.toString(maximum), COLUMNS);

        MigPanel panel = new MigPanel(new MigLayout("wrap 1"));
        panel.add(GUI.getDefaultTextArea(Messages.message(question)));
        panel.add(this.input);
        
        panel.setSize(panel.getPreferredSize());

        initialize(true, panel, null, "ok", "cancel");
    }

    /**
     * {@inheritDoc}
     */
    protected Integer getInputValue() {
        int result;
        try {
            result = Integer.parseInt(input.getText());
        } catch (NumberFormatException nfe) {
            return null;
        }
        return (result <= 0 || result > maximum) ? null : result;
    }
}
