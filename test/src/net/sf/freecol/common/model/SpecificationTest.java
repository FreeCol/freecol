/**
 *  Copyright (C) 2002-2012  The FreeCol Team
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
import java.io.FileInputStream;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.util.test.FreeColTestCase;

public final class SpecificationTest extends FreeColTestCase {

    /**
     * Make sure that a specification object can be created without an exception
     * being thrown.
     *
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
        // weight 3 is the default
        assertTrue(smith.getWeight(3) == smith.getWeight(0));
        assertTrue(smith.getWeight(3) == smith.getWeight(34));
        // check for ability
        assertTrue(smith.hasAbility("model.ability.buildFactory"));
    }

    public void testModifiers() {
        Specification spec = spec();

        // Percentage Modifier
        BuildingType ironWorks = spec.getBuildingType("model.building.ironWorks");
        Modifier modifier = ironWorks.getModifierSet("model.goods.tools").iterator().next();
        assertEquals(Modifier.Type.PERCENTAGE, modifier.getType());
        assertEquals(50f, modifier.getValue());

        // Additive Modifier
        BuildingType depot = spec.getBuildingType("model.building.depot");
        modifier = depot.getModifierSet("model.modifier.warehouseStorage").iterator().next();
        assertEquals(Modifier.Type.ADDITIVE, modifier.getType());
        assertEquals(100f, modifier.getValue());

        // Multiplicative Modifier
        UnitType blackSmith = spec.getUnitType("model.unit.masterBlacksmith");
        modifier = blackSmith.getModifierSet("model.goods.tools").iterator().next();
        assertEquals(Modifier.Type.MULTIPLICATIVE, modifier.getType());
        assertEquals(2f, modifier.getValue());
    }

    public void testNations() {

        Specification spec = spec();

        List<Nation> europeanNations = spec.getEuropeanNations();
        assertEquals(8, europeanNations.size());
        List<Nation> indianNations = spec.getIndianNations();
        assertEquals(8, indianNations.size());
        List<Nation> REFNations = spec.getREFNations();
        assertEquals(REFNations.size(), europeanNations.size());

    }

    public void testNationTypes() {

        Specification spec = spec();

        List<IndianNationType> indianNationTypes = spec.getIndianNationTypes();
        assertEquals(8, indianNationTypes.size());
        List<EuropeanNationType> REFNationTypes = spec.getREFNationTypes();
        assertEquals(1, REFNationTypes.size());

    }

    public void testReqAbilitiesForEquipmentTypes() {
        String equipmentTypeStr;
        Map<String,Boolean> abilitiesReq, expectAbilities;
        Specification spec = spec();

        Map<String,Map<String,Boolean>> eqTypesAbilities = new Hashtable<String,Map<String,Boolean>>();

        // Abilities
        equipmentTypeStr = "model.equipment.horses";
        expectAbilities = new Hashtable<String,Boolean>();
        expectAbilities.put("model.ability.canBeEquipped", true);
        expectAbilities.put(Ability.BORN_IN_INDIAN_SETTLEMENT, false);
        eqTypesAbilities.put(equipmentTypeStr, expectAbilities);

        equipmentTypeStr = "model.equipment.muskets";
        expectAbilities = new Hashtable<String,Boolean>();
        expectAbilities.put("model.ability.canBeEquipped", true);
        expectAbilities.put(Ability.BORN_IN_INDIAN_SETTLEMENT, false);
        eqTypesAbilities.put(equipmentTypeStr, expectAbilities);

        equipmentTypeStr = "model.equipment.indian.horses";
        expectAbilities = new Hashtable<String,Boolean>();
        expectAbilities.put("model.ability.canBeEquipped", true);
        expectAbilities.put(Ability.BORN_IN_INDIAN_SETTLEMENT, true);
        eqTypesAbilities.put(equipmentTypeStr, expectAbilities);

        equipmentTypeStr = "model.equipment.indian.muskets";
        expectAbilities = new Hashtable<String,Boolean>();
        expectAbilities.put("model.ability.canBeEquipped", true);
        expectAbilities.put(Ability.BORN_IN_INDIAN_SETTLEMENT, true);
        eqTypesAbilities.put(equipmentTypeStr, expectAbilities);

        // Verify
        for (Entry<String, Map<String, Boolean>> entry : eqTypesAbilities.entrySet()){
            equipmentTypeStr = entry.getKey();
            expectAbilities = entry.getValue();

            EquipmentType equipmentType = spec.getEquipmentType(equipmentTypeStr);
            abilitiesReq = equipmentType.getRequiredAbilities();
            for (Entry<String, Boolean> ability : expectAbilities.entrySet()) {
                String key = ability.getKey();
                boolean hasAbility = abilitiesReq.containsKey(key);
                assertTrue(equipmentTypeStr + " missing req. ability " + key,hasAbility);
                assertEquals(equipmentTypeStr + " has wrong value for req. ability " + key,abilitiesReq.get(key),ability.getValue());
            }
        }
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

    public void testLoadFragment() {
        try {
            Specification specification = new Specification(new FreeColTcFile("freecol").getSpecificationInputStream());
            int numberOfUnitTypes = specification.numberOfUnitTypes();
            specification.loadFragment(new FileInputStream("data/mods/example/specification.xml"));
            UnitType milkmaid = specification.getUnitType("model.unit.milkmaid");
            assertEquals(numberOfUnitTypes + 1, specification.numberOfUnitTypes());
        } catch(Exception e) {
            fail(e.getMessage());
        }

    }


}
