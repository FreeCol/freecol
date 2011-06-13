package net.sf.freecol.common.model;

import net.sf.freecol.common.model.Specification;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;

public class EuropeTest extends FreeColTestCase {

    public void testMissionary() {

        Game game = getGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Europe amsterdam = dutch.getEurope();

        UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        Unit colonist = new ServerUnit(game, amsterdam, dutch, colonistType,
                                       Unit.UnitState.ACTIVE);

        assertTrue(amsterdam.hasAbility("model.ability.dressMissionary"));
        assertTrue(colonist.hasAbility("model.ability.dressMissionary"));

    }


}
