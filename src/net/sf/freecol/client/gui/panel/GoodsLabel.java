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


import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.resources.ResourceManager;

/**
 * This label holds Goods data in addition to the JLabel data, which makes it
 * ideal to use for drag and drop purposes.
 */
public final class GoodsLabel extends AbstractGoodsLabel {

    /**
     * Initializes this JLabel with the given goods data.
     *
     * @param goods The Goods that this JLabel will visually represent.
     * @param parent The parent that knows more than we do.
     */
    public GoodsLabel(Goods goods, Canvas parent) {
        this(goods, parent, false);
    }

    /**
     * Initializes this JLabel with the given goods data.
     *
     * @param goods The Goods that this JLabel will visually represent.
     * @param parent The parent that knows more than we do.
     * @param isSmall A smaller picture will be used if <code>true</code>.
     */
    public GoodsLabel(Goods goods, Canvas parent, boolean isSmall) {
        super(goods, parent, isSmall);
        initializeDisplay();
    }

    /**
     * Initializes the display that shows the goods.
     */
    private void initializeDisplay() {
        Player player = null;
        Goods goods = getGoods();
        Location location = goods.getLocation();

        if (location instanceof Ownable) {
            player = ((Ownable) location).getOwner();
        }
        if (player == null
            || !goods.getType().isStorable()
            || player.canTrade(goods)
            || (location instanceof Colony
                && player.getGame().getSpecification().getBoolean(GameOptions.CUSTOM_IGNORE_BOYCOTT)
                && ((Colony) location).hasAbility("model.ability.export"))) {
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
     * Returns this GoodsLabel's goods data.
     *
     * @return This GoodsLabel's goods data.
     */
    @Override
    public Goods getGoods() {
        return (Goods) super.getGoods();
    }

}
