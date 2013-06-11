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

import javax.swing.JLabel;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.GoodsType;


/**
 * This label represents AbstractGoods.
 */
public class AbstractGoodsLabel extends JLabel {

    private final AbstractGoods goods;

    private boolean partialChosen = false;

    private boolean toEquip = false;

    private GUI gui;


    /**
     * Initializes this JLabel with the given goods data.
     *
     * @param goods The <code>AbstractGoods</code> that this JLabel
     *     will visually represent.
     * @param gui The <code>GUI</code> to extract an icon from.
     */
    public AbstractGoodsLabel(AbstractGoods goods, GUI gui) {
        super(gui.getImageLibrary().getGoodsImageIcon(goods.getType()));
        this.goods = goods;
        setToolTipText(Messages.message(goods.getNameKey()));
        this.gui = gui;
    }


    /**
     * Get the label gui.
     *
     * @return The label gui.
     */
    protected GUI getGUI() {
        return gui;
    }

    /**
     * Has a partial amount been selected?
     *
     * @return True if a partial amount has been selected.
     */
    public boolean isPartialChosen() {
        return partialChosen;
    }

    /**
     * Set the partial amount state.
     *
     * @param partialChosen The new partial amount state.
     */
    public void setPartialChosen(boolean partialChosen) {
        this.partialChosen = partialChosen;
    }

    /**
     * Has unit equipping been selected?
     *
     * @return True if a unit equipping operation has been selected.
     */
    public boolean isToEquip() {
        return toEquip;
    }

    /**
     * Set the unit equipping state.
     *
     * @param toEquip The new unit equipping state.
     */
    public void toEquip(boolean toEquip) {
        this.toEquip = toEquip;
    }

    /**
     * Get the goods data.
     *
     * @return The goods data for this label.
     */
    public AbstractGoods getGoods() {
        return goods;
    }

    /**
     * Get the goods type.
     *
     * @return The goods type for this label.
     */
    public GoodsType getType() {
        return goods.getType();
    }

    /**
     * Get the goods amount.
     *
     * @return The goods amount.
     */
    public int getAmount() {
        return goods.getAmount();
    }

    /**
     * Sets the amount of the goods wrapped by this Label to a default
     * value.  By default, do nothing.  Override this method if
     * necessary.
     */
    public void setDefaultAmount() {
        // do nothing
    }
}
