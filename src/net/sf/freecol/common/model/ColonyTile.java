
package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Vector;
import java.util.Iterator;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.FreeColException;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
* Represents a work location on a tile.
*/
public class ColonyTile extends FreeColGameObject implements WorkLocation {
    private static final Logger logger = Logger.getLogger(ColonyTile.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    private Colony colony;
    private Tile workTile;
    private Unit unit;
    private boolean colonyCenterTile;

    /**
    * Creates a new <code>ColonyTile</code>.
    *
    * @param game The <code>Game</code> this object belongs to.
    * @param colony The <code>Colony</code> this object belongs to.
    * @param workTile The tile in which this <code>ColonyTile</code> represents a
    *                 <code>WorkLocation</code> for.
    */
    public ColonyTile(Game game, Colony colony, Tile workTile) {
        super(game);

        this.colony = colony;
        this.workTile = workTile;

        if (colony.getTile() == workTile) {
            colonyCenterTile = true;
        } else {
            colonyCenterTile = false;
        }
    }


    /**
    * Initiates a new <code>ColonyTile</code> from an <code>Element</code>.
    *
    * @param game The <code>Game</code> this object belongs to.
    * @param element The <code>Element</code> (in a DOM-parsed XML-tree) that describes
    *                this object.
    */
    public ColonyTile(Game game, Element element) {
        super(game, element);

        readFromXMLElement(element);
    }



    
    /**
    * Checks if this is the tile where the <code>Colony</code> is located.
    * @return The result.
    */
    public boolean isColonyCenterTile() {
        return colonyCenterTile;
    }


    /**
    * Gets the work tile.
    *
    * @return The tile in which this <code>ColonyTile</code> represents a
    *         <code>WorkLocation</code> for.
    */
    public Tile getWorkTile() {
        return workTile;
    }


    /**
    * Gets the tile where the colony is located.
    * @return The <code>Tile</code>.
    */
    public Tile getTile() {
        return colony.getTile();
    }


    /**
    * Gets the <code>Unit</code> currently working on this <code>ColonyTile</code>.
    *
    * @return The <code>Unit</code> or <i>null</i> if no unit is present.
    * @see #setUnit
    */
    public Unit getUnit() {
        return unit;
    }
    
    /**
    * Gets a pointer to the colony containing this tile.
    * @return The <code>Colony</code>.
    */
    public Colony getColony() {
        return colony;
    }

    /**
    * Sets a <code>Unit</code> to this <code>ColonyTile</code>.
    *
    * @param unit The <code>Unit</code>.
    * @see #getUnit
    */
    public void setUnit(Unit unit) {
        this.unit = unit;
    }


    /**
    * Gets the amount of Units at this <code>ColonyTile</code>.
    * @return The amount of Units at this <code>ColonyTile</code>.
    */
    public int getUnitCount() {
        if (getUnit() != null) {
            return 1;
        } else {
            return 0;
        }
    }


    /**
    * Checks if the specified <code>Locatable</code> may be added to this <code>WorkLocation</code>.
    *
    * @param locatable the <code>Locatable</code>.
    * @return <code>true</code> if the <code>Unit</code> may be added and <code>false</code> otherwise.
    */
    public boolean canAdd(Locatable locatable) {
        if (!isColonyCenterTile() && getUnit() == null && locatable instanceof Unit) {
            return true;
        } else {
            return false;
        }
    }


    /**
    * Add the specified <code>Locatable</code> to this <code>WorkLocation</code>.
    * @param locatable The <code>Locatable</code> that shall be added to this <code>WorkLocation</code>.
    */
    public void add(Locatable locatable) {
        if (isColonyCenterTile() || unit != null) {
            throw new IllegalStateException("Other unit present while adding a unit to ColonyTile:" + getID());
        }

        if (!(locatable instanceof Unit)) {
            throw new IllegalArgumentException("Can only add a 'Unit' to this location!");
        }

        setUnit((Unit) locatable);
    }


    /**
    * Remove the specified <code>Locatable</code> from this <code>WorkLocation</code>.
    *
    * @param locatable The <code>Locatable</code> that shall be removed from this <code>WorkLocation</code>.
    * @return <code>true</code> if the code>Locatable</code> was removed and <code>false</code> otherwise.
    */
    public void remove(Locatable locatable) {
        if (!getUnit().equals(locatable)) {
            return;
        }

        setUnit(null);
    }


    public Iterator getUnitIterator() {
        ArrayList units = new ArrayList();
        units.add(getUnit());
        return units.iterator();
    }


    /**
    * Checks if this <code>ColonyTile</code> contains the given <code>Locatable</code>.
    * 
    * @param locatable The <code>Locatable</code>.
    * @return The result.
    */
    public boolean contains(Locatable locatable) {
        return (locatable == unit) ? true:false;
    }


    /**
    * Gets the <code>Unit</code> currently working on this <code>ColonyTile</code>.
    *
    * @return The <code>Unit</code> or <i>null</i> if no unit is present.
    * @see #setUnit
    */
    public Unit getFirstUnit() {
        return getUnit();
    }


    /**
    * Gets the <code>Unit</code> currently working on this <code>ColonyTile</code>.
    *
    * @return The <code>Unit</code> or <i>null</i> if no unit is present.
    * @see #setUnit
    */
    public Unit getLastUnit() {
        return getUnit();
    }


    /**
    * Prepares this <code>ColonyTile</code> for a new turn.
    */
    public void newTurn() {
        if ((getUnit() == null) && !(isColonyCenterTile())) {
            return; // Produce nothing if there's nobody to work the terrain.
        }
        
        if (!(isColonyCenterTile())) {
                int amount = getUnit().getFarmedPotential(getUnit().getWorkType(), workTile);
                Goods g = new Goods(getGame(), null, getUnit().getWorkType(), amount);
            g.setLocation(colony);
        } else {
            int amount1 = workTile.potential(Goods.FOOD);
            Goods g = new Goods(getGame(), null, Goods.FOOD, amount1);
            g.setLocation(colony);
            int type2 = workTile.secondaryGoods();
            int amount2 = workTile.potential(type2);
            g = new Goods(getGame(), null, type2, amount2);
            g.setLocation(colony);
        }
        //colony.add(g);
    }


    /**
    * Makes an XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "ColonyTile".
    */
    public Element toXMLElement(Player player, Document document) {
        Element colonyTileElement = document.createElement(getXMLElementTagName());

        colonyTileElement.setAttribute("ID", getID());
        colonyTileElement.setAttribute("colony", colony.getID());
        colonyTileElement.setAttribute("workTile", workTile.getID());

        if (unit != null) {
            colonyTileElement.appendChild(unit.toXMLElement(player, document));
        }

        return colonyTileElement;
    }


    /**
    * Initialize this object from an XML-representation of this object.
    * @param colonyTileElement The DOM-element ("Document Object Model") made to represent this "ColonyTile".
    */
    public void readFromXMLElement(Element colonyTileElement) {
        setID(colonyTileElement.getAttribute("ID"));

        colony = (Colony) getGame().getFreeColGameObject(colonyTileElement.getAttribute("colony"));
        workTile = (Tile) getGame().getFreeColGameObject(colonyTileElement.getAttribute("workTile"));

        Element unitElement = (Element) colonyTileElement.getElementsByTagName(Unit.getXMLElementTagName()).item(0);
        if (unitElement != null) {
            Unit unit = (Unit) getGame().getFreeColGameObject(unitElement.getAttribute("ID"));
            if (unit != null) {
                unit.readFromXMLElement(unitElement);
            } else {
                this.unit = new Unit(getGame(), unitElement);
            }
        } else {
            unit = null;
        }

        if (colony.getTile() == workTile) {
            colonyCenterTile = true;
        } else {
            colonyCenterTile = false;
        }
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "colonyTile".
    */
    public static String getXMLElementTagName() {
        return "colonyTile";
    }
}
