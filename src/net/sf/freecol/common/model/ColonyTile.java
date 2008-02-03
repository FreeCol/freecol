/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */


package net.sf.freecol.common.model;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;

import org.w3c.dom.Element;

/**
* Represents a work location on a tile.
*/
public class ColonyTile extends FreeColGameObject implements WorkLocation, Ownable {


    private static final Logger logger = Logger.getLogger(ColonyTile.class.getName());

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
     * Initiates a new <code>Building</code> from an
     * XML representation.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if an error occured during parsing.
     */
    public ColonyTile(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        readFromXML(in);
    }
    
    /**
     * Initiates a new <code>Building</code> from an
     * XML representation.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param e An XML-element that will be used to initialize
     *      this object.
     */
    public ColonyTile(Game game, Element e) {
        super(game, e);

        readFromXMLElement(e);
    }

    /**
     * Initiates a new <code>ColonyTile</code> 
     * with the given ID. The object should later be
     * initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public ColonyTile(Game game, String id) {
        super(game, id);
    }

    /**
     * Returns the (non-unique) name of this <code>ColonyTile</code>.
     * @return The name of this ColonyTile.
     */
    public String getLocationName() {
        String name = getColony().getName();
        if (isColonyCenterTile()) {
            return name;
        } else {
            return Messages.message("nearLocation", "%location%", name);
        }
    }
    
    /**
     * Returns a description of the tile, with the name of the tile
     * and any improvements made to it (road/plow).
     *
     * @return The description label for this tile
     */
    public String getLabel() {
        return workTile.getLabel();
    }


    /**
    * Gets the owner of this <code>Ownable</code>.
    *
    * @return The <code>Player</code> controlling this
    *         {@link Ownable}.
    */
    public Player getOwner() {
        return colony.getOwner();
    }

    /**
     * Sets the owner of this <code>Ownable</code>.
     *
     * @param p The <code>Player</code> that should take ownership
     *      of this {@link Ownable}.
     * @exception UnsupportedOperationException is always thrown by
     *      this method.
     */
    public void setOwner(Player p) {
        throw new UnsupportedOperationException();
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
        return (getUnit() != null) ? 1 : 0;
    }

    /**
     * Relocates any worker on this <code>ColonyTile</code>.
     * The workers are added to another {@link WorkLocation}
     * within the {@link Colony}.
     */
    public void relocateWorkers() {
        if (getUnit() != null) {
            for (WorkLocation wl : getColony().getWorkLocations()) {
                if (wl != this && wl.canAdd(getUnit())) {
                    getUnit().setLocation(wl);
                    break;
                }
            }
        }
    }

    /**
    * Checks if the specified <code>Locatable</code> may be added to this <code>WorkLocation</code>.
    *
    * @param locatable the <code>Locatable</code>.
    * @return <code>true</code> if the <code>Unit</code> may be added and <code>false</code> otherwise.
    */
    public boolean canAdd(Locatable locatable) {
        if (getWorkTile().getOwningSettlement() != null && getWorkTile().getOwningSettlement() != getColony()) {
            return false;
        }

        if (!(workTile.isLand() || getColony().hasAbility("model.ability.produceInWater"))) {
            return false;
        }
        
        if (!((Unit) locatable).getType().hasSkill()) {
            return false;
        }

        return ( ! isColonyCenterTile()  &&  locatable instanceof Unit
                &&  ( getUnit() == null || locatable == getUnit()) );
    }


    /**
    * Add the specified <code>Locatable</code> to this <code>WorkLocation</code>.
    * @param locatable The <code>Locatable</code> that shall be added to this <code>WorkLocation</code>.
    */
    public void add(Locatable locatable) {
        if (isColonyCenterTile() || unit != null) {
            throw new IllegalStateException("Other unit present while adding a unit to ColonyTile:" + getId());
        }

        if (!canAdd(locatable)) {
            if (getWorkTile().getOwningSettlement() != null && getWorkTile().getOwningSettlement() != getColony()) {
                throw new IllegalArgumentException("Cannot add locatable to this location: somebody else owns this land!");
            }
            throw new IllegalArgumentException("Cannot add locatable to this location: there is a unit here already!");
        }

        Unit u = (Unit) locatable;

        getWorkTile().takeOwnership(u.getOwner(), getColony());

        if (u != null) {
            u.removeAllEquipment(false);
        }

        setUnit(u);
        
        getColony().updatePopulation();

        if (unit != null) {
            getWorkTile().setOwningSettlement(getColony());
        } else {
            getWorkTile().setOwningSettlement(null);
        }
    }
    

    /**
    * Remove the specified <code>Locatable</code> from this <code>WorkLocation</code>.
    * @param locatable The <code>Locatable</code> that shall be removed from this <code>WorkLocation</code>.
    */
    public void remove(Locatable locatable) {
        if (getUnit() == null) {
            return;
        }

        if (!getUnit().equals(locatable)) {
            return;
        }

        getUnit().setMovesLeft(0);
        getWorkTile().setOwningSettlement(null);
        setUnit(null);
        getColony().updatePopulation();
    }

    public List<Unit> getUnitList() {
        if(getUnit() == null) {
            return new ArrayList<Unit>();
        } else {
            return Collections.singletonList(getUnit());
        }
    }

    public Iterator <Unit> getUnitIterator() {
        return getUnitList().iterator();
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
     * Returns the unit who is occupying the tile
     * @return the unit who is occupying the tile
     * @see #isOccupied()
     */
    public Unit getOccupyingUnit() {
        return workTile.getOccupyingUnit();
    }

    /**
     * Checks whether there is a fortified enemy unit in the tile.
     * Units can't produce in occupied tiles
     * @return <code>true</code> if an fortified enemy unit is in the tile
     */
    public boolean isOccupied() {
        return workTile.isOccupied();
    }
    
    /**
    * Prepares this <code>ColonyTile</code> for a new turn.
    */
    public void newTurn() {
        if (isColonyCenterTile()) {
            produceGoodsCenterTile();
        } else if (getUnit() != null && !isOccupied()) {
            produceGoods();
            workTile.expendResource(getUnit().getWorkType(), colony);
        }
    }

    private void produceGoods() {
        int amount = getProductionOf(getUnit().getWorkType());

        if (amount > 0) {
            colony.addGoods(getUnit().getWorkType(), amount);
            unit.modifyExperience(amount);
        }
    }

    private void produceGoodsCenterTile() {
        
    	GoodsType goodsFood = workTile.primaryGoods();
        colony.addGoods(goodsFood, getProductionOf(goodsFood));
        
        GoodsType type2 = workTile.secondaryGoods();
        if (type2 != null)
        	colony.addGoods(type2, getProductionOf(type2));

        if (unit != null) {
            getWorkTile().setOwningSettlement(getColony());
        } else {
            getWorkTile().setOwningSettlement(null);
        }
    }
   
    /**
     * Returns a worktype for a unit.
     *
     * @param unit a <code>Unit</code> value
     * @return a workType
     */
    public GoodsType getWorkType(Unit unit) {
        GoodsType workType = unit.getWorkType();
        int amount = getProductionOf(unit, workType);
        if (amount == 0) {
            List<GoodsType> farmedGoodsTypes = FreeCol.getSpecification().getFarmedGoodsTypeList();
            for(GoodsType farmedGoods : farmedGoodsTypes) {
                int newAmount = getProductionOf(unit, farmedGoods);
                if (newAmount > amount) {
                    amount = newAmount;
                    workType = farmedGoods;
                }
            }
        }
        return workType;
    }
    
    /**
    * Returns the production of the given type of goods.
    */
    public int getProductionOf(GoodsType goodsType) {
        if (goodsType == null) {
            throw new IllegalArgumentException("GoodsType must not be 'null'.");
        } else if (getUnit() == null) {
            if (isColonyCenterTile()) {
                return getProductionOf(null, goodsType);
            } else {
                // Produce nothing if there's nobody to work the terrain.
                return 0;
            }
        } else if (goodsType.equals(getUnit().getWorkType())) {
            return getProductionOf(getUnit(), goodsType);
        } else {
            return 0;
        }
    }

    /**
    * Returns the production of the given type of goods which would be
    * produced by the given unit
    */
    public int getProductionOf(Unit unit, GoodsType goodsType) {
        int production = 0;
        if (!(isColonyCenterTile())) {
            if (!workTile.isLand() && !colony.hasAbility("model.ability.produceInWater")) {
                return 0;
            }

            production = unit.getProductionOf(goodsType, workTile.potential(goodsType));
        } else if (goodsType.isFoodType() || goodsType == workTile.secondaryGoods()) {
            production = workTile.potential(goodsType);
        }
        
        if (production > 0) {
            production = Math.max(1, production + colony.getProductionBonus());
        }
        return production;
    }


    public void dispose() {
        if (unit != null) {
            getWorkTile().setOwningSettlement(null);
            unit.dispose();
        }

        super.dispose();
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * <br><br>
     * 
     * Only attributes visible to the given <code>Player</code> will 
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *  
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation 
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> 
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());
        
        // Add attributes:       
        out.writeAttribute("ID", getId());
        out.writeAttribute("colony", colony.getId());
        out.writeAttribute("workTile", workTile.getId());

        if (unit != null) {
            unit.toXML(out, player, showAll, toSavedGame);
        }

        // End element:
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));

        colony = (Colony) getGame().getFreeColGameObject(in.getAttributeValue(null, "colony"));
        if (colony == null) {
            colony = new Colony(getGame(), in.getAttributeValue(null, "colony"));
        }
        workTile = (Tile) getGame().getFreeColGameObject(in.getAttributeValue(null, "workTile"));
        if (workTile == null) {
            workTile = new Tile(getGame(), in.getAttributeValue(null, "workTile"));
        }
        
        unit = null;
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(Unit.getXMLElementTagName())) {
                Unit unit = (Unit) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (unit != null) {
                    unit.readFromXML(in);
                    this.unit = unit;
                } else {
                    this.unit = new Unit(getGame(), in);
                } 
            }
        }

        if (colony.getTile() == workTile) {
            colonyCenterTile = true;
        } else {
            colonyCenterTile = false;
        }
    }

    /**
     * Will return the position of the tile and the name of the colony in
     * addition to the FreeColObject.toString().
     * 
     * @return A representation of a colony-tile that can be used for debugging.
     */
    public String toString() {
        return getWorkTile().getPosition().toString() + " in '" + getColony().getName() + "'" + super.toString();
    }

    /**
    * Gets the tag name of the root element representing this object.
    * @return "colonyTile".
    */
    public static String getXMLElementTagName() {
        return "colonyTile";
    }
}
