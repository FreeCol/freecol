/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.util.Comparator;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.PlayerExploredTile;

import org.w3c.dom.Element;

/**
 * Contains <code>TileItem</code>s and can be used by a {@link Tile}
 * to make certain tasks easier.
 */
public class TileItemContainer extends FreeColGameObject {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(TileItemContainer.class.getName());

    /**
     * The owner of this <code>TileItemContainer</code>.
     */
    private Tile tile;

    /**
     * All tile items sorted by zIndex.
     */
    private List<TileItem> tileItems = new ArrayList<TileItem>();

    // sort tile items ascending by zIndex
    private final Comparator<TileItem> tileItemComparator = new Comparator<TileItem>() {
        public int compare(TileItem tileItem1, TileItem tileItem2) {
            return tileItem1.getZIndex() - tileItem2.getZIndex();
        }
    };


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

    /**
     * Get the <code>TileItems</code> value.
     *
     * @return a <code>List<TileItem></code> value
     */
    public final List<TileItem> getTileItems() {
        return tileItems;
    }

    /**
     * Set the <code>TileItems</code> value.
     *
     * @param newTileItems The new TileItems value.
     */
    public final void setTileItems(final List<TileItem> newTileItems) {
        this.tileItems = newTileItems;
    }

    public Resource getResource() {
        for (TileItem item : tileItems) {
            if (item instanceof Resource) {
                return (Resource) item;
            }
        }
        return null;
    }

    public TileImprovement getRoad() {
        for (TileItem item : tileItems) {
            if (item instanceof TileImprovement && ((TileImprovement) item).isRoad()) {
                return (TileImprovement) item;
            }
        }
        return null;
    }

    public TileImprovement getRiver() {
        for (TileItem item : tileItems) {
            if (item instanceof TileImprovement && ((TileImprovement) item).isRiver()) {
                return (TileImprovement) item;
            }
        }
        return null;
    }

    /**
     * Get the <code>LostCityRumour</code> value.
     *
     * @return a <code>LostCityRumour</code> value
     */
    public final LostCityRumour getLostCityRumour() {
        for (TileItem item : tileItems) {
            if (item instanceof LostCityRumour) {
                return (LostCityRumour) item;
            }
        }
        return null;
    }

    public void clear() {
        for (TileItem item : tileItems) {
            item.dispose();
        }
        tileItems.clear();
    }

    /**
     * Remove improvements incompatible with the given TileType. This
     * method is called whenever the type of the container's tile
     * changes, i.e. due to clearing.
     */
    public void removeIncompatibleImprovements() {
        TileType tileType = tile.getType();
        Iterator<TileItem> iterator = tileItems.iterator();
        while (iterator.hasNext()) {
            TileItem item = iterator.next();
            if ((item instanceof TileImprovement
                 && !((TileImprovement) item).getType().isTileTypeAllowed(tileType))
                || (item instanceof Resource
                    && !tileType.canHaveResourceType(((Resource) item).getType()))) {
                iterator.remove();
                item.dispose();
            }
        }
    }

    /**
     * Returns a <code>List</code> of the <code>TileImprovement</code>s
     * in this <code>TileItemContainer</code>.
     *
     * @return The <code>List</code>.
     */
    public List<TileImprovement> getImprovements() {
        return getImprovements(false);
    }
        
    /**
     * Returns a <code>List</code> of the completed
     * <code>TileImprovement</code>s in this
     * <code>TileItemContainer</code>.
     *
     * @return The <code>List</code>.
     */
    public List<TileImprovement> getCompletedImprovements() {
        return getImprovements(true);
    }

    /**
     * Returns a <code>List</code> of the <code>TileImprovement</code>s
     * in this <code>TileItemContainer</code>.
     *
     * @return The <code>List</code>.
     */
    private List<TileImprovement> getImprovements(boolean completedOnly) {
        List<TileImprovement> improvements = new ArrayList<TileImprovement>();
        for (TileItem item : tileItems) {
            if (item instanceof TileImprovement
                && (!completedOnly || ((TileImprovement) item).isComplete())) {
                improvements.add((TileImprovement) item);
            }
        }
        return improvements;
    }

    /**
     * Determine the total bonus for a GoodsType. Checks Resource and all Improvements.
     * @param g a <code>GoodsType</code> value
     * @param unitType an <code>UnitType</code> value
     * @param tilePotential an <code>int</code> value
     * @return The total bonus
     */
    public int getTotalBonusPotential(GoodsType g, UnitType unitType, int tilePotential) {
        int potential = tilePotential;
        int improvementBonus = 0;
        for (TileItem item : tileItems) {
            if (item instanceof TileImprovement) {
                improvementBonus += ((TileImprovement) item).getBonus(g);
            } else if (item instanceof Resource) {
                potential = ((Resource) item).getBonus(g, unitType, potential);
            }
        }
        if (potential > 0) {
            potential += improvementBonus;
        }
        return potential;
    }

    /**
     * Describe <code>getProductionBonus</code> method here.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @param unitType a <code>UnitType</code> value
     * @return a <code>Modifier</code> value
     */
    public Set<Modifier> getProductionBonus(GoodsType goodsType, UnitType unitType) {
        Set<Modifier> result = new HashSet<Modifier>();
        for (TileItem item : tileItems) {
            if (item instanceof Resource) {
                result.addAll(((Resource) item).getType().getProductionModifier(goodsType, unitType));
            } else if (item instanceof TileImprovement) {
                Modifier modifier = ((TileImprovement) item).getProductionModifier(goodsType);
                if (modifier != null) {
                    result.add(modifier);
                }
            }
        }
        return result;
    }

    /**
     * Determine the movement cost to this <code>Tile</code> from another <code>Tile</code>.
     * Does not consider special unit abilities.
     *
     * @param basicMoveCost an <code>int</code> value
     * @param fromTile a <code>Tile</code> value
     * @return The movement cost
     */
    public int getMoveCost(int basicMoveCost, Tile fromTile) {
        int moveCost = basicMoveCost;
        for (TileItem item : tileItems) {
            if (item instanceof TileImprovement) {
                moveCost = ((TileImprovement) item).getMovementCost(moveCost, fromTile);
            }
        }
        return moveCost;
    }

    /**
     * Returns a description of the tile, with the name of the tile
     * and any improvements made to it (road/plow)
     *
     * @return The description label for this tile
     */
    public StringTemplate getLabel() {
        StringTemplate label = StringTemplate.label("/");
        for (TileItem item : tileItems) {
            if (item instanceof Resource) {
                label.add(((Resource) item).getType().getNameKey());
            } else if (item instanceof TileImprovement
                       && ((TileImprovement) item).isComplete()) {
                label.add(((TileImprovement) item).getType().getNameKey());
            }
        }
        return label;
    }

    // ------------------------------------------------------------ add/remove from container

    /**
     * Adds a <code>TileItem</code> to this container.
     *
     * @param item The TileItem to add to this container.
     * @return The added TileItem or the existing TileItem or <code>null</code> on error
     */
    public TileItem addTileItem(TileItem item) {
        if (item == null) {
            return null;
        } else {
            for (int index = 0; index < tileItems.size(); index++) {
                TileItem oldItem = tileItems.get(index);
                if (item instanceof TileImprovement
                    && oldItem instanceof TileImprovement
                    && ((TileImprovement) oldItem).getType().getId()
                    .equals(((TileImprovement) item).getType().getId())) {
                    if (((TileImprovement) oldItem).getMagnitude() < ((TileImprovement) item).getMagnitude()) {
                        tileItems.set(index, item);
                        oldItem.dispose();
                        return item;
                    } else {
                        // Found it, but not replacing.
                        return oldItem;
                    }
                } else if (oldItem.getZIndex() > item.getZIndex()) {
                    tileItems.add(index, item);
                    return item;
                }
            }
            tileItems.add(item);
            return item;
        }
    }

    /**
     * Removes TileItem from this container.
     *
     * @param item The TileItem to remove from this container.
     * @return The TileItem that has been removed from this container (if any).
     */
    public TileItem removeTileItem(TileItem item) {
        return (tileItems.remove(item) ? item : null);
    }

    public void removeAll(Class<?> c) {
        Iterator<TileItem> iterator = tileItems.iterator();
        while (iterator.hasNext()) {
            if (c.isInstance(iterator.next())) {
                iterator.remove();
            }
        }
    }
    
    public void copyFrom(TileItemContainer tic) {
        copyFrom(tic, true, false);
    }
    public void copyFrom(TileItemContainer tic, boolean importResources) {
        copyFrom(tic, importResources, false);
    }
    public void copyFrom(TileItemContainer tic, boolean importResources, boolean copyOnlyNatural) {
        tileItems.clear();
        for (TileItem item : tic.getTileItems()) {
            if (item instanceof Resource) {
                if (importResources) {
                    Resource ticR = (Resource) item;
                    Resource r = new Resource(getGame(), tile, ticR.getType(), ticR.getQuantity());
                    tileItems.add(r);
                }
            } else if (item instanceof LostCityRumour && !copyOnlyNatural) {
                LostCityRumour ticR = (LostCityRumour) item;
                LostCityRumour r = new LostCityRumour(getGame(), tile, ticR.getType(), ticR.getName());
                addTileItem(r);
            } else if (item instanceof TileImprovement) {
                if (!copyOnlyNatural || ((TileImprovement) item).getType().isNatural()) {
                    TileImprovement ti = (TileImprovement) item;
                    TileImprovement newTI = new TileImprovement(getGame(), tile, ti.getType());
                    newTI.setMagnitude(ti.getMagnitude());
                    newTI.setStyle(ti.getStyle());
                    newTI.setTurnsToComplete(ti.getTurnsToComplete());
                    addTileItem(newTI);
                }
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
        return tileItems.contains(t);
    }

    /**
     * Checks if a TileImprovement of this Type is already in this container.
     *
     * @param type The <code>TileImprovementType</code> to test the presence of.
     * @return The result.
     */
    public TileImprovement findTileImprovementType(TileImprovementType type) {
        for (TileItem item : tileItems) {
            if (item instanceof TileImprovement && ((TileImprovement) item).getType() == type) {
                return (TileImprovement) item;
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
        TileImprovement river = new TileImprovement(getGame(), tile, getSpecification()
                                                    .getTileImprovementType("model.improvement.river"));
        river = (TileImprovement) addTileItem(river);
        river.setMagnitude(magnitude);
        river.setStyle(style);
        return river;
    }

    /**
     * Removes the river <code>TileImprovement</code> from this Tile/Container.
     */
    // Change neighbours' River Style with {@link #adjustNeighbourRiverStyle}.
    public TileImprovement removeRiver() {
        Iterator<TileItem> iterator = tileItems.iterator();
        while (iterator.hasNext()) {
            TileItem item = iterator.next();
            if (item instanceof TileImprovement && ((TileImprovement) item).isRiver()) {
                iterator.remove();
                return (TileImprovement) item;
            }
        }
        return null;
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
        PlayerExploredTile pet = (showAll) ? null
            : tile.getPlayerExploredTile(player);

        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("ID", getId());
        out.writeAttribute("tile", tile.getId());

        if (getGame().isClientTrusted() || showAll || toSavedGame
            || player.canSee(tile)) {
            for (TileItem item : tileItems) {
                item.toXML(out, player, showAll, toSavedGame);
            }
        } else if (pet != null) {
            List<TileItem> petItems = pet.getAllTileItems();
            Collections.sort(petItems, tileItemComparator);
            for (TileItem item : petItems) {
                item.toXML(out, player, showAll, toSavedGame);
            }
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

        tileItems.clear();

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            TileItem item = (TileItem) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
            if (item == null) {
                if (in.getLocalName().equals(Resource.getXMLElementTagName())) {
                    item = new Resource(getGame(), in);
                } else if (in.getLocalName().equals(LostCityRumour.getXMLElementTagName())) {
                    item = new LostCityRumour(getGame(), in);
                } else if (in.getLocalName().equals(TileImprovement.getXMLElementTagName())) {
                    item = new TileImprovement(getGame(), in);
                }
            } else {
                item.readFromXML(in);
            }
            tileItems.add(item);
        }
        // TODO: remove this some time; at this point, sorting is only
        // required for old savegames
        Collections.sort(tileItems, tileItemComparator);
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
        for (TileItem item : tileItems) {
            sb.append(item.toString() + ", ");
        }
        return sb.toString();
    }

}
