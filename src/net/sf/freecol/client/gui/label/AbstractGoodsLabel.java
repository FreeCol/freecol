/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

package net.sf.freecol.client.gui.label;


import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.util.Utils;


/**
 * This label represents AbstractGoods.
 */
public class AbstractGoodsLabel extends FreeColLabel {

    private final AbstractGoods abstractGoods;

    private boolean partialChosen;

    private boolean fullChosen;

    /**
     * Special flag for SHIFT+ALT drag functionality on
     * {@code DefaultTransferHandler}.
     */
    private boolean superFullChosen;


    /**
     * Initializes this JLabel with the given goods data.
     *
     * @param lib The {@code ImageLibrary} to use to display the label.
     * @param abstractGoods The {@code AbstractGoods} that this JLabel
     *     will visually represent.
     */
    public AbstractGoodsLabel(ImageLibrary lib, AbstractGoods abstractGoods) {
        super(new ImageIcon(lib.getScaledGoodsTypeImage(abstractGoods.getType())));

        this.abstractGoods = abstractGoods;

        setToolTipText(Messages.getName(abstractGoods));
    }

    /**
     * Has the SHIFT-ALT been pressed on drag?
     * 
     * @return True if this label was dragged with SHIFT-ALT
     */
    public boolean isSuperFullChosen() {
        return superFullChosen;
    }

    /**
     * Set DRAG-ALL functionality when SHIFT+ALT used on drag from {@code DragListener}
     * 
     * @param superFullChosen
     *            The new state of drag-all 
     */
    public void setSuperFullChosen(boolean superFullChosen) {
        this.superFullChosen = superFullChosen;
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

    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof AbstractGoodsLabel) {
            AbstractGoodsLabel other = (AbstractGoodsLabel)o;
            return Utils.equals(this.abstractGoods, other.abstractGoods)
                && this.partialChosen == other.partialChosen
                && this.fullChosen == other.fullChosen
                && this.superFullChosen == other.superFullChosen;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 31 * hash + Utils.hashCode(this.abstractGoods);
        hash = 31 * hash + ((this.partialChosen) ? 1 : 0)
            + ((this.fullChosen) ? 2 : 0)
            + ((this.superFullChosen) ? 4 : 0);
        return hash;
    }
}
