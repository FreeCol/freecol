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

import net.sf.freecol.common.model.Operand.OperandType;

import org.w3c.dom.Element;


/**
 * The <code>Limit</code> class encapsulates a limit on the
 * availability of FreeColObjects. It consists of a left hand side, an
 * operator and a right hand side, and can be used to limit the number
 * of units of a particular type (e.g. wagon trains) to the number of
 * a player's colonies, for example. The left hand side must apply to
 * the object on which a limit is to be placed.
 */
public final class Limit extends FreeColGameObjectType {

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

    public Limit() {
        // empty constructor
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

    public boolean appliesTo(FreeColObject object) {
        return leftHandSide.appliesTo(object);
    }

    /**
     * Describe <code>evaluate</code> method here.
     *
     * @param game a <code>Game</code> value
     * @return a <code>boolean</code> value
     */
    public boolean evaluate(Game game) {
        Integer lhs = null;
        switch(leftHandSide.getScopeLevel()) {
        case GAME:
            lhs = leftHandSide.getValue(game);
            break;
        default:
            lhs = leftHandSide.getValue();
        }

        Integer rhs = null;
        switch(rightHandSide.getScopeLevel()) {
        case GAME:
            rhs = rightHandSide.getValue(game);
            break;
        default:
            rhs = rightHandSide.getValue();
        }

        return evaluate(lhs, rhs);
    }

    /**
     * Describe <code>evaluate</code> method here.
     *
     * @param player a <code>Player</code> value
     * @return a <code>boolean</code> value
     */
    public boolean evaluate(Player player) {
        Integer lhs = null;
        switch(leftHandSide.getScopeLevel()) {
        case PLAYER:
            lhs = leftHandSide.getValue(player);
            break;
        case GAME:
            lhs = leftHandSide.getValue(player.getGame());
            break;
        default:
            lhs = leftHandSide.getValue();
        }

        Integer rhs = null;
        switch(rightHandSide.getScopeLevel()) {
        case PLAYER:
            rhs = rightHandSide.getValue(player);
            break;
        case GAME:
            rhs = rightHandSide.getValue(player.getGame());
            break;
        default:
            rhs = rightHandSide.getValue();
        }

        return evaluate(lhs, rhs);
    }

    /**
     * Describe <code>evaluate</code> method here.
     *
     * @param settlement a <code>Settlement</code> value
     * @return a <code>boolean</code> value
     */
    public boolean evaluate(Settlement settlement) {
        Integer lhs = null;
        switch(leftHandSide.getScopeLevel()) {
        case SETTLEMENT:
            lhs = leftHandSide.getValue(settlement);
            break;
        case PLAYER:
            lhs = leftHandSide.getValue(settlement.getOwner());
            break;
        case GAME:
            lhs = leftHandSide.getValue(settlement.getGame());
            break;
        default:
            lhs = leftHandSide.getValue();
        }

        Integer rhs = null;
        switch(rightHandSide.getScopeLevel()) {
        case SETTLEMENT:
            rhs = rightHandSide.getValue(settlement);
            break;
        case PLAYER:
            rhs = rightHandSide.getValue(settlement.getOwner());
            break;
        case GAME:
            rhs = rightHandSide.getValue(settlement.getGame());
            break;
        default:
            rhs = rightHandSide.getValue();
        }

        return evaluate(lhs, rhs);
    }

    /**
     * Returns true if at least one of the Operands has the given
     * OperandType.
     *
     * @param type an <code>OperandType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasOperandType(OperandType type) {
        return leftHandSide.getOperandType() == type
            || rightHandSide.getOperandType() == type;
    }

    private boolean evaluate(Integer lhs, Integer rhs) {
        if (lhs == null || rhs == null) {
            return true;
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

    @Override
    public void readAttributes(XMLStreamReader in, Specification specification)
        throws XMLStreamException {
        operator = Enum.valueOf(Operator.class, in.getAttributeValue(null, "operator"));
    }

    @Override
    public void readChildren(XMLStreamReader in, Specification specification)
        throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if ("leftHandSide".equals(in.getLocalName())) {
                leftHandSide = new Operand();
                leftHandSide.readFromXMLImpl(in);
            } else if ("rightHandSide".equals(in.getLocalName())) {
                rightHandSide = new Operand();
                rightHandSide.readFromXMLImpl(in);
            } else {
                logger.warning("Unsupported child element: " + in.getLocalName());
            }
        }
    }
    
    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        out.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        out.writeAttribute("operator", operator.toString());
    }

    public void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        leftHandSide.toXMLImpl(out, "leftHandSide");
        rightHandSide.toXMLImpl(out, "rightHandSide");
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
