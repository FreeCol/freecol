
package net.sf.freecol.common;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.TileType;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.sun.org.apache.xerces.internal.parsers.SAXParser;


/**
 * This class encapsulates any parts of the "specification" for FreeCol that are
 * expressed best using XML.  The XML is loaded through the class loader from
 * the resource named "specification.xml" in the same package as this class.
 */
public final class Specification
{
    private final  List  buildingTypeList;
    private final  List  tileTypeList;


    // ----------------------------------------------------------- constructors

    public Specification() {

        try {
            buildingTypeList = new ArrayList();
            tileTypeList = new ArrayList();

            SAXParser  parser = new SAXParser();
            parser.setContentHandler( new XmlHandler() );
            InputStream  in = Specification.class.getResourceAsStream("specification.xml");
            parser.parse( new InputSource(in) );
        }
        catch ( SAXException e ) {
            throw new RuntimeException( e );
        }
        catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }


    // ------------------------------------------------------------ API methods

    public int numberOfBuildingTypes() {

        return buildingTypeList.size();
    }


    public BuildingType buildingType( int buildingTypeIndex ) {

        return (BuildingType) buildingTypeList.get( buildingTypeIndex );
    }


    public int numberOfTileTypes() {

        return tileTypeList.size();
    }


    public TileType tileType( int tileTypeIndex ) {

        return (TileType) tileTypeList.get( tileTypeIndex );
    }


    // ----------------------------------------------------------- nested types

    private final class XmlHandler extends DefaultHandler {

        private  BuildingType  contextBuildingType;
        private  TileType      contextTileType;

        public void startElement( String      namespaceUuri,
                                  String      localName,
                                  String      elementName,
                                  Attributes  attributes ) throws SAXException {

            if ( "freecol-specification".equals(elementName) ) {

                // do nothing
            }
            else if ( "building-types".equals(elementName) ) {

                // do nothing
            }
            else if ( "building-type".equals(elementName) ) {

                contextBuildingType = new BuildingType();
            }
            else if ( "building-level".equals(elementName) ) {

                BuildingType.Level  level =
                    new BuildingType.Level( Messages.message(attributes.getValue("name")),
                            Integer.parseInt(attributes.getValue("hammers-required")),
                            Integer.parseInt(attributes.getValue("tools-required")),
                            Integer.parseInt(attributes.getValue("population-required"))
                    );
                contextBuildingType.add( level );
            }
            else if ( "tile-types".equals(elementName) ) {

                // do nothing
            }
            else if ( "tile-type".equals(elementName) ) {

                contextTileType =
                    new TileType( Messages.message(attributes.getValue("name")),
                                  Integer.parseInt(attributes.getValue("basic-move-cost")),
                                  Integer.parseInt(attributes.getValue("defence-bonus")) );

            }
            else if ( "when-forested".equals(elementName) ) {

                TileType  tileType =
                    new TileType( Messages.message(attributes.getValue("name")),
                                  Integer.parseInt(attributes.getValue("basic-move-cost")),
                                  Integer.parseInt(attributes.getValue("defence-bonus")) );

                contextTileType.whenForested = tileType;
            }
            else {
                throw new RuntimeException( elementName );
            }
        }

        public void endElement( String  namespaceUri,
                                String  localName,
                                String  elementName ) throws SAXException {

            if ( "freecol-specification".equals(elementName) ) {

                // do nothing
            }
            else if ( "building-types".equals(elementName) ) {

                // do nothing
            }
            else if ( "building-type".equals(elementName) ) {

                buildingTypeList.add( contextBuildingType );
                contextBuildingType = null;
            }
            else if ( "building-level".equals(elementName) ) {

                // do nothing
            }
            else if ( "tile-types".equals(elementName) ) {

                // do nothing
            }
            else if ( "tile-type".equals(elementName) ) {

                tileTypeList.add( contextTileType );
                contextTileType = null;
            }
            else if ( "when-forested".equals(elementName) ) {

                // do nothing
            }
            else {
                throw new RuntimeException( elementName );
            }
        }
    }

}
