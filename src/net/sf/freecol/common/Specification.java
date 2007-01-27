
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
 * expressed best using XML.  The XML is loaded through the class loader from
 * the resource named "specification.xml" in the same package as this class.
 */
public final class Specification {

    public static final  String  COPYRIGHT = "Copyright (C) 2006-2007 The FreeCol Team";
    public static final  String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION = "$Revision$";

    private final  List<BuildingType>  buildingTypeList;
    private final  List<TileType>      tileTypeList;
    private final  List<GoodsType>     goodsTypeList;
    private final  List<UnitType>      unitTypeList;


    // ----------------------------------------------------------- constructors

    public Specification() {

        buildingTypeList = new ArrayList<BuildingType>();
        tileTypeList = new ArrayList<TileType>();
        goodsTypeList = new ArrayList<GoodsType>();
        unitTypeList = new ArrayList<UnitType>();

        InputStream  in = Specification.class.getResourceAsStream( "specification.xml" );
        Document  specificationDocument = Xml.documentFrom( in );

        final  Map  goodsTypeByRef = new HashMap();

        /* this method is invoked for each child element of the root element */
        final Xml.Method  method = new Xml.Method() {
            public void invokeOn( Node xml ) {

                String  childName = xml.getNodeName();

                if ( "building-types".equals(childName) ) {

                    ObjectFactory  factory = new ObjectFactory() {
                        public Object objectFrom( Node xml ) {

                            BuildingType  buildingType = new BuildingType();
                            buildingType.readFromXmlElement( xml );
                            return buildingType;
                        }
                    };

                    makeListFromXml( buildingTypeList, xml, factory );
                }
                else if ( "tile-types".equals(childName) ) {

                    ObjectFactory  factory = new ObjectFactory() {
                        public Object objectFrom( Node xml ) {

                            TileType  tileType = new TileType();
                            tileType.readFromXmlElement( xml );
                            return tileType;
                        }
                    };

                    makeListFromXml( tileTypeList, xml, factory );
                }
                else if ( "goods-types".equals(childName) ) {

                    ObjectFactory  factory = new ObjectFactory() {
                        public Object objectFrom( Node xml ) {

                            GoodsType  goodsType = new GoodsType();
                            goodsType.readFromXmlElement( xml, goodsTypeByRef );
                            goodsTypeByRef.put( Xml.attribute(xml, "ref"), goodsType );
                            return goodsType;
                        }
                    };

                    makeListFromXml( goodsTypeList, xml, factory );
                }
                else if ( "unit-types".equals(childName) ) {

                    ObjectFactory  factory = new ObjectFactory() {
                        public Object objectFrom( Node xml ) {

                            UnitType  unitType = new UnitType();
                            unitType.readFromXmlElement( xml, goodsTypeByRef );
                            return unitType;
                        }
                    };

                    makeListFromXml( unitTypeList, xml, factory );
                }
                else {
                    throw new RuntimeException( "unexpected: " + xml );
                }
            }
        };

        /* this method is invoked for each child element of the document, which
         * includes the "revision" comment and the root element at the moment */
        Xml.Method  documentMethod = new Xml.Method() {
            public void invokeOn( Node xml ) {

                if ( "freecol-specification".equals(xml.getNodeName()) ) {

                    // for each child element of the document root element..
                    Xml.forEachChild( xml, method );
                }
            }
        };

        Xml.forEachChild( specificationDocument, documentMethod );
    }


    // ------------------------------------------------------------ API methods

    public List<BuildingType> getBuildingTypes() {
        return buildingTypeList;
    }

    public List<TileType> getTileTypes() {
        return tileTypeList;
    }

    public List<GoodsType> getGoodsTypes() {
        return goodsTypeList;
    }

    public List<UnitType> getUnitTypes() {
        return unitTypeList;
    }


    public int numberOfBuildingTypes() {

        return buildingTypeList.size();
    }


    public BuildingType buildingType( int buildingTypeIndex ) {

        return buildingTypeList.get( buildingTypeIndex );
    }


    public int numberOfTileTypes() {

        return tileTypeList.size();
    }


    public TileType tileType( int tileTypeIndex ) {

        return tileTypeList.get( tileTypeIndex );
    }


    public int numberOfGoodsTypes() {

        return goodsTypeList.size();
    }


    public GoodsType goodsType( int goodsTypeIndex ) {

        return goodsTypeList.get( goodsTypeIndex );
    }


    public int numberOfUnitTypes() {

        return unitTypeList.size();
    }


    public UnitType unitType( int unitTypeIndex ) {

        return unitTypeList.get( unitTypeIndex );
    }


    // -------------------------------------------------------- support methods

    private void makeListFromXml( final List list, Node xml, final ObjectFactory factory ) {

        Xml.Method  method = new Xml.Method() {
            public void invokeOn( Node xml ) {

                // construct an object from "xml" and add it to the list
                list.add( factory.objectFrom(xml) );
            }
        };

        // for each child element of "xml"..
        Xml.forEachChild( xml, method );
    }


    // ----------------------------------------------------------- nested types

    interface ObjectFactory {

        public Object objectFrom( Node xml );
    }

}
