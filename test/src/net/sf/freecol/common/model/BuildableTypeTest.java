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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony.NoBuildReason;
import net.sf.freecol.util.test.FreeColTestCase;

public class BuildableTypeTest extends FreeColTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
    }

    private static class TestBuildable extends BuildableType {
        public TestBuildable(String id, Specification spec) {
            super(id, spec);
        }

        @Override
        public NoBuildReason canBeBuiltInColony(Colony colony, List<BuildableType> assumeBuilt) {
            return null;
        }

        @Override
        public String getXMLTagName() {
            return "testBuildable";
        }
    }

    public void testRequiredAbilities() {
        TestBuildable buildable = new TestBuildable("test", spec());
        
        buildable.addRequiredAbility("model.ability.buildShips", true);
        assertTrue(buildable.requiresAbility("model.ability.buildShips"));
        
        buildable.removeRequiredAbility("model.ability.buildShips");
        assertFalse(buildable.requiresAbility("model.ability.buildShips"));
    }

    public void testPopulationRequirement() {
        TestBuildable buildable = new TestBuildable("test", spec());
        assertEquals(1, buildable.getRequiredPopulation());
        
        buildable.setRequiredPopulation(5);
        assertEquals(5, buildable.getRequiredPopulation());
    }

    public void testAvailability() {
        TestBuildable buildable = new TestBuildable("test", spec());
        buildable.addRequiredAbility("model.ability.navalUnit", true);

        Game game = getGame(); 
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        dutch.addAbility(new Ability("model.ability.navalUnit", true));

        assertTrue("Should be available to dutch after adding ability", 
                   buildable.isAvailableTo(dutch));
    }

    public void testRequiredAbilitiesWithFalseValue() {
        TestBuildable buildable = new TestBuildable("test", spec());
        buildable.addRequiredAbility("model.ability.native", false);
        
        Player dutch = getGame().getPlayerByNationId("model.nation.dutch");
        assertTrue("Should be available because player lacks the blocked ability", 
                   buildable.isAvailableTo(dutch));

        dutch.addAbility(new Ability("model.ability.native", true));
        assertFalse("Should be blocked because player now has the forbidden ability", 
                    buildable.isAvailableTo(dutch));
    }

    public void testGetRequiredAmountOfMissingGoods() {
        TestBuildable buildable = new TestBuildable("test", spec());
        GoodsType hammers = spec().getGoodsType("model.goods.hammers");
        GoodsType lumber = spec().getGoodsType("model.goods.lumber");
        
        buildable.setRequiredGoods(Arrays.asList(new AbstractGoods(hammers, 50)));

        assertEquals(50, buildable.getRequiredAmountOf(hammers));
        assertEquals("Should return 0 for goods not in the list", 
                     0, buildable.getRequiredAmountOf(lumber));
    }

    public void testIsAvailableToMultipleObjects() {
        TestBuildable buildable = new TestBuildable("test", spec());
        buildable.addRequiredAbility("model.ability.foundingFather", true);

        Player dutch = getGame().getPlayerByNationId("model.nation.dutch");
        Colony colony = createStandardColony(3); 
        
        assertFalse(buildable.isAvailableTo(dutch, colony));

        colony.addAbility(new Ability("model.ability.foundingFather", true));
        
        assertTrue("Should be available if ANY of the objects have the ability", 
                   buildable.isAvailableTo(dutch, colony));
    }

    public void testNeedsGoodsToBuild() {
        TestBuildable buildable = new TestBuildable("test", spec());
        assertFalse(buildable.needsGoodsToBuild());

        GoodsType hammers = spec().getGoodsType("model.goods.hammers");
        buildable.setRequiredGoods(Arrays.asList(new AbstractGoods(hammers, 10)));

        assertTrue(buildable.needsGoodsToBuild());
    }

    public void testRequiredGoodsListIsDefensiveCopy() {
        TestBuildable buildable = new TestBuildable("test", spec());
        GoodsType hammers = spec().getGoodsType("model.goods.hammers");

        buildable.setRequiredGoods(Arrays.asList(new AbstractGoods(hammers, 10)));

        List<AbstractGoods> list = buildable.getRequiredGoodsList();
        list.get(0).setAmount(999); 

        assertEquals(10, buildable.getRequiredGoodsList().get(0).getAmount());
    }

    public void testCopyInCopiesFields() {
        Specification spec = spec();
        TestBuildable a = new TestBuildable("test", spec);
        TestBuildable b = new TestBuildable("test", spec);

        GoodsType hammers = spec.getGoodsType("model.goods.hammers");
        a.setRequiredGoods(Arrays.asList(new AbstractGoods(hammers, 20)));
        a.addRequiredAbility("model.ability.buildShips", true);
        a.setRequiredPopulation(3);

        b.copyIn(a);

        assertEquals(3, b.getRequiredPopulation());
        assertTrue(b.requiresAbility("model.ability.buildShips"));
        assertEquals(20, b.getRequiredAmountOf(hammers));
    }

    public void testIsUniqueInQueueDefault() {
        TestBuildable buildable = new TestBuildable("test", spec());
        assertFalse(buildable.isUniqueInQueue());
    }

    public void testSerializationRoundTrip() throws Exception {
        Specification spec = spec();
        TestBuildable buildable = new TestBuildable("test", spec);

        GoodsType hammers = spec.getGoodsType("model.goods.hammers");
        buildable.setRequiredGoods(Arrays.asList(new AbstractGoods(hammers, 15)));
        buildable.addRequiredAbility("model.ability.buildShips", true);
        buildable.setRequiredPopulation(4);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FreeColXMLWriter xw = new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false);

        xw.writeStartElement(buildable.getXMLTagName());
        buildable.writeAttributes(xw);
        buildable.writeChildren(xw);
        xw.writeEndElement();
        xw.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        FreeColXMLReader xr = new FreeColXMLReader(in);
        xr.nextTag();

        TestBuildable loaded = new TestBuildable("test", spec);
        loaded.readFromXML(xr);
        xr.close();

        assertEquals(4, loaded.getRequiredPopulation());
        assertTrue(loaded.requiresAbility("model.ability.buildShips"));
        assertEquals(15, loaded.getRequiredAmountOf(hammers));
    }
}
