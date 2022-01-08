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

package net.sf.freecol.client.gui.panel;

import java.awt.Component;
import java.awt.LayoutManager;
import java.util.List;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.gui.label.GoodsTypeLabel;
import net.sf.freecol.common.model.GoodsType;


/**
 * Simple panel for a goods type.
 *
 * Used in the trade route code to show the cargo a carrier is
 * supposed to take on board at a certain stop.
 *
 * FIXME: use in the CaptureGoodsDialog?
 */
public class GoodsTypePanel extends MigPanel implements DropTarget {

    /** Are duplicate entries allowed? */
    private boolean unique;
    

    /**
     * Build a new cargo panel.
     *
     * @param unique Should the goods types be unique or are
     *     duplicates allowed?
     */
    public GoodsTypePanel(boolean unique) {
        this(new MigLayout(), unique);
    }

    /**
     * Build a new goods type panel.
     *
     * @param layout The {@code LayoutManager} to use for this panel.
     * @param unique Should the goods types be unique or are
     *     duplicates allowed?
     */
    public GoodsTypePanel(LayoutManager layout, boolean unique) {
        super(layout);

        this.unique = unique;
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
     * Find the sub-component goods type label for a given goods type.
     *
     * @param gt The {@code GoodsType} to find.
     * @return The existing subcomponent for that goods type, or null.
     */
    private Component findLabel(GoodsType gt) {
        for (Component child : getComponents()) {
            if (child instanceof GoodsTypeLabel
                && ((GoodsTypeLabel)child).getType() == gt) return child;
        }
        return null;
    }
        
    /**
     * Add a single label.
     *
     * Do not repaint, that will be done top down.
     *
     * @param label The {@code GoodsTypeLabel} to add.
     * @return True if the label was added.
     */
    public boolean addLabel(GoodsTypeLabel label) {
        if (label != null
            && (!this.unique || findLabel(label.getType()) == null)) {
            Component ret = super.add(label);
            revalidate();
            repaint();
            return ret != null;
        }
        return false;
    }

    /**
     * Remove labels that correspond to a given goods type.
     *
     * @param gt The {@code GoodsType} to remove.
     * @return True if the goods were removed.
     */
    public boolean removeGoodsType(GoodsType gt) {
        Component child = findLabel(gt);
        if (child != null) {
            super.remove(child);
            revalidate();
            repaint();
            return true;
        }
        return false;
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
        if (comp instanceof GoodsTypeLabel
            && addLabel((GoodsTypeLabel)comp)) return comp;
        return null;
    }
}
