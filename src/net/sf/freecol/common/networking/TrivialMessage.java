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

import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The basic trivial message, with just a name and possibly some attributes.
 */
public class TrivialMessage extends DOMMessage {

    private static final String TRIVIAL_TAG = "trivial";

    /*
     * True trivial messages have no distinguishing parts, so we might
     * as well just use some explicit constants.
     */
    public static final String CLOSE_MENUS_TAG = "closeMenus";
    public static final TrivialMessage CLOSE_MENUS_MESSAGE
        = new TrivialMessage(CLOSE_MENUS_TAG);
    public static final String CONTINUE_TAG = "continuePlaying";
    public static final TrivialMessage CONTINUE_MESSAGE
        = new TrivialMessage(CONTINUE_TAG);
    public static final String END_TURN_TAG = "endTurn";
    public static final TrivialMessage END_TURN_MESSAGE
        = new TrivialMessage(END_TURN_TAG);
    public static final String ENTER_REVENGE_MODE_TAG = "enterRevengeMode";
    public static final TrivialMessage ENTER_REVENGE_MODE_MESSAGE
        = new TrivialMessage(ENTER_REVENGE_MODE_TAG);
    public static final String RECONNECT_TAG = "reconnect";
    public static final TrivialMessage RECONNECT_MESSAGE
        = new TrivialMessage(RECONNECT_TAG);
    public static final String REQUEST_LAUNCH_TAG = "requestLaunch";
    public static final TrivialMessage REQUEST_LAUNCH_MESSAGE
        = new TrivialMessage(REQUEST_LAUNCH_TAG);
    public static final String RETIRE_TAG = "retire";
    public static final TrivialMessage RETIRE_MESSAGE
        = new TrivialMessage(RETIRE_TAG);
    public static final String START_GAME_TAG = "startGame";
    public static final TrivialMessage START_GAME_MESSAGE
        = new TrivialMessage(START_GAME_TAG);

    /** The actual message type. */
    private final String type;


    /**
     * Create a new {@code TrivialMessage} of a given type.
     *
     * @param type The message type.
     */
    protected TrivialMessage(String type) {
        super(type);

        this.type = type;
    }

    /**
     * Create a new {@code TrivialMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public TrivialMessage(@SuppressWarnings("unused") Game game,
                          Element element) {
        this(element.getTagName());
    }


    private InGameController igc(FreeColServer freeColServer) {
        return freeColServer.getInGameController();
    }

    private PreGameController pgc(FreeColServer freeColServer) {
        return freeColServer.getPreGameController();
    }


    // Public interface

    /**
     * Get the type of the message.
     *
     * @return The type name.
     */
    @Override
    public String getType() {
        return this.type;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        switch (this.type) {
        case CLOSE_MENUS_TAG:
            return serverPlayer.clientError("Close menus in server?!?");
        case CONTINUE_TAG:
            return igc(freeColServer).continuePlaying(serverPlayer);
        case END_TURN_TAG:
            return igc(freeColServer).endTurn(serverPlayer);
        case ENTER_REVENGE_MODE_TAG:
            return igc(freeColServer).enterRevengeMode(serverPlayer);
        case RECONNECT_TAG: // Only sent to client
            break;
        case REQUEST_LAUNCH_TAG:
            return pgc(freeColServer).requestLaunch(serverPlayer);
        case RETIRE_TAG:
            return igc(freeColServer).retire(serverPlayer);
        case START_GAME_TAG:
            break;
        default:
            return super.serverHandler(freeColServer, serverPlayer);
        }
        return null;
    }        
}
