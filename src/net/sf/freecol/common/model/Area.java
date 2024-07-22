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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * An area of the map. Areas can be used to define starting locations and other
 * behavior where a list of tiles is needed.
 * 
 * Note that areas can overlap.
 * 
 * @see Region
 */
public class Area extends FreeColGameObject {

    public static final String TAG = "area";
    
    public static final String PREFIX_PLAYER_STARTING_POSITION = "model.area.starting."; 

    private String nameKey = null;
    private String name = null;
    
    private List<Tile> tiles = new ArrayList<>();
    

    /**
     * Creates a new {@code Region} instance.
     *
     * @param game The enclosing {@code Game}.
     */
    public Area(Game game) {
        super(game);
    }
    
    /**
     * Creates a new {@code Region} instance.
     *
     * @param game The enclosing {@code Game}.
     * @param id The object identifier.
     */
    public Area(Game game, String id) {
        super(game, id);
    }        

    /**
     * Creates a new {@code Region} instance.
     *
     * @param game The enclosing {@code Game}.
     * @param id The object identifier.
     * @param nameKey A key for getting a translatable name of this area.
     */
    public Area(Game game, String id, String nameKey) {
        super(game, id);
        
        this.nameKey = nameKey;
    }
    
    public Area(Game game, Area copyFrom) {
        super(game, copyFrom.getId());
        this.name = copyFrom.name;
        this.nameKey = copyFrom.nameKey;
        for (Tile copyFromTile : copyFrom.tiles) {
            tiles.add(game.getMap().getTile(copyFromTile.getX(), copyFromTile.getY()));
        }
    }

    
    /**
     * Gets a list of tiles within this {@code Area}.
     */
    public List<Tile> getTiles() {
        return Collections.unmodifiableList(tiles);
    }
    
    /**
     * Adds a new tile to the area.
     * @param tile The tile to be added.
     */
    public void addTile(Tile tile) {
        tiles.add(tile);
    }
    
    /**
     * Checks if this area contains the given tile.
     * @param tile The tile to checked,
     */
    public boolean containsTile(Tile tile) {
        return tiles.contains(tile);
    }
    
    /**
     * Removes a tile from the area.
     * @param tile The tile to be removed.
     */
    public void removeTile(Tile tile) {
        tiles.remove(tile);
    }
    
    /**
     * Gets the name of this area.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the name of this area.
     * @param name The name.
     */
    public void setName(String name) {
        if (nameKey != null) {
            throw new IllegalArgumentException("Cannot change the name of areas with a fixed nameKey.");
        }
        this.name = name;
    }
    
    /**
     * Gets a i18n key for naming this area.
     * @return The key to be used with {@link Messages#message(String)}.
     */
    public String getNameKey() {
        return nameKey;
    }
    
    /**
     * Checks if this area has no tiles.
     * @return {@code true} if there are no tiles attached to this area.
     */
    public boolean isEmpty() {
        return tiles.isEmpty();
    }
    
    /**
     * Returns the color to be used for displaying the area in the map editor.
     * @return The color that can be used by the map editor.
     */
    public Color getColor() {
        if (getId().startsWith(PREFIX_PLAYER_STARTING_POSITION)) {
            final String nationId = getId().substring(PREFIX_PLAYER_STARTING_POSITION.length());
            final Nation nation = getGame().getSpecification().getAlreadyInitializedType(nationId, Nation.class);
            if (nation != null) {
                return nation.getColor();
            }
        }
        final int hashCode = getId().hashCode();
        final int r = hashCode % 256;
        final int g = (hashCode / 256) % 256;
        final int b = (hashCode / (256 * 256)) % 256;
        return new Color(r, g, b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        Area o = copyInCast(other, Area.class);
        if (o == null || !super.copyIn(o)) return false;
        final Game game = getGame();
        this.tiles = game.updateRef(tiles);
        return true;
    }


    // Serialization

    private static final String TILE_REFERENCE_TAG = "tileReference";
    private static final String NAME_KEY_TAG = "nameKey";
    private static final String NAME_TAG = "name";
    

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);
        
        if (nameKey != null) {
            xw.writeAttribute(NAME_KEY_TAG, nameKey);
        }
        
        if (name != null) {
            xw.writeAttribute(NAME_TAG, name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (Tile tile : tiles) {
            xw.writeStartElement(TILE_REFERENCE_TAG);
            xw.writeAttribute(ID_ATTRIBUTE_TAG, tile.getId());
            xw.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);
        
        nameKey = xr.getAttribute(NAME_KEY_TAG, (String)null);
        name = xr.getAttribute(NAME_TAG, (String)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        this.tiles = new ArrayList<>();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (TILE_REFERENCE_TAG.equals(tag)) {
            tiles.add(xr.makeFreeColObject(getGame(), ID_ATTRIBUTE_TAG, Tile.class, true));
            xr.closeTag(TILE_REFERENCE_TAG);
        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() {
        return TAG;
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[" + getId() + "]";
    }
}
