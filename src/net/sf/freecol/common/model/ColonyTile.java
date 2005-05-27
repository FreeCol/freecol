
package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

/**
* Represents a work location on a tile.
*/
public class ColonyTile extends FreeColGameObject implements WorkLocation {
    private static final Logger logger = Logger.getLogger(ColonyTile.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
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

    
    public GoodsContainer getGoodsContainer() {
        return null;
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
        if (getWorkTile().getOwner() != null && getWorkTile().getOwner() != getColony()) {
            return false;
        }

        if (!isColonyCenterTile() && locatable instanceof Unit && (getUnit() == null || locatable == getUnit())) {
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

        if (!canAdd(locatable)) {
            if (getWorkTile().getOwner() != null && getWorkTile().getOwner() != getColony()) {
                throw new IllegalArgumentException("Cannot add locatable to this location: somebody else owns this land!");
            } else {                
                throw new IllegalArgumentException("Cannot add locatable to this location: there is a unit here already!");
            }
        }

        Unit u = (Unit) locatable;

        getWorkTile().takeOwnership(u.getOwner());

        if (u != null) {
            if (u.isArmed()) {
                u.setArmed(false);
            }

            if (u.isMounted()) {
                u.setMounted(false);
            }

            if (u.isMissionary()) {
                u.setMissionary(false);
            }

            if (u.getNumberOfTools() > 0) {
                u.setNumberOfTools(0);
            }
        }

        setUnit(u);
        
        getColony().updatePopulation();
        
        if (unit != null) {
            getWorkTile().setOwner(getColony());
        } else {
            getWorkTile().setOwner(null);
        }
    }


    /**
    * Remove the specified <code>Locatable</code> from this <code>WorkLocation</code>.
    *
    * @param locatable The <code>Locatable</code> that shall be removed from this <code>WorkLocation</code>.
    * @return <code>true</code> if the code>Locatable</code> was removed and <code>false</code> otherwise.
    */
    public void remove(Locatable locatable) {
        if (getUnit() == null) {
            return;
        }

        if (!getUnit().equals(locatable)) {
            return;
        }

        getWorkTile().setOwner(null);
        setUnit(null);
        getColony().updatePopulation();
    }


    public Iterator getUnitIterator() {
        ArrayList units = new ArrayList();
        if (getUnit() != null) {
            units.add(getUnit());
        }
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

            if (!workTile.isLand() && !colony.getBuilding(Building.DOCK).isBuilt()) {
                amount = 0;
            }

            if (amount > 0) {
                //amount += colony.getProductionBonus();
                //if (amount < 1) amount = 1;
                colony.addGoods(getUnit().getWorkType(), amount);
            }
        } else {
            int amount1 = workTile.potential(Goods.FOOD);
            colony.addGoods(Goods.FOOD, amount1);

            int type2 = workTile.secondaryGoods();
            int amount2 = workTile.potential(type2);
            colony.addGoods(type2, amount2);
            
            if (unit != null) {
                getWorkTile().setOwner(getColony());
            } else {
                getWorkTile().setOwner(null);
            }
        }
    }


    /**
    * Returns the production of food on this tile.
    */
    public int getFoodProduction() {
        return getProductionOf(Goods.FOOD);
    }


    /**
    * Returns the production of the given type of goods.
    */
    public int getProductionOf(int goodsType) {
        if ((getUnit() == null) && !(isColonyCenterTile())) {
            return 0; // Produce nothing if there's nobody to work the terrain.
        }

        if (!(isColonyCenterTile())) {
            if (getUnit().getWorkType() != goodsType) {
                return 0;
            }

            int amount = getUnit().getFarmedPotential(getUnit().getWorkType(), workTile);

            if (!workTile.isLand() && !colony.getBuilding(Building.DOCK).isBuilt()) {
                amount = 0;
            }

            /*if (amount > 0) {
                amount += colony.getProductionBonus();
            }*/
            
            return Math.max(0, amount);
        } else {
            if (goodsType == Goods.FOOD) {
                return workTile.potential(Goods.FOOD);
            } else if (goodsType == workTile.secondaryGoods()) {
                return workTile.potential(workTile.secondaryGoods());
            } else {
                return 0;
            }
        }
    }


    public void dispose() {
        if (unit != null) {
            getWorkTile().setOwner(null);
        }

        if (unit != null) {
            unit.dispose();
        }

        super.dispose();
    }


    /**
    * Makes an XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "ColonyTile".
    */
    public Element toXMLElement(Player player, Document document, boolean showAll, boolean toSavedGame) {
        Element colonyTileElement = document.createElement(getXMLElementTagName());

        colonyTileElement.setAttribute("ID", getID());
        colonyTileElement.setAttribute("colony", colony.getID());
        colonyTileElement.setAttribute("workTile", workTile.getID());

        if (unit != null) {
            colonyTileElement.appendChild(unit.toXMLElement(player, document, showAll, toSavedGame));
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

        Element unitElement = getChildElement(colonyTileElement, Unit.getXMLElementTagName());
        if (unitElement != null) {
            Unit unit = (Unit) getGame().getFreeColGameObject(unitElement.getAttribute("ID"));
            if (unit != null) {
                unit.readFromXMLElement(unitElement);
                this.unit = unit;
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
