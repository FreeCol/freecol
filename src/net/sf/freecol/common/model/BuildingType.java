
package net.sf.freecol.common.model;


import java.util.ArrayList;
import java.util.List;


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

    public void add( Level level ) {

        levelList.add( level );
    }


    public int numberOfLevels() {

        return levelList.size();
    }


    public Level level( int levelIndex ) {

        return (Level) levelList.get( levelIndex );
    }


    // ----------------------------------------------------------- nested types

    public static final class Level {

        public final  String  name;
        public final  int     hammersRequired;
        public final  int     toolsRequired;
        public final  int     populationRequired;

        public Level( String  name,
                      int     hammersRequired,
                      int     toolsRequired,
                      int     populationRequired ) {

            this.name = name;
            this.hammersRequired = hammersRequired;
            this.toolsRequired = toolsRequired;
            this.populationRequired = populationRequired;
        }
    }

}
