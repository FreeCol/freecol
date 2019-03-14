/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.CollectionUtils.*;
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

    /** Default class index for FreeColGameObjects. */
    private static final int FREECOL_GAME_OBJECT_CLASS_INDEX = 50;

    /** The game this object belongs to. */
    private Game game;

    /** Has this object been disposed? */
    private boolean disposed = false;

    /** Has this object been initialized? */
    protected boolean initialized;


    /**
     * Special constructor solely for initializing a Game.
     *
     * Do *not* call internId as the Game is not ready to register objects.
     */
    protected FreeColGameObject() {
        this.game = (Game)this;
        this.disposed = false;
        this.initialized = false;
    }
    
    /**
     * Create and initialize a new {@code FreeColGameObject}.
     *
     * @param game The enclosing {@code Game}.
     */
    public FreeColGameObject(Game game) {
        this.game = game; // Set game before calling internId
        internId(getXMLTagName() + ":" + game.getNextId());
        this.disposed = false;
        this.initialized = getId() != null;
    }        

    /**
     * Creates a new {@code FreeColGameObject}.
     * If an identifier is supplied, use that, otherwise leave it undefined.
     *
     * This routine should be used when we know that the object will need
     * further initialization.
     *
     * @param game The enclosing {@code Game}.
     * @param id The object identifier.
     */
    public FreeColGameObject(Game game, String id) {
        this.game = game;
        internId(id);
        this.initialized = false;
        this.disposed = false;
    }

    /**
     * Sets the unique identifier of this object and registers it in its
     * {@code Game} with that identifier, i.e. "intern" this object.
     *
     * @param newId The unique identifier of this object.
     */
    public final void internId(final String newId) {
        if (this.game != null && newId != null && isInternable()) {
            final String oldId = getId();
            if (newId.equals(oldId)) {
                // This happens when the client receives an update.
                this.game.setFreeColGameObject(newId, this);
            } else {
                if (oldId != null) {
                    this.game.removeFreeColGameObject(oldId, "override");
                }
                setId(newId);
                this.game.addFreeColGameObject(newId, this);
            }
        } else {
            setId(newId);
        }
    }

    /**
     * Intern this object.
     */
    public void intern() {
        internId(getId());
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
        for (FreeColGameObject fcgo : toList(getDisposables())) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        FreeColGameObject fcgo = copyInCast(other, FreeColGameObject.class);
        if (fcgo == null || !super.copyIn(fcgo)) return false;
        final Game game = getGame();
        this.game = game.updateRef(fcgo.getGame());
        this.disposed = fcgo.isDisposed();
        this.initialized = fcgo.isInitialized();
        return true;
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
     * Collect this object and all its subparts that should be
     * disposed of when this object goes away.
     *
     * Overriding routines should call upwards towards this routine,
     * arranging that the object itself is last.
     *
     * @return A stream of {@code FreeColGameObject}s to dispose of.
     */
    public Stream<FreeColGameObject> getDisposables() {
        return Stream.of(this);
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
     * @param player The {@code Player} to make a link for.
     * @return A suitable link target if available, although usually null.
     */
    public FreeColGameObject getLinkTarget(Player player) {
        return null;
    }

    /**
     * Checks the integrity of this game object.
     *
     * @param fix If true, fix problems if possible.
     * @param lb A {@code LogBuilder} to log to.
     * @return A suitable {@code IntegrityType}.
     */
    public IntegrityType checkIntegrity(boolean fix, LogBuilder lb) {
        return IntegrityType.INTEGRITY_GOOD;
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
        throw new RuntimeException("Can not set specification for: " + this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Game getGame() {
        return this.game;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setGame(Game game) {
        if (game == null) throw new RuntimeException("Null game: " + this);
        this.game = game;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getClassIndex () {
        return FREECOL_GAME_OBJECT_CLASS_INDEX;
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

    // getXMLTagName left to subclasses


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
            FreeColGameObject other = (FreeColGameObject)o;
            return this.getGame() == other.getGame()
                && super.equals(other);
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
}
