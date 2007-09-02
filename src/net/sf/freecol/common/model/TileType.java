package net.sf.freecol.common.model;

import java.awt.Color;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.Random;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.util.Xml;

import org.w3c.dom.Node;


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
    private int attackFactor;
    private int defenceFactor;
    
    public static final int HUMIDITY = 0, TEMPERATURE = 1, ALTITUDE = 2, LATITUDE = 3;
    
    private int[] humidity;
    private int[] temperature;
    private int[] altitude;
    private int[] latitude;

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

    public int getAttackFactor() {
        return attackFactor;
    }

    public int getDefenceFactor() {
        return defenceFactor;
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

    public void readFromXmlElement(Node xml, final Map<String, GoodsType> goodsTypeByRef,
                                    final Map<String, ResourceType> resourceTypeByRef) {

        setID(Xml.attribute(xml, "id"));
        basicMoveCost = Xml.intAttribute(xml, "basic-move-cost");
        basicWorkTurns = Xml.intAttribute(xml, "basic-work-turns");
        forest = Xml.booleanAttribute(xml, "is-forest", false);
        water = Xml.booleanAttribute(xml, "is-water", false);
        canSettle = Xml.booleanAttribute(xml, "can-settle", (water) ? false : true);
        canSailToEurope = Xml.booleanAttribute(xml, "sail-to-europe", false);
        canHaveRiver = !(Xml.booleanAttribute(xml, "no-river", !water));
        attackFactor = 100;
        defenceFactor = 100;
        
        artBasic = null;
        producedType = new ArrayList<GoodsType>();
        producedAmount = new ArrayList<Integer>();
        resourceType = new ArrayList<ResourceType>();
        resourceProbability = new ArrayList<Integer>();
        
        Xml.Method method = new Xml.Method() {
            public void invokeOn(Node xml) {
                String childName = xml.getNodeName();

                if ("art".equals(childName)) {
                    artBasic = Xml.attribute(xml, "basic", null);
                    artOverlay = Xml.attribute(xml, "overlay", null);
                    artForest = Xml.attribute(xml, "forest", null);
                    artCoast = Xml.attribute(xml, "coast", (water ? null : "terrain/beach/"));
                    float[] defaultArray = new float[] {0.0f, 0.0f, 0.0f};
                    float[] colorValues = Xml.floatArrayAttribute(xml, "minimap-color", defaultArray);
                    minimapColor = new Color(colorValues[0], colorValues[1], colorValues[2]);
                } else if ("gen".equals(childName)) {
                    int[] defaultArray = new int[] {-3, 3};
                    humidity = Xml.intArrayAttribute(xml, "humidity", defaultArray);
                    temperature = Xml.intArrayAttribute(xml, "temperature", defaultArray);
                    defaultArray = new int[] {0, 0};
                    altitude = Xml.intArrayAttribute(xml, "altitude", defaultArray);
                    defaultArray = new int[] {-1, -1};
                    latitude = Xml.intArrayAttribute(xml, "latitude", defaultArray);
                } else if ("skirmish".equals(childName)) {
                    attackFactor = Xml.intAttribute(xml, "attack-factor", 100);
                    defenceFactor = Xml.intAttribute(xml, "defence-factor", 100);
                } else if ("production".equals(childName)) {
                    String g = Xml.attribute(xml, "goods-type");
                    GoodsType gt = goodsTypeByRef.get(g);
                    producedType.add(gt);
                    producedAmount.add(Xml.intAttribute(xml, "value"));
                } else if ("resource".equals(childName)) {
                    String r = Xml.attribute(xml, "type");
                    ResourceType rt = resourceTypeByRef.get(r);
                    resourceType.add(rt);
                    resourceProbability.add(Xml.intAttribute(xml, "probability"));
                } else {
                    throw new RuntimeException("unexpected: " + xml);
                }
            }
        };
        Xml.forEachChild(xml, method);
        if (artBasic == null) {
            throw new RuntimeException("TileType " + getID() + " has no art defined!");
        }
    }

}
