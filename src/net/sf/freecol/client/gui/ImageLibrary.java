package net.sf.freecol.client.gui;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.ImageIcon;

import net.sf.freecol.common.model.*;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.client.gui.panel.ImageProvider;


/**
 * Holds various images that can be called upon by others in order to display
 * certain things.
 */
public final class ImageLibrary extends ImageProvider {
    public static final String COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String REVISION = "$Revision$";

    public static final int UNIT_SELECT = 0,
                            PLOWED = 4,
                            TILE_TAKEN = 5,
                            MISC_COUNT = 6;

    /**
     * These finals represent the EXTRA terrain graphics; the ones that
     * can not be found in the Tile class.
     * These finals together with the ones from Tile make up all the
     * different types of terrain graphics.
     */
    public static final int BEACH = 12,
                            FOREST = 13,
                            HILLS = 14,
                            MOUNTAINS = 15,
                            TERRAIN_COUNT = 16;

    /**
     * These finals represent the different parts of a tile and its
     * surroundings. Each basic tile is accompanied by several 'border'
     * tiles that are also of the same terrain type. They can be found in
     * the same directory so they need to be distinguished by these finals.
     */
    private static final int LAND_EAST = 0,          // corner tile, some 'land' can be found in the far east of this tile
                            LAND_SOUTH_EAST = 1,    // border tile, some 'land' can be found in south-east of this tile
                            LAND_SOUTH = 2,         // corner tile, some 'land' can be found in the far south of this tile
                            LAND_SOUTH_WEST = 3,    // border tile, some 'land' can be found in south-west of this tile
                            LAND_WEST = 4,          // corner tile, some 'land' can be found in the far west of this tile
                            LAND_NORTH_WEST = 5,    // border tile, some 'land' can be found in north-west of this tile
                            LAND_NORTH = 6,         // corner tile, some 'land' can be found in the far north of this tile
                            LAND_NORTH_EAST = 7,    // border tile, some 'land' can be found in north-east of this tile
                            LAND_CENTER = 8;        // ordinary tile, filled entirely with 'land'

    /**
     * These finals represent the unit graphics that are available.
     * It's important to note the difference between these finals and
     * the unit type finals. There are more unit graphics than unit
     * types. Example: type:hardy pioneer, corresponding graphics:
     * hardy pioneer with tools and hardy pioneer without tools.
     */
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

    private static final int UNIT_BUTTON_WAIT = 0,
                            UNIT_BUTTON_DONE = 1,
                            UNIT_BUTTON_FORTIFY = 2,
                            UNIT_BUTTON_SENTRY = 3,
                            UNIT_BUTTON_CLEAR = 4,
                            UNIT_BUTTON_PLOW = 5,
                            UNIT_BUTTON_ROAD = 6,
                            UNIT_BUTTON_BUILD = 7,
                            UNIT_BUTTON_DISBAND = 8,
                            UNIT_BUTTON_COUNT = 9;

    private static final int COLONY_SMALL = 0,
                            COLONY_MEDIUM = 1,
                            COLONY_LARGE = 2,
                            COLONY_STOCKADE = 3,
                            COLONY_FORT = 4,
                            COLONY_FORTRESS = 5,

                            COLONY_COUNT = 6,

                            INDIAN_SETTLEMENT_CAMP = 0,
                            INDIAN_SETTLEMENT_VILLAGE = 1,
                            INDIAN_SETTLEMENT_AZTEC = 2,
                            INDIAN_SETTLEMENT_INCA = 3,

                            INDIAN_COUNT = 4;

    public static final int GOODS_FOOD = 0,
                            GOODS_SUGAR = 1,
                            GOODS_TOBACCO = 2,
                            GOODS_COTTON = 3,
                            GOODS_FURS = 4,
                            GOODS_LUMBER = 5,
                            GOODS_ORE = 6,
                            GOODS_SILVER = 7,
                            GOODS_HORSES = 8,
                            GOODS_RUM = 9,
                            GOODS_CIGARS = 10,
                            GOODS_CLOTH = 11,
                            GOODS_COATS = 12,
                            GOODS_TRADE_GOODS = 13,
                            GOODS_TOOLS = 14,
                            GOODS_MUSKETS = 15,
                            GOODS_FISH = 16,
                            GOODS_BELLS = 17,
                            GOODS_CROSSES = 18,
                            GOODS_HAMMERS = 19,
                            GOODS_COUNT = 20;

    /**
     * The filename of the graphical representation of a specific unit is the following:
     * homeDirectory + path + unitsDirectory + unitsName + UNITTYPE + extension
     * where '+' is the concatenation of Strings. and UNITTYPE is the type of the unit (as in the Unit class).
     */
    private static final String path = new String("images/"),
                                unitsDirectory = new String("units/"),
                                unitsName = new String("Unit"),
                                terrainDirectory = new String("terrain/"),
                                terrainName = new String("terrain"),
                                miscDirectory = new String("misc/"),
                                miscName = new String("Misc"),
                                unitButtonDirectory = new String("order-buttons/"),
                                unitButtonName = new String("button"),
                                colonyDirectory = new String("colonies/"),
                                colonyName = new String("Colony"),
                                indianDirectory = new String("indians/"),
                                indianName = new String("Indians"),
                                goodsDirectory = new String("goods/"),
                                goodsName = new String("Goods"),
                                extension = new String(".png");
    private final String dataDirectory;

    /**
     * A Vector of Image objects.
     */
    private Vector units, // Holds ImageIcon objects
                   terrain1, // Holds Vectors that hold ImageIcon objects
                   terrain2, // Holds Vectors that hold ImageIcon objects
                   misc, // Holds ImageIcon objects
                   colonies, //Holds ImageIcon objects
                   indians, //Holds ImageIcon objects
                   goods; //Holds ImageIcon objects
    private Vector[] unitButtons; //Holds the unit-order buttons
    private Hashtable colorChips;  // Color is key, BufferedImage is value

    /**
     * The constructor to use.
     * @throws FreeColException If one of the data files could not be found.
     */
    public ImageLibrary() throws FreeColException {
        // This is the location of the data directory when running FreeCol
        // from its default location in the CVS repository.
        //dataDirectory = "";
        //init(true);
        this("");
    }

    /**
     * A constructor that takes a directory as FreeCol's home.
     * @param freeColHome The home of the freecol files.
     * @throws FreeColException If one of the data files could not be found.
     */
    public ImageLibrary(String freeColHome) throws FreeColException {
        // TODO: normally this check shouldn't be here. init(false) is the way to go.
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
     * @throws FreeColException If one of the data files could not be found.
     * @param doLookup Must be set to 'false' if the path to the image files
     * has been manually provided by the user. If set to 'true' then a
     * lookup will be done to search for image files from net.sf.freecol,
     * in this case the images need to be placed in net.sf.freecol/images.
     */
    private void init(boolean doLookup) throws FreeColException {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

        Class resourceLocator = net.sf.freecol.FreeCol.class;

        loadUnits(gc, resourceLocator, doLookup);
        loadTerrain(gc, resourceLocator, doLookup);
        loadMisc(gc, resourceLocator, doLookup);
        loadUnitButtons(gc, resourceLocator, doLookup);
        loadColonies(gc, resourceLocator, doLookup);
        loadIndians(gc, resourceLocator, doLookup);
        loadGoods(gc, resourceLocator, doLookup);
        loadColorChips(gc);
    }

    /**
    * Finds the image file in the given <code>filePath</code>.
    *
    * @doLookup If <i>true</i> then the <code>resourceLocator</code>
    *           is used when searching for the image file.
    * @return An ImageIcon with data loaded from the image file.
    */
    private ImageIcon findImage(String filePath, Class resourceLocator, boolean doLookup) throws FreeColException {
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
     * @param gc The GraphicsConfiguration is needed to create images that are compatible with the
     * local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files
     * has been manually provided by the user. If set to 'true' then a
     * lookup will be done to search for image files from net.sf.freecol,
     * in this case the images need to be placed in net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadUnits(GraphicsConfiguration gc, Class resourceLocator, boolean doLookup) throws FreeColException {
        units = new Vector(UNIT_GRAPHICS_COUNT);

        for (int i = 0; i < UNIT_GRAPHICS_COUNT; i++) {
            String filePath = dataDirectory + path + unitsDirectory + unitsName + i + extension;
            units.add(findImage(filePath, resourceLocator, doLookup));
        }

        /*
        If all units are patched together in one graphics file then this is the way to load
        them into different images:

        Image unitsImage = new ImageIcon(url).getImage();
        BufferedImage tempImage = gc.createCompatibleImage(42, 63, Transparency.TRANSLUCENT);
        tempImage.getGraphics().drawImage(unitsImage, 0, 0, null);
        units.add(tempImage);
        */
    }

    /**
     * Loads the terrain-images from file into memory.
     * @param gc The GraphicsConfiguration is needed to create images that are compatible with the
     * local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files
     * has been manually provided by the user. If set to 'true' then a
     * lookup will be done to search for image files from net.sf.freecol,
     * in this case the images need to be placed in net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadTerrain(GraphicsConfiguration gc, Class resourceLocator, boolean doLookup) throws FreeColException {
        terrain1 = new Vector(TERRAIN_COUNT);
        terrain2 = new Vector(TERRAIN_COUNT);

        for (int i = 0; i < TERRAIN_COUNT; i++) {
            Vector tempVector1,
                tempVector2;
            String numberString = String.valueOf(i);
            if (i < 10) {
                numberString = "0" + numberString;
            }

            char lastChar;
            if (i == BEACH) {
                lastChar = 'h';
                tempVector1 = new Vector(8);
                tempVector2 = new Vector(8);
            } else {
                lastChar = 'i';
                tempVector1 = new Vector(9);
                tempVector2 = new Vector(9);
            }

            for (char c = 'a'; c <= lastChar; c++) {
                String filePath = dataDirectory + path + terrainDirectory + terrainName + numberString + "/"
                    + terrainName + numberString + c + "0" + extension;
                tempVector1.add(findImage(filePath, resourceLocator, doLookup));

                filePath = dataDirectory + path + terrainDirectory + terrainName + numberString + "/"
                    + terrainName + numberString + c + "1" + extension;
                tempVector2.add(findImage(filePath, resourceLocator, doLookup));
            }

            terrain1.add(tempVector1);
            terrain2.add(tempVector2);
        }
    }

    /**
     * Loads miscellaneous images from file into memory.
     * @param gc The GraphicsConfiguration is needed to create images that are compatible with the
     * local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files
     * has been manually provided by the user. If set to 'true' then a
     * lookup will be done to search for image files from net.sf.freecol,
     * in this case the images need to be placed in net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadMisc(GraphicsConfiguration gc, Class resourceLocator, boolean doLookup) throws FreeColException {
        misc = new Vector(MISC_COUNT);

        for (int i = 0; i < MISC_COUNT; i++) {
            String filePath = dataDirectory + path + miscDirectory + miscName + i + extension;
            misc.add(findImage(filePath, resourceLocator, doLookup));
        }
    }

    /**
     * Loads the unit-order buttons from files into memory.
     * @param gc The GraphicsConfiguration is needed to create images that are compatible with the
     * local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files
     * has been manually provided by the user. If set to 'true' then a
     * lookup will be done to search for image files from net.sf.freecol,
     * in this case the images need to be placed in net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadUnitButtons(GraphicsConfiguration gc, Class resourceLocator, boolean doLookup) throws FreeColException {
        unitButtons = new Vector[4];
        for(int i = 0; i < 4; i++) {
            unitButtons[i] = new Vector(UNIT_BUTTON_COUNT);
        }

        for(int i = 0; i < 4; i++) {
            String subDirectory;
            switch(i) {
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
            for(int j = 0; j < UNIT_BUTTON_COUNT; j++) {
                String filePath = dataDirectory + path + unitButtonDirectory + subDirectory + unitButtonName + j + extension;
                unitButtons[i].add(findImage(filePath, resourceLocator, doLookup));
            }
        }
    }

    /**
     * Loads the colony pictures from files into memory.
     * @param gc The GraphicsConfiguration is needed to create images that are compatible with the
     * local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files
     * has been manually provided by the user. If set to 'true' then a
     * lookup will be done to search for image files from net.sf.freecol,
     * in this case the images need to be placed in net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadColonies(GraphicsConfiguration gc, Class resourceLocator, boolean doLookup) throws FreeColException {
        colonies = new Vector(COLONY_COUNT);

        for(int i = 0; i < COLONY_COUNT; i++) {
            String filePath = dataDirectory + path + colonyDirectory + colonyName + i + extension;
            colonies.add(findImage(filePath, resourceLocator, doLookup));
        }
    }

    /**
     * Loads the indian settlement pictures from files into memory.
     * @param gc The GraphicsConfiguration is needed to create images that are compatible with the
     * local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files
     * has been manually provided by the user. If set to 'true' then a
     * lookup will be done to search for image files from net.sf.freecol,
     * in this case the images need to be placed in net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadIndians(GraphicsConfiguration gc, Class resourceLocator, boolean doLookup) throws FreeColException {
        indians = new Vector(INDIAN_COUNT);

        for(int i = 0; i < INDIAN_COUNT; i++) {
            String filePath = dataDirectory + path + indianDirectory + indianName + i + extension;
            indians.add(findImage(filePath, resourceLocator, doLookup));
        }
    }

     /**
     * Loads the goods-images from file into memory.
     * @param gc The GraphicsConfiguration is needed to create images that are compatible with the
     * local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files
     * has been manually provided by the user. If set to 'true' then a
     * lookup will be done to search for image files from net.sf.freecol,
     * in this case the images need to be placed in net/sf/freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadGoods(GraphicsConfiguration gc, Class resourceLocator, boolean doLookup) throws FreeColException {
        goods = new Vector(GOODS_COUNT);

        for (int i = 0; i < GOODS_COUNT; i++) {
            String filePath = dataDirectory + path + goodsDirectory + goodsName + i + extension;
            goods.add(findImage(filePath, resourceLocator, doLookup));
        }

        /*
        If all units are patched together in one graphics file then this is the way to load
        them into different images:

        Image unitsImage = new ImageIcon(url).getImage();
        BufferedImage tempImage = gc.createCompatibleImage(42, 63, Transparency.TRANSLUCENT);
        tempImage.getGraphics().drawImage(unitsImage, 0, 0, null);
        units.add(tempImage);
        */
    }

    /**
     * Generates color chip images and stores them in memory. Color chips
     * can be seen next to a unit to indicate its state.
     * @param gc The GraphicsConfiguration is needed to create images that are compatible with the
     * local environment.
     */
    private void loadColorChips(GraphicsConfiguration gc) {
        colorChips = new Hashtable(8);

        /*
        loadColorChip(gc, Color.BLACK);
        loadColorChip(gc, Color.BLUE);
        loadColorChip(gc, Color.CYAN);
        loadColorChip(gc, Color.GRAY);
        loadColorChip(gc, Color.GREEN);
        loadColorChip(gc, Color.MAGENTA);
        loadColorChip(gc, Color.ORANGE);
        loadColorChip(gc, Color.PINK);
        loadColorChip(gc, Color.RED);
        loadColorChip(gc, Color.WHITE);
        loadColorChip(gc, Color.YELLOW);
        */

        /*
        loadColorChip(gc, IndianSettlement.indianColors[0]);
        loadColorChip(gc, IndianSettlement.indianColors[1]);
        loadColorChip(gc, IndianSettlement.indianColors[2]);
        loadColorChip(gc, IndianSettlement.indianColors[3]);
        loadColorChip(gc, IndianSettlement.indianColors[4]);
        loadColorChip(gc, IndianSettlement.indianColors[5]);
        loadColorChip(gc, IndianSettlement.indianColors[6]);
        loadColorChip(gc, IndianSettlement.indianColors[7]);
        */
    }

    /**
     * Generates a color chip image and stores it in memory.
     * @param gc The GraphicsConfiguration is needed to create images that are compatible with the
     * local environment.
     * @param c The color of the color chip to create.
     */
    private void loadColorChip(GraphicsConfiguration gc, Color c) {
        BufferedImage tempImage = gc.createCompatibleImage(11, 17);
        Graphics g = tempImage.getGraphics();
        if (c == Color.BLACK) {
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
     * Returns the unit-image at the given index.
     * @param index The index of the unit-image to return.
     * @return The unit-image at the given index.
     */
    public Image getUnitImage(int index) {
        return ((ImageIcon) units.get(index)).getImage();
    }

    /**
     * Returns the unit-ImageIcon at the given index.
     * @param index The index of the unit-ImageIcon to return.
     * @return The unit-ImageIcon at the given index.
     */
    public ImageIcon getUnitImageIcon(int index) {
        return (ImageIcon) units.get(index);
    }

    /**
     * Returns the terrain-image at the given index.
     * @param index The index of the terrain-image to return.
     * @param x The x-coordinate of the location of the tile that is being drawn.
     * @param y The x-coordinate of the location of the tile that is being drawn.
     * @return The terrain-image at the given index.
     */
    public Image getTerrainImage(int index, int x, int y) {
        return getTerrainImage(index, LAND_CENTER, x, y);
    }

    /**
     * Returns the terrain-image at the given index with the specified type.
     * @param index The index of the terrain-image to return.
     * @param borderType The border type.
     * @param x The x-coordinate of the location of the tile that is being drawn.
     * @param y The x-coordinate of the location of the tile that is being drawn.
     * @return The terrain-image at the given index.
     */
    public Image getTerrainImage(int index, int borderType, int x, int y) {

        final int[] directionsAndImageLibOffsets = new int[] {
            ImageLibrary.LAND_NORTH,
            ImageLibrary.LAND_NORTH_EAST,
            ImageLibrary.LAND_EAST,
            ImageLibrary.LAND_SOUTH_EAST,
            ImageLibrary.LAND_SOUTH,
            ImageLibrary.LAND_SOUTH_WEST,
            ImageLibrary.LAND_WEST,
            ImageLibrary.LAND_NORTH_WEST,
            ImageLibrary.LAND_CENTER
        };

        borderType = directionsAndImageLibOffsets[borderType];

        if ((x + y) % 2 == 0) {
            return ((ImageIcon) ((Vector) terrain1.get(index)).get(borderType)).getImage();
        } else {
            return ((ImageIcon) ((Vector) terrain2.get(index)).get(borderType)).getImage();
        }
    }

    /**
     * Returns the image at the given index.
     * @param index The index of the image to return.
     * @return The image at the given index.
     */
    public Image getMiscImage(int index) {
        return ((ImageIcon) misc.get(index)).getImage();
    }

    /**
     * Returns the image at the given index.
     * @param index The index of the image to return.
     * @return The image at the given index.
     */
    public ImageIcon getMiscImageIcon(int index) {
        return (ImageIcon) misc.get(index);
    }

    /**
     * Returns the unit-button image at the given index in the given state
     * @param index The index of the image to return.
     * @param state The state (normal, highlighted, pressed, disabled)
     * @return The image pointer
     */
    public ImageIcon getUnitButtonImageIcon(int index, int state) {
        return (ImageIcon) unitButtons[state].get(index);
    }

    /**
     * Returns the indian settlement image at the given index
     * @param index The index of the image to return.
     * @return The image pointer
     */
    public Image getIndianSettlementImage(int index) {
        return ((ImageIcon) indians.get(index)).getImage();
    }

    /**
     * Returns the goods-image at the given index.
     * @param index The index of the goods-image to return.
     * @return The goods-image at the given index.
     */
    public Image getGoodsImage(int index) {
        return ((ImageIcon) goods.get(index)).getImage();
    }

    /**
     * Returns the goods-image at the given index.
     * @param index The index of the goods-image to return.
     * @return The goods-image at the given index.
     */
    public ImageIcon getGoodsImageIcon(int index) {
        return ((ImageIcon) goods.get(index));
    }

    /**
     * Returns the colony image at the given index
     * @param index The index of the image to return.
     * @return The image pointer
     */
    public Image getColonyImage(int index) {
        return ((ImageIcon) colonies.get(index)).getImage();
    }

    /**
     * Returns the color chip with the given color.
     * @param color The color of the color chip to return.
     * @return The color chip with the given color.
     */
    public Image getColorChip(Color color) {
        Image colorChip = (Image) colorChips.get(color);
        if (colorChip == null) {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
            loadColorChip(gc, color);
            colorChip = (Image) colorChips.get(color);
        }
        return colorChip;
    }

    /**
     * Returns the width of the terrain-image at the given index.
     * @param index The index of the terrain-image.
     * @return The width of the terrain-image at the given index.
     */
    public int getTerrainImageWidth(int index) {
        return ((ImageIcon) ((Vector) terrain1.get(index)).get(LAND_CENTER)).getIconWidth();
    }

    /**
     * Returns the height of the terrain-image at the given index.
     * @param index The index of the terrain-image.
     * @return The height of the terrain-image at the given index.
     */
    public int getTerrainImageHeight(int index) {
        return ((ImageIcon) ((Vector) terrain1.get(index)).get(LAND_CENTER)).getIconHeight();
    }

    /**
     * Returns the width of the Colony-image at the given index.
     * @param index The index of the Colony-image.
     * @return The width of the Colony-image at the given index.
     */
    public int getColonyImageWidth(int index) {
        return ((ImageIcon) colonies.get(index)).getIconWidth();
    }

    /**
     * Returns the height of the Colony-image at the given index.
     * @param index The index of the Colony-image.
     * @return The height of the Colony-image at the given index.
     */
    public int getColonyImageHeight(int index) {
        return ((ImageIcon) colonies.get(index)).getIconHeight();
    }

    /**
     * Returns the width of the IndianSettlement-image at the given index.
     * @param index The index of the IndianSettlement-image.
     * @return The width of the IndianSettlement-image at the given index.
     */
    public int getIndianSettlementImageWidth(int index) {
        return ((ImageIcon) indians.get(index)).getIconWidth();
    }

    /**
     * Returns the height of the IndianSettlement-image at the given index.
     * @param index The index of the IndianSettlement-image.
     * @return The height of the IndianSettlement-image at the given index.
     */
    public int getIndianSettlementImageHeight(int index) {
        return ((ImageIcon) indians.get(index)).getIconHeight();
    }

    /**
     * Returns the width of the unit-image at the given index.
     * @param index The index of the unit-image.
     * @return The width of the unit-image at the given index.
     */
    public int getUnitImageWidth(int index) {
        return ((ImageIcon) units.get(index)).getIconWidth();
    }

    /**
     * Returns the height of the unit-image at the given index.
     * @param index The index of the unit-image.
     * @return The height of the unit-image at the given index.
     */
    public int getUnitImageHeight(int index) {
        return ((ImageIcon) units.get(index)).getIconHeight();
    }


     /**
     * Returns the graphics that will represent the given settlement.
     * @param settlement The settlement whose graphics type is needed.
     * @return The graphics that will represent the given settlement.
     */
    public int getSettlementGraphicsType(Settlement settlement) {

        if (settlement instanceof Colony) {
            Colony colony = (Colony) settlement;

            int stockadeLevel = colony.getBuilding(Building.STOCKADE).getLevel();

            if (stockadeLevel == Building.HOUSE) {
                return COLONY_STOCKADE;
            } else if (stockadeLevel == Building.SHOP) {
                return COLONY_FORT;
            } else if (stockadeLevel == Building.FACTORY) {
                return COLONY_FORTRESS;
            } else if (colony.getUnitCount() <= 3) {
                return COLONY_SMALL;
            } else if (colony.getUnitCount() <= 7) {
                return COLONY_MEDIUM;
            } else {
                return COLONY_LARGE;
            }

        } else {  // IndianSettlement
            IndianSettlement indianSettlement = (IndianSettlement) settlement;

            return INDIAN_SETTLEMENT_CAMP;

            /* TODO: Use when we have graphics:
            if (indianSettlement.getKind() == IndianSettlement.CAMP) {
                return INDIAN_SETTLEMENT_CAMP;
            } else if (indianSettlement.getKind() == IndianSettlement.VILLAGE) {
                return INDIAN_SETTLEMENT_VILLAGE;
            } else { //CITY
                if (indianSettlement.getTribe() == IndianSettlement.AZTEC)
                    return INDIAN_SETTLEMENT_AZTEC;
                else // INCA
                    return INDIAN_SETTLEMENT_INCA;
            }
            */
        }
    }


    /**
     * Returns the graphics that will represent the given unit.
     * @param unit The unit whose graphics type is needed.
     * @return The graphics that will represent the given unit.
     */
    public int getUnitGraphicsType(Unit unit) {
        switch (unit.getType()) {
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
                if (unit.isArmed() && unit.isMounted()) {
                    return DRAGOON;
                } else if (unit.isArmed()) {
                    return SOLDIER;
                } else if (unit.isMounted()) {
                    return UNARMED_DRAGOON;
                } else if (unit.getNumberOfTools() > 0) {
                    return FREE_COLONIST_WITH_TOOLS;
                } else if (unit.isMissionary()) {
                    return MISSIONARY_FREE_COLONIST;
                } else {
                    // The ints representing the types are exactly the same
                    // as the ints representing the graphics. This is only the
                    // case for these units and it may change at ANY time.
                    return unit.getType();
                }
            case Unit.SEASONED_SCOUT:
                if (unit.isArmed() && unit.isMounted()) {
                    return DRAGOON;
                } else if (unit.isArmed()) {
                    return SOLDIER;
                } else if (unit.isMounted()) {
                    return SEASONED_SCOUT_MOUNTED;
                } else if (unit.getNumberOfTools() > 0) {
                    return FREE_COLONIST_WITH_TOOLS;
                } else if (unit.isMissionary()) {
                    return MISSIONARY_FREE_COLONIST;
                } else {
                    return SEASONED_SCOUT_NOT_MOUNTED;
                }
            case Unit.HARDY_PIONEER:
                if (unit.isArmed() && unit.isMounted()) {
                    return DRAGOON;
                } else if (unit.isArmed()) {
                    return SOLDIER;
                } else if (unit.isMounted()) {
                    return UNARMED_DRAGOON;
                } else if (unit.getNumberOfTools() > 0) {
                    return HARDY_PIONEER_WITH_TOOLS;
                } else if (unit.isMissionary()) {
                    return MISSIONARY_FREE_COLONIST;
                } else {
                    return HARDY_PIONEER_NO_TOOLS;
                }
            case Unit.VETERAN_SOLDIER:
                if (unit.isArmed() && unit.isMounted()) {
                    return VETERAN_DRAGOON;
                } else if (unit.isArmed()) {
                    return VETERAN_SOLDIER;
                } else if (unit.isMounted()) {
                    return UNARMED_VETERAN_DRAGOON;
                } else if (unit.getNumberOfTools() > 0) {
                    return FREE_COLONIST_WITH_TOOLS;
                } else if (unit.isMissionary()) {
                    return MISSIONARY_FREE_COLONIST;
                } else {
                    return UNARMED_VETERAN_SOLDIER;
                }
            case Unit.JESUIT_MISSIONARY:
                if (unit.isArmed() && unit.isMounted()) {
                    return DRAGOON;
                } else if (unit.isArmed()) {
                    return SOLDIER;
                } else if (unit.isMounted()) {
                    return UNARMED_DRAGOON;
                } else if (unit.getNumberOfTools() > 0) {
                    return FREE_COLONIST_WITH_TOOLS;
                } else if (unit.isMissionary()) {
                    return JESUIT_MISSIONARY;
                } else {
                    return JESUIT_MISSIONARY_NO_CROSS;
                }
            case Unit.INDENTURED_SERVANT:
                if (unit.isArmed() && unit.isMounted()) {
                    return DRAGOON;
                } else if (unit.isArmed()) {
                    return SOLDIER;
                } else if (unit.isMounted()) {
                    return UNARMED_DRAGOON;
                } else if (unit.getNumberOfTools() > 0) {
                    return FREE_COLONIST_WITH_TOOLS;
                } else if (unit.isMissionary()) {
                    return MISSIONARY_FREE_COLONIST;
                } else {
                    return INDENTURED_SERVANT;
                }
            case Unit.PETTY_CRIMINAL:
                if (unit.isArmed() && unit.isMounted()) {
                    return DRAGOON;
                } else if (unit.isArmed()) {
                    return SOLDIER;
                } else if (unit.isMounted()) {
                    return UNARMED_DRAGOON;
                } else if (unit.getNumberOfTools() > 0) {
                    return FREE_COLONIST_WITH_TOOLS;
                } else if (unit.isMissionary()) {
                    return MISSIONARY_FREE_COLONIST;
                } else {
                    return PETTY_CRIMINAL;
                }
            case Unit.INDIAN_CONVERT:
                if (unit.isArmed() && unit.isMounted()) {
                    return DRAGOON;
                } else if (unit.isArmed()) {
                    return SOLDIER;
                } else if (unit.isMounted()) {
                    return UNARMED_DRAGOON;
                } else if (unit.getNumberOfTools() > 0) {
                    return FREE_COLONIST_WITH_TOOLS;
                } else if (unit.isMissionary()) {
                    return MISSIONARY_FREE_COLONIST;
                } else {
                    return INDIAN_CONVERT;
                }
            case Unit.BRAVE:
                if (unit.isArmed() && unit.isMounted()) {
                    return INDIAN_DRAGOON;
                } else if (unit.isArmed()) {
                    return ARMED_BRAVE;
                } else if (unit.isMounted()) {
                    return MOUNTED_BRAVE;
                } else {
                    return BRAVE;
                }
            case Unit.COLONIAL_REGULAR:
                if (unit.isArmed() && unit.isMounted()) {
                    return COLONIAL_CAVALRY;
                } else if (unit.isArmed()) {
                    return COLONIAL_REGULAR;
                } else if (unit.isMounted()) {
                    return UNARMED_COLONIAL_CAVALRY;
                } else if (unit.getNumberOfTools() > 0) {
                    return FREE_COLONIST_WITH_TOOLS;
                } else if (unit.isMissionary()) {
                    return MISSIONARY_FREE_COLONIST;
                } else {
                    return UNARMED_COLONIAL_REGULAR;
                }
            case Unit.KINGS_REGULAR:
                if (unit.isArmed() && unit.isMounted()) {
                    return KINGS_CAVALRY;
                } else if (unit.isArmed()) {
                    return KINGS_REGULAR;
                } else if (unit.isMounted()) {
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
            default:
                // This case can NOT occur.
                return -1;
        }
    }
}
