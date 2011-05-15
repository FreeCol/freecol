/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.GoodsType;


/**
 * This label represents AbstractGoods.
 */
public class AbstractGoodsLabel extends JLabel {

    private final AbstractGoods goods;

    private final Canvas parent;

    private boolean partialChosen = false;

    private boolean toEquip = false;


    /**
     * Initializes this JLabel with the given goods data.
     *
     * @param goods The AbstractGoods that this JLabel will visually represent.
     * @param parent The parent that knows more than we do.
     */
    public AbstractGoodsLabel(AbstractGoods goods, Canvas parent) {
        this(goods, parent, false);
    }

    /**
     * Initializes this JLabel with the given goods data.
     *
     * @param goods The AbstractGoods that this JLabel will visually represent.
     * @param parent The parent that knows more than we do.
     * @param isSmall A smaller picture will be used if <code>true</code>.
     */
    public AbstractGoodsLabel(AbstractGoods goods, Canvas parent, boolean isSmall) {
        super(parent.getImageLibrary().getGoodsImageIcon(goods.getType()));
        this.goods = goods;
        setToolTipText(Messages.message(goods.getNameKey()));
        this.parent = parent;
        setSmall(isSmall);
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
     * Returns the parent Canvas object.
     *
     * @return This UnitLabel's Canvas.
     */
    public Canvas getCanvas() {
        return parent;
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

    /**
     * Sets that this <code>GoodsLabel</code> should be small.
     *
     * @param isSmall A smaller picture will be used if <code>true</code>.
     */
    public void setSmall(boolean isSmall) {
        if (isSmall) {
            ImageIcon imageIcon = parent.getImageLibrary().getGoodsImageIcon(goods.getType());
            setIcon(new ImageIcon(imageIcon.getImage().getScaledInstance(imageIcon.getIconWidth() / 2,
                    imageIcon.getIconHeight() / 2, Image.SCALE_DEFAULT)));
        } else {
            setIcon(parent.getImageLibrary().getGoodsImageIcon(goods.getType()));
        }
    }
}
