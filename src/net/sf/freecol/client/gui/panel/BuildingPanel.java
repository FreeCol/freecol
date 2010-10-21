/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.awt.Graphics2D;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolTip;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.resources.ResourceManager;

import net.miginfocom.swing.MigLayout;

/**
 * This panel represents a single building in a Colony.
 */
public class BuildingPanel extends JPanel implements PropertyChangeListener {

    private final Canvas parent;

    private final Building building;

    private ProductionLabel productionOutput = null;;

    private List<UnitLabel> unitLabels = new ArrayList<UnitLabel>();

    /**
     * Creates this BuildingPanel.
     * 
     * @param building The building to display information from.
     * @param parent a <code>Canvas</code> value
     */
    public BuildingPanel(Building building, Canvas parent) {

        this.building = building;
        this.parent = parent;

        addPropertyChangeListeners();

        setToolTipText(" ");
        setLayout(new MigLayout("", "[32][32][32]", "[32][44]"));
        initialize();
    }

    public void initialize() {
   
        removeAll();
        unitLabels.clear();

        if (building.getProductionNextTurn() == 0) {
            add(new JLabel(), "span");
        } else {
            productionOutput = new ProductionLabel(building.getGoodsOutputType(),
                                                   building.getProductionNextTurn(),
                                                   building.getMaximumProduction(), parent);
            add(productionOutput, "span, align center");
        }

        for (Unit unit : building.getUnitList()) {
            UnitLabel unitLabel = new UnitLabel(unit, parent, true);
            unitLabels.add(unitLabel);
            add(unitLabel);
        }

//        setSize(getPreferredSize());
        setSize(new Dimension(96,76));
        revalidate();
        repaint();
    }

    /**
     * Paints this component.
     * 
     * @param g The graphics context in which to paint.
     */
    public void paintComponent(Graphics g) {
        BufferedImage bgImage = fadeImage(ResourceManager.getImage(building.getType().getId() + ".image"), 0.6f, 192.0f);
        g.drawImage(bgImage, 0, 0, this);
    }

    public BufferedImage fadeImage(Image img, float fade, float target) {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.getGraphics();
        g.drawImage(img, 0, 0, null);

        float offset = target * (1.0f - fade);
        float[] scales = { fade, fade, fade, 1.0f };
        float[] offsets = { offset, offset, offset, 0.0f };
        RescaleOp rop = new RescaleOp(scales, offsets, null);

        Graphics2D g2d = (Graphics2D) g;
        g2d.drawImage(bi, rop, 0, 0);
		return bi;
    }

    public Building getBuilding() {
        return building;
    }

    public void updateProductionLabel() {
        initialize();
    }

    public List<UnitLabel> getUnitLabels() {
        return unitLabels;
    }

    public JToolTip createToolTip() {
        return new BuildingToolTip(building, parent);
    }

    public void addPropertyChangeListeners() {
        building.addPropertyChangeListener(this);
        Colony colony = building.getColony();
        GoodsType type = building.getGoodsInputType();
        if (type != null) {
            colony.addPropertyChangeListener(type.getId(), this);
        }
        type = building.getGoodsOutputType();
        if (type != null) {
            colony.addPropertyChangeListener(type.getId(), this);
        }
    }

    public void removePropertyChangeListeners() {
        building.removePropertyChangeListener(this);
        Colony colony = building.getColony();
        GoodsType type = building.getGoodsInputType();
        if (type != null) {
            colony.removePropertyChangeListener(type.getId(), this);
        }
        type = building.getGoodsOutputType();
        if (type != null) {
            colony.removePropertyChangeListener(type.getId(), this);
        }
    }

    public void propertyChange(PropertyChangeEvent event) {
        String property = event.getPropertyName();
        if (Building.UNIT_CHANGE.toString().equals(property)) {
            Colony colony = building.getColony();
            GoodsType type = building.getGoodsInputType();
            if (type != null) {
                colony.firePropertyChange(type.getId(), 0, 1);
            }
            type = building.getGoodsOutputType();
            if (type != null) {
                colony.firePropertyChange(type.getId(), 0, 1);
            }
        }
        initialize();
    }

}
