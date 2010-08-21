/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;
import net.sf.freecol.FreeCol;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.RangeOption;
import net.sf.freecol.util.test.FreeColTestCase;

public final class SpecificationTest extends FreeColTestCase {

    /**
     * Make sure that a specification object can be created without an exception
     * being thrown.
     * 
     */
    public void testLoad() {

    	Specification spec = Specification.getSpecification();
    	
    	assertNotNull(spec);
    	
    	assertEquals(spec, Specification.getSpecification());
    }

    /**
     * Test for some typical abilities.
     */
    public void testUnitAbilities() {
    	Specification spec = Specification.getSpecification();

        UnitType colonist = spec.getUnitType("model.unit.freeColonist");
        assertTrue(colonist.hasAbility("model.ability.foundColony"));
        assertFalse(colonist.hasAbility("model.ability.bornInIndianSettlement"));
        assertTrue(colonist.isRecruitable());
        assertFalse(colonist.hasAbility("model.ability.navalUnit"));
        assertFalse(colonist.hasAbility("model.ability.carryGoods"));
        assertFalse(colonist.hasAbility("model.ability.carryUnits"));
        assertFalse(colonist.hasAbility("model.ability.captureGoods"));

        UnitType wagon = spec.getUnitType("model.unit.wagonTrain");
        assertFalse(wagon.hasAbility("model.ability.foundColony"));
        assertFalse(wagon.isRecruitable());
        assertFalse(wagon.hasAbility("model.ability.navalUnit"));
        assertTrue(wagon.hasAbility("model.ability.carryGoods"));
        assertFalse(wagon.hasAbility("model.ability.carryUnits"));
        assertFalse(wagon.hasAbility("model.ability.captureGoods"));

        UnitType brave = spec.getUnitType("model.unit.brave");
        //assertFalse(brave.hasAbility("model.ability.foundColony"));
        assertTrue(brave.hasAbility("model.ability.bornInIndianSettlement"));
        assertFalse(brave.isRecruitable());
        assertFalse(brave.hasAbility("model.ability.navalUnit"));
        assertTrue(brave.hasAbility("model.ability.carryGoods"));
        assertFalse(brave.hasAbility("model.ability.carryUnits"));
        assertFalse(brave.hasAbility("model.ability.captureGoods"));

        UnitType caravel = spec.getUnitType("model.unit.caravel");
        assertFalse(caravel.hasAbility("model.ability.foundColony"));
        assertFalse(caravel.isRecruitable());
        assertTrue(caravel.hasAbility("model.ability.navalUnit"));
        assertTrue(caravel.hasAbility("model.ability.carryGoods"));
        assertTrue(caravel.hasAbility("model.ability.carryUnits"));
        assertFalse(caravel.hasAbility("model.ability.captureGoods"));
                   
        UnitType privateer = spec.getUnitType("model.unit.privateer");
        assertFalse(privateer.hasAbility("model.ability.foundColony"));
        assertFalse(privateer.isRecruitable());
        assertTrue(privateer.hasAbility("model.ability.navalUnit"));
        assertTrue(privateer.hasAbility("model.ability.carryGoods"));
        assertTrue(privateer.hasAbility("model.ability.carryUnits"));
        assertTrue(privateer.hasAbility("model.ability.captureGoods"));

    }

    public void testFoundingFathers() {

        Specification spec = Specification.getSpecification();

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

    // Check options presence and value
    public void testOptions() {
        Specification spec = Specification.getSpecification();

        //assertTrue(spec.getIntegerOption(GameOptions.STARTING_MONEY).getValue() == 0);

        assertFalse(spec.getBooleanOption(GameOptions.CUSTOM_IGNORE_BOYCOTT).getValue());
        assertFalse(spec.getBooleanOption(GameOptions.EXPERTS_HAVE_CONNECTIONS).getValue());
        assertFalse(spec.getBooleanOption(GameOptions.SAVE_PRODUCTION_OVERFLOW).getValue());
        assertTrue(spec.getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION).getValue());
    }

    // Check difficulty levels presence and values
    public void testDifficultyLevels() {
        assertEquals(6, spec().getDifficultyLevels().size());
        RangeOption diffOpt = (RangeOption) spec().getOption(GameOptions.DIFFICULTY);

        assertTrue(diffOpt.getValue() == 2);
        assertTrue(diffOpt.getItemValues().size() == 5);

        IntegerOption option = null;

        try {
            // should fail, because it is part of uninitialized server difficulty options
            option = spec().getIntegerOption("model.option.crossesIncrement");
            fail();
        } catch (IllegalArgumentException e) {
        }

        // initializing server difficulty options
        spec().applyDifficultyLevel(2);
      
        // should succeed now
        option = spec().getIntegerOption("model.option.crossesIncrement");
        assertNotNull(option);
        assertEquals(10, option.getValue());
    }

    public void testModifiers() {

    	Specification spec = Specification.getSpecification();
    	
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

        Specification spec = Specification.getSpecification();

        List<Nation> europeanNations = spec.getEuropeanNations();
        assertEquals(8, europeanNations.size());
        List<Nation> indianNations = spec.getIndianNations();
        assertEquals(8, indianNations.size());
        List<Nation> REFNations = spec.getREFNations();
        assertEquals(REFNations.size(), europeanNations.size());

    }

    public void testNationTypes() {

        Specification spec = Specification.getSpecification();

        List<IndianNationType> indianNationTypes = spec.getIndianNationTypes();
        assertEquals(8, indianNationTypes.size());
        List<EuropeanNationType> REFNationTypes = spec.getREFNationTypes();
        assertEquals(1, REFNationTypes.size());

    }
    
    public void testReqAbilitiesForEquipmentTypes() {
    	String equipmentTypeStr;
    	Map<String,Boolean> abilitiesReq, expectAbilities;
    	@SuppressWarnings("unused")
        Specification spec = Specification.getSpecification();

        Map<String,Map<String,Boolean>> eqTypesAbilities = new Hashtable<String,Map<String,Boolean>>();
        
        // Abilities
        equipmentTypeStr = "model.equipment.horses";
        expectAbilities = new Hashtable<String,Boolean>();
        expectAbilities.put("model.ability.canBeEquipped", true);
        expectAbilities.put("model.ability.bornInIndianSettlement", false);
        eqTypesAbilities.put(equipmentTypeStr, expectAbilities);
        
        equipmentTypeStr = "model.equipment.muskets";
        expectAbilities = new Hashtable<String,Boolean>();
        expectAbilities.put("model.ability.canBeEquipped", true);
        expectAbilities.put("model.ability.bornInIndianSettlement", false);
        eqTypesAbilities.put(equipmentTypeStr, expectAbilities);
        
        equipmentTypeStr = "model.equipment.indian.horses";
        expectAbilities = new Hashtable<String,Boolean>();
        expectAbilities.put("model.ability.canBeEquipped", true);
        expectAbilities.put("model.ability.bornInIndianSettlement", true);
        eqTypesAbilities.put(equipmentTypeStr, expectAbilities);
        
        equipmentTypeStr = "model.equipment.indian.muskets";
        expectAbilities = new Hashtable<String,Boolean>();
        expectAbilities.put("model.ability.canBeEquipped", true);
        expectAbilities.put("model.ability.bornInIndianSettlement", true);
        eqTypesAbilities.put(equipmentTypeStr, expectAbilities);
        
        // Verify
        for (Entry<String, Map<String, Boolean>> entry : eqTypesAbilities.entrySet()){
            equipmentTypeStr = entry.getKey();
            expectAbilities = entry.getValue();

            EquipmentType equipmentType = Specification.getSpecification().getEquipmentType(equipmentTypeStr);
            abilitiesReq = equipmentType.getAbilitiesRequired();
            for (Entry<String, Boolean> ability : expectAbilities.entrySet()) {
                String key = ability.getKey();
                boolean hasAbility = abilitiesReq.containsKey(key);
                assertTrue(equipmentTypeStr + " missing req. ability " + key,hasAbility);
                assertEquals(equipmentTypeStr + " has wrong value for req. ability " + key,abilitiesReq.get(key),ability.getValue());
            }
        }
    }

    public void testGoodsTypes() {

        GoodsType food = spec().getGoodsType("model.goods.food");
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


}
