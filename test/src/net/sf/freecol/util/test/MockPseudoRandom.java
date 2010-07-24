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

    public MockPseudoRandom(){
        this(new ArrayList<Integer>(),false);
    }
    
    public MockPseudoRandom(List<Integer> setNumbers,boolean toCycle){
        pos = 0;
        setNumberList = setNumbers;
        cycleNumbers = toCycle;
        random = null;
    }
    
    public void setNextNumbers(List<Integer> setNumbers,boolean toCycle){
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

    public int nextInt(int n) {
        int number = getNext();
        if (number < 0) return random.nextInt(n);
        if (number >= n) {
            throw new IllegalArgumentException("Number in queue is bigger than " + n);
        }
        return number;
    }

    public float nextFloat() {
        int number = getNext();
        return (number < 0) ? random.nextFloat() : number * scale;
    }
}
