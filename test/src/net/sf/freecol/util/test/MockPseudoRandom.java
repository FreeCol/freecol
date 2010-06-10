package net.sf.freecol.util.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MockPseudoRandom extends Random {
    int pos;
    private List<Integer> setNumberList;
    private boolean cycleNumbers;
    private Random random;
    
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
    
    //@Override
    public int nextInt(int n) {
        if(pos < setNumberList.size()){
            int number = setNumberList.get(pos);
            if(number >= n){
                throw new IllegalArgumentException("Number in queue is bigger than " + n);
            }
            pos++;
            return number;
        }
        if(cycleNumbers && !setNumberList.isEmpty()){
            int number = setNumberList.get(0);
            pos = 1;
            if(number >= n){
                throw new IllegalArgumentException("Number in queue is bigger than " + n);
            }
            return number;
        }
        if(random == null){
            random = new Random(0);
        }
        return random.nextInt(n);
    }
}
