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
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


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

    // Visible Colony data.
    private int colonyUnitCount = 0;

    // Visible IndianSettlement data.
    private Unit missionary = null;
    private Tension alarm = null;
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
     * Get the tile items in this pet.
     *
     * @return A list of <code>TileItems</code>.
     */
    private List<TileItem> getTileItems() {
        return (tileItems == null) ? Collections.<TileItem>emptyList()
            : tileItems;
    }

    /**
     * Add a tile item to this pet.
     *
     * @param item The <code>TileItem</code> to add.
     */
    private void addTileItem(TileItem item) {
        if (tileItems == null) tileItems = new ArrayList<>();
        tileItems.add(item);
    }

    // @compat 0.10.7
    /**
     * Use this PET so set an approximation to the correct cached
     * tile.  This is impossible in general, and will only happen once
     * when converting an old saved game, but at least try to do
     * something credible.
     */
    public void fixCache() {
        if (!getSpecification().getBoolean(GameOptions.FOG_OF_WAR)) {
            tile.setCachedTile(player, tile);
            return;
        }

        Tile copied = tile.getTileToCache();
        boolean ok = true;
        if (tile.getOwner() != owner) {
            copied.setOwner(owner);
            ok = false;
        }

        if (tile.getOwningSettlement() != owningSettlement) {
            copied.setOwningSettlement(owningSettlement);
            ok = false;
        }

        List<TileItem> ti = (copied.getTileItemContainer() == null) ? null
            : copied.getTileItemContainer().getTileItems();
        if ((ti == null) != (tileItems == null)
            || (ti != null && ti.size() != tileItems.size())) {
            // Not trying too hard to match up the tile items
            ok = false;
        }

        Settlement ts = copied.getSettlement();
        if (ts instanceof Colony) {
            Colony colony = (Colony)ts;
            if (colonyUnitCount != colony.getUnitCount()) {
                colony.setDisplayUnitCount(colonyUnitCount);
                ok = false;
            }
        } else if (ts instanceof IndianSettlement) {
            if (missionary == null && mostHated == null && alarm == null) {
                copied.setSettlement(null);
            } else {
                IndianSettlement is = (IndianSettlement)ts;
                if (missionary != is.getMissionary()) {
                    // Do not try to be clever with a unit that might be gone.
                    is.setMissionary(null);
                    ok = false;
                }
                if (mostHated != is.getMostHated()) {
                    is.setMostHated(mostHated);
                    ok = false;
                }
                if (alarm != is.getAlarm(player)) {
                    is.setAlarm(player, alarm);
                    ok = false;
                }
            }
        }
        tile.setCachedTile(player, (ok) ? tile : copied);
    }
        

    // Serialization
    
    private static final String ALARM_TAG = "alarm";
    private static final String COLONY_UNIT_COUNT_TAG = "colonyUnitCount";
    private static final String LEARNABLE_SKILL_TAG = "learnableSkill";
    private static final String MISSIONARY_TAG = "missionary";
    private static final String MOST_HATED_TAG = "mostHated";
    private static final String OWNER_TAG = "owner";
    private static final String OWNING_SETTLEMENT_TAG = "owningSettlement";
    private static final String PLAYER_TAG = "player";
    private static final String TILE_TAG = "tile";
    private static final String WANTED_GOODS_TAG = "wantedGoods";
    // @compat 0.11.3
    private static final String OLD_TILE_IMPROVEMENT_TAG = "tileimprovement";
    // end @compat 0.11.3


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(PLAYER_TAG, player);

        xw.writeAttribute(TILE_TAG, tile);

        if (owner != null) xw.writeAttribute(OWNER_TAG, owner);

        if (owningSettlement != null) {
            xw.writeAttribute(OWNING_SETTLEMENT_TAG, owningSettlement);
        }

        if (colonyUnitCount > 0) {
            xw.writeAttribute(COLONY_UNIT_COUNT_TAG, colonyUnitCount);
        }

        if (alarm != null) xw.writeAttribute(ALARM_TAG, alarm.getValue());

        if (mostHated != null) {
            xw.writeAttribute(MOST_HATED_TAG, mostHated.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (missionary != null
            // Hack to avoid writing now-invalid missionary.
            && tile.getSettlement() != null
            && tile.getSettlement() instanceof IndianSettlement
            && ((IndianSettlement)tile.getSettlement()).getMissionary() == missionary) {
            xw.writeStartElement(MISSIONARY_TAG);

            missionary.toXML(xw);

            xw.writeEndElement();
        }

        for (TileItem ti : getTileItems()) {
            ti.toXML(xw);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();
        final Game game = getGame();

        player = xr.makeFreeColGameObject(game, PLAYER_TAG,
                                          Player.class, true);

        tile = xr.makeFreeColGameObject(game, TILE_TAG, Tile.class, true);

        owner = xr.makeFreeColGameObject(game, OWNER_TAG, Player.class, false);

        // FIXME: makeFreeColGameObject is more logical, but will fail ATM
        // if the settlement has been destroyed while this pet-player can
        // not see it.  Since pets are only read in the server, there will be
        // a ServerObject for existing settlements so findFreeColGameObject
        // will do the right thing for now.
        owningSettlement
            = xr.findFreeColGameObject(game, OWNING_SETTLEMENT_TAG,
                Settlement.class, (Settlement)null, false);

        colonyUnitCount = xr.getAttribute(COLONY_UNIT_COUNT_TAG, 0);

        alarm = new Tension(xr.getAttribute(ALARM_TAG, 0));

        mostHated = xr.makeFreeColGameObject(game, MOST_HATED_TAG,
                                             Player.class, false);


        // @compat 0.10.7
        IndianSettlement is = tile.getIndianSettlement();
        if (is != null) {
            UnitType skill = xr.getType(spec, LEARNABLE_SKILL_TAG,
                                        UnitType.class, (UnitType)null);

            boolean natives = skill != null;
            GoodsType[] wanted = new GoodsType[IndianSettlement.WANTED_GOODS_COUNT];
            for (int i = 0; i < IndianSettlement.WANTED_GOODS_COUNT; i++) {
                wanted[i] = xr.getType(spec, WANTED_GOODS_TAG + i,
                                       GoodsType.class, (GoodsType)null);
                natives |= wanted[i] != null;
            }
            
            if (natives) {
                tile.setIndianSettlementInternals(player, skill, wanted);
            }
        }
        // end @compat
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        if (tileItems != null) tileItems.clear();
        missionary = null;

        super.readChildren(xr);

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
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Game game = getGame();
        final String tag = xr.getLocalName();

        if (MISSIONARY_TAG.equals(tag)) {
            xr.nextTag(); // advance to the Unit tag
            missionary = xr.readFreeColGameObject(game, Unit.class);
            xr.closeTag(MISSIONARY_TAG);

        } else if (LostCityRumour.getXMLElementTagName().equals(tag)) {
            addTileItem(xr.readFreeColGameObject(game, LostCityRumour.class));

        } else if (Resource.getXMLElementTagName().equals(tag)) {
            addTileItem(xr.readFreeColGameObject(game, Resource.class));

        } else if (TileImprovement.getXMLElementTagName().equals(tag)
                   // @compat 0.11.3
                   || OLD_TILE_IMPROVEMENT_TAG.equals(tag)
                   // end @compat 0.11.3
                   ) {
            addTileItem(xr.readFreeColGameObject(game, TileImprovement.class));

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "playerExploredTile".
     */
    public static String getXMLElementTagName() {
        return "playerExploredTile";
    }
}
