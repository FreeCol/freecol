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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ComponentInputMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Colony.ColonyChangeEvent;
import net.sf.freecol.common.resources.ResourceManager;

import net.miginfocom.swing.MigLayout;

/**
 * This is a panel for the Colony display. It shows the units that are working
 * in the colony, the buildings and much more.
 */
public final class ColonyPanel extends FreeColPanel implements ActionListener,PropertyChangeListener {


    private static Logger logger = Logger.getLogger(ColonyPanel.class.getName());

    /**
     * The height of the area in which autoscrolling should happen.
     */
    public static final int SCROLL_AREA_HEIGHT = 40;

    /**
     * The speed of the scrolling.
     */
    public static final int SCROLL_SPEED = 40;

    private static final int EXIT = 0, UNLOAD = 2, WAREHOUSE = 4, FILL = 5;

    private final JLabel rebelLabel, bonusLabel, royalistLabel;

    private final JPanel productionPanel;

    private final JComboBox nameBox;

    private final OutsideColonyPanel outsideColonyPanel;

    private final InPortPanel inPortPanel;

    private final ColonyCargoPanel cargoPanel;

    private final WarehousePanel warehousePanel;

    private final TilePanel tilePanel;

    private final BuildingsPanel buildingsPanel;

    private final DefaultTransferHandler defaultTransferHandler;

    private final MouseListener pressListener;

    private final MouseListener releaseListener;

    private Colony colony;

    private UnitLabel selectedUnit;

    private JButton exitButton = new JButton(Messages.message("close"));

    private JButton unloadButton = new JButton(Messages.message("unload"));

    private JButton fillButton = new JButton(Messages.message("fill"));

    private JButton warehouseButton = new JButton(Messages.message("warehouseDialog.name"));

    private static final Font hugeFont = new Font("Dialog", Font.BOLD, 24);


    /**
     * The constructor for the panel.
     * 
     * @param parent The parent of this panel
     */
    public ColonyPanel(Canvas parent) {
        super(parent);

        setFocusCycleRoot(true);

        // Use ESCAPE for closing the ColonyPanel:
        InputMap closeInputMap = new ComponentInputMap(exitButton);
        closeInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "pressed");
        closeInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true), "released");
        SwingUtilities.replaceUIInputMap(exitButton, JComponent.WHEN_IN_FOCUSED_WINDOW, closeInputMap);

        InputMap unloadInputMap = new ComponentInputMap(unloadButton);
        unloadInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0, false), "pressed");
        unloadInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0, true), "released");
        SwingUtilities.replaceUIInputMap(unloadButton, JComponent.WHEN_IN_FOCUSED_WINDOW, unloadInputMap);

        InputMap fillInputMap = new ComponentInputMap(fillButton);
        fillInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0, false), "pressed");
        fillInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0, true), "released");
        SwingUtilities.replaceUIInputMap(fillButton, JComponent.WHEN_IN_FOCUSED_WINDOW, fillInputMap);

        productionPanel = new JPanel();
        productionPanel.setOpaque(false);

        outsideColonyPanel = new OutsideColonyPanel();
        outsideColonyPanel.setToolTipText(Messages.message("outsideColony"));
        outsideColonyPanel.setLayout(new GridLayout(0, 8));

        inPortPanel = new InPortPanel();
        inPortPanel.setToolTipText(Messages.message("inPort"));
        inPortPanel.setLayout(new GridLayout(0, 2));

        warehousePanel = new WarehousePanel(this);
        warehousePanel.setToolTipText(Messages.message("goods"));
        warehousePanel.setLayout(new GridLayout(1, 0));

        tilePanel = new TilePanel(this);
        tilePanel.setToolTipText(Messages.message("surroundingArea"));

        buildingsPanel = new BuildingsPanel(this);
        buildingsPanel.setToolTipText(Messages.message("buildings"));

        cargoPanel = new ColonyCargoPanel(parent);
        cargoPanel.setParentPanel(this);
        cargoPanel.setToolTipText(Messages.message("cargoOnCarrier"));
        cargoPanel.setLayout(new GridLayout(1, 0));

        defaultTransferHandler = new DefaultTransferHandler(parent, this);
        pressListener = new DragListener(this);
        releaseListener = new DropListener();

        rebelLabel = new JLabel(Messages.message("colonyPanel.rebelLabel"));
        bonusLabel = new JLabel(Messages.message("colonyPanel.bonusLabel"));
        royalistLabel = new JLabel(Messages.message("colonyPanel.royalistLabel"));

        JScrollPane outsideColonyScroll = new JScrollPane(outsideColonyPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        outsideColonyScroll.getVerticalScrollBar().setUnitIncrement( 16 );
        JScrollPane inPortScroll = new JScrollPane(inPortPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        inPortScroll.getVerticalScrollBar().setUnitIncrement( 16 );
        JScrollPane cargoScroll = new JScrollPane(cargoPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane warehouseScroll = new JScrollPane(warehousePanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane tilesScroll = new JScrollPane(tilePanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane buildingsScroll = new JScrollPane(buildingsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        buildingsScroll.getVerticalScrollBar().setUnitIncrement( 16 );

        // Make the colony label
        nameBox = new JComboBox();
        nameBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                initialize((Colony) nameBox.getSelectedItem());
            }
        });
        nameBox.setFont(smallHeaderFont);

        buildingsScroll.setAutoscrolls(true);

        /** Borders */
        tilesScroll.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        buildingsScroll.setBorder(BorderFactory.createEtchedBorder());
        warehouseScroll.setBorder(BorderFactory.createEtchedBorder());
        cargoScroll.setBorder(BorderFactory.createEtchedBorder());
        inPortScroll.setBorder(BorderFactory.createEtchedBorder());
        outsideColonyScroll.setBorder(BorderFactory.createEtchedBorder());

        exitButton.setActionCommand(String.valueOf(EXIT));
        enterPressesWhenFocused(exitButton);
        exitButton.addActionListener(this);

        unloadButton.setActionCommand(String.valueOf(UNLOAD));
        enterPressesWhenFocused(unloadButton);
        unloadButton.addActionListener(this);

        fillButton.setActionCommand(String.valueOf(FILL));
        enterPressesWhenFocused(fillButton);
        fillButton.addActionListener(this);
        
        warehouseButton.setActionCommand(String.valueOf(WAREHOUSE));
        enterPressesWhenFocused(warehouseButton);
        warehouseButton.addActionListener(this);

        selectedUnit = null;

        // See the message of Ulf Onnen for more information about the presence
        // of this fake mouse listener.
        addMouseListener(new MouseAdapter() {
        });

        setLayout(new MigLayout("fill, wrap 2", "", ""));

        add(nameBox, "growx, height 48:");
        add(productionPanel, "align center");
        add(tilesScroll, "width 390!, height 200!, top");
        add(buildingsScroll, "span 1 3, grow 200");
        add(rebelLabel, "split 3");
        add(bonusLabel, "gap left push, gap right push");
        add(royalistLabel);
        add(inPortScroll, "grow, height 60:");
        add(cargoScroll, "grow, height 60:");
        add(outsideColonyScroll, "grow, height 60:");
        add(warehouseScroll, "span, height 60!, growx");
        add(unloadButton, "span, split 5, align center");
        add(fillButton);
        add(warehouseButton);
        add(exitButton);

        setSize(parent.getWidth(), parent.getHeight() - parent.getMenuBarHeight());

    }

    @Override
    public void requestFocus() {
        exitButton.requestFocus();
    }

    /**
     * Initialize the data on the window. This is the same as calling:
     * <code>initialize(colony, game, null)</code>.
     * 
     * @param colony The <code>Colony</code> to be displayed.
     * @param game The <code>Game</code> in which the given
     *            <code>Colony</code> is a part of.
     */
    public void initialize(Colony colony) {
        initialize(colony, null);
    }

    /**
     * Initialize the data on the window.
     * 
     * @param colony The <code>Colony</code> to be displayed.
     * @param preSelectedUnit This <code>Unit</code> will be selected if it is
     *            not <code>null</code> and it is a carrier located in the
     *            given <code>Colony</code>.
     */
    public void initialize(final Colony colony, Unit preSelectedUnit) {
        setColony(colony);

        rebelLabel.setIcon(new ImageIcon(ResourceManager.getImage(colony.getOwner().getNation().getId()
                                                                  + ".coat-of-arms.image", 0.5)));
        rebelLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        royalistLabel.setIcon(new ImageIcon(ResourceManager.getImage(colony.getOwner().getNation().getRefNation().getId()
                                                                     + ".coat-of-arms.image", 0.5)));
        royalistLabel.setHorizontalTextPosition(SwingConstants.LEFT);

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
        nameBox.setEnabled(isEditable());

        //
        // Remove the old components from the panels.
        //

        cargoPanel.removeAll();
        warehousePanel.removeAll();
        //outsideColonyPanel.removeAll();
        inPortPanel.removeAll();
        tilePanel.removeAll();

        //
        // Units outside the colony:
        //

        Tile tile = colony.getTile();

        UnitLabel lastCarrier = null;
        UnitLabel preSelectedUnitLabel = null;
        selectedUnit = null;

        Iterator<Unit> tileUnitIterator = tile.getUnitIterator();
        while (tileUnitIterator.hasNext()) {
            Unit unit = tileUnitIterator.next();

            UnitLabel unitLabel = new UnitLabel(unit, getCanvas());
            if (isEditable()) {
                unitLabel.setTransferHandler(defaultTransferHandler);
            }
            if (isEditable() || unit.isCarrier()) {
                unitLabel.addMouseListener(pressListener);
            }

            if (!unit.isCarrier()) {
                //outsideColonyPanel.add(unitLabel, false);
            } else {
                TradeRoute tradeRoute;
                if ((tradeRoute = unit.getTradeRoute()) != null) {
                    unitLabel.setDescriptionLabel(unit.getName() + " ("
                                                  + tradeRoute.getName() + ")");
                }
                inPortPanel.add(unitLabel);
                lastCarrier = unitLabel;
                if (unit == preSelectedUnit) {
                    preSelectedUnitLabel = unitLabel;
                }

            }
        }

        if (preSelectedUnitLabel == null) {
            setSelectedUnitLabel(lastCarrier);
        } else {
            setSelectedUnitLabel(preSelectedUnitLabel);
        }

        updateCarrierButtons();

        //
        // Warehouse panel:
        //

        warehousePanel.initialize();

        //
        // Units in buildings:
        //

        buildingsPanel.initialize();

        //
        // TilePanel:
        //

        tilePanel.initialize();

        updateNameBox();
        updateProductionPanel();
        updateSoLLabel();


        outsideColonyPanel.setColony(colony);

    }

    /**
     * Enables the unload and fill buttons if the currently selected unit is a
     * carrier with some cargo.
     */
    private void updateCarrierButtons() {
        unloadButton.setEnabled(false);
        fillButton.setEnabled(false);
        if (isEditable() && selectedUnit != null) {
            Unit unit = selectedUnit.getUnit();
            if (unit != null && unit.isCarrier() && unit.getSpaceLeft() < unit.getType().getSpace()) {
                unloadButton.setEnabled(true);
                for (Goods goods : unit.getGoodsList()) {
                    if (getColony().getGoodsCount(goods.getType()) > 0) {
                        fillButton.setEnabled(true);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Updates the SoL membership label.
     */
    private void updateSoLLabel() {
        if (getColony() == null) {
            // ApgetCanvas()ly this can happen
            logger.warning("Colony panel has 'null' colony.");
            return;
        }
        rebelLabel.setText(Messages.message("colonyPanel.rebelLabel", "%number%",
                                            Integer.toString(getColony().getSoL())));
        //"%solNumber%", Integer.toString(getColony().getMembers()),
        bonusLabel.setText(Messages.message("colonyPanel.bonusLabel", "%number%",
                                            Integer.toString(getColony().getProductionBonus())));
        royalistLabel.setText(Messages.message("colonyPanel.royalistLabel", "%number%",
                                               Integer.toString(getColony().getTory())));
        //"%toryNumber%", Integer.toString(getColony().getUnitCount() - getColony().getMembers()),
    }

    public void updateNameBox() {
        // Remove all action listeners, so the update has no effect (except
        // updating the list).
        ActionListener[] listeners = nameBox.getActionListeners();
        for (ActionListener al : listeners)
            nameBox.removeActionListener(al);

        if (getColony() == null) {
            // ApgetCanvas()ly this can happen
            return;
        }
        nameBox.removeAllItems();
        List<Colony> settlements = getColony().getOwner().getColonies();
        sortColonies(settlements);
        for (Colony colony : settlements) {
            nameBox.addItem(colony);
        }
        nameBox.setSelectedItem(getColony());

        for (ActionListener al : listeners) {
            nameBox.addActionListener(al);
        }
    }

    private void sortBuildings(List<Building> buildings) {
        Collections.sort(buildings, Building.getBuildingComparator());
    }
    
    private void sortColonies(List<Colony> colonies) {
        Collections.sort(colonies, getClient().getClientOptions().getColonyComparator());
    }

    public void updateProductionPanel() {
        productionPanel.removeAll();

        final int foodFarmsProduction = colony.getProductionOf(Goods.FOOD);
        final int foodFishProduction = colony.getProductionOf(Goods.FISH);
        final int humanFoodConsumption = colony.getFoodConsumption();
        int horsesProduced = colony.getProductionOf(Goods.HORSES);
        final int horseStock = colony.getGoodsCount(Goods.HORSES);
        final int bells = colony.getProductionOf(Goods.BELLS);
        final int crosses = colony.getProductionOf(Goods.CROSSES);
        
        int foodProduction = foodFarmsProduction + foodFishProduction;
        horsesProduced = (horseStock <= 0)? 0 : horsesProduced;
        
        // The food that is used. If not enough food is produced, the
        // complete production.  Horses consume 1 food each, so they
        // need to be added to the used food.
        
        //Fish should be consumed preferably, to allow surplus for production of horses
        final int usedFood = Math.min(humanFoodConsumption, foodProduction) + horsesProduced;
        final int usedFish = colony.getFoodConsumptionByType(Goods.FISH);
        final int usedCorn = Math.max(usedFood - usedFish, 0);
        
        if (usedFish == 0) {
            ProductionLabel label = new ProductionLabel(Goods.FOOD, usedFood, getCanvas());
            label.setToolTipPrefix(Messages.message("totalProduction"));
            productionPanel.add(label);
        } else {
            ProductionMultiplesLabel label = new ProductionMultiplesLabel(Goods.FOOD, usedCorn,
                                                                          Goods.FISH, usedFish, getCanvas());
            label.setToolTipPrefix(Messages.message("totalProduction"));
            productionPanel.add(label);
        }
        
        int remainingCorn = foodFarmsProduction - usedCorn;
        final int remainingFish = foodFishProduction  - usedFish;

        int surplusFood = foodProduction - humanFoodConsumption - horsesProduced;
        remainingCorn = Math.min(surplusFood, remainingCorn);
        ProductionMultiplesLabel surplusLabel =
            new ProductionMultiplesLabel(Goods.FOOD, remainingCorn, Goods.FISH, remainingFish, getCanvas());
        surplusLabel.setDrawPlus(true);
        surplusLabel.setToolTipPrefix(Messages.message("surplusProduction"));
        productionPanel.add(surplusLabel);

        if( horsesProduced != 0 ) {			// Skip the horses label if there is no stock
            ProductionLabel horseLabel = new ProductionLabel(Goods.HORSES, horsesProduced, getCanvas());
            horseLabel.setMaxGoodsIcons(1);
            productionPanel.add(horseLabel);
        }

        ProductionLabel bellsLabel = new ProductionLabel(Goods.BELLS, bells, getCanvas());
        bellsLabel.setToolTipPrefix(Messages.message("totalProduction"));
        productionPanel.add(bellsLabel);
        int surplusBells = bells - colony.getConsumption(Goods.BELLS);
        ProductionLabel bellsSurplusLabel = new ProductionLabel(Goods.BELLS, surplusBells, getCanvas());
        bellsSurplusLabel.setToolTipPrefix(Messages.message("surplusProduction"));
        productionPanel.add(bellsSurplusLabel);
        
        productionPanel.add(new ProductionLabel(Goods.CROSSES, crosses, getCanvas()));
        productionPanel.revalidate();
    }
    
    /**
     * Returns the currently select unit.
     * 
     * @return The currently select unit.
     */
    public Unit getSelectedUnit() {
        if (selectedUnit == null)
            return null;
        else
            return selectedUnit.getUnit();
    }

    /**
     * Returns the currently select unit.
     * 
     * @return The currently select unit.
     */
    public UnitLabel getSelectedUnitLabel() {
        return selectedUnit;
    }

    /**
     * Analyzes an event and calls the right external methods to take care of
     * the user's request.
     * 
     * @param event The incoming action event
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            switch (Integer.valueOf(command).intValue()) {
            case EXIT:
                closeColonyPanel();
                break;
            case UNLOAD:
                unload();
                break;
            case WAREHOUSE:
                if (getCanvas().showFreeColDialog(new WarehouseDialog(getCanvas(), colony))) {
                    warehousePanel.update();
                }
                break;
            case FILL:
                fill();
                break;
            default:
                logger.warning("Invalid action");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid action number: " + command);
        }
    }

    /**
     * Unloads all goods and units from the carrier currently selected.
     */
    private void unload() {
        Unit unit = getSelectedUnit();
        if (unit != null && unit.isCarrier()) {
            Iterator<Goods> goodsIterator = unit.getGoodsIterator();
            while (goodsIterator.hasNext()) {
                Goods goods = goodsIterator.next();
                getController().unloadCargo(goods);
            }
            Iterator<Unit> unitIterator = unit.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit newUnit = unitIterator.next();
                getController().leaveShip(newUnit);
            }
        }
        unloadButton.setEnabled(false);
        fillButton.setEnabled(false);
    }

    /**
     * Fill goods from the carrier currently selected until 100 units.
     */
    private void fill() {
        Unit unit = getSelectedUnit();
        if (unit != null && unit.isCarrier()) {
            Iterator<Goods> goodsIterator = unit.getGoodsIterator();
            while (goodsIterator.hasNext()) {
                Goods goods = goodsIterator.next();
                if (goods.getAmount() < 100 && colony.getGoodsCount(goods.getType()) > 0) {
                    int amount = Math.min(100 - goods.getAmount(), colony.getGoodsCount(goods.getType()));
                    getController().loadCargo(new Goods(goods.getGame(), colony, goods.getType(), amount), unit);
                }
            }
        }
    }

    /**
     * Closes the <code>ColonyPanel</code>.
     */
    public void closeColonyPanel() {
        if (getColony().getUnitCount() > 0 ||
            getCanvas().showConfirmDialog("abandonColony.text",
                                          "abandonColony.yes",
                                          "abandonColony.no")) {
            if (getColony().getUnitCount() <= 0) {
                getController().abandonColony(getColony());
            }

            getCanvas().remove(this);

            // remove property listeners
            if (colony != null) {
                colony.removePropertyChangeListener(this);
                colony.getTile().removePropertyChangeListener(Tile.UNIT_CHANGE, outsideColonyPanel);
                colony.getGoodsContainer().removePropertyChangeListener(warehousePanel);
            }
            if (getSelectedUnit() != null) {
                getSelectedUnit().removePropertyChangeListener(this);
            }
            buildingsPanel.removePropertyChangeListeners();
            tilePanel.removePropertyChangeListeners();

            if (getGame().getCurrentPlayer() == getMyPlayer()) {
                getController().nextModelMessage();
                Unit activeUnit = getCanvas().getGUI().getActiveUnit();
                if (activeUnit == null || activeUnit.getTile() == null || activeUnit.getMovesLeft() <= 0
                        || (!(activeUnit.getLocation() instanceof Tile) && !(activeUnit.isOnCarrier()))) {
                    getCanvas().getGUI().setActiveUnit(null);
                    getController().nextActiveUnit();
                }
            }
            getClient().getGUI().restartBlinking();
        }
    }

    /**
     * Selects a unit that is located somewhere on this panel.
     * 
     * @param unit The unit that is being selected.
     */
    public void setSelectedUnit(Unit unit) {
        Component[] components = inPortPanel.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof UnitLabel && ((UnitLabel) components[i]).getUnit() == unit) {
                setSelectedUnitLabel((UnitLabel) components[i]);
                break;
            }
        }
        updateCarrierButtons();
    }

    /**
     * Selects a unit that is located somewhere on this panel.
     * 
     * @param unitLabel The unit that is being selected.
     */
    public void setSelectedUnitLabel(UnitLabel unitLabel) {
        if (selectedUnit != unitLabel) {
            if (selectedUnit != null) {
                selectedUnit.setSelected(false);
                selectedUnit.getUnit().removePropertyChangeListener(this);
            }
            selectedUnit = unitLabel;
            if (unitLabel == null) {
                cargoPanel.setCarrier(null);
            } else {
                cargoPanel.setCarrier(unitLabel.getUnit());
                unitLabel.setSelected(true);
                unitLabel.getUnit().addPropertyChangeListener(this);
            }
        }
    }

    /**
     * Returns a pointer to the <code>CargoPanel</code>-object in use.
     * 
     * @return The <code>CargoPanel</code>.
     */
    public final CargoPanel getCargoPanel() {
        return cargoPanel;
    }

    /**
     * Returns a pointer to the <code>WarehousePanel</code>-object in use.
     * 
     * @return The <code>WarehousePanel</code>.
     */
    public final WarehousePanel getWarehousePanel() {
        return warehousePanel;
    }

    /**
     * Returns a pointer to the <code>TilePanel</code>-object in use.
     * 
     * @return The <code>TilePanel</code>.
     */
    public final TilePanel getTilePanel() {
        return tilePanel;
    }

    /**
     * Returns a pointer to the <code>Colony</code>-pointer in use.
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
    	if (this.colony != null){
            this.colony.removePropertyChangeListener(this);
    	}
        this.colony = colony;
        if (this.colony != null){
            this.colony.addPropertyChangeListener(this);
    	}
        editable = (colony.getOwner() == getMyPlayer());
    }

    /**
     * This panel shows the content of a carrier in the colony
     */
    public final class ColonyCargoPanel extends CargoPanel {

        public ColonyCargoPanel(Canvas canvas) {
            super(canvas, false);
        }

        @Override
        public String getUIClassID() {
            return "CargoPanelUI";
        }
    }

    /**
     * This panel is a list of the colony's buildings.
     */
    public final class BuildingsPanel extends JPanel {

        private final ColonyPanel colonyPanel;


        /**
         * Creates this BuildingsPanel.
         * 
         * @param colonyPanel The panel that holds this BuildingsPanel.
         */
        public BuildingsPanel(ColonyPanel colonyPanel) {
            setLayout(new GridLayout(0, 4));
            this.colonyPanel = colonyPanel;
        }

        @Override
        public String getUIClassID() {
            return "BuildingsPanelUI";
        }

        /**
         * Initializes the <code>BuildingsPanel</code> by loading/displaying
         * the buildings of the colony.
         */
        public void initialize() {
            removeAll();

            MouseAdapter mouseAdapter = new MouseAdapter() {
                    BuildQueuePanel queuePanel = new BuildQueuePanel(colony, getCanvas());
                    public void mousePressed(MouseEvent e) {
                        getCanvas().showSubPanel(queuePanel);
                    }
                };
            ASingleBuildingPanel aSingleBuildingPanel;

            List<Building> buildings = getColony().getBuildings();
            sortBuildings(buildings);
            for (Building building : buildings) {
                aSingleBuildingPanel = new ASingleBuildingPanel(building);
                if (colonyPanel.isEditable()) {
                    aSingleBuildingPanel.addMouseListener(releaseListener);
                    aSingleBuildingPanel.setTransferHandler(defaultTransferHandler);
                }
                aSingleBuildingPanel.setOpaque(false);
                aSingleBuildingPanel.addMouseListener(mouseAdapter);
                add(aSingleBuildingPanel);
            }
            add(new BuildingSitePanel(colony, getCanvas()));

        }

        public void removePropertyChangeListeners() {
            for (Component component : getComponents()) {
                if (component instanceof ASingleBuildingPanel) {
                    ((ASingleBuildingPanel) component).removePropertyChangeListeners();
                } else if (component instanceof BuildingSitePanel) {
                    ((BuildingSitePanel) component).removePropertyChangeListeners();
                }
            }
        }


        /**
         * This panel is a single line (one building) in the
         * <code>BuildingsPanel</code>.
         */
        public final class ASingleBuildingPanel extends BuildingPanel implements Autoscroll {

            /**
             * Creates this ASingleBuildingPanel.
             * 
             * @param building The building to display information from.
             */
            public ASingleBuildingPanel(Building building) {
                super(building, getCanvas());
            }

            public void initialize() {
                super.initialize();
                if (colonyPanel.isEditable()) {
                    for (UnitLabel unitLabel : getUnitLabels()) {
                        unitLabel.setTransferHandler(defaultTransferHandler);
                        unitLabel.addMouseListener(pressListener);
                    }
                }
            }

            public void autoscroll(Point p) {
                JViewport vp = (JViewport) colonyPanel.buildingsPanel.getParent();
                if (getLocation().y + p.y - vp.getViewPosition().y < SCROLL_AREA_HEIGHT) {
                    vp.setViewPosition(new Point(vp.getViewPosition().x, Math.max(
                                                                                  vp.getViewPosition().y - SCROLL_SPEED, 0)));
                } else if (getLocation().y + p.y - vp.getViewPosition().y >= vp.getHeight() - SCROLL_AREA_HEIGHT) {
                    vp.setViewPosition(new Point(vp.getViewPosition().x, Math.min(
                                                                                  vp.getViewPosition().y + SCROLL_SPEED, colonyPanel.buildingsPanel.getHeight()
                                                                                  - vp.getHeight())));
                }
            }

            public Insets getAutoscrollInsets() {
                Rectangle r = getBounds();
                return new Insets(r.x, r.y, r.width, r.height);
            }


            /**
             * Adds a component to this ASingleBuildingPanel and makes sure that
             * the unit that the component represents gets modified so that it
             * will be located in the colony.
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
                Component c;
                Container oldParent = comp.getParent();

                if (editState) {
                    if (comp instanceof UnitLabel) {
                        Unit unit = ((UnitLabel) comp).getUnit();

                        if (getBuilding().canAdd(unit)) {
                            oldParent.remove(comp);
                            getController().work(unit, getBuilding());
                        } else {
                            return null;
                        }
                    } else {
                        logger.warning("An invalid component got dropped on this BuildingsPanel.");
                        return null;
                    }
                }
                initialize();
                return null;
            }

        }
    }

    /**
     * A panel that holds UnitsLabels that represent Units that are standing in
     * front of a colony.
     */
    public final class OutsideColonyPanel extends JPanel implements PropertyChangeListener {

        private Colony colony;

        public void setColony(Colony newColony) {
            if (colony != null) {
                colony.getTile().removePropertyChangeListener(Tile.UNIT_CHANGE, this);
            }
            this.colony = newColony;
            if (colony != null) {
                colony.getTile().addPropertyChangeListener(Tile.UNIT_CHANGE, this);
            }
            initialize();
        }

        public void initialize() {
            
            removeAll();
            if (colony == null) {
                return;
            }

            Tile colonyTile = colony.getTile();
            for (Unit unit : colonyTile.getUnitList()) {

                UnitLabel unitLabel = new UnitLabel(unit, getCanvas());
                if (isEditable()) {
                    unitLabel.setTransferHandler(defaultTransferHandler);
                    unitLabel.addMouseListener(pressListener);
                }

                if (!unit.isCarrier()) {
                    add(unitLabel, false);
                }
            }
            revalidate();
            repaint();
        }

        public Colony getColony() {
            return colony;
        }

        @Override
        public String getUIClassID() {
            return "OutsideColonyPanelUI";
        }

        /**
         * Adds a component to this OutsideColonyPanel and makes sure that the
         * unit that the component represents gets modified so that it will be
         * located in the colony.
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

        public void propertyChange(PropertyChangeEvent event) {
            initialize();
        }

    }

    /**
     * A panel that holds UnitsLabels that represent naval Units that are
     * waiting in the port of the colony.
     */
    public final class InPortPanel extends JPanel {

        @Override
        public String getUIClassID() {
            return "InPortPanelUI";
        }
    }

    /**
     * A panel that holds goods that represent cargo that is inside the Colony.
     */
    public final class WarehousePanel extends JPanel implements PropertyChangeListener {

        private final ColonyPanel colonyPanel;

        /**
         * Creates this WarehousePanel.
         * 
         * @param colonyPanel The panel that holds this WarehousePanel.
         */
        public WarehousePanel(ColonyPanel colonyPanel) {
            this.colonyPanel = colonyPanel;
        }

        public void initialize() {
            // get notified of warehouse changes
            colony.getGoodsContainer().addPropertyChangeListener(this);
            update();
            revalidate();
            repaint();
        }

        private void update() {
            removeAll();
            for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
                if (goodsType.isStorable()) {
                    Goods goods = colony.getGoodsContainer().getGoods(goodsType);
                    GoodsLabel goodsLabel = new GoodsLabel(goods, getCanvas());
                    if (colonyPanel.isEditable()) {
                        goodsLabel.setTransferHandler(defaultTransferHandler);
                        goodsLabel.addMouseListener(pressListener);
                    }
                    add(goodsLabel, false);
                }
            }
        }

        @Override
        public String getUIClassID() {
            return "WarehousePanelUI";
        }


        /**
         * Adds a component to this WarehousePanel and makes sure that the unit or
         * good that the component represents gets modified so that it is on
         * board the currently selected ship.
         * 
         * @param comp The component to add to this WarehousePanel.
         * @param editState Must be set to 'true' if the state of the component
         *            that is added (which should be a dropped component
         *            representing a Unit or good) should be changed so that the
         *            underlying unit or goods are on board the currently
         *            selected ship.
         * @return The component argument.
         */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (comp instanceof GoodsLabel) {
                    comp.getParent().remove(comp);
                    ((GoodsLabel) comp).setSmall(false);
                    return comp;
                }
                logger.warning("An invalid component got dropped on this WarehousePanel.");
                return null;
            }

            Component c = add(comp);

            return c;
        }

        public void propertyChange(PropertyChangeEvent event) {
            update();
        }

    }


    /**
     * A panel that displays the tiles in the immediate area around the colony.
     */
    public final class TilePanel extends FreeColPanel {

        private final ColonyPanel colonyPanel;

        /**
         * Creates this TilePanel.
         * 
         * @param colonyPanel The panel that holds this TilePanel.
         */
        public TilePanel(ColonyPanel colonyPanel) {
            super(colonyPanel.getCanvas());
            this.colonyPanel = colonyPanel;
            setBackground(Color.BLACK);
            setBorder(null);
            setLayout(null);
        }

        public void initialize() {
            int layer = 2;

            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    ColonyTile tile = getColony().getColonyTile(x, y);
                    if (tile==null)
                        continue;
                    ASingleTilePanel p = new ASingleTilePanel(tile, x, y);
                    add(p, new Integer(layer));
                    layer++;
                }
            }
        }


        public void removePropertyChangeListeners() {
            for (Component component : getComponents()) {
                if (component instanceof ASingleTilePanel) {
                    ((ASingleTilePanel) component).removePropertyChangeListeners();
                }
            }
        }

        @Override
        public void paintComponent(Graphics g) {
            GUI colonyTileGUI = getCanvas().getColonyTileGUI();
            Game game = colony.getGame();

            g.setColor(Color.black);
            g.fillRect(0, 0, getWidth(), getHeight());

            if (getColony() != null) {
                for (int x = 0; x < 3; x++) {
                    for (int y = 0; y < 3; y++) {
                        TileType tileType = getColony().getTile().getType();
                        Tile tile = getColony().getTile(x, y);
                        if (tile==null)
                            continue;
                        colonyTileGUI.displayColonyTile((Graphics2D) g, game.getMap(), tile, ((2 - x) + y)
                                                        * getLibrary().getTerrainImageWidth(tileType) / 2,
                                                        (x + y) * getLibrary().getTerrainImageHeight(tileType) / 2,
                                getColony());

                    }
                }
            }
        }


        /**
         * Panel for visualizing a <code>ColonyTile</code>. The component
         * itself is not visible, however the content of the component is (i.e.
         * the people working and the production)
         */
        public final class ASingleTilePanel extends JPanel implements PropertyChangeListener {

            private ColonyTile colonyTile;

            public ASingleTilePanel(ColonyTile colonyTile, int x, int y) {
                setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
                this.colonyTile = colonyTile;

                colonyTile.addPropertyChangeListener(this);

                setOpaque(false);
                TileType tileType = colonyTile.getTile().getType();
                // Size and position:
                setSize(getLibrary().getTerrainImageWidth(tileType), getLibrary().getTerrainImageHeight(tileType));
                setLocation(((2 - x) + y) * getLibrary().getTerrainImageWidth(tileType) / 2,
                            (x + y) * getLibrary().getTerrainImageHeight(tileType) / 2);
                initialize();
            }
                
            private void initialize() {

                removeAll();
                UnitLabel unitLabel = null;
                if (colonyTile.getUnit() != null) {
                    Unit unit = colonyTile.getUnit();
                    unitLabel = new UnitLabel(unit, getCanvas());
                    if (colonyPanel.isEditable()) {
                        unitLabel.setTransferHandler(defaultTransferHandler);
                        unitLabel.addMouseListener(pressListener);
                    }
                    super.add(unitLabel);

                }
                updateDescriptionLabel(unitLabel, true);

                if (colonyTile.isColonyCenterTile()) {
                    initializeAsCenterTile();
                }

                if (colonyPanel.isEditable()) {
                    setTransferHandler(defaultTransferHandler);
                    addMouseListener(releaseListener);
                }

                revalidate();
                repaint();
            }

            /**
             * Initialized the center of the colony panel tile. The one
             * containing the city.
             * 
             * @param lib an ImageGetLibrary()
             */
            private void initializeAsCenterTile() {

                setLayout(new GridLayout(2, 1));

                TileType tileType = colonyTile.getTile().getType();
                // A colony always produces food.
                GoodsType primaryGood = colonyTile.getTile().primaryGoods();
                ImageIcon goodsIcon = getLibrary().getGoodsImageIcon(primaryGood);
                ProductionLabel pl = new ProductionLabel(primaryGood, colonyTile.getProductionOf(primaryGood), getCanvas());
                pl.setSize(getLibrary().getTerrainImageWidth(tileType), goodsIcon.getIconHeight());
                add(pl);

                // A colony may produce one additional good
                GoodsType secondaryGood = colonyTile.getTile().secondaryGoods();
                if (colonyTile.getProductionOf(secondaryGood) != 0) {
                    goodsIcon = getLibrary().getGoodsImageIcon(secondaryGood);
                    ProductionLabel sl = new ProductionLabel(secondaryGood, colonyTile.getProductionOf(secondaryGood), getCanvas());
                    sl.setSize(getLibrary().getTerrainImageWidth(tileType), goodsIcon.getIconHeight());
                    add(sl);
                }
            }

            public void removePropertyChangeListeners() {
                colonyTile.removePropertyChangeListener(this);
            }

            /**
             * Updates the description label The description label is a tooltip
             * with the terrain type, road and plow indicator if any
             * 
             * If a unit is on it update the tooltip of it instead
             */
            private void updateDescriptionLabel() {
                updateDescriptionLabel(null, false);
            }

            /**
             * Updates the description label The description label is a tooltip
             * with the terrain type, road and plow indicator if any
             * 
             * If a unit is on it update the tooltip of it instead
             */
            private void updateDescriptionLabel(UnitLabel unit, boolean toAdd) {
                String tileDescription = this.colonyTile.getLabel();

                if (unit == null) {
                    setToolTipText(tileDescription);
                } else {
                    String unitDescription = unit.getUnit().getName();
                    if (toAdd) {
                        unitDescription = tileDescription + " [" + unitDescription + "]";
                    }
                    unit.setDescriptionLabel(unitDescription);
                }
            }

            /**
             * Adds a component to this CargoPanel and makes sure that the unit
             * or good that the component represents gets modified so that it is
             * on board the currently selected ship.
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
                        Unit unit = ((UnitLabel) comp).getUnit();
                        Tile tile = colonyTile.getWorkTile();
                        Player player = unit.getOwner();

                        logger.warning("Colony " + colony.getName()
                                       + " claims tile " + tile.toString()
                                       + " with unit " + unit.getId());
                        if ((tile.getOwner() != player
                             || tile.getOwningSettlement() != colony)
                            && !getController().claimLand(tile, colony, 0)) {
                            logger.warning("Colony " + colony.getName()
                                           + " could not claim tile " + tile.toString()
                                           + " with unit " + unit.getId());
                            return null;
                        }

                        if (colonyTile.canAdd(unit)) { 
                            oldParent.remove(comp);

                            getController().work(unit, colonyTile);

                            // check whether worktype is suitable
                            GoodsType workType = colonyTile.getWorkType(unit);
                            if (workType != unit.getWorkType()) {
                                getController().changeWorkType(unit, workType);
                            }

                            //updateDescriptionLabel((UnitLabel) comp, true);

                            ((UnitLabel) comp).setSmall(false);

                            //colonyPanel.updateSoLLabel();
                        } else {
                            // could not add the unit on the tile
                            Canvas canvas = getCanvas();
                            Tile workTile = colonyTile.getWorkTile();
                            Settlement s = workTile.getOwningSettlement();

                            if (s != null && s != getColony()) {
                                if (s.getOwner() == player) {
                                    // Its one of ours
                                    canvas.errorMessage("tileTakenSelf");
                                } else if (s.getOwner().isEuropean()) {
                                    // occupied by a foreign european colony
                                    canvas.errorMessage("tileTakenEuro");
                                } else if (s instanceof IndianSettlement) {
                                    // occupied by an indian settlement
                                    canvas.errorMessage("tileTakenInd");
                                }
                            } else {
                                if (!workTile.isLand()) { // no docks
                                    canvas.errorMessage("tileNeedsDocks");
                                } else if (workTile.hasLostCityRumour()) {
                                    canvas.errorMessage("tileHasRumour");
                                }
                            }
                            return null;
                        }
                    } else {
                        logger.warning("An invalid component got dropped on this CargoPanel.");
                        return null;
                    }
                }

                /*
                 * At this point, the panel has already been updated
                 * via the property change listener.
                 *
                 removeAll();
                 Component c = super.add(comp);
                 refresh();
                */
                return comp;
            }
    
            public void propertyChange(PropertyChangeEvent event) {
                initialize();
            }

            /**
             * Checks if this <code>JComponent</code> contains the given
             * coordinate.
             */
            @Override
            public boolean contains(int px, int py) {
                /**
                 * We are assuming the tile size is 128x64.
                 * 
                 * How this nasty piece of code works:
                 * 
                 * We have a rectangle of 128x64. Inside of it is a diamond
                 * (rectangle on its side) whose corners are in the middle of
                 * the rectangle edges.
                 * 
                 * We have to figure out if the (x,y) coords are within this
                 * diamond.
                 * 
                 * I do this by using the y axis as a reference point. If you
                 * look at this diamond, it is widest when y=32, and smallest
                 * when y=0 && y=63.
                 * 
                 * So we'return basically saying, when y=32, active x is 100% of
                 * 128. When y=10 then active x = 31.25% of 128. 31.25% of
                 * 128pixels is 40 pixels, situated in the middle of 128. The
                 * middle 40 pixels of 128 is 63-20 and 63+20
                 * 
                 * Tada. A way of detecting if the x,y is within the diamond.
                 * This algorithm should work no matter how tall or short the
                 * rectangle (and therefore the diamond within) is.
                 */

                int activePixels;

                // Check if the value is in the rectangle at all.
                if (!super.contains(px, py)) {
                    return false;
                }

                if (py >= 32) {
                    py = 32 - (py - 31);
                }

                // Determine active amount of pixels
                activePixels = (py * 128) / 64; // 64 --> /32 /2
                // Now determine if x is in the diamond.
                return ((px >= 63 - activePixels) && (px <= 63 + activePixels));
            }
        }
    }
    
    public void propertyChange(PropertyChangeEvent e){        

        if (!isShowing() || getColony() == null) {
            return;
    	}
        String property = e.getPropertyName();

        if (Unit.CARGO_CHANGE.equals(property)) {
            updateCarrierButtons();
        } else if (ColonyChangeEvent.POPULATION_CHANGE.toString().equals(property)) {
            updateSoLLabel();
        } else if (ColonyChangeEvent.BONUS_CHANGE.toString().equals(property)) {
            ModelMessage msg = getColony().checkForGovMgtChangeMessage();
            if (msg != null){
                getCanvas().showInformationMessage(msg.getId(), msg.getDisplay(), msg.getData());
            }
            updateSoLLabel();
        } else if (property.startsWith("model.goods.")) {
            updateProductionPanel();
        } else {
            logger.warning("Unknown property change event: " + e.getPropertyName());
        }
    }

}
