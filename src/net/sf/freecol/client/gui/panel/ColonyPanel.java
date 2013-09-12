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

import java.awt.Color;
import java.awt.Component;
import java.util.Comparator;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
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
import net.sf.freecol.common.model.FreeColObject;
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
import net.sf.freecol.common.model.ProductionType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitLocation.NoAddReason;
import net.sf.freecol.common.model.UnitType;


/**
 * This is a panel for the Colony display.  It shows the units that
 * are working in the colony, the buildings and much more.
 *
 * Beware that in debug mode, this might be a server-side version of the colony
 * which is why we need to call getColony().getSpecification() to get the
 * spec that corresponds to the good types in this colony.
 */
public final class ColonyPanel extends PortPanel
    implements ActionListener, PropertyChangeListener {

    private static Logger logger = Logger.getLogger(ColonyPanel.class.getName());

    private static final int EXIT = 0,
        BUILDQUEUE = 1,
        UNLOAD = 2,
        WAREHOUSE = 4,
        FILL = 5,
        COLONY_UNITS = 6,
        SETGOODS = 7;

    /** The height of the area in which autoscrolling should happen. */
    public static final int SCROLL_AREA_HEIGHT = 40;

    /** The speed of the scrolling. */
    public static final int SCROLL_SPEED = 40;

    // Buttons
    private JButton unloadButton
        = new JButton(Messages.message("unload"));

    private JButton fillButton
        = new JButton(Messages.message("fill"));

    private JButton warehouseButton
        = new JButton(Messages.message("warehouseDialog.name"));

    private JButton buildQueueButton
        = new JButton(Messages.message("colonyPanel.buildQueue"));

    private JButton colonyUnitsButton
        = new JButton(Messages.message("colonyPanel.colonyUnits"));

    private JButton setGoodsButton
        = (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS))
            ? new JButton("Set Goods")
            : null;

    /** The <code>Colony</code> this panel is displaying. */
    private Colony colony = null;

    // inherit PortPanel.pressListener
    // inherit PortPanel.defaultTransferHandler
    // inherit PortPanel.selectedUnitLabel

    private ActionListener nameActionListener = null;

    private MouseListener releaseListener = null;

    private MouseAdapter buildQueueListener = null;

    // Subparts
    private JComboBox nameBox = null;

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


    /**
     * The constructor for the panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param colony The <code>Colony</code> to display in this panel.
     */
    @SuppressWarnings("unchecked") // FIXME in Java7
    public ColonyPanel(FreeColClient freeColClient, Colony colony) {
        super(freeColClient,
            new MigLayout("fill, wrap 2, insets 2",
                          "[390!][fill]",
                          "[][]0[]0[][growprio 200,shrinkprio 10]"
                          + "[growprio 150,shrinkprio 50]"));

        setFocusCycleRoot(true);

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
        unloadButton.setActionCommand(String.valueOf(UNLOAD));
        enterPressesWhenFocused(unloadButton);

        InputMap fillIM = new ComponentInputMap(fillButton);
        fillIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0, false),
                   "pressed");
        fillIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0, true),
                   "released");
        SwingUtilities.replaceUIInputMap(fillButton,
            JComponent.WHEN_IN_FOCUSED_WINDOW, fillIM);
        fillButton.setActionCommand(String.valueOf(FILL));
        enterPressesWhenFocused(fillButton);

        InputMap warehouseIM = new ComponentInputMap(warehouseButton);
        warehouseIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, false),
                        "pressed");
        warehouseIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, true),
                        "released");
        SwingUtilities.replaceUIInputMap(warehouseButton,
            JComponent.WHEN_IN_FOCUSED_WINDOW, warehouseIM);
        warehouseButton.setActionCommand(String.valueOf(WAREHOUSE));
        enterPressesWhenFocused(warehouseButton);

        InputMap buildQueueIM = new ComponentInputMap(buildQueueButton);
        buildQueueIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0, false),
                         "pressed");
        buildQueueIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0, true),
                         "released");
        SwingUtilities.replaceUIInputMap(buildQueueButton,
            JComponent.WHEN_IN_FOCUSED_WINDOW, buildQueueIM);
        buildQueueButton.setActionCommand(String.valueOf(BUILDQUEUE));
        enterPressesWhenFocused(buildQueueButton);

        InputMap colonyUnitsIM = new ComponentInputMap(colonyUnitsButton);
        colonyUnitsIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0, false),
                          "pressed");
        colonyUnitsIM.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0, true),
                          "released");
        SwingUtilities.replaceUIInputMap(colonyUnitsButton,
            JComponent.WHEN_IN_FOCUSED_WINDOW, colonyUnitsIM);
        colonyUnitsButton.setActionCommand(String.valueOf(COLONY_UNITS));
        enterPressesWhenFocused(colonyUnitsButton);

        if (setGoodsButton != null) {
            setGoodsButton.setActionCommand(String.valueOf(SETGOODS));
            enterPressesWhenFocused(setGoodsButton);
        }

        defaultTransferHandler
            = new DefaultTransferHandler(freeColClient, this);
        nameActionListener = new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    initialize((Colony)nameBox.getSelectedItem());
                }
            };
        pressListener = new DragListener(freeColClient, this);
        releaseListener = new DropListener();
        buildQueueListener = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    getGUI().showBuildQueuePanel(getColony());
                }
            };
        selectedUnitLabel = null;

        // Make the colony label
        nameBox = new JComboBox();
        nameBox.setFont(smallHeaderFont);
        for (Colony aColony : freeColClient.getMySortedColonies()) {
            nameBox.addItem(aColony);
        }
        nameBox.setSelectedItem(colony);
        nameBox.getInputMap().put(KeyStroke.getKeyStroke("LEFT"),
                                  "selectPrevious2");
        nameBox.getInputMap().put(KeyStroke.getKeyStroke("RIGHT"),
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
        buildingsScroll.setBorder(BorderFactory.createEtchedBorder());

        cargoPanel = new ColonyCargoPanel(freeColClient);
        cargoScroll = new JScrollPane(cargoPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        cargoScroll.setBorder(BorderFactory.createEtchedBorder());

        constructionPanel = new ConstructionPanel(freeColClient, colony, true);

        inPortPanel = new ColonyInPortPanel();
        inPortScroll = new JScrollPane(inPortPanel);
        inPortScroll.getVerticalScrollBar().setUnitIncrement(16);
        inPortScroll.setBorder(BorderFactory.createEtchedBorder());

        outsideColonyPanel = new OutsideColonyPanel();
        outsideColonyScroll = new JScrollPane(outsideColonyPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        outsideColonyScroll.getVerticalScrollBar().setUnitIncrement(16);
        outsideColonyScroll.setBorder(BorderFactory.createEtchedBorder());

        populationPanel = new PopulationPanel();

        tilesPanel = new TilesPanel();
        tilesScroll = new JScrollPane(tilesPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tilesScroll.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

        warehousePanel = new WarehousePanel();
        warehouseScroll = new JScrollPane(warehousePanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        warehouseScroll.setBorder(BorderFactory.createEtchedBorder());

        InputMap nameIM = new ComponentInputMap(nameBox);
        nameIM.put(KeyStroke.getKeyStroke("LEFT"), "selectPrevious2");
        nameIM.put(KeyStroke.getKeyStroke("RIGHT"), "selectNext2");
        SwingUtilities.replaceUIInputMap(nameBox,
            JComponent.WHEN_IN_FOCUSED_WINDOW, nameIM);

        // See the message of Ulf Onnen for more information about the
        // presence of this fake mouse listener.
        // Disabled 2013/09, no longer appears necessary.
        //addMouseListener(new MouseAdapter() {});

        initialize(colony);
        restoreSavedSize(850, 600);
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
     * @param colony The <code>Colony</code> to be displayed.
     */
    private void initialize(Colony colony) {
        cleanup();

        setColony(colony);
        // Do not just use colony.getOwner() == getMyPlayer() because
        // in debug mode we are in the *server* colony, and the equality
        // will fail.
        editable = colony.getOwner().getId().equals(getMyPlayer().getId());

        addPropertyChangeListeners();
        addMouseListeners();
        setTransferHandlers(isEditable());

        // Enable/disable widgets
        unloadButton.addActionListener(this);
        fillButton.addActionListener(this);
        warehouseButton.addActionListener(this);
        buildQueueButton.addActionListener(this);
        colonyUnitsButton.addActionListener(this);
        if (setGoodsButton != null) setGoodsButton.addActionListener(this);

        unloadButton.setEnabled(isEditable());
        fillButton.setEnabled(isEditable());
        warehouseButton.setEnabled(isEditable());
        buildQueueButton.setEnabled(isEditable());
        colonyUnitsButton.setEnabled(isEditable());
        if (setGoodsButton != null) {
            setGoodsButton.setEnabled(isEditable());
        }
        nameBox.setEnabled(isEditable());
        nameBox.addActionListener(nameActionListener);
        updateNetProductionPanel();

        buildingsPanel.initialize();
        cargoPanel.initialize();
        constructionPanel.initialize();
        inPortPanel.initialize();
        outsideColonyPanel.initialize();
        populationPanel.initialize();
        tilesPanel.initialize();
        warehousePanel.initialize();

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

        update();
    }

    /**
     * Clean up this colony panel.
     */
    private void cleanup() {
        unloadButton.removeActionListener(this);
        fillButton.removeActionListener(this);
        warehouseButton.removeActionListener(this);
        buildQueueButton.removeActionListener(this);
        colonyUnitsButton.removeActionListener(this);
        if (setGoodsButton != null) setGoodsButton.removeActionListener(this);

        nameBox.removeActionListener(nameActionListener);

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
        updateTilesPanel();
        updateBuildingsPanel();
        updateNetProductionPanel();
        updateConstructionPanel();
    }

    /**
     * Update the entire colony panel.
     */
    private void update() {
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
        if (isEditable() && selectedUnitLabel != null) {
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
            Building workingInBuilding = unit.getWorkBuilding();
            ColonyTile workingOnLand = unit.getWorkTile();
            GoodsType goodsType = unit.getWorkType();
            Unit student = unit.getStudent();

            String menuTitle;
            unitIcon = getLibrary().getUnitImageIcon(unit, 0.5);
            if (student != null) {
                menuTitle = new String(Messages.message(unit.getLabel())
                    + " " + Messages.message("producing.name")
                    + " " + Messages.message(unit.getType().getSkillTaught()
                                                           .getNameKey())
                    + " " + Integer.toString(unit.getTurnsOfTraining())
                    + "/" + Integer.toString(unit.getNeededTurnsOfTraining()));
            } else if (workingOnLand != null && goodsType != null) {
                int producing = workingOnLand.getProductionOf(unit, goodsType);
                String nominative = Messages.message(StringTemplate.template(
                    goodsType.getNameKey()).addAmount("%amount%", producing));
                menuTitle = new String(Messages.message(unit.getLabel())
                    + " " + Messages.message("producing.name")
                    + " " + producing + " " + nominative);
            } else if (workingInBuilding != null && goodsType != null) {
                int producing = workingInBuilding.getProductionOf(unit, 
                                                                  goodsType);
                String nominative = Messages.message(
                    StringTemplate.template(goodsType.getNameKey())
                        .addAmount("%amount%", producing));
                menuTitle = new String(Messages.message(unit.getLabel())
                    + " " + Messages.message("producing.name")
                    + " " + producing + " " + nominative);
            } else {
                menuTitle = new String(Messages.message(unit.getLabel())
                    + " " + Messages.message("producing.name")
                    + " " + Messages.message("nothing"));
            }
            subMenu = new JMenuItem(menuTitle, unitIcon);
            subMenu.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        unitMenu.createUnitMenu(new UnitLabel(freeColClient,
                                                              unit));
                        unitMenu.show(getGUI().getCanvas(), 0, 0);
                    }
                });
            unitNumber++;
            colonyUnitsMenu.add(subMenu);
        }
        colonyUnitsMenu.addSeparator();
        for (final Unit unit : colonyTile.getUnitList()) {
            if(unit.isCarrier()){
                unitIcon = getLibrary().getUnitImageIcon(unit, 0.5);
                String menuTitle = new String(Messages.message(unit.getLabel()) +
                    " " + Messages.message("inPort.name"));
                subMenu = new JMenuItem(menuTitle, unitIcon);
                subMenu.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        unitMenu.createUnitMenu(new UnitLabel(freeColClient,
                                                              unit));
                        unitMenu.show(getGUI().getCanvas(), 0, 0);
                    }
                });
                unitNumber++;
                colonyUnitsMenu.add(subMenu);
                if(unit.getUnitList() != null){
                    for(final Unit innerUnit : unit.getUnitList()){
                        unitIcon = getLibrary().getUnitImageIcon(innerUnit, 0.5);
                        menuTitle = new String(Messages.message(innerUnit.getLabel()) + " Cargo On " + Messages.message(unit.getLabel()));
                        subMenu = new JMenuItem(menuTitle, unitIcon);
                        subMenu.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                unitMenu.createUnitMenu(new UnitLabel(freeColClient, innerUnit));
                                unitMenu.show(getGUI().getCanvas(), 0, 0);
                            }
                        });
                        unitNumber++;
                        colonyUnitsMenu.add(subMenu);
                    }
                }
            }else if(!unit.isOnCarrier()){
                unitIcon = getLibrary().getUnitImageIcon(unit, 0.5);
                String menuTitle = new String(Messages.message(unit.getLabel()) +
                        " " + Messages.message("outsideOfColony.name"));
                subMenu = new JMenuItem(menuTitle, unitIcon);
                subMenu.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        unitMenu.createUnitMenu(new UnitLabel(freeColClient, unit));
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


    // Public base accessors and mutators

    /**
     * Gets the <code>Colony</code> in use.
     *
     * Try to use this at the top of all the routines that need the colony,
     * to get a stable value.  There have been nasty races when the colony
     * changes out from underneath us, and more may be lurking.
     *
     * @return The <code>Colony</code>.
     */
    public synchronized final Colony getColony() {
        return colony;
    }

    /**
     * Gets the <code>TilesPanel</code>-object in use.
     *
     * @return The <code>TilesPanel</code>.
     */
    public final TilesPanel getTilesPanel() {
        return tilesPanel;
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
     * Gets the currently select unit.
     *
     * @return The currently select <code>Unit</code>.
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


    // Public update routines

    public void updateBuildingsPanel() {
        buildingsPanel.update();
    }

    public void updateConstructionPanel() {
        constructionPanel.update();
    }

    public void updateInPortPanel() {
        inPortPanel.update();
    }

    public void updateNetProductionPanel() {
        final Colony colony = getColony();
        final Specification spec = colony.getSpecification();
        // TODO: find out why the cache needs to be explicitly invalidated
        colony.invalidateCache();
        netProductionPanel.removeAll();

        for (GoodsType goodsType : spec.getGoodsTypeList()) {
            int amount = colony.getAdjustedNetProductionOf(goodsType);
            if (amount != 0) {
                netProductionPanel.add(new ProductionLabel(getFreeColClient(),
                                                           goodsType, amount));
            }
        }

        netProductionPanel.revalidate();
    }

    public void updateOutsideColonyPanel() {
        outsideColonyPanel.update();
    }

    public void updatePopulationPanel() {
        populationPanel.update();
    }

    public void updateTilesPanel() {
        tilesPanel.update();
    }

    public void updateWarehousePanel() {
        warehousePanel.update();
    }


    /**
     * Close this <code>ColonyPanel</code>.
     */
    public void closeColonyPanel() {
        final Colony colony = getColony();
        boolean abandon = false;
        if (colony.getUnitCount() == 0) {
            if (!getGUI().showConfirmDialog("abandonColony.text",
                                            "abandonColony.yes",
                                            "abandonColony.no")) return;
            abandon = true;
        }                
        if (!abandon) {
            BuildableType buildable = colony.getCurrentlyBuilding();
            if (buildable != null
                && buildable.getRequiredPopulation() > colony.getUnitCount()
                && !getGUI().showConfirmDialog(null,
                    StringTemplate.template("colonyPanel.reducePopulation")
                        .addName("%colony%", colony.getName())
                        .addAmount("%number%", buildable.getRequiredPopulation())
                        .add("%buildable%", buildable.getNameKey()),
                    "ok", "cancel")) {
                return;
            }
        }

        cleanup();

        getGUI().removeFromCanvas(this);
        getGUI().getMapViewer().restartBlinking();

        // Talk to the controller last, allow all the cleanup to happen first.
        if (abandon) getController().abandonColony(colony);
        if (getFreeColClient().currentPlayerIsMyPlayer()) {
            getController().nextModelMessage();
            Unit activeUnit = getGUI().getActiveUnit();
            if (activeUnit == null || !activeUnit.hasTile()
                || (!(activeUnit.getLocation() instanceof Tile)
                    && !activeUnit.isOnCarrier())) {
                getController().nextActiveUnit();
            }
        }
    }


    // Interface PortPanel

    /**
     * Gets the list of units on the colony tile.
     * Note, does not include the units *inside* the colony.
     *
     * @return A sorted list of units on the colony tile.
     */
    public List<Unit> getUnitList() {
        return FreeColObject.getSortedCopy(colony.getTile().getUnitList());
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent event) {
        final Colony colony = getColony();
        final String command = event.getActionCommand();
        final Unit unit = getSelectedUnit();

        if (OK.equals(command)) {
            closeColonyPanel();
        } else {
            int cmd;
            try {
                cmd = Integer.valueOf(command).intValue();
            } catch (NumberFormatException nfe) {
                logger.warning("Invalid action number: " + command);
                return;
            }
            switch (cmd) {
            case UNLOAD:
                if (unit == null || !unit.isCarrier()) break;
                for (Goods goods : unit.getGoodsContainer().getGoods()) {
                    getController().unloadCargo(goods, false);
                }
                for (Unit u : unit.getUnitList()) {
                    getController().leaveShip(u);
                }
                cargoPanel.update();
                updateOutsideColonyPanel();
                unloadButton.setEnabled(false);
                fillButton.setEnabled(false);
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
                if (unit == null || !unit.isCarrier()) break;
                for (Goods goods : unit.getGoodsContainer().getGoods()) {
                    int space = GoodsContainer.CARGO_SIZE - goods.getAmount();
                    int count = colony.getGoodsCount(goods.getType());
                    if (space > 0 && count > 0) {
                        Goods newGoods = new Goods(goods.getGame(), colony,
                                                   goods.getType(),
                                                   Math.min(space, count));
                        getController().loadCargo(newGoods, unit);
                    }
                }
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
                super.actionPerformed(event);
            }
        }
    }


    // Interface PropertyChangeListener

    /**
     * {@inheritDoc}
     */
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
            updateInPortPanel();
        } else if (ColonyChangeEvent.POPULATION_CHANGE.toString().equals(property)) {
            updatePopulationPanel();
            updateNetProductionPanel(); // food production changes
        } else if (ColonyChangeEvent.BONUS_CHANGE.toString().equals(property)) {
            ModelMessage msg = colony.checkForGovMgtChangeMessage();
            if (msg != null) {
                getGUI().showInformationMessage(colony, msg);
            }
            updatePopulationPanel();
        } else if (ColonyChangeEvent.UNIT_TYPE_CHANGE.toString().equals(property)) {
            FreeColGameObject object = (FreeColGameObject)event.getSource();
            UnitType oldType = (UnitType) event.getOldValue();
            UnitType newType = (UnitType) event.getNewValue();
            getGUI().showInformationMessage(object,
                StringTemplate.template("model.colony.unitChange")
                    .add("%oldType%", oldType.getNameKey())
                    .add("%newType%", newType.getNameKey()));
            updateTilesPanel();
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


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        if (nameBox == null) return; // Been here already

        // Alas, ColonyPanel is often leaky.
        unloadButton = null;
        fillButton = null;
        warehouseButton = null;
        buildQueueButton = null;
        colonyUnitsButton = null;
        setGoodsButton = null;
        nameBox = null;
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

        nameActionListener = null;
        releaseListener = null;
        buildQueueListener = null;

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
         * @param freeColClient The <code>FreeColClient</code> for the game.
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
            super(new MigLayout("wrap 5, fill, insets 0",
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
            final int uc = colony.getUnitCount();
            final int solPercent = colony.getSoL();
            final int rebels = Colony.calculateRebels(uc, solPercent);
            final Nation nation = colony.getOwner().getNation();
            final int grow = colony.getPreferredSizeChange();
            final int bonus = colony.getProductionBonus();
            StringTemplate t;

            removeAll();

            rebelShield.setIcon(new ImageIcon(getLibrary()
                    .getCoatOfArmsImage(nation, 0.5)));
            add(rebelShield, "bottom");

            t = StringTemplate.template("colonyPanel.rebelLabel")
                              .addAmount("%number%", rebels);
            rebelLabel.setText(Messages.message(t));
            add(rebelLabel, "split 2, flowy");

            rebelMemberLabel.setText(solPercent + "%");
            add(rebelMemberLabel);

            t = StringTemplate.template("colonyPanel.populationLabel")
                              .addAmount("%number%", uc);
            popLabel.setText(Messages.message(t));
            add(popLabel, "split 2, flowy");

            t = StringTemplate.template("colonyPanel.bonusLabel")
                              .addAmount("%number%", bonus)
                              .add("%extra%", ((grow == 0) ? ""
                                      : "(" + grow + ")"));
            bonusLabel.setText(Messages.message(t));
            add(bonusLabel);

            t = StringTemplate.template("colonyPanel.royalistLabel")
                              .addAmount("%number%", uc - rebels);
            royalistLabel.setText(Messages.message(t));
            add(royalistLabel, "split 2, flowy");

            royalistMemberLabel.setText(colony.getTory() + "%");
            add(royalistMemberLabel);

            royalistShield.setIcon(new ImageIcon(getLibrary()
                    .getCoatOfArmsImage(nation.getREFNation(), 0.5)));
            add(royalistShield, "bottom");

            revalidate();
            repaint();
        }

        public JToolTip createToolTip() {
            return new RebelToolTip(getFreeColClient(), getColony());
        }


        // Override JLabel

        /**
         * {@inheritDoc}
         */
        @Override
        public String getUIClassID() {
            return "PopulationPanelUI";
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
            super(ColonyPanel.this, null, ColonyPanel.this.isEditable());

            setLayout(new MigLayout("wrap 4, fill, insets 0"));
            setBorder(BorderFactory.createTitledBorder(BorderFactory
                    .createEmptyBorder(), Messages.message("outsideColony")));
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
        public boolean accepts(Unit unit) {
            return !unit.isCarrier();
        }

        /**
         * {@inheritDoc}
         */
        public boolean accepts(Goods goods) {
            return false;
        }

        /**
         * {@inheritDoc}
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
                    } else if (!(unit.getLocation() instanceof Tile)
                        && !unit.isOnCarrier()) {
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
                Component c = add(comp);
                return c;
            }
        }


        // Override JPanel

        /**
         * {@inheritDoc}
         */
        @Override
        public String getUIClassID() {
            return "OutsideColonyPanelUI";
        }
    }

    /**
     * A panel that holds UnitsLabels that represent naval Units that are
     * waiting in the port of the colony.
     */
    public final class ColonyInPortPanel extends InPortPanel {

        /**
         * Creates this ColonyInPortPanel.
         */
        public ColonyInPortPanel() {
            super(ColonyPanel.this, null, ColonyPanel.this.isEditable());

            setLayout(new MigLayout("wrap 3, fill, insets 0"));
            setTitledBorder("inPort");
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
    public final class WarehousePanel extends JPanel
        implements DropTarget, PropertyChangeListener {

        /**
         * Creates a WarehousePanel.
         */
        public WarehousePanel() {
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
         * Clean up this WarehousePanel.
         */
        public void cleanup() {
            removePropertyChangeListeners();
            removeAll();
        }

        /**
         * Add the property change listeners to this panel.
         */
        protected void addPropertyChangeListeners() {
            final Colony colony = getColony();
            if (colony != null) {
                colony.getGoodsContainer().addPropertyChangeListener(this);
            }
        }

        /**
         * Remove the property change listeners from this panel.
         */
        protected void removePropertyChangeListeners() {
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
            for (GoodsType goodsType : spec.getGoodsTypeList()) {
                if (!goodsType.isStorable()) continue;
                int count = colony.getGoodsCount(goodsType);
                if (count >= threshold) {
                    Goods goods = new Goods(game, colony, goodsType, count);
                    GoodsLabel goodsLabel = new GoodsLabel(goods, getGUI());
                    if (ColonyPanel.this.isEditable()) {
                        goodsLabel.setTransferHandler(defaultTransferHandler);
                        goodsLabel.addMouseListener(pressListener);
                    }
                    add(goodsLabel, false);
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
        public boolean accepts(Unit unit) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public boolean accepts(Goods goods) {
            return true;
        }

        /**
         * {@inheritDoc}
         */
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


        // Interface PropertyChangeListener

        /**
         * {@inheritDoc}
         */
        public void propertyChange(PropertyChangeEvent event) {
            final Colony colony = getColony();
            logger.finest(colony.getName() + "-warehouse change "
                          + event.getPropertyName()
                          + ": " + event.getOldValue()
                          + " -> " + event.getNewValue());
            update();
        }


        // Override JPanel

        /**
         * {@inheritDoc}
         */
        @Override
        public String getUIClassID() {
            return "WarehousePanelUI";
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
    public final class BuildingsPanel extends JPanel {

        /**
         * Creates this BuildingsPanel.
         */
        public BuildingsPanel() {
            setLayout(new MigLayout("fill, wrap 4, insets 0, gap 0:10:10:push"));
        }


        /**
         * Initializes the game data in this buildings panel.
         */
        public void initialize() {
            final Colony colony = getColony();
            if (colony == null) return;
            cleanup();

            List<Building> buildings = colony.getBuildings();
            Collections.sort(buildings);
            for (Building building : buildings) {
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


        // Override JPanel

        /**
         * {@inheritDoc}
         */
        @Override
        public String getUIClassID() {
            return "BuildingsPanelUI";
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
         * <code>BuildingsPanel</code>.
         */
        public final class ASingleBuildingPanel extends BuildingPanel
            implements DropTarget  {

            /**
             * Creates this ASingleBuildingPanel.
             *
             * @param building The <code>Building</code> to display
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
            public void cleanup() {
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
             * @param unit The <code>Unit</code> to try.
             * @return True if the work begins.
             */
            private boolean tryWork(Unit unit) {
                Building building = getBuilding();
                NoAddReason reason = building.getNoAddReason(unit);
                if (reason != NoAddReason.NONE) {
                    getGUI().showInformationMessage(building, "noAddReason."
                        + reason.toString().toLowerCase(Locale.US));
                    return false;
                }

                getController().work(unit, building);
                // check to see if the unit actually starts working at
                // the building some units like a teacher may not have
                // actually started working there
                if (unit.getWorkBuilding() == building) {
                    return true;
                }
                return false;
            }


            // Interface DropTarget

            /**
             * {@inheritDoc}
             */
            public boolean accepts(Unit unit) {
                return unit.isPerson();
            }

            /**
             * {@inheritDoc}
             */
            public boolean accepts(Goods goods) {
                return false;
            }

            /**
             * {@inheritDoc}
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
                    update();
                }
                return null;
            }


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
     */
    public final class TilesPanel extends JPanel {

        /** The tiles around the colony. */
        private Tile[][] tiles = new Tile[3][3];


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
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    if (tiles[x][y] == null) continue;
                    ColonyTile colonyTile = colony.getColonyTile(tiles[x][y]);
                    ASingleTilePanel aSTP = new ASingleTilePanel(colonyTile,
                                                                 x, y);
                    aSTP.initialize();
                    add(aSTP, new Integer(layer++));
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


        // Override JComponent

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintComponent(Graphics g) {
            final Colony colony = getColony();
            g.setColor(Color.black);
            g.fillRect(0, 0, getWidth(), getHeight());
            if (colony == null) return;

            final Tile tile = colony.getTile();
            final TileType tileType = tile.getType();
            final Image image = getLibrary().getTerrainImage(tileType,
                tile.getX(), tile.getY());
            int tileWidth = image.getWidth(null) / 2;
            int tileHeight = image.getHeight(null) / 2;
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

            /** The colony tile to monitor. */
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
                final Tile tile = colonyTile.getTile();
                final TileType tileType = tile.getType();
                final Image image = getLibrary().getTerrainImage(tileType,
                    tile.getX(), tile.getY());
                // Size and position:
                final int width = image.getWidth(null);
                final int height = image.getHeight(null);
                setSize(width, height);
                setLocation(((2 - x) + y) * width / 2, (x + y) * height / 2);
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

            protected void addPropertyChangeListeners() {
                if (colonyTile != null) {
                    colonyTile.addPropertyChangeListener(this);
                }
            }

            protected void removePropertyChangeListeners() {
                if (colonyTile != null) {
                    colonyTile.removePropertyChangeListener(this);
                }
            }

            /**
             * Update this single tile panel.
             */
            public void update() {
                removeAll();

                UnitLabel label = null;
                for (Unit unit : colonyTile.getUnitList()) {
                    label = new UnitLabel(getFreeColClient(), unit);
                    if (ColonyPanel.this.isEditable()) {
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
                            ProductionLabel productionLabel
                                = new ProductionLabel(getFreeColClient(), ag);
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
                return colonyTile;
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
                        = Messages.message(unit.getUnit().getFullLabel());
                    if (toAdd) unitMsg = tileMsg + " [" + unitMsg + "]";
                    unit.setDescriptionLabel(unitMsg);
                }
            }

            /**
             * Try to work this tile with a specified unit.
             *
             * @param unit The <code>Unit</code> to work the tile.
             * @return True if the unit succeeds.
             */
            private boolean tryWork(Unit unit) {
                final Colony colony = getColony();
                Tile tile = colonyTile.getWorkTile();
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
                        getGUI().showInformationMessage(tile, "noClaimReason."
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
                    getGUI().showInformationMessage(colonyTile, "noAddReason."
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
                if (workType == null) {
                    ProductionType productionType = colonyTile.getBestProductionType(unit);
                    if (productionType != null) {
                        GoodsType goodsType = productionType.getOutputs().get(0).getType();
                        if (colonyTile.getProductionOf(unit, goodsType) > 0) {
                            workType = goodsType;
                        }
                    }
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
                            .addStringTemplate("%unit%", unit.getFullLabel())
                            .add("%goods%", workType.getNameKey())
                            .addStringTemplate("%tile%", best.getLabel());
                        getGUI().showInformationMessage(best, template);
                    }
                }
                return true;
            }


            // Interface DropTarget

            /**
             * {@inheritDoc}
             */
            public boolean accepts(Unit unit) {
                return unit.isPerson();
            }

            /**
             * {@inheritDoc}
             */
            public boolean accepts(Goods goods) {
                return false;
            }

            /**
             * {@inheritDoc}
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


            // Interface PropertyChangeListener

            /**
             * {@inheritDoc}
             */
            public void propertyChange(PropertyChangeEvent event) {
                String property = event.getPropertyName();
                logger.finest(colonyTile.getId() + " change " + property
                              + ": " + event.getOldValue()
                              + " -> " + event.getNewValue());
                ColonyPanel.this.updateProduction();
            }


            // Override JComponent

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
        }
    }
}
