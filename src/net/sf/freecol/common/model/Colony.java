
package net.sf.freecol.common.model;

import java.util.Vector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.FreeColException;


/**
* Represents a colony. A colony contains {@link Building}s and {@link ColonyTile}s.
* The latter represents the tiles around the <code>Colony</code> where working is
* possible.
*/
public final class Colony extends Settlement {
    private static final Logger logger = Logger.getLogger(Colony.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    /** The name of the colony. */
    private String name;

    /** Places a unit may work. Either a <code>Building</code> or a <code>ColonyTile</code>. */
    private ArrayList workLocations = new ArrayList();





    /**
    * Creates a new <code>Colony</code>.
    *
    * @param game The <code>Game</code> in which this object belongs.
    * @param owner The <code>Player</code> owning this <code>Colony</code>.
    * @param name The name of the new <code>Colony</code>.
    * @param tile The location of the <code>Colony</code>.
    */
    public Colony(Game game, Player owner, String name, Tile tile) {
        super(game, owner, tile);

        this.name = name;

        workLocations.add(new ColonyTile(getGame(), this, tile));
        workLocations.add(new ColonyTile(getGame(), this, getGame().getMap().getNeighbourOrNull(Map.N, tile)));
        workLocations.add(new ColonyTile(getGame(),this, getGame().getMap().getNeighbourOrNull(Map.NE, tile)));
        workLocations.add(new ColonyTile(getGame(),this, getGame().getMap().getNeighbourOrNull(Map.E, tile)));
        workLocations.add(new ColonyTile(getGame(),this, getGame().getMap().getNeighbourOrNull(Map.NW, tile)));
        workLocations.add(new ColonyTile(getGame(),this, getGame().getMap().getNeighbourOrNull(Map.SE, tile)));
        workLocations.add(new ColonyTile(getGame(),this, getGame().getMap().getNeighbourOrNull(Map.W, tile)));
        workLocations.add(new ColonyTile(getGame(),this, getGame().getMap().getNeighbourOrNull(Map.SW, tile)));
        workLocations.add(new ColonyTile(getGame(),this, getGame().getMap().getNeighbourOrNull(Map.S, tile)));

        workLocations.add(new Building(getGame(),this, Building.TOWN_HALL, Building.HOUSE));
        workLocations.add(new Building(getGame(),this, Building.CARPENTER, Building.HOUSE));
        workLocations.add(new Building(getGame(),this, Building.BLACKSMITH, Building.HOUSE));
        workLocations.add(new Building(getGame(),this, Building.TOBACCONIST, Building.HOUSE));
        workLocations.add(new Building(getGame(),this, Building.WEAVER, Building.HOUSE));
        workLocations.add(new Building(getGame(),this, Building.DISTILLER, Building.HOUSE));
        workLocations.add(new Building(getGame(),this, Building.FUR_TRADER, Building.HOUSE));
        workLocations.add(new Building(getGame(),this, Building.STOCKADE, Building.NOT_BUILT));
        workLocations.add(new Building(getGame(),this, Building.ARMORY, Building.NOT_BUILT));
        workLocations.add(new Building(getGame(),this, Building.DOCK, Building.NOT_BUILT));
        workLocations.add(new Building(getGame(),this, Building.SCHOOLHOUSE, Building.NOT_BUILT));
        workLocations.add(new Building(getGame(),this, Building.WAREHOUSE, Building.NOT_BUILT));
        workLocations.add(new Building(getGame(),this, Building.STABLES, Building.NOT_BUILT));
        workLocations.add(new Building(getGame(),this, Building.CHURCH, Building.NOT_BUILT));
        workLocations.add(new Building(getGame(),this, Building.PRINTING_PRESS, Building.NOT_BUILT));
        workLocations.add(new Building(getGame(),this, Building.CUSTOM_HOUSE, Building.NOT_BUILT));
    }


    /**
    * Initiates a new <code>Colony</code> from an <code>Element</code>.
    *
    * @param game The <code>Game</code> this object belongs to.
    * @param element The <code>Element</code> (in a DOM-parsed XML-tree) that describes
    *                this object.
    */
    public Colony(Game game, Element element) {
        super(game, element);

        readFromXMLElement(element);
    }





    /**
    * Gets an <code>Iterator</code> of every location in this <code>Colony</code>
    * where a {@link Unit} can work.
    *
    * @return The <code>Iterator</code>.
    * @see WorkLocation
    */
    public Iterator getWorkLocationIterator() {
        return workLocations.iterator();
    }


    /**
    * Gets an <code>Iterator</code> of every {@link Building} in this <code>Colony</code>.
    *
    * @return The <code>Iterator</code>.
    * @see Building
    */
    public Iterator getBuildingIterator() {
        ArrayList b = new ArrayList();

        Iterator w = getWorkLocationIterator();
        while (w.hasNext()) {
            Object o = w.next();

            if (o instanceof Building) {
                b.add(o);
            }
        }

        return b.iterator();
    }


    /**
    * Gets an <code>Iterator</code> of every {@link ColonyTile} in this <code>Colony</code>.
    *
    * @return The <code>Iterator</code>.
    * @see ColonyTile
    */
    public Iterator getColonyTileIterator() {
        ArrayList b = new ArrayList();

        Iterator w = getWorkLocationIterator();
        while (w.hasNext()) {
            Object o = w.next();

            if (o instanceof ColonyTile) {
                b.add(o);
            }
        }

        return b.iterator();
    }


    /**
    * Gets a <code>Building</code> of the specified type.
    *
    * @param type The type of building to get.
    * @return The <code>Building</code>.
    */
    public Building getBuilding(int type) {
        Iterator buildingIterator = getBuildingIterator();

        while (buildingIterator.hasNext()) {
            Building building = (Building) buildingIterator.next();
            if (building.isType(type)) {
                return building;
            }
        }

        return null;
    }


    /**
    * Gets a <code>Tile</code> from the neighbourhood of this <code>Colony</code>
    * @return The <code>Tile</code>.
    */
    public Tile getTile(int x, int y) {
        if (x==0 && y==0) {
            return getGame().getMap().getNeighbourOrNull(Map.N, tile);
        } else if (x==0 && y== 1) {
            return getGame().getMap().getNeighbourOrNull(Map.NE, tile);
        } else if (x==0 && y== 2) {
            return getGame().getMap().getNeighbourOrNull(Map.E, tile);
        } else if (x==1 && y== 0) {
            return getGame().getMap().getNeighbourOrNull(Map.NW, tile);
        } else if (x==1 && y== 1) {
            return tile;
        } else if (x==1 && y== 2) {
            return getGame().getMap().getNeighbourOrNull(Map.SE, tile);
        } else if (x==2 && y== 0) {
            return getGame().getMap().getNeighbourOrNull(Map.W, tile);
        } else if (x==2 && y== 1) {
            return getGame().getMap().getNeighbourOrNull(Map.SW, tile);
        } else if (x==2 && y== 2) {
            return getGame().getMap().getNeighbourOrNull(Map.S, tile);
        } else {
            return null;
        }
    }


    /**
    * Gets the specified <code>ColonyTile</code>.
    */
    public ColonyTile getColonyTile(int x, int y) {
        Tile t = getTile(x, y);

        Iterator i = getColonyTileIterator();
        while (i.hasNext()) {
            ColonyTile c = (ColonyTile) i.next();

            if (c.getWorkTile() == t) {
                return c;
            }
        }

        return null;
    }


    /**
    * Adds a <code>Locatable</code> to this Location.
    * @param locatable The code>Locatable</code> to add to this Location.
    */
    public void add(Locatable locatable) {
        if (locatable instanceof Unit) {
            Iterator i = getWorkLocationIterator();
            while (i.hasNext()) {
                WorkLocation w = (WorkLocation) i.next();
                if (w.canAdd(locatable)) {
                    //w.add(locatable);
                    locatable.setLocation(w);
                    return;
                }
            }
        } else {
            logger.warning("Tried to add an unrecognized 'Locatable' to a 'Colony'.");
        }
    }


    /**
    * Removes a code>Locatable</code> from this Location.
    * @param locatable The <code>Locatable</code> to remove from this Location.
    */
    public void remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            Iterator i = getWorkLocationIterator();
            while (i.hasNext()) {
                WorkLocation w = (WorkLocation) i.next();
                if (w.contains(locatable)) {
                    w.remove(locatable);
                    return;
                }
            }
        } else {
            logger.warning("Tried to remove an unrecognized 'Locatable' from a 'Colony'.");
        }
    }


    /**
    * Gets the amount of Units at this Location. These units are
    * located in a {@link WorkLocation} in this <code>Colony</code>.
    *
    * @return The amount of Units at this Location.
    */
    public int getUnitCount() {
        int count = 0;

        Iterator i = getWorkLocationIterator();
        while (i.hasNext()) {
            WorkLocation w = (WorkLocation) i.next();
            count += w.getUnitCount();
        }

        return count;
    }


    public Iterator getUnitIterator() {
        throw new UnsupportedOperationException();
    }


    public boolean contains(Locatable locatable) {
        throw new UnsupportedOperationException();
    }


    public boolean canAdd(Locatable locatable) {
        throw new UnsupportedOperationException();
    }


    /**
    * Gets a string representation of the Colony. Currently this method
    * just returns the name of the <code>Colony</code>, but that may
    * change later.
    *
    * @return The name of the colony.
    * @see #getName
    */
    public String toString() {
        return name;
    }

    
    /**
    * Gets the name of this <code>Colony</code>.
    * @return The name as a <code>String</code>.
    */
    public String getName() {
        return name;
    }


    /**
    * Prepares this <code>Colony</code> for a new turn.
    */
    public void newTurn() {

    }


    /**
    * Make a XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "Colony".
    */
    public Element toXMLElement(Player player, Document document) {
        Element colonyElement = document.createElement(getXMLElementTagName());

        colonyElement.setAttribute("ID", getID());
        colonyElement.setAttribute("name", name);
        colonyElement.setAttribute("owner", owner.getID());
        colonyElement.setAttribute("tile", tile.getID());

        Iterator workLocationIterator = workLocations.iterator();
        while (workLocationIterator.hasNext()) {
            colonyElement.appendChild(((FreeColGameObject) workLocationIterator.next()).toXMLElement(player, document));
        }

        return colonyElement;
    }


    /**
    * Initialize this object from an XML-representation of this object.
    * @param colonyElement The DOM-element ("Document Object Model") made to represent this "Colony".
    */
    public void readFromXMLElement(Element colonyElement) {
        setID(colonyElement.getAttribute("ID"));

        name = colonyElement.getAttribute("name");
        owner = (Player) getGame().getFreeColGameObject(colonyElement.getAttribute("owner"));
        tile = (Tile) getGame().getFreeColGameObject(colonyElement.getAttribute("tile"));

        NodeList childNodes = colonyElement.getChildNodes();
        for (int i=0; i<childNodes.getLength(); i++) {
            Element childElement = (Element) childNodes.item(i);

            if (childElement.getTagName().equals(ColonyTile.getXMLElementTagName())) {
                ColonyTile ct = (ColonyTile) getGame().getFreeColGameObject(childElement.getAttribute("ID"));

                if (ct != null) {
                    ct.readFromXMLElement(childElement);
                } else {
                    workLocations.add(new ColonyTile(getGame(), childElement));
                }
            } else if (childElement.getTagName().equals(Building.getXMLElementTagName())) {
                Building b = (Building) getGame().getFreeColGameObject(childElement.getAttribute("ID"));

                if (b != null) {
                    b.readFromXMLElement(childElement);
                } else {
                    workLocations.add(new Building(getGame(), childElement));
                }
            }
        }
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "colony".
    */
    public static String getXMLElementTagName() {
        return "colony";
    }
}
