/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.util.Utils;


/**
 * This label represents AbstractGoods.
 */
public class AbstractGoodsLabel extends FreeColLabel {
    
    public enum AmountType {
        DEFAULT,
        PARTIAL,
        FULL,
        
        /**
         * Special flag for SHIFT+ALT drag functionality on
         * {@code DefaultTransferHandler}.
         */
        SUPER_FULL
    }

    private final ImageLibrary lib;
    private final AbstractGoods abstractGoods;

    private AmountType amountType = AmountType.DEFAULT;


    /**
     * Initializes this JLabel with the given goods data.
     *
     * @param freeColClient The enclosing {@code FreeColClient}.
     * @param abstractGoods The {@code AbstractGoods} that this JLabel
     *     will visually represent.
     */
    public AbstractGoodsLabel(FreeColClient freeColClient,
                              AbstractGoods abstractGoods) {
        super(new ImageIcon(freeColClient.getGUI().getFixedImageLibrary()
                .getScaledGoodsTypeImage(abstractGoods.getType())));

        this.lib = freeColClient.getGUI().getFixedImageLibrary();
        this.abstractGoods = abstractGoods;

        setToolTipText(Messages.getName(abstractGoods));
    }

    /**
     * Get the image library for this label.
     *
     * @return The {@code ImageLibrary}.
     */
    protected ImageLibrary getImageLibrary() {
        return this.lib;
    }
    
    /**
     * Sets the type of transfer being used on drag.
     * @param amountType
     */
    public void setAmountType(AmountType amountType) {
        this.amountType = (amountType == null) ? AmountType.DEFAULT : amountType;
    }

    /**
     * Has the SHIFT-ALT been pressed on drag?
     * 
     * @return True if this label was dragged with SHIFT-ALT
     */
    public boolean isSuperFullChosen() {
        return amountType == AmountType.SUPER_FULL;
    }

    /**
     * Has a partial amount been selected?
     *
     * @return True if a partial amount has been selected.
     */
    public boolean isPartialChosen() {
        return amountType == AmountType.PARTIAL;
    }

    /**
     * Has full amount been selected?
     *
     * @return True if a full amount has been selected.
     */
    public boolean isFullChosen() {
        return amountType == AmountType.FULL;
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
                && this.amountType == other.amountType;
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
        hash = 31 * hash + this.amountType.ordinal();
        return hash;
    }
}
