/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.option.AbstractOption;
import net.sf.freecol.common.option.AbstractUnitOption;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.RangeOption;
import net.sf.freecol.common.option.StringOption;
import net.sf.freecol.common.option.UnitListOption;


/**
 * This class encapsulates any parts of the "specification" for
 * FreeCol that are expressed best using XML.  The XML is loaded
 * through the class loader from the resource named
 * "specification.xml" in the same package as this class.
 */
public final class Specification {

    private static final Logger logger = Logger.getLogger(Specification.class.getName());

    public static class Source extends FreeColGameObjectType {

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
            throw new RuntimeException("Can not happen");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return getId();
        }

        /**
         * {@inheritDoc}
         */
        public String getXMLTagName() { return "source"; }
    };

    public static final Source MOVEMENT_PENALTY_SOURCE
        = new Source("model.source.movementPenalty");
    public static final Source ARTILLERY_PENALTY_SOURCE
        = new Source("model.source.artilleryInTheOpen");
    public static final Source ATTACK_BONUS_SOURCE
        = new Source("model.source.attackBonus");
    public static final Source FORTIFICATION_BONUS_SOURCE
        = new Source("model.source.fortified");
    public static final Source INDIAN_RAID_BONUS_SOURCE
        = new Source("model.source.artilleryAgainstRaid");
    public static final Source AMPHIBIOUS_ATTACK_PENALTY_SOURCE
        = new Source("model.source.amphibiousAttack");
    public static final Source BASE_OFFENCE_SOURCE
        = new Source("model.source.baseOffence");
    public static final Source BASE_DEFENCE_SOURCE
        = new Source("model.source.baseDefence");
    public static final Source CARGO_PENALTY_SOURCE
        = new Source("model.source.cargoPenalty");
    public static final Source AMBUSH_BONUS_SOURCE
        = new Source("model.source.ambushBonus");
    public static final Source COLONY_GOODS_PARTY_SOURCE
        = new Source("model.source.colonyGoodsParty");
    public static final Source SHIP_TRADE_PENALTY_SOURCE
        = new Source("model.source.shipTradePenalty");
    public static final Source SOL_MODIFIER_SOURCE
        = new Source("model.source.solModifier");


    /** A map from specification element to a reader for that element. */
    private final Map<String, ChildReader> readerMap
        = new HashMap<String, ChildReader>();

    /* Containers filled from readers in the readerMap. */

    // readerMap("building-types")
    private final List<BuildingType> buildingTypeList
        = new ArrayList<BuildingType>();
    // readerMap("disasters")
    private final List<Disaster> disasters
        = new ArrayList<Disaster>();
    // readerMap("equipment-types")
    private final List<EquipmentType> equipmentTypes
        = new ArrayList<EquipmentType>();
    // readerMap("european-nation-types")
    private final List<EuropeanNationType> europeanNationTypes
        = new ArrayList<EuropeanNationType>();
    // readerMap("events")
    private final List<Event> events
        = new ArrayList<Event>();
    // readerMap("founding-fathers")
    private final List<FoundingFather> foundingFathers
        = new ArrayList<FoundingFather>();
    // readerMap("goods-types")
    private final List<GoodsType> goodsTypeList
        = new ArrayList<GoodsType>();
    // readerMap("indian-nation-types")
    private final List<IndianNationType> indianNationTypes
        = new ArrayList<IndianNationType>();
    // readerMap("nations")
    private final List<Nation> nations
        = new ArrayList<Nation>();
    // readerMap("resource-types")
    private final List<ResourceType> resourceTypeList
        = new ArrayList<ResourceType>();
    // readerMap("roles")
    private final List<Role> roles
        = new ArrayList<Role>();
    // readerMap("tile-types")
    private final List<TileType> tileTypeList
        = new ArrayList<TileType>();
    // readerMap("tileimprovement-types")
    private final List<TileImprovementType> tileImprovementTypeList
        = new ArrayList<TileImprovementType>();
    // readerMap("unit-types")
    private final List<UnitType> unitTypeList
        = new ArrayList<UnitType>();

    // readerMap("modifiers")
    private final Map<String, List<Modifier>> allModifiers
        = new HashMap<String, List<Modifier>>();
    private final List<Modifier> specialModifiers
        = new ArrayList<Modifier>();

    // readerMap("options")
    private final Map<String, AbstractOption> allOptions
        = new HashMap<String, AbstractOption>();
    private final Map<String, OptionGroup> allOptionGroups
        = new HashMap<String, OptionGroup>();

    /* Containers derived from readerMap containers */

    // Derived from readerMap container: goodsTypeList
    private final List<GoodsType> farmedGoodsTypeList
        = new ArrayList<GoodsType>();
    private final List<GoodsType> foodGoodsTypeList
        = new ArrayList<GoodsType>();
    private final List<GoodsType> newWorldGoodsTypeList
        = new ArrayList<GoodsType>();
    private final List<GoodsType> libertyGoodsTypeList
        = new ArrayList<GoodsType>();
    private final List<GoodsType> immigrationGoodsTypeList
        = new ArrayList<GoodsType>();
    private final List<GoodsType> rawBuildingGoodsTypeList
        = new ArrayList<GoodsType>();
    private int storableTypes = 0;

    // Derived from readerMap container: nations
    private final List<Nation> europeanNations = new ArrayList<Nation>();
    private final List<Nation> REFNations = new ArrayList<Nation>();
    private final List<Nation> indianNations = new ArrayList<Nation>();
    // Derived from readerMap containers: indianNationTypes europeanNationTypes
    private final List<NationType> nationTypes
        = new ArrayList<NationType>();
    // Derived from readerMap container: europeanNationTypes
    private final List<EuropeanNationType> REFNationTypes
        = new ArrayList<EuropeanNationType>();

    // Derived from readerMap container: unitTypeList
    private final Map<GoodsType, UnitType> experts
        = new HashMap<GoodsType, UnitType>();
    private final List<UnitType> unitTypesTrainedInEurope
        = new ArrayList<UnitType>();
    private final List<UnitType> unitTypesPurchasedInEurope
        = new ArrayList<UnitType>();
    private UnitType fastestLandUnitType = null;
    private UnitType fastestNavalUnitType = null;


    /* Other containers. */

    private final Map<String, FreeColGameObjectType> allTypes
        = new HashMap<String, FreeColGameObjectType>();

    private final Map<String, List<Ability>> allAbilities
        = new HashMap<String, List<Ability>>();


    private boolean initialized = false;

    /** The specification identifier. */
    private String id;

    /** The specification version. */
    private String version;

    /** The name of the difficulty level option group. */
    private String difficultyLevel;


    /**
     * Creates a new Specification object.
     */
    public Specification() {
        logger.fine("Initializing Specification");
        for (Source source : new Source[] {
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
            }) {
            allTypes.put(source.getId(), source);
        }

        readerMap.put(BUILDING_TYPES_TAG,
                      new TypeReader<BuildingType>(BuildingType.class, buildingTypeList));
        readerMap.put(DISASTERS_TAG,
                      new TypeReader<Disaster>(Disaster.class, disasters));
        readerMap.put(EQUIPMENT_TYPES_TAG,
                      new TypeReader<EquipmentType>(EquipmentType.class, equipmentTypes));
        readerMap.put(EUROPEAN_NATION_TYPES_TAG,
                      new TypeReader<EuropeanNationType>(EuropeanNationType.class, europeanNationTypes));
        readerMap.put(EVENTS_TAG,
                      new TypeReader<Event>(Event.class, events));
        readerMap.put(FOUNDING_FATHERS_TAG,
                      new TypeReader<FoundingFather>(FoundingFather.class, foundingFathers));
        readerMap.put(GOODS_TYPES_TAG,
                      new TypeReader<GoodsType>(GoodsType.class, goodsTypeList));
        readerMap.put(INDIAN_NATION_TYPES_TAG,
                      new TypeReader<IndianNationType>(IndianNationType.class, indianNationTypes));
        readerMap.put(NATIONS_TAG,
                      new TypeReader<Nation>(Nation.class, nations));
        readerMap.put(RESOURCE_TYPES_TAG,
                      new TypeReader<ResourceType>(ResourceType.class, resourceTypeList));
        readerMap.put(ROLES_TAG,
                      new TypeReader<Role>(Role.class, roles));
        readerMap.put(TILE_TYPES_TAG,
                      new TypeReader<TileType>(TileType.class, tileTypeList));
        readerMap.put(TILEIMPROVEMENT_TYPES_TAG,
                      new TypeReader<TileImprovementType>(TileImprovementType.class, tileImprovementTypeList));
        readerMap.put(UNIT_TYPES_TAG,
                      new TypeReader<UnitType>(UnitType.class, unitTypeList));

        readerMap.put(MODIFIERS_TAG, new ModifierReader());
        readerMap.put(OPTIONS_TAG, new OptionReader());
    }

    /**
     * Creates a new Specification object by loading it from the
     * given <code>FreeColXMLReader</code>.
     *
     * @param xr The <code>FreeColXMLReader</code> to read from.
     */
    public Specification(FreeColXMLReader xr) {
        this();
        initialized = false;
        load(xr);
        clean("load from stream");
        initialized = true;
    }

    /**
     * Load a specification from a stream.
     *
     * @param xr The <code>FreeColXMLReader</code> to read from.
     */
    private void load(FreeColXMLReader xr) {
        try {
            readFromXML(xr);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Load exception", e);
            throw new RuntimeException("Error parsing specification: "
                + e.getMessage());
        }
    }

    /**
     * Creates a new Specification object by loading it from the
     * given <code>InputStream</code>.
     *
     * @param in The <code>InputStream</code> to read from.
     */
    public Specification(InputStream in) {
        this();
        initialized = false;
        load(in);
        clean("load from InputStream");
        initialized = true;
    }

    /**
     * Load a specification from a stream.
     *
     * @param in The <code>InputStream</code> to read from.
     */
    private void load(InputStream in) {
        FreeColXMLReader xr = null;
        try {
            xr = new FreeColXMLReader(in);
            xr.nextTag();
            load(xr);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Load stream exception", e);
            throw new RuntimeException("Error parsing specification: "
                + e.getMessage());
        } finally {
            if (xr != null) xr.close();
        }
    }

    /**
     * Load mods into this specification.
     *
     * @param mods A list of <code>FreeColModFile</code>s to load.
     * @return True if any mod was loaded.
     */
    public boolean loadMods(List<FreeColModFile> mods) {
        initialized = false;
        boolean loadedMod = false;
        for (FreeColModFile mod : mods) {
            InputStream sis = null;
            try {
                sis = mod.getSpecificationInputStream();
                load(sis);
                loadedMod = true;
                logger.info("Loaded mod " + mod.getId());
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Read error in mod " + mod.getId(),
                    ioe);
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

        Iterator<FreeColGameObjectType> typeIterator
            = allTypes.values().iterator();
        while (typeIterator.hasNext()) {
            FreeColGameObjectType type = typeIterator.next();
            if (type.isAbstractType()) {
                typeIterator.remove();
            }
        }

        farmedGoodsTypeList.clear();
        foodGoodsTypeList.clear();
        newWorldGoodsTypeList.clear();
        libertyGoodsTypeList.clear();
        immigrationGoodsTypeList.clear();
        rawBuildingGoodsTypeList.clear();
        storableTypes = 0;
        for (GoodsType goodsType : goodsTypeList) {
            if (goodsType.isFarmed()) {
                farmedGoodsTypeList.add(goodsType);
            }
            if (goodsType.isFoodType()) {
                foodGoodsTypeList.add(goodsType);
            }
            if (goodsType.isNewWorldGoodsType()) {
                newWorldGoodsTypeList.add(goodsType);
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
            if (goodsType.isStorable()) {
                storableTypes++;
            }
        }

        REFNations.clear();
        europeanNations.clear();
        indianNations.clear();
        for (Nation nation : nations) {
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
        Iterator<EuropeanNationType> iterator = europeanNationTypes.iterator();
        while (iterator.hasNext()) {
            EuropeanNationType nationType = iterator.next();
            if (nationType.isREF()) {
                REFNationTypes.add(nationType);
                iterator.remove();
            }
        }

        experts.clear();
        unitTypesTrainedInEurope.clear();
        unitTypesPurchasedInEurope.clear();
        int bestLandValue = -1, bestNavalValue = -1;
        for (UnitType unitType : unitTypeList) {
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

        // Set difficulty level before options processing.
        if (difficultyLevel != null) {
            applyDifficultyLevel(difficultyLevel);
        }

        // Initialize UI containers.
        for (AbstractOption option : allOptions.values()) {
            option.generateChoices();
        }

        // Initialize the Turn class using GameOptions.
        try {
            int startingYear = getInteger(GameOptions.STARTING_YEAR);
            int seasonYear = getInteger(GameOptions.SEASON_YEAR);
            if (seasonYear < startingYear) seasonYear = startingYear;
            Turn.setStartingYear(startingYear);
            Turn.setSeasonYear(seasonYear);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to set year options", e);
        }

        logger.info("Specification clean following " + why + " complete"
                    + ", starting year=" + Turn.getStartingYear()
                    + ", season year=" + Turn.getSeasonYear()
                    + ", " + allTypes.size() + " FreeColGameObjectTypes"
                    + ", " + allOptions.size() + " Options"
                    + ", " + allAbilities.size() + " Abilities"
                    + ", " + allModifiers.size() + " Modifiers read.");
    }

    /**
     * Generate the dynamic options.
     *
     * Only call this in the server.  If clients call it the European
     * prices can be desynchronized.
     */
    public void generateDynamicOptions() {
        logger.finest("Generating dynamic options.");
        OptionGroup prices = new OptionGroup("gameOptions.prices", this);
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
            } else if (goodsType.getPrice() < FreeColGameObjectType.INFINITY) {
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


    private interface ChildReader {
        public void readChildren(FreeColXMLReader xr) throws XMLStreamException;
    }

    private class ModifierReader implements ChildReader {

        public void readChildren(FreeColXMLReader xr) throws XMLStreamException {
            while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                Modifier modifier = new Modifier(xr, Specification.this);
                Specification.this.addModifier(modifier);
                Specification.this.specialModifiers.add(modifier);
            }
        }
    }

    private class TypeReader<T extends FreeColGameObjectType> implements ChildReader {

        private Class<T> type;
        private List<T> result;
        private int index = 0;

        // Is there really no easy way to capture T?
        public TypeReader(Class<T> type, List<T> listToFill) {
            result = listToFill;
            this.type = type;
        }

        public void readChildren(FreeColXMLReader xr) throws XMLStreamException {
            while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                final String tag = xr.getLocalName();
                String id = xr.readId();
                if (id == null) {
                    logger.warning("Null identifier, tag: " + tag);

                } else if (FreeColGameObjectType.DELETE_TAG.equals(tag)) {
                    FreeColGameObjectType object = allTypes.remove(id);
                    if (object != null) result.remove(object);

                } else {
                    T object = getType(id, type);
                    allTypes.put(id, object);

                    // If this an existing object (with id) and the
                    // PRESERVE tag is present, then leave the
                    // attributes intact and only read the child
                    // elements, otherwise do a full attribute
                    // inclusive read.  This allows mods and spec
                    // extensions to not have to re-specify all the
                    // attributes when just changing the children.
                    if (object.getId() != null
                        && xr.getAttribute(FreeColGameObjectType.PRESERVE_TAG,
                                           (String)null) != null) {
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

    /**
     * Options are special as they live in the allOptionGroups
     * collection, which has its own particular semantics.  So they
     * need their own reader.
     */
    private class OptionReader implements ChildReader {

        private static final String RECURSIVE_TAG = "recursive";

        public void readChildren(FreeColXMLReader xr) throws XMLStreamException {
            while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                readChild(xr);
            }
        }

        private void readChild(FreeColXMLReader xr) throws XMLStreamException {
            final String tag = xr.getLocalName();

            boolean recursive = xr.getAttribute(RECURSIVE_TAG, true);

            if (OptionGroup.getXMLElementTagName().equals(tag)) {
                String id = xr.readId();
                OptionGroup group = allOptionGroups.get(id);
                if (group == null) {
                    group = new OptionGroup(id, Specification.this);
                    allOptionGroups.put(id, group);
                }
                group.readFromXML(xr);
                Specification.this.addOptionGroup(group, recursive);

            } else {
                logger.warning(OptionGroup.getXMLElementTagName()
                    + " expected in OptionReader, not: " + tag);
                xr.nextTag();
            }
        }
    }

    // ---------------------------------------------------------- retrieval
    // methods

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
     * Registers an Ability as defined.
     *
     * @param ability an <code>Ability</code> value
     */
    public void addAbility(Ability ability) {
        String id = ability.getId();
        addAbility(id);
        allAbilities.get(id).add(ability);
    }

    /**
     * Registers an Ability's id as defined.  This is useful for
     * abilities that are required rather than provided by
     * FreeColGameObjectTypes.
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
     */
    public List<Ability> getAbilities(String id) {
        return allAbilities.get(id);
    }

    /**
     * Add a modifier.
     *
     * @param modifier The <code>Modifier</code> to add.
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
     */
    public List<Modifier> getModifiers(String id) {
        return allModifiers.get(id);
    }

    // Option routines

    /**
     * Is option with this identifier present?  This is helpful when
     * options are optionally(!) present, for example
     * model.option.priceIncrease.artillery exists but
     * model.option.priceIncrease.frigate does not.
     *
     * @param id The object identifier to test.
     * @return True/false on presence of option id.
     */
    public boolean hasOption(String id) {
        return id != null && allOptions.containsKey(id);
    }

    /**
     * Get the <code>AbstractOption</code> with the given identifier.
     *
     * @param id The object identifier.
     * @return The <code>AbstractOption</code> found.
     * @exception IllegalArgumentException if the identifier is null
     *     or not present.
     */
    public AbstractOption getOption(String id) throws IllegalArgumentException {
        if (id == null) {
            throw new IllegalArgumentException("AbstractOption with null id.");
        } else if (!allOptions.containsKey(id)) {
            throw new IllegalArgumentException("Missing AbstractOption: " + id);
        } else {
            return allOptions.get(id);
        }
    }

    /**
     * Get the <code>OptionGroup</code> with the given identifier.
     *
     * @param id The object identifier.
     * @return The <code>OptionGroup</code> found.
     * @exception IllegalArgumentException if the identifier is null
     *     or not present.
     */
    public OptionGroup getOptionGroup(String id) throws IllegalArgumentException {
        if (id == null) {
            throw new IllegalArgumentException("OptionGroup with null id.");
        } else if (!allOptionGroups.containsKey(id)) {
            throw new IllegalArgumentException("Missing OptionGroup: " + id);
        } else {
            return allOptionGroups.get(id);
        }
    }

    /**
     * Fix up missing option groups.
     *
     * @param optionGroup The missing <code>OptionGroup</code>.
     * @param difficulty If true, add the option group to the difficulty levels.
     */
    public void fixOptionGroup(OptionGroup optionGroup, boolean difficulty) {
        if (difficulty) {
            for (Option option : allOptionGroups.get("difficultyLevels")
                     .getOptions()) {
                if (option instanceof OptionGroup) {
                    OptionGroup level = (OptionGroup) option;
                    if (level.hasOptionGroup()) {
                        level.add(optionGroup);
                    } else {
                        for (Option o : optionGroup.getOptions()) {
                            level.add(o);
                        }
                    }
                }
            }
        } else {
            if (!allOptionGroups.containsKey(optionGroup.getId())) {
                allOptionGroups.put(optionGroup.getId(), optionGroup);
            }
        }
    }

    /**
     * Adds an <code>OptionGroup</code> to this specification.
     *
     * @param optionGroup The <code>OptionGroup</code> to add.
     * @param recursive If true, add recursively to subgroups.
     */
    public void addOptionGroup(OptionGroup optionGroup, boolean recursive) {
        // Add the options of the group
        Iterator<Option> iter = optionGroup.iterator();

        while (iter.hasNext()) {
            Option option = iter.next();
            if (option instanceof OptionGroup) {
                allOptionGroups.put(option.getId(), (OptionGroup) option);
                if (recursive) {
                    addOptionGroup((OptionGroup) option, true);
                }
            } else {
                addAbstractOption((AbstractOption) option);
            }
        }
    }

    /**
     * Adds an <code>AbstractOption</code> to this specification.
     *
     * @param abstractOption The <code>AbstractOption</code> to add.
     */
    public void addAbstractOption(AbstractOption abstractOption) {
        // Add the option
        allOptions.put(abstractOption.getId(), abstractOption);
    }

    /**
     * Get the <code>IntegerOption</code> with the given identifier.
     *
     * @param id The object identifier.
     * @return The <code>IntegerOption</code> found.
     */
    public IntegerOption getIntegerOption(String id) {
        return (IntegerOption)getOption(id);
    }

    /**
     * Get the <code>RangeOption</code> with the given identifier.
     *
     * @param id The object identifier.
     * @return The <code>RangeOption</code> found.
     */
    public RangeOption getRangeOption(String id) {
        return (RangeOption)getOption(id);
    }

    /**
     * Get the <code>BooleanOption</code> with the given identifier.
     *
     * @param id The object identifier.
     * @return The <code>BooleanOption</code> found.
     */
    public BooleanOption getBooleanOption(String id) {
        return (BooleanOption)getOption(id);
    }

    /**
     * Get the <code>StringOption</code> with the given identifier.
     *
     * @param id The object identifier.
     * @return The <code>StringOption</code> found.
     */
    public StringOption getStringOption(String id) {
        return (StringOption) getOption(id);
    }

    /**
     * Gets the boolean value of an option.
     *
     * @param id The object identifier.
     * @return The value.
     * @exception IllegalArgumentException If there is no boolean
     *     value associated with the specified option.
     * @exception NullPointerException if the given
     *     <code>Option</code> does not exist.
     */
    public boolean getBoolean(String id) {
        try {
            return getBooleanOption(id).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Not a boolean option: " + id);
        }
    }

    /**
     * Gets the integer value of an option.
     *
     * @param id The object identifier.
     * @return The value.
     * @exception IllegalArgumentException If there is no integer
     *     value associated with the specified option.
     * @exception NullPointerException if the given
     *     <code>Option</code> does not exist.
     */
    public int getInteger(String id) {
        try {
            return getIntegerOption(id).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Not an integer option: " + id);
        }
    }

    /**
     * Gets the string value of an option.
     *
     * @param id The object identifier.
     * @return The value.
     * @exception IllegalArgumentException If there is no string
     *     value associated with the specified option.
     * @exception NullPointerException if the given
     *     <code>Option</code> does not exist.
     */
    public String getString(String id) {
        try {
            return getStringOption(id).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Not a string option: " + id);
        }
    }


    // -- Buildings --

    public List<BuildingType> getBuildingTypeList() {
        return buildingTypeList;
    }

    /**
     * Get a building type by identifier.
     *
     * @param id The object identifier.
     * @return The <code>BuildingType</code> found.
     */
    public BuildingType getBuildingType(String id) {
        return getType(id, BuildingType.class);
    }

    // -- Goods --

    public List<GoodsType> getGoodsTypeList() {
        return goodsTypeList;
    }

    public List<GoodsType> getFarmedGoodsTypeList() {
        return farmedGoodsTypeList;
    }

    public List<GoodsType> getNewWorldGoodsTypeList() {
        return newWorldGoodsTypeList;
    }

    public List<GoodsType> getLibertyGoodsTypeList() {
        return libertyGoodsTypeList;
    }

    public List<GoodsType> getImmigrationGoodsTypeList() {
        return immigrationGoodsTypeList;
    }

    public List<GoodsType> getFoodGoodsTypeList() {
        return foodGoodsTypeList;
    }

    public final List<GoodsType> getRawBuildingGoodsTypeList() {
        return rawBuildingGoodsTypeList;
    }

    /**
     * The general "Food" type is handled as a special case in many places.
     * Introduce this routine to collect them into one place, in the hope
     * we can one day deprecate this routine and clean up the special cases.
     *
     * @return The main food type ("model.goods.food").
     */
    public GoodsType getPrimaryFoodType() {
        return getGoodsType("model.goods.food");
    }

    /**
     * Get the initial <em>minimum</em> price of the given goods
     * type. The initial price in a particular Market may be higher.
     *
     * @param goodsType The <code>GoodsType</code> to check.
     * @return The minimum price.
     */
    public int getInitialPrice(GoodsType goodsType) {
        String suffix = goodsType.getSuffix("model.goods.");
        String minPrice = "model.option." + suffix + ".minimumPrice";
        String maxPrice = "model.option." + suffix + ".maximumPrice";
        return (hasOption(minPrice) && hasOption(maxPrice))
            ? Math.min(getInteger(minPrice), getInteger(maxPrice))
            : goodsType.getInitialSellPrice();
    }

    /**
     * Get a goods type by identifier.
     *
     * @param id The object identifier.
     * @return The <code>GoodsType</code> found.
     */
    public GoodsType getGoodsType(String id) {
        return getType(id, GoodsType.class);
    }

    // -- Resources --

    public List<ResourceType> getResourceTypeList() {
        return resourceTypeList;
    }

    /**
     * Get a resource type by identifier.
     *
     * @param id The object identifier.
     * @return The <code>ResourceType</code> found.
     */
    public ResourceType getResourceType(String id) {
        return getType(id, ResourceType.class);
    }

    // -- Tiles --

    public List<TileType> getTileTypeList() {
        return tileTypeList;
    }

    /**
     * Get a tile type by identifier.
     *
     * @param id The object identifier.
     * @return The <code>TileType</code> found.
     */
    public TileType getTileType(String id) {
        return getType(id, TileType.class);
    }

    // -- Improvements --

    public List<TileImprovementType> getTileImprovementTypeList() {
        return tileImprovementTypeList;
    }

    /**
     * Get a tile improvement type by identifier.
     *
     * @param id The object identifier.
     * @return The <code>TileImprovementType</code> found.
     */
    public TileImprovementType getTileImprovementType(String id) {
        return getType(id, TileImprovementType.class);
    }

    // -- Units --

    public List<UnitType> getUnitTypeList() {
        return unitTypeList;
    }

    /**
     * Gets the most vanilla unit type.
     *
     * Provides a type to use to make a neutral comparison of the
     * productivity of work locations.
     *
     * @return The free colonist unit type.
     */
    public UnitType getDefaultUnitType() {
        return getUnitType("model.unit.freeColonist");
    }

    /**
     * Get the unit type that is the expert for producing a type of goods.
     *
     * @param goodsType The <code>GoodsType</code> to check.
     * @return The expert <code>UnitType</code>, or null if none.
     */
    public UnitType getExpertForProducing(GoodsType goodsType) {
        return experts.get(goodsType);
    }

    /**
     * Get the unit types which have any of the given abilities
     *
     * @param abilities The abilities for the search
     * @return A list of <code>UnitType</code>s with the abilities.
     */
    public List<UnitType> getUnitTypesWithAbility(String... abilities) {
        return getTypesWithAbility(UnitType.class, abilities);
    }

    /**
     * Get the unit types which have none of the given abilities
     *
     * @param abilities The abilities for the search
     * @return A list of <code>UnitType</code>s without the abilities.
     */
    public List<UnitType> getUnitTypesWithoutAbility(String... abilities) {
        return getTypesWithoutAbility(UnitType.class, abilities);
    }

    /**
     * Gets the unit types that can be trained in Europe.
     *
     * @return A list of Europe-trainable <code>UnitType</code>s.
     */
    public List<UnitType> getUnitTypesTrainedInEurope() {
        return unitTypesTrainedInEurope;
    }

    /**
     * Get the unit types that can be purchased in Europe.
     *
     * @return A list of Europe-purchasable <code>UnitType</code>s.
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
     */
    public List<UnitType> getREFUnitTypes(boolean naval) {
        List<UnitType> types = new ArrayList<UnitType>();
        for (UnitType ut : getUnitTypesWithAbility("model.ability.refUnit")) {
            if (naval == ut.isNaval()) types.add(ut);
        }
        return types;
    }

    /**
     * Get a unit type by identifier.
     *
     * @param id The object identifier.
     * @return The <code>UnitType</code> found.
     */
    public UnitType getUnitType(String id) {
        return getType(id, UnitType.class);
    }

    // -- Founding Fathers --

    public List<FoundingFather> getFoundingFathers() {
        return foundingFathers;
    }

    /**
     * Get a founding father type by identifier.
     *
     * @param id The object identifier.
     * @return The <code>FoundingFather</code> found.
     */
    public FoundingFather getFoundingFather(String id) {
        return getType(id, FoundingFather.class);
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
     * @return The <code>NationType</code> found.
     */
    public NationType getNationType(String id) {
        return getType(id, NationType.class);
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
     * @return The <code>Nation</code> found.
     */
    public Nation getNation(String id) {
        return getType(id, Nation.class);
    }

    // -- Roles --

    public List<Role> getRoles() {
        return roles;
    }

    /**
     * Get a role by identifier.
     *
     * @param id The object identifier.
     * @return The <code>Role</code> found.
     */
    public Role getRole(String id) {
        return getType(id, Role.class);
    }

    // -- EquipmentTypes --

    public List<EquipmentType> getEquipmentTypeList() {
        return equipmentTypes;
    }

    /**
     * Get an equipment type by identifier.
     *
     * @param id The object identifier.
     * @return The <code>EquipmentType</code> found.
     */
    public EquipmentType getEquipmentType(String id) {
        return getType(id, EquipmentType.class);
    }

    // -- DifficultyLevels --

    /**
     * Gets the difficulty levels in this specification.
     *
     * @return A list of difficulty levels in this specification.
     */
    public List<OptionGroup> getDifficultyLevels() {
        List<OptionGroup> result = new ArrayList<OptionGroup>();
        for (Option option : allOptionGroups.get("difficultyLevels").getOptions()) {
            if (option instanceof OptionGroup) {
                result.add((OptionGroup) option);
            }
        }
        return result;
    }

    /**
     * Gets the current difficulty level.
     *
     * @return The current difficulty level.
     */
    public OptionGroup getDifficultyLevel() {
        return allOptionGroups.get(difficultyLevel);
    }

    /**
     * Gets a difficulty level by id.
     *
     * @param id The id to look for.
     * @return The corresponding difficulty level, if any.
     */
    public OptionGroup getDifficultyLevel(String id) {
        return allOptionGroups.get(id);
    }

    /**
     * Applies the difficulty level identified by the given String to
     * the current specification.
     *
     * @param difficultyLevel The id of a difficulty level to apply
     */
    public void applyDifficultyLevel(String difficultyLevel) {
        applyDifficultyLevel(getDifficultyLevel(difficultyLevel));
    }

    /**
     * Applies the given difficulty level to the current
     * specification.
     *
     * @param level The difficulty level <code>OptionGroup</code> to apply.
     */
    public void applyDifficultyLevel(OptionGroup level) {
        if (level == null) return;
        logger.fine("Applying difficulty level " + level.getId());
        addOptionGroup(level, true);

        for (FreeColGameObjectType type : allTypes.values()) {
            type.applyDifficultyLevel(level);
        }

        this.difficultyLevel = level.getId();
    }

    public OptionGroup getGameOptions() {
        return getOptionGroup("gameOptions");
    }

    public OptionGroup getMapGeneratorOptions() {
        return getOptionGroup(MapGeneratorOptions.getXMLElementTagName());
    }


    // -- Events --

    public List<Event> getEvents() {
        return events;
    }

    /**
     * Get an event by identifier.
     *
     * @param id The object identifier.
     * @return The <code>Event</code> found.
     */
    public Event getEvent(String id) {
        return getType(id, Event.class);
    }

    // -- Disasters --

    public List<Disaster> getDisasters() {
        return disasters;
    }

    /**
     * Get a disaster by identifier.
     *
     * @param id The object identifier.
     * @return The <code>Disaster</code> found.
     */
    public Disaster getDisaster(String id) {
        return getType(id, Disaster.class);
    }

    // General type retrieval

    /**
     * Get the <code>FreeColGameObjectType</code> with the given identifier.
     *
     * @param id The object identifier to look for.
     * @param type The expected <code>Class</code>.
     * @return The <code>FreeColGameObjectType</code> found.
     */
    public <T extends FreeColGameObjectType> T getType(String id, Class<T> type) {
        FreeColGameObjectType o = findType(id);
        if (o != null) {
            return type.cast(allTypes.get(id));

        } else if (initialized) {
            throw new IllegalArgumentException("Undefined FCGOT: " + id);

        } else { // forward declaration of new type
            try {
                Constructor<T> c = type.getConstructor(String.class,
                                                       Specification.class);
                T result = c.newInstance(id, this);
                allTypes.put(id, result);
                return result;
            } catch (Exception e) {
                logger.log(Level.WARNING, "Could not construct: " + id, e);
            }
        }
        return null;
    }

    // @compat 0.9.x
    private String mangle(String id) {
        int index = id.lastIndexOf('.');
        return (index == -1) ? id
            : id.substring(0, index + 1) + id.substring(index + 1,
                index + 2).toLowerCase(Locale.US) + id.substring(index + 2);
    }
    // end @compat

    /**
     * Find a <code>FreeColGameObjectType</code> by id.
     *
     * @param id The identifier to look for, which must not be null.
     * @return The <code>FreeColGameObjectType</code> found if any.
     */
    public FreeColGameObjectType findType(String id) throws IllegalArgumentException {
        if (id == null) {
            throw new IllegalArgumentException("Null id");

        } else if (allTypes.containsKey(id)) {
            return allTypes.get(id);

        // @compat 0.9.x
        } else if (allTypes.containsKey(mangle(id))) {
            return allTypes.get(mangle(id));
        // end @compat

        } else {
            return null;
        }
    }

    /**
     * Get the FreeColGameObjectTypes that provide the required ability.
     *
     * @param id The object identifier.
     * @param value The ability value to check.
     * @return A list of <code>FreeColGameObjectType</code>s that
     *     provide the required ability.
     */
    public List<FreeColGameObjectType> getTypesProviding(String id,
                                                         boolean value) {
        List<FreeColGameObjectType> result
            = new ArrayList<FreeColGameObjectType>();
        for (Ability ability : getAbilities(id)) {
            if (ability.getValue() == value
                && ability.getSource() instanceof FreeColGameObjectType) {
                result.add((FreeColGameObjectType) ability.getSource());
            }
        }
        return result;
    }

    /**
     * Get all types which have any of the given abilities.
     *
     * @param abilities The abilities for the search
     * @return A list of <code>FreeColGameObjectType</code>s with at
     *     least one of the given abilities.
     */
    public <T extends FreeColGameObjectType> List<T>
                      getTypesWithAbility(Class<T> resultType,
                                          String... abilities) {
        List<T> result = new ArrayList<T>();
        for (FreeColGameObjectType type : allTypes.values()) {
            if (resultType.isInstance(type)) {
                for (String ability : abilities) {
                    if (type.hasAbility(ability)) {
                        result.add(resultType.cast(type));
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Get all types which have none of the given abilities.
     *
     * @param abilities The abilities for the search
     * @return A list of <code>FreeColGameObjectType</code>s without the
     *     given abilities.
     */
    public <T extends FreeColGameObjectType> List<T>
                      getTypesWithoutAbility(Class<T> resultType,
                                             String... abilities) {
        List<T> result = new ArrayList<T>();
        type: for (FreeColGameObjectType type : allTypes.values()) {
            if (resultType.isInstance(type)) {
                for (String ability : abilities) {
                    if (type.hasAbility(ability)) continue type;
                }
                result.add(resultType.cast(type));
            }
        }
        return result;
    }


    // Serialization

    private static final String BUILDING_TYPES_TAG = "building-types";
    private static final String DIFFICULTY_LEVEL_TAG = "difficultyLevel";
    private static final String DISASTERS_TAG = "disasters";
    private static final String EQUIPMENT_TYPES_TAG = "equipment-types";
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
    private static final String TILEIMPROVEMENT_TYPES_TAG = "tileimprovement-types";
    private static final String UNIT_TYPES_TAG = "unit-types";
    private static final String VERSION_TAG = "version";


    /**
     * Write an XML-representation of this object to the given stream.
     *
     * @param xw The <code>FreeColXMLWriter</code> to write to.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        // Start element
        xw.writeStartElement(getXMLElementTagName());

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
        writeSection(xw, EQUIPMENT_TYPES_TAG, equipmentTypes);
        writeSection(xw, TILEIMPROVEMENT_TYPES_TAG, tileImprovementTypeList);
        writeSection(xw, UNIT_TYPES_TAG, unitTypeList);
        writeSection(xw, BUILDING_TYPES_TAG, buildingTypeList);
        writeSection(xw, FOUNDING_FATHERS_TAG, foundingFathers);
        writeSection(xw, EUROPEAN_NATION_TYPES_TAG, europeanNationTypes);
        writeSection(xw, EUROPEAN_NATION_TYPES_TAG, REFNationTypes);
        writeSection(xw, INDIAN_NATION_TYPES_TAG, indianNationTypes);
        writeSection(xw, NATIONS_TAG, nations);

        // option tree has been flattened
        xw.writeStartElement(OPTIONS_TAG);
        for (OptionGroup item : allOptionGroups.values()) {
            if ("".equals(item.getGroup())) {
                item.toXML(xw);
            }
        }
        xw.writeEndElement();

        // End element
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
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @exception XMLStreamException if there are any problems reading
     *     the stream.
     */
    public void readFromXML(FreeColXMLReader xr) throws XMLStreamException {
        String newId = xr.readId();
        if (difficultyLevel == null) {
            difficultyLevel = xr.getAttributeValue(null, DIFFICULTY_LEVEL_TAG);
        }
        logger.fine("Difficulty level is " + difficultyLevel);
        if (id == null) id = newId; // don't overwrite id with parent id!

        logger.fine("Reading specification " + newId);
        String parentId = xr.getAttribute(FreeColGameObjectType.EXTENDS_TAG,
                                          (String)null);
        if (parentId != null) {
            try {
                FreeColTcFile parent = new FreeColTcFile(parentId);
                load(parent.getSpecificationInputStream());
                initialized = false;
            } catch (IOException e) {
                throw new XMLStreamException("Failed to open parent specification: " + e);
            }
        }
        while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = xr.getLocalName();
            logger.finest("Found child named " + childName);
            ChildReader reader = readerMap.get(childName);
            if (reader == null) {
                // @compat 0.9.x
                if ("improvementaction-types".equals(childName)) {
                    while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        // skip children
                        while ("action".equals(xr.getLocalName())) {
                            xr.nextTag();
                        }
                    }
                // end @compat
                } else {
                    throw new RuntimeException("unexpected: " + childName);
                }
            } else {
                reader.readChildren(xr);
            }
        }

        // @compat 0.9.x
        for (BuildingType bt : getBuildingTypeList()) {
            bt.fixup09x();
        }
        if (getModifiers("model.modifier.nativeTreasureModifier") != null) {
            for (FoundingFather ff : getFoundingFathers()) {
                ff.fixup09x();
            }
        }
        // end @compat

        // @compat 0.10.1
        String[] years = new String[] {
            "startingYear", "seasonYear", "mandatoryColonyYear",
            "lastYear", "lastColonialYear"
        };
        int[] values = new int[] { 1492, 1600, 1600, 1850, 1800 };
        for (int index = 0; index < years.length; index++) {
            String id = "model.option." + years[index];
            if (allOptions.get(id) == null) {
                IntegerOption option = new IntegerOption(id);
                option.setValue(values[index]);
                allOptions.put(id, option);
            }
        }
        // end @compat

        // @compat 0.10.5
        String id = "model.option.interventionBells";
        if (allOptions.get(id) == null) {
            IntegerOption interventionBells = new IntegerOption(id);
            interventionBells.setValue(5000);
            allOptions.put(id, interventionBells);
        }
        id = "model.option.interventionTurns";
        if (allOptions.get(id) == null) {
            IntegerOption interventionTurns = new IntegerOption(id);
            interventionTurns.setValue(52);
            allOptions.put(id, interventionTurns);
        }
        id = "model.option.interventionForce";
        if (allOptions.get(id) == null) {
            UnitListOption interventionForce = new UnitListOption(id);
            AbstractUnitOption regulars
                = new AbstractUnitOption(id + ".regulars");
            regulars.setValue(new AbstractUnit("model.unit.colonialRegular",
                                               Unit.Role.SOLDIER, 2));
            interventionForce.getValue().add(regulars);
            AbstractUnitOption dragoons
                = new AbstractUnitOption(id + ".dragoons");
            dragoons.setValue(new AbstractUnit("model.unit.colonialRegular",
                                               Unit.Role.DRAGOON, 2));
            interventionForce.getValue().add(dragoons);
            AbstractUnitOption artillery
                = new AbstractUnitOption(id + ".artillery");
            artillery.setValue(new AbstractUnit("model.unit.artillery",
                                                Unit.Role.DEFAULT, 2));
            interventionForce.getValue().add(artillery);
            AbstractUnitOption menOfWar
                = new AbstractUnitOption(id + ".menOfWar");
            menOfWar.setValue(new AbstractUnit("model.unit.manOWar",
                                               Unit.Role.DEFAULT, 2));
            interventionForce.getValue().add(menOfWar);
            allOptions.put(id, interventionForce);
        }
        id = "model.option.mercenaryForce";
        if (allOptions.get(id) == null) {
            UnitListOption mercenaryForce = new UnitListOption(id);
            AbstractUnitOption regulars
                = new AbstractUnitOption(id + ".regulars");
            regulars.setValue(new AbstractUnit("model.unit.veteranSoldier",
                                               Unit.Role.SOLDIER, 2));
            mercenaryForce.getValue().add(regulars);
            AbstractUnitOption dragoons
                = new AbstractUnitOption(id + ".dragoons");
            dragoons.setValue(new AbstractUnit("model.unit.veteranSoldier",
                                               Unit.Role.DRAGOON, 2));
            mercenaryForce.getValue().add(dragoons);
            AbstractUnitOption artillery
                = new AbstractUnitOption(id + ".artillery");
            artillery.setValue(new AbstractUnit("model.unit.artillery",
                                                Unit.Role.DEFAULT, 2));
            mercenaryForce.getValue().add(artillery);
            AbstractUnitOption menOfWar
                = new AbstractUnitOption(id + ".menOfWar");
            menOfWar.setValue(new AbstractUnit("model.unit.manOWar",
                                               Unit.Role.DEFAULT, 2));
            mercenaryForce.getValue().add(menOfWar);
            allOptions.put(id, mercenaryForce);
        }
        id = "model.option.goodGovernmentLimit";
        if (allOptions.get(id) == null) {
            IntegerOption goodGovernmentLimit = new IntegerOption(id);
            goodGovernmentLimit.setValue(50);
            allOptions.put(id, goodGovernmentLimit);
        }
        id = "model.option.veryGoodGovernmentLimit";
        if (allOptions.get(id) == null) {
            IntegerOption veryGoodGovernmentLimit = new IntegerOption(id);
            veryGoodGovernmentLimit.setValue(100);
            allOptions.put(id, veryGoodGovernmentLimit);
        }
        EquipmentType missionaryEquipment
            = getEquipmentType("model.equipment.missionary");
        if (missionaryEquipment != null) {
            for (String as : new String[] { "model.ability.establishMission",
                                            "model.ability.denounceHeresy",
                                            "model.ability.inciteNatives" }) {
                List<Ability> al = allAbilities.get(as);
                if (al != null) {
                    for (Ability a : al) missionaryEquipment.addAbility(a);
                }
            }
        }
        // end @compat

        // @compat 0.10.7
        for (EuropeanNationType ent : europeanNationTypes) {
            if (ent.hasAbility(Ability.FOUND_COLONY)) {
                ent.removeAbilities(Ability.FOUND_COLONY);
                ent.addAbility(new Ability(Ability.FOUNDS_COLONIES, ent, true));
            }
        }
        // end @compat

        if (getREFUnitTypes(true).isEmpty()) {
            logger.warning("No naval REF units, REF will not function.");
        } else if (getREFUnitTypes(false).isEmpty()) {
            logger.warning("No land REF units, REF will not function.");
        }

        initialized = true;
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "freecol-specification".
     */
    public static String getXMLElementTagName() {
        return "freecol-specification";
    }
}
