/**
 *  Copyright (C) 2002-2024  The FreeCol Team
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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class MessageRoundTripTest extends FreeColTestCase {

    private final TileType plainsType = spec().getTileType("model.tile.plains");
    private final UnitType colonistType = spec().getUnitType("model.unit.freeColonist");

    private Message roundTrip(Game game, Message message) throws Exception {
        StringWriter out = new StringWriter();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(out)) {
            message.toXML(xw);
            xw.flush();
        }
        String xml = out.toString();
        try (FreeColXMLReader xr = new FreeColXMLReader(new StringReader(xml))) {
            xr.nextTag();
            return Message.read(game, xr);
        }
    }

    private void assertRoundTrip(Game game, Message message) throws Exception {
        Map<String, String> expected = new HashMap<>(message.getStringAttributeMap());
        Message parsed = roundTrip(game, message);
        assertEquals(message.getType(), parsed.getType());
        assertEquals(expected, parsed.getStringAttributeMap());
    }

    public void testMoveMessageRoundTrip() throws Exception {
        Game game = getStandardGame();
        net.sf.freecol.common.model.Map map = getTestMap(plainsType, true);
        game.changeMap(map);

        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Tile tile = map.getTile(6, 8);
        Unit unit = new ServerUnit(game, tile, dutch, colonistType);

        Message message = new MoveMessage(unit, Direction.NE);
        assertRoundTrip(game, message);
    }

    public void testBuildColonyMessageRoundTrip() throws Exception {
        Game game = getStandardGame();
        net.sf.freecol.common.model.Map map = getTestMap(plainsType, true);
        game.changeMap(map);

        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Tile tile = map.getTile(6, 8);
        Unit unit = new ServerUnit(game, tile, dutch, colonistType);

        Message message = new BuildColonyMessage("New Amsterdam", unit);
        assertRoundTrip(game, message);
    }

    public void testEndTurnMessageRoundTrip() throws Exception {
        Game game = getStandardGame();
        Message message = new EndTurnMessage();
        assertRoundTrip(game, message);
    }
}
