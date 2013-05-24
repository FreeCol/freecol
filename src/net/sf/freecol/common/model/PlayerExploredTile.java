/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * This class contains the mutable tile data visible to a specific player.
 *
 * Sometimes a tile contains information that should not be given to a
 * player. For instance; a settlement that was built after the player last
 * viewed the tile.
 *
 * The <code>toXMLElement</code> of {@link Tile} uses information from
 * this class to hide information that is not available.
 */
public class PlayerExploredTile extends FreeColGameObject {

    private static final Logger logger = Logger.getLogger(PlayerExploredTile.class.getName());

    /** The maximum number of wanted goods. */
    private static final int WANTED_GOODS_COUNT = 3;

    /** The owner of this view. */
    private Player player;

    /** The tile viewed. */
    private Tile tile;

    /** The owner of the tile. */
    private Player owner;

    /** The owning settlement of the tile, if any. */
    private Settlement owningSettlement;

    /** All known TileItems. */
    private List<TileItem> tileItems = null;

    // Colony data.
    private int colonyUnitCount = 0;
    private String colonyStockadeKey = null;

    // IndianSettlement data.
    private UnitType skill = null;
    private GoodsType[] wantedGoods = { null, null, null };
    private Unit missionary = null;
    private Player mostHated = null;


    /**
     * Creates a new <code>PlayerExploredTile</code>.
     *
     * @param game The enclosing <code>Game</code>.
     * @param player The <code>Player</code> that owns this view.
     * @param tile The <code>Tile</code> to view.
     */
    public PlayerExploredTile(Game game, Player player, Tile tile) {
        super(game);
        this.player = player;
        this.tile = tile;
    }

    /**
     * Create a new player explored tile.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public PlayerExploredTile(Game game, String id) {
        super(game, id);
    }


    /**
     * Update this PlayerExploredTile with the current state of its tile.
     *
     * @param full If true, update information hidden by settlements.
     */
    public void update(boolean full) {
        owner = tile.getOwner();
        owningSettlement = tile.getOwningSettlement();

        if (tileItems != null) tileItems.clear();
        TileItemContainer tic = tile.getTileItemContainer();
        if (tic != null) {
            for (TileItem ti : tic.getImprovements()) addTileItem(ti);
            if (tic.getResource() != null) {
                addTileItem(tic.getResource());
            }
            if (tic.getLostCityRumour() != null) {
                addTileItem(tic.getLostCityRumour());
            }
        }

        Colony colony = tile.getColony();
        if (colony == null) {
            colonyUnitCount = -1;
            colonyStockadeKey = null;
        } else {
            colonyUnitCount = colony.getDisplayUnitCount();
            colonyStockadeKey = colony.getTrueStockadeKey();
        }
        IndianSettlement is = tile.getIndianSettlement();
        if (is == null) {
            missionary = null;
            skill = null;
            wantedGoods = new GoodsType[] { null, null, null };
            mostHated = null;
        } else {
            missionary = is.getMissionary();
            if (full) {
                skill = is.getLearnableSkill();
            }
            // Do not hide.  Has to be visible for the pre-speak-to-chief
            // dialog message.  Yes this is odd.
            wantedGoods = is.getWantedGoods();
            mostHated = is.getMostHated();
        }
    }

    /**
     * Get the tile items in this pet.
     *
     * @return A list of <code>TileItems</code>.
     */
    public List<TileItem> getTileItems() {
        if (tileItems == null) return Collections.emptyList();
        return tileItems;
    }

    /**
     * Add a tile item to this pet.
     *
     * @param item The <code>TileItem</code> to add.
     */
    public void addTileItem(TileItem item) {
        if (tileItems == null) tileItems = new ArrayList<TileItem>();
        tileItems.add(item);
    }


    // Trivial public accessors.

    public Player getOwner() {
        return owner;
    }

    public Settlement getOwningSettlement() {
        return owningSettlement;
    }

    public int getColonyUnitCount() {
        return colonyUnitCount;
    }

    public String getColonyStockadeKey() {
        return colonyStockadeKey;
    }

    public Unit getMissionary() {
        return missionary;
    }

    public UnitType getSkill() {
        return skill;
    }

    public GoodsType[] getWantedGoods() {
        return wantedGoods;
    }

    public Player getMostHated() {
        return mostHated;
    }

    // @compat 0.9.x
    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public void setOwningSettlement(Settlement owningSettlement) {
        this.owningSettlement = owningSettlement;
    }

    public void setColonyUnitCount(int colonyUnitCount) {
        this.colonyUnitCount = colonyUnitCount;
    }

    public void setColonyStockadeKey(String colonyStockadeKey) {
        this.colonyStockadeKey = colonyStockadeKey;
    }

    public void setMissionary(Unit missionary) {
        this.missionary = missionary;
    }
    // end @compat


    /**
     * Check for any integrity problems.
     *
     * @param fix Fix problems if possible.
     * @return Negative if there are problems remaining, zero if
     *     problems were fixed, positive if no problems found at all.
     */
    public int checkIntegrity(boolean fix) {
        int result = 1;
        if (tileItems != null) {
            for (TileItem ti : new ArrayList<TileItem>(tileItems)) {
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
                    // end @compat
                    if (integ < 0) {
                        logger.warning("Removing broken improvement at: "
                                       + tile);
                        tileItems.remove(ti);
                    }
                }
                result = Math.min(result, integ);
            }
        }
        return result;
    }


    // Serialization
    
    private static final String COLONY_UNIT_COUNT_TAG = "colonyUnitCount";
    private static final String COLONY_STOCKADE_KEY_TAG = "colonyStockadeKey";
    private static final String LEARNABLE_SKILL_TAG = "learnableSkill";
    private static final String MISSIONARY_TAG = "missionary";
    private static final String MOST_HATED_TAG = "mostHated";
    private static final String OWNER_TAG = "owner";
    private static final String OWNING_SETTLEMENT_TAG = "owningSettlement";
    private static final String PLAYER_TAG = "player";
    private static final String TILE_TAG = "tile";
    private static final String WANTED_GOODS_TAG = "wantedGoods";


    /**
     * {@inheritDoc}
     */
    @Override
    public void toXMLImpl(XMLStreamWriter out, Player player,
                          boolean showAll,
                          boolean toSavedGame) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName(), player, showAll, toSavedGame);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeAttributes(XMLStreamWriter out, Player player,
                                boolean showAll,
                                boolean toSavedGame) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, PLAYER_TAG, player);

        writeAttribute(out, TILE_TAG, tile);

        if (owner != null) {
            writeAttribute(out, OWNER_TAG, owner);
        }

        if (owningSettlement != null) {
            writeAttribute(out, OWNING_SETTLEMENT_TAG, owningSettlement);
        }

        if (colonyUnitCount > 0) {
            writeAttribute(out, COLONY_UNIT_COUNT_TAG, colonyUnitCount);
        }

        if (colonyStockadeKey != null) {
            writeAttribute(out, COLONY_STOCKADE_KEY_TAG, colonyStockadeKey);
        }

        if (skill != null) {
            writeAttribute(out, LEARNABLE_SKILL_TAG, skill);
        }

        for (int i = 0; i < WANTED_GOODS_COUNT; i++) {
            if (wantedGoods[i] != null) {
                writeAttribute(out, WANTED_GOODS_TAG + i, wantedGoods[i]);
            }
        }

        if (mostHated != null) {
            writeAttribute(out, MOST_HATED_TAG, mostHated.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChildren(XMLStreamWriter out, Player player,
                              boolean showAll,
                              boolean toSavedGame) throws XMLStreamException {
        super.writeChildren(out);

        if (missionary != null) {
            out.writeStartElement(MISSIONARY_TAG);

            missionary.toXML(out, player, showAll, toSavedGame);

            out.writeEndElement();
        }

        for (TileItem ti : getTileItems()) {
            ti.toXML(out, player, showAll, toSavedGame);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();
        final Game game = getGame();

        super.readAttributes(in);

        player = makeFreeColGameObject(in, PLAYER_TAG, Player.class, true);

        tile = makeFreeColGameObject(in, TILE_TAG, Tile.class, true);

        owner = makeFreeColGameObject(in, OWNER_TAG, Player.class, false);

        // TODO: makeFreeColGameObject is more logical, but will fail ATM
        // if the settlement has been destroyed while this pet-player can
        // not see it.  Since pets are only read in the server, there will be
        // a ServerObject for existing settlements so findFreeColGameObject
        // will do the right thing for now.
        owningSettlement = findFreeColGameObject(in, OWNING_SETTLEMENT_TAG,
            Settlement.class, (Settlement)null, false);

        colonyUnitCount = getAttribute(in, COLONY_UNIT_COUNT_TAG, 0);

        colonyStockadeKey = getAttribute(in, COLONY_STOCKADE_KEY_TAG,
                                         (String)null);

        skill = spec.getType(in, LEARNABLE_SKILL_TAG,
                             UnitType.class, (UnitType)null);

        for (int i = 0; i < WANTED_GOODS_COUNT; i++) {
            wantedGoods[i] = spec.getType(in, WANTED_GOODS_TAG + i,
                                          GoodsType.class, (GoodsType)null);
        }

        mostHated = makeFreeColGameObject(in, MOST_HATED_TAG,
                                          Player.class, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        // Clear containers.
        if (tileItems != null) tileItems.clear();
        missionary = null;

        super.readChildren(in);

        // Workaround for BR#2508, problem possibly dates as late as 0.10.5.
        if (tile.getIndianSettlement() == null && missionary != null) {
            logger.warning("Dropping ghost missionary " + missionary.getId()
                + " from " + this.getId());
            Player p = missionary.getOwner();
            if (p != null) p.removeUnit(missionary);
            missionary = null;
        }           
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final Game game = getGame();
        final String tag = in.getLocalName();

        if (MISSIONARY_TAG.equals(tag)) {
            in.nextTag(); // advance to the Unit tag
            missionary = readFreeColGameObject(in, Unit.class);
            closeTag(in, MISSIONARY_TAG);

        } else if (LostCityRumour.getXMLElementTagName().equals(tag)) {
            addTileItem(readFreeColGameObject(in, LostCityRumour.class));

        } else if (Resource.getXMLElementTagName().equals(tag)) {
            addTileItem(readFreeColGameObject(in, Resource.class));

        } else if (TileImprovement.getXMLElementTagName().equals(tag)) {
            addTileItem(readFreeColGameObject(in, TileImprovement.class));

        } else {
            super.readChild(in);
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "playerExploredTile".
     */
    public static String getXMLElementTagName() {
        return "playerExploredTile";
    }
}
