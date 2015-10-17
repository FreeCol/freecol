/**
 *  Copyright (C) 2002-2015  The FreeCol Team
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

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class ScopeTest extends FreeColTestCase {


    UnitType carpenter = spec().getUnitType("model.unit.masterCarpenter");
    UnitType frigate = spec().getUnitType("model.unit.frigate");

    public void testEmptyScope(){
    	
        Scope testScope = new Scope();

        // empty scope applies to all
        assertTrue(testScope.appliesTo(carpenter));
        assertTrue(testScope.appliesTo(frigate));

        // unless negated
        testScope.setMatchNegated(true);
        assertFalse(testScope.appliesTo(carpenter));
        assertFalse(testScope.appliesTo(frigate));

    }

    public void testTypeScope() {

        Scope testScope = new Scope();

        testScope.setType("model.unit.frigate");
        assertTrue(testScope.appliesTo(frigate));
        assertFalse(testScope.appliesTo(carpenter));
        testScope.setMatchNegated(true);
        assertFalse(testScope.appliesTo(frigate));
        assertTrue(testScope.appliesTo(carpenter));

        testScope.setMatchNegated(false);
        testScope.setType("model.unit.masterCarpenter");
        assertFalse(testScope.appliesTo(frigate));
        assertTrue(testScope.appliesTo(carpenter));
        testScope.setMatchNegated(true);
        assertTrue(testScope.appliesTo(frigate));
        assertFalse(testScope.appliesTo(carpenter));

    }

    public void testAbilityScope() {

        Scope testScope = new Scope();

        testScope.setAbilityId(Ability.NAVAL_UNIT);
        assertEquals(frigate.hasAbility(Ability.NAVAL_UNIT),
                     testScope.appliesTo(frigate));
        assertEquals(carpenter.hasAbility(Ability.NAVAL_UNIT),
                     testScope.appliesTo(carpenter));

    }
    
    public void testMethodScope() {

        Scope testScope = new Scope();

        testScope.setMethodName("getLineOfSight");
        testScope.setMethodValue("1");
        assertTrue(frigate.getLineOfSight() != 1);
        assertFalse(testScope.appliesTo(frigate));
        assertTrue(carpenter.getLineOfSight() == 1);
        assertTrue(testScope.appliesTo(carpenter));

    }

    public void testCombinedScope() {

        Scope testScope = new Scope();

        testScope.setType("model.unit.frigate");
        testScope.setAbilityId(Ability.NAVAL_UNIT);
        testScope.setMethodName("getLineOfSight");
        testScope.setMethodValue("2");
        assertTrue(testScope.appliesTo(frigate));

        testScope.setMethodValue("1");
        assertFalse(testScope.appliesTo(frigate));

        testScope.setMethodValue("2");
        testScope.setAbilityId(Ability.FOUND_COLONY);
        assertFalse(testScope.appliesTo(frigate));
    }

    public void testMatchesNull() {

        Scope testScope = new Scope();
        testScope.setType("model.unit.frigate");

        assertTrue(testScope.appliesTo(frigate));
        assertTrue(testScope.appliesTo(null));
        testScope.setMatchesNull(false);
        assertTrue(testScope.appliesTo(frigate));
        assertFalse(testScope.appliesTo(null));

    }

    public void testEquality() {

        Scope testScope1 = new Scope();
        testScope1.setType("model.unit.frigate");
        testScope1.setAbilityId(Ability.NAVAL_UNIT);
        testScope1.setMethodName("getLineOfSight");
        testScope1.setMethodValue("2");
        testScope1.setMatchesNull(true);
        testScope1.setMatchNegated(false);
        assertTrue(testScope1.equals(testScope1));

        Scope testScope2 = new Scope();
        testScope2.setType("model.unit.frigate");
        testScope2.setAbilityId(Ability.NAVAL_UNIT);
        testScope2.setMethodName("getLineOfSight");
        testScope2.setMethodValue("2");
        testScope2.setMatchesNull(true);
        testScope2.setMatchNegated(false);
        assertTrue(testScope2.equals(testScope2));

        assertTrue(testScope1.equals(testScope2));
        assertTrue(testScope2.equals(testScope1));

        testScope1.setType("model.unit.carpenter");

        assertFalse(testScope1.equals(testScope2));
        assertFalse(testScope2.equals(testScope1));

        testScope1.setType("model.unit.frigate");
        testScope1.setAbilityId(Ability.FOUND_COLONY);

        assertFalse(testScope1.equals(testScope2));
        assertFalse(testScope2.equals(testScope1));

        testScope1.setAbilityId(Ability.NAVAL_UNIT);
        testScope1.setAbilityValue(false);

        assertFalse(testScope1.equals(testScope2));
        assertFalse(testScope2.equals(testScope1));

        testScope1.setAbilityValue(true);
        testScope1.setMethodName("getOffence");

        assertFalse(testScope1.equals(testScope2));
        assertFalse(testScope2.equals(testScope1));

        testScope1.setMethodName("getLineOfSight");
        testScope1.setMethodValue("9");

        assertFalse(testScope1.equals(testScope2));
        assertFalse(testScope2.equals(testScope1));

        testScope1.setMethodValue("2");

        assertTrue(testScope1.equals(testScope2));
        assertTrue(testScope2.equals(testScope1));

        testScope1.setMatchesNull(false);

        assertFalse(testScope1.equals(testScope2));
        assertFalse(testScope2.equals(testScope1));

        testScope1.setMatchesNull(true);

        assertTrue(testScope1.equals(testScope2));
        assertTrue(testScope2.equals(testScope1));

        testScope1.setMatchNegated(true);

        assertFalse(testScope1.equals(testScope2));
        assertFalse(testScope2.equals(testScope1));

        testScope1.setMatchNegated(false);

        assertTrue(testScope1.equals(testScope2));
        assertTrue(testScope2.equals(testScope1));

    }

    public void testGameObjects() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Map map = getTestMap();
        game.setMap(map);

        Tile unitTile = map.getTile(6, 8);
        UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        
        Unit unit = new ServerUnit(game, unitTile, dutch, colonistType);

        Scope scope = new Scope();
        scope.setAbilityId(Ability.FOUND_COLONY);
        assertTrue(scope.appliesTo(unit));
        scope.setType("model.unit.freeColonist");
        assertTrue(scope.appliesTo(unit));
        scope.setType("model.unit.hardyPioneer");
        assertFalse(scope.appliesTo(unit));

    }
        

}
