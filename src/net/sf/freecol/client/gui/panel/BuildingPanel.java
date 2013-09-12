/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
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
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * This panel represents a single building in a Colony.
 */
public class BuildingPanel extends MigPanel implements PropertyChangeListener {

    private static Logger logger = Logger.getLogger(BuildingPanel.class.getName());

    /** The enclosing client. */
    private final FreeColClient freeColClient;

    /** The Building to display. */
    private final Building building;

    /** A label for the production, if any. */
    private ProductionLabel productionOutput = null;

    /** Labels for any units present. */
    private List<UnitLabel> unitLabels = new ArrayList<UnitLabel>();


    /**
     * Creates this BuildingPanel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param building The building to display information from.
     */
    public BuildingPanel(FreeColClient freeColClient, Building building) {
        super(new MigLayout("", "[32][32][32]", "[32][44]"));

        this.freeColClient = freeColClient;
        this.building = building;

        setToolTipText(" ");
    }


    /**
     * Initialize this building panel.
     */
    public void initialize() {
        cleanup();
        addPropertyChangeListeners();
        update();
    }

    /**
     * Clean up this building panel.
     */
    public void cleanup() {
        unitLabels.clear();
        productionOutput = null;
        removePropertyChangeListeners();
        removeAll();
    }

    /**
     * Add any property change listeners.
     */
    protected void addPropertyChangeListeners() {
        if (building != null) {
            building.addPropertyChangeListener(this);
        }
    }

    /**
     * Remove any property change listeners.
     */
    protected void removePropertyChangeListeners() {
        if (building != null) {
            building.removePropertyChangeListener(this);
        }
    }

    /**
     * Update up this building panel.
     */
    public void update() {
        removeAll();
        unitLabels.clear();
        productionOutput = null;

        ProductionInfo info = building.getProductionInfo();
        if (info != null && !info.getProduction().isEmpty()) {
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
                productionOutput = new ProductionLabel(freeColClient,
                                                       output, maximum);
            }
        }
        JLabel upkeep = null;
        if (building.getSpecification().getBoolean(GameOptions.ENABLE_UPKEEP)
            && building.getType().getUpkeep() > 0) {
            upkeep = new UpkeepLabel(building.getType().getUpkeep());
        }
        if (productionOutput == null) {
            if (upkeep != null) {
                add(upkeep, "span, align center");
            }
        } else if (upkeep == null) {
            add(productionOutput, "span, align center");
        } else {
            add(productionOutput, "span, split 2, align center");
            add(upkeep);
        }

        for (Unit unit : building.getUnitList()) {
            UnitLabel unitLabel = new UnitLabel(freeColClient, unit, true);
            unitLabels.add(unitLabel);
            add(unitLabel);
        }

        setSize(new Dimension(96,76));
        revalidate();
        repaint();
    }


    /**
     * Get the building this panel displays.
     *
     * @return The displayed <code>Building</code>.
     */
    public Building getBuilding() {
        return building;
    }

    /**
     * Get any unit labels for the units present.
     *
     * @return A list of <code>UnitLabel</code>s.
     */
    public List<UnitLabel> getUnitLabels() {
        return unitLabels;
    }


    // Interface PropertyChangeListener

    /**
     * {@inheritDoc}
     */
    public void propertyChange(PropertyChangeEvent event) {
        String property = event.getPropertyName();
        logger.finest(building.getId() + " change " + property
                      + ": " + event.getOldValue()
                      + " -> " + event.getNewValue());
        update();
    }


    // Override JComponent

    /**
     * {@inheritDoc}
     */
    @Override
    public JToolTip createToolTip() {
        return new BuildingToolTip(freeColClient, building);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintComponent(Graphics g) {
        ImageLibrary lib = freeColClient.getGUI().getImageLibrary();
        g.drawImage(lib.fadeImage(lib.getBuildingImage(building), 0.6f, 192.0f),
                    0, 0, this);
    }


    /**
     * A special label to display the building upkeep required.
     */
    public class UpkeepLabel extends JLabel {

        /** The base image to display. */
        private final Image image;

        /**
         * Create an upkeep label.
         *
         * @param number The upkeep cost.
         */
        public UpkeepLabel(int number) {
            super(freeColClient.getGUI().getImageLibrary()
                .getMiscImageIcon("coin"));

            ImageLibrary lib = freeColClient.getGUI().getImageLibrary();
            image = lib.getStringImage(freeColClient.getGUI().getCanvas()
                .getGraphics(),
                Integer.toString(number), getForeground(),
                ResourceManager.getFont("SimpleFont", Font.BOLD, 12f));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintComponent(Graphics g) {
            getIcon().paintIcon(null, g, 0, 0);
            g.drawImage(image, 
                (getIcon().getIconWidth() - image.getWidth(null))/2,
                (getIcon().getIconHeight() - image.getHeight(null))/2, null);
        }
    }
}
