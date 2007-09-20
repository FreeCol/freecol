package net.sf.freecol.common.model;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.Specification;
import junit.framework.TestCase;

public class NationTypeTest extends TestCase {

	public void testHasType(){
		
		Specification s = FreeCol.getSpecification();
		
		// Make sure that each nation has a type
		for (Nation n : s.getNations()){
			assertNotNull(n.getName(), n.getType());
		}
	}
	
	public void testisRef() {

		Specification s = FreeCol.getSpecification();

		assertTrue(s.getNationType("model.nationType.trade").isEuropean());
		assertTrue(s.getNationType("model.nationType.default").isEuropean());
		assertTrue(s.getNationType("model.nationType.ref").isEuropean());
		assertTrue(s.getNationType("model.nationType.cooperation").isEuropean());
		assertTrue(s.getNationType("model.nationType.immigration").isEuropean());
		assertTrue(s.getNationType("model.nationType.conquest").isEuropean());

		assertFalse(s.getNationType("model.nationType.apache").isEuropean());
		assertFalse(s.getNationType("model.nationType.sioux").isEuropean());
		assertFalse(s.getNationType("model.nationType.tupi").isEuropean());
		assertFalse(s.getNationType("model.nationType.arawak").isEuropean());
		assertFalse(s.getNationType("model.nationType.cherokee").isEuropean());
		assertFalse(s.getNationType("model.nationType.iroquois").isEuropean());
		assertFalse(s.getNationType("model.nationType.inca").isEuropean());
		assertFalse(s.getNationType("model.nationType.aztec").isEuropean());
	}

	public void testIsREF() {
		Specification s = FreeCol.getSpecification();

		assertFalse(s.getNationType("model.nationType.trade").isREF());
		assertFalse(s.getNationType("model.nationType.default").isREF());
		assertTrue(s.getNationType("model.nationType.ref").isREF());
		assertFalse(s.getNationType("model.nationType.cooperation").isREF());
		assertFalse(s.getNationType("model.nationType.immigration").isREF());
		assertFalse(s.getNationType("model.nationType.conquest").isREF());

		assertFalse(s.getNationType("model.nationType.apache").isREF());
		assertFalse(s.getNationType("model.nationType.sioux").isREF());
		assertFalse(s.getNationType("model.nationType.tupi").isREF());
		assertFalse(s.getNationType("model.nationType.arawak").isREF());
		assertFalse(s.getNationType("model.nationType.cherokee").isREF());
		assertFalse(s.getNationType("model.nationType.iroquois").isREF());
		assertFalse(s.getNationType("model.nationType.inca").isREF());
		assertFalse(s.getNationType("model.nationType.aztec").isREF());
	}

}
