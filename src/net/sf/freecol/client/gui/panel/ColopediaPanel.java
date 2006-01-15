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

/**
* This panel displays the Colopedia.
*/
public final class ColopediaPanel extends FreeColPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
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

    // This is copied from net.sf.freecol.client.gui.ImageLibrary where it is private 
    private static final int FREE_COLONIST = 0,
                            EXPERT_FARMER = 1,
                            EXPERT_FISHERMAN = 2,
                            EXPERT_FUR_TRAPPER = 3,
                            EXPERT_SILVER_MINER = 4,
                            EXPERT_LUMBER_JACK = 5,
                            EXPERT_ORE_MINER = 6,
                            MASTER_SUGAR_PLANTER = 7,
                            MASTER_COTTON_PLANTER = 8,
                            MASTER_TOBACCO_PLANTER = 9,

                            FIREBRAND_PREACHER = 10,
                            ELDER_STATESMAN = 11,
                        
                            MASTER_CARPENTER = 12,
                            MASTER_DISTILLER = 13,
                            MASTER_WEAVER = 14,
                            MASTER_TOBACCONIST = 15,
                            MASTER_FUR_TRADER = 16,
                            MASTER_BLACKSMITH = 17,
                            MASTER_GUNSMITH = 18,
                        
                            SEASONED_SCOUT_NOT_MOUNTED = 19,
                            HARDY_PIONEER_NO_TOOLS = 20,
                            UNARMED_VETERAN_SOLDIER = 21,
                            JESUIT_MISSIONARY = 22,
                            MISSIONARY_FREE_COLONIST = 23,
                        
                            SEASONED_SCOUT_MOUNTED = 24,
                            HARDY_PIONEER_WITH_TOOLS = 25,
                            FREE_COLONIST_WITH_TOOLS = 26,
                            INDENTURED_SERVANT = 27,
                            PETTY_CRIMINAL = 28,

                            INDIAN_CONVERT = 29,
                            BRAVE = 30,
                        
                            UNARMED_COLONIAL_REGULAR = 31,
                            UNARMED_KINGS_REGULAR = 32,
                        
                            SOLDIER = 33,
                            VETERAN_SOLDIER = 34,
                            COLONIAL_REGULAR = 35,
                            KINGS_REGULAR = 36,
                            UNARMED_DRAGOON = 37,
                            UNARMED_VETERAN_DRAGOON = 38,
                            UNARMED_COLONIAL_CAVALRY = 39,
                            UNARMED_KINGS_CAVALRY = 40,
                            DRAGOON = 41,
                            VETERAN_DRAGOON = 42,
                            COLONIAL_CAVALRY = 43,
                            KINGS_CAVALRY = 44,
                        
                            ARMED_BRAVE = 45,
                            MOUNTED_BRAVE = 46,
                            INDIAN_DRAGOON = 47,
                        
                            CARAVEL = 48,
                            FRIGATE = 49,
                            GALLEON = 50,
                            MAN_O_WAR = 51,
                            MERCHANTMAN = 52,
                            PRIVATEER = 53,
                        
                            ARTILLERY = 54,
                            DAMAGED_ARTILLERY = 55,
                            TREASURE_TRAIN = 56,
                            WAGON_TRAIN = 57,
                        
                            MILKMAID = 58,
                            JESUIT_MISSIONARY_NO_CROSS = 59,
                        
                            UNIT_GRAPHICS_COUNT = 60;

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
        
        header = new JLabel("Colonizopedia", JLabel.CENTER);
        header.setFont(((Font) UIManager.get("HeaderFont")).deriveFont(0, 48));
        header.setBorder(new EmptyBorder(20, 0, 0, 0));
        add(header, BorderLayout.NORTH);

        listPanel = new JPanel();
        listPanel.setOpaque(false);
        listPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
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
        buildTerrainButton("Plains",     Tile.PLAINS);
        buildTerrainButton("Grasslands", Tile.GRASSLANDS);
        buildTerrainButton("Prairie",    Tile.PRAIRIE);
        buildTerrainButton("Savannah",   Tile.SAVANNAH);
        buildTerrainButton("Marsh",      Tile.MARSH);
        buildTerrainButton("Swamp",      Tile.SWAMP);
        buildTerrainButton("Desert",     Tile.DESERT);
        buildTerrainButton("Tundra",     Tile.TUNDRA);
        buildTerrainButton("Arctic",     Tile.ARCTIC);
        buildTerrainButton("Ocean",      Tile.OCEAN);
        buildTerrainButton("High Seas",  Tile.HIGH_SEAS);
        buildTerrainButton("Forest",     ImageLibrary.FOREST);
        buildTerrainButton("Hill",       ImageLibrary.HILLS);
        buildTerrainButton("Mountain",   ImageLibrary.MOUNTAINS);
    }

    /**
     * Builds the buttons for all the units.
     */
    private void buildUnitList() {
        listPanel.setLayout(new GridLayout(8, 2));
        buildUnitButton(Unit.FREE_COLONIST,      FREE_COLONIST,      .5f); //ImageLibrary.FREE_COLONIST);
        buildUnitButton(Unit.INDENTURED_SERVANT, INDENTURED_SERVANT, .5f); //ImageLibrary.INDENTURED_SERVANT);
        buildUnitButton(Unit.PETTY_CRIMINAL,     PETTY_CRIMINAL,     .5f); //ImageLibrary.PETTY_CRIMINAL);
        buildUnitButton(Unit.INDIAN_CONVERT,     INDIAN_CONVERT,     .5f); //ImageLibrary.INDIAN_CONVERT);
        buildUnitButton(Unit.BRAVE,              BRAVE,              .5f); //ImageLibrary.BRAVE);
        buildUnitButton(Unit.COLONIAL_REGULAR,   COLONIAL_REGULAR,   .5f); //ImageLibrary.COLONIAL_REGULAR);
        buildUnitButton(Unit.KINGS_REGULAR,      KINGS_REGULAR,      .5f); //ImageLibrary.KINGS_REGULAR);
        buildUnitButton(Unit.CARAVEL,            CARAVEL,            .5f); //ImageLibrary.CARAVEL);
        buildUnitButton(Unit.FRIGATE,            FRIGATE,            .5f); //ImageLibrary.FRIGATE);
        buildUnitButton(Unit.GALLEON,            GALLEON,            .5f); //ImageLibrary.GALLEON);
        buildUnitButton(Unit.MAN_O_WAR,          MAN_O_WAR,          .5f); //ImageLibrary.MAN_O_WAR);
        buildUnitButton(Unit.MERCHANTMAN,        MERCHANTMAN,        .5f); //ImageLibrary.MERCHANTMAN);
        buildUnitButton(Unit.PRIVATEER,          PRIVATEER,          .5f); //ImageLibrary.PRIVATEER);
        buildUnitButton(Unit.ARTILLERY,          ARTILLERY,          .5f); //ImageLibrary.ARTILLERY);
        buildUnitButton(Unit.TREASURE_TRAIN,     TREASURE_TRAIN,     .5f); //ImageLibrary.TREASURE_TRAIN);
        buildUnitButton(Unit.WAGON_TRAIN,        WAGON_TRAIN,        .5f); //ImageLibrary.WAGON_TRAIN);
//        buildUnitButton(Unit.MILKMAID,           MILKMAID,           .5f); //ImageLibrary.MILKMAID);
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
        buildUnitButton(Unit.EXPERT_FARMER,          EXPERT_FARMER,            .5f); //ImageLibrary.EXPERT_FARMER);
        buildUnitButton(Unit.EXPERT_FISHERMAN,       EXPERT_FISHERMAN,         .5f); //ImageLibrary.EXPERT_FISHERMAN);
        buildUnitButton(Unit.EXPERT_FUR_TRAPPER,     EXPERT_FUR_TRAPPER,       .5f); //ImageLibrary.EXPERT_FUR_TRAPPER);
        buildUnitButton(Unit.EXPERT_SILVER_MINER,    EXPERT_SILVER_MINER,      .5f); //ImageLibrary.EXPERT_SILVER_MINER);
        buildUnitButton(Unit.EXPERT_LUMBER_JACK,     EXPERT_LUMBER_JACK,       .5f); //ImageLibrary.EXPERT_LUMBER_JACK);
        buildUnitButton(Unit.EXPERT_ORE_MINER,       EXPERT_ORE_MINER,         .5f); //ImageLibrary.EXPERT_ORE_MINER);
        buildUnitButton(Unit.MASTER_SUGAR_PLANTER,   MASTER_SUGAR_PLANTER,     .5f); //ImageLibrary.MASTER_SUGAR_PLANTER);
        buildUnitButton(Unit.MASTER_COTTON_PLANTER,  MASTER_COTTON_PLANTER,    .5f); //ImageLibrary.MASTER_COTTON_PLANTER);
        buildUnitButton(Unit.MASTER_TOBACCO_PLANTER, MASTER_TOBACCO_PLANTER,   .5f); //ImageLibrary.MASTER_TOBACCO_PLANTER);
        buildUnitButton(Unit.FIREBRAND_PREACHER,     FIREBRAND_PREACHER,       .5f); //ImageLibrary.FIREBRAND_PREACHER);
        buildUnitButton(Unit.ELDER_STATESMAN,        ELDER_STATESMAN,          .5f); //ImageLibrary.ELDER_STATESMAN);
        buildUnitButton(Unit.MASTER_CARPENTER,       MASTER_CARPENTER,         .5f); //ImageLibrary.MASTER_CARPENTER);
        buildUnitButton(Unit.MASTER_DISTILLER,       MASTER_DISTILLER,         .5f); //ImageLibrary.MASTER_DISTILLER);
        buildUnitButton(Unit.MASTER_WEAVER,          MASTER_WEAVER,            .5f); //ImageLibrary.MASTER_WEAVER);
        buildUnitButton(Unit.MASTER_TOBACCONIST,     MASTER_TOBACCONIST,       .5f); //ImageLibrary.MASTER_TOBACCONIST);
        buildUnitButton(Unit.MASTER_FUR_TRADER,      MASTER_FUR_TRADER,        .5f); //ImageLibrary.MASTER_FUR_TRADER);
        buildUnitButton(Unit.MASTER_BLACKSMITH,      MASTER_BLACKSMITH,        .5f); //ImageLibrary.MASTER_BLACKSMITH);
        buildUnitButton(Unit.MASTER_GUNSMITH,        MASTER_GUNSMITH,          .5f); //ImageLibrary.MASTER_GUNSMITH);
        buildUnitButton(Unit.SEASONED_SCOUT,         SEASONED_SCOUT_MOUNTED,   .5f); //ImageLibrary.SEASONED_SCOUT_MOUNTED);
        buildUnitButton(Unit.HARDY_PIONEER,          HARDY_PIONEER_WITH_TOOLS, .5f); //ImageLibrary.HARDY_PIONEER_WITH_TOOLS);
        buildUnitButton(Unit.VETERAN_SOLDIER,        VETERAN_SOLDIER,          .5f); //ImageLibrary.VETERAN_SOLDIER);
        buildUnitButton(Unit.JESUIT_MISSIONARY,      JESUIT_MISSIONARY,        .5f); //ImageLibrary.JESUIT_MISSIONARY);
    }

    /**
     * Builds the buttons for all the buildings.
     */
    private void buildBuildingList() {
        listPanel.setLayout(new GridLayout(19, 2));
        buildBuildingButton(Building.TOWN_HALL,      Building.HOUSE);
        buildBuildingButton(Building.CARPENTER,      Building.HOUSE);
        buildBuildingButton(Building.CARPENTER,      Building.SHOP);
        buildBuildingButton(Building.BLACKSMITH,     Building.HOUSE);
        buildBuildingButton(Building.BLACKSMITH,     Building.SHOP);
        buildBuildingButton(Building.BLACKSMITH,     Building.FACTORY);
        buildBuildingButton(Building.TOBACCONIST,    Building.HOUSE);
        buildBuildingButton(Building.TOBACCONIST,    Building.SHOP);
        buildBuildingButton(Building.TOBACCONIST,    Building.FACTORY);
        buildBuildingButton(Building.WEAVER,         Building.HOUSE);
        buildBuildingButton(Building.WEAVER,         Building.SHOP);
        buildBuildingButton(Building.WEAVER,         Building.FACTORY);
        buildBuildingButton(Building.DISTILLER,      Building.HOUSE);
        buildBuildingButton(Building.DISTILLER,      Building.SHOP);
        buildBuildingButton(Building.DISTILLER,      Building.FACTORY);
        buildBuildingButton(Building.FUR_TRADER,     Building.HOUSE);
        buildBuildingButton(Building.FUR_TRADER,     Building.SHOP);
        buildBuildingButton(Building.FUR_TRADER,     Building.FACTORY);
        buildBuildingButton(Building.SCHOOLHOUSE,    Building.HOUSE);
        buildBuildingButton(Building.SCHOOLHOUSE,    Building.SHOP);
        buildBuildingButton(Building.SCHOOLHOUSE,    Building.FACTORY);
        buildBuildingButton(Building.ARMORY,         Building.HOUSE);
        buildBuildingButton(Building.ARMORY,         Building.SHOP);
        buildBuildingButton(Building.CHURCH,         Building.HOUSE);
        buildBuildingButton(Building.CHURCH,         Building.SHOP);
        buildBuildingButton(Building.STOCKADE,       Building.HOUSE);
        buildBuildingButton(Building.STOCKADE,       Building.SHOP);
        buildBuildingButton(Building.STOCKADE,       Building.FACTORY);
        buildBuildingButton(Building.WAREHOUSE,      Building.HOUSE);
        buildBuildingButton(Building.WAREHOUSE,      Building.SHOP);
        buildBuildingButton(Building.STABLES,        Building.HOUSE);
        buildBuildingButton(Building.DOCK,           Building.HOUSE);
        buildBuildingButton(Building.DOCK,           Building.SHOP);
        buildBuildingButton(Building.DOCK,           Building.FACTORY);
        buildBuildingButton(Building.PRINTING_PRESS, Building.HOUSE);
        buildBuildingButton(Building.PRINTING_PRESS, Building.SHOP);
        buildBuildingButton(Building.CUSTOM_HOUSE,   Building.HOUSE);
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
        button.setHorizontalAlignment(JButton.LEFT);
        button.setActionCommand(String.valueOf(terrain));
        button.addActionListener(this);
        listPanel.add(button);
    }

    /**
     * Builds the button for the given unit.
     * @param unit
     * @param unitIcon
     */
    private void buildUnitButton(int unit, int unitIcon, float scale) {
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
            button.setVerticalAlignment(JButton.TOP);
            button.setVerticalTextPosition(JButton.TOP);
        }
        else {
            button = new JButton(name);
        }
        button.setHorizontalAlignment(JButton.LEFT);
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
        button.setHorizontalAlignment(JButton.LEFT);
        button.setActionCommand(String.valueOf(goods));
        button.addActionListener(this);
        listPanel.add(button);
    }
    
    /**
     * Builds the button for the given building.
     * @param building
     * @param level
     */
    private void buildBuildingButton(int building, int level) {
        String name = Messages.message("colopedia.buildings.name." + buildingCalls[building][level-1]);
        JButton button = new JButton(name);
        button.setActionCommand(String.valueOf(((building << 2) | (level-1))));
        button.addActionListener(this);
        listPanel.add(button);
    }

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

        JLabel name = new JLabel("Terrain", JLabel.CENTER);
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
        
        int potentialtable[][][] = {
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

        JLabel name = new JLabel(Unit.getName(unit), JLabel.CENTER);
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

        JLabel name = new JLabel(Goods.getName(goods), JLabel.CENTER);
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
        detailPanel.setLayout(new FlowLayout());

        int building = action >> 2;
        int level = (action & 0x03);
      
        int[] buildingSpecialist = {
            Unit.ELDER_STATESMAN,       // Town hall
            Unit.MASTER_CARPENTER,      // Carpenter's house, Lumber mill
            Unit.MASTER_BLACKSMITH,     // Blacksmith's house, Blacksmith's shop, Iron works
            Unit.MASTER_TOBACCONIST,    // Tobacconist's house, Tobacconist's shop, Cigar factory
            Unit.MASTER_WEAVER,         // Weaver's house, Weaver's shop, Textile mill
            Unit.MASTER_DISTILLER,      // Distiller's house, Rum distillery, Rum factory
            Unit.MASTER_FUR_TRADER,     // Fur trader's house, Fur trading post, Fur factory
            -1,                         // Schoolhouse, College, University
            Unit.MASTER_GUNSMITH,       // Armory, Magazine, Arsenal
            Unit.FIREBRAND_PREACHER,    // Church, Cathedral
            -1,                         // Stockade, Fort, Fortress
            -1,                         // Warehouse, Warehouse expansion
            -1,                         // Stables
            -1,                         // Docks, Drydock, Shipyard
            -1,                         // Printing press, Newspaper
            -1                          // Custom house
        };

        int[] buildingSpecialistIcon = {
            ELDER_STATESMAN,            // Town hall
            MASTER_CARPENTER,           // Carpenter's house, Lumber mill
            MASTER_BLACKSMITH,          // Blacksmith's house, Blacksmith's shop, Iron works
            MASTER_TOBACCONIST,         // Tobacconist's house, Tobacconist's shop, Cigar factory
            MASTER_WEAVER,              // Weaver's house, Weaver's shop, Textile mill
            MASTER_DISTILLER,           // Distiller's house, Rum distillery, Rum factory
            MASTER_FUR_TRADER,          // Fur trader's house, Fur trading post, Fur factory
            -1,                         // Schoolhouse, College, University
            MASTER_GUNSMITH,            // Armory, Magazine, Arsenal
            FIREBRAND_PREACHER,         // Church, Cathedral
            -1,                         // Stockade, Fort, Fortress
            -1,                         // Warehouse, Warehouse expansion
            -1,                         // Stables
            -1,                         // Docks, Drydock, Shipyard
            -1,                         // Printing press, Newspaper
            -1                          // Custom house
        };

        int[] buildingNeeds = {
            -1,                         // Town hall
            ImageLibrary.GOODS_LUMBER,  // Carpenter's house, Lumber mill
            ImageLibrary.GOODS_ORE,     // Blacksmith's house, Blacksmith's shop, Iron works
            ImageLibrary.GOODS_TOBACCO, // Tobacconist's house, Tobacconist's shop, Cigar factory
            ImageLibrary.GOODS_COTTON,  // Weaver's house, Weaver's shop, Textile mill
            ImageLibrary.GOODS_SUGAR,   // Distiller's house, Rum distillery, Rum factory
            ImageLibrary.GOODS_FURS,    // Fur trader's house, Fur trading post, Fur factory
            -1,                         // Schoolhouse, College, University
            ImageLibrary.GOODS_TOOLS,   // Armory, Magazine, Arsenal
            -1,                         // Church, Cathedral
            -1,                         // Stockade, Fort, Fortress
            -1,                         // Warehouse, Warehouse expansion
            -1,                         // Stables
            -1,                         // Docks, Drydock, Shipyard
            -1,                         // Printing press, Newspaper
            -1                          // Custom house
        };
        
        int[] buildingProduces = {
            ImageLibrary.GOODS_BELLS,   // Town hall
            ImageLibrary.GOODS_HAMMERS, // Carpenter's house, Lumber mill
            ImageLibrary.GOODS_TOOLS,   // Blacksmith's house, Blacksmith's shop, Iron works
            ImageLibrary.GOODS_CIGARS,  // Tobacconist's house, Tobacconist's shop, Cigar factory
            ImageLibrary.GOODS_CLOTH,   // Weaver's house, Weaver's shop, Textile mill
            ImageLibrary.GOODS_RUM,     // Distiller's house, Rum distillery, Rum factory
            ImageLibrary.GOODS_COATS,   // Fur trader's house, Fur trading post, Fur factory
            -1,                         // Schoolhouse, College, University
            ImageLibrary.GOODS_MUSKETS, // Armory, Magazine, Arsenal
            ImageLibrary.GOODS_CROSSES, // Church, Cathedral
            -1,                         // Stockade, Fort, Fortress
            -1,                         // Warehouse, Warehouse expansion
            -1,                         // Stables
            -1,                         // Docks, Drydock, Shipyard
            -1,                         // Printing press, Newspaper
            -1                          // Custom house
        };

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

        JLabel name = new JLabel(Messages.message("colopedia.buildings.name." + buildingCalls[building][level]), JLabel.CENTER);
        name.setFont(((Font) UIManager.get("HeaderFont")).deriveFont(0, 24));
        name.setPreferredSize(new Dimension(detailPanel.getWidth(), 50));
        detailPanel.add(name);

        JPanel overview = new JPanel();
        overview.setLayout(new GridLayout(8,2));
        // Requires - prerequisit to build
        overview.add(new JLabel(Messages.message("colopedia.buildings.requires")));
        JTextArea requires = new JTextArea();
        requires.setBorder(null);
        requires.setOpaque(false);
        requires.setLineWrap(true);
        requires.setEditable(false);
        requires.setWrapStyleWord(true);
        requires.setFocusable(false);
        requires.setFont(requires.getFont().deriveFont(Font.BOLD));
        BuildingType  buildingType = FreeCol.specification.buildingType( building );
        BuildingType.Level  buildingLevel = level < buildingType.numberOfLevels() ? buildingType.level(level) : null;
        String requiresText = (buildingLevel != null ? buildingLevel.populationRequired : -1) + " colonists";
        if (level > 0) {
          requiresText = requiresText + "\n" +
                         Messages.message("colopedia.buildings.name." + buildingCalls[building][level-1]);
        }
        if (level > 1 && (building==Building.BLACKSMITH ||
                          building==Building.TOBACCONIST ||
                          building==Building.WEAVER ||
                          building==Building.DISTILLER ||
                          building==Building.FUR_TRADER ||
                          building==Building.ARMORY)) {
            requiresText = requiresText + "\n" + Messages.message(FoundingFather.getName(FoundingFather.ADAM_SMITH)) + " in congress";
        }
        if (building==Building.CUSTOM_HOUSE) {
            requiresText = requiresText + "\n" + Messages.message(FoundingFather.getName(FoundingFather.PETER_STUYVESANT)) + " in congress";
        }
        requires.setText(requiresText);
        overview.add(requires);
        // Costs to build - Hammers & Tools
        overview.add(new JLabel(Messages.message("colopedia.buildings.cost")));
        JPanel costs = new JPanel();
        costs.setLayout(new FlowLayout(FlowLayout.LEFT));
        costs.add(new JLabel(Integer.toString(buildingLevel != null ? buildingLevel.hammersRequired : -1), library.getGoodsImageIcon(ImageLibrary.GOODS_HAMMERS), JLabel.LEFT));
        costs.add(new JLabel(Integer.toString(buildingLevel != null ? buildingLevel.toolsRequired : -1), library.getGoodsImageIcon(ImageLibrary.GOODS_TOOLS), JLabel.LEFT));
        overview.add(costs);
        // Specialist
        overview.add(new JLabel(Messages.message("colopedia.buildings.specialist")));
        overview.add(buildingSpecialist[building]>=0 ? new JLabel(Unit.getName(buildingSpecialist[building]), library.getUnitImageIcon(buildingSpecialistIcon[building]), JLabel.LEFT) : new JLabel());
        // Production - Needs & Produces
        overview.add(new JLabel(Messages.message("colopedia.buildings.production")));
        JPanel production = new JPanel();
        production.setLayout(new FlowLayout(FlowLayout.LEFT));
        if (buildingNeeds[building] >= 0) {
            JLabel label = new JLabel(Messages.message("colopedia.buildings.needs"), library.getGoodsImageIcon(buildingNeeds[building]), JLabel.LEADING);
            label.setHorizontalTextPosition(JLabel.LEADING);
            production.add(label);
        }
        if (buildingProduces[building] >= 0) {
            JLabel label = new JLabel(Messages.message("colopedia.buildings.produces"), library.getGoodsImageIcon(buildingProduces[building]), JLabel.LEADING);
            label.setHorizontalTextPosition(JLabel.LEADING);
            production.add(label);
        }
        overview.add(production);
        // Upkeep
        overview.add(new JLabel(Messages.message("colopedia.buildings.upkeep")));
        overview.add(new JLabel(Integer.toString(buildingUpkeep[building][level])));
        // Notes
        overview.add(new JLabel(Messages.message("colopedia.buildings.notes")));
        JTextArea notes = new JTextArea();
        notes.setBorder(null);
        notes.setOpaque(false);
        notes.setLineWrap(true);
        notes.setEditable(false);
        notes.setWrapStyleWord(true);
        notes.setFocusable(false);
        notes.setFont(requires.getFont().deriveFont(Font.BOLD));
        notes.setText(Messages.message("colopedia.buildings.notes." + buildingCalls[building][level]));
        notes.setSize(detailPanel.getWidth()/2, super.getPreferredSize().height);
        overview.add(notes);
        detailPanel.add(overview);

        JTextArea description = new JTextArea();
        description.setBorder(null);
        description.setOpaque(false);
        description.setLineWrap(true);
        description.setEditable(false);
        description.setWrapStyleWord(true);
        description.setFocusable(false);
        description.setText("");
        description.setSize(detailPanel.getWidth(), super.getPreferredSize().height);
        detailPanel.add(description);

        detailPanel.doLayout();
    }

    /**
     * Builds the details panel for the given founding father.
     * @param foundingFather
     */
    private void buildFatherDetail(int foundingFather) {
        detailPanel.removeAll();
        detailPanel.setLayout(new FlowLayout());

        JLabel name = new JLabel(Messages.message(FoundingFather.getName(foundingFather)), JLabel.CENTER);
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

        JTextArea description = new JTextArea();
        description.setBorder(null);
        description.setOpaque(false);
        description.setLineWrap(true);
        description.setEditable(false);
        description.setWrapStyleWord(true);
        description.setFocusable(false);
        description.setText(Messages.message(FoundingFather.getDescription(foundingFather)) +
                            "\n\n" + "[" + Messages.message(FoundingFather.getBirthAndDeath(foundingFather)) +
                            "] " + Messages.message(FoundingFather.getText(foundingFather)));
        description.setSize(detailPanel.getWidth(), super.getPreferredSize().height);
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
