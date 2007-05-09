package net.sf.freecol.client.gui.panel;

import java.awt.BorderLayout;
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
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
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
public final class ColopediaPanel extends FreeColPanel implements ActionListener {

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

    private static final Logger logger = Logger.getLogger(ColopediaPanel.class.getName());

    private static final int OK = -1;

    private final Canvas parent;

    private final ImageLibrary library;

    private JLabel header;

    private JPanel listPanel;

    private JPanel detailPanel;

    private JButton ok;

    public int type;


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
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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
        this.type = type;
        listPanel.removeAll();
        detailPanel.removeAll();
        switch (type) {
        case COLOPEDIA_TERRAIN:
            buildTerrainList();
            if (action == NO_DETAILS) {
                action = Tile.UNEXPLORED;
            }
            buildTerrainDetail(action);
            break;
        case COLOPEDIA_UNIT:
            buildUnitList();
            if (action == NO_DETAILS) {
                action = Unit.FREE_COLONIST;
            }
            buildUnitDetail(action);
            break;
        case COLOPEDIA_GOODS:
            buildGoodsList();
            if (action == NO_DETAILS) {
                action = Goods.FOOD;
            }
            buildGoodsDetail(action);
            break;
        case COLOPEDIA_SKILLS:
            buildSkillsList();
            if (action == NO_DETAILS) {
                action = Unit.EXPERT_FARMER;
            }
            buildUnitDetail(action);
            break;
        case COLOPEDIA_BUILDING:
            buildBuildingList();
            if (action == NO_DETAILS) {
                action = Building.TOWN_HALL;
            }
            buildBuildingDetail(action);
            break;
        case COLOPEDIA_FATHER:
            buildFatherList();
            if (action == NO_DETAILS) {
                action = FoundingFather.ADAM_SMITH;
            }
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
     * Builds the buttons for all the terrains.
     */
    private void buildTerrainList() {
        listPanel.setLayout(new GridLayout(0, 1));

        // TODO: use specification for terrain list
        // If so we need to know if a certain tile should yield a single
        // call with false or two calls (false/true).
        // int numberOfTypes = FreeCol.specification.numberOfTileTypes();

        // type zero is unexplored
        for (int type = 1; type < Tile.ARCTIC; type++) {
            buildTerrainButton(type, false);
            buildTerrainButton(type, true);
        }
        buildTerrainButton(Tile.ARCTIC, false);
        buildTerrainButton(ImageLibrary.HILLS, false);
        buildTerrainButton(ImageLibrary.MOUNTAINS, false);
        buildTerrainButton(Tile.OCEAN, false);
        buildTerrainButton(Tile.HIGH_SEAS, false);

    }

    /**
     * Builds the buttons for all the units.
     */
    private void buildUnitList() {
        listPanel.setLayout(new GridLayout(0, 1));
        buildUnitButton(Unit.FREE_COLONIST, .5f);
        buildUnitButton(Unit.INDENTURED_SERVANT, .5f);
        buildUnitButton(Unit.PETTY_CRIMINAL, .5f);
        buildUnitButton(Unit.INDIAN_CONVERT, .5f);
        buildUnitButton(Unit.BRAVE, .5f);
        buildUnitButton(Unit.COLONIAL_REGULAR, .5f);
        buildUnitButton(Unit.KINGS_REGULAR, .5f);
        buildUnitButton(Unit.CARAVEL, .5f);
        buildUnitButton(Unit.FRIGATE, .5f);
        buildUnitButton(Unit.GALLEON, .5f);
        buildUnitButton(Unit.MAN_O_WAR, .5f);
        buildUnitButton(Unit.MERCHANTMAN, .5f);
        buildUnitButton(Unit.PRIVATEER, .5f);
        buildUnitButton(Unit.ARTILLERY, .5f);
        buildUnitButton(Unit.TREASURE_TRAIN, .5f);
        buildUnitButton(Unit.WAGON_TRAIN, .5f);
        // buildUnitButton(Unit.MILKMAID, .5f);
    }

    /**
     * Builds the buttons for all the goods.
     */
    private void buildGoodsList() {
        listPanel.setLayout(new GridLayout(0, 1));
        buildGoodsButton(Goods.FOOD, ImageLibrary.GOODS_FOOD);
        buildGoodsButton(Goods.SUGAR, ImageLibrary.GOODS_SUGAR);
        buildGoodsButton(Goods.TOBACCO, ImageLibrary.GOODS_TOBACCO);
        buildGoodsButton(Goods.COTTON, ImageLibrary.GOODS_COTTON);
        buildGoodsButton(Goods.FURS, ImageLibrary.GOODS_FURS);
        buildGoodsButton(Goods.LUMBER, ImageLibrary.GOODS_LUMBER);
        buildGoodsButton(Goods.ORE, ImageLibrary.GOODS_ORE);
        buildGoodsButton(Goods.SILVER, ImageLibrary.GOODS_SILVER);
        buildGoodsButton(Goods.HORSES, ImageLibrary.GOODS_HORSES);
        buildGoodsButton(Goods.RUM, ImageLibrary.GOODS_RUM);
        buildGoodsButton(Goods.CIGARS, ImageLibrary.GOODS_CIGARS);
        buildGoodsButton(Goods.CLOTH, ImageLibrary.GOODS_CLOTH);
        buildGoodsButton(Goods.COATS, ImageLibrary.GOODS_COATS);
        buildGoodsButton(Goods.TRADE_GOODS, ImageLibrary.GOODS_TRADE_GOODS);
        buildGoodsButton(Goods.TOOLS, ImageLibrary.GOODS_TOOLS);
        buildGoodsButton(Goods.MUSKETS, ImageLibrary.GOODS_MUSKETS);
    }

    /**
     * Builds the buttons for all the skills.
     */
    private void buildSkillsList() {
        listPanel.setLayout(new GridLayout(0, 1));
        int numberOfTypes = FreeCol.specification.numberOfUnitTypes();
        for (int type = 0; type < numberOfTypes; type++) {
            UnitType unitType = FreeCol.specification.unitType(type);
            if (unitType.skill > 0) {
                buildUnitButton(type, 0.5f);
            }
        }
    }

    /**
     * Builds the buttons for all the buildings.
     */
    private void buildBuildingList() {
        listPanel.setLayout(new GridLayout(0, 1));
        int numberOfTypes = FreeCol.specification.numberOfBuildingTypes();
        for (int type = 0; type < numberOfTypes; type++) {
            BuildingType buildingType = FreeCol.specification.buildingType(type);
            for (int level = 0; level < buildingType.numberOfLevels(); level++) {
                BuildingType.Level buildingLevel = buildingType.level(level);
                JButton button = new JButton(Messages.message(buildingLevel.name));
                button.setActionCommand(String.valueOf(((type << 2) | (level))));
                button.addActionListener(this);
                listPanel.add(button);
            }
        }
    }

    /**
     * Builds the buttons for all the founding fathers.
     */
    private void buildFatherList() {
        listPanel.setLayout(new GridLayout(0, 1));
        for (int i = 0; i < FoundingFather.FATHER_COUNT; i++) {
            buildFatherButton(i);
        }
    }

    /**
     * Builds the button for the given terrain.
     * 
     * @param terrain the type of terrain
     * @param forested whether it is forested
     */
    private void buildTerrainButton(int terrain, boolean forested) {
        Image scaledImage = library.getScaledTerrainImage(terrain, forested, 0.5f);
        ImageIcon icon = new ImageIcon(scaledImage);
        JButton button = new JButton(icon);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setActionCommand(String.valueOf(terrain * 2 + (forested ? 1 : 0)));
        button.addActionListener(this);
        listPanel.add(button);
    }

    /**
     * Builds the button for the given unit.
     * 
     * @param unit
     * @param unitIcon
     */
    private void buildUnitButton(int unit, float scale) {
        int tools = 0;
        if (unit == Unit.HARDY_PIONEER) {
            tools = 100;
        }
        int unitIcon = ImageLibrary.getUnitGraphicsType(unit, false, false, tools, false);
        String name = Unit.getName(unit);
        JButton button;
        if (unitIcon >= 0) {
            ImageIcon icon = library.getScaledUnitImageIcon(unitIcon, scale);
            button = new JButton(name, icon);
            button.setVerticalAlignment(SwingConstants.TOP);
            button.setVerticalTextPosition(SwingConstants.TOP);
        } else {
            button = new JButton(name);
        }
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setActionCommand(String.valueOf(unit));
        button.addActionListener(this);
        listPanel.add(button);
    }

    /**
     * Builds the button for the given goods.
     * 
     * @param goods
     * @param goodsIcon
     */
    private void buildGoodsButton(int goods, int goodsIcon) {
        String name = Goods.getName(goods);
        ImageIcon icon = library.getGoodsImageIcon(goodsIcon);
        JButton button = new JButton(name, icon);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setActionCommand(String.valueOf(goods));
        button.addActionListener(this);
        listPanel.add(button);
    }

    /**
     * Builds the button for the given building.
     * 
     * @param building
     * @param level
     */
    /*
     * private void buildBuildingButton(int building, int level) { String name =
     * Messages.message("colopedia.buildings.name." +
     * buildingCalls[building][level-1]); JButton button = new JButton(name);
     * button.setActionCommand(String.valueOf(((building << 2) | (level-1))));
     * button.addActionListener(this); listPanel.add(button); }
     */

    /**
     * Builds the button for the given founding father.
     * 
     * @param foundingFather
     */
    private void buildFatherButton(int foundingFather) {
        String name = Messages.message(FoundingFather.getName(foundingFather));
        JButton button = new JButton(name);
        button.setActionCommand(String.valueOf(foundingFather));
        button.addActionListener(this);
        listPanel.add(button);
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
        int[] heights = new int[11];
        for (int index = 0; index < 4; index++) {
            heights[2 * index + 1] = margin;
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

        switch (type) {
        case ImageLibrary.HILLS:
            id = "hills";
            name = Messages.message("hills");
            addition = Tile.ADD_HILLS;
            productionIndex = 12;
            defenseBonus = "150%";
            break;
        case ImageLibrary.MOUNTAINS:
            id = "mountains";
            name = Messages.message("mountains");
            addition = Tile.ADD_MOUNTAINS;
            productionIndex = 13;
            defenseBonus = "200%";
            break;
        default:
            TileType tileType = (forested ? FreeCol.specification.tileType(type).whenForested :
                                 FreeCol.specification.tileType(type));
            id = tileType.id;
            name = Messages.message(tileType.name);
            defenseBonus = String.valueOf(tileType.defenceBonus) + "%";
        }

        JLabel nameLabel = new JLabel(name, SwingConstants.CENTER);
        nameLabel.setFont(smallHeaderFont);
        detailPanel.add(nameLabel, higConst.rcwh(row, leftColumn, widths.length, 1));
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.terrain.terrainImage")), higConst.rc(row, leftColumn));
        Image terrainImage = library.getScaledTerrainImage(type, forested, 1f);
        detailPanel.add(new JLabel(new ImageIcon(terrainImage)), higConst.rc(row, rightColumn));
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

        UnitType type = FreeCol.specification.unitType(unit);
        String price = "";
        if (type.id.equals("model.unit.artillery")) {
            price = String.valueOf(parent.getClient().getMyPlayer().getEurope().getArtilleryPrice());
        } else if (type.price > 0) {
            price = String.valueOf(type.price);
        }
        String hammersRequired = "";
        if (type.hammersRequired > 0) {
            hammersRequired = String.valueOf(type.hammersRequired);
        }
        String toolsRequired = "";
        if (type.toolsRequired > 0) {
            toolsRequired = String.valueOf(type.toolsRequired);
        }
        String skill = "";
        if (type.skill != UnitType.UNDEFINED) {
            skill = String.valueOf(type.skill);
        }
        int[] widths = { 0, 3 * margin, 0 };
        int[] heights = new int[17];
        for (int index = 0; index < 8; index++) {
            heights[2 * index + 1] = margin;
        }
        int labelColumn = 1;
        int valueColumn = 3;

        HIGLayout layout = new HIGLayout(widths, heights);
        layout.setColumnWeight(valueColumn, 1);
        detailPanel.setLayout(layout);

        int row = 1;
        JLabel name = new JLabel(Unit.getName(unit), SwingConstants.CENTER);
        name.setFont(smallHeaderFont);
        // name.setPreferredSize(new Dimension(detailPanel.getWidth(), 50));
        detailPanel.add(name, higConst.rcwh(row, labelColumn, widths.length, 1));
        row += 2;

        detailPanel.add(new JLabel(Messages.message("colopedia.unit.offensivePower")), higConst.rc(row, labelColumn));
        detailPanel.add(new JLabel(String.valueOf(type.offence)), higConst.rc(row, valueColumn, "r"));
        row += 2;
        detailPanel.add(new JLabel(Messages.message("colopedia.unit.defensivePower")), higConst.rc(row, labelColumn));
        detailPanel.add(new JLabel(String.valueOf(type.defence)), higConst.rc(row, valueColumn, "r"));
        row += 2;
        if (type.hasAbility("carry-goods") || type.hasAbility("naval")) {
            detailPanel.add(new JLabel(Messages.message("colopedia.unit.capacity")), higConst.rc(row, labelColumn));
            detailPanel.add(new JLabel(String.valueOf(Unit.getInitialSpaceLeft(unit))), higConst.rc(row, valueColumn,
                    "r"));
        } else {
            detailPanel.add(new JLabel(Messages.message("colopedia.unit.skill")), higConst.rc(row, labelColumn));
            detailPanel.add(new JLabel(skill), higConst.rc(row, valueColumn, "r"));
        }
        row += 2;
        detailPanel.add(new JLabel(Messages.message("colopedia.unit.price")), higConst.rc(row, labelColumn));
        detailPanel.add(new JLabel(price), higConst.rc(row, valueColumn, "r"));
        row += 2;
        detailPanel.add(new JLabel(Messages.message("colopedia.unit.hammersRequired")), higConst.rc(row, labelColumn));
        detailPanel.add(new JLabel(hammersRequired), higConst.rc(row, valueColumn, "r"));
        row += 2;
        detailPanel.add(new JLabel(Messages.message("colopedia.unit.toolsRequired")), higConst.rc(row, labelColumn));
        detailPanel.add(new JLabel(toolsRequired), higConst.rc(row, valueColumn, "r"));
        row += 2;
        detailPanel
                .add(new JLabel(Messages.message("colopedia.unit.description")), higConst.rc(row, labelColumn, "tl"));
        detailPanel.add(getDefaultTextArea(Messages.message(type.id + ".description")), higConst.rc(row, valueColumn));

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

        GoodsType type = FreeCol.specification.goodsType(goods);

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
        // name.setPreferredSize(new Dimension(detailPanel.getWidth(), 50));
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

        BuildingType buildingType = FreeCol.specification.buildingType(building);
        BuildingType.Level buildingLevel = buildingType.level(level);

        /**
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
        name.setPreferredSize(new Dimension(detailPanel.getWidth(), 50));
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
        // description.setSize(detailPanel.getWidth(),
        // super.getPreferredSize().height);
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
        } else {
            switch (type) {
            case COLOPEDIA_TERRAIN:
                buildTerrainDetail(action);
                break;
            case COLOPEDIA_UNIT:
                buildUnitDetail(action);
                break;
            case COLOPEDIA_GOODS:
                buildGoodsDetail(action);
                break;
            case COLOPEDIA_SKILLS:
                buildUnitDetail(action);
                break;
            case COLOPEDIA_BUILDING:
                buildBuildingDetail(action);
                break;
            case COLOPEDIA_FATHER:
                buildFatherDetail(action);
                break;
            default:
                logger.warning("Invalid ActionCommand: invalid type " + type);
                break;
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
