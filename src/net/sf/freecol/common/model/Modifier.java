
package net.sf.freecol.common.model;

import java.util.ArrayList;

public class Modifier {

    public static final  String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final  String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public final String id;
    public final int addend;
    public final int numerator;
    public final int denominator;


    public Modifier(String id, int addend) {
	this.id = id;
	this.addend = addend;
	this.numerator = 0;
	this.denominator = 1;
    }

    public Modifier(String id, int numerator, int denominator) {
	this.id = id;
	this.addend = 0;
	this.numerator = numerator;
	this.denominator = denominator;
    }



    public static int getOffensePower(Unit attacker, Unit defender) {
	ArrayList<Modifier> modifiers = getOffensiveModifiers(attacker, defender);
	return modifiers.get(modifiers.size() - 1).addend;
    }

    
    public static ArrayList<Modifier> getOffensiveModifiers(Unit attacker, Unit defender) {
	ArrayList<Modifier> result = new ArrayList<Modifier>();

	int totalAddend = attacker.getUnitType().offence;
	int totalNumerator = 1;
	int totalDenominator = 1;

	result.add(new Modifier("modifiers.baseOffense", totalAddend));

        if (attacker.isNaval()) {
	    int goodsCount = attacker.getGoodsCount();
            if (goodsCount > 0) {
		// -12.5% penalty for every unit of cargo.
		result.add(new Modifier("modifiers.cargoPenalty", 8 - goodsCount, 8));
		totalNumerator *= 8 - goodsCount;
		totalDenominator *= 8;
            }
            if (attacker.getType() == Unit.PRIVATEER && 
                attacker.getOwner().hasFather(FoundingFather.FRANCIS_DRAKE)) {
		// Drake grants 50% attack bonus
		result.add(new Modifier("modifiers.drake", 3, 2));
		totalNumerator *= 3;
		totalDenominator *= 2;
            }
        } else {

	    if (attacker.isArmed()) {
		if (totalAddend == 0) {
		    // civilian
		    result.add(new Modifier("modifiers.armed", 2));
		    totalAddend += 2;
		} else {
		    // brave or REF
		    result.add(new Modifier("modifiers.armed", 1));
		    totalAddend += 1;
		}
	    }

	    if (attacker.isMounted()) {
		result.add(new Modifier("modifiers.mounted", 1));
                totalAddend += 1;
	    }

	    // 50% attack bonus
	    result.add(new Modifier("modifiers.attackBonus", 3, 2)); 
	    totalNumerator *= 3;
	    totalDenominator *= 2;

	    // movement penalty
	    int movesLeft = attacker.getMovesLeft();
	    if (movesLeft < 3) {
		result.add(new Modifier("modifiers.movementPenalty", movesLeft, 3));
		totalNumerator *= movesLeft;
		totalDenominator *= 3;
	    }

	    // In the open
	    if (defender != null &&
                defender.getTile() != null &&
		defender.getTile().getSettlement() == null) {
		
		// Ambush bonus in the open: defender's defense bonus
		if ((attacker.getType() != Unit.BRAVE && 
		     defender.getOwner().isREF()) ||
		    (attacker.getType() == Unit.BRAVE && 
		     defender.getOwner().isREF())) {
		    int defenseBonus = defender.getTile().defenseBonus();
		    result.add(new Modifier("modifiers.ambushBonus", defenseBonus, 100));
		    totalNumerator *= defenseBonus;
		    totalDenominator *= 100;
		}

		// REF bombardment bonus
		if (attacker.getOwner().isREF()) {
		    result.add(new Modifier("modifiers.REFbonus", 3, 2));
		    totalNumerator *= 3;
		    totalDenominator *= 2;
		}

		// 75% Artillery in the open penalty
		if (attacker.getType() == Unit.ARTILLERY ||
		    attacker.getType() == Unit.DAMAGED_ARTILLERY) {
		    result.add(new Modifier("modifiers.artilleryPenalty", 1, 4));
		    totalDenominator *= 4;
		}
	    }
	}

	int offensivePower = (totalAddend * totalNumerator) / totalDenominator;
	result.add(new Modifier("modifiers.finalResult", offensivePower));
        return result;
    }

    public static int getDefensePower(Unit attacker, Unit defender) {
	ArrayList<Modifier> modifiers = getDefensiveModifiers(attacker, defender);
	return modifiers.get(modifiers.size() - 1).addend;
    }


    public static ArrayList<Modifier> getDefensiveModifiers(Unit attacker, Unit defender) {

	ArrayList<Modifier> result = new ArrayList<Modifier>();
        if (defender == null) {
            return result;
        }

	int totalAddend = attacker.getUnitType().defence;
	int totalNumerator = 1;
	int totalDenominator = 1;

	result.add(new Modifier("modifiers.baseDefense", totalAddend));

        if (defender.isNaval()) {
	    int goodsCount = defender.getGoodsCount();
            if (goodsCount > 0) {
		// -12.5% penalty for every unit of cargo.
		result.add(new Modifier("modifiers.cargoPenalty", 8 - goodsCount, 8));
		totalNumerator *= 8 - goodsCount;
		totalDenominator *= 8;
            }
            if (defender.getType() == Unit.PRIVATEER && 
                defender.getOwner().hasFather(FoundingFather.FRANCIS_DRAKE)) {
		// Drake grants 50% attack bonus
		result.add(new Modifier("modifiers.drake", 3, 2));
		totalNumerator *= 3;
		totalDenominator *= 2;
            }
        } else {

	    if (defender.getOwner().hasFather(FoundingFather.PAUL_REVERE) && 
		defender.isColonist() &&
		totalAddend == 1 &&
		defender.getLocation() instanceof WorkLocation) {
		result.add(new Modifier("modifiers.paulRevere", 1));
		totalAddend += 1;
	    }

	    if (defender.isArmed()) {
		result.add(new Modifier("modifiers.armed", 1));
		totalAddend += 1;
	    }

	    if (defender.isMounted()) {
		result.add(new Modifier("modifiers.mounted", 1));
                totalAddend += 1;
	    }

	    // 50% fortify bonus
	    if (defender.getState() == Unit.FORTIFIED) {
		result.add(new Modifier("modifiers.fortified", 3, 2));
		totalNumerator *= 3;
		totalDenominator *= 2;
	    }

	    if (defender.getTile() != null && 
		defender.getTile().getSettlement() != null) {
		if (defender.getTile().getSettlement() instanceof Colony) {
		    // Colony defensive bonus.
		    Colony colony = ((Colony) defender.getTile().getSettlement());
		    int numerator = 2; 
		    switch(colony.getBuilding(Building.STOCKADE).getLevel()) {
		    case Building.NOT_BUILT:
		    default:
			// 50% colony bonus
			numerator = 3;
                        result.add(new Modifier("modifiers.inColony", numerator, 2));
			break;
		    case Building.HOUSE:
			// 100% stockade bonus
			numerator = 4;
                        result.add(new Modifier("modifiers.stockade", numerator, 2));
			break;
		    case Building.SHOP:
			// 150% fort bonus
			numerator = 5;
                        result.add(new Modifier("modifiers.fort", numerator, 2));
			break;
		    case Building.FACTORY:
			// 200% fortress bonus
			numerator = 6;
                        result.add(new Modifier("modifiers.fortress", numerator, 2));
		    }
		    totalNumerator *= numerator;
		    totalDenominator *= 2;
		    if ((defender.getType() == Unit.ARTILLERY || 
			 defender.getType() == Unit.DAMAGED_ARTILLERY) &&
			attacker.getType() == Unit.BRAVE) {
			// 100% defense bonus against an Indian raid
			result.add(new Modifier("modifiers.artilleryAgainstRaid", 2, 1));
			totalNumerator *= 2;
		    }
		} else if (defender.getTile().getSettlement() instanceof IndianSettlement) {
		    // Indian settlement defensive bonus.
		    result.add(new Modifier("modifiers.inSettlement", 3, 2));
		    totalNumerator *= 3;
		    totalDenominator *= 2;
		} else {
		    // In the open
		    if (!((attacker.getType() != Unit.BRAVE && 
			   defender.getOwner().isREF()) ||
			  (attacker.getType() == Unit.BRAVE && 
			   defender.getOwner().isREF()))) {
			// Terrain defensive bonus.
			int defenseBonus = defender.getTile().defenseBonus();
			result.add(new Modifier("modifiers.terrainBonus", defenseBonus, 100));
			totalNumerator *= defenseBonus;
			totalDenominator *= 100;
		    }
		    if ((defender.getType() == Unit.ARTILLERY || 
			 defender.getType() == Unit.DAMAGED_ARTILLERY) &&
			defender.getState() != Unit.FORTIFIED) {
			// -75% Artillery in the Open penalty
			result.add(new Modifier("modifiers.artilleryPenalty", 1, 4));
			totalDenominator *= 4;
		    }
		}

	    }

        }
	int defensivePower = (totalAddend * totalNumerator) / totalDenominator;
	result.add(new Modifier("modifiers.finalResult", defensivePower));
        return result;
    }


}
