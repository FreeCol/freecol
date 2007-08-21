package net.sf.freecol.common;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.util.Xml;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * This class encapsulates any parts of the "specification" for FreeCol that are
 * expressed best using XML. The XML is loaded through the class loader from the
 * resource named "specification.xml" in the same package as this class.
 */
public final class Specification {

    public static final String COPYRIGHT = "Copyright (C) 2006-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private final List<BuildingType> buildingTypeList;

    private final List<GoodsType> goodsTypeList;
    private final List<GoodsType> farmedGoodsTypeList;

    private final List<ResourceType> resourceTypeList;

    private final List<TileType> tileTypeList;
    private final List<TileImprovementType> tileImprovementTypeList;

    private final List<UnitType> unitTypeList;


    public Specification() {

        buildingTypeList = new ArrayList<BuildingType>();
        goodsTypeList = new ArrayList<GoodsType>();
        resourceTypeList = new ArrayList<ResourceType>();
        tileTypeList = new ArrayList<TileType>();
        tileImprovementTypeList = new ArrayList<TileImprovementType>();
        unitTypeList = new ArrayList<UnitType>();

        final Map<String, GoodsType> goodsTypeByRef = new HashMap<String, GoodsType>();
        final Map<String, ResourceType> resourceTypeByRef = new HashMap<String, ResourceType>();
        final Map<String, TileType> tileTypeByRef = new HashMap<String, TileType>();

        InputStream in = Specification.class.getResourceAsStream("specification.xml");
        Document specificationDocument = Xml.documentFrom(in);

        /*  Moved into main class for referencing
        final Map<String, GoodsType> goodsTypeByRef = new HashMap<String, GoodsType>();
        */

        /* this method is invoked for each child element of the root element */
        final Xml.Method method = new Xml.Method() {
            public void invokeOn(Node xml) {

                String childName = xml.getNodeName();

                if ("building-types".equals(childName)) {

                    ObjectFactory<BuildingType> factory = new ObjectFactory<BuildingType>() {
                        public BuildingType objectFrom(Node xml) {
                            BuildingType buildingType = new BuildingType();
                            buildingType.readFromXmlElement(xml);
                            return buildingType;
                        }
                    };
                    buildingTypeList.addAll(makeListFromXml(xml, factory));

                } else if ("goods-types".equals(childName)) {

                    ObjectFactory<GoodsType> factory = new ObjectFactory<GoodsType>() {
                        int goodsIndex = 0;
                        public GoodsType objectFrom(Node xml) {
                            GoodsType goodsType = new GoodsType(goodsIndex++);
                            goodsType.readFromXmlElement(xml, goodsTypeByRef);
                            goodsTypeByRef.put(Xml.attribute(xml, "ref"), goodsType);
                            if (goodsType.isFarmed()) {
                                farmedGoodsTypeList.add(goodsType);
                            }
                            return goodsType;
                        }
                    };
                    goodsTypeList.addAll(makeListFromXml(xml, factory));

                } else if ("resource-types".equals(childName)) {

                    ObjectFactory<ResourceType> factory = new ObjectFactory<ResourceType>() {
                        int resIndex = 0;
                        public ResourceType objectFrom(Node xml) {
                            ResourceType resourceType = new ResourceType(resIndex++);
                            resourceType.readFromXmlElement(xml, goodsTypeByRef);
                            resourceTypeByRef.put(Xml.attribute(xml, "ref"), resourceType);
                            return resourceType;
                        }
                    };
                    resourceTypeList.addAll(makeListFromXml(xml, factory));

                } else if ("tile-types".equals(childName)) {

                    ObjectFactory<TileType> factory = new ObjectFactory<TileType>() {
                        int tileIndex = 0;
                        public TileType objectFrom(Node xml) {
                            TileType tileType = new TileType(tileIndex++);
                            tileType.readFromXmlElement(xml, goodsTypeByRef, resourceTypeByRef);
                            tileTypeByRef.put(Xml.attribute(xml, "ref"), tileType);
                            return tileType;
                        }
                    };
                    tileTypeList.addAll(makeListFromXml(xml, factory));

                } else if ("tileimprovement-types".equals(childName)) {

                    ObjectFactory<TileImprovementType> factory = new ObjectFactory<TileImprovementType>() {
                        int impIndex = 0;
                        public TileImprovementType objectFrom(Node xml) {
                            TileImprovementType tileImprovementType = new TileImprovementType(impIndex++);
                            tileImprovementType.readFromXmlElement(xml, tileTypeList, tileTypeByRef, goodsTypeByRef);
                            return tileImprovementType;
                        }
                    };
                    tileImprovementTypeList.addAll(makeListFromXml(xml, factory));

                } else if ("unit-types".equals(childName)) {

                    ObjectFactory<UnitType> factory = new ObjectFactory<UnitType>() {
                        int unitIndex = 0;
                        public UnitType objectFrom(Node xml) {
                            UnitType unitType = new UnitType(unitIndex++);
                            unitType.readFromXmlElement(xml, goodsTypeByRef);
                            return unitType;
                        }
                    };
                    unitTypeList.addAll(makeListFromXml(xml, factory));

                } else {
                    throw new RuntimeException("unexpected: " + xml);
                }
            }
        };

        /*
         * this method is invoked for each child element of the document, which
         * includes the "revision" comment and the root element at the moment
         */
        Xml.Method documentMethod = new Xml.Method() {
            public void invokeOn(Node xml) {

                if ("freecol-specification".equals(xml.getNodeName())) {

                    // for each child element of the document root element..
                    Xml.forEachChild(xml, method);
                }
            }
        };

        Xml.forEachChild(specificationDocument, documentMethod);
        
        // Post specification actions
        // Get Food, Bells, Crosses and Hammers
        Goods.initialize(getGoodsTypeList(), numberOfGoodsTypes());
        Tile.initialize(numberOfTileTypes());
    }

    // ---------------------------------------------------------- retrieval methods
    
    // -- Buildings --
    public List<BuildingType> getBuildingTypeList() {
        return buildingTypeList;
    }

    public int numberOfBuildingTypes() {
        return buildingTypeList.size();
    }

    public BuildingType buildingType(int buildingTypeIndex) {
        return buildingTypeList.get(buildingTypeIndex);
    }

    public int getBuildingIndex(BuildingType b) {
        return buildingTypeList.indexOf(b);
    }

    public BuildingType getBuildingType(String name) {
        for (BuildingType b : buildingTypeList) {
            if (b.getName().equals(name)) {
                return b;
            }
        }
        return null;
    }

    // -- Goods --
    public List<GoodsType> getGoodsTypeList() {
        return goodsTypeList;
    }

    public int numberOfGoodsTypes() {
        return goodsTypeList.size();
    }

    public int numberOfStoredGoodsTypes() {
        int n = 0;
        for (GoodsType g : goodsTypeList) {
            if (g.getStoreable()) {
                n++;
            }
        }
        return n;
    }

    public List<GoodsType> getFarmedGoodsTypeList() {
        return farmedGoodsTypeList;
    }

    public int numberOfFarmedGoodsTypes() {
        return farmedGoodsTypeList.size();
    }

    public GoodsType getGoodsType(int goodsTypeIndex) {
        return goodsTypeList.get(goodsTypeIndex);
    }

    public GoodsType getGoodsType(String name) {
        for (GoodsType g : goodsTypeList) {
            if (g.getName().equals(name)) {
                return g;
            }
        }
        return null;
    }

    // -- Resources --
    public List<ResourceType> getResourceTypeList() {
        return resourceTypeList;
    }

    public ResourceType getResourceType(int index) {
        return resourceTypeList.get(index);
    }

    public ResourceType getResourceType(String name) {
        for (ResourceType r : resourceTypeList) {
            if (r.getName().equals(name)) {
                return r;
            }
        }
        return null;
    }

    // -- Tiles --
    public List<TileType> getTileTypeList() {
        return tileTypeList;
    }

    public int numberOfTileTypes() {
        return tileTypeList.size();
    }

    public TileType getTileType(int index) {
        return tileTypeList.get(index);
    }

    public TileType getTileType(String name) {
        for (TileType t : tileTypeList) {
            if (t.getName().equals(name)) {
                return t;
            }
        }
        return null;
    }

    // -- Improvements --
    public List<TileImprovementType> getTileImprovementTypeList() {
        return tileImprovementTypeList;
    }

    public TileImprovementType getTileImprovementType(int index) {
        return tileImprovementTypeList.get(index);
    }

    public TileImprovementType getTileImprovementType(String name) {
        for (TileImprovementType ti : tileImprovementTypeList) {
            if (ti.getName().equals(name)) {
                return ti;
            }
        }
        return null;
    }

    // -- Units --
    public List<UnitType> getUnitTypeList() {
        return unitTypeList;
    }

    public int numberOfUnitTypes() {
        return unitTypeList.size();
    }

    public UnitType unitType(int unitTypeIndex) {
        return unitTypeList.get(unitTypeIndex);
    }

    public int getUnitIndex(UnitType b) {
        return unitTypeList.indexOf(b);
    }

    public UnitType getUnitType(String name) {
        for (UnitType u : unitTypeList) {
            if (u.getName().equals(name)) {
                return u;
            }
        }
        return null;
    }
    
    public UnitType getExpertForProducing(GoodsType goodsType) {
        for (UnitType unitType : getUnitTypeList()) {
            if (unitType.getExpertProduction() == goodsType) {
                return unitType;
            }
        }
        return null;
    }

    /**
     * Takes an XML node with child nodes that represent objects of the type
     * <code>T</code> and returns a list of the deserialized objects of type
     * <code>T</code>.
     * 
     * @param <T> the type of objects to deserialize
     * @param xml the XML node to whose children to deserialize.
     * @param factory the factory used to deserialize the object
     * @return a list containing all the child elements of the node deserialized
     */
    private <T> List<T> makeListFromXml(Node xml, final ObjectFactory<T> factory) {
        final ArrayList<T> list = new ArrayList<T>();
        Xml.Method method = new Xml.Method() {
            public void invokeOn(Node xml) {
                list.add(factory.objectFrom(xml));
            }
        };
        Xml.forEachChild(xml, method);
        return list;
    }


    /**
     * The interface that needs to be implemented and passed to
     * <code>makeListFromXml</code> to deserialize the objects from the XML
     * file.
     * 
     * @param <T> the type object that will be deserialized.
     */
    static interface ObjectFactory<T> {

        /**
         * Converts an XML node to an object of the generic type of the factory.
         * 
         * @param xml an XML node to convert to an object
         * @return the object
         */
        public T objectFrom(Node xml);
    }
}
