package net.sf.freecol.common.model;

import junit.framework.TestCase;

public class PlayerTest extends TestCase {
    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public void testIsEuropean() {
        assertEquals(true, Player.isEuropean(Player.DUTCH));
        assertEquals(true, Player.isEuropean(Player.ENGLISH));
        assertEquals(true, Player.isEuropean(Player.FRENCH));
        assertEquals(true, Player.isEuropean(Player.SPANISH));
        assertEquals(false, Player.isEuropean(Player.INCA));
        assertEquals(false, Player.isEuropean(Player.AZTEC));
        assertEquals(false, Player.isEuropean(Player.ARAWAK));
        assertEquals(false, Player.isEuropean(Player.CHEROKEE));
        assertEquals(false, Player.isEuropean(Player.IROQUOIS));
        assertEquals(false, Player.isEuropean(Player.SIOUX));
        assertEquals(false, Player.isEuropean(Player.APACHE));
        assertEquals(false, Player.isEuropean(Player.TUPI));
        assertEquals(true, Player.isEuropean(Player.REF_DUTCH));
        assertEquals(true, Player.isEuropean(Player.REF_ENGLISH));
        assertEquals(true, Player.isEuropean(Player.REF_FRENCH));
        assertEquals(true, Player.isEuropean(Player.REF_SPANISH));
    }

    public void testIsEuropeanNoREF() {
        assertEquals(true, Player.isEuropeanNoREF(Player.DUTCH));
        assertEquals(true, Player.isEuropeanNoREF(Player.ENGLISH));
        assertEquals(true, Player.isEuropeanNoREF(Player.FRENCH));
        assertEquals(true, Player.isEuropeanNoREF(Player.SPANISH));
        assertEquals(false, Player.isEuropeanNoREF(Player.INCA));
        assertEquals(false, Player.isEuropeanNoREF(Player.AZTEC));
        assertEquals(false, Player.isEuropeanNoREF(Player.ARAWAK));
        assertEquals(false, Player.isEuropeanNoREF(Player.CHEROKEE));
        assertEquals(false, Player.isEuropeanNoREF(Player.IROQUOIS));
        assertEquals(false, Player.isEuropeanNoREF(Player.SIOUX));
        assertEquals(false, Player.isEuropeanNoREF(Player.APACHE));
        assertEquals(false, Player.isEuropeanNoREF(Player.TUPI));
        assertEquals(false, Player.isEuropeanNoREF(Player.REF_DUTCH));
        assertEquals(false, Player.isEuropeanNoREF(Player.REF_ENGLISH));
        assertEquals(false, Player.isEuropeanNoREF(Player.REF_FRENCH));
        assertEquals(false, Player.isEuropeanNoREF(Player.REF_SPANISH));
    }
    
    public void testIsREF() {
        assertEquals(false, Player.isREF(Player.DUTCH));
        assertEquals(false, Player.isREF(Player.ENGLISH));
        assertEquals(false, Player.isREF(Player.FRENCH));
        assertEquals(false, Player.isREF(Player.SPANISH));
        assertEquals(false, Player.isREF(Player.INCA));
        assertEquals(false, Player.isREF(Player.AZTEC));
        assertEquals(false, Player.isREF(Player.ARAWAK));
        assertEquals(false, Player.isREF(Player.CHEROKEE));
        assertEquals(false, Player.isREF(Player.IROQUOIS));
        assertEquals(false, Player.isREF(Player.SIOUX));
        assertEquals(false, Player.isREF(Player.APACHE));
        assertEquals(false, Player.isREF(Player.TUPI));
        assertEquals(true, Player.isREF(Player.REF_DUTCH));
        assertEquals(true, Player.isREF(Player.REF_ENGLISH));
        assertEquals(true, Player.isREF(Player.REF_FRENCH));
        assertEquals(true, Player.isREF(Player.REF_SPANISH));
    }
}
