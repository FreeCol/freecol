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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.Specification;
import org.w3c.dom.Element;

/**
 * Represents Europe in the game. Each <code>Player</code> has it's own
 * <code>Europe</code>. <p/> <br>
 * <br>
 * <p/> Europe is the place where you can {@link #recruit} and {@link #train}
 * new units. You may also sell/buy goods.
 */
public final class Europe extends FreeColGameObject implements Location, Ownable, Named {

    private static final Logger logger = Logger.getLogger(Europe.class.getName());

    private static final int RECRUIT_PRICE_INITIAL = 200;

    private static final int LOWER_CAP_INITIAL = 80;

    public static final String UNITS_TAG_NAME = "units";

    public static final String UNIT_CHANGE = "unitChange";

    /**
     * This array represents the types of the units that can be recruited in
     * Europe. They correspond to the slots that can be seen in the gui and that
     * can be used to communicate with the server/client. The array holds
     * exactly 3 elements and element 0 corresponds to recruit slot 1.
     */
    private UnitType[] recruitables = { null, null, null };
    public static final int RECRUIT_COUNT = 3;

    private java.util.Map<UnitType, Integer> unitPrices = new HashMap<UnitType, Integer>();

    private int recruitPrice;

    private int recruitLowerCap;

    /**
     * Contains the units on this location.
     */
    private List<Unit> units = Collections.emptyList();

    private Player owner;


    /**
     * Creates a new <code>Europe</code>.
     * 
     * @param game The <code>Game</code> in which this object belong.
     * @param owner The <code>Player</code> that will be using this object of
     *            <code>Europe</code>.
     */
    public Europe(Game game, Player owner) {
        super(game);
        this.owner = owner;

        recruitPrice = RECRUIT_PRICE_INITIAL;
        recruitLowerCap = LOWER_CAP_INITIAL;
    }

    /**
     * Initializes this object from an XML-representation of this object.
     * 
     * @param game The <code>Game</code> in which this object belong.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if an error occurred during parsing.
     */
    public Europe(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        readFromXML(in);
    }

    /**
     * Initializes this object from an XML-representation of this object.
     * 
     * @param game The <code>Game</code> in which this object belong.
     * @param e An XML-element that will be used to initialize this object.
     */
    public Europe(Game game, Element e) {
        super(game, e);

        readFromXMLElement(e);
    }

    /**
     * Initiates a new <code>Europe</code> with the given ID. The object
     * should later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     * 
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public Europe(Game game, String id) {
        super(game, id);
    }

    /**
     * Return true if this Europe could build at least one item of the
     * given EquipmentType.
     *
     * @param equipmentType an <code>EquipmentType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean canBuildEquipment(EquipmentType equipmentType) {
        for (AbstractGoods requiredGoods : equipmentType.getGoodsRequired()) {
            GoodsType goodsType = requiredGoods.getType();
            if (!(getOwner().canTrade(goodsType) &&
                  getOwner().getGold() >= getOwner().getMarket().getBidPrice(goodsType, requiredGoods.getAmount()))) {
                return false;
            }
        }
        return true;
    }


    /**
     * Returns true if not all recruitables are of the same type.
     * 
     * @return a <code>boolean</code> value
     */
    public boolean recruitablesDiffer() {
        return !(recruitables[0].equals(recruitables[1]) && recruitables[0].equals(recruitables[2]));
    }

    /**
     * Gets the type of the recruitable in Europe at the given slot.
     * 
     * @param slot The slot of the recruitable whose type needs to be returned.
     *            Should be 0, 1 or 2. NOTE - used to be 1, 2 or 3 and was
     *            called with 1-3 by some classes and 0-2 by others, the method
     *            itself expected 0-2.
     * @return The type of the recruitable in Europe at the given slot.
     * @exception IllegalArgumentException if the given <code>slot</code> does
     *                not exist.
     */
    public UnitType getRecruitable(int slot) {
        if ((slot >= 0) && (slot < RECRUIT_COUNT)) {
            return recruitables[slot];
        }
        throw new IllegalArgumentException("Wrong recruitement slot: " + slot);
    }

    /**
     * Sets the type of the recruitable in Europe at the given slot to the given
     * type.
     * 
     * @param slot The slot of the recruitable whose type needs to be set.
     *            Should be 0, 1 or 2. NOTE - changed in order to match
     *            getRecruitable above!
     * @param type The new type for the unit at the given slot in Europe. Should
     *            be a valid unit type.
     */
    public void setRecruitable(int slot, UnitType type) {
        // Note - changed in order to match getRecruitable
        if (slot >= 0 && slot < RECRUIT_COUNT) {
            recruitables[slot] = type;
        } else {
            logger.warning("setRecruitable: invalid slot(" + slot + ") given.");
        }
    }

    /**
     * Recruits a unit from Europe.
     * 
     * @param slot The slot the recruited unit(type) came from. This is needed
     *            for setting a new recruitable to this slot.
     * @param unit The recruited unit.
     * @param newRecruitable The recruitable that will fill the now empty slot.
     * @exception IllegalArgumentException if <code>unit == null</code>.
     * @exception IllegalStateException if the player recruiting the unit cannot
     *                afford the price.
     */
    public void recruit(int slot, Unit unit, UnitType newRecruitable) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit must not be 'null'.");
        } else if (getRecruitPrice() > unit.getOwner().getGold()) {
            throw new IllegalStateException("Not enough gold to recruit " + unit.getName() + ".");
        }

        unit.getOwner().modifyGold(-getRecruitPrice());
        incrementRecruitPrice();
        unit.setLocation(this);
        firePropertyChange(UNIT_CHANGE, getUnitCount() - 1, getUnitCount());
        unit.getOwner().updateImmigrationRequired();
        unit.getOwner().reduceImmigration();

        setRecruitable(slot, newRecruitable);
    }

    /**
     * Returns <i>null</i>.
     * 
     * @return <i>null</i>.
     */
    public Tile getTile() {
        return null;
    }

    /**
     * Europe does not belong to a colony.
     * 
     * @return Always returns null.
     */
    public Colony getColony() {
        return null;
    }

    /**
     * Adds a <code>Locatable</code> to this Location.
     * 
     * @param locatable The <code>Locatable</code> to add to this Location.
     */
    public void add(Locatable locatable) {
        if (!(locatable instanceof Unit)) {
            throw new IllegalArgumentException("Only units can be added to Europe.");
        } else if (!units.contains(locatable)) {
            if (units.equals(Collections.emptyList())) {
                units = new ArrayList<Unit>();
            }
            Unit newUnit = (Unit) locatable;
            units.add(newUnit);
            if (!(newUnit.getState() == Unit.UnitState.TO_EUROPE
                  || newUnit.getState() == Unit.UnitState.TO_AMERICA)) {
                newUnit.setState(Unit.UnitState.SENTRY);
            }
            firePropertyChange(UNIT_CHANGE, getUnitCount() - 1, getUnitCount());
        }
    }

    /**
     * Removes a <code>Locatable</code> from this Location.
     * 
     * @param locatable The <code>Locatable</code> to remove from this
     *            Location.
     */
    public void remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            units.remove(locatable);
            firePropertyChange(UNIT_CHANGE, getUnitCount() + 1, getUnitCount());
        } else {
            logger.warning("Tried to remove an unrecognized 'Locatable' from a europe.");
        }
    }

    /**
     * Checks if the specified <code>Locatable</code> is at this
     * <code>Location</code>.
     * 
     * @param locatable The <code>Locatable</code> to test the presence of.
     * @return The result.
     */
    public boolean contains(Locatable locatable) {
        if (locatable instanceof Unit) {
            return units.contains(locatable);
        }

        return false;
    }

    public GoodsContainer getGoodsContainer() {
        return null;
    }

    /**
     * Checks whether or not the specified locatable may be added to this
     * <code>Location</code>.
     * 
     * @param locatable The <code>Locatable</code> to test the addabillity of.
     * @return <i>true</i>.
     */
    public boolean canAdd(Locatable locatable) {
        return true;
    }

    /**
     * Gets the amount of Units at this Location.
     * 
     * @return The amount of Units at this Location.
     */
    public int getUnitCount() {
        return units.size();
    }

    /**
     * Gets a <code>List</code> of every <code>Unit</code> directly located
     * in this <code>Europe</code>. This does not include <code>Unit</code>s
     * on ships.
     * 
     * @return The <code>List</code>.
     */
    public List<Unit> getUnitList() {
        return units;
    }

    /**
     * Dispose of all units in this <code>Europe</code>.
     */
    public void disposeUnitList() {
        while (!units.isEmpty()) {
            Unit unit = units.remove(0);
            unit.dispose();
        }
        units = null;
    }

    /**
     * Gets an <code>Iterator</code> of every <code>Unit</code> directly
     * located in this <code>Europe</code>. This does not include
     * <code>Unit</code>s on ships.
     * 
     * @return The <code>Iterator</code>.
     */
    public Iterator<Unit> getUnitIterator() {
        return units.iterator();
    }

    /**
     * Gets the first <code>Unit</code> in this <code>Europe</code>.
     * 
     * @return The first <code>Unit</code> in this <code>Europe</code>.
     */
    public Unit getFirstUnit() {
        if (units.isEmpty()) {
            return null;
        } else {
            return units.get(0);
        }
    }

    /**
     * Gets the last <code>Unit</code> in this <code>Europe</code>.
     * 
     * @return The last <code>Unit</code> in this <code>Europe</code>.
     */
    public Unit getLastUnit() {
        if (units.isEmpty()) {
            return null;
        } else {
            return units.get(units.size() - 1);
        }
    }

    /**
     * Returns the price of a unit in Europe.
     * 
     * @param unitType The type of unit of which you need the price.
     * @return The price of this unit when trained in Europe.
     *         'UnitType.UNDEFINED' is returned in case the unit cannot be
     *         bought.
     */
    public int getUnitPrice(UnitType unitType) {
        Integer price = unitPrices.get(unitType);
        if (price != null) {
            return price.intValue();
        } else {
            return unitType.getPrice();
        }
    }

    /**
     * Train or purchase a unit in Europe.
     * 
     * @param unit The trained unit.
     * @exception IllegalArgumentException if <code>unit == null</code>.
     * @exception IllegalArgumentException if the unit to be trained doesn't
     *                have price
     * @exception IllegalStateException if the player recruiting the unit cannot
     *                afford the price.
     */
    public void train(Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit must not be 'null'.");
        }

        int price = getUnitPrice(unit.getType());
        if (price <= 0) {
            throw new IllegalArgumentException("Unit price must be a positive integer.");
        } else if (getUnitPrice(unit.getType()) > unit.getOwner().getGold()) {
            throw new IllegalStateException("Not enough gold to train " + unit.getName() + ".");
        }

        unit.getOwner().modifyGold(-price);
        increasePrice(unit, price);
        unit.setLocation(this);
        firePropertyChange(UNIT_CHANGE, getUnitCount() - 1, getUnitCount());
    }

    /**
     * Increases the price for a unit, if needed.  Applicable to both trained
     * and purchased units.
     *
     * @param unit The unit, trained or purchased
     * @param price The current price of the unit
     */
    private void increasePrice(Unit unit, int price) {
        Specification spec = Specification.getSpecification();
        String baseOption = "model.option.priceIncreasePerType";
        String name = unit.getType().getId().substring(unit.getType().getId().lastIndexOf('.'));
        String option = (spec.getBooleanOption(baseOption).getValue()) 
            ? "model.option.priceIncrease" + name
            : "model.option.priceIncrease";
        int increase = (spec.hasOption(option)) 
            ? spec.getIntegerOption(option).getValue()
            : 0;
        if (increase != 0) {
            unitPrices.put(unit.getType(), new Integer(price + increase));
        }
    }

    /**
     * Gets the current price for a recruit.
     * 
     * @return The current price of the recruit in this <code>Europe</code>.
     */
    public int getRecruitPrice() {
        int required = owner.getImmigrationRequired();
        int immigration = owner.getImmigration();
        int difference = Math.max(required - immigration, 0);
        return Math.max((recruitPrice * difference) / required, recruitLowerCap);
    }

    private void incrementRecruitPrice() {
        recruitPrice += Specification.getSpecification().getIntegerOption("model.option.recruitPriceIncrease").getValue();
        recruitLowerCap += Specification.getSpecification().getIntegerOption("model.option.lowerCapIncrease")
                .getValue();
    }

    /**
     * Gets the <code>Player</code> using this <code>Europe</code>.
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * Sets the owner of this <code>Ownable</code>.
     * 
     * @param p The <code>Player</code> that should take ownership of this
     *            {@link Ownable}.
     * @exception UnsupportedOperationException is always thrown by this method.
     */
    public void setOwner(Player p) {
        throw new UnsupportedOperationException();
    }

    /**
     * Prepares this object for a new turn. TODO: give Europe a shipyard and
     * remove this
     */
    public void newTurn() {
        // Repair any damaged ships:
        for (Unit unit : getUnitList()) {
            if (unit.isNaval() && unit.isUnderRepair()) {
                unit.setHitpoints(unit.getHitpoints() + 1);
                if (!unit.isUnderRepair()) {
                    addModelMessage(new ModelMessage("model.unit.shipRepaired", this, unit)
                                    .addName("%unit%", unit.getName())
                                    .addStringTemplate("%repairLocation%", getLocationName()));
                }
            }
        }
    }

    /**
     * Returns the name of this location.
     * 
     * @return The name of this location.
     */
    public StringTemplate getLocationName() {
        return StringTemplate.name(getName());
    }

    /**
     * Returns the name of the owner's home port.
     * 
     * @return The name of this location.
     */
    public String getName() {
        return getOwner().getEuropeName();
    }

    /**
     * Returns a suitable name.
     */
    public String toString() {
        return "Europe";
    }

    private void unitsToXML(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
            throws XMLStreamException {
        if (!units.isEmpty()) {
            out.writeStartElement(UNITS_TAG_NAME);
            for (Unit unit : units) {
                unit.toXML(out, player, showAll, toSavedGame);
            }
            out.writeEndElement();
        }
    }

    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     * 
     * <br>
     * <br>
     * 
     * Only attributes visible to the given <code>Player</code> will be added
     * to that representation if <code>showAll</code> is set to
     * <code>false</code>.
     * 
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation should be
     *            made for, or <code>null</code> if
     *            <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> will be
     *            added to the representation if <code>showAll</code> is set
     *            to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that is only
     *            needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
            throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute(ID_ATTRIBUTE, getId());
        for (int index = 0; index < recruitables.length; index++) {
            if (recruitables[index] != null) {
                out.writeAttribute("recruit" + index, recruitables[index].getId());
            }
        }
        out.writeAttribute("recruitPrice", Integer.toString(recruitPrice));
        out.writeAttribute("recruitLowerCap", Integer.toString(recruitLowerCap));
        out.writeAttribute("owner", owner.getId());

        for (Entry<UnitType, Integer> entry : unitPrices.entrySet()) {
            out.writeStartElement("unitPrice");
            out.writeAttribute("unitType", entry.getKey().getId());
            out.writeAttribute("price", entry.getValue().toString());
            out.writeEndElement();
        }

        unitsToXML(out, player, showAll, toSavedGame);

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * 
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, ID_ATTRIBUTE));

        Specification spec = FreeCol.getSpecification();
        for (int index = 0; index < recruitables.length; index++) {
            String unitTypeId = in.getAttributeValue(null, "recruit" + index);
            if (unitTypeId != null) {
                recruitables[index] = spec.getUnitType(unitTypeId);
            }
        }

        owner = getFreeColGameObject(in, "owner", Player.class);

        recruitPrice = getAttribute(in, "recruitPrice", RECRUIT_PRICE_INITIAL);
        recruitLowerCap = getAttribute(in, "recruitLowerCap", LOWER_CAP_INITIAL);

        units.clear();
        unitPrices.clear();
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(UNITS_TAG_NAME)) {
                units = new ArrayList<Unit>();
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    if (in.getLocalName().equals(Unit.getXMLElementTagName())) {
                        units.add(updateFreeColGameObject(in, Unit.class));
                    }
                }
            } else if (in.getLocalName().equals("unitPrice")) {
                String unitTypeId = in.getAttributeValue(null, "unitType");
                Integer price = new Integer(in.getAttributeValue(null, "price"));
                unitPrices.put(spec.getUnitType(unitTypeId), price);
                in.nextTag(); // close "unitPrice" tag
            }
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     * 
     * @return "europe".
     */
    public static String getXMLElementTagName() {
        return "europe";
    }

}
