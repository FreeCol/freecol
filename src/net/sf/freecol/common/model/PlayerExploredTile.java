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

    // The owner of this view.
    private Player player;

    // The tile viewed.
    private Tile tile;

    // The owner of the tile.
    private Player owner;

    // The owning settlement of the tile, if any.
    private Settlement owningSettlement;

    // All known TileItems.
    private List<TileItem> tileItems;

    // Colony data.
    private int colonyUnitCount = 0;
    private String colonyStockadeKey = null;

    // IndianSettlement data.
    private UnitType skill = null;
    private GoodsType[] wantedGoods = { null, null, null };
    private Unit missionary = null;


    /**
     * Creates a new <code>PlayerExploredTile</code>.
     * 
     * @param player The <code>Player</code> that owns this view.
     * @param tile The <code>Tile</code> to view.
     */
    public PlayerExploredTile(Game game, Player player, Tile tile) {
        super(game);
        this.player = player;
        this.tile = tile;
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * 
     * @param in The XML stream to read the data from.
     * @throws XMLStreamException if an error occurred during parsing.
     */
    public PlayerExploredTile(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXML(in);
    }

    /**
     * Update this PlayerExploredTile with the current state of its tile.
     *
     * @param full If true, update information hidden by settlements.
     */
    public void update(boolean full) {
        owner = tile.getOwner();
        owningSettlement = tile.getOwningSettlement();

        tileItems = new ArrayList<TileItem>();
        TileItemContainer tic = tile.getTileItemContainer();
        if (tic != null) {
            tileItems.addAll(tic.getImprovements());
            if (tic.getResource() != null) {
                tileItems.add(tic.getResource());
            }
            if (tic.getLostCityRumour() != null) {
                tileItems.add(tic.getLostCityRumour());
            }
        }

        Colony colony = tile.getColony();
        if (colony != null) {
            colonyUnitCount = colony.getUnitCount();
            colonyStockadeKey = colony.getTrueStockadeKey();
            missionary = null;
        }
        IndianSettlement is = tile.getIndianSettlement();
        if (is != null) {
            colonyUnitCount = 0;
            colonyStockadeKey = null;
            missionary = is.getMissionary();
            if (full) {
                skill = is.getLearnableSkill();
                wantedGoods = is.getWantedGoods();
            }
        }
    }

    // Trivial public accessors.

    public Player getOwner() {
        return owner;
    }

    public Settlement getOwningSettlement() {
        return owningSettlement;
    }

    public List<TileItem> getAllTileItems() {
        return new ArrayList<TileItem>(tileItems);
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

    // Only needed for 0.9.x workaround in Tile.readFromXMLImpl.
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
    // End 0.9.x workarounds.


    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     * 
     * <br>
     * <br>
     * 
     * Only attributes visible to the given <code>Player</code> will be
     * added to that representation if <code>showAll</code> is set to
     * <code>false</code>.
     * 
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation
     *            should be made for, or <code>null</code> if
     *            <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> will
     *            be added to the representation if <code>showAll</code>
     *            is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that is
     *            only needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    public void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
        throws XMLStreamException {

        // Start element:
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute(ID_ATTRIBUTE, getId());

        out.writeAttribute("player", player.getId());
        out.writeAttribute("tile", tile.getId());

        if (tile.getOwner() != owner && owner != null) {
            out.writeAttribute("owner", owner.getId());
        }
        if (owningSettlement != null) {
            out.writeAttribute("owningSettlement", owningSettlement.getId());
        }

        Settlement settlement = tile.getSettlement();
        if (settlement instanceof Colony) {
            out.writeAttribute("colonyUnitCount", Integer.toString(colonyUnitCount));
            if (colonyStockadeKey != null) {
                out.writeAttribute("colonyStockadeKey", colonyStockadeKey);
            }
        } else if (settlement instanceof IndianSettlement) {
            writeAttribute(out, "learnableSkill", skill);
            writeAttribute(out, "wantedGoods0", wantedGoods[0]);
            writeAttribute(out, "wantedGoods1", wantedGoods[1]);
            writeAttribute(out, "wantedGoods2", wantedGoods[2]);
            // attributes end here
            if (missionary != null) {
                out.writeStartElement("missionary");
                missionary.toXML(out, player, showAll, toSavedGame);
                out.writeEndElement();
            }
        } else if (settlement != null) {
            throw new IllegalStateException("Bogus settlement");
        }
        for (TileItem ti : tileItems) {
            ti.toXML(out, player, showAll, toSavedGame);
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * 
     * @param in The input stream with the XML.
     * @throws XMLStreamException if an error occurred during parsing.
     */
    public void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        Specification spec = getSpecification();
        Game game = getGame();

        setId(in.getAttributeValue(null, ID_ATTRIBUTE));
        
        player = getFreeColGameObject(in, "player", Player.class);
        tile = getFreeColGameObject(in, "tile", Tile.class);
        owner = getFreeColGameObject(in, "owner",
                                     Player.class, tile.getOwner());
        owningSettlement = getFreeColGameObject(in, "owningSettlement",
                                                Settlement.class, null);
        colonyUnitCount = getAttribute(in, "colonyUnitCount", 0);
        colonyStockadeKey = in.getAttributeValue(null, "colonyStockadeKey");
        skill = spec.getType(in, "learnableSkill", UnitType.class, null);
        wantedGoods[0] = spec.getType(in, "wantedGoods0", GoodsType.class,
                                      null);
        wantedGoods[1] = spec.getType(in, "wantedGoods1", GoodsType.class,
                                      null);
        wantedGoods[2] = spec.getType(in, "wantedGoods2", GoodsType.class,
                                      null);

        missionary = null;
        tileItems = new ArrayList<TileItem>();
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(IndianSettlement.MISSIONARY_TAG_NAME)) {
                in.nextTag(); // advance to the Unit tag
                missionary = updateFreeColGameObject(in, Unit.class);
                in.nextTag(); // close <missionary> tag
            } else if (in.getLocalName().equals(Resource.getXMLElementTagName())) {
                Resource resource = (Resource) game.getFreeColGameObject(in.getAttributeValue(null, ID_ATTRIBUTE));
                if (resource != null) {
                    resource.readFromXML(in);
                } else {
                    resource = new Resource(game, in);
                }
                tileItems.add(resource);
            } else if (in.getLocalName().equals(LostCityRumour.getXMLElementTagName())) {
                LostCityRumour lostCityRumour = (LostCityRumour) game.getFreeColGameObject(in.getAttributeValue(null, ID_ATTRIBUTE));
                if (lostCityRumour != null) {
                    lostCityRumour.readFromXML(in);
                } else {
                    lostCityRumour = new LostCityRumour(game, in);
                }
                tileItems.add(lostCityRumour);
            } else if (in.getLocalName().equals(TileImprovement.getXMLElementTagName())) {
                TileImprovement ti = (TileImprovement) game.getFreeColGameObject(in.getAttributeValue(null, ID_ATTRIBUTE));
                if (ti != null) {
                    ti.readFromXML(in);
                } else {
                    ti = new TileImprovement(game, in);
                }
                tileItems.add(ti);
            } else {
                logger.warning("Unknown tag: " + in.getLocalName()
                               + " loading PlayerExploredTile");
                in.nextTag();
            }
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     * 
     * @return "playerExploredTile".
     */
    public static String getXMLElementTagName() {
        return "playerExploredTile";
    }
}
