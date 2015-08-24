/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JToolTip;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.Unit;


/**
 * This panel represents a single building in a Colony.
 */
public class BuildingPanel extends MigPanel implements PropertyChangeListener {

    private static final Logger logger = Logger.getLogger(BuildingPanel.class.getName());

    /** The enclosing client. */
    private final FreeColClient freeColClient;

    /** The Building to display. */
    private final Building building;

    /** Labels for any units present. */
    private final List<UnitLabel> unitLabels = new ArrayList<>();


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

        final Colony colony = building.getColony();
        ProductionLabel productionOutput = null;
        ProductionInfo info = building.getProductionInfo();
        if (info != null && !info.getProduction().isEmpty()) {
            AbstractGoods output = info.getProduction().get(0);
            if (output.getAmount() > 0) {
                if (building.hasAbility(Ability.AVOID_EXCESS_PRODUCTION)) {
                    int stored = colony.getGoodsCount(output.getType());
                    int capacity = colony.getWarehouseCapacity();
                    if (output.getAmount() + stored > capacity) {
                        output = new AbstractGoods(output.getType(),
                                                   capacity - stored);
                    }
                }
                AbstractGoods maximum = info.getMaximumProduction().isEmpty()
                    ? output : info.getMaximumProduction().get(0);
                productionOutput = new ProductionLabel(freeColClient, output,
                                                       maximum.getAmount());
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

        ImageLibrary lib = freeColClient.getGUI().getImageLibrary();
        Image buildingImage = lib.getBuildingImage(building);
        setPreferredSize(new Dimension(buildingImage.getWidth(null), 
                                       buildingImage.getHeight(null)));
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
    @Override
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
        g.drawImage(lib.getBuildingImage(building), 0, 0, this);
    }


    /**
     * A special label to display the building upkeep required.
     */
    public class UpkeepLabel extends JLabel {

        /** The base image to display. */
        private final int number;

        /**
         * Create an upkeep label.
         *
         * @param number The upkeep cost.
         */
        public UpkeepLabel(int number) {
            super(new ImageIcon(freeColClient.getGUI().getImageLibrary()
                .getMiscImage(ImageLibrary.ICON_COIN)));
            this.number = number;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintComponent(Graphics g) {
            getIcon().paintIcon(null, g, 0, 0);
            GUI gui = freeColClient.getGUI();
            ImageLibrary lib = gui.getImageLibrary();
            BufferedImage image = lib.getStringImage(
                g,
                Integer.toString(number), getForeground(),
                FontLibrary.createFont(FontLibrary.FontType.SIMPLE,
                    FontLibrary.FontSize.TINY, Font.BOLD,
                    lib.getScaleFactor()));
            g.drawImage(image,
                (getIcon().getIconWidth() - image.getWidth(null))/2,
                (getIcon().getIconHeight() - image.getHeight(null))/2, null);
        }
    }
}
