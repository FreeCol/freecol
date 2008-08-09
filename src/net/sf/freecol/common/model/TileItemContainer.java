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
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;

import org.w3c.dom.Element;

/**
 * Contains <code>TileItem</code>s and can be used by a {@link Tile}
 * to make certain tasks easier.
 */
public class TileItemContainer extends FreeColGameObject {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(Location.class.getName());

    /** The list of TileItems stored in this <code>TileItemContainer</code>. */
    private final List<TileImprovement> improvements = new ArrayList<TileImprovement>();
    private Resource resource = null;

    /** Quick Pointers */
    private TileImprovement road = null;
    private TileImprovement river = null;

    /** The owner of this <code>TileItemContainer</code>. */
    private Tile tile;

    // ------------------------------------------------------------ constructor

    /**
     * Creates an empty <code>TileItemContainer</code>.
     *
     * @param game The <code>Game</code> in which this <code>TileItemContainer</code> belong.
     * @param tile The <code>Tile</code> this <code>TileItemContainer</code> will be containg TileItems for.
     */
    public TileItemContainer(Game game, Tile tile) {
        super(game);

        if (tile == null) {
            throw new IllegalArgumentException("Tile must not be 'null'.");
        }

        this.tile = tile;
    }

    /**
     * Initiates a new <code>TileItemContainer</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this <code>TileItemContainer</code>
     *       belong.
     * @param tile The <code>Tile</code> using this <code>TileItemContainer</code>
     *       for storing it's TileItem.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public TileItemContainer(Game game, Tile tile, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        if (tile == null) {
            throw new IllegalArgumentException("Tile must not be 'null'.");
        }

        this.tile = tile;
        readFromXML(in);
    }

    /**
     * Initiates a new <code>TileItemContainer</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this <code>TileItemContainer</code>
     *       belong.
     * @param tile The <code>Tile</code> using this <code>TileItemContainer</code>
     *       for storing it's TileItem.
     * @param e An XML-element that will be used to initialize
     *      this object.
     */
    public TileItemContainer(Game game, Tile tile, Element e) {
        super(game, e);

        if (tile == null) {
            throw new IllegalArgumentException("Tile must not be 'null'.");
        }

        this.tile = tile;
        readFromXMLElement(e);
    }

    /**
     * Clone functions for making a clone of this TileItemContainer
     */
    public TileItemContainer clone() {
        return clone(true, false);
    }
    public TileItemContainer clone(boolean importBonuses) {
        return clone(importBonuses, false);
    }
    public TileItemContainer clone(boolean importBonuses, boolean copyOnlyNatural) {
        TileItemContainer ticClone = new TileItemContainer(getGame(), getTile());
        ticClone.copyFrom(this, importBonuses, copyOnlyNatural);
        return ticClone;
    }

    // ------------------------------------------------------------ checking/retrieval functions

    public Tile getTile() {
        return tile;
    }

    public boolean hasRoad() {
        return (road != null && road.isComplete());
    }

    public boolean hasRiver() {
        return (river != null);
    }

    public boolean hasResource() {
        return (resource != null);
    }

    public TileImprovement getRoad() {
        return road;
    }

    public TileImprovement getRiver() {
        return river;
    }

    public Resource getResource() {
        return resource;
    }

    public void clearResource() {
        if (hasResource()) {
            resource.dispose();
            resource = null;
        }
    }

    public void clear() {
        clearResource();
        for (TileImprovement t : improvements) {
            t.dispose();
        }
        improvements.clear();
        road = null;
        river = null;
    }

    /**
     * Remove improvements incompatible with the given TileType. This
     * method is called whenever the type of the container's tile
     * changes, i.e. due to clearing.
     */
    public void removeIncompatibleImprovements() {
        TileType tileType = tile.getType();
        Iterator<TileImprovement> improvementIterator = getImprovementIterator();
        while (improvementIterator.hasNext()) {
            TileImprovement improvement = improvementIterator.next();
            if (!improvement.getType().isTileTypeAllowed(tileType)) {
                improvementIterator.remove();
                if (improvement == road) {
                    road = null;
                } else if (improvement == river) {
                    river = null;
                }
                improvement.dispose();
            }
        }
        // TODO: we could discover another one
        clearResource();
    }

    /**
     * Gets an <code>Iterator</code> of every <code>TileImprovement</code> in this
     * <code>TileItemContainer</code>.
     *
     * @return The <code>Iterator</code>.
     */
    public Iterator<TileImprovement> getImprovementIterator() {
        return getImprovements().iterator();
    }

    /**
     * Returns a <code>List</code> of the <code>TileImprovement</code>s
     * in this <code>TileItemContainer</code>.
     *
     * @return The <code>List</code>.
     * @see #getImprovementIterator
     */
    public List<TileImprovement> getImprovements() {
        return improvements;
    }

    /**
     * Determine the total bonus from all Improvements
     * @return The total bonus
     */
    public int getImprovementBonusPotential(GoodsType g) {
        int bonus = 0;
        for (TileImprovement ti : improvements) {
            bonus += ti.getBonus(g);
        }
        return bonus;
    }

    /**
     * Determine the total bonus from Resource if any
     * @return The total bonus
     */
    public int getResourceBonusPotential(GoodsType g, int potential) {
        if (hasResource()) {
            potential = resource.getBonus(g, potential);
        }
        return potential;
    }

    /**
     * Determine the total bonus for a GoodsType. Checks Resource and all Improvements.
     * @return The total bonus
     */
    public int getTotalBonusPotential(GoodsType g, int tilePotential) {
        int potential = tilePotential + getImprovementBonusPotential(g);
        potential = getResourceBonusPotential(g, potential);
        return potential;
    }

    /**
     * Describe <code>getProductionBonus</code> method here.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return a <code>Modifier</code> value
     */
    public Set<Modifier> getProductionBonus(GoodsType goodsType) {
        Set<Modifier> result = new HashSet<Modifier>();
        for (TileImprovement improvement : improvements) {
            result.add(improvement.getProductionBonus(goodsType));
        }
        return result;
    }

    /**
     * Determine the movement cost to this <code>Tile</code> from another <code>Tile</code>.
     * Does not consider special unit abilities.
     * @return The movement cost
     */
    public int getMoveCost(int basicMoveCost, Tile fromTile) {
        // Go through the TileImprovements to find reductions to Movement Cost
        int moveCost = basicMoveCost;
        for (TileImprovement ti : improvements) {
            moveCost = ti.getMovementCost(moveCost, fromTile);
        }       
        return moveCost;
    }

    /**
     * Returns a description of the tile, with the name of the tile
     * and any improvements made to it (road/plow)
     * @param separator The separator to be used (e.g. "/")
     * @return The description label for this tile
     */
    public String getLabel(String separator) {
        String label = new String();
        for (TileImprovement ti : improvements) {
            label += separator + Messages.message(ti.getName());
        }
        return label;
    }

    public String getLabel() {
        return getLabel("/");
    }

    // ------------------------------------------------------------ add/remove from container

    /**
     * Adds a <code>TileItem</code> to this container.
     * @param t The TileItem to add to this container.
     * @return The added TileItem or the existing TileItem or <code>null</code> on error
     */
    public TileItem addTileItem(TileItem t) {
        if (t == null) {
            return null;
        }
        if (t instanceof Resource) {
            // Disposes existing resource and replaces with new one
            if (resource != null) {
                resource.dispose();
            }
            resource = (Resource) t;
            return t;
        } else if (t instanceof TileImprovement) {
            TileImprovement improvement = (TileImprovement) t;
            // Check all improvements to find any to replace
            String typeId = improvement.getType().getId();
            if (typeId != null) {
                Iterator<TileImprovement> ti = improvements.iterator();
                while (ti.hasNext()) {
                    TileImprovement imp = ti.next();
                    if (imp.getType().getId().equals(typeId)) {
                        if (imp.getMagnitude() < improvement.getMagnitude()) {
                            removeTileItem(imp);
                            break;
                        } else {
                            // Found it, but not replacing.
                            return imp;
                        }
                    }
                }
            }
            if (improvement.isRoad()) {
                road = improvement;
            } else if (improvement.isRiver()) {
                river = improvement;
            }
            improvements.add(improvement);
            return improvement;
        } else {
            logger.warning("TileItem " + t.getClass().getSimpleName() + " has not be implemented yet.");
            return null;
        }
    }

    /**
     * Removes TileItem from this container.
     *
     * @param t The TileItem to remove from this container.
     * @return The TileItem that has been removed from this container (if any).
     */
    public TileItem removeTileItem(TileItem t) {
        if (t == null) {
            return null;
        }
        if (t instanceof Resource && resource == t) {
            resource = null;
            return t;
        } else if (t instanceof TileImprovement) {
            if (river == t) {
                river = null;
            } else if (road == t) {
                road = null;
            }
            return (improvements.remove(t)) ? t : null;
        } else {
            logger.warning("TileItem " + t.getClass().getSimpleName() + " has not be implemented yet.");
            return null;
        }
    }
    
    public void copyFrom(TileItemContainer tic) {
        copyFrom(tic, true, false);
    }
    public void copyFrom(TileItemContainer tic, boolean importBonuses) {
        copyFrom(tic, importBonuses, false);
    }
    public void copyFrom(TileItemContainer tic, boolean importBonuses, boolean copyOnlyNatural) {
        clear();
        if (tic.hasResource() && importBonuses) {
            Resource ticR = tic.getResource();
            Resource r = new Resource(getGame(), tile, ticR.getType());
            r.setQuantity(ticR.getQuantity());
            addTileItem(r);
        }
        for (TileImprovement ti : tic.getImprovements()) {
            if (!copyOnlyNatural || ti.getType().isNatural()) {
                TileImprovement newTI = new TileImprovement(getGame(), tile, ti.getType());
                newTI.setMagnitude(ti.getMagnitude());
                newTI.setStyle(ti.getStyle());
                newTI.setTurnsToComplete(ti.getTurnsToComplete());
                addTileItem(newTI);
            }
        }
    }

    /**
     * Removes all TileItems.
     */
    public void removeAll() {
        clear();
    }

    /**
     * Checks if the specified <code>TileItem</code> is in this container.
     *
     * @param t The <code>TileItem</code> to test the presence of.
     * @return The result.
     */
    public boolean contains(TileItem t) {
        if (t instanceof Resource) {
            return ((Resource) t) == resource;
        } else if (t instanceof TileImprovement) {
            return (improvements.indexOf((TileImprovement) t) >= 0);
        }
        return false;
    }

    /**
     * Checks if a TileImprovement of this Type is already in this container.
     *
     * @param type The <code>TileImprovementType</code> to test the presence of.
     * @return The result.
     */
    public TileImprovement findTileImprovementType(TileImprovementType type) {
        for (TileImprovement ti : improvements) {
            if (ti.getType() == type) {
                return ti;
            }
        }
        return null;
    }
    
    /**
     * Will check whether this tile has a completed improvement of the given
     * type.
     * 
     * Useful for checking whether the tile for instance has a road or is
     * plowed.
     * 
     * @param type
     *            The type to check for.
     * @return Whether the tile has the improvement and the improvement is
     *         completed.
     */
    public boolean hasImprovement(TileImprovementType type) {
        TileImprovement improvement = findTileImprovementType(type);
        return improvement != null && improvement.isComplete();
    }

    /**
     * Removes all references to this object.
     */
    public void dispose() {
        clear();
        super.dispose();
    }

    // ------------------------------------------------------------ manipulation methods

    /**
     * Creates a river <code>TileImprovement</code> and adds to this Tile/Container.
     * Checking for overwrite is done by {@link #addTileItem}.
     * @param magnitude The Magnitude of the river to be created
     * @param style an <code>int</code> value
     * @return The new river added, or the existing river TileImprovement
     */
    public TileImprovement addRiver(int magnitude, int style) {
        if (magnitude == TileImprovement.NO_RIVER) {
            return null;
        }
        if (!hasRiver()) {
            river = new TileImprovement(getGame(), tile, FreeCol.getSpecification()
                                        .getTileImprovementType("model.improvement.River"));
            addTileItem(river);
        }
        river.setMagnitude(magnitude);
        river.setStyle(style);
        return river;
    }

    /**
     * Removes the river <code>TileImprovement</code> from this Tile/Container.
     * Change neighbours' River Style with {@link #adjustNeighbourRiverStyle}.
     */
    public TileImprovement removeRiver() {
        return (TileImprovement) removeTileItem(river);
    }

    public int getRiverStyle() {
        if (river == null) {
            return 0;
        }
        return river.getStyle();
    }

    /**
     * Disposes all <code>TileItem</code>s in this <code>TileItemContainer</code>.
     */
    public void disposeAllTileItems() {
        clear();
    }

    // ------------------------------------------------------------ API methods

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
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame) 
        throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("ID", getId());
        out.writeAttribute("tile", tile.getId());

        if (hasResource()) {
            resource.toXML(out, player, showAll, toSavedGame);
        }

        Iterator<TileImprovement> ti = getImprovementIterator();
        while (ti.hasNext()) {
            TileImprovement t = ti.next();
            t.toXML(out, player, showAll, toSavedGame);
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));

        tile = (Tile) getGame().getFreeColGameObject(in.getAttributeValue(null, "tile"));
        if (tile == null) {
            tile = new Tile(getGame(), in.getAttributeValue(null, "tile"));
        }

        improvements.clear();

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(Resource.getXMLElementTagName())) {
                resource = (Resource) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (resource == null) {
                    resource = new Resource(getGame(), in);
                } else {
                    resource.readFromXML(in);
                }
            } else if (in.getLocalName().equals(TileImprovement.getXMLElementTagName())) {
                TileImprovement ti = (TileImprovement) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (ti == null) {
                    ti = new TileImprovement(getGame(), in);
                    improvements.add(ti);
                } else {
                    ti.readFromXML(in);
                    if (!improvements.contains(ti)) {
                        improvements.add(ti);
                    }
                }
                if (ti.isRoad()) {
                    road = ti;
                } else if (ti.isRiver()) {
                    river = ti;
                }
            }
        }
    }


    /**
     * Gets the tag name of the root element representing this object.
     * @return "tileitemcontainer".
     */
    public static String getXMLElementTagName() {
        return "tileitemcontainer";
    }
    
    
    /**
     * Creates a <code>String</code> representation of this
     * <code>TileItemContainer</code>.    
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(60);
        sb.append("TileItemContainer with: ");
        if (hasResource()) {
            sb.append(resource.toString() + ", ");
        }
        for (TileImprovement i : improvements) {
            sb.append(i.toString() + ", ");
        }
        return sb.toString();
    }

}
