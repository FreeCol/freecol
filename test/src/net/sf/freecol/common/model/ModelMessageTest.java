package net.sf.freecol.common.model;

import java.util.HashSet;

import net.sf.freecol.FreeCol;
import net.sf.freecol.util.test.FreeColTestCase;

public class ModelMessageTest extends FreeColTestCase {

    public void testHashCode() {
    	
    	GoodsType cotton = FreeCol.getSpecification().getGoodsType("model.goods.Cotton");
    	
        ModelMessage mm1 = new ModelMessage(getGame(), "buildColony.landLocked", null, ModelMessage.MISSING_GOODS,
                cotton);
        ModelMessage mm2 = new ModelMessage(getGame(), "buildColony.landLocked", null, ModelMessage.MISSING_GOODS,
                cotton);
        assertEquals(mm1, mm2);
        assertEquals(mm1.hashCode(), mm2.hashCode());
        
        ModelMessage mm3 = new ModelMessage(getGame(), "buildColony.landLocked", null, ModelMessage.MISSING_GOODS,
            cotton);
        ModelMessage mm4 = new ModelMessage(getGame(), "buildColony.landLocked", null, ModelMessage.MISSING_GOODS,
            cotton);
        assertNotSame(mm3, mm4);
        assertNotSame(mm3.hashCode(), mm4.hashCode());
    }
    
    public void testModelMapSet2() {
    	
    	GoodsType cotton = FreeCol.getSpecification().getGoodsType("model.goods.Cotton");
    	
        ModelMessage mm1 = new ModelMessage(getGame(), "model.building.warehouseSoonFull",
                                    new String [][] {{"%goods%", cotton.getName()},
                                                     {"%colony%", getName()},
                                                     {"%amount%", String.valueOf(10)}},
                                    ModelMessage.WAREHOUSE_CAPACITY,
                                    cotton);
        ModelMessage mm2 = new ModelMessage(getGame(), "model.building.warehouseSoonFull",
                new String [][] {{"%goods%", cotton.getName()},
                                 {"%colony%", getName()},
                                 {"%amount%", String.valueOf(10)}},
                ModelMessage.WAREHOUSE_CAPACITY,
                cotton);
        HashSet<ModelMessage> set = new HashSet<ModelMessage>();
        assertEquals(mm1, mm2);
        assertEquals(mm1.hashCode(), mm2.hashCode());
        set.add(mm1);
        assertTrue(set.remove(mm2));
    }
    
}
