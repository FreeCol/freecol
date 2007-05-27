package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
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
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
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
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import cz.autel.dmi.HIGLayout;

/**
 * This is a panel for the Colony display. It shows the units that are working
 * in the colony, the buildings and much more.
 */
public final class ColonyPanel extends FreeColPanel implements ActionListener {
    public static final String COPYRIGHT =
            "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private static Logger logger =
            Logger.getLogger(ColonyPanel.class.getName());

    /**
     * The height of the area in which autoscrolling should happen.
     */
    public static final int SCROLL_AREA_HEIGHT = 40;

    /**
     * The speed of the scrolling.
     */
    public static final int SCROLL_SPEED = 10;

    private static final int EXIT = 0, BUY_BUILDING = 1, UNLOAD = 2,
            RENAME = 3, WAREHOUSE = 4;

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

    private final ProductionPanel productionPanel;

    private final BuildingBox buildingBox;

    private final JComboBox nameBox;

    private final OutsideColonyPanel outsideColonyPanel;

    private final InPortPanel inPortPanel;

    private final CargoPanel cargoPanel;

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
        closeInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false),
                "pressed");
        closeInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true),
                "released");
        SwingUtilities.replaceUIInputMap(exitButton,
                JComponent.WHEN_IN_FOCUSED_WINDOW, closeInputMap);

        InputMap unloadInputMap = new ComponentInputMap(unloadButton);
        unloadInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0, false),
                "pressed");
        unloadInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0, true),
                "released");
        SwingUtilities.replaceUIInputMap(unloadButton,
                JComponent.WHEN_IN_FOCUSED_WINDOW, unloadInputMap);

        productionPanel = new ProductionPanel(this);
        outsideColonyPanel = new OutsideColonyPanel(this);
        inPortPanel = new InPortPanel();
        cargoPanel = new CargoPanel(this);
        warehousePanel = new WarehousePanel(this);
        tilePanel = new TilePanel(this);
        buildingsPanel = new BuildingsPanel(this);

        defaultTransferHandler = new DefaultTransferHandler(parent, this);
        outsideColonyPanel.setTransferHandler(defaultTransferHandler);
        inPortPanel.setTransferHandler(defaultTransferHandler);
        cargoPanel.setTransferHandler(defaultTransferHandler);
        warehousePanel.setTransferHandler(defaultTransferHandler);

        pressListener = new DragListener(this);
        releaseListener = new DropListener();
        outsideColonyPanel.addMouseListener(releaseListener);
        inPortPanel.addMouseListener(releaseListener);
        cargoPanel.addMouseListener(releaseListener);
        warehousePanel.addMouseListener(releaseListener);

        outsideColonyPanel.setLayout(new GridLayout(0, 2));
        inPortPanel.setLayout(new GridLayout(0, 2));
        cargoPanel.setLayout(new GridLayout(1, 0));
        warehousePanel.setLayout(new GridLayout(2, 8));

        solLabel =
                new JLabel(Messages.message("sonsOfLiberty") + ": 0%, "
                        + Messages.message("tory") + ": 100%");
        hammersLabel = new FreeColProgressBar(parent, Goods.HAMMERS);
        toolsLabel = new FreeColProgressBar(parent, Goods.TOOLS);

        buildingBox = new BuildingBox(this);

        JScrollPane outsideColonyScroll =
                new JScrollPane(outsideColonyPanel,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollPane inPortScroll =
                new JScrollPane(inPortPanel,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollPane cargoScroll =
                new JScrollPane(cargoPanel,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane warehouseScroll =
                new JScrollPane(warehousePanel,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane tilesScroll =
                new JScrollPane(tilePanel,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane buildingsScroll =
                new JScrollPane(buildingsPanel,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
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
        tilesScroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), Messages.message("surroundingArea")), new BevelBorder(BevelBorder.LOWERED)));
        buildingsScroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), Messages.message("buildings")), BorderFactory.createEtchedBorder()));
        warehouseScroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), Messages.message("goods")), BorderFactory.createEtchedBorder()));
        cargoBorder = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), Messages.message("cargoOnCarrier"));
        cargoScroll.setBorder(BorderFactory.createCompoundBorder(cargoBorder, BorderFactory.createEtchedBorder()));
        inPortScroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), Messages.message("inPort")), BorderFactory.createEtchedBorder()));
        outsideColonyScroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), Messages.message("outsideColony")), BorderFactory.createEtchedBorder()));

        // manual border corrections
        Border correctBorder = BorderFactory.createEmptyBorder(0, BORDER_CORRECT, 0, BORDER_CORRECT);
        productionPanel.setBorder(correctBorder);
        solLabel.setBorder(correctBorder);
        unloadButton.setBorder(BorderFactory.createCompoundBorder(correctBorder, unloadButton.getBorder()));
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
        renameButton.setActionCommand(String.valueOf(RENAME));
        warehouseButton.setActionCommand(String.valueOf(WAREHOUSE));

        buyBuilding.addActionListener(this);
        exitButton.addActionListener(this);
        unloadButton.addActionListener(this);
        renameButton.addActionListener(this);
        warehouseButton.addActionListener(this);

        selectedUnit = null;

        // See the message of Ulf Onnen for more information about the presence
        // of this fake mouse listener.
        addMouseListener(new MouseAdapter() {
        });

        int[] widths = { 250, margin, 138, margin, 215, margin, 204 };
        int[] heights = { 
            0, // colony select box
            margin, 225, // colony tiles and buildings
            margin, 8, // extra space for production panel
            -8, // hammers label, same size as tools label
            margin, -6, // tools label, same size as hammers label
            margin, 120, // port and cargo panels
            margin, 140, // warehouse
            margin, 0 // buttons
        };
        setLayout(new HIGLayout(widths, heights));

        int row = 1;
        add(nameBox, higConst.rcwh(row, 1, 7, 1, ""));
        row += 2;
        add(tilesScroll, higConst.rcwh(row, 1, 3, 1));
        add(buildingsScroll, higConst.rcwh(row, 5, 3, 1));
        row += 2;
        add(productionPanel, higConst.rcwh(row, 1, 3, 2));
        row += 1;
        add(buildingBox, higConst.rc(row, 5));
        add(hammersLabel, higConst.rc(row, 7));
        row += 2;
        add(solLabel, higConst.rcwh(row, 1, 3, 1));
        add(buyBuilding, higConst.rc(row, 5));
        add(toolsLabel, higConst.rc(row, 7));
        row += 2;
        add(inPortScroll, higConst.rc(row, 1));
        add(cargoScroll, higConst.rcwh(row, 3, 3, 1));
        add(outsideColonyScroll, higConst.rcwh(row, 7, 1, 3));
        row += 2;
        add(warehouseScroll, higConst.rcwh(row, 1, 5, 1));
        row += 2;
        add(unloadButton, higConst.rc(row, 1, "l"));
        add(warehouseButton, higConst.rc(row, 3, "l"));
        add(renameButton, higConst.rc(row, 5, "r"));
        add(exitButton, higConst.rc(row, 7, "r"));

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
            unitLabel.setTransferHandler(defaultTransferHandler);
            unitLabel.addMouseListener(pressListener);

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

        updateUnloadButton();

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

        buildingBox.initialize();
        updateNameBox();

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
     * Enables the unload button if the currently selected unit is a carrier
     * with some cargo.
     */
    private void updateUnloadButton() {
        unloadButton.setEnabled(false);
        if (selectedUnit != null) {
            Unit unit = selectedUnit.getUnit();
            if (unit != null && unit.isCarrier()
                    && unit.getSpaceLeft() < unit.getInitialSpaceLeft()) {
                unloadButton.setEnabled(true);
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
                                                  new String[][] {{"%name%", selectedUnit.getUnit().getName()},
                                                                  {"%space%", String.valueOf(selectedUnit.getUnit().getSpaceLeft())}}));
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
        solLabel
                .setText(Messages.message("sonsOfLiberty")
                        + ": "
                        + getColony().getSoL()
                        + "% ("
                        + getColony().getMembers()
                        + "), "
                        + Messages.message("tory")
                        + ": "
                        + getColony().getTory()
                        + "% ("
                        + (getColony().getUnitCount() - getColony()
                                .getMembers()) + ")");
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
        
        for(ActionListener al : listeners) {
            nameBox.addActionListener(al);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void sortColonies(List colonies) {
        Collections.sort(colonies, freeColClient.getClientOptions()
                .getColonyComparator());
    }

    public void updateBuildingBox() {
        buildingBox.initialize();
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
        if (getColony().getCurrentlyBuilding() == Building.NONE) {
            hammersLabel.update(0, 0, 0, 0);
            toolsLabel.update(0, 0, 0, 0);
        } else {
            final int hammers = getColony().getHammers();
            final int tools = getColony().getGoodsCount(Goods.TOOLS);
            final int nextHammers =
                    getColony().getBuildingForProducing(Goods.HAMMERS)
                            .getProductionNextTurn();
            final int nextTools =
                    getColony().getBuildingForProducing(Goods.TOOLS)
                            .getProductionNextTurn();

            int hammersNeeded = 0;
            int toolsNeeded = 0;
            if (getColony().getCurrentlyBuilding() < Colony.BUILDING_UNIT_ADDITION) {
                hammersNeeded =
                        getColony().getBuilding(
                                getColony().getCurrentlyBuilding())
                                .getNextHammers();
                toolsNeeded =
                        getColony().getBuilding(
                                getColony().getCurrentlyBuilding())
                                .getNextTools();
            } else {
                hammersNeeded =
                        Unit.getNextHammers(getColony().getCurrentlyBuilding()
                                - Colony.BUILDING_UNIT_ADDITION);
                toolsNeeded =
                        Unit.getNextTools(getColony().getCurrentlyBuilding()
                                - Colony.BUILDING_UNIT_ADDITION);
            }
            hammersNeeded = Math.max(hammersNeeded, 0);
            toolsNeeded = Math.max(toolsNeeded, 0);

            hammersLabel.update(0, hammersNeeded, hammers, nextHammers);
            toolsLabel.update(0, toolsNeeded, tools, nextTools);

            buyBuilding.setEnabled(getColony().getCurrentlyBuilding() >= 0
                    && getColony().getPriceForBuilding() <= freeColClient
                            .getMyPlayer().getGold());
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
            unitLabel.setTransferHandler(defaultTransferHandler);
            unitLabel.addMouseListener(pressListener);

            if (!unit.isCarrier()) {
                outsideColonyPanel.add(unitLabel, false);
            }
        }
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
                freeColClient.getCanvas().showWarehouseDialog(colony);
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
                updateCargoLabel();
                updateCargoPanel();
                updateOutsideColonyPanel();
                outsideColonyPanel.revalidate();
                getCargoPanel().revalidate();
                refresh();
            }
        }
        unloadButton.setEnabled(false);
    }

    /**
     * Closes the <code>ColonyPanel</code>.
     */
    public void closeColonyPanel() {
        parent.remove(this);
        freeColClient.getInGameController().nextModelMessage();
        Unit activeUnit = parent.getGUI().getActiveUnit();
        if (activeUnit == null
                || activeUnit.getTile() == null
                || activeUnit.getMovesLeft() <= 0
                || (!(activeUnit.getLocation() instanceof Tile) && !(activeUnit
                        .getLocation() instanceof Unit))) {
            parent.getGUI().setActiveUnit(null);
            freeColClient.getInGameController().nextActiveUnit();
        }
        freeColClient.getGUI().restartBlinking();
    }

    /**
     * Selects a unit that is located somewhere on this panel.
     * 
     * @param unit The unit that is being selected.
     */
    public void setSelectedUnit(Unit unit) {
        Component[] components = inPortPanel.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof UnitLabel
                    && ((UnitLabel) components[i]).getUnit() == unit) {
                setSelectedUnitLabel((UnitLabel) components[i]);
                break;
            }
        }
        updateUnloadButton();
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
            updateCargoPanel();
            updateCargoLabel();
        }
        updateUnloadButton();
        cargoPanel.revalidate();
        refresh();
    }

    private void updateCargoPanel() {
        cargoPanel.removeAll();

        if (selectedUnit != null) {
            selectedUnit.setSelected(true);
            Unit selUnit = selectedUnit.getUnit();

            Iterator<Unit> unitIterator = selUnit.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit unit = unitIterator.next();

                UnitLabel label = new UnitLabel(unit, parent);
                label.setTransferHandler(defaultTransferHandler);
                label.addMouseListener(pressListener);

                cargoPanel.add(label, false);
            }

            Iterator<Goods> goodsIterator = selUnit.getGoodsIterator();
            while (goodsIterator.hasNext()) {
                Goods g = goodsIterator.next();

                GoodsLabel label = new GoodsLabel(g, parent);
                label.setTransferHandler(defaultTransferHandler);
                label.addMouseListener(pressListener);

                cargoPanel.add(label, false);
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
    }

    /**
     * Returns the current <code>Game</code>.
     * 
     * @return The current <code>Game</code>.
     */
    public final Game getGame() {
        return game;
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
            super(new GridLayout(0, 1));
            this.colonyPanel = colonyPanel;
        }

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

            Iterator<Building> buildingIterator = getColony().getBuildingIterator();
            while (buildingIterator.hasNext()) {
                Building building = buildingIterator.next();
                if (building.isBuilt()) {
                    aSingleBuildingPanel = new ASingleBuildingPanel(building);
                    aSingleBuildingPanel.addMouseListener(releaseListener);
                    aSingleBuildingPanel
                            .setTransferHandler(defaultTransferHandler);
                    aSingleBuildingPanel.setOpaque(false);
                    add(aSingleBuildingPanel);
                }
            }
        }


        /**
         * This panel is a single line (one building) in the
         * <code>BuildingsPanel</code>.
         */
        public final class ASingleBuildingPanel extends JPanel implements
                Autoscroll {
            Building building;

            JPanel productionInBuildingPanel = new JPanel();
            public final int[] widths = {160, 140, 100};
            public final int[] heights = {60};


            /**
             * Creates this ASingleBuildingPanel.
             * 
             * @param building The building to display information from.
             */
            public ASingleBuildingPanel(Building building) {
                this.building = building;

                removeAll();
                setBackground(Color.WHITE);

                setLayout(new HIGLayout(widths, heights));

                JPanel colonistsInBuildingPanel = new JPanel();
                colonistsInBuildingPanel.setLayout(new FlowLayout(
                        FlowLayout.LEFT));
                colonistsInBuildingPanel.setBackground(Color.WHITE);

                if (building.getMaxUnits() == 0) {
                    add(new JLabel("(" + building.getName() + ")"), higConst.rc(1, 1));
                } else {
                    add(new JLabel(building.getName()), higConst.rc(1, 1));
                }

                Iterator<Unit> unitIterator = building.getUnitIterator();
                while (unitIterator.hasNext()) {
                    Unit unit = unitIterator.next();
                    UnitLabel unitLabel = new UnitLabel(unit, parent, true);
                    unitLabel.setTransferHandler(defaultTransferHandler);
                    unitLabel.addMouseListener(pressListener);
                    colonistsInBuildingPanel.add(unitLabel);
                }

                colonistsInBuildingPanel.setOpaque(false);
                add(colonistsInBuildingPanel, higConst.rc(1, 2));

                updateProductionInBuildingPanel();
                add(productionInBuildingPanel, higConst.rc(1, 3));

                setPreferredSize(new Dimension(getWidth(), (parent.getGUI()
                        .getImageLibrary().getUnitImageHeight(0) / 3) * 2 + 5));
            }

            public void autoscroll(Point p) {
                JViewport vp =
                        (JViewport) colonyPanel.buildingsPanel.getParent();
                if (getLocation().y + p.y - vp.getViewPosition().y < SCROLL_AREA_HEIGHT) {
                    vp.setViewPosition(new Point(vp.getViewPosition().x, Math
                            .max(vp.getViewPosition().y - SCROLL_SPEED, 0)));
                } else if (getLocation().y + p.y - vp.getViewPosition().y >= vp
                        .getHeight()
                        - SCROLL_AREA_HEIGHT) {
                    vp.setViewPosition(new Point(vp.getViewPosition().x, Math
                            .min(vp.getViewPosition().y + SCROLL_SPEED,
                                    colonyPanel.buildingsPanel.getHeight()
                                            - vp.getHeight())));
                }
            }

            public Insets getAutoscrollInsets() {
                Rectangle r = getBounds();
                return new Insets(r.x, r.y, r.width, r.height);
            }

            public void updateProductionInBuildingPanel() {
                productionInBuildingPanel.removeAll();
                productionInBuildingPanel.setOpaque(false);
                productionInBuildingPanel.setLayout(new FlowLayout());

                int production = building.getProductionNextTurn();
                if (production > 0) {
                    ImageIcon goodsIcon =
                            parent.getImageProvider().getGoodsImageIcon(
                                    building.getGoodsOutputType());
                    int width = goodsIcon.getIconWidth() * 3;
                    BufferedImage productionImage =
                            parent.getGUI().createProductionImage(goodsIcon,
                                    production, width,
                                    goodsIcon.getIconHeight());
                    JLabel productionLabel =
                            new JLabel(new ImageIcon(productionImage));
                    productionInBuildingPanel.add(productionLabel);

                    if (production != building.getMaximumProduction()) {
                        // JLabel tl = new JLabel("(" +
                        // (building.getMaximumProduction() - production) +
                        // ")");
                        // tl.setForeground(Color.RED);
                        JLabel tl =
                                new JLabel(Integer.toString(production
                                        - building.getMaximumProduction()));
                        productionInBuildingPanel.add(tl);
                    }
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
                        logger
                                .warning("An invalid component got dropped on this BuildingsPanel.");
                        return null;
                    }
                }

                ((UnitLabel) comp).setSmall(true);
                c = ((JPanel) getComponent(1)).add(comp);
                refresh();
                colonyPanel.updateSoLLabel();

                updateProductionInBuildingPanel();
                if (oldParent.getParent() instanceof ASingleBuildingPanel) {
                    ((ASingleBuildingPanel) oldParent.getParent())
                            .updateProductionInBuildingPanel();
                }

                return c;
            }
        }
    }

    /**
     * This panel holds the information of the current food, liberty bell and
     * cross production.
     */
    public static final class ProductionPanel extends JPanel {
        private final ColonyPanel colonyPanel;


        public ProductionPanel(ColonyPanel colonyPanel) {
            this.colonyPanel = colonyPanel;
        }

        private Colony getColony() {
            return colonyPanel.getColony();
        }

        private Canvas getCanvas() {
            return colonyPanel.getParentCanvas();
        }

        public void paintComponent(Graphics g) {
            if (getColony() == null) {
                // Apparently this can happen
                return;
            }
            int need = getColony().getFoodConsumption();
            int surplus = getColony().getFoodProduction() - need;
            final int horses = getColony().getHorseProduction();
            final int bells = getColony().getProductionOf(Goods.BELLS);
            final int crosses = getColony().getProductionOf(Goods.CROSSES);

            ImageIcon goodsIcon =
                    getCanvas().getImageProvider()
                            .getGoodsImageIcon(Goods.FOOD);
            BufferedImage productionImage;
            int nextX = 0;
            int add;

            // Food production:
            if (need != 0) {
                nextX =
                        (need > 12) ? goodsIcon.getIconWidth() : Math.min(need,
                                4)
                                * goodsIcon.getIconWidth();
                if (horses == 0) {
                    productionImage =
                            getCanvas().getGUI().createProductionImage(
                                    goodsIcon, need, nextX, getHeight(), 12);
                } else {
                    need += horses;
                    surplus -= horses;
                    nextX = goodsIcon.getIconWidth();
                    productionImage =
                            getCanvas().getGUI().createProductionImage(
                                    goodsIcon, need, nextX, getHeight(), 1);
                }
                g.drawImage(productionImage, BORDER_CORRECT, 0, null);
                nextX += goodsIcon.getIconWidth() / 4;
            }

            // Food surplus:
            if (surplus != 0) {
                if (surplus > 6 || surplus < 0 || surplus == 1) {
                    productionImage =
                            getCanvas().getGUI().createProductionImage(
                                    goodsIcon, surplus,
                                    goodsIcon.getIconWidth(), getHeight(), 6,
                                    true);
                    add = goodsIcon.getIconWidth();
                } else {
                    productionImage =
                            getCanvas().getGUI().createProductionImage(
                                    goodsIcon, surplus,
                                    goodsIcon.getIconWidth() * 2, getHeight(),
                                    6, true);
                    add = goodsIcon.getIconWidth() * 3;
                }

                g.drawImage(productionImage, nextX, 0, null);
                nextX += productionImage.getWidth() / 4 + add;
            } else {
                // Show it even if zero surplus
                productionImage =
                        getCanvas().getGUI().createProductionImage(goodsIcon,
                                surplus, goodsIcon.getIconWidth(), getHeight(),
                                -1, 1, -1, true);
                add = goodsIcon.getIconWidth();
                g.drawImage(productionImage, nextX, 0, null);
                nextX += productionImage.getWidth() / 4 + add;
            }

            // Horses:
            if (horses != 0) {
                goodsIcon =
                        getCanvas().getImageProvider().getGoodsImageIcon(
                                Goods.HORSES);
                if (horses > 2 || horses == 1) {
                    productionImage =
                            getCanvas().getGUI().createProductionImage(
                                    goodsIcon, horses,
                                    goodsIcon.getIconWidth(), getHeight(), 2);
                    add = goodsIcon.getIconWidth();
                } else {
                    productionImage =
                            getCanvas().getGUI().createProductionImage(
                                    goodsIcon, horses,
                                    goodsIcon.getIconWidth() * 2, getHeight(),
                                    2);
                    add = goodsIcon.getIconWidth() * 2;
                }
                g.drawImage(productionImage, nextX, 0, null);
                nextX += productionImage.getWidth() / 4 + add;
            }

            // Liberty bells:
            if (bells != 0) {
                goodsIcon =
                        getCanvas().getImageProvider().getGoodsImageIcon(
                                Goods.BELLS);
                if (bells > 6 || bells == 1) {
                    productionImage =
                            getCanvas().getGUI().createProductionImage(
                                    goodsIcon, bells, goodsIcon.getIconWidth(),
                                    getHeight(), 6);
                    add = goodsIcon.getIconWidth();
                } else {
                    productionImage =
                            getCanvas().getGUI().createProductionImage(
                                    goodsIcon, bells,
                                    goodsIcon.getIconWidth() * 2, getHeight(),
                                    6);
                    add = goodsIcon.getIconWidth() * 2;
                }
                g.drawImage(productionImage, nextX, 0, null);
                nextX += productionImage.getWidth() / 4 + add;
            }

            // Crosses:
            if (crosses != 0) {
                goodsIcon =
                        getCanvas().getImageProvider().getGoodsImageIcon(
                                Goods.CROSSES);
                if (crosses > 6 || crosses == 1) {
                    productionImage =
                            getCanvas().getGUI().createProductionImage(
                                    goodsIcon, crosses,
                                    goodsIcon.getIconWidth(), getHeight(), 6);
                    add = goodsIcon.getIconWidth();
                } else {
                    productionImage =
                            getCanvas().getGUI().createProductionImage(
                                    goodsIcon, crosses,
                                    goodsIcon.getIconWidth() * 2, getHeight(),
                                    6);
                    add = goodsIcon.getIconWidth() * 2;
                }
                g.drawImage(productionImage, nextX, 0, null);
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
                    } else if (!(unit.getLocation() instanceof Tile)
                            && !(unit.getLocation() instanceof Unit)) {
                        return null;
                    }

                    oldParent.remove(comp);
                    updateBuildingBox();
                } else {
                    logger
                            .warning("An invalid component got dropped on this ColonistsPanel.");
                    return null;
                }
            }

            ((UnitLabel) comp).setSmall(false);
            updateCargoLabel();
            Component c = add(comp);
            refresh();
            colonyPanel.updateSoLLabel();
            if (oldParent != null
                    && oldParent.getParent() instanceof BuildingsPanel.ASingleBuildingPanel) {
                ((BuildingsPanel.ASingleBuildingPanel) oldParent.getParent())
                        .updateProductionInBuildingPanel();
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
        public Component add(Component comp) {
            return super.add(comp);
        }

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
         * Creates this CargoPanel.
         * 
         * @param colonyPanel The panel that holds this CargoPanel.
         */
        public WarehousePanel(ColonyPanel colonyPanel) {
            this.colonyPanel = colonyPanel;
        }

        public String getUIClassID() {
            return "WarehousePanelUI";
        }

        /**
         * Adds a component to this CargoPanel and makes sure that the unit or
         * good that the component represents gets modified so that it is on
         * board the currently selected ship.
         * 
         * @param comp The component to add to this CargoPanel.
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
                logger
                        .warning("An invalid component got dropped on this WarehousePanel.");
                return null;
            }

            Component c = add(comp);

            refresh();
            return c;
        }

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
            Iterator<Goods> goodsIterator =
                    getColony().getGoodsContainer().getFullGoodsIterator();
            while (goodsIterator.hasNext()) {
                Goods goods = goodsIterator.next();

                GoodsLabel goodsLabel = new GoodsLabel(goods, parent);
                goodsLabel.setTransferHandler(defaultTransferHandler);
                goodsLabel.addMouseListener(pressListener);

                warehousePanel.add(goodsLabel, false);
            }

            warehousePanel.revalidate();
        }
    }

    /**
     * A panel that holds units and goods that represent Units and cargo that
     * are on board the currently selected ship.
     */
    public final class CargoPanel extends JPanel {
        private final ColonyPanel colonyPanel;


        /**
         * Creates this CargoPanel.
         * 
         * @param colonyPanel The panel that holds this CargoPanel.
         */
        public CargoPanel(ColonyPanel colonyPanel) {
            this.colonyPanel = colonyPanel;
        }

        public String getUIClassID() {
            return "CargoPanelUI";
        }

        /**
         * Adds a component to this CargoPanel and makes sure that the unit or
         * good that the component represents gets modified so that it is on
         * board the currently selected ship.
         * 
         * @param comp The component to add to this CargoPanel.
         * @param editState Must be set to 'true' if the state of the component
         *            that is added (which should be a dropped component
         *            representing a Unit or good) should be changed so that the
         *            underlying unit or goods are on board the currently
         *            selected ship.
         * @return The component argument.
         */
        public Component add(Component comp, boolean editState) {
            Container oldParent = comp.getParent();
            if (selectedUnit == null) {
                return null;
            }

            if (editState) {
                if (comp instanceof UnitLabel) {
                    if (oldParent != null) {
                        oldParent.remove(comp);
                    }
                    Unit unit = ((UnitLabel) comp).getUnit();
                    if (!unit.isCarrier()) {// No, you cannot load ships onto
                        // other ships.
                        if (!selectedUnit.getUnit().canAdd(unit)) {
                            if (oldParent != null) {
                                oldParent.add(comp);
                            }
                            return null;
                        }

                        ((UnitLabel) comp).setSmall(false);
                        if (inGameController.boardShip(unit, selectedUnit
                                .getUnit())) {
                            if (unit.getTile().getSettlement() == null) {
                                closeColonyPanel();
                            }

                            updateBuildingBox();
                            colonyPanel.updateSoLLabel();
                        } else {
                            if (oldParent != null) {
                                oldParent.add(comp);
                            }
                            return null;
                        }
                    } else {
                        if (oldParent != null) {
                            oldParent.add(comp);
                        }

                        return null;
                    }
                } else if (comp instanceof GoodsLabel) {
                    Goods g = ((GoodsLabel) comp).getGoods();

                    Unit carrier = getSelectedUnit();
                    int newAmount = g.getAmount();
                    if (carrier.getSpaceLeft() == 0
                            && carrier.getGoodsContainer().getGoodsCount(
                                    g.getType())
                                    % 100 + g.getAmount() > 100) {
                        newAmount =
                                100 - carrier.getGoodsContainer()
                                        .getGoodsCount(g.getType()) % 100;
                    } else if (g.getAmount() > 100) {
                        newAmount = 100;
                    }

                    if (newAmount == 0) {
                        return null;
                    }

                    Goods goodsToAdd =
                            new Goods(game, g.getLocation(), g.getType(),
                                    newAmount);
                    if (!selectedUnit.getUnit().canAdd(goodsToAdd)) {
                        return null;
                    }
                    g.setAmount(g.getAmount() - newAmount);

                    ((GoodsLabel) comp).setSmall(false);
                    inGameController.loadCargo(goodsToAdd, selectedUnit
                            .getUnit());
                    colonyPanel.getWarehousePanel().revalidate();

                    reinitialize();

                    return comp;
                } else {
                    logger
                            .warning("An invalid component got dropped on this CargoPanel.");
                    return null;
                }
            }

            updateCargoLabel();
            Component c = add(comp);

            refresh();
            if (oldParent != null
                    && oldParent.getParent() instanceof BuildingsPanel.ASingleBuildingPanel) {
                ((BuildingsPanel.ASingleBuildingPanel) oldParent.getParent())
                        .updateProductionInBuildingPanel();
            }
            return c;
        }

        public boolean isActive() {
            return (getSelectedUnit() != null);
        }

        public void remove(Component comp) {
            if (comp instanceof UnitLabel) {
                Unit unit = ((UnitLabel) comp).getUnit();
                inGameController.leaveShip(unit);

                super.remove(comp);
            } else if (comp instanceof GoodsLabel) {
                Goods g = ((GoodsLabel) comp).getGoods();
                inGameController.unloadCargo(g);
                super.remove(comp);
                colonyPanel.getWarehousePanel().revalidate();
                colonyPanel.getCargoPanel().revalidate();

                // TODO: Make this look prettier :-)
                UnitLabel t = selectedUnit;
                selectedUnit = null;
                setSelectedUnitLabel(t);
            }
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
                    ASingleTilePanel p =
                            new ASingleTilePanel(getColony()
                                    .getColonyTile(x, y), x, y);
                    add(p, new Integer(layer));
                    layer++;
                }
            }
        }

        public void paintComponent(Graphics g) {
            GUI gui = parent.getGUI();
            ImageLibrary lib = parent.getGUI().getImageLibrary();

            g.setColor(Color.black);
            g.fillRect(0, 0, getWidth(), getHeight());

            if (getColony() != null) {
                for (int x = 0; x < 3; x++) {
                    for (int y = 0; y < 3; y++) {
                        gui.displayColonyTile((Graphics2D) g, game.getMap(),
                                getColony().getTile(x, y), ((2 - x) + y)
                                        * lib.getTerrainImageWidth(1) / 2,
                                (x + y) * lib.getTerrainImageHeight(1) / 2,
                                getColony());

                    }
                }
            }
        }


        /**
         * Panel for visualizing a <code>ColonyTile</code>.
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
                    unitLabel.setTransferHandler(defaultTransferHandler);
                    unitLabel.addMouseListener(pressListener);

                    add(unitLabel);

                    updateDescriptionLabel(unitLabel, true);
                }

                ImageLibrary lib = parent.getGUI().getImageLibrary();

                if (colonyTile.isColonyCenterTile()) {
                    int width = lib.getTerrainImageWidth(1) * 2 / 3;

                    ImageIcon goodsIcon =
                            parent.getImageProvider().getGoodsImageIcon(
                                    Goods.FOOD);
                    BufferedImage productionImage =
                            parent.getGUI().createProductionImage(goodsIcon,
                                    colonyTile.getTile().potential(Goods.FOOD),
                                    width, goodsIcon.getIconHeight());
                    JLabel sl =
                            new JLabel(new ImageIcon(productionImage),
                                    SwingConstants.CENTER);
                    sl.setSize(lib.getTerrainImageWidth(1), goodsIcon
                            .getIconHeight());
                    add(sl);

                    if (colonyTile.getTile().potential(
                            colonyTile.getTile().secondaryGoods()) != 0) {
                        goodsIcon =
                                parent.getImageProvider().getGoodsImageIcon(
                                        colonyTile.getTile().secondaryGoods());
                        productionImage =
                                parent.getGUI().createProductionImage(
                                        goodsIcon,
                                        colonyTile.getTile().potential(
                                                colonyTile.getTile()
                                                        .secondaryGoods()),
                                        width, goodsIcon.getIconHeight());
                        sl =
                                new JLabel(new ImageIcon(productionImage),
                                        SwingConstants.CENTER);
                        sl.setSize(lib.getTerrainImageWidth(1), goodsIcon
                                .getIconHeight());
                        add(sl);
                    }
                }

                setTransferHandler(defaultTransferHandler);
                addMouseListener(releaseListener);

                // Size and position:
                setSize(lib.getTerrainImageWidth(1), lib
                        .getTerrainImageHeight(1));
                setLocation(((2 - x) + y) * lib.getTerrainImageWidth(1) / 2,
                        (x + y) * lib.getTerrainImageHeight(1) / 2);

            }

            public void paintComponent(Graphics g) {

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
                        unitDescription =
                                tileDescription + " [" + unitDescription + "]";
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
                        if (colonyTile.getWorkTile().getOwner() != null
                                && colonyTile.getWorkTile().getOwner() != getColony()) {
                            if (colonyTile.getWorkTile().getOwner().getOwner()
                                    .isEuropean()) {
                                parent.errorMessage("tileTakenEuro");
                            } else { // its an indian setttlement
                                parent.errorMessage("tileTakenInd");
                            }
                            return null;
                        }
                        if (colonyTile.getWorkTile().getNationOwner() != Player.NO_NATION
                                && colonyTile.getWorkTile().getNationOwner() != unit
                                        .getOwner().getNation()
                                && !Player.isEuropean(colonyTile.getWorkTile()
                                        .getNationOwner())
                                && !unit.getOwner().hasFather(
                                        FoundingFather.PETER_MINUIT)) {
                            int nation =
                                    colonyTile.getWorkTile().getNationOwner();
                            int price =
                                    game.getPlayer(
                                            colonyTile.getWorkTile()
                                                    .getNationOwner())
                                            .getLandPrice(
                                                    colonyTile.getWorkTile());
                            ChoiceItem[] choices =
                                    {
                                            new ChoiceItem(
                                                    Messages
                                                            .message(
                                                                    "indianLand.pay")
                                                            .replaceAll(
                                                                    "%amount%",
                                                                    Integer
                                                                            .toString(price)),
                                                    1),
                                            new ChoiceItem(
                                                    Messages
                                                            .message("indianLand.take"),
                                                    2) };
                            ChoiceItem ci =
                                    (ChoiceItem) parent
                                            .showChoiceDialog(
                                                    Messages
                                                            .message(
                                                                    "indianLand.text")
                                                            .replaceAll(
                                                                    "%player%",
                                                                    Player
                                                                            .getNationAsString(nation)),
                                                    Messages
                                                            .message("indianLand.cancel"),
                                                    choices);
                            if (ci == null) {
                                return null;
                            } else if (ci.getChoice() == 1) {
                                if (price > freeColClient.getMyPlayer()
                                        .getGold()) {
                                    parent.errorMessage("notEnoughGold");
                                    return null;
                                }

                                inGameController.buyLand(colonyTile
                                        .getWorkTile());
                            }
                        }

                        if (colonyTile.canAdd(unit)) {
                            oldParent.remove(comp);

                            inGameController.work(unit, colonyTile);

                            // check whether worktype is suitable
                            int workType = colonyTile.getWorkType(unit);
                            if (workType != unit.getWorkType()) {
                                inGameController.changeWorkType(unit, workType);
                            }

                            updateDescriptionLabel((UnitLabel) comp, true);

                            updateBuildingBox();
                            updateWarehouse();
                            updateBuildingsPanel();

                            ((UnitLabel) comp).setSmall(false);

                            colonyPanel.updateSoLLabel();
                        } else {
                            return null;
                        }
                    } else {
                        logger
                                .warning("An invalid component got dropped on this CargoPanel.");
                        return null;
                    }
                }

                updateCargoLabel();
                Component c = add(comp);
                refresh();
                if (oldParent != null
                        && oldParent.getParent() instanceof BuildingsPanel.ASingleBuildingPanel) {
                    ((BuildingsPanel.ASingleBuildingPanel) oldParent
                            .getParent()).updateProductionInBuildingPanel();
                }
                return c;
            }

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
                 * Tada. A way of detecting if the x,y is withing the diamond.
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

    /**
     * A combo box that contains a list of all the buildings that can be built
     * in this colony.
     */
    public final class BuildingBox extends JComboBox {

        private final ColonyPanel colonyPanel;

        private final BuildingBoxListener buildingBoxListener;


        /**
         * Creates a new BuildingBox for this Colony.
         * 
         * @param colonyPanel The <code>ColonyPanel</code> this object is
         *            created for.
         */
        public BuildingBox(ColonyPanel colonyPanel) {
            super();
            this.colonyPanel = colonyPanel;

            buildingBoxListener = new BuildingBoxListener(this, colonyPanel);
            super.addActionListener(buildingBoxListener);
        }

        /**
         * Sets up the BuildingBox such that it contains the appropriate
         * buildings.
         */
        public void initialize() {
            super.removeActionListener(buildingBoxListener);
            removeAllItems();
            BuildingBoxItem nothingItem =
                    new BuildingBoxItem(Messages.message("nothing"), -1);
            this.addItem(nothingItem);
            BuildingBoxItem toSelect = nothingItem;
            for (int i = 0; i < Building.NUMBER_OF_TYPES; i++) {
                if (colonyPanel.getColony().getBuilding(i).canBuildNext()) {
                    String theText =
                            new String(getColony().getBuilding(i).getNextName()
                                    + " ("
                                    + Integer.toString(colonyPanel.getColony()
                                            .getBuilding(i).getNextHammers())
                                    + " "
                                    + Messages.message("model.goods.Hammers")
                                            .toLowerCase());

                    if (colonyPanel.getColony().getBuilding(i).getNextTools() > 0) {
                        theText +=
                                ", "
                                        + Integer.toString(getColony()
                                                .getBuilding(i).getNextTools())
                                        + " "
                                        + Messages.message("model.goods.Tools")
                                                .toLowerCase();
                    }

                    theText += ")";
                    BuildingBoxItem nextItem = new BuildingBoxItem(theText, i);
                    this.addItem(nextItem);
                    if (i == colonyPanel.getColony().getCurrentlyBuilding()) {
                        toSelect = nextItem;
                    }
                }
            }

            Iterator<Integer> buildableUnitIterator =
                    colonyPanel.getColony().getBuildableUnitIterator();
            while (buildableUnitIterator.hasNext()) {
                int unitID = buildableUnitIterator.next();

                String theText =
                        new String(Unit.getName(unitID) + " ("
                                + Unit.getNextHammers(unitID) + " "
                                + Messages.message("model.goods.Hammers"));
                if (Unit.getNextTools(unitID) > 0) {
                    theText +=
                            ", " + Integer.toString(Unit.getNextTools(unitID))
                                    + " "
                                    + Messages.message("model.goods.Tools");
                }

                theText += ")";

                int i = unitID + Colony.BUILDING_UNIT_ADDITION;
                BuildingBoxItem uItem = new BuildingBoxItem(theText, i);
                addItem(uItem);
                if (i == colonyPanel.getColony().getCurrentlyBuilding()) {
                    toSelect = uItem;
                }
            }

            this.setSelectedItem(toSelect);
            super.addActionListener(buildingBoxListener);
            colonyPanel.updateProgressLabel();
        }


        /**
         * Represents a type of building, and the text of the next building in
         * that type.
         */
        public final class BuildingBoxItem {
            private final String text;

            private final int type;


            /**
             * Sets up the text and the type.
             * 
             * @param text The text presented to the user for identifying the
             *            item.
             * @param type An <code>int</code> for identifying the item.
             */
            public BuildingBoxItem(String text, int type) {
                this.text = text;
                this.type = type;
            }

            /**
             * Gets the text associated with this item.
             * 
             * @return The text associated with this item.
             */
            public String getText() {
                return text;
            }

            /**
             * Gets the building type associated with that item.
             * 
             * @return The building type associated with this item.
             */
            public int getType() {
                return type;
            }

            public String toString() {
                return getText();
            }
        }

        /**
         * The ActionListener for the BuildingBox.
         */
        public final class BuildingBoxListener implements ActionListener {

            private ColonyPanel colonyPanel;

            private BuildingBox buildingBox;


            /**
             * Sets up this BuildingBoxListener's buildingBox and colonyPanel.
             * 
             * @param buildingBox The <code>BuildingBox</code> to be listening
             *            on.
             * @param colonyPanel The <code>ColonyPanel</code> this object is
             *            created for.
             */
            public BuildingBoxListener(BuildingBox buildingBox,
                    ColonyPanel colonyPanel) {
                super();
                this.buildingBox = buildingBox;
                this.colonyPanel = colonyPanel;
            }

            /**
             * Sets the ColonyPanel's Colony's type of building.
             */
            public void actionPerformed(ActionEvent e) {
                colonyPanel.getClient().getInGameController()
                        .setCurrentlyBuilding(
                                colonyPanel.getColony(),
                                ((BuildingBoxItem) buildingBox
                                        .getSelectedItem()).getType());
                colonyPanel.updateProgressLabel();
            }
        }
    }

}
