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

import java.awt.Image;

import javax.swing.ImageIcon;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.resources.ResourceManager;

/**
 * This label holds Goods data in addition to the JLabel data, which makes it
 * ideal to use for drag and drop purposes.
 */
public final class GoodsLabel extends AbstractGoodsLabel
    implements Draggable {

    /**
     * Initializes this JLabel with the given goods data.
     *
     * @param goods The Goods that this JLabel will visually represent.
     * @param gui The <code>GUI</code> to display on.
     */
    public GoodsLabel(Goods goods, GUI gui) {
        super(goods, gui);
        initializeDisplay();
    }


    /**
     * Initializes the display that shows the goods.
     */
    private void initializeDisplay() {
        Player player = null;
        Goods goods = getGoods();
        Location location = goods.getLocation();

        if (getAmount() < GoodsContainer.CARGO_SIZE) {
            setPartialChosen(true);
        }

        if (location instanceof Ownable) {
            player = ((Ownable) location).getOwner();
        }
        if (player == null
            || !goods.getType().isStorable()
            || player.canTrade(goods.getType())
            || (location instanceof Colony
                && player.getGame().getSpecification().getBoolean(GameOptions.CUSTOM_IGNORE_BOYCOTT)
                && ((Colony) location).hasAbility(Ability.EXPORT))) {
            setToolTipText(Messages.message(goods.getNameKey()));
        } else {
            setToolTipText(Messages.message(goods.getLabel(false)));
            setIcon(getDisabledIcon());
        }

        if (!goods.getType().limitIgnored()
            && location instanceof Colony
            && ((Colony) location).getWarehouseCapacity() < goods.getAmount()) {
            setForeground(ResourceManager.getColor("goodsLabel.capacityExceeded.color"));
        } else if (location instanceof Colony
                   && goods.getType().isStorable()
                   && ((Colony) location).getExportData(goods.getType()).isExported()) {
            setForeground(ResourceManager.getColor("goodsLabel.exported.color"));
        } else if (goods.getAmount() == 0) {
            setForeground(ResourceManager.getColor("goodsLabel.zeroAmount.color"));
        } else if (goods.getAmount() < 0) {
            setForeground(ResourceManager.getColor("goodsLabel.negativeAmount.color"));
        } else {
            setForeground(ResourceManager.getColor("goodsLabel.positiveAmount.color"));
        }

        super.setText(String.valueOf(goods.getAmount()));
    }

    /**
     * Set whether only a partial amount is to be selected.
     *
     * @param partialChosen a <code>boolean</code> value
     */
    @Override
    public void setPartialChosen(boolean partialChosen) {
        super.setPartialChosen(partialChosen);
        Image image = getGUI().getImageLibrary()
            .getGoodsImage(getType(), partialChosen ? 0.75f : 1f);
        setIcon(new ImageIcon(image));
    }

    /**
     * Returns this GoodsLabel's goods data.
     *
     * @return This GoodsLabel's goods data.
     */
    @Override
    public Goods getGoods() {
        return (Goods) super.getGoods();
    }

    public boolean isOnCarrier() {
        Goods goods = getGoods();
        return goods != null && goods.getLocation() instanceof Unit;
    }

}
