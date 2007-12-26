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

package net.sf.freecol.common.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.client.gui.i18n.Messages;

/**
 * Contains information on buildable types.
 */
public class BuildableType extends FreeColGameObjectType implements Features {

    public static final int UNDEFINED = Integer.MIN_VALUE;

    public static final BuildableType NOTHING = new BuildableType("model.buildableType.nothing");
    
    /**
     * The minimum population that a Colony needs in order to build
     * this type.
     */
    private int populationRequired = 1;

    /**
     * Contains the abilities and modifiers of this type.
     */
    private FeatureContainer featureContainer = new FeatureContainer();

    /**
     * A list of AbstractGoods required to build this type.
     */
    private List<AbstractGoods> goodsRequired;
    
    /**
     * Stores the abilities required by this Type.
     */
    private HashMap<String, Boolean> requiredAbilities = new HashMap<String, Boolean>();
    
    public BuildableType() {}

    private BuildableType(String id) {
        setId(id);
    }

    public String getGoodsRequiredAsString() {
        String result = "";
        if (goodsRequired != null) {
            for (AbstractGoods goods : goodsRequired) {
                result += Messages.message("model.goods.goodsAmount",
                                           "%amount%", String.valueOf(goods.getAmount()),
                                           "%goods%", goods.getType().getName()) +
                    ", ";
            }
            if (result.length() > 1) {
                result = result.substring(0, result.length() - 2);
            }
        }
        return result;
    }

    /**
     * Get the <code>GoodsRequired</code> value.
     *
     * @return a <code>List<AbstractGoods></code> value
     */
    public final List<AbstractGoods> getGoodsRequired() {
        return goodsRequired;
    }

    /**
     * Set the <code>GoodsRequired</code> value.
     *
     * @param newGoodsRequired The new GoodsRequired value.
     */
    public final void setGoodsRequired(final List<AbstractGoods> newGoodsRequired) {
        this.goodsRequired = newGoodsRequired;
    }

    /**
     * Get the <code>PopulationRequired</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getPopulationRequired() {
        return populationRequired;
    }

    /**
     * Set the <code>PopulationRequired</code> value.
     *
     * @param newPopulationRequired The new PopulationRequired value.
     */
    public void setPopulationRequired(final int newPopulationRequired) {
        this.populationRequired = newPopulationRequired;
    }

    /**
     * Returns the abilities required by this Type.
     *
     * @return the abilities required by this Type.
     */
    public Map<String, Boolean> getAbilitiesRequired() {
        return requiredAbilities;
    }

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
    }

    /**
     * Returns a copy of the object's features.
     *
     * @return a <code>List</code> value
     */
    public List<Feature> getFeatures() {
        return featureContainer.getFeatures();
    }

    /**
     * Get the <code>Ability</code> value.
     *
     * @param id a <code>String</code> value
     * @return a <code>Ability</code> value
     */
    public final Ability getAbility(String id) {
        return featureContainer.getAbility(id);
    }

    /**
     * Returns true if the Object has the ability identified by
     * <code>id</code>.
     *
     * @param id a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasAbility(String id) {
        return featureContainer.hasAbility(id);
    }

    /**
     * Returns the Modifier identified by <code>id</code>.
     *
     * @param id a <code>String</code> value
     * @return a <code>Modifier</code> value
     */
    public Modifier getModifier(String id) {
        return featureContainer.getModifier(id);
    }

    /**
     * Add the given Feature to the Features Map. If the Feature given
     * can not be combined with a Feature with the same ID already
     * present, the old Feature will be replaced.
     *
     * @param feature a <code>Feature</code> value
     */
    public void addFeature(Feature feature) {
        featureContainer.addFeature(feature);
    }

}
