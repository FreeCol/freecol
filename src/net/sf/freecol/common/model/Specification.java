/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.UnitChangeType;
import net.sf.freecol.common.option.AbstractOption;
import net.sf.freecol.common.option.AbstractUnitOption;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.FileOption;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionContainer;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.PercentageOption;
import net.sf.freecol.common.option.RangeOption;
import net.sf.freecol.common.option.StringOption;
import net.sf.freecol.common.option.TextOption;
import net.sf.freecol.common.option.UnitListOption;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.Introspector;
import net.sf.freecol.common.util.LogBuilder;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * This class encapsulates any parts of the "specification" for
 * FreeCol that are expressed best using XML.  The XML is loaded
 * through the class loader from the resource named
 * "specification.xml" in the same package as this class.
 */
public final class Specification implements OptionContainer {

    private static final Logger logger = Logger.getLogger(Specification.class.getName());

    /** Fixed class array argument used in newType(). */
    private static final Class[] newTypeClasses
        = new Class[] { String.class, Specification.class };
    
    // Special reader classes for spec objects
    private interface ChildReader {
        public void readChildren(FreeColXMLReader xr) throws XMLStreamException;
    }

    /**
     * Modifiers have to be hooked to the specification and added to
     * the special modifiers list, hence the special purpose reader.
     */
    private class ModifierReader implements ChildReader {

        /**
         * {@inheritDoc}
         */
        @Override
        public void readChildren(FreeColXMLReader xr)
            throws XMLStreamException {
            while (xr.moreTags()) {
                Modifier modifier = new Modifier(xr, Specification.this);
                Specification.this.addModifier(modifier);
                Specification.this.specialModifiers.add(modifier);
            }
        }
    }

    /**
     * Options are special as they live in the allOptionGroups
     * collection, which has its own particular semantics.  So they
     * need their own reader.
     */
    private class OptionReader implements ChildReader {

        private static final String RECURSIVE_TAG = "recursive";

        /**
         * {@inheritDoc}
         */
        @Override
        public void readChildren(FreeColXMLReader xr)
            throws XMLStreamException {
            while (xr.moreTags()) {
                readChild(xr);
            }
        }

        private void readChild(FreeColXMLReader xr) throws XMLStreamException {
            final String tag = xr.getLocalName();

            boolean recursive = xr.getAttribute(RECURSIVE_TAG, true);

            if (OptionGroup.TAG.equals(tag)) {
                String id = xr.readId();
                OptionGroup group = allOptionGroups.get(id);
                if (group == null) {
                    group = new OptionGroup(id, Specification.this);
                    allOptionGroups.put(id, group);
                }
                group.readFromXML(xr);
                Specification.this.addOptionGroup(group, recursive);

            } else {
                logger.warning(OptionGroup.TAG + " expected in OptionReader"
                    + ", not: " + tag);
                xr.nextTag();
            }
        }
    }

    /** A reader for ordinary spec object types. */
    private class TypeReader<T extends FreeColSpecObjectType>
        implements ChildReader {

        /** The class of objects to read. */
        private final Class<T> type;

        /** The list to append new objects to. */
        private final List<T> result;

        /** Internal index to impose an ordering. */
        private int index = 0;

        /**
         * Build a type read.
         *
         * @param type The class to read.
         * @param listToFill A list of read types.
         */
        public TypeReader(Class<T> type, List<T> listToFill) {
            result = listToFill;
            this.type = type;
        }


        /**
         * {@inheritDoc}
         */
        @SuppressFBWarnings(value="DLC_DUBIOUS_LIST_COLLECTION",
                            justification="List required externally")
        @Override
        public void readChildren(FreeColXMLReader xr)
            throws XMLStreamException {
            while (xr.moreTags()) {
                final String tag = xr.getLocalName();
                String id = xr.readId();
                if (id == null) {
                    logger.warning("Null identifier, tag: " + tag);

                } else if (FreeColSpecObjectType.DELETE_TAG.equals(tag)) {
                    FreeColSpecObjectType object = removeType(id);
                    if (object != null) {
                        result.remove(object);
                    } else {
                        logger.warning("Delete " + id + " failed");
                    }

                } else {
                    T object = getType(id, type);
                    if (object == null) {
                        object = newType(id, type);
                        addType(id, object);
                    }
                    // If this an existing object (with id) and the
                    // PRESERVE tag is present, then leave the
                    // attributes intact and only read the child
                    // elements, otherwise do a full attribute
                    // inclusive read.  This allows mods and spec
                    // extensions to not have to re-specify all the
                    // attributes when just changing the children.
                    if (object.getId() != null
                        && xr.getAttribute(FreeColSpecObjectType.PRESERVE_TAG,
                                           false)) {
                        object.readChildren(xr);
                    } else {
                        object.readFromXML(xr);
                    }
                    if (!object.isAbstractType() && !result.contains(object)) {
                        result.add(object);
                        object.setIndex(index);
                        index++;
                    }
                }
            }
        }
    }

    /** Sources.  Special static spec objects. */
    public static class Source extends FreeColSpecObjectType {

        /**
         * Trivial constructor.
         *
         * @param id The object identifier.
         */
        public Source(String id) {
            super(id);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void toXML(FreeColXMLWriter xw) {
            throw new RuntimeException("Can not happen: " + this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getXMLTagName() { return "source"; }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return getId();
        }
    };

    public static final Source AMBUSH_BONUS_SOURCE
        = new Source("model.source.ambushBonus");
    public static final Source AMPHIBIOUS_ATTACK_PENALTY_SOURCE
        = new Source("model.source.amphibiousAttack");
    public static final Source ARTILLERY_PENALTY_SOURCE
        = new Source("model.source.artilleryInTheOpen");
    public static final Source ATTACK_BONUS_SOURCE
        = new Source("model.source.attackBonus");
    public static final Source BASE_DEFENCE_SOURCE
        = new Source("model.source.baseDefence");
    public static final Source BASE_OFFENCE_SOURCE
        = new Source("model.source.baseOffence");
    public static final Source CARGO_PENALTY_SOURCE
        = new Source("model.source.cargoPenalty");
    public static final Source COLONY_GOODS_PARTY_SOURCE
        = new Source("model.source.colonyGoodsParty");
    public static final Source FORTIFICATION_BONUS_SOURCE
        = new Source("model.source.fortified");
    public static final Source INDIAN_RAID_BONUS_SOURCE
        = new Source("model.source.artilleryAgainstRaid");
    public static final Source MOVEMENT_PENALTY_SOURCE
        = new Source("model.source.movementPenalty");
    public static final Source SHIP_TRADE_PENALTY_SOURCE
        = new Source("model.source.shipTradePenalty");
    public static final Source SOL_MODIFIER_SOURCE
        = new Source("model.source.solModifier");
    /** All the special static sources. */
    private static final Source[] sources = new Source[] {
        MOVEMENT_PENALTY_SOURCE,
        ARTILLERY_PENALTY_SOURCE,
        ATTACK_BONUS_SOURCE,
        FORTIFICATION_BONUS_SOURCE,
        INDIAN_RAID_BONUS_SOURCE,
        AMPHIBIOUS_ATTACK_PENALTY_SOURCE,
        BASE_OFFENCE_SOURCE,
        BASE_DEFENCE_SOURCE,
        CARGO_PENALTY_SOURCE,
        AMBUSH_BONUS_SOURCE,
        COLONY_GOODS_PARTY_SOURCE,
        SHIP_TRADE_PENALTY_SOURCE,
        SOL_MODIFIER_SOURCE
    };

    // @compat 0.10.x/0.11.x
    /** A map of default nation colours. */
    private static final Map<String, Color> defaultColors = new HashMap<>();
    static {
        defaultColors.put("model.nation.dutch",         new Color(0xff9d3c));
        defaultColors.put("model.nation.french",        new Color(0x0000ff));
        defaultColors.put("model.nation.english",       new Color(0xff0000));
        defaultColors.put("model.nation.spanish",       new Color(0xffff00));
        defaultColors.put("model.nation.inca",          new Color(0xf4f0c4));
        defaultColors.put("model.nation.aztec",         new Color(0xc4a020));
        defaultColors.put("model.nation.arawak",        new Color(0x6888c0));
        defaultColors.put("model.nation.cherokee",      new Color(0x6c3c18));
        defaultColors.put("model.nation.iroquois",      new Color(0x74a44c));
        defaultColors.put("model.nation.sioux",         new Color(0xc0ac84));
        defaultColors.put("model.nation.apache",        new Color(0x900000));
        defaultColors.put("model.nation.tupi",          new Color(0x045c04));
        defaultColors.put("model.nation.dutchREF",      new Color(0xcc5500));
        defaultColors.put("model.nation.frenchREF",     new Color(0x6050dc));
        defaultColors.put("model.nation.englishREF",    new Color(0xde3163));
        defaultColors.put("model.nation.spanishREF",    new Color(0xffdf00));
        defaultColors.put("model.nation.portuguese",    new Color(0x00ff00));
        defaultColors.put("model.nation.swedish",       new Color(0x00bfff));
        defaultColors.put("model.nation.danish",        new Color(0xff00bf));
        defaultColors.put("model.nation.russian",       new Color(0xffffff));
        defaultColors.put("model.nation.portugueseREF", new Color(0xbfff00));
        defaultColors.put("model.nation.swedishREF",    new Color(0x367588));
        defaultColors.put("model.nation.danishREF",     new Color(0x91006d));
        defaultColors.put("model.nation.russianREF",    new Color(0xbebebe));
        defaultColors.put(Nation.UNKNOWN_NATION_ID,     Nation.UNKNOWN_NATION_COLOR);
    }
    // end @compat 0.10.x/0.11.x

    public static final String TAG = "freecol-specification";

    /** The difficulty levels option group is special. */
    public static final String DIFFICULTY_LEVELS = "difficultyLevels";

    /** Roles backward compatibility fragment. */
    public static final String ROLES_COMPAT_FILE_NAME = "roles-compat.xml";

    /** Unit change types backward compatibility fragment. */
    public static final String UNIT_CHANGE_TYPES_COMPAT_FILE_NAME
        = "unit-change-types-compat.xml";

    /** The default food type. */
    private static final String DEFAULT_FOOD_TYPE = "model.goods.food";

    /** The default nation type, which does nothing special. */
    private static final String DEFAULT_NATION_TYPE
        = "model.nationType.default";

    /** The default role. */
    public static final String DEFAULT_ROLE_ID = "model.role.default";

    /** How many game ages. */
    public static final int NUMBER_OF_AGES = 3;

    /** The option groups to save. */
    private static final String[] coreOptionGroups = {
        GameOptions.TAG, MapGeneratorOptions.TAG, DIFFICULTY_LEVELS
    };

    /** A map from specification object group identifier to a reader for it. */
    private final Map<String, ChildReader> readerMap = new HashMap<>(20);

    // Containers filled from readers in the readerMap
    // readerMap("building-types")
    private final List<BuildingType> buildingTypeList = new ArrayList<>();
    // readerMap("disasters")
    private final List<Disaster> disasters = new ArrayList<>();
    // readerMap("european-nation-types")
    private final List<EuropeanNationType> europeanNationTypes = new ArrayList<>();
    // readerMap("events")
    private final List<Event> events = new ArrayList<>();
    // readerMap("founding-fathers")
    private final List<FoundingFather> foundingFathers = new ArrayList<>();
    // readerMap("goods-types")
    private final List<GoodsType> goodsTypeList = new ArrayList<>();
    // readerMap("indian-nation-types")
    private final List<IndianNationType> indianNationTypes = new ArrayList<>();
    // readerMap("nations")
    private final List<Nation> nations = new ArrayList<>();
    // readerMap("resource-types")
    private final List<ResourceType> resourceTypeList = new ArrayList<>();
    // readerMap("roles")
    private final List<Role> roles = new ArrayList<>();
    // readerMap("tile-types")
    private final List<TileType> tileTypeList = new ArrayList<>();
    // readerMap("tile-improvement-types")
    private final List<TileImprovementType> tileImprovementTypeList = new ArrayList<>();
    // readerMap("unit-change-types")
    private final List<UnitChangeType> unitChangeTypeList = new ArrayList<>();
    // readerMap("unit-types")
    private final List<UnitType> unitTypeList = new ArrayList<>();

    // readerMap("modifiers")
    private final Map<String, List<Modifier>> allModifiers = new HashMap<>();
    private final List<Modifier> specialModifiers = new ArrayList<>();

    // readerMap("options")
    private final Map<String, AbstractOption> allOptions = new HashMap<>();
    private final Map<String, OptionGroup> allOptionGroups = new HashMap<>();

    /* Containers derived from readerMap containers */

    // Derived from readerMap container: goodsTypeList
    private final List<GoodsType> storableGoodsTypeList = new ArrayList<>();
    private final List<GoodsType> farmedGoodsTypeList = new ArrayList<>();
    private final List<GoodsType> foodGoodsTypeList = new ArrayList<>();
    private final List<GoodsType> newWorldGoodsTypeList = new ArrayList<>();
    private final List<GoodsType> newWorldLuxuryGoodsTypeList = new ArrayList<>();
    private final List<GoodsType> libertyGoodsTypeList = new ArrayList<>();
    private final List<GoodsType> immigrationGoodsTypeList = new ArrayList<>();
    private final List<GoodsType> rawBuildingGoodsTypeList = new ArrayList<>();

    // Derived from readerMap container: nations
    private final List<Nation> europeanNations = new ArrayList<>();
    private final List<Nation> REFNations = new ArrayList<>();
    private final List<Nation> indianNations = new ArrayList<>();
    // Derived from readerMap containers: indianNationTypes europeanNationTypes
    private final List<NationType> nationTypes = new ArrayList<>();
    // Derived from readerMap container: europeanNationTypes
    private final List<EuropeanNationType> REFNationTypes = new ArrayList<>();

    // Derived from readerMap container: unitTypeList
    private final ArrayList<UnitType> buildableUnitTypes = new ArrayList<>();
    private final Map<GoodsType, UnitType> experts = new HashMap<>();
    private final List<UnitType> unitTypesTrainedInEurope = new ArrayList<>();
    private final List<UnitType> unitTypesPurchasedInEurope = new ArrayList<>();
    private UnitType fastestLandUnitType = null;
    private UnitType fastestNavalUnitType = null;
    private final List<UnitType> defaultUnitTypes = new ArrayList<>();

    // Other containers
    /** All the FreeColSpecObjectType objects, indexed by identifier. */
    private final Map<String, FreeColSpecObjectType> allTypes = new HashMap<>(256);

    /** All the abilities by identifier. */
    private final Map<String, List<Ability>> allAbilities = new HashMap<>(128);

    /** A cache of the military roles in decreasing order.  Do not serialize. */
    private List<Role> militaryRoles = null;

    private boolean initialized = false;

    /** The specification identifier. */
    private String id;

    /** The specification version. */
    private String version;

    /** The name of the difficulty level option group. */
    private String difficultyLevel = null;

    /** The turn number for the game ages for FF recruitment. */
    private final int[] ages = new int[NUMBER_OF_AGES];


    /**
     * Creates a new Specification object.
     */
    public Specification() {
        logger.fine("Initializing Specification");
        for (Source source : sources) addType(source.getId(), source);

        readerMap.put(BUILDING_TYPES_TAG,
                      new TypeReader<>(BuildingType.class, buildingTypeList));
        readerMap.put(DISASTERS_TAG,
                      new TypeReader<>(Disaster.class, disasters));
        readerMap.put(EUROPEAN_NATION_TYPES_TAG,
                      new TypeReader<>(EuropeanNationType.class, europeanNationTypes));
        readerMap.put(EVENTS_TAG,
                      new TypeReader<>(Event.class, events));
        readerMap.put(FOUNDING_FATHERS_TAG,
                      new TypeReader<>(FoundingFather.class, foundingFathers));
        readerMap.put(GOODS_TYPES_TAG,
                      new TypeReader<>(GoodsType.class, goodsTypeList));
        readerMap.put(INDIAN_NATION_TYPES_TAG,
                      new TypeReader<>(IndianNationType.class, indianNationTypes));
        readerMap.put(NATIONS_TAG,
                      new TypeReader<>(Nation.class, nations));
        readerMap.put(RESOURCE_TYPES_TAG,
                      new TypeReader<>(ResourceType.class, resourceTypeList));
        readerMap.put(ROLES_TAG,
                      new TypeReader<>(Role.class, roles));
        readerMap.put(TILE_TYPES_TAG,
                      new TypeReader<>(TileType.class, tileTypeList));
        readerMap.put(TILE_IMPROVEMENT_TYPES_TAG,
                      new TypeReader<>(TileImprovementType.class, tileImprovementTypeList));
        // @compat 0.11.3
        readerMap.put(OLD_TILEIMPROVEMENT_TYPES_TAG,
                      new TypeReader<>(TileImprovementType.class, tileImprovementTypeList));
        // end @compat 0.11.3
        readerMap.put(UNIT_CHANGE_TYPES_TAG,
                      new TypeReader<>(UnitChangeType.class, unitChangeTypeList));
        readerMap.put(UNIT_TYPES_TAG,
                      new TypeReader<>(UnitType.class, unitTypeList));

        readerMap.put(MODIFIERS_TAG, new ModifierReader());
        readerMap.put(OPTIONS_TAG, new OptionReader());
    }


    /**
     * Creates a new Specification object by loading it from the
     * given {@code FreeColXMLReader}.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem with the stream.
     */
    public Specification(FreeColXMLReader xr) throws XMLStreamException {
        this();
        initialized = false;
        readFromXML(xr);
        prepare(null, difficultyLevel);
        clean("load from stream");
        initialized = true;
    }

    /**
     * Creates a new Specification object by loading it from the
     * given {@code InputStream}.
     *
     * @param in The {@code InputStream} to read from.
     * @exception XMLStreamException if there is a problem with the stream.
     */
    public Specification(InputStream in) throws XMLStreamException {
        this();
        initialized = false;
        load(in);
        prepare(null, difficultyLevel);
        clean("load from InputStream");
        initialized = true;
    }

    /**
     * Load a specification or fragment from a stream.
     *
     * @param in The {@code InputStream} to read from.
     * @exception XMLStreamException if there is a problem with the stream.
     */
    private void load(InputStream in) throws XMLStreamException {
        try (
            FreeColXMLReader xr = new FreeColXMLReader(in);
        ) {
            xr.nextTag();
            readFromXML(xr);
        }
    }

    /**
     * Load mods into this specification.
     *
     * @param mods A list of {@code FreeColModFile}s for the active mods.
     * @return True if any mod was loaded.
     */
    public boolean loadMods(List<FreeColModFile> mods) {
        initialized = false;
        boolean loadedMod = false;
        for (FreeColModFile mod : mods) {
            InputStream sis = null;
            try {
                if ((sis = mod.getSpecificationInputStream()) != null) {
                    // Some mods are resource only
                    load(sis);
                }
                loadedMod = true;
                logger.info("Loaded mod " + mod.getId());
            } catch (IOException|XMLStreamException ex) {
                logger.log(Level.WARNING, "Read error in mod " + mod.getId(),
                    ex);
            } catch (RuntimeException rte) {
                logger.log(Level.WARNING, "Parse error in mod " + mod.getId(),
                    rte);
            }
        }
        if (loadedMod) clean("mod loading");
        initialized = true;
        return loadedMod;
    }

    /**
     * Prepare a specification with given advantages and difficulty level.
     *
     * @param advantages An optional {@code Advantages} setting.
     * @param difficulty An optional identifier for the difficulty level.
     */
    public void prepare(Advantages advantages, String difficulty) {
        prepare(advantages, (difficulty == null) ? null
            : getDifficultyOptionGroup(difficulty));
    }

    /**
     * Prepare a specification with given advantages and difficulty level.
     *
     * @param advantages An optional {@code Advantages} setting.
     * @param difficulty An optional difficulty level {@code OptionGroup}.
     */
    public void prepare(Advantages advantages, OptionGroup difficulty) {
        applyFixes();
        if (advantages == Advantages.NONE) {
            clearEuropeanNationalAdvantages();
        }
        if (difficulty != null) {
            setDifficultyOptionGroup(difficulty);
            applyDifficultyLevel(difficulty);
        }
    }

    /**
     * Clean up the specification.
     *
     * Builds all the cached containers and secondary variables.  This
     * *must* clear any containers before building as it may be called
     * multiple times in response to various specification updates.
     *
     * @param why A short statement of why the specification needed to
     *     be cleaned.
     */
    public void clean(String why) {
        logger.finest("Cleaning up specification following " + why + ".");

        // Drop all abstract types
        removeInPlace(allTypes, e -> e.getValue().isAbstractType());

        // Fix up the GoodsType derived attributes.  Several GoodsType
        // predicates are likely to fail until this is done.
        GoodsType.setDerivedAttributes(this);

        storableGoodsTypeList.clear();
        farmedGoodsTypeList.clear();
        foodGoodsTypeList.clear();
        newWorldGoodsTypeList.clear();
        newWorldLuxuryGoodsTypeList.clear();
        libertyGoodsTypeList.clear();
        immigrationGoodsTypeList.clear();
        rawBuildingGoodsTypeList.clear();
        for (GoodsType goodsType : goodsTypeList) {
            if (goodsType.isStorable()) {
                storableGoodsTypeList.add(goodsType);
            }
            if (goodsType.isFarmed()) {
                farmedGoodsTypeList.add(goodsType);
            }
            if (goodsType.isFoodType()) {
                foodGoodsTypeList.add(goodsType);
            }
            if (goodsType.isNewWorldGoodsType()) {
                newWorldGoodsTypeList.add(goodsType);
                if (goodsType.isNewWorldLuxuryType()) {
                    newWorldLuxuryGoodsTypeList.add(goodsType);
                }
            }
            if (goodsType.isLibertyType()) {
                libertyGoodsTypeList.add(goodsType);
            }
            if (goodsType.isImmigrationType()) {
                immigrationGoodsTypeList.add(goodsType);
            }
            if (goodsType.isRawBuildingMaterial() && !goodsType.isFoodType()) {
                rawBuildingGoodsTypeList.add(goodsType);
            }
        }

        REFNations.clear();
        europeanNations.clear();
        indianNations.clear();
        for (Nation nation : nations) {
            if (nation.isUnknownEnemy()) continue;
            if (nation.getType().isEuropean()) {
                if (nation.getType().isREF()) {
                    REFNations.add(nation);
                } else {
                    europeanNations.add(nation);
                }
            } else {
                indianNations.add(nation);
            }
        }

        nationTypes.clear();
        nationTypes.addAll(indianNationTypes);
        nationTypes.addAll(europeanNationTypes);
        REFNationTypes.addAll(transform(europeanNationTypes, NationType::isREF));
        europeanNationTypes.removeAll(REFNationTypes);

        experts.clear();
        unitTypesTrainedInEurope.clear();
        unitTypesPurchasedInEurope.clear();
        defaultUnitTypes.clear();
        int bestLandValue = -1, bestNavalValue = -1;
        for (UnitType unitType : unitTypeList) {
            if (unitType.isDefaultUnitType()) defaultUnitTypes.add(unitType);
            if (unitType.needsGoodsToBuild()
                && !unitType.hasAbility(Ability.BORN_IN_COLONY)) {
                buildableUnitTypes.add(unitType);
            }
            if (unitType.getExpertProduction() != null) {
                experts.put(unitType.getExpertProduction(), unitType);
            }
            if (unitType.hasPrice()) {
                if (unitType.getSkill() > 0) {
                    unitTypesTrainedInEurope.add(unitType);
                } else if (!unitType.hasSkill()) {
                    unitTypesPurchasedInEurope.add(unitType);
                }
            }
            if (unitType.isNaval()) {
                if (bestNavalValue < unitType.getMovement()) {
                    bestNavalValue = unitType.getMovement();
                    fastestNavalUnitType = unitType;
                }
            } else {
                if (bestLandValue < unitType.getMovement()) {
                    bestLandValue = unitType.getMovement();
                    fastestLandUnitType = unitType;
                }
            }
        }

        // Initialize UI containers.
        for (OptionGroup og : allOptionGroups.values()) {
            og.generateChoices();
        }

        // Initialize the Turn class using GameOptions and messages.
        Turn.initialize(getInteger(GameOptions.STARTING_YEAR),
                        getInteger(GameOptions.SEASON_YEAR),
                        getInteger(GameOptions.SEASONS));
        boolean badAges = !hasOption(GameOptions.AGES, TextOption.class);
        String agesValue = "";
        if (!badAges) {
            agesValue = getText(GameOptions.AGES);
            String a[] = agesValue.split(",");
            badAges = a.length != NUMBER_OF_AGES-1;
            if (!badAges) {
                try {
                    ages[0] = 1;
                    ages[1] = Turn.yearToTurn(Integer.parseInt(a[0]));
                    ages[2] = Turn.yearToTurn(Integer.parseInt(a[1]));
                    if (ages[1] < 1 || ages[2] < 1) {
                        badAges = true;
                    } else if (ages[1] > ages[2]) {
                        int tmp = ages[1];
                        ages[1] = ages[2];
                        ages[2] = tmp;
                    }
                } catch (NumberFormatException nfe) {
                    badAges = true;
                }
            }
        }
        if (badAges) {
            logger.warning("Bad ages: " + agesValue);
            ages[0] = 1;   // First turn
            ages[1] = Turn.yearToTurn(1600);
            ages[2] = Turn.yearToTurn(1700);
        }

        // Apply the customs on coast restriction
        boolean customsOnCoast = getBoolean(GameOptions.CUSTOMS_ON_COAST);
        for (Ability a : iterable(getBuildingType("model.building.customHouse")
                .getAbilities(Ability.COASTAL_ONLY))) {
            a.setValue(customsOnCoast);
        }

        StringBuilder sb = new StringBuilder(1024);
        sb.append("Specification clean following ").append(why)
            .append(" complete, starting year=").append(Turn.getStartingYear())
            .append(", season year=").append(Turn.getSeasonYear())
            .append(", ages=[").append(ages[0])
            .append(',').append(ages[1])
            .append(',').append(ages[2])
            .append("], seasons=").append(Turn.getSeasonNumber())
            .append(", difficulty=").append(difficultyLevel)
            .append(", ").append(allTypes.size()).append(" Types")
            .append(", ").append(allAbilities.size()).append(" Abilities")
            .append(", ").append(buildingTypeList.size()).append(" BuildingTypes")
            .append(", ").append(disasters.size()).append(" Disasters")
            .append(", ").append(europeanNationTypes.size()).append(" EuropeanNationTypes")
            .append(", ").append(events.size()).append(" Events")
            .append(", ").append(foundingFathers.size()).append(" FoundingFathers")
            .append(", ").append(goodsTypeList.size()).append(" GoodsTypes")
            .append(", ").append(indianNationTypes.size()).append(" IndianNationTypes")
            .append(", ").append(allModifiers.size()).append(" Modifiers")
            .append(", ").append(nations.size()).append(" Nations")
            .append(", ").append(allOptions.size()).append(" Options")
            .append(", ").append(allOptionGroups.size()).append(" Option Groups")
            .append(", ").append(resourceTypeList.size()).append(" ResourceTypes")
            .append(", ").append(roles.size()).append(" Roles")
            .append(", ").append(tileTypeList.size()).append(" TileTypes")
            .append(", ").append(tileImprovementTypeList.size()).append(" TileImprovementTypes")
            .append(", ").append(unitChangeTypeList.size()).append(" UnitChangeTypes")
            .append(", ").append(unitTypeList.size()).append(" UnitTypes")
            .append(" read.");
        logger.info(sb.toString());
    }


    // Basic methods

    /**
     * Get the specification identifier.
     *
     * @return The specification identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the specification version.
     *
     * @return The specification version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get a {@code FreeColSpecObjectType} by id.
     *
     * @param id The identifier to look for.
     * @return The {@code FreeColSpecObjectType} found if any.
     */
    public FreeColSpecObjectType getType(String id) {
        return allTypes.get(id);
    }

    /**
     * Find a {@code FreeColSpecObjectType} by id and class.
     *
     * @param <T> The actual return type.
     * @param id The object identifier to look for.
     * @param returnClass The expected {@code Class}.
     * @return The {@code FreeColSpecObjectType} found if any.
     */
    private <T extends FreeColSpecObjectType> T getType(String id,
                                                        Class<T> returnClass) {
        FreeColSpecObjectType ret = getType(id);
        return (ret == null) ? null : returnClass.cast(ret);
    }

    /**
     * Add a type by identifier.
     *
     * @param id The identifier to associate with.
     * @param type The {@code FreeColSpecObjectType} to add.
     */
    private void addType(String id, FreeColSpecObjectType type) {
        allTypes.put(id, type);
    }

    /**
     * Remove reference to a type.
     *
     * @param id The type identifier to remove.
     * @return The removed {@code FreeColSpecObjectType}.
     */
    private FreeColSpecObjectType removeType(String id) {
        return allTypes.remove(id);
    }
    
    /**
     * Registers an Ability as defined.
     *
     * @param ability an {@code Ability} value
     */
    public void addAbility(Ability ability) {
        String id = ability.getId();
        addAbility(id);
        allAbilities.get(id).add(ability);
    }

    /**
     * Registers an Ability's id as defined.  This is useful for
     * abilities that are required rather than provided by
     * FreeColSpecObjectTypes.
     *
     * @param id The object identifier.
     */
    public void addAbility(String id) {
        if (!allAbilities.containsKey(id)) {
            allAbilities.put(id, new ArrayList<Ability>());
        }
    }

    /**
     * Get all the Abilities with the given identifier.
     *
     * @param id The object identifier to look for.
     * @return A stream of {@code Ability}s.
     */
    public Stream<Ability> getAbilities(String id) {
        List<Ability> result = allAbilities.get(id);
        return (result == null) ? Stream.<Ability>empty() : result.stream();
    }

    /**
     * Add a modifier.
     *
     * @param modifier The {@code Modifier} to add.
     */
    public void addModifier(Modifier modifier) {
        String id = modifier.getId();
        if (!allModifiers.containsKey(id)) {
            allModifiers.put(id, new ArrayList<Modifier>());
        }
        allModifiers.get(id).add(modifier);
    }

    /**
     * Get all the Modifiers with the given identifier.
     *
     * @param id The object identifier to look for.
     * @return A stream of {@code Modifier}s.
     */
    public Stream<Modifier> getModifiers(String id) {
        List<Modifier> result = allModifiers.get(id);
        return (result == null) ? Stream.<Modifier>empty() : result.stream();
    }

    /**
     * Add a father, for test purposes.
     *
     * @param ff The {@code FoundingFather} to add.
     */
    public void addTestFather(FoundingFather ff) {
        addType(ff.getId(), ff);
        foundingFathers.add(ff);
    }

    /**
     * Disable editing of some critical option groups.
     */
    public void disableEditing() {
        for (String s : coreOptionGroups) {
            OptionGroup og = allOptionGroups.get(s);
            if (og != null) og.setEditable(false);
        }
    }

    /**
     * Compare a spec version number with the current one.
     *
     * @param other The other spec version number.
     * @return Positive if the current version is greater than the other,
     *     negative if the current version is lower than the other,
     *     zero if they are equal.
     */
    private int compareVersion(String other) {
        String version = getVersion();
        if (version == null) return 0;

        final String rex = "\\.";
        String[] sv = version.split(rex, 2);
        String[] so = other.split(rex, 2);
        if (sv.length == 2 && so.length == 2) {
            int cmp;
            try {
                cmp = Integer.compare(Integer.parseInt(sv[0]),
                                      Integer.parseInt(so[0]));
                if (cmp != 0) return cmp;
                return Integer.compare(Integer.parseInt(sv[1]),
                                       Integer.parseInt(so[1]));
            } catch (NumberFormatException nfe) {}
        }
        throw new RuntimeException("Bad version: " + other);
    }


    // Option routines including DifficultyLevels which are option groups
    // Interface OptionContainer

    /**
     * {@inheritDoc}
     */
    public <T extends Option> boolean hasOption(String id,
                                                Class<T> returnClass) {
        if (id == null) return false;
        AbstractOption val = this.allOptions.get(id);
        return (val == null) ? false
            : returnClass.isAssignableFrom(val.getClass());
    }

    /**
     * {@inheritDoc}
     */
    public <T extends Option> T getOption(String id,
                                          Class<T> returnClass) {
        if (id == null) {
            throw new RuntimeException("Null identifier for "
                + returnClass.getName());
        } else if (!this.allOptions.containsKey(id)) {
            throw new RuntimeException("Missing option: " + id);
        } else {
            AbstractOption op = this.allOptions.get(id);
            try {
                return returnClass.cast(op);
            } catch (ClassCastException cce) {
                throw new RuntimeException("Not a " + returnClass.getName()
                    + ": " + id, cce);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public OptionGroup getOptionGroup(String id) {
        if (id == null) {
            throw new RuntimeException("OptionGroup with null id: " + this);
        } else if (!this.allOptionGroups.containsKey(id)) {
            throw new RuntimeException("Missing OptionGroup: " + id);
        } else {
            return this.allOptionGroups.get(id);
        }
    }

    /**
     * Adds an {@code OptionGroup} to this specification.
     *
     * @param optionGroup The {@code OptionGroup} to add.
     * @param recursive If true, add recursively to subgroups.
     */
    private void addOptionGroup(OptionGroup optionGroup, boolean recursive) {
        // Add the options of the group
        for (Option option : optionGroup.getOptions()) {
            if (option instanceof OptionGroup) {
                allOptionGroups.put(option.getId(), (OptionGroup)option);
                if (recursive) {
                    addOptionGroup((OptionGroup)option, true);
                }
            } else {
                addAbstractOption((AbstractOption)option);
            }
        }
    }

    /**
     * Adds an {@code AbstractOption} to this specification.
     *
     * @param abstractOption The {@code AbstractOption} to add.
     */
    private void addAbstractOption(AbstractOption abstractOption) {
        // Add the option
        allOptions.put(abstractOption.getId(), abstractOption);
    }

    /**
     * Merge an option group into the spec.
     *
     * @param group The {@code OptionGroup} to merge.
     * @return The merged {@code OptionGroup} from this
     *     {@code Specification}.
     */
    private OptionGroup mergeGroup(OptionGroup group) {
        OptionGroup realGroup = allOptionGroups.get(group.getId());
        if (realGroup == null || !realGroup.isEditable()) return realGroup;

        for (Option o : group.getOptions()) {
            if (o instanceof OptionGroup) {
                mergeGroup((OptionGroup)o);
            } else {
                realGroup.add(o);
            }
        }
        return realGroup;
    }

    /**
     * Gets the difficulty levels in this specification.
     *
     * @return A list of difficulty levels in this specification.
     */
    public List<OptionGroup> getDifficultyLevels() {
        OptionGroup group = allOptionGroups.get(DIFFICULTY_LEVELS);
        Stream<Option> stream = (group == null) ? Stream.<Option>empty()
            : group.getOptions().stream();
        return transform(stream, (Option o) -> o instanceof OptionGroup,
                         o -> (OptionGroup)o);
    }

    /**
     * Get the current difficulty level.
     *
     * @return The difficulty level.
     */
    public String getDifficultyLevel() {
        return this.difficultyLevel;
    }

    /**
     * Gets the current difficulty level options.
     *
     * @return The current difficulty level {@code OptionGroup}.
     */
    public OptionGroup getDifficultyOptionGroup() {
        return getDifficultyOptionGroup(this.difficultyLevel);
    }

    /**
     * Gets difficulty level options by id.
     *
     * @param id The difficulty level identifier to look for.
     * @return The corresponding difficulty level {@code OptionGroup},
     *     if any.
     */
    public OptionGroup getDifficultyOptionGroup(String id) {
        return find(getDifficultyLevels(),
                    matchKeyEquals(id, FreeColObject::getId));
    }

    /**
     * Add/overwrite a difficulty option group.
     *
     * @param difficulty The {@code OptionGroup} to add.
     */
    private void setDifficultyOptionGroup(OptionGroup difficulty) {
        OptionGroup group = allOptionGroups.get(DIFFICULTY_LEVELS);
        if (group != null) group.add(difficulty);
        allOptionGroups.put(difficulty.getId(), difficulty);
    }

    /**
     * Applies the difficulty level identified by the given String to
     * the current specification.
     *
     * @param difficulty The identifier of a difficulty level to apply.
     */
    public void applyDifficultyLevel(String difficulty) {
        applyDifficultyLevel(getDifficultyOptionGroup(difficulty));
    }

    /**
     * Applies the given difficulty level to the current
     * specification.
     *
     * Public for the test suite.
     *
     * @param level The difficulty level {@code OptionGroup} to apply.
     */
    public void applyDifficultyLevel(OptionGroup level) {
        if (level == null) {
            logger.warning("Null difficulty level supplied");
            return;
        }
        logger.config("Applying difficulty level " + level.getId());
        addOptionGroup(level, true);
        this.difficultyLevel = level.getId();
    }

    public OptionGroup getGameOptions() {
        return getOptionGroup(GameOptions.TAG);
    }

    public void setGameOptions(OptionGroup go) {
        allOptionGroups.put(GameOptions.TAG, go);
        addOptionGroup(go, true);
    }

    public OptionGroup getMapGeneratorOptions() {
        return getOptionGroup(MapGeneratorOptions.TAG);
    }

    public void setMapGeneratorOptions(OptionGroup mgo) {
        allOptionGroups.put(MapGeneratorOptions.TAG, mgo);
        addOptionGroup(mgo, true);
    }

    /**
     * Update the game and map options from the user configuration files.
     *
     * @return True if any option was fixed.
     */
    public boolean updateGameAndMapOptions() {
        boolean ret = false;
        String gtag = GameOptions.TAG;
        File gof = FreeColDirectories
            .getOptionsFile(FreeColDirectories.GAME_OPTIONS_FILE_NAME);
        OptionGroup gog = (!gof.exists()) ? null
            : OptionGroup.loadOptionGroup(gof, gtag, this);
        if (gog != null) {
            gog = mergeGroup(gog);
            ret |= fixGameOptions();
        } else {
            gog = getOptionGroup(gtag);
        }
        gog.save(gof, null, true);
        
        String mtag = MapGeneratorOptions.TAG;
        File mof = FreeColDirectories
            .getOptionsFile(FreeColDirectories.MAP_GENERATOR_OPTIONS_FILE_NAME);
        OptionGroup mog = (!mof.exists()) ? null
            : OptionGroup.loadOptionGroup(mof, mtag, this);
        if (mog != null) {
            mog = mergeGroup(mog);
            ret |= fixMapGeneratorOptions();
        } else {
            mog = getOptionGroup(mtag);
        }
        mog.save(mof, null, true);
        return ret;
    }

    /**
     * Generate the dynamic options.
     *
     * Only call this in the server.  If clients call it the European
     * prices can be desynchronized.
     */
    public void generateDynamicOptions() {
        logger.finest("Generating dynamic options.");
        OptionGroup prices = new OptionGroup(GameOptions.GAMEOPTIONS_PRICES, this);
        allOptionGroups.put(prices.getId(), prices);
        for (GoodsType goodsType : goodsTypeList) {
            String name = goodsType.getSuffix("model.goods.");
            String base = "model.option." + name + ".";
            if (goodsType.getInitialSellPrice() > 0) {
                int diff = (goodsType.isNewWorldGoodsType()
                    || goodsType.isNewWorldLuxuryType()) ? 3 : 0;
                IntegerOption minimum
                    = new IntegerOption(base + "minimumPrice", this);
                minimum.setValue(goodsType.getInitialSellPrice());
                minimum.setMinimumValue(1);
                minimum.setMaximumValue(100);
                prices.add(minimum);
                addAbstractOption(minimum);
                IntegerOption maximum
                    = new IntegerOption(base + "maximumPrice", this);
                maximum.setValue(goodsType.getInitialSellPrice() + diff);
                maximum.setMinimumValue(1);
                maximum.setMaximumValue(100);
                prices.add(maximum);
                addAbstractOption(maximum);
                IntegerOption spread
                    = new IntegerOption(base + "spread", this);
                spread.setValue(goodsType.getPriceDifference());
                spread.setMinimumValue(1);
                spread.setMaximumValue(100);
                prices.add(spread);
                addAbstractOption(spread);
            } else if (goodsType.getPrice() < INFINITY) {
                IntegerOption price
                    = new IntegerOption(base + "price", this);
                price.setValue(goodsType.getPrice());
                price.setMinimumValue(1);
                price.setMaximumValue(100);
                prices.add(price);
                addAbstractOption(price);
            }
        }
        getGameOptions().add(prices);
    }

    /**
     * Merge in a new set of game options.
     *
     * @param newGameOptions The new game options {@code OptionGroup}
     *     to merge.
     * @param who Where are we, client or server?
     * @return True if the merge succeeded.
     */
    public boolean mergeGameOptions(OptionGroup newGameOptions, String who) {
        OptionGroup go = getGameOptions();
        LogBuilder lb = new LogBuilder(64);
        boolean ret = go.merge(newGameOptions, lb);
        lb.shrink("\n"); lb.log(logger, Level.FINEST);
        if (!ret) return false;
        addOptionGroup(go, true); // make sure allOptions is seeing any changes
        clean("merged game options (" + who + ")");
        return true;
    }

    /**
     * Merge in a new set of map options.
     *
     * @param newMapGeneratorOptions The new game options
     *     {@code OptionGroup} to merge.
     * @param who Where are we, client or server?
     * @return True if the merge succeeded.
     */
    public boolean mergeMapGeneratorOptions(OptionGroup newMapGeneratorOptions,
                                            String who) {
        OptionGroup go = getMapGeneratorOptions();
        LogBuilder lb = new LogBuilder(64);
        boolean ret = go.merge(newMapGeneratorOptions, lb);
        lb.shrink("\n"); lb.log(logger, Level.FINEST);
        if (!ret) return false;
        addOptionGroup(go, true); // make sure allOptions is seeing any changes
        clean("merged map options (" + who + ")");
        return true;
    }


    // -- Ages --

    /**
     * Gets the age corresponding to a given turn.
     *
     * @param turn The {@code Turn} to check.
     * @return The age of the given turn.
     */
    public int getAge(Turn turn) {
        int n = turn.getNumber();
        return (n < ages[0]) ? -1
            : (n < ages[1]) ? 0
            : (n < ages[2]) ? 1
            : 2;
    }

    // -- Buildables --

    public BuildableType getBuildableType(String id) {
        return getType(id, BuildableType.class);
    }

    // -- Buildings --

    public List<BuildingType> getBuildingTypeList() {
        return buildingTypeList;
    }

    /**
     * Get a building type by identifier.
     *
     * @param id The object identifier.
     * @return The {@code BuildingType} found.
     */
    public BuildingType getBuildingType(String id) {
        return getType(id, BuildingType.class);
    }

    // -- Disasters --

    public List<Disaster> getDisasters() {
        return disasters;
    }

    /**
     * Get a disaster by identifier.
     *
     * @param id The object identifier.
     * @return The {@code Disaster} found.
     */
    public Disaster getDisaster(String id) {
        return getType(id, Disaster.class);
    }

    // -- Events --

    public List<Event> getEvents() {
        return events;
    }

    /**
     * Get an event by identifier.
     *
     * @param id The object identifier.
     * @return The {@code Event} found.
     */
    public Event getEvent(String id) {
        return getType(id, Event.class);
    }

    // -- Founding Fathers --

    public List<FoundingFather> getFoundingFathers() {
        return foundingFathers;
    }

    /**
     * Get a founding father type by identifier.
     *
     * @param id The object identifier.
     * @return The {@code FoundingFather} found.
     */
    public FoundingFather getFoundingFather(String id) {
        return getType(id, FoundingFather.class);
    }

    // -- Goods --

    public List<GoodsType> getGoodsTypeList() {
        return new ArrayList<>(goodsTypeList);
    }

    public List<GoodsType> getStorableGoodsTypeList() {
        return new ArrayList<>(storableGoodsTypeList);
    }

    public List<GoodsType> getFarmedGoodsTypeList() {
        return new ArrayList<>(farmedGoodsTypeList);
    }

    public List<GoodsType> getNewWorldGoodsTypeList() {
        return new ArrayList<>(newWorldGoodsTypeList);
    }

    public List<GoodsType> getNewWorldLuxuryGoodsTypeList() {
        return new ArrayList<>(newWorldLuxuryGoodsTypeList);
    }

    public List<GoodsType> getLibertyGoodsTypeList() {
        return new ArrayList<>(libertyGoodsTypeList);
    }

    public List<GoodsType> getImmigrationGoodsTypeList() {
        return new ArrayList<>(immigrationGoodsTypeList);
    }

    public List<GoodsType> getFoodGoodsTypeList() {
        return new ArrayList<>(foodGoodsTypeList);
    }

    public final List<GoodsType> getRawBuildingGoodsTypeList() {
        return new ArrayList<>(rawBuildingGoodsTypeList);
    }

    /**
     * Get the primary food type.
     *
     * FIXME: The "Food" type is handled as a special case in many
     * places.  This routine was added to collect them into one place,
     * in the hope we can one day deprecate this routine and clean up
     * the special cases.
     *
     * @return The main food type.
     */
    public GoodsType getPrimaryFoodType() {
        return getGoodsType(DEFAULT_FOOD_TYPE);
    }

    /**
     * Get the initial <em>minimum</em> price of the given goods
     * type. The initial price in a particular Market may be higher.
     *
     * @param goodsType The {@code GoodsType} to check.
     * @return The minimum price.
     */
    public int getInitialPrice(GoodsType goodsType) {
        String suffix = goodsType.getSuffix("model.goods.");
        String minPrice = "model.option." + suffix + ".minimumPrice";
        String maxPrice = "model.option." + suffix + ".maximumPrice";
        return (hasOption(minPrice, IntegerOption.class)
            && hasOption(maxPrice, IntegerOption.class))
            ? Math.min(getInteger(minPrice), getInteger(maxPrice))
            : goodsType.getInitialSellPrice();
    }

    /**
     * Get a goods type by identifier.
     *
     * @param id The object identifier.
     * @return The {@code GoodsType} found.
     */
    public GoodsType getGoodsType(String id) {
        return getType(id, GoodsType.class);
    }

    // -- Improvements --

    public List<TileImprovementType> getTileImprovementTypeList() {
        return tileImprovementTypeList;
    }

    /**
     * Get a tile improvement type by identifier.
     *
     * @param id The object identifier.
     * @return The {@code TileImprovementType} found.
     */
    public TileImprovementType getTileImprovementType(String id) {
        return getType(id, TileImprovementType.class);
    }

    // -- NationTypes --

    public List<NationType> getNationTypes() {
        return nationTypes;
    }

    public List<EuropeanNationType> getEuropeanNationTypes() {
        return europeanNationTypes;
    }

    public List<EuropeanNationType> getREFNationTypes() {
        return REFNationTypes;
    }

    public List<IndianNationType> getIndianNationTypes() {
        return indianNationTypes;
    }

    /**
     * Get a nation type by identifier.
     *
     * @param id The object identifier.
     * @return The {@code NationType} found.
     */
    public NationType getNationType(String id) {
        return getType(id, NationType.class);
    }

    public NationType getDefaultNationType() {
        return getNationType(DEFAULT_NATION_TYPE);
    }


    // -- Nations --

    public List<Nation> getNations() {
        return nations;
    }

    public List<Nation> getEuropeanNations() {
        return europeanNations;
    }

    public List<Nation> getIndianNations() {
        return indianNations;
    }

    public List<Nation> getREFNations() {
        return REFNations;
    }

    /**
     * Get a nation by identifier.
     *
     * @param id The object identifier.
     * @return The {@code Nation} found.
     */
    public Nation getNation(String id) {
        return getType(id, Nation.class);
    }

    /**
     * Get the special unknown enemy nation.
     *
     * @return The unknown enemy {@code Nation}.
     */
    public Nation getUnknownEnemyNation() {
        return getNation(Nation.UNKNOWN_NATION_ID);
    }

    /**
     * Clear all European advantages.  Implements the Advantages==NONE setting.
     */
    public void clearEuropeanNationalAdvantages() {
        for (Nation n : getEuropeanNations()) {
            n.setType(getDefaultNationType());
        }
    }

    // -- Resources --

    public List<ResourceType> getResourceTypeList() {
        return resourceTypeList;
    }

    /**
     * Get a resource type by identifier.
     *
     * @param id The object identifier.
     * @return The {@code ResourceType} found.
     */
    public ResourceType getResourceType(String id) {
        return getType(id, ResourceType.class);
    }

    // -- Roles --

    /**
     * Get all the available roles.
     *
     * @return A list of available {@code Role}s.
     */
    public List<Role> getRolesList() {
        return this.roles;
    }

    /**
     * Get all the available roles as a stream.
     *
     * @return A stream of available {@code Role}s.
     */
    public Stream<Role> getRoles() {
        return getRolesList().stream();
    }

    /**
     * Get a role by identifier.
     *
     * @param id The object identifier.
     * @return The {@code Role} found.
     */
    public Role getRole(String id) {
        return getType(id, Role.class);
    }

    /**
     * Get the default role.
     *
     * @return The default {@code Role}.
     */
    public Role getDefaultRole() {
        return getRole(DEFAULT_ROLE_ID);
    }

    /**
     * Get the military roles in this specification, in decreasing order
     * of effectiveness.
     *
     * @return An unmodifiable list of military {@code Role}s.
     */
    public List<Role> getMilitaryRolesList() {
        if (this.militaryRoles == null) {
            this.militaryRoles = Collections.<Role>unmodifiableList(
                transform(this.roles, Role::isOffensive,
                          Function.<Role>identity(),
                          Role.militaryComparator));
        }
        return this.militaryRoles;
    }

    /**
     * Get the available military roles as a stream.
     *
     * @return A stream of military {@code Role}s.
     */
    public Stream<Role> getMilitaryRoles() {
        return getMilitaryRolesList().stream();
    }

    /**
     * Gets the roles suitable for a REF unit.
     *
     * @param naval If true, choose roles for naval units, if not, land units.
     * @return A list of {@code Role}s suitable for REF units.
     */
    public List<Role> getREFRolesList(boolean naval) {
        return transform(((naval) ? Stream.of(getDefaultRole())
                             : getMilitaryRoles()),
                         r -> r.requiresAbility(Ability.REF_UNIT));
    }

    /**
     * Gets the roles suitable for a REF unit as a stream.
     *
     * @param naval If true, choose roles for naval units, if not, land units.
     * @return A stream of {@code Role}s suitable for REF units.
     */
    public Stream<Role> getREFRoles(boolean naval) {
        return getREFRolesList(naval).stream();
    }

    /**
     * Get a role with an ability.
     *
     * @param id The ability identifier to look for.
     * @param roles An optional list of {@code Role}s to look in,
     *     if null all roles are used.
     * @return The first {@code Role} found with the required
     *     ability, or null if none found.
     */
    public Role getRoleWithAbility(String id, List<Role> roles) {
        return find(getRoles(), r -> r.hasAbility(id));
    }

    /**
     * Get the missionary role.
     *
     * @return The missionary {@code Role}.
     */
    public Role getMissionaryRole() {
        return getRoleWithAbility(Ability.ESTABLISH_MISSION, null);
    }

    /**
     * Get the pioneer role.
     *
     * @return The pioneer {@code Role}.
     */
    public Role getPioneerRole() {
        return getRoleWithAbility(Ability.IMPROVE_TERRAIN, null);
    }

    /**
     * Get the scout role.
     *
     * @return The scout {@code Role}.
     */
    public Role getScoutRole() {
        return getRoleWithAbility(Ability.SPEAK_WITH_CHIEF, null);
    }

    // -- Tiles --

    public List<TileType> getTileTypeList() {
        return tileTypeList;
    }

    /**
     * Get a tile type by identifier.
     *
     * @param id The object identifier.
     * @return The {@code TileType} found.
     */
    public TileType getTileType(String id) {
        return getType(id, TileType.class);
    }

    // -- UnitChangeTypes --

    /**
     * Get the list of all unit change types.
     *
     * @return The unit change type list.
     */
    public List<UnitChangeType> getUnitChangeTypeList() {
        return unitChangeTypeList;
    }

    /**
     * Get a specific type of unit change type.
     *
     * Suitable indexing constants are in UnitChangeType.
     *
     * @param id The identifier for the required change type.
     * @return The {@code UnitChangeType} found, or null if none present.
     */
    public UnitChangeType getUnitChangeType(String id) {
        return find(unitChangeTypeList,
                    matchKeyEquals(id, UnitChangeType::getId));
    }

    /**
     * Get a specific unit change for a given unit change type and
     * source unit type to change.
     *
     * Suitable indexing constants are in UnitChangeType.
     *
     * @param id The identifier for the required change type.
     * @param fromType The {@code UnitType} to change.
     * @return A list of {@code UnitChange}s.
     */
    public List<UnitTypeChange> getUnitChanges(String id, UnitType fromType) {
        UnitChangeType uct = getUnitChangeType(id);
        return (uct == null) ? Collections.<UnitTypeChange>emptyList()
            : uct.getUnitChanges(fromType);
    }

    /**
     * Get a specific unit change for a given unit change type, a
     * source unit type to change, and a destination unit type.
     *
     * Suitable indexing constants are in UnitChangeType.
     *
     * @param id The identifier for the required change type.
     * @param fromType The {@code UnitType} to change from.
     * @return The {@code UnitChange} found, or null if the
     *     change is impossible.
     */
    public UnitTypeChange getUnitChange(String id, UnitType fromType) {
        return getUnitChange(id, fromType, null);
    }

    /**
     * Get a specific unit change for a given unit change type, a
     * source unit type to change, and a destination unit type.
     *
     * Suitable indexing constants are in UnitChangeType.
     *
     * @param id The identifier for the required change type.
     * @param fromType The {@code UnitType} to change from.
     * @param toType The {@code UnitType} to change to.
     * @return The {@code UnitChange} found, or null if the
     *     change is impossible.
     */
    public UnitTypeChange getUnitChange(String id, UnitType fromType,
                                        UnitType toType) {
        UnitChangeType uct = getUnitChangeType(id);
        return (uct == null) ? null
            : uct.getUnitChange(fromType, toType);
    }

    /**
     * Gets the number of turns a unit has to train to educate a student.
     * This value is only meaningful for units that can be put in a school.
     *
     * @param typeTeacher The teacher {@code UnitType}.
     * @param typeStudent the student {@code UnitType}.
     * @return The turns of training needed.
     */
    public int getNeededTurnsOfTraining(UnitType typeTeacher,
                                        UnitType typeStudent) {
        UnitType learn = typeStudent.getTeachingType(typeTeacher);
        if (learn == null) {
            throw new RuntimeException("Can not learn: teacher=" + typeTeacher
                + " student=" + typeStudent);
        }
        return getUnitChange(UnitChangeType.EDUCATION, typeStudent, learn).turns;
    }

    // -- Units --

    public List<UnitType> getUnitTypeList() {
        return unitTypeList;
    }

    /**
     * Get the most vanilla unit type for a given player.
     *
     * @param player The {@code Player} to find the default
     *     unit type for, or null indicating a normal player nation
     *     (i.e. non-REF European).
     * @return The default unit type.
     */
    public UnitType getDefaultUnitType(Player player) {
        return (player == null) ? getDefaultUnitType()
            : getDefaultUnitType(player.getNationType());
    }

    /**
     * Get the most vanilla unit type for a type of nation.
     *
     * Provides a type to use to make a neutral comparison of the
     * productivity of work locations.
     *
     * @param nationType The {@code NationType} to find the default
     *     unit type for, or null indicating a normal player nation
     *     (i.e. non-REF European).
     * @return The free colonist unit type.
     */
    public UnitType getDefaultUnitType(NationType nationType) {
        final Predicate<UnitType> p = (nationType == null)
            ? ut -> !ut.hasAbility(Ability.BORN_IN_INDIAN_SETTLEMENT)
                && !ut.hasAbility(Ability.REF_UNIT)
            : (nationType.isIndian())
            ? ut -> ut.hasAbility(Ability.BORN_IN_INDIAN_SETTLEMENT)
                && !ut.hasAbility(Ability.REF_UNIT)
            : (nationType.isREF())
            ? ut -> !ut.hasAbility(Ability.BORN_IN_INDIAN_SETTLEMENT)
                && ut.hasAbility(Ability.REF_UNIT)
            : ut -> !ut.hasAbility(Ability.BORN_IN_INDIAN_SETTLEMENT)
                && !ut.hasAbility(Ability.REF_UNIT);
        return find(defaultUnitTypes, p, getDefaultUnitType());
    }

    /**
     * Get the most vanilla unit type.
     *
     * @return The default unit type.
     */
    public UnitType getDefaultUnitType() {
        return getUnitType("model.unit.freeColonist"); // Drop this soon
    }

    /**
     * Get the list of buildable unit types.
     *
     * @return The list of buildable unit types.
     */
    public List<UnitType> getBuildableUnitTypes() {
        return buildableUnitTypes;
    }

    /**
     * Get the unit type that is the expert for producing a type of goods.
     *
     * @param goodsType The {@code GoodsType} to check.
     * @return The expert {@code UnitType}, or null if none.
     */
    public UnitType getExpertForProducing(GoodsType goodsType) {
        return experts.get(goodsType);
    }

    /**
     * Get the unit types which have any of the given abilities
     *
     * @param abilities The abilities for the search
     * @return A list of {@code UnitType}s with the abilities.
     */
    public List<UnitType> getUnitTypesWithAbility(String... abilities) {
        return getTypesWithAbility(UnitType.class, abilities);
    }

    /**
     * Get the unit types which have none of the given abilities
     *
     * @param abilities The abilities for the search
     * @return A list of {@code UnitType}s without the abilities.
     */
    public List<UnitType> getUnitTypesWithoutAbility(String... abilities) {
        return getTypesWithoutAbility(UnitType.class, abilities);
    }

    /**
     * Gets the unit types that can be trained in Europe.
     *
     * @return A list of Europe-trainable {@code UnitType}s.
     */
    public List<UnitType> getUnitTypesTrainedInEurope() {
        return unitTypesTrainedInEurope;
    }

    /**
     * Get the unit types that can be purchased in Europe.
     *
     * @return A list of Europe-purchasable {@code UnitType}s.
     */
    public List<UnitType> getUnitTypesPurchasedInEurope() {
        return unitTypesPurchasedInEurope;
    }

    /**
     * Gets the fastest land unit type in this specification.
     *
     * @return The fastest land unit type.
     */
    public UnitType getFastestLandUnitType() {
        return fastestLandUnitType;
    }

    /**
     * Gets the fastest naval unit type in this specification.
     *
     * @return The fastest naval unit type.
     */
    public UnitType getFastestNavalUnitType() {
        return fastestNavalUnitType;
    }

    /**
     * Gets the REF unit types.
     *
     * @param naval If true, choose naval units, if not, land units.
     * @return A list of {@code UnitType}s allowed for the REF.
     */
    public List<UnitType> getREFUnitTypes(boolean naval) {
        return transform(getUnitTypesWithAbility(Ability.REF_UNIT),
                         matchKey(naval, UnitType::isNaval));
    }

    /**
     * Get a unit type by identifier.
     *
     * @param id The object identifier.
     * @return The {@code UnitType} found.
     */
    public UnitType getUnitType(String id) {
        return getType(id, UnitType.class);
    }

    // General type retrieval

    /**
     * Find the {@code FreeColSpecObjectType} with the given identifier.
     *
     * @param <T> The actual return type.
     * @param id The object identifier to look for.
     * @param returnClass The expected {@code Class}.
     * @return The {@code FreeColSpecObjectType} found.
     */
    public <T extends FreeColSpecObjectType> T findType(String id,
                                                        Class<T> returnClass) {
        T o = getType(id, returnClass);
        if (o != null) return o;

        if (initialized) {
            throw new RuntimeException("Undefined FCGOT: " + id);
        }

        // forward declaration of new type
        o = newType(id, returnClass);
        addType(id, o);
        return o;
    }

    /**
     * Build a new {@code FreeColSpecObjectType} with given identifier
     * and class.
     *
     * @param <T> The actual return type.
     * @param id The identifier to look for.
     * @param returnClass The expected {@code Class}.
     * @return The new {@code FreeColSpecObjectType} or null on error.
     */
    private <T extends FreeColSpecObjectType> T newType(String id,
                                                        Class<T> returnClass) {
        try {
            return Introspector.instantiate(returnClass, newTypeClasses,
                                            new Object[] { id, this });
        } catch (Introspector.IntrospectorException ie) {
            logger.log(Level.WARNING, "newType(" + id
                + "," + returnClass.getName() + ") failed", ie);
        }
        return null;
    }

    /**
     * Get the FreeColSpecObjectTypes that provide the required ability.
     *
     * @param id The object identifier.
     * @param value The ability value to check.
     * @return A list of {@code FreeColSpecObjectType}s that
     *     provide the required ability.
     */
    public List<FreeColSpecObjectType> getTypesProviding(String id,
                                                         boolean value) {
        return transform(getAbilities(id),
                         a -> a.getValue() == value
                             && a.getSource() instanceof FreeColSpecObjectType,
                         a -> (FreeColSpecObjectType)a.getSource());
    }

    /**
     * Get all types which have any of the given abilities.
     *
     * @param <T> The actual return type.
     * @param resultType The expected result type.
     * @param abilities The abilities for the search.
     * @return A list of {@code FreeColSpecObjectType}s with at
     *     least one of the given abilities.
     */
    public <T extends FreeColSpecObjectType> List<T>
                      getTypesWithAbility(Class<T> resultType,
                                          String... abilities) {
        return transform(allTypes.values(),
                         type -> resultType.isInstance(type)
                             && any(abilities, a -> type.hasAbility(a)),
                         type -> resultType.cast(type));
    }

    /**
     * Get all types which have none of the given abilities.
     *
     * @param <T> The actual return type.
     * @param resultType The expected result type.
     * @param abilities The abilities for the search
     * @return A list of {@code FreeColSpecObjectType}s without the
     *     given abilities.
     */
    public <T extends FreeColSpecObjectType> List<T>
                      getTypesWithoutAbility(Class<T> resultType,
                                             String... abilities) {
        return transform(allTypes.values(),
                         type -> resultType.isInstance(type)
                             && none(abilities, a -> type.hasAbility(a)),
                         type -> resultType.cast(type));
    }


    // Backward compatibility fixes.
    // We do not pretend to fix everything, some specs are just too broken,
    // or some changes too radical.  But we make an effort.

    /**
     * Apply all the special fixes to bring older specifications up to date.
     *
     * @return True if a change was made.
     */
    private boolean applyFixes() {
        boolean ret = false;
        ret |= fixDifficultyOptions();
        ret |= fixGameOptions();
        ret |= fixMapGeneratorOptions();
        ret |= fixOrphanOptions();
        // @compat 0.11.0
        ret |= fixRoles();
        // end @compat 0.11.0
        // @compat 0.11.6
        ret |= fixUnitChanges();
        // end @compat 0.11.6
        ret |= fixSpec();
        return ret;
    }

    /**
     * Find and remove orphan options/groups that are not part of the
     * core tree of options.
     *
     * @return True if a fix was made.
     */
    private boolean fixOrphanOptions() {
        Collection<AbstractOption> allO = new HashSet<>(allOptions.values());
        Collection<AbstractOption> allG = new HashSet<>(allOptionGroups.values());
        for (String id : coreOptionGroups) {
            dropOptions(allOptionGroups.get(id), allO);
            dropOptions(allOptionGroups.get(id), allG);
        }
        for (AbstractOption ao : allO) {
            // Drop from actual allOptions map
            dropOptions(ao, allOptions.values());
            if (ao instanceof OptionGroup) allOptionGroups.remove(ao.getId());
            logger.warning("Dropping orphan option: " + ao);
        }
        for (AbstractOption ao : allG) {
            allOptionGroups.remove(ao.getId());
            logger.warning("Dropping orphan option group: " + ao);
        }
        return !allO.isEmpty() || !allG.isEmpty();
    }

    /**
     * Drop an option and its descendents from a collection.
     *
     * @param o The {@code AbstractOption} to drop.
     * @param all The collection of options to drop from.
     */
    private void dropOptions(AbstractOption o, Collection<AbstractOption> all) {
        all.remove(o);
        if (o instanceof OptionGroup) {
            OptionGroup og = (OptionGroup)o;
            for (Option op : og.getOptions()) {
                if (op instanceof AbstractOption) {
                    dropOptions((AbstractOption)op, all);
                }
            }
        }
    }

    // @compat 0.11.0
    /**
     * Handle the reworking of roles that landed in 0.11.0.
     *
     * Deliberately not part of applyFixes(), this is called from readFromXML
     * which can most accurately determine whether it is needed.
     *
     * @return True if a fix was made.
     */
    private boolean fixRoles() {
        if (!roles.isEmpty()) return false; // Trust what is there

        if (compareVersion("0.85") > 0) return false;

        File rolf = FreeColDirectories.getCompatibilityFile(ROLES_COMPAT_FILE_NAME);
        try (InputStream fis = Files.newInputStream(rolf.toPath())) {
            load(fis);
        } catch (IOException|XMLStreamException e) {
            logger.log(Level.WARNING, "Failed to load remedial roles.", e);
            return false;
        }

        logger.info("Loading role backward compatibility fragment: "
            + ROLES_COMPAT_FILE_NAME + " with roles: "
            + transform(getRoles(), alwaysTrue(), Role::getId,
                        Collectors.joining(" ")));
        return true;
    }
    // end @compat 0.11.0

    // @compat 0.11.6
    private boolean fixUnitChanges() {
        if (compareVersion("0.116") > 0) return false;

        // Unlike roles, we can not trust what is already there, as the changes
        // are deeper.  However we preserve the ENTER_COLONY change as that
        // comes in from the convertUpgrade mod.
        UnitChangeType enter = find(unitChangeTypeList,
            matchKeyEquals(UnitChangeType.ENTER_COLONY, UnitChangeType::getId));
        File uctf = FreeColDirectories.getCompatibilityFile(UNIT_CHANGE_TYPES_COMPAT_FILE_NAME);
        try (InputStream fis = Files.newInputStream(uctf.toPath())) {
            load(fis);
        } catch (IOException|XMLStreamException e) {
            logger.log(Level.WARNING, "Failed to load unit changes.", e);
            return false;
        }
        if (enter != null) {
            unitChangeTypeList.add(enter);
        }

        logger.info("Loading unit-changes backward compatibility fragment: "
            + UNIT_CHANGE_TYPES_COMPAT_FILE_NAME + " with changes: "
            + transform(getUnitChangeTypeList(), alwaysTrue(),
                UnitChangeType::getId, Collectors.joining(" ")));
        return true;
    }
    // end @compat 0.11.6

    /**
     * Backward compatibility for the Specification in general.
     *
     * @return True if a fix was made.
     */
    private boolean fixSpec() {
        boolean ret = false;
        // @compat 0.10.x/0.11.x
        // This section contains a bunch of things that were
        // discovered and fixed post 0.11.0.
        //
        // We need to keep these fixes for now as they are still
        // needed for games validly upgraded by 0.11.x series FreeCol.

        // 0.10.x had no unknown enemy nation, just an unknown enemy
        // player, and the type was poorly established.
        Nation ue = findType(Nation.UNKNOWN_NATION_ID, Nation.class);
        ue.setType(getNationType("model.nationType.default"));
        if (!nations.contains(ue)) {
            nations.add(ue);
            ret = true;
        }

        // Nation colour moved (back) into the spec in 0.11.0, but the fixup
        // code was just a wrapper, and did not modify the underlying spec.
        for (Nation nation : nations) {
            if (nation.getColor() == null) {
                nation.setColor(defaultColors.get(nation.getId()));
                ret = true;
            }
        }

        // Coronado gained an ability in the freecol ruleset
        FoundingFather coronado
            = getFoundingFather("model.foundingFather.franciscoDeCoronado");
        if ("freecol".equals(getId())
            && !coronado.hasAbility(Ability.SEE_ALL_COLONIES)) {
            coronado.addAbility(new Ability(Ability.SEE_ALL_COLONIES,
                                            coronado, true));
            ret = true;
        }

        // Fix REF roles, soldier -> infantry, dragoon -> cavalry
        // Older specs (<= 0.10.5 ?) had refSize directly under difficulty
        // level, later moved it under the monarch group.
        for (OptionGroup level : getDifficultyLevels()) {
            Option monarch = level.getOption(GameOptions.DIFFICULTY_MONARCH);
            Option refSize = ((OptionGroup)((monarch instanceof OptionGroup)
                    ? monarch : level)).getOption(GameOptions.REF_FORCE);
            if (!(refSize instanceof UnitListOption)) continue;
            boolean changed;
            for (AbstractUnit au
                     : ((UnitListOption)refSize).getOptionValues()) {
                changed = true;
                if ("DEFAULT".equals(au.getRoleId())) {
                    au.setRoleId(DEFAULT_ROLE_ID);
                } else if ("model.role.soldier".equals(au.getRoleId())
                    || "SOLDIER".equals(au.getRoleId())) {
                    au.setRoleId("model.role.infantry");
                } else if ("model.role.dragoon".equals(au.getRoleId())
                    || "DRAGOON".equals(au.getRoleId())) {
                    au.setRoleId("model.role.cavalry");
                } else {
                    changed = false;
                }
                if (changed) ret = true;
            }
        }

        // Fix all other UnitListOptions
        List<Option> todo = new ArrayList<>(getDifficultyLevels());
        while (!todo.isEmpty()) {
            Option o = todo.remove(0);
            if (o instanceof OptionGroup) {
                List<Option> next = ((OptionGroup)o).getOptions();
                todo.addAll(new ArrayList<>(next));
            } else if (o instanceof UnitListOption) {
                boolean changed;
                for (AbstractUnit au : ((UnitListOption)o).getOptionValues()) {
                    String roleId = au.getRoleId();
                    changed = true;
                    if (roleId == null) {
                        au.setRoleId(DEFAULT_ROLE_ID);
                    } else if (au.getRoleId().startsWith("model.role.")) {
                        changed = false; // OK
                    } else if ("DEFAULT".equals(au.getRoleId())) {
                        au.setRoleId(DEFAULT_ROLE_ID);
                    } else if ("DRAGOON".equals(au.getRoleId())) {
                        au.setRoleId("model.role.dragoon");
                    } else if ("MISSIONARY".equals(au.getRoleId())) {
                        au.setRoleId("model.role.missionary");
                    } else if ("PIONEER".equals(au.getRoleId())) {
                        au.setRoleId("model.role.pioneer");
                    } else if ("SCOUT".equals(au.getRoleId())) {
                        au.setRoleId("model.role.scout");
                    } else if ("SOLDIER".equals(au.getRoleId())) {
                        au.setRoleId("model.role.soldier");
                    } else {
                        au.setRoleId(DEFAULT_ROLE_ID);
                    }
                    if (changed) ret = true;
                }
            }
        }

        // is-military was added to goods type
        GoodsType goodsType = getGoodsType("model.goods.horses");
        if (!goodsType.getMilitary()) {
            goodsType.setMilitary();
            ret = true;
        }
        goodsType = getGoodsType("model.goods.muskets");
        if (!goodsType.getMilitary()) {
            goodsType.setMilitary();
            ret = true;
        }
        // end @compat 0.10.x/0.11.x

        // @compat 0.11.0
        // Bolivar changed from being an event, then to a liberty modifier,
        // and now to a SoL% modifier.
        FoundingFather bolivar
            = getFoundingFather("model.foundingFather.simonBolivar");
        boolean bolivarAdd = false;
        if (!bolivar.getEvents().isEmpty()) {
            bolivar.setEvents(Collections.<Event>emptyList());
            bolivarAdd = true;
        } else if (bolivar.hasModifier(Modifier.LIBERTY)) {
            bolivar.removeModifiers(Modifier.LIBERTY);
            bolivarAdd = true;
        }
        if (bolivarAdd) {
            bolivar.addModifier(new Modifier(Modifier.SOL, 20,
                    Modifier.ModifierType.ADDITIVE, bolivar, 0));
            ret = true;
        }

        // The COASTAL_ONLY attribute was added to customs house.
        BuildingType customs = getBuildingType("model.building.customHouse");
        if (!customs.hasAbility(Ability.COASTAL_ONLY)) {
            customs.addAbility(new Ability(Ability.COASTAL_ONLY, null, false));
            ret = true;
        }
        // end @compat 0.11.0

        // @compat 0.11.3
        // Added the cargo penalty modifier
        if (none(getModifiers(Modifier.CARGO_PENALTY))) {
            addModifier(new Modifier(Modifier.CARGO_PENALTY, -12.5f,
                    Modifier.ModifierType.PERCENTAGE, CARGO_PENALTY_SOURCE,
                    Modifier.GENERAL_COMBAT_INDEX));
            ret = true;
        }

        // Backwards compatibility for the fixes to BR#2873.
        Event event = getEvent("model.event.declareIndependence");
        if (event != null) {
            Limit limit = event.getLimit("model.limit.independence.coastalColonies");
            if (limit != null) {
                limit.setOperator(Limit.Operator.GE);
                limit.getRightHandSide().setValue(1);
                ret = true;
            }
        }
        // end @compat 0.11.3

        // @compat 0.11.5
        // Added a modifier to hardy pioneer
        UnitType hardyPioneer = getUnitType("model.unit.hardyPioneer");
        if (none(hardyPioneer.getModifiers(Modifier.TILE_TYPE_CHANGE_PRODUCTION))) {
            Modifier m = new Modifier(Modifier.TILE_TYPE_CHANGE_PRODUCTION,
                2.0f, Modifier.ModifierType.MULTIPLICATIVE);
            Scope scope = new Scope();
            scope.setType("model.goods.lumber");
            m.addScope(scope);
            hardyPioneer.addModifier(m);
            ret = true;
        }

        // Added modifier to Coronado
        if (!coronado.hasModifier(Modifier.EXPOSED_TILES_RADIUS)) {
            coronado.addModifier(new Modifier(Modifier.EXPOSED_TILES_RADIUS,
                    3.0f, Modifier.ModifierType.ADDITIVE, coronado, 0));
            ret = true;
        }
        // end @compat 0.11.5

        // @compat 0.11.6
        // PlunderType became a FCSOT and thus gained identifiers
        for (IndianNationType nt : indianNationTypes) {
            for (SettlementType st : nt.getSettlementTypes()) {
                for (PlunderType pt : st.getPlunderTypes()) {
                    if (pt.getId() == null) {
                        Scope scope = find(pt.getScopes(),
                            matchKey("model.ability.plunderNatives",
                                     Scope::getAbilityId));
                        String id = "plunder." + nt.getSuffix()
                            + ((st.isCapital()) ? ".capital" : "")
                            + ((scope == null) ? "" : ".extra");
                        pt.setId(id);
                        ret = true;
                    }
                }
            }
        }

        // <upgrade> element dropped from FoundingFather, replaced by an
        // ability.  Only impacts Casas.
        FoundingFather casas
            = getFoundingFather("model.foundingFather.bartolomeDeLasCasas");
        if (casas != null && !casas.hasAbility(Ability.UPGRADE_CONVERT)) {
            Ability uc = new Ability(Ability.UPGRADE_CONVERT, casas, true);
            addAbility(uc);
            casas.addAbility(uc);
            ret = true;
        }

        // Added mercenary-price to manOWar
        UnitType mow = getUnitType("model.unit.manOWar");
        if (mow != null && mow.getMercenaryPrice() < 0) {
            mow.setMercenaryPrice(10000);
            ret = true;
        }
        // Old manOWars required independenceDeclared which was moved to
        // independentNation a while back, but surfaced again in old games
        final String maidId = "model.ability.independenceDeclared";
        if (mow != null && mow.requiresAbility(maidId)) {
            mow.removeRequiredAbility(maidId);
            mow.addRequiredAbility(Ability.INDEPENDENT_NATION, true);
            ret = true;
        }

        // Added canBeSurrendered ability to all the REF units.
        // REF Units are the units that can be *added* to the REF.
        // The REF can capture other units, but they can all automatically
        // be surrendered, whereas of the actual REF units, only the artillery
        // can be handed over to the victorious independent nation.
        for (UnitType ut : getUnitTypesWithAbility(Ability.REF_UNIT)) {
            if (!ut.containsAbilityKey(Ability.CAN_BE_SURRENDERED)) {
                ut.addAbility(new Ability(Ability.CAN_BE_SURRENDERED, null,
                        "model.unit.artillery".equals(ut.getId())));
                ret = true;
            }
        }
        // end @compat 0.11.6
        return ret;
    }

    /**
     * Backward compatibility code to make sure this specification
     * contains a default value for every difficulty option.
     *
     * When a new difficulty option is added to the spec, add a
     * sensible default here.
     *
     * @return True if an option was missing and added.
     */
    private boolean fixDifficultyOptions() {
        boolean ret = false;
        String id;
        AbstractOption op;
        OptionGroup og;
        UnitListOption ulo;

        LogBuilder lb = new LogBuilder(64);
        lb.mark();

        // SAVEGAME_VERSION == 11
        // SAVEGAME_VERSION == 12

        // @compat 0.10.x/0.11.x
        // For 0.10.6 we moved from a three level structure:
        //   difficultyLevels/<difficulty>/<option>
        // to a four level structure
        //   difficultyLevels/<difficulty>/<group>/<option>
        // so add the groups in, and move the options that could be present
        // in anything up to 0.10.5 into their destination groups.
        //
        // Alas, there were weaknesses in the checkDifficulty*()
        // routines that were not fixed through 0.11.x, so some 0.10.x
        // games were only partially upgraded to the 0.11.x format.
        //
        // So keep this around until SAVEGAME_VERSION == 15.
        ret |= checkDifficultyOptionGroup(GameOptions.DIFFICULTY_IMMIGRATION, lb,
            GameOptions.CROSSES_INCREMENT,
            GameOptions.RECRUIT_PRICE_INCREASE,
            GameOptions.LOWER_CAP_INCREASE,
            GameOptions.PRICE_INCREASE + ".artillery",
            GameOptions.PRICE_INCREASE_PER_TYPE,
            GameOptions.EXPERT_STARTING_UNITS,
            GameOptions.IMMIGRANTS);
        ret |= checkDifficultyOptionGroup(GameOptions.DIFFICULTY_NATIVES, lb,
            GameOptions.LAND_PRICE_FACTOR,
            GameOptions.NATIVE_CONVERT_PROBABILITY,
            GameOptions.BURN_PROBABILITY,
            GameOptions.NATIVE_DEMANDS,
            GameOptions.RUMOUR_DIFFICULTY,
            GameOptions.SHIP_TRADE_PENALTY,
            GameOptions.BUILD_ON_NATIVE_LAND,
            GameOptions.SETTLEMENT_NUMBER);
        ret |= checkDifficultyOptionGroup(GameOptions.DIFFICULTY_MONARCH, lb,
            GameOptions.MONARCH_MEDDLING,
            GameOptions.TAX_ADJUSTMENT,
            GameOptions.MERCENARY_PRICE,
            GameOptions.MAXIMUM_TAX,
            GameOptions.MONARCH_SUPPORT,
            GameOptions.TREASURE_TRANSPORT_FEE,
            GameOptions.REF_FORCE,
            GameOptions.INTERVENTION_BELLS,
            GameOptions.INTERVENTION_TURNS,
            GameOptions.INTERVENTION_FORCE,
            GameOptions.MERCENARY_FORCE);
        ret |= checkDifficultyOptionGroup(GameOptions.DIFFICULTY_GOVERNMENT, lb,
            GameOptions.BAD_GOVERNMENT_LIMIT,
            GameOptions.VERY_BAD_GOVERNMENT_LIMIT,
            GameOptions.GOOD_GOVERNMENT_LIMIT,
            GameOptions.VERY_GOOD_GOVERNMENT_LIMIT);
        ret |= checkDifficultyOptionGroup(GameOptions.DIFFICULTY_OTHER, lb,
            GameOptions.STARTING_MONEY,
            GameOptions.FOUNDING_FATHER_FACTOR,
            GameOptions.ARREARS_FACTOR,
            GameOptions.UNITS_THAT_USE_NO_BELLS,
            GameOptions.TILE_PRODUCTION);
        ret |= checkDifficultyOptionGroup(GameOptions.DIFFICULTY_CHEAT, lb,
            GameOptions.LIFT_BOYCOTT_CHEAT,
            GameOptions.EQUIP_SCOUT_CHEAT,
            GameOptions.LAND_UNIT_CHEAT,
            GameOptions.OFFENSIVE_NAVAL_UNIT_CHEAT,
            GameOptions.TRANSPORT_NAVAL_UNIT_CHEAT);

        id = GameOptions.REF_FORCE; // Yes, really "refSize"
        ulo = checkDifficultyUnitListOption(id, GameOptions.DIFFICULTY_MONARCH, lb);
        if (ulo != null) {
            AbstractUnitOption regulars
                = new AbstractUnitOption(id + ".regulars", this);
            regulars.setValue(new AbstractUnit("model.unit.kingsRegular",
                                               "model.role.infantry", 31));
            ulo.getValue().add(regulars);
            AbstractUnitOption dragoons
                = new AbstractUnitOption(id + ".dragoons", this);
            dragoons.setValue(new AbstractUnit("model.unit.kingsRegular",
                                               "model.role.cavalry", 15));
            ulo.getValue().add(dragoons);
            AbstractUnitOption artillery
                = new AbstractUnitOption(id + ".artillery", this);
            artillery.setValue(new AbstractUnit("model.unit.artillery",
                                                DEFAULT_ROLE_ID, 14));
            ulo.getValue().add(artillery);
            AbstractUnitOption menOfWar
                = new AbstractUnitOption(id + ".menOfWar", this);
            menOfWar.setValue(new AbstractUnit("model.unit.manOWar",
                                               DEFAULT_ROLE_ID, 8));
            ulo.getValue().add(menOfWar);
            ret = true;
        }
        id = GameOptions.IMMIGRANTS;
        ulo = checkDifficultyUnitListOption(id, GameOptions.DIFFICULTY_IMMIGRATION, lb);
        if (ulo != null) {
            AbstractUnitOption i1
                = new AbstractUnitOption(id + ".1", this);
            i1.setValue(new AbstractUnit("model.unit.masterCarpenter",
                                         DEFAULT_ROLE_ID, 1));
            ulo.getValue().add(i1);
            ret = true;
        }

        ret |= checkDifficultyIntegerOption(GameOptions.INTERVENTION_BELLS,
                                            GameOptions.DIFFICULTY_MONARCH, lb,
                                            5000);
        ret |= checkDifficultyIntegerOption(GameOptions.INTERVENTION_TURNS,
                                            GameOptions.DIFFICULTY_MONARCH, lb,
                                            52);
        id = GameOptions.INTERVENTION_FORCE;
        ulo = checkDifficultyUnitListOption(id, GameOptions.DIFFICULTY_MONARCH, lb);
        if (ulo != null) {
            AbstractUnitOption regulars
                = new AbstractUnitOption(id + ".regulars", this);
            regulars.setValue(new AbstractUnit("model.unit.colonialRegular",
                                               "model.role.soldier", 2));
            ulo.getValue().add(regulars);
            AbstractUnitOption dragoons
                = new AbstractUnitOption(id + ".dragoons", this);
            dragoons.setValue(new AbstractUnit("model.unit.colonialRegular",
                                               "model.role.dragoon", 2));
            ulo.getValue().add(dragoons);
            AbstractUnitOption artillery
                = new AbstractUnitOption(id + ".artillery", this);
            artillery.setValue(new AbstractUnit("model.unit.artillery",
                                                DEFAULT_ROLE_ID, 2));
            ulo.getValue().add(artillery);
            AbstractUnitOption menOfWar
                = new AbstractUnitOption(id + ".menOfWar", this);
            menOfWar.setValue(new AbstractUnit("model.unit.manOWar",
                                               DEFAULT_ROLE_ID, 2));
            ulo.getValue().add(menOfWar);
            ret = true;
        }
        id = GameOptions.MERCENARY_FORCE;
        ulo = checkDifficultyUnitListOption(id, GameOptions.DIFFICULTY_MONARCH, lb);
        if (ulo != null) {
            AbstractUnitOption regulars
                = new AbstractUnitOption(id + ".regulars", this);
            regulars.setValue(new AbstractUnit("model.unit.veteranSoldier",
                                               "model.role.soldier", 2));
            ulo.getValue().add(regulars);
            AbstractUnitOption dragoons
                = new AbstractUnitOption(id + ".dragoons", this);
            dragoons.setValue(new AbstractUnit("model.unit.veteranSoldier",
                                               "model.role.dragoon", 2));
            ulo.getValue().add(dragoons);
            AbstractUnitOption artillery
                = new AbstractUnitOption(id + ".artillery", this);
            artillery.setValue(new AbstractUnit("model.unit.artillery",
                                                DEFAULT_ROLE_ID, 2));
            ulo.getValue().add(artillery);
            AbstractUnitOption menOfWar
                = new AbstractUnitOption(id + ".menOfWar", this);
            menOfWar.setValue(new AbstractUnit("model.unit.manOWar",
                                               DEFAULT_ROLE_ID, 2));
            ulo.getValue().add(menOfWar);
            ret = true;
        }
        ret |= checkDifficultyIntegerOption(GameOptions.GOOD_GOVERNMENT_LIMIT,
                                            GameOptions.DIFFICULTY_GOVERNMENT, lb,
                                            50);
        ret |= checkDifficultyIntegerOption(GameOptions.VERY_GOOD_GOVERNMENT_LIMIT,
                                            GameOptions.DIFFICULTY_GOVERNMENT, lb,
                                            100);
        ret |= checkDifficultyIntegerOption(GameOptions.LIFT_BOYCOTT_CHEAT,
                                            GameOptions.DIFFICULTY_CHEAT, lb,
                                            10);
        ret |= checkDifficultyIntegerOption(GameOptions.EQUIP_SCOUT_CHEAT,
                                            GameOptions.DIFFICULTY_CHEAT, lb,
                                            10);
        ret |= checkDifficultyIntegerOption(GameOptions.LAND_UNIT_CHEAT,
                                            GameOptions.DIFFICULTY_CHEAT, lb,
                                            10);
        ret |= checkDifficultyIntegerOption(GameOptions.OFFENSIVE_NAVAL_UNIT_CHEAT,
                                            GameOptions.DIFFICULTY_CHEAT, lb,
                                            10);
        ret |= checkDifficultyIntegerOption(GameOptions.TRANSPORT_NAVAL_UNIT_CHEAT,
                                            GameOptions.DIFFICULTY_CHEAT, lb,
                                            10);
        // end @compat 0.10.x/0.11.x

        // SAVEGAME_VERSION == 13

        // @compat 0.11.0
        ret |= checkDifficultyIntegerOption(GameOptions.DESTROY_SETTLEMENT_SCORE,
                                            GameOptions.DIFFICULTY_NATIVES, lb,
                                            -5);
        ret |= checkDifficultyPercentageOption(GameOptions.BAD_RUMOUR,
                                               GameOptions.DIFFICULTY_OTHER, lb,
                                               23);
        ret |= checkDifficultyPercentageOption(GameOptions.GOOD_RUMOUR,
                                               GameOptions.DIFFICULTY_OTHER, lb,
                                               48);
        ret |= checkDifficultyIntegerOption(GameOptions.OFFENSIVE_LAND_UNIT_CHEAT,
                                            GameOptions.DIFFICULTY_CHEAT, lb,
                                            4);
        ret |= checkDifficultyIntegerOption(GameOptions.EQUIP_PIONEER_CHEAT,
                                            GameOptions.DIFFICULTY_CHEAT, lb,
                                            10);
        // end @compat 0.11.0

        // @compat 0.11.3
        id = GameOptions.WAR_SUPPORT_FORCE;
        ulo = checkDifficultyUnitListOption(id, GameOptions.DIFFICULTY_MONARCH, lb);
        if (ulo != null) {
            AbstractUnitOption support = new AbstractUnitOption(id, this);
            support.setValue(new AbstractUnit("model.unit.veteranSoldier",
                                              "model.role.soldier", 4));
            ulo.getValue().add(support);
            ret = true;
        }
        ret |= checkDifficultyIntegerOption(GameOptions.WAR_SUPPORT_GOLD,
                                            GameOptions.DIFFICULTY_MONARCH, lb,
                                            1500);
        // end @compat 0.11.3

        // Ensure 2 levels of groups, and one level of leaves
        for (OptionGroup level : getDifficultyLevels()) {
            for (Option o : level.getOptions()) {
                if (o instanceof OptionGroup) {
                    og = (OptionGroup)o;
                    for (Option o2 : og.getOptions()) {
                        if (o2 instanceof OptionGroup) {
                            lb.add("?-group ", level.getId() + "/" + og.getId()
                                + "/" + o2.getId());
                        }
                    }
                } else {
                    lb.add("? ", level.getId() + "/" + o.getId());
                }
            }
        }

        if (lb.grew("Check difficulty options:")) {
            lb.log(logger, Level.INFO);
        }

        // SAVEGAME_VERSION == 14
        return ret;
    }

    private boolean checkDifficultyOptionGroup(String gr, LogBuilder lb,
                                               String... ids) {
        boolean ret = false;
        for (OptionGroup level : getDifficultyLevels()) {
            OptionGroup og = null;
            for (Option o : level.getOptions()) {
                if (o.getId().equals(gr) && o instanceof OptionGroup) {
                    og = (OptionGroup)o;
                    break;
                }
            }
            if (og == null) {
                level.remove(gr);
                og = new OptionGroup(gr, this);
                level.add(og);
                og.setGroup(level.getId());
                lb.add("\n  +", level.getId(), "/", gr);
                ret = true;
            }
            for (String id : ids) {
                Option op = null;
                for (Option o : level.getOptions()) {
                    if (o.getId().equals(id) && !(o instanceof OptionGroup)) {
                        op = o;
                        break;
                    }
                }
                if (op != null) {
                    level.remove(op.getId());
                    if (op instanceof AbstractOption) {
                        ((AbstractOption)op).setGroup(og.getId());
                    }
                    og.add(op);
                    lb.add("\n  ~", level.getId(), "/", id, " -> ",
                        level.getId(), "/" + og.getId());
                    ret = true;
                }
            }
        }
        return ret;
    }

    private boolean checkDifficultyIntegerOption(String id, String gr,
                                                 LogBuilder lb,
                                                 int defaultValue) {
        boolean ret = false;
        for (OptionGroup level : getDifficultyLevels()) {
            OptionGroup og = null;
            for (Option o : level.getOptions()) {
                if (o.getId().equals(gr) && o instanceof OptionGroup) {
                    og = (OptionGroup)o;
                    break;
                }
            }
            if (og == null) continue;
            Option op = null;
            for (Option o : level.getOptions()) {
                if (o.getId().equals(id) && !(o instanceof OptionGroup)) {
                    op = o;
                    break;
                }
            }
            if (op != null) {
                level.remove(op.getId());
                if (!(op instanceof IntegerOption)) {
                    IntegerOption iop = new IntegerOption(id, this);
                    iop.setValue(defaultValue);
                    op = iop;
                }
                op.setGroup(gr);
                og.add(op);
                lb.add("\n  ~", level.getId(), "/", id, " -> ",
                    level.getId(), "/" + og.getId());
            } else {
                op = null;
                for (Option o : og.getOptions()) {
                    if (o.getId().equals(id)) {
                        op = o;
                        break;
                    }
                }
                if (op instanceof IntegerOption) continue;
                if (op != null) og.remove(id);
                IntegerOption iop = new IntegerOption(id, this);
                iop.setGroup(gr);
                iop.setValue(defaultValue);
                og.add(iop);
                lb.add("\n  +", level.getId(), "/", og.getId(),
                    "/", id, "=", defaultValue);
            }
            ret = true;
        }
        return ret;
    }

    private boolean checkDifficultyPercentageOption(String id, String gr,
                                                    LogBuilder lb,
                                                    int defaultValue) {
        boolean ret = false;
        for (OptionGroup level : getDifficultyLevels()) {
            OptionGroup og = null;
            for (Option o : level.getOptions()) {
                if (o.getId().equals(gr) && o instanceof OptionGroup) {
                    og = (OptionGroup)o;
                    break;
                }
            }
            if (og == null) continue;
            Option op = null;
            for (Option o : level.getOptions()) {
                if (o.getId().equals(id) && !(o instanceof OptionGroup)) {
                    op = o;
                    break;
                }
            }
            if (op != null) {
                level.remove(op.getId());
                if (!(op instanceof PercentageOption)) {
                    PercentageOption pop = new PercentageOption(id, this);
                    pop.setValue(defaultValue);
                    op = pop;
                }
                op.setGroup(gr);
                og.add(op);
                lb.add("\n  ~", level.getId(), "/", id, " -> ",
                    level.getId(), "/" + og.getId());
            } else {
                op = null;
                for (Option o : og.getOptions()) {
                    if (o.getId().equals(id)) {
                        op = o;
                        break;
                    }
                }
                if (op instanceof PercentageOption) continue;
                if (op != null) og.remove(id);
                PercentageOption pop = new PercentageOption(id, this);
                pop.setValue(defaultValue);
                pop.setGroup(gr);
                og.add(pop);
                lb.add("\n  +", level.getId(), "/", og.getId(),
                    "/", id, "=", defaultValue);
            }
            ret = true;
        }
        return ret;
    }

    private UnitListOption checkDifficultyUnitListOption(String id, String gr,
                                                         LogBuilder lb) {
        UnitListOption ulo = null;
        for (OptionGroup level : getDifficultyLevels()) {
            OptionGroup og = null;
            for (Option o : level.getOptions()) {
                if (o.getId().equals(gr) && o instanceof OptionGroup) {
                    og = (OptionGroup)o;
                    break;
                }
            }
            if (og == null) continue;
            Option op = null;
            for (Option o : level.getOptions()) {
                if (o.getId().equals(id) && !(o instanceof OptionGroup)) {
                    op = o;
                    break;
                }
            }
            if (op != null) {
                level.remove(op.getId());
                if (op instanceof AbstractOption) {
                    ((AbstractOption)op).setGroup(og.getId());
                }
                og.add(op);
                lb.add("\n  ~", level.getId(), "/", id, " -> ",
                    level.getId(), "/" + og.getId());
            } else {
                op = null;
                for (Option o : og.getOptions()) {
                    if (o.getId().equals(id)) {
                        op = o;
                        break;
                    }
                }
                if (op instanceof UnitListOption) continue;
                if (op != null) og.remove(id);
                if (ulo == null) ulo = new UnitListOption(id, this);
                og.add(ulo);
                lb.add("\n  +", level.getId(), "/", og.getId(),
                    "/[", id, "]");
            }
        }
        return ulo;
    }

    /**
     * Backward compatibility code to make sure this specification
     * contains a default value for every game option.
     *
     * When a new game option is added to the spec, add a sensible
     * default here.
     *
     * @return True if an option was missing and added.
     */
    private boolean fixGameOptions() {
        boolean ret = false;
        // SAVEGAME_VERSION == 11

        // SAVEGAME_VERSION == 12

        // @compat 0.10.5
        ret |= checkOp(GameOptions.ENABLE_UPKEEP,
                       GameOptions.GAMEOPTIONS_COLONY,
                       Boolean.FALSE, BooleanOption.class);
        ret |= checkOp(GameOptions.NATURAL_DISASTERS,
                       GameOptions.GAMEOPTIONS_COLONY,
                       0, PercentageOption.class);
        ret |= checkOp(GameOptions.GIFT_PROBABILITY,
                       GameOptions.GAMEOPTIONS_MAP,
                       5, PercentageOption.class);
        ret |= checkOp(GameOptions.DEMAND_PROBABILITY,
                       GameOptions.GAMEOPTIONS_MAP,
                       10, PercentageOption.class);
        ret |= checkOp(GameOptions.EMPTY_TRADERS,
                       GameOptions.GAMEOPTIONS_MAP,
                       Boolean.FALSE, BooleanOption.class);
        // end @compat 0.10.5

        // SAVEGAME_VERSION == 13

        // @compat 0.10.7
        ret |= checkOp(GameOptions.ONLY_NATURAL_IMPROVEMENTS,
                       GameOptions.GAMEOPTIONS_COLONY,
                       Boolean.TRUE, BooleanOption.class);
        ret |= checkOp(GameOptions.PEACE_PROBABILITY,
                       GameOptions.GAMEOPTIONS_MAP,
                       90, PercentageOption.class);
        ret |= checkOp(GameOptions.INITIAL_IMMIGRATION,
                       GameOptions.GAMEOPTIONS_MAP,
                       15, IntegerOption.class);
        ret |= checkOp(GameOptions.EUROPEAN_UNIT_IMMIGRATION_PENALTY,
                       GameOptions.GAMEOPTIONS_MAP,
                       -4, IntegerOption.class);
        ret |= checkOp(GameOptions.PLAYER_IMMIGRATION_BONUS,
                       GameOptions.GAMEOPTIONS_MAP,
                       2, IntegerOption.class);
        ret |= checkOp(GameOptions.FOUND_COLONY_DURING_REBELLION,
                       GameOptions.GAMEOPTIONS_COLONY,
                       Boolean.TRUE, BooleanOption.class);
        // end @compat 0.10.7

        // @compat 0.11.0
        ret |= checkOp(GameOptions.BELL_ACCUMULATION_CAPPED,
                       GameOptions.GAMEOPTIONS_COLONY,
                       Boolean.FALSE, BooleanOption.class);
        ret |= checkOp(GameOptions.CAPTURE_UNITS_UNDER_REPAIR,
                       GameOptions.GAMEOPTIONS_COLONY,
                       Boolean.FALSE, BooleanOption.class);
        ret |= checkOp(GameOptions.PAY_FOR_BUILDING,
                       GameOptions.GAMEOPTIONS_COLONY,
                       Boolean.TRUE, BooleanOption.class);
        ret |= checkOp(GameOptions.CLEAR_HAMMERS_ON_CONSTRUCTION_SWITCH,
                       GameOptions.GAMEOPTIONS_COLONY,
                       Boolean.FALSE, BooleanOption.class);
        ret |= checkOp(GameOptions.CUSTOMS_ON_COAST,
                       GameOptions.GAMEOPTIONS_COLONY,
                       Boolean.FALSE, BooleanOption.class);
        ret |= checkOp(GameOptions.EQUIP_EUROPEAN_RECRUITS,
                       GameOptions.GAMEOPTIONS_COLONY,
                       Boolean.TRUE, BooleanOption.class);
        // end @compat 0.11.0

        // @compat 0.11.3
        ret |= checkOp(GameOptions.MISSION_INFLUENCE,
                       GameOptions.GAMEOPTIONS_MAP,
                       -10, IntegerOption.class);
        ret |= checkOp(GameOptions.INDEPENDENCE_TURN,
                       GameOptions.GAMEOPTIONS_YEARS,
                       468, IntegerOption.class);
        ret |= checkOp(GameOptions.AGES,
                       GameOptions.GAMEOPTIONS_YEARS,
                       "1600,1700", TextOption.class);
        ret |= checkOp(GameOptions.SEASONS,
                       GameOptions.GAMEOPTIONS_YEARS,
                       2, IntegerOption.class);
        ret |= checkOp(GameOptions.DISEMBARK_IN_COLONY,
                       GameOptions.GAMEOPTIONS_COLONY,
                       Boolean.FALSE, BooleanOption.class);
        ret |= checkOp(GameOptions.ENHANCED_TRADE_ROUTES,
                       GameOptions.GAMEOPTIONS_MAP,
                       Boolean.FALSE, BooleanOption.class);
        // end @compat 0.11.3

        // @compat 0.11.6
        ret |= checkOp(GameOptions.SETTLEMENT_NUMBER_OF_GOODS_TO_SELL,
                       GameOptions.GAMEOPTIONS_MAP,
                       3, IntegerOption.class);
        ret |= checkOp(GameOptions.ALARM_BONUS_BUY,
                       GameOptions.GAMEOPTIONS_MAP,
                       20, PercentageOption.class);
        ret |= checkOp(GameOptions.ALARM_BONUS_SELL,
                       GameOptions.GAMEOPTIONS_MAP,
                       20, PercentageOption.class);
        ret |= checkOp(GameOptions.ALARM_BONUS_GIFT,
                       GameOptions.GAMEOPTIONS_MAP,
                       40, PercentageOption.class);
        // end @compat 0.11.6

        // SAVEGAME_VERSION == 14
        return ret;
    }

    /**
     * Backward compatibility code to make sure this specification
     * contains a default value for every map option.
     *
     * When a new map option is added to the spec, add a sensible
     * default here.
     *
     * @return True if an option was missing and added.
     */
    private boolean fixMapGeneratorOptions() {
        boolean ret = false;
        // SAVEGAME_VERSION == 11
        // SAVEGAME_VERSION == 12
        // SAVEGAME_VERSION == 13
        // SAVEGAME_VERSION == 14
        return ret;
    }

    /**
     * Check if an option exists, and if not, create and install it.
     *
     * FIXME: Handles failure to create a needed option badly.
     *
     * @param <R> The underlying type encapsulated by the option.
     * @param <T> The option type.
     * @param id The option identifier.
     * @param gr The option group identifier.
     * @param defaultValue The default value of the option.
     * @param returnClass The expected class of option.
     * @return True if the option did not exist and was successfully created.
     */
    private <R,T extends Option<R>> boolean checkOp(String id, String gr,
                                                    R defaultValue,
                                                    Class<T> returnClass) {
        if (hasOption(id, returnClass)) return false;
        T op;
        try {
            op = Introspector.instantiate(returnClass,
                new Class[] { String.class, Specification.class },
                new Object[] { id, this });
        } catch (Introspector.IntrospectorException ie) {
            logger.warning("Failed to make " + returnClass.getName()
                + ": " + id);
            return false;
        }
        op.setGroup(gr);
        op.setValue(defaultValue);
        getOptionGroup(gr).add(op);
        addAbstractOption((AbstractOption)op);
        return true;
    }
        

    // Serialization

    private static final String BUILDING_TYPES_TAG = "building-types";
    private static final String DIFFICULTY_LEVEL_TAG = "difficulty-level";
    private static final String DISASTERS_TAG = "disasters";
    private static final String EUROPEAN_NATION_TYPES_TAG = "european-nation-types";
    private static final String EVENTS_TAG = "events";
    private static final String FOUNDING_FATHERS_TAG = "founding-fathers";
    private static final String GOODS_TYPES_TAG = "goods-types";
    private static final String INDIAN_NATION_TYPES_TAG = "indian-nation-types";
    private static final String MODIFIERS_TAG = "modifiers";
    private static final String NATIONS_TAG = "nations";
    private static final String OPTIONS_TAG = "options";
    private static final String RESOURCE_TYPES_TAG = "resource-types";
    private static final String ROLES_TAG = "roles";
    private static final String TILE_TYPES_TAG = "tile-types";
    private static final String TILE_IMPROVEMENT_TYPES_TAG = "tile-improvement-types";
    private static final String UNIT_CHANGE_TYPES_TAG = "unit-change-types";
    private static final String UNIT_TYPES_TAG = "unit-types";
    private static final String VERSION_TAG = "version";
    // @compat 0.11.3
    private static final String OLD_DIFFICULTY_LEVEL_TAG = "difficultyLevel";
    private static final String OLD_TILEIMPROVEMENT_TYPES_TAG = "tileimprovement-types";
    // end @compat 0.11.3
    // @compat 0.11.x
    private static final String OLD_EQUIPMENT_TYPES_TAG = "equipment-types";
    // end @compat 0.11.x

    /**
     * Write an XML-representation of this object to the given stream.
     *
     * @param xw The {@code FreeColXMLWriter} to write to.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        xw.writeStartElement(TAG);

        // Add attributes
        xw.writeAttribute(FreeColObject.ID_ATTRIBUTE_TAG, getId());
        if (difficultyLevel != null) {
            xw.writeAttribute(DIFFICULTY_LEVEL_TAG, difficultyLevel);
        }
        if (version != null) {
            xw.writeAttribute(VERSION_TAG, version);
        }

        // copy the order of section in specification.xml
        writeSection(xw, MODIFIERS_TAG, specialModifiers);
        writeSection(xw, EVENTS_TAG, events);
        writeSection(xw, DISASTERS_TAG, disasters);
        writeSection(xw, GOODS_TYPES_TAG, goodsTypeList);
        writeSection(xw, RESOURCE_TYPES_TAG, resourceTypeList);
        writeSection(xw, TILE_TYPES_TAG, tileTypeList);
        writeSection(xw, ROLES_TAG, roles);
        writeSection(xw, TILE_IMPROVEMENT_TYPES_TAG, tileImprovementTypeList);
        writeSection(xw, UNIT_CHANGE_TYPES_TAG, unitChangeTypeList);
        writeSection(xw, UNIT_TYPES_TAG, unitTypeList);
        writeSection(xw, BUILDING_TYPES_TAG, buildingTypeList);
        writeSection(xw, FOUNDING_FATHERS_TAG, foundingFathers);
        writeSection(xw, EUROPEAN_NATION_TYPES_TAG, europeanNationTypes);
        writeSection(xw, EUROPEAN_NATION_TYPES_TAG, REFNationTypes);
        writeSection(xw, INDIAN_NATION_TYPES_TAG, indianNationTypes);
        writeSection(xw, NATIONS_TAG, nations);

        xw.writeStartElement(OPTIONS_TAG);
        for (String id : coreOptionGroups) {
            OptionGroup og = allOptionGroups.get(id);
            if (og != null) og.toXML(xw);
        }
        xw.writeEndElement();

        xw.writeEndElement();
    }

    private <T extends FreeColObject> void writeSection(FreeColXMLWriter xw,
        String section, Collection<T> items) throws XMLStreamException {
        xw.writeStartElement(section);

        for (T item : items) item.toXML(xw);

        xw.writeEndElement();
    }

    /**
     * Initializes this object from its XML-representation.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there are any problems reading
     *     the stream.
     */
    public void readFromXML(FreeColXMLReader xr) throws XMLStreamException {
        // Beware!  We load mods onto an existing specification, but
        // we do not want to overwrite its main attributes (id,
        // difficulty, version) So only set those variables if they
        // are currently null, as will be the case when we load the
        // root specification.
        String newId = xr.readId();
        if (id == null) id = newId;

        if (difficultyLevel == null) {
            difficultyLevel = xr.getAttribute(DIFFICULTY_LEVEL_TAG,
                                              (String)null);
            // @compat 0.11.3
            if (difficultyLevel == null) {
                difficultyLevel = xr.getAttribute(OLD_DIFFICULTY_LEVEL_TAG,
                                                  (String)null);
            }
            // end @compat 0.11.3
        }

        if (version == null) {
            version = xr.getAttribute(VERSION_TAG, (String)null);
        }

        logger.fine("Reading specification " + newId
            + " difficulty=" + difficultyLevel
            + " version=" + version);

        String parentId = xr.getAttribute(FreeColSpecObjectType.EXTENDS_TAG,
                                          (String)null);
        if (parentId != null) {
            try {
                FreeColTcFile parent = FreeColTcFile.getFreeColTcFile(parentId);
                load(parent.getSpecificationInputStream());
                initialized = false;
            } catch (IOException e) {
                throw new XMLStreamException("Failed to open parent specification: ", e);
            }
        }

        while (xr.moreTags()) {
            String childName = xr.getLocalName();
            // @compat 0.11.0
            if (OLD_EQUIPMENT_TYPES_TAG.equals(childName)) {
                xr.swallowTag(OLD_EQUIPMENT_TYPES_TAG);
                continue;
            }
            // end @compat 0.11.0
            ChildReader reader = readerMap.get(childName);
            if (reader == null) {
                logger.warning("No reader found for: " + childName);
            } else {
                reader.readChildren(xr);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }
}
