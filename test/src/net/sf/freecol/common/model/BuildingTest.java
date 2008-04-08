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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.util.test.FreeColTestCase;

public class BuildingTest extends FreeColTestCase {

    public void testCanBuildNext() {
    	
    	Colony colony = getStandardColony();

        // First check with a building that can be fully built with a
        // normal colony
        BuildingType warehouseType = FreeCol.getSpecification().getBuildingType("model.building.Depot");
        Building warehouse = new Building(getGame(), colony, warehouseType);
        colony.addBuilding(warehouse);
        assertTrue(warehouse.canBuildNext());
        warehouse.upgrade();
        assertTrue(warehouse.canBuildNext());
        warehouse.upgrade();
        assertFalse(warehouse.canBuildNext());
        
        try {
        	warehouse.upgrade();
        	fail();
        } catch (IllegalStateException e){
        	// Should throw exception
        }
        assertFalse(warehouse.canBuildNext());

        // Check whether population restrictions work

        // Colony smallColony = getStandardColony(1);
        // 
        // Colony largeColony = getStandardColony(6);
        // ...

        // Check whether founding fathers work

    }

    public void testInitialColony() {

        Colony colony = getStandardColony();

        BuildingType warehouseType = FreeCol.getSpecification().getBuildingType("model.building.Warehouse");
        Building warehouse = colony.getBuilding(warehouseType);

        // Is build as depot...
        assertTrue(warehouse != null);
        
        assertTrue(warehouse.canBuildNext());

        // Check other building...

        // Check dock -> only possible if not landlocked...

    }

    public void testChurch() {
        Colony colony = getStandardColony(6);
        BuildingType churchType = FreeCol.getSpecification().getBuildingType("model.building.Chapel");
        assertFalse(churchType.hasAbility("model.ability.dressMissionary"));

        Building church = colony.getBuilding(churchType);
        assertTrue(church != null);
        assertFalse(colony.hasAbility("model.ability.dressMissionary"));

        church.upgrade();
        assertTrue(church.getType().hasAbility("model.ability.dressMissionary"));
        assertTrue(colony.hasAbility("model.ability.dressMissionary"));
        
    }

    public void testCanAddToBuilding() {
    	
        Colony colony = getStandardColony(6);
        List<Unit> units = colony.getUnitList();

        for (Building building : colony.getBuildings()) {
            
            // schoolhouse is special, see testCanAddToSchool
            if (building.getType().hasAbility("model.ability.teach")) 
            	continue;
            
            int maxUnits = building.getMaxUnits();
            
            assertEquals(0, building.getUnitCount());
            
            for (int index = 0; index < maxUnits; index++) {
                assertTrue("unable to add unit " + index + " to building type " +
                           building.getType().getName(), building.canAdd(units.get(index)));
                building.add(units.get(index));
            }
            assertFalse("able to add unit " + maxUnits + " to building type " +
                        building.getType(),
                        building.canAdd(units.get(maxUnits)));
            for (int index = 0; index < maxUnits; index++) {
                building.remove(building.getFirstUnit());
            }
        }
    }
    
    
    /**
     * WARNING! This test makes implicit assumptions about the
     * schoolhouse that could be invalidated by the
     * specification. 
     * 
     * TODO: make this more generic.
     */
    public void testCanAddToSchool(){

        UnitType freeColonistType = spec().getUnitType("model.unit.freeColonist");
        UnitType indenturedServantType = spec().getUnitType("model.unit.indenturedServant");
        UnitType pettyCriminalType = spec().getUnitType("model.unit.pettyCriminal");
        UnitType expertFarmerType = spec().getUnitType("model.unit.expertFarmer");
        UnitType masterCarpenterType = spec().getUnitType("model.unit.masterCarpenter");
        UnitType masterDistillerType = spec().getUnitType("model.unit.masterDistiller");
        UnitType elderStatesmanType = spec().getUnitType("model.unit.elderStatesman");
        UnitType indianConvertType = spec().getUnitType("model.unit.indianConvert");

        Colony colony = getStandardColony(10);
        
        Iterator<Unit> units = colony.getUnitIterator();
        
        Unit farmer = units.next();
        farmer.setType(expertFarmerType);
        
        Unit colonist = units.next();
        colonist.setType(freeColonistType);
        
        Unit criminal = units.next();
        criminal.setType(pettyCriminalType);
        
        Unit servant = units.next();
        servant.setType(indenturedServantType);
        
        Unit indian = units.next();
        indian.setType(indianConvertType);
        
        Unit distiller = units.next();
        distiller.setType(masterDistillerType);
        
        Unit elder = units.next();
        elder.setType(elderStatesmanType);

        Unit carpenter = units.next();
        carpenter.setType(masterCarpenterType);
        
        // Check school
        BuildingType schoolType = FreeCol.getSpecification().getBuildingType("model.building.Schoolhouse");
        Building school = colony.getBuilding(schoolType);
        assertTrue(school == null);

        // build school
        colony.addBuilding(new Building(getGame(), colony, schoolType));
        school = colony.getBuilding(schoolType);
        assertTrue(school != null);

        // these can never teach
        assertFalse("able to add free colonist to Schoolhouse",
                    school.canAdd(colonist));
        assertFalse("able to add petty criminal to Schoolhouse",
                    school.canAdd(criminal));
        assertFalse("able to add indentured servant to Schoolhouse",
                    school.canAdd(servant));
        assertFalse("able to add indian convert to Schoolhouse",
                    school.canAdd(indian));
        
        assertFalse("able to add elder statesman to Schoolhouse",
                    school.canAdd(elder));
        assertFalse("able to add master distiller to Schoolhouse",
                    school.canAdd(distiller));
        assertTrue("unable to add master farmer to Schoolhouse",
                   school.canAdd(farmer));
        school.add(farmer);
        assertFalse("able to add master carpenter to Schoolhouse",
                    school.canAdd(carpenter));
        school.remove(farmer);

        school.upgrade();
        // these can never teach
        assertFalse("able to add free colonist to College",
                    school.canAdd(colonist));
        assertFalse("able to add petty criminal to College",
                    school.canAdd(criminal));
        assertFalse("able to add indentured servant to College",
                    school.canAdd(servant));
        assertFalse("able to add indian convert to College",
                    school.canAdd(indian));
        
        assertFalse("able to add elder statesman to College",
                    school.canAdd(elder));
        assertTrue("unable to add master distiller to College",
                   school.canAdd(distiller));
        school.add(distiller);
        assertTrue("unable to add master farmer to College",
                   school.canAdd(farmer));
        school.add(farmer);
        assertFalse("able to add master carpenter to College",
                    school.canAdd(carpenter));
        school.remove(distiller);
        school.remove(farmer);

        school.upgrade();
        
        assertEquals(school.getType().getName(), school.getType(), spec().getBuildingType("model.building.University"));
        
        // these can never teach
        assertFalse("able to add free colonist to University",
                    school.canAdd(colonist));
        assertFalse("able to add petty criminal to University",
                    school.canAdd(criminal));
        assertFalse("able to add indentured servant to University",
                    school.canAdd(servant));
        assertFalse("able to add indian convert to University",
                    school.canAdd(indian));
        
        assertTrue("unable to add elder statesman to University",
                   school.canAdd(elder));
        school.add(elder);
        assertTrue("unable to add master distiller to University",
                   school.canAdd(distiller));
        school.add(distiller);
        assertTrue("unable to add master farmer to University",
                   school.canAdd(farmer));
        school.add(farmer);
        assertFalse("able to add master carpenter to University",
                    school.canAdd(carpenter));
        school.remove(elder);
        school.remove(distiller);
        school.remove(farmer);

    }

    public void testSerialize() {

        Colony colony = getStandardColony(6);
        List<Unit> units = colony.getUnitList();

        for (Building building : colony.getBuildings()) {
            try {
                StringWriter sw = new StringWriter();
                XMLOutputFactory xif = XMLOutputFactory.newInstance();
                XMLStreamWriter xsw = xif.createXMLStreamWriter(sw);
                building.toXML(xsw, building.getColony().getOwner(), true, true);
                xsw.close();
            } catch (XMLStreamException e) {
            }
        }
    }

    public void testStockade() {
        
        BuildingType stockade = spec().getBuildingType("model.building.Stockade");
        assertEquals(100, stockade.getDefenceBonus());
        
        BuildingType fort = spec().getBuildingType("model.building.Fort");
        assertEquals(150, fort.getDefenceBonus());
        
        BuildingType fortress = spec().getBuildingType("model.building.Fortress");
        assertEquals(200, fortress.getDefenceBonus());
    }

}
