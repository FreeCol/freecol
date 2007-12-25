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

package net.sf.freecol.common.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public final class TileType extends FreeColGameObjectType implements Features {

    private String artBasic;
    private String artOverlay;
    private String artForest;
    private String artCoast;
    private Color minimapColor;

    private boolean forest;
    private boolean water;
    private boolean canSettle;
    private boolean canSailToEurope;
    private boolean canHaveRiver;

    private int basicMoveCost;
    private int basicWorkTurns;
    private int attackBonus;
    private int defenceBonus;
    
    public static final int HUMIDITY = 0, TEMPERATURE = 1, ALTITUDE = 2, LATITUDE = 3;
    
    private int[] humidity = new int[2];
    private int[] temperature = new int[2];
    private int[] altitude = new int[2];
    private int[] latitude = new int[2];

    private List<ResourceType> resourceType;
    private List<Integer> resourceProbability;

    private GoodsType secondaryGoods = null;

    /**
     * Describe production here.
     */
    private List<AbstractGoods> production;

    /**
     * Contains the abilities and modifiers of this type.
     */
    private FeatureContainer featureContainer = new FeatureContainer();

    // ------------------------------------------------------------ constructor

    public TileType(int index) {
        setIndex(index);
    }

    // ------------------------------------------------------------ retrieval methods

    public String getArtBasic() {
        return artBasic;
    }

    public String getArtOverlay() {
        return artOverlay;
    }

    public String getArtForest() {
        return artForest;
    }

    public String getArtCoast() {
        return artCoast;
    }

    public Color getMinimapColor() {
        return minimapColor;
    }

    public boolean isForested() {
        return forest;
    }

    public boolean isWater() {
        return water;
    }

    public boolean canSettle() {
        return canSettle;
    }

    public boolean canSailToEurope() {
        return canSailToEurope;
    }

    public boolean canHaveRiver() {
        return canHaveRiver;
    }

    public int getBasicMoveCost() {
        return basicMoveCost;
    }

    public int getBasicWorkTurns() {
        return basicWorkTurns;
    }

    // this isn't actually used anywhere
    public int getAttackBonus() {
        return attackBonus;
    }

    public Modifier getDefenceBonus() {
        return getModifier("model.modifier.defence");
    }

    public int getPotential(GoodsType goodsType) {
        for (AbstractGoods goods : production) {
            if (goods.getType() == goodsType) {
                return goods.getAmount();
            }
        }
        return 0;
    }

    public GoodsType getSecondaryGoods() {
        return secondaryGoods;
    }

    /**
     * Get the <code>Production</code> value.
     *
     * @return a <code>List<AbstractGoods></code> value
     */
    public List<AbstractGoods> getProduction() {
        return production;
    }

    public List<ResourceType> getResourceTypeList() {
        return resourceType;
    }

    public ResourceType getRandomResourceType() {
        int size = resourceType.size();
        if (size <= 0) {
            return null;
        }
        int totalProb = 0;
        int[] prob = new int[size];
        for (int i = 0; i < size; i++) {
            totalProb += resourceProbability.get(i);
            prob[i] = totalProb;
        }
        Random rand = new Random();
        int decision = rand.nextInt(totalProb);
        for (int i = 0; i < size; i++) {
            if (decision <= prob[i]) {
                return resourceType.get(i);
            }
        }
        // Not supposed to end up here
        return null;
    }

    public boolean withinRange(int rangeType, int value) {
        switch (rangeType) {
        case HUMIDITY:
            return (humidity[0] <= value && value <= humidity[1]);
        case TEMPERATURE:
            return (temperature[0] <= value && value <= temperature[1]);
        case ALTITUDE:
            return (altitude[0] <= value && value <= altitude[1]);
        case LATITUDE:
            return (latitude[0] <= value && (latitude[1] == -1 || value <= latitude[1]));
        default:
            return false;
        }
    }
    
    /**
     * Returns true if the Object has the ability identified by
     * <code>id</code>.
     *
     * @param id a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasAbility(String id) {
        return featureContainer.hasAbility(id);
    }

    /**
     * Returns the Modifier identified by <code>id</code>.
     *
     * @param id a <code>String</code> value
     * @return a <code>Modifier</code> value
     */
    public Modifier getModifier(String id) {
        return featureContainer.getModifier(id);
    }

    /**
     * Add the given Feature to the Features Map. If the Feature given
     * can not be combined with a Feature with the same ID already
     * present, the old Feature will be replaced.
     *
     * @param feature a <code>Feature</code> value
     */
    public void addFeature(Feature feature) {
        featureContainer.addFeature(feature);
    }

    /**
     * Removes and returns a Feature from this feature set.
     *
     * @param oldFeature a <code>Feature</code> value
     * @return a <code>Feature</code> value
     */
    public Feature removeFeature(Feature oldFeature) {
        return featureContainer.removeFeature(oldFeature);
    }

    // ------------------------------------------------------------ API methods

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in, null, null);
    }

    public void readFromXML(XMLStreamReader in, final Map<String, GoodsType> goodsTypeByRef,
            final Map<String, ResourceType> resourceTypeByRef) throws XMLStreamException {
        setId(in.getAttributeValue(null, "id"));
        basicMoveCost = Integer.parseInt(in.getAttributeValue(null, "basic-move-cost"));
        basicWorkTurns = Integer.parseInt(in.getAttributeValue(null, "basic-work-turns"));
        forest = getAttribute(in, "is-forest", false);
        water = getAttribute(in, "is-water", false);
        canSettle = getAttribute(in, "can-settle", !water);
        canSailToEurope = getAttribute(in, "sail-to-europe", false);
        canHaveRiver = !(getAttribute(in, "no-river", water));
        attackBonus = 0;
        defenceBonus = 0;

        if (!water && canSettle) {
            secondaryGoods = goodsTypeByRef.get(in.getAttributeValue(null, "secondary-goods"));
        }
        
        artBasic = null;
        production = new ArrayList<AbstractGoods>();
        resourceType = new ArrayList<ResourceType>();
        resourceProbability = new ArrayList<Integer>();

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if ("art".equals(childName)) {
                artBasic = in.getAttributeValue(null, "basic");
                artOverlay = in.getAttributeValue(null, "overlay");
                artForest = in.getAttributeValue(null, "forest");
                artCoast = getAttribute(in, "coast", (water ? null : "terrain/beach/"));
                float[] defaultArray = new float[] {0.0f, 0.0f, 0.0f};
                minimapColor = new Color(Integer.decode(in.getAttributeValue(null, "minimap-color")));
                in.nextTag(); // close this element
            } else if ("gen".equals(childName)) {
                humidity[0] = getAttribute(in, "humidityMin", -3);
                humidity[1] = getAttribute(in, "humidityMax", 3);
                temperature[0] = getAttribute(in, "temperatureMin", -3);
                temperature[1] = getAttribute(in, "temperatureMax", 3);
                altitude[0] = getAttribute(in, "altitudeMin", 0);
                altitude[1] = getAttribute(in, "altitudeMax", 0);
                latitude[0] = getAttribute(in, "latitudeMin", -1);
                latitude[1] = getAttribute(in, "latitudeMax", -1);
                in.nextTag(); // close this element
            } else if ("modifier".equals(childName)) {
                Modifier modifier = new Modifier(in);
                addFeature(modifier); // close this element
            } else if ("production".equals(childName)) {
                GoodsType type = goodsTypeByRef.get(in.getAttributeValue(null, "goods-type"));
                int amount = Integer.parseInt(in.getAttributeValue(null, "value"));
                production.add(new AbstractGoods(type, amount));
                in.nextTag(); // close this element
            } else if ("resource".equals(childName)) {
                String r = in.getAttributeValue(null, "type");
                ResourceType rt = resourceTypeByRef.get(r);
                resourceType.add(rt);
                resourceProbability.add(getAttribute(in, "probability", 100));
                in.nextTag(); // close this element
            } else {
                throw new RuntimeException("unexpected: " + childName);
            }
        }
        
        if (artBasic == null) {
            throw new RuntimeException("TileType " + getId() + " has no art defined!");
        }
    }

}
