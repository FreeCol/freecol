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

package net.sf.freecol.common.model;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;

import org.w3c.dom.Element;


/**
 * The <code>Ability</code> class encapsulates a bonus or penalty
 * that can be applied to any action within the game, most obviously
 * combat.
 */
public final class Ability extends Feature {

    /**
     * The ability to add the current tax as a bonus to the production
     * of bells.  Provided by the {@link FoundingFather} Thomas Paine.
     */
    public static final String ADD_TAX_TO_BELLS
        = "model.ability.addTaxToBells";

    /** The ability to always receive a peace offer (Franklin). */
    public static final String ALWAYS_OFFERED_PEACE
        = "model.ability.alwaysOfferedPeace";

    /** The ability to ambush other units. */
    public static final String AMBUSH_BONUS
        = "model.ability.ambushBonus";

    /** The susceptibility to ambush from other units. */
    public static final String AMBUSH_PENALTY
        = "model.ability.ambushPenalty";

    /** Terrain in which an ambush may occur. */
    public static final String AMBUSH_TERRAIN
        = "model.ability.ambushTerrain";

    /** Equipment type ability denoting the holder is armed. */
    public static final String ARMED
        = "model.ability.armed";

    /** The ability of a colony to automatocally arm defenders (Revere). */
    public static final String AUTOMATIC_EQUIPMENT
        = "model.ability.automaticEquipment";

    /** The ability to automatically promote combat winners (Washington). */
    public static final String AUTOMATIC_PROMOTION
        = "model.ability.automaticPromotion";

    /**
     * The ability of certain buildings (e.g. the stables) to produce
     * goods even if no units are present.
     */
    public static final String AUTO_PRODUCTION
        = "model.ability.autoProduction";

    /**
     * The ability of certain buildings (e.g. the stables) to avoid
     * producing more goods than the colony can store, which would
     * normally go to waste.
     */
    public static final String AVOID_EXCESS_PRODUCTION
        = "model.ability.avoidExcessProduction";

    /** The ability for better foreign affairs reporting (deWitt). */
    public static final String BETTER_FOREIGN_AFFAIRS_REPORT
        = "model.ability.betterForeignAffairsReport";

    /** The ability of a unit to bombard other units. */
    public static final String BOMBARD
        = "model.ability.bombard";

    /** The ability of a colony to bombard ships. */
    public static final String BOMBARD_SHIPS
        = "model.ability.bombardShips";

    /**
     * The ability to be born in a Colony.  Only Units with this
     * ability can be produced by a Colony.
     */
    public static final String BORN_IN_COLONY
        = "model.ability.bornInColony";

    /**
     * The ability to be born in an IndianSettlement.  Only Units with
     * this ability can be produced by an IndianSettlement.
     */
    public static final String BORN_IN_INDIAN_SETTLEMENT
        = "model.ability.bornInIndianSettlement";

    /**
     * The ability to build {@link BuildableType}s, such as units.  The
     * shipyard provides the ability to build ships, for example.
     */
    public static final String BUILD
        = "model.ability.build";

    /**
     * The ability to build a customs house.  Yes this is misspelled.
     */
    public static final String BUILD_CUSTOM_HOUSE
        = "model.ability.buildCustomHouse";

    /** The ability to build a factories. */
    public static final String BUILD_FACTORY
        = "model.ability.buildFactory";

    /**
     * The ability of certain unarmed units to be captured by another
     * player's units. Units lacking this ability (e.g. braves) will
     * be destroyed instead.
     */
    public static final String CAN_BE_CAPTURED
        = "model.ability.canBeCaptured";

    /** The ability of certain units to be equipped with tools, muskets, etc. */
    public static final String CAN_BE_EQUIPPED
        = "model.ability.canBeEquipped";

    /** The ability of a player to recruit units. */
    public static final String CAN_RECRUIT_UNIT
        = "model.ability.canRecruitUnit";

    /** The ability of certain armed units to capture equipment.*/
    public static final String CAPTURE_EQUIPMENT
        = "model.ability.captureEquipment";

    /**
     * The ability of certain units (e.g. privateers) to capture goods
     * carried by another player's units.
     */
    public static final String CAPTURE_GOODS
        = "model.ability.captureGoods";

    /** The ability of certain armed units to capture another player's units.*/
    public static final String CAPTURE_UNITS
        = "model.ability.captureUnits";

    /** The ability of certain units (e.g. wagon trains) to carry goods. */
    public static final String CARRY_GOODS
        = "model.ability.carryGoods";

    /**
     * The ability of certain units (e.g. treasure trains) to carry
     * treasure.
     */
    public static final String CARRY_TREASURE
        = "model.ability.carryTreasure";

    /** The ability of certain units (e.g. ships) to carry other units. */
    public static final String CARRY_UNITS
        = "model.ability.carryUnits";

    /** Restrict some buildings to only be buildable on the coast. */
    public static final String COASTAL_ONLY
        = "model.ability.coastalOnly";

    /**
     * The ability of certain consumers (e.g. BuildQueues) to consume
     * a large amount of goods at once instead of turn by turn.
     */
    public static final String CONSUME_ALL_OR_NOTHING
        = "model.ability.consumeAllOrNothing";

    /** The ability of customs houses to trade with other players. */
    public static final String CUSTOM_HOUSE_TRADES_WITH_FOREIGN_COUNTRIES
        = "model.ability.customHouseTradesWithForeignCountries";

    /** The ability to demand tribute even when unarmed. */
    public static final String DEMAND_TRIBUTE
        = "model.ability.demandTribute";
    
    /** Units with this ability are demoted on losing all equipment. */
    public static final String DEMOTE_ON_ALL_EQUIPMENT_LOST
        = "model.ability.demoteOnAllEquipLost";

    /** The ability to denounce heresy. */
    public static final String DENOUNCE_HERESY
        = "model.ability.denounceHeresy";

    /** Units with this ability die on losing all equipment. */
    public static final String DISPOSE_ON_ALL_EQUIPMENT_LOST
        = "model.ability.disposeOnAllEquipLost";

    /** Units with this ability die on losing a combat. */
    public static final String DISPOSE_ON_COMBAT_LOSS
        = "model.ability.disposeOnCombatLoss";

    /** The ability to bless a missionary. */
    public static final String DRESS_MISSIONARY
        = "model.ability.dressMissionary";

    /** The ability to elect founding fathers. */
    public static final String ELECT_FOUNDING_FATHER
        = "model.ability.electFoundingFather";

    /** The ability to establish a mission. */
    public static final String ESTABLISH_MISSION
        = "model.ability.establishMission";

    /** The ability to evade naval attack. */
    public static final String EVADE_ATTACK
        = "model.ability.evadeAttack";

    /**
     * The ability of certain units to work as missionaries more
     * effectively.
     */
    public static final String EXPERT_MISSIONARY
        = "model.ability.expertMissionary";

    /** The ability of certain units to build TileImprovements faster. */
    public static final String EXPERT_PIONEER
        = "model.ability.expertPioneer";

    /** The ability of certain units to work as scouts more effectively. */
    public static final String EXPERT_SCOUT
        = "model.ability.expertScout";

    /** The ability of certain units to work as soldiers more effectively. */
    public static final String EXPERT_SOLDIER
        = "model.ability.expertSoldier";

    /**
     * The somewhat controversial ability of expert units in factory
     * level buildings to produce a certain amount of goods even when
     * no raw materials are available.  Allegedly, this is a feature of
     * the original game.
     */
    public static final String EXPERTS_USE_CONNECTIONS
        = "model.ability.expertsUseConnections";

    /** The ability to export goods to Europe directly. */
    public static final String EXPORT
        = "model.ability.export";

    /** The ability of a unit to found a colony. */
    public static final String FOUND_COLONY
        = "model.ability.foundColony";

    /** The ability of a unit to be found in a lost city. */
    public static final String FOUND_IN_LOST_CITY
        = "model.ability.foundInLostCity";

    /** The ability of a player to found colonies. */
    public static final String FOUNDS_COLONIES
        = "model.ability.foundsColonies";

    /** The ability of a colony which is a port. */
    public static final String HAS_PORT
        = "model.ability.hasPort";

    /** The ability to ignore the monarchs wars. */
    public static final String IGNORE_EUROPEAN_WARS
        = "model.ability.ignoreEuropeanWars";

    /** The ability of a unit to make terrain improvements. */
    public static final String IMPROVE_TERRAIN
        = "model.ability.improveTerrain";

    /** The ability to incite the natives. */
    public static final String INCITE_NATIVES
        = "model.ability.inciteNatives";

    /**
     * The ability denoting that a declaration of independence has
     * been made.
     */
    public static final String INDEPENDENCE_DECLARED
        = "model.ability.independenceDeclared";

    /**
     * The ability denoting that this is an independent nation.
     * Note: this differs from INDEPENDENCE_DECLARED in that
     * the REF is also (representing) an independent nation.
     */
    public static final String INDEPENDENT_NATION
        = "model.ability.independentNation";

    /** Units with this ability can be chosen as mercenaries support units. */
    public static final String MERCENARY_UNIT
        = "model.ability.mercenaryUnit";

    /** Equipment type ability denoting the holder is mounted. */
    public static final String MOUNTED
        = "model.ability.mounted";

    /** The ability to move to Europe from a tile. */
    public static final String MOVE_TO_EUROPE
        = "model.ability.moveToEurope";

    /** The ability to attack multiple times. */
    public static final String MULTIPLE_ATTACKS
        = "model.ability.multipleAttacks";

    /** The ability of being a native unit. */
    public static final String NATIVE
        = "model.ability.native";

    /** The ability of ships to move across water tiles. */
    public static final String NAVAL_UNIT
        = "model.ability.navalUnit";

    /** The ability to engage in diplomatic negotiation. */
    public static final String NEGOTIATE
        = "model.ability.negotiate";

    /** Units with this property are persons, not a ship or wagon etc. */
    public static final String PERSON
        = "model.ability.person";

    /** The ability to pillage unprotected colonies. */
    public static final String PILLAGE_UNPROTECTED_COLONY
        = "model.ability.pillageUnprotectedColony";

    /**
     * The ability of certain units (e.g. privateers) to attack and
     * plunder another player's units without causing war.
     */
    public static final String PIRACY
        = "model.ability.piracy";

    /**
     * An ability that enhances the treasure plundered from native
     * settlements.
     */
    public static final String PLUNDER_NATIVES
        = "model.ability.plunderNatives";

    /** The ability to produce goods (e.g. fish) on water tiles. */
    public static final String PRODUCE_IN_WATER
        = "model.ability.produceInWater";

    /** Units with this ability can be added to the REF. */
    public static final String REF_UNIT
        = "model.ability.refUnit";

    /** The ability to repair certain units. */
    public static final String REPAIR_UNITS
        = "model.ability.repairUnits";

    /** A national ability required to generate a REF. */
    public static final String ROYAL_EXPEDITIONARY_FORCE
        = "model.ability.royalExpeditionaryForce";

    /** LCRs always yield positive results (deSoto). */
    public static final String RUMOURS_ALWAYS_POSITIVE
        = "model.ability.rumoursAlwaysPositive";

    /** The ability to see all colonies (Coronado). */
    public static final String SEE_ALL_COLONIES
        = "model.ability.seeAllColonies";

    /** The ability to select recruits (Brewster). */
    public static final String SELECT_RECRUIT
        = "model.ability.selectRecruit";

    /** The ability to speak to a native settlement chief. */
    public static final String SPEAK_WITH_CHIEF
        = "model.ability.speakWithChief";

    /** The ability to spy on a colony. */
    public static final String SPY_ON_COLONY
        = "model.ability.spyOnColony";

    /**
     * Units with this ability can be chosen as support units from
     * the crown.
     */
    public static final String SUPPORT_UNIT
        = "model.ability.supportUnit";

    /** Buildings with this ability can be used to teach. */
    public static final String TEACH
        = "model.ability.teach";

    /** The ability to trade with foreign colonies (deWitt). */
    public static final String TRADE_WITH_FOREIGN_COLONIES
        = "model.ability.tradeWithForeignColonies";

    /** Undead units have this ability. */
    public static final String UNDEAD
        = "model.ability.undead";


    /** The ability value. */
    private boolean value = true;


    /**
     * Creates a new <code>Ability</code> instance.
     *
     * @param id The object identifier.
     */
    public Ability(String id) {
        this(id, null, true);
    }

    /**
     * Creates a new <code>Ability</code> instance.
     *
     * @param id The object identifier.
     * @param value The ability value.
     */
    public Ability(String id, boolean value) {
        this(id, null, value);
    }

    /**
     * Creates a new <code>Ability</code> instance.
     *
     * @param id The object identifier.
     * @param source The source <code>FreeColGameObjectType</code>.
     * @param value The ability value.
     */
    public Ability(String id, FreeColGameObjectType source, boolean value) {
        setId(id);
        setSource(source);
        this.value = value;
    }

    /**
     * Creates a new <code>Ability</code> instance.
     *
     * @param template An <code>Ability</code> to copy from.
     */
    public Ability(Ability template) {
        copyFrom(template);
        this.value = template.value;
    }

    /**
     * Creates a new <code>Ability</code> instance.
     *
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @param specification A <code>Specification</code> to refer to.
     * @exception XMLStreamException if an error occurs
     */
    public Ability(FreeColXMLReader xr,
                   Specification specification) throws XMLStreamException {
        setSpecification(specification);
        readFromXML(xr);
    }

    /**
     * Creates a new <code>Ability</code> instance.
     *
     * @param e The <code>Element</code> to read from.
     * @param specification A <code>Specification</code> to refer to.
     */
    public Ability(Element e, Specification specification) {
        setSpecification(specification);
        readFromXMLElement(e);
    }


    /**
     * Get the ability value.
     *
     * @return The ability value.
     */
    public boolean getValue() {
        return value;
    }

    /**
     * Set the ability value.
     *
     * @param newValue The new ability value.
     */
    public void setValue(final boolean newValue) {
        this.value = newValue;
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Ability) {
            return this.value == ((Ability)o).value
                && super.equals(o);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash += (value) ? 1 : 0;
        return hash;
    }


    // Serialization


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(VALUE_TAG, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        value = xr.getAttribute(VALUE_TAG, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("[ ").append(getId());
        if (getSource() != null) {
            sb.append(" (").append(getSource().getId()).append(")");
        }
        sb.append(" = ").append(value).append(" ]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the XML tag name for this element.
     *
     * @return "ability".
     */
    public static String getXMLElementTagName() {
        return "ability";
    }
}
