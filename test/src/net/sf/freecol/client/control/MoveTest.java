/**
 *  Copyright (C) 2002-2008  The FreeCol Team
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

package net.sf.freecol.client.control;

import java.io.File;
import java.io.IOException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.ClientTestHelper;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.util.test.FreeColTestCase;

public class MoveTest extends FreeColTestCase {

    TileType plains = spec().getTileType("model.tile.plains");

    public void testSimpleMove() {
        FreeColClient client = ClientTestHelper.startClient();

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile plain = map.getTile(5, 8);
        map.getTile(5, 8).setExploredBy(dutch, true);
        map.getTile(5, 7).setExploredBy(dutch, true);

        Unit hardyPioneer = new Unit(game, plain, dutch, spec().getUnitType("model.unit.hardyPioneer"), 
                                     UnitState.ACTIVE);

        client.getInGameController().move(hardyPioneer, Direction.NE);
    }

}