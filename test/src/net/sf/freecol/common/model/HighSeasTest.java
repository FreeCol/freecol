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

package net.sf.freecol.common.model;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.UnitLocation.NoAddReason;


public class HighSeasTest extends FreeColTestCase {

    public void testAddAndRemoveDestination() {
        Game game = getGame();
        Map map = getTestMap();
        Tile tile = map.getTile(5, 5);

        HighSeas hs = new HighSeas(game);

        assertTrue(hs.getDestinations().isEmpty());

        hs.addDestination(tile);
        assertEquals(1, hs.getDestinations().size());
        assertTrue(hs.getDestinations().contains(tile));

        hs.addDestination(tile);
        assertEquals(1, hs.getDestinations().size());

        hs.removeDestination(tile);
        assertTrue(hs.getDestinations().isEmpty());
    }

    public void testNavalUnitAllowed() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        HighSeas hs = dutch.getHighSeas();

        UnitType caravel = spec().getUnitType("model.unit.caravel");
        Unit naval = new ServerUnit(game, null, dutch, caravel);

        assertEquals(NoAddReason.NONE, hs.getNoAddReason(naval));
    }

    public void testLandUnitRejected() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        HighSeas hs = dutch.getHighSeas();

        UnitType colonist = spec().getUnitType("model.unit.freeColonist");
        Unit land = new ServerUnit(game, null, dutch, colonist);

        assertEquals(NoAddReason.WRONG_TYPE, hs.getNoAddReason(land));
    }

    public void testLinkTargetIsEurope() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        HighSeas hs = dutch.getHighSeas();

        FreeColGameObject target = hs.getLinkTarget(dutch);
        assertEquals(dutch.getEurope(), target);
    }

    public void testSerializationRoundTrip() throws Exception {
        Game game = getGame();
        Map map = getTestMap();
        Tile t1 = map.getTile(2, 2);
        Tile t2 = map.getTile(4, 4);

        HighSeas hs = new HighSeas(game);
        hs.addDestination(t1);
        hs.addDestination(t2);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FreeColXMLWriter xw =
            new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false);

        xw.writeStartElement(hs.getXMLTagName());
        hs.writeAttributes(xw);
        hs.writeChildren(xw);
        xw.writeEndElement();
        xw.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        FreeColXMLReader xr = new FreeColXMLReader(in);

        xr.nextTag();

        HighSeas hs2 = new HighSeas(game);
        hs2.readFromXML(xr);
        xr.close();

        assertEquals(2, hs2.getDestinations().size());
        assertTrue(hs2.getDestinations().contains(t1));
        assertTrue(hs2.getDestinations().contains(t2));
    }

    public void testAddNullDestinationDoesNotCrash() {
        Game game = getGame();
        HighSeas hs = new HighSeas(game);

        hs.addDestination(null); // should only log a warning
        assertTrue(hs.getDestinations().isEmpty());
    }
}
