package net.sf.freecol.client.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.panel.ImageProvider;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Resource;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;

/**
 * Holds various images that can be called upon by others in order to display
 * certain things.
 */
public final class ImageLibrary extends ImageProvider {
    private static final Logger logger = Logger.getLogger(ImageLibrary.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public static final int UNIT_SELECT = 0, PLOWED = 4, TILE_TAKEN = 5, TILE_OWNED_BY_INDIANS = 6,
            LOST_CITY_RUMOUR = 7, DARKNESS = 8, MISC_COUNT = 10;

    /**
     * These finals are for quick reference. These should be made softcoded when next possible.
     */
    public static final int TERRAIN_COUNT = 16, BONUS_COUNT = 9, GOODS_COUNT = 20, FOREST_COUNT = 9;

    /**
     * These finals represent the different parts of a tile and its
     * surroundings. Each basic tile is accompanied by several 'border' tiles
     * that are also of the same terrain type. They can be found in the same
     * directory so they need to be distinguished by these finals.
     */
    private static final int LAND_EAST = 0, // corner tile, some 'land' can be
                                            // found in the far east of this
                                            // tile
            LAND_SOUTH_EAST = 1, // border tile, some 'land' can be found in
                                    // south-east of this tile
            LAND_SOUTH = 2, // corner tile, some 'land' can be found in the far
                            // south of this tile
            LAND_SOUTH_WEST = 3, // border tile, some 'land' can be found in
                                    // south-west of this tile
            LAND_WEST = 4, // corner tile, some 'land' can be found in the far
                            // west of this tile
            LAND_NORTH_WEST = 5, // border tile, some 'land' can be found in
                                    // north-west of this tile
            LAND_NORTH = 6, // corner tile, some 'land' can be found in the far
                            // north of this tile
            LAND_NORTH_EAST = 7, // border tile, some 'land' can be found in
                                    // north-east of this tile
            LAND_CENTER = 8; // ordinary tile, filled entirely with 'land'
/*
    public static final int BONUS_NONE = -1, BONUS_SILVER = 0, BONUS_FOOD = 1, BONUS_TOBACCO = 2, BONUS_COTTON = 3,
            BONUS_SUGAR = 4, BONUS_FISH = 5, BONUS_ORE = 6, BONUS_FURS = 7, BONUS_LUMBER = 8, BONUS_COUNT = 9;
*/
    public static final int MONARCH_COUNT = 4;

    /**
     * These finals represent the unit graphics that are available. It's
     * important to note the difference between these finals and the unit type
     * finals. There are more unit graphics than unit types. Example: type:hardy
     * pioneer, corresponding graphics: hardy pioneer with tools and hardy
     * pioneer without tools.
     */
    public static final int FREE_COLONIST = 0, EXPERT_FARMER = 1, EXPERT_FISHERMAN = 2, EXPERT_FUR_TRAPPER = 3,
            EXPERT_SILVER_MINER = 4, EXPERT_LUMBER_JACK = 5, EXPERT_ORE_MINER = 6, MASTER_SUGAR_PLANTER = 7,
            MASTER_COTTON_PLANTER = 8, MASTER_TOBACCO_PLANTER = 9,

            FIREBRAND_PREACHER = 10, ELDER_STATESMAN = 11,

            MASTER_CARPENTER = 12, MASTER_DISTILLER = 13, MASTER_WEAVER = 14, MASTER_TOBACCONIST = 15,
            MASTER_FUR_TRADER = 16, MASTER_BLACKSMITH = 17, MASTER_GUNSMITH = 18,

            SEASONED_SCOUT_NOT_MOUNTED = 19, HARDY_PIONEER_NO_TOOLS = 20, UNARMED_VETERAN_SOLDIER = 21,
            JESUIT_MISSIONARY = 22, MISSIONARY_FREE_COLONIST = 23,

            SEASONED_SCOUT_MOUNTED = 24, HARDY_PIONEER_WITH_TOOLS = 25, FREE_COLONIST_WITH_TOOLS = 26,
            INDENTURED_SERVANT = 27, PETTY_CRIMINAL = 28,

            INDIAN_CONVERT = 29, BRAVE = 30,

            UNARMED_COLONIAL_REGULAR = 31, UNARMED_KINGS_REGULAR = 32,

            SOLDIER = 33, VETERAN_SOLDIER = 34, COLONIAL_REGULAR = 35, KINGS_REGULAR = 36, UNARMED_DRAGOON = 37,
            UNARMED_VETERAN_DRAGOON = 38, UNARMED_COLONIAL_CAVALRY = 39, UNARMED_KINGS_CAVALRY = 40, DRAGOON = 41,
            VETERAN_DRAGOON = 42, COLONIAL_CAVALRY = 43, KINGS_CAVALRY = 44,

            ARMED_BRAVE = 45, MOUNTED_BRAVE = 46, INDIAN_DRAGOON = 47,

            CARAVEL = 48, FRIGATE = 49, GALLEON = 50, MAN_O_WAR = 51, MERCHANTMAN = 52, PRIVATEER = 53,

            ARTILLERY = 54, DAMAGED_ARTILLERY = 55, TREASURE_TRAIN = 56, WAGON_TRAIN = 57,

            MILKMAID = 58, JESUIT_MISSIONARY_NO_CROSS = 59, REVENGER = 60, FLYING_DUTCHMAN = 61, UNDEAD = 62,

            UNIT_GRAPHICS_COUNT = 63;

    public static final int UNIT_BUTTON_WAIT = 0, UNIT_BUTTON_DONE = 1, UNIT_BUTTON_FORTIFY = 2,
            UNIT_BUTTON_SENTRY = 3, UNIT_BUTTON_CLEAR = 4, UNIT_BUTTON_PLOW = 5, UNIT_BUTTON_ROAD = 6,
            UNIT_BUTTON_BUILD = 7, UNIT_BUTTON_DISBAND = 8, UNIT_BUTTON_ZOOM_IN = 9, UNIT_BUTTON_ZOOM_OUT = 10,
            UNIT_BUTTON_COUNT = 11;

    private static final int COLONY_SMALL = 0, COLONY_MEDIUM = 1, COLONY_LARGE = 2, COLONY_STOCKADE = 3,
            COLONY_FORT = 4, COLONY_FORTRESS = 5, COLONY_MEDIUM_STOCKADE = 6, COLONY_LARGE_STOCKADE = 7,
            COLONY_LARGE_FORT = 8, COLONY_UNDEAD = 9, COLONY_COUNT = 10,

            INDIAN_SETTLEMENT_CAMP = 0,
            // INDIAN_SETTLEMENT_VILLAGE = 1,
            // INDIAN_SETTLEMENT_AZTEC = 2,
            // INDIAN_SETTLEMENT_INCA = 3,

            INDIAN_COUNT = 4;
/*
    public static final int GOODS_FOOD = 0, GOODS_SUGAR = 1, GOODS_TOBACCO = 2, GOODS_COTTON = 3, GOODS_FURS = 4,
            GOODS_LUMBER = 5, GOODS_ORE = 6, GOODS_SILVER = 7, GOODS_HORSES = 8, GOODS_RUM = 9, GOODS_CIGARS = 10,
            GOODS_CLOTH = 11, GOODS_COATS = 12, GOODS_TRADE_GOODS = 13, GOODS_TOOLS = 14, GOODS_MUSKETS = 15,
            GOODS_FISH = 16, GOODS_BELLS = 17, GOODS_CROSSES = 18, GOODS_HAMMERS = 19, GOODS_COUNT = 20;
*/
    /**
     * The filename of the graphical representation of a specific unit is the
     * following: homeDirectory + path + unitsDirectory + unitsName + UNITTYPE +
     * extension where '+' is the concatenation of Strings. and UNITTYPE is the
     * type of the unit (as in the Unit class).
     */
    private static final String path = new String("images/"), unitsDirectory = new String("units/"),
            unitsName = new String("Unit"), terrainDirectory = new String("terrain/"), terrainName = new String(
                    "terrain"), forestDirectory = new String("forest/"), forestName = new String("forest"),
            riverDirectory = new String("river/"), riverName = new String("river"),
            miscDirectory = new String("misc/"), miscName = new String("Misc"), unitButtonDirectory = new String(
                    "order-buttons/"), unitButtonName = new String("button"),
            colonyDirectory = new String("colonies/"), colonyName = new String("Colony"), indianDirectory = new String(
                    "indians/"), indianName = new String("Indians"), goodsDirectory = new String("goods/"),
            goodsName = new String("Goods"), bonusDirectory = new String("bonus/"), bonusName = new String("Bonus"),
            monarchDirectory = new String("monarch/"), monarchName = new String("Monarch"), extension = new String(
                    ".png");

    private final String dataDirectory;

    /**
     * A Vector of Image objects.
     */
    private Vector<ImageIcon> units, // Holds ImageIcon objects
            unitsGrayscale, // Holds ImageIcon objects of units in grayscale
            forests, // Holds ImageIcon objects
            rivers, // Holds ImageIcon objects
            misc, // Holds ImageIcon objects
            colonies, // Holds ImageIcon objects
            indians, // Holds ImageIcon objects
            goods, // Holds ImageIcon objects
            bonus, // Holds ImageIcon objects
            monarch; // Holds ImageIcon objects

    private Vector<Vector<ImageIcon>> terrain1, terrain2;

    private Vector<Vector<ImageIcon>> unitButtons; // Holds the unit-order
                                                    // buttons

    private Image[] alarmChips;

    private Hashtable<Color, BufferedImage> colorChips;

    private Hashtable<Color, BufferedImage> missionChips;

    private Hashtable<Color, BufferedImage> expertMissionChips;


    /**
     * The constructor to use.
     * 
     * @throws FreeColException If one of the data files could not be found.
     */
    public ImageLibrary() throws FreeColException {
        // This is the location of the data directory when running FreeCol
        // from its default location in the CVS repository.
        // dataDirectory = "";
        // init(true);
        this("");
    }

    /**
     * A constructor that takes a directory as FreeCol's home.
     * 
     * @param freeColHome The home of the freecol files.
     * @throws FreeColException If one of the data files could not be found.
     */
    public ImageLibrary(String freeColHome) throws FreeColException {
        // TODO: normally this check shouldn't be here. init(false) is the way
        // to go.
        if (freeColHome.equals("")) {
            dataDirectory = "data/";
            init(true);
        } else {
            dataDirectory = freeColHome;
            init(false);
        }

    }

    /**
     * Performs all necessary init operations such as loading of data files.
     * 
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found. *
     */
    private void init(boolean doLookup) throws FreeColException {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration();

        Class<FreeCol> resourceLocator = net.sf.freecol.FreeCol.class;

        loadUnits(gc, resourceLocator, doLookup);
        loadTerrain(gc, resourceLocator, doLookup);
        loadForests(gc, resourceLocator, doLookup);
        loadRivers(gc, resourceLocator, doLookup);
        loadMisc(gc, resourceLocator, doLookup);
        loadUnitButtons(gc, resourceLocator, doLookup);
        loadColonies(gc, resourceLocator, doLookup);
        loadIndians(gc, resourceLocator, doLookup);
        loadGoods(gc, resourceLocator, doLookup);
        loadBonus(gc, resourceLocator, doLookup);
        loadMonarch(gc, resourceLocator, doLookup);

        alarmChips = new Image[Tension.NUMBER_OF_LEVELS];
        colorChips = new Hashtable<Color, BufferedImage>();
        missionChips = new Hashtable<Color, BufferedImage>();
        expertMissionChips = new Hashtable<Color, BufferedImage>();
    }

    /**
     * Finds the image file in the given <code>filePath</code>.
     * 
     * @param filePath The path to the image file.
     * @param doLookup If <i>true</i> then the <code>resourceLocator</code>
     *            is used when searching for the image file.
     * @return An ImageIcon with data loaded from the image file.
     * @exception FreeColException If the image could not be found.
     */
    private ImageIcon findImage(String filePath, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        if (doLookup) {
            URL url = resourceLocator.getResource(filePath);
            if (url != null) {
                return new ImageIcon(url);
            }
        }

        File tmpFile = new File(filePath);
        if ((tmpFile == null) || !tmpFile.exists() || !tmpFile.isFile() || !tmpFile.canRead()) {
            throw new FreeColException("The data file \"" + filePath + "\" could not be found.");
        }

        return new ImageIcon(filePath);
    }

    /**
     * Loads the unit-images from file into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadUnits(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        units = new Vector<ImageIcon>(UNIT_GRAPHICS_COUNT);
        unitsGrayscale = new Vector<ImageIcon>(UNIT_GRAPHICS_COUNT);

        for (int i = 0; i < UNIT_GRAPHICS_COUNT; i++) {
            String filePath = dataDirectory + path + unitsDirectory + unitsName + i + extension;
            units.add(findImage(filePath, resourceLocator, doLookup));
            unitsGrayscale.add(convertToGrayscale(units.get(i).getImage()));
        }

        /*
         * If all units are patched together in one graphics file then this is
         * the way to load them into different images:
         * 
         * Image unitsImage = new ImageIcon(url).getImage(); BufferedImage
         * tempImage = gc.createCompatibleImage(42, 63,
         * Transparency.TRANSLUCENT);
         * tempImage.getGraphics().drawImage(unitsImage, 0, 0, null);
         * units.add(tempImage);
         */
    }

    /**
     * Loads the terrain-images from file into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadTerrain(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        terrain1 = new Vector<Vector<ImageIcon>>(TERRAIN_COUNT);
        terrain2 = new Vector<Vector<ImageIcon>>(TERRAIN_COUNT);

        for (int i = 0; i < TERRAIN_COUNT; i++) {
            Vector<ImageIcon> tempVector1, tempVector2;
            String numberString = String.valueOf(i);
            if (i < 10) {
                numberString = "0" + numberString;
            }

            char lastChar;
            if (i == 12) {  // Special - the beach terrain has only 8 pairs of images
                lastChar = 'h';
                tempVector1 = new Vector<ImageIcon>(8);
                tempVector2 = new Vector<ImageIcon>(8);
            } else {
                lastChar = 'i';
                tempVector1 = new Vector<ImageIcon>(9);
                tempVector2 = new Vector<ImageIcon>(9);
            }

            for (char c = 'a'; c <= lastChar; c++) {
                String filePath = dataDirectory + path + terrainDirectory + terrainName + numberString + "/"
                        + terrainName + numberString + c + "0" + extension;
                tempVector1.add(findImage(filePath, resourceLocator, doLookup));

                filePath = dataDirectory + path + terrainDirectory + terrainName + numberString + "/" + terrainName
                        + numberString + c + "1" + extension;
                tempVector2.add(findImage(filePath, resourceLocator, doLookup));
            }

            terrain1.add(tempVector1);
            terrain2.add(tempVector2);
        }
    }

    /**
     * Loads the river images from file into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadRivers(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        int combinations = 81;
        rivers = new Vector<ImageIcon>(combinations);
        for (int i = 0; i < combinations; i++) {
            String filePath = dataDirectory + path + riverDirectory + riverName + i + extension;
            rivers.add(findImage(filePath, resourceLocator, doLookup));
        }
    }

    /**
     * Loads the forest images from file into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadForests(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {

        forests = new Vector<ImageIcon>(FOREST_COUNT);
        forests.add(null);
        for (int i = 1; i < FOREST_COUNT; i++) {
            String filePath = dataDirectory + path + forestDirectory + forestName + i + extension;
            forests.add(findImage(filePath, resourceLocator, doLookup));
        }
    }

    /**
     * Loads miscellaneous images from file into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadMisc(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        misc = new Vector<ImageIcon>(MISC_COUNT);

        for (int i = 0; i < MISC_COUNT; i++) {
            String filePath = dataDirectory + path + miscDirectory + miscName + i + extension;
            misc.add(findImage(filePath, resourceLocator, doLookup));
        }
    }

    /**
     * Loads the unit-order buttons from files into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadUnitButtons(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        unitButtons = new Vector<Vector<ImageIcon>>(4);
        for (int i = 0; i < 4; i++) {
            unitButtons.add(new Vector<ImageIcon>(UNIT_BUTTON_COUNT));
        }

        for (int i = 0; i < 4; i++) {
            String subDirectory;
            switch (i) {
            case 0:
                subDirectory = new String("order-buttons00/");
                break;
            case 1:
                subDirectory = new String("order-buttons01/");
                break;
            case 2:
                subDirectory = new String("order-buttons02/");
                break;
            case 3:
                subDirectory = new String("order-buttons03/");
                break;
            default:
                subDirectory = new String("");
                break;
            }
            for (int j = 0; j < UNIT_BUTTON_COUNT; j++) {
                String filePath = dataDirectory + path + unitButtonDirectory + subDirectory + unitButtonName + j
                        + extension;
                unitButtons.get(i).add(findImage(filePath, resourceLocator, doLookup));
            }
        }
    }

    /**
     * Loads the colony pictures from files into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadColonies(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        colonies = new Vector<ImageIcon>(COLONY_COUNT);

        for (int i = 0; i < COLONY_COUNT; i++) {
            String filePath = dataDirectory + path + colonyDirectory + colonyName + i + extension;
            colonies.add(findImage(filePath, resourceLocator, doLookup));
        }
    }

    /**
     * Loads the indian settlement pictures from files into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadIndians(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        indians = new Vector<ImageIcon>(INDIAN_COUNT);

        for (int i = 0; i < INDIAN_COUNT; i++) {
            String filePath = dataDirectory + path + indianDirectory + indianName + i + extension;
            indians.add(findImage(filePath, resourceLocator, doLookup));
        }
    }

    /**
     * Loads the goods-images from file into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net/sf/freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadGoods(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        goods = new Vector<ImageIcon>(GOODS_COUNT);

        for (int i = 0; i < GOODS_COUNT; i++) {
            String filePath = dataDirectory + path + goodsDirectory + goodsName + i + extension;
            goods.add(findImage(filePath, resourceLocator, doLookup));
        }

        /*
         * If all units are patched together in one graphics file then this is
         * the way to load them into different images:
         * 
         * Image unitsImage = new ImageIcon(url).getImage(); BufferedImage
         * tempImage = gc.createCompatibleImage(42, 63,
         * Transparency.TRANSLUCENT);
         * tempImage.getGraphics().drawImage(unitsImage, 0, 0, null);
         * units.add(tempImage);
         */
    }

    /**
     * Loads the bonus-images from file into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net/sf/freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadBonus(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        bonus = new Vector<ImageIcon>(BONUS_COUNT);

        for (int i = 0; i < BONUS_COUNT; i++) {
            String filePath = dataDirectory + path + bonusDirectory + bonusName + i + extension;
            bonus.add(findImage(filePath, resourceLocator, doLookup));
        }
    }

    /**
     * Loads the monarch-images from file into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net/sf/freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadMonarch(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        monarch = new Vector<ImageIcon>(MONARCH_COUNT);

        for (int i = 0; i < MONARCH_COUNT; i++) {
            String filePath = dataDirectory + path + monarchDirectory + monarchName + i + extension;
            monarch.add(findImage(filePath, resourceLocator, doLookup));
        }
    }

    /**
     * Generates a color chip image and stores it in memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param c The color of the color chip to create.
     */
    private void loadColorChip(GraphicsConfiguration gc, Color c) {
        BufferedImage tempImage = gc.createCompatibleImage(11, 17);
        Graphics g = tempImage.getGraphics();
        if (c.equals(Color.BLACK)) {
            g.setColor(Color.WHITE);
        } else {
            g.setColor(Color.BLACK);
        }
        g.drawRect(0, 0, 10, 16);
        g.setColor(c);
        g.fillRect(1, 1, 9, 15);
        colorChips.put(c, tempImage);
    }

    /**
     * Generates a mission chip image and stores it in memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param c The color of the color chip to create.
     * @param expertMission Should be <code>true</code> if the mission chip
     *            should represent an expert missionary.
     */
    private void loadMissionChip(GraphicsConfiguration gc, Color c, boolean expertMission) {
        BufferedImage tempImage = gc.createCompatibleImage(10, 17);
        Graphics2D g = (Graphics2D) tempImage.getGraphics();

        if (expertMission) {
            g.setColor(Color.BLACK);
        } else {
            g.setColor(Color.DARK_GRAY);
        }
        g.fillRect(0, 0, 10, 17);

        GeneralPath cross = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
        cross.moveTo(4, 1);
        cross.lineTo(6, 1);
        cross.lineTo(6, 4);
        cross.lineTo(9, 4);
        cross.lineTo(9, 6);
        cross.lineTo(6, 6);
        cross.lineTo(6, 16);
        cross.lineTo(4, 16);
        cross.lineTo(4, 6);
        cross.lineTo(1, 6);
        cross.lineTo(1, 4);
        cross.lineTo(4, 4);
        cross.closePath();

        if (expertMission && c.equals(Color.BLACK)) {
            g.setColor(Color.DARK_GRAY);
        } else if ((!expertMission) && c.equals(Color.DARK_GRAY)) {
            g.setColor(Color.BLACK);
        } else {
            g.setColor(c);
        }
        g.fill(cross);

        if (expertMission) {
            expertMissionChips.put(c, tempImage);
        } else {
            missionChips.put(c, tempImage);
        }
    }

    /**
     * Generates a alarm chip image and stores it in memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param alarm The alarm level.
     */
    private void loadAlarmChip(GraphicsConfiguration gc, int alarm) {
        BufferedImage tempImage = gc.createCompatibleImage(10, 17);
        Graphics2D g = (Graphics2D) tempImage.getGraphics();

        g.setColor(Color.BLACK);
        g.drawRect(0, 0, 10, 16);

        if (alarm == Tension.HAPPY) {
            g.setColor(Color.GREEN);
        } else if (alarm == Tension.CONTENT) {
            g.setColor(Color.BLUE);
        } else if (alarm == Tension.DISPLEASED) {
            g.setColor(Color.YELLOW);
        } else if (alarm == Tension.ANGRY) {
            g.setColor(Color.ORANGE);
        } else if (alarm == Tension.HATEFUL) {
            g.setColor(Color.RED);
        } else {
            logger.warning("Unknown alarm level: " + alarm);
            return;
        }

        g.fillRect(1, 1, 8, 15);
        g.setColor(Color.BLACK);

        g.fillRect(4, 3, 2, 7);
        g.fillRect(4, 12, 2, 2);

        alarmChips[alarm] = tempImage;
    }

    /**
     * Returns the monarch-image for the given tile.
     * 
     * @param nation The nation this monarch rules.
     * @return the monarch-image for the given nation.
     */
    public Image getMonarchImage(int nation) {
        return monarch.get(nation).getImage();
    }

    /**
     * Returns the monarch-image icon for the given tile.
     * 
     * @param nation The nation this monarch rules.
     * @return the monarch-image for the given nation.
     */
    public ImageIcon getMonarchImageIcon(int nation) {
        return monarch.get(nation);
    }

    /**
     * Returns the bonus-image for the given tile.
     * 
     * @param tile
     * @return the bonus-image for the given tile.
     */
    public Image getBonusImage(Tile tile) {
        if (tile.hasResource()) {
            return getBonusImage(tile.getTileItemContainer().getResource().getType());
        } else {
            return null;
        }
    }

    public Image getBonusImage(ResourceType type) {
        return bonus.get(type.art).getImage();
    }
/*  Depreciated
    //TODO: IMPORTANT: This method duplicates a similar method in the
    //Tile class and errors were introduced because the two changed
    //independently. We really need to put the bonus information in
    //the TileType (and make hills and mountains TileTypes too).
    public static int getBonusImageType(int type, int addition, boolean forested) {
        if (addition == Tile.ADD_MOUNTAINS) {
            return BONUS_SILVER;
        } else if (addition == Tile.ADD_HILLS) {
            return BONUS_ORE;
        } else if (forested) {
            if (type == Tile.GRASSLANDS || type == Tile.SAVANNAH) {
                return BONUS_LUMBER;
            } else if (type == Tile.MARSH || type == Tile.SWAMP) {
                return BONUS_ORE;
            } else {
                return BONUS_FURS;
            }
        } else {
            switch(type) {
                case Tile.UNEXPLORED:
                    return -1;
                case Tile.PLAINS:
                    return BONUS_FOOD;
                case Tile.GRASSLANDS:
                    return BONUS_TOBACCO;
                case Tile.PRAIRIE:
                    return BONUS_COTTON;
                case Tile.SAVANNAH:
                    return BONUS_SUGAR;
                case Tile.MARSH:
                    return BONUS_ORE;
                case Tile.SWAMP:
                    return BONUS_SILVER;
                case Tile.DESERT:
                    return BONUS_FOOD;
                case Tile.TUNDRA:
                    return BONUS_ORE;                    
                case Tile.ARCTIC:
                    return -1;
                case Tile.OCEAN:
                    return BONUS_FISH;
                case Tile.HIGH_SEAS:
                    return -1;
                default:
                    // Should never happen
                    throw new IllegalArgumentException("Unknown tile type " + type + " for getBonusType!");
            }
        }
    }
*/
    /**
     * Returns the bonus-ImageIcon at the given index.
     * 
     * @param index The index of the bonus-ImageIcon to return.
     * @return The bonus-ImageIcon at the given index.
     */
    public ImageIcon getBonusImageIcon(int index) {
        return bonus.get(index);
    }

    public ImageIcon getBonusImageIcon(ResourceType type) {
        return bonus.get(type.art);
    }

    /**
     * Converts an image to grayscale
     * 
     * @param image Source image to convert
     * @return The image in grayscale
     */
    private ImageIcon convertToGrayscale(Image image) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);

        ColorConvertOp filter = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        BufferedImage srcImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        srcImage.createGraphics().drawImage(image, 0, 0, null);
        return new ImageIcon(filter.filter(srcImage, null));
    }

    /**
     * Returns the unit-image at the given index.
     * 
     * @param index The index of the unit-image to return.
     * @return The unit-image at the given index.
     */
    public Image getUnitImage(int index) {
        return getUnitImage(index, false);
    }

    /**
     * Returns the unit-image at the given index.
     * 
     * @param index The index of the unit-image to return.
     * @param grayscale If <code>true</code> return the image in grayscale
     * @return The unit-image at the given index.
     */
    public Image getUnitImage(int index, boolean grayscale) {
        return getUnitImageIcon(index, grayscale).getImage();
    }

    /**
     * Returns the unit-ImageIcon at the given index.
     * 
     * @param index The index of the unit-ImageIcon to return.
     * @return The unit-ImageIcon at the given index.
     */
    public ImageIcon getUnitImageIcon(int index) {
        return getUnitImageIcon(index, false);
    }

    /**
     * Returns the unit-ImageIcon at the given index.
     * 
     * @param index The index of the unit-ImageIcon to return.
     * @return The unit-ImageIcon at the given index.
     */
    public ImageIcon getUnitImageIcon(int index, boolean grayscale) {
        if (grayscale)
            return unitsGrayscale.get(index);
        else
            return units.get(index);
    }

    /**
     * Returns the scaled unit-ImageIcon at the given index.
     * 
     * @param index The index of the unit-ImageIcon to return.
     * @param scale The scale of the unit-ImageIcon to return.
     * @return The unit-ImageIcon at the given index.
     */
    public ImageIcon getScaledUnitImageIcon(int index, float scale) {
        if (index >= 0) {
            ImageIcon icon = getUnitImageIcon(index);
            if (scale != 1) {
                Image image;
                image = icon.getImage();
                int width = (int) (scale * image.getWidth(null));
                int height = (int) (scale * image.getHeight(null));
                image = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                icon = new ImageIcon(image);
            }
            return icon;
        }
        return null;
    }
    
    /**
     * Returns the Colopedia-specific scaled unit-ImageIcon at the given index.
     * 
     * @param index The index of the unit-ImageIcon to return.
     * @param scale The scale of the unit-ImageIcon to return.
     * @return The unit-ImageIcon at the given index.
     */
    public ImageIcon getColopediaUnitImageIcon(int index, float scale) {
        if (index >= 0) {
            ImageIcon icon = getUnitImageIcon(index);
            if (scale != 1) {
                Image image;
                image = icon.getImage();
                int width = (int) (scale * image.getWidth(null));
                if (width > 30) {
                    width = 30;
                }
                int height = (int) (scale * image.getHeight(null));
                if (height > 30) {
                    height = 30;
                }
                image = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                icon = new ImageIcon(image);
            }
            return icon;
        }
        return null;
    }

    /**
     * Returns the scaled terrain-image at the given index (and position 0, 0).
     * 
     * @param index The index of the terrain-image to return.
     * @param forested Whether the image should be forested.
     * @param scale The scale of the terrain image to return.
     * @return The terrain-image at the given index.
     */
    public Image getScaledTerrainImage(TileType type, float scale) {
        // Index used for drawing the base is the artBasic value
        int index = type.artBasic;
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration();
        Image terrainImage = getTerrainImage(index, 0, 0);
        int width = getTerrainImageWidth(index);
        int height = getTerrainImageHeight(index);
        // Currently used for hills and mountains
        if (type.artOverlay > -1) {
            BufferedImage compositeImage = gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
            Graphics2D g = compositeImage.createGraphics();
            g.drawImage(terrainImage, 0, 0, null);
            g.drawImage(getTerrainImage(type.artOverlay, 0, 0), 0, 0);
            g.dispose();
            terrainImage = compositeImage;
        }
        if (type.isForested()) {
            BufferedImage compositeImage = gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
            Graphics2D g = compositeImage.createGraphics();
            g.drawImage(terrainImage, 0, 0, null);
            g.drawImage(getForestImage(type.artForest), 0, 0, null);
            g.dispose();
            terrainImage = compositeImage;
        }
        if (scale == 1f) {
            return terrainImage;
        } else {
            return terrainImage.getScaledInstance((int) (width * scale), (int) (height * scale), Image.SCALE_SMOOTH);
        }
    }

    /**
     * Returns the terrain-image at the given index.
     * 
     * @param index The index of the terrain-image to return.
     * @param x The x-coordinate of the location of the tile that is being
     *            drawn.
     * @param y The x-coordinate of the location of the tile that is being
     *            drawn.
     * @return The terrain-image at the given index.
     */
    public Image getTerrainImage(int index, int x, int y) {
        return getTerrainImage(index, LAND_CENTER, x, y);
    }

    /**
     * Returns the terrain-image at the given index with the specified type.
     * 
     * @param index The index of the terrain-image to return.
     * @param borderType The border type.
     * @param x The x-coordinate of the location of the tile that is being
     *            drawn.
     * @param y The x-coordinate of the location of the tile that is being
     *            drawn.
     * @return The terrain-image at the given index.
     */
    public Image getTerrainImage(int index, int borderType, int x, int y) {

        final int[] directionsAndImageLibOffsets = new int[] { ImageLibrary.LAND_NORTH, ImageLibrary.LAND_NORTH_EAST,
                ImageLibrary.LAND_EAST, ImageLibrary.LAND_SOUTH_EAST, ImageLibrary.LAND_SOUTH,
                ImageLibrary.LAND_SOUTH_WEST, ImageLibrary.LAND_WEST, ImageLibrary.LAND_NORTH_WEST,
                ImageLibrary.LAND_CENTER };

        borderType = directionsAndImageLibOffsets[borderType];

        if ((x + y) % 2 == 0) {
            return terrain1.get(index).get(borderType).getImage();
        } else {
            return terrain2.get(index).get(borderType).getImage();
        }
    }

    /**
     * Returns the river image at the given index.
     * 
     * @param index The index of the image to return.
     * @return The image at the given index.
     */
    public Image getRiverImage(int index) {
        return rivers.get(index).getImage();
    }

    /**
     * Returns the forest image at the given index.
     * 
     * @param index The index of the image to return.
     * @return The image at the given index.
     */
    public Image getForestImage(int index) {
        return forests.get(index).getImage();
    }

    /**
     * Returns the image at the given index.
     * 
     * @param index The index of the image to return.
     * @return The image at the given index.
     */
    public Image getMiscImage(int index) {
        return misc.get(index).getImage();
    }

    /**
     * Returns the image at the given index.
     * 
     * @param index The index of the image to return.
     * @return The image at the given index.
     */
    public ImageIcon getMiscImageIcon(int index) {
        return misc.get(index);
    }

    /**
     * Returns the unit-button image at the given index in the given state.
     * 
     * @param index The index of the image to return.
     * @param state The state (normal, highlighted, pressed, disabled)
     * @return The image pointer
     */
    public ImageIcon getUnitButtonImageIcon(int index, int state) {
        return unitButtons.get(state).get(index);
    }

    /**
     * Returns the indian settlement image at the given index.
     * 
     * @param index The index of the image to return.
     * @return The image pointer
     */
    public Image getIndianSettlementImage(int index) {
        return indians.get(index).getImage();
    }

    /**
     * Returns the goods-image at the given index.
     * 
     * @param index The index of the goods-image to return.
     * @return The goods-image at the given index.
     */
    public Image getGoodsImage(int index) {
        return goods.get(index).getImage();
    }
    public Image getGoodsImage(GoodsType g) {
        return goods.get(g.index).getImage();
    }

    /**
     * Returns the goods-image at the given index.
     * 
     * @param index The index of the goods-image to return.
     * @return The goods-image at the given index.
     */
    public ImageIcon getGoodsImageIcon(int index) {
        return goods.get(index);
    }
    public ImageIcon getGoodsImageIcon(GoodsType g) {
        return goods.get(g.index);
    }

    /**
     * Returns the scaled goods-ImageIcon at the given index.
     * 
     * @param index The index of the goods-ImageIcon to return.
     * @param scale The scale of the goods-ImageIcon to return.
     * @return The goods-ImageIcon at the given index.
     */
    public ImageIcon getScaledGoodsImageIcon(int index, float scale) {
        if (index >= 0) {
            ImageIcon icon = getGoodsImageIcon(index);
            if (scale != 1) {
                Image image;
                image = icon.getImage();
                int width = (int) (scale * image.getWidth(null));
                int height = (int) (scale * image.getHeight(null));
                image = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                icon = new ImageIcon(image);
            }
            return icon;
        }
        return null;
    }
    public ImageIcon getScaledGoodsImageIcon(GoodsType g, float scale) {
        return getScaledGoodsImageIcon(g.index, scale);
    }

    /**
     * Returns the colony image at the given index.
     * 
     * @param index The index of the image to return.
     * @return The image pointer
     */
    public Image getColonyImage(int index) {
        return colonies.get(index).getImage();
    }

    /**
     * Returns the color chip with the given color.
     * 
     * @param color The color of the color chip to return.
     * @return The color chip with the given color.
     */
    public Image getColorChip(Color color) {
        Image colorChip = colorChips.get(color);
        if (colorChip == null) {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration();
            loadColorChip(gc, color);
            colorChip = colorChips.get(color);
        }
        return colorChip;
    }

    /**
     * Returns the mission chip with the given color.
     * 
     * @param color The color of the color chip to return.
     * @param expertMission Indicates whether or not the missionary is an
     *            expert.
     * @return The color chip with the given color.
     */
    public Image getMissionChip(Color color, boolean expertMission) {
        Image missionChip;
        if (expertMission) {
            missionChip = expertMissionChips.get(color);
        } else {
            missionChip = missionChips.get(color);
        }

        if (missionChip == null) {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration();
            loadMissionChip(gc, color, expertMission);

            if (expertMission) {
                missionChip = expertMissionChips.get(color);
            } else {
                missionChip = missionChips.get(color);
            }
        }
        return missionChip;
    }

    /**
     * Returns the alarm chip with the given color.
     * 
     * @param alarm The alarm level.
     * @return The alarm chip.
     */
    public Image getAlarmChip(int alarm) {
        Image alarmChip = alarmChips[alarm];

        if (alarmChip == null) {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration();
            loadAlarmChip(gc, alarm);
            alarmChip = alarmChips[alarm];
        }
        return alarmChip;
    }

    /**
     * Returns the width of the terrain-image at the given index.
     * 
     * @param index The index of the terrain-image.
     * @return The width of the terrain-image at the given index.
     */
    public int getTerrainImageWidth(int index) {
        return terrain1.get(index).get(LAND_CENTER).getIconWidth();
    }
    public int getTerrainImageWidth(TileType type) {
        return terrain1.get(type.index).get(LAND_CENTER).getIconWidth();
    }

    /**
     * Returns the height of the terrain-image at the given index.
     * 
     * @param index The index of the terrain-image.
     * @return The height of the terrain-image at the given index.
     */
    public int getTerrainImageHeight(int index) {
        return terrain1.get(index).get(LAND_CENTER).getIconHeight();
    }
    public int getTerrainImageHeight(TileType type) {
        return terrain1.get(type.index).get(LAND_CENTER).getIconHeight();
    }

    /**
     * Returns the width of the Colony-image at the given index.
     * 
     * @param index The index of the Colony-image.
     * @return The width of the Colony-image at the given index.
     */
    public int getColonyImageWidth(int index) {
        return colonies.get(index).getIconWidth();
    }

    /**
     * Returns the height of the Colony-image at the given index.
     * 
     * @param index The index of the Colony-image.
     * @return The height of the Colony-image at the given index.
     */
    public int getColonyImageHeight(int index) {
        return colonies.get(index).getIconHeight();
    }

    /**
     * Returns the width of the IndianSettlement-image at the given index.
     * 
     * @param index The index of the IndianSettlement-image.
     * @return The width of the IndianSettlement-image at the given index.
     */
    public int getIndianSettlementImageWidth(int index) {
        return indians.get(index).getIconWidth();
    }

    /**
     * Returns the height of the IndianSettlement-image at the given index.
     * 
     * @param index The index of the IndianSettlement-image.
     * @return The height of the IndianSettlement-image at the given index.
     */
    public int getIndianSettlementImageHeight(int index) {
        return indians.get(index).getIconHeight();
    }

    /**
     * Returns the width of the unit-image at the given index.
     * 
     * @param index The index of the unit-image.
     * @return The width of the unit-image at the given index.
     */
    public int getUnitImageWidth(int index) {
        return units.get(index).getIconWidth();
    }

    /**
     * Returns the height of the unit-image at the given index.
     * 
     * @param index The index of the unit-image.
     * @return The height of the unit-image at the given index.
     */
    public int getUnitImageHeight(int index) {
        return units.get(index).getIconHeight();
    }

    /**
     * Returns the graphics that will represent the given settlement.
     * 
     * @param settlement The settlement whose graphics type is needed.
     * @return The graphics that will represent the given settlement.
     */
    public int getSettlementGraphicsType(Settlement settlement) {

        if (settlement instanceof Colony) {
            Colony colony = (Colony) settlement;

            int stockadeLevel = colony.getBuilding(Building.STOCKADE).getLevel();

            if (colony.isUndead()) {
                return COLONY_UNDEAD;
            } else if (stockadeLevel == Building.HOUSE) {
                if (colony.getUnitCount() > 7) {
                    return COLONY_LARGE_STOCKADE;
                } else if (colony.getUnitCount() > 3) {
                    return COLONY_MEDIUM_STOCKADE;
                } else {
                    return COLONY_STOCKADE;
                }
            } else if (stockadeLevel == Building.SHOP) {
                if (colony.getUnitCount() > 7) {
                    return COLONY_LARGE_FORT;
                } else {
                    return COLONY_FORT;
                }
            } else if (stockadeLevel == Building.FACTORY) {
                return COLONY_FORTRESS;
            } else if (colony.getUnitCount() <= 3) {
                return COLONY_SMALL;
            } else if (colony.getUnitCount() <= 7) {
                return COLONY_MEDIUM;
            } else {
                return COLONY_LARGE;
            }

        } else { // IndianSettlement
            return INDIAN_SETTLEMENT_CAMP;

            /*
             * TODO: Use when we have graphics: IndianSettlement
             * indianSettlement = (IndianSettlement) settlement; if
             * (indianSettlement.getKind() == IndianSettlement.CAMP) { return
             * INDIAN_SETTLEMENT_CAMP; } else if (indianSettlement.getKind() ==
             * IndianSettlement.VILLAGE) { return INDIAN_SETTLEMENT_VILLAGE; }
             * else { //CITY if (indianSettlement.getTribe() ==
             * IndianSettlement.AZTEC) return INDIAN_SETTLEMENT_AZTEC; else //
             * INCA return INDIAN_SETTLEMENT_INCA; }
             */
        }
    }

    /**
     * Returns the graphics that will represent the given unit.
     * 
     * @param unit The unit whose graphics type is needed.
     * @return The graphics that will represent the given unit.
     */
    public int getUnitGraphicsType(Unit unit) {
        return getUnitGraphicsType(unit.getType(), unit.isArmed(), unit.isMounted(), unit.getNumberOfTools(), unit
                .isMissionary());
    }

    /**
     * Returns the graphics that will represent the given unit.
     * 
     * @param type The type of unit whose graphics type is needed.
     * @param armed Whether the unit is armed.
     * @param mounted Whether the unit is mounted.
     * @param numberOfTools The number of tools this unit has.
     * @param missionary Whether the unit is missionary.
     * @return The graphics that will represent the given unit.
     */
    public static int getUnitGraphicsType(int type, boolean armed, boolean mounted, int numberOfTools,
            boolean missionary) {
        switch (type) {
        case Unit.FREE_COLONIST:
        case Unit.EXPERT_FARMER:
        case Unit.EXPERT_FISHERMAN:
        case Unit.EXPERT_FUR_TRAPPER:
        case Unit.EXPERT_SILVER_MINER:
        case Unit.EXPERT_LUMBER_JACK:
        case Unit.EXPERT_ORE_MINER:
        case Unit.MASTER_SUGAR_PLANTER:
        case Unit.MASTER_COTTON_PLANTER:
        case Unit.MASTER_TOBACCO_PLANTER:
        case Unit.FIREBRAND_PREACHER:
        case Unit.ELDER_STATESMAN:
        case Unit.MASTER_CARPENTER:
        case Unit.MASTER_DISTILLER:
        case Unit.MASTER_WEAVER:
        case Unit.MASTER_TOBACCONIST:
        case Unit.MASTER_FUR_TRADER:
        case Unit.MASTER_BLACKSMITH:
        case Unit.MASTER_GUNSMITH:
            if (armed && mounted) {
                return DRAGOON;
            } else if (armed) {
                return SOLDIER;
            } else if (mounted) {
                return UNARMED_DRAGOON;
            } else if (numberOfTools > 0) {
                return FREE_COLONIST_WITH_TOOLS;
            } else if (missionary) {
                return MISSIONARY_FREE_COLONIST;
            } else {
                // The ints representing the types are exactly the same
                // as the ints representing the graphics. This is only the
                // case for these units and it may change at ANY time.
                return type;
            }
        case Unit.SEASONED_SCOUT:
            if (armed && mounted) {
                return DRAGOON;
            } else if (armed) {
                return SOLDIER;
            } else if (mounted) {
                return SEASONED_SCOUT_MOUNTED;
            } else if (numberOfTools > 0) {
                return FREE_COLONIST_WITH_TOOLS;
            } else if (missionary) {
                return MISSIONARY_FREE_COLONIST;
            } else {
                return SEASONED_SCOUT_NOT_MOUNTED;
            }
        case Unit.HARDY_PIONEER:
            if (armed && mounted) {
                return DRAGOON;
            } else if (armed) {
                return SOLDIER;
            } else if (mounted) {
                return UNARMED_DRAGOON;
            } else if (numberOfTools > 0) {
                return HARDY_PIONEER_WITH_TOOLS;
            } else if (missionary) {
                return MISSIONARY_FREE_COLONIST;
            } else {
                return HARDY_PIONEER_NO_TOOLS;
            }
        case Unit.VETERAN_SOLDIER:
            if (armed && mounted) {
                return VETERAN_DRAGOON;
            } else if (armed) {
                return VETERAN_SOLDIER;
            } else if (mounted) {
                return UNARMED_VETERAN_DRAGOON;
            } else if (numberOfTools > 0) {
                return FREE_COLONIST_WITH_TOOLS;
            } else if (missionary) {
                return MISSIONARY_FREE_COLONIST;
            } else {
                return UNARMED_VETERAN_SOLDIER;
            }
        case Unit.JESUIT_MISSIONARY:
            if (armed && mounted) {
                return DRAGOON;
            } else if (armed) {
                return SOLDIER;
            } else if (mounted) {
                return UNARMED_DRAGOON;
            } else if (numberOfTools > 0) {
                return FREE_COLONIST_WITH_TOOLS;
            } else if (missionary) {
                return JESUIT_MISSIONARY;
            } else {
                return JESUIT_MISSIONARY_NO_CROSS;
            }
        case Unit.INDENTURED_SERVANT:
            if (armed && mounted) {
                return DRAGOON;
            } else if (armed) {
                return SOLDIER;
            } else if (mounted) {
                return UNARMED_DRAGOON;
            } else if (numberOfTools > 0) {
                return FREE_COLONIST_WITH_TOOLS;
            } else if (missionary) {
                return MISSIONARY_FREE_COLONIST;
            } else {
                return INDENTURED_SERVANT;
            }
        case Unit.PETTY_CRIMINAL:
            if (armed && mounted) {
                return DRAGOON;
            } else if (armed) {
                return SOLDIER;
            } else if (mounted) {
                return UNARMED_DRAGOON;
            } else if (numberOfTools > 0) {
                return FREE_COLONIST_WITH_TOOLS;
            } else if (missionary) {
                return MISSIONARY_FREE_COLONIST;
            } else {
                return PETTY_CRIMINAL;
            }
        case Unit.INDIAN_CONVERT:
            if (armed && mounted) {
                return DRAGOON;
            } else if (armed) {
                return SOLDIER;
            } else if (mounted) {
                return UNARMED_DRAGOON;
            } else if (numberOfTools > 0) {
                return FREE_COLONIST_WITH_TOOLS;
            } else if (missionary) {
                return MISSIONARY_FREE_COLONIST;
            } else {
                return INDIAN_CONVERT;
            }
        case Unit.BRAVE:
            if (armed && mounted) {
                return INDIAN_DRAGOON;
            } else if (armed) {
                return ARMED_BRAVE;
            } else if (mounted) {
                return MOUNTED_BRAVE;
            } else {
                return BRAVE;
            }
        case Unit.COLONIAL_REGULAR:
            if (armed && mounted) {
                return COLONIAL_CAVALRY;
            } else if (armed) {
                return COLONIAL_REGULAR;
            } else if (mounted) {
                return UNARMED_COLONIAL_CAVALRY;
            } else if (numberOfTools > 0) {
                return FREE_COLONIST_WITH_TOOLS;
            } else if (missionary) {
                return MISSIONARY_FREE_COLONIST;
            } else {
                return UNARMED_COLONIAL_REGULAR;
            }
        case Unit.KINGS_REGULAR:
            if (armed && mounted) {
                return KINGS_CAVALRY;
            } else if (armed) {
                return KINGS_REGULAR;
            } else if (mounted) {
                return UNARMED_KINGS_CAVALRY;
            } else {
                return UNARMED_KINGS_REGULAR;
            }
        case Unit.CARAVEL:
            return CARAVEL;
        case Unit.FRIGATE:
            return FRIGATE;
        case Unit.GALLEON:
            return GALLEON;
        case Unit.MAN_O_WAR:
            return MAN_O_WAR;
        case Unit.MERCHANTMAN:
            return MERCHANTMAN;
        case Unit.PRIVATEER:
            return PRIVATEER;
        case Unit.ARTILLERY:
            return ARTILLERY;
        case Unit.DAMAGED_ARTILLERY:
            return DAMAGED_ARTILLERY;
        case Unit.TREASURE_TRAIN:
            return TREASURE_TRAIN;
        case Unit.WAGON_TRAIN:
            return WAGON_TRAIN;
        case Unit.MILKMAID:
            return MILKMAID;
        case Unit.REVENGER:
            return REVENGER;
        case Unit.FLYING_DUTCHMAN:
            return FLYING_DUTCHMAN;
        case Unit.UNDEAD:
            return UNDEAD;
        default:
            // This case can NOT occur.
            return -1;
        }
    }
}
