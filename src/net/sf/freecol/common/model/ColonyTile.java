
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

import org.w3c.dom.Element;

import net.sf.freecol.client.gui.i18n.Messages;

/**
* Represents a work location on a tile.
*/
public class ColonyTile extends FreeColGameObject implements WorkLocation, Ownable {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

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
            return Messages.message("nearLocation", new String[][] {{"%location%", name}});
        }
    }
    
    /**
     *  Returns a description of the tile, with the name of the tile
     *and any improvements made to it (road/plow)
     * @return The description label for this tile
     */
    public String getLabel() {
        
        Tile tile = getWorkTile();
        
        String label = tile.getName();
        
        if(tile.hasRiver())
            label += "/"+ Messages.message("river");
        
        if(tile.hasRoad())
            label += "/"+ Messages.message("road");
        
        if(tile.isPlowed())
            label += "/"+ Messages.message("plowed");
        
        return label;
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
            Iterator<WorkLocation> wli = getColony().getWorkLocationIterator();
            while (wli.hasNext()) {
                WorkLocation wl = wli.next();
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
        if (getWorkTile().getOwner() != null && getWorkTile().getOwner() != getColony()) {
            return false;
        }

        if (!(workTile.isLand() || getColony().getBuilding(Building.DOCK).isBuilt())) {
            return false;
        }
        
        if (!((Unit) locatable).isColonist() && ((Unit) locatable).getType() != Unit.INDIAN_CONVERT) {
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
            throw new IllegalStateException("Other unit present while adding a unit to ColonyTile:" + getID());
        }

        if (!canAdd(locatable)) {
            if (getWorkTile().getOwner() != null && getWorkTile().getOwner() != getColony()) {
                throw new IllegalArgumentException("Cannot add locatable to this location: somebody else owns this land!");
            }
            throw new IllegalArgumentException("Cannot add locatable to this location: there is a unit here already!");
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
    * @param locatable The <code>Locatable</code> that shall be removed from this <code>WorkLocation</code>.
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
    * Prepares this <code>ColonyTile</code> for a new turn.
    */
    public void newTurn() {
        if (isColonyCenterTile()) {
            produceGoodsCenterTile();
        } else if (getUnit() != null) {
            produceGoods();
        }
    }

    private void produceGoods() {
        int amount = getUnit().getFarmedPotential(getUnit().getWorkType(), workTile);

        if (!workTile.isLand() && !colony.getBuilding(Building.DOCK).isBuilt()) {
            amount = 0;
        }
        
        if (amount > 0) {
            //amount += colony.getProductionBonus();
            //if (amount < 1) amount = 1;
            colony.addGoods(getUnit().getWorkType(), amount);
            unit.modifyExperience(amount);
        }
    }

    private void produceGoodsCenterTile() {
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


    /**
    * Returns the production of food on this tile.
    * This is the same as calling:
    * <code>getProductionOf(Goods.FOOD)</code>.
    * 
    * @return The production of food in this colony.
    */
    public int getFoodProduction() {
        return getProductionOf(Goods.FOOD);
    }


   
    /**
     * Returns a worktype for a unit.
     *
     * @param unit a <code>Unit</code> value
     * @return a workType
     */
    public int getWorkType(Unit unit) {
        int workType = unit.getWorkType();
        int amount = unit.getFarmedPotential(workType, workTile);
        if (amount == 0) {
            for (int goods = 0; goods < Goods.NUMBER_OF_TYPES; goods++) {
                if (Goods.isFarmedGoods(goods)) {
                    int newAmount = unit.getFarmedPotential(goods, workTile);
                    if (newAmount > amount) {
                        amount = newAmount;
                        workType = goods;
                    }
                }
            }
        }
        return workType;
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
        }

        if (goodsType == Goods.FOOD) {
            return workTile.potential(Goods.FOOD);
        } else if (goodsType == workTile.secondaryGoods()) {
            return workTile.potential(workTile.secondaryGoods());
        } else {
            return 0;
        }
    }
    
    
    /**
    * Returns the unit type producing the greatest amount of the
    * given goods at this tile.
    *
    * @param goodsType The type of goods.
    * @return The {@link Unit#getType unit type}.
    * @see Unit#getExpertWorkType
    * @see Building#getExpertUnitType
    */
    public int getExpertForProducing(int goodsType) {
        switch (goodsType) {
            case Goods.FOOD:
                return ( getWorkTile().isLand() )
                    ? Unit.EXPERT_FARMER
                    : Unit.EXPERT_FISHERMAN;
            case Goods.FURS:
                return Unit.EXPERT_FUR_TRAPPER;
            case Goods.SILVER:
                return Unit.EXPERT_SILVER_MINER;
            case Goods.LUMBER:
                return Unit.EXPERT_LUMBER_JACK;
            case Goods.ORE:
                return Unit.EXPERT_ORE_MINER;
            case Goods.SUGAR:
                return Unit.MASTER_SUGAR_PLANTER;
            case Goods.COTTON:
                return Unit.MASTER_COTTON_PLANTER;
            case Goods.TOBACCO:
                return Unit.MASTER_TOBACCO_PLANTER;
            default:
                logger.warning("Unknown type of goods.");
                return Unit.FREE_COLONIST;
            }
    }


    public void dispose() {
        if (unit != null) {
            getWorkTile().setOwner(null);
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
        out.writeAttribute("ID", getID());
        out.writeAttribute("colony", colony.getID());
        out.writeAttribute("workTile", workTile.getID());

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
        setID(in.getAttributeValue(null, "ID"));

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
    * Gets the tag name of the root element representing this object.
    * @return "colonyTile".
    */
    public static String getXMLElementTagName() {
        return "colonyTile";
    }
}
