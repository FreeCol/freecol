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
import javax.swing.JToolTip;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Colony.ColonyChangeEvent;
import net.sf.freecol.common.resources.ResourceManager;

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

    private static final int EXIT = 0, BUILDQUEUE = 1, UNLOAD = 2, WAREHOUSE = 4, FILL = 5;

    private final JLabel rebelShield = new JLabel();
    private final JLabel rebelLabel = new JLabel();
    private final JLabel bonusLabel = new JLabel();
    private final JLabel royalistLabel = new JLabel();
    private final JLabel royalistShield = new JLabel();
    private final JLabel rebelMemberLabel = new JLabel();
    private final JLabel popLabel = new JLabel();
    private final JLabel royalistMemberLabel = new JLabel();

    private final JPanel netProductionPanel = new JPanel();
    private final JPanel populationPanel = new JPanel() {
            public JToolTip createToolTip() {
                return new RebelToolTip(colony, getCanvas());
            }

            @Override
            public String getUIClassID() {
                return "PopulationPanelUI";
            }
        };

    private final JComboBox nameBox;

    private final OutsideColonyPanel outsideColonyPanel;

    private final InPortPanel inPortPanel;

    private final ColonyCargoPanel cargoPanel;

    private final WarehousePanel warehousePanel;

    private final TilePanel tilePanel;

    private final BuildingsPanel buildingsPanel;

    private final ConstructionPanel constructionPanel;

    private final DefaultTransferHandler defaultTransferHandler;

    private final MouseListener pressListener;

    private final MouseListener releaseListener;

    private Colony colony;

    private UnitLabel selectedUnit;

    private JButton exitButton = new JButton(Messages.message("close"));

    private JButton unloadButton = new JButton(Messages.message("unload"));

    private JButton fillButton = new JButton(Messages.message("fill"));

    private JButton warehouseButton = new JButton(Messages.message("warehouseDialog.name"));

    private JButton buildQueueButton = new JButton(Messages.message("colonyPanel.buildQueue"));

    /**
     * The saved size of this panel.
     */
    private static Dimension savedSize = null;


    /**
     * The constructor for the panel.
     * 
     * @param parent The parent of this panel
     */
    public ColonyPanel(final Canvas parent, Colony colony) {
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

        netProductionPanel.setOpaque(false);

        populationPanel.setOpaque(true);
        populationPanel.setToolTipText(" ");
        populationPanel.setLayout(new MigLayout("wrap 5, fill, insets 0",
                                                "[][]:push[center]:push[right][]", ""));
        populationPanel.add(rebelShield, "bottom");
        populationPanel.add(rebelLabel, "split 2, flowy");
        populationPanel.add(rebelMemberLabel);
        populationPanel.add(popLabel, "split 2, flowy");
        populationPanel.add(bonusLabel);
        populationPanel.add(royalistLabel, "split 2, flowy");
        populationPanel.add(royalistMemberLabel);
        populationPanel.add(royalistShield, "bottom");

        constructionPanel = new ConstructionPanel(parent, colony);
        constructionPanel.setOpaque(true);

        outsideColonyPanel = new OutsideColonyPanel();

        inPortPanel = new InPortPanel();

        warehousePanel = new WarehousePanel(this);

        tilePanel = new TilePanel(this);

        buildingsPanel = new BuildingsPanel(this);

        cargoPanel = new ColonyCargoPanel(parent);
        cargoPanel.setParentPanel(this);

        defaultTransferHandler = new DefaultTransferHandler(parent, this);
        pressListener = new DragListener(this);
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
                                                      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        buildingsScroll.getVerticalScrollBar().setUnitIncrement( 16 );

        // Make the colony label
        nameBox = new JComboBox();
        nameBox.setFont(smallHeaderFont);
        List<Colony> settlements = colony.getOwner().getColonies();
        sortColonies(settlements);
        for (Colony aColony : settlements) {
            nameBox.addItem(aColony);
        }
        nameBox.setSelectedItem(colony);
        nameBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    initialize((Colony) nameBox.getSelectedItem());
                }
            });

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

        buildQueueButton.setActionCommand(String.valueOf(BUILDQUEUE));
        enterPressesWhenFocused(buildQueueButton);
        buildQueueButton.addActionListener(this);

        selectedUnit = null;

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
        add(unloadButton, "span, split 5, align center");
        add(fillButton);
        add(warehouseButton);
        add(buildQueueButton);
        add(exitButton);

        initialize(colony);
        if (savedSize != null) {
            setPreferredSize(savedSize);
        }

    }

    @Override
    public void requestFocus() {
        exitButton.requestFocus();
    }

    /**
     * Get the <code>SavedSize</code> value.
     *
     * @return a <code>Dimension</code> value
     */
    public final Dimension getSavedSize() {
        return savedSize;
    }

    /**
     * Set the <code>SavedSize</code> value.
     *
     * @param newSavedSize The new SavedSize value.
     */
    public final void setSavedSize(final Dimension newSavedSize) {
        ColonyPanel.savedSize = newSavedSize;
    }

    /**
     * Initialize the data on the window. This is the same as calling:
     * <code>initialize(colony, game, null)</code>.
     * 
     * @param colony The <code>Colony</code> to be displayed.
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

        rebelShield.setIcon(new ImageIcon(ResourceManager.getImage(colony.getOwner().getNation().getId()
                                                                   + ".coat-of-arms.image", 0.5)));
        royalistShield.setIcon(new ImageIcon(ResourceManager.getImage(colony.getOwner().getNation().getRefNation().getId()
                                                                      + ".coat-of-arms.image", 0.5)));
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

        // update all the subpanels
        cargoPanel.removeAll();
        warehousePanel.removeAll();
        tilePanel.removeAll();

        inPortPanel.initialize(preSelectedUnit);
        warehousePanel.initialize();
        buildingsPanel.initialize();
        tilePanel.initialize();

        updateProductionPanel();
        updateSoLLabel();

        constructionPanel.setColony(colony);
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
        int population = getColony().getUnitCount();
        int members = getColony().getMembers();
        int rebels = getColony().getSoL();
        String rebelNumber = Messages.message("colonyPanel.rebelLabel", "%number%",
                                              Integer.toString(members));
        String royalistNumber = Messages.message("colonyPanel.royalistLabel", "%number%",
                                                 Integer.toString(population - members));

        popLabel.setText(Messages.message("colonyPanel.populationLabel", "%number%",
                                          Integer.toString(population)));
        rebelLabel.setText(rebelNumber);
        rebelMemberLabel.setText(Integer.toString(rebels) + "%");
        bonusLabel.setText(Messages.message("colonyPanel.bonusLabel", "%number%",
                                            Integer.toString(getColony().getProductionBonus())));
        royalistLabel.setText(royalistNumber);
        royalistMemberLabel.setText(Integer.toString(getColony().getTory()) + "%");
    }
    
    public void updateInPortPanel() {
        inPortPanel.initialize(null);
    }

    public void updateWarehousePanel() {
        warehousePanel.update();
    }

    public void updateTilePanel() {
        tilePanel.initialize();
    }

    private void sortBuildings(List<Building> buildings) {
        Collections.sort(buildings);
    }
    
    private void sortColonies(List<Colony> colonies) {
        Collections.sort(colonies, getClient().getClientOptions().getColonyComparator());
    }

    public void updateProductionPanel() {
        netProductionPanel.removeAll();
        
        int gross = 0, net = 0;
        Specification spec = getSpecification();
        
        
        // food
        List<AbstractGoods> ratios;
        List<GoodsType> goodsTypes = spec.getFoodGoodsTypeList();
        for (GoodsType goodsType : goodsTypes) {
            gross += getColony().getProductionOf(goodsType);
            net += getColony().getProductionNetOf(goodsType);
        }
        if (net != 0) {
            GoodsType goodsType = spec.getGoodsType("model.goods.food");
            netProductionPanel.add(new ProductionLabel(goodsType, net, getCanvas()));
//            ratios = new ArrayList<AbstractGoods>();
//            for (GoodsType goodsType : goodsTypes) {
//                // get food production proportions so we can represent surplus in the same ratios
//                ratios.add(new AbstractGoods(goodsType, colony.getProductionOf(goodsType) * net / gross));
//            }
//            netProductionPanel.add(new ProductionMultiplesLabel(ratios, getCanvas()));
        }
        
        // liberty
        gross = net = 0;
        goodsTypes = spec.getLibertyGoodsTypeList();
        for (GoodsType goodsType : goodsTypes) {
            gross += getColony().getProductionOf(goodsType);
            net += getColony().getProductionNetOf(goodsType);
        }
        if (net != 0) {
            GoodsType goodsType = spec.getGoodsType("model.goods.bells");
            netProductionPanel.add(new ProductionLabel(goodsType, net, getCanvas()));
//            ratios = new ArrayList<AbstractGoods>();
//            for (GoodsType goodsType : goodsTypes) {
//                ratios.add(new AbstractGoods(goodsType, colony.getProductionOf(goodsType) * net / gross));
//            }
//            netProductionPanel.add(new ProductionMultiplesLabel(ratios, getCanvas()));
        }
        
        
        // immigration
        gross = net = 0;
        goodsTypes = spec.getImmigrationGoodsTypeList();
        for (GoodsType goodsType : goodsTypes) {
            gross += getColony().getProductionOf(goodsType);
            net += getColony().getProductionNetOf(goodsType);
        }
        if (net != 0) {
            GoodsType goodsType = spec.getGoodsType("model.goods.crosses");
            netProductionPanel.add(new ProductionLabel(goodsType, net, getCanvas()));
//            ratios = new ArrayList<AbstractGoods>();
//            for (GoodsType goodsType : goodsTypes) {
//                ratios.add(new AbstractGoods(goodsType, colony.getProductionOf(goodsType) * net / gross));
//            }
//            netProductionPanel.add(new ProductionMultiplesLabel(ratios, getCanvas()));
        }
        
        
        
        List<GoodsType> generalGoods = new ArrayList<GoodsType>(spec.getGoodsTypeList());
        generalGoods.removeAll(spec.getFoodGoodsTypeList());
        generalGoods.removeAll(spec.getLibertyGoodsTypeList());
        generalGoods.removeAll(spec.getImmigrationGoodsTypeList());
        generalGoods.removeAll(spec.getFarmedGoodsTypeList());
        
        // non-storable goods
        goodsTypes = new ArrayList<GoodsType>(generalGoods);
        for (GoodsType goodsType : goodsTypes) {
            if (!goodsType.isStorable()) {
                generalGoods.remove(goodsType);
                net = getColony().getProductionNetOf(goodsType);
                if (net != 0) {
                    netProductionPanel.add(new ProductionLabel(goodsType, net, getCanvas()));
                }
            }
        }
        
        
        // farmed goods
        goodsTypes = new ArrayList<GoodsType>(spec.getFarmedGoodsTypeList());
        goodsTypes.removeAll(spec.getFoodGoodsTypeList());
        goodsTypes.removeAll(spec.getLibertyGoodsTypeList());
        goodsTypes.removeAll(spec.getImmigrationGoodsTypeList());
        for (GoodsType goodsType : goodsTypes) {
            net = getColony().getProductionNetOf(goodsType);
            if (net != 0) {
                netProductionPanel.add(new ProductionLabel(goodsType, net, getCanvas()));
            }
        }
        
        
        // everything left except military & breedables
        goodsTypes = new ArrayList<GoodsType>(generalGoods);
        for (GoodsType goodsType : goodsTypes) {
            if (!goodsType.isMilitaryGoods() && !goodsType.isBreedable()) {
                generalGoods.remove(goodsType);
                net = getColony().getProductionNetOf(goodsType);
                if (net != 0) {
                    netProductionPanel.add(new ProductionLabel(goodsType, net, getCanvas()));
                }
            }
        }
        
        
        // military goods
        goodsTypes = new ArrayList<GoodsType>(generalGoods);
        for (GoodsType goodsType : goodsTypes) {
            if (!goodsType.isBreedable()) {
                generalGoods.remove(goodsType);
                net = getColony().getProductionNetOf(goodsType);
                if (net != 0) {
                    netProductionPanel.add(new ProductionLabel(goodsType, net, getCanvas()));
                }
            }
        }
        
        
        // breedable things go last
        goodsTypes = new ArrayList<GoodsType>(generalGoods);
        for (GoodsType goodsType : goodsTypes) {
            generalGoods.remove(goodsType);
            net = getColony().getProductionNetOf(goodsType);
            if (net != 0) {
                netProductionPanel.add(new ProductionLabel(goodsType, net, getCanvas()));
            }
        }
        
/*
        GoodsType grain = getSpecification().getGoodsType("model.goods.food");
        int food = 0;

        List<AbstractGoods> foodProduction = new ArrayList<AbstractGoods>();
        for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
            int production = colony.getProductionOf(goodsType);
            if (production != 0) {
                if (goodsType.isFoodType()) {
                    foodProduction.add(new AbstractGoods(goodsType, production));
                    food += production;
                } else if (goodsType.isBreedable()) {
                    ProductionLabel horseLabel = new ProductionLabel(goodsType, production, getCanvas());
                    horseLabel.setMaxGoodsIcons(1);
                    netProductionPanel.add(horseLabel);
                } else if (goodsType.isImmigrationType() || goodsType.isLibertyType()) {
                    int consumption = colony.getConsumption(goodsType);
                    ProductionLabel bellsLabel = new ProductionLabel(goodsType, production, getCanvas());
                    bellsLabel.setToolTipPrefix(Messages.message("totalProduction"));
                    if (consumption != 0) {
                        int surplus = production - consumption;
                        ProductionLabel surplusLabel = new ProductionLabel(goodsType, surplus, getCanvas());
                        surplusLabel.setToolTipPrefix(Messages.message("surplusProduction"));
                        netProductionPanel.add(surplusLabel, 0);
                    }
                    netProductionPanel.add(bellsLabel, 0);
                } else {
                    production = colony.getProductionNetOf(goodsType);
                    netProductionPanel.add(new ProductionLabel(goodsType, production, getCanvas()));
                }
            } 
        }
        
        netProductionPanel.add(new ProductionLabel(grain, food - colony.getFoodConsumption(), getCanvas()), 0);

        ProductionMultiplesLabel label = new ProductionMultiplesLabel(foodProduction, getCanvas());
        label.setToolTipPrefix(Messages.message("totalProduction"));
        netProductionPanel.add(label, 0);
*/
        netProductionPanel.revalidate();
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
                if (getCanvas().showFreeColDialog(new WarehouseDialog(getCanvas(), getColony()))) {
                    warehousePanel.update();
                }
                break;
            case BUILDQUEUE:
                getCanvas().showSubPanel(new BuildQueuePanel(getColony(), getCanvas()));
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
                getController().unloadCargo(goods, false);
            }
            Iterator<Unit> unitIterator = unit.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit newUnit = unitIterator.next();
                getController().leaveShip(newUnit);
            }
            cargoPanel.initialize();
            outsideColonyPanel.initialize();
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
                if (goods.getAmount() < 100 && getColony().getGoodsCount(goods.getType()) > 0) {
                    int amount = Math.min(100 - goods.getAmount(), getColony().getGoodsCount(goods.getType()));
                    getController().loadCargo(new Goods(goods.getGame(), getColony(), goods.getType(), amount), unit);
                }
            }
        }
    }

    /**
     * Closes the <code>ColonyPanel</code>.
     */
    public void closeColonyPanel() {
        Canvas canvas = getCanvas();
        if (getColony().getUnitCount() == 0) {
            if (canvas.showConfirmDialog("abandonColony.text",
                                         "abandonColony.yes",
                                         "abandonColony.no")) {
                canvas.remove(this);
                getController().abandonColony(getColony());
            }
        } else {
            BuildableType buildable = colony.getCurrentlyBuilding();
            if (buildable != null
                && buildable.getPopulationRequired() > colony.getUnitCount()
                && !canvas.showConfirmDialog(null, StringTemplate.template("colonyPanel.reducePopulation")
                                             .addName("%colony%", colony.getName())
                                             .addAmount("%number%", buildable.getPopulationRequired())
                                             .add("%buildable%", buildable.getNameKey()),
                                             "ok", "cancel")) {
                return;
            }
            canvas.remove(this);
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
                Unit activeUnit = canvas.getGUI().getActiveUnit();
                if (activeUnit == null || activeUnit.getTile() == null || activeUnit.getMovesLeft() <= 0
                    || (!(activeUnit.getLocation() instanceof Tile) && !(activeUnit.isOnCarrier()))) {
                    canvas.getGUI().setActiveUnit(null);
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
        if (this.colony != null) {
            this.colony.removePropertyChangeListener(this);
            this.colony.getTile().removePropertyChangeListener(this);
        }
        this.colony = colony;
        if (this.colony != null) {
            this.colony.addPropertyChangeListener(this);
            this.colony.getTile().addPropertyChangeListener(this);
        }
        editable = (colony.getOwner() == getMyPlayer());
    }

    /**
     * This panel shows the content of a carrier in the colony
     */
    public final class ColonyCargoPanel extends CargoPanel {

        public ColonyCargoPanel(Canvas canvas) {
            super(canvas, true);
            setLayout(new MigLayout("wrap 4, fill"));
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
            setLayout(new MigLayout("fill, wrap 4, gap 0:10:10:push"));
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
                    public void mousePressed(MouseEvent e) {
                        getCanvas().showSubPanel(new BuildQueuePanel(colony, getCanvas()));
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
        }
        
        public void update(){
        	initialize();
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
     * A panel that holds UnitLabels that represent Units that are standing in
     * front of a colony.
     */
    public final class OutsideColonyPanel extends JPanel implements PropertyChangeListener {

        private Colony colony;

        public OutsideColonyPanel() {
            super(new MigLayout("wrap 5, fill"));
            setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                                                       Messages.message("outsideColony")));
        }

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
            	// we only deal with land, non-carrier units here
            	if(unit.isNaval() || unit.isCarrier()){
            		continue;
            	}

                UnitLabel unitLabel = new UnitLabel(unit, getCanvas());
                if (isEditable()) {
                    unitLabel.setTransferHandler(defaultTransferHandler);
                    unitLabel.addMouseListener(pressListener);
                }
                add(unitLabel, false);
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

        public InPortPanel() {
            super(new MigLayout("wrap 3, fill"));
            setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                                                       Messages.message("inPort")));
        }

        @Override
        public String getUIClassID() {
            return "InPortPanelUI";
        }
        
        public void initialize(Unit selectedUnit) {
            // This is required
            UnitLabel oldSelectedUnitLabel = ColonyPanel.this.getSelectedUnitLabel();
            if(oldSelectedUnitLabel != null){
                if(selectedUnit == null){
                    selectedUnit = oldSelectedUnitLabel.getUnit();
                }
                ColonyPanel.this.setSelectedUnit(null);
            }
            
            removeAll();
            if (colony == null) {
                return;
            }

            Tile colonyTile = colony.getTile();
            Unit lastCarrier = null;
            for (Unit unit : colonyTile.getUnitList()) {
                if(!unit.isCarrier()){
                    continue;
                }
                
                lastCarrier = unit;
                UnitLabel unitLabel = new UnitLabel(unit, getCanvas());
                TradeRoute tradeRoute = unit.getTradeRoute();
                if (tradeRoute != null) {
                    unitLabel.setDescriptionLabel(Messages.message(Messages.getLabel(unit))
                                                  + " (" + tradeRoute.getName() + ")");
                }
                if (isEditable()) {
                    unitLabel.setTransferHandler(defaultTransferHandler);
                    unitLabel.addMouseListener(pressListener);
                }
                add(unitLabel);
            }
            revalidate();
            repaint();
            
            // last carrier is selected by default, if no other should be
            if(selectedUnit == null && lastCarrier != null){
                selectedUnit = lastCarrier;
            }
            // select the unit
            if(selectedUnit != null){
                ColonyPanel.this.setSelectedUnit(selectedUnit);
            }
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
            setLayout(new MigLayout("fill, gap push"));
        }

        public void initialize() {
            // get notified of warehouse changes
            getColony().getGoodsContainer().addPropertyChangeListener(this);
            update();
            revalidate();
            repaint();
        }

        private void update() {
            removeAll();
            for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
                if (goodsType.isStorable()) {
                    Goods goods = getColony().getGoodsContainer().getGoods(goodsType);
                    if (goods.getAmount() >= getClient().getClientOptions()
                        .getInteger(ClientOptions.MIN_NUMBER_FOR_DISPLAYING_GOODS)) {
                        GoodsLabel goodsLabel = new GoodsLabel(goods, getCanvas());
                        if (colonyPanel.isEditable()) {
                            goodsLabel.setTransferHandler(defaultTransferHandler);
                            goodsLabel.addMouseListener(pressListener);
                        }
                        add(goodsLabel, false);
                    }
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
        private Tile[][] tiles = new Tile[3][3];

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
            Tile tile = getColony().getTile();
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
                    if (tiles[x][y] != null) {
                        ColonyTile colonyTile = getColony().getColonyTile(tiles[x][y]);
                        ASingleTilePanel p = new ASingleTilePanel(colonyTile, x, y);
                        add(p, new Integer(layer));
                        layer++;
                    }
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
            Game game = getColony().getGame();

            g.setColor(Color.black);
            g.fillRect(0, 0, getWidth(), getHeight());

            TileType tileType = getColony().getTile().getType();
            int tileWidth = getLibrary().getTerrainImageWidth(tileType) / 2;
            int tileHeight = getLibrary().getTerrainImageHeight(tileType) / 2;
            if (getColony() != null) {
                for (int x = 0; x < 3; x++) {
                    for (int y = 0; y < 3; y++) {
                        if (tiles[x][y] != null) {
                            int xx = ((2 - x) + y) * tileWidth;
                            int yy = (x + y) * tileHeight;
                            g.translate(xx, yy);
                            colonyTileGUI.displayColonyTile((Graphics2D) g, game.getMap(), tiles[x][y], getColony());
                            g.translate(-xx, -yy);
                        }
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
                            (x + y) * getLibrary().getTerrainImage(tileType, 0, 0).getHeight(null) / 2);
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
             */
            private void initializeAsCenterTile() {

                setLayout(new GridLayout(2, 1));

                TileType tileType = colonyTile.getTile().getType();

                AbstractGoods primaryGoods = tileType.getPrimaryGoods();
                if (primaryGoods != null) {
                    GoodsType goodsType = primaryGoods.getType();
                    ImageIcon goodsIcon = getLibrary().getGoodsImageIcon(goodsType);
                    ProductionLabel pl =
                        new ProductionLabel(goodsType, colonyTile.getProductionOf(goodsType),
                                            getCanvas());
                    pl.setSize(getLibrary().getTerrainImageWidth(tileType), goodsIcon.getIconHeight());
                    add(pl);
                }

                AbstractGoods secondaryGoods = tileType.getSecondaryGoods();
                if (secondaryGoods != null) {
                    GoodsType goodsType = secondaryGoods.getType();
                    ImageIcon goodsIcon = getLibrary().getGoodsImageIcon(goodsType);
                    ProductionLabel pl =
                        new ProductionLabel(goodsType, colonyTile.getProductionOf(goodsType),
                                            getCanvas());
                    pl.setSize(getLibrary().getTerrainImageWidth(tileType), goodsIcon.getIconHeight());
                    add(pl);
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
            private void updateDescriptionLabel(UnitLabel unit, boolean toAdd) {
                String tileDescription = Messages.message(this.colonyTile.getLabel());

                if (unit == null) {
                    setToolTipText(tileDescription);
                } else {
                    String unitDescription = Messages.message(Messages.getLabel(unit.getUnit()));
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

                        logger.info("Colony " + getColony().getName()
                                    + " claims tile " + tile.toString()
                                    + " with unit " + unit.getId());
                        if ((tile.getOwner() != player
                             || tile.getOwningSettlement() != getColony())
                            && !getController().claimLand(tile, getColony(), 0)) {
                            logger.warning("Colony " + getColony().getName()
                                           + " could not claim tile " + tile.toString()
                                           + " with unit " + unit.getId());
                            return null;
                        }

                        if (colonyTile.canAdd(unit)) {
                            oldParent.remove(comp);

                            GoodsType workType = colonyTile.getWorkType(unit);

                            getController().work(unit, colonyTile);
                            // check whether worktype is suitable
                            if (workType != unit.getWorkType()) {
                                getController().changeWorkType(unit, workType);
                            }

                            ((UnitLabel) comp).setSmall(false);

                            if (getClient().getClientOptions().getBoolean(ClientOptions.SHOW_NOT_BEST_TILE)) {
                                ColonyTile bestTile = getColony().getVacantColonyTileFor(unit, false, workType);
                                if (colonyTile != bestTile
                                    && (colonyTile.getProductionOf(unit, workType)
                                        < bestTile.getProductionOf(unit, workType))) {
                                    StringTemplate template = StringTemplate.template("colonyPanel.notBestTile")
                                        .addStringTemplate("%unit%", Messages.getLabel(unit))
                                        .add("%goods%", workType.getNameKey())
                                        .addStringTemplate("%tile%", bestTile.getLabel());
                                    getCanvas().showInformationMessage(template);
                                }
                            }
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
                                if(colonyTile.getUnitCount() > 0) { // Tile is already occupied
                                    ;
                                } else if (!workTile.isLand()) { // no docks
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
                int w = getWidth();
                int h = getHeight();
                int dx = Math.abs(w/2 - px);
                int dy = Math.abs(h/2 - py);
                return (dx + w * dy / h) <= w/2;
            }
        }
    }
    
    public void propertyChange(PropertyChangeEvent e) {
        if (!isShowing() || getColony() == null) {
            return;
        }
        String property = e.getPropertyName();

        if (Unit.CARGO_CHANGE.equals(property)) {
            updateInPortPanel();
        } else if (ColonyChangeEvent.POPULATION_CHANGE.toString().equals(property)) {
            updateSoLLabel();
        } else if (ColonyChangeEvent.BONUS_CHANGE.toString().equals(property)) {
            ModelMessage msg = getColony().checkForGovMgtChangeMessage();
            if (msg != null) {
                getCanvas().showInformationMessage(msg);
            }
            updateSoLLabel();
        } else if (ColonyChangeEvent.UNIT_TYPE_CHANGE.toString().equals(property)) {
            FreeColGameObject object = (FreeColGameObject) e.getSource();
            String oldType = (String) e.getOldValue();
            String newType = (String) e.getNewValue();
            getCanvas().showInformationMessage(object,
                                               "model.colony.unitChange",
                                               "%oldType%", oldType,
                                               "%newType%", newType);
            updateTilePanel();
        } else if (ColonyTile.UNIT_CHANGE.toString().equals(property)) {
            updateTilePanel();
            updateProductionPanel();
        } else if (property.startsWith("model.goods.")) {
            // changes to warehouse goods count may affect building production
            //which requires a view update
            updateProductionPanel();
            updateWarehousePanel();
            buildingsPanel.update();
        } else if (Building.UNIT_CHANGE.equals(property)) {
            // already processed by BuildingPanel
        } else if (Tile.UNIT_CHANGE.equals(property)) {
            outsideColonyPanel.initialize();
        } else {
            logger.warning("Unknown property change event: " + e.getPropertyName());
        }
    }

}
