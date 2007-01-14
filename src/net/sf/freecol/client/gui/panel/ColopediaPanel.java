
package net.sf.freecol.client.gui.panel;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;

import cz.autel.dmi.HIGLayout;

/**
* This panel displays the Colopedia.
*/
public final class ColopediaPanel extends FreeColPanel implements ActionListener {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    public static final int COLOPEDIA_TERRAIN = 0;
    public static final int COLOPEDIA_UNIT    = 1;
    public static final int COLOPEDIA_GOODS   = 2;
    public static final int COLOPEDIA_SKILLS  = 3;
    public static final int COLOPEDIA_BUILDING = 4;
    public static final int COLOPEDIA_FATHER  = 5;
    
    private static final String[][] buildingCalls = {
        {"TownHall", null, null},
        {"CarpenterHouse", "LumberMill", null},
        {"BlacksmithHouse", "BlacksmithShop", "IronWorks"},
        {"TobacconistHouse", "TobacconistShop", "CigarFactory"},
        {"WeaverHouse", "WeaverShop", "TextileMill"},
        {"DistillerHouse", "RumDistillery", "RumFactory"},
        {"FurTraderHouse", "FurTradingPost", "FurFactory"},
        {"Schoolhouse", "College", "University"},
        {"Armory", "Magazine", "Arsenal"},
        {"Church", "Cathedral", null},
        {"Stockade", "Fort", "Fortress"},
        {"Warehouse", "WarehouseExpansion", null},
        {"Stables", null, null},
        {"Docks", "Drydock", "Shipyard"},
        {"PrintingPress", "Newspaper", null},
        {"CustomHouse", null, null}
    };


    private static final Logger logger = Logger.getLogger(ColopediaPanel.class.getName());
    private static final int    OK = -1;

    private final Canvas parent;
    private final ImageLibrary library;
    private JLabel header;
    private JPanel listPanel;
    private JPanel detailPanel;
    private JButton ok;
    
    public int type;
    
    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ColopediaPanel(Canvas parent) {
        super(new FlowLayout(FlowLayout.CENTER, 1000, 10));
        this.parent = parent;
        this.library = (ImageLibrary) parent.getImageProvider();
        
        setLayout(new BorderLayout());
        
        header = new JLabel(Messages.message("menuBar.colopedia"), SwingConstants.CENTER);
        header.setFont(((Font) UIManager.get("HeaderFont")).deriveFont(0, 48));
        header.setBorder(new EmptyBorder(20, 0, 0, 0));
        add(header, BorderLayout.NORTH);

        listPanel = new JPanel();
        listPanel.setOpaque(false);
        //listPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        add(listPanel, BorderLayout.WEST);

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

    
    /**
     * Prepares this panel to be displayed.
     * @param type - the panel type
     */
    public void initialize(int type) {
        this.type = type;
        listPanel.removeAll();
        switch (type) {
            case COLOPEDIA_TERRAIN:
                buildTerrainList();
                break;
            case COLOPEDIA_UNIT:
                buildUnitList();
                break;
            case COLOPEDIA_GOODS:
                buildGoodsList();
                break;
            case COLOPEDIA_SKILLS:
                buildSkillsList();
                break;
            case COLOPEDIA_BUILDING:
                buildBuildingList();
                break;
            case COLOPEDIA_FATHER:
                buildFatherList();
                break;
            default:
                break;
        }
        detailPanel.removeAll();
        detailPanel.doLayout();
    }

    /**
     * 
     */
    public void requestFocus() {
        ok.requestFocus();
    }

    /**
     * Builds the buttons for all the terrains.
     */
    private void buildTerrainList() {
        listPanel.setLayout(new GridLayout(7, 2));
        buildTerrainButton(Tile.PLAINS);
        buildTerrainButton(Tile.GRASSLANDS);
        buildTerrainButton(Tile.PRAIRIE);
        buildTerrainButton(Tile.SAVANNAH);
        buildTerrainButton(Tile.MARSH);
        buildTerrainButton(Tile.SWAMP);
        buildTerrainButton(Tile.DESERT);
        buildTerrainButton(Tile.TUNDRA);
        buildTerrainButton(Tile.ARCTIC);
        buildTerrainButton(Tile.OCEAN);
        buildTerrainButton(Tile.HIGH_SEAS);
        buildTerrainButton(Messages.message("forest"),    ImageLibrary.FOREST);
        buildTerrainButton(Messages.message("hills"),     ImageLibrary.HILLS);
        buildTerrainButton(Messages.message("mountains"), ImageLibrary.MOUNTAINS);
    }

    /**
     * Builds the buttons for all the units.
     */
    private void buildUnitList() {
        listPanel.setLayout(new GridLayout(8, 2));
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
//        buildUnitButton(Unit.MILKMAID, .5f);
    }

    /**
     * Builds the buttons for all the goods.
     */
    private void buildGoodsList() {
        listPanel.setLayout(new GridLayout(8, 2));
        buildGoodsButton(Goods.FOOD,        ImageLibrary.GOODS_FOOD);
        buildGoodsButton(Goods.SUGAR,       ImageLibrary.GOODS_SUGAR);
        buildGoodsButton(Goods.TOBACCO,     ImageLibrary.GOODS_TOBACCO);
        buildGoodsButton(Goods.COTTON,      ImageLibrary.GOODS_COTTON);
        buildGoodsButton(Goods.FURS,        ImageLibrary.GOODS_FURS);
        buildGoodsButton(Goods.LUMBER,      ImageLibrary.GOODS_LUMBER);
        buildGoodsButton(Goods.ORE,         ImageLibrary.GOODS_ORE);
        buildGoodsButton(Goods.SILVER,      ImageLibrary.GOODS_SILVER);
        buildGoodsButton(Goods.HORSES,      ImageLibrary.GOODS_HORSES);
        buildGoodsButton(Goods.RUM,         ImageLibrary.GOODS_RUM);
        buildGoodsButton(Goods.CIGARS,      ImageLibrary.GOODS_CIGARS);
        buildGoodsButton(Goods.CLOTH,       ImageLibrary.GOODS_CLOTH);
        buildGoodsButton(Goods.COATS,       ImageLibrary.GOODS_COATS);
        buildGoodsButton(Goods.TRADE_GOODS, ImageLibrary.GOODS_TRADE_GOODS);
        buildGoodsButton(Goods.TOOLS,       ImageLibrary.GOODS_TOOLS);
        buildGoodsButton(Goods.MUSKETS,     ImageLibrary.GOODS_MUSKETS);
    }

    /**
     * Builds the buttons for all the skills.
     */
    private void buildSkillsList() {
        listPanel.setLayout(new GridLayout(11, 2));
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
        listPanel.setLayout(new GridLayout(19, 2));
        int numberOfTypes = FreeCol.specification.numberOfBuildingTypes();
        for (int type = 0; type < numberOfTypes; type++) {
            BuildingType buildingType = FreeCol.specification.buildingType(type);
            for (int level = 0; level < buildingType.numberOfLevels(); level++) {
                BuildingType.Level buildingLevel = buildingType.level(level);
                JButton button = new JButton(buildingLevel.name);
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
        listPanel.setLayout(new GridLayout((FoundingFather.FATHER_COUNT+1)/2, 2));
        for (int i=0; i<FoundingFather.FATHER_COUNT; i++) {
            buildFatherButton(i);
        }
    }

    /**
     * Builds the button for the given terrain.
     * @param name
     * @param terrain
     */
    private void buildTerrainButton(String name, int terrain) {
        ImageIcon icon = new ImageIcon(library.getTerrainImage(terrain, 0, 0));
        JButton button = new JButton(name, icon);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setActionCommand(String.valueOf(terrain));
        button.addActionListener(this);
        listPanel.add(button);
    }

    private void buildTerrainButton(int terrain) {
        ImageIcon icon = new ImageIcon(library.getTerrainImage(terrain, 0, 0));
        JButton button = new JButton(FreeCol.specification.tileType(terrain).name, icon);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setActionCommand(String.valueOf(terrain));
        button.addActionListener(this);
        listPanel.add(button);
    }

    /**
     * Builds the button for the given unit.
     * @param unit
     * @param unitIcon
     */
    private void buildUnitButton(int unit, float scale) {
        int tools = 0;
        if (unit == Unit.HARDY_PIONEER) {
            tools = 100;
        }
        int unitIcon = library.getUnitGraphicsType(unit, false, false, tools, false);
        String name = Unit.getName(unit);
        JButton button;
        if (unitIcon >= 0) {
            ImageIcon icon = library.getUnitImageIcon(unitIcon);
            if (scale != 1) {
              Image image;
              image = icon.getImage();
              int width = (int) (scale * image.getWidth(this));
              int height = (int) (scale * image.getHeight(this));
              image = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
              icon = new ImageIcon(image);
            }
            button = new JButton(name, icon);
            button.setVerticalAlignment(SwingConstants.TOP);
            button.setVerticalTextPosition(SwingConstants.TOP);
        }
        else {
            button = new JButton(name);
        }
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setActionCommand(String.valueOf(unit));
        button.addActionListener(this);
        listPanel.add(button);
    }

    /**
     * Builds the button for the given goods.
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
     * @param building
     * @param level
     */
    /*
    private void buildBuildingButton(int building, int level) {
        String name = Messages.message("colopedia.buildings.name." + buildingCalls[building][level-1]);
        JButton button = new JButton(name);
        button.setActionCommand(String.valueOf(((building << 2) | (level-1))));
        button.addActionListener(this);
        listPanel.add(button);
    }
    */

    /**
     * Builds the button for the given founding father.
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
     * @param terrain
     */
    private void buildTerrainDetail(int terrain) {
        detailPanel.removeAll();
        detailPanel.setLayout(new FlowLayout());

        JLabel name = new JLabel("Terrain", SwingConstants.CENTER);
        name.setFont(((Font) UIManager.get("HeaderFont")).deriveFont(0, 24));
        name.setPreferredSize(new Dimension(detailPanel.getWidth(), 50));
        detailPanel.add(name);

        Image terrainImage = library.getTerrainImage(terrain, 0, 0);
        ImageIcon icon = new ImageIcon(terrainImage);
        JLabel imageLabel = new JLabel(icon);
        detailPanel.add(imageLabel);
        
        int tileWidth = library.getTerrainImageWidth(0);
        int tileHeight = library.getTerrainImageHeight(0);
        BufferedImage terrainImage2 = new BufferedImage(tileWidth,
                                                        tileHeight,
                                                        BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = terrainImage2.createGraphics();
        g.drawImage(terrainImage, 0, 0, null);
        Image bonusImage = library.getBonusImageIcon(0).getImage();
        g.drawImage(bonusImage, tileWidth/2 - bonusImage.getWidth(null)/2, tileHeight/2 - bonusImage.getHeight(null)/2, null);

        JTextArea description = new JTextArea();
        description.setBorder(null);
        description.setOpaque(false);
        description.setLineWrap(true);
        description.setEditable(false);
        description.setWrapStyleWord(true);
        description.setFocusable(false);
        //TODO
        description.setText("Production Values");
        description.setSize(detailPanel.getWidth(), super.getPreferredSize().height);
        detailPanel.add(description);
        
        int[][][] potentialtable = {
             // Food    Sugar  Tobac  Cotton Furs   Wood   Ore    Silver Horses Rum    Cigars Cloth  Coats  T.G.   Tools  Musket
                {{0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Unexp
                {{5,3}, {0,0}, {0,0}, {2,1}, {0,3}, {0,6}, {1,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Plains
                {{3,2}, {0,0}, {3,1}, {0,0}, {0,2}, {0,4}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Grasslands
                {{3,2}, {0,0}, {0,0}, {3,1}, {0,2}, {0,6}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Prairie
                {{4,3}, {3,1}, {0,0}, {0,0}, {0,2}, {0,4}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Savannah
                {{3,2}, {0,0}, {0,0}, {0,0}, {0,2}, {0,4}, {2,1}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Marsh
                {{3,2}, {2,1}, {2,1}, {0,0}, {0,1}, {0,4}, {2,1}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Swamp
                {{2,2}, {0,0}, {0,0}, {1,1}, {0,2}, {0,2}, {2,1}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Desert
                {{3,2}, {0,0}, {0,0}, {0,0}, {0,3}, {0,4}, {2,1}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Tundra
                {{0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Arctic
                {{4,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Ocean
                {{4,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // High seas
                {{2,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {4,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}, // Hills
                {{0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {3,0}, {1,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}, {0,0}}  // Mountains
            };

        JPanel production = new JPanel();
        production.setLayout(new GridLayout(2,8));
        production.add(new JLabel(library.getGoodsImageIcon(ImageLibrary.GOODS_FOOD)));
        production.add(new JLabel(library.getGoodsImageIcon(ImageLibrary.GOODS_SUGAR)));
        production.add(new JLabel(library.getGoodsImageIcon(ImageLibrary.GOODS_TOBACCO)));
        production.add(new JLabel(library.getGoodsImageIcon(ImageLibrary.GOODS_COTTON)));
        production.add(new JLabel(library.getGoodsImageIcon(ImageLibrary.GOODS_FURS)));
        production.add(new JLabel(library.getGoodsImageIcon(ImageLibrary.GOODS_LUMBER)));
        production.add(new JLabel(library.getGoodsImageIcon(ImageLibrary.GOODS_ORE)));
        production.add(new JLabel(library.getGoodsImageIcon(ImageLibrary.GOODS_SILVER)));
//        production.add(new JLabel(library.getGoodsImageIcon(ImageLibrary.GOODS_FISH)));
        production.add(new JLabel(potentialtable[terrain][0][0] + "/" + potentialtable[terrain][0][1]));
        production.add(new JLabel(potentialtable[terrain][1][0] + "/" + potentialtable[terrain][1][1]));
        production.add(new JLabel(potentialtable[terrain][2][0] + "/" + potentialtable[terrain][2][1]));
        production.add(new JLabel(potentialtable[terrain][3][0] + "/" + potentialtable[terrain][3][1]));
        production.add(new JLabel(potentialtable[terrain][4][0] + "/" + potentialtable[terrain][4][1]));
        production.add(new JLabel(potentialtable[terrain][5][0] + "/" + potentialtable[terrain][5][1]));
        production.add(new JLabel(potentialtable[terrain][6][0] + "/" + potentialtable[terrain][6][1]));
        production.add(new JLabel(potentialtable[terrain][7][0] + "/" + potentialtable[terrain][7][1]));
//        production.add(new JLabel(potentialtable[terrain][0][0] + "/" + potentialtable[terrain][0][1]));
        detailPanel.add(production);
        
        detailPanel.doLayout();
    }
    
    /**
     * Builds the details panel for the given unit.
     * @param unit
     */
    private void buildUnitDetail(int unit) {
        detailPanel.removeAll();
        detailPanel.setLayout(new FlowLayout());

        JLabel name = new JLabel(Unit.getName(unit), SwingConstants.CENTER);
        name.setFont(((Font) UIManager.get("HeaderFont")).deriveFont(0, 24));
        name.setPreferredSize(new Dimension(detailPanel.getWidth(), 50));
        detailPanel.add(name);

        JTextArea description = new JTextArea();
        description.setBorder(null);
        description.setOpaque(false);
        description.setLineWrap(true);
        description.setEditable(false);
        description.setWrapStyleWord(true);
        description.setFocusable(false);
        //TODO
        description.setText("");
        description.setSize(detailPanel.getWidth(), super.getPreferredSize().height);
        detailPanel.add(description);

        detailPanel.doLayout();
    }

    /**
     * Builds the details panel for the given goods.
     * @param goods
     */
    private void buildGoodsDetail(int goods) {
        detailPanel.removeAll();
        detailPanel.setLayout(new FlowLayout());

        JLabel name = new JLabel(Goods.getName(goods), SwingConstants.CENTER);
        name.setFont(((Font) UIManager.get("HeaderFont")).deriveFont(0, 24));
        name.setPreferredSize(new Dimension(detailPanel.getWidth(), 50));
        detailPanel.add(name);

        JTextArea description = new JTextArea();
        description.setBorder(null);
        description.setOpaque(false);
        description.setLineWrap(true);
        description.setEditable(false);
        description.setWrapStyleWord(true);
        description.setFocusable(false);
        //TODO
        description.setText("");
        description.setSize(detailPanel.getWidth(), super.getPreferredSize().height);
        detailPanel.add(description);

        detailPanel.doLayout();
    }

    /**
     * Builds the details panel for the given building.
     * @param action
     */
    private void buildBuildingDetail(int action) {
        detailPanel.removeAll();
        detailPanel.repaint();

        int[] widths = {0, 3 * margin, 0};
        int[] heights = new int[15];
        for (int index = 0; index < 7; index++) {
            heights[2 * index + 1] = margin;
        }
        int row = 1;
        int leftColumn = 1;
        int rightColumn = 3;
        detailPanel.setLayout(new HIGLayout(widths, heights));

        int building = action >> 2;
        int level = (action & 0x03);

        BuildingType buildingType = FreeCol.specification.buildingType(building);
        BuildingType.Level buildingLevel = buildingType.level(level);

        /** don't need this at the moment
            int[][] buildingUpkeep = {
            {0, -1, -1},                // Town hall
            {0, 10, -1},                // Carpenter's house, Lumber mill
            {0, 5, 15},                 // Blacksmith's house, Blacksmith's shop, Iron works
            {0, 5, 15},                 // Tobacconist's house, Tobacconist's shop, Cigar factory
            {0, 5, 15},                 // Weaver's house, Weaver's shop, Textile mill
            {0, 5, 15},                 // Distiller's house, Rum distillery, Rum factory
            {0, 5, 15},                 // Fur trader's house, Fur trading post, Fur factory
            {5, 10, 15},                // Schoolhouse, College, University
            {5, 10, 15},                // Armory, Magazine, Arsenal
            {5, 15, -1},                // Church, Cathedral
            {0, 10, 15},                // Stockade, Fort, Fortress
            {5, 5, -1},                 // Warehouse, Warehouse expansion
            {5, -1, -1},                // Stables
            {5, 10, 15},                // Docks, Drydock, Shipyard
            {5, 10, -1},                // Printing press, Newspaper
            {15, -1, -1}                // Custom house
            };
        */

        JLabel name = new JLabel(buildingLevel.name, SwingConstants.CENTER);
        name.setFont(((Font) UIManager.get("HeaderFont")).deriveFont(0, 24));
        name.setPreferredSize(new Dimension(detailPanel.getWidth(), 50));
        detailPanel.add(name, higConst.rcwh(row, leftColumn, widths.length, 1));
        row += 2;

        // Requires - prerequisites to build
        String requiresText = "";
        if (buildingLevel.populationRequired > 0) {
            requiresText += String.valueOf(buildingLevel.populationRequired) + " " +
                            Messages.message("colonists");
        }
        if (level > 0) {
          requiresText += "\n" + buildingType.level(level - 1).name;
        }
        if (level > 1 && (building==Building.BLACKSMITH ||
                          building==Building.TOBACCONIST ||
                          building==Building.WEAVER ||
                          building==Building.DISTILLER ||
                          building==Building.FUR_TRADER ||
                          building==Building.ARMORY)) {
            requiresText += "\n" + Messages.message(FoundingFather.getName(FoundingFather.ADAM_SMITH));
        }
        if (building==Building.CUSTOM_HOUSE) {
            requiresText += "\n" + Messages.message(FoundingFather.getName(FoundingFather.PETER_STUYVESANT));
        }

        JTextArea requires = getDefaultTextArea(requiresText);
        detailPanel.add(new JLabel(Messages.message("colopedia.buildings.requires")),
                        higConst.rc(row, leftColumn, "tl"));
        detailPanel.add(requires, higConst.rc(row, rightColumn));
        row += 2;

        // Costs to build - Hammers & Tools
        JPanel costs = new JPanel();
        costs.setOpaque(false);
        costs.setLayout(new FlowLayout(FlowLayout.LEFT));
        costs.add(new JLabel(Integer.toString(buildingLevel != null ? buildingLevel.hammersRequired : -1), library.getGoodsImageIcon(ImageLibrary.GOODS_HAMMERS), SwingConstants.LEFT));
        costs.add(new JLabel(Integer.toString(buildingLevel != null ? buildingLevel.toolsRequired : -1), library.getGoodsImageIcon(ImageLibrary.GOODS_TOOLS), SwingConstants.LEFT));
        detailPanel.add(new JLabel(Messages.message("colopedia.buildings.cost")),
                        higConst.rc(row, leftColumn));
        detailPanel.add(costs, higConst.rc(row, rightColumn));
        row += 2;

        // Specialist
        JLabel specialist = new JLabel();
        int unitType = Building.getExpertUnitType(building);
        if (unitType >= 0) {
            int graphicsType = library.getUnitGraphicsType(unitType, false, false, 0, false);
            specialist.setIcon(library.getUnitImageIcon(graphicsType));
            specialist.setText(Unit.getName(unitType));
            specialist.setHorizontalAlignment(SwingConstants.LEFT);
        }
        detailPanel.add(new JLabel(Messages.message("colopedia.buildings.specialist")),
                     higConst.rc(row, leftColumn));
        detailPanel.add(specialist, higConst.rc(row, rightColumn));
        row += 2;

        // Production - Needs & Produces
        JPanel production = new JPanel();
        production.setOpaque(false);
        production.setLayout(new FlowLayout(FlowLayout.LEFT));
        int inputType = Building.getGoodsInputType(building);
        if (inputType >= 0) {
            JLabel label = new JLabel(Messages.message("colopedia.buildings.needs"),
                                      library.getGoodsImageIcon(inputType),
                                      SwingConstants.LEADING);
            label.setHorizontalTextPosition(SwingConstants.LEADING);
            production.add(label);
        }
        int outputType = Building.getGoodsOutputType(building);
        if (outputType >= 0) {
            JLabel label = new JLabel(Messages.message("colopedia.buildings.produces"),
                                      library.getGoodsImageIcon(outputType),
                                      SwingConstants.LEADING);
            label.setHorizontalTextPosition(SwingConstants.LEADING);
            production.add(label);
        }
        detailPanel.add(new JLabel(Messages.message("colopedia.buildings.production")),
                     higConst.rc(row, leftColumn));
        detailPanel.add(production, higConst.rc(row, rightColumn));
        row += 2;

        // Upkeep
        //detailPanel.add(new JLabel(Messages.message("colopedia.buildings.upkeep")));
        //detailPanel.add(new JLabel(Integer.toString(buildingUpkeep[building][level])));

        // Notes
        JTextArea notes = getDefaultTextArea(Messages.message("colopedia.buildings.notes." + 
                                                              buildingCalls[building][level]));

        detailPanel.add(new JLabel(Messages.message("colopedia.buildings.notes")),
                     higConst.rc(row, leftColumn, "tl"));
        detailPanel.add(notes, higConst.rc(row, rightColumn));

        detailPanel.doLayout();
    }

    /**
     * Builds the details panel for the given founding father.
     * @param foundingFather
     */
    private void buildFatherDetail(int foundingFather) {
        detailPanel.removeAll();
        detailPanel.repaint();
        detailPanel.setLayout(new FlowLayout());

        JLabel name = new JLabel(Messages.message(FoundingFather.getName(foundingFather)), SwingConstants.CENTER);
        name.setFont(((Font) UIManager.get("HeaderFont")).deriveFont(0, 24));
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

        String text = Messages.message(FoundingFather.getDescription(foundingFather)) +
                      "\n\n" + "[" + Messages.message(FoundingFather.getBirthAndDeath(foundingFather)) +
                      "] " + Messages.message(FoundingFather.getText(foundingFather));
        JTextArea description = getDefaultTextArea(text);
        description.setColumns(32);
        description.setSize(description.getPreferredSize());
        //description.setSize(detailPanel.getWidth(), super.getPreferredSize().height);
        detailPanel.add(description);
        
        detailPanel.doLayout();
    }

    /**
     * This function analyses an event and calls the right methods to take
     * care of the user's requests.
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
}
