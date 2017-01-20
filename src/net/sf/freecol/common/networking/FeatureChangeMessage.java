/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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

package net.sf.freecol.common.networking;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import net.sf.freecol.common.util.DOMUtils;
import org.w3c.dom.Element;


/**
 * The message sent when to add or remove a feature.
 */
public class FeatureChangeMessage extends DOMMessage {

    public static final String TAG = "featureChange";
    private static final String ADD_TAG = "add";
    private static final String ID_TAG = FreeColObject.ID_ATTRIBUTE_TAG;

    private FreeColObject fco = null;


    /**
     * Create a new {@code FeatureChangeMessage} for the game object
     * and feature.
     *
     * @param fcgo The parent {@code FreeColGameObject} to manipulate.
     * @param fco The {@code FreeColObject} to add or remove.
     * @param add If true the object is added.
     */
    public FeatureChangeMessage(FreeColGameObject fcgo, FreeColObject fco,
                                boolean add) {
        super(TAG, ID_TAG, fcgo.getId(),
              ADD_TAG, String.valueOf(add));

        this.fco = fco;
    }

    /**
     * Create a new {@code FeatureChangeMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public FeatureChangeMessage(Game game, Element element) {
        super(TAG, ID_TAG, getStringAttribute(element, ID_TAG),
              ADD_TAG, getStringAttribute(element, ADD_TAG));

        this.fco = DOMUtils.getChild(game, element, 0);
    }


    // Public interface

    /**
     * Get the parent object to add/remove to.
     *
     * @param game The {@code Game} to look in.
     * @return The parent {@code FreeColGameObject}.
     */
    public FreeColGameObject getParent(Game game) {
        return game.getFreeColGameObject(getStringAttribute(ID_TAG));
    }

    /**
     * Get the child object to add/remove.
     *
     * @return The child {@code FreeColObject}.
     */
    public FreeColObject getChild() {
        return this.fco;
    }

    /**
     * Get the add/remove state.
     *
     * @return True if the child object should be added to the parent.
     */
    public boolean getAdd() {
        return getBooleanAttribute(ADD_TAG, false);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        return null; // Only sent to client
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG,
            ID_TAG, getStringAttribute(ID_TAG),
            ADD_TAG, getStringAttribute(ADD_TAG))
            .add(this.fco).toXMLElement();
    }
}
