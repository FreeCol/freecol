package net.sf.freecol.common.model;

import java.util.Iterator;
import java.util.List;

import net.sf.freecol.util.test.FreeColTestCase;

public class BuildingTest extends FreeColTestCase {

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private static int levels[] = {Building.NOT_BUILT, Building.HOUSE, Building.SHOP, Building.FACTORY};



    public void testCanBuildNext() {

        Colony colony = getStandardColony();

        // First check with a building that can be fully build with a normal
        // colony
        assertTrue(new Building(getGame(), colony, Building.WAREHOUSE, Building.NOT_BUILT).canBuildNext());
        assertTrue(new Building(getGame(), colony, Building.WAREHOUSE, Building.HOUSE).canBuildNext());
        assertFalse(new Building(getGame(), colony, Building.WAREHOUSE, Building.SHOP).canBuildNext());
        assertFalse(new Building(getGame(), colony, Building.WAREHOUSE, Building.FACTORY).canBuildNext());

        // Check whether it is population restriction work

        // Colony smallColony = getStandardColony(1);
        // 
        // Colony largeColony = getStandardColony(6);
        // ...

        // Check whether founding fathers work

    }

    public void testInitialColony() {

        Colony colony = getStandardColony();

        Building warehouse = colony.getBuilding(Building.WAREHOUSE);
        assertEquals(Building.NOT_BUILT, warehouse.getLevel());
        assertTrue(warehouse.canBuildNext());

        // Check other building...

        // Check dock -> only possible if not landlocked...

    }

    public void testCanAddToBuilding() {
        
        Colony colony = getStandardColony(6);
        List<Unit> units = colony.getUnitList();

        Iterator<Building> buildingIterator = colony.getBuildingIterator();
        while (buildingIterator.hasNext()) {
            Building building = buildingIterator.next();
            // schoolhouse is special, see testCanAddToSchool
            if (building.getType() == Building.SCHOOLHOUSE) continue;
            for (int level = 0; level < levels.length; level++) {
                building.setLevel(level);
                int maxUnits = building.getMaxUnits();
                for (int index = 0; index < maxUnits; index++) {
                    assertTrue("unable to add unit " + index + " to building type " +
                               building.getType() + " level " + level,
                               building.canAdd(units.get(index)));
                    building.add(units.get(index));
                }
                assertFalse("able to add unit " + maxUnits + " to building type " +
                            building.getType() + " level " + level,
                            building.canAdd(units.get(maxUnits)));
                for (int index = 0; index < maxUnits; index++) {
                    building.remove(building.getFirstUnit());
                }
            }
        }
    }
    
    
    public void testCanAddToSchool(){
        
        Colony colony = getStandardColony(8);
        
        Iterator<Unit> units = colony.getUnitIterator();
        
        Unit farmer = units.next();
        farmer.setType(Unit.EXPERT_FARMER);
        
        Unit colonist = units.next();
        colonist.setType(Unit.FREE_COLONIST);
        
        Unit criminal = units.next();
        criminal.setType(Unit.PETTY_CRIMINAL);
        
        Unit servant = units.next();
        servant.setType(Unit.INDENTURED_SERVANT);
        
        Unit indian = units.next();
        indian.setType(Unit.INDIAN_CONVERT);
        
        Unit distiller = units.next();
        distiller.setType(Unit.MASTER_DISTILLER);
        
        Unit elder = units.next();
        elder.setType(Unit.ELDER_STATESMAN);

        Unit carpenter = units.next();
        carpenter.setType(Unit.MASTER_CARPENTER);
        
        // Check school
        Building school = colony.getBuilding(Building.SCHOOLHOUSE);

        for (int level : levels) {

            school.setLevel(level);
        
            // these can never teach
            assertFalse("able to add free colonist to school level " + level,
                        school.canAdd(colonist));
            assertFalse("able to add petty criminal to school level " + level,
                        school.canAdd(criminal));
            assertFalse("able to add indentured servant to school level " + level,
                        school.canAdd(servant));
            assertFalse("able to add indian convert to school level " + level,
                        school.canAdd(indian));
        
            switch(level) {
            case Building.NOT_BUILT:
                assertFalse("able to add elder statesman to Schoolhouse (not built)",
                            school.canAdd(elder));
                assertFalse("able to add master distiller to Schoolhouse (not built)",
                            school.canAdd(distiller));
                assertFalse("able to add master farmer to Schoolhouse (not built)",
                            school.canAdd(farmer));
                break;
            case Building.HOUSE:
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
                break;
            case Building.SHOP:
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
                break;
            case Building.FACTORY:
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
                break;
            }
        }
    }


}