/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.option.AbstractOption;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.RangeOption;
import net.sf.freecol.common.option.StringOption;

/**
 * This class encapsulates any parts of the "specification" for FreeCol that are
 * expressed best using XML. The XML is loaded through the class loader from the
 * resource named "specification.xml" in the same package as this class.
 */
public final class Specification {

    public static final FreeColGameObjectType MOVEMENT_PENALTY_SOURCE =
        new FreeColGameObjectType("model.source.movementPenalty");
    public static final FreeColGameObjectType ARTILLERY_PENALTY_SOURCE =
        new FreeColGameObjectType("model.source.artilleryInTheOpen");
    public static final FreeColGameObjectType ATTACK_BONUS_SOURCE =
        new FreeColGameObjectType("model.source.attackBonus");
    public static final FreeColGameObjectType FORTIFICATION_BONUS_SOURCE =
        new FreeColGameObjectType("model.source.fortified");
    public static final FreeColGameObjectType INDIAN_RAID_BONUS_SOURCE =
        new FreeColGameObjectType("model.source.artilleryAgainstRaid");
    public static final FreeColGameObjectType BASE_OFFENCE_SOURCE =
        new FreeColGameObjectType("model.source.baseOffence");
    public static final FreeColGameObjectType BASE_DEFENCE_SOURCE =
        new FreeColGameObjectType("model.source.baseDefence");
    public static final FreeColGameObjectType CARGO_PENALTY_SOURCE =
        new FreeColGameObjectType("model.source.cargoPenalty");
    public static final FreeColGameObjectType AMBUSH_BONUS_SOURCE =
        new FreeColGameObjectType("model.source.ambushBonus");
    public static final FreeColGameObjectType COLONY_GOODS_PARTY_SOURCE =
        new FreeColGameObjectType("model.source.colonyGoodsParty");


    private static final Logger logger = Logger.getLogger(Specification.class.getName());

    private final Map<String, FreeColGameObjectType> allTypes = new HashMap<String, FreeColGameObjectType>();

    private final Map<String, AbstractOption> allOptions = new HashMap<String, AbstractOption>();

    private final Map<String, OptionGroup> allOptionGroups = new HashMap<String, OptionGroup>();

    private final Map<GoodsType, UnitType> experts = new HashMap<GoodsType, UnitType>();

    private final Map<String, List<Ability>> allAbilities = new HashMap<String, List<Ability>>();

    private final Map<String, List<Modifier>> allModifiers = new HashMap<String, List<Modifier>>();

    private final List<BuildingType> buildingTypeList = new ArrayList<BuildingType>();

    private final List<GoodsType> goodsTypeList = new ArrayList<GoodsType>();
    private final List<GoodsType> farmedGoodsTypeList = new ArrayList<GoodsType>();
    private final List<GoodsType> foodGoodsTypeList = new ArrayList<GoodsType>();
    private final List<GoodsType> newWorldGoodsTypeList = new ArrayList<GoodsType>();
    private final List<GoodsType> libertyGoodsTypeList = new ArrayList<GoodsType>();
    private final List<GoodsType> immigrationGoodsTypeList = new ArrayList<GoodsType>();

    private final List<ResourceType> resourceTypeList = new ArrayList<ResourceType>();

    private final List<TileType> tileTypeList = new ArrayList<TileType>();

    private final List<TileImprovementType> tileImprovementTypeList = new ArrayList<TileImprovementType>();

    private final List<UnitType> unitTypeList = new ArrayList<UnitType>();
    private final List<UnitType> unitTypesTrainedInEurope = new ArrayList<UnitType>();
    private final List<UnitType> unitTypesPurchasedInEurope = new ArrayList<UnitType>();

    private final List<FoundingFather> foundingFathers = new ArrayList<FoundingFather>();

    private final List<Nation> nations = new ArrayList<Nation>();
    private final List<Nation> europeanNations = new ArrayList<Nation>();
    private final List<Nation> REFNations = new ArrayList<Nation>();
    private final List<Nation> indianNations = new ArrayList<Nation>();

    private final List<NationType> nationTypes = new ArrayList<NationType>();
    private final List<EuropeanNationType> europeanNationTypes = new ArrayList<EuropeanNationType>();
    private final List<EuropeanNationType> REFNationTypes = new ArrayList<EuropeanNationType>();
    private final List<IndianNationType> indianNationTypes = new ArrayList<IndianNationType>();

    private final List<EquipmentType> equipmentTypes = new ArrayList<EquipmentType>();

    private final List<Event> events = new ArrayList<Event>();
    private final List<Modifier> specialModifiers = new ArrayList<Modifier>();

    private final Map<String, ChildReader> readerMap = new HashMap<String, ChildReader>();

    private int storableTypes = 0;

    private boolean initialized = false;

    private String id;

    private String difficultyLevel;


    /**
     * Creates a new Specification object by loading it from the
     * specification.xml.
     *
     * This method is protected, since only one Specification object may exist.
     * This is due to static links from type {@link Goods} to the most important
     * GoodsTypes. If another specification object is created these links would
     * not work anymore for the previously created specification.
     *
     * To get hold of an Specification object use the static method
     * {@link #getSpecification()} which returns a singleton instance of the
     * Specification class.
     */
    public Specification(InputStream in) {
        this();
        initialized = false;
        load(in);
        clean();
        initialized = true;
    }

    public Specification() {
        logger.info("Initializing Specification");
        for (FreeColGameObjectType source : new FreeColGameObjectType[] {
                MOVEMENT_PENALTY_SOURCE,
                ARTILLERY_PENALTY_SOURCE,
                ATTACK_BONUS_SOURCE,
                FORTIFICATION_BONUS_SOURCE,
                INDIAN_RAID_BONUS_SOURCE,
                BASE_OFFENCE_SOURCE,
                BASE_DEFENCE_SOURCE,
                CARGO_PENALTY_SOURCE,
                AMBUSH_BONUS_SOURCE,
                COLONY_GOODS_PARTY_SOURCE
            }) {
            allTypes.put(source.getId(), source);
        }

        readerMap.put("nations",
                      new TypeReader<Nation>(Nation.class, nations));
        readerMap.put("building-types",
                      new TypeReader<BuildingType>(BuildingType.class, buildingTypeList));
        readerMap.put("european-nation-types",
                      new TypeReader<EuropeanNationType>(EuropeanNationType.class, europeanNationTypes));
        readerMap.put("equipment-types",
                      new TypeReader<EquipmentType>(EquipmentType.class, equipmentTypes));
        readerMap.put("events", new TypeReader<Event>(Event.class, events));
        readerMap.put("founding-fathers",
                      new TypeReader<FoundingFather>(FoundingFather.class, foundingFathers));
        readerMap.put("goods-types",
                      new TypeReader<GoodsType>(GoodsType.class, goodsTypeList));
        readerMap.put("indian-nation-types",
                      new TypeReader<IndianNationType>(IndianNationType.class, indianNationTypes));
        readerMap.put("resource-types",
                      new TypeReader<ResourceType>(ResourceType.class, resourceTypeList));
        readerMap.put("tile-types",
                      new TypeReader<TileType>(TileType.class, tileTypeList));
        readerMap.put("tileimprovement-types",
                      new TypeReader<TileImprovementType>(TileImprovementType.class, tileImprovementTypeList));
        readerMap.put("unit-types",
                      new TypeReader<UnitType>(UnitType.class, unitTypeList));
        readerMap.put("modifiers", new ModifierReader());
        readerMap.put("options", new OptionReader());


    }

    private void load(InputStream in) {

        try {
            XMLStreamReader xsr = XMLInputFactory.newInstance().createXMLStreamReader(in);
            xsr.nextTag();
            readFromXMLImpl(xsr);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            throw new RuntimeException("Error parsing specification: " + e.toString());
        }
    }

    public void loadFragment(InputStream in) {
        initialized = false;
        load(in);
        initialized = true;
    }

    public void clean() {

        logger.finest("Cleaning up specification.");

        Iterator<FreeColGameObjectType> typeIterator = allTypes.values().iterator();
        while (typeIterator.hasNext()) {
            FreeColGameObjectType type = typeIterator.next();
            if (type.isAbstractType()) {
                typeIterator.remove();
            }
        }

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
        }

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
            if (goodsType.isLibertyGoodsType()) {
                libertyGoodsTypeList.add(goodsType);
            }
            if (goodsType.isImmigrationGoodsType()) {
                immigrationGoodsTypeList.add(goodsType);
            }
            if (goodsType.isStorable()) {
                storableTypes++;
            }
        }

        for (Option group : getOptionGroup("difficultyLevels").getOptions()) {
            if (group instanceof OptionGroup) {
                OptionGroup difficultyLevel = (OptionGroup) group;
                for (Option option : difficultyLevel.getOptions()) {
                    if (option instanceof StringOption) {
                        ((StringOption) option).generateChoices(this);
                    }
                }
            }
        }

        if (difficultyLevel != null) {
            applyDifficultyLevel(difficultyLevel);
        }

        logger.info("Specification initialization complete. "
                    + allTypes.size() + " FreeColGameObjectTypes,\n"
                    + allOptions.size() + " Options, "
                    + allAbilities.size() + " Abilities, "
                    + allModifiers.size() + " Modifiers read.");
    }

    private interface ChildReader {
        public void readChildren(XMLStreamReader xsr, Specification specification) throws XMLStreamException;
    }

    private class ModifierReader implements ChildReader {

        public void readChildren(XMLStreamReader xsr, Specification specification) throws XMLStreamException {
            while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                Modifier modifier = new Modifier(xsr, specification);
                specification.addModifier(modifier);
                specification.specialModifiers.add(modifier);
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

        public void readChildren(XMLStreamReader xsr, Specification specification) throws XMLStreamException {
            while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                if ("delete".equals(xsr.getLocalName())) {
                    String id = xsr.getAttributeValue(null, FreeColObject.ID_ATTRIBUTE_TAG);
                    FreeColGameObjectType object = allTypes.remove(id);
                    if (object != null) {
                        result.remove(object);
                    }
                } else {
                    T object = getType(xsr.getAttributeValue(null, FreeColObject.ID_ATTRIBUTE_TAG), type);
                    object.readFromXML(xsr);
                    if (!object.isAbstractType() && !result.contains(object)) {
                        result.add(object);
                        object.setIndex(index);
                        index++;
                    }
                }
            }
        }
    }

    private class OptionReader implements ChildReader {

        public void readChildren(XMLStreamReader xsr, Specification specification) throws XMLStreamException {
            while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                String optionType = xsr.getLocalName();
                String recursiveString = xsr.getAttributeValue(null, "recursive");
                boolean recursive = "false".equals(recursiveString) ? false : true;
                if (OptionGroup.getXMLElementTagName().equals(optionType)) {
                    String id = xsr.getAttributeValue(null, FreeColObject.ID_ATTRIBUTE_TAG);
                    OptionGroup group = allOptionGroups.get(id);
                    if (group == null) {
                        group = new OptionGroup(xsr);
                        allOptionGroups.put(id, group);
                    } else {
                        group.readFromXML(xsr);
                    }
                    specification.addOptionGroup(group, recursive);
                } else {
                    logger.finest("Parsing of " + optionType + " is not implemented yet");
                    xsr.nextTag();
                }
            }
        }
    }

    // ---------------------------------------------------------- retrieval
    // methods

    /**
     * Describe <code>getId</code> method here.
     *
     * @return a <code>String</code> value
     */
    public String getId() {
        return id;
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
     * Registers an Ability's id as defined. This is useful for
     * abilities that are required rather than provided by
     * FreeColGameObjectTypes.
     *
     * @param id a <code>String</code> value
     */
    public void addAbility(String id) {
        if (!allAbilities.containsKey(id)) {
            allAbilities.put(id, new ArrayList<Ability>());
        }
    }

    /**
     * Return a list of all Abilities with the given id.
     *
     * @param id the ability id
     */
    public List<Ability> getAbilities(String id) {
        return allAbilities.get(id);
    }

    /**
     * Return a list of FreeColGameObjectTypes that provide the required ability.
     *
     * @param id the ability id
     * @param value the ability value
     * @return a list of FreeColGameObjectTypes that provide the required ability.
     */
    public List<FreeColGameObjectType> getTypesProviding(String id, boolean value) {
        List<FreeColGameObjectType> result = new ArrayList<FreeColGameObjectType>();
        for (Ability ability : getAbilities(id)) {
            if (ability.getValue() == value && ability.getSource() != null) {
                result.add(ability.getSource());
            }
        }
        return result;
    }

    /**
     * Add a modifier.
     *
     * @param modifier a <code>Modifier</code> value
     */
    public void addModifier(Modifier modifier) {
        String id = modifier.getId();
        if (!allModifiers.containsKey(id)) {
            allModifiers.put(id, new ArrayList<Modifier>());
        }
        allModifiers.get(id).add(modifier);
    }

    /**
     * Return a list of all Modifiers with the given id.
     *
     * @param id the modifier id
     */
    public List<Modifier> getModifiers(String id) {
        return allModifiers.get(id);
    }

    /**
     * Returns the <code>FreeColGameObjectType</code> with the given
     * ID.  Throws an IllegalArgumentException if the ID is
     * null. Throws and IllegalArgumentException if no such Type
     * can be retrieved and initialization is complete.
     *
     * @param Id a <code>String</code> value
     * @param type a <code>Class</code> value
     * @return a <code>FreeColGameObjectType</code> value
     * @exception IllegalArgumentException if an error occurs
     */
    public <T extends FreeColGameObjectType> T getType(String Id, Class<T> type)
        throws IllegalArgumentException {
        if (Id == null) {
            throw new IllegalArgumentException("Trying to retrieve FreeColGameObjectType" + " with ID 'null'.");
        } else if (allTypes.containsKey(Id)) {
            try {
                return type.cast(allTypes.get(Id));
            } catch(ClassCastException cce) {
                logger.warning(Id + " caused ClassCastException!");
                throw(cce);
            }
        } else if (allTypes.containsKey(mangle(Id))) {
            // TODO: remove 0.9.x compatibility code
            return type.cast(allTypes.get(mangle(Id)));
        } else if (initialized) {
            throw new IllegalArgumentException("Undefined FreeColGameObjectType" + " with ID '" + Id + "'.");
        } else {
            // forward declaration of new type
            try {
                Constructor<T> c = type.getConstructor(String.class, Specification.class);
                T result = c.newInstance(Id, this);
                allTypes.put(Id, result);
                return result;
            } catch(Exception e) {
                logger.warning(e.toString());
                return null;
            }
        }
    }

    // TODO: remove compatibility code
    private String mangle(String id) {
        int index = id.lastIndexOf('.');
        if (index == -1) {
            return id;
        } else {
            return id.substring(0, index + 1) + id.substring(index + 1, index + 2).toLowerCase(Locale.US)
                + id.substring(index + 2);
        }
    }

    public FreeColGameObjectType getType(String Id) throws IllegalArgumentException {
        return getType(Id, FreeColGameObjectType.class);
    }


    /**
     * Return all types which have any of the given abilities.
     *
     * @param abilities The abilities for the search
     * @return a <code>List</code> of <code>UnitType</code>
     */
    public <T extends FreeColGameObjectType> List<T>
                      getTypesWithAbility(Class<T> resultType, String... abilities) {
        ArrayList<T> result = new ArrayList<T>();
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
     * Is option with this ID present?  This is helpful when options are
     * optionally(!) present, for example model.option.priceIncrease.artillery
     * exists but model.option.priceIncrease.frigate does not.
     *
     * @param Id a <code>String</code> value
     * @return True/false on presence of option Id
     */
    public boolean hasOption(String Id) {
        return Id != null && allOptions.containsKey(Id);
    }

    /**
     * Returns the <code>AbstractOption</code> with the given ID. Throws an
     * IllegalArgumentException if the ID is null or unknown.
     *
     * @param Id a <code>String</code> value
     * @return an <code>AbstractOption</code> value
     */
    public AbstractOption getOption(String Id) throws IllegalArgumentException {
        if (Id == null) {
            throw new IllegalArgumentException("Trying to retrieve AbstractOption" + " with ID 'null'.");
        } else if (!allOptions.containsKey(Id)) {
            throw new IllegalArgumentException("Trying to retrieve AbstractOption" + " with ID '" + Id
                    + "' returned 'null'.");
        } else {
            return allOptions.get(Id);
        }
    }

    /**
     * Returns the <code>OptionGroup</code> with the given ID. Throws an
     * IllegalArgumentException if the ID is null or unknown.
     *
     * @param Id a <code>String</code> value
     * @return an <code>OptionGroup</code> value
     */
    public OptionGroup getOptionGroup(String Id) throws IllegalArgumentException {
        if (Id == null) {
            throw new IllegalArgumentException("Trying to retrieve OptionGroup" + " with ID 'null'.");
        } else if (!allOptionGroups.containsKey(Id)) {
            throw new IllegalArgumentException("Trying to retrieve OptionGroup" + " with ID '" + Id
                    + "' returned 'null'.");
        } else {
            return allOptionGroups.get(Id);
        }
    }

    /**
     * Adds an <code>OptionGroup</code> to the specification
     *
     * @param optionGroup <code>OptionGroup</code> to add
     * @param recursive a <code>boolean</code> value
     */
    private void addOptionGroup(OptionGroup optionGroup, boolean recursive) {
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
     * Adds an <code>AbstractOption</code> to the specification
     *
     * @param abstractOption <code>AbstractOption</code> to add
     */
    public void addAbstractOption(AbstractOption abstractOption) {
        // Add the option
        allOptions.put(abstractOption.getId(), abstractOption);
    }


    /**
     * Returns the <code>IntegerOption</code> with the given ID. Throws an
     * IllegalArgumentException if the ID is null, or if no such Type can be
     * retrieved.
     *
     * @param Id a <code>String</code> value
     * @return an <code>IntegerOption</code> value
     */
    public IntegerOption getIntegerOption(String Id) {
        return (IntegerOption) getOption(Id);
    }

    /**
     * Returns the <code>RangeOption</code> with the given ID. Throws an
     * IllegalArgumentException if the ID is null, or if no such Type can be
     * retrieved.
     *
     * @param Id a <code>String</code> value
     * @return an <code>RangeOption</code> value
     */
    public RangeOption getRangeOption(String Id) {
        return (RangeOption) getOption(Id);
    }

    /**
     * Returns the <code>BooleanOption</code> with the given ID. Throws an
     * IllegalArgumentException if the ID is null, or if no such Type can be
     * retrieved.
     *
     * @param Id a <code>String</code> value
     * @return an <code>BooleanOption</code> value
     */
    public BooleanOption getBooleanOption(String Id) {
        return (BooleanOption) getOption(Id);
    }

    /**
     * Returns the <code>StringOption</code> with the given ID. Throws an
     * IllegalArgumentException if the ID is null, or if no such Type can be
     * retrieved.
     *
     * @param Id a <code>String</code> value
     * @return an <code>StringOption</code> value
     */
    public StringOption getStringOption(String Id) {
        return (StringOption) getOption(Id);
    }

    /**
     * Gets the integer value of an option.
     *
     * @param id The id of the option.
     * @return The value.
     * @exception IllegalArgumentException If there is no integer
     *            value associated with the specified option.
     * @exception NullPointerException if the given <code>Option</code> does not exist.
     */
    public int getInteger(String id) {
        try {
            return ((IntegerOption) getOption(id)).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No integer value associated with the specified option.");
        }
    }

    /**
     * Gets the boolean value of an option.
     *
     * @param id The id of the option.
     * @return The value.
     * @exception IllegalArgumentException If there is no boolean
     *            value associated with the specified option.
     * @exception NullPointerException if the given <code>Option</code> does not exist.
     */
    public boolean getBoolean(String id) {
        try {
            return ((BooleanOption) getOption(id)).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No boolean value associated with the specified option.");
        }
    }


    // -- Buildings --
    public List<BuildingType> getBuildingTypeList() {
        return buildingTypeList;
    }

    /**
     * Describe <code>numberOfBuildingTypes</code> method here.
     *
     * @return an <code>int</code> value
     */
    public int numberOfBuildingTypes() {
        return buildingTypeList.size();
    }

    /**
     * Describe <code>getBuildingType</code> method here.
     *
     * @param buildingTypeIndex an <code>int</code> value
     * @return a <code>BuildingType</code> value
     */
    public BuildingType getBuildingType(int buildingTypeIndex) {
        return buildingTypeList.get(buildingTypeIndex);
    }

    public BuildingType getBuildingType(String id) {
        return getType(id, BuildingType.class);
    }

    // -- Goods --
    public List<GoodsType> getGoodsTypeList() {
        return goodsTypeList;
    }

    /**
     * Describe <code>numberOfGoodsTypes</code> method here.
     *
     * @return an <code>int</code> value
     */
    public int numberOfGoodsTypes() {
        return goodsTypeList.size();
    }

    public int numberOfStoredGoodsTypes() {
        return storableTypes;
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

    public int numberOfFarmedGoodsTypes() {
        return farmedGoodsTypeList.size();
    }

    /**
     * Describe <code>getGoodsType</code> method here.
     *
     * @param id a <code>String</code> value
     * @return a <code>GoodsType</code> value
     */
    public GoodsType getGoodsType(String id) {
        return getType(id, GoodsType.class);
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

    // -- Resources --
    public List<ResourceType> getResourceTypeList() {
        return resourceTypeList;
    }

    public int numberOfResourceTypes() {
        return resourceTypeList.size();
    }

    public ResourceType getResourceType(String id) {
        return getType(id, ResourceType.class);
    }

    // -- Tiles --
    public List<TileType> getTileTypeList() {
        return tileTypeList;
    }

    public int numberOfTileTypes() {
        return tileTypeList.size();
    }

    public TileType getTileType(String id) {
        return getType(id, TileType.class);
    }

    // -- Improvements --
    public List<TileImprovementType> getTileImprovementTypeList() {
        return tileImprovementTypeList;
    }

    public TileImprovementType getTileImprovementType(String id) {
        return getType(id, TileImprovementType.class);
    }

    // -- Units --
    public List<UnitType> getUnitTypeList() {
        return unitTypeList;
    }

    public int numberOfUnitTypes() {
        return unitTypeList.size();
    }

    public UnitType getUnitType(String id) {
        return getType(id, UnitType.class);
    }

    public UnitType getExpertForProducing(GoodsType goodsType) {
        return experts.get(goodsType);
    }

    /**
     * Return the unit types which have any of the given abilities
     *
     * @param abilities The abilities for the search
     * @return a <code>List</code> of <code>UnitType</code>
     */
    public List<UnitType> getUnitTypesWithAbility(String... abilities) {
        return getTypesWithAbility(UnitType.class, abilities);
    }

    /**
     * Returns the unit types that can be trained in Europe.
     */
    public List<UnitType> getUnitTypesTrainedInEurope() {
        return unitTypesTrainedInEurope;
    }

    /**
     * Returns the unit types that can be purchased in Europe.
     */
    public List<UnitType> getUnitTypesPurchasedInEurope() {
        return unitTypesPurchasedInEurope;
    }

    // -- Founding Fathers --

    public List<FoundingFather> getFoundingFathers() {
        return foundingFathers;
    }

    public int numberOfFoundingFathers() {
        return foundingFathers.size();
    }

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

    public int numberOfNationTypes() {
        return nationTypes.size();
    }

    public NationType getNationType(String id) {
        return getType(id, NationType.class);
    }

    // -- Nations --

    public List<Nation> getNations() {
        return nations;
    }

    public Nation getNation(String id) {
        return getType(id, Nation.class);
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

    // -- EquipmentTypes --
    public List<EquipmentType> getEquipmentTypeList() {
        return equipmentTypes;
    }

    public EquipmentType getEquipmentType(String id) {
        return getType(id, EquipmentType.class);
    }

    // -- DifficultyLevels --
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
     * Return the current difficulty level.
     *
     * @return the current difficulty level
     */
    public OptionGroup getDifficultyLevel() {
        return allOptionGroups.get(difficultyLevel);
    }

    /**
     * Describe <code>getDifficultyLevel</code> method here.
     *
     * @param id a <code>String</code> value
     * @return a <code>DifficultyLevel</code> value
     */
    public OptionGroup getDifficultyLevel(String id) {
        return allOptionGroups.get(id);
    }

    /**
     * Describe <code>getDifficultyLevel</code> method here.
     *
     * @param level an <code>int</code> value
     * @return a <code>DifficultyLevel</code> value
     */
    public OptionGroup getDifficultyLevel(int level) {
        return getDifficultyLevels().get(level);
    }

    /**
     * Applies the difficulty level identified by the given integer to
     * the current specification.
     *
     * @param difficultyLevel index of difficulty level to apply
     */
    public void applyDifficultyLevel(int difficultyLevel) {
        applyDifficultyLevel(getDifficultyLevel(difficultyLevel));
    }

    /**
     * Applies the difficulty level identified by the given String to
     * the current specification.
     *
     * @param difficultyLevel id of difficulty level to apply
     */
    public void applyDifficultyLevel(String difficultyLevel) {
        applyDifficultyLevel(getDifficultyLevel(difficultyLevel));
    }


    /**
     * Applies the given difficulty level to the current
     * specification.
     *
     * @param level difficulty level to apply
     */
    public void applyDifficultyLevel(OptionGroup level) {
        logger.info("Applying difficulty level " + level.getId());
        addOptionGroup(level, true);

        for (FreeColGameObjectType type : allTypes.values()) {
            type.applyDifficultyLevel(level);
        }

        // TODO: find a better place for this!
        if (FreeCol.isInFullDebugMode()) {
            getIntegerOption(GameOptions.STARTING_MONEY).setValue(10000);
        }

        this.difficultyLevel = level.getId();
    }

    // -- Events --
    public List<Event> getEvents() {
        return events;
    }

    public Event getEvent(String id) {
        return getType(id, Event.class);
    }

    /**
     * Returns the FreeColGameObjectType identified by the
     * attributeName, or the default value if there is no such
     * attribute.
     *
     * @param in the XMLStreamReader
     * @param attributeName the name of the attribute identifying the
     * FreeColGameObjectType
     * @param returnClass the class of the return value
     * @param defaultValue the value to return if there is no
     * attribute named attributeName
     * @return a FreeColGameObjectType value
     */
    public <T extends FreeColGameObjectType> T getType(XMLStreamReader in, String attributeName,
                                                       Class<T> returnClass, T defaultValue) {
        final String attributeString = in.getAttributeValue(null, attributeName);
        if (attributeString != null) {
            return getType(attributeString, returnClass);
        } else {
            return defaultValue;
        }
    }


    /**
     * Makes an XML-representation of this object.
     *
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        // Add attributes:
        out.writeAttribute(FreeColObject.ID_ATTRIBUTE_TAG, getId());
        if (difficultyLevel != null) {
            out.writeAttribute("difficultyLevel", difficultyLevel);
        }

        // copy the order of section in specification.xml
        writeSection(out, "modifiers", specialModifiers);
        writeSection(out, "events", events);
        writeSection(out, "goods-types", goodsTypeList);
        writeSection(out, "resource-types", resourceTypeList);
        writeSection(out, "tile-types", tileTypeList);
        writeSection(out, "equipment-types", equipmentTypes);
        writeSection(out, "tileimprovement-types", tileImprovementTypeList);
        writeSection(out, "unit-types", unitTypeList);
        writeSection(out, "building-types", buildingTypeList);
        writeSection(out, "founding-fathers", foundingFathers);
        writeSection(out, "european-nation-types", europeanNationTypes);
        writeSection(out, "european-nation-types", REFNationTypes);
        writeSection(out, "indian-nation-types", indianNationTypes);
        writeSection(out, "nations", nations);
        // option tree has been flattened
        out.writeStartElement("options");
        for (OptionGroup item : allOptionGroups.values()) {
            if ("".equals(item.getGroup())) {
                item.toXML(out);
            }
        }
        out.writeEndElement();

        // End element:
        out.writeEndElement();

    }

    private <T extends FreeColObject> void writeSection(XMLStreamWriter out, String section, Collection<T> items)
        throws XMLStreamException {
        out.writeStartElement(section);
        for (T item : items) {
            item.toXMLImpl(out);
        }
        out.writeEndElement();
    }

    public void readFromXMLImpl(XMLStreamReader xsr) throws XMLStreamException {
        String newId = xsr.getAttributeValue(null, FreeColObject.ID_ATTRIBUTE_TAG);
        if (difficultyLevel == null) {
            difficultyLevel = xsr.getAttributeValue(null, "difficultyLevel");
        }
        logger.info("Difficulty level is " + difficultyLevel);
        if (id == null) {
            // don't overwrite id with parent id!
            id = newId;
        }
        logger.info("Reading specification " + newId);
        String parentId = xsr.getAttributeValue(null, "extends");
        if (parentId != null) {
            try {
                FreeColTcFile parent = new FreeColTcFile(parentId);
                load(parent.getSpecificationInputStream());
                initialized = false;
            } catch(IOException e) {
                throw new XMLStreamException("Failed to open parent specification: " + e);
            }
        }
        while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = xsr.getLocalName();
            logger.finest("Found child named " + childName);
            ChildReader reader = readerMap.get(childName);
            if (reader == null) {
                if ("improvementaction-types".equals(childName)) {
                    // TODO: remove compatibility code after 0.10.0
                    while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        // skip children
                        while ("action".equals(xsr.getLocalName())) {
                            xsr.nextTag();
                        }
                    }
                } else {
                    throw new RuntimeException("unexpected: " + childName);
                }
            } else {
                reader.readChildren(xsr, this);
            }
        }
        if (difficultyLevel != null) {
            applyDifficultyLevel(difficultyLevel);
        }
        initialized = true;
    }

    public static String getXMLElementTagName() {
        return "freecol-specification";
    }

}
