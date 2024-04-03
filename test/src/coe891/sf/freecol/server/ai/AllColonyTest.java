package coe891.sf.freecol.server.ai;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.util.test.FreeColTestCase;

public class AllColonyTest extends FreeColTestCase {

    private static final TileType savannahType
            = spec().getTileType("model.tile.savannah");
    private static final UnitType artilleryType
            = spec().getUnitType("model.unit.artillery");

    public void testBestDefender() {
        Game game = ServerTestHelper.startServerGame(getTestMap(savannahType));
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        Colony colony = getStandardColony();
        assertEquals(artilleryType, colony.getBestDefenderType());
    }
}
