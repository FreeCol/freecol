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

    public void testCombatAttributes() {
        Specification spec = spec();
        UnitType colonist = spec.getUnitType("model.unit.freeColonist");
        
        assertTrue(colonist.getOffence() >= 0);
        assertTrue(colonist.getDefence() >= 1);
        
        // Find any unit that is actually offensive in this spec
        UnitType offensiveUnit = spec.getUnitTypeList().stream()
            .filter(ut -> ut.getBaseOffence() > 0)
            .findFirst()
            .orElse(null);

        if (offensiveUnit != null) {
            assertTrue("Unit with baseOffence > 0 must be offensive", 
                offensiveUnit.isOffensive());
        }
    }

    public void testCapacityLogic() {
        Specification spec = spec();
        UnitType merchantman = spec.getUnitType("model.unit.merchantman");
        
        if (merchantman != null) {
            assertTrue(merchantman.isNaval());
            assertTrue(merchantman.canCarryGoods());
            assertTrue(merchantman.getSpace() > 0);
            assertTrue(merchantman.getSpaceTaken() > merchantman.getSpace());
        }
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

    public void testEducationLogic() {
        Specification spec = spec();
        UnitType teacher = spec.getUnitType("model.unit.masterCarpenter");
        UnitType student = spec.getUnitType("model.unit.freeColonist");
        
        if (teacher != null && student != null) {
            assertNotNull(teacher.getSkillTaught());
            UnitType taught = student.getTeachingType(teacher);
            assertNotNull(taught);
        }
    }

    public void testAbilitiesArePresent() {
        Specification spec = spec();
        UnitType unit = spec.getUnitType("model.unit.freeColonist");

        assertTrue("Colonist should have at least one ability",
            unit.getAbilities().findAny().isPresent());
    }

    public void testModifiersArePresent() {
        Specification spec = spec();
        UnitType unit = spec.getUnitType("model.unit.freeColonist");

        assertTrue("Colonist should have at least one modifier",
            unit.getModifiers().findAny().isPresent());
    }

    public void testDefaultRoleIsLoaded() {
        Specification spec = spec();
        UnitType unit = spec.getUnitType("model.unit.freeColonist");

        assertNotNull("Default role must not be null", unit.getDefaultRole());
    }

    public void testUnitChangesArePresent() {
        Specification spec = spec();
        boolean hasEducation = spec.getUnitTypeList().stream()
            .anyMatch(ut -> !spec.getUnitChanges(UnitChangeType.EDUCATION, ut).isEmpty());

        assertTrue("The specification should contain education unit changes", 
            hasEducation);
    }

    public void testCopyInCopiesBasicFields() {
        Specification spec = spec();
        final String typeId = "model.unit.testCopy";
        UnitType a = new UnitType(typeId, spec);
        UnitType b = new UnitType(typeId, spec);

        setField(a, "baseOffence", 10);
        setField(a, "movement", 15);
        setField(a, "hitPoints", 100);

        assertTrue(b.copyIn(a));
        assertEquals(10, b.getBaseOffence());
        assertEquals(15, b.getMovement());
        assertEquals(100, b.getHitPoints());
    }

    public void testCopyInCopiesDefaultRole() {
        Specification spec = spec();
        final String typeId = "model.unit.testCopyRole";
        UnitType a = new UnitType(typeId, spec);
        UnitType b = new UnitType(typeId, spec);

        Role role = spec.getRole("model.role.soldier");
        setField(a, "defaultRole", role);

        assertTrue(b.copyIn(a));
        assertEquals(role, b.getDefaultRole());
    }

    public void testCopyInCopiesConsumptionReference() {
        Specification spec = spec();
        final String typeId = "model.unit.testCopyConsumption";
        UnitType a = new UnitType(typeId, spec);
        UnitType b = new UnitType(typeId, spec);

        GoodsType food = spec.getGoodsType("model.goods.food");
        TypeCountMap<GoodsType> map = new TypeCountMap<>();
        map.incrementCount(food, 3);
        setField(a, "consumption", map);

        assertTrue(b.copyIn(a));
        assertEquals(3, b.getConsumptionOf(food));
    }

    public void testCopyIn() {
        Specification spec = spec();
        final String typeId = "model.unit.testCopy";
        UnitType a = new UnitType(typeId, spec);
        UnitType b = new UnitType(typeId, spec);
        
        setField(a, "baseOffence", 10);
        setField(a, "movement", 15);
        setField(a, "hitPoints", 100);
        
        boolean result = b.copyIn(a);
        
        assertTrue("copyIn should return true when IDs match", result);
        assertEquals("baseOffence should be copied", 10, b.getBaseOffence());
        assertEquals("movement should be copied", 15, b.getMovement());
        assertEquals("hitPoints should be copied", 100, b.getHitPoints());
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
}
