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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.Autoscroll;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ComponentInputMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolTip;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.debug.DebugUtils;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Colony.ColonyChangeEvent;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.NoClaimReason;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitLocation.NoAddReason;
import net.sf.freecol.common.model.UnitType;

/**
 * This is a panel for the Colony display. It shows the units that are working
 * in the colony, the buildings and much more.
 *
 * Beware that in debug mode, this might be a server-side version of the colony
 * which is why we need to call getColony().getSpecification() to get the
 * spec that corresponds to the good types in this colony.
 */
public final class ColonyPanel extends PortPanel
    implements ActionListener, PropertyChangeListener {

    private static Logger logger = Logger.getLogger(ColonyPanel.class.getName());

    /**
     * The height of the area in which autoscrolling should happen.
     */
    public static final int SCROLL_AREA_HEIGHT = 40;

    /**
     * The speed of the scrolling.
     */
    public static final int SCROLL_SPEED = 40;

    private static final int EXIT = 0,
        BUILDQUEUE = 1,
        UNLOAD = 2,
        WAREHOUSE = 4,
        FILL = 5,
        COLONY_UNITS = 6,
        SETGOODS = 7;

    private final JPanel netProductionPanel = new JPanel();
    private final PopulationPanel populationPanel = new PopulationPanel();

    private final JComboBox nameBox;

    private final OutsideColonyPanel outsideColonyPanel;

    private final WarehousePanel warehousePanel;

    private final TilePanel tilePanel;

    private final BuildingsPanel buildingsPanel;

    private final ConstructionPanel constructionPanel;

    private final MouseListener releaseListener;

    private Colony colony;

    private JButton unloadButton = new JButton(Messages.message("unload"));

    private JButton fillButton = new JButton(Messages.message("fill"));

    private JButton warehouseButton = new JButton(Messages.message("warehouseDialog.name"));

    private JButton buildQueueButton = new JButton(Messages.message("colonyPanel.buildQueue"));

    private JButton colonyUnitsButton = new JButton(Messages.message("colonyPanel.colonyUnits"));

    private JButton setGoodsButton
        = (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS))
        ? new JButton("Set Goods") : null;


    /**
     * The constructor for the panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param gui The <code>GUI</code> to display on.
     * @param colony The <code>Colony</code> to display in this panel.
     */
    @SuppressWarnings("unchecked") // FIXME in Java7
    public ColonyPanel(FreeColClient freeColClient, GUI gui, Colony colony) {
        super(freeColClient, gui);

        setFocusCycleRoot(true);

        // Use ESCAPE for closing the ColonyPanel:
        InputMap closeInputMap = new ComponentInputMap(okButton);
        closeInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "pressed");
        closeInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true), "released");
        SwingUtilities.replaceUIInputMap(okButton, JComponent.WHEN_IN_FOCUSED_WINDOW, closeInputMap);

        InputMap unloadInputMap = new ComponentInputMap(unloadButton);
        unloadInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0, false), "pressed");
        unloadInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0, true), "released");
        SwingUtilities.replaceUIInputMap(unloadButton, JComponent.WHEN_IN_FOCUSED_WINDOW, unloadInputMap);

        InputMap fillInputMap = new ComponentInputMap(fillButton);
        fillInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0, false), "pressed");
        fillInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0, true), "released");
        SwingUtilities.replaceUIInputMap(fillButton, JComponent.WHEN_IN_FOCUSED_WINDOW, fillInputMap);

        InputMap warehouseInputMap = new ComponentInputMap(warehouseButton);
        warehouseInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, false), "pressed");
        warehouseInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, true), "released");
        SwingUtilities.replaceUIInputMap(warehouseButton, JComponent.WHEN_IN_FOCUSED_WINDOW, warehouseInputMap);

        InputMap buildQueueInputMap = new ComponentInputMap(buildQueueButton);
        buildQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0, false), "pressed");
        buildQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0, true), "released");
        SwingUtilities.replaceUIInputMap(buildQueueButton, JComponent.WHEN_IN_FOCUSED_WINDOW, buildQueueInputMap);

        InputMap colonyUnitsInputMap = new ComponentInputMap(colonyUnitsButton);
        colonyUnitsInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0, false), "pressed");
        colonyUnitsInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0, true), "released");
        SwingUtilities.replaceUIInputMap(colonyUnitsButton, JComponent.WHEN_IN_FOCUSED_WINDOW, colonyUnitsInputMap);

        netProductionPanel.setOpaque(false);

        constructionPanel = new ConstructionPanel(gui, colony, true);
        constructionPanel.setOpaque(true);

        outsideColonyPanel = new OutsideColonyPanel();

        inPortPanel = new ColonyInPortPanel();

        warehousePanel = new WarehousePanel();

        tilePanel = new TilePanel(freeColClient);

        buildingsPanel = new BuildingsPanel();

        cargoPanel = new ColonyCargoPanel(freeColClient);

        defaultTransferHandler = new DefaultTransferHandler(freeColClient, gui, this);
        pressListener = new DragListener(freeColClient, gui, this);
        releaseListener = new DropListener();

        JScrollPane outsideColonyScroll = new JScrollPane(outsideColonyPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        outsideColonyScroll.getVerticalScrollBar().setUnitIncrement( 16 );
        JScrollPane inPortScroll = new JScrollPane(inPortPanel);
        inPortScroll.getVerticalScrollBar().setUnitIncrement( 16 );
        JScrollPane cargoScroll = new JScrollPane(cargoPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane warehouseScroll = new JScrollPane(warehousePanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane tilesScroll = new JScrollPane(tilePanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane buildingsScroll = new JScrollPane(buildingsPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        buildingsScroll.getVerticalScrollBar().setUnitIncrement( 16 );

        // Make the colony label
        nameBox = new JComboBox();
        nameBox.setFont(smallHeaderFont);
        for (Colony aColony : getSortedColonies()) {
            nameBox.addItem(aColony);
        }
        nameBox.setSelectedItem(colony);
        nameBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    initialize((Colony)nameBox.getSelectedItem());
                }
            });

        // the following actions are pre-defined
        nameBox.getInputMap().put(KeyStroke.getKeyStroke("LEFT"), "selectPrevious2");
        nameBox.getInputMap().put(KeyStroke.getKeyStroke("RIGHT"), "selectNext2");
        InputMap nameInputMap = new ComponentInputMap(nameBox);
        nameInputMap.put(KeyStroke.getKeyStroke("LEFT"), "selectPrevious2");
        nameInputMap.put(KeyStroke.getKeyStroke("RIGHT"), "selectNext2");
        SwingUtilities.replaceUIInputMap(nameBox, JComponent.WHEN_IN_FOCUSED_WINDOW, nameInputMap);

        buildingsScroll.setAutoscrolls(true);
        buildingsScroll.getViewport().setOpaque(false);
        buildingsPanel.setOpaque(false);

        /** Borders */
        tilesScroll.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        buildingsScroll.setBorder(BorderFactory.createEtchedBorder());
        warehouseScroll.setBorder(BorderFactory.createEtchedBorder());
        cargoScroll.setBorder(BorderFactory.createEtchedBorder());
        inPortScroll.setBorder(BorderFactory.createEtchedBorder());
        outsideColonyScroll.setBorder(BorderFactory.createEtchedBorder());

        okButton.setText(Messages.message("close"));

        unloadButton.setActionCommand(String.valueOf(UNLOAD));
        enterPressesWhenFocused(unloadButton);
        unloadButton.addActionListener(this);

        fillButton.setActionCommand(String.valueOf(FILL));
        enterPressesWhenFocused(fillButton);
        fillButton.addActionListener(this);

        warehouseButton.setActionCommand(String.valueOf(WAREHOUSE));
        enterPressesWhenFocused(warehouseButton);
        warehouseButton.addActionListener(this);

        buildQueueButton.setActionCommand(String.valueOf(BUILDQUEUE));
        enterPressesWhenFocused(buildQueueButton);
        buildQueueButton.addActionListener(this);

        colonyUnitsButton.setActionCommand(String.valueOf(COLONY_UNITS));
        enterPressesWhenFocused(colonyUnitsButton);
        colonyUnitsButton.addActionListener(this);

        if (setGoodsButton != null) {
            setGoodsButton.setActionCommand(String.valueOf(SETGOODS));
            enterPressesWhenFocused(setGoodsButton);
            setGoodsButton.addActionListener(this);
        }

        selectedUnitLabel = null;

        // See the message of Ulf Onnen for more information about the presence
        // of this fake mouse listener.
        addMouseListener(new MouseAdapter() {});

        setLayout(new MigLayout("fill, wrap 2, insets 2", "[390!][fill]",
                                "[][]0[]0[][growprio 200,shrinkprio 10][growprio 150,shrinkprio 50]"));

        add(nameBox, "height 48:, grow");
        add(netProductionPanel, "growx");
        add(tilesScroll, "width 390!, height 200!, top");
        add(buildingsScroll, "span 1 3, grow");
        add(populationPanel, "grow");
        add(constructionPanel, "grow, top");
        add(inPortScroll, "span, split 3, grow, sg, height 60:121:");
        add(cargoScroll, "grow, sg, height 60:121:");
        add(outsideColonyScroll, "grow, sg, height 60:121:");
        add(warehouseScroll, "span, height 40:60:, growx");
        add(unloadButton, "span, split "
            + Integer.toString((setGoodsButton == null) ? 6 : 7)
            + ", align center");
        add(fillButton);
        add(warehouseButton);
        add(buildQueueButton);
        add(colonyUnitsButton);
        if (setGoodsButton != null) add(setGoodsButton);
        add(okButton, "tag ok");

        initialize(colony);
        restoreSavedSize(850, 600);
    }

    /**
     * Gets the <code>Colony</code>-pointer in use.
     *
     * @return The <code>Colony</code>.
     */
    public synchronized final Colony getColony() {
        return colony;
    }

    /**
     * Set the current colony.
     *
     * @param colony The new colony value.
     */
    private synchronized void setColony(Colony colony) {
        this.colony = colony;
    }

    /**
     * Gets the <code>WarehousePanel</code>-object in use.
     *
     * @return The <code>WarehousePanel</code>.
     */
    public final WarehousePanel getWarehousePanel() {
        return warehousePanel;
    }

    /**
     * Gets the <code>TilePanel</code>-object in use.
     *
     * @return The <code>TilePanel</code>.
     */
    public final TilePanel getTilePanel() {
        return tilePanel;
    }

    public void updateConstructionPanel() {
        constructionPanel.update();
    }

    public void updateInPortPanel() {
        inPortPanel.update();
    }

    public void updateWarehousePanel() {
        warehousePanel.update();
    }

    public void updateOutsideColonyPanel() {
        outsideColonyPanel.update();
    }

    public void updateBuildingsPanel() {
        buildingsPanel.update();
    }

    public void updateTilePanel() {
        tilePanel.update();
    }

    public void updateProductionPanel() {
        final Specification spec = colony.getSpecification();
        // TODO: find out why the cache needs to be explicitly invalidated
        colony.invalidateCache();
        netProductionPanel.removeAll();

        for (GoodsType goodsType : spec.getGoodsTypeList()) {
            int amount = colony.getAdjustedNetProductionOf(goodsType);
            if (amount != 0) {
                netProductionPanel.add(new ProductionLabel(getFreeColClient(),
                                       getGUI(), goodsType, amount));
            }
        }

        netProductionPanel.revalidate();
    }

    /**
     * Update all the production-related panels.
     *
     * This has to be very broad as a change at one work location can
     * have a secondary effect on another, and especially if the
     * population hits a bonus boundary.  A simple example is that
     * adding extra lumber production may improve the production of the
     * lumber mill.  These changes can then flow on to production and
     * construction displays.
     */
    private void updateProduction() {
        updateTilePanel();
        updateBuildingsPanel();
        updateProductionPanel();
        updateConstructionPanel();
    }

    private void sortBuildings(List<Building> buildings) {
        Collections.sort(buildings);
    }

    /**
     * Analyzes an event and calls the right external methods to take
     * care of the user's request.
     *
     * @param event The incoming action event
     */
    public void actionPerformed(ActionEvent event) {
        Colony colony = getColony();
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            closeColonyPanel();
        } else {
            try {
                switch (Integer.valueOf(command).intValue()) {
                case UNLOAD:
                    unload();
                    break;
                case WAREHOUSE:
                    if (getGUI().showWarehouseDialog(colony)) {
                        updateWarehousePanel();
                    }
                    break;
                case BUILDQUEUE:
                    getGUI().showBuildQueuePanel(colony);
                    updateConstructionPanel();
                    break;
                case FILL:
                    fill();
                    break;
                case COLONY_UNITS:
                    generateColonyUnitsMenu();
                    break;
                case SETGOODS:
                    DebugUtils.setColonyGoods(getFreeColClient(), colony);
                    updateWarehousePanel();
                    updateProduction();
                    break;
                default:
                    logger.warning("Invalid action");
                    break;
                }
            } catch (NumberFormatException e) {
                logger.warning("Invalid action number: " + command);
            }
        }
    }

    /**
     * Unloads all goods and units from the carrier currently selected.
     */
    private void unload() {
        Unit unit = getSelectedUnit();
        if (unit != null && unit.isCarrier()) {
            for (Goods goods : unit.getGoodsContainer().getGoods()) {
                getController().unloadCargo(goods, false);
            }
            for (Unit u : unit.getUnitList()) {
                getController().leaveShip(u);
            }
            cargoPanel.update();
            outsideColonyPanel.update();
        }
        unloadButton.setEnabled(false);
        fillButton.setEnabled(false);
    }

    /**
     * Fill goods from the carrier currently selected to capacity.
     */
    private void fill() {
        Unit unit = getSelectedUnit();
        if (unit != null && unit.isCarrier()) {
            for (Goods goods : unit.getGoodsContainer().getGoods()) {
                int space = GoodsContainer.CARGO_SIZE - goods.getAmount();
                int count = getColony().getGoodsCount(goods.getType());
                if (space > 0 && count > 0) {
                    Goods newGoods = new Goods(goods.getGame(), getColony(),
                                               goods.getType(),
                                               Math.min(space, count));
                    getController().loadCargo(newGoods, unit);
                }
            }
        }
    }

    /**
     * Enables the unload and fill buttons if the currently selected unit is a
     * carrier with some cargo.
     */
    private void updateCarrierButtons() {
        unloadButton.setEnabled(false);
        fillButton.setEnabled(false);
        if (isEditable() && selectedUnitLabel != null) {
            Unit unit = selectedUnitLabel.getUnit();
            if (unit != null && unit.isCarrier() && unit.hasCargo()) {
                unloadButton.setEnabled(true);
                for (Goods goods : unit.getGoodsList()) {
                    if (getColony().getGoodsCount(goods.getType()) > 0) {
                        fillButton.setEnabled(true);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Returns the currently select unit.
     *
     * @return The currently select unit.
     */
    public Unit getSelectedUnit() {
        return (selectedUnitLabel == null) ? null
            : selectedUnitLabel.getUnit();
    }

    /**
     * Selects a unit that is located somewhere on this panel.
     *
     * @param unitLabel The <code>UnitLabel</code> for the unit that
     *     is being selected.
     */
    public void setSelectedUnitLabel(UnitLabel unitLabel) {
        if (selectedUnitLabel != unitLabel) {
            if (selectedUnitLabel != null) {
                selectedUnitLabel.setSelected(false);
                selectedUnitLabel.getUnit().removePropertyChangeListener(this);
            }
            selectedUnitLabel = unitLabel;
            if (unitLabel == null) {
                cargoPanel.setCarrier(null);
            } else {
                cargoPanel.setCarrier(unitLabel.getUnit());
                unitLabel.setSelected(true);
                unitLabel.getUnit().addPropertyChangeListener(this);
            }
        }
        updateCarrierButtons();
        inPortPanel.revalidate();
        inPortPanel.repaint();
    }

    /**
     * Gets the list of units on the colony tile.
     * Note, does not include the units *inside* the colony.
     *
     * @return A list of units on the colony tile.
     */
    public List<Unit> getUnitList() {
        return colony.getTile().getUnitList();
    }

    /**
     * Initialize the entire panel.
     *
     * We can arrive here normally when a colony panel is created,
     * or when an existing colony panel is changed via the colony name
     * menu in the nameBox.
     *
     * @param colony The <code>Colony</code> to be displayed.
     */
    private void initialize(Colony colony) {
        removePropertyChangeListeners();
        setColony(colony);
        editable = colony.getOwner() == getMyPlayer();
        addPropertyChangeListeners();

        // Set listeners and transfer handlers
        outsideColonyPanel.removeMouseListener(releaseListener);
        inPortPanel.removeMouseListener(releaseListener);
        cargoPanel.removeMouseListener(releaseListener);
        warehousePanel.removeMouseListener(releaseListener);
        if (isEditable()) {
            outsideColonyPanel.setTransferHandler(defaultTransferHandler);
            inPortPanel.setTransferHandler(defaultTransferHandler);
            cargoPanel.setTransferHandler(defaultTransferHandler);
            warehousePanel.setTransferHandler(defaultTransferHandler);

            outsideColonyPanel.addMouseListener(releaseListener);
            inPortPanel.addMouseListener(releaseListener);
            cargoPanel.addMouseListener(releaseListener);
            warehousePanel.addMouseListener(releaseListener);
        } else {
            outsideColonyPanel.setTransferHandler(null);
            inPortPanel.setTransferHandler(null);
            cargoPanel.setTransferHandler(null);
            warehousePanel.setTransferHandler(null);
        }

        // Enable/disable widgets
        unloadButton.setEnabled(isEditable());
        fillButton.setEnabled(isEditable());
        warehouseButton.setEnabled(isEditable());
        colonyUnitsButton.setEnabled(isEditable());
        nameBox.setEnabled(isEditable());

        // update all the subpanels
        cargoPanel.setCarrier(null);

        inPortPanel.initialize();
        warehousePanel.initialize();
        buildingsPanel.initialize();
        tilePanel.initialize();
        outsideColonyPanel.initialize();

        updateProductionPanel();
        populationPanel.update();

        constructionPanel.setColony(colony);
        inPortPanel.setName(colony.getName() + " - port");
    }

    /**
     * Closes the <code>ColonyPanel</code>.
     */
    public void closeColonyPanel() {
        if (getColony().getUnitCount() == 0) {
            if (getGUI().showConfirmDialog("abandonColony.text",
                                         "abandonColony.yes",
                                         "abandonColony.no")) {
                getGUI().removeFromCanvas(this);
                getController().abandonColony(getColony());
            }
        } else {
            BuildableType buildable = getColony().getCurrentlyBuilding();
            if (buildable != null
                && buildable.getRequiredPopulation() > getColony().getUnitCount()
                && !getGUI().showConfirmDialog(null,
                    StringTemplate.template("colonyPanel.reducePopulation")
                        .addName("%colony%", getColony().getName())
                        .addAmount("%number%", buildable.getRequiredPopulation())
                        .add("%buildable%", buildable.getNameKey()),
                    "ok", "cancel")) {
                return;
            }
            getGUI().removeFromCanvas(this);

            // remove property listeners
            removePropertyChangeListeners();
            if (getSelectedUnit() != null) {
                getSelectedUnit().removePropertyChangeListener(this);
            }
            buildingsPanel.cleanup();
            warehousePanel.cleanup();
            tilePanel.cleanup();
            constructionPanel.removePropertyChangeListeners();
            cargoPanel.setCarrier(null);
            outsideColonyPanel.cleanup();

            if (getFreeColClient().currentPlayerIsMyPlayer()) {
                getController().nextModelMessage();
                Unit activeUnit = getGUI().getActiveUnit();
                if (activeUnit == null || activeUnit.getTile() == null
                    || (!(activeUnit.getLocation() instanceof Tile)
                        && !activeUnit.isOnCarrier())) {
                    getController().nextActiveUnit();
                }
            }
            getGUI().getMapViewer().restartBlinking();
        }
    }

    /**
     * Generates a menu containing the units currently accessible
     * from the Colony Panel allowing keyboard access to said units.
     */
    private void generateColonyUnitsMenu() {
        JPopupMenu colonyUnitsMenu = new JPopupMenu("Colony Units");
        ImageLibrary imageLibrary = super.getLibrary();
        ImageIcon unitIcon = null;
        final QuickActionMenu unitMenu = new QuickActionMenu(getFreeColClient(), getGUI(), this);
        Tile colonyTile = colony.getTile();
        int unitNumber = 0;
        JMenuItem subMenu = null;

        for (final Unit unit : colony.getUnitList()) {
            ColonyTile workingOnLand = unit.getWorkTile();
            if (workingOnLand != null) {
                GoodsType goodsType = unit.getWorkType();
                int producing = workingOnLand.getProductionOf(unit, goodsType);
                unitIcon = imageLibrary.getUnitImageIcon(unit, 0.5);
                String nominative = Messages.message(StringTemplate.template(
                    goodsType.getNameKey()).addAmount("%amount%", producing));
                String menuTitle = new String(Messages.message(unit.getLabel()) + " " +
                    Messages.message("producing.name") + " " + producing + " " + nominative);
                subMenu = new JMenuItem(menuTitle, unitIcon);
  	            subMenu.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        unitMenu.createUnitMenu(new UnitLabel(getFreeColClient(), unit, getGUI()));
                        unitMenu.show(getGUI().getCanvas(), 0, 0);
                    }
                });
                unitNumber++;
                colonyUnitsMenu.add(subMenu);
            }else{
                Building workingInBuilding = unit.getWorkLocation();
                if(workingInBuilding != null){
                    GoodsType goodsType = unit.getWorkType();
                    int producing = workingInBuilding.getProductionOf(unit, workingInBuilding.getGoodsOutputType());
                    unitIcon = imageLibrary.getUnitImageIcon(unit, 0.5);
                    String nominative = Messages.message(StringTemplate.template(
                        goodsType.getNameKey()).addAmount("%amount%", producing));
                    String menuTitle = new String(Messages.message(unit.getLabel()) + " " +
                        Messages.message("producing.name") + " " + producing + " " + nominative);
                    subMenu = new JMenuItem(menuTitle, unitIcon);
                    subMenu.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            unitMenu.createUnitMenu(new UnitLabel(getFreeColClient(), unit, getGUI()));
                            unitMenu.show(getGUI().getCanvas(), 0, 0);
                        }
                    });
                    unitNumber++;
                    colonyUnitsMenu.add(subMenu);
                }
            }
        }
        colonyUnitsMenu.addSeparator();
        for (final Unit unit : colonyTile.getUnitList()) {
            if(unit.isCarrier()){
                unitIcon = imageLibrary.getUnitImageIcon(unit, 0.5);
                String menuTitle = new String(Messages.message(unit.getLabel()) +
                    " " + Messages.message("inPort.name"));
                subMenu = new JMenuItem(menuTitle, unitIcon);
                subMenu.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        unitMenu.createUnitMenu(new UnitLabel(getFreeColClient(), unit, getGUI()));
                        unitMenu.show(getGUI().getCanvas(), 0, 0);
                    }
                });
                unitNumber++;
                colonyUnitsMenu.add(subMenu);
                if(unit.getUnitList() != null){
                    for(final Unit innerUnit : unit.getUnitList()){
                        unitIcon = imageLibrary.getUnitImageIcon(innerUnit, 0.5);
                        menuTitle = new String(Messages.message(innerUnit.getLabel()) + " Cargo On " + Messages.message(unit.getLabel()));
                        subMenu = new JMenuItem(menuTitle, unitIcon);
                        subMenu.addActionListener(new ActionListener() {
    	                    public void actionPerformed(ActionEvent e) {
                                unitMenu.createUnitMenu(new UnitLabel(getFreeColClient(), innerUnit, getGUI()));
                                unitMenu.show(getGUI().getCanvas(), 0, 0);
                            }
                        });
                        unitNumber++;
                        colonyUnitsMenu.add(subMenu);
                    }
                }
            }else if(!unit.isOnCarrier()){
                unitIcon = imageLibrary.getUnitImageIcon(unit, 0.5);
                String menuTitle = new String(Messages.message(unit.getLabel()) +
                        " " + Messages.message("outsideOfColony.name"));
                subMenu = new JMenuItem(menuTitle, unitIcon);
                subMenu.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        unitMenu.createUnitMenu(new UnitLabel(getFreeColClient(), unit, getGUI()));
                        unitMenu.show(getGUI().getCanvas(), 0, 0);
                        }
                });
                unitNumber++;
                colonyUnitsMenu.add(subMenu);
            }
        }
        colonyUnitsMenu.addSeparator();
        if (colonyUnitsMenu != null) {
            int elements = colonyUnitsMenu.getSubElements().length;
            if (elements > 0) {
                int lastIndex = colonyUnitsMenu.getComponentCount() - 1;
                if (colonyUnitsMenu.getComponent(lastIndex) instanceof JPopupMenu.Separator) {
                    colonyUnitsMenu.remove(lastIndex);
                }
            }
        }
        colonyUnitsMenu.show(getGUI().getCanvas(), 0, 0);
    }
    /**
     * Add property change listeners needed by this ColonyPanel.
     */
    private void addPropertyChangeListeners() {
        if (colony != null) {
            colony.addPropertyChangeListener(this);
            colony.getGoodsContainer().addPropertyChangeListener(this);
            colony.getTile().addPropertyChangeListener(this);
        }
    }

    /**
     * Remove the property change listeners of ColonyPanel.
     */
    private void removePropertyChangeListeners() {
        if (colony != null) {
            colony.removePropertyChangeListener(this);
            colony.getGoodsContainer().removePropertyChangeListener(this);
            colony.getTile().removePropertyChangeListener(this);
        }
    }

    /**
     * Handle a property change event sent to this ColonyPanel.
     *
     * @param event The <code>PropertyChangeEvent</code> to handle.
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (!isShowing() || colony == null) return;
        String property = event.getPropertyName();
        logger.finest(colony.getName() + " change " + property
                      + ": " + event.getOldValue()
                      + " -> " + event.getNewValue());

        if (property == null) {
            logger.warning("Null property change");
        } else if (Unit.CARGO_CHANGE.equals(property)) {
            updateInPortPanel();
        } else if (ColonyChangeEvent.POPULATION_CHANGE.toString().equals(property)) {
            populationPanel.update();
            updateProductionPanel(); // food production changes
        } else if (ColonyChangeEvent.BONUS_CHANGE.toString().equals(property)) {
            ModelMessage msg = colony.checkForGovMgtChangeMessage();
            if (msg != null) {
                getGUI().showInformationMessage(msg);
            }
            populationPanel.update();
        } else if (ColonyChangeEvent.UNIT_TYPE_CHANGE.toString().equals(property)) {
            FreeColGameObject object = (FreeColGameObject) event.getSource();
            UnitType oldType = (UnitType) event.getOldValue();
            UnitType newType = (UnitType) event.getNewValue();
            getGUI().showInformationMessage(object,
                StringTemplate.template("model.colony.unitChange")
                    .add("%oldType%", oldType.getNameKey())
                    .add("%newType%", newType.getNameKey()));
            updateTilePanel();
        } else if (property.startsWith("model.goods.")) {
            // Changes to warehouse goods count may affect building production
            // which requires a view update.
            updateWarehousePanel();
            updateProduction();
        } else if (Tile.UNIT_CHANGE.equals(property)) {
            updateOutsideColonyPanel();
            updateInPortPanel();
        } else {
            // ColonyTiles and Buildings now have their own
            // propertyChangeListeners so {ColonyTile,Building}.UNIT_CHANGE
            // events should not arrive here.
            logger.warning("Unknown property change event: "
                           + event.getPropertyName());
        }
    }


    /**
     * This panel shows the content of a carrier in the colony
     */
    public final class ColonyCargoPanel extends CargoPanel {

        /**
         * Create this colony cargo panel.
         *
         * @param freeColClient The containing <code>FreeColClient</code>.
         */
        public ColonyCargoPanel(FreeColClient freeColClient) {
            super(freeColClient, freeColClient.getGUI(), true);

            setLayout(new MigLayout("wrap 6, fill, insets 0"));
        }

        @Override
        public void update() {
            super.update();
            // May have un/loaded cargo, "Unload" could have changed validity
            updateCarrierButtons();
        }
    }

    /**
     * The panel to display the population breakdown for this colony.
     */
    public final class PopulationPanel extends JPanel {

        // Predefine all the required labels.
        private final JLabel rebelShield = new JLabel();
        private final JLabel rebelLabel = new JLabel();
        private final JLabel bonusLabel = new JLabel();
        private final JLabel royalistLabel = new JLabel();
        private final JLabel royalistShield = new JLabel();
        private final JLabel rebelMemberLabel = new JLabel();
        private final JLabel popLabel = new JLabel();
        private final JLabel royalistMemberLabel = new JLabel();


        /**
         * Create a new population panel.
         */
        public PopulationPanel() {
            setOpaque(false);
            setToolTipText(" ");
            setLayout(new MigLayout("wrap 5, fill, insets 0",
                                    "[][]:push[center]:push[right][]"));
            add(rebelShield, "bottom");
            add(rebelLabel, "split 2, flowy");
            add(rebelMemberLabel);
            add(popLabel, "split 2, flowy");
            add(bonusLabel);
            add(royalistLabel, "split 2, flowy");
            add(royalistMemberLabel);
            add(royalistShield, "bottom");
        }

        /**
         * Update this population panel.
         */
        public void update() {
            final int uc = colony.getUnitCount();
            final int solPercent = colony.getSoL();
            final int rebels = Colony.calculateRebels(uc, solPercent);
            StringTemplate t;

            t = StringTemplate.template("colonyPanel.rebelLabel")
                              .addAmount("%number%", rebels);
            rebelLabel.setText(Messages.message(t));

            t = StringTemplate.template("colonyPanel.royalistLabel")
                              .addAmount("%number%", uc - rebels);
            royalistLabel.setText(Messages.message(t));

            t = StringTemplate.template("colonyPanel.populationLabel")
                              .addAmount("%number%", uc);
            popLabel.setText(Messages.message(t));

            rebelMemberLabel.setText(solPercent + "%");

            final int grow = colony.getPreferredSizeChange();
            final int bonus = colony.getProductionBonus();
            t = StringTemplate.template("colonyPanel.bonusLabel")
                              .addAmount("%number%", bonus)
                              .add("%extra%",
                                  ((grow == 0) ? "" : "(" + grow + ")"));
            bonusLabel.setText(Messages.message(t));

            royalistMemberLabel.setText(colony.getTory() + "%");

            final Nation nation = colony.getOwner().getNation();
            rebelShield.setIcon(new ImageIcon(getLibrary()
                    .getCoatOfArmsImage(nation, 0.5)));

            royalistShield.setIcon(new ImageIcon(getLibrary()
                    .getCoatOfArmsImage(nation.getRefNation(), 0.5)));

            revalidate();
            repaint();
        }

        public JToolTip createToolTip() {
            return new RebelToolTip(getFreeColClient(), getGUI(), getColony());
        }

        @Override
        public String getUIClassID() {
            return "PopulationPanelUI";
        }
    }

    /**
     * A panel that holds UnitLabels that represent Units that are standing in
     * front of a colony.
     */
    public final class OutsideColonyPanel extends UnitPanel
        implements DropTarget {

        public OutsideColonyPanel() {
            super(ColonyPanel.this, null, ColonyPanel.this.isEditable());
            setLayout(new MigLayout("wrap 4, fill, insets 0"));
            setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                                                       Messages.message("outsideColony")));
        }

        /**
         * Initialize this OutsideColonyPanel.
         */
        @Override
        public void initialize() {
            cleanup();
            super.initialize();
            if (colony != null) setName(colony.getName() + " - port");
        }

        /**
         * Adds a component to this OutsideColonyPanel and makes sure
         * that the unit that the component represents gets modified
         * so that it will be located in the colony.
         *
         * @param comp The component to add to this ColonistsPanel.
         * @param editState Must be set to 'true' if the state of the component
         *            that is added (which should be a dropped component
         *            representing a Unit) should be changed so that the
         *            underlying unit will be located in the colony.
         * @return The component argument.
         */
        public Component add(Component comp, boolean editState) {
            Container oldParent = comp.getParent();
            if (editState) {
                if (comp instanceof UnitLabel) {
                    UnitLabel unitLabel = ((UnitLabel) comp);
                    Unit unit = unitLabel.getUnit();

                    if (!unit.isOnCarrier()) {
                        getController().putOutsideColony(unit);
                    }

                    if (unit.getColony() == null) {
                        closeColonyPanel();
                        return null;
                    } else if (!(unit.getLocation() instanceof Tile) && !unit.isOnCarrier()) {
                        return null;
                    }

                    oldParent.remove(comp);
                    initialize();
                    return comp;
                } else {
                    logger.warning("An invalid component got dropped on this ColonistsPanel.");
                    return null;
                }
            } else {
                ((UnitLabel) comp).setSmall(false);
                Component c = add(comp);
                return c;
            }
        }

        @Override
        protected void addPropertyChangeListeners() {
            if (colony != null) {
                colony.getTile().addPropertyChangeListener(Tile.UNIT_CHANGE,
                                                           this);
            }
        }

        @Override
        protected void removePropertyChangeListeners() {
            if (colony != null) {
                colony.getTile().removePropertyChangeListener(Tile.UNIT_CHANGE,
                                                              this);
            }
        }

        @Override
        public String getUIClassID() {
            return "OutsideColonyPanelUI";
        }

        public boolean accepts(Unit unit) {
            return !unit.isCarrier();
        }

        public boolean accepts(Goods goods) {
            return false;
        }
    }

    /**
     * A panel that holds UnitsLabels that represent naval Units that are
     * waiting in the port of the colony.
     */
    public final class ColonyInPortPanel extends InPortPanel {

        public ColonyInPortPanel() {
            super(ColonyPanel.this, null, ColonyPanel.this.isEditable());
            setLayout(new MigLayout("wrap 3, fill, insets 0"));
            setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                                                       Messages.message("inPort")));
        }

        public void initialize() {
            if (colony != null) {
                setName(colony.getName() + " - port");
                super.initialize();
            }
        }

        public boolean accepts(Unit unit) {
            return unit.isCarrier();
        }
    }

    /**
     * A panel that holds goods that represent cargo that is inside
     * the Colony.
     */
    public final class WarehousePanel extends JPanel
        implements DropTarget, PropertyChangeListener {

        /** The enclosing colony panel. */
        private final ColonyPanel colonyPanel;


        /**
         * Creates a WarehousePanel.
         *
         * @param colonyPanel The panel that holds this WarehousePanel.
         */
        public WarehousePanel() {
            this.colonyPanel = ColonyPanel.this;
            setLayout(new MigLayout("fill, gap push, insets 0"));
        }

        /**
         * Initialize this WarehousePanel.
         */
        public void initialize() {
            cleanup();
            addPropertyChangeListeners();
            update();
        }

        /**
         * Update this WarehousePanel.
         */
        private void update() {
            removeAll();
            if (colony == null) return;

            ClientOptions options = getClientOptions();
            final int threshold = (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS))
                ? 1
                : options.getInteger(ClientOptions.MIN_NUMBER_FOR_DISPLAYING_GOODS);
            final Game game = colony.getGame();
            final Specification spec = colony.getSpecification();

            for (GoodsType goodsType : spec.getGoodsTypeList()) {
                if (!goodsType.isStorable()) continue;
                int count = colony.getGoodsCount(goodsType);
                if (count >= threshold) {
                    Goods goods = new Goods(game, colony, goodsType, count);
                    GoodsLabel goodsLabel = new GoodsLabel(goods, getGUI());
                    if (colonyPanel.isEditable()) {
                        goodsLabel.setTransferHandler(defaultTransferHandler);
                        goodsLabel.addMouseListener(pressListener);
                    }
                    add(goodsLabel, false);
                }
            }
            revalidate();
            repaint();
        }

        /**
         * Clean up this WarehousePanel.
         */
        public void cleanup() {
            removePropertyChangeListeners();
        }

        /**
         * Adds a component to this WarehousePanel and makes sure that
         * the unit or good that the component represents gets
         * modified so that it is on board the currently selected
         * ship.
         *
         * @param comp The component to add to this WarehousePanel.
         * @param editState Must be set to 'true' if the state of the
         *            component that is added (which should be a
         *            dropped component representing a Unit or good)
         *            should be changed so that the underlying unit or
         *            goods are on board the currently selected ship.
         * @return The component argument.
         */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (!(comp instanceof GoodsLabel)) {
                    logger.warning("Invalid component dropped on this WarehousePanel.");
                    return null;
                }
                comp.getParent().remove(comp);
                return comp;
            }

            return add(comp);
        }

        private void addPropertyChangeListeners() {
            Colony colony = getColony();
            if (colony != null) {
                colony.getGoodsContainer().addPropertyChangeListener(this);
            }
        }

        private void removePropertyChangeListeners() {
            Colony colony = getColony();
            if (colony != null) {
                colony.getGoodsContainer().removePropertyChangeListener(this);
            }
        }

        public void propertyChange(PropertyChangeEvent event) {
            logger.finest(getColony().getName() + "-warehouse change "
                          + event.getPropertyName()
                          + ": " + event.getOldValue()
                          + " -> " + event.getNewValue());
            update();
        }

        public boolean accepts(Unit unit) {
            return false;
        }

        public boolean accepts(Goods goods) {
            return true;
        }

        @Override
        public String getUIClassID() {
            return "WarehousePanelUI";
        }
    }


    /**
     * This panel is a list of the colony's buildings.
     */
    public final class BuildingsPanel extends JPanel {

        /** The parent colony panel. */
        private final ColonyPanel colonyPanel;


        /**
         * Creates this BuildingsPanel.
         */
        public BuildingsPanel() {
            this.colonyPanel = ColonyPanel.this;
            setLayout(new MigLayout("fill, wrap 4, insets 0, gap 0:10:10:push"));
        }

        /**
         * Initializes the game data in this buildings panel.
         */
        public void initialize() {
            cleanup();
            if (colony == null) return;

            MouseAdapter mouseAdapter = new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        getGUI().showBuildQueuePanel(getColony());
                    }
                };

            List<Building> buildings = colony.getBuildings();
            sortBuildings(buildings);
            for (Building building : buildings) {
                ASingleBuildingPanel aSBP = new ASingleBuildingPanel(building);
                if (colonyPanel.isEditable()) {
                    aSBP.addMouseListener(releaseListener);
                    aSBP.setTransferHandler(defaultTransferHandler);
                }
                aSBP.setOpaque(false);
                aSBP.addMouseListener(mouseAdapter);
                add(aSBP);
            }

            update();
        }

        /**
         * Update this buildings panel.
         */
        public void update() {
            for (Component component : getComponents()) {
                if (component instanceof ASingleBuildingPanel) {
                    ((ASingleBuildingPanel)component).update();
                }
            }
            repaint();
        }

        /**
         * Clean up this buildings panel.
         */
        public void cleanup() {
            for (Component component : getComponents()) {
                if (component instanceof ASingleBuildingPanel) {
                    ((ASingleBuildingPanel)component).cleanup();
                }
            }
            removeAll();
        }

        @Override
        public String getUIClassID() {
            return "BuildingsPanelUI";
        }


        /**
         * This panel is a single line (one building) in the
         * <code>BuildingsPanel</code>.
         */
        public final class ASingleBuildingPanel extends BuildingPanel
            implements Autoscroll, DropTarget  {

            /**
             * Creates this ASingleBuildingPanel.
             *
             * @param building The building to display information from.
             */
            public ASingleBuildingPanel(Building building) {
                super(getFreeColClient(), building, getGUI());
            }

            @Override
            public void initialize() {
                super.initialize();

                update();
            }

            @Override
            public void update() {
                super.update();

                if (colonyPanel.isEditable()) {
                    for (UnitLabel unitLabel : getUnitLabels()) {
                        unitLabel.setTransferHandler(defaultTransferHandler);
                        unitLabel.addMouseListener(pressListener);
                    }
                }
            }

            // Do not need an overriding cleanup, BuildingPanel.cleanup is good.

            public void autoscroll(Point p) {
                JViewport vp = (JViewport) colonyPanel.buildingsPanel.getParent();
                if (getLocation().y + p.y - vp.getViewPosition().y < SCROLL_AREA_HEIGHT) {
                    vp.setViewPosition(new Point(vp.getViewPosition().x,
                                                 Math.max(vp.getViewPosition().y - SCROLL_SPEED, 0)));
                } else if (getLocation().y + p.y - vp.getViewPosition().y >= vp.getHeight() - SCROLL_AREA_HEIGHT) {
                    vp.setViewPosition(new Point(vp.getViewPosition().x,
                                                 Math.min(vp.getViewPosition().y + SCROLL_SPEED,
                                                          colonyPanel.buildingsPanel.getHeight()
                                                          - vp.getHeight())));
                }
            }

            public Insets getAutoscrollInsets() {
                Rectangle r = getBounds();
                return new Insets(r.x, r.y, r.width, r.height);
            }

            /**
             * Adds a component to this ASingleBuildingPanel and makes
             * sure that the unit that the component represents gets
             * modified so that it will be located in the colony.
             *
             * @param comp The component to add to this ColonistsPanel.
             * @param editState Must be set to 'true' if the state of the
             *            component that is added (which should be a dropped
             *            component representing a Unit) should be changed so
             *            that the underlying unit will be located in the
             *            colony.
             * @return The component argument.
             */
            public Component add(Component comp, boolean editState) {
                Container oldParent = comp.getParent();
                if (editState) {
                    if (comp instanceof UnitLabel) {
                        if (tryWork(((UnitLabel) comp).getUnit())) {
                            oldParent.remove(comp);
                        } else {
                            return null;
                        }
                    } else {
                        logger.warning("An invalid component was dropped"
                            + " on this ASingleBuildingPanel.");
                        return null;
                    }
                }
                update();
                return null;
            }

            private boolean tryWork(Unit unit) {
                Building building = getBuilding();
                NoAddReason reason = building.getNoAddReason(unit);
                if (reason != NoAddReason.NONE) {
                    getGUI().errorMessage("noAddReason."
                        + reason.toString().toLowerCase(Locale.US));
                    return false;
                }

                getController().work(unit, building);
                return true;
            }

            @Override
            public void propertyChange(PropertyChangeEvent event) {
                super.propertyChange(event);
                
                colonyPanel.updateProduction();
            }

            public boolean accepts(Unit unit) {
                return unit.isPerson();
            }

            public boolean accepts(Goods goods) {
                return false;
            }
        }
    }

    /**
     * A panel that displays the tiles in the immediate area around the colony.
     */
    public final class TilePanel extends FreeColPanel {

        /** The parent colony panel. */
        private final ColonyPanel colonyPanel;

        /** The tiles around the colony. */
        private Tile[][] tiles = new Tile[3][3];


        /**
         * Creates a TilePanel.
         *
         * @param freeColClient The container <code>FreeColClient</code>.
         */
        public TilePanel(FreeColClient freeColClient) {
            super(freeColClient, freeColClient.getGUI());

            this.colonyPanel = ColonyPanel.this;
            setBackground(Color.BLACK);
            setBorder(null);
            setLayout(null);
        }

        /**
         * Initialize the game data in this Tile panel.
         */
        public void initialize() {
            cleanup();
            if (colony == null) return;

            Tile tile = colony.getTile();
            tiles[0][0] = tile.getNeighbourOrNull(Direction.N);
            tiles[0][1] = tile.getNeighbourOrNull(Direction.NE);
            tiles[0][2] = tile.getNeighbourOrNull(Direction.E);
            tiles[1][0] = tile.getNeighbourOrNull(Direction.NW);
            tiles[1][1] = tile;
            tiles[1][2] = tile.getNeighbourOrNull(Direction.SE);
            tiles[2][0] = tile.getNeighbourOrNull(Direction.W);
            tiles[2][1] = tile.getNeighbourOrNull(Direction.SW);
            tiles[2][2] = tile.getNeighbourOrNull(Direction.S);

            int layer = 2;
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    if (tiles[x][y] == null) continue;
                    ColonyTile colonyTile = colony.getColonyTile(tiles[x][y]);
                    ASingleTilePanel p = new ASingleTilePanel(colonyTile, x, y);
                    p.initialize();
                    add(p, new Integer(layer));
                    layer++;
                }
            }

            update();
        }

        /**
         * Update this tile panel.
         */
        public void update() {
            for (Component component : getComponents()) {
                if (component instanceof ASingleTilePanel) {
                    ((ASingleTilePanel)component).update();
                }
            }
            repaint();
        }

        /**
         * Clean up this tile panel.
         */
        public void cleanup() {
            for (Component component : getComponents()) {
                if (component instanceof ASingleTilePanel) {
                    ((ASingleTilePanel)component).cleanup();
                }
            }
            removeAll();
        }

        @Override
        public void paintComponent(Graphics g) {
            g.setColor(Color.black);
            g.fillRect(0, 0, getWidth(), getHeight());

            if (colony == null) return;
            TileType tileType = colony.getTile().getType();
            int tileWidth = getLibrary().getTerrainImageWidth(tileType) / 2;
            int tileHeight = getLibrary().getTerrainImageHeight(tileType) / 2;
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    if (tiles[x][y] == null) continue;
                    int xx = ((2 - x) + y) * tileWidth;
                    int yy = (x + y) * tileHeight;
                    g.translate(xx, yy);
                    getGUI().displayColonyTile((Graphics2D)g, tiles[x][y],
                                               colony);
                    g.translate(-xx, -yy);
                }
            }
        }

        /**
         * Panel for visualizing a <code>ColonyTile</code>.  The
         * component itself is not visible, however the content of the
         * component is (i.e. the people working and the production)
         */
        public final class ASingleTilePanel extends JPanel
            implements DropTarget, PropertyChangeListener {

            /**
             * The colony tile to monitor.
             */
            private ColonyTile colonyTile;


            /**
             * Create a new single tile panel.
             *
             * @param colonyTile The <code>ColonyTile</code> to monitor.
             * @param x The x offset.
             * @param y The y offset.
             */
            public ASingleTilePanel(ColonyTile colonyTile, int x, int y) {
                this.colonyTile = colonyTile;

                setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
                setOpaque(false);
                TileType tileType = colonyTile.getTile().getType();
                // Size and position:
                int width = getLibrary().getTerrainImageWidth(tileType);
                int height = getLibrary().getTerrainImageHeight(tileType);
                setSize(width, height);
                setLocation(((2 - x) + y) * width / 2, (x + y) * height / 2);
            }

            /**
             * Initialize this single tile panel.
             */
            public void initialize() {
                cleanup();

                addPropertyChangeListeners();
                update();
            }

            /**
             * Update this single tile panel.
             */
            public void update() {
                removeAll();

                UnitLabel label = null;
                for (Unit unit : colonyTile.getUnitList()) {
                    label = new UnitLabel(getFreeColClient(), unit, getGUI());
                    if (colonyPanel.isEditable()) {
                        label.setTransferHandler(defaultTransferHandler);
                        label.addMouseListener(pressListener);
                    }
                    super.add(label);
                }
                updateDescriptionLabel(label, true);

                if (colonyTile.isColonyCenterTile()) {
                    setLayout(new GridLayout(2, 1));
                    ProductionInfo info = colony.getProductionInfo(colonyTile);
                    if (info != null) {
                        for (AbstractGoods ag : info.getProduction()) {
                            add(new ProductionLabel(getFreeColClient(), getGUI(), ag));
                        }
                    }
                }

                if (colonyPanel.isEditable()) {
                    setTransferHandler(defaultTransferHandler);
                    addMouseListener(releaseListener);
                }
            }

            /**
             * Clean up this single tile panel.
             */
            public void cleanup() {
                removePropertyChangeListeners();
            }

            /**
             * Updates the description label, which is a tooltip with
             * the terrain type, road and plow indicator, if any.
             *
             * If a unit is on it update the tooltip of it instead.
             */
            private void updateDescriptionLabel(UnitLabel unit, boolean toAdd) {
                String tileMsg = Messages.message(colonyTile.getLabel());
                if (unit == null) {
                    setToolTipText(tileMsg);
                } else {
                    String unitMsg
                        = Messages.message(Messages.getLabel(unit.getUnit()));
                    if (toAdd) unitMsg = tileMsg + " [" + unitMsg + "]";
                    unit.setDescriptionLabel(unitMsg);
                }
            }

            /**
             * Checks if this <code>JComponent</code> contains the given
             * coordinate.
             *
             * @param px The x coordinate to check.
             * @param py The y coordinate to check.
             */
            @Override
            public boolean contains(int px, int py) {
                int w = getWidth();
                int h = getHeight();
                int dx = Math.abs(w/2 - px);
                int dy = Math.abs(h/2 - py);
                return (dx + w * dy / h) <= w/2;
            }

            /**
             * Adds a component to this CargoPanel and makes sure that
             * the unit or good that the component represents gets
             * modified so that it is on board the currently selected
             * ship.
             *
             * @param comp The component to add to this CargoPanel.
             * @param editState Must be set to 'true' if the state of the
             *            component that is added (which should be a dropped
             *            component representing a Unit or good) should be
             *            changed so that the underlying unit or goods are on
             *            board the currently selected ship.
             * @return The component argument.
             */
            public Component add(Component comp, boolean editState) {
                Container oldParent = comp.getParent();
                if (editState) {
                    if (comp instanceof UnitLabel) {
                        if (tryWork(((UnitLabel) comp).getUnit())) {
                            oldParent.remove(comp);
                            ((UnitLabel) comp).setSmall(false);
                        } else {
                            return null;
                        }
                    } else {
                        logger.warning("An invalid component was dropped"
                                       + " on this ASingleTilePanel.");
                        return null;
                    }
                }

                update();
                return comp;
            }

            /**
             * Try to work this tile with a specified unit.
             *
             * @param unit The <code>Unit</code> to work the tile.
             * @return True if the unit succeeds.
             */
            private boolean tryWork(Unit unit) {
                Tile tile = colonyTile.getWorkTile();
                Colony colony = getColony();
                Player player = unit.getOwner();

                if (tile.getOwningSettlement() != colony) {
                    // Need to acquire the tile before working it.
                    NoClaimReason claim
                        = player.canClaimForSettlementReason(tile);
                    switch (claim) {
                    case NONE: case NATIVES:
                        if (getController().claimLand(tile, colony, 0)
                            && tile.getOwningSettlement() == colony) {
                            logger.info("Colony " + colony.getName()
                                + " claims tile " + tile.toString()
                                + " with unit " + unit.getId());
                        } else {
                            logger.warning("Colony " + colony.getName()
                                + " did not claim " + tile.toString()
                                + " with unit " + unit.getId());
                            return false;
                        }
                        break;
                    default: // Otherwise, can not use land
                        getGUI().errorMessage("noClaimReason."
                            + claim.toString().toLowerCase(Locale.US));
                        return false;
                    }
                    // Check reason again, claim should be satisfied.
                    if (tile.getOwningSettlement() != colony) {
                        throw new IllegalStateException("Claim failed");
                    }
                }

                // Claim sorted, but complain about other failure.
                NoAddReason reason = colonyTile.getNoAddReason(unit);
                if (reason != NoAddReason.NONE) {
                    getGUI().errorMessage("noAddReason."
                        + reason.toString().toLowerCase(Locale.US));
                    return false;
                }

                // Choose the work to be done.
                // FTM, do not change the work type unless explicitly
                // told to as this destroys experience (TODO: allow
                // multiple experience accumulation?).
                GoodsType workType;
                if ((workType = unit.getWorkType()) != null
                    && colonyTile.getProductionOf(unit, workType) <= 0) {
                    workType = null;
                }
                if (workType == null // Try experience.
                    && (workType = unit.getExperienceType()) != null
                    && colonyTile.getProductionOf(unit, workType) <= 0) {
                    workType = null;
                }
                if (workType == null // Try expertise?
                    && (workType = unit.getType().getExpertProduction()) != null
                    && colonyTile.getProductionOf(unit, workType) <= 0) {
                    workType = null;
                }
                // Try best work type?
                if (workType == null
                    && (workType = colonyTile.getBestWorkType(unit)) != null
                    && colonyTile.getProductionOf(unit, workType) <= 0) {
                    workType = null;
                }
                // No good, just leave it alone then.
                if (workType == null) workType = unit.getWorkType();
                // Set the unit to work.  Note this might upgrade the
                // unit, and possibly even change its work type as the
                // server has the right to maintain consistency.
                getController().work(unit, colonyTile);
                // Now recheck, and see if we want to change to the
                // expected work type.
                if (workType != null
                    && workType != unit.getWorkType()) {
                    getController().changeWorkType(unit, workType);
                }

                if (getClientOptions()
                    .getBoolean(ClientOptions.SHOW_NOT_BEST_TILE)) {
                    ColonyTile best = colony.getVacantColonyTileFor(unit, false,
                                                                    workType);
                    if (best != null && colonyTile != best
                        && (colonyTile.getPotentialProduction(workType, unit.getType())
                            < best.getPotentialProduction(workType, unit.getType()))) {
                        StringTemplate template
                            = StringTemplate.template("colonyPanel.notBestTile")
                            .addStringTemplate("%unit%", Messages.getLabel(unit))
                            .add("%goods%", workType.getNameKey())
                            .addStringTemplate("%tile%", best.getLabel());
                        getGUI().showInformationMessage(template);
                    }
                }
                return true;
            }

            private void addPropertyChangeListeners() {
                colonyTile.addPropertyChangeListener(this);
            }

            private void removePropertyChangeListeners() {
                colonyTile.removePropertyChangeListener(this);
            }

            /**
             * Handle a property change event sent to this single tile panel.
             *
             * @param event The <code>PropertyChangeEvent</code> to handle.
             */
            public void propertyChange(PropertyChangeEvent event) {
                String property = event.getPropertyName();
                logger.finest(colonyTile.getId() + " change " + property
                              + ": " + event.getOldValue()
                              + " -> " + event.getNewValue());
                colonyPanel.updateProduction();
            }

            public boolean accepts(Unit unit) {
                return unit.isPerson();
            }

            public boolean accepts(Goods goods) {
                return false;
            }
        }
    }
}
