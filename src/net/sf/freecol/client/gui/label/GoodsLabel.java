/**
 * Copyright (C) 2002-2019   The FreeCol Team
 *
 * This file is part of FreeCol.
 *
 * FreeCol is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * FreeCol is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.label;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;

import javax.swing.ImageIcon;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.panel.CargoPanel;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.option.GameOptions;


/**
 * This label holds Goods data in addition to the JLabel data, which
 * makes it ideal to use for drag and drop purposes.
 */
public final class GoodsLabel extends AbstractGoodsLabel
        implements CargoLabel, Draggable {


    /**
     * Initializes this FreeColLabel with the given goods data.
     *
     * @param gui The {@code GUI} to display on.
     * @param goods The {@code Goods} that this label will represent.
     */
    public GoodsLabel(GUI gui, Goods goods) {
        super(gui.getImageLibrary(), goods);

        initialize();
    }


    /**
     * Initialize this label.
     */
    private void initialize() {
        final Goods goods = getGoods();
        final Location location = goods.getLocation();
        final Player player = (location instanceof Ownable)
                              ? ((Ownable) location).getOwner()
                              : null;
        final GoodsType type = goods.getType();
        final Specification spec = goods.getGame().getSpecification();

        if (getAmount() < GoodsContainer.CARGO_SIZE) setPartialChosen(true);

        setForeground(ImageLibrary.getGoodsColor(type, goods.getAmount(),
                                                 location));
        setText(String.valueOf(goods.getAmount()));

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
    }


    /**
     * Get the goods being labelled.
     *
     * @return The {@code Goods} we have labelled.
     */
    public Goods getGoods() {
        return (Goods) getAbstractGoods();
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


    // Interface CargoLabel

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addCargo(Component comp, Unit carrier, CargoPanel cargoPanel) {
        Goods goods = ((GoodsLabel) comp).getGoods();
        int loadable = carrier.getLoadableAmount(goods.getType());
        if (loadable <= 0) return false;
        if (loadable > goods.getAmount()) loadable = goods.getAmount();
        Goods toAdd = new Goods(goods.getGame(), goods.getLocation(),
                                goods.getType(), loadable);
        goods.setAmount(goods.getAmount() - loadable);
        cargoPanel.igc().loadCargo(toAdd, carrier);
        cargoPanel.update();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeCargo(Component comp, CargoPanel cargoPanel) {
        Goods g = ((GoodsLabel) comp).getGoods();
        cargoPanel.igc().unloadCargo(g, false);
        cargoPanel.update();
    }
}
