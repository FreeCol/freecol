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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolTip;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.resources.ResourceManager;

/**
 * This panel represents a single building in a Colony.
 */
public class BuildingPanel extends JPanel implements PropertyChangeListener {

    private static Logger logger = Logger.getLogger(BuildingPanel.class.getName());


    private final Building building;

    private ProductionLabel productionOutput = null;

    private List<UnitLabel> unitLabels = new ArrayList<UnitLabel>();

    private FreeColClient freeColClient;

    private GUI gui;

    /**
     * Creates this BuildingPanel.
     *
     * @param building The building to display information from.
     * @param parent a <code>Canvas</code> value
     */
    public BuildingPanel(FreeColClient freeColClient, Building building, GUI gui) {

        this.freeColClient = freeColClient;
        this.gui = gui;
        this.building = building;

        addPropertyChangeListeners();

        setToolTipText(" ");
        setLayout(new MigLayout("", "[32][32][32]", "[32][44]"));
        initialize();
    }

    public void initialize() {

        removeAll();
        unitLabels.clear();

        ProductionInfo info = building.getProductionInfo();

        if (info == null || info.getProduction().isEmpty()) {
            add(new JLabel(), "span");
        } else {
            AbstractGoods output = info.getProduction().get(0);
            if (output.getAmount() > 0) {
                if (building.hasAbility(Ability.AVOID_EXCESS_PRODUCTION)) {
                    int stored = building.getColony().getGoodsCount(output.getType());
                    int capacity = building.getColony().getWarehouseCapacity();
                    if (output.getAmount() + stored > capacity) {
                        output = new AbstractGoods(output.getType(), capacity - stored);
                    }
                }
                AbstractGoods maximum = info.getMaximumProduction().isEmpty()
                    ? output : info.getMaximumProduction().get(0);
                productionOutput = new ProductionLabel(freeColClient, gui, output, maximum);
                add(productionOutput, "span, align center");
            }
        }

        for (Unit unit : building.getUnitList()) {
            UnitLabel unitLabel = new UnitLabel(freeColClient, unit, gui, true);
            unitLabels.add(unitLabel);
            add(unitLabel);
        }

        setSize(new Dimension(96,76));
        revalidate();
        repaint();
    }

    /**
     * Paints this component.
     *
     * @param g The graphics context in which to paint.
     */
    @Override
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

    @Override
    public JToolTip createToolTip() {
        return new BuildingToolTip(freeColClient, building, gui);
    }

    public void addPropertyChangeListeners() {
        building.addPropertyChangeListener(this);
    }

    public void removePropertyChangeListeners() {
        building.removePropertyChangeListener(this);
    }

    public void propertyChange(PropertyChangeEvent event) {
        String property = event.getPropertyName();
        logger.finest(building.getId() + " change " + property
                      + ": " + event.getOldValue()
                      + " -> " + event.getNewValue());
        initialize();
    }

}
