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


/**
 * Keeps track of the available game options. New options must be
 * added to the {@link Specification} and each option should be given
 * an unique identifier (defined as a constant in this class).
 */
public class GameOptions {

    /** Map game option group. */
    public static final String GAMEOPTIONS_MAP
        = "gameOptions.map";
    
    /** The number of turns to sail between Europe and the New World. */
    public static final String TURNS_TO_SAIL
        = "model.option.turnsToSail";

    /** Changes the settlement limits. */
    public static final String SETTLEMENT_LIMIT_MODIFIER
        = "model.option.settlementLimitModifier";

    /** Enables/disables fog of war. */
    public static final String FOG_OF_WAR
        = "model.option.fogOfWar";

    /** Whether to award exploration points or not. */
    public static final String EXPLORATION_POINTS
        = "model.option.explorationPoints";

    /** Allow amphibious moves. */
    public static final String AMPHIBIOUS_MOVES
        = "model.option.amphibiousMoves";

    /** Allow empty units to trade. */
    public static final String EMPTY_TRADERS
        = "model.option.emptyTraders";

    /** Do settlement actions consume the chief contact bonus. */
    public static final String SETTLEMENT_ACTIONS_CONTACT_CHIEF
        = "model.option.settlementActionsContactChief";

    /** Do missionaries provide extra benefits. */
    public static final String ENHANCED_MISSIONARIES
        = "model.option.enhancedMissionaries";

    /** The strength of the influence of a Mission to the
        alarm level of the natives in a settlement. */
    public static final String MISSION_INFLUENCE
        = "model.option.missionInfluence";

    /** Probability of a settlement with a surplus offering a gift. */
    public static final String GIFT_PROBABILITY
        = "model.option.giftProbability";

    /** Probability of an angry settlement making a demand. */
    public static final String DEMAND_PROBABILITY
        = "model.option.demandProbability";

    /** Continue recruiting founding fathers after declaring independence. */
    public static final String CONTINUE_FOUNDING_FATHER_RECRUITMENT
        = "model.option.continueFoundingFatherRecruitment";

    /** Does the REF "teleport" to its first target. */
    public static final String TELEPORT_REF
        = "model.option.teleportREF";

    /** How to determine the starting positions of European players. */
    public static final String STARTING_POSITIONS
        = "model.option.startingPositions";
    public static final int STARTING_POSITIONS_CLASSIC = 0;
    public static final int STARTING_POSITIONS_RANDOM = 1;
    public static final int STARTING_POSITIONS_HISTORICAL = 2;

    /** Percent chance peace will hold if there is a recent treaty. */
    public static final String PEACE_PROBABILITY
        = "model.option.peaceProbability";

    /** Initial immigration target value. */
    public static final String INITIAL_IMMIGRATION
        = "model.option.initialImmigration";

    /** Per turn immigration penalty for units in Europe. */
    public static final String EUROPEAN_UNIT_IMMIGRATION_PENALTY
        = "model.option.europeanUnitImmigrationPenalty";

    /** Per turn player immigration bonus. */
    public static final String PLAYER_IMMIGRATION_BONUS
        = "model.option.playerImmigrationBonus";

    /** Equip European recruits. */
    public static final String EQUIP_EUROPEAN_RECRUITS
        = "model.option.equipEuropeanRecruits";


    /** Colony game option group. */
    public static final String GAMEOPTIONS_COLONY
        = "gameOptions.colony";

    /** Is bell accumulation capped when 100% rebels achieved? */
    public static final String BELL_ACCUMULATION_CAPPED
        = "model.option.bellAccumulationCapped";

    /** Capture units under repair with colony. */
    public static final String CAPTURE_UNITS_UNDER_REPAIR
        = "model.option.captureUnitsUnderRepair";

    /** Does the Custom House sell boycotted goods? */
    public static final String CUSTOM_IGNORE_BOYCOTT
        = "model.option.customIgnoreBoycott";

    /** Custom Houses only allowed in coastal colonies? */
    public static final String CUSTOMS_ON_COAST
        = "model.option.customsOnCoast";

    /** All units disembark when a carrier arrives in a colony (Col1). */
    public static final String DISEMBARK_IN_COLONY
        = "model.option.disembarkInColony";

    /**
     * Whether experts have connections, producing without raw
     * materials in factories.
     */
    public static final String EXPERTS_HAVE_CONNECTIONS
        = "model.option.expertsHaveConnections";

    /** Can colonies be founded during the war of independence? */
    public static final String FOUND_COLONY_DURING_REBELLION
        = "model.option.foundColonyDuringRebellion";

    /**
     * Can colony buildings be completed quickly by paying for the
     * missing goods?
     */
    public static final String PAY_FOR_BUILDING
        = "model.option.payForBuilding";

    /**
     * Does a hammer surplus after a build completes accumulate to
     * the next build?
     */
    public static final String SAVE_PRODUCTION_OVERFLOW
        = "model.option.saveProductionOverflow";

    /** Clear the hammers when changing production. */
    public static final String CLEAR_HAMMERS_ON_CONSTRUCTION_SWITCH
        = "model.option.clearHammersOnConstructionSwitch";

    /**
     * Whether to educate the least skilled unit first. This is the
     * behaviour of the original game and disallows manually assigning
     * students to teachers.
     */
    public static final String ALLOW_STUDENT_SELECTION
        = "model.option.allowStudentSelection";

    /** Enable upkeep requirement on buildings. */
    public static final String ENABLE_UPKEEP
        = "model.option.enableUpkeep";

    /**
     * Toggle whether artifical tile improvements contribute to the
     * production of secondary (non-food) goods on the colony center
     * tile.
     */
    public static final String ONLY_NATURAL_IMPROVEMENTS
        = "model.option.onlyNaturalImprovements";

    /** Enable natural disasters striking colonies. */
    public static final String NATURAL_DISASTERS
        = "model.option.naturalDisasters";

    ///** The cost of a single hammer when buying a building in a colony. */
    //Unused at the moment
    // public static final String HAMMER_PRICE = "hammerPrice";


    /** Victory conditions game option group. */
    public static final String GAMEOPTIONS_VICTORY_CONDITIONS
        = "gameOptions.victoryConditions";

    /**
     * Victory condition: Should the <code>Player</code> who first
     * defeats the Royal Expeditionary Force win the game?
     */
    public static final String VICTORY_DEFEAT_REF
        = "model.option.victoryDefeatREF";

    /**
     * Victory condition: Should a <code>Player</code> who
     * defeats all other European players win the game?
     */
    public static final String VICTORY_DEFEAT_EUROPEANS
        = "model.option.victoryDefeatEuropeans";

    /**
     * Victory condition: Should a <code>Player</code> who defeats all
     * other human players win the game?
     */
    public static final String VICTORY_DEFEAT_HUMANS
        = "model.option.victoryDefeatHumans";


    /** Years game option group. */
    public static final String GAMEOPTIONS_YEARS
        = "gameOptions.years";

    /**
     * The year in which the game starts. At the moment, changing this
     * value only shortens the game.  In future, it might cause the map
     * generator to create foreign colonies.
     */
    public static final String STARTING_YEAR
        = "model.option.startingYear";

    /**
     * The first year in which there are two seasons.  Changing this
     * value influences the duration of the game.
     */
    public static final String SEASON_YEAR
        = "model.option.seasonYear";

    /** The year in which owning at least one colony becomes mandatory. */
    public static final String MANDATORY_COLONY_YEAR
        = "model.option.mandatoryColonyYear";

    /** The very last year of the game. */
    public static final String LAST_YEAR
        = "model.option.lastYear";

    /**
     * The last year of the game for colonial players.  In other
     * words, if a colonial player does not declare independence by
     * the end of this year, the game is lost.
     */
    public static final String LAST_COLONIAL_YEAR
        = "model.option.lastColonialYear";

    /**
     * Score bonus on declaring independence = (1780, Spring) - turn
     * 1780 is documented in the Col1 manual:
     *   ``if you've declared your independence before 1780, your score
     *     is increased; the sooner you declare; the better your Bonus.''
     * which suggests this needs to cut off at 1780.  Using turns
     * rather than years though so that scores are equivalent in games
     * with &gt;2 seasons.
     */
    public static final String INDEPENDENCE_TURN
        = "model.option.independenceTurn";

    /** The "ages" where founding father weights change, specified as years. */
    public static final String AGES
        = "model.option.ages";

    /** The number of seasons. */
    public static final String SEASONS
        = "model.option.seasons";
    
    
    /**
     * Prices game option group.
     * This group is synthesized at game initialization.
     */
    public static final String GAMEOPTIONS_PRICES
        = "gameOptions.prices";
    

    // Difficulty options are effectively special multi-valued game options.

    /** Immigration (difficulty) group. */
    public static final String DIFFICULTY_IMMIGRATION
        = "model.difficulty.immigration";

    /** Rate cross requirement increases. */
    public static final String CROSSES_INCREMENT
        = "model.option.crossesIncrement";

    /** Rate recruitment prices increase. */
    public static final String RECRUIT_PRICE_INCREASE
        = "model.option.recruitPriceIncrease";

    /** Lower bound on the recruitment price. */
    public static final String LOWER_CAP_INCREASE
        = "model.option.lowerCapIncrease";

    /** A price increase prefix. */
    public static final String PRICE_INCREASE
        = "model.option.priceIncrease";

    /** Are there price increases specific to unit type. */
    public static final String PRICE_INCREASE_PER_TYPE
        = "model.option.priceIncreasePerType";

    /** Are the initial immigrants expert units. */
    public static final String EXPERT_STARTING_UNITS
        = "model.option.expertStartingUnits";

    /** Predefined initial immigrants. */
    public static final String IMMIGRANTS
        = "model.option.immigrants";


    /** Native (difficulty) group. */
    public static final String DIFFICULTY_NATIVES
        = "model.difficulty.natives";

    /** Base multiplier from land productivity to price. */
    public static final String LAND_PRICE_FACTOR
        = "model.option.landPriceFactor";

    /** Probability of an attack extracting converts. */
    public static final String NATIVE_CONVERT_PROBABILITY
        = "model.option.nativeConvertProbability";

    /** Probability of angry natives burning missions. */
    public static final String BURN_PROBABILITY
        = "model.option.burnProbability";

    /** Factor for how a successful native demand reduces tension. */
    public static final String NATIVE_DEMANDS
        = "model.option.nativeDemands";

    /** Rumour difficulty factor. [FIXME: should go away] */
    public static final String RUMOUR_DIFFICULTY
        = "model.option.rumourDifficulty";

    /** The penalty applied to trading with the natives from a ship. */
    public static final String SHIP_TRADE_PENALTY
        = "model.option.shipTradePenalty";

    /** The score penalty for destroying a settlement. */
    public static final String DESTROY_SETTLEMENT_SCORE
        = "model.option.destroySettlementScore";

    /** The behaviour when building on native land. */
    public static final String BUILD_ON_NATIVE_LAND
        = "model.option.buildOnNativeLand";
    public static final String BUILD_ON_NATIVE_LAND_ALWAYS
        = "model.option.buildOnNativeLand.always";
    public static final String BUILD_ON_NATIVE_LAND_FIRST
        = "model.option.buildOnNativeLand.first";
    public static final String BUILD_ON_NATIVE_LAND_FIRST_AND_UNCONTACTED
        = "model.option.buildOnNativeLand.firstAndUncontacted";
    public static final String BUILD_ON_NATIVE_LAND_NEVER
        = "model.option.buildOnNativeLand.never";

    /**
     * Option for setting the number of settlements on the map.
     * Note: *not* a MapGeneratorOption because it is difficulty-sensitive.
     */
    public static final String SETTLEMENT_NUMBER
        = "model.option.settlementNumber";


    /** Monarch (difficulty) group. */
    public static final String DIFFICULTY_MONARCH
        = "model.difficulty.monarch";

    /**
     * The grace period at the start of the game before the monarch
     * begins to meddle.
     */
    public static final String MONARCH_MEDDLING
        = "model.option.monarchMeddling";

    /** Moderating factor for tax raises. */
    public static final String TAX_ADJUSTMENT
        = "model.option.taxAdjustment";

    /** Percentage of real price to change for mercenary units. */
    public static final String MERCENARY_PRICE
        = "model.option.mercenaryPrice";

    /** Maximum tax rate. */
    public static final String MAXIMUM_TAX
        = "model.option.maximumTax";

    /** The degree of monarch support. */
    public static final String MONARCH_SUPPORT
        = "model.option.monarchSupport";

    /** Percentage fee for transporting treasures to Europe. */
    public static final String TREASURE_TRANSPORT_FEE
        = "model.option.treasureTransportFee";

    /** Bells to generate to trigger the intervention force. */
    public static final String INTERVENTION_BELLS
        = "model.option.interventionBells";

    /** How often to update the intervention force. */
    public static final String INTERVENTION_TURNS
        = "model.option.interventionTurns";

    /** The basic composition of the REF.  "refSize" is a legacy term. */
    public static final String REF_FORCE
        = "model.option.refSize";

    /** The basic composition of the intervention force. */
    public static final String INTERVENTION_FORCE
        = "model.option.interventionForce";

    /** The basic composition of the mercenary force. */
    public static final String MERCENARY_FORCE
        = "model.option.mercenaryForce";

    /** The base mercenary force sometimes supplied with a war declaration. */
    public static final String WAR_SUPPORT_FORCE
        = "model.option.warSupportForce";

    /** The base amount of gold sometimes supplied with a war declaration. */
    public static final String WAR_SUPPORT_GOLD
        = "model.option.warSupportGold";

    /** Government (difficulty) group. */
    public static final String DIFFICULTY_GOVERNMENT
        = "model.difficulty.government";

    /** The percent SoL to achieve "bad" government. */
    public static final String BAD_GOVERNMENT_LIMIT
        = "model.option.badGovernmentLimit";

    /** The percent SoL to achieve "very bad" government. */
    public static final String VERY_BAD_GOVERNMENT_LIMIT
        = "model.option.veryBadGovernmentLimit";

    /** The percent SoL to achieve "good" government. */
    public static final String GOOD_GOVERNMENT_LIMIT
        = "model.option.goodGovernmentLimit";

    /** The percent SoL to achieve "very good" government. */
    public static final String VERY_GOOD_GOVERNMENT_LIMIT
        = "model.option.veryGoodGovernmentLimit";


    /** Other (difficulty) group. */
    public static final String DIFFICULTY_OTHER
        = "model.difficulty.other";

    /** The amount of money each player will receive before the game starts. */
    public static final String STARTING_MONEY
        = "model.option.startingMoney";

    /** Rate the bells for founding father recruitment grows. */
    public static final String FOUNDING_FATHER_FACTOR
        = "model.option.foundingFatherFactor";

    /** Retributive markup on tea party goods. */
    public static final String ARREARS_FACTOR
        = "model.option.arrearsFactor";

    /** The number of units that do not consume bells. */
    public static final String UNITS_THAT_USE_NO_BELLS
        = "model.option.unitsThatUseNoBells";

    /** Tile production. */
    public static final String TILE_PRODUCTION
        = "model.option.tileProduction";

    /** Bad rumour chance. */
    public static final String BAD_RUMOUR
        = "model.option.badRumour";

    /** Good rumour chance. */
    public static final String GOOD_RUMOUR
        = "model.option.goodRumour";


    /** Cheat (difficulty) group. */
    public static final String DIFFICULTY_CHEAT
        = "model.difficulty.cheat";

    public static final String LIFT_BOYCOTT_CHEAT
        = "model.option.liftBoycottCheat";
    public static final String EQUIP_SCOUT_CHEAT
        = "model.option.equipScoutCheat";
    public static final String EQUIP_PIONEER_CHEAT
        = "model.option.equipPioneerCheat";
    public static final String LAND_UNIT_CHEAT
        = "model.option.landUnitCheat";
    public static final String OFFENSIVE_LAND_UNIT_CHEAT
        = "model.option.offensiveLandUnitCheat";
    public static final String OFFENSIVE_NAVAL_UNIT_CHEAT
        = "model.option.offensiveNavalUnitCheat";
    public static final String TRANSPORT_NAVAL_UNIT_CHEAT
        = "model.option.transportNavalUnitCheat";


    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "gameOptions".
     */
    public static String getXMLElementTagName() {
        return "gameOptions";
    }
}
