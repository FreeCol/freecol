/**
 *  Copyright (C) 2002-2021  The FreeCol Team
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

import net.sf.freecol.common.util.CollectionUtils;

import net.sf.freecol.util.test.FreeColTestCase;


public class UtilsTest extends FreeColTestCase {

    private List<Integer> makeList(int... args) {
        List<Integer> l = new ArrayList<>();
        for (int a : args) l.add(a);
        return l;
    }

    public void testGetPermutations() {
        List<Integer> l = new ArrayList<>();
        l.add(1);
        l.add(2);
        l.add(3);
        List<List<Integer>> p = new ArrayList<>();
        for (List<Integer> li : CollectionUtils.getPermutations(l)) p.add(li);
        assertEquals(p.size(), 6);
        assertEquals(p.get(0), makeList(1,2,3));
        assertEquals(p.get(1), makeList(1,3,2));
        assertEquals(p.get(2), makeList(2,1,3));
        assertEquals(p.get(3), makeList(2,3,1));
        assertEquals(p.get(4), makeList(3,1,2));
        assertEquals(p.get(5), makeList(3,2,1));
    }

    public void testComparator() {
        // This is more to prove that I know what I am doing with some
        // trivial comparators than to actually test the code:-), MTP.
        List<Double> d = new ArrayList<>();
        d.add(1.0);
        d.add(2.0);
        d.add(3.0);
        d.sort(CollectionUtils.descendingDoubleComparator);
        assertEquals(d.get(0), 3.0);
        d.sort(CollectionUtils.ascendingDoubleComparator);
        assertEquals(d.get(0), 1.0);
        
        List<List<Object>> o = new ArrayList<>();
        List<Object> o1 = new ArrayList<Object>();
        List<Object> o2 = new ArrayList<Object>();
        List<Object> o3 = new ArrayList<Object>();
        o.add(o1);
        o.add(o2);
        o.add(o3);
        o1.add(1);
        o2.add(1);o2.add(2);
        o3.add(1);o3.add(2);o3.add(3);
        o.sort(CollectionUtils.descendingListLengthComparator);
        assertEquals(o.get(0), o3);
    }
}
