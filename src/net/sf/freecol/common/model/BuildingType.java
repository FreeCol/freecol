
package net.sf.freecol.common.model;


import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.util.Xml;

import org.w3c.dom.Node;


public final class BuildingType
{
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final  String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION = "$Revision$";

    private final  List  levelList;


    // ----------------------------------------------------------- constructors

    public BuildingType() {

        levelList = new ArrayList();
    }


    // ------------------------------------------------------------ API methods

    public void readFromXmlElement( Node xml )
    {
        Xml.Method  method = new Xml.Method() {
            public void invokeOn( Node xml ) {

                BuildingType.Level  level = new BuildingType.Level();
                level.readFromXmlElement( xml );
                levelList.add( level );
            }
        };

        Xml.forEachChild( xml, method );
    }


    public int numberOfLevels() {

        return levelList.size();
    }


    public Level level( int levelIndex ) {

        return (Level) levelList.get( levelIndex );
    }


    // ----------------------------------------------------------- nested types

    public static final class Level {

        public  String  name;
        public  int     hammersRequired;
        public  int     toolsRequired;
        public  int     populationRequired;
        public  int     workPlaces;

        void readFromXmlElement( Node xml ) {

            name = Xml.messageAttribute(xml, "name");
            hammersRequired = Xml.intAttribute(xml, "hammers-required");
            toolsRequired = Xml.intAttribute(xml, "tools-required");
            populationRequired = Xml.intAttribute(xml, "population-required");
            workPlaces = Xml.intAttribute(xml, "workplaces");
        }
    }

}
