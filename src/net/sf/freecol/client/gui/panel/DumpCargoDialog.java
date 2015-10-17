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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Unit;


/**
 * This panel is used to handle dumping cargo.
 */
public final class DumpCargoDialog extends FreeColDialog<List<Goods>> {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(DumpCargoDialog.class.getName());

    /** The list of goods to choose what to dump from. */
    private final List<Goods> goodsList;

    /** Check boxes corresponding to the goods list. */
    private final List<JCheckBox> checkBoxes;


    /**
     * Creates a dialog for choosing cargo for a unit to dump.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     * @param unit The <code>Unit</code> that is dumping cargo.
     */
    public DumpCargoDialog(FreeColClient freeColClient, JFrame frame, Unit unit) {
        super(freeColClient, frame);

        this.goodsList = unit.getGoodsList();
        this.checkBoxes = new ArrayList<>(goodsList.size());

        for (Goods goods : goodsList) {
            // FIXME: find out why check box is not displayed when icon
            // is present
            JCheckBox checkBox
                = new JCheckBox(Messages.message(goods.getLabel(true)),
                                //lib.getSmallImageIcon(goods.getType()),
                                true);
            checkBoxes.add(checkBox);
        }

        MigPanel panel = new MigPanel(new MigLayout("wrap 1", "", ""));
        panel.add(Utility.localizedHeader("dumpCargo", true));
        for (JCheckBox c : checkBoxes) panel.add(c);
        panel.setSize(panel.getPreferredSize());

        List<Goods> fake = null;
        List<ChoiceItem<List<Goods>>> c = choices();
        c.add(new ChoiceItem<>(Messages.message("ok"), fake)
            .okOption().defaultOption());
        c.add(new ChoiceItem<>(Messages.message("cancel"), fake)
            .cancelOption());
        initializeDialog(frame, DialogType.QUESTION, false, panel,
            new ImageIcon(getImageLibrary().getUnitImage(unit)), c);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<Goods> getResponse() {
        Object value = getValue();
        List<Goods> gl = new ArrayList<>();
        if (options.get(0).equals(value)) {
            for (int i = 0; i < checkBoxes.size(); i++) {
                if (checkBoxes.get(i).isSelected()) gl.add(goodsList.get(i));
            }
        }
        return gl;
    }            
}
