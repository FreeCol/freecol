/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

package net.sf.freecol.util.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MockPseudoRandom extends Random {
    int pos;
    private List<Integer> setNumberList;
    private boolean cycleNumbers;
    private Random random;
    private final float scale = 1.0f / (float) Integer.MAX_VALUE;

    public MockPseudoRandom() {
        this(new ArrayList<Integer>(), false);
    }
    
    public MockPseudoRandom(List<Integer> setNumbers, boolean toCycle) {
        pos = 0;
        setNumberList = setNumbers;
        cycleNumbers = toCycle;
        random = null;
    }
    
    public void setNextNumbers(List<Integer> setNumbers, boolean toCycle) {
        pos = 0;
        setNumberList = setNumbers;
        cycleNumbers = toCycle;
    }
    
    private int getNext() {
        if (pos < setNumberList.size()) {
            int number = setNumberList.get(pos);
            pos++;
            return number;
        }
        if (cycleNumbers && !setNumberList.isEmpty()) {
            int number = setNumberList.get(0);
            pos = 1;
            return number;
        }
        if (random == null) {
            random = new Random(0);
        }
        return -1;
    }

    @Override
    public int nextInt(int n) {
        int number = getNext();
        if (number < 0 || number >= n) {
            System.err.println("MockPseudoRandom out of range: " + number);
            return random.nextInt(n);
        }
        return number;
    }

    @Override
    public float nextFloat() {
        int number = getNext();
        return (number < 0) ? random.nextFloat() : number * scale;
    }

    @Override
    public double nextDouble() {
        int number = getNext();
        return (number < 0) ? random.nextDouble() : number * scale;
    }
}
