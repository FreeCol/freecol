package net.sf.freecol.client.gui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Logger;

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
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Resource;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Colopedia.
 */
public final class ColopediaPanel extends FreeColPanel implements ActionListener, TreeSelectionListener {

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public static final int NO_DETAILS = 0;

    public static final int COLOPEDIA_TERRAIN = 0;
    public static final int COLOPEDIA_RESOURCE = 1;
    public static final int COLOPEDIA_UNIT = 2;
    public static final int COLOPEDIA_GOODS = 3;
    public static final int COLOPEDIA_SKILLS = 4;
    public static final int COLOPEDIA_BUILDING = 5;
    public static final int COLOPEDIA_FATHER = 6;

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ColopediaPanel.class.getName());

    private static final int OK = -1;

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
        this.library = (ImageLibrary) parent.getImageProvider();

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
    public void initialize(int type) {
        initialize(type, NO_DETAILS);
    }

    /**
     * Prepares this panel to be displayed.
     * 
     * @param type - the panel type
     * @param action - the details
     */
    public void initialize(int type, int action) {
        listPanel.removeAll();
        detailPanel.removeAll();
        tree = buildTree();
        switch (type) {
        case COLOPEDIA_TERRAIN:
            tree.expandRow(type);
            buildTerrainDetail(action);
            break;
        case COLOPEDIA_RESOURCE:
            if (action == NO_DETAILS) {
                action = 0;
            }
            tree.expandRow(type);
            buildResourceDetail(action);
            break;
        case COLOPEDIA_UNIT:
            tree.expandRow(type);
            buildUnitDetail(action);
            break;
        case COLOPEDIA_GOODS:
            tree.expandRow(type);
            buildGoodsDetail(action);
            break;
        case COLOPEDIA_SKILLS:
            tree.expandRow(type);
            buildUnitDetail(action);
            break;
        case COLOPEDIA_BUILDING:
            tree.expandRow(type);
            buildBuildingDetail(action);
            break;
        case COLOPEDIA_FATHER:
            tree.expandRow(type);
            buildFatherDetail(action);
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
        root = new DefaultMutableTreeNode(new ColopediaTreeItem(Messages.message("menuBar.colopedia"),null));
        
        DefaultMutableTreeNode terrain;
        terrain = new DefaultMutableTreeNode(new ColopediaTreeItem(Messages.message("menuBar.colopedia.terrain"),null));
        buildTerrainSubtree(terrain);
        root.add(terrain);
        
        DefaultMutableTreeNode resource;
        resource = new DefaultMutableTreeNode(new ColopediaTreeItem(Messages.message("menuBar.colopedia.resource"),null));
        buildResourceSubtree(resource);
        root.add(resource);
        
        DefaultMutableTreeNode units;
        units = new DefaultMutableTreeNode(new ColopediaTreeItem(Messages.message("menuBar.colopedia.unit"),null));
        buildUnitSubtree(units);
        root.add(units);
        
        DefaultMutableTreeNode goods;
        goods = new DefaultMutableTreeNode(new ColopediaTreeItem(Messages.message("menuBar.colopedia.goods"),null));
        buildGoodsSubtree(goods);
        root.add(goods);
        
        DefaultMutableTreeNode skills;
        skills = new DefaultMutableTreeNode(new ColopediaTreeItem(Messages.message("menuBar.colopedia.skill"),null));
        buildSkillsSubtree(skills);
        root.add(skills);
        
        DefaultMutableTreeNode buildings;
        buildings = new DefaultMutableTreeNode(new ColopediaTreeItem(Messages.message("menuBar.colopedia.building"),null));
        buildBuildingSubtree(buildings);
        root.add(buildings);
        
        DefaultMutableTreeNode fathers;
        fathers = new DefaultMutableTreeNode(new ColopediaTreeItem(Messages.message("menuBar.colopedia.father"),null));
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
            buildTerrainItem(t.getIndex(), parent);
        }
    }
    
    /**
     * Builds the buttons for all the resources.
     * @param parent
     */
    private void buildResourceSubtree(DefaultMutableTreeNode parent) {
        for (ResourceType r : FreeCol.getSpecification().getResourceTypeList()) {
            buildResourceItem(r.getIndex(), parent);
        }
    }
    
    /**
     * Builds the buttons for all the units.
     * @param parent
     */
    private void buildUnitSubtree(DefaultMutableTreeNode parent) {
        for (UnitType u : FreeCol.getSpecification().getUnitTypeList()) {
            if (u.getSkill() <= 0) {
                buildUnitItem(u.getIndex(), 0.5f, parent);
            }
        }
    }
    
    /**
     * Builds the buttons for all the goods.
     * @param parent
     */
    private void buildGoodsSubtree(DefaultMutableTreeNode parent) {
        for (GoodsType g : FreeCol.getSpecification().getGoodsTypeList()) {
            buildGoodsItem(g.getIndex(), parent);
        }
    }
    
    /**
     * Builds the buttons for all the skills.
     * @param parent
     */
    private void buildSkillsSubtree(DefaultMutableTreeNode parent) {
        for (UnitType u : FreeCol.getSpecification().getUnitTypeList()) {
            if (u.getSkill() > 0) {
                buildUnitItem(u.getIndex(), 0.5f, parent);
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
            DefaultMutableTreeNode item = new DefaultMutableTreeNode(new ColopediaTreeItem(buildingType.getName(), buildingIcon));
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
     * @param tileType the TileType
     * @param parent the parent node
     */
    private void buildTerrainItem(int tileIndex, DefaultMutableTreeNode parent) {
        TileType tileType = FreeCol.getSpecification().getTileType(tileIndex);
        ImageIcon icon = new ImageIcon(library.getScaledTerrainImage(tileType, 0.25f));
        DefaultMutableTreeNode item = new DefaultMutableTreeNode(new ColopediaTreeItem(tileType.getName(), icon));
        parent.add(item);
    }
    
    /**
     * Builds the button for the given resource.
     * 
     * @param type the ResourceType
     * @param parent the parent node
     */
    private void buildResourceItem(int resIndex, DefaultMutableTreeNode parent) {
        ResourceType resType = FreeCol.getSpecification().getResourceType(resIndex);
        ImageIcon icon = library.getBonusImageIcon(resType);
        DefaultMutableTreeNode item = new DefaultMutableTreeNode(new ColopediaTreeItem(resType.getName(), icon));
        parent.add(item);
    }
    
    /**
     * Builds the button for the given unit.
     * 
     * @param unit
     * @param scale
     * @param parent
     */
    private void buildUnitItem(int unit, float scale, DefaultMutableTreeNode parent) {
        UnitType unitType = FreeCol.getSpecification().getUnitType(unit);
        int unitIcon = ImageLibrary.getUnitGraphicsType(unit, false, false, 0, false);
        ImageIcon icon = library.getColopediaUnitImageIcon(unitIcon, 0.5f);
        DefaultMutableTreeNode item = new DefaultMutableTreeNode(new ColopediaTreeItem(unitType.getName(), icon));
        parent.add(item);
    }
    
    /**
     * Builds the button for the given goods.
     * 
     * @param goods
     * @param goodsIcon
     * @param parent
     */
    private void buildGoodsItem(int goodsIndex, DefaultMutableTreeNode parent) {
        GoodsType goodsType = FreeCol.getSpecification().getGoodsType(goodsIndex);
        ImageIcon icon = library.getScaledGoodsImageIcon(goodsType, 0.75f);
        DefaultMutableTreeNode item = new DefaultMutableTreeNode(new ColopediaTreeItem(goodsType.getName(), icon));
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
        DefaultMutableTreeNode item = new DefaultMutableTreeNode(new ColopediaTreeItem(name, icon));
        parent.add(item);
    }

    /**
     * Builds the details panel for the given tile.
     * 
     * @param tileType The TileType
     */
    private void buildTerrainDetail(int tileIndex) {
        TileType tileType = FreeCol.getSpecification().getTileType(tileIndex);
        detailPanel.removeAll();
        repaint();

        int[] widths = { 0, 3 * margin, 0 };
        int[] heights = new int[13];
        for (int index = 1; index < 12; index += 2) {
            heights[index] = margin;
        }
        int row = 1;
        int leftColumn = 1;
        int rightColumn = 3;

        HIGLayout layout = new HIGLayout(widths, heights);
        layout.setColumnWeight(rightColumn, 1);
        detailPanel.setLayout(layout);

        String movementCost = String.valueOf(tileType.getBasicMoveCost() / 3);
        String defenseBonus = "0%";
        Modifier defence = tileType.getDefenceBonus();
        if (defence != null) {
            defenseBonus = String.valueOf(defence.getValue()) + "%";
        }

        GoodsType secondaryGoodsType = Tile.secondaryGoods(tileType);

        JLabel nameLabel = new JLabel(tileType.getName(), SwingConstants.CENTER);
        nameLabel.setFont(smallHeaderFont);
        detailPanel.add(nameLabel, higConst.rcwh(row, leftColumn, widths.length, 1));
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.terrainImage")), higConst.rc(row, leftColumn));
        Image terrainImage = library.getScaledTerrainImage(tileType, 1f);
        detailPanel.add(new JLabel(new ImageIcon(terrainImage)), higConst.rc(row, rightColumn));
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.movementCost")), higConst.rc(row, leftColumn));
        detailPanel.add(new JLabel(movementCost), higConst.rc(row, rightColumn));
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.defenseBonus")), higConst.rc(row, leftColumn));
        detailPanel.add(new JLabel(defenseBonus), higConst.rc(row, rightColumn));
        row += 2;

        List<ResourceType> resourceList = tileType.getResourceTypeList();
        if (resourceList.size() > 0) {
            detailPanel.add(new JLabel(Messages.message("colopedia.terrain.resource")), higConst.rc(row, leftColumn));
            JPanel resourcePanel = new JPanel(new GridLayout(0, resourceList.size(), margin, 0));
            resourcePanel.setOpaque(false);
            for (ResourceType r : resourceList) {
                JLabel resourceLabel = new JLabel(library.getBonusImageIcon(r));
                resourceLabel.setText(r.getName());
                resourcePanel.add(resourceLabel);
            }
            detailPanel.add(resourcePanel, higConst.rc(row, rightColumn));
            row += 2;
        }

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.production")), higConst.rc(row, leftColumn));
        JPanel goodsPanel = new JPanel(new GridLayout(0, 8, margin, 0));
        goodsPanel.setOpaque(false);
        List<GoodsType> productionTypes = tileType.getPotentialTypeList();
        for(GoodsType goodsType : productionTypes) {
            JLabel goodsLabel = new JLabel(library.getGoodsImageIcon(goodsType));
            goodsLabel.setText(String.valueOf(tileType.getPotential(goodsType)));
            detailPanel.add(goodsLabel, higConst.rc(row, rightColumn));
        }
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.description")), higConst.rc(row, leftColumn,
                "tl"));
//        String key = "colopedia.terrain." + id + ".description";
        detailPanel.add(getDefaultTextArea(tileType.getDescription()), higConst.rc(row, rightColumn));

        detailPanel.validate();
    }

    /**
     * Builds the details panel for the given resource.
     * 
     * @param type The ResourceType
     */
    private void buildResourceDetail(int resIndex) {
        ResourceType type = FreeCol.getSpecification().getResourceType(resIndex);
        detailPanel.removeAll();
        detailPanel.repaint();

        int[] widths = { 0, 3 * margin, 0 };
        int[] heights = new int[7];
        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = margin;
        }

        int labelColumn = 1;
        int valueColumn = 3;
        HIGLayout layout = new HIGLayout(widths, heights);
        layout.setColumnWeight(valueColumn, 1);
        detailPanel.setLayout(layout);

        int row = 1;
        int leftColumn = 1;
        int rightColumn = 3;

        JLabel name = new JLabel(type.getName(), SwingConstants.CENTER);
        name.setFont(smallHeaderFont);
        detailPanel.add(name, higConst.rcwh(row, labelColumn, widths.length, 1));
        row += 2;

        List<GoodsType> goodsList = type.getBonusTypeList();
        List<Integer> amountList = type.getBonusAmountList();

        detailPanel.add(new JLabel(Messages.message("colopedia.resource.bonusProduction")), higConst.rc(row, leftColumn));
        JPanel goodsPanel = new JPanel(new GridLayout(0, goodsList.size(), margin, 0));
        goodsPanel.setOpaque(false);
        for (int i = 0; i < goodsList.size(); i++) {
            JLabel goodsLabel = new JLabel(library.getGoodsImageIcon(goodsList.get(i)));
            goodsLabel.setText(String.valueOf(amountList.get(i)));
            goodsPanel.add(goodsLabel);
        }
        detailPanel.add(goodsPanel, higConst.rc(row, rightColumn));
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.resource.description")), higConst.rc(row, leftColumn,
                "tl"));
        detailPanel.add(getDefaultTextArea(type.getDescription()), higConst.rc(row, rightColumn));

        detailPanel.validate();
    }

    /**
     * Builds the details panel for the given unit.
     * 
     * @param unit
     */
    private void buildUnitDetail(int unit) {
        detailPanel.removeAll();
        detailPanel.repaint();

        Europe europe = parent.getClient().getMyPlayer().getEurope();
        UnitType type = FreeCol.getSpecification().getUnitType(unit);
        String price = "";
        if (europe != null) {
            price = String.valueOf(europe.getUnitPrice(type));
        } else if (type.getPrice() > 0) {
            price = String.valueOf(type.getPrice());
        }
        String hammersRequired = "";
        if (type.getHammersRequired() > 0) {
            hammersRequired = String.valueOf(type.getHammersRequired());
        }
        String toolsRequired = "";
        if (type.getToolsRequired() > 0) {
            toolsRequired = String.valueOf(type.getToolsRequired());
        }
        String skill = "";
        String schoolType = null;
        if (type.hasSkill()) {
            skill = String.valueOf(type.getSkill());
            
            for (BuildingType buildingType : FreeCol.getSpecification().getBuildingTypeList()) {
                if (buildingType.hasAbility("model.ability.teach") && buildingType.canAdd(type)) {
                    if (schoolType == null) {
                        schoolType = buildingType.getName();
                    } else {
                        schoolType += ", " + buildingType.getName();
                    }
                }
            }
        } else {
            schoolType = "";
        }
        int[] widths = { 0, 3 * margin, 0 };
        int[] heights = new int[21];
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
        if (type.hasAbility("carry-goods") || type.hasAbility("naval")) {
            detailPanel.add(new JLabel(Messages.message("colopedia.unit.capacity")),
                            higConst.rc(row, labelColumn));
            detailPanel.add(new JLabel(String.valueOf(Unit.getInitialSpaceLeft(type))),
                            higConst.rc(row, valueColumn, "r"));
        } else {
            detailPanel.add(new JLabel(Messages.message("colopedia.unit.skill")),
                            higConst.rc(row, labelColumn));
            detailPanel.add(new JLabel(skill), higConst.rc(row, valueColumn, "r"));
            row += 2;
            detailPanel.add(new JLabel(Messages.message("colopedia.unit.school")),
                            higConst.rc(row, labelColumn));
            detailPanel.add(new JLabel(schoolType), higConst.rc(row, valueColumn, "r"));
        }
        row += 2;
        detailPanel.add(new JLabel(Messages.message("colopedia.unit.price")),
                        higConst.rc(row, labelColumn));
        detailPanel.add(new JLabel(price), higConst.rc(row, valueColumn, "r"));
        row += 2;
        detailPanel.add(new JLabel(Messages.message("colopedia.unit.hammersRequired")),
                        higConst.rc(row, labelColumn));
        detailPanel.add(new JLabel(hammersRequired),
                        higConst.rc(row, valueColumn, "r"));
        row += 2;
        detailPanel.add(new JLabel(Messages.message("colopedia.unit.toolsRequired")),
                        higConst.rc(row, labelColumn));
        detailPanel.add(new JLabel(toolsRequired),
                        higConst.rc(row, valueColumn, "r"));
        row += 2;
        detailPanel.add(new JLabel(Messages.message("colopedia.unit.description")),
                        higConst.rc(row, labelColumn, "tl"));
        detailPanel.add(getDefaultTextArea(type.getDescription()),
                        higConst.rc(row, valueColumn));

        detailPanel.validate();
    }

    /**
     * Builds the details panel for the given goods.
     * 
     * @param goods
     */
    private void buildGoodsDetail(int goodsIndex) {
        detailPanel.removeAll();
        detailPanel.repaint();

        GoodsType type = FreeCol.getSpecification().getGoodsType(goodsIndex);

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

        String madeFrom = (type.isRefined()) ? type.getRawMaterial().getName() : "";
        String makes = (type.isRawMaterial()) ? type.getProducedMaterial().getName() : "";

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
            String improvedByPlowing = Messages.message(type.getIndex()<=(Goods.COTTON).getIndex() ? "yes" : "no");
            String improvedByRiver = Messages.message(type.getIndex()<=(Goods.FURS).getIndex() ? "yes" : "no");
            String improvedByRoad = Messages.message((Goods.FURS).getIndex()<=type.getIndex()
                                                    && type.getIndex()<=(Goods.SILVER).getIndex() ? "yes" : "no");

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
            row += 2;
        } else {
            detailPanel.add(new JLabel(Messages.message("colopedia.goods.madeFrom")), higConst.rc(row, labelColumn));
            detailPanel.add(new JLabel(madeFrom), higConst.rc(row, valueColumn, "r"));
            row += 2;
        }

        detailPanel.add(new JLabel(Messages.message("colopedia.goods.makes")), higConst.rc(row, labelColumn));
        detailPanel.add(new JLabel(makes), higConst.rc(row, valueColumn, "r"));
        row += 2;
        detailPanel.add(new JLabel(Messages.message("colopedia.goods.description")), higConst
                .rc(row, labelColumn, "tl"));
        detailPanel.add(getDefaultTextArea(type.getDescription()), higConst.rc(row, valueColumn));

        detailPanel.validate();
    }

    /**
     * Builds the details panel for the given building.
     * 
     * @param action
     */
    private void buildBuildingDetail(int building) {
        detailPanel.removeAll();
        detailPanel.repaint();

        BuildingType buildingType = FreeCol.getSpecification().getBuildingType(building);
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
        String requiresText = "";
        if (buildingType.getPopulationRequired() > 0) {
            requiresText += String.valueOf(buildingType.getPopulationRequired()) + " " + Messages.message("colonists");
        }
        if (buildingType.getUpgradesFrom() != null) {
            requiresText += "\n" + buildingType.getUpgradesFrom().getName();
        }
        /* TODO: add other requirements
        if () {
            requiresText += "\n" + ;
        }
         */

        JTextArea requires = getDefaultTextArea(requiresText);
        detailPanel.add(new JLabel(Messages.message("colopedia.buildings.requires")), higConst
                .rc(row, leftColumn, "tl"));
        detailPanel.add(requires, higConst.rc(row, rightColumn));
        row += 2;

        // Costs to build - Hammers & Tools
        JPanel costs = new JPanel();
        costs.setOpaque(false);
        costs.setLayout(new FlowLayout(FlowLayout.LEFT));
        costs.add(new JLabel(Integer.toString(buildingType.getHammersRequired()),
                library.getGoodsImageIcon(Goods.HAMMERS), SwingConstants.LEFT));
        costs.add(new JLabel(Integer.toString(buildingType.getToolsRequired()),
                library.getGoodsImageIcon(Goods.TOOLS), SwingConstants.LEFT));
        detailPanel.add(new JLabel(Messages.message("colopedia.buildings.cost")), higConst.rc(row, leftColumn));
        detailPanel.add(costs, higConst.rc(row, rightColumn));
        row += 2;

        // Specialist
        JLabel specialist = new JLabel();
        UnitType unitType = FreeCol.getSpecification().getExpertForProducing(buildingType.getProducedGoodsType());
        if (unitType != null) {
            int graphicsType = ImageLibrary.getUnitGraphicsType(unitType.getIndex(), false, false, 0, false);
            specialist.setIcon(library.getUnitImageIcon(graphicsType));
            specialist.setText(unitType.getName());
            specialist.setHorizontalAlignment(SwingConstants.LEFT);
        }
        detailPanel.add(new JLabel(Messages.message("colopedia.buildings.specialist")), higConst.rc(row, leftColumn));
        detailPanel.add(specialist, higConst.rc(row, rightColumn));
        row += 2;

        // Production - Needs & Produces
        JPanel production = new JPanel();
        production.setOpaque(false);
        production.setLayout(new FlowLayout(FlowLayout.LEFT));
        GoodsType inputType = buildingType.getConsumedGoodsType();
        if (inputType != null) {
            JLabel label = new JLabel(Messages.message("colopedia.buildings.needs"), library
                    .getGoodsImageIcon(inputType), SwingConstants.LEADING);
            label.setHorizontalTextPosition(SwingConstants.LEADING);
            production.add(label);
        }
        GoodsType outputType = buildingType.getProducedGoodsType();
        if (outputType != null) {
            JLabel label = new JLabel(Messages.message("colopedia.buildings.produces"), library
                    .getGoodsImageIcon(outputType), SwingConstants.LEADING);
            label.setHorizontalTextPosition(SwingConstants.LEADING);
            production.add(label);
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
     * @param foundingFather
     */
    private void buildFatherDetail(int foundingFather) {
        detailPanel.removeAll();
        detailPanel.repaint();
        detailPanel.setLayout(new FlowLayout());

        FoundingFather father = FreeCol.getSpecification().getFoundingFather(foundingFather);

        JLabel name = new JLabel(father.getName(), SwingConstants.CENTER);
        name.setFont(smallHeaderFont);
        name.setPreferredSize(new Dimension(400, 50));
        detailPanel.add(name);

        Image image = null;
        switch (father.getType()) {
        case FoundingFather.TRADE:
            image = (Image) UIManager.get("FoundingFather.trade");
            break;
        case FoundingFather.EXPLORATION:
            image = (Image) UIManager.get("FoundingFather.exploration");
            break;
        case FoundingFather.MILITARY:
            image = (Image) UIManager.get("FoundingFather.military");
            break;
        case FoundingFather.POLITICAL:
            image = (Image) UIManager.get("FoundingFather.political");
            break;
        case FoundingFather.RELIGIOUS:
            image = (Image) UIManager.get("FoundingFather.religious");
            break;
        }

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
        int action = Integer.valueOf(command).intValue();
        if (action == OK) {
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
        if(event.getSource() == tree) {
            TreePath path = tree.getSelectionPath();
            if (path == null) {
                return;
            }
            
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode)path.getParentPath().getLastPathComponent();
            ColopediaTreeItem parentItem = (ColopediaTreeItem)parent.getUserObject();
            ColopediaTreeItem nodeItem = (ColopediaTreeItem)node.getUserObject();
            String parentTitle = parentItem.toString();
            String nodeTitle = nodeItem.toString();
            
            if (parentTitle.equals(Messages.message("menuBar.colopedia.terrain"))) {
                // Tile
                List<TileType> tileTypes = FreeCol.getSpecification().getTileTypeList();
                for (TileType tileType : tileTypes) {
                    if (nodeTitle.equals(tileType.getName())) {
                        buildTerrainDetail(tileType.getIndex());
                    }
                }
            } else if (parentTitle.equals(Messages.message("menuBar.colopedia.resource"))) {
                // Resources
                List<ResourceType> resList = FreeCol.getSpecification().getResourceTypeList();
                for (ResourceType resType : resList) {
                    if (nodeTitle.equals(resType.getName())) {
                        buildResourceDetail(resType.getIndex());
                    }
                }
            } else if (parentTitle.equals(Messages.message("menuBar.colopedia.unit")) ||
                    parentTitle.equals(Messages.message("menuBar.colopedia.skill"))) {
                // Units
                List<UnitType> unitTypes = FreeCol.getSpecification().getUnitTypeList();
                for (UnitType unitType : unitTypes) {
                    if (nodeTitle.equals(Messages.message(unitType.getName()))) {
                        buildUnitDetail(unitType.getIndex());
                    }
                }
            } else if (parentTitle.equals(Messages.message("menuBar.colopedia.goods"))) {
                //Goods
                List<GoodsType> goodsTypes = FreeCol.getSpecification().getGoodsTypeList();
                for (GoodsType goodsType : goodsTypes) {
                    if (nodeTitle.equals(goodsType.getName())) {
                        buildGoodsDetail(goodsType.getIndex());
                    }
                }
            } else if (parentTitle.equals(Messages.message("menuBar.colopedia.building"))) {
                // Buildings
                List<BuildingType> buildingTypes = FreeCol.getSpecification().getBuildingTypeList();
                for (BuildingType buildingType : buildingTypes) {
                    if (nodeTitle.equals(buildingType.getName())) {
                        buildBuildingDetail(buildingType.getIndex());
                    }
                }
            } else if (parentTitle.equals(Messages.message("menuBar.colopedia.father"))) {
                // Founding Fathers
                List<FoundingFather> foundingFathers = FreeCol.getSpecification().getFoundingFathers();
                for (FoundingFather foundingFather : foundingFathers) {
                    if (nodeTitle.equals(Messages.message(foundingFather.getName()))) {
                        buildFatherDetail(foundingFather.getIndex());
                    }
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

}
