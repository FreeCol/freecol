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

import junit.framework.Test;
import junit.framework.TestSuite;


public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for net.sf.freecol.common.model");
        suite.addTestSuite(SpecificationTest.class);
        //$JUnit-BEGIN$
        suite.addTestSuite(BaseCostDeciderTest.class);
        suite.addTestSuite(BuildingTest.class);
        suite.addTestSuite(ColonyTest.class);
        suite.addTestSuite(ColonyProductionTest.class);
        suite.addTestSuite(CombatTest.class);
        suite.addTestSuite(DisasterTest.class);
        suite.addTestSuite(EuropeTest.class);
        suite.addTestSuite(FoundingFatherTest.class);
        suite.addTestSuite(GameTest.class);
        suite.addTestSuite(GoodsTest.class);
        suite.addTestSuite(GoodsContainerTest.class);
        suite.addTestSuite(IndianSettlementTest.class);
        suite.addTestSuite(IndividualFatherTest.class);
        suite.addTestSuite(LimitTest.class);
        suite.addTestSuite(MapTest.class);
        suite.addTestSuite(MarketTest.class);
        suite.addTestSuite(ModelMessageTest.class);
        suite.addTestSuite(ModifierTest.class);
        suite.addTestSuite(MonarchTest.class);
        suite.addTestSuite(MovementTest.class);
        suite.addTestSuite(NationTypeTest.class);
        suite.addTestSuite(PlayerTest.class);
        suite.addTestSuite(ProductionTypeTest.class);
        suite.addTestSuite(RandomRangeTest.class);
        suite.addTestSuite(RoleTest.class);
        suite.addTestSuite(SchoolTest.class);
        suite.addTestSuite(ScopeTest.class);
        suite.addTestSuite(SerializationTest.class);
        suite.addTestSuite(SettlementTest.class);
        suite.addTestSuite(SoLTest.class);
        suite.addTestSuite(TileImprovementTest.class);
        suite.addTestSuite(TileItemContainerTest.class);
        suite.addTestSuite(TileTest.class);
        suite.addTestSuite(TradeRouteTest.class);
        suite.addTestSuite(UnitTest.class);
        suite.addTestSuite(UnitTypeChangeTest.class);
        //$JUnit-END$
        return suite;
    }
}
