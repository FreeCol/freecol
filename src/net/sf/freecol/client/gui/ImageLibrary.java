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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.panel.ImageProvider;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Settlement.SettlementType;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.UnitType;

/**
 * Holds various images that can be called upon by others in order to display
 * certain things.
 */
public final class ImageLibrary extends ImageProvider {

    private static final Logger logger = Logger.getLogger(ImageLibrary.class.getName());

    public static final int UNIT_SELECT = 0, PLOWED = 4, TILE_TAKEN = 5, TILE_OWNED_BY_INDIANS = 6,
            LOST_CITY_RUMOUR = 7, DARKNESS = 8, MISC_COUNT = 10;

    public static final int UNIT_BUTTON_WAIT = 0, UNIT_BUTTON_DONE = 1, UNIT_BUTTON_FORTIFY = 2,
            UNIT_BUTTON_SENTRY = 3, UNIT_BUTTON_CLEAR = 4, UNIT_BUTTON_PLOW = 5, UNIT_BUTTON_ROAD = 6,
            UNIT_BUTTON_BUILD = 7, UNIT_BUTTON_DISBAND = 8, UNIT_BUTTON_ZOOM_IN = 9, UNIT_BUTTON_ZOOM_OUT = 10,
            UNIT_BUTTON_COUNT = 11;

    private static final String path = new String("images/"), extension = new String(".png"),
            unitsDirectory = new String("units/"),
            terrainDirectory = new String("terrain/"),
            tileName = new String("center"), borderName = new String("border"),
            unexploredDirectory = new String("unexplored/"), unexploredName = new String("unexplored"),
            riverDirectory = new String("river/"), riverName = new String("river"),
            miscDirectory = new String("misc/"), miscName = new String("Misc"),
            unitButtonDirectory = new String("order-buttons/"), unitButtonName = new String("button"),
            settlementDirectory = new String("settlements/"),
            monarchDirectory = new String("monarch/"),
            coatOfArmsDirectory = new String("coat-of-arms/");

    private final String dataDirectory;

    /**
     * A ArrayList of Image objects.
     */
    private List<ImageIcon> rivers, misc;

    private Map<Nation, ImageIcon> monarch;

    private Map<Nation, ImageIcon> coatOfArms;

    private EnumMap<SettlementType, Image> settlements;

    private EnumMap<Role, Map<UnitType, ImageIcon>> units, unitsGrayscale;

    private Map<String, ImageIcon> terrain1, terrain2, overlay1, overlay2,
            forests, bonus, goods;

    private Map<String, ArrayList<ImageIcon>> border1, border2, coast1, coast2;

    // Holds the unit-order buttons
    private List<ArrayList<ImageIcon>> unitButtons; 

    private EnumMap<Tension.Level, Image> alarmChips;

    private Map<Color, Image> colorChips;

    private Map<Color, Image> missionChips;

    private Map<Color, Image> expertMissionChips;

    /**
     * The scaling factor used when creating this
     * <code>ImageLibrary</code>. The value
     * <code>1</code> is used if this object is not
     * a result of a scaling operation.
     */
    private final float scalingFactor;


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
        this.scalingFactor = 1;
        // TODO: normally this check shouldn't be here. init(false) is the way
        // to go.
        if ("".equals(freeColHome)) {
            dataDirectory = "data/";
            init(true);
        } else {
            dataDirectory = freeColHome;
            init(false);
        }
    }

    /**
     * Private constructor used for cloning and getting a
     * scaled version of this <code>ImageLibrary</code>.
     * @param scalingFactor The scaling factor.
     * @see #getScaledImageLibrary
     */
    private ImageLibrary(float scalingFactor,
            EnumMap<Role, Map<UnitType, ImageIcon>> units,
            EnumMap<Role, Map<UnitType, ImageIcon>> unitsGrayscale,
            List<ImageIcon> rivers,
            List<ImageIcon> misc,
            EnumMap<SettlementType, Image> settlements,
            Map<String, ImageIcon>  terrain1,
            Map<String, ImageIcon>  terrain2,
            Map<String, ImageIcon> overlay1,
            Map<String, ImageIcon> overlay2,
            Map<String, ImageIcon> forests,
            Map<String, ImageIcon> bonus,
            Map<String, ImageIcon> goods,
            Map<String, ArrayList<ImageIcon>> border1,
            Map<String, ArrayList<ImageIcon>> border2,
            Map<String, ArrayList<ImageIcon>> coast1,
            Map<String, ArrayList<ImageIcon>> coast2,
            List<ArrayList<ImageIcon>> unitButtons,
            EnumMap<Tension.Level, Image> alarmChips,
            Map<Color, Image> colorChips,
            Map<Color, Image> missionChips,
            Map<Color, Image> expertMissionChips) {
        dataDirectory = "";

        this.scalingFactor = scalingFactor;
        this.units = units;
        this.unitsGrayscale = unitsGrayscale;
        this.rivers = rivers;
        this.misc = misc;
        this.settlements = settlements;
        this.terrain1 = terrain1;
        this.terrain2 = terrain2;
        this.overlay1 = overlay1;
        this.overlay2 = overlay2;
        this.forests = forests;
        this.bonus = bonus;
        this.goods = goods;
        this.border1 = border1;
        this.border2 = border2;
        this.coast1 = coast1;
        this.coast2 = coast2;

        this.unitButtons = unitButtons;
        this.alarmChips = alarmChips;
        this.colorChips = colorChips;
        this.missionChips = missionChips;
        this.expertMissionChips = expertMissionChips;

        scaleImages(scalingFactor);
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
        loadSettlements(gc, resourceLocator, doLookup);
        loadGoods(gc, resourceLocator, doLookup);
        loadBonus(gc, resourceLocator, doLookup);
        loadMonarch(gc, resourceLocator, doLookup);
        loadCoatOfArms(gc, resourceLocator, doLookup);

        alarmChips = new EnumMap<Tension.Level, Image>(Tension.Level.class);
        colorChips = new HashMap<Color, Image>();
        missionChips = new HashMap<Color, Image>();
        expertMissionChips = new HashMap<Color, Image>();
    }

    /**
     * Returns the scaling factor used when creating this ImageLibrary.
     * @return 1 unless {@link #getScaledImageLibrary} was used to create
     *      this object.
     */
    public float getScalingFactor() {
        return scalingFactor;
    }

    /**
     * Gets a scaled version of this <code>ImageLibrary</code>.
     * @param scalingFactor The factor used when scaling. 2 is twice
     *      the size of the original images and 0.5 is half.
     * @return A new <code>ImageLibrary</code>.
     */
    public ImageLibrary getScaledImageLibrary(float scalingFactor) {
        return new ImageLibrary(scalingFactor, units, unitsGrayscale, rivers,
                                misc, settlements, terrain1, terrain2, overlay1, overlay2,
                                forests, bonus, goods, border1, border2, coast1, coast2, unitButtons,
                                alarmChips, colorChips, missionChips, expertMissionChips);
    }

    /**
     * Scales the images in this <code>ImageLibrary</code>
     * using the given factor.
     * @param scalingFactor The factor used when scaling. 2 is twice
     *      the size of the original images and 0.5 is half.
     */
    private void scaleImages(float scalingFactor) {
        units = scaleUnitImages(units, scalingFactor);
        unitsGrayscale = scaleUnitImages(unitsGrayscale, scalingFactor);
        rivers = scaleImages(rivers, scalingFactor);
        misc = scaleImages(misc, scalingFactor);
        settlements = scaleImages(settlements, scalingFactor);
        //monarch = scaleImages(monarch);

        terrain1 = scaleImages(terrain1, scalingFactor);
        terrain2 = scaleImages(terrain2, scalingFactor);
        overlay1 = scaleImages(overlay1, scalingFactor);
        overlay2 = scaleImages(overlay2, scalingFactor);
        forests = scaleImages(forests, scalingFactor);
        bonus = scaleImages(bonus, scalingFactor);
        goods = scaleImages(goods, scalingFactor);
        
        border1 = scaleImages2(border1, scalingFactor);
        border2 = scaleImages2(border2, scalingFactor);
        coast1 = scaleImages2(coast1, scalingFactor);
        coast2 = scaleImages2(coast2, scalingFactor);
        //unitButtons = scaleImages2(unitButtons);
        /*
        alarmChips = scaleChips(alarmChips, scalingFactor);
        colorChips = scaleChips(colorChips, scalingFactor);
        missionChips = scaleChips(missionChips, scalingFactor);
        expertMissionChips = scaleChips(expertMissionChips, scalingFactor);
        */
    }

    private Image scaleImage(Image image, float scale) {
        return image.getScaledInstance(Math.round(image.getWidth(null) * scale),
                                       Math.round(image.getHeight(null) * scale),
                                       Image.SCALE_SMOOTH);
    }

    private ImageIcon scaleIcon(ImageIcon icon, float scale) {
        return new ImageIcon(scaleImage(icon.getImage(), scale));
    }

    private EnumMap<SettlementType, Image> scaleImages(EnumMap<SettlementType, Image> input, float scale) {
        EnumMap<SettlementType, Image> result = new EnumMap<SettlementType, Image>(SettlementType.class);
        for (Entry<SettlementType, Image> entry : input.entrySet()) {
            result.put(entry.getKey(), scaleImage(entry.getValue(), scale));
        }
        return result;
    }

    private Map<Color, Image> scaleChips(Map<Color, Image> input, float scale) {
        HashMap<Color, Image> output = new HashMap<Color, Image>();
        for (Entry<Color, Image> entry : input.entrySet()) {
            output.put(entry.getKey(), scaleImage(entry.getValue(), scale));
        }
        return output;
    }

    private Map<String, ImageIcon> scaleImages(Map<String, ImageIcon> input, float scale) {
        HashMap<String, ImageIcon> output = new HashMap<String, ImageIcon>();
        for (Entry<String, ImageIcon> entry : input.entrySet()) {
            output.put(entry.getKey(), scaleIcon(entry.getValue(), scale));
        }
        return output;
    }

    private ArrayList<ImageIcon> scaleImages(List<ImageIcon> input, float scale) {
        ArrayList<ImageIcon> output = new ArrayList<ImageIcon>();
        for (ImageIcon icon : input) {
            if (icon == null) {
                output.add(null);
            } else {
                output.add(scaleIcon(icon, scale));
            }
        }
        return output;
    }

    private Map<String, ArrayList<ImageIcon>> scaleImages2(Map<String, ArrayList<ImageIcon>> input, float scale) {
        HashMap<String, ArrayList<ImageIcon>> output = new HashMap<String, ArrayList<ImageIcon>>();
        for (Entry<String, ArrayList<ImageIcon>> entry : input.entrySet()) {
            if (entry.getValue() == null) {
                output.put(entry.getKey(), null);
            } else {
                output.put(entry.getKey(), scaleImages(entry.getValue(), scale));
            }
        }
        return output;
    }


    private ArrayList<ArrayList<ImageIcon>> scaleImages2(ArrayList<ArrayList<ImageIcon>> input, float scale) {
        ArrayList<ArrayList<ImageIcon>> output = new ArrayList<ArrayList<ImageIcon>>();
        for (ArrayList<ImageIcon> list : input) {
            if (list == null) {
                output.add(null);
            } else {
                output.add(scaleImages(list, scale));
            }
        }
        return output;
    }


    private EnumMap<Role, Map<UnitType, ImageIcon>> scaleUnitImages(EnumMap<Role, Map<UnitType, ImageIcon>> input,
                                                                    float f) {
        EnumMap<Role, Map<UnitType, ImageIcon>> result = new EnumMap<Role, Map<UnitType, ImageIcon>>(Role.class);
        for (Role role : Role.values()) {
            Map<UnitType, ImageIcon> oldMap = input.get(role);
            Map<UnitType, ImageIcon> newMap = new HashMap<UnitType, ImageIcon>();
            for (Entry<UnitType, ImageIcon> entry : oldMap.entrySet()) {
                ImageIcon oldIcon = entry.getValue();
                ImageIcon newIcon = new ImageIcon(oldIcon.getImage()
                                                  .getScaledInstance(Math.round(oldIcon.getIconWidth() * f),
                                                                     Math.round(oldIcon.getIconHeight() * f),
                                                                     Image.SCALE_SMOOTH));
                newMap.put(entry.getKey(), newIcon);
            }
            result.put(role, newMap);
        }
        return result;
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

        units = new EnumMap<Role, Map<UnitType, ImageIcon>>(Role.class);
        unitsGrayscale = new EnumMap<Role, Map<UnitType, ImageIcon>>(Role.class);

        for (Role role : Role.values()) {
            Map<UnitType, ImageIcon> unitMap = new HashMap<UnitType, ImageIcon>();
            Map<UnitType, ImageIcon> grayMap = new HashMap<UnitType, ImageIcon>();
            String filePath = dataDirectory + path + unitsDirectory;
            String fileName = null;

            if (role == Role.DEFAULT) {
                fileName = "unit" + extension;
            } else {
                String roleName = role.toString().toLowerCase();
                filePath += roleName + "/";
                fileName = roleName + extension;
            }
            ImageIcon defaultIcon = findImage(filePath + fileName, resourceLocator, doLookup);
            ImageIcon defaultIconGrayscale = convertToGrayscale(defaultIcon.getImage());

            for (UnitType unitType : FreeCol.getSpecification().getUnitTypeList()) {
                fileName = unitType.getArt() + extension;
                try {
                    ImageIcon unitIcon = findImage(filePath + fileName, resourceLocator, doLookup);
                    unitMap.put(unitType, unitIcon);
                    grayMap.put(unitType, convertToGrayscale(unitIcon.getImage()));
                } catch (FreeColException e) {
                    logger.fine("Using default icon for UnitType " + unitType.getName());
                    unitMap.put(unitType, defaultIcon);
                    grayMap.put(unitType, defaultIconGrayscale);
                }
            }
            units.put(role, unitMap);
            unitsGrayscale.put(role, grayMap);
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
        terrain1 = new HashMap<String, ImageIcon>();
        terrain2 = new HashMap<String, ImageIcon>();
        overlay1 = new HashMap<String, ImageIcon>();
        overlay2 = new HashMap<String, ImageIcon>();
        border1 = new HashMap<String, ArrayList<ImageIcon>>();
        border2 = new HashMap<String, ArrayList<ImageIcon>>();
        coast1 = new HashMap<String, ArrayList<ImageIcon>>();
        coast2 = new HashMap<String, ArrayList<ImageIcon>>();
        
        for (TileType type : FreeCol.getSpecification().getTileTypeList()) {
            String filePath = dataDirectory + path + type.getArtBasic() + tileName;
            terrain1.put(type.getId(), findImage(filePath + "0" + extension, resourceLocator, doLookup));
            terrain2.put(type.getId(), findImage(filePath + "1" + extension, resourceLocator, doLookup));

            if (type.getArtOverlay() != null) {
                filePath = dataDirectory + path + type.getArtOverlay();
                overlay1.put(type.getId(), findImage(filePath + "0" + extension, resourceLocator, doLookup));
                overlay2.put(type.getId(), findImage(filePath + "1" + extension, resourceLocator, doLookup));
            }
            
            ArrayList<ImageIcon> tempArrayList1 = new ArrayList<ImageIcon>();
            ArrayList<ImageIcon> tempArrayList2 = new ArrayList<ImageIcon>();
            for (Direction direction : Direction.values()) {
                filePath = dataDirectory + path + type.getArtBasic() + borderName + "_" +
                    direction.toString();
                tempArrayList1.add(findImage(filePath + "_even" + extension, resourceLocator, doLookup));
                tempArrayList2.add(findImage(filePath + "_odd" + extension, resourceLocator, doLookup));
            }

            border1.put(type.getId(), tempArrayList1);
            border2.put(type.getId(), tempArrayList2);
            
            if (type.getArtCoast() != null) {
                tempArrayList1 = new ArrayList<ImageIcon>();
                tempArrayList2 = new ArrayList<ImageIcon>();
                for (Direction direction : Direction.values()) {
                    filePath = dataDirectory + path + type.getArtCoast() + borderName + "_" +
                        direction.toString();
                    tempArrayList1.add(findImage(filePath + "_even" + extension, resourceLocator, doLookup));
                    tempArrayList2.add(findImage(filePath + "_odd" + extension, resourceLocator, doLookup));
                }
                
                coast1.put(type.getId(), tempArrayList1);
                coast2.put(type.getId(), tempArrayList2);
            }
        }
        
        String unexploredPath = dataDirectory + path + terrainDirectory + unexploredDirectory + tileName;
        terrain1.put(unexploredName, findImage(unexploredPath + "0" + extension, resourceLocator, doLookup));
        terrain2.put(unexploredName, findImage(unexploredPath + "1" + extension, resourceLocator, doLookup));
        
        ArrayList<ImageIcon> unexploredArrayList1 = new ArrayList<ImageIcon>();
        ArrayList<ImageIcon> unexploredArrayList2 = new ArrayList<ImageIcon>();
        for (Direction direction : Direction.values()) {
            unexploredPath = dataDirectory + path + terrainDirectory + unexploredDirectory + borderName + 
                "_" + direction.toString();
            unexploredArrayList1.add(findImage(unexploredPath + "_even" + extension, resourceLocator, doLookup));
            unexploredArrayList2.add(findImage(unexploredPath + "_odd" + extension, resourceLocator, doLookup));
        }

        border1.put(unexploredName, unexploredArrayList1);
        border2.put(unexploredName, unexploredArrayList2);
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
        rivers = new ArrayList<ImageIcon>(combinations);
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
        forests = new HashMap<String, ImageIcon>();
        
        for (TileType type : FreeCol.getSpecification().getTileTypeList()) {
            if (type.getArtForest() != null) {
                String filePath = dataDirectory + path + type.getArtForest();
                forests.put(type.getId(), findImage(filePath, resourceLocator, doLookup));
            }
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
        misc = new ArrayList<ImageIcon>(MISC_COUNT);

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
        unitButtons = new ArrayList<ArrayList<ImageIcon>>(4);
        for (int i = 0; i < 4; i++) {
            unitButtons.add(new ArrayList<ImageIcon>(UNIT_BUTTON_COUNT));
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
    private void loadSettlements(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        settlements = new EnumMap<SettlementType, Image>(SettlementType.class);

        for (SettlementType settlementType : SettlementType.values()) {
            String filePath = dataDirectory + path + settlementDirectory + 
                settlementType.toString().toLowerCase() + extension;
            settlements.put(settlementType, findImage(filePath, resourceLocator, doLookup).getImage());
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
        goods = new HashMap<String, ImageIcon>();
        
        for (GoodsType type : FreeCol.getSpecification().getGoodsTypeList()) {
            String filePath = dataDirectory + path + type.getArt();
            goods.put(type.getId(), findImage(filePath, resourceLocator, doLookup));
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
        bonus = new HashMap<String, ImageIcon>();
        
        for (ResourceType type : FreeCol.getSpecification().getResourceTypeList()) {
            String filePath = dataDirectory + path + type.getArt();
            bonus.put(type.getId(), findImage(filePath, resourceLocator, doLookup));
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
        monarch = new HashMap<Nation, ImageIcon>();

        for (Nation nation : FreeCol.getSpecification().getNations()) {
            String monarchName = nation.getMonarchArt();
            String filePath = dataDirectory + path + monarchDirectory + monarchName;
            monarch.put(nation, findImage(filePath, resourceLocator, doLookup));
        }
    }

    /**
     * Loads the coat-of-arms images from file into memory.
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
    private void loadCoatOfArms(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
        throws FreeColException {
        coatOfArms = new HashMap<Nation, ImageIcon>();

        for (Nation nation : FreeCol.getSpecification().getNations()) {
            String coatOfArmsName = nation.getCoatOfArms();
            String filePath = dataDirectory + path + coatOfArmsDirectory + coatOfArmsName;
            coatOfArms.put(nation, findImage(filePath, resourceLocator, doLookup));
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
    private void loadAlarmChip(GraphicsConfiguration gc, Tension.Level alarm) {
        BufferedImage tempImage = gc.createCompatibleImage(10, 17);
        Graphics2D g = (Graphics2D) tempImage.getGraphics();

        g.setColor(Color.BLACK);
        g.drawRect(0, 0, 10, 16);

        switch(alarm) {
        case HAPPY:
            g.setColor(Color.GREEN);
            break;
        case CONTENT:
            g.setColor(Color.BLUE);
            break;
        case DISPLEASED:
            g.setColor(Color.YELLOW);
            break;
        case ANGRY:
            g.setColor(Color.ORANGE);
            break;
        case HATEFUL:
            g.setColor(Color.RED);
            break;
        }

        g.fillRect(1, 1, 8, 15);
        g.setColor(Color.BLACK);

        g.fillRect(4, 3, 2, 7);
        g.fillRect(4, 12, 2, 2);

        alarmChips.put(alarm, tempImage);
    }

    /**
     * Returns the monarch-image for the given tile.
     * 
     * @param nation The nation this monarch rules.
     * @return the monarch-image for the given nation.
     */
    public Image getMonarchImage(Nation nation) {
        return monarch.get(nation).getImage();
    }

    /**
     * Returns the monarch-image icon for the given Nation.
     * 
     * @param nation The nation this monarch rules.
     * @return the monarch-image for the given nation.
     */
    public ImageIcon getMonarchImageIcon(Nation nation) {
        return monarch.get(nation);
    }

    /**
     * Returns the coat-of-arms image for the given Nation.
     * 
     * @param nation The nation.
     * @return the coat-of-arms of this nation
     */
    public ImageIcon getCoatOfArmsImageIcon(Nation nation) {
        return coatOfArms.get(nation);
    }

    /**
     * Returns the coat-of-arms image for the given Nation.
     * 
     * @param nation The nation.
     * @return the coat-of-arms of this nation
     */
    public Image getCoatOfArmsImage(Nation nation) {
        return coatOfArms.get(nation).getImage();
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
        return getBonusImageIcon(type).getImage();
    }

    /**
     * Returns the bonus-ImageIcon at the given index.
     * 
     * @param type The type of the bonus-ImageIcon to return.
     */
    public ImageIcon getBonusImageIcon(ResourceType type) {
        return bonus.get(type.getId());
    }

    public ImageIcon getScaledBonusImageIcon(ResourceType type, float scale) {
        return getScaledImageIcon(getBonusImageIcon(type), scale);
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
     * Returns the scaled terrain-image for a terrain type (and position 0, 0).
     * 
     * @param type The type of the terrain-image to return.
     * @param scale The scale of the terrain image to return.
     * @return The terrain-image
     */
    public Image getScaledTerrainImage(TileType type, float scale) {
        // Index used for drawing the base is the artBasic value
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration();
        Image terrainImage = getTerrainImage(type, 0, 0);
        int width = getTerrainImageWidth(type);
        int height = getTerrainImageHeight(type);
        // Currently used for hills and mountains
        if (type.getArtOverlay() != null) {
            BufferedImage compositeImage = gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
            Graphics2D g = compositeImage.createGraphics();
            g.drawImage(terrainImage, 0, 0, null);
            g.drawImage(getOverlayImage(type, 0, 0), 0, 0, null);
            g.dispose();
            terrainImage = compositeImage;
        }
        if (type.isForested()) {
            BufferedImage compositeImage = gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
            Graphics2D g = compositeImage.createGraphics();
            g.drawImage(terrainImage, 0, 0, null);
            g.drawImage(getForestImage(type), 0, 0, null);
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
     * Returns the overlay-image for the given type.
     * 
     * @param type The type of the terrain-image to return.
     * @param x The x-coordinate of the location of the tile that is being
     *            drawn.
     * @param y The x-coordinate of the location of the tile that is being
     *            drawn.
     * @return The terrain-image at the given index.
     */
    public Image getOverlayImage(TileType type, int x, int y) {
        if ((x + y) % 2 == 0) {
            return overlay1.get(type.getId()).getImage();
        } else {
            return overlay2.get(type.getId()).getImage();
        }
    }

    /**
     * Returns the terrain-image for the given type.
     * 
     * @param type The type of the terrain-image to return.
     * @param x The x-coordinate of the location of the tile that is being
     *            drawn.
     * @param y The x-coordinate of the location of the tile that is being
     *            drawn.
     * @return The terrain-image at the given index.
     */
    public Image getTerrainImage(TileType type, int x, int y) {
        String key;
        if (type != null) {
            key = type.getId();
        } else {
            key = unexploredName;
        }
        if ((x + y) % 2 == 0) {
            return terrain1.get(key).getImage();
        } else {
            return terrain2.get(key).getImage();
        }
    }

    /**
     * Returns the border terrain-image for the given type.
     * 
     * @param type The type of the terrain-image to return.
     * @param direction a <code>Direction</code> value
     * @param x The x-coordinate of the location of the tile that is being
     *            drawn.
     * @param y The x-coordinate of the location of the tile that is being
     *            drawn.
     * @return The terrain-image at the given index.
     */
    public Image getBorderImage(TileType type, Direction direction, int x, int y) {

        int borderType = direction.ordinal();
        
        String key;
        if (type != null) {
            key = type.getId();
        } else {
            key = unexploredName;
        }

        if ((x + y) % 2 == 0) {
            return border1.get(key).get(borderType).getImage();
        } else {
            return border2.get(key).get(borderType).getImage();
        }
    }

    /**
     * Returns the coast terrain-image for the given type.
     * 
     * @param type The type of the terrain-image to return.
     * @param direction a <code>Direction</code> value
     * @param x The x-coordinate of the location of the tile that is being
     *            drawn.
     * @param y The x-coordinate of the location of the tile that is being
     *            drawn.
     * @return The terrain-image at the given index.
     */
    public Image getCoastImage(TileType type, Direction direction, int x, int y) {

        int borderType = direction.ordinal();
        
        String key;
        if (type != null) {
            key = type.getId();
        } else {
            key = unexploredName;
        }

        if ((x + y) % 2 == 0) {
            return coast1.get(key).get(borderType).getImage();
        } else {
            return coast2.get(key).get(borderType).getImage();
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
     * Returns the forest image for a terrain type.
     * 
     * @param type The type of the terrain-image to return.
     * @return The image at the given index.
     */
    public Image getForestImage(TileType type) {
        return forests.get(type.getId()).getImage();
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
     * Returns the goods-image at the given index.
     * 
     * @param g The type of the goods-image to return.
     * @return The goods-image at the given index.
     */
    public Image getGoodsImage(GoodsType g) {
        return getGoodsImageIcon(g).getImage();
    }

    /**
     * Returns the goods-image for a goods type.
     * 
     * @param g The type of the goods-image to return.
     * @return The goods-image at the given index.
     */
    public ImageIcon getGoodsImageIcon(GoodsType g) {
        return goods.get(g.getId());
    }

    /**
     * Returns the scaled goods-ImageIcon for a goods type.
     * 
     * @param type The type of the goods-ImageIcon to return.
     * @param scale The scale of the goods-ImageIcon to return.
     * @return The goods-ImageIcon at the given index.
     */
    public ImageIcon getScaledGoodsImageIcon(GoodsType type, float scale) {
        return getScaledImageIcon(getGoodsImageIcon(type), scale);
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
    public Image getAlarmChip(Tension.Level alarm) {
        Image alarmChip = alarmChips.get(alarm);

        if (alarmChip == null) {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration();
            loadAlarmChip(gc, alarm);
            alarmChip = alarmChips.get(alarm);
        }
        return alarmChip;
    }

    /**
     * Returns the width of the terrain-image for a terrain type.
     * 
     * @param type The type of the terrain-image.
     * @return The width of the terrain-image at the given index.
     */
    public int getTerrainImageWidth(TileType type) {
        String key;
        if (type != null) {
            key = type.getId();
        } else {
            key = unexploredName;
        }
        return terrain1.get(key).getIconWidth();
    }

    /**
     * Returns the height of the terrain-image for a terrain type.
     * 
     * @param type The type of the terrain-image.
     * @return The height of the terrain-image at the given index.
     */
    public int getTerrainImageHeight(TileType type) {
        String key;
        if (type != null) {
            key = type.getId();
        } else {
            key = unexploredName;
        }
        return terrain1.get(key).getIconHeight();
    }

    /**
     * Returns the graphics that will represent the given settlement.
     * 
     * @param settlement The settlement whose graphics type is needed.
     * @return The graphics that will represent the given settlement.
     */
    public Image getSettlementImage(Settlement settlement) {

        if (settlement instanceof Colony) {
            Colony colony = (Colony) settlement;

            // TODO: Put it in specification
            if (colony.isUndead()) {
                return settlements.get(SettlementType.UNDEAD);
            } else {
                int stockadeLevel = 0;
                if (colony.getStockade() != null) {
                    stockadeLevel = colony.getStockade().getLevel();
                }
                switch(stockadeLevel) {
                case 0:
                    if (colony.getUnitCount() <= 3) {
                        return settlements.get(SettlementType.SMALL);
                    } else if (colony.getUnitCount() <= 7) {
                        return settlements.get(SettlementType.MEDIUM);
                    } else {
                        return settlements.get(SettlementType.LARGE);
                    }
                case 1:
                    if (colony.getUnitCount() > 7) {
                        return settlements.get(SettlementType.LARGE_STOCKADE);
                    } else if (colony.getUnitCount() > 3) {
                        return settlements.get(SettlementType.MEDIUM_STOCKADE);
                    } else {
                        return settlements.get(SettlementType.SMALL_STOCKADE);
                    }
                case 2:
                    if (colony.getUnitCount() > 7) {
                        return settlements.get(SettlementType.LARGE_FORT);
                    } else {
                        return settlements.get(SettlementType.MEDIUM_FORT);
                    }
                case 3:
                    return settlements.get(SettlementType.LARGE_FORTRESS);
                default:
                    return settlements.get(SettlementType.SMALL);
                }
            }

        } else { // IndianSettlement
            return settlements.get(((IndianSettlement) settlement).getTypeOfSettlement());
        }
    }

    /**
     * Returns the ImageIcon that will represent the given unit.
     * 
     * @param unit The unit whose graphics type is needed.
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(Unit unit) {
        return getUnitImageIcon(unit.getType(), unit.getRole());
    }

    /**
     * Returns the ImageIcon that will represent a unit of the given type.
     *
     * @param unitType an <code>UnitType</code> value
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(UnitType unitType) {
        return units.get(Role.DEFAULT).get(unitType);
    }
    
    /**
     * Returns the ImageIcon that will represent a unit of the given
     * type and role.
     *
     * @param unitType an <code>UnitType</code> value
     * @param role a <code>Role</code> value
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(UnitType unitType, Role role) {
        return units.get(role).get(unitType);
    }

    /**
     * Returns the ImageIcon that will represent the given unit.
     *
     * @param unit an <code>Unit</code> value
     * @param grayscale a <code>boolean</code> value
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(Unit unit, boolean grayscale) {
        return getUnitImageIcon(unit.getType(), unit.getRole(), grayscale);
    }

    /**
     * Returns the ImageIcon that will represent a unit of the given type.
     *
     * @param unitType an <code>UnitType</code> value
     * @param grayscale a <code>boolean</code> value
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(UnitType unitType, boolean grayscale) {
        return getUnitImageIcon(unitType, Role.DEFAULT, grayscale);
    }

    /**
     * Returns the ImageIcon that will represent a unit of the given
     * type and role.
     *
     * @param unitType an <code>UnitType</code> value
     * @param role a <code>Role</code> value
     * @param grayscale a <code>boolean</code> value
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(UnitType unitType, Role role, boolean grayscale) {
        if (grayscale) {
            return unitsGrayscale.get(role).get(unitType);
        } else {
            return units.get(role).get(unitType);
        }
    }

    /**
     * Returns the scaled ImageIcon.
     * 
     * @param inputIcon an <code>ImageIcon</code> value
     * @param scale The scale of the ImageIcon to return.
     * @return The scaled ImageIcon.
     */
    public ImageIcon getScaledImageIcon(ImageIcon inputIcon, float scale) {
        Image image = inputIcon.getImage();
        return new ImageIcon(image.getScaledInstance(Math.round(image.getWidth(null) * scale),
                                                     Math.round(image.getHeight(null) * scale),
                                                     Image.SCALE_SMOOTH));
    }
    

}
