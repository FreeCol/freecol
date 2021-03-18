/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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

import java.awt.Component;
import java.awt.event.MouseListener;

import java.util.List;
import java.util.logging.Logger;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.gui.label.GoodsTypeLabel;

import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Unit;


/**
 * Simple panel for a goods type.
 *
 * Used in the trade route code to show the cargo a carrier is
 * supposed to take on board at a certain stop.
 *
 * FIXME: use in the CaptureGoodsDialog?
 */
public class GoodsTypePanel extends MigPanel implements DropTarget {

    /**
     * Build a new cargo panel.
     */
    public GoodsTypePanel() {
        super(new MigLayout());

        setOpaque(false);
        setBorder(Utility.localizedBorder("cargoOnCarrier"));
    }

    /**
     * Set the labels to display on this panel.
     *
     * @param labels The {@code GoodsTypeLabel}s to display.
     */
    public void setLabels(List<GoodsTypeLabel> labels) {
        removeAll();
        if (labels != null) {
            for (GoodsTypeLabel label : labels) add(label);
        }
        revalidate();
        repaint();
    }

    /**
     * Add a single label.
     *
     * Do not repaint, that will be done top down.
     *
     * @param label The {@code GoodsTypeLabel} to add.
     */
    public void addLabel(GoodsTypeLabel label) {
        if (label != null) {
            add(label);
            revalidate();
        }
    }

    /**
     * Remove labels that correspond to a given goods type.
     *
     * @param gt The {@code GoodsType} to remove.
     */
    public void removeType(GoodsType gt) {
        for (Component child : getComponents()) {
            if (child instanceof GoodsTypeLabel) {
                if (((GoodsTypeLabel)child).getType() == gt) {
                    remove(child);
                }
            }
        }
        revalidate();
    }

    // Implement DropTarget

    /**
     * {@inheritDoc}
     */
    public boolean accepts(GoodsType goodsType) { return true; }

    /**
     * {@inheritDoc}
     */
    public Component add(Component comp, boolean editState) {
        if (comp instanceof GoodsTypeLabel) {
            addLabel((GoodsTypeLabel)comp);
            return comp;
        }
        return null;
    }
}
