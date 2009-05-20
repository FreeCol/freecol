/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;

import net.miginfocom.swing.MigLayout;

/**
 * This dialog is used to edit an Indian settlement (map editor only).
 */
public final class EditSettlementDialog extends FreeColDialog<IndianSettlement> {

    private static final Logger logger = Logger.getLogger(EditSettlementDialog.class.getName());

    private final IndianSettlement settlement;

    private final JCheckBox capital;
    private final JComboBox owner;

    /**
     * The constructor that will add the items to this panel.
     */
    public EditSettlementDialog(Canvas canvas, IndianSettlement settlement) {
        
        super(canvas);
        this.settlement = settlement;

        okButton.addActionListener(this);

        Vector<Nation> natives = new Vector<Nation>();
        for (Player player : settlement.getGame().getPlayers()) {
            if (player.isAI() && player.isIndian()) {
                natives.add(player.getNation());
            }
        }

        setLayout(new MigLayout("wrap 2, gapx 20", "", ""));

        add(new JLabel(Messages.message("nation")));
        owner = new JComboBox(natives);
        add(owner);

        add(new JLabel(Messages.message("capital")));
        capital = new JCheckBox();
        capital.setSelected(settlement.isCapital());
        add(capital);
        
        add(okButton, "newline 20, span, split 2, tag ok");
        add(cancelButton, "tag cancel");

        setSize(getPreferredSize());
    }

    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            Nation newNation = (Nation) owner.getSelectedItem();
            if (newNation != settlement.getOwner().getNation()) {
                settlement.setOwner(settlement.getGame().getPlayer(newNation.getId()));
            }
            if (capital.isSelected() && !settlement.isCapital()) {
                // make sure we downgrade the old capital
                for (IndianSettlement indianSettlement : settlement.getOwner().getIndianSettlements()) {
                    indianSettlement.setCapital(false);
                }
                settlement.setCapital(true);
            } else if (!capital.isSelected() && settlement.isCapital()) {
                settlement.setCapital(false);
            }
            getCanvas().remove(this);
        }
    }
}
