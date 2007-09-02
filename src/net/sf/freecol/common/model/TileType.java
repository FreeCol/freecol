package net.sf.freecol.common.model;

import java.awt.Color;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public final class TileType extends FreeColGameObjectType
{
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final  String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION = "$Revision$";

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

    private List<GoodsType> producedType;
    private List<Integer> producedAmount;
    private List<ResourceType> resourceType;
    private List<Integer> resourceProbability;

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

    public int getAttackBonus() {
        return attackBonus;
    }

    public int getDefenceBonus() {
        return defenceBonus;
    }

    public int getPotential(GoodsType goodsType) {
        int index = producedType.indexOf(goodsType);
        if (index >= 0) {
            return producedAmount.get(index);
        } else {
            return 0;
        }
    }

    public List<GoodsType> getPotentialTypeList() {
        return producedType;
    }
    
    public List<Integer> getPotentialAmountList() {
        return producedAmount;
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
    
    // ------------------------------------------------------------ API methods

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in, null, null);
    }

    public void readFromXML(XMLStreamReader in, final Map<String, GoodsType> goodsTypeByRef,
            final Map<String, ResourceType> resourceTypeByRef) throws XMLStreamException {
        setID(in.getAttributeValue(null, "id"));
        basicMoveCost = Integer.parseInt(in.getAttributeValue(null, "basic-move-cost"));
        basicWorkTurns = Integer.parseInt(in.getAttributeValue(null, "basic-work-turns"));
        forest = getAttribute(in, "is-forest", false);
        water = getAttribute(in, "is-water", false);
        canSettle = getAttribute(in, "can-settle", !water);
        canSailToEurope = getAttribute(in, "sail-to-europe", false);
        canHaveRiver = !(getAttribute(in, "no-river", water));
        attackBonus = 0;
        defenceBonus = 0;
        
        artBasic = null;
        producedType = new ArrayList<GoodsType>();
        producedAmount = new ArrayList<Integer>();
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
                minimapColor = new Color(Integer.parseInt(in.getAttributeValue(null, "minimap-color"), 16));
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
            } else if ("skirmish".equals(childName)) {
                attackBonus = getAttribute(in, "attack-factor", 0);
                defenceBonus = getAttribute(in, "defence-factor", 0);
                in.nextTag(); // close this element
            } else if ("production".equals(childName)) {
                String g = in.getAttributeValue(null, "goods-type");
                GoodsType gt = goodsTypeByRef.get(g);
                producedType.add(gt);
                producedAmount.add(Integer.parseInt(in.getAttributeValue(null, "value")));
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
            throw new RuntimeException("TileType " + getID() + " has no art defined!");
        }
    }

}
