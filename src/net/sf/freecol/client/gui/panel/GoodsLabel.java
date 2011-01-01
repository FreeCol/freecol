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
import java.util.logging.Logger;

import javax.swing.ImageIcon;
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
public final class GoodsLabel extends JLabel {
    
    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(GoodsLabel.class.getName());
    
    private final Goods goods;
    
    private final Canvas parent;
    
    private boolean partialChosen;
    
    private boolean toEquip;
    
    
    /**
     * Initializes this JLabel with the given goods data.
     *
     * @param goods The Goods that this JLabel will visually represent.
     * @param parent The parent that knows more than we do.
     */
    public GoodsLabel(Goods goods, Canvas parent) {
        super(parent.getImageLibrary().getGoodsImageIcon(goods.getType()));
        this.goods = goods;
        setToolTipText(Messages.message(goods.getNameKey()));
        this.parent = parent;
        partialChosen = false;
        toEquip = false;
        initializeDisplay();
    }
    
    /**
     * Initializes this JLabel with the given goods data.
     *
     * @param goods The Goods that this JLabel will visually represent.
     * @param parent The parent that knows more than we do.
     * @param isSmall A smaller picture will be used if <code>true</code>.
     */
    public GoodsLabel(Goods goods, Canvas parent, boolean isSmall) {
        this(goods, parent);
        setSmall(true);
    }
    
    /**
     * Initializes the display that shows the goods.
     */
    private void initializeDisplay() {
        Player player = null;
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
    
    public boolean isPartialChosen() {
        return partialChosen;
    }
    
    public void setPartialChosen(boolean partialChosen) {
        this.partialChosen = partialChosen;
    }
    
    public boolean isToEquip() {
        return toEquip;
    }
    
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
    public Goods getGoods() {
        return goods;
    }
    
    /**
     * Sets whether or not this goods should be selected.
     *
     * @param b Whether or not this goods should be selected.
     */
    public void setSelected(boolean b) {
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
