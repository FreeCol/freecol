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

import java.awt.Color;
import java.awt.Image;

import javax.swing.ImageIcon;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * This label holds Goods data in addition to the JLabel data, which
 * makes it ideal to use for drag and drop purposes.
 */
public final class GoodsLabel extends AbstractGoodsLabel
    implements Draggable {

    GUI gui;
 
    /**
     * Initializes this JLabel with the given goods data.
     *
     * @param goods The Goods that this JLabel will visually represent.
     */
    public GoodsLabel(GUI gui, Goods goods) {
        super(gui.getImageLibrary(), goods);

        this.gui = gui;
        initialize();
    }


    /**
     * Public routine to get a foreground color for a given goods type and
     * amount in a given location.
     *
     * @param goodsType The <code>GoodsType</code> to use.
     * @param amount The amount of goods.
     * @param location The <code>Location</code> for the goods.
     * @return A suitable <code>color</code>.
     */
    public static Color getColor(GoodsType goodsType, int amount,
                                 Location location) {
        String key = (!goodsType.limitIgnored()
            && location instanceof Colony
            && ((Colony)location).getWarehouseCapacity() < amount)
            ? "color.foreground.GoodsLabel.capacityExceeded"
            : (location instanceof Colony && goodsType.isStorable()
                && ((Colony)location).getExportData(goodsType).getExported())
            ? "color.foreground.GoodsLabel.exported"
            : (amount == 0)
            ? "color.foreground.GoodsLabel.zeroAmount"
            : (amount < 0)
            ? "color.foreground.GoodsLabel.negativeAmount"
            : "color.foreground.GoodsLabel.positiveAmount";
        return ResourceManager.getColor(key);
    }

    /**
     * Initialize this label.
     */
    private void initialize() {
        final Goods goods = getGoods();
        final Location location = goods.getLocation();
        final Player player = (location instanceof Ownable)
            ? ((Ownable)location).getOwner()
            : null;
        final GoodsType type = goods.getType();
        final Specification spec = goods.getGame().getSpecification();

        if (getAmount() < GoodsContainer.CARGO_SIZE) setPartialChosen(true);

        if (player == null
            || !type.isStorable()
            || player.canTrade(type)
            || (location instanceof Colony
                && spec.getBoolean(GameOptions.CUSTOM_IGNORE_BOYCOTT)
                && ((Colony)location).hasAbility(Ability.EXPORT))) {
            Utility.localizeToolTip(this, goods.getLabel(true));
        } else {
            Utility.localizeToolTip(this, goods.getLabel(false));
            setIcon(getDisabledIcon());
        }

        setForeground(getColor(type, goods.getAmount(), location));
        super.setText(String.valueOf(goods.getAmount()));
    }

    /**
     * Get the goods being labelled.
     *
     * @return The <code>Goods</code> we have labelled.
     */
    public Goods getGoods() {
        return (Goods)getAbstractGoods();
    }


    // Override AbstractGoods

    /**
     * Set whether only a partial amount is to be selected.
     *
     * @param partialChosen The new partial choice.
     */
    @Override
    public void setPartialChosen(boolean partialChosen) {
        super.setPartialChosen(partialChosen);
        ImageLibrary lib = gui.getImageLibrary();
        Image image = partialChosen
            ? lib.getSmallIconImage(getType())
            : lib.getIconImage(getType());
        setIcon(new ImageIcon(image));
    }


    // Implement Draggable

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOnCarrier() {
        Goods goods = getGoods();
        return goods != null && goods.getLocation() instanceof Unit;
    }
}
