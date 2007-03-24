
package net.sf.freecol.common.model;

import java.util.ArrayList;

/**
 * The <code>Modifier</code> class encapsulates a bonus or penalty
 * that can be applied to any action within the game, most obviously
 * combat.
 */
public final class Modifier {

    public static final  String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final  String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    
    // the ID of the modifier, used to look up name, etc.
    public final String id;
    // whether this modifier is additive or multiplicative
    private final boolean isAdditive;
    // the addend of an additive modifier, defaults to zero
    public final int addend;
    // the numerator of a multiplicative modifier, defaults to zero
    public final int numerator;
    // the denominator of a multiplicative modifier, defaults to one
    public final int denominator;


    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param addend an <code>int</code> value
     */
    public Modifier(String id, int addend) {
        this.id = id;
        this.isAdditive = true;
        this.addend = addend;
        this.numerator = 0;
        this.denominator = 1;
    }

    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param numerator an <code>int</code> value
     * @param denominator an <code>int</code> value
     */
    public Modifier(String id, int numerator, int denominator) {
        this.id = id;
        this.isAdditive = false;
        this.addend = 0;
        this.numerator = numerator;
        this.denominator = denominator;
    }



    /**
     * Return the offensive power of the attacker versus the defender
     * as an int.
     *
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return an <code>int</code> value
     */
    public static int getOffensePower(Unit attacker, Unit defender) {
        ArrayList<Modifier> modifiers = getOffensiveModifiers(attacker, defender);
        return modifiers.get(modifiers.size() - 1).addend;
    }


    /**
     * Return a list of all offensive modifiers that apply to the
     * attacker versus the defender.
     *
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return an <code>ArrayList</code> of Modifiers
     */
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

    /**
     * Return the defensive power of the defender versus the attacker
     * as an int.
     *
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return an <code>int</code> value
     */
    public static int getDefensePower(Unit attacker, Unit defender) {
        ArrayList<Modifier> modifiers = getDefensiveModifiers(attacker, defender);
        return modifiers.get(modifiers.size() - 1).addend;
    }


    /**
     * Return a list of all defensive modifiers that apply to the
     * defender versus the attacker.
     *
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @return an <code>ArrayList</code> of Modifiers
     */
    public static ArrayList<Modifier> getDefensiveModifiers(Unit attacker, Unit defender) {

        ArrayList<Modifier> result = new ArrayList<Modifier>();
        if (defender == null) {
            return result;
        }

        int totalAddend = defender.getUnitType().defence;
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
            // Paul Revere makes an unarmed colonist in a settlement pick up
            // a stock-piled musket if attacked, so the bonus should be applied
            // for unarmed colonists inside colonies where there are muskets
            // available.
            if (defender.isArmed()) {
                result.add(new Modifier("modifiers.armed", 1));
                totalAddend += 1;
            } else if (defender.getOwner().hasFather(FoundingFather.PAUL_REVERE) && 
                defender.isColonist() &&
                defender.getLocation() instanceof WorkLocation) {
                Colony colony = ((WorkLocation)defender.getLocation()).getColony();
                if(colony.getGoodsCount(Goods.MUSKETS) >= 50) {
                    result.add(new Modifier("modifiers.paulRevere", 1));
                    totalAddend += 1;
                }
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
                Modifier settlementModifier = getSettlementModifier(attacker, defender.getTile().getSettlement());
                result.add(settlementModifier);
                totalNumerator *= settlementModifier.numerator;
                totalDenominator *= settlementModifier.denominator;
                if ((defender.getType() == Unit.ARTILLERY || 
                     defender.getType() == Unit.DAMAGED_ARTILLERY) &&
                    attacker.getType() == Unit.BRAVE) {
                    // 100% defense bonus against an Indian raid
                    result.add(new Modifier("modifiers.artilleryAgainstRaid", 2, 1));
                    totalNumerator *= 2;
                }
            } else {
                // In the open
                if (!((attacker.getType() != Unit.BRAVE && 
                       defender.getOwner().isREF()) ||
                      (attacker.getType() == Unit.BRAVE && 
                       defender.getOwner().isREF()))) {
                    // Terrain defensive bonus.
                    // terrain defense bonus has different scale
                    int defenseBonus = defender.getTile().defenseBonus() + 100;
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
        int defensivePower = (totalAddend * totalNumerator) / totalDenominator;
        result.add(new Modifier("modifiers.finalResult", defensivePower));
        return result;
    }

    /**
     * Return the defensive modifier that applies to defenders in the
     * given settlement versus the attacker.
     *
     * @param attacker an <code>Unit</code> value
     * @param settlement a <code>Settlement</code> value
     * @return a <code>Modifier</code>
     */
    public static Modifier getSettlementModifier(Unit attacker, Settlement settlement) {

        if (settlement instanceof Colony) {
            // Colony defensive bonus.
            Colony colony = (Colony) settlement;
            switch(colony.getBuilding(Building.STOCKADE).getLevel()) {
            case Building.NOT_BUILT:
            default:
                // 50% colony bonus
                return new Modifier("modifiers.inColony", 3, 2);
            case Building.HOUSE:
                // 100% stockade bonus
                return new Modifier("modifiers.stockade", 4, 2);
            case Building.SHOP:
                // 150% fort bonus
                return new Modifier("modifiers.fort", 5, 2);
            case Building.FACTORY:
                // 200% fortress bonus
                return new Modifier("modifiers.fortress", 6, 2);
            }
        } else if (settlement instanceof IndianSettlement) {
            // Indian settlement defensive bonus.
            return new Modifier("modifiers.inSettlement", 3, 2);
        } else {
            return new Modifier(null, 0);
        }
    }

    /**
     * Return a formatted string appropriate for the value this
     * modifier represents.
     *
     * @return a <code>String</code> value
     */
    public String getFormattedResult() {
        if (isAdditive) {
            if (addend == Integer.MIN_VALUE) {
                return "?";
            } else if (addend > 0) {
                return "+" + String.valueOf(addend);
            } else {
                return String.valueOf(addend);
            }
        } else {
            float modifier = (100 * numerator / denominator) - 100;
            if (modifier > 0) {
                return "+" + String.valueOf(modifier) + "%";
            } else {
                return String.valueOf(modifier) + "%";
            }
        }
    }

}
