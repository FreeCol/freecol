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

package net.sf.freecol.common.networking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent to choose a founding father.
 */
public class ChooseFoundingFatherMessage extends DOMMessage {

    public static final String TAG = "chooseFoundingFather";
    private static final String FOUNDING_FATHER_TAG = "foundingFather";
    private static final List<String> fatherKeys
        = Collections.<String>unmodifiableList(transform(FoundingFatherType.values(),
                alwaysTrue(), ft -> ft.toString()));

    /** The fathers to offer. */
    private final List<FoundingFather> fathers;

    /** The selected father. */
    private String foundingFatherId;


    /**
     * Create a new {@code ChooseFoundingFatherMessage} with the specified
     * fathers.
     *
     * @param fathers The {@code FoundingFather}s to choose from.
     * @param ff The {@code FoundingFather} to select.
     */
    public ChooseFoundingFatherMessage(List<FoundingFather> fathers,
                                       FoundingFather ff) {
        super(getTagName());

        this.fathers = new ArrayList<>();
        this.fathers.addAll(fathers);
        setFather(ff);
    }

    /**
     * Create a new {@code ChooseFoundingFatherMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public ChooseFoundingFatherMessage(Game game, Element element) {
        super(getTagName());

        final Specification spec = game.getSpecification();
        List<String> found = transform(fatherKeys, alwaysTrue(),
                                       k -> element.getAttribute(k));
        this.fathers = transform(found, id -> id != null && !id.isEmpty(),
                                 id -> spec.getFoundingFather(id));
        this.foundingFatherId = getStringAttribute(element, FOUNDING_FATHER_TAG);
    }


    // Public interface

    /**
     * Get the chosen father.
     *
     * @param game The {@code Game} to lookup the father in.
     * @return The chosen {@code FoundingFather}, or null if none set.
     */
    public final FoundingFather getFather(Game game) {
        return (this.foundingFatherId == null) ? null
            : game.getSpecification().getFoundingFather(this.foundingFatherId);
    }

    /**
     * Sets the chosen father.
     *
     * @param ff The {@code FoundingFather} to choose.
     * @return This message.
     */
    public final ChooseFoundingFatherMessage setFather(FoundingFather ff) {
        this.foundingFatherId = (ff == null) ? null : ff.getId();
        return this;
    }

    /**
     * Get the list of offered fathers.
     *
     * @return The offered {@code FoundingFather}s.
     */
    public final List<FoundingFather> getFathers() {
        return this.fathers;
    }


    /**
     * Handle a "chooseFoundingFather"-message.
     * The server does not need to handle this message type.
     *
     * @param server The {@code FreeColServer} handling the request.
     * @param player The {@code Player} abandoning the colony.
     * @param connection The {@code Connection} the message is from.
     *
     * @return Null.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final Game game = server.getGame();
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final List<FoundingFather> offered = serverPlayer.getOfferedFathers();
        final FoundingFather ff = getFather(game);

        if (!serverPlayer.canRecruitFoundingFather()) {
            return serverPlayer.clientError("Player can not recruit fathers: "
                + serverPlayer.getId())
                .build(serverPlayer);
        } else if (ff == null) {
            return serverPlayer.clientError("No founding father selected")
                .build(serverPlayer);
        } else if (!offered.contains(ff)) {
            return serverPlayer.clientError("Founding father not offered: "
                + ff.getId())
                .build(serverPlayer);
        }

        serverPlayer.updateCurrentFather(ff);
        return null;
    }

    /**
     * Convert this ChooseFoundingFatherMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
                              FOUNDING_FATHER_TAG, this.foundingFatherId)
            .setAttributes(transform(getFathers(), alwaysTrue(),
                                     Function.identity(),
                                     Collectors.toMap(f -> f.getType().toString(),
                                                      f -> f.getId())))
            .toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "chooseFoundingFather".
     */
    public static String getTagName() {
        return TAG;
    }
}
