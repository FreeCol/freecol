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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.Utils;

import org.w3c.dom.Element;


/**
 * The superclass of all game objects in FreeCol.
 *
 * All FreeColGameObjects need to be able to refer to the game they belong
 * to.  Therefore, the game attribute must not be null.
 */
public abstract class FreeColGameObject extends FreeColObject {

    private static final Logger logger = Logger.getLogger(FreeColGameObject.class.getName());

    /** The game this object belongs to. */
    private Game game;

    /** Has this object been disposed. */
    private boolean disposed = false;

    /** Has this object been initialized. */
    private boolean uninitialized;


    /**
     * Creates a new <code>FreeColGameObject</code>.  Automatically
     * assign an object identifier and register this object at the
     * specified <code>Game</code>, unless this object is a
     * <code>Game</code> in which case it is given an identifier of
     * zero.
     *
     * @param game The <code>Game</code> in which this object belongs.
     */
    public FreeColGameObject(Game game) {
        if (game != null) {
            this.game = game;
            internId(getXMLTagName() + ":" + game.getNextId());

        } else if (this instanceof Game) {
            this.game = (Game)this;
            setId("0");

        } else {
            throw new IllegalArgumentException("FCGO with null game.");
        }
        this.uninitialized = getId() == null;
    }

    /**
     * Creates a new <code>FreeColGameObject</code>.
     * If an identifier is supplied, use that, otherwise leave it undefined.
     *
     * This routine should be used when we intend later to call one of:
     * - {@link #readFromXML(FreeColXMLReader)}
     * - {@link #readFromXMLElement(Element)}
     *
     * @param game The <code>Game</code> in which this object belongs.
     * @param id The object identifier.
     */
    public FreeColGameObject(Game game, String id) {
        if (game == null) {
            throw new IllegalArgumentException("FCGO(id=" + id
                + ") with null game");
        }

        this.game = game;
        if (id != null) internId(id);
        this.uninitialized = true;
    }


    /**
     * Gets the game object this <code>FreeColGameObject</code> belongs to.
     *
     * @return The <code>Game</code> this object belongs to.
     */
    public Game getGame() {
        return game;
    }

    /**
     * Sets the game object this <code>FreeColGameObject</code> belongs to.
     *
     * @param game The <code>Game</code> to set.
     */
    public void setGame(Game game) {
        this.game = game;
    }

    /**
     * Has this object not yet been initialized?
     *
     * @return True if this object is not initialized.
     */
    public boolean isUninitialized() {
        return this.uninitialized;
    }

    /**
     * Has this object been disposed?
     *
     * @return True if this object has been disposed.
     * @see #dispose
     */
    public final boolean isDisposed() {
        return this.disposed;
    }

    /**
     * Collect a list of this object and all its subparts that should be
     * disposed of when this object goes away.  Arrange that the object
     * itself is last.
     *
     * To be overridden in subclasses, but reference this routine.
     *
     * @return A list of <code>FreeColGameObject</code>s to dispose of.
     */
    public List<FreeColGameObject> getDisposeList() {
        List<FreeColGameObject> fcgos = new ArrayList<>();
        fcgos.add(this);
        return fcgos;
    }

    /**
     * Low level base dispose, removing the object from the game.
     */
    public final void fundamentalDispose() {
        getGame().removeFreeColGameObject(getId(), "dispose");
        this.disposed = true;
    }

    /**
     * Dispose of the resources of this object, and finally remove it from the
     * game.
     *
     * To be extended by subclasses, but they must tail call up
     * towards this.
     */
    public void disposeResources() {
        fundamentalDispose();
    }

    /**
     * Destroy this object and all its parts, releasing resources and
     * removing references.
     */
    public final void dispose() {
        if (this.disposed) return;
        LogBuilder lb = new LogBuilder(64);
        lb.add("Destroying:");
        for (FreeColGameObject fcgo : getDisposeList()) {
            lb.add(" ", fcgo.getId());
            fcgo.disposeResources();
        }
        lb.log(logger, Level.INFO);
    }

    /**
     * "Clone" this FreeColGameObject by serializing it and creating a
     * new object from the resulting XML.  We need to pass the result
     * class, since the object we are about to "clone" is likely a
     * server object.
     *
     * @param returnClass The class to clone.
     * @return The "clone" of the <code>FreeColGameObject</code>.
     */
    public <T extends FreeColGameObject> T cloneFreeColGameObject(Class<T> returnClass) {
        final Game game = getGame();
        try {
            String xml = this.serialize();

            Field nextId = Game.class.getDeclaredField("nextId");
            nextId.setAccessible(true);
            int id = nextId.getInt(game);
            nextId.setInt(game, id + 1);
            xml = xml.replace(getId(), T.getXMLElementTagName() + ":" + id);

            return game.unserialize(xml, returnClass);

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to clone " + getId(), e);
        }
        return null;
    }

    /**
     * Get a suitable game object to use as a clickable link in messages
     * to a player.
     *
     * Objects do not have links by default, hence the null return
     * here.  However, for example, a player's colony should return
     * itself as a link target.
     *
     * @param player The <code>Player</code> to make a link for.
     * @return A suitable link target if available, although usually null.
     */
    public FreeColGameObject getLinkTarget(Player player) {
        return null;
    }

    /**
     * Checks the integrity of this game object.
     *
     * To be overridden by subclasses where this is meaningful.
     * 
     * @param fix If true, fix problems if possible.
     * @return Negative if there are problems remaining, zero if
     *     problems were fixed, positive if no problems found at all.
     */
    public int checkIntegrity(boolean fix) {
        return 1;
    }


    // Override FreeColObject

    /**
     * Sets the unique identifier of this object and registers it in its
     * <code>Game</code> with that identifier, i.e. "intern" this object.
     *
     * @param newId The unique identifier of this object.
     */
    @Override
    public final void internId(final String newId) {
        if (game != null && !(this instanceof Game)) {
            if (!newId.equals(getId())) {
                FreeColObject ret = null;
                if (getId() != null) {
                    game.removeFreeColGameObject(getId(), "override");
                }

                setId(newId);
                game.setFreeColGameObject(newId, this);
            }
        } else {
            setId(newId);
        }
    }

    /**
     * Get the specification for this game object.
     *
     * @return The <code>Specification</code> of this game.
     */
    @Override
    public Specification getSpecification() {
        return (game == null) ? null : game.getSpecification();
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof FreeColGameObject) {
            // FreeColGameObjects are equal if the two fcgos are in
            // the same game and have the same identifier.
            FreeColGameObject fco = (FreeColGameObject)o;
            return this.getGame() == fco.getGame()
                && super.equals(o);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        return 31 * hash + Utils.hashCode(this.game);
    }


    // Serialization


    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param xr The input <code>FreeColXMLReader</code>.
     * @exception XMLStreamException if there problems reading the stream.
     */
    @Override
    public final void readFromXML(FreeColXMLReader xr) throws XMLStreamException {
        uninitialized = false;
        super.readFromXML(xr);
    }

    /**
     * Gets the tag name of the root element representing this object.
     * This method should be overwritten by any sub-class, preferably
     * with the name of the class with the first letter in lower case.
     *
     * @return "unknown".
     */
    public static String getXMLElementTagName() {
        return "unknown";
    }
}
