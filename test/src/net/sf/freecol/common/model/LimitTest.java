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

import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Operand.OperandType;
import net.sf.freecol.common.model.Operand.ScopeLevel;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class LimitTest extends FreeColTestCase {

    public void testOperand() {

        UnitType carpenter = spec().getUnitType("model.unit.masterCarpenter");
        UnitType frigate = spec().getUnitType("model.unit.frigate");

        Operand operand = new Operand();
        assertEquals(OperandType.NONE, operand.getOperandType());
        assertEquals(ScopeLevel.NONE, operand.getScopeLevel());

        operand.setType("model.unit.frigate");
        assertTrue(operand.appliesTo(frigate));
        assertFalse(operand.appliesTo(carpenter));

    }

    public void testWagonTrainLimit() {

        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Map map = getTestMap();
        game.setMap(map);

        Colony colony = getStandardColony(3);
        Building armory = new ServerBuilding(getGame(), colony, spec().getBuildingType("model.building.armory"));
        colony.addBuilding(armory);

        UnitType wagonTrain = spec().getUnitType("model.unit.wagonTrain");
        UnitType artillery = spec().getUnitType("model.unit.artillery");

        Limit wagonTrainLimit = wagonTrain.getLimits().get(0);

        assertTrue(colony.canBuild(artillery));
        assertFalse(wagonTrainLimit.getLeftHandSide().appliesTo(artillery));

        assertTrue(wagonTrainLimit.evaluate(colony));
        assertTrue(colony.canBuild(wagonTrain));

        Unit wagon = new ServerUnit(game, colony.getTile(), dutch, wagonTrain);
        assertNotNull(wagon);
        assertEquals(Colony.NoBuildReason.LIMIT_EXCEEDED,
                     colony.getNoBuildReason(wagonTrain, null));
        assertFalse(wagonTrainLimit.evaluate(colony));
        assertFalse(colony.canBuild(wagonTrain));
        assertTrue(colony.canBuild(artillery));
    }

    public void testIndependenceLimits() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Map map = getTestMap();
        game.setMap(map);

        Colony colony = getStandardColony(3);

        Event event = spec().getEvent("model.event.declareIndependence");
        assertNotNull(event);
        assertNotNull(event.getLimits());
        assertNotNull(spec().getOption(GameOptions.LAST_COLONIAL_YEAR));

        Limit rebelLimit = event.getLimit("model.limit.independence.rebels");
        Limit colonyLimit = event.getLimit("model.limit.independence.coastalColonies");
        Limit yearLimit = event.getLimit("model.limit.independence.year");

        assertNotNull(rebelLimit);
        assertEquals(Limit.Operator.GE, rebelLimit.getOperator());
        assertEquals(Operand.OperandType.NONE, rebelLimit.getLeftHandSide().getOperandType());
        assertEquals(Operand.ScopeLevel.PLAYER, rebelLimit.getLeftHandSide().getScopeLevel());
        assertEquals(Integer.valueOf(0), rebelLimit.getLeftHandSide().getValue(dutch));
        assertEquals(Integer.valueOf(50), rebelLimit.getRightHandSide().getValue(dutch));
        assertFalse(rebelLimit.evaluate(dutch));

        assertNotNull(colonyLimit);
        assertEquals(Limit.Operator.GE, colonyLimit.getOperator());
        assertEquals(Operand.OperandType.SETTLEMENTS, colonyLimit.getLeftHandSide().getOperandType());
        assertEquals(Operand.ScopeLevel.PLAYER, colonyLimit.getLeftHandSide().getScopeLevel());
        assertEquals("isConnectedPort", colonyLimit.getLeftHandSide().getMethodName());
        assertFalse(colony.isConnectedPort());
        assertEquals(Integer.valueOf(0), colonyLimit.getLeftHandSide().getValue(dutch));
        assertEquals(Integer.valueOf(1), colonyLimit.getRightHandSide().getValue(dutch));
        assertFalse(colonyLimit.evaluate(dutch));

        assertNotNull(yearLimit);
        assertEquals(Limit.Operator.LE, yearLimit.getOperator());
        assertEquals(Operand.OperandType.YEAR, yearLimit.getLeftHandSide().getOperandType());
        assertEquals(Operand.OperandType.OPTION, yearLimit.getRightHandSide().getOperandType());
        assertEquals(GameOptions.LAST_COLONIAL_YEAR, yearLimit.getRightHandSide().getType());
        assertEquals(Integer.valueOf(1492), yearLimit.getLeftHandSide().getValue(dutch));
        assertEquals(Integer.valueOf(1800), yearLimit.getRightHandSide().getValue(dutch));
        assertTrue(yearLimit.evaluate(dutch));

        colony.modifyLiberty(10000);
        colony.updateSoL();
        assertTrue(rebelLimit.evaluate(dutch));

        Tile tile = colony.getTile().getNeighbourOrNull(Direction.N);
        tile.setType(spec().getTileType("model.tile.ocean"));
        tile.setHighSeasCount(5);
        tile.setExplored(dutch, true);
        assertTrue(tile.isExploredBy(dutch));
        assertTrue(tile.isHighSeasConnected());
        assertTrue(!tile.isLand());
        assertTrue(colony.isConnectedPort());
        assertTrue(colonyLimit.getLeftHandSide().appliesTo(colony));
        assertTrue(colonyLimit.evaluate(dutch));

        IntegerOption option = spec()
            .getIntegerOption(GameOptions.LAST_COLONIAL_YEAR);
        option.setMinimumValue(1300);
        option.setValue(1300);
        assertFalse(yearLimit.evaluate(dutch));
    }

    public void testSuccessionLimits() {

        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Map map = getTestMap();
        game.setMap(map);

        Colony colony = getStandardColony(3);

        Event event = spec().getEvent("model.event.spanishSuccession");
        assertNotNull(event);
        assertNotNull(event.getLimits());

        Limit weakestPlayerLimit = event.getLimit("model.limit.spanishSuccession.weakestPlayer");
        Limit strongestPlayerLimit = event.getLimit("model.limit.spanishSuccession.strongestPlayer");
        Limit yearLimit = event.getLimit("model.limit.spanishSuccession.year");

        assertNotNull(strongestPlayerLimit);
        assertEquals(Limit.Operator.GT, strongestPlayerLimit.getOperator());
        assertEquals(Operand.OperandType.NONE, strongestPlayerLimit.getLeftHandSide().getOperandType());
        assertEquals(Operand.ScopeLevel.PLAYER, strongestPlayerLimit.getLeftHandSide().getScopeLevel());
        assertEquals(Integer.valueOf(0), strongestPlayerLimit.getLeftHandSide().getValue(dutch));
        assertEquals(Integer.valueOf(50), strongestPlayerLimit.getRightHandSide().getValue(dutch));
        assertFalse(strongestPlayerLimit.evaluate(dutch));

        assertNotNull(weakestPlayerLimit);
        assertEquals(Limit.Operator.LT, weakestPlayerLimit.getOperator());
        assertEquals(Operand.OperandType.NONE, weakestPlayerLimit.getLeftHandSide().getOperandType());
        assertEquals(Operand.ScopeLevel.PLAYER, weakestPlayerLimit.getLeftHandSide().getScopeLevel());
        assertEquals(Integer.valueOf(0), weakestPlayerLimit.getLeftHandSide().getValue(dutch));
        assertEquals(Integer.valueOf(50), weakestPlayerLimit.getRightHandSide().getValue(dutch));
        assertTrue(weakestPlayerLimit.evaluate(dutch));

        assertNotNull(yearLimit);
        assertEquals(Limit.Operator.GE, yearLimit.getOperator());
        assertEquals(Operand.OperandType.YEAR, yearLimit.getLeftHandSide().getOperandType());
        assertEquals(Operand.ScopeLevel.GAME, yearLimit.getLeftHandSide().getScopeLevel());
        assertEquals(Integer.valueOf(1492), yearLimit.getLeftHandSide().getValue(game));
        assertEquals(Integer.valueOf(1600), yearLimit.getRightHandSide().getValue());
        assertFalse(yearLimit.evaluate(game));

        colony.modifyLiberty(10000);
        colony.updateSoL();
        assertTrue(strongestPlayerLimit.evaluate(dutch));
        assertFalse(weakestPlayerLimit.evaluate(dutch));
    }
}
