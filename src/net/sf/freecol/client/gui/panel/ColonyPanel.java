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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.Autoscroll;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ComponentInputMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import cz.autel.dmi.HIGLayout;

/**
 * This is a panel for the Colony display. It shows the units that are working
 * in the colony, the buildings and much more.
 */
public final class ColonyPanel extends FreeColPanel implements ActionListener, ContainerListener {


    private static Logger logger = Logger.getLogger(ColonyPanel.class.getName());

    /**
     * The height of the area in which autoscrolling should happen.
     */
    public static final int SCROLL_AREA_HEIGHT = 40;

    /**
     * The speed of the scrolling.
     */
    public static final int SCROLL_SPEED = 40;

    private static final int EXIT = 0, BUY_BUILDING = 1, UNLOAD = 2, RENAME = 3, WAREHOUSE = 4, FILL = 5;

    /**
     * The additional border required to make layout fit.
     */
    private static final int BORDER_CORRECT = 4;

    private final Canvas parent;

    private final FreeColClient freeColClient;

    private InGameController inGameController;

    private final JLabel solLabel;

    private final FreeColProgressBar hammersLabel;

    private final FreeColProgressBar toolsLabel;

    private final JPanel productionPanel;

    private final JComboBox buildingBox;

    private final ActionListener buildingBoxListener;

    private final JComboBox nameBox;

    private final OutsideColonyPanel outsideColonyPanel;

    private final InPortPanel inPortPanel;

    private final ColonyCargoPanel cargoPanel;

    private final TitledBorder cargoBorder;

    private final WarehousePanel warehousePanel;

    private final TilePanel tilePanel;

    private final BuildingsPanel buildingsPanel;

    private final DefaultTransferHandler defaultTransferHandler;

    private final MouseListener pressListener;

    private final MouseListener releaseListener;

    private Colony colony;

    private Game game;

    private UnitLabel selectedUnit;

    private JButton exitButton = new JButton(Messages.message("close"));

    private JButton buyBuilding = new JButton(Messages.message("buyBuilding"));

    private JButton unloadButton = new JButton(Messages.message("unload"));

    private JButton fillButton = new JButton(Messages.message("fill"));

    private JButton renameButton = new JButton(Messages.message("rename"));

    private JButton warehouseButton = new JButton(Messages.message("warehouseDialog.name"));


    /**
     * The constructor for the panel.
     * 
     * @param parent The parent of this panel
     * @param freeColClient The main controller object for the client.
     */
    public ColonyPanel(Canvas parent, FreeColClient freeColClient) {
        this.parent = parent;
        this.freeColClient = freeColClient;
        this.inGameController = freeColClient.getInGameController();

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
        fillInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0, false), "pressed");
        fillInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0, true), "released");
        SwingUtilities.replaceUIInputMap(fillButton, JComponent.WHEN_IN_FOCUSED_WINDOW, fillInputMap);

        productionPanel = new JPanel();
        productionPanel.setOpaque(false);
        outsideColonyPanel = new OutsideColonyPanel(this);
        inPortPanel = new InPortPanel();
        warehousePanel = new WarehousePanel(this);
        tilePanel = new TilePanel(this);
        buildingsPanel = new BuildingsPanel(this);
        cargoPanel = new ColonyCargoPanel(parent);
        cargoPanel.addContainerListener(this);

        defaultTransferHandler = new DefaultTransferHandler(parent, this);
        pressListener = new DragListener(this);
        releaseListener = new DropListener();

        outsideColonyPanel.setLayout(new GridLayout(0, 2));
        inPortPanel.setLayout(new GridLayout(0, 2));
        cargoPanel.setLayout(new GridLayout(1, 0));
        warehousePanel.setLayout(new GridLayout(2, 8));

        solLabel = new JLabel(Messages.message("sonsOfLiberty") + ": 0%, " + Messages.message("tory") + ": 100%");
        hammersLabel = new FreeColProgressBar(parent, Goods.HAMMERS);
        toolsLabel = new FreeColProgressBar(parent, Goods.TOOLS);

        buildingBox = new JComboBox();

        buildingBoxListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    getClient().getInGameController()
                        .setCurrentlyBuilding(getColony(), ((BuildableType) buildingBox.getSelectedItem()));
                    updateProgressLabel();
                }};

        buildingBox.setRenderer(new ListCellRenderer() {
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                              boolean cellHasFocus) {
                    BuildableType type = (BuildableType) value;
                    String requirements = type.getGoodsRequiredAsString();
                    if ("".equals(requirements)) {
                        return new JLabel(type.getName());
                    } else {
                        return new JLabel(new String(type.getName() + " (" + requirements + ")"));
                    }
                }
            });

        JScrollPane outsideColonyScroll = new JScrollPane(outsideColonyPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollPane inPortScroll = new JScrollPane(inPortPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollPane cargoScroll = new JScrollPane(cargoPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane warehouseScroll = new JScrollPane(warehousePanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane tilesScroll = new JScrollPane(tilePanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane buildingsScroll = new JScrollPane(buildingsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Make the colony label
        nameBox = new JComboBox();
        nameBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                initialize((Colony) nameBox.getSelectedItem(), getGame());
            }
        });
        nameBox.setFont(smallHeaderFont);

        buildingsScroll.setAutoscrolls(true);

        /** Borders */
        tilesScroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEmptyBorder(), Messages.message("surroundingArea")), new BevelBorder(BevelBorder.LOWERED)));
        buildingsScroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEmptyBorder(), Messages.message("buildings")), BorderFactory.createEtchedBorder()));
        warehouseScroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEmptyBorder(), Messages.message("goods")), BorderFactory.createEtchedBorder()));
        cargoBorder = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), Messages
                .message("cargoOnCarrier"));
        cargoScroll.setBorder(BorderFactory.createCompoundBorder(cargoBorder, BorderFactory.createEtchedBorder()));
        inPortScroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEmptyBorder(), Messages.message("inPort")), BorderFactory.createEtchedBorder()));
        outsideColonyScroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEmptyBorder(), Messages.message("outsideColony")), BorderFactory.createEtchedBorder()));

        // manual border corrections
        Border correctBorder = BorderFactory.createEmptyBorder(0, BORDER_CORRECT, 0, BORDER_CORRECT);
        productionPanel.setBorder(correctBorder);
        solLabel.setBorder(correctBorder);
        unloadButton.setBorder(BorderFactory.createCompoundBorder(correctBorder, unloadButton.getBorder()));
        fillButton.setBorder(BorderFactory.createCompoundBorder(correctBorder, fillButton.getBorder()));
        exitButton.setBorder(BorderFactory.createCompoundBorder(correctBorder, exitButton.getBorder()));
        warehouseButton.setBorder(BorderFactory.createCompoundBorder(correctBorder, warehouseButton.getBorder()));
        renameButton.setBorder(BorderFactory.createCompoundBorder(correctBorder, renameButton.getBorder()));
        buyBuilding.setBorder(BorderFactory.createCompoundBorder(correctBorder, buyBuilding.getBorder()));
        hammersLabel.setBorder(BorderFactory.createCompoundBorder(correctBorder, hammersLabel.getBorder()));
        toolsLabel.setBorder(BorderFactory.createCompoundBorder(correctBorder, toolsLabel.getBorder()));
        buildingBox.setBorder(BorderFactory.createCompoundBorder(correctBorder, buildingBox.getBorder()));

        buyBuilding.setActionCommand(String.valueOf(BUY_BUILDING));
        exitButton.setActionCommand(String.valueOf(EXIT));
        unloadButton.setActionCommand(String.valueOf(UNLOAD));
        fillButton.setActionCommand(String.valueOf(FILL));
        renameButton.setActionCommand(String.valueOf(RENAME));
        warehouseButton.setActionCommand(String.valueOf(WAREHOUSE));
        
        enterPressesWhenFocused(buyBuilding);
        enterPressesWhenFocused(exitButton);
        enterPressesWhenFocused(unloadButton);
        enterPressesWhenFocused(fillButton);
        enterPressesWhenFocused(renameButton);
        enterPressesWhenFocused(warehouseButton);

        buyBuilding.addActionListener(this);
        exitButton.addActionListener(this);
        unloadButton.addActionListener(this);
        fillButton.addActionListener(this);
        renameButton.addActionListener(this);
        warehouseButton.addActionListener(this);

        selectedUnit = null;

        // See the message of Ulf Onnen for more information about the presence
        // of this fake mouse listener.
        addMouseListener(new MouseAdapter() {
        });

        int[] widths = { 125, 125, margin, 138, margin, 215, margin, 204 };
        int[] heights = { 0, // colony select box
                margin, 225, // colony tiles and buildings
                margin, 28, // hammers label, same size as tools label
                margin, 28, // tools label, same size as hammers label
                margin, 120, // port and cargo panels
                margin, 140, // warehouse
                margin, 0 // buttons
        };
        setLayout(new HIGLayout(widths, heights));

        int row = 1;
        add(nameBox, higConst.rcwh(row, 1, 8, 1, ""));
        row += 2;
        add(tilesScroll, higConst.rcwh(row, 1, 4, 1));
        add(buildingsScroll, higConst.rcwh(row, 6, 3, 1));
        row += 2;
        add(productionPanel, higConst.rcwh(row, 1, 4, 1, "tl"));
        add(buildingBox, higConst.rc(row, 6));
        add(hammersLabel, higConst.rc(row, 8));
        row += 2;
        add(solLabel, higConst.rcwh(row, 1, 4, 1));
        add(buyBuilding, higConst.rc(row, 6));
        add(toolsLabel, higConst.rc(row, 8));
        row += 2;
        add(inPortScroll, higConst.rcwh(row, 1, 2, 1));
        add(cargoScroll, higConst.rcwh(row, 4, 3, 1));
        add(outsideColonyScroll, higConst.rcwh(row, 8, 1, 3));
        row += 2;
        add(warehouseScroll, higConst.rcwh(row, 1, 6, 1));
        row += 2;
        add(unloadButton, higConst.rc(row, 1, "l"));
        add(fillButton, higConst.rc(row, 2, "l"));
        add(warehouseButton, higConst.rc(row, 4, "l"));
        add(renameButton, higConst.rc(row, 6, "r"));
        add(exitButton, higConst.rc(row, 8, "r"));

        setSize(getPreferredSize());

    }

    /**
     * Get the parent canvas, added for inner classes.
     * 
     * @return parent canvas.
     */
    private Canvas getParentCanvas() {
        return this.parent;
    }

    @Override
    public void requestFocus() {
        exitButton.requestFocus();
    }

    /**
     * Refreshes this panel.
     */
    public void refresh() {
        repaint(0, 0, getWidth(), getHeight());
    }

    /**
     * Initialize the data on the window. This is the same as calling:
     * <code>initialize(colony, game, null)</code>.
     * 
     * @param colony The <code>Colony</code> to be displayed.
     * @param game The <code>Game</code> in which the given
     *            <code>Colony</code> is a part of.
     */
    public void initialize(Colony colony, Game game) {
        initialize(colony, game, null);
    }

    /**
     * Initialize the data on the window.
     * 
     * @param colony The <code>Colony</code> to be displayed.
     * @param game The <code>Game</code> in which the given
     *            <code>Colony</code> is a part of.
     * @param preSelectedUnit This <code>Unit</code> will be selected if it is
     *            not <code>null</code> and it is a carrier located in the
     *            given <code>Colony</code>.
     */
    public void initialize(final Colony colony, Game game, Unit preSelectedUnit) {
        setColony(colony);
        this.game = game;

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
        buyBuilding.setEnabled(isEditable());
        unloadButton.setEnabled(isEditable());
        fillButton.setEnabled(isEditable());
        renameButton.setEnabled(isEditable());
        warehouseButton.setEnabled(isEditable());
        buildingBox.setEnabled(isEditable());
        nameBox.setEnabled(isEditable());

        //
        // Remove the old components from the panels.
        //

        cargoPanel.removeAll();
        warehousePanel.removeAll();
        outsideColonyPanel.removeAll();
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

            UnitLabel unitLabel = new UnitLabel(unit, parent);
            if (isEditable()) {
                unitLabel.setTransferHandler(defaultTransferHandler);
            }
            if (isEditable() || unit.isCarrier()) {
                unitLabel.addMouseListener(pressListener);
            }

            if (!unit.isCarrier()) {
                outsideColonyPanel.add(unitLabel, false);
            } else {
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

        updateBuildingBox();
        updateNameBox();
        updateProductionPanel();
        updateSoLLabel();
        updateProgressLabel();

    }

    public void reinitialize() {
        if (selectedUnit != null) {
            initialize(getColony(), game, selectedUnit.getUnit());
        } else {
            initialize(getColony(), game, null);
        }
    }

    /**
     * Enables the unload and fill buttons if the currently selected unit is a
     * carrier with some cargo.
     */
    private void updateCarrierButtons() {
        unloadButton.setEnabled(false);
        fillButton.setEnabled(false);
        if (!isEditable())
            return;
        if (selectedUnit != null) {
            Unit unit = selectedUnit.getUnit();
            if (unit != null && unit.isCarrier() && unit.getSpaceLeft() < unit.getInitialSpaceLeft()) {
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
     * Updates the label that is placed above the cargo panel. It shows the name
     * of the unit whose cargo is displayed and the amount of space left on that
     * unit.
     */
    private void updateCargoLabel() {
        if (selectedUnit != null) {
            cargoPanel.getParent().setEnabled(true);
            cargoBorder.setTitle(Messages.message("cargoOnCarrierLong", 
                    "%name%", selectedUnit.getUnit().getName(),
                    "%space%", String.valueOf(selectedUnit.getUnit().getSpaceLeft())));
        } else {
            cargoPanel.getParent().setEnabled(false);
            cargoBorder.setTitle(Messages.message("cargoOnCarrier"));
        }
    }

    /**
     * Updates the SoL membership label.
     */
    private void updateSoLLabel() {
        if (getColony() == null) {
            // Apparently this can happen
            return;
        }
        solLabel.setText(Messages.message("sonsOfLiberty") + ": " + getColony().getSoL() + "% ("
                + getColony().getMembers() + "), " + Messages.message("tory") + ": " + getColony().getTory() + "% ("
                + (getColony().getUnitCount() - getColony().getMembers()) + ")");
    }

    public void updateNameBox() {
        // Remove all action listeners, so the update has no effect (except
        // updating the list).
        ActionListener[] listeners = nameBox.getActionListeners();
        for (ActionListener al : listeners)
            nameBox.removeActionListener(al);

        if (getColony() == null) {
            // Apparently this can happen
            return;
        }
        nameBox.removeAllItems();
        List<Settlement> settlements = getColony().getOwner().getSettlements();
        sortColonies(settlements);
        Iterator<Settlement> settlementIterator = settlements.iterator();
        while (settlementIterator.hasNext()) {
            nameBox.addItem(settlementIterator.next());
        }
        nameBox.setSelectedItem(getColony());

        for (ActionListener al : listeners) {
            nameBox.addActionListener(al);
        }
    }

    @SuppressWarnings("unchecked")
    private void sortColonies(List colonies) {
        Collections.sort(colonies, freeColClient.getClientOptions().getColonyComparator());
    }

    public void updateBuildingBox() {
        buildingBox.removeActionListener(buildingBoxListener);
        buildingBox.removeAllItems();
        buildingBox.addItem(BuildableType.NOTHING);

        BuildableType currentlyBuilding = getColony().getCurrentlyBuilding();
        List<BuildableType> buildableTypes = new ArrayList<BuildableType>(FreeCol.getSpecification().getBuildingTypeList());
        buildableTypes.addAll(FreeCol.getSpecification().getUnitTypeList());
        for (BuildableType buildableType : buildableTypes) {
            if (getColony().canBuild(buildableType)) {
                buildingBox.addItem(buildableType);
                if (buildableType == currentlyBuilding) {
                    buildingBox.setSelectedIndex(buildingBox.getItemCount() - 1);
                }
            }
        }
        buildingBox.addActionListener(buildingBoxListener);
        updateProgressLabel();
    }

    public void updateWarehouse() {
        warehousePanel.initialize();
    }

    public void updateBuildingsPanel() {
        buildingsPanel.initialize();
    }

    /**
     * Updates the building progress label.
     */
    private void updateProgressLabel() {
        if (getColony() == null) {
            // Apparently this can happen
            return;
        }
        if (getColony().getCurrentlyBuilding() == BuildableType.NOTHING) {
            hammersLabel.update(0, 0, 0, 0);
            toolsLabel.update(0, 0, 0, 0);
            buyBuilding.setEnabled(false);
        } else {
            final int hammers = getColony().getHammers();
            final int tools = getColony().getGoodsCount(Goods.TOOLS);
            final int nextHammers = getColony().getBuildingForProducing(Goods.HAMMERS).getProductionNextTurn();
            int nextTools = getColony().getBuildingForProducing(Goods.TOOLS).getProductionNextTurn();

            // subtract the tools used for production
            GoodsType produced = Goods.TOOLS.getProducedMaterial();
            if (produced != null) {
                nextTools -= getColony().getProductionNextTurn(produced);
            }
            int hammersNeeded = 0, toolsNeeded = 0;
            /** TODO: make this more generic */
            for (AbstractGoods requiredGoods : getColony().getCurrentlyBuilding().getGoodsRequired()) {
                if (requiredGoods.getType() == Goods.HAMMERS) {
                    hammersNeeded = Math.max(requiredGoods.getAmount(), 0);
                } else if (requiredGoods.getType() == Goods.TOOLS) {
                    toolsNeeded = Math.max(requiredGoods.getAmount(), 0);
                }
            }
            hammersLabel.update(0, hammersNeeded, hammers, nextHammers);
            toolsLabel.update(0, toolsNeeded, tools, nextTools);

            // The buy button should only be active if:
            //    - the panel is active,
            //    - the building isn't finished,
            //    - the player has enough money
            int price = getColony().getPriceForBuilding();
            buyBuilding.setEnabled(isEditable() && price > 0 &&
                                   price <= freeColClient.getMyPlayer().getGold());
        }
    }

    private void updateOutsideColonyPanel() {
        if (getColony() == null) {
            // Apparently this can happen
            return;
        }
        outsideColonyPanel.removeAll();
        Tile tile = getColony().getTile();
        Iterator<Unit> tileUnitIterator = tile.getUnitIterator();
        while (tileUnitIterator.hasNext()) {
            Unit unit = tileUnitIterator.next();

            UnitLabel unitLabel = new UnitLabel(unit, parent);
            if (isEditable()) {
                unitLabel.setTransferHandler(defaultTransferHandler);
                unitLabel.addMouseListener(pressListener);
            }

            if (!unit.isCarrier()) {
                outsideColonyPanel.add(unitLabel, false);
            }
        }
    }

    public void updateProductionPanel() {
        productionPanel.removeAll();

        final int foodProduction = colony.getFoodProduction();
        final int foodConsumption = colony.getFoodConsumption();
        final int horses = colony.getProductionOf(Goods.HORSES);
        final int bells = colony.getProductionOf(Goods.BELLS);
        final int crosses = colony.getProductionOf(Goods.CROSSES);

        // The food that is used. If not enough food is produced, the
        // complete production.  Horses consume 1 food each, so they
        // need to be added to the used food.
            
        int usedFood = (foodConsumption < foodProduction ? foodConsumption : foodProduction) + horses;
        productionPanel.add(new ProductionLabel(Goods.FOOD, usedFood, parent));

        int surplusFood = foodProduction - foodConsumption - horses;
        ProductionLabel surplusLabel = new ProductionLabel(Goods.FOOD, surplusFood, parent);
        surplusLabel.setDrawPlus(true);
        productionPanel.add(surplusLabel);

        ProductionLabel horseLabel = new ProductionLabel(Goods.HORSES, horses, parent);
        horseLabel.setMaxGoodsIcons(1);
        productionPanel.add(horseLabel);

        productionPanel.add(new ProductionLabel(Goods.BELLS, bells, parent));
        productionPanel.add(new ProductionLabel(Goods.CROSSES, crosses, parent));

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
            case BUY_BUILDING:
                freeColClient.getInGameController().payForBuilding(getColony());
                reinitialize();
                freeColClient.getCanvas().updateGoldLabel();
                requestFocus();
                break;
            case EXIT:
                closeColonyPanel();
                break;
            case UNLOAD:
                unload();
                break;
            case RENAME:
                freeColClient.getInGameController().rename(getColony());
                updateNameBox();
                break;
            case WAREHOUSE:
                if (freeColClient.getCanvas().showWarehouseDialog(colony)) {
                    updateWarehouse();
                }
                break;
            case FILL:
                fill();
                break;
            default:
                logger.warning("Invalid action");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid action number");
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
                inGameController.unloadCargo(goods);
                updateWarehouse();
                updateCargoPanel();
                getCargoPanel().revalidate();
                refresh();
            }
            Iterator<Unit> unitIterator = unit.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit newUnit = unitIterator.next();
                inGameController.leaveShip(newUnit);
                updateCargoPanel();
                updateOutsideColonyPanel();
                outsideColonyPanel.revalidate();
                getCargoPanel().revalidate();
                refresh();
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
                    inGameController.loadCargo(new Goods(goods.getGame(), colony, goods.getType(), amount), unit);
                    updateWarehouse();
                    updateCargoPanel();
                    getCargoPanel().revalidate();
                    refresh();
                }
            }
        }
    }

    /**
     * Closes the <code>ColonyPanel</code>.
     */
    public void closeColonyPanel() {
        if (getColony().getUnitCount() > 0
                || freeColClient.getCanvas().showConfirmDialog("abandonColony.text", "abandonColony.yes",
                        "abandonColony.no")) {
            if (getColony().getUnitCount() <= 0) {
                freeColClient.getInGameController().abandonColony(getColony());
            }

            parent.remove(this);
            if (freeColClient.getGame().getCurrentPlayer() == freeColClient.getMyPlayer()) {
                freeColClient.getInGameController().nextModelMessage();
                Unit activeUnit = parent.getGUI().getActiveUnit();
                if (activeUnit == null || activeUnit.getTile() == null || activeUnit.getMovesLeft() <= 0
                        || (!(activeUnit.getLocation() instanceof Tile) && !(activeUnit.getLocation() instanceof Unit))) {
                    parent.getGUI().setActiveUnit(null);
                    freeColClient.getInGameController().nextActiveUnit();
                }
            }
            freeColClient.getGUI().restartBlinking();
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
            }
            selectedUnit = unitLabel;
            if (unitLabel == null) {
                cargoPanel.setCarrier(null);
            } else {
                cargoPanel.setCarrier(unitLabel.getUnit());
                unitLabel.setSelected(true);
            }
            updateCargoLabel();
            //updateCarrierButtons();
            cargoPanel.revalidate();
            refresh();
        }
    }

    private void updateCargoPanel() {
        cargoPanel.initialize();
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
     * Returns a pointer to the <code>FreeColClient</code> which uses this
     * panel.
     * 
     * @return The <code>FreeColClient</code>.
     */
    public final FreeColClient getClient() {
        return freeColClient;
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
        this.colony = colony;
        editable = colony.getOwner() == freeColClient.getMyPlayer();
    }

    /**
     * Returns the current <code>Game</code>.
     * 
     * @return The current <code>Game</code>.
     */
    public final Game getGame() {
        return game;
    }

    public void componentAdded(ContainerEvent event) {
        if (event.getComponent() instanceof ColonyCargoPanel ||
            event.getComponent() instanceof UnitLabel) {
            updateCargoLabel();
            updateCarrierButtons();
            updateWarehouse();
            updateProductionPanel();
            refresh();
        }
    }

    public void componentRemoved(ContainerEvent event) {
        if (event.getChild() instanceof UnitLabel) {
            if (((UnitLabel) event.getChild()).getUnit().getTile().getSettlement() == null) {
                closeColonyPanel();
                return;
            }
            updateProductionPanel();
            updateBuildingBox();
            updateSoLLabel();
            updateOutsideColonyPanel();
        } else if (event.getChild() instanceof GoodsLabel) {
            updateWarehouse();
        }
        updateCargoLabel();
        updateCarrierButtons();
    }

    public final class ColonyCargoPanel extends CargoPanel {
        public ColonyCargoPanel(Canvas parent) {
            super(parent, false);
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
            //super(new HIGLayout(new int[] {0}, new int[] {0}));
            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
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

            ASingleBuildingPanel aSingleBuildingPanel;

            for (Building building : getColony().getBuildings()) {
                aSingleBuildingPanel = new ASingleBuildingPanel(building);
                if (colonyPanel.isEditable()) {
                    aSingleBuildingPanel.addMouseListener(releaseListener);
                    aSingleBuildingPanel.setTransferHandler(defaultTransferHandler);
                }
                aSingleBuildingPanel.setOpaque(false);
                add(aSingleBuildingPanel);
            }
        }


        /**
         * This panel is a single line (one building) in the
         * <code>BuildingsPanel</code>.
         */
        public final class ASingleBuildingPanel extends JPanel implements Autoscroll {
            Building building;

            ProductionLabel productionLabel;

            public final int[] widths = { 162, 46, 46, 46, 90 };

            public final int[] heights = { 60, 0 };

            public static final int labelColumn = 1;
            public static final int unitColumn = 2;
            public static final int productionColumn = 5;


            /**
             * Creates this ASingleBuildingPanel.
             * 
             * @param building The building to display information from.
             */
            public ASingleBuildingPanel(Building building) {
                this.building = building;

                setBackground(Color.WHITE);
                
                setLayout(new HIGLayout(widths, heights));

                initialize();
            }

            public void initialize() {

                removeAll();
                if (building.getMaxUnits() == 0) {
                    add(new JLabel("(" + building.getName() + ")"), higConst.rc(1, 1));
                } else {
                    add(new JLabel(building.getName()), higConst.rc(1, labelColumn));
                }

                List<Unit> unitList = building.getUnitList();
                for (int index = 0; index < unitList.size(); index++) {
                    Unit unit = unitList.get(index);
                    UnitLabel unitLabel = new UnitLabel(unit, parent, true);
                    if (colonyPanel.isEditable()) {
                        unitLabel.setTransferHandler(defaultTransferHandler);
                        unitLabel.addMouseListener(pressListener);
                    }
                    add(unitLabel, higConst.rc(1, unitColumn + index));
                    if (building.hasAbility("model.ability.teach")) {
                        if (unit.getStudent() != null) {
                            UnitLabel studentLabel = new UnitLabel(unit.getStudent(), parent, true);
                            studentLabel.setIgnoreLocation(true);
                            add(studentLabel, higConst.rc(2, unitColumn + index));
                        }
                    }
                }

                productionLabel = new ProductionLabel(building.getGoodsOutputType(),
                                                      building.getProductionNextTurn(),
                                                      building.getMaximumProduction(), parent);
                add(productionLabel, higConst.rc(1, productionColumn));

                setSize(getPreferredSize());
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

            public void updateProductionLabel() {
                productionLabel.setProduction(building.getProductionNextTurn());
                productionLabel.setMaximumProduction(building.getMaximumProduction());
                if (building.hasAbility("model.ability.teach")) {
                    initialize();
                }
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

                        if (building.canAdd(unit)) {
                            oldParent.remove(comp);
                            inGameController.work(unit, building);
                            updateBuildingBox();
                            updateWarehouse();
                        } else {
                            return null;
                        }
                    } else {
                        logger.warning("An invalid component got dropped on this BuildingsPanel.");
                        return null;
                    }
                }
                /*
                ((UnitLabel) comp).setSmall(true);
                c = ((JPanel) getComponent(1)).add(comp);
                refresh();
                */
                initialize();
                colonyPanel.updateSoLLabel();

                updateProductionPanel();
                updateProductionLabel();
                if (oldParent instanceof ASingleBuildingPanel) {
                    ((ASingleBuildingPanel) oldParent).updateProductionLabel();
                } else if (oldParent instanceof TilePanel.ASingleTilePanel) {
                    updateBuildingsPanel();
                }

                return null;
            }

            public void remove(Component comp) {
                super.remove(comp);
                updateProductionLabel();
            }
        }
    }

    /**
     * A panel that holds UnitsLabels that represent Units that are standing in
     * front of a colony.
     */
    public final class OutsideColonyPanel extends JPanel {
        private final ColonyPanel colonyPanel;


        /**
         * Creates this OutsideColonyPanel.
         * 
         * @param colonyPanel The panel that holds this OutsideColonyPanel.
         */
        public OutsideColonyPanel(ColonyPanel colonyPanel) {
            this.colonyPanel = colonyPanel;
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

                    if (!(unit.getLocation() instanceof Unit)) {
                        inGameController.putOutsideColony(unit);
                    }

                    if (unit.getColony() == null) {
                        closeColonyPanel();
                        return null;
                    } else if (!(unit.getLocation() instanceof Tile) && !(unit.getLocation() instanceof Unit)) {
                        return null;
                    }

                    oldParent.remove(comp);
                    updateBuildingsPanel();
                    updateBuildingBox();
                    updateProductionPanel();
                } else {
                    logger.warning("An invalid component got dropped on this ColonistsPanel.");
                    return null;
                }
            }

            ((UnitLabel) comp).setSmall(false);
            Component c = add(comp);
            refresh();
            colonyPanel.updateSoLLabel();
            if (oldParent != null && oldParent.getParent() instanceof BuildingsPanel.ASingleBuildingPanel) {
                ((BuildingsPanel.ASingleBuildingPanel) oldParent.getParent()).updateProductionLabel();
            }
            return c;
        }
    }

    /**
     * A panel that holds UnitsLabels that represent naval Units that are
     * waiting in the port of the colony.
     */
    public final class InPortPanel extends JPanel {
        /**
         * Adds a component to this InPortPanel.
         * 
         * @param comp The component to add to this InPortPanel.
         * @return The component argument.
         */
        @Override
        public Component add(Component comp) {
            return super.add(comp);
        }

        @Override
        public String getUIClassID() {
            return "InPortPanelUI";
        }
    }

    /**
     * A panel that holds goods that represent cargo that is inside the Colony.
     */
    public final class WarehousePanel extends JPanel {
        private final ColonyPanel colonyPanel;


        /**
         * Creates this WarehousePanel.
         * 
         * @param colonyPanel The panel that holds this WarehousePanel.
         */
        public WarehousePanel(ColonyPanel colonyPanel) {
            this.colonyPanel = colonyPanel;
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
                    // Goods g = ((GoodsLabel)comp).getGoods();
                    ((GoodsLabel) comp).setSmall(false);
                    // inGameController.unloadCargo(g, selectedUnit.getUnit());
                    // colonyPanel.getWarehousePanel().revalidate();
                    // colonyPanel.getCargoPanel().revalidate();
                    // updateCargoLabel();
                    // buildingsPanel.initialize();
                    // initialize();
                    reinitialize();
                    return comp;
                }
                logger.warning("An invalid component got dropped on this WarehousePanel.");
                return null;
            }

            Component c = add(comp);

            refresh();
            return c;
        }

        @Override
        public void remove(Component comp) {
            if (comp instanceof GoodsLabel) {
                // Goods g = ((GoodsLabel)comp).getGoods();

                super.remove(comp);

                colonyPanel.getWarehousePanel().revalidate();
                colonyPanel.getCargoPanel().revalidate();
            }
        }

        public void initialize() {
            warehousePanel.removeAll();
            List<Goods> allGoods = getColony().getGoodsContainer().getFullGoods();
            for (Goods goods : allGoods) {
                if (goods.getType().isStorable()) {
                    GoodsLabel goodsLabel = new GoodsLabel(goods, parent);
                    if (colonyPanel.isEditable()) {
                        goodsLabel.setTransferHandler(defaultTransferHandler);
                        goodsLabel.addMouseListener(pressListener);
                    }
                    warehousePanel.add(goodsLabel, false);
                }
            }

            warehousePanel.revalidate();
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
            this.colonyPanel = colonyPanel;
            setBackground(Color.BLACK);
            // setOpaque(false);
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

        @Override
        public void paintComponent(Graphics g) {
            GUI colonyTileGUI = parent.getColonyTileGUI();
            ImageLibrary lib = colonyTileGUI.getImageLibrary();

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
                                * lib.getTerrainImageWidth(tileType) / 2, (x + y) * lib.getTerrainImageHeight(tileType) / 2,
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
        public final class ASingleTilePanel extends JPanel {

            private ColonyTile colonyTile;


            public ASingleTilePanel(ColonyTile colonyTile, int x, int y) {
                setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
                this.colonyTile = colonyTile;

                setOpaque(false);

                updateDescriptionLabel();

                if (colonyTile.getUnit() != null) {
                    Unit unit = colonyTile.getUnit();
                    UnitLabel unitLabel = new UnitLabel(unit, parent);
                    if (colonyPanel.isEditable()) {
                        unitLabel.setTransferHandler(defaultTransferHandler);
                        unitLabel.addMouseListener(pressListener);
                    }
                    add(unitLabel);

                    updateDescriptionLabel(unitLabel, true);
                }

                ImageLibrary lib = parent.getColonyTileGUI().getImageLibrary();

                if (colonyTile.isColonyCenterTile()) {
                    initializeAsCenterTile(lib);
                }

                if (colonyPanel.isEditable()) {
                    setTransferHandler(defaultTransferHandler);
                    addMouseListener(releaseListener);
                }

                TileType tileType = colonyTile.getTile().getType();
                // Size and position:
                setSize(lib.getTerrainImageWidth(tileType), lib.getTerrainImageHeight(tileType));
                setLocation(((2 - x) + y) * lib.getTerrainImageWidth(tileType) / 2, (x + y) * lib.getTerrainImageHeight(tileType) / 2);
            }

            /**
             * Initialized the center of the colony panel tile. The one
             * containing the city.
             * 
             * @param lib an ImageLibrary
             */
            private void initializeAsCenterTile(ImageLibrary lib) {

                setLayout(new GridLayout(2, 1));

                TileType tileType = colonyTile.getTile().getType();
                // A colony always produces food.
                GoodsType primaryGood = colonyTile.getTile().primaryGoods();
                ImageIcon goodsIcon = parent.getGUI().getImageLibrary().getGoodsImageIcon(primaryGood);
                ProductionLabel pl = new ProductionLabel(primaryGood, colonyTile.getProductionOf(primaryGood), parent);
                pl.setSize(lib.getTerrainImageWidth(tileType), goodsIcon.getIconHeight());
                add(pl);

                // A colony may produce one additional good
                GoodsType secondaryGood = colonyTile.getTile().secondaryGoods();
                if (colonyTile.getProductionOf(secondaryGood) != 0) {
                    goodsIcon = parent.getGUI().getImageLibrary().getGoodsImageIcon(secondaryGood);
                    ProductionLabel sl = new ProductionLabel(secondaryGood, colonyTile.getProductionOf(secondaryGood), parent);
                    sl.setSize(lib.getTerrainImageWidth(tileType), goodsIcon.getIconHeight());
                    add(sl);
                }
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

            public ColonyTile getColonyTile() {
                return colonyTile;
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
                        if (colonyTile.getWorkTile().getOwningSettlement() != null
                                && colonyTile.getWorkTile().getOwningSettlement() != getColony()) {
                            if (colonyTile.getWorkTile().getOwningSettlement().getOwner().isEuropean()) {
                                parent.errorMessage("tileTakenEuro");
                            } else { // its an indian settlement
                                parent.errorMessage("tileTakenInd");
                            }
                            return null;
                        }
                        int price = unit.getOwner().getLandPrice(colonyTile.getWorkTile());
                        if (price > 0) {
                            Player player = colonyTile.getWorkTile().getOwner();
                            ChoiceItem[] choices = {
                                    new ChoiceItem(Messages.message("indianLand.pay").replaceAll("%amount%",
                                            Integer.toString(price)), 1),
                                    new ChoiceItem(Messages.message("indianLand.take"), 2) };
                            ChoiceItem ci = (ChoiceItem) parent.
                                showChoiceDialog(Messages.message("indianLand.text",
                                                                  "%player%", player.getNationAsString()),
                                                 Messages.message("indianLand.cancel"), choices);
                            if (ci == null) {
                                return null;
                            } else if (ci.getChoice() == 1) {
                                if (price > freeColClient.getMyPlayer().getGold()) {
                                    parent.errorMessage("notEnoughGold");
                                    return null;
                                }

                                inGameController.buyLand(colonyTile.getWorkTile());
                            }
                        }

                        if (colonyTile.canAdd(unit)) {
                            oldParent.remove(comp);

                            inGameController.work(unit, colonyTile);

                            // check whether worktype is suitable
                            GoodsType workType = colonyTile.getWorkType(unit);
                            if (workType != unit.getWorkType()) {
                                inGameController.changeWorkType(unit, workType);
                            }

                            updateDescriptionLabel((UnitLabel) comp, true);

                            updateBuildingBox();
                            updateWarehouse();
                            updateBuildingsPanel();
                            updateProductionPanel();

                            ((UnitLabel) comp).setSmall(false);

                            colonyPanel.updateSoLLabel();
                        } else {
                            return null;
                        }
                    } else {
                        logger.warning("An invalid component got dropped on this CargoPanel.");
                        return null;
                    }
                }

                Component c = add(comp);
                refresh();
                if (oldParent != null && oldParent instanceof BuildingsPanel.ASingleBuildingPanel) {
                    ((BuildingsPanel.ASingleBuildingPanel) oldParent).updateProductionLabel();
                }
                return c;
            }

            @Override
            public void remove(Component comp) {
                if (comp instanceof UnitLabel) {
                    ((UnitLabel) comp).setSmall(false);
                    updateDescriptionLabel((UnitLabel) comp, false);
                }
                super.remove(comp);
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

}
