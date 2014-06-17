/**
 *  Copyright (C) 2002-2013  The FreeCol Team
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
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Modifier.ModifierType;
import net.sf.freecol.util.test.FreeColTestCase;


public final class SpecificationTest extends FreeColTestCase {

    private static final GoodsType horsesType
        = spec().getGoodsType("model.goods.horses");
    private static final GoodsType musketsType
        = spec().getGoodsType("model.goods.muskets");
    private static final GoodsType toolsType
        = spec().getGoodsType("model.goods.tools");


    /**
     * Make sure that a specification object can be created without an exception
     * being thrown.
     */
    public void testLoad() {
        Specification spec = null;
        try {
            spec = new FreeColTcFile("freecol").getSpecification();
        } catch(Exception e) {
            e.printStackTrace();
            fail();
        }
        assertNotNull(spec);
    }

    /**
     * Test for some typical abilities.
     */
    public void testUnitAbilities() {
        Specification spec = spec();

        UnitType colonist = spec.getUnitType("model.unit.freeColonist");
        assertTrue(colonist.hasAbility(Ability.FOUND_COLONY));
        assertFalse(colonist.hasAbility(Ability.BORN_IN_INDIAN_SETTLEMENT));
        assertTrue(colonist.isRecruitable());
        assertFalse(colonist.hasAbility(Ability.NAVAL_UNIT));
        assertFalse(colonist.hasAbility(Ability.CARRY_GOODS));
        assertFalse(colonist.hasAbility(Ability.CARRY_UNITS));
        assertFalse(colonist.hasAbility(Ability.CAPTURE_GOODS));

        UnitType wagon = spec.getUnitType("model.unit.wagonTrain");
        assertFalse(wagon.hasAbility(Ability.FOUND_COLONY));
        assertFalse(wagon.isRecruitable());
        assertFalse(wagon.hasAbility(Ability.NAVAL_UNIT));
        assertTrue(wagon.hasAbility(Ability.CARRY_GOODS));
        assertFalse(wagon.hasAbility(Ability.CARRY_UNITS));
        assertFalse(wagon.hasAbility(Ability.CAPTURE_GOODS));

        UnitType brave = spec.getUnitType("model.unit.brave");
        //assertFalse(brave.hasAbility(Ability.FOUND_COLONY));
        assertTrue(brave.hasAbility(Ability.BORN_IN_INDIAN_SETTLEMENT));
        assertFalse(brave.isRecruitable());
        assertFalse(brave.hasAbility(Ability.NAVAL_UNIT));
        assertTrue(brave.hasAbility(Ability.CARRY_GOODS));
        assertFalse(brave.hasAbility(Ability.CARRY_UNITS));
        assertFalse(brave.hasAbility(Ability.CAPTURE_GOODS));

        UnitType caravel = spec.getUnitType("model.unit.caravel");
        assertFalse(caravel.hasAbility(Ability.FOUND_COLONY));
        assertFalse(caravel.isRecruitable());
        assertTrue(caravel.hasAbility(Ability.NAVAL_UNIT));
        assertTrue(caravel.hasAbility(Ability.CARRY_GOODS));
        assertTrue(caravel.hasAbility(Ability.CARRY_UNITS));
        assertFalse(caravel.hasAbility(Ability.CAPTURE_GOODS));

        UnitType privateer = spec.getUnitType("model.unit.privateer");
        assertFalse(privateer.hasAbility(Ability.FOUND_COLONY));
        assertFalse(privateer.isRecruitable());
        assertTrue(privateer.hasAbility(Ability.NAVAL_UNIT));
        assertTrue(privateer.hasAbility(Ability.CARRY_GOODS));
        assertTrue(privateer.hasAbility(Ability.CARRY_UNITS));
        assertTrue(privateer.hasAbility(Ability.CAPTURE_GOODS));
    }

    public void testFoundingFathers() {
        Specification spec = spec();

        FoundingFather smith = spec.getFoundingFather("model.foundingFather.adamSmith");
        assertFalse(smith == null);
        assertTrue(smith.getType() == FoundingFather.FoundingFatherType.TRADE);
        // weight is some value in [0, 10]
        assertTrue(smith.getWeight(1) >= 0);
        assertTrue(smith.getWeight(2) >= 0);
        assertTrue(smith.getWeight(3) >= 0);
        assertTrue(smith.getWeight(1) <= 10);
        assertTrue(smith.getWeight(2) <= 10);
        assertTrue(smith.getWeight(3) <= 10);

        assertTrue(smith.getWeight(0) == 0);
        assertTrue(smith.getWeight(34) == 0);
        // check for ability
        assertTrue(smith.hasAbility(Ability.BUILD_FACTORY));
    }

    public void testModifiers() {
        Specification spec = spec();

        // Percentage Modifier
        /*
        BuildingType ironWorks = spec.getBuildingType("model.building.ironWorks");
        Modifier modifier = ironWorks.getModifierSet("model.goods.tools").iterator().next();
        assertEquals(Modifier.Type.PERCENTAGE, modifier.getType());
        assertEquals(50f, modifier.getValue());
        */

        // Additive Modifier
        BuildingType depot = spec.getBuildingType("model.building.depot");
        Modifier modifier = depot.getModifierSet(Modifier.WAREHOUSE_STORAGE).iterator().next();
        assertEquals(ModifierType.ADDITIVE, modifier.getType());
        assertEquals(100f, modifier.getValue());

        // Multiplicative Modifier
        UnitType blackSmith = spec.getUnitType("model.unit.masterBlacksmith");
        modifier = blackSmith.getModifierSet("model.goods.tools").iterator().next();
        assertEquals(ModifierType.MULTIPLICATIVE, modifier.getType());
        assertEquals(2f, modifier.getValue());
    }

    public void testNations() {
        // 4 original European nations, 4 in freecol rules, 1 unknown enemy
        List<Nation> europeanNations = spec().getEuropeanNations();
        assertEquals(9, europeanNations.size());

        // 8 original native nations
        List<Nation> indianNations = spec().getIndianNations();
        assertEquals(8, indianNations.size());

        // Unknown enemy has no REF
        List<Nation> REFNations = spec().getREFNations();
        assertEquals(REFNations.size(), europeanNations.size() - 1);
    }

    public void testNationTypes() {

        Specification spec = spec();

        List<IndianNationType> indianNationTypes = spec.getIndianNationTypes();
        assertEquals(8, indianNationTypes.size());
        List<EuropeanNationType> REFNationTypes = spec.getREFNationTypes();
        assertEquals(1, REFNationTypes.size());

    }

    private void checkRequiredGoods(String roleId, AbstractGoods... check) {
        Role role = spec().getRole(roleId);
        assertNotNull(role);
        List<AbstractGoods> required
            = new ArrayList<AbstractGoods>(role.getRequiredGoods());
        for (AbstractGoods ag : check) {
            assertTrue(roleId + " requires " + ag, required.contains(ag));
            required.remove(ag);
        }
        assertTrue(roleId + " requires more goods", required.isEmpty());
    }
              
    public void testRequiredAbilitiesForRoles() {
        final Specification spec = spec();
        Map<String, Boolean> abilitiesReq, expectAbilities;
        Map<String, Map<String, Boolean>> roleAbilities
            = new HashMap<String,Map<String,Boolean>>();

        expectAbilities = new HashMap<String, Boolean>();
        expectAbilities.put(Ability.CAN_BE_EQUIPPED, true);
        expectAbilities.put(Ability.NATIVE, false);
        roleAbilities.put("model.role.scout", expectAbilities);

        expectAbilities = new HashMap<String, Boolean>();
        expectAbilities.put(Ability.CAN_BE_EQUIPPED, true);
        expectAbilities.put(Ability.NATIVE, false);
        roleAbilities.put("model.role.soldier", expectAbilities);

        expectAbilities = new HashMap<String, Boolean>();
        expectAbilities.put(Ability.CAN_BE_EQUIPPED, true);
        expectAbilities.put(Ability.NATIVE, false);
        roleAbilities.put("model.role.dragoon", expectAbilities);

        expectAbilities = new HashMap<String, Boolean>();
        expectAbilities.put(Ability.CAN_BE_EQUIPPED, true);
        expectAbilities.put(Ability.NATIVE, false);
        roleAbilities.put("model.role.pioneer", expectAbilities);

        expectAbilities = new HashMap<String, Boolean>();
        expectAbilities.put(Ability.CAN_BE_EQUIPPED, true);
        expectAbilities.put(Ability.NATIVE, false);
        expectAbilities.put(Ability.DRESS_MISSIONARY, true);
        roleAbilities.put("model.role.missionary", expectAbilities);

        expectAbilities = new HashMap<String, Boolean>();
        expectAbilities.put(Ability.CAN_BE_EQUIPPED, true);
        expectAbilities.put(Ability.NATIVE, false);
        expectAbilities.put(Ability.REF_UNIT, true);
        expectAbilities.put(Ability.ROYAL_EXPEDITIONARY_FORCE, true);
        roleAbilities.put("model.role.infantry", expectAbilities);

        expectAbilities = new HashMap<String, Boolean>();
        expectAbilities.put(Ability.CAN_BE_EQUIPPED, true);
        expectAbilities.put(Ability.NATIVE, false);
        expectAbilities.put(Ability.REF_UNIT, true);
        expectAbilities.put(Ability.ROYAL_EXPEDITIONARY_FORCE, true);
        roleAbilities.put("model.role.cavalry", expectAbilities);

        expectAbilities = new HashMap<String, Boolean>();
        expectAbilities.put(Ability.NATIVE, true);
        roleAbilities.put("model.role.mountedBrave", expectAbilities);

        expectAbilities = new HashMap<String, Boolean>();
        expectAbilities.put(Ability.NATIVE, true);
        roleAbilities.put("model.role.armedBrave", expectAbilities);

        expectAbilities = new HashMap<String, Boolean>();
        expectAbilities.put(Ability.NATIVE, true);
        roleAbilities.put("model.role.nativeDragoon", expectAbilities);

        // Verify
        for (Entry<String, Map<String, Boolean>> entry
                 : roleAbilities.entrySet()) {
            Role role = spec.getRole(entry.getKey());
            Map<String, Boolean> required = role.getRequiredAbilities();
            expectAbilities = entry.getValue();
            for (String key : required.keySet()) {
                Boolean req = required.get(key);
                Boolean val = expectAbilities.get(key);
                assertNotNull(role.getId() + " missing " + key, val);
                assertEquals(role.getId() + " " + key + " value != " + req,
                    req, val);
                expectAbilities.remove(key);
            }
            assertEquals(role.getId() + " excess abilities", 0,
                expectAbilities.size());
        }

        checkRequiredGoods("model.role.default");
        checkRequiredGoods("model.role.scout",
            new AbstractGoods(horsesType, 50));
        checkRequiredGoods("model.role.soldier",
            new AbstractGoods(musketsType, 50));
        checkRequiredGoods("model.role.dragoon",
            new AbstractGoods(horsesType, 50),
            new AbstractGoods(musketsType, 50));
        checkRequiredGoods("model.role.pioneer",
            new AbstractGoods(toolsType, 20));
        checkRequiredGoods("model.role.missionary");
        checkRequiredGoods("model.role.infantry",
            new AbstractGoods(musketsType, 50));
        checkRequiredGoods("model.role.cavalry",
            new AbstractGoods(horsesType, 50),
            new AbstractGoods(musketsType, 50));
        checkRequiredGoods("model.role.mountedBrave",
            new AbstractGoods(horsesType, 25));
        checkRequiredGoods("model.role.armedBrave",
            new AbstractGoods(musketsType, 25));
        checkRequiredGoods("model.role.nativeDragoon",
            new AbstractGoods(horsesType, 25),
            new AbstractGoods(musketsType, 25));
    }

    public void testGoodsTypes() {
        GoodsType food = spec().getPrimaryFoodType();
        assertTrue(food.isFarmed());
        assertTrue(spec().getFarmedGoodsTypeList().contains(food));
        assertTrue(food.isFoodType());
        assertTrue(spec().getFoodGoodsTypeList().contains(food));

        GoodsType fish = spec().getGoodsType("model.goods.fish");
        assertTrue(fish.isFarmed());
        assertTrue(spec().getFarmedGoodsTypeList().contains(fish));
        assertTrue(fish.isFoodType());
        assertTrue(spec().getFoodGoodsTypeList().contains(fish));
    }

    public void testExtends() {
        String specification = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<freecol-specification id=\"test\" extends=\"freecol\">"
            + "<unit-types>"
            + "<unit-type id=\"model.unit.milkmaid\" extends=\"colonist\" "
            + "expert-production=\"model.goods.food\" />"
            + "</unit-types>"
            + "</freecol-specification>";

        Specification spec = new Specification(new ByteArrayInputStream(specification.getBytes()));

        assertNotNull(spec.getUnitType("model.unit.milkmaid"));
        assertNotNull(spec.getUnitType("model.unit.caravel"));

        // restore original values
        try {
            spec = new Specification(new FreeColTcFile("freecol").getSpecificationInputStream());
        } catch(Exception e) {
            System.out.println(e);
        }
    }

    public void testExtendsDelete() {
        String specification = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<freecol-specification id=\"test\" extends=\"freecol\">"
            + "<unit-types>"
            + "<delete id=\"model.unit.caravel\" />"
            + "</unit-types>"
            + "</freecol-specification>";

        Specification spec = new Specification(new ByteArrayInputStream(specification.getBytes()));

        try {
            spec.getUnitType("model.unit.caravel");
            fail("Caravel is defined.");
        } catch(IllegalArgumentException e) {
        }

        for (UnitType unitType : spec.getUnitTypeList()) {
            assertFalse("model.unit.caravel".equals(unitType.getId()));
        }

        // restore original values
        try {
            spec = new Specification(new FreeColTcFile("freecol").getSpecificationInputStream());
        } catch(Exception e) {
            System.out.println(e);
        }
    }

    public void testLoadMods() {
        try {
            Specification specification = new Specification(new FreeColTcFile("freecol").getSpecificationInputStream());
            int numberOfUnitTypes = specification.getUnitTypeList().size();
            List<FreeColModFile> mods = new ArrayList<FreeColModFile>();
            mods.add(new FreeColModFile(new File("data/mods/example")));
            specification.loadMods(mods);
            UnitType milkmaid = specification.getUnitType("model.unit.milkmaid");
            assertEquals(numberOfUnitTypes + 1, 
                specification.getUnitTypeList().size());
        } catch(Exception e) {
            fail(e.getMessage());
        }
    }
}
