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

package net.sf.freecol.common.networking;

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent to choose a founding father.
 */
public class ChooseFoundingFatherMessage extends DOMMessage {

    /** The fathers to offer. */
    private final List<FoundingFather> fathers;

    /** The selected father. */
    private String foundingFatherId;


    /**
     * Create a new <code>ChooseFoundingFatherMessage</code> with the specified
     * fathers.
     *
     * @param fathers The <code>FoundingFather</code>s to choose from.
     * @param ff The <code>FoundingFather</code> to select.
     */
    public ChooseFoundingFatherMessage(List<FoundingFather> fathers,
                                       FoundingFather ff) {
        super(getXMLElementTagName());

        this.fathers = new ArrayList<>();
        this.fathers.addAll(fathers);
        setFather(ff);
    }

    /**
     * Create a new <code>ChooseFoundingFatherMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public ChooseFoundingFatherMessage(Game game, Element element) {
        super(getXMLElementTagName());

        final Specification spec = game.getSpecification();
        this.fathers = new ArrayList<>();
        for (FoundingFatherType type : FoundingFatherType.values()) {
            String id = element.getAttribute(type.toString());
            if (id == null || id.isEmpty()) continue;
            FoundingFather ff = spec.getFoundingFather(id);
            this.fathers.add(ff);
        }
        foundingFatherId = element.getAttribute("foundingFather");
    }


    // Public interface

    /**
     * Get the chosen father.
     *
     * @param game The <code>Game</code> to lookup the father in.
     * @return The chosen <code>FoundingFather</code>, or null if none set.
     */
    public final FoundingFather getFather(Game game) {
        return (foundingFatherId == null) ? null
            : game.getSpecification().getFoundingFather(this.foundingFatherId);
    }

    /**
     * Sets the chosen father.
     *
     * @param ff The <code>FoundingFather</code> to choose.
     */
    public final ChooseFoundingFatherMessage setFather(FoundingFather ff) {
        this.foundingFatherId = (ff == null) ? null : ff.getId();
        return this;
    }

    /**
     * Get the list of offered fathers.
     *
     * @return The offered <code>FoundingFather</code>s.
     */
    public final List<FoundingFather> getFathers() {
        return fathers;
    }


    /**
     * Handle a "chooseFoundingFather"-message.
     * The server does not need to handle this message type.
     *
     * @param server The <code>FreeColServer</code> handling the request.
     * @param player The <code>Player</code> abandoning the colony.
     * @param connection The <code>Connection</code> the message is from.
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
            return DOMMessage.clientError("Player can not recruit fathers: "
                + serverPlayer.getId());
        } else if (ff == null) {
            return DOMMessage.clientError("No founding father selected");
        } else if (!offered.contains(ff)) {
            return DOMMessage.clientError("Founding father not offered: "
                + ff.getId());
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
        Element result = createMessage(getXMLElementTagName());
        for (FoundingFather f : getFathers()) {
            result.setAttribute(f.getType().toString(), f.getId());
        }
        if (this.foundingFatherId != null) {
            result.setAttribute("foundingFather", foundingFatherId);
        }
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "chooseFoundingFather".
     */
    public static String getXMLElementTagName() {
        return "chooseFoundingFather";
    }
}
