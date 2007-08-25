package net.sf.freecol.client.gui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
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

    public static final int NO_DETAILS = -1;

    public static final int COLOPEDIA_TERRAIN = 0;

    public static final int COLOPEDIA_UNIT = 1;

    public static final int COLOPEDIA_GOODS = 2;

    public static final int COLOPEDIA_SKILLS = 3;

    public static final int COLOPEDIA_BUILDING = 4;

    public static final int COLOPEDIA_FATHER = 5;

    private static final String[][] buildingCalls = { { "TownHall", null, null },
            { "CarpenterHouse", "LumberMill", null }, { "BlacksmithHouse", "BlacksmithShop", "IronWorks" },
            { "TobacconistHouse", "TobacconistShop", "CigarFactory" }, { "WeaverHouse", "WeaverShop", "TextileMill" },
            { "DistillerHouse", "RumDistillery", "RumFactory" }, { "FurTraderHouse", "FurTradingPost", "FurFactory" },
            { "Schoolhouse", "College", "University" }, { "Armory", "Magazine", "Arsenal" },
            { "Church", "Cathedral", null }, { "Stockade", "Fort", "Fortress" },
            { "Warehouse", "WarehouseExpansion", null }, { "Stables", null, null }, { "Docks", "Drydock", "Shipyard" },
            { "PrintingPress", "Newspaper", null }, { "CustomHouse", null, null } };

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
        initialize(type, null);
    }

    /**
     * Prepares this panel to be displayed.
     * 
     * @param type - the panel type
     * @param action - the details
     */
    public void initialize(int type, Object action) {
        listPanel.removeAll();
        detailPanel.removeAll();
        tree = buildTree();
        switch (type) {
        case COLOPEDIA_TERRAIN:
            if (action == null) {
                action = FreeCol.getSpecification().getTileType(0);
            }
            tree.expandRow(type);
            buildTerrainDetail(action);
            break;
        case COLOPEDIA_UNIT:
            if (action == null) {
                // TODO: get first non-expert unit
                action = FreeCol.getSpecification().unitType(0);
            }
            tree.expandRow(type);
            buildUnitDetail(action);
            break;
        case COLOPEDIA_GOODS:
            if (action == null) {
                action = FreeCol.getSpecification().getGoodsType(0);
            }
            tree.expandRow(type);
            buildGoodsDetail(action);
            break;
        case COLOPEDIA_SKILLS:
            if (action == null) {
                // TODO: get first expert unit
                action = FreeCol.getSpecification().unitType(0);
            }
            tree.expandRow(type);
            buildUnitDetail(action);
            break;
        case COLOPEDIA_BUILDING:
            if (action == null) {
                action = FreeCol.getSpecification().getBuildingTypeList().get(0);
            }
            tree.expandRow(type);
            buildBuildingDetail(action);
            break;
        case COLOPEDIA_FATHER:
            if (action == null) {
                action = FreeCol.getSpecification().getFoundingFathers().get(0);
            }
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
     * Builds the buttons for all the terrains.
     * @param parent
     */
    private void buildTerrainSubtree(DefaultMutableTreeNode parent) {
        // TODO: use specification for terrain list
        // If so we need to know if a certain tile should yield a single
        // call with false or two calls (false/true).
        // int numberOfTypes = FreeCol.specification.numberOfTileTypes();

        // type zero is unexplored
        int numberOfTypes = FreeCol.getSpecification().numberOfTileTypes();
        for (int type = 1; type < numberOfTypes; type++) {
            if (type < Tile.ARCTIC) {
                buildTerrainItem(type, false, parent);
                buildTerrainItem(type, true, parent);
            }
            else buildTerrainItem(type, false, parent);
        }
        buildTerrainItem(ImageLibrary.HILLS, false, parent);
        buildTerrainItem(ImageLibrary.MOUNTAINS, false, parent);
    }
    
    /**
     * Builds the buttons for all the units.
     * @param parent
     */
    private void buildUnitSubtree(DefaultMutableTreeNode parent) {
        int numberOfTypes = FreeCol.getSpecification().numberOfUnitTypes();
        for (int type = 0; type < numberOfTypes-4; type++) { // Ugly, but needed to remove testing units like "Milkmaid"
            UnitType unitType = FreeCol.getSpecification().unitType(type);
            if (unitType.getSkill() <= 0) {
                buildUnitItem(type, 0.5f, parent);
            }
        }
    }
    
    /**
     * Builds the buttons for all the goods.
     * @param parent
     */
    private void buildGoodsSubtree(DefaultMutableTreeNode parent) {
        int numberOfTypes = FreeCol.getSpecification().numberOfGoodsTypes();
        for (int i = 0; i < numberOfTypes; i++) {
            buildGoodsItem(i, parent);
        }
    }
    
    /**
     * Builds the buttons for all the skills.
     * @param parent
     */
    private void buildSkillsSubtree(DefaultMutableTreeNode parent) {
        int numberOfTypes = FreeCol.getSpecification().numberOfUnitTypes();
        for (int type = 0; type < numberOfTypes; type++) {
            UnitType unitType = FreeCol.getSpecification().unitType(type);
            if (unitType.getSkill() > 0) {
                buildUnitItem(type, 0.5f, parent);
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

        int numberOfTypes = FreeCol.getSpecification().numberOfBuildingTypes();
        for (int type = 0; type < numberOfTypes; type++) {
            BuildingType buildingType = FreeCol.getSpecification().buildingType(type);
            for (int level = 0; level < buildingType.numberOfLevels(); level++) {
                BuildingType.Level buildingLevel = buildingType.level(level);
                DefaultMutableTreeNode item = new DefaultMutableTreeNode(new ColopediaTreeItem(Messages.message(buildingLevel.name), buildingIcon));
                parent.add(item);
            }
        }
    }
    
    /**
     * Builds the buttons for all the founding fathers.
     * @param parent
     */
    private void buildFathersSubtree(DefaultMutableTreeNode parent) {
        for (int i = 0; i < FoundingFather.FATHER_COUNT; i++) {
            buildFatherItem(i, parent);
        }
    }
    
    /**
     * Builds the button for the given terrain.
     * 
     * @param terrain the type of terrain
     * @param forested whether it is forested
     * @param parent the parent node
     */
    private void buildTerrainItem(int terrain, boolean forested, DefaultMutableTreeNode parent) {
        String name;
        ImageIcon icon;
        DefaultMutableTreeNode item;
        
        //Get name of terrain type
        switch (terrain) {
        case ImageLibrary.HILLS:
            name = Messages.message("hills");
            break;
        case ImageLibrary.MOUNTAINS:
            name = Messages.message("mountains");
            break;
        default:
            TileType tileType = (forested ? FreeCol.getSpecification().getTileType(terrain).whenForested :
                                 FreeCol.getSpecification().getTileType(terrain));
            name = Messages.message(tileType.name);
        }
        icon = new ImageIcon(library.getScaledTerrainImage(terrain, forested, 0.25f));
        item = new DefaultMutableTreeNode(new ColopediaTreeItem(name, icon));
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
        String name = Unit.getName(unit);
        int unitIcon = ImageLibrary.getUnitGraphicsType(unit, false, false, 0, false);
        ImageIcon icon = library.getColopediaUnitImageIcon(unitIcon, 0.5f);
        DefaultMutableTreeNode item;
        item = new DefaultMutableTreeNode(new ColopediaTreeItem(name, icon));
        parent.add(item);
    }
    
    /**
     * Builds the button for the given goods.
     * 
     * @param goods
     * @param goodsIcon
     * @param parent
     */
    private void buildGoodsItem(int goods, DefaultMutableTreeNode parent) {
        String name = Goods.getName(goods);
        ImageIcon icon = library.getScaledGoodsImageIcon(goods, 0.75f);
        DefaultMutableTreeNode item = new DefaultMutableTreeNode(new ColopediaTreeItem(name, icon));
        parent.add(item);
    }
    
    /**
     * Builds the button for the given founding father.
     * 
     * @param foundingFather
     * @param parent
     */
    private void buildFatherItem(int foundingFather, DefaultMutableTreeNode parent) {
        String name = Messages.message(FoundingFather.getName(foundingFather));
        ImageIcon icon = library.getScaledGoodsImageIcon(Goods.BELLS, 0.75f);
        DefaultMutableTreeNode item = new DefaultMutableTreeNode(new ColopediaTreeItem(name, icon));
        parent.add(item);
    }

    /**
     * Builds the details panel for the given terrain.
     * 
     * @param terrain
     */
    private void buildTerrainDetail(int terrain) {
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

        int type = terrain / 2;
        int productionIndex = type;
        boolean forested = (terrain % 2 == 1);
        int addition = Tile.ADD_NONE;
        String id = null;
        String name = null;
        String defenseBonus = null;
        int movementCost;

        switch (type) {
        case ImageLibrary.HILLS:
            id = "hills";
            name = Messages.message("hills");
            addition = Tile.ADD_HILLS;
            productionIndex = 12;
            defenseBonus = "150%";
            movementCost = 6;
            break;
        case ImageLibrary.MOUNTAINS:
            id = "mountains";
            name = Messages.message("mountains");
            addition = Tile.ADD_MOUNTAINS;
            productionIndex = 13;
            defenseBonus = "200%";
            movementCost = 9;
            break;
        default:
            TileType tileType = (forested ? FreeCol.getSpecification().getTileType(type).whenForested :
                                 FreeCol.getSpecification().getTileType(type));
            id = tileType.id;
            name = Messages.message(tileType.name);
            defenseBonus = String.valueOf(tileType.getDefenceFactor()) + "%";
            movementCost = tileType.basicMoveCost;
        }
        movementCost /= 3;

        int secondaryGoodsType = Tile.secondaryGoods(type, forested, addition);

        JLabel nameLabel = new JLabel(name, SwingConstants.CENTER);
        nameLabel.setFont(smallHeaderFont);
        detailPanel.add(nameLabel, higConst.rcwh(row, leftColumn, widths.length, 1));
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.terrainImage")), higConst.rc(row, leftColumn));
        Image terrainImage = library.getScaledTerrainImage(type, forested, 1f);
        detailPanel.add(new JLabel(new ImageIcon(terrainImage)), higConst.rc(row, rightColumn));
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.movementCost")), higConst.rc(row, leftColumn));
        detailPanel.add(new JLabel(String.valueOf(movementCost)), higConst.rc(row, rightColumn));
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.defenseBonus")), higConst.rc(row, leftColumn));
        detailPanel.add(new JLabel(defenseBonus), higConst.rc(row, rightColumn));
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.resource")), higConst.rc(row, leftColumn));
        int bonusType = ImageLibrary.getBonusImageType(type, addition, forested);
        if (bonusType >= 0) {
            ImageIcon bonusIcon = library.getBonusImageIcon(bonusType);
            detailPanel.add(new JLabel(bonusIcon), higConst.rc(row, rightColumn));
        }
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.production")), higConst.rc(row, leftColumn));
        JPanel goodsPanel = new JPanel(new GridLayout(0, 8, margin, 0));
        goodsPanel.setOpaque(false);
        if (type == Tile.OCEAN || type == Tile.HIGH_SEAS) {
            JLabel goodsLabel = new JLabel(library.getGoodsImageIcon(Goods.FISH));
            goodsLabel.setText(String.valueOf(Tile.potentialtable[type][Goods.FOOD][0]));
            detailPanel.add(goodsLabel, higConst.rc(row, rightColumn));
        } else {
            for (int index = 0; index < Goods.NUMBER_OF_TYPES; index++) {
                if (Goods.isFarmedGoods(index)) {
                    JLabel goodsLabel = new JLabel(library.getGoodsImageIcon(index));
                    if (index == secondaryGoodsType) {
                        goodsLabel.setForeground(Color.GREEN);
                    }
                    goodsLabel.setText(String.valueOf(Tile.potentialtable[productionIndex][index][(forested ? 1 : 0)]));
                    goodsPanel.add(goodsLabel);
                }
            }
            detailPanel.add(goodsPanel, higConst.rc(row, rightColumn));
        }
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.description")), higConst.rc(row, leftColumn,
                "tl"));
        String key = "colopedia.terrain." + id + ".description";
        detailPanel.add(getDefaultTextArea(Messages.message(key)), higConst.rc(row, rightColumn));

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
        UnitType type = FreeCol.getSpecification().unitType(unit);
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
        String schoolType = "";
        if (type.getSkill() != UnitType.UNDEFINED) {
            skill = String.valueOf(type.getSkill());
            int schoolLevel = 1;
            if (type.getSkill() > 1) {
                schoolLevel = type.getSkill();
            }
            schoolType = Building.getName(Building.SCHOOLHOUSE, schoolLevel);
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
        JLabel name = new JLabel(Unit.getName(unit), SwingConstants.CENTER);
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
            detailPanel.add(new JLabel(String.valueOf(Unit.getInitialSpaceLeft(unit))),
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
        detailPanel.add(getDefaultTextArea(Messages.message(type.getId() + ".description")),
                        higConst.rc(row, valueColumn));

        detailPanel.validate();
    }

    /**
     * Builds the details panel for the given goods.
     * 
     * @param goods
     */
    private void buildGoodsDetail(int goods) {
        detailPanel.removeAll();
        detailPanel.repaint();

        GoodsType type = FreeCol.getSpecification().goodsType(goods);

        String isFarmed = Messages.message(type.isFarmed ? "yes" : "no");
        int numberOfLines = type.isFarmed ? 8 : 6;

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

        String madeFrom = "";
        if (type.madeFrom != null) {
            madeFrom = Messages.message(type.madeFrom.name);
        }
        String makes = "";
        if (type.makes != null) {
            makes = Messages.message(type.makes.name);
        }

        int row = 1;
        JLabel name = new JLabel(Goods.getName(goods), SwingConstants.CENTER);
        name.setFont(smallHeaderFont);
        detailPanel.add(name, higConst.rcwh(row, labelColumn, widths.length, 1));
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.goods.isFarmed")), higConst.rc(row, labelColumn));
        detailPanel.add(new JLabel(isFarmed), higConst.rc(row, valueColumn, "r"));
        row += 2;

        if (type.isFarmed) {

            String improvedByPlowing = Messages.message(type.improvedByPlowing ? "yes" : "no");
            String improvedByRiver = Messages.message(type.improvedByRiver ? "yes" : "no");
            String improvedByRoad = Messages.message(type.improvedByRoad ? "yes" : "no");

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
        detailPanel.add(getDefaultTextArea(Messages.message(type.id + ".description")), higConst.rc(row, valueColumn));

        detailPanel.validate();
    }

    /**
     * Builds the details panel for the given building.
     * 
     * @param action
     */
    private void buildBuildingDetail(int action) {
        detailPanel.removeAll();
        detailPanel.repaint();

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

        int building = action >> 2;
        int level = (action & 0x03);

        BuildingType buildingType = FreeCol.getSpecification().buildingType(building);
        BuildingType.Level buildingLevel = buildingType.level(level);

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

        JLabel name = new JLabel(Messages.message(buildingLevel.name), SwingConstants.CENTER);
        name.setFont(smallHeaderFont);
        // name.setPreferredSize(new Dimension(detailPanel.getWidth(), 50));
        detailPanel.add(name, higConst.rcwh(row, leftColumn, widths.length, 1));
        row += 2;

        // Requires - prerequisites to build
        String requiresText = "";
        if (buildingLevel.populationRequired > 0) {
            requiresText += String.valueOf(buildingLevel.populationRequired) + " " + Messages.message("colonists");
        }
        if (level > 0) {
            requiresText += "\n" + Messages.message(buildingType.level(level - 1).name);
        }
        if (level > 1
                && (building == Building.BLACKSMITH || building == Building.TOBACCONIST || building == Building.WEAVER
                        || building == Building.DISTILLER || building == Building.FUR_TRADER || building == Building.ARMORY)) {
            requiresText += "\n" + Messages.message(FoundingFather.getName(FoundingFather.ADAM_SMITH));
        }
        if (building == Building.CUSTOM_HOUSE) {
            requiresText += "\n" + Messages.message(FoundingFather.getName(FoundingFather.PETER_STUYVESANT));
        }

        JTextArea requires = getDefaultTextArea(requiresText);
        detailPanel.add(new JLabel(Messages.message("colopedia.buildings.requires")), higConst
                .rc(row, leftColumn, "tl"));
        detailPanel.add(requires, higConst.rc(row, rightColumn));
        row += 2;

        // Costs to build - Hammers & Tools
        JPanel costs = new JPanel();
        costs.setOpaque(false);
        costs.setLayout(new FlowLayout(FlowLayout.LEFT));
        costs.add(new JLabel(Integer.toString(buildingLevel != null ? buildingLevel.hammersRequired : -1), library
                .getGoodsImageIcon(ImageLibrary.GOODS_HAMMERS), SwingConstants.LEFT));
        costs.add(new JLabel(Integer.toString(buildingLevel != null ? buildingLevel.toolsRequired : -1), library
                .getGoodsImageIcon(ImageLibrary.GOODS_TOOLS), SwingConstants.LEFT));
        detailPanel.add(new JLabel(Messages.message("colopedia.buildings.cost")), higConst.rc(row, leftColumn));
        detailPanel.add(costs, higConst.rc(row, rightColumn));
        row += 2;

        // Specialist
        JLabel specialist = new JLabel();
        int unitType = Building.getExpertUnitType(building);
        if (unitType >= 0) {
            int graphicsType = ImageLibrary.getUnitGraphicsType(unitType, false, false, 0, false);
            specialist.setIcon(library.getUnitImageIcon(graphicsType));
            specialist.setText(Unit.getName(unitType));
            specialist.setHorizontalAlignment(SwingConstants.LEFT);
        }
        detailPanel.add(new JLabel(Messages.message("colopedia.buildings.specialist")), higConst.rc(row, leftColumn));
        detailPanel.add(specialist, higConst.rc(row, rightColumn));
        row += 2;

        // Production - Needs & Produces
        JPanel production = new JPanel();
        production.setOpaque(false);
        production.setLayout(new FlowLayout(FlowLayout.LEFT));
        int inputType = Building.getGoodsInputType(building);
        if (inputType >= 0) {
            JLabel label = new JLabel(Messages.message("colopedia.buildings.needs"), library
                    .getGoodsImageIcon(inputType), SwingConstants.LEADING);
            label.setHorizontalTextPosition(SwingConstants.LEADING);
            production.add(label);
        }
        int outputType = Building.getGoodsOutputType(building);
        if (outputType >= 0) {
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
        JTextArea notes = getDefaultTextArea(Messages.message("colopedia.buildings.notes."
                + buildingCalls[building][level]));

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

        JLabel name = new JLabel(Messages.message(FoundingFather.getName(foundingFather)), SwingConstants.CENTER);
        name.setFont(smallHeaderFont);
        name.setPreferredSize(new Dimension(400, 50));
        detailPanel.add(name);

        Image image = null;
        switch (FoundingFather.getType(foundingFather)) {
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

        String text = Messages.message(FoundingFather.getDescription(foundingFather)) + "\n\n" + "["
                + Messages.message(FoundingFather.getBirthAndDeath(foundingFather)) + "] "
                + Messages.message(FoundingFather.getText(foundingFather));
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
                // Terrain
                int numberOfTypes = FreeCol.getSpecification().numberOfTileTypes();
                for (int type = 0; type < numberOfTypes; type++) {
                    if (type < Tile.ARCTIC) {
                        if(nodeTitle.equals(Tile.getName(type,false,0))) {
                            buildTerrainDetail(type*2);
                        }
                        else if (nodeTitle.equals(Tile.getName(type,true,0))) {
                            buildTerrainDetail(type*2+1);
                        }
                    }
                    else if (nodeTitle.equals(Tile.getName(type,false,0))) {
                        buildTerrainDetail(type*2);
                    }
                }
                if (nodeTitle.equals(Messages.message("hills"))) {
                    buildTerrainDetail(ImageLibrary.HILLS*2);
                } else if (nodeTitle.equals(Messages.message("mountains"))) {
                    buildTerrainDetail(ImageLibrary.MOUNTAINS*2);
                }
            } else if (parentTitle.equals(Messages.message("menuBar.colopedia.unit"))) {
                // Units
                int numberOfTypes = FreeCol.getSpecification().numberOfUnitTypes();
                for (int i = 0; i < numberOfTypes; i++) {
                    if (nodeTitle.equals(Unit.getName(i))) {
                        buildUnitDetail(i);
                    }
                }
            } else if (parentTitle.equals(Messages.message("menuBar.colopedia.goods"))) {
                //Goods
                int numberOfTypes = FreeCol.getSpecification().numberOfGoodsTypes();
                for (int i = 0; i < numberOfTypes; i++) {
                    if (nodeTitle.equals(Goods.getName(i))) {
                        buildGoodsDetail(i);
                    }
                }
            } else if (parentTitle.equals(Messages.message("menuBar.colopedia.skill"))) {
                // Skills
                int numberOfTypes = FreeCol.getSpecification().numberOfUnitTypes();
                for (int type = 0; type < numberOfTypes; type++) {
                    UnitType unitType = FreeCol.getSpecification().unitType(type);
                    if (unitType.getSkill() > 0) {
                        if (nodeTitle.equals(Unit.getName(type))) { 
                            buildUnitDetail(type);
                        }
                    }
                }
            } else if (parentTitle.equals(Messages.message("menuBar.colopedia.building"))) {
                // Buildings
                int numberOfTypes = FreeCol.getSpecification().numberOfBuildingTypes();
                for (int type = 0; type < numberOfTypes; type++) {
                    BuildingType buildingType = FreeCol.getSpecification().buildingType(type);
                    for (int level = 0; level < buildingType.numberOfLevels(); level++) {
                        BuildingType.Level buildingLevel = buildingType.level(level);
                        if (nodeTitle.equals(Messages.message(buildingLevel.name))) {
                            buildBuildingDetail(((type << 2) | (level)));
                        }
                    }
                }
            } else if (parentTitle.equals(Messages.message("menuBar.colopedia.father"))) {
                // Founding Fathers
                for (int i = 0; i < FoundingFather.FATHER_COUNT; i++) {
                    if (nodeTitle.equals(Messages.message(FoundingFather.getName(i)))) {
                        buildFatherDetail(i);
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
