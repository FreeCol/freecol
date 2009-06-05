package net.sf.freecol.util.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.sf.freecol.common.PseudoRandom;

public class MockPseudoRandom implements PseudoRandom {
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
    
    @Override
    public int nextInt(int n) {
        if(pos < setNumberList.size()){
            pos++;
            return setNumberList.get(pos-1);
        }
        if(cycleNumbers && !setNumberList.isEmpty()){
            pos = 1;
            return setNumberList.get(pos-1);
        }
        if(random == null){
            random = new Random(0);
        }
        return random.nextInt();
    }
}
