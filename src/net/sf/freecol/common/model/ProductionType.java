/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * This class describes a possible production type of a tile or building.
 */
public class ProductionType extends FreeColObject {

    /** Whether this production type applies only to colony center tiles. */
    private boolean unattended;

    /**
     * The production level of this production type (usually a
     * difficulty level key).
     */
    private String productionLevel;

    /** The goods that are produced by this production type. */
    private List<AbstractGoods> outputs = null;

    /** The goods that are consumed by this production type. */
    private List<AbstractGoods> inputs = null;


    /**
     * Simple constructor.
     *
     * @param specification The enclosing <code>Specification</code>.
     */
    public ProductionType(Specification specification) {
        setSpecification(specification);
    }

    /**
     * Creates a new production type that consumes no raw materials
     * and produces the given output.
     *
     * @param outputs A list of the <code>AbstractGoods</code> produced.
     */
    public ProductionType(List<AbstractGoods> outputs) {
        this.outputs = outputs;
    }

    /**
     * Creates a new production type that consumes the given raw materials
     * and produces the given output.
     *
     * @param inputs A list of the <code>AbstractGoods</code> consumed.
     * @param outputs A list of the <code>AbstractGoods</code> produced.
     */
    public ProductionType(List<AbstractGoods> inputs, List<AbstractGoods> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    /**
     * Convenience constructor for a production type with a single output.
     *
     * @param output The <code>AbstractGoods</code> produced.
     * @param center True if this is a colony center tile.
     * @param level The difficulty level key.
     */
    public ProductionType(AbstractGoods output, boolean center, String level) {
        outputs = new ArrayList<AbstractGoods>();
        outputs.add(output);
        unattended = center;
        productionLevel = level;
    }

    /**
     * Convenience constructor for a new <code>ProductionType</code>
     * instance with a single input and output.
     *
     * @param input The <code>GoodsType</code> consumed.
     * @param output The <code>GoodsType</code> produced.
     * @param amount The amount of goods both produced and consumed.
     */
    public ProductionType(GoodsType input, GoodsType output, int amount) {
        if (input != null) {
            inputs = new ArrayList<AbstractGoods>();
            inputs.add(new AbstractGoods(input, amount));
        }
        if (output != null) {
            outputs = new ArrayList<AbstractGoods>();
            outputs.add(new AbstractGoods(output, amount));
        }
    }

    /**
     * Creates a new <code>ProductionType</code> instance.
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     */
    public ProductionType(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in);
    }

    /**
     * Get the input goods.
     *
     * @return A list of the input <code>AbstractGoods</code>.
     */
    public final List<AbstractGoods> getInputs() {
        return inputs;
    }

    /**
     * Set the input goods.
     *
     * @param newInputs The new list of input <code>AbstractGoods</code>.
     */
    public final void setInputs(final List<AbstractGoods> newInputs) {
        this.inputs = newInputs;
    }

    /**
     * Get the output goods.
     *
     * @return A list of the output <code>AbstractGoods</code>.
     */
    public final List<AbstractGoods> getOutputs() {
        return outputs;
    }

    /**
     * Set the output goods.
     *
     * @param newOutputs The new list of output <code>AbstractGoods</code>.
     */
    public final void setOutputs(final List<AbstractGoods> newOutputs) {
        this.outputs = newOutputs;
    }

    public AbstractGoods getOutput(GoodsType goodsType) {
        if (inputs == null && outputs != null) {
            for (AbstractGoods output : outputs) {
                if (goodsType == output.getType()) {
                    return output;
                }
            }
        }
        return null;
    }

    /**
     * Is this production from a colony center tile?
     *
     * @return True if this production is from a colony center tile.
     */
    public final boolean isColonyCenterTile() {
        return unattended;
    }

    /**
     * The production level of this type of production (used by
     * difficulty levels).
     *
     * @return The production level key.
     */
    public final String getProductionLevel() {
        return productionLevel;
    }

    /**
     * Does this production apply to a given difficulty level.
     *
     * @param The difficulty level key to check.
     * @return True if this production applies.
     */
    public boolean appliesTo(String level) {
        return level == null
            || productionLevel == null
            || level.equals(productionLevel);
    }


    // Serialization

    private static final String UNATTENDED_TAG = "unattended";
    private static final String GOODS_TYPE_TAG = "goods-type";
    private static final String INPUT_TAG = "input";
    private static final String OUTPUT_TAG = "output";
    private static final String PRODUCTION_LEVEL_TAG = "productionLevel";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        // ProductionType does not need an id.
        // No need for: super.writeAttributes(out);

        if (unattended) {
            writeAttribute(out, UNATTENDED_TAG, unattended);
        }

        if (productionLevel != null) {
            writeAttribute(out, PRODUCTION_LEVEL_TAG, productionLevel);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        if (inputs != null) {
            for (AbstractGoods input : inputs) {
                out.writeStartElement(INPUT_TAG);

                writeAttribute(out, GOODS_TYPE_TAG, input.getType());

                writeAttribute(out, VALUE_TAG, input.getAmount());

                out.writeEndElement();
            }
        }

        if (outputs != null) {
            for (AbstractGoods output : outputs) {
                out.writeStartElement(OUTPUT_TAG);

                writeAttribute(out, GOODS_TYPE_TAG, output.getType());

                writeAttribute(out, VALUE_TAG, output.getAmount());

                out.writeEndElement();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readAttributes(XMLStreamReader in) throws XMLStreamException {
        // ProductionType does not need an id.
        // No need for: super.readAttributes(in);
        // TODO: as soon as we allow the user to select a production type,
        // we will need an ID

        unattended = getAttribute(in, UNATTENDED_TAG, false);

        productionLevel = getAttribute(in, PRODUCTION_LEVEL_TAG, (String)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChildren(XMLStreamReader in) throws XMLStreamException {
        // Clear containers
        if (inputs != null) inputs.clear();
        if (outputs != null) outputs.clear();

        super.readChildren(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChild(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = in.getLocalName();

        if (INPUT_TAG.equals(tag)) {
            GoodsType type = spec.getType(in, GOODS_TYPE_TAG,
                                          GoodsType.class, (GoodsType)null);
            if (type == null) {
                logger.warning("Invalid goods type: "
                    + in.getAttributeValue(null, GOODS_TYPE_TAG));
            }

            int amount = getAttribute(in, VALUE_TAG, -1);
            if (amount < 0) {
                logger.warning("Invalid amount: "
                    + in.getAttributeValue(null, VALUE_TAG));
            }

            if (type != null && amount >= 0) {
                if (inputs == null) inputs = new ArrayList<AbstractGoods>(1);
                inputs.add(new AbstractGoods(type, amount));
            }
            in.nextTag();

        } else if (OUTPUT_TAG.equals(tag)) {
            GoodsType type = spec.getType(in, GOODS_TYPE_TAG,
                                          GoodsType.class, (GoodsType)null);
            if (type == null) {
                logger.warning("Invalid goods type: "
                    + in.getAttributeValue(null, GOODS_TYPE_TAG));
            }

            int amount = getAttribute(in, VALUE_TAG, -1);
            if (amount < 0) {
                logger.warning("Invalid amount: "
                    + in.getAttributeValue(null, VALUE_TAG));
            }

            if (type != null && amount >= 00) {
                if (outputs == null) outputs = new ArrayList<AbstractGoods>(1);
                outputs.add(new AbstractGoods(type, amount));
            }
            in.nextTag();

        } else {
            super.readChild(in);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("[");
        result.append(getXMLElementTagName()).append(": ");
        if (productionLevel != null) {
            result.append(productionLevel);
        }
        if (unattended) {
            result.append(", colony center tile");
        }
        if (!(inputs == null || inputs.isEmpty())) {
            result.append(" [inputs: ");
            for (AbstractGoods input : inputs) {
                result.append(input).append(", ");
            }
            int length = result.length();
            result.replace(length - 2, length, "]");
        }
        if (!(outputs == null || outputs.isEmpty())) {
            result.append(" [outputs: ");
            for (AbstractGoods output : outputs) {
                result.append(output).append(", ");
            }
            int length = result.length();
            result.replace(length - 2, length, "]");
        }
        result.append("]");
        return result.toString();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "production".
     */
    public static String getXMLElementTagName() {
        return "production";
    }
}
