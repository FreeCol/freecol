/**
 *  Copyright (C) 2002-2021  The FreeCol Team
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Modifier.ModifierType;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.util.test.FreeColTestCase;


public final class SpecificationTest extends FreeColTestCase {

    private static final BuildingType depotType
        = spec().getBuildingType("model.building.depot");

    private static final GoodsType fishType
        = spec().getGoodsType("model.goods.fish");
    private static final GoodsType foodType
        = spec().getPrimaryFoodType();
    private static final GoodsType horsesType
        = spec().getGoodsType("model.goods.horses");
    private static final GoodsType musketsType
        = spec().getGoodsType("model.goods.muskets");
    private static final GoodsType toolsType
        = spec().getGoodsType("model.goods.tools");

    private static final Role armedBraveRole
        = spec().getRole("model.role.armedBrave");
    private static final Role cavalryRole
        = spec().getRole("model.role.cavalry");
    private static final Role dragoonRole
        = spec().getRole("model.role.dragoon");
    private static final Role infantryRole
        = spec().getRole("model.role.infantry");
    private static final Role missionaryRole
        = spec().getRole("model.role.missionary");
    private static final Role nativeDragoonRole
        = spec().getRole("model.role.nativeDragoon");
    private static final Role soldierRole
        = spec().getRole("model.role.soldier");

    private static final UnitType blackSmithType
        = spec().getUnitType("model.unit.masterBlacksmith");
    private static final UnitType braveType
        = spec().getUnitType("model.unit.brave");
    private static final UnitType caravelType
        = spec().getUnitType("model.unit.caravel");
    private static final UnitType freeColonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType kingsRegularType
        = spec().getUnitType("model.unit.kingsRegular");
    private static final UnitType privateerType
        = spec().getUnitType("model.unit.privateer");
    private static final UnitType wagonType
        = spec().getUnitType("model.unit.wagonTrain");


    /**
     * Make sure that a specification object can be created without an exception
     * being thrown.
     */
    public void testLoad() {
        Specification spec = null;
        try {
            spec = FreeColTcFile.getFreeColTcFile("classic").getSpecification();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        assertNotNull(spec);
    }

    /**
     * Test for some typical abilities.
     */
    public void testUnitAbilities() {
        assertTrue(freeColonistType.hasAbility(Ability.FOUND_COLONY));
        assertFalse(freeColonistType.hasAbility(Ability.BORN_IN_INDIAN_SETTLEMENT));
        assertTrue(freeColonistType.isRecruitable());
        assertFalse(freeColonistType.hasAbility(Ability.NAVAL_UNIT));
        assertFalse(freeColonistType.hasAbility(Ability.CARRY_GOODS));
        assertFalse(freeColonistType.hasAbility(Ability.CARRY_UNITS));
        assertFalse(freeColonistType.hasAbility(Ability.CAPTURE_GOODS));

        assertFalse(wagonType.hasAbility(Ability.FOUND_COLONY));
        assertFalse(wagonType.isRecruitable());
        assertFalse(wagonType.hasAbility(Ability.NAVAL_UNIT));
        assertTrue(wagonType.hasAbility(Ability.CARRY_GOODS));
        assertFalse(wagonType.hasAbility(Ability.CARRY_UNITS));
        assertFalse(wagonType.hasAbility(Ability.CAPTURE_GOODS));

        //assertFalse(brave.hasAbility(Ability.FOUND_COLONY));
        assertTrue(braveType.hasAbility(Ability.BORN_IN_INDIAN_SETTLEMENT));
        assertFalse(braveType.isRecruitable());
        assertFalse(braveType.hasAbility(Ability.NAVAL_UNIT));
        assertTrue(braveType.hasAbility(Ability.CARRY_GOODS));
        assertFalse(braveType.hasAbility(Ability.CARRY_UNITS));
        assertFalse(braveType.hasAbility(Ability.CAPTURE_GOODS));

        assertFalse(caravelType.hasAbility(Ability.FOUND_COLONY));
        assertFalse(caravelType.isRecruitable());
        assertTrue(caravelType.hasAbility(Ability.NAVAL_UNIT));
        assertTrue(caravelType.hasAbility(Ability.CARRY_GOODS));
        assertTrue(caravelType.hasAbility(Ability.CARRY_UNITS));
        assertFalse(caravelType.hasAbility(Ability.CAPTURE_GOODS));

        assertFalse(privateerType.hasAbility(Ability.FOUND_COLONY));
        assertFalse(privateerType.isRecruitable());
        assertTrue(privateerType.hasAbility(Ability.NAVAL_UNIT));
        assertTrue(privateerType.hasAbility(Ability.CARRY_GOODS));
        assertTrue(privateerType.hasAbility(Ability.CARRY_UNITS));
        assertTrue(privateerType.hasAbility(Ability.CAPTURE_GOODS));
    }

    public void testFoundingFathers() {
        final FoundingFather smith
            = spec().getFoundingFather("model.foundingFather.adamSmith");
        assertNotNull(smith);
        assertEquals(FoundingFather.FoundingFatherType.TRADE, smith.getType());
        // weight is some value in [0, 10]
        assertTrue(smith.getWeight(0) >= 0);
        assertTrue(smith.getWeight(1) >= 0);
        assertTrue(smith.getWeight(2) >= 0);
        assertTrue(smith.getWeight(0) <= 10);
        assertTrue(smith.getWeight(1) <= 10);
        assertTrue(smith.getWeight(2) <= 10);

        assertEquals(0, smith.getWeight(-1));
        assertEquals(0, smith.getWeight(34));
        // check for ability
        assertTrue(smith.hasAbility(Ability.BUILD_FACTORY));
    }

    public void testModifiers() {
        // Percentage Modifier
        /*
        BuildingType ironWorks = spec.getBuildingType("model.building.ironWorks");
        Modifier modifier = ironWorks.getModifiers("model.goods.tools").iterator().next();
        assertEquals(Modifier.Type.PERCENTAGE, modifier.getType());
        assertEquals(50f, modifier.getValue());
        */

        // Additive Modifier
        Modifier modifier = first(depotType.getModifiers(Modifier.WAREHOUSE_STORAGE));
        assertEquals(ModifierType.ADDITIVE, modifier.getType());
        assertEquals(100f, modifier.getValue());

        // Multiplicative Modifier
        modifier = first(blackSmithType.getModifiers("model.goods.tools"));
        assertEquals(ModifierType.MULTIPLICATIVE, modifier.getType());
        assertEquals(2f, modifier.getValue());
    }

    public void testNations() {
        // 4 original European nations, 4 in freecol rules
        // The unknown enemy is deliberately absent from the European
        // nations list
        List<Nation> europeanNations = spec().getEuropeanNations();
        assertEquals(8, europeanNations.size());

        // 8 original native nations
        List<Nation> indianNations = spec().getIndianNations();
        assertEquals(8, indianNations.size());

        // Unknown enemy has no REF
        List<Nation> REFNations = spec().getREFNations();
        assertEquals(europeanNations.size(), REFNations.size());
    }

    public void testNationTypes() {
        final Specification spec = spec();

        List<IndianNationType> indianNationTypes = spec.getIndianNationTypes();
        assertEquals(8, indianNationTypes.size());
        List<EuropeanNationType> REFNationTypes = spec.getREFNationTypes();
        assertEquals(1, REFNationTypes.size());

    }

    public void testRequiredAbilitiesForRoles() {
        final Specification spec = spec();
        Map<String, Boolean> abilitiesReq, expectAbilities;
        Map<String, Map<String, Boolean>> roleAbilities = new HashMap<>();

        expectAbilities = new HashMap<>();
        expectAbilities.put(Ability.CAN_BE_EQUIPPED, true);
        expectAbilities.put(Ability.NATIVE, false);
        expectAbilities.put(Ability.REF_UNIT, false);
        roleAbilities.put("model.role.scout", expectAbilities);

        expectAbilities = new HashMap<>();
        expectAbilities.put(Ability.CAN_BE_EQUIPPED, true);
        expectAbilities.put(Ability.NATIVE, false);
        expectAbilities.put(Ability.REF_UNIT, false);
        roleAbilities.put("model.role.soldier", expectAbilities);

        expectAbilities = new HashMap<>();
        expectAbilities.put(Ability.CAN_BE_EQUIPPED, true);
        expectAbilities.put(Ability.NATIVE, false);
        expectAbilities.put(Ability.REF_UNIT, false);
        roleAbilities.put("model.role.dragoon", expectAbilities);

        expectAbilities = new HashMap<>();
        expectAbilities.put(Ability.CAN_BE_EQUIPPED, true);
        expectAbilities.put(Ability.NATIVE, false);
        expectAbilities.put(Ability.REF_UNIT, false);
        roleAbilities.put("model.role.pioneer", expectAbilities);

        expectAbilities = new HashMap<>();
        expectAbilities.put(Ability.CAN_BE_EQUIPPED, true);
        expectAbilities.put(Ability.DRESS_MISSIONARY, true);
        expectAbilities.put(Ability.NATIVE, false);
        expectAbilities.put(Ability.REF_UNIT, false);
        roleAbilities.put("model.role.missionary", expectAbilities);

        expectAbilities = new HashMap<>();
        expectAbilities.put(Ability.CAN_BE_EQUIPPED, true);
        expectAbilities.put(Ability.NATIVE, false);
        expectAbilities.put(Ability.REF_UNIT, true);
        expectAbilities.put(Ability.ROYAL_EXPEDITIONARY_FORCE, true);
        roleAbilities.put("model.role.infantry", expectAbilities);

        expectAbilities = new HashMap<>();
        expectAbilities.put(Ability.CAN_BE_EQUIPPED, true);
        expectAbilities.put(Ability.NATIVE, false);
        expectAbilities.put(Ability.REF_UNIT, true);
        expectAbilities.put(Ability.ROYAL_EXPEDITIONARY_FORCE, true);
        roleAbilities.put("model.role.cavalry", expectAbilities);

        expectAbilities = new HashMap<>();
        expectAbilities.put(Ability.CAN_BE_EQUIPPED, true);
        expectAbilities.put(Ability.NATIVE, true);
        expectAbilities.put(Ability.REF_UNIT, false);
        roleAbilities.put("model.role.mountedBrave", expectAbilities);

        expectAbilities = new HashMap<>();
        expectAbilities.put(Ability.CAN_BE_EQUIPPED, true);
        expectAbilities.put(Ability.NATIVE, true);
        expectAbilities.put(Ability.REF_UNIT, false);
        roleAbilities.put("model.role.armedBrave", expectAbilities);

        expectAbilities = new HashMap<>();
        expectAbilities.put(Ability.CAN_BE_EQUIPPED, true);
        expectAbilities.put(Ability.NATIVE, true);
        expectAbilities.put(Ability.REF_UNIT, false);
        roleAbilities.put("model.role.nativeDragoon", expectAbilities);

        // Verify
        forEachMapEntry(roleAbilities, e -> {
                Role role = spec.getRole(e.getKey());
                Map<String, Boolean> required = role.getRequiredAbilities();
                Map<String, Boolean> expect = e.getValue();
                for (String key : required.keySet()) {
                    Boolean req = required.get(key);
                    Boolean val = expect.get(key);
                    assertNotNull(role.getId() + " missing " + key, val);
                    assertEquals(role.getId() + " " + key + " value != " + req,
                                 req, val);
                    expect.remove(key);
                }
                assertEquals(role.getId() + " excess abilities", 0,
                             expect.size());
            });

        Role role;
        role = spec.getRole("model.role.default");
        assertNotNull(role);
        checkGoods(role.getId(), role.getRequiredGoodsList());

        role = spec.getRole("model.role.scout");
        assertNotNull(role);
        checkGoods(role.getId(), role.getRequiredGoodsList(),
            new AbstractGoods(horsesType, 50));

        role = spec.getRole("model.role.soldier");
        assertNotNull(role);
        checkGoods(role.getId(), role.getRequiredGoodsList(),
            new AbstractGoods(musketsType, 50));

        role = spec.getRole("model.role.dragoon");
        assertNotNull(role);
        checkGoods(role.getId(), role.getRequiredGoodsList(),
            new AbstractGoods(horsesType, 50),
            new AbstractGoods(musketsType, 50));

        role = spec.getRole("model.role.pioneer");
        assertNotNull(role);
        checkGoods(role.getId(), role.getRequiredGoodsList(),
            new AbstractGoods(toolsType, 20));

        role = spec.getRole("model.role.missionary");
        assertNotNull(role);
        checkGoods(role.getId(), role.getRequiredGoodsList());

        role = spec.getRole("model.role.infantry");
        assertNotNull(role);
        checkGoods(role.getId(), role.getRequiredGoodsList(),
            new AbstractGoods(musketsType, 50));

        role = spec.getRole("model.role.cavalry");
        assertNotNull(role);
        checkGoods(role.getId(), role.getRequiredGoodsList(),
            new AbstractGoods(horsesType, 50),
            new AbstractGoods(musketsType, 50));

        role = spec.getRole("model.role.mountedBrave");
        assertNotNull(role);
        checkGoods(role.getId(), role.getRequiredGoodsList(),
            new AbstractGoods(horsesType, 25));

        role = spec.getRole("model.role.armedBrave");
        assertNotNull(role);
        checkGoods(role.getId(), role.getRequiredGoodsList(),
            new AbstractGoods(musketsType, 25));

        role = spec.getRole("model.role.nativeDragoon");
        assertNotNull(role);
        checkGoods(role.getId(), role.getRequiredGoodsList(),
            new AbstractGoods(horsesType, 25),
            new AbstractGoods(musketsType, 25));
    }

    public void testGoodsTypes() {
        assertTrue(foodType.isFarmed());
        assertTrue(spec().getFarmedGoodsTypeList().contains(foodType));
        assertTrue(foodType.isFoodType());
        assertTrue(spec().getFoodGoodsTypeList().contains(foodType));

        assertTrue(fishType.isFarmed());
        assertTrue(spec().getFarmedGoodsTypeList().contains(fishType));
        assertTrue(fishType.isFoodType());
        assertTrue(spec().getFoodGoodsTypeList().contains(fishType));
    }

    public void testExtends() {
        String specification = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<freecol-specification id=\"test\" extends=\"freecol\">"
            + "<unit-types>"
            + "<unit-type id=\"model.unit.milkmaid\" extends=\"colonist\" "
            + "expert-production=\"model.goods.food\" />"
            + "</unit-types>"
            + "</freecol-specification>";

        Specification spec = null;
        try {
            spec = new Specification(new ByteArrayInputStream(specification.getBytes()));
        } catch (XMLStreamException xse) {
            fail("Spec read fail");
        }
        assertNotNull(spec.getUnitType("model.unit.milkmaid"));
        assertNotNull(spec.getUnitType("model.unit.caravel"));

        // restore original values
        try {
            spec = FreeColTcFile.getFreeColTcFile("freecol").getSpecification();
        } catch (Exception e) {
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

        Specification spec = null;
        try {
            spec = new Specification(new ByteArrayInputStream(specification.getBytes()));
        } catch (XMLStreamException xse) {
            fail("Spec read fail");
        }
        assertNull("Caravel should be undefined",
                   spec.getUnitType("model.unit.caravel"));

        for (UnitType unitType : spec.getUnitTypeList()) {
            assertFalse("model.unit.caravel".equals(unitType.getId()));
        }
    }

    public void testGetDefaultUnitType() {
        NationType europeanNationType = null;
        NationType nativeNationType = null;
        NationType refNationType = null;
        for (NationType nt : spec().getNationTypes()) {
            if (nt.isIndian()) {
                if (nativeNationType == null) nativeNationType = nt;
            } else if (nt.isREF()) {
                if (refNationType == null) refNationType = nt;
            } else {
                if (europeanNationType == null) europeanNationType = nt;
            }
        }
        assertNotNull("No European nation type", europeanNationType);
        assertNotNull("No native nation type", nativeNationType);
        assertNotNull("No REF nation type", refNationType);
                
        assertEquals("Should find free colonist", freeColonistType,
                     spec().getDefaultUnitType(europeanNationType));
        assertEquals("Should find brave", braveType,
                     spec().getDefaultUnitType(nativeNationType));
        assertEquals("Should find kings regular", kingsRegularType,
                     spec().getDefaultUnitType(refNationType));
    }        
        
    public void testLoadMods() {
        try {
            Specification spec = FreeColTcFile.getFreeColTcFile("freecol")
                .getSpecification();
            int numberOfUnitTypes = spec.getUnitTypeList().size();
            List<FreeColModFile> mods = new ArrayList<>();
            mods.add(new FreeColModFile(new File("data/mods/example")));
            spec.loadMods(mods);
            UnitType milkmaid = spec.getUnitType("model.unit.milkmaid");
            assertEquals(numberOfUnitTypes + 1, spec.getUnitTypeList().size());
        } catch (IOException|XMLStreamException ex) {
            fail(ex.toString());
        }
    }
}
