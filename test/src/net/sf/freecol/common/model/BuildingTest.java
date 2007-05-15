package net.sf.freecol.common.model;

import java.util.Iterator;

import net.sf.freecol.util.test.FreeColTestCase;

public class BuildingTest extends FreeColTestCase {

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";


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
    
    public void testCanAdd(){
        
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
        
        
        // Check school
        Building school = colony.getBuilding(Building.SCHOOLHOUSE);
        
        assertFalse(school.canAdd(colonist));
        assertFalse(school.canAdd(criminal));
        assertFalse(school.canAdd(servant));
        assertFalse(school.canAdd(indian));
        
        assertFalse(school.canAdd(farmer));
        assertFalse(school.canAdd(distiller));
        assertFalse(school.canAdd(elder));
        
        school.setLevel(Building.HOUSE);
        
        assertFalse(school.canAdd(colonist));
        assertFalse(school.canAdd(criminal));
        assertFalse(school.canAdd(servant));
        assertFalse(school.canAdd(indian));
        
        assertTrue(school.canAdd(farmer));
        assertFalse(school.canAdd(distiller));
        assertFalse(school.canAdd(elder));
        
        school.setLevel(Building.SHOP);
        
        assertFalse(school.canAdd(colonist));
        assertFalse(school.canAdd(criminal));
        assertFalse(school.canAdd(servant));
        assertFalse(school.canAdd(indian));
        
        assertTrue(school.canAdd(farmer));
        assertTrue(school.canAdd(distiller));
        assertFalse(school.canAdd(elder));

        school.setLevel(Building.FACTORY);
        
        assertFalse(school.canAdd(colonist));
        assertFalse(school.canAdd(criminal));
        assertFalse(school.canAdd(servant));
        assertFalse(school.canAdd(indian));
        
        assertTrue(school.canAdd(farmer));
        assertTrue(school.canAdd(distiller));
        assertTrue(school.canAdd(elder));
        
        // Check other buildings...
        
        
    }
    
}