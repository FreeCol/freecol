package net.sf.freecol.common;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.TileType;
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

    private final List<TileType> tileTypeList;

    private final List<GoodsType> goodsTypeList;

    private final List<UnitType> unitTypeList;


    public Specification() {

        buildingTypeList = new ArrayList<BuildingType>();
        tileTypeList = new ArrayList<TileType>();
        goodsTypeList = new ArrayList<GoodsType>();
        unitTypeList = new ArrayList<UnitType>();

        InputStream in = Specification.class.getResourceAsStream("specification.xml");
        Document specificationDocument = Xml.documentFrom(in);

        final Map<String, GoodsType> goodsTypeByRef = new HashMap<String, GoodsType>();

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
                } else if ("tile-types".equals(childName)) {

                    ObjectFactory<TileType> factory = new ObjectFactory<TileType>() {
                        public TileType objectFrom(Node xml) {
                            TileType tileType = new TileType();
                            tileType.readFromXmlElement(xml);
                            return tileType;
                        }
                    };

                    tileTypeList.addAll(makeListFromXml(xml, factory));
                } else if ("goods-types".equals(childName)) {

                	ObjectFactory<GoodsType> factory = new ObjectFactory<GoodsType>() {
                		int goodsIndex = 0;
                        public GoodsType objectFrom(Node xml) {
                            GoodsType goodsType = new GoodsType(goodsIndex++);
                            goodsType.readFromXmlElement(xml, goodsTypeByRef);
                            goodsTypeByRef.put(Xml.attribute(xml, "ref"), goodsType);
                            return goodsType;
                        }
                    };

                    goodsTypeList.addAll(makeListFromXml(xml, factory));
                } else if ("unit-types".equals(childName)) {

                    ObjectFactory<UnitType> factory = new ObjectFactory<UnitType>() {
                        public UnitType objectFrom(Node xml) {
                            UnitType unitType = new UnitType();
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
    }

    public int numberOfBuildingTypes() {

        return buildingTypeList.size();
    }

    public BuildingType buildingType(int buildingTypeIndex) {

        return buildingTypeList.get(buildingTypeIndex);
    }

    public int numberOfTileTypes() {

        return tileTypeList.size();
    }

    public TileType tileType(int tileTypeIndex) {

        return tileTypeList.get(tileTypeIndex);
    }

    public int numberOfGoodsTypes() {

        return goodsTypeList.size();
    }

    public GoodsType goodsType(int goodsTypeIndex) {

        return goodsTypeList.get(goodsTypeIndex);
    }

    public int numberOfUnitTypes() {

        return unitTypeList.size();
    }

    public UnitType unitType(int unitTypeIndex) {

        return unitTypeList.get(unitTypeIndex);
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
