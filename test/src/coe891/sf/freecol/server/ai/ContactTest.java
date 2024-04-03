package coe891.sf.freecol.server.ai;

import net.sf.freecol.common.model.*;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;

public class ContactTest extends  net.sf.freecol.server.ai.ContactTest {

    private static final TileType plains
            = spec().getTileType("model.tile.plains");
    private static final TileType ocean
            = spec().getTileType("model.tile.ocean");

    private static final UnitType galleonType
            = spec().getUnitType("model.unit.galleon");
    private static final UnitType braveType
            = spec().getUnitType("model.unit.brave");
    private static final UnitType colonistType
            = spec().getUnitType("model.unit.freeColonist");

    public void testEuropeanMeetsNative() throws Exception {
        Game game = ServerTestHelper.startServerGame(getTestMap(plains));
        Map map = game.getMap();
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");
        ServerPlayer iroquois = getServerPlayer(game, "model.nation.iroquois");
        Tile tile1 = map.getTile(6, 8);
        tile1.setExplored(dutch, true);
        Tile tile2 = map.getTile(5, 8);
        tile2.setExplored(dutch, true);
        Tile tile3 = map.getTile(4, 8);
        tile3.setExplored(dutch, true);

        assertFalse(iroquois.hasContacted(dutch));
        assertFalse(dutch.hasContacted(iroquois));

        ServerUnit colonist = new ServerUnit(game, tile1, dutch, colonistType);
        colonist.setState(Unit.UnitState.FORTIFYING);
        colonist.setState(Unit.UnitState.FORTIFIED);
        ServerUnit soldier = new ServerUnit(game, tile3, iroquois, braveType);
        assertNotNull(soldier);

        igc.move(dutch, colonist, tile2);

        assertTrue(iroquois.hasContacted(dutch));
        assertTrue(dutch.hasContacted(iroquois));
        assertEquals(Stance.PEACE, iroquois.getStance(dutch));
        assertEquals(Stance.PEACE, dutch.getStance(iroquois));

        assertNotNull(iroquois.getTension(dutch));
    }
}
