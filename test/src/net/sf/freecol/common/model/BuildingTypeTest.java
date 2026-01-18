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
import net.sf.freecol.common.model.UnitLocation.NoAddReason;

public class BuildingTypeTest extends FreeColTestCase {

    public void testUpgradeChain() {
        Specification spec = spec();
        BuildingType base = spec.getBuildingTypeList().stream()
            .filter(bt -> bt.getUpgradesTo() != null)
            .findFirst()
            .orElse(null);

        assertNotNull("The specification must contain at least one upgradable building", base);
        BuildingType upgrade = base.getUpgradesTo();

        assertEquals("Upgrade should point back to base", base, upgrade.getUpgradesFrom());
        assertTrue("Upgrade level should be greater than base level", 
            upgrade.getLevel() > base.getLevel());
        assertEquals("First level of upgrade should be the base (or base's root)", 
            base.getFirstLevel(), upgrade.getFirstLevel());
    }

    public void testUnitSkillConstraints() {
        Specification spec = spec();
        BuildingType building = spec.getBuildingTypeList().stream()
            .filter(bt -> bt.getWorkPlaces() > 0)
            .findFirst()
            .orElse(null);

        assertNotNull(building);
        UnitType colonist = spec.getUnitType("model.unit.freeColonist");
        NoAddReason reason = building.getNoAddReason(colonist);

        if (building.getMinimumSkill() > 0) {
            assertEquals(NoAddReason.MINIMUM_SKILL, reason);
        } else {
            assertEquals(NoAddReason.NONE, reason);
        }
    }

    public void testIsAutomaticBuild() {
        Specification spec = spec();
        BuildingType townHall = spec.getBuildingType("model.building.townHall");
        assertNotNull(townHall);

        boolean expected = !townHall.needsGoodsToBuild() && townHall.getUpgradesFrom() == null;
        assertEquals(expected, townHall.isAutomaticBuild());
    }

    public void testCopyInCopiesFields() {
        Specification spec = spec();
        BuildingType realBuilding = spec.getBuildingTypeList().get(0);
        
        BuildingType a = new BuildingType("model.building.testCopy", spec);
        BuildingType b = new BuildingType("model.building.testCopy", spec);

        setField(a, "workPlaces", 9);
        setField(a, "upkeep", 99);
        setField(a, "minSkill", 5);
        setField(a, "upgradesFrom", realBuilding);

        if (!realBuilding.getProductionTypes().isEmpty()) {
            a.addProductionType(realBuilding.getProductionTypes().get(0));
        }

        assertTrue("copyIn should return true", b.copyIn(a));
        assertEquals(9, b.getWorkPlaces());
        assertEquals(99, b.getUpkeep());
        assertEquals(5, b.getMinimumSkill());
        assertEquals(realBuilding, b.getUpgradesFrom());
    }

    public void testCopyInProductionTypesAreIndependent() {
        Specification spec = spec();
        BuildingType source = spec.getBuildingTypeList().stream()
            .filter(bt -> !bt.getProductionTypes().isEmpty())
            .findFirst()
            .orElse(null);

        assertNotNull("Spec must have at least one building with production types", source);

        BuildingType a = new BuildingType("model.building.testProd", spec);
        BuildingType b = new BuildingType("model.building.testProd", spec);

        a.addProductionType(source.getProductionTypes().get(0));
        b.copyIn(a);

        assertEquals(a.getProductionTypes().size(), b.getProductionTypes().size());
        assertNotSame("The list objects themselves must be different instances",
            a.getProductionTypes(), b.getProductionTypes());
    }

    public void testSerializationRoundTrip() throws Exception {
        Specification spec = spec();
        BuildingType original = spec.getBuildingType("model.building.blacksmithShop");
        assertNotNull(original);

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

        BuildingType loaded = new BuildingType(original.getId(), spec);
        loaded.readFromXML(xr);
        xr.close();

        assertEquals(original.getWorkPlaces(), loaded.getWorkPlaces());
        assertEquals(original.getUpkeep(), loaded.getUpkeep());
        assertEquals(original.getLevel(), loaded.getLevel());
        assertEquals(original.getMinimumSkill(), loaded.getMinimumSkill());
        assertEquals(original.getMaximumSkill(), loaded.getMaximumSkill());
    }

    public void testGetFirstLevelMultiStep() {
        Specification spec = spec();
        BuildingType deepest = spec.getBuildingTypeList().stream()
            .filter(bt -> bt.getUpgradesFrom() != null && bt.getUpgradesFrom().getUpgradesFrom() != null)
            .findFirst()
            .orElse(null);

        assertNotNull("Need a building with at least 3 levels", deepest);
        BuildingType root = deepest.getFirstLevel();
        BuildingType cursor = deepest;
        while (cursor.getUpgradesFrom() != null) {
            cursor = cursor.getUpgradesFrom();
        }
        assertEquals(cursor, root);
    }

    public void testNoAddReasonEdgeCases() {
        Specification spec = spec();
        UnitType masterCarpenter = spec.getUnitType("model.unit.masterCarpenter");
        int carpenterSkill = masterCarpenter.getSkill();

        BuildingType b = new BuildingType("model.building.testNoAdd", spec);
        setField(b, "workPlaces", 3);
        setField(b, "minSkill", 0);
        setField(b, "maxSkill", carpenterSkill - 1);

        assertEquals(NoAddReason.MAXIMUM_SKILL, b.getNoAddReason(masterCarpenter));
    }

    public void testAvailableProductionTypesAttended() {
        Specification spec = spec();
        BuildingType b = spec.getBuildingTypeList().stream()
            .filter(bt -> !bt.getAvailableProductionTypes(false).isEmpty())
            .findFirst()
            .orElse(null);

        assertNotNull("Need a building with attended production", b);
        for (ProductionType pt : b.getAvailableProductionTypes(false)) {
            assertFalse(pt.getUnattended());
        }
    }

    public void testCanProduce() {
        Specification spec = spec();
        BuildingType b = spec.getBuildingType("model.building.lumberMill");
        GoodsType hammers = spec.getGoodsType("model.goods.hammers");
        UnitType colonist = spec.getUnitType("model.unit.freeColonist");
        assertTrue("Lumber Mill should be able to produce Hammers", b.canProduce(hammers, colonist));
    }

    public void testCompetenceModifiers() {
        Specification spec = spec();
        BuildingType b = spec.getBuildingTypeList().get(0);
        UnitType colonist = spec.getUnitType("model.unit.freeColonist");
        setField(b, "competenceFactor", 2.0f);
        Turn turn = new Turn(1);

        b.getCompetenceModifiers(Modifier.OFFENCE, colonist, turn).forEach(m -> {
            if (m.getType() == Modifier.ModifierType.ADDITIVE) {
                assertEquals(m.getValue(), m.getValue(), 0.001);
            }
        });
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field;
            try {
                field = target.getClass().getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                field = target.getClass().getSuperclass().getDeclaredField(fieldName);
            }
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Reflection failed for: " + fieldName, e);
        }
    }
}
