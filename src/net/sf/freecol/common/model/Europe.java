package net.sf.freecol.common.model;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import net.sf.freecol.FreeCol;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.Specification;

import org.w3c.dom.Element;

/**
 * Represents Europe in the game. Each <code>Player</code> has it's own
 * <code>Europe</code>.
 * 
 * <br>
 * <br>
 * 
 * Europe is the place where you can {@link #recruit} and {@link #train} new
 * units. You may also sell/buy goods.
 */
public final class Europe extends FreeColGameObject implements Location,
        Ownable {

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(Europe.class
            .getName());

    /**
     * This array represents the types of the units that can be recruited in
     * Europe. They correspond to the slots that can be seen in the gui and that
     * can be used to communicate with the server/client. The array holds
     * exactly 3 elements and element 0 corresponds to recruit slot 1.
     */
    private UnitType[] recruitables = { null, null, null };

    private Hashtable<String, Integer> unitPrices = new Hashtable<String, Integer>();
    private int recruitPrice;
    private static final int RECRUIT_PRICE_INITIAL = 200;

    /**
     * Contains the units on this location.
     */
    private UnitContainer unitContainer;

    private Player owner;

    /**
     * Creates a new <code>Europe</code>.
     * 
     * @param game
     *            The <code>Game</code> in which this object belong.
     * @param owner
     *            The <code>Player</code> that will be using this object of
     *            <code>Europe</code>.
     */
    public Europe(Game game, Player owner) {
        super(game);
        this.owner = owner;

        unitContainer = new UnitContainer(game, this);

        setRecruitable(0, owner.generateRecruitable());
        setRecruitable(1, owner.generateRecruitable());
        setRecruitable(2, owner.generateRecruitable());

        recruitPrice = RECRUIT_PRICE_INITIAL;
    }

    /**
     * Initializes this object from an XML-representation of this object.
     * 
     * @param game
     *            The <code>Game</code> in which this object belong.
     * @param in
     *            The input stream containing the XML.
     * @throws XMLStreamException
     *             if an error occured during parsing.
     */
    public Europe(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        readFromXML(in);
    }

    /**
     * Initializes this object from an XML-representation of this object.
     * 
     * @param game
     *            The <code>Game</code> in which this object belong.
     * @param e
     *            An XML-element that will be used to initialize this object.
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
     * @param game
     *            The <code>Game</code> in which this object belong.
     * @param id
     *            The unique identifier for this object.
     */
    public Europe(Game game, String id) {
        super(game, id);
    }

    /**
     * Gets the type of the recruitable in Europe at the given slot.
     * 
     * @param slot
     *            The slot of the recruitable whose type needs to be returned.
     *            Should be 0, 1 or 2. NOTE - used to be 1, 2 or 3 and was
     *            called with 1-3 by some classes and 0-2 by others, the method
     *            itself expected 0-2.
     * @return The type of the recruitable in Europe at the given slot.
     * @exception IllegalArgumentException
     *                if the given <code>slot</code> does not exist.
     */
    public UnitType getRecruitable(int slot) {
        if ((slot >= 0) && (slot < 3)) {
            return recruitables[slot];
        }
        throw new IllegalArgumentException("Wrong recruitement slot: " + slot);
    }

    /**
     * Sets the type of the recruitable in Europe at the given slot to the given
     * type.
     * 
     * @param slot
     *            The slot of the recruitable whose type needs to be set. Should
     *            be 0, 1 or 2. NOTE - changed in order to match getRecruitable
     *            above!
     * @param type
     *            The new type for the unit at the given slot in Europe. Should
     *            be a valid unit type.
     */
    public void setRecruitable(int slot, UnitType type) {
        // Note - changed in order to match getRecruitable
        if (slot >= 0 && slot < 3) {
            recruitables[slot] = type;
        } else {
            logger.warning("setRecruitable: invalid slot(" + slot + ") given.");
        }
    }

    /**
     * Recruits a unit from Europe.
     * 
     * @param slot
     *            The slot the recruited unit(type) came from. This is needed
     *            for setting a new recruitable to this slot.
     * @param unit
     *            The recruited unit.
     * @param newRecruitable
     *            The recruitable that will fill the now empty slot.
     * @exception NullPointerException
     *                if <code>unit == null</code>.
     * @exception IllegalStateException
     *                if the player recruiting the unit cannot afford the price.
     */
    public void recruit(int slot, Unit unit, UnitType newRecruitable) {
        if (unit == null) {
            throw new NullPointerException();
        }

        if (getRecruitPrice() > unit.getOwner().getGold()) {
            throw new IllegalStateException();
        }

        unit.getOwner().modifyGold(-getRecruitPrice());
        incrementRecruitPrice();
        unit.setLocation(this);
        unit.getOwner().updateCrossesRequired();
        unit.getOwner().setCrosses(0);

        setRecruitable(slot, newRecruitable);
    }

    /**
     * Causes a unit to emigrate from Europe.
     * 
     * @param slot
     *            The slot the emigrated unit(type) came from. This is needed
     *            for setting a new recruitable to this slot.
     * @param unit
     *            The recruited unit.
     * @param newRecruitable
     *            The recruitable that will fill the now empty slot.
     * @exception NullPointerException
     *                If <code>unit == null</code>.
     * @exception IllegalStateException
     *                If there is not enough crosses to emigrate the
     *                <code>Unit</code>.
     */
    public void emigrate(int slot, Unit unit, UnitType newRecruitable) {
        if (unit == null) {
            throw new NullPointerException();
        }

        if (!unit.getOwner().checkEmigrate()) {
            throw new IllegalStateException(
                    "Not enough crosses to emigrate unit: "
                            + unit.getOwner().getCrosses() + "/"
                            + unit.getOwner().getCrossesRequired());
        }

        unit.setLocation(this);
        // TODO: shouldn't we subtract a certain amount of crosses instead of
        // just removing all
        // crosses? I'm not sure how this was done in the original.
        unit.getOwner().updateCrossesRequired();
        unit.getOwner().setCrosses(0);

        if (!unit.getOwner().hasFather(FreeCol.getSpecification().getFoundingFather("model.foundingFather.williamBrewster"))) {
            addModelMessage(this, "model.europe.emigrate", new String[][] { {
                    "%unit%", unit.getName() } }, ModelMessage.UNIT_ADDED, unit);
        }
        // In case William Brewster is in the congress we don't need to show a
        // message to the
        // user because he has already been busy picking a unit.

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
     * Adds a <code>Locatable</code> to this Location.
     * 
     * @param locatable
     *            The <code>Locatable</code> to add to this Location.
     */
    public void add(Locatable locatable) {
        if (!(locatable instanceof Unit)) {
            throw new IllegalArgumentException("Only units can be added to Europe.");
        }
        unitContainer.addUnit((Unit) locatable);
    }

    /**
     * Removes a <code>Locatable</code> from this Location.
     * 
     * @param locatable
     *            The <code>Locatable</code> to remove from this Location.
     */
    public void remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            unitContainer.removeUnit((Unit) locatable);
        } else {
            logger.warning("Tried to remove an unrecognized 'Locatable' from a europe.");
        }
    }

    /**
     * Checks if the specified <code>Locatable</code> is at this
     * <code>Location</code>.
     * 
     * @param locatable
     *            The <code>Locatable</code> to test the presence of.
     * @return The result.
     */
    public boolean contains(Locatable locatable) {
        if (locatable instanceof Unit) {
            return unitContainer.contains((Unit) locatable);
        }

        return false;
    }

    public GoodsContainer getGoodsContainer() {
        return null;
    }

    /**
     * Checks wether or not the specified locatable may be added to this
     * <code>Location</code>.
     * 
     * @param locatable
     *            The <code>Locatable</code> to test the addabillity of.
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
        return unitContainer.getUnitCount();
    }

    /**
     * Gets a <code>List</code> of every <code>Unit</code> directly located
     * in this <code>Europe</code>. This does not include <code>Unit</code>s
     * on ships.
     * 
     * @return The <code>List</code>.
     */
    public List<Unit> getUnitList() {
        return unitContainer.getUnitsClone();
    }

    /**
     * Gets an <code>Iterator</code> of every <code>Unit</code> directly
     * located in this <code>Europe</code>. This does not include
     * <code>Unit</code>s on ships.
     * 
     * @return The <code>Iterator</code>.
     */
    public Iterator<Unit> getUnitIterator() {
        return getUnitList().iterator();
    }

    /**
     * Gets the first <code>Unit</code> in this <code>Europe</code>.
     * 
     * @return The first <code>Unit</code> in this <code>Europe</code>.
     */
    public Unit getFirstUnit() {
        return unitContainer.getFirstUnit();
    }

    /**
     * Gets the last <code>Unit</code> in this <code>Europe</code>.
     * 
     * @return The last <code>Unit</code> in this <code>Europe</code>.
     */
    public Unit getLastUnit() {
        return unitContainer.getLastUnit();
    }

    /**
     * Returns the price of a unit in Europe.
     * 
     * @param unitType The type of unit of which you need the price.
     * @return The price of this unit when trained in Europe. 'UnitType.UNDEFINED' is returned
     *         in case the unit cannot be bought.
     */
    public int getUnitPrice(UnitType unitType) {
        Integer price = unitPrices.get(unitType.getID());
        if (price != null) {
            return price.intValue();
        } else {
            return unitType.getPrice();
        }
    }

    /**
     * Trains a unit in Europe.
     * 
     * @param unit
     *            The trained unit.
     * @exception NullPointerException
     *                if <code>unit == null</code>.
     * @exception IllegalArgumentException
     *                if the unit to be trained doesn't have price
     * @exception IllegalStateException
     *                if the player recruiting the unit cannot afford the price.
     */
    public void train(Unit unit) {
        if (unit == null) {
            throw new NullPointerException();
        }

        int price = getUnitPrice(unit.getUnitType());
        if (price <= 0) {
            throw new IllegalArgumentException();
        }
        
        if (getUnitPrice(unit.getUnitType()) > unit.getOwner().getGold()) {
            throw new IllegalStateException();
        }

        unit.getOwner().modifyGold(price);
        unit.setLocation(this);

        int increasingPrice = unit.getUnitType().getIncreasingPrice();
        if (increasingPrice > 0) {
            unitPrices.put(unit.getUnitType().getID(), new Integer(price + increasingPrice));
        }
    }

    /**
     * Gets the current price for a recruit.
     * 
     * @return The current price of the recruit in this <code>Europe</code>.
     */
    public int getRecruitPrice() {
        int required = owner.getCrossesRequired();
        int crosses = owner.getCrosses();
        int difference = Math.max(required - crosses, 0);
        return Math.max((recruitPrice * difference) / required, 80);
    }

    private void incrementRecruitPrice() {
        recruitPrice += 20 + getOwner().getDifficulty() * 10;
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
     * @param p
     *            The <code>Player</code> that should take ownership of this
     *            {@link Ownable}.
     * @exception UnsupportedOperationException
     *                is always thrown by this method.
     */
    public void setOwner(Player p) {
        throw new UnsupportedOperationException();
    }

    /**
     * Prepares this object for a new turn.
     */
    public void newTurn() {
        // Repair any damaged ships:
        for (Unit unit : getUnitList()) {
            if (unit.isNaval() && unit.isUnderRepair()) {
                unit.setHitpoints(unit.getHitpoints() + 1);
                if (!unit.isUnderRepair()) {
                    addModelMessage(this, "model.unit.shipRepaired",
                                    new String[][] {
                                        { "%unit%", unit.getName() },
                                        { "%repairLocation%", getLocationName() } },
                                    ModelMessage.DEFAULT, this);
                }
            }
        }
    }

    /**
     * Returns the name of this location.
     * 
     * @return The name of this location.
     */
    public String getLocationName() {
        return toString();
    }

    /**
     * Returns the name of the owner's home port.
     * 
     * @return The name of this location.
     */
    public String getName() {
        return toString();
    }

    /**
     * Returns a suitable name.
     */
    public String toString() {
        switch (getOwner().getNation()) {
        case Player.DUTCH:
            return Messages.message("model.nation.Dutch.Europe");
        case Player.ENGLISH:
            return Messages.message("model.nation.English.Europe");
        case Player.FRENCH:
            return Messages.message("model.nation.French.Europe");
        case Player.SPANISH:
            return Messages.message("model.nation.Spanish.Europe");
        default:
            return "Europe";
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
     * @param out
     *            The target stream.
     * @param player
     *            The <code>Player</code> this XML-representation should be
     *            made for, or <code>null</code> if
     *            <code>showAll == true</code>.
     * @param showAll
     *            Only attributes visible to <code>player</code> will be added
     *            to the representation if <code>showAll</code> is set to
     *            <i>false</i>.
     * @param toSavedGame
     *            If <code>true</code> then information that is only needed
     *            when saving a game is added.
     * @throws XMLStreamException
     *             if there are any problems writing to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out, Player player,
            boolean showAll, boolean toSavedGame) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("ID", getID());
        out.writeAttribute("recruit0", Integer.toString(recruitables[0].getIndex()));
        out.writeAttribute("recruit1", Integer.toString(recruitables[1].getIndex()));
        out.writeAttribute("recruit2", Integer.toString(recruitables[2].getIndex()));
        out.writeAttribute("recruitPrice", Integer.toString(recruitPrice));
        out.writeAttribute("owner", owner.getID());
        Enumeration<String> keys = unitPrices.keys();
        while (keys.hasMoreElements()) {
            String unitType = keys.nextElement();
            out.writeStartElement("unitPrice");
            out.writeAttribute("unitType", unitType);
            out.writeAttribute("price", unitPrices.get(unitType).toString());
            out.writeEndElement();
        }

        unitContainer.toXML(out, player, showAll, toSavedGame);

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * 
     * @param in
     *            The input stream with the XML.
     * @throws XMLStreamException
     *             if a problem was encountered during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
            throws XMLStreamException {
        setID(in.getAttributeValue(null, "ID"));

        Specification spec = FreeCol.getSpecification();
        recruitables[0] = spec.getUnitType(Integer.parseInt(in.getAttributeValue(null, "recruit0")));
        recruitables[1] = spec.getUnitType(Integer.parseInt(in.getAttributeValue(null, "recruit1")));
        recruitables[2] = spec.getUnitType(Integer.parseInt(in.getAttributeValue(null, "recruit2")));
        String recruitPriceString = in.getAttributeValue(null, "recruitPrice");
        owner = (Player) getGame().getFreeColGameObject(
                in.getAttributeValue(null, "owner"));
        if (owner == null) {
            owner = new Player(getGame(), in.getAttributeValue(null, "owner"));
        }
        if (recruitPriceString != null)
            recruitPrice = Integer.parseInt(recruitPriceString);
        else
            recruitPrice = RECRUIT_PRICE_INITIAL;

        unitPrices.clear();
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(UnitContainer.getXMLElementTagName())) {
                unitContainer = (UnitContainer) getGame().getFreeColGameObject(
                        in.getAttributeValue(null, "ID"));
                if (unitContainer != null) {
                    unitContainer.readFromXML(in);
                } else {
                    unitContainer = new UnitContainer(getGame(), this, in);
                }
            } else if (in.getLocalName().equals("unitPrice")) {
                String unitType = in.getAttributeValue(null, "unitType");
                Integer price = new Integer(in.getAttributeValue(null, "price"));
                unitPrices.put(unitType, price);
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

    /**
     * Europe does not belong to a colony.
     * 
     * @return Always returns null.
     */
    public Colony getColony() {
        return null;
    }

}
