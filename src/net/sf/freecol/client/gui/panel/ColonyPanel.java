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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.label.GoodsLabel;
import net.sf.freecol.client.gui.label.ProductionLabel;
import net.sf.freecol.client.gui.label.UnitLabel;
import net.sf.freecol.client.gui.plaf.FreeColLookAndFeel;
import net.sf.freecol.client.gui.tooltip.*;
import net.sf.freecol.client.gui.Size;
import net.sf.freecol.common.debug.DebugUtils;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Colony.ColonyChangeEvent;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.NoClaimReason;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitLocation.NoAddReason;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * This is a panel for the Colony display.  It shows the units that
 * are working in the colony, the buildings and much more.
 * <p>
 * Beware that in debug mode, this might be a server-side version of
 * the colony which is why we need to call getColony().getSpecification()
 * to get the spec that corresponds to the good types in this colony.
 * <p>
 * Panel Layout:
 * <p style="display: block; font-family: monospace; white-space: pre; margin: 1em 0;">
 * |--------------------------------------------------------|
 * | nameBox           |         netProductionPanel         |
 * |--------------------------------------------------------|
 * | tilesPanel        | buildingsPanel                     |
 * |-------------------|                                    |
 * | populationPanel   |                                    |
 * |-------------------|                                    |
 * | constructionPanel |                                    |
 * |--------------------------------------------------------|
 * | inPortPanel     | cargoPanel      | outsideColonyPanel |
 * |--------------------------------------------------------|
 * |                      warehousePanel                    |
 * |--------------------------------------------------------|
 */
public final class ColonyPanel extends PortPanel
    implements PropertyChangeListener {

    private static final Logger logger = Logger.getLogger(ColonyPanel.class.getName());

    /** The height of the area in which autoscrolling should happen. */
    public static final int SCROLL_AREA_HEIGHT = 40;

    /** The speed of the scrolling. */
    public static final int SCROLL_SPEED = 40;

    // Buttons
    private JButton unloadButton
        = Utility.localizedButton("unload");

    private JButton fillButton
        = Utility.localizedButton("load");

    private JButton warehouseButton
        = Utility.localizedButton("colonyPanel.warehouse");

    private JButton buildQueueButton
        = Utility.localizedButton("colonyPanel.buildQueue");

    private JButton colonyUnitsButton
        = Utility.localizedButton("colonyPanel.colonyUnits");

    // Only present in debug mode
    private JButton setGoodsButton = null;
    private JButton traceWorkButton = null;

    /** The {@code Colony} this panel is displaying. */
    private Colony colony;

    // inherit PortPanel.pressListener
    // inherit PortPanel.defaultTransferHandler
    // inherit PortPanel.selectedUnitLabel

    private transient MouseListener releaseListener = null;

    // Subparts
    private final JComboBox<Colony> nameBox = new JComboBox<>();

    private JPanel netProductionPanel = null;

    private JScrollPane buildingsScroll = null;
    private BuildingsPanel buildingsPanel = null;

    private JScrollPane cargoScroll = null;
    // inherit protected PortPanel.cargoPanel

    private ConstructionPanel constructionPanel = null;

    private JScrollPane inPortScroll = null;
    // inherit protected PortPanel.inPortPanel

    private JScrollPane outsideColonyScroll = null;
    private OutsideColonyPanel outsideColonyPanel = null;

    private PopulationPanel populationPanel = null;

    private JScrollPane tilesScroll = null;
    private TilesPanel tilesPanel = null;

    private JScrollPane warehouseScroll = null;
    private WarehousePanel warehousePanel = null;

    // The action commands

    private final ActionListener unloadCmd = ae -> {
        final Unit unit = getSelectedUnit();
        if (unit == null || !unit.isCarrier()) return;
        for (Goods goods : unit.getGoodsList()) {
            igc().unloadCargo(goods, false);
        }
        for (Unit u : unit.getUnitList()) {
            igc().leaveShip(u);
        }
        cargoPanel.update();
        updateOutsideColonyPanel();
        unloadButton.setEnabled(false);
        fillButton.setEnabled(false);
    };

    private final ActionListener warehouseCmd = ae -> {
        if (getGUI().showWarehouseDialog(getColony())) {
            updateWarehousePanel();
        }
    };

    private final ActionListener buildQueueCmd = ae -> {
        getGUI().showBuildQueuePanel(getColony());
        updateConstructionPanel();
    };

    private final ActionListener fillCmd = ae -> {
        final Unit unit = getSelectedUnit();
        if (unit == null || !unit.isCarrier()) return;
        final Colony colony = getColony();
        for (Goods goods : unit.getCompactGoodsList()) {
            final GoodsType type = goods.getType();
            int space = unit.getLoadableAmount(type);
            int count = colony.getGoodsCount(type);
            if (space > 0 && count > 0) {
                int n = Math.min(space, count);
                igc().loadCargo(new Goods(goods.getGame(), colony, type, n),
                                unit);
            }
        }
    };

    private final ActionListener colonyUnitsCmd =
        ae -> generateColonyUnitsMenu();

    private final ActionListener setGoodsCmd = ae -> {
        DebugUtils.setColonyGoods(getFreeColClient(), colony);
        updateWarehousePanel();
        updateProduction();
    };

    private final ActionListener occupationCmd =
        ae -> colony.setOccupationTrace(!colony.getOccupationTrace());


    /**
     * The constructor for the panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param colony The {@code Colony} to display in this panel.
     */
    public ColonyPanel(FreeColClient freeColClient, Colony colony) {
        super(freeColClient,
            new MigLayout("fill, wrap 2, insets 2",
                          "[390!][fill]",
                          "[growprio 100,shrinkprio 10][]0[]0[]"
                          + "[growprio 150,shrinkprio 50]"
                          + "[growprio 150,shrinkprio 50][]"));

        final Player player = getMyPlayer();
        // Do not just use colony.getOwner() == getMyPlayer() because
        // in debug mode we are in the *server* colony, and the equality
        // will fail.
        editable = colony.getOwner().getId().equals(player.getId());

        // Only enable the set goods button in debug mode when not spying
        if (editable
            && FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)) {
            setGoodsButton = Utility.localizedButton("colonyPanel.setGoods");
            traceWorkButton = Utility.localizedButton("colonyPanel.traceWork");
        }

        // Use ESCAPE for closing the ColonyPanel:
        InputMap closeIM = new ComponentInputMap(okButton);
        closeIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false),
                    "pressed");
        closeIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true),
                    "released");
        SwingUtilities.replaceUIInputMap(okButton,
            JComponent.WHEN_IN_FOCUSED_WINDOW, closeIM);
        okButton.setText(Messages.message("close"));

        InputMap unloadIM = new ComponentInputMap(unloadButton);
        unloadIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0, false),
                     "pressed");
        unloadIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0, true),
                     "released");
        SwingUtilities.replaceUIInputMap(unloadButton,
            JComponent.WHEN_IN_FOCUSED_WINDOW, unloadIM);

        InputMap fillIM = new ComponentInputMap(fillButton);
        fillIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0, false),
                   "pressed");
        fillIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0, true),
                   "released");
        SwingUtilities.replaceUIInputMap(fillButton,
            JComponent.WHEN_IN_FOCUSED_WINDOW, fillIM);

        InputMap warehouseIM = new ComponentInputMap(warehouseButton);
        warehouseIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, false),
                        "pressed");
        warehouseIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, true),
                        "released");
        SwingUtilities.replaceUIInputMap(warehouseButton,
            JComponent.WHEN_IN_FOCUSED_WINDOW, warehouseIM);

        InputMap buildQueueIM = new ComponentInputMap(buildQueueButton);
        buildQueueIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0, false),
                         "pressed");
        buildQueueIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0, true),
                         "released");
        SwingUtilities.replaceUIInputMap(buildQueueButton,
            JComponent.WHEN_IN_FOCUSED_WINDOW, buildQueueIM);

        InputMap colonyUnitsIM = new ComponentInputMap(colonyUnitsButton);
        colonyUnitsIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0, false),
                          "pressed");
        colonyUnitsIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0, true),
                          "released");
        SwingUtilities.replaceUIInputMap(colonyUnitsButton,
            JComponent.WHEN_IN_FOCUSED_WINDOW, colonyUnitsIM);

        defaultTransferHandler
            = new DefaultTransferHandler(freeColClient, this);
        pressListener = new DragListener(freeColClient, this);
        releaseListener = new DropListener();
        selectedUnitLabel = null;

        // Make the colony label
        StringBuilder sb = new StringBuilder(32);
        String compat = colony.getName();
        if (editable) {
            for (Colony c : player.getColonyList()) {
                this.nameBox.addItem(c);
                sb.append(c.getName());
            }
        } else {
            this.nameBox.addItem(colony);
            sb.append(colony.getName());
        }
        Font nameBoxFont = FontLibrary.getUnscaledFont("header-plain-big",
                                                       compat);
        this.nameBox.setFont(nameBoxFont);
        this.nameBox.setSelectedItem(colony);
        this.nameBox.getInputMap().put(KeyStroke.getKeyStroke("LEFT"),
                                       "selectPrevious2");
        this.nameBox.getInputMap().put(KeyStroke.getKeyStroke("RIGHT"),
                                       "selectNext2");

        netProductionPanel = new JPanel();
        netProductionPanel.setOpaque(false);

        buildingsPanel = new BuildingsPanel();
        buildingsScroll = new JScrollPane(buildingsPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        buildingsScroll.getVerticalScrollBar().setUnitIncrement(16);
        buildingsScroll.getViewport().setOpaque(false);
        buildingsPanel.setOpaque(false);
        buildingsScroll.setBorder(Utility.ETCHED_BORDER);

        cargoPanel = new ColonyCargoPanel(freeColClient);
        cargoScroll = new JScrollPane(cargoPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        cargoScroll.setBorder(Utility.ETCHED_BORDER);

        constructionPanel = new ConstructionPanel(freeColClient, colony, true);

        inPortPanel = new ColonyInPortPanel();
        inPortScroll = new JScrollPane(inPortPanel);
        inPortScroll.getVerticalScrollBar().setUnitIncrement(16);
        inPortScroll.setBorder(Utility.ETCHED_BORDER);

        outsideColonyPanel = new OutsideColonyPanel();
        outsideColonyScroll = new JScrollPane(outsideColonyPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        outsideColonyScroll.getVerticalScrollBar().setUnitIncrement(16);
        outsideColonyScroll.setBorder(Utility.ETCHED_BORDER);

        populationPanel = new PopulationPanel();

        tilesPanel = new TilesPanel();
        tilesScroll = new JScrollPane(tilesPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tilesScroll.setBorder(Utility.BEVEL_BORDER);

        warehousePanel = new WarehousePanel();
        warehouseScroll = new JScrollPane(warehousePanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        warehouseScroll.setBorder(Utility.ETCHED_BORDER);

        InputMap nameIM = new ComponentInputMap(this.nameBox);
        nameIM.put(KeyStroke.getKeyStroke("LEFT"), "selectPrevious2");
        nameIM.put(KeyStroke.getKeyStroke("RIGHT"), "selectNext2");
        SwingUtilities.replaceUIInputMap(this.nameBox,
            JComponent.WHEN_IN_FOCUSED_WINDOW, nameIM);

        initialize(colony);
        getGUI().restoreSavedSize(this, new Dimension(1050, 725));
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
     * Initialize the entire panel.
     *
     * We can arrive here normally when a colony panel is created,
     * or when an existing colony panel is changed via the colony name
     * menu in the nameBox.
     *
     * @param colony The {@code Colony} to be displayed.
     */
    private void initialize(Colony colony) {
        cleanup();

        setColony(colony);

        addPropertyChangeListeners();
        addMouseListeners();
        setTransferHandlers(isEditable());

        // Enable/disable widgets
        unloadButton.addActionListener(unloadCmd);
        fillButton.addActionListener(fillCmd);
        warehouseButton.addActionListener(warehouseCmd);
        buildQueueButton.addActionListener(buildQueueCmd);
        colonyUnitsButton.addActionListener(colonyUnitsCmd);
        if (setGoodsButton != null) {
            setGoodsButton.addActionListener(setGoodsCmd);
        }
        if (traceWorkButton != null) {
            traceWorkButton.addActionListener(occupationCmd);
        }

        unloadButton.setEnabled(isEditable());
        fillButton.setEnabled(isEditable());
        warehouseButton.setEnabled(isEditable());
        buildQueueButton.setEnabled(isEditable());
        colonyUnitsButton.setEnabled(isEditable());
        if (setGoodsButton != null) {
            setGoodsButton.setEnabled(isEditable());
        }
        if (traceWorkButton != null) {
            traceWorkButton.setEnabled(isEditable());
        }

        final GUI gui = getGUI();
        this.nameBox.setEnabled(isEditable());
        this.nameBox.addActionListener((ActionEvent ae) -> {
                final Colony newColony = (Colony)nameBox.getSelectedItem();
                closeColonyPanel();
                gui.showColonyPanel(newColony, null);
            });
        updateNetProductionPanel();

        buildingsPanel.initialize();
        cargoPanel.initialize();
        constructionPanel.initialize();
        inPortPanel.initialize();
        outsideColonyPanel.initialize();
        populationPanel.initialize();
        tilesPanel.initialize();
        warehousePanel.initialize();

        add(this.nameBox, "height 42:, grow");
        int tmp = ImageLibrary.ICON_SIZE.height;
        add(netProductionPanel,
            "grow, height " + (tmp+10) + ":" + (2*tmp+10) + ":" + (2*tmp+10));
        add(tilesScroll, "width 390!, height 200!, top");
        add(buildingsScroll, "span 1 3, grow");
        add(populationPanel, "grow");
        add(constructionPanel, "grow, top");
        add(inPortScroll, "span, split 3, grow, sg, height 60:121:");
        add(cargoScroll, "grow, sg, height 60:121:");
        add(outsideColonyScroll, "grow, sg, height 60:121:");
        add(warehouseScroll, "span, height 40:60:, growx");
        int buttonFields = 6;
        if (setGoodsButton != null) buttonFields++;
        if (traceWorkButton != null) buttonFields++;
        add(unloadButton, "span, split " + Integer.toString(buttonFields)
            + ", align center");
        add(fillButton);
        add(warehouseButton);
        add(buildQueueButton);
        add(colonyUnitsButton);
        if (setGoodsButton != null) add(setGoodsButton);
        if (traceWorkButton != null) add(traceWorkButton);
        add(okButton, "tag ok");

        update();
    }

    /**
     * Clean up this colony panel.
     */
    private void cleanup() {
        unloadButton.removeActionListener(unloadCmd);
        fillButton.removeActionListener(fillCmd);
        warehouseButton.removeActionListener(warehouseCmd);
        buildQueueButton.removeActionListener(buildQueueCmd);
        colonyUnitsButton.removeActionListener(colonyUnitsCmd);
        if (setGoodsButton != null) {
            setGoodsButton.removeActionListener(setGoodsCmd);
        }
        if (traceWorkButton != null) {
            traceWorkButton.removeActionListener(occupationCmd);
        }

        removePropertyChangeListeners();
        if (getSelectedUnit() != null) {
            getSelectedUnit().removePropertyChangeListener(this);
        }
        removeMouseListeners();
        for (MouseListener listener : getMouseListeners()) {
            removeMouseListener(listener);
        }
        setTransferHandlers(false);

        buildingsPanel.cleanup();
        cargoPanel.cleanup();
        constructionPanel.cleanup();
        inPortPanel.cleanup();
        outsideColonyPanel.cleanup();
        populationPanel.cleanup();
        tilesPanel.cleanup();
        warehousePanel.cleanup();

        removeAll();
    }

    private void addMouseListeners() {
        if (isEditable()) {
            cargoPanel.addMouseListener(releaseListener);
            inPortPanel.addMouseListener(releaseListener);
            outsideColonyPanel.addMouseListener(releaseListener);
            warehousePanel.addMouseListener(releaseListener);
        }
    }

    private void removeMouseListeners() {
        cargoPanel.removeMouseListener(releaseListener);
        inPortPanel.removeMouseListener(releaseListener);
        outsideColonyPanel.removeMouseListener(releaseListener);
        warehousePanel.removeMouseListener(releaseListener);
    }

    private void setTransferHandlers(boolean enable) {
        DefaultTransferHandler dth = (enable) ? defaultTransferHandler : null;
        cargoPanel.setTransferHandler(dth);
        inPortPanel.setTransferHandler(dth);
        outsideColonyPanel.setTransferHandler(dth);
        warehousePanel.setTransferHandler(dth);
    }

    /**
     * Add property change listeners needed by this ColonyPanel.
     */
    private void addPropertyChangeListeners() {
        final Colony colony = getColony();
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
        final Colony colony = getColony();
        if (colony != null) {
            colony.removePropertyChangeListener(this);
            colony.getGoodsContainer().removePropertyChangeListener(this);
            colony.getTile().removePropertyChangeListener(this);
        }
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
        final Colony colony = getColony();
        if (colony == null) return;

        // Check for non-producing locations that can now produce.
        for (WorkLocation wl : colony.getCurrentWorkLocationsList()) {
            boolean change = false, check = wl.getProductionType() == null;
            for (Unit unit : transform(wl.getUnits(), u ->
                    (check || !wl.produces(u.getWorkType())))) {
                GoodsType workType = wl.getWorkFor(unit);
                if (workType != null && workType != unit.getWorkType()) {
                    change |= igc().changeWorkType(unit, workType);
                }
            }
            if (change) wl.updateProductionType();
        }

        updateTilesPanel();
        updateBuildingsPanel();
        updateNetProductionPanel();
        updateConstructionPanel();
    }

    /**
     * Update the entire colony panel.
     */
    public void update() {
        buildingsPanel.update();
        constructionPanel.update();
        inPortPanel.update();
        updateNetProductionPanel();
        outsideColonyPanel.update();
        populationPanel.update();
        tilesPanel.update();
        warehousePanel.update();
    }

    /**
     * Enables the unload and fill buttons if the currently selected unit is a
     * carrier with some cargo.
     */
    private void updateCarrierButtons() {
        final Colony colony = getColony();
        unloadButton.setEnabled(false);
        fillButton.setEnabled(false);
        if (selectedUnitLabel != null && isEditable()) {
            Unit unit = selectedUnitLabel.getUnit();
            if (unit != null && unit.isCarrier() && unit.hasCargo()) {
                unloadButton.setEnabled(true);
                for (Goods goods : unit.getCompactGoodsList()) {
                    if (colony.getGoodsCount(goods.getType()) > 0) {
                        fillButton.setEnabled(true);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Generates a menu containing the units currently accessible
     * from the Colony Panel allowing keyboard access to said units.
     */
    private void generateColonyUnitsMenu() {
        final FreeColClient freeColClient = getFreeColClient();
        ImageLibrary lib = getImageLibrary();
        final Colony colony = getColony();
        JPopupMenu colonyUnitsMenu
            = new JPopupMenu(Messages.message("colonyPanel.colonyUnits"));
        ImageIcon unitIcon = null;
        final QuickActionMenu unitMenu
            = new QuickActionMenu(freeColClient, this);
        Tile colonyTile = colony.getTile();
        int unitNumber = 0;
        JMenuItem subMenu = null;

        for (final Unit unit : colony.getUnitList()) {
            WorkLocation wl = unit.getWorkLocation();
            GoodsType goodsType = unit.getWorkType();
            Unit student = unit.getStudent();

            unitIcon = new ImageIcon(lib.getSmallerUnitImage(unit));
            StringBuilder sb = new StringBuilder(64);
            String prodLabel = Messages.message("colonyPanel.producing");
            if (student != null) {
                sb.append(unit.getDescription())
                    .append(' ').append(prodLabel).append(' ')
                    .append(Messages.getName(unit.getType()
                            .getSkillTaught()))
                    .append(' ')
                    .append(unit.getTurnsOfTraining())
                    .append('/')
                    .append(unit.getNeededTurnsOfTraining());
            } else if (wl != null && goodsType != null) {
                int producing = wl.getProductionOf(unit, goodsType);
                sb.append(unit.getDescription())
                    .append(' ').append(prodLabel).append(' ')
                    .append(producing)
                    .append(' ')
                    .append(Messages.message(StringTemplate.template(goodsType)
                            .addAmount("%amount%", producing)));
            } else {
                sb.append(unit.getDescription())
                    .append(' ').append(prodLabel).append(' ')
                    .append(Messages.message("nothing"));
            }
            String menuTitle = sb.toString();
            subMenu = new JMenuItem(menuTitle, unitIcon);
            subMenu.addActionListener((ActionEvent ae) -> {
                    unitMenu.addMenuItems(new UnitLabel(freeColClient, unit));
                    getGUI().showPopupMenu(unitMenu, 0, 0);
                });
            unitNumber++;
            colonyUnitsMenu.add(subMenu);
        }
        colonyUnitsMenu.addSeparator();
        for (final Unit unit : colonyTile.getUnitList()) {
            if (unit.isCarrier()) {
                unitIcon = new ImageIcon(lib.getSmallerUnitImage(unit));
                String menuTitle = unit.getDescription()
                    + " " + Messages.message("colonyPanel.inPort");
                subMenu = new JMenuItem(menuTitle, unitIcon);
                subMenu.addActionListener((ActionEvent ae) -> {
                        unitMenu.addMenuItems(new UnitLabel(freeColClient, unit));
                        getGUI().showPopupMenu(unitMenu, 0, 0);
                    });
                unitNumber++;
                colonyUnitsMenu.add(subMenu);
                for (final Unit innerUnit : unit.getUnitList()) {
                    unitIcon = new ImageIcon(lib.getSmallerUnitImage(innerUnit));
                    menuTitle = innerUnit.getDescription()
                        + " " + Messages.message("cargoOnCarrier")
                        + " " + unit.getDescription();
                    subMenu = new JMenuItem(menuTitle, unitIcon);
                    subMenu.addActionListener((ActionEvent ae) -> {
                            unitMenu.addMenuItems(new UnitLabel(freeColClient, innerUnit));
                            getGUI().showPopupMenu(unitMenu, 0, 0);
                        });
                    unitNumber++;
                    colonyUnitsMenu.add(subMenu);
                }
            } else if (!unit.isOnCarrier()) {
                unitIcon = new ImageIcon(lib.getSmallerUnitImage(unit));
                String menuTitle = unit.getDescription()
                    + " " + Messages.message("colonyPanel.outsideColony");
                subMenu = new JMenuItem(menuTitle, unitIcon);
                subMenu.addActionListener((ActionEvent ae) -> {
                        unitMenu.addMenuItems(new UnitLabel(freeColClient, unit));
                        getGUI().showPopupMenu(unitMenu, 0, 0);
                    });
                unitNumber++;
                colonyUnitsMenu.add(subMenu);
            }
        }
        colonyUnitsMenu.addSeparator();
        int elements = colonyUnitsMenu.getSubElements().length;
        if (elements > 0) {
            int lastIndex = colonyUnitsMenu.getComponentCount() - 1;
            if (colonyUnitsMenu.getComponent(lastIndex) instanceof JPopupMenu.Separator) {
                colonyUnitsMenu.remove(lastIndex);
            }
        }
        getGUI().showPopupMenu(colonyUnitsMenu, 0, 0);
    }

    /**
     * Try to assign a unit to work in a colony work location.
     *
     * @param unit The {@code Unit} that is to work.
     * @param wl The {@code WorkLocation} to work in.
     * @return True if the unit is now in the work location.
     */
    private boolean tryWork(Unit unit, WorkLocation wl) {
        // Choose the best work type.
        GoodsType workType = wl.getWorkFor(unit);

        // Set the unit to work.  Note this might upgrade the unit,
        // and possibly even change its work type as the server has
        // the right to maintain consistency.
        igc().work(unit, wl);

        // Did it work?
        if (unit.getLocation() != wl) return false;

        // Now recheck, and see if we want to change to the expected
        // work type.
        if (workType != null && workType != unit.getWorkType()) {
            igc().changeWorkType(unit, workType);
        }
        return true;
    }


    // Public base accessors and mutators

    /**
     * Gets the {@code Colony} in use.
     *
     * Try to use this at the top of all the routines that need the colony,
     * to get a stable value.  There have been nasty races when the colony
     * changes out from underneath us, and more may be lurking.
     *
     * @return The {@code Colony}.
     */
    public final synchronized Colony getColony() {
        return colony;
    }

    /**
     * Gets the {@code TilesPanel}-object in use.
     *
     * @return The {@code TilesPanel}.
     */
    public final TilesPanel getTilesPanel() {
        return tilesPanel;
    }

    /**
     * Gets the {@code WarehousePanel}-object in use.
     *
     * @return The {@code WarehousePanel}.
     */
    public final WarehousePanel getWarehousePanel() {
        return warehousePanel;
    }


    // Override PortPanel

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSelectedUnitLabel(UnitLabel unitLabel) {
        if (selectedUnitLabel != unitLabel) {
            inPortPanel.removePropertyChangeListeners();
            if (selectedUnitLabel != null) {
                selectedUnitLabel.setSelected(false);
                selectedUnitLabel.getUnit().removePropertyChangeListener(this);
            }
            super.setSelectedUnitLabel(unitLabel);
            if (unitLabel == null) {
                cargoPanel.setCarrier(null);
            } else {
                cargoPanel.setCarrier(unitLabel.getUnit());
                unitLabel.setSelected(true);
                unitLabel.getUnit().addPropertyChangeListener(this);
            }
            inPortPanel.addPropertyChangeListeners();
        }
        updateCarrierButtons();
        inPortPanel.revalidate();
        inPortPanel.repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setSelectedUnit(Unit unit) {
        return ((unit.isCarrier()) ? inPortPanel.setSelectedUnit(unit)
            : super.setSelectedUnit(unit));
    }


    // Update routines

    private void updateBuildingsPanel() {
        buildingsPanel.update();
    }

    private void updateConstructionPanel() {
        constructionPanel.update();
    }

    private void updateInPortPanel() {
        inPortPanel.update();
    }

    private void updateNetProductionPanel() {
        final FreeColClient freeColClient = getFreeColClient();
        final Colony colony = getColony();
        final Specification spec = colony.getSpecification();
        // FIXME: find out why the cache needs to be explicitly invalidated
        colony.invalidateCache();

        netProductionPanel.removeAll();
        for (GoodsType goodsType : spec.getGoodsTypeList()) {
            int amount = colony.getAdjustedNetProductionOf(goodsType);
            if (amount != 0) {
                AbstractGoods ag = new AbstractGoods(goodsType, amount);
                netProductionPanel.add(new ProductionLabel(freeColClient, ag));
            }
        }
        netProductionPanel.revalidate();
        netProductionPanel.repaint();
    }

    private void updateOutsideColonyPanel() {
        outsideColonyPanel.update();
    }

    private void updatePopulationPanel() {
        populationPanel.update();
    }

    private void updateTilesPanel() {
        tilesPanel.update();
    }

    private void updateWarehousePanel() {
        warehousePanel.update();
    }


    /**
     * Close this {@code ColonyPanel}.
     */
    public void closeColonyPanel() {
        final Colony colony = getColony();
        final Player player = getMyPlayer();
        boolean abandon = false;
        BuildableType buildable;
        if (player.owns(colony)) {
            if (colony.getUnitCount() == 0) {
                final String key = (player.isRebel()
                    && player.getNumberOfPorts() == 1
                    && colony.isConnectedPort())
                    ? "abandonColony.lastPort.text"
                    : "abandonColony.text";
                if (!getGUI().confirm(null, StringTemplate.key(key), colony,
                        "abandonColony.yes", "abandonColony.no")) return;
                abandon = true;
            } else if ((buildable = colony.getCurrentlyBuilding()) != null) {
                int required = buildable.getRequiredPopulation();
                if (required > colony.getUnitCount()
                    && !getGUI().confirm(null, StringTemplate
                        .template("colonyPanel.reducePopulation")
                            .addName("%colony%", colony.getName())
                            .addAmount("%number%", required)
                            .addNamed("%buildable%", buildable),
                        colony, "ok", "cancel")) return;
            }
        }

        cleanup();
        getGUI().removeComponent(this);
        // Abandon colony and active unit handling is in IGC
        igc().closeColony(colony, abandon);
    }


    // Interface PortPanel

    /**
     * Gets the list of units on the colony tile.
     * Note, does not include the units *inside* the colony.
     *
     * @return A sorted list of units on the colony tile.
     */
    @Override
    public List<Unit> getUnitList() {
        return sort(colony.getTile().getUnits());
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        if (OK.equals(ae.getActionCommand())) {
            closeColonyPanel();
        } else {
            super.actionPerformed(ae);
        }
    }


    // Interface PropertyChangeListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        final Colony colony = getColony();
        if (!isShowing() || colony == null) return;
        String property = event.getPropertyName();
        logger.finest(colony.getName() + " change " + property
                      + ": " + event.getOldValue()
                      + " -> " + event.getNewValue());

        if (property == null) {
            logger.warning("Null property change");
        } else if (Unit.CARGO_CHANGE.equals(property)) {
            cargoPanel.update();
            updateInPortPanel();
        } else if (ColonyChangeEvent.POPULATION_CHANGE.toString().equals(property)) {
            updatePopulationPanel();
            updateNetProductionPanel(); // food production changes
        } else if (ColonyChangeEvent.BONUS_CHANGE.toString().equals(property)) {
            if (colony.getUnitCount() > 0) { // Ignore messages when abandoning
                ModelMessage msg = colony.checkForGovMgtChangeMessage();
                if (msg != null) {
                    getGUI().showInformationPanel(colony, msg);
                }
            }
            updatePopulationPanel();
        } else if (ColonyChangeEvent.BUILD_QUEUE_CHANGE.toString().equals(property)) {
            updateProduction();
        } else if (ColonyChangeEvent.UNIT_TYPE_CHANGE.toString().equals(property)) {
            FreeColGameObject object = (FreeColGameObject)event.getSource();
            UnitType oldType = (UnitType) event.getOldValue();
            UnitType newType = (UnitType) event.getNewValue();
            getGUI().showInformationPanel(object, StringTemplate
                .template("colonyPanel.unitChange")
                .addName("%colony%", colony.getName())
                .addNamed("%oldType%", oldType)
                .addNamed("%newType%", newType));
            updateTilesPanel();
        } else if (property.startsWith("model.goods.")) {
            // Changes to warehouse goods count may affect building production
            // which requires a view update.
            updateWarehousePanel();
            updateProduction();
        } else if (Tile.UNIT_CHANGE.equals(property)) {
            updateOutsideColonyPanel();
            updateInPortPanel();
            updatePopulationPanel();
            updateWarehousePanel();// Role change changes equipment goods
        } else if (Unit.MOVE_CHANGE.equals(property)) {
            updateOutsideColonyPanel();
        } else {
            // ColonyTiles and Buildings now have their own
            // propertyChangeListeners so {ColonyTile,Building}.UNIT_CHANGE
            // events should not arrive here.
            logger.warning("Unknown property change event: "
                           + event.getPropertyName());
        }
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        if (colony == null) return; // Been here already
        colony.setOccupationTrace(false);
        colony = null; // Now Canvas.getColonyPanel will no longer find this

        super.removeNotify();

        // Alas, ColonyPanel is often leaky.
        unloadButton = null;
        fillButton = null;
        warehouseButton = null;
        buildQueueButton = null;
        colonyUnitsButton = null;
        setGoodsButton = null;
        traceWorkButton = null;
        netProductionPanel = null;
        buildingsPanel = null;
        buildingsScroll = null;
        cargoScroll = null;
        constructionPanel = null;
        inPortScroll = null;
        outsideColonyPanel = null;
        outsideColonyScroll = null;
        populationPanel = null;
        tilesPanel = null;
        tilesScroll = null;
        warehousePanel = null;
        warehouseScroll = null;

        releaseListener = null;

        // Inherited from PortPanel
        //cargoPanel = null;
        //inPortPanel = null;
        //defaultTransferHandler = null;
        //pressListener = null;
        //selectedUnitLabel = null;
    }


    // Subpanel classes

    /**
     * This panel shows the content of a carrier in the colony
     */
    public final class ColonyCargoPanel extends CargoPanel {

        /**
         * Create this colony cargo panel.
         *
         * @param freeColClient The {@code FreeColClient} for the game.
         */
        public ColonyCargoPanel(FreeColClient freeColClient) {
            super(freeColClient, true);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void update() {
            super.update();
            // May have un/loaded cargo, "Unload" could have changed validity
            updateCarrierButtons();
            updatePopulationPanel();
        }
    }

    /**
     * The panel to display the population breakdown for this colony.
     */
    public final class PopulationPanel extends MigPanel {

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
            super("PopulationPanelUI",
                  new MigLayout("wrap 5, fill, insets 0",
                                "[][]:push[center]:push[right][]"));

            setOpaque(false);
            setToolTipText(" ");
        }


        /**
         * Initialize this population panel.
         */
        public void initialize() {
            cleanup();
            update();
        }

        /**
         * Clean up this population panel.
         */
        public void cleanup() {
            // Nothing yet
        }

        /**
         * Update this population panel.
         */
        public void update() {
            final Colony colony = getColony();
            if (colony == null) return;
            final ImageLibrary lib = getImageLibrary();
            final Font font = FontLibrary.getUnscaledFont("normal-plain-smaller");
            final int uc = colony.getUnitCount();
            final int solPercent = colony.getSoL();
            final int rebels = Colony.calculateRebels(uc, solPercent);
            final Nation nation = colony.getOwner().getNation();
            final int grow = colony.getPreferredSizeChange();
            final int bonus = colony.getProductionBonus();
            StringTemplate t;

            removeAll();

            rebelShield.setIcon(new ImageIcon(lib.getSmallerNationImage(nation)));
            add(rebelShield, "bottom");

            t = StringTemplate.template("colonyPanel.rebelLabel")
                              .addAmount("%number%", rebels);
            rebelLabel.setText(Messages.message(t));
            rebelLabel.setFont(font);
            add(rebelLabel, "split 2, flowy");

            rebelMemberLabel.setText(solPercent + "%");
            rebelMemberLabel.setFont(font);
            add(rebelMemberLabel);

            t = StringTemplate.template("colonyPanel.populationLabel")
                              .addAmount("%number%", uc);
            popLabel.setText(Messages.message(t));
            popLabel.setFont(font);
            add(popLabel, "split 2, flowy");

            String growth = (grow == 0) ? ""
                : '(' + ((grow >= Colony.CHANGE_UPPER_BOUND)
                    ? Messages.message("many")
                    : String.valueOf(grow)) + ')';
            t = StringTemplate.template("colonyPanel.bonusLabel")
                              .addAmount("%number%", bonus)
                              .addName("%extra%", growth);
            bonusLabel.setText(Messages.message(t));
            bonusLabel.setFont(font);
            add(bonusLabel);

            t = StringTemplate.template("colonyPanel.royalistLabel")
                              .addAmount("%number%", uc - rebels);
            royalistLabel.setText(Messages.message(t));
            royalistLabel.setFont(font);
            add(royalistLabel, "split 2, flowy");

            royalistMemberLabel.setText(colony.getTory() + "%");
            royalistMemberLabel.setFont(font);
            add(royalistMemberLabel);

            Nation other = (colony.getOwner().isREF())
                ? nation.getRebelNation()
                : nation.getREFNation();
            try {
                royalistShield.setIcon(new ImageIcon(lib.getSmallerNationImage(other)));
                add(royalistShield, "bottom");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Shield: " + nation + "/" + other, e);
            }

            revalidate();
            repaint();
        }

        /**
         * Generates the {@code JToolTip} with the colony's
         * detailed population information.
         *
         * @return The JToolTip created.
         */
        @Override
        public JToolTip createToolTip() {
            JToolTip toolTip = new RebelToolTip(getFreeColClient(), getColony());
            toolTip.setPreferredSize(new Dimension(400, 185));
            return toolTip;
        }


        // Override Component

        /**
         * {@inheritDoc}
         */
        @Override
        public void removeNotify() {
            super.removeNotify();

            removeAll();
            setLayout(null);
        }
    }

    /**
     * A panel that holds UnitLabels that represent Units that are standing in
     * front of a colony.
     */
    public final class OutsideColonyPanel extends UnitPanel
        implements DropTarget {

        /**
         * Create this OutsideColonyPanel.
         */
        public OutsideColonyPanel() {
            super("OutsideColonyPanelUI",
                  new MigLayout("wrap 3, fill, insets 0"), ColonyPanel.this,
                  null, ColonyPanel.this.isEditable());

            setBorder(Utility.localizedBorder("colonyPanel.outsideColony"));
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void initialize() {
            super.initialize();

            final Colony colony = getColony();
            if (colony != null) {
                setName(colony.getName() + " - " + Messages.message("port"));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void cleanup() {
            super.cleanup();

            removeAll();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void addPropertyChangeListeners() {
            final Colony colony = getColony();
            if (colony != null) {
                colony.getTile().addPropertyChangeListener(Tile.UNIT_CHANGE,
                                                           this);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void removePropertyChangeListeners() {
            final Colony colony = getColony();
            if (colony != null) {
                colony.getTile().removePropertyChangeListener(Tile.UNIT_CHANGE,
                                                              this);
            }
        }

        // Inherit UnitPanel.update


        // Interface DropTarget

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accepts(Goods goods) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accepts(GoodsType goodsType) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accepts(Unit unit) {
            return !unit.isCarrier();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void selectLabel() {
            // do nothing
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public Component add(Component comp, boolean editState) {
            Container oldParent = comp.getParent();
            if (editState) {
                if (comp instanceof UnitLabel) {
                    UnitLabel unitLabel = (UnitLabel)comp;
                    Unit unit = unitLabel.getUnit();

                    if (!unit.isOnCarrier()) {
                        igc().putOutsideColony(unit);
                    }

                    if (unit.getColony() == null) {
                        closeColonyPanel();
                        return null;
                    } else if (!unit.isOnTile() && !unit.isOnCarrier()) {
                        return null;
                    }

                    oldParent.remove(comp);
                    initialize();
                    return comp;
                } else {
                    logger.warning("Invalid component: " + comp);
                    return null;
                }
            } else {
                ((UnitLabel)comp).setSmall(false);
                return add(comp);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int suggested(GoodsType type) { return -1; } // N/A
    }

    /**
     * A panel that holds UnitLabels that represent naval Units that are
     * waiting in the port of the colony.
     */
    public final class ColonyInPortPanel extends InPortPanel {

        /**
         * Creates this ColonyInPortPanel.
         */
        public ColonyInPortPanel() {
            super(new MigLayout("wrap 3, fill, insets 0"), ColonyPanel.this,
                  null, ColonyPanel.this.isEditable());

            setBorder(Utility.localizedBorder("colonyPanel.inPort"));
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void initialize() {
            super.initialize();

            final Colony colony = getColony();
            if (colony != null) {
                setName(colony.getName() + " - " + Messages.message("port"));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void addPropertyChangeListeners() {
            Unit selected = getSelectedUnit();
            if (selected != null) {
                selected.addPropertyChangeListener(Unit.CARGO_CHANGE, this);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void removePropertyChangeListeners() {
            Unit selected = getSelectedUnit();
            if (selected != null) {
                selected.removePropertyChangeListener(Unit.CARGO_CHANGE, this);
            }
        }


        // extend UnitPanel

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accepts(Unit unit) {
            return unit.isCarrier();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void selectLabel() {
            removePropertyChangeListeners();
            super.selectLabel();
            addPropertyChangeListeners();
        }
    }

    /**
     * A panel that holds goods that represent cargo that is inside
     * the Colony.
     */
    public final class WarehousePanel extends MigPanel
        implements DropTarget, PropertyChangeListener {

        /**
         * Creates a WarehousePanel.
         */
        public WarehousePanel() {
            super("WarehousePanelUI",
                  new MigLayout("fill, gap push, insets 0"));
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
         * Clean up this WarehousePanel.
         */
        public void cleanup() {
            removePropertyChangeListeners();
            removeAll();
        }

        /**
         * Add the property change listeners to this panel.
         */
        private void addPropertyChangeListeners() {
            final Colony colony = getColony();
            if (colony != null) {
                colony.getGoodsContainer().addPropertyChangeListener(this);
            }
        }

        /**
         * Remove the property change listeners from this panel.
         */
        private void removePropertyChangeListeners() {
            final Colony colony = getColony();
            if (colony != null) {
                colony.getGoodsContainer().removePropertyChangeListener(this);
            }
        }

        /**
         * Update this WarehousePanel.
         */
        public void update() {
            final Colony colony = getColony();
            if (colony == null) return;
            removeAll();

            ClientOptions options = getClientOptions();
            final int threshold = (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS))
                ? 1
                : options.getInteger(ClientOptions.MIN_NUMBER_FOR_DISPLAYING_GOODS);
            final Game game = colony.getGame();
            final Specification spec = colony.getSpecification();
            for (GoodsType goodsType : spec.getStorableGoodsTypeList()) {
                int count = colony.getGoodsCount(goodsType);
                if (count >= threshold) {
                    Goods goods = new Goods(game, colony, goodsType, count);
                    GoodsLabel goodsLabel
                        = new GoodsLabel(getFreeColClient(), goods);
                    if (ColonyPanel.this.isEditable()) {
                        goodsLabel.setTransferHandler(defaultTransferHandler);
                        goodsLabel.addMouseListener(pressListener);
                    }
                    add(goodsLabel, "alignx center");
                }
            }
            ColonyPanel.this.updateProduction();
            revalidate();
            repaint();
        }


        // Interface DropTarget

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accepts(Goods goods) {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accepts(GoodsType goodsType) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accepts(Unit unit) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (!(comp instanceof GoodsLabel)) {
                    logger.warning("Invalid component: " + comp);
                    return null;
                }
                comp.getParent().remove(comp);
                return comp;
            }

            return add(comp);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int suggested(GoodsType type) {
            Colony colony = getColony();
            return colony.getWarehouseCapacity() - colony.getGoodsCount(type);
        }


        // Interface PropertyChangeListener

        /**
         * {@inheritDoc}
         */
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            final Colony colony = getColony();
            if (colony != null && event != null) {
                logger.finest(colony.getName() + "-warehouse change "
                    + event.getPropertyName()
                    + ": " + event.getOldValue()
                    + " -> " + event.getNewValue());
            }
            update();
        }


        // Override Component

        /**
         * {@inheritDoc}
         */
        @Override
        public void removeNotify() {
            super.removeNotify();

            removeAll();
            setLayout(null);
        }
    }


    /**
     * This panel is a list of the colony's buildings.
     */
    public final class BuildingsPanel extends MigPanel {

        /** Always pop up the build queue when clicking on a building. */
        private final MouseAdapter buildQueueListener
            = new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        getGUI().showBuildQueuePanel(getColony());
                    }
                };


        /**
         * Creates this BuildingsPanel.
         */
        public BuildingsPanel() {
            super("BuildingsPanelUI",
                  new MigLayout("fill, wrap 4, insets 0, gap 0:10:10:push"));
        }


        /**
         * Initializes the game data in this buildings panel.
         */
        public void initialize() {
            final Colony colony = getColony();
            if (colony == null) return;
            cleanup();

            for (Building building : sort(colony.getBuildings())) {
                ASingleBuildingPanel aSBP = new ASingleBuildingPanel(building);
                aSBP.initialize();
                add(aSBP);
            }

            update();
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


        // Override Component

        /**
         * {@inheritDoc}
         */
        @Override
        public void removeNotify() {
            super.removeNotify();

            removeAll();
            setLayout(null);
        }


        /**
         * This panel is a single line (one building) in the
         * {@code BuildingsPanel}.
         */
        public final class ASingleBuildingPanel extends BuildingPanel
            implements DropTarget  {

            /**
             * Creates this ASingleBuildingPanel.
             *
             * @param building The {@code Building} to display
             *     information from.
             */
            public ASingleBuildingPanel(Building building) {
                super(getFreeColClient(), building);

                setOpaque(false);
            }


            /**
             * {@inheritDoc}
             */
            @Override
            public void initialize() {
                if (ColonyPanel.this.isEditable()) {
                    super.initialize();

                    addMouseListener(releaseListener);
                    addMouseListener(buildQueueListener);
                    setTransferHandler(defaultTransferHandler);
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void cleanup() {
                super.cleanup();

                removeMouseListener(releaseListener);
                removeMouseListener(buildQueueListener);
                setTransferHandler(null);
                removeAll();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void update() {
                super.update();

                if (ColonyPanel.this.isEditable()) {
                    for (UnitLabel unitLabel : getUnitLabels()) {
                        unitLabel.setTransferHandler(defaultTransferHandler);
                        unitLabel.addMouseListener(pressListener);
                    }
                }
            }

            /**
             * Try to assign a unit to work this building.
             *
             * @param unit The {@code Unit} to try.
             * @return True if the work begins.
             */
            private boolean tryWork(Unit unit) {
                Building building = getBuilding();
                NoAddReason reason = building.getNoAddReason(unit);
                if (reason != NoAddReason.NONE) {
                    getGUI().showInformationPanel(building, reason.getDescriptionKey());
                    return false;
                }

                return ColonyPanel.this.tryWork(unit, building);
            }


            // Interface DropTarget

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean accepts(Goods goods) {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean accepts(GoodsType goodsType) {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean accepts(Unit unit) {
                return unit.isPerson();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Component add(Component comp, boolean editState) {
                if (editState) {
                    if (comp instanceof UnitLabel) {
                        if (!tryWork(((UnitLabel)comp).getUnit())) return null;
                    } else {
                        logger.warning("An invalid component was dropped"
                            + " on this ASingleBuildingPanel.");
                        return null;
                    }
                    update();
                }
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public int suggested(GoodsType type) { return -1; } // N/A


            // Interface PropertyChangeListener

            /**
             * {@inheritDoc}
             */
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                super.propertyChange(event);

                ColonyPanel.this.updateProduction();
            }
        }
    }

    /**
     * A panel that displays the tiles in the immediate area around the colony.
     * TilesPanel class must use the ImageLibrary inside SwingGUI.tileMapViewer
     * for everything.
     */
    public final class TilesPanel extends JPanel {

        /** The tiles around the colony. */
        private final Tile[][] tiles = new Tile[3][3];

        /** A currently displayed production message. */
        private FreeColPanel cachedPanel = null;
        /** The work location that would be better to produce with. */
        private WorkLocation bestLocation = null;

        
        /**
         * Creates a TilesPanel.
         */
        public TilesPanel() {
            setBackground(Color.BLACK);
            setBorder(null);
            setLayout(null);
        }


        /**
         * Initialize the game data in this tiles panel.
         */
        public void initialize() {
            final Colony colony = getColony();
            if (colony == null) return;
            cleanup();

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
            for (int x = 0; x < tiles.length; x++) {
                for (int y = 0; y < tiles[x].length; y++) {
                    if (tiles[x][y] == null) {
                        logger.warning("Null tile for " + getColony()
                            + " at " + x + "," + y);
                        continue;
                    }
                    ColonyTile colonyTile = colony.getColonyTile(tiles[x][y]);
                    if (colonyTile == null) {
                        logger.warning("Null colony tile for " + getColony()
                            + " on " + tiles[x][y] + " at " + x + "," + y);
                        dump("TILES", colony.getColonyTiles());
                        continue;
                    }
                    ASingleTilePanel aSTP
                        = new ASingleTilePanel(colonyTile, x, y);
                    aSTP.initialize();
                    add(aSTP, Integer.valueOf(layer++));
                }
            }

            update();
        }

        /**
         * Clean up this tiles panel.
         */
        public void cleanup() {
            for (Component component : getComponents()) {
                if (component instanceof ASingleTilePanel) {
                    ((ASingleTilePanel)component).cleanup();
                }
            }
            removeAll();
        }

        /**
         * Update this tiles panel.
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
         * Display the poor production message.
         *
         * @param best The better work location.
         * @param template The {@code StringTemplate} with the message.
         */
        public void showPoorProduction(WorkLocation best,
                                       StringTemplate template) {
            // Already warned about this?  Do nothing if so.
            if (this.bestLocation == best) return;
            
            if (this.cachedPanel != null) {
                getGUI().removeComponent(this.cachedPanel);
            }
            this.cachedPanel = getGUI().showInformationPanel(best, template);
            this.bestLocation = best;
        }

            
        // Override JComponent

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintComponent(Graphics g) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());

            final Colony colony = getColony();
            if (colony != null) {
                getGUI().displayColonyTiles((Graphics2D)g, tiles, colony);
            }
        }

        /**
         * Panel for visualizing a {@code ColonyTile}.  The
         * component itself is not visible, however the content of the
         * component is (i.e. the people working and the production)
         */
        public final class ASingleTilePanel extends JPanel
            implements DropTarget, PropertyChangeListener {

            /** The colony tile to monitor. */
            private final ColonyTile colonyTile;


            /**
             * Create a new single tile panel.
             *
             * @param colonyTile The {@code ColonyTile} to monitor.
             * @param x The x offset.
             * @param y The y offset.
             */
            public ASingleTilePanel(ColonyTile colonyTile, int x, int y) {
                this.colonyTile = colonyTile;

                setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
                setOpaque(false);
                // Size and position:
                Dimension size = getImageLibrary().getTileSize();
                setSize(size);
                setLocation(((2 - x) + y) * size.width / 2,
                    (x + y) * size.height / 2);
            }


            /**
             * Initialize this single tile panel.
             */
            public void initialize() {
                if (ColonyPanel.this.isEditable()) {
                    cleanup();

                    addMouseListener(pressListener);
                    addMouseListener(releaseListener);
                    setTransferHandler(defaultTransferHandler);
                    addPropertyChangeListeners();
                }
            }

            /**
             * Clean up this single tile panel.
             */
            public void cleanup() {
                removeMouseListener(pressListener);
                removeMouseListener(releaseListener);
                setTransferHandler(null);
                removePropertyChangeListeners();
                removeAll();
            }

            /**
             * Adds PropertyChangeListeners
             */
            private void addPropertyChangeListeners() {
                if (this.colonyTile != null) {
                    this.colonyTile.addPropertyChangeListener(this);
                }
            }

            /**
             * Removes PropertyChangeListeners
             */
            private void removePropertyChangeListeners() {
                if (this.colonyTile != null) {
                    this.colonyTile.removePropertyChangeListener(this);
                }
            }

            /**
             * Update this single tile panel.
             */
            public void update() {
                removeAll();
                if (this.colonyTile == null) {
                    logger.warning("Update of " + getColony()
                        + " null colony tile.");
                    return;
                }

                final FreeColClient fcc = getFreeColClient();
                UnitLabel label = null;
                for (Unit unit : this.colonyTile.getUnitList()) {
                    label = new UnitLabel(fcc, unit, false, false);
                    if (ColonyPanel.this.isEditable()) {
                        label.setTransferHandler(defaultTransferHandler);
                        label.addMouseListener(pressListener);
                    }
                    super.add(label);
                }
                updateDescriptionLabel(label);
                if (this.colonyTile.isColonyCenterTile()) {
                    setLayout(new GridLayout(2, 1));
                    ProductionInfo info
                        = colony.getProductionInfo(this.colonyTile);
                    if (info != null) {
                        for (AbstractGoods ag : info.getProduction()) {
                            ProductionLabel productionLabel
                                = new ProductionLabel(fcc, ag);
                            productionLabel.addMouseListener(pressListener);
                            add(productionLabel);
                        }
                    }
                }
            }

            /**
             * Gets the colony tile this panel is handling.
             *
             * @return The colony tile.
             */
            public ColonyTile getColonyTile() {
                return this.colonyTile;
            }

            /**
             * Updates the description label, which is a tooltip with
             * the terrain type, road and plow indicator, if any.
             *
             * If a unit is on it update the tooltip of it instead.
             *
             * @param unitLabel The {@code UnitLabel} to update.
             */
            private void updateDescriptionLabel(UnitLabel unitLabel) {
                String tileMsg = Messages.message(this.colonyTile.getLabel());
                if (unitLabel == null) {
                    setToolTipText(tileMsg);
                } else {
                    final Unit unit = unitLabel.getUnit();
                    unitLabel.setDescriptionLabel(tileMsg + " ["
                        + unit.getDescription(Unit.UnitLabelType.NATIONAL)
                        + "]");
                }
            }

            /**
             * Try to work this tile with a specified unit.
             *
             * @param unit The {@code Unit} to work the tile.
             * @return True if the unit succeeds.
             */
            private boolean tryWork(Unit unit) {
                final Colony colony = getColony();
                Tile tile = this.colonyTile.getWorkTile();
                Player player = unit.getOwner();

                if (tile.getOwningSettlement() != colony) {
                    // Need to acquire the tile before working it.
                    NoClaimReason claim
                        = player.canClaimForSettlementReason(tile);
                    switch (claim) {
                    case NONE: case NATIVES:
                        if (igc().claimTile(tile, colony)
                            && tile.getOwningSettlement() == colony) {
                            logger.info("Colony " + colony.getName()
                                + " claims tile " + tile
                                + " with unit " + unit.getId());
                        } else {
                            logger.warning("Colony " + colony.getName()
                                + " did not claim " + tile
                                + " with unit " + unit.getId());
                            return false;
                        }
                        break;
                    default: // Otherwise, can not use land
                        getGUI().showInformationPanel(tile, claim.getDescriptionKey());
                        return false;
                    }
                    // Check reason again, claim should be satisfied.
                    if (tile.getOwningSettlement() != colony) {
                        throw new RuntimeException("Claim failed: " + claim);
                    }
                }

                // Claim sorted, but complain about other failure.
                NoAddReason reason = this.colonyTile.getNoAddReason(unit);
                if (reason != NoAddReason.NONE) {
                    getGUI().showInformationPanel(this.colonyTile,
                        StringTemplate.template(reason.getDescriptionKey()));
                    return false;
                }

                if (!ColonyPanel.this.tryWork(unit, this.colonyTile)) return false;

                GoodsType workType = unit.getWorkType();
                if (workType != null
                    && getClientOptions().getBoolean(ClientOptions.SHOW_NOT_BEST_TILE)) {
                    WorkLocation best = colony.getWorkLocationFor(unit,
                                                                  workType);
                    if (best != null && this.colonyTile != best
                        && (this.colonyTile.getPotentialProduction(workType, unit.getType())
                            < best.getPotentialProduction(workType, unit.getType()))) {
                        StringTemplate template = StringTemplate
                            .template("colonyPanel.notBestTile")
                            .addStringTemplate("%unit%",
                                unit.getLabel(Unit.UnitLabelType.NATIONAL))
                            .addNamed("%goods%", workType)
                            .addStringTemplate("%tile%", best.getLabel());
                        showPoorProduction(best, template);
                    }
                }
                return true;
            }


            // Interface DropTarget

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean accepts(Unit unit) {
                return unit.isPerson();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Component add(Component comp, boolean editState) {
                if (editState) {
                    if (comp instanceof UnitLabel) {
                        UnitLabel label = (UnitLabel)comp;
                        if (!tryWork(label.getUnit())) return null;
                        label.setSmall(false);
                    } else {
                        logger.warning("An invalid component was dropped"
                                       + " on this ASingleTilePanel.");
                        return null;
                    }
                }

                update();
                return comp;
            }


            // Interface PropertyChangeListener

            /**
             * {@inheritDoc}
             *
             * Updates the production value via {@link #updateProduction()}
             */
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                String property = event.getPropertyName();
                logger.finest(this.colonyTile.getId() + " change " + property
                              + ": " + event.getOldValue()
                              + " -> " + event.getNewValue());
                ColonyPanel.this.updateProduction();
            }


            // Override JComponent

            /**
             * Checks if this {@code JComponent} contains the given
             * coordinate.
             *
             * @param px The x coordinate to check.
             * @param py The y coordinate to check.
             * @return true if the coordinate is inside.
             */
            @Override
            public boolean contains(int px, int py) {
                int w = getWidth();
                int h = getHeight();
                int dx = Math.abs(w/2 - px);
                int dy = Math.abs(h/2 - py);
                return (dx + w * dy / h) <= w/2;
            }
        }
    }
}
