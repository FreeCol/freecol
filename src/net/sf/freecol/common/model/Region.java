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
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * A named region on the map.
 */
public class Region extends FreeColGameObject implements Nameable {

    private static final Logger logger = Logger.getLogger(Region.class.getName());

    public static final String PACIFIC_NAME_KEY = "model.region.pacific";
    public static final String CHILD_TAG = "child";

    public static enum RegionType {
        OCEAN,
        COAST,
        LAKE,
        RIVER,
        LAND,
        MOUNTAIN,
        DESERT;

        /**
         * Gets a name index key for this region type.
         *
         * @return A name index key.
         */
        public String getNameIndexKey() {
            return "index." + toString().toLowerCase(Locale.US);
        }
    }

    /**
     * The name of this Region.
     */
    protected String name;

    /**
     * Key used to retrieve description from Messages.
     */
    protected String nameKey;

    /**
     * The parent Region of this Region.
     */
    protected Region parent;

    /**
     * Whether this Region is claimable. Ocean Regions and non-leaf
     * Regions should not be claimable.
     */
    protected boolean claimable = false;

    /**
     * Whether this Region is discoverable. The Eastern Ocean regions
     * should not be discoverable. In general, non-leaf regions should
     * not be discoverable. The Pacific Ocean is an exception, however.
     */
    protected boolean discoverable = false;

    /**
     * Which Turn the Region was discovered in.
     */
    protected Turn discoveredIn;

    /**
     * Which Player the Region was discovered by.
     */
    protected Player discoveredBy;

    /**
     * Whether the Region is already discovered when the game starts.
     */
    protected boolean prediscovered = false;

    /**
     * How much discovering this Region contributes to your score.
     * This should be zero unless the Region is discoverable.
     */
    protected int scoreValue = 0;

    /**
     * Describe type here.
     */
    protected RegionType type;

    /**
     * The children Regions of this Region.
     */
    protected List<Region> children = new ArrayList<Region>();


    /**
     * Creates a new <code>Region</code> instance.
     *
     * @param game a <code>Game</code> value
     */
    public Region(Game game) {
        super(game);
    }

    /**
     * Creates a new <code>Region</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param id a <code>String</code> value
     */
    public Region(Game game, String id) {
        super(game, id);
    }

    /**
     * Initiates a new <code>Region</code> from an XML representation.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if an error occurred during parsing.
     */
    public Region(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, null);

        readFromXML(in);
    }

    /**
     * Get the explicit region name.
     *
     * @return The name, or null if it does not have one.
     */
    public final String getName() {
        return name;
    }

    /**
     * Sets the region name.
     *
     * @param newName The new name value.
     */
    public final void setName(final String newName) {
        this.name = newName;
    }

    /**
     * Gets the name key.
     *
     * @return The name key.
     */
    public final String getNameKey() {
        return nameKey;
    }

    /**
     * Set the name key.
     *
     * @param newNameKey The new name key.
     */
    public final void setNameKey(final String newNameKey) {
        this.nameKey = newNameKey;
    }

    /**
     * Returns <code>true</code> if this Region is the Pacific
     * Ocean. The Pacific Ocean is special in so far as it is the only
     * Region that could be discovered in the original game.
     *
     * @return True if this region is the Pacific.
     */
    public boolean isPacific() {
        if (PACIFIC_NAME_KEY.equals(nameKey)) {
            return true;
        } else if (parent != null) {
            return parent.isPacific();
        } else {
            return false;
        }
    }

    /**
     * Returns the name or default name of this Region.
     *
     * @return The i18n-ready name for the region.
     */
    public StringTemplate getLabel() {
        if (prediscovered || isPacific()) {
            return StringTemplate.key(nameKey);
        } else if (name == null) {
            return StringTemplate.key("model.region."
                + type.toString().toLowerCase(Locale.US) + ".unknown");
        } else {
            return StringTemplate.name(name);
        }
    }

    public String getTypeNameKey() {
        return "model.region." + type.toString().toLowerCase(Locale.US)
            + ".name";
    }

    /**
     * Gets the enclosing parent region.
     *
     * @return The parent region
     */
    public final Region getParent() {
        return parent;
    }

    /**
     * Sets the parent region.
     *
     * @param newParent The new parent region.
     */
    public final void setParent(final Region newParent) {
        this.parent = newParent;
    }

    /**
     * Get a list of the child regions.
     *
     * @return The child regions.
     */
    public final List<Region> getChildren() {
        if (children == null) return Collections.emptyList();
        return children;
    }

    /**
     * Sets the child regions.
     *
     * @param newChildren The new child regions.
     */
    public final void setChildren(final List<Region> newChildren) {
        this.children = newChildren;
    }

    /**
     * Add a child region to this region.
     *
     * @param child The child <code>Region</code> to add.
     */
    public void addChild(Region child) {
        children.add(child);
    }

    /**
     * Can this region be claimed?
     *
     * @return True if the region can be claimed.
     */
    public final boolean isClaimable() {
        return claimable;
    }

    /**
     * Set the claimable value.
     *
     * @param newClaimable The new claimable value.
     */
    public final void setClaimable(final boolean newClaimable) {
        this.claimable = newClaimable;
    }

    /**
     * Can this region be discovered?
     *
     * @return True if the region can be discovered.
     */
    public final boolean isDiscoverable() {
        return discoverable;
    }

    /**
     * Set the discoverable value.
     *
     * @param newDiscoverable The new discoverable value.
     */
    public final void setDiscoverable(final boolean newDiscoverable) {
        this.discoverable = newDiscoverable;
        if (discoverable) {
            prediscovered = false;
        }
    }

    /**
     * Returns a discoverable Region or null. If this region is
     * discoverable, it is returned. If not, a discoverable parent is
     * returned, unless there is none. This is intended for
     * discovering the Pacific Ocean when discovering one of its
     * sub-Regions.
     *
     * @return A discoverable a region, or null if none found.
     */
    public Region getDiscoverableRegion() {
        if (isDiscoverable()) {
            return this;
        } else if (parent != null) {
            return parent.getDiscoverableRegion();
        } else {
            return null;
        }
    }

    /**
     * Gets the turn the region was discovered in.
     *
     * @return The discovery turn.
     */
    public final Turn getDiscoveredIn() {
        return discoveredIn;
    }

    /**
     * Sets the discovery turn.
     *
     * @param newDiscoveredIn The new discoveredy turn.
     */
    public final void setDiscoveredIn(final Turn newDiscoveredIn) {
        this.discoveredIn = newDiscoveredIn;
    }

    /**
     * Gets the player that discovered the region.
     *
     * @return The discovering <code>Player</code>.
     */
    public final Player getDiscoveredBy() {
        return discoveredBy;
    }

    /**
     * Sets the discovering player.
     *
     * @param newDiscoveredBy The new discovering player.
     */
    public final void setDiscoveredBy(final Player newDiscoveredBy) {
        this.discoveredBy = newDiscoveredBy;
    }

    /**
     * Is the region pre-discovered (e.g. the Atlantic).
     *
     * @return True if the region is prediscovered.
     */
    public final boolean isPrediscovered() {
        return prediscovered;
    }

    /**
     * Sets the prediscovered value.
     *
     * @param newPrediscovered The new prediscovered value.
     */
    public final void setPrediscovered(final boolean newPrediscovered) {
        this.prediscovered = newPrediscovered;
    }

    /**
     * Mark the Region as discovered.
     *
     * @param player The discovering <code>Player</code>.
     * @param turn The discovery <code>Turn</code>.
     * @param newName The name of the region.
     */
    public HistoryEvent discover(Player player, Turn turn, String newName) {
        discoveredBy = player;
        discoveredIn = turn;
        name = newName;
        discoverable = false;
        if (getSpecification().getBoolean(GameOptions.EXPLORATION_POINTS)
            || isPacific()) {
            player.modifyScore(getScoreValue());
        }
        for (Region r : getChildren()) r.setDiscoverable(false);
        return new HistoryEvent(turn, HistoryEvent.EventType.DISCOVER_REGION)
            .addStringTemplate("%nation%", player.getNationName())
            .addName("%region%", newName);
    }

    /**
     * Gets the score for discovering this region.
     *
     * @return The score.
     */
    public final int getScoreValue() {
        return scoreValue;
    }

    /**
     * Sets the score for discovering this region.
     *
     * @param newScoreValue The new score.
     */
    public final void setScoreValue(final int newScoreValue) {
        this.scoreValue = newScoreValue;
    }

    /**
     * Gets the type of the region.
     *
     * @return The region type.
     */
    public final RegionType getType() {
        return type;
    }

    /**
     * Sets the region type.
     *
     * @param newType The new type value.
     */
    public final void setType(final RegionType newType) {
        this.type = newType;
    }

    /**
     * Returns true if this is a leaf node.
     *
     * @return True if the region has no children.
     */
    public boolean isLeaf() {
        return children == null;
    }


    // Serialization

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
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation should be
     *            made for, or <code>null</code> if
     *            <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> will be
     *            added to the representation if <code>showAll</code> is set
     *            to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that is only
     *            needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out, Player player,
                             boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        out.writeAttribute("nameKey", nameKey);
        writeAttribute(out, "type", type);
        if (name != null) {
            out.writeAttribute("name", name);
        }
        if (prediscovered) {
            out.writeAttribute("prediscovered", Boolean.toString(prediscovered));
        }
        if (claimable) {
            out.writeAttribute("claimable", Boolean.toString(claimable));
        }
        if (discoverable) {
            out.writeAttribute("discoverable", Boolean.toString(discoverable));
        }
        if (parent != null) {
            out.writeAttribute("parent", parent.getId());
        }
        if (discoveredIn != null) {
            out.writeAttribute("discoveredIn", String.valueOf(discoveredIn.getNumber()));
        }
        if (discoveredBy != null) {
            out.writeAttribute("discoveredBy", discoveredBy.getId());
        }
        if (scoreValue > 0) {
            out.writeAttribute("scoreValue", String.valueOf(scoreValue));
        }
        if (children != null) {
            for (Region child : children) {
                out.writeStartElement(CHILD_TAG);
                out.writeAttribute(ID_ATTRIBUTE_TAG, child.getId());
                out.writeEndElement();
            }
        }
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);
        nameKey = in.getAttributeValue(null, "nameKey");
        name = in.getAttributeValue(null, "name");
        claimable = getAttribute(in, "claimable", false);
        discoverable = getAttribute(in, "discoverable", false);
        prediscovered = getAttribute(in, "prediscovered", false);
        scoreValue = getAttribute(in, "scoreValue", 0);
        type = getAttribute(in, "type", RegionType.class, (RegionType)null);
        int turn = getAttribute(in, "discoveredIn", -1);
        if (turn > 0) {
            discoveredIn = new Turn(turn);
        }

        discoveredBy = makeFreeColGameObject(in, "discoveredBy", Player.class);
        parent = makeFreeColGameObject(in, "parent", Region.class);
    }

    public void readChildren(XMLStreamReader in) throws XMLStreamException {
        children = new ArrayList<Region>();
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            final String tag = in.getLocalName();
            if (tag.equals("children")) {
                // TODO: remove support for old format
                String[] childArray = readFromArrayElement("children", in, new String[0]);
                for (String child : childArray) {
                    children.add(getGame().getMap().getRegion(child));
                }
            } else if (CHILD_TAG.equals(tag)) {
                children.add(makeFreeColGameObject(in, ID_ATTRIBUTE_TAG, Region.class));
                in.nextTag();
            } else {
                logger.warning("Bad Region tag: " + tag);
            }
        }
        if (children.isEmpty()) {
            children = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[Region " + getId()
            + " " + ((name == null) ? "(null)" : name)
            + " " + nameKey + " " + type + "]";
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "region".
     */
    public static String getXMLElementTagName() {
        return "region";
    }
}
