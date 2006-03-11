
package net.sf.freecol.common;


import junit.framework.TestCase;


public final class SpecificationTest extends TestCase
{

    public void testLoad() {

        try {
            new Specification();
        }
        catch ( Exception e ) {
            e.printStackTrace();
            fail();
        }
    }

}
