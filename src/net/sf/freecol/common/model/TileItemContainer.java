package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;

/**
* Contains <code>TileItem</code>s and can be used by a {@link Tile}
* to make certain tasks easier.
*/
public class TileItemContainer extends FreeColGameObject {
    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(Location.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision: 1.00 $";

    /** The list of TileItems stored in this <code>TileItemContainer</code>. */
    private List<TileImprovement> improvements;
    private Resource resource = null;

    /** Quick Pointers */
    private TileImprovement road = null;
    private TileImprovement river = null;

    /** The owner of this <code>TileItemContainer</code>. */
    private Tile parent;

    // ------------------------------------------------------------ constructor

    /**
     * Creates an empty <code>TileItemContainer</code>.
     *
     * @param game The <code>Game</code> in which this <code>TileItemContainer</code> belong.
     * @param parent The <code>Tile</code> this <code>TileItemContainer</code> will be containg TileItems for.
     */
    public TileItemContainer(Game game, Tile parent) {
        super(game);

        if (parent == null) {
            throw new NullPointerException();
        }

        this.parent = parent;
    }

    /**
     * Initiates a new <code>TileItemContainer</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this <code>TileItemContainer</code>
     *       belong.
     * @param parent The <code>Tile</code> using this <code>TileItemContainer</code>
     *       for storing it's TileItem.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public TileItemContainer(Game game, Tile parent, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        if (parent == null) {
            throw new NullPointerException();
        }

        this.parent = parent;
        readFromXML(in);
    }

    /**
     * Initiates a new <code>TileItemContainer</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this <code>TileItemContainer</code>
     *       belong.
     * @param parent The <code>Tile</code> using this <code>TileItemContainer</code>
     *       for storing it's TileItem.
     * @param e An XML-element that will be used to initialize
     *      this object.
     */
    public TileItemContainer(Game game, Tile parent, Element e) {
        super(game, e);

        if (parent == null) {
            throw new NullPointerException();
        }

        this.parent = parent;
        readFromXMLElement(e);
    }

    // ------------------------------------------------------------ checking/retrieval functions

    // TODO: change name to getTile
    public Tile getParent() {
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

    public void clear() {
        if (hasResource()) {
            resource.dispose();
        }
        for (TileImprovement t : improvements) {
            t.dispose();
        }
        improvements.clear();
        road = null;
        river = null;
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
     * @param separator The separator to be used (eg. "/")
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
     * Adds a <code>TileItem</code> to this containter.
     * @param t The TileItem to add to this container.
     @ @return The added TileItem or the existing TileItem or <code>null</code> on error
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
            // Check all improvements to find any to replace
            String typeId = ((TileImprovement) t).getTypeId();
            if (typeId != null) {
                Iterator<TileImprovement> ti = improvements.iterator();
                while (ti.hasNext()) {
                    TileImprovement imp = ti.next();
                    if (imp.getTypeId().equals(typeId)) {
                        if (imp.getMagnitude() < ((TileImprovement) t).getMagnitude()) {
                            removeTileItem(imp);
                            break;
                        } else {
                            // Found it, but not replacing.
                            return imp;
                        }
                    }
                }
            }
            if (t.hasRoad()) {
                road = t;
            } else if (t.isRiver()) {
                river = t;
            }
            improvements.add(t);
        } else {
            logger.warning("TileItem " + t.class.name() + " has not be implemented yet.");
            return null;
        }
    }

    /**
     * Removes TileItem from this containter.
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
            logger.warning("TileItem " + t.getClass().name() + " has not be implemented yet.");
            return null;
        }
    }
    
    /**
     * Removes all TileItems.
     */
    public void removeAll() {
        resource = null;
        road = null;
        river = null;
        improvements.clear();
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
    * @param t The <code>TileImprovementType</code> to test the presence of.
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
     * Removes all references to this object.
     */
    public void dispose() {
        super.dispose();
    }

    // ------------------------------------------------------------ manipulation methods

    /**
     * Creates a river <code>TileImprovement</code> and adds to this Tile/Container.
     * Checking for overwrite is done by {@link #addTileItem}.
     * @param magnitude The Magnitude of the river to be created
     * @return The new river added, or the existing river TileImprovement
     */
    public TileImprovement addRiver(int magnitude) {
        if (magnitude == 0) {
            return null;
        }
        if (hasRiver()) {
            // Already have a river here, see if magnitude is correct, return existing river.
            if (river.getMagnitude() != magnitude) {
                setRiverMagnitude(magnitude);
            }
            return river;
        }
        // Get the list of ImprovementTypes
        List<TileImprovementType> tiTypeList = FreeCol.getSpecification().getTileImprovementList();
        // Get the first river that matches or is below
        for (TileImprovementType tiType : tiTypeList) {
            if ("river".equals(tiType.getTypeId()) && tiType.getMagnitude() <= magnitude) {
                TileImprovement river = new TileImprovement(getGame(), parent, tiType);
                this.river = river;
                adjustNeighbourRiverStyle(0);
                return addTileItem(river);
            }
        }
        // Don't have any river ImprovementTypes? Throw exception
        throw new RuntimeException("No TileImprovementType with TypeId == river");
    }

    /**
     * Removes the river <code>TileImprovement</code> from this Tile/Container.
     * Change neighbours' River Style with {@link #adjustNeighbourRiverStyle}.
     */
    public TileImprovement removeRiver() {
        return removeTileItem(river);
    }

    /**
     * This function is for adjusting the river magnitude of an existing river in this Tile/Container.
     * <ol>
     * <li>Redirects to appropriate function for adding or removing rivers if intended.
     * <li>Sets the magnitude of the river in this Tile/Container
     * <li>Adjusts the style of this and adjacent Tiles/Containers
     * </ol>
     * @param magnitude The 'size' of the river, 0=none, 1=minor, 2=major
     */
    public void setRiverMagnitude(int magnitude) {
        if (!hasRiver() && magnitude > 0) {
            // No river at the moment, create a new one
            addRiver(magnitude);
            return;
        }
        if (river.getMagnitude() == magnitude) {
            return;
        }
        if (magnitude == 0) {
            // Remove river
            removeRiver();
            return;
        }
        int oldMagnitude = river.getMagnitude();
        // Get the list of ImprovementTypes
        List<TileImprovementType> tiTypeList = FreeCol.getSpecification().getTileImprovementList();
        // Check if there is another river type defined for this magnitude
        for (TileImprovementType tiType : tiTypeList) {
            if ("river".equals(tiType.getTypeId()) && tiType.getMagnitude <= magnitude) {
                if (tiType != river.getType()) {
                    // Has a different river type for this magnitude
                    TileImprovement r = new TileImprovement(getGame(), parent, tiType);
                    this.river = river;
                } else {
                    // Same river type, adjust magnitude
                    river.setMagnitude(magnitude);
                }
                adjustNeighbourRiverStyle(oldMagnitude);
                return;
            }
        }
        // Don't have any river ImprovementTypes? Throw exception
        throw new RuntimeException("No TileImprovementType with TypeId == river");
    }

    /**
     * Call this function after changing River Magnitude or when adding/removing a River
     * @param oldMagnitude The magnitude of the River before the change
     */
    public void adjustNeighbourRiverStyle(int oldMagnitude) {
        if (river == null || river.getMagnitude() == oldMagnitude) {
            return;
        }
        int magChange = river.getMagnitude() - oldMagnitude;
        int[] directions = {Map.NE, Map.SE, Map.SW, Map.NW};
        int[] base = Map.getBase(directions, 3);
        for (int i = 0; i < directions.length; i++) {
            Tile t = getParent().getMap().getNeighbourOrNull(directions[i], this);
            if (t == null) {
                continue;
            }
            int otherRiver = t.getRiver();
            if (otherRiver == null) {   // This tile doesn't have a river   
                continue;
            }
            int otherDirection = Map.getReverseDirection(directions[i]);
            otherRiver.addStyle(base[otherDirection] * magChange);
        }
    }

    public int getRiverStyle() {
        if (river == null) {
            return 0;
        }
        return river.getStyle();
    }

    public void updateRiver() {
        if (river == null) {
            return;
        }
        int[] directions = {Map.NE, Map.SE, Map.SW, Map.NW};
        int[] base = Map.getBase(directions, 3);
        style = 0;
        for (int i = 0; i < directions.length; i++) {
            Tile t = getParent().getMap().getNeighbourOrNull(directions[i], this);
            if (t == null) {
                continue;
            }
            style += base[i] * t.getRiverStyle();
        }
    }

    /**
     * Removes all references to this object.
     */
    /*
    public void dispose() {
        disposeAllTileItems();
        super.dispose();
    }
    */

    /**
     * Disposes all <code>TileItem</code>s in this <code>TileItemContainer</code>.
     */
    public void disposeAllTileItems() {
        if (hasResource()) {
            resource.dispose();
            resource = null;
        }
        while(! improvements.isEmpty()) {
            TileImprovement ti = improvements.remove(improvements.size() - 1);
            ti.dispose();
        }
        road = null;
        river = null;
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
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("ID", getID());
        out.writeAttribute("tile", tile.getID());

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
        setID(in.getAttributeValue(null, "ID"));

        tile = (Tile) getGame().getFreeColGameObject(in.getAttributeValue(null, "tile"));
        if (tile == null) {
            tile = new Tile(getGame(), in.getAttributeValue(null, "tile"));
        }

        improvements.clear();

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(Resource.getXMLElementTagName())) {
                resource = (Resource) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (resource != null) {
                    resource.readFromXML(in);
                } else {
                    resource = new Resource(getGame(), tile, in);
                }
            } else if (in.getLocalName().equals(TileImprovement.getXMLElementTagName())) {
                TileImprovement ti = (TileImprovement) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (ti != null) {
                    ti.readFromXML(in);
                    if (!improvements.contains(ti)) {
                        improvements.add(ti);
                    }
                } else {
                    ti = new TileImprovement(getGame(), tile, in);
                    improvements.add(ti);
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
        StringBuffer sb = new StringBuffer();
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
