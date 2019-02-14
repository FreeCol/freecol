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

package net.sf.freecol.common.networking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent to choose a founding father.
 */
public class ChooseFoundingFatherMessage extends AttributeMessage {

    public static final String TAG = "chooseFoundingFather";
    private static final String FOUNDING_FATHER_TAG = "foundingFather";
    private static final List<String> fatherKeys = Collections.<String>
        unmodifiableList(transform(FoundingFatherType.values(),
                                   alwaysTrue(), FoundingFatherType::getKey));


    /**
     * Create a new {@code ChooseFoundingFatherMessage} with the specified
     * fathers.
     *
     * @param fathers The {@code FoundingFather}s to choose from.
     * @param ff The {@code FoundingFather} to select.
     */
    public ChooseFoundingFatherMessage(List<FoundingFather> fathers,
                                       FoundingFather ff) {
        super(TAG);

        setFatherAttributes(fathers);
        if (ff != null) setStringAttribute(FOUNDING_FATHER_TAG, ff.getId());
    }

    /**
     * Create a new {@code ChooseFoundingFatherMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public ChooseFoundingFatherMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, getAttributeMap(xr));

        xr.closeTag(TAG);
    }


    /**
     * Set the attributes arising from a list of founding fathers.
     *
     * @param fathers A list of {@code FoundingFather}.
     */
    private void setFatherAttributes(List<FoundingFather> fathers) {
        setStringAttributeMap(transform(fathers, alwaysTrue(),
                Function.<FoundingFather>identity(),
                Collectors.toMap(ff -> ff.getType().getKey(),
                                 FoundingFather::getId)));
    }

    /**
     * Get a map of attributes from the reader.
     *
     * @param xr The {@code FreeColXMLReader} to query.
     * @return A map of attributes.
     */
    private static Map<String, String> getAttributeMap(FreeColXMLReader xr) {
        Map<String, String> ret = new HashMap<>(fatherKeys.size());
        for (String key : fatherKeys) {
            String val = xr.getAttribute(key, (String)null);
            ret.put(key, val);
        }
        String ffk = xr.getAttribute(FOUNDING_FATHER_TAG, (String)null);
        if (ffk != null) ret.put(FOUNDING_FATHER_TAG, ffk);
        return ret;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean currentPlayerMessage() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.EARLY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void aiHandler(FreeColServer freeColServer, AIPlayer aiPlayer) {
        final Game game = freeColServer.getGame();
        final List<FoundingFather> fathers = getFathers(game);

        aiPlayer.chooseFoundingFatherHandler(fathers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final Game game = freeColClient.getGame();
        final List<FoundingFather> fathers = getFathers(game);
        
        igc(freeColClient).chooseFoundingFatherHandler(fathers);
        clientGeneric(freeColClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = freeColServer.getGame();
        final FoundingFather ff = getFather(game);

        if (ff == null) {
            return serverPlayer.clientError("No founding father selected");
        }        

        return igc(freeColServer)
            .chooseFoundingFather(serverPlayer, ff);
    }


    // Public interface

    /**
     * Get the chosen father.
     *
     * @param game The {@code Game} to lookup the father in.
     * @return The chosen {@code FoundingFather}, or null if none set.
     */
    public final FoundingFather getFather(Game game) {
        String id = getStringAttribute(FOUNDING_FATHER_TAG);
        return (id == null) ? null
            : game.getSpecification().getFoundingFather(id);
    }

    /**
     * Sets the chosen father.
     *
     * @param ff The {@code FoundingFather} to choose.
     * @return This message.
     */
    public final ChooseFoundingFatherMessage setFather(FoundingFather ff) {
        setStringAttribute(FOUNDING_FATHER_TAG, (ff == null) ? null : ff.getId());
        return this;
    }

    /**
     * Get the list of offered fathers.
     *
     * @param game The FreeCol game being played.
     * @return The offered {@code FoundingFather}s.
     */
    public final List<FoundingFather> getFathers(Game game) {
        final Specification spec = game.getSpecification();
        return transform(fatherKeys, tid -> hasAttribute(tid),
                         tid -> spec.getFoundingFather(getStringAttribute(tid)));
    }
}
