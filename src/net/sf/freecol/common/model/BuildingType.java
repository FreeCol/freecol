package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.util.Xml;

import org.w3c.dom.Node;

/**
 * Contains information on building types, like the number of upgrade levels a
 * given building type can have. The levels contain the information about the
 * name of the building in a given level and what is needed to build it.
 */
public final class BuildingType {
    public static final String COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private final List<Level> levelList;


    public BuildingType() {
        levelList = new ArrayList<Level>();
    }

    /**
     * Reads the content of this BuildingType object from the given XML node.
     * 
     * @param xml an XML node from which to fill this BuildingType's fields.
     */
    public void readFromXmlElement(Node xml) {
        Xml.Method method = new Xml.Method() {
            public void invokeOn(Node xml) {
                BuildingType.Level level = new BuildingType.Level();
                level.readFromXmlElement(xml);
                levelList.add(level);
            }
        };

        Xml.forEachChild(xml, method);
    }

    /**
     * Returns a list containing all the possible levels for the given building
     * type.
     * 
     * @return a list containing all the <code>Level</code> objects for the
     *         given building.
     */
    public List<Level> getLevels() {
        return levelList;
    }

    /**
     * Returns the number of levels a the given building type can have.
     * 
     * @return the number of possible levels for the given building type.
     */
    public int numberOfLevels() {
        return levelList.size();
    }

    /**
     * Returns a <code>Level</code> object giving informations about the
     * possible levels of a building.
     * 
     * @param levelIndex the level for which to retrieve information.
     * @return a <code>Level</code> object.
     */
    public Level level(int levelIndex) {
        return levelList.get(levelIndex);
    }


    /**
     * Gives informations about the different levels a building can have.
     */
    public static final class Level {

        public String name;

        public int hammersRequired;

        public int toolsRequired;

        public int populationRequired;

        public int workPlaces;


        void readFromXmlElement(Node xml) {
            name = Xml.attribute(xml, "name");
            hammersRequired = Xml.intAttribute(xml, "hammers-required");
            toolsRequired = Xml.intAttribute(xml, "tools-required");
            populationRequired = Xml.intAttribute(xml, "population-required");
            workPlaces = Xml.intAttribute(xml, "workplaces");
        }
    }

}
