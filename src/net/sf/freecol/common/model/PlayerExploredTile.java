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
 * <br>
 * <br>
 * 
 * Sometimes a tile contains information that should not be given to a
 * player. For instance; a settlement that was built after the player last
 * viewed the tile.
 * 
 * <br>
 * <br>
 * 
 * The <code>toXMLElement</code> of {@link Tile} uses information from
 * this class to hide information that is not available.
 */
public class PlayerExploredTile extends FreeColGameObject {

    private static final Logger logger = Logger.getLogger(PlayerExploredTile.class.getName()); 

    /**
     * The owner of this view.
     */
    private Player player;

    private boolean explored = false;

    /**
     * The owner of this tile.
     */
    private Player owner;

    // All known TileItems
    private Resource resource;
    private LostCityRumour lostCityRumour;
    private List<TileImprovement> improvements;
    private TileImprovement road;
    private TileImprovement river;

    // Colony data:
    private int colonyUnitCount = 0, colonyStockadeLevel;

    // IndianSettlement data:
    private UnitType skill = null;
    private GoodsType[] wantedGoods = {null, null, null};
    private boolean settlementVisited = false;

    private Unit missionary = null;

    private boolean connected = false;

    private Tile tile;

    /**
     * Creates a new <code>PlayerExploredTile</code>.
     * 
     * @param player the player 
     * @param tile a tile 
     */
    public PlayerExploredTile(Game game, Player player, Tile tile) {
        super(game);
        this.player = player;
        this.tile = tile;
        getTileItemInfo(tile.getTileItemContainer());
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
     * Copies given TileItemContainer
     * @param tic The <code>TileItemContainer</code> to copy from
     */
    public void getTileItemInfo(TileItemContainer tic) {
        if (tic != null) {
            resource = tic.getResource();
            improvements = tic.getImprovements();
            road = tic.getRoad();
            river = tic.getRiver();
            lostCityRumour = tic.getLostCityRumour();
        } else {
            improvements = Collections.emptyList();
        }
    }

    public void setColonyUnitCount(int colonyUnitCount) {
        this.colonyUnitCount = colonyUnitCount;
    }

    public int getColonyUnitCount() {
        return colonyUnitCount;
    }

    public void setColonyStockadeLevel(int colonyStockadeLevel) {
        this.colonyStockadeLevel = colonyStockadeLevel;
    }

    public int getColonyStockadeLevel() {
        return colonyStockadeLevel;
    }

    public boolean hasRoad() {
        return (road != null);
    }

    public TileImprovement getRoad() {
        return road;
    }

    public boolean hasRiver() {
        return (river != null);
    }

    public TileImprovement getRiver() {
        return river;
    }

    public Resource getResource() {
        return resource;
    }

    public LostCityRumour getLostCityRumour() {
        return lostCityRumour;
    }

    public List<TileImprovement> getImprovements() {
        return improvements;
    }

    public void setLostCityRumour(LostCityRumour lostCityRumour) {
        this.lostCityRumour = lostCityRumour;
    }

    public boolean hasLostCityRumour() {
        return lostCityRumour != null;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setExplored(boolean explored) {
        this.explored = explored;
    }

    /**
     * Checks if this <code>Tile</code> has been explored.
     * 
     * @return <i>true</i> if the tile has been explored.
     */
    public boolean isExplored() {
        return explored;
    }

    public void setSkill(UnitType newSkill) {
        this.skill = newSkill;
    }

    public UnitType getSkill() {
        return skill;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public Player getOwner() {
        return owner;
    }

    public void setWantedGoods(GoodsType[] newWantedGoods) {
        this.wantedGoods = newWantedGoods;
    }

    public GoodsType[] getWantedGoods() {
        return wantedGoods;
    }

    public void setMissionary(Unit missionary) {
        this.missionary = missionary;
    }

    public Unit getMissionary() {
        return missionary;
    }

    public void setVisited() {
        settlementVisited = true;
    }

    public boolean hasBeenVisited() {
        return settlementVisited;
    }

    /**
     * Gets the Player owning this object (not the Tile).
     * 
     * @return The Player of this <code>PlayerExploredTile</code>.
     */
    public Player getPlayer() {
        return player;
    }

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

        if (!explored) {
            out.writeAttribute("explored", Boolean.toString(explored));
        }
        if (tile.getOwner() != owner && owner != null) {
            out.writeAttribute("owner", owner.getId());
        }

        out.writeAttribute("connected", Boolean.toString(connected));

        if (tile.getSettlement() != null) {
            if (tile.getSettlement() instanceof Colony) {
                out.writeAttribute("colonyUnitCount", Integer.toString(colonyUnitCount));
                out.writeAttribute("colonyStockadeLevel", Integer.toString(colonyStockadeLevel));
            } else if (settlementVisited) {
                out.writeAttribute("settlementVisited", Boolean.toString(settlementVisited));
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
            }
        }
        if (tile.hasResource()) {
            resource.toXML(out, player, showAll, toSavedGame);
        }
        if (tile.hasLostCityRumour()) {
            lostCityRumour.toXML(out, player, showAll, toSavedGame);
        }
        for (TileImprovement t : improvements) { 
            t.toXML(out, player, showAll, toSavedGame);
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

        setId(in.getAttributeValue(null, ID_ATTRIBUTE));
        
        player = getFreeColGameObject(in, "player", Player.class);
        tile = getFreeColGameObject(in, "tile", Tile.class);

        explored = getAttribute(in, "explored", true);
        colonyUnitCount = getAttribute(in, "colonyUnitCount", 0);
        colonyStockadeLevel = getAttribute(in, "colonyStockadeLevel", 0);
        connected = getAttribute(in, "connected", false);

        owner = getFreeColGameObject(in, "owner", Player.class, tile.getOwner());

        settlementVisited = getAttribute(in, "settlementVisited", false);
        if (settlementVisited) {
            Specification spec = getSpecification();
            skill = spec.getType(in, "learnableSkill", UnitType.class, null);
            wantedGoods[0] = spec.getType(in, "wantedGoods0", GoodsType.class, null);
            wantedGoods[1] = spec.getType(in, "wantedGoods1", GoodsType.class, null);
            wantedGoods[2] = spec.getType(in, "wantedGoods2", GoodsType.class, null);
        }

        missionary = null;
        TileItemContainer tileItemContainer = new TileItemContainer(getGame(), tile);
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(IndianSettlement.MISSIONARY_TAG_NAME)) {
                in.nextTag(); // advance to the Unit tag
                missionary = (Unit) getGame().getFreeColGameObject(in.getAttributeValue(null, ID_ATTRIBUTE));
                if (missionary == null) {
                    missionary = new Unit(getGame(), in);
                } else {
                    missionary.readFromXML(in);
                }
                in.nextTag(); // close <missionary> tag
            } else if (in.getLocalName().equals(Resource.getXMLElementTagName())) {
                Resource resource = (Resource) getGame().getFreeColGameObject(in.getAttributeValue(null, ID_ATTRIBUTE));
                if (resource != null) {
                    resource.readFromXML(in);
                } else {
                    resource = new Resource(getGame(), in);
                }
                tileItemContainer.addTileItem(resource);
            } else if (in.getLocalName().equals(LostCityRumour.getXMLElementTagName())) {
                LostCityRumour lostCityRumour = (LostCityRumour) getGame().getFreeColGameObject(in.getAttributeValue(null, ID_ATTRIBUTE));
                if (lostCityRumour != null) {
                    lostCityRumour.readFromXML(in);
                } else {
                    lostCityRumour = new LostCityRumour(getGame(), in);
                }
                tileItemContainer.addTileItem(lostCityRumour);
            } else if (in.getLocalName().equals(TileImprovement.getXMLElementTagName())) {
                TileImprovement ti = (TileImprovement) getGame().getFreeColGameObject(in.getAttributeValue(null, ID_ATTRIBUTE));
                if (ti != null) {
                    ti.readFromXML(in);
                } else {
                    ti = new TileImprovement(getGame(), in);
                }
                tileItemContainer.addTileItem(ti);
            } else {
                logger.warning("Unknown tag: " + in.getLocalName() + " loading PlayerExploredTile");
                in.nextTag();
            }
        }
        getTileItemInfo(tileItemContainer);
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
