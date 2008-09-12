/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

package net.sf.freecol.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.client.gui.action.ImprovementActionType;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.DifficultyLevel;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.EuropeanNationType;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.AbstractOption;
import net.sf.freecol.common.option.FileOption;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.LanguageOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.RangeOption;
import net.sf.freecol.common.option.SelectOption;

/**
 * This class encapsulates any parts of the "specification" for FreeCol that are
 * expressed best using XML. The XML is loaded through the class loader from the
 * resource named "specification.xml" in the same package as this class.
 */
public final class Specification {

    /**
     * Singleton
     */
    protected static Specification specification;

    private static final Logger logger = Logger.getLogger(Specification.class.getName());

    private final Map<String, FreeColGameObjectType> allTypes;

    private final Map<String, AbstractOption> allOptions;

    private final Map<String, OptionGroup> allOptionGroups;

    private final Map<GoodsType, UnitType> experts;

    private final Map<String, List<Ability>> allAbilities;

    private final Map<String, List<Modifier>> allModifiers;

    private final List<BuildingType> buildingTypeList;

    private final List<GoodsType> goodsTypeList;
    private final List<GoodsType> farmedGoodsTypeList;
    private final List<GoodsType> foodGoodsTypeList;
    private final List<GoodsType> newWorldGoodsTypeList;

    private final List<ResourceType> resourceTypeList;

    private final List<TileType> tileTypeList;

    private final List<TileImprovementType> tileImprovementTypeList;

    private final List<ImprovementActionType> improvementActionTypeList;

    private final List<UnitType> unitTypeList;
    private final List<UnitType> unitTypesTrainedInEurope;
    private final List<UnitType> unitTypesPurchasedInEurope;

    private final List<FoundingFather> foundingFathers;

    private final List<Nation> nations;
    private final List<Nation> europeanNations;
    private final List<Nation> REFNations;
    private final List<Nation> classicNations;
    private final List<Nation> indianNations;

    private final List<NationType> nationTypes;
    private final List<EuropeanNationType> europeanNationTypes;
    private final List<EuropeanNationType> REFNationTypes;
    private final List<EuropeanNationType> classicNationTypes;
    private final List<IndianNationType> indianNationTypes;

    private final List<EquipmentType> equipmentTypes;

    private final List<DifficultyLevel> difficultyLevels;

    private int storableTypes = 0;

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
    protected Specification(InputStream in) {
        logger.info("Initializing Specification");

        allTypes = new HashMap<String, FreeColGameObjectType>();
        allOptions = new HashMap<String, AbstractOption>();
        allOptionGroups = new HashMap<String, OptionGroup>();
        experts = new HashMap<GoodsType, UnitType>();

        allAbilities = new HashMap<String, List<Ability>>();
        allModifiers = new HashMap<String, List<Modifier>>();

        buildingTypeList = new ArrayList<BuildingType>();

        goodsTypeList = new ArrayList<GoodsType>();
        foodGoodsTypeList = new ArrayList<GoodsType>();
        farmedGoodsTypeList = new ArrayList<GoodsType>();
        newWorldGoodsTypeList = new ArrayList<GoodsType>();

        resourceTypeList = new ArrayList<ResourceType>();
        tileTypeList = new ArrayList<TileType>();
        tileImprovementTypeList = new ArrayList<TileImprovementType>();
        improvementActionTypeList = new ArrayList<ImprovementActionType>();

        unitTypeList = new ArrayList<UnitType>();
        unitTypesPurchasedInEurope = new ArrayList<UnitType>();
        unitTypesTrainedInEurope = new ArrayList<UnitType>();

        foundingFathers = new ArrayList<FoundingFather>();

        nations = new ArrayList<Nation>();
        europeanNations = new ArrayList<Nation>();
        REFNations = new ArrayList<Nation>();
        classicNations = new ArrayList<Nation>();
        indianNations = new ArrayList<Nation>();

        nationTypes = new ArrayList<NationType>();
        europeanNationTypes = new ArrayList<EuropeanNationType>();
        REFNationTypes = new ArrayList<EuropeanNationType>();
        classicNationTypes = new ArrayList<EuropeanNationType>();
        indianNationTypes = new ArrayList<IndianNationType>();

        equipmentTypes = new ArrayList<EquipmentType>();
        difficultyLevels = new ArrayList<DifficultyLevel>();

        try {
            XMLStreamReader xsr = XMLInputFactory.newInstance().createXMLStreamReader(in);
            xsr.nextTag();
            while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                String childName = xsr.getLocalName();
                logger.finest("Found child named " + childName);

                if ("goods-types".equals(childName)) {

                    int goodsIndex = 0;
                    while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        GoodsType goodsType = new GoodsType(goodsIndex++);
                        goodsType.readFromXML(xsr, this);
                        goodsTypeList.add(goodsType);
                        allTypes.put(goodsType.getId(), goodsType);
                        if (goodsType.isFarmed()) {
                            farmedGoodsTypeList.add(goodsType);
                        }
                        if (goodsType.isFoodType()) {
                            foodGoodsTypeList.add(goodsType);
                        }
                        if (goodsType.isNewWorldGoodsType()) {
                            newWorldGoodsTypeList.add(goodsType);
                        }
                        if (goodsType.isStorable()) {
                            storableTypes++;
                        }
                    }

                } else if ("building-types".equals(childName)) {

                    int buildingIndex = 0;
                    while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        BuildingType buildingType = new BuildingType(buildingIndex++);
                        buildingType.readFromXML(xsr, this);
                        allTypes.put(buildingType.getId(), buildingType);
                        buildingTypeList.add(buildingType);
                    }

                } else if ("resource-types".equals(childName)) {

                    int resIndex = 0;
                    while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        ResourceType resourceType = new ResourceType(resIndex++);
                        resourceType.readFromXML(xsr, this);
                        allTypes.put(resourceType.getId(), resourceType);
                        resourceTypeList.add(resourceType);
                    }

                } else if ("tile-types".equals(childName)) {

                    int tileIndex = 0;
                    while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        TileType tileType = new TileType(tileIndex++);
                        tileType.readFromXML(xsr, this);
                        allTypes.put(tileType.getId(), tileType);
                        tileTypeList.add(tileType);
                    }

                } else if ("tileimprovement-types".equals(childName)) {

                    int impIndex = 0;
                    while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        TileImprovementType tileImprovementType = new TileImprovementType(impIndex++);
                        tileImprovementType.readFromXML(xsr, this);
                        allTypes.put(tileImprovementType.getId(), tileImprovementType);
                        tileImprovementTypeList.add(tileImprovementType);
                    }

                } else if ("improvementaction-types".equals(childName)) {

                    while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        ImprovementActionType impActionType = new ImprovementActionType();
                        impActionType.readFromXML(xsr, this);
                        allTypes.put(impActionType.getId(), impActionType);
                        improvementActionTypeList.add(impActionType);
                    }

                } else if ("unit-types".equals(childName)) {

                    int unitIndex = 0;
                    while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        UnitType unitType = new UnitType(unitIndex++);
                        unitType.readFromXML(xsr, this);
                        allTypes.put(unitType.getId(), unitType);
                        unitTypeList.add(unitType);
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

                } else if ("founding-fathers".equals(childName)) {

                    int fatherIndex = 0;
                    while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        FoundingFather foundingFather = new FoundingFather(fatherIndex++);
                        foundingFather.readFromXML(xsr, this);
                        allTypes.put(foundingFather.getId(), foundingFather);
                        foundingFathers.add(foundingFather);
                    }

                } else if ("nation-types".equals(childName)) {

                    int nationIndex = 0;
                    while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        NationType nationType;
                        if ("european-nation-type".equals(xsr.getLocalName())) {
                            nationType = new EuropeanNationType(nationIndex++);
                            nationType.readFromXML(xsr, this);
                            if (nationType.isREF()) {
                                REFNationTypes.add((EuropeanNationType) nationType);
                            } else {
                                europeanNationTypes.add((EuropeanNationType) nationType);
                            }
                        } else {
                            nationType = new IndianNationType(nationIndex++);
                            nationType.readFromXML(xsr, this);
                            indianNationTypes.add((IndianNationType) nationType);
                        }
                        allTypes.put(nationType.getId(), nationType);
                        nationTypes.add(nationType);

                    }

                } else if ("nations".equals(childName)) {

                    int nationIndex = 0;
                    while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        Nation nation = new Nation(nationIndex++);
                        nation.readFromXML(xsr, this);
                        allTypes.put(nation.getId(), nation);
                        nations.add(nation);

                        if (nation.getType().isEuropean()) {
                            if (nation.getType().isREF()) {
                                REFNations.add(nation);
                            } else {
                                europeanNations.add(nation);
                            }
                            if (nation.isClassic()) {
                                classicNations.add(nation);
                                classicNationTypes.add((EuropeanNationType) nation.getType());
                            }
                        } else {
                            indianNations.add(nation);
                        }
                    }

                } else if ("equipment-types".equals(childName)) {

                    int equipmentIndex = 0;
                    while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        EquipmentType equipmentType = new EquipmentType(equipmentIndex++);
                        equipmentType.readFromXML(xsr, this);
                        allTypes.put(equipmentType.getId(), equipmentType);
                        equipmentTypes.add(equipmentType);
                    }

                } else if ("difficultyLevels".equals(childName)) {

                    int levelIndex = 0;
                    while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        DifficultyLevel level = new DifficultyLevel(levelIndex++);
                        level.readFromXML(xsr, this);
                        allTypes.put(level.getId(), level);
                        difficultyLevels.add(level);
                    }

                } else if ("options".equals(childName)) {

                    while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        AbstractOption option = (AbstractOption) null;
                        String optionType = xsr.getLocalName();
                        if (OptionGroup.getXMLElementTagName().equals(optionType)) {
                            option = new OptionGroup(xsr);
                        } else if (IntegerOption.getXMLElementTagName().equals(optionType) || "integer-option".equals(optionType)) {
                            option = new IntegerOption(xsr);
                        } else if (BooleanOption.getXMLElementTagName().equals(optionType) || "boolean-option".equals(optionType)) {
                            option = new BooleanOption(xsr);
                        } else if (RangeOption.getXMLElementTagName().equals(optionType) || "range-option".equals(optionType)) {
                            option = new RangeOption(xsr);
                        } else if (SelectOption.getXMLElementTagName().equals(optionType) || "select-option".equals(optionType)) {
                            option = new SelectOption(xsr);
                        } else if (LanguageOption.getXMLElementTagName().equals(optionType) || "language-option".equals(optionType)) {
                            option = new LanguageOption(xsr);
                        } else if (FileOption.getXMLElementTagName().equals(optionType) || "file-option".equals(optionType)) {
                            option = new FileOption(xsr);
                        } else {
                            logger.finest("Parsing of " + optionType + " is not implemented yet");
                            xsr.nextTag();
                        }

                        // If the option is valid, add it to Specification options
                        if (option != (AbstractOption) null) {
                            if(option instanceof OptionGroup) {
                                this.addOptionGroup((OptionGroup) option);
                            } else {
                                this.addAbstractOption((AbstractOption) option);
                            }
                        }
                    }

                } else {
                    throw new RuntimeException("unexpected: " + childName);
                }
            }

            // Post specification actions
            // Get Food, Bells, Crosses and Hammers
            Goods.initialize(getGoodsTypeList(), numberOfGoodsTypes());

            logger.info("Specification initialization complete");
        } catch (XMLStreamException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            throw new RuntimeException("Error parsing specification");
        }
    }

    // ---------------------------------------------------------- retrieval
    // methods

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
     * Returns the <code>FreeColGameObjectType</code> with the given ID.
     * Throws an IllegalArgumentException if the ID is null, or if no such Type
     * can be retrieved.
     *
     * @param Id a <code>String</code> value
     * @return a <code>FreeColGameObjectType</code> value
     */
    public FreeColGameObjectType getType(String Id) throws IllegalArgumentException {
        if (Id == null) {
            throw new IllegalArgumentException("Trying to retrieve FreeColGameObjectType" + " with ID 'null'.");
        } else if (!allTypes.containsKey(Id)) {
            throw new IllegalArgumentException("Trying to retrieve FreeColGameObjectType" + " with ID '" + Id
                    + "' returned 'null'.");
        } else {
            return allTypes.get(Id);
        }
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
     */
    public void addOptionGroup(OptionGroup optionGroup) {
        // Add the option group
        allOptionGroups.put(optionGroup.getId(), optionGroup);

        // Add the options of the group
        Iterator<Option> iter = optionGroup.iterator();

        while(iter.hasNext()){
            Option option = iter.next();
            addAbstractOption((AbstractOption) option);
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
        return (BuildingType) getType(id);
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

    /**
     * Describe <code>numberOfFarmedGoodsTypes</code> method here.
     *
     * @return an <code>int</code> value
     */
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
        return (GoodsType) getType(id);
    }

    public List<GoodsType> getGoodsFood() {
        return foodGoodsTypeList;
    }

    // -- Resources --
    public List<ResourceType> getResourceTypeList() {
        return resourceTypeList;
    }

    public int numberOfResourceTypes() {
        return resourceTypeList.size();
    }

    public ResourceType getResourceType(String id) {
        return (ResourceType) getType(id);
    }

    // -- Tiles --
    public List<TileType> getTileTypeList() {
        return tileTypeList;
    }

    public int numberOfTileTypes() {
        return tileTypeList.size();
    }

    public TileType getTileType(String id) {
        return (TileType) getType(id);
    }

    // -- Improvements --
    public List<TileImprovementType> getTileImprovementTypeList() {
        return tileImprovementTypeList;
    }

    public TileImprovementType getTileImprovementType(String id) {
        return (TileImprovementType) getType(id);
    }

    // -- Improvement Actions --
    public List<ImprovementActionType> getImprovementActionTypeList() {
        return improvementActionTypeList;
    }

    public ImprovementActionType getImprovementActionType(String id) {
        return (ImprovementActionType) getType(id);
    }

    // -- Units --
    public List<UnitType> getUnitTypeList() {
        return unitTypeList;
    }

    public int numberOfUnitTypes() {
        return unitTypeList.size();
    }

    public UnitType getUnitType(String id) {
        return (UnitType) getType(id);
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
        ArrayList<UnitType> unitTypes = new ArrayList<UnitType>();
        for (UnitType unitType : getUnitTypeList()) {
            for (String ability : abilities) {
                if (unitType.hasAbility(ability)) {
                    unitTypes.add(unitType);
                    break;
                }
            }
        }
        return unitTypes;
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
        return (FoundingFather) getType(id);
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

    public List<EuropeanNationType> getClassicNationTypes() {
        return classicNationTypes;
    }

    public int numberOfNationTypes() {
        return nationTypes.size();
    }

    public NationType getNationType(String id) {
        return (NationType) getType(id);
    }

    // -- Nations --

    public List<Nation> getNations() {
        return nations;
    }

    public Nation getNation(String id) {
        return (Nation) getType(id);
    }

    public List<Nation> getClassicNations() {
        return classicNations;
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
        return (EquipmentType) getType(id);
    }

    // -- DifficultyLevels --
    public List<DifficultyLevel> getDifficultyLevels() {
        return difficultyLevels;
    }

    /**
     * Describe <code>getDifficultyLevel</code> method here.
     *
     * @param id a <code>String</code> value
     * @return a <code>DifficultyLevel</code> value
     */
    public DifficultyLevel getDifficultyLevel(String id) {
        return (DifficultyLevel) getType(id);
    }

    /**
     * Describe <code>getDifficultyLevel</code> method here.
     *
     * @param level an <code>int</code> value
     * @return a <code>DifficultyLevel</code> value
     */
    public DifficultyLevel getDifficultyLevel(int level) {
        return difficultyLevels.get(level);
    }

    /**
     * Applies the difficulty level to the current specification.
     *
     * @param difficultyLevel difficulty level to apply
     */
    public void applyDifficultyLevel(int difficultyLevel) {
        for (String key : difficultyLevels.get(difficultyLevel).getOptions().keySet()) {
            allOptions.put(key, difficultyLevels.get(difficultyLevel).getOptions().get(key));
        }
    }

    /**
     * Loads the specification.
     *
     * @param is The stream to load the specification from.
     */
    public static void createSpecification(InputStream is) {
        specification = new Specification(is);
    }

    // FIXME urgently!
    public static Specification getSpecification() {
        if (specification == null) {
            try {
                specification = new Specification(new FileInputStream("data/freecol/specification.xml"));
                logger.info("getSpecification()");
            } catch (Exception e) {
            }
        }
        return specification;
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
            return returnClass.cast(getType(attributeString));
        } else {
            return defaultValue;
        }
    }
}
