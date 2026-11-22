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

import java.util.List;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;

public class UnitTypeTest extends FreeColTestCase {

    public void testCapacityLogic() {
        Specification spec = spec();
        UnitType merchantman = spec.getUnitType("model.unit.merchantman");
        assertNotNull(merchantman);

        assertTrue(merchantman.isNaval());
        assertTrue(merchantman.canCarryGoods());
        assertTrue(merchantman.getSpace() > 0);
        assertTrue(merchantman.getSpaceTaken() > merchantman.getSpace());
    }

    public void testConsumptionLogic() {
        Specification spec = spec();
        UnitType unit = new UnitType("model.unit.testConsumer", spec);
        GoodsType food = spec.getGoodsType("model.goods.food");
        
        assertEquals(0, unit.getConsumptionOf(food));
        
        TypeCountMap<GoodsType> map = new TypeCountMap<>();
        map.incrementCount(food, 2);
        setField(unit, "consumption", map);
        
        assertEquals(2, unit.getConsumptionOf(food));
        List<AbstractGoods> consumed = unit.getConsumedGoods();
        assertEquals(1, consumed.size());
        assertEquals(food, consumed.get(0).getType());
        assertEquals(2, consumed.get(0).getAmount());
    }

    public void testDefaultRoleIsLoaded() {
        Specification spec = spec();
        UnitType unit = spec.getUnitType("model.unit.freeColonist");

        assertNotNull("Default role must not be null", unit.getDefaultRole());
    }

    public void testSerialization() throws Exception {
        Specification spec = spec();
        UnitType original = spec.getUnitType("model.unit.freeColonist");
        
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        FreeColXMLWriter xw = new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false);
        
        xw.writeStartElement(original.getXMLTagName());
        original.writeAttributes(xw);
        original.writeChildren(xw);
        xw.writeEndElement();
        xw.close();
        
        java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(out.toByteArray());
        FreeColXMLReader xr = new FreeColXMLReader(in);
        xr.nextTag();
        
        UnitType loaded = new UnitType(original.getId(), spec);
        loaded.readFromXML(xr);
        xr.close();
        
        assertEquals(original.getMovement(), loaded.getMovement());
        assertEquals(original.getLineOfSight(), loaded.getLineOfSight());
        assertEquals(original.getSkill(), loaded.getSkill());
        assertEquals(original.getBaseOffence(), loaded.getBaseOffence());
        assertEquals(original.getBaseDefence(), loaded.getBaseDefence());
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = null;
            Class<?> c = target.getClass();
            while (c != null && field == null) {
                try {
                    field = c.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    c = c.getSuperclass();
                }
            }
            if (field == null) throw new NoSuchFieldException(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Reflection failed for: " + fieldName, e);
        }
    }

    public void testHasSkill() {
        UnitType unit = new UnitType("test", spec());

        setField(unit, "skill", Constants.UNDEFINED);
        assertFalse(unit.hasSkill());

        setField(unit, "skill", 5);
        assertTrue(unit.hasSkill());
    }

    public void testHasPrice() {
        UnitType unit = new UnitType("test", spec());

        setField(unit, "price", Constants.UNDEFINED);
        assertFalse(unit.hasPrice());

        setField(unit, "price", 100);
        assertTrue(unit.hasPrice());
    }

    public void testGetSpaceTakenUsesMaximum() {
        UnitType unit = new UnitType("test", spec());

        unit.setSpace(5);
        unit.setSpaceTaken(1);

        assertEquals(6, unit.getSpaceTaken());
    }

    public void testIsRecruitable() {
        UnitType unit = new UnitType("test", spec());

        setField(unit, "recruitProbability", 0);
        assertFalse(unit.isRecruitable());

        setField(unit, "recruitProbability", 10);
        assertTrue(unit.isRecruitable());
    }

    public void testGetDisplayRoleId() {
        UnitType colonist =
            spec().getUnitType("model.unit.masterCarpenter");

        assertNotNull(colonist.getDisplayRoleId());
    }

    public void testGetWorkingAsKey() {
        UnitType colonist =
            spec().getUnitType("model.unit.freeColonist");

        assertEquals(
            "model.unit.freeColonist.workingAs",
            colonist.getWorkingAsKey());
    }

    public void testCanCarryFlags() {
        Specification spec = spec();

        UnitType carrier = spec.getUnitType("model.unit.galleon");

        assertTrue(carrier.canCarryGoods());
        assertTrue(carrier.canCarryUnits());
    }

    public void testCanMoveToHighSeasMatchesNaval() {
        UnitType unit = spec().getUnitType("model.unit.freeColonist");

        assertEquals(unit.isNaval(), unit.canMoveToHighSeas());
    }

    public void testIsDefensive() {
        UnitType defensive = spec().getUnitTypeList().stream()
            .filter(ut -> ut.getBaseDefence() > UnitType.DEFAULT_DEFENCE)
            .findFirst()
            .orElse(null);

        assertNotNull(defensive);
        assertTrue(defensive.isDefensive());
    }

    public void testConsumptionEmpty() {
        UnitType unit = new UnitType("test", spec());

        assertTrue(unit.getConsumedGoods().isEmpty());
    }

    public void testConsumptionOfUnknownGoods() {
        Specification spec = spec();
        UnitType unit = new UnitType("test", spec);

        GoodsType food = spec.getGoodsType("model.goods.food");
        GoodsType lumber = spec.getGoodsType("model.goods.lumber");

        TypeCountMap<GoodsType> map = new TypeCountMap<>();
        map.incrementCount(food, 2);

        setField(unit, "consumption", map);

        assertEquals(0, unit.getConsumptionOf(lumber));
    }

    public void testConsumptionOfNullGoods() {
        UnitType unit = new UnitType("test", spec());

        assertEquals(0, unit.getConsumptionOf(null));
    }

    public void testIsOffensive() {
        UnitType offensive = spec().getUnitTypeList().stream()
            .filter(ut -> ut.getBaseOffence() > UnitType.DEFAULT_OFFENCE)
            .findFirst()
            .orElse(null);

        assertNotNull(offensive);
        assertTrue(offensive.isOffensive());
    }

    public void testPersonUnit() {
        UnitType colonist = spec().getUnitType("model.unit.freeColonist");

        assertTrue(colonist.isPerson());
    }

    public void testCanBuildColony() {
        UnitType colonist = spec().getUnitType("model.unit.freeColonist");

        assertTrue(colonist.canBuildColony());
    }

    public void testSkillTaught() {
        UnitType teacher = spec().getUnitType("model.unit.masterCarpenter");

        assertNotNull(teacher.getSkillTaught());
    }

    public void testCanCarryTreasure() {
        UnitType treasureCarrier = spec().getUnitTypeList().stream()
            .filter(UnitType::canCarryTreasure)
            .findFirst()
            .orElse(null);

        assertNotNull(treasureCarrier);
        assertTrue(treasureCarrier.canCarryTreasure());
    }

    public void testFreeColonistIsNotOffensive() {
        UnitType colonist =
            spec().getUnitType("model.unit.freeColonist");

        assertFalse(colonist.isOffensive());
    }

    public void testFreeColonistIsNotDefensive() {
        UnitType colonist =
            spec().getUnitType("model.unit.freeColonist");

        assertFalse(colonist.isDefensive());
    }
}
