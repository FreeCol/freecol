/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
     * Return true if not the entire amount has been selected.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isPartialChosen() {
        return partialChosen;
    }

    /**
     * Set whether only a partial amount is to be selected.
     *
     * @param partialChosen a <code>boolean</code> value
     */
    public void setPartialChosen(boolean partialChosen) {
        this.partialChosen = partialChosen;
    }

    /**
     * Returns
     *
     * @return a <code>boolean</code> value
     */
    public boolean isToEquip() {
        return toEquip;
    }

    /**
     * Set whether the goods will be used to equip a Unit.
     *
     * @param toEquip a <code>boolean</code> value
     */
    public void toEquip(boolean toEquip) {
        this.toEquip = toEquip;
    }

    /**
     * Returns this GoodsLabel's goods data.
     *
     * @return This GoodsLabel's goods data.
     */
    public AbstractGoods getGoods() {
        return goods;
    }

    /**
     * Returns this label's goods type.
     * @return This label's goods type.
     */
    public GoodsType getType() {
        return goods.getType();
    }

    /**
     * Returns this label's goods amount.
     * @return This label's goods amount.
     */
    public int getAmount() {
        return goods.getAmount();
    }
    
    protected GUI getGUI() {
        return gui;
    }

}
