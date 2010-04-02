/**
 *  Copyright (C) 2002-2010  The FreeCol Team
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
 *  MERCHANTLIMIT or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


import org.w3c.dom.Element;


/**
 * The <code>Limit</code> class encapsulates a limit on the
 * availability of FreeColObjects. It can be used to limit the number
 * of units of a particular type (e.g. wagon trains) to the number of
 * a player's colonies, for example.
 */
public final class Limit extends FreeColObject {

    public static enum Operator {
        EQ, LT, GT, LE, GE
    }

    /**
     * Describe operator here.
     */
    private Operator operator;

    /**
     * Describe leftHandSide here.
     */
    private Operand leftHandSide;

    /**
     * Describe rightHandSide here.
     */
    private Operand rightHandSide;

    /**
     * Creates a new <code>Limit</code> instance.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @exception XMLStreamException if an error occurs
     */
    public Limit(XMLStreamReader in) throws XMLStreamException {
        readFromXMLImpl(in);
    }

    public Limit(String id, Operand lhs, Operator op, Operand rhs) {
        setId(id);
        leftHandSide = lhs;
        rightHandSide = rhs;
        operator = op;
    }
    
    /**
     * Get the <code>Operator</code> value.
     *
     * @return an <code>Operator</code> value
     */
    public Operator getOperator() {
        return operator;
    }

    /**
     * Set the <code>Operator</code> value.
     *
     * @param newOperator The new Operator value.
     */
    public void setOperator(final Operator newOperator) {
        this.operator = newOperator;
    }

    /**
     * Get the <code>LeftHandSide</code> value.
     *
     * @return an <code>Operand</code> value
     */
    public Operand getLeftHandSide() {
        return leftHandSide;
    }

    /**
     * Set the <code>LeftHandSide</code> value.
     *
     * @param newLeftHandSide The new LeftHandSide value.
     */
    public void setLeftHandSide(final Operand newLeftHandSide) {
        this.leftHandSide = newLeftHandSide;
    }

    /**
     * Get the <code>RightHandSide</code> value.
     *
     * @return an <code>Operand</code> value
     */
    public Operand getRightHandSide() {
        return rightHandSide;
    }

    /**
     * Set the <code>RightHandSide</code> value.
     *
     * @param newRightHandSide The new RightHandSide value.
     */
    public void setRightHandSide(final Operand newRightHandSide) {
        this.rightHandSide = newRightHandSide;
    }

    public boolean appliesTo(Game game) {
        return evaluate(leftHandSide.getValue(game),
                        rightHandSide.getValue(game));
    }

    public boolean appliesTo(Player player) {
        return evaluate(leftHandSide.getValue(player),
                        rightHandSide.getValue(player));
    }

    public boolean appliesTo(Settlement settlement) {
        return evaluate(leftHandSide.getValue(settlement),
                        rightHandSide.getValue(settlement));
    }

    private boolean evaluate(Integer lhs, Integer rhs) {
        if (lhs == null || rhs == null) {
            return false;
        }
        switch(operator) {
        case EQ: return lhs == rhs;
        case LT: return lhs < rhs;
        case GT: return lhs > rhs;
        case LE: return lhs <= rhs;
        case GE: return lhs >= rhs;
        default: return false;
        }
    }


    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */    
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        writeAttributes(out);
        writeChildren(out);
        out.writeEndElement();
    }

    public void readAttributes(XMLStreamReader in) throws XMLStreamException {
        operator = Enum.valueOf(Operator.class, in.getAttributeValue(null, "operator"));
    }

    public void readChildren(XMLStreamReader in) throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if ("left-hand-side".equals(in.getLocalName())) {
                leftHandSide = new Operand();
                leftHandSide.readFromXMLImpl(in);
            } else if ("right-hand-side".equals(in.getLocalName())) {
                rightHandSide = new Operand();
                rightHandSide.readFromXMLImpl(in);
            } else {
                logger.warning("Unknown child: " + in.getLocalName());
            }
        }
    }
    
    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        out.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        out.writeAttribute("operator", operator.toString());
    }

    public void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        leftHandSide.toXMLImpl(out, "left-hand-side");
        rightHandSide.toXMLImpl(out, "right-hand-side");
    }

    /**
     * Returns the XML tag name for this element.
     *
     * @return a <code>String</code> limit
     */
    public static String getXMLElementTagName() {
        return "limit";
    }

    public String toString() {
        return leftHandSide.toString() + " " + operator.toString() + " "
            + rightHandSide.toString();
    }


}
