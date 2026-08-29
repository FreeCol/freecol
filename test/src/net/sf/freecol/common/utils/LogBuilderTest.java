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

package net.sf.freecol.common.utils;

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.util.test.FreeColTestCase;

public class LogBuilderTest extends FreeColTestCase {

    public void testAdd() {
        LogBuilder lb = new LogBuilder(64);
        lb.add("Test ", 1, " ", null);
        assertEquals("Test 1 null", lb.toString());
        
        lb = new LogBuilder(64);
        Object[] arr = new Object[] {"A", "B"};
        lb.add(arr);
        assertEquals("AB", lb.toString());
    }

    public void testMarkAndGrew() {
        LogBuilder lb = new LogBuilder(64);
        lb.add("Initial");
        lb.mark();
        lb.add("Data");
        
        assertTrue(lb.grew(": "));
        assertEquals("Initial: Data", lb.toString());
        
        lb.mark();
        assertFalse(lb.grew("ShouldNotAppear"));
        assertEquals("Initial: Data", lb.toString());
    }

    public void testWide() {
        assertEquals("ABC       ", LogBuilder.wide(10, "ABC"));
        assertEquals("       ABC", LogBuilder.wide(-10, "ABC"));
        assertEquals("AB", LogBuilder.wide(2, "ABC"));
    }

    public void testIterator() {
        LogBuilder lb = new LogBuilder(64);
        lb.add("Line1\nLine2\nLine3");
        
        List<String> lines = new ArrayList<>();
        for (String s : lb) {
            lines.add(s);
        }
        
        assertEquals(3, lines.size());
        assertEquals("Line1", lines.get(0));
        assertEquals("Line2", lines.get(1));
        assertEquals("Line3", lines.get(2));
    }

    public void testAddCollection() {
        LogBuilder lb = new LogBuilder(64);
        List<String> items = new ArrayList<>();
        items.add("One");
        items.add("Two");
        
        lb.addCollection(", ", items);
        assertEquals("One, Two", lb.toString());
    }

    public void testAddDoesNotFlattenCollections() {
        LogBuilder lb = new LogBuilder(64);
        List<String> list = new ArrayList<>();
        list.add("A");
        list.add("B");

        lb.add(list);
        assertEquals("[A, B]", lb.toString());
    }

    public void testAddCollectionHandlesNulls() {
        LogBuilder lb = new LogBuilder(64);
        List<String> items = new ArrayList<>();
        items.add("One");
        items.add(null);
        items.add("Three");

        lb.addCollection(", ", items);
        assertEquals("One, null, Three", lb.toString());
    }

    public void testMarkStackOrder() {
        LogBuilder lb = new LogBuilder(64);
        lb.add("A");
        lb.mark();
        lb.add("B");
        lb.mark();
        lb.add("C");

        lb.grew("X");
        lb.grew("Y");

        assertEquals("AYBXC", lb.toString());
    }

    public void testShrinkOnlyRemovesTrailingDelimiter() {
        LogBuilder lb = new LogBuilder(64);
        lb.add("A, B, C, ");
        lb.shrink(", ");

        assertEquals("A, B, C", lb.toString());
    }
}
