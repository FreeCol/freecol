package net.sf.freecol.common.model;

import java.util.HashSet;

import net.sf.freecol.util.test.FreeColTestCase;

public class ModelMessageTest extends FreeColTestCase {

    public void testHashCode() {
        
        ModelMessage mm1 = new ModelMessage(getGame(), "buildColony.landLocked", null, ModelMessage.MISSING_GOODS,
                new Goods(Goods.FISH));
        ModelMessage mm2 = new ModelMessage(getGame(), "buildColony.landLocked", null, ModelMessage.MISSING_GOODS,
                new Goods(Goods.FISH));
        assertEquals(mm1, mm2);
        assertEquals(mm1.hashCode(), mm2.hashCode());
    }
    
    public void testModelMapSet() {
        ModelMessage mm1 = new ModelMessage(getGame(), "buildColony.landLocked", null, ModelMessage.MISSING_GOODS,
                new Goods(Goods.FISH));
        ModelMessage mm2 = new ModelMessage(getGame(), "buildColony.landLocked", null, ModelMessage.MISSING_GOODS,
                new Goods(Goods.FISH));
        HashSet<ModelMessage> set = new HashSet<ModelMessage>();
        set.add(mm1);
        assertTrue(set.remove(mm2));
    }
    
    public void testModelMapSet2() {
        ModelMessage mm1 = new ModelMessage(getGame(), "model.building.warehouseSoonFull",
                                    new String [][] {{"%goods%", Goods.getName(Goods.COTTON)},
                                                     {"%colony%", getName()},
                                                     {"%amount%", String.valueOf(10)}},
                                    ModelMessage.WAREHOUSE_CAPACITY,
                                    new Goods(Goods.COTTON));
        ModelMessage mm2 = new ModelMessage(getGame(), "model.building.warehouseSoonFull",
                new String [][] {{"%goods%", Goods.getName(Goods.COTTON)},
                                 {"%colony%", getName()},
                                 {"%amount%", String.valueOf(10)}},
                ModelMessage.WAREHOUSE_CAPACITY,
                new Goods(Goods.COTTON));
        HashSet<ModelMessage> set = new HashSet<ModelMessage>();
        assertEquals(mm1, mm2);
        assertEquals(mm1.hashCode(), mm2.hashCode());
        set.add(mm1);
        assertTrue(set.remove(mm2));
    }
    
}
