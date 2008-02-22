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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Feature;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Colopedia.
 */
public final class ColopediaPanel extends FreeColPanel implements ActionListener, TreeSelectionListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ColopediaPanel.class.getName());

    public static enum PanelType { TERRAIN, RESOURCES, UNITS, GOODS, SKILLS, BUILDINGS, FATHERS }

    private static final TileImprovementType road = FreeCol.getSpecification()
        .getTileImprovementType("model.improvement.Road");
    private static final TileImprovementType river = FreeCol.getSpecification()
        .getTileImprovementType("model.improvement.River");
    private static final TileImprovementType plowing = FreeCol.getSpecification()
        .getTileImprovementType("model.improvement.Plow");

    private static final String OK = "OK";
    private static final String ROOT = "ROOT";

    // layout of production modifier panel
    private static final int MODIFIERS_PER_ROW = 5;

    private final Canvas parent;

    private final ImageLibrary library;

    private JLabel header;

    private JPanel listPanel;

    private JPanel detailPanel;

    private JButton ok;
    
    private JTree tree;

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ColopediaPanel(Canvas parent) {
        super(new FlowLayout(FlowLayout.CENTER, 1000, 10));
        this.parent = parent;
        this.library = parent.getGUI().getImageLibrary();

        setLayout(new BorderLayout());

        header = getDefaultHeader(Messages.message("menuBar.colopedia"));
        add(header, BorderLayout.NORTH);

        listPanel = new JPanel();
        listPanel.setOpaque(false);
        // listPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        JScrollPane sl = new JScrollPane(listPanel, 
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sl.getViewport().setOpaque(false);
        add(sl, BorderLayout.WEST);

        detailPanel = new JPanel();
        detailPanel.setOpaque(false);
        detailPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        add(detailPanel, BorderLayout.CENTER);

        ok = new JButton(Messages.message("ok"));
        ok.setActionCommand(String.valueOf(OK));
        ok.addActionListener(this);
        enterPressesWhenFocused(ok);
        setCancelComponent(ok);
        add(ok, BorderLayout.SOUTH);

        setSize(850, 600);
    }
    
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(850, 600);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    /**
     * Prepares this panel to be displayed.
     * 
     * @param type - the panel type
     */
    public void initialize(PanelType type) {
        initialize(type, null);
    }

    /**
     * Prepares this panel to be displayed.
     * 
     * @param panelType - the panel type
     * @param type - the FreeColGameObjectType of the item to be displayed
     */
    public void initialize(PanelType panelType, FreeColGameObjectType type) {
        listPanel.removeAll();
        detailPanel.removeAll();
        tree = buildTree();
        tree.expandRow(panelType.ordinal());

        switch (panelType) {
        case TERRAIN:
            buildTerrainDetail((TileType) type);
            break;
        case RESOURCES:
            buildResourceDetail((ResourceType) type);
            break;
        case UNITS:
        case SKILLS:
            buildUnitDetail((UnitType) type);
            break;
        case GOODS:
            buildGoodsDetail((GoodsType) type);
            break;
        case BUILDINGS:
            buildBuildingDetail((BuildingType) type);
            break;
        case FATHERS:
            buildFatherDetail((FoundingFather) type);
            break;
        default:
            break;
        }
        detailPanel.validate();
    }

    /**
     * 
     */
    @Override
    public void requestFocus() {
        ok.requestFocus();
    }
    
    /**
     * Builds the JTree which represents the navigation menu and then returns it
     * 
     * @return The navigation tree.
     */
    private JTree buildTree() {
        DefaultMutableTreeNode root;
        root = new DefaultMutableTreeNode(new ColopediaTreeItem(null, Messages.message("menuBar.colopedia")));
        
        DefaultMutableTreeNode terrain;
        terrain = new DefaultMutableTreeNode(new ColopediaTreeItem(PanelType.TERRAIN,
                                                                   Messages.message("menuBar.colopedia.terrain")));
        buildTerrainSubtree(terrain);
        root.add(terrain);
        
        DefaultMutableTreeNode resource;
        resource = new DefaultMutableTreeNode(new ColopediaTreeItem(PanelType.RESOURCES, 
                                                                    Messages.message("menuBar.colopedia.resource")));
        buildResourceSubtree(resource);
        root.add(resource);
        
        DefaultMutableTreeNode units;
        units = new DefaultMutableTreeNode(new ColopediaTreeItem(PanelType.UNITS,
                                                                 Messages.message("menuBar.colopedia.unit")));
        buildUnitSubtree(units);
        root.add(units);
        
        DefaultMutableTreeNode goods;
        goods = new DefaultMutableTreeNode(new ColopediaTreeItem(PanelType.GOODS,
                                                                 Messages.message("menuBar.colopedia.goods")));
        buildGoodsSubtree(goods);
        root.add(goods);
        
        DefaultMutableTreeNode skills;
        skills = new DefaultMutableTreeNode(new ColopediaTreeItem(PanelType.SKILLS,
                                                                  Messages.message("menuBar.colopedia.skill")));
        buildSkillsSubtree(skills);
        root.add(skills);
        
        DefaultMutableTreeNode buildings;
        buildings = new DefaultMutableTreeNode(new ColopediaTreeItem(PanelType.BUILDINGS,
                                                                     Messages.message("menuBar.colopedia.building")));
        buildBuildingSubtree(buildings);
        root.add(buildings);
        
        DefaultMutableTreeNode fathers;
        fathers = new DefaultMutableTreeNode(new ColopediaTreeItem(PanelType.FATHERS,
                                                                   Messages.message("menuBar.colopedia.father")));
        buildFathersSubtree(fathers);
        root.add(fathers);
        
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(200, super.getPreferredSize().height);
            }
        };
        tree.setRootVisible(false);
        tree.setCellRenderer(new ColopediaTreeCellRenderer());
        tree.setOpaque(false);
        tree.addTreeSelectionListener(this);
        
        listPanel.setLayout(new GridLayout(0, 1));
        listPanel.add(tree);

        return tree;
    }
    
    /**
     * Builds the buttons for all the tiles.
     * @param parent
     */
    private void buildTerrainSubtree(DefaultMutableTreeNode parent) {
        for (TileType t : FreeCol.getSpecification().getTileTypeList()) {
            buildTerrainItem(t, parent);
        }
    }
    
    /**
     * Builds the buttons for all the resources.
     * @param parent
     */
    private void buildResourceSubtree(DefaultMutableTreeNode parent) {
        for (ResourceType r : FreeCol.getSpecification().getResourceTypeList()) {
            buildResourceItem(r, parent);
        }
    }
    
    /**
     * Builds the buttons for all the units.
     * @param parent
     */
    private void buildUnitSubtree(DefaultMutableTreeNode parent) {
        for (UnitType u : FreeCol.getSpecification().getUnitTypeList()) {
            if (u.getSkill() <= 0) {
                buildUnitItem(u, 0.5f, parent);
            }
        }
    }
    
    /**
     * Builds the buttons for all the goods.
     * @param parent
     */
    private void buildGoodsSubtree(DefaultMutableTreeNode parent) {
        for (GoodsType g : FreeCol.getSpecification().getGoodsTypeList()) {
            buildGoodsItem(g, parent);
        }
    }
    
    /**
     * Builds the buttons for all the skills.
     * @param parent
     */
    private void buildSkillsSubtree(DefaultMutableTreeNode parent) {
        for (UnitType u : FreeCol.getSpecification().getUnitTypeList()) {
            if (u.getSkill() > 0) {
                buildUnitItem(u, 0.5f, parent);
            }
        }
    }
    
    /**
     * Builds the buttons for all the buildings.
     * @param parent
     */
    private void buildBuildingSubtree(DefaultMutableTreeNode parent) {
        Image buildingImage = (Image) UIManager.get("Colopedia.buildingSection.image");
        ImageIcon buildingIcon = new ImageIcon((buildingImage != null) ? buildingImage : null);

        List<BuildingType> buildingTypes = FreeCol.getSpecification().getBuildingTypeList();
        for (BuildingType buildingType : buildingTypes) {
            DefaultMutableTreeNode item = new DefaultMutableTreeNode(new ColopediaTreeItem(buildingType, 
                                                                                           buildingType.getName(),
                                                                                           buildingIcon));
            parent.add(item);
        }
    }
    
    /**
     * Builds the buttons for all the founding fathers.
     * @param parent
     */
    private void buildFathersSubtree(DefaultMutableTreeNode parent) {
        for (FoundingFather foundingFather : FreeCol.getSpecification().getFoundingFathers()) {
            buildFatherItem(foundingFather, parent);
        }
    }
    
    /**
     * Builds the button for the given tile.
     * 
     * @param tileType - the TileType
     * @param parent - the parent node
     */
    private void buildTerrainItem(TileType tileType, DefaultMutableTreeNode parent) {
        ImageIcon icon = new ImageIcon(library.getScaledTerrainImage(tileType, 0.25f));
        DefaultMutableTreeNode item = new DefaultMutableTreeNode(new ColopediaTreeItem(tileType, 
                                                                                       tileType.getName(), icon));
        parent.add(item);
    }
    
    /**
     * Builds the button for the given resource.
     * 
     * @param resType - the ResourceType
     * @param parent - the parent node
     */
    private void buildResourceItem(ResourceType resType, DefaultMutableTreeNode parent) {
        ImageIcon icon = library.getScaledBonusImageIcon(resType, 0.75f);
        DefaultMutableTreeNode item = new DefaultMutableTreeNode(new ColopediaTreeItem(resType,
                                                                                       resType.getName(),
                                                                                       icon));
        parent.add(item);
    }
    
    /**
     * Builds the button for the given unit.
     * 
     * @param unitType
     * @param scale
     * @param parent
     */
    private void buildUnitItem(UnitType unitType, float scale, DefaultMutableTreeNode parent) {
        ImageIcon icon = library.getScaledImageIcon(library.getUnitImageIcon(unitType), 0.5f);
        DefaultMutableTreeNode item = new DefaultMutableTreeNode(new ColopediaTreeItem(unitType,
                                                                                       unitType.getName(), icon));
        parent.add(item);
    }
    
    /**
     * Builds the button for the given goods.
     * 
     * @param goodsType The GoodsType
     * @param parent The parent tree node
     */
    private void buildGoodsItem(GoodsType goodsType, DefaultMutableTreeNode parent) {
        ImageIcon icon = library.getScaledGoodsImageIcon(goodsType, 0.75f);
        DefaultMutableTreeNode item = new DefaultMutableTreeNode(new ColopediaTreeItem(goodsType,
                                                                                       goodsType.getName(), icon));
        parent.add(item);
    }
    
    /**
     * Builds the button for the given founding father.
     * 
     * @param foundingFather
     * @param parent
     */
    private void buildFatherItem(FoundingFather foundingFather, DefaultMutableTreeNode parent) {
        String name = foundingFather.getName();
        ImageIcon icon = library.getScaledGoodsImageIcon(Goods.BELLS, 0.75f);
        DefaultMutableTreeNode item = new DefaultMutableTreeNode(new ColopediaTreeItem(foundingFather,
                                                                                       name, icon));
        parent.add(item);
    }

    private JButton getButton(String text, ImageIcon icon) {
        JButton button = new JButton(text, icon);
        button.setOpaque(false);
        button.setForeground(LINK_COLOR);
        button.setBorder(BorderFactory.createEmptyBorder());
        return button;
    }

    private JButton getGoodsButton(final GoodsType goodsType, int amount) {
        if (amount > 0) {
            return getGoodsButton(goodsType, String.valueOf(amount));
        } else {
            return getGoodsButton(goodsType, null);
        }
    }

    private JButton getGoodsButton(final GoodsType goodsType, String text) {
        JButton goodsButton = getButton(text, library.getGoodsImageIcon(goodsType));
        goodsButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    initialize(PanelType.GOODS, goodsType);
                }
            });
        return goodsButton;
    }

    private JButton getResourceButton(final ResourceType resourceType) {
        JButton resourceButton = getButton(resourceType.getName(), 
                                           library.getBonusImageIcon(resourceType));
        resourceButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    initialize(PanelType.RESOURCES, resourceType);
                }
            });
        return resourceButton;
    }

    private JButton getBuildingButton(final BuildingType buildingType) {
        JButton buildingButton = getButton(buildingType.getName(), null);
        buildingButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    initialize(PanelType.BUILDINGS, buildingType);
                }
            });
        return buildingButton;
    }


    private JButton getUnitButton(final UnitType unitType) {
        JButton unitButton = getButton(unitType.getName(), library.getUnitImageIcon(unitType));
        unitButton.setHorizontalAlignment(SwingConstants.LEFT);
        unitButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    if (unitType.hasSkill()) {
                        initialize(PanelType.SKILLS, unitType);
                    } else {
                        initialize(PanelType.UNITS, unitType);
                    }
                }
            });
        return unitButton;
    }


    /**
     * Builds the details panel for the given tile.
     * 
     * @param tileType The TileType
     */
    private void buildTerrainDetail(TileType tileType) {
        detailPanel.removeAll();
        detailPanel.repaint();
        if (tileType == null) {
            return;
        }

        int[] widths = { 0, 3 * margin, 0 };
        int[] heights = new int[13];
        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = margin;
        }
        int row = 1;
        int leftColumn = 1;
        int rightColumn = 3;

        HIGLayout layout = new HIGLayout(widths, heights);
        layout.setColumnWeight(rightColumn, 1);
        detailPanel.setLayout(layout);

        String movementCost = String.valueOf(tileType.getBasicMoveCost() / 3);
        String defenseBonus = "";
        Modifier defence = tileType.getDefenceBonus();
        if (defence != null) {
            defenseBonus = getModifierAsString(defence);
        }

        GoodsType secondaryGoodsType = tileType.getSecondaryGoods();

        JLabel nameLabel = new JLabel(tileType.getName(), SwingConstants.CENTER);
        nameLabel.setFont(smallHeaderFont);
        detailPanel.add(nameLabel, higConst.rcwh(row, leftColumn, widths.length, 1));
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.terrainImage")),
                        higConst.rc(row, leftColumn));
        Image terrainImage = library.getScaledTerrainImage(tileType, 1f);
        detailPanel.add(new JLabel(new ImageIcon(terrainImage)), higConst.rc(row, rightColumn, "l"));
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.movementCost")),
                        higConst.rc(row, leftColumn));
        detailPanel.add(new JLabel(movementCost), higConst.rc(row, rightColumn));
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.defenseBonus")), 
                        higConst.rc(row, leftColumn));
        detailPanel.add(new JLabel(defenseBonus), higConst.rc(row, rightColumn));
        row += 2;

        List<ResourceType> resourceList = tileType.getResourceTypeList();
        if (resourceList.size() > 0) {
            detailPanel.add(new JLabel(Messages.message("colopedia.terrain.resource")), higConst.rc(row, leftColumn));
            JPanel resourcePanel = new JPanel();
            resourcePanel.setOpaque(false);
            for (final ResourceType resourceType : resourceList) {
                resourcePanel.add(getResourceButton(resourceType));
            }
            detailPanel.add(resourcePanel, higConst.rc(row, rightColumn, "l"));
            row += 2;
        }

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.production")), higConst.rc(row, leftColumn));
        JPanel goodsPanel = new JPanel();
        goodsPanel.setOpaque(false);
        List<AbstractGoods> production = tileType.getProduction();
        for (final AbstractGoods goods : production) {
            goodsPanel.add(getGoodsButton(goods.getType(), goods.getAmount()));
        }
        detailPanel.add(goodsPanel, higConst.rc(row, rightColumn, "l"));
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.description")), 
                        higConst.rc(row, leftColumn, "tl"));
        detailPanel.add(getDefaultTextArea(tileType.getDescription()), higConst.rc(row, rightColumn));

        detailPanel.validate();
    }

    /**
     * Builds the details panel for the given resource.
     * 
     * @param type The ResourceType
     */
    private void buildResourceDetail(ResourceType type) {
        detailPanel.removeAll();
        detailPanel.repaint();
        if (type == null) {
            return;
        }

        int[] widths = { 0, 3 * margin, 0 };
        int[] heights = new int[7];
        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = margin;
        }

        int row = 1;
        int leftColumn = 1;
        int rightColumn = 3;

        HIGLayout layout = new HIGLayout(widths, heights);
        layout.setColumnWeight(rightColumn, 1);
        detailPanel.setLayout(layout);

        JLabel name = new JLabel(type.getName(), SwingConstants.CENTER);
        name.setFont(smallHeaderFont);
        detailPanel.add(name, higConst.rcwh(row, leftColumn, widths.length, 1));
        row += 2;

        Map<GoodsType, Modifier> modifiers = type.getModifiers();

        detailPanel.add(new JLabel(Messages.message("colopedia.resource.bonusProduction")),
                        higConst.rc(row, leftColumn));
        JPanel goodsPanel = new JPanel();
        goodsPanel.setOpaque(false);
        for (Entry<GoodsType, Modifier> entry : modifiers.entrySet()) {
            String text = getModifierAsString(entry.getValue());
            JButton goodsButton = getGoodsButton(entry.getKey(), text);
            goodsPanel.add(goodsButton);
        }
        detailPanel.add(goodsPanel, higConst.rc(row, rightColumn, "l"));
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.resource.description")),
                        higConst.rc(row, leftColumn, "tl"));
        detailPanel.add(getDefaultTextArea(type.getDescription()),
                        higConst.rc(row, rightColumn));

        detailPanel.validate();
    }

    /**
     * Builds the details panel for the given unit.
     * 
     * @param type - the UnitType
     */
    private void buildUnitDetail(UnitType type) {
        detailPanel.removeAll();
        detailPanel.repaint();
        if (type == null) {
            return;
        }

        Player player = parent.getClient().getMyPlayer();
        // player can be null when using the map editor
        Europe europe = (player==null) ? null : player.getEurope();

        // the number of rows required for layout
        int rowsRequired = 5;

        String price = null;
        if (europe != null && europe.getUnitPrice(type) > 0) {
            price = String.valueOf(europe.getUnitPrice(type));
        } else if (type.getPrice() > 0) {
            price = String.valueOf(type.getPrice());
        }

        if (price != null) {
            rowsRequired++;
        }

        String capacity = null;
        if (type.hasAbility("model.unit.carryGoods") || 
            type.hasAbility("model.unit.carryUnits")) {
            capacity = String.valueOf(type.getSpace());
            rowsRequired++;
        }

        JPanel goodsRequired = null;
        if (type.getGoodsRequired() != null) {
            rowsRequired++;
            goodsRequired = new JPanel();
            goodsRequired.setOpaque(false);
            for (final AbstractGoods goods : type.getGoodsRequired()) {
                goodsRequired.add(getGoodsButton(goods.getType(), goods.getAmount()));
            }
        }

        String skill = null;
        JPanel schoolPanel = null;
        if (type.hasSkill()) {
            rowsRequired += 2;
            skill = String.valueOf(type.getSkill());
            schoolPanel = new JPanel();
            schoolPanel.setOpaque(false);
            for (final BuildingType buildingType : FreeCol.getSpecification().getBuildingTypeList()) {
                if (buildingType.hasAbility("model.ability.teach") && 
                    buildingType.canAdd(type)) {
                    schoolPanel.add(getBuildingButton(buildingType));
                }
            }
        }

        JPanel productionPanel = null;
        List<Modifier> bonusList = new ArrayList<Modifier>();
        for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
            Modifier productionBonus = type.getModifier(goodsType.getId());
            if (productionBonus != null) {
                bonusList.add(productionBonus);
            }
        }
        int bonusNumber = bonusList.size();
        if (bonusNumber > 0) {
            int rows = bonusNumber / MODIFIERS_PER_ROW;
            if (bonusNumber % MODIFIERS_PER_ROW != 0) {
                rows++;
            }
            int widths[] = new int[2 * MODIFIERS_PER_ROW - 1];
            for (int index = 1; index < widths.length; index += 2) {
                widths[index] = 3 * margin;
            }
            int heights[] = new int[2 * rows - 1];
            for (int index = 1; index < heights.length; index += 2) {
                heights[index] = margin;
            }
            productionPanel = new JPanel(new HIGLayout(widths, heights));
            productionPanel.setOpaque(false);
            rowsRequired++;

            int row = 1;
            int column = 1;
            for (Modifier productionBonus : bonusList) {
                GoodsType goodsType = FreeCol.getSpecification().getGoodsType(productionBonus.getId());
                String bonus = getModifierAsString(productionBonus);
                productionPanel.add(getGoodsButton(goodsType, bonus),
                                    higConst.rc(row, column));
                column += 2;
                if (column == 11) {
                    column = 1;
                    row += 2;
                }
            }
        }

        int[] widths = { 0, 3 * margin, 0 };
        int[] heights = new int[2 * rowsRequired - 1];
        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = margin;
        }
        int labelColumn = 1;
        int valueColumn = 3;

        HIGLayout layout = new HIGLayout(widths, heights);
        layout.setColumnWeight(valueColumn, 1);
        detailPanel.setLayout(layout);

        int row = 1;
        JLabel name = new JLabel(type.getName(), SwingConstants.CENTER);
        name.setFont(smallHeaderFont);

        detailPanel.add(name, higConst.rcwh(row, labelColumn, widths.length, 1));
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.unit.offensivePower")),
                        higConst.rc(row, labelColumn));
        detailPanel.add(new JLabel(String.valueOf(type.getOffence())),
                        higConst.rc(row, valueColumn, "r"));
        row += 2;
        detailPanel.add(new JLabel(Messages.message("colopedia.unit.defensivePower")),
                        higConst.rc(row, labelColumn));
        detailPanel.add(new JLabel(String.valueOf(type.getDefence())), 
                        higConst.rc(row, valueColumn, "r"));
        row += 2;
        detailPanel.add(new JLabel(Messages.message("colopedia.unit.movement")),
                        higConst.rc(row, labelColumn));
        detailPanel.add(new JLabel(String.valueOf(type.getMovement()/3)),
                        higConst.rc(row, valueColumn, "r"));
        row += 2;
        if (capacity != null) {
            detailPanel.add(new JLabel(Messages.message("colopedia.unit.capacity")),
                            higConst.rc(row, labelColumn));
            detailPanel.add(new JLabel(capacity),
                            higConst.rc(row, valueColumn, "r"));
            row += 2;
        } 
        if (skill != null) {
            detailPanel.add(new JLabel(Messages.message("colopedia.unit.skill")),
                            higConst.rc(row, labelColumn));
            detailPanel.add(new JLabel(skill), higConst.rc(row, valueColumn, "r"));
            row += 2;
            detailPanel.add(new JLabel(Messages.message("colopedia.unit.school")),
                            higConst.rc(row, labelColumn));
            detailPanel.add(schoolPanel, higConst.rc(row, valueColumn, "l"));
            row += 2;
        }
        if (productionPanel != null) {
            detailPanel.add(new JLabel(Messages.message("colopedia.unit.productionBonus")),
                            higConst.rc(row, labelColumn, "tl"));
            detailPanel.add(productionPanel, higConst.rc(row, valueColumn, "l"));
            row += 2;
        }
        if (price != null) {
            detailPanel.add(new JLabel(Messages.message("colopedia.unit.price")),
                            higConst.rc(row, labelColumn));
            detailPanel.add(new JLabel(price), higConst.rc(row, valueColumn, "r"));
            row += 2;
        }
        if (goodsRequired != null) {
            detailPanel.add(new JLabel(Messages.message("colopedia.unit.goodsRequired")),
                            higConst.rc(row, labelColumn));
            detailPanel.add(goodsRequired, higConst.rc(row, valueColumn, "l"));
            row += 2;
        }
        detailPanel.add(new JLabel(Messages.message("colopedia.unit.description")),
                        higConst.rc(row, labelColumn, "tl"));
        detailPanel.add(getDefaultTextArea(type.getDescription()),
                        higConst.rc(row, valueColumn));

        detailPanel.validate();
    }

    /**
     * Builds the details panel for the given goods.
     * 
     * @param type The GoodsType
     */
    private void buildGoodsDetail(GoodsType type) {
        detailPanel.removeAll();
        detailPanel.repaint();
        if (type == null) {
            return;
        }

        String isFarmed = Messages.message(type.isFarmed() ? "yes" : "no");
        int numberOfLines = type.isFarmed() ? 8 : 6;

        int[] widths = { 0, 3 * margin, 0 };
        int[] heights = new int[2 * numberOfLines - 1];
        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = margin;
        }

        int labelColumn = 1;
        int valueColumn = 3;
        HIGLayout layout = new HIGLayout(widths, heights);
        layout.setColumnWeight(valueColumn, 1);
        detailPanel.setLayout(layout);

        int row = 1;
        JLabel name = new JLabel(type.getName(), SwingConstants.CENTER);
        name.setFont(smallHeaderFont);
        detailPanel.add(name, higConst.rcwh(row, labelColumn, widths.length, 1));
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.goods.isFarmed")), higConst.rc(row, labelColumn));
        detailPanel.add(new JLabel(isFarmed), higConst.rc(row, valueColumn, "r"));
        row += 2;

        if (type.isFarmed()) {
            // Hardcoded for now - Come back and change later
            String improvedByPlowing = Messages.message(plowing.getBonus(type) > 0 ? "yes" : "no");
            String improvedByRiver = Messages.message(river.getBonus(type) > 0 ? "yes" : "no");
            String improvedByRoad = Messages.message(road.getBonus(type) > 0 ? "yes" : "no");

            detailPanel.add(new JLabel(Messages.message("colopedia.goods.improvedByPlowing")), higConst.rc(row,
                    labelColumn));
            detailPanel.add(new JLabel(improvedByPlowing), higConst.rc(row, valueColumn, "r"));
            row += 2;
            detailPanel.add(new JLabel(Messages.message("colopedia.goods.improvedByRiver")), higConst.rc(row,
                    labelColumn));
            detailPanel.add(new JLabel(improvedByRiver), higConst.rc(row, valueColumn, "r"));
            row += 2;
            detailPanel.add(new JLabel(Messages.message("colopedia.goods.improvedByRoad")), higConst.rc(row,
                    labelColumn));
            detailPanel.add(new JLabel(improvedByRoad), higConst.rc(row, valueColumn, "r"));
        } else {
            detailPanel.add(new JLabel(Messages.message("colopedia.goods.madeFrom")),
                            higConst.rc(row, labelColumn));
            if (type.isRefined()) {
                detailPanel.add(getGoodsButton(type.getRawMaterial(), 0),
                                higConst.rc(row, valueColumn, "l"));
            }
        }
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.goods.makes")), higConst.rc(row, labelColumn));
        if (type.isRawMaterial()) {
            detailPanel.add(getGoodsButton(type.getProducedMaterial(), 0), 
                            higConst.rc(row, valueColumn, "l"));
        }
        row += 2;
        detailPanel.add(new JLabel(Messages.message("colopedia.goods.description")), higConst
                .rc(row, labelColumn, "tl"));
        detailPanel.add(getDefaultTextArea(type.getDescription()), higConst.rc(row, valueColumn));

        detailPanel.validate();
    }

    /**
     * Builds the details panel for the given building.
     * 
     * @param buildingType The BuildingType
     */
    private void buildBuildingDetail(BuildingType buildingType) {
        detailPanel.removeAll();
        detailPanel.repaint();
        if (buildingType == null) {
            return;
        }

        int[] widths = { 0, 3 * margin, 0 };
        int[] heights = new int[15];
        for (int index = 0; index < 7; index++) {
            heights[2 * index + 1] = margin;
        }
        int row = 1;
        int leftColumn = 1;
        int rightColumn = 3;

        HIGLayout layout = new HIGLayout(widths, heights);
        layout.setColumnWeight(rightColumn, 1);
        detailPanel.setLayout(layout);

        /*
         * don't need this at the moment int[][] buildingUpkeep = { {0, -1, -1}, //
         * Town hall {0, 10, -1}, // Carpenter's house, Lumber mill {0, 5, 15}, //
         * Blacksmith's house, Blacksmith's shop, Iron works {0, 5, 15}, //
         * Tobacconist's house, Tobacconist's shop, Cigar factory {0, 5, 15}, //
         * Weaver's house, Weaver's shop, Textile mill {0, 5, 15}, //
         * Distiller's house, Rum distillery, Rum factory {0, 5, 15}, // Fur
         * trader's house, Fur trading post, Fur factory {5, 10, 15}, //
         * Schoolhouse, College, University {5, 10, 15}, // Armory, Magazine,
         * Arsenal {5, 15, -1}, // Church, Cathedral {0, 10, 15}, // Stockade,
         * Fort, Fortress {5, 5, -1}, // Warehouse, Warehouse expansion {5, -1,
         * -1}, // Stables {5, 10, 15}, // Docks, Drydock, Shipyard {5, 10, -1}, //
         * Printing press, Newspaper {15, -1, -1} // Custom house };
         */

        JLabel name = new JLabel(buildingType.getName(), SwingConstants.CENTER);
        name.setFont(smallHeaderFont);
        // name.setPreferredSize(new Dimension(detailPanel.getWidth(), 50));
        detailPanel.add(name, higConst.rcwh(row, leftColumn, widths.length, 1));
        row += 2;

        // Requires - prerequisites to build
        StringBuilder requiresText = new StringBuilder();
        if (buildingType.getUpgradesFrom() != null) {
            requiresText.append(buildingType.getUpgradesFrom().getName() + "\n");
        }
        if (buildingType.getPopulationRequired() > 0) {
            requiresText.append(String.valueOf(buildingType.getPopulationRequired()) + " " + 
                                Messages.message("colonists") + "\n");
        }
        for (Entry<String, Boolean> entry : buildingType.getAbilitiesRequired().entrySet()) {
            if (entry.getValue()) {
                requiresText.append(Messages.message(entry.getKey() + ".name"));
                father: for (FoundingFather father : FreeCol.getSpecification().getFoundingFathers()) {
                    for (Feature feature : father.getFeatures()) {
                        // this assumes that a particular ability is
                        // granted by only one FoundingFather
                        if (entry.getKey().equals(feature.getId())) {
                            requiresText.append(" ");
                            requiresText.append(Messages.message("colopedia.abilityGrantedBy", "%father%", 
                                                                 father.getName()));
                            break father;
                        }
                    }
                }
                requiresText.append("\n");
            }
        }

        JTextArea requires = getDefaultTextArea(requiresText.toString());
        detailPanel.add(new JLabel(Messages.message("colopedia.buildings.requires")), higConst
                .rc(row, leftColumn, "tl"));
        detailPanel.add(requires, higConst.rc(row, rightColumn));
        row += 2;

        // Costs to build - Hammers & Tools
        detailPanel.add(new JLabel(Messages.message("colopedia.buildings.cost")), higConst.rc(row, leftColumn));
        if (buildingType.getGoodsRequired() != null) {
            JPanel costs = new JPanel();
            costs.setOpaque(false);
            costs.setLayout(new FlowLayout(FlowLayout.LEFT));
            for (AbstractGoods goodsRequired : buildingType.getGoodsRequired()) {
                costs.add(getGoodsButton(goodsRequired.getType(), goodsRequired.getAmount()));
            }
            detailPanel.add(costs, higConst.rc(row, rightColumn));
        }
        row += 2;

        // Specialist
        detailPanel.add(new JLabel(Messages.message("colopedia.buildings.specialist")), higConst.rc(row, leftColumn));
        final UnitType unitType = FreeCol.getSpecification().getExpertForProducing(buildingType.getProducedGoodsType());
        if (unitType != null) {
            detailPanel.add(getUnitButton(unitType), higConst.rc(row, rightColumn, "l"));
        }
        row += 2;

        // Production - Needs & Produces
        JPanel production = new JPanel();
        production.setOpaque(false);
        production.setLayout(new FlowLayout(FlowLayout.LEFT));
        GoodsType inputType = buildingType.getConsumedGoodsType();
        if (inputType != null) {
            JLabel label = new JLabel(Messages.message("colopedia.buildings.needs"));
            label.setHorizontalTextPosition(SwingConstants.LEADING);
            production.add(label);
            production.add(getGoodsButton(inputType, 0));
        }
        GoodsType outputType = buildingType.getProducedGoodsType();
        if (outputType != null) {
            JLabel label = new JLabel(Messages.message("colopedia.buildings.produces"));
            label.setHorizontalTextPosition(SwingConstants.LEADING);
            production.add(label);
            production.add(getGoodsButton(outputType, 0));
        }
        detailPanel.add(new JLabel(Messages.message("colopedia.buildings.production")), higConst.rc(row, leftColumn));
        detailPanel.add(production, higConst.rc(row, rightColumn));
        row += 2;

        // Upkeep
        // detailPanel.add(new
        // JLabel(Messages.message("colopedia.buildings.upkeep")));
        // detailPanel.add(new
        // JLabel(Integer.toString(buildingUpkeep[building][level])));

        // Notes
        JTextArea notes = getDefaultTextArea(buildingType.getDescription());

        detailPanel.add(new JLabel(Messages.message("colopedia.buildings.notes")), higConst.rc(row, leftColumn, "tl"));
        detailPanel.add(notes, higConst.rc(row, rightColumn));

        detailPanel.validate();
    }

    /**
     * Builds the details panel for the given founding father.
     * 
     * @param father - the FoundingFather
     */
    private void buildFatherDetail(FoundingFather father) {
        detailPanel.removeAll();
        detailPanel.repaint();
        if (father == null) {
            return;
        }

        detailPanel.setLayout(new FlowLayout());

        JLabel name = new JLabel(father.getName(), SwingConstants.CENTER);
        name.setFont(smallHeaderFont);
        name.setPreferredSize(new Dimension(400, 50));
        detailPanel.add(name);

        Image image = (Image) UIManager.get("FoundingFather." + father.getType().toString().toLowerCase());

        JLabel imageLabel;
        if (image != null) {
            imageLabel = new JLabel(new ImageIcon(image));
        } else {
            imageLabel = new JLabel();
        }
        detailPanel.add(imageLabel);

        String text = Messages.message(father.getDescription()) + "\n\n" + "["
                + Messages.message(father.getBirthAndDeath()) + "] "
                + Messages.message(father.getText());
        JTextArea description = getDefaultTextArea(text);
        description.setColumns(32);
        description.setSize(description.getPreferredSize());
        detailPanel.add(description);

        detailPanel.validate();
    }
    
    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            parent.remove(this);
        }
    }
    
    /**
     * This function analyses a tree selection event and calls the right methods to take care
     * of building the requested unit's details.
     * 
     * @param event The incoming TreeSelectionEvent.
     */
    public void valueChanged(TreeSelectionEvent event) {
        if (event.getSource() == tree) {
            TreePath path = tree.getSelectionPath();
            if (path == null) {
                return;
            }
            
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode)path.getParentPath().getLastPathComponent();
            ColopediaTreeItem parentItem = (ColopediaTreeItem)parent.getUserObject();
            ColopediaTreeItem nodeItem = (ColopediaTreeItem)node.getUserObject();

            if (parentItem.getPanelType() != null) {
                switch(parentItem.getPanelType()) {
                case TERRAIN:
                    buildTerrainDetail((TileType) nodeItem.getFreeColGameObjectType());
                    break;
                case RESOURCES:
                    buildResourceDetail((ResourceType) nodeItem.getFreeColGameObjectType());
                    break;
                case UNITS:
                case SKILLS:
                    buildUnitDetail((UnitType) nodeItem.getFreeColGameObjectType());
                    break;
                case GOODS:
                    buildGoodsDetail((GoodsType) nodeItem.getFreeColGameObjectType());
                    break;
                case BUILDINGS:
                    buildBuildingDetail((BuildingType) nodeItem.getFreeColGameObjectType());
                    break;
                case FATHERS:
                    buildFatherDetail((FoundingFather) nodeItem.getFreeColGameObjectType());
                    break;
                }
            }
        }
    }

    /**
     * Returns a text area with standard settings suitable for use in FreeCol
     * dialogs.
     * 
     * @param text The text to display in the text area.
     * @return a text area with standard settings suitable for use in FreeCol
     *         dialogs.
     */
    public static JTextArea getDefaultTextArea(String text) {
        JTextArea textArea = new JTextArea(text);
        textArea.setOpaque(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFocusable(false);
        textArea.setFont(defaultFont);
        return textArea;
    }

    public String getModifierAsString(Modifier modifier) {
        String bonus = String.valueOf(modifier.getValue());
        switch(modifier.getType()) {
        case ADDITIVE:
            if (modifier.getValue() > 0) {
                bonus = "+" + bonus;
            }
            break;
        case PERCENTAGE:
            if (modifier.getValue() > 0) {
                bonus = "+" + bonus;
            }
            bonus = bonus + "%";
            break;
        case MULTIPLICATIVE:
            bonus = "\u00D7" + bonus;
            break;
        default:
        }
        return bonus;
    }

}
