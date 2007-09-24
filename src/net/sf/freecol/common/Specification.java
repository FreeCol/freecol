package net.sf.freecol.common;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.client.gui.action.ImprovementActionType;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.EuropeanNationType;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.UnitType;

/**
 * This class encapsulates any parts of the "specification" for FreeCol that are
 * expressed best using XML. The XML is loaded through the class loader from the
 * resource named "specification.xml" in the same package as this class.
 */
public final class Specification {
    private static final Logger logger = Logger.getLogger(Specification.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2006-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private final List<BuildingType> buildingTypeList;

    private final List<GoodsType> goodsTypeList;
    private final List<GoodsType> farmedGoodsTypeList;

    private final List<ResourceType> resourceTypeList;

    private final List<TileType> tileTypeList;
    private final List<TileImprovementType> tileImprovementTypeList;
    private final List<ImprovementActionType> improvementActionTypeList;

    private final List<UnitType> unitTypeList;

    private final List<FoundingFather> foundingFathers;

    private final List<Nation> nations;
    private final List<NationType> nationTypes;

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
    protected Specification() {
        logger.info("Initializing Specification");

        buildingTypeList = new ArrayList<BuildingType>();
        goodsTypeList = new ArrayList<GoodsType>();
        resourceTypeList = new ArrayList<ResourceType>();
        tileTypeList = new ArrayList<TileType>();
        tileImprovementTypeList = new ArrayList<TileImprovementType>();
        improvementActionTypeList = new ArrayList<ImprovementActionType>();
        unitTypeList = new ArrayList<UnitType>();
        foundingFathers = new ArrayList<FoundingFather>();
        nations = new ArrayList<Nation>();
        nationTypes = new ArrayList<NationType>();

        final Map<String, GoodsType> goodsTypeByRef = new HashMap<String, GoodsType>();
        final Map<String, ResourceType> resourceTypeByRef = new HashMap<String, ResourceType>();
        final Map<String, TileType> tileTypeByRef = new HashMap<String, TileType>();
        final Map<String, TileImprovementType> tileImprovementTypeByRef = new HashMap<String, TileImprovementType>();
        final Map<String, BuildingType> buildingTypeByRef = new HashMap<String, BuildingType>();
        final Map<String, UnitType> unitTypeByRef = new HashMap<String, UnitType>();
        final Map<String, NationType> nationTypeByRef = new HashMap<String, NationType>();
        farmedGoodsTypeList = new ArrayList<GoodsType>();


        try {
            InputStream in = Specification.class.getResourceAsStream("specification.xml");
            XMLStreamReader xsr = XMLInputFactory.newInstance().createXMLStreamReader(in);
            xsr.nextTag();
            while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                String childName = xsr.getLocalName();

                if ("goods-types".equals(childName)) {

                    logger.finest("Found child named " + childName);
                    ObjectFactory<GoodsType> factory = new ObjectFactory<GoodsType>() {
                        int goodsIndex = 0;
                        public GoodsType objectFrom(XMLStreamReader in) throws XMLStreamException {
                            GoodsType goodsType = new GoodsType(goodsIndex++);
                            goodsType.readFromXML(in, goodsTypeByRef);
                            goodsTypeByRef.put(goodsType.getID(), goodsType);
                            if (goodsType.isFarmed()) {
                                farmedGoodsTypeList.add(goodsType);
                            }
                            return goodsType;
                        }
                    };
                    goodsTypeList.addAll(makeListFromXml(xsr, factory));

                } else if ("building-types".equals(childName)) {

                    logger.finest("Found child named " + childName);
                    ObjectFactory<BuildingType> factory = new ObjectFactory<BuildingType>() {
                        int buildingIndex = 0;
                        public BuildingType objectFrom(XMLStreamReader in) throws XMLStreamException {
                            BuildingType buildingType = new BuildingType(buildingIndex++);
                            buildingType.readFromXML(in, goodsTypeByRef, buildingTypeByRef);
                            buildingTypeByRef.put(buildingType.getID(), buildingType);
                            return buildingType;
                        }
                    };
                    buildingTypeList.addAll(makeListFromXml(xsr, factory));

                } else if ("resource-types".equals(childName)) {

                    logger.finest("Found child named " + childName);
                    ObjectFactory<ResourceType> factory = new ObjectFactory<ResourceType>() {
                        int resIndex = 0;
                        public ResourceType objectFrom(XMLStreamReader in) throws XMLStreamException {
                            ResourceType resourceType = new ResourceType(resIndex++);
                            resourceType.readFromXML(in, goodsTypeByRef);
                            resourceTypeByRef.put(resourceType.getID(), resourceType);
                            return resourceType;
                        }
                    };
                    resourceTypeList.addAll(makeListFromXml(xsr, factory));

                } else if ("tile-types".equals(childName)) {

                    logger.finest("Found child named " + childName);
                    ObjectFactory<TileType> factory = new ObjectFactory<TileType>() {
                        int tileIndex = 0;
                        public TileType objectFrom(XMLStreamReader in) throws XMLStreamException {
                            TileType tileType = new TileType(tileIndex++);
                            tileType.readFromXML(in, goodsTypeByRef, resourceTypeByRef);
                            tileTypeByRef.put(tileType.getID(), tileType);
                            return tileType;
                        }
                    };
                    tileTypeList.addAll(makeListFromXml(xsr, factory));

                } else if ("tileimprovement-types".equals(childName)) {

                    logger.finest("Found child named " + childName);
                    ObjectFactory<TileImprovementType> factory = new ObjectFactory<TileImprovementType>() {
                        int impIndex = 0;
                        public TileImprovementType objectFrom(XMLStreamReader in) throws XMLStreamException {
                            TileImprovementType tileImprovementType = new TileImprovementType(impIndex++);
                            tileImprovementType.readFromXML(in, tileTypeList,
                                                    tileTypeByRef, goodsTypeByRef, tileImprovementTypeByRef);
                            tileImprovementTypeByRef.put(tileImprovementType.getID(), tileImprovementType);
                            return tileImprovementType;
                        }
                    };
                    tileImprovementTypeList.addAll(makeListFromXml(xsr, factory));

                } else if ("improvementaction-types".equals(childName)) {

                    logger.finest("Found child named " + childName);
                    ObjectFactory<ImprovementActionType> factory = new ObjectFactory<ImprovementActionType>() {

                        public ImprovementActionType objectFrom(XMLStreamReader in) throws XMLStreamException {
                            ImprovementActionType impActionType = new ImprovementActionType();
                            impActionType.readFromXML(in, tileImprovementTypeByRef);
                            return impActionType;
                        }
                    };
                    improvementActionTypeList.addAll(makeListFromXml(xsr, factory));

                } else if ("unit-types".equals(childName)) {

                    logger.finest("Found child named " + childName);
                    ObjectFactory<UnitType> factory = new ObjectFactory<UnitType>() {
                        int unitIndex = 0;
                        public UnitType objectFrom(XMLStreamReader in) throws XMLStreamException {
                            UnitType unitType = new UnitType(unitIndex++);
                            unitType.readFromXML(in, goodsTypeByRef);
                            unitTypeByRef.put(unitType.getID(), unitType);
                            return unitType;
                        }
                    };
                    unitTypeList.addAll(makeListFromXml(xsr, factory));

                } else if ("founding-fathers".equals(childName)) {

                    logger.finest("Found child named " + childName);
                    ObjectFactory<FoundingFather> factory = new ObjectFactory<FoundingFather>() {
                        int fatherIndex = 0;
                        public FoundingFather objectFrom(XMLStreamReader in) throws XMLStreamException {
                            FoundingFather foundingFather = new FoundingFather(fatherIndex++);
                            foundingFather.readFromXML(in, goodsTypeByRef);
                            return foundingFather;
                        }
                    };
                    foundingFathers.addAll(makeListFromXml(xsr, factory));

                } else if ("nation-types".equals(childName)) {

                    logger.finest("Found child named " + childName);
                    ObjectFactory<NationType> factory = new ObjectFactory<NationType>() {
                        int nationIndex = 0;
                        public NationType objectFrom(XMLStreamReader in) throws XMLStreamException {
                            NationType nationType;
                            if ("european-nation-type".equals(in.getLocalName())) {
                                nationType = new EuropeanNationType(nationIndex++);
                            } else {
                                nationType = new IndianNationType(nationIndex++);
                            }                             
                            nationType.readFromXML(in, unitTypeByRef);
                            nationTypeByRef.put(nationType.getID(), nationType);
                            return nationType;
                        }
                    };
                    nationTypes.addAll(makeListFromXml(xsr, factory));

                } else if ("nations".equals(childName)) {

                    logger.finest("Found child named " + childName);
                    ObjectFactory<Nation> factory = new ObjectFactory<Nation>() {
                        int nationIndex = 0;
                        public Nation objectFrom(XMLStreamReader in) throws XMLStreamException {
                            Nation nation = new Nation(nationIndex++);
                            nation.readFromXML(in, nationTypeByRef);
                            return nation;
                        }
                    };
                    nations.addAll(makeListFromXml(xsr, factory));

                } else {
                    throw new RuntimeException("unexpected: " + childName);
                }
            }
        
            // Post specification actions
            // Get Food, Bells, Crosses and Hammers
            Goods.initialize(getGoodsTypeList(), numberOfGoodsTypes());
            Tile.initialize(numberOfTileTypes());

            logger.info("Specification initialization complete");
        } catch (XMLStreamException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            throw new RuntimeException("Error parsing specification");
        }
    }

    // ---------------------------------------------------------- retrieval methods
    
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

    /**
     * Describe <code>getBuildingIndex</code> method here.
     *
     * @param b a <code>BuildingType</code> value
     * @return an <code>int</code> value
     */
    public int getBuildingIndex(BuildingType b) {
        return buildingTypeList.indexOf(b);
    }

    public BuildingType getBuildingType(String id) {
        for (BuildingType b : buildingTypeList) {
            if (b.getID().equals(id)) {
                return b;
            }
        }
        return null;
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
        int n = 0;
        for (GoodsType g : goodsTypeList) {
            if (g.isStorable()) {
                n++;
            }
        }
        return n;
    }

    public List<GoodsType> getFarmedGoodsTypeList() {
        return farmedGoodsTypeList;
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
     * @param goodsTypeIndex an <code>int</code> value
     * @return a <code>GoodsType</code> value
     */
    public GoodsType getGoodsType(int goodsTypeIndex) {
        return goodsTypeList.get(goodsTypeIndex);
    }

    /**
     * Describe <code>getGoodsType</code> method here.
     *
     * @param name a <code>String</code> value
     * @return a <code>GoodsType</code> value
     */
    public GoodsType getGoodsType(String id) {
        for (GoodsType g : goodsTypeList) {
            if (g.getID().equals(id)) {
                return g;
            }
        }
        return null;
    }

    public List<GoodsType> getGoodsFood() {
        ArrayList<GoodsType> goods = new ArrayList<GoodsType>();
        for (GoodsType g : goodsTypeList) {
            if (g.isFoodType()) {
                goods.add(g);
            }
        }
        return goods;
    }


    /**
     * Returns a list of GoodsTypes for which the given predicate
     * obtains. For example, getMatchingGoodsTypes("isFarmed")
     * returns a list of all GoodsTypes g for which g.isFarmed()
     * returns true.
     *
     * @param methodName the name of the method to invoke on the GoodsType
     * @return a list of <code>GoodsType</code>s for which the given
     * predicate returns true
    */
    public List<GoodsType> getMatchingGoodsTypes(String methodName) throws Exception {
        Method method = GoodsType.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        ArrayList<GoodsType> goodsTypes = new ArrayList<GoodsType>();
        for (GoodsType goodsType : goodsTypeList) {
            if ((Boolean) method.invoke(goodsType)) {
                goodsTypes.add(goodsType);
            }
        }
        return goodsTypes;
    }   


    // -- Resources --
    public List<ResourceType> getResourceTypeList() {
        return resourceTypeList;
    }

    public int numberOfResourceTypes() {
        return resourceTypeList.size();
    }

    public ResourceType getResourceType(int index) {
        return resourceTypeList.get(index);
    }

    public ResourceType getResourceType(String id) {
        for (ResourceType r : resourceTypeList) {
            if (r.getID().equals(id)) {
                return r;
            }
        }
        return null;
    }

    // -- Tiles --
    public List<TileType> getTileTypeList() {
        return tileTypeList;
    }

    public int numberOfTileTypes() {
        return tileTypeList.size();
    }

    public TileType getTileType(int index) {
        return tileTypeList.get(index);
    }

    public TileType getTileType(String id) {
        for (TileType t : tileTypeList) {
            if (t.getID().equals(id)) {
                return t;
            }
        }
        return null;
    }

    // -- Improvements --
    public List<TileImprovementType> getTileImprovementTypeList() {
        return tileImprovementTypeList;
    }

    public TileImprovementType getTileImprovementType(int index) {
        return tileImprovementTypeList.get(index);
    }

    public TileImprovementType getTileImprovementType(String id) {
        for (TileImprovementType ti : tileImprovementTypeList) {
            if (ti.getID().equals(id)) {
                return ti;
            }
        }
        return null;
    }

    // -- Improvement Actions --
    public List<ImprovementActionType> getImprovementActionTypeList() {
        return improvementActionTypeList;
    }

    public ImprovementActionType getImprovementActionType(int index) {
        return improvementActionTypeList.get(index);
    }

    public ImprovementActionType getImprovementActionType(String id) {
        for (ImprovementActionType ia : improvementActionTypeList) {
            if (ia.getID().equals(id)) {
                return ia;
            }
        }
        return null;
    }

    // -- Units --
    public List<UnitType> getUnitTypeList() {
        return unitTypeList;
    }

    public int numberOfUnitTypes() {
        return unitTypeList.size();
    }

    public UnitType getUnitType(int unitTypeIndex) {
        return unitTypeList.get(unitTypeIndex);
    }

    public int getUnitIndex(UnitType b) {
        return unitTypeList.indexOf(b);
    }

    public UnitType getUnitType(String id) {
        for (UnitType u : unitTypeList) {
            if (u.getID().equals(id)) {
                return u;
            }
        }
        return null;
    }
    
    public UnitType getExpertForProducing(GoodsType goodsType) {
        for (UnitType unitType : getUnitTypeList()) {
            if (unitType.getExpertProduction() == goodsType) {
                return unitType;
            }
        }
        return null;
    }
    
    /**
     * Return the unit types which have any of the given abilities
     *
     * @param abilities The abilities for the search
     * @return a <code>List</code> of <code>UnitType</code>
     */
    public List<UnitType> getUnitTypesWithAnyAbility(String[] abilities) {
        ArrayList<UnitType> unitTypes = new ArrayList<UnitType>();
        for (UnitType unitType : getUnitTypeList()) {
            for (int i = 0; i < abilities.length; i++) {
                if (unitType.hasAbility(abilities[i])) {
                    unitTypes.add(unitType);
                    break;
                }
            }
        }
        return unitTypes;
    }

    public List<UnitType> getUnitTypesWithAbility(String ability) {
        return getUnitTypesWithAnyAbility(new String[] { ability });
    }

    // -- Founding Fathers --

    public List<FoundingFather> getFoundingFathers() {
        return foundingFathers;
    }

    public int numberOfFoundingFathers() {
        return foundingFathers.size();
    }

    public FoundingFather getFoundingFather(int foundingFatherIndex) {
        return foundingFathers.get(foundingFatherIndex);
    }

    public int getFoundingFatherIndex(FoundingFather father) {
        return foundingFathers.indexOf(father);
    }

    public FoundingFather getFoundingFather(String id) {
        for (FoundingFather father : foundingFathers) {
            if (father.getID().equals(id)) {
                return father;
            }
        }
        return null;
    }

    // -- NationTypes --

    public List<NationType> getNationTypes() {
        return nationTypes;
    }

    public List<EuropeanNationType> getEuropeanNationTypes() {
        List<EuropeanNationType> result = new ArrayList<EuropeanNationType>();
        for (NationType nationType : nationTypes) {
            if (nationType.isEuropean() && !nationType.isREF()) {
                result.add((EuropeanNationType) nationType);
            }
        }
        return result;
    }

    public List<EuropeanNationType> getREFNationTypes() {
        List<EuropeanNationType> result = new ArrayList<EuropeanNationType>();
        for (NationType nationType : nationTypes) {
            if (nationType.isEuropean() && nationType.isREF()) {
                result.add((EuropeanNationType) nationType);
            }
        }
        return result;
    }

    public List<IndianNationType> getIndianNationTypes() {
        List<IndianNationType> result = new ArrayList<IndianNationType>();
        for (NationType nationType : nationTypes) {
            if (!nationType.isEuropean()) {
                result.add((IndianNationType) nationType);
            }
        }
        return result;
    }

    public List<EuropeanNationType> getClassicNationTypes() {
        ArrayList<EuropeanNationType> result = new ArrayList<EuropeanNationType>();
        for (Nation nation : nations) {
            if (nation.isClassic()) {
                result.add((EuropeanNationType) nation.getType());
            }
        }
        return result;
    }

    public int numberOfNationTypes() {
        return nationTypes.size();
    }

    public NationType getNationType(int nationIndex) {
        return nationTypes.get(nationIndex);
    }

    public int getNationTypeIndex(NationType nation) {
        return nationTypes.indexOf(nation);
    }

    public NationType getNationType(String id) {
        for (NationType nation : nationTypes) {
            if (nation.getID().equals(id)) {
                return nation;
            }
        }
        return null;
    }

    public List<Nation> getNations() {
        return nations;
    }

    public Nation getNation(int index) {
        return nations.get(index);
    }

    public Nation getNation(String id) {
        for (Nation nation : nations) {
            if (nation.getID().equals(id)) {
                return nation;
            }
        }
        return null;
    }

    public List<Nation> getClassicNations() {
        ArrayList<Nation> result = new ArrayList<Nation>();
        for (Nation nation : nations) {
            if (nation.isClassic()) {
                result.add(nation);
            }
        }
        return result;
    }

    public List<Nation> getEuropeanNations() {
        ArrayList<Nation> result = new ArrayList<Nation>();
        for (Nation nation : nations) {
            if (nation.isSelectable()) {
                result.add(nation);
            }
        }
        return result;
    }

    /**
     * Takes an XML node with child nodes that represent objects of the type
     * <code>T</code> and returns a list of the deserialized objects of type
     * <code>T</code>.
     * 
     * @param <T> the type of objects to deserialize
     * @param xml the XML node to whose children to deserialize.
     * @param factory the factory used to deserialize the object
     * @return a list containing all the child elements of the node deserialized
     */
    private <T> List<T> makeListFromXml(XMLStreamReader in, final ObjectFactory<T> factory) throws XMLStreamException {
        final ArrayList<T> list = new ArrayList<T>();
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            list.add(factory.objectFrom(in));
        }
        return list;
    }


    /**
     * The interface that needs to be implemented and passed to
     * <code>makeListFromXml</code> to deserialize the objects from the XML
     * file.
     * 
     * @param <T> the type object that will be deserialized.
     */
    static interface ObjectFactory<T> {

        /**
         * Converts an XML node to an object of the generic type of the factory.
         * 
         * @param xml an XML node to convert to an object
         * @return the object
         */
        public T objectFrom(XMLStreamReader in) throws XMLStreamException;
    }

    /**
     * Singleton
     */
    protected static Specification specification;
    
	public static Specification getSpecification() {
		if (specification == null){
            specification = new Specification();
        }
        return specification;
	}
}
