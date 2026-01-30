/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.util.Utils;


/**
 * Represents locatable goods of a specified type and amount. Use
 * AbstractGoods to represent abstract or potential goods that need
 * not be present in any particular location.
 *
 * @see AbstractGoods
 */
public class Goods extends AbstractGoods implements Locatable, Ownable {

    public static final String TAG = "goods";


    /** 
     * The game containing these goods.  It would be nice to make this
     * a FreeColGameObject, but then it could not extend AbstractGoods.
     */
    private final Game game;

    /**
     * The location of these goods. Never null.
     * If goods are temporarily unassigned (e.g. during looting),
     * this is {@link LootLocation#INSTANCE}.
     */
    private Location location;


    /**
     * Fundamental constructor.
     *
     * @param game The enclosing {@code Game}.
     */
    private Goods(Game game) {
        if (game == null) throw new RuntimeException("Null game: " + this);
        this.game = game;
    }
    
    /**
     * Creates a standard {@code Goods}-instance given the place where
     * the goods is.
     *
     * Used by Game.newInstance.
     *
     * @param game The enclosing {@code Game}.
     * @param id The identifier (ignored, type gives identifier here).
     */
    public Goods(Game game, @SuppressWarnings("unused") String id) {
        this(game);
    }
    
    /**
     * Creates a standard {@code Goods}-instance given the place
     * where the goods is.
     *
     * This constructor only asserts that the game and that the
     * location (if given) can store goods. The goods will not be
     * added to the location (use Location.add for this).
     *
     * @param game The enclosing {@code Game}.
     * @param location The {@code Location} of the goods (may be null).
     * @param type The {@code GoodsType} for the goods.
     * @param amount The amount of the goods.
     */
    public Goods(Game game, Location location, GoodsType type, int amount) {
        this(game);
        if (type == null) {
            throw new RuntimeException("Null type: " + this);
        }
        if (location != null && location.getGoodsContainer() == null) {
            throw new RuntimeException("Can not store goods at: " + location);
        }

        setId(type.getId());
        setType(type);
        setAmount(amount);
        this.location = (location == null) ? LootLocation.INSTANCE : location;
    }

    /**
     * Creates a new {@code Goods} instance.
     *
     * @param game The enclosing {@code Game}.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if an error occurs
     */
    public Goods(Game game, FreeColXMLReader xr) throws XMLStreamException {
        this(game);

        readFromXML(xr);
    }


    /**
     * If the amount of goods is greater than the container can hold,
     * then this method adjusts the amount to the maximum amount possible.
     */
    public void adjustAmount() {
        final GoodsContainer gc = this.location.getGoodsContainer();
        if (gc != null) {
            int maxAmount = gc.getGoodsCount(getType());
            if (getAmount() > maxAmount) setAmount(maxAmount);
        }
    }

    /**
     * Returns whether these goods are in a temporary looting state.
     *
     * @return true if the location is a {@link LootLocation}.
     */
    public boolean isLoot() {
        return this.location instanceof LootLocation;
    }


    // Interface Locatable

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getLocation() {
        return this.location;
    }

    /**
     * @param location The Settlement that is located on this Tile
     */
    @Override
    public boolean setLocation(Location location) {
        this.location = (location == null) ? LootLocation.INSTANCE : location;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInEurope() {
        return (this.location instanceof Europe)
            || (this.location instanceof Unit
                && ((Unit)this.location).isInEurope());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tile getTile() {
        return this.location.getTile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSpaceTaken() {
        return 1;
    }


    // Interface Ownable

    /**
     * {@inheritDoc}
     */
    @Override
    public Player getOwner() {
        Location loc = this.location;
        return (loc instanceof Ownable) ? ((Ownable) loc).getOwner() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOwner(Player p) {
        throw new UnsupportedOperationException();
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public Specification getSpecification() {
        return getGame().getSpecification();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSpecification(Specification specification) {
        throw new RuntimeException("Can not set specification: " + this);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Game getGame() {
        return this.game;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGame(Game game) {
        throw new RuntimeException("Can not set game: " + this);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColObject getDisplayObject() {
        return getType();
    }

    
    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        Goods o = copyInCast(other, Goods.class);
        if (o == null || !super.copyIn(o)) return false;
        //Game can not change.  No: this.game = o.getGame();
        Location newLoc = getGame().updateLocationRef(o.getLocation());
        this.location = (newLoc == null) ? LootLocation.INSTANCE : newLoc;
        return true;
    }


    // Serialization

    private static final String AMOUNT_TAG = "amount";
    private static final String LOCATION_TAG = "location";
    private static final String TYPE_TAG = "type";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(TYPE_TAG, this.type);

        xw.writeAttribute(AMOUNT_TAG, this.amount);

        xw.writeLocationAttribute(LOCATION_TAG, this.location);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();

        super.readAttributes(xr);

        this.type = xr.getType(spec, TYPE_TAG, GoodsType.class, (GoodsType)null);
        if (this.type == null) {
            throw new XMLStreamException("Null goods type: " + this);
        } else {
            setId(this.type.getId());
        }

        this.amount = xr.getAttribute(AMOUNT_TAG, 0);

        Location loc = xr.getLocationAttribute(game, LOCATION_TAG, true);
        this.location = (loc == null) ? LootLocation.INSTANCE : loc;
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Goods)) return false;
        Goods other = (Goods) o;
        return super.equals(other)
            && Utils.equals(this.location, other.location);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        return 31 * hash + Utils.hashCode(this.location);
    }
}
