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


import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.GoodsType;


/**
 * This label represents AbstractGoods.
 */
public class AbstractGoodsLabel extends JLabel {

    private final AbstractGoods abstractGoods;

    private boolean partialChosen = false;

    private boolean fullChosen = false;


    /**
     * Initializes this JLabel with the given goods data.
     *
     * @param abstractGoods The <code>AbstractGoods</code> that this JLabel
     *     will visually represent.
     */
    public AbstractGoodsLabel(ImageLibrary lib, AbstractGoods abstractGoods) {
        super(new ImageIcon(lib.getIconImage(abstractGoods.getType())));

        this.abstractGoods = abstractGoods;

        setToolTipText(Messages.getName(abstractGoods));
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
     * Has full amount been selected?
     *
     * @return True if a full amount has been selected.
     */
    public boolean isFullChosen() {
        return fullChosen;
    }

    /**
     * Set the full amount state.
     *
     * @param fullChosen The new full amount state.
     */
    public void setFullChosen(boolean fullChosen) {
        this.fullChosen = fullChosen;
    }

    /**
     * Get the goods data.
     *
     * @return The goods data for this label.
     */
    public AbstractGoods getAbstractGoods() {
        return abstractGoods;
    }

    /**
     * Get the goods type.
     *
     * @return The goods type for this label.
     */
    public GoodsType getType() {
        return abstractGoods.getType();
    }

    /**
     * Get the goods amount.
     *
     * @return The goods amount.
     */
    public int getAmount() {
        return abstractGoods.getAmount();
    }

    /**
     * Set the goods amount.
     *
     * @param amount The amount of goods.
     */
    public void setAmount(int amount) {
        abstractGoods.setAmount(amount);
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
