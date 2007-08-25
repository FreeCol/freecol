package net.sf.freecol.common.model;

import java.util.List;
import java.util.Map;
import java.util.Random;

import net.sf.freecol.common.util.Xml;

import org.w3c.dom.Node;


public final class TileType
{
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final  String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION = "$Revision$";

    public int index;
    public String id;
    public String name;
    
    private String artBasic;
    private String artOverlay;
    private String artUnexplored;
    private String artCoast;
    
    public boolean forest;
    public boolean water;
    public boolean canSettle;
    public boolean canSailToEurope;
    public int basicMoveCost;
    public int basicWorkTurns;

    public int attackFactor;
    public int defenceFactor;
    
    public int[] humidity;
    public int[] temperature;
    public int[] altitude;
    public int[] latitude;

    private List<GoodsType> producedType;
    private List<Integer> producedAmount;
    private List<ResourceType> resourceType;
    private List<Integer> resourceProbability;

    // ------------------------------------------------------------ constructor

    public TileType(int index) {
        this.index = index;
    }

    // ------------------------------------------------------------ retrieval methods

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public String getArtBasic() {
        return artBasic;
    }

    public String getArtOverlay() {
        return artOverlay;
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
            totalProb += resourceProbability.intValue();
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

    // ------------------------------------------------------------ API methods

    public void readFromXmlElement(Node xml, Map<String, GoodsType> goodsTypeByRef,
                                    Map<String, ResourceType> resourceTypeByRef) {

        id = Xml.attribute(xml, "name");
        name = Xml.attribute(xml, "name");
        forest = Xml.booleanAttribute(xml, "is-forest", false);
        water = Xml.booleanAttribute(xml, "is-water", false);
        canSettle = Xml.booleanAttribute(xml, "can-settle", (water) ? false : true);
        basicMoveCost = Xml.intArrayAttribute(xml, "basic-move-cost");
        basicWorkTurns = Xml.intArrayAttribute(xml, "basic-work-turns");
        canSailToEurope = Xml.booleanAttribute(xml, "sail-to-europe", false);
        attackFactor = 100;
        defenceFactor = 100;

        Xml.Method method = new Xml.Method() {
            public void invokeOn(Node xml) {
                String childName = xml.getNodeName();

                if ("art".equals(childName)) {
                    artBasic = Xml.attribute(xml, "basic");
                    artOverlay = Xml.attribute(xml, "overlay", "");
                    artUnexplored = Xml.attribute(xml, "unexplored", "model.tile.unexplored");
                    artCoast = Xml.attribute(xml, "coast", "");
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
    }

}
