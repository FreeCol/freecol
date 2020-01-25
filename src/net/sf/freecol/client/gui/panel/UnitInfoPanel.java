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

package net.sf.freecol.client.gui.panel;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * Panel for displaying {@code Unit}-information.
 */
public final class UnitInfoPanel extends FreeColPanel
    implements PropertyChangeListener {

    private static final int SLACK = 5; // Small gap

    /** The unit to display. */
    private Unit unit;


    /**
     * Create a new unit information panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public UnitInfoPanel(FreeColClient freeColClient) {
        super(freeColClient, null,
              new MigLayout("wrap 5, fill, gap 0 0", "", ""));

        setSize(260, 130);
        setBorder(null);
        setOpaque(false);
        this.unit = null;
    }


    /**
     * Get the tile under the unit displayed.
     *
     * @return The <code>Tile</code>.
     */
    public Tile getTile() {
        return (this.unit == null) ? null : this.unit.getTile();
    }

    /**
     * Updates this unit information panel to use a new unit.
     *
     * @param unit The displayed {@code Unit} (may be null).
     */
    public void update(Unit unit) {
        if (this.unit != unit) {
            if (this.unit != null) {
                this.unit.removePropertyChangeListener(this);
                GoodsContainer gc = this.unit.getGoodsContainer();
                if (gc != null) gc.removePropertyChangeListener(this);
            }
            if (unit != null) {
                unit.addPropertyChangeListener(this);
                GoodsContainer gc = unit.getGoodsContainer();
                if (gc != null) gc.addPropertyChangeListener(this);
            }
            this.unit = unit;
            update();
        }
    }

    /**
     * Unconditionally update this panel.
     */
    private void update() {
        removeAll();

        final ImageLibrary lib = getGUI().getTileImageLibrary();
        Font font = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.TINY, lib.getScaleFactor());
        String text;
        JLabel textLabel;
        if (this.unit != null) {
            ImageIcon ii = new ImageIcon(lib.getScaledUnitImage(this.unit));
            JLabel imageLabel = new JLabel(ii);
            add(imageLabel, "spany, gapafter 5px");
            int width = getWidth() - ii.getIconWidth() - SLACK;
            text = this.unit.getDescription(Unit.UnitLabelType.FULL);
            for (String s : splitText(text, " /",
                                      getFontMetrics(font), width)) {
                textLabel = new JLabel(s);
                textLabel.setFont(font);
                add(textLabel, "span 5");
            }

            text = (this.unit.isInEurope())
                ? Messages.getName(this.unit.getOwner().getEurope())
                : Messages.message("infoPanel.moves")
                    + " " + this.unit.getMovesAsString();
            textLabel = new JLabel(text);
            textLabel.setFont(font);
            add(textLabel, "span 5");

            if (this.unit.isCarrier()) {
                ImageIcon icon;
                JLabel label;
                for (Goods goods : this.unit.getGoodsList()) {
                    int amount = goods.getAmount();
                    GoodsType gt = goods.getType();
                    // FIXME: Get size of full stack from appropriate place.
                    if(amount == 100) {
                        icon = new ImageIcon(lib.getScaledGoodsTypeImage(gt));
                        label = new JLabel(icon);
                    } else {
                        icon = new ImageIcon(lib.getSmallGoodsTypeImage(gt));
                        label = new JLabel(String.valueOf(amount),
                                           icon, JLabel.RIGHT);
                    }
                    text = Messages.message(goods.getLabel(true));
                    label.setFont(font);
                    label.setToolTipText(text);
                    add(label);
                }
                for (Unit carriedUnit : this.unit.getUnitList()) {
                    icon = new ImageIcon(lib.getSmallerUnitImage(carriedUnit));
                    label = new JLabel(icon);
                    text = carriedUnit.getDescription(Unit.UnitLabelType.NATIONAL);
                    label.setFont(font);
                    label.setToolTipText(text);
                    add(label);
                }
            }
        }
        revalidate();
    }


    // Interface PropertyChangeListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        update();
    }
}
