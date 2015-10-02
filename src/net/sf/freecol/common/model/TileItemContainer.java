/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Map.Layer;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Contains <code>TileItem</code>s and can be used by a {@link Tile}
 * to make certain tasks easier.
 */
public class TileItemContainer extends FreeColGameObject {

    private static final Logger logger = Logger.getLogger(TileItemContainer.class.getName());

    /** A comparator to sort by ascending zIndex. */
    private static final Comparator<TileItem> tileItemComparator
        = new Comparator<TileItem>() {
            @Override
            public int compare(TileItem tileItem1, TileItem tileItem2) {
                int cmp = tileItem1.getZIndex() - tileItem2.getZIndex();
                if (cmp == 0) {
                    cmp = compareIds(tileItem1,
                            tileItem2);
                }
                return cmp;
            }
        };

    /** The tile owner for which this is the container. */
    private Tile tile;

    /** All tile items, sorted by zIndex. */
    private final List<TileItem> tileItems = new ArrayList<>();


    /**
     * Create an empty <code>TileItemContainer</code>.
     *
     * @param game The enclosing <code>Game</code>.
     * @param tile The <code>Tile</code> this
     *     <code>TileItemContainer</code> contains
     *     <code>TileItems</code> for.
     */
    public TileItemContainer(Game game, Tile tile) {
        super(game);

        if (tile == null) {
            throw new IllegalArgumentException("Tile must not be 'null'.");
        }

        this.tile = tile;
    }

    /**
     * Initiates a new <code>TileItemContainer</code> from an XML stream.
     *
     * @param game The enclosing <code>Game</code>.
     * @param tile The <code>Tile</code> this
     *     <code>TileItemContainer</code> contains
     *     <code>TileItems</code> for.
     * @param template A <code>TileItemContainer</code> to copy.
     * @param layer A maximum allowed <code>Layer</code>.
     */
    public TileItemContainer(Game game, Tile tile, TileItemContainer template,
                             Layer layer) {
        this(game, tile);

        final Specification spec = getSpecification();
        for (TileItem item : template.getTileItems()) {
            if (item instanceof Resource) {
                Resource resource = (Resource)item;
                if (layer.compareTo(Layer.RESOURCES) >= 0) {
                    addTileItem(new Resource(game, tile,
                            spec.getResourceType(resource.getId()),
                            resource.getQuantity()));
                }
            } else if (item instanceof LostCityRumour) {
                LostCityRumour rumour = (LostCityRumour)item;
                if (layer.compareTo(Layer.NATIVES) >= 0) {
                    addTileItem(new LostCityRumour(game, tile, rumour.getType(),
                            rumour.getName()));
                }
            } else if (item instanceof TileImprovement) {
                TileImprovement improvement = (TileImprovement)item;
                if (layer.compareTo(Layer.RIVERS) >= 0
                    || improvement.getType().isNatural()) {
                    addTileItem(new TileImprovement(game, tile, improvement));
                }
            } else {
                logger.warning("Bogus tile item: " + item.getId());
            }
        }
    }

    /**
     * Create a new <code>TileItemContainer</code>.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public TileItemContainer(Game game, String id) {
        super(game, id);
    }


    /**
     * Get the tile this container belongs to.
     *
     * @return The owning <code>Tile</code>.
     */
    public final Tile getTile() {
        return tile;
    }

    /**
     * Get the tile items in this container.
     *
     * @return A list of <code>TileItem</code>s.
     */
    public final List<TileItem> getTileItems() {
        return tileItems;
    }

    /**
     * Set the tile items.
     *
     * @param newTileItems The new tile items list.
     */
    public final void setTileItems(final List<TileItem> newTileItems) {
        this.tileItems.clear();
        if (newTileItems != null) this.tileItems.addAll(newTileItems);
        invalidateCache();
    }

    /**
     * Invalidate the production cache of the owning colony if any
     * but only if the tile is actually being used.
     */
    private void invalidateCache() {
        Colony colony = tile.getColony();
        if (colony != null && colony.isTileInUse(tile)) {
            colony.invalidateCache();
        }
    }

    /**
     * Gets any lost city rumour in this container.
     *
     * @return A <code>LostCityRumour</code> item if any, or null if
     *     not found.
     */
    public final LostCityRumour getLostCityRumour() {
        return (LostCityRumour)find(tileItems,
            ti -> ti instanceof LostCityRumour);
    }

    /**
     * Gets any resource item.
     *
     * @return A <code>Resource</code> item, or null is none found.
     */
    public final Resource getResource() {
        return (Resource)find(tileItems, ti -> ti instanceof Resource);
    }

    /**
     * Check whether this tile has a completed improvement of the given
     * type.
     *
     * @param type The <code>TileImprovementType</code> to check for.
     * @return Whether the tile has the improvement and the improvement is
     *     completed.
     */
    public boolean hasImprovement(TileImprovementType type) {
        TileImprovement improvement = getImprovement(type);
        return improvement != null && improvement.isComplete();
    }

    /**
     * Gets the tile improvement of the given type if any.
     *
     * @param type The <code>TileImprovementType</code> to look for.
     * @return The <code>TileImprovement</code> of the given type if
     *     present, otherwise null.
     */
    public TileImprovement getImprovement(TileImprovementType type) {
        return (TileImprovement)find(tileItems,
            ti -> ti instanceof TileImprovement
                && ((TileImprovement)ti).getType() == type);
    }

    /**
     * Gets any road improvement in this container.
     *
     * @return A road <code>TileImprovement</code> if any, or null if
     *     not found.
     */
    public TileImprovement getRoad() {
        return (TileImprovement)find(tileItems,
            ti -> ti instanceof TileImprovement
                && ((TileImprovement)ti).isRoad());
    }

    /**
     * Gets any river improvement in this container.
     *
     * @return A river <code>TileImprovement</code> if any, or null if
     *     not found.
     */
    public TileImprovement getRiver() {
        return (TileImprovement)find(tileItems,
            ti -> ti instanceof TileImprovement
                && ((TileImprovement)ti).isRiver());
    }

    /**
     * Remove improvements incompatible with the given TileType.  This
     * method is called whenever the type of the container's tile
     * changes, i.e. due to clearing.
     */
    public void removeIncompatibleImprovements() {
        TileType tileType = tile.getType();
        Iterator<TileItem> iterator = tileItems.iterator();
        boolean removed = false;
        while (iterator.hasNext()) {
            TileItem item = iterator.next();
            if (!item.isTileTypeAllowed(tileType)) {
                iterator.remove();
                item.dispose();
                removed = true;
            }
        }
        if (removed) invalidateCache();
    }

    /**
     * Gets a list of the <code>TileImprovement</code>s in this
     * <code>TileItemContainer</code>.
     *
     * @param completedOnly If true select only the completed improvements.
     * @return A list of <code>TileImprovement</code>s.
     */
    private List<TileImprovement> getImprovements(boolean completedOnly) {
        List<TileImprovement> improvements = new ArrayList<>();
        for (TileItem item : tileItems) {
            if (item instanceof TileImprovement
                && (!completedOnly || ((TileImprovement)item).isComplete())) {
                improvements.add((TileImprovement)item);
            }
        }
        return improvements;
    }

    /**
     * Gets a list of the <code>TileImprovement</code>s in this
     * <code>TileItemContainer</code>.
     *
     * @return A list of <code>TileImprovement</code>s.
     */
    public List<TileImprovement> getImprovements() {
        return getImprovements(false);
    }

    /**
     * Gets a list of the completed <code>TileImprovement</code>s in
     * this <code>TileItemContainer</code>.
     *
     * @return A list of <code>TileImprovement</code>s.
     */
    public List<TileImprovement> getCompletedImprovements() {
        return getImprovements(true);
    }

    /**
     * Try to add a <code>TileItem</code> to this container.
     * If the item is of lower magnitude than an existing one the existing
     * one stands.
     *
     * @param item The <code>TileItem</code> to add to this container.
     * @return The added <code>TileItem</code> or the existing
     *     <code>TileItem</code> if of higher magnitude, or null on error.
     */
    public final TileItem addTileItem(TileItem item) {
        if (item == null) return null;
        for (int index = 0; index < tileItems.size(); index++) {
            TileItem oldItem = tileItems.get(index);
            if (item instanceof TileImprovement
                && oldItem instanceof TileImprovement) {
                TileImprovement oldTip = (TileImprovement)oldItem;
                TileImprovement newTip = (TileImprovement)item;
                if (oldTip.getType().getId().equals(newTip.getType().getId())) {
                    if (oldTip.getMagnitude() < newTip.getMagnitude()) {
                        tileItems.set(index, item);
                        oldItem.dispose();
                        invalidateCache();
                        return item;
                    } else {
                        return oldItem; // Found it, but not replacing.
                    }
                } else if (oldItem.getZIndex() > item.getZIndex()) {
                    break;
                }
            }
        }
        tileItems.add(item);
        invalidateCache();
        return item;
    }

    /**
     * Removes a <code>TileItem</code> from this container.
     *
     * @param item The <code>TileItem</code> to remove from this container.
     * @return The <code>TileItem</code> that has been removed from
     *     this container (if any).
     */
    public final <T extends TileItem> T removeTileItem(T item) {
        boolean removed = tileItems.remove(item);
        if (removed) {
            item.setLocation(null);
            invalidateCache();
            return item;
        }
        return null;
    }

    /**
     * Removes all tile items of a given class.
     *
     * @param c The <code>Class</code> to remove.
     */
    public final <T extends TileItem> void removeAll(Class<T> c) {
        Iterator<TileItem> iterator = tileItems.iterator();
        while (iterator.hasNext()) {
            if (c.isInstance(iterator.next())) iterator.remove();
        }
    }

    /**
     * Determine the total bonus for a goods type.  Checks resources
     * and all improvements, unless onlyNatural is true, in which case
     * only natural improvements will be considered.
     *
     * This is not used for normal production, but is necessary to
     * calculate colony center tile secondary production, which does
     * not profit from artificial improvements such as plowing.  It is
     * also used to assess which goods are likely to be most
     * productive on a tile.
     *
     * @param goodsType The <code>GoodsType</code> to check.
     * @param unitType The <code>UnitType</code> to check.
     * @param potential The base potential production.
     * @param onlyNatural Only allow natural improvements.
     * @return The resulting production.
     */
    public int getTotalBonusPotential(GoodsType goodsType, UnitType unitType,
                                      int potential, boolean onlyNatural) {
        int result = potential;
        for (TileItem item : tileItems) {
            if (item.isNatural() || !onlyNatural) {
                result = item.applyBonus(goodsType, unitType, result);
            }
        }
        return result;
    }

    /**
     * Gets the production modifiers for the given type of goods and unit.
     *
     * @param goodsType The <code>GoodsType</code> to produce.
     * @param unitType The optional <code>unitType</code> to produce them.
     * @return A list of the applicable modifiers.
     */
    public List<Modifier> getProductionModifiers(GoodsType goodsType,
                                                 UnitType unitType) {
        List<Modifier> result = new ArrayList<>();
        for (TileItem item : tileItems) {
            result.addAll(item.getProductionModifiers(goodsType, unitType));
        }
        return result;
    }

    /**
     * Does this container contain an item that allows the tile to 
     * produce a goods type?
     *
     * @param goodsType The <code>GoodsType</code> to produce.
     * @param unitType The optional <code>unitType</code> to produce with.
     * @return True if this container allows the goods type to be produced.
     */
    public boolean canProduce(GoodsType goodsType, UnitType unitType) {
        return any(tileItems, ti -> ti.canProduce(goodsType, unitType));
    }

    /**
     * Determine the movement cost to this <code>Tile</code> from
     * another <code>Tile</code>.
     * Does not consider special unit abilities.
     *
     * @param fromTile The <code>Tile</code> to move from.
     * @param targetTile The <code>Tile</code> to move to.
     * @param basicMoveCost The basic cost.
     * @return The movement cost.
     */
    public int getMoveCost(Tile fromTile, Tile targetTile, int basicMoveCost) {
        int moveCost = basicMoveCost;
        for (TileItem item : tileItems) {
            if (item instanceof TileImprovement
                && ((TileImprovement)item).isComplete()) {
                Direction direction = targetTile.getDirection(fromTile);
                if (direction == null) return INFINITY;
                moveCost = Math.min(moveCost, 
                    ((TileImprovement)item).getMoveCost(direction, moveCost));
            }
        }
        return moveCost;
    }

    /**
     * Copy from another <code>TileItemContainer</code> including resources.
     *
     * @param tic The <code>TileItemContainer</code> to copy from.
     */
    public void copyFrom(TileItemContainer tic) {
        copyFrom(tic, true, false);
    }

    /**
     * Copy from another <code>TileItemContainer</code>.
     *
     * @param tic The <code>TileItemContainer</code> to copy from.
     * @param importResources If true, import resources.
     */
    public void copyFrom(TileItemContainer tic, boolean importResources) {
        copyFrom(tic, importResources, false);
    }

    /**
     * Copy from another <code>TileItemContainer</code>.
     *
     * @param tic The <code>TileItemContainer</code> to copy from.
     * @param importResources If true, import resources.
     * @param copyOnlyNatural Restrict import to natural resources.
     */
    public void copyFrom(TileItemContainer tic, boolean importResources,
                         boolean copyOnlyNatural) {
        tileItems.clear();
        for (TileItem item : tic.getTileItems()) {
            if (item instanceof Resource) {
                if (importResources) {
                    Resource ticR = (Resource) item;
                    Resource r = new Resource(getGame(), tile,
                        ticR.getType(), ticR.getQuantity());
                    tileItems.add(r);
                }
            } else if (item instanceof LostCityRumour) {
                if (!copyOnlyNatural) {
                    LostCityRumour ticR = (LostCityRumour) item;
                    LostCityRumour r = new LostCityRumour(getGame(), tile,
                        ticR.getType(), ticR.getName());
                    addTileItem(r);
                }
            } else if (item instanceof TileImprovement) {
                if (!copyOnlyNatural
                    || ((TileImprovement)item).getType().isNatural()) {
                    addTileItem(new TileImprovement(getGame(), tile, 
                                                    (TileImprovement)item));
                }
            }
        }
    }

    /**
     * Checks if the specified <code>TileItem</code> is in this container.
     *
     * @param t The <code>TileItem</code> to test the presence of.
     * @return True if the tile item is present.
     */
    public boolean contains(TileItem t) {
        return tileItems.contains(t);
    }


    // Low level

    /**
     * Removes all references to this object.
     */
    @Override
    public void disposeResources() {
        tileItems.clear();
        super.disposeResources();
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public int checkIntegrity(boolean fix) {
        int result = super.checkIntegrity(fix);
        for (TileItem ti : new ArrayList<>(tileItems)) {
            int integ = ti.checkIntegrity(fix);
            if (fix) {
                // @compat 0.10.5
                // Somewhere around 0.10.5 there were maps with LCRs
                // that reference the wrong tile.
                if (ti.getTile() != tile) {
                    logger.warning("Fixing improvement tile at: " + tile
                                   + " / " + ti.getId());
                    ti.setLocation(tile);
                    integ = Math.min(integ, 0);
                }
                // There are also still maps floating around with
                // rivers that go nowhere.
                if (ti instanceof TileImprovement) {
                    TileImprovement tim = (TileImprovement)ti;
                    if (tim.isRiver()
                        && (tim.getStyle() == null
                            || "0000".equals(tim.getStyle().toString()))) {
                        logger.warning("Style 0000 river: " + tim);
                        integ = -1;
                    }
                }
                // end @compat
                if (integ < 0) {
                    logger.warning("Removing broken improvement at: " + tile
                                   + " / " + ti.getId());
                    tileItems.remove(ti);
                    integ = 0;
                }
            }
            result = Math.min(result, integ);
        }
        return result;
    }


    // Serialization

    private static final String TILE_TAG = "tile";
    // @compat 0.11.3
    private static final String OLD_TILE_IMPROVEMENT_TAG = "tileimprovement";
    // end @compat 0.11.3


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(TILE_TAG, tile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (TileItem item : tileItems) {
            item.toXML(xw);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        tile = xr.findFreeColGameObject(getGame(), TILE_TAG,
                                        Tile.class, (Tile)null, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        tileItems.clear();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Game game = getGame();
        final String tag = xr.getLocalName();

        if (LostCityRumour.getXMLElementTagName().equals(tag)) {
            LostCityRumour lcr = xr.readFreeColGameObject(game, LostCityRumour.class);
            if (lcr != null) {
                tileItems.add(lcr);
                // @compat 0.10.4
                // Fix LCR tile, see LostCityRumour.readAttributes
                lcr.setTile(tile);
                // end @compat 0.10.4
            }

        } else if (Resource.getXMLElementTagName().equals(tag)) {
            tileItems.add(xr.readFreeColGameObject(game, Resource.class));

        } else if (TileImprovement.getXMLElementTagName().equals(tag)
                   // @compat 0.11.3
                   || OLD_TILE_IMPROVEMENT_TAG.equals(tag)
                   // end @compat 0.11.3
                   ) {
            tileItems.add(xr.readFreeColGameObject(game, TileImprovement.class));

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("[").append(getId());
        for (TileItem item : tileItems) sb.append(" ").append(item);
        sb.append("]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "tileItemContainer".
     */
    public static String getXMLElementTagName() {
        return "tileItemContainer";
    }
}
