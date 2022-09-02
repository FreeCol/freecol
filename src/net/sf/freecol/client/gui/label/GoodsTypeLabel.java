/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.util.Utils;


/**
 * This label represents a goods type.
 */
public class GoodsTypeLabel extends FreeColLabel {

    /** The image library used for this label. */
    private final ImageLibrary imageLibrary;

    /** The goods type represented. */
    private final GoodsType goodsType;


    /**
     * Initializes this label with the given goods type data.
     *
     * @param freeColClient The enclosing {@code FreeColClient}.
     * @param goodsType The {@code GoodsType} to represent.
     */
    public GoodsTypeLabel(FreeColClient freeColClient, GoodsType goodsType) {
        this(freeColClient.getGUI().getFixedImageLibrary(), goodsType);
    }
    
    public GoodsTypeLabel(GoodsTypeLabel goodsTypeLabel) {
        this(goodsTypeLabel.getImageLibrary(), goodsTypeLabel.getType());
    }

    /**
     * Initializes this label with the given goods type data.
     *
     * @param imageLibrary The {@code ImageLibrary} to create the icon with.
     * @param goodsType The {@code GoodsType} to represent.
     */
    private GoodsTypeLabel(ImageLibrary imageLibrary, GoodsType goodsType) {
        super(new ImageIcon(imageLibrary.getScaledGoodsTypeImage(goodsType)));

        this.imageLibrary = imageLibrary;
        this.goodsType = goodsType;

        setToolTipText(Messages.getName(goodsType));
    }


    /**
     * Get the image library for this label.
     *
     * @return The {@code ImageLibrary}.
     */
    protected ImageLibrary getImageLibrary() {
        return this.imageLibrary;
    }

    /**
     * Get the goods type of this label.
     *
     * @return The {@code GoodsType}.
     */
    public GoodsType getType() {
        return this.goodsType;
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof GoodsTypeLabel) {
            GoodsTypeLabel other = (GoodsTypeLabel)o;
            return Utils.equals(this.goodsType, other.goodsType);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 31 * hash + Utils.hashCode(this.goodsType);
        return hash;
    }
}
