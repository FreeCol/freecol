/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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
import net.sf.freecol.common.util.Introspector;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.Utils;


/**
 * The superclass of all game objects in FreeCol.
 *
 * All FreeColGameObjects need to be able to refer to the game they belong
 * to.  Therefore, the game attribute must not be null, except in the special
 * case where a Game is being initially created.
 *
 * Most FreeColGameObjects are intended to be accessible by identifier (@see
 * Game#getFreeColObject) but some are not, and should override isInternable
 * to return false.
 */
public abstract class FreeColGameObject extends FreeColObject {

    private static final Logger logger = Logger.getLogger(FreeColGameObject.class.getName());

    /** The game this object belongs to. */
    private Game game;

    /** Has this object been disposed? */
    private boolean disposed = false;

    /** Has this object been initialized? */
    protected boolean initialized;


    /**
     * Creates a new <code>FreeColGameObject</code>.
     *
     * Automatically assign an object identifier and register this
     * object at the specified <code>Game</code>.
     *
     * @param game The enclosing <code>Game</code>.
     */
    public FreeColGameObject(Game game) {
        if (game != null) {
            setGame(game); // Set game before calling internId
            internId(getXMLTagName() + ":" + game.getNextId());
        }
        this.initialized = getId() != null;
        this.disposed = false;
    }

    /**
     * Creates a new <code>FreeColGameObject</code>.
     * If an identifier is supplied, use that, otherwise leave it undefined.
     *
     * This routine should be used when we know that the object will need
     * further initialization.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public FreeColGameObject(Game game, String id) {
        setGame(game); // Set game before calling internId
        if (id != null) internId(id);
        this.initialized = false;
        this.disposed = false;
    }


    /**
     * Instantiate an uninitialized FreeColGameObject within a game.
     *
     * @param <T> The actual return type.
     * @param game The <code>Game</code> to instantiate within.
     * @param returnClass The required <code>FreeColObject</code> class.
     * @return The new uninitialized object, or null on error.
     */
    public static <T extends FreeColObject> T newInstance(Game game,
        Class<T> returnClass) {
        try {
            return Introspector.instantiate(returnClass,
                new Class[] { Game.class, String.class },
                new Object[] { game, (String)null }); // No intern!
        } catch (Introspector.IntrospectorException ex) {}
        // OK, did not work, try the simpler constructors
        return (FreeColSpecObject.class.isAssignableFrom(returnClass))
            ? FreeColSpecObject.newInstance(game.getSpecification(),
                                            returnClass)
            : FreeColObject.newInstance(returnClass);
    }

    /**
     * Sets the unique identifier of this object and registers it in its
     * <code>Game</code> with that identifier, i.e. "intern" this object.
     *
     * @param newId The unique identifier of this object.
     */
    public final void internId(final String newId) {
        final Game game = getGame();
        if (game != null && newId != null && isInternable()) {
            final String oldId = getId();
            if (!newId.equals(oldId)) {
                if (oldId != null) {
                    game.removeFreeColGameObject(oldId, "override");
                }
                setId(newId);
                if (newId != null) {
                    game.setFreeColGameObject(newId, this);
                }
            }
        } else {
            setId(newId);
        }
    }

    /**
     * Has this object been initialized?
     *
     * @return True if this object is initialized.
     */
    public final boolean isInitialized() {
        return this.initialized;
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
     * Low level base dispose, removing the object from the game.
     */
    public final void fundamentalDispose() {
        getGame().removeFreeColGameObject(getId(), "dispose");
        this.disposed = true;
    }


    // Routines to be overridden where meaningful by subclasses.

    /**
     * Should this object be interned into its Game?
     *
     * Usually true, but there are some special containers that have to be
     * FCGOs but are unsuitable to be interned.  These classes will override
     * this routine.
     *
     * @return True if this object should be interned.
     */
    public boolean isInternable() {
        return true;
    }

    /**
     * Collect a list of this object and all its subparts that should be
     * disposed of when this object goes away.
     *
     * Overriding routines should reference this routine, and arrange
     * that the object itself is last.
     *
     * @return A list of <code>FreeColGameObject</code>s to dispose of.
     */
    public List<FreeColGameObject> getDisposeList() {
        List<FreeColGameObject> fcgos = new ArrayList<>();
        fcgos.add(this);
        return fcgos;
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
     * @param fix If true, fix problems if possible.
     * @return Negative if there are problems remaining, zero if
     *     problems were fixed, positive if no problems found at all.
     */
    public int checkIntegrity(boolean fix) {
        return 1;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public Specification getSpecification() {
        return (this.game == null) ? null : this.game.getSpecification();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSpecification(Specification specification) {
        throw new RuntimeException("Can not set specification");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Game getGame() {
        return this.game;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGame(Game game) {
        if (game == null) throw new RuntimeException("Null game");
        this.game = game;
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
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        if (xr.shouldIntern()) internId(getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readFromXML(FreeColXMLReader xr) throws XMLStreamException {
        this.initialized = true;
        super.readFromXML(xr);
    }
}
