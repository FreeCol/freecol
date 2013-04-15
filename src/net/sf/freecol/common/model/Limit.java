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
 *  MERCHANTLIMIT or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Operand.OperandType;


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
     * The operator to apply when evaluating the limit expression.
     */
    private Operator operator;

    /**
     * The left hand side term of the limit expression.
     */
    private Operand leftHandSide;

    /**
     * The right hand side term of the limit expression.
     */
    private Operand rightHandSide;


    /**
     * Create a new limit.
     *
     * @param specification The containing <code>Specification</code>.
     */
    public Limit(Specification specification) {
        super(specification);
    }

    /**
     * Create a new limit.
     *
     * @param id The identifier for this <code>FreeColObject</code>.
     * @param lhs The left hand side <code>Operand</code>.
     * @param op The <code>Operator</code> to apply.
     * @param rhs The right hand side <code>Operand</code>.
     */
    public Limit(String id, Operand lhs, Operator op, Operand rhs) {
        setId(id);
        leftHandSide = lhs;
        rightHandSide = rhs;
        operator = op;
    }

    /**
     * Get the <code>Operator</code> value.
     *
     * @return The <code>Operator</code> of this limit.
     */
    public Operator getOperator() {
        return operator;
    }

    /**
     * Set the <code>Operator</code> value.
     *
     * @param newOperator The new <code>Operator</code> value.
     */
    public void setOperator(final Operator newOperator) {
        this.operator = newOperator;
    }

    /**
     * Get the left hand side <code>Operand</code>.
     *
     * @return The left hand side <code>Operand</code>.
     */
    public Operand getLeftHandSide() {
        return leftHandSide;
    }

    /**
     * Set the left hand side <code>Operand</code>.
     *
     * @param newLeftHandSide The new left hand side <code>Operand</code>.
     */
    public void setLeftHandSide(final Operand newLeftHandSide) {
        this.leftHandSide = newLeftHandSide;
    }

    /**
     * Get the right hand side <code>Operand</code>.
     *
     * @return The right hand side <code>Operand</code>.
     */
    public Operand getRightHandSide() {
        return rightHandSide;
    }

    /**
     * Set the right hand side <code>Operand</code>.
     *
     * @param newRightHandSide The new right hand side <code>Operand</code>.
     */
    public void setRightHandSide(final Operand newRightHandSide) {
        this.rightHandSide = newRightHandSide;
    }

    /**
     * Does this limit apply to an object?
     *
     * @param object The object to test.
     * @return True if the limit is applicable.
     */
    public boolean appliesTo(FreeColObject object) {
        return leftHandSide.appliesTo(object);
    }

    /**
     * Evaluate this limit within a game.
     *
     * @param game The <code>Game</code> to use.
     * @return The result of the evaluation.
     */
    public boolean evaluate(Game game) {
        Integer lhs = null;
        switch (leftHandSide.getScopeLevel()) {
        case GAME:
            lhs = leftHandSide.getValue(game);
            break;
        default:
            lhs = leftHandSide.getValue();
            break;
        }

        Integer rhs = null;
        switch (rightHandSide.getScopeLevel()) {
        case GAME:
            rhs = rightHandSide.getValue(game);
            break;
        default:
            rhs = rightHandSide.getValue();
            break;
        }

        return evaluate(lhs, rhs);
    }

    /**
     * Evaluate this limit with respect to a player.
     *
     * @param player The <code>Player</code> to use.
     * @return The result of the evaluation.
     */
    public boolean evaluate(Player player) {
        Integer lhs = null;
        switch (leftHandSide.getScopeLevel()) {
        case PLAYER:
            lhs = leftHandSide.getValue(player);
            break;
        case GAME:
            lhs = leftHandSide.getValue(player.getGame());
            break;
        default:
            lhs = leftHandSide.getValue();
            break;
        }

        Integer rhs = null;
        switch (rightHandSide.getScopeLevel()) {
        case PLAYER:
            rhs = rightHandSide.getValue(player);
            break;
        case GAME:
            rhs = rightHandSide.getValue(player.getGame());
            break;
        default:
            rhs = rightHandSide.getValue();
            break;
        }

        return evaluate(lhs, rhs);
    }

    /**
     * Evaluate this limit with respect to a player.
     *
     * @param settlement The <code>Settlement</code> to use.
     * @return The result of the evaluation.
     */
    public boolean evaluate(Settlement settlement) {
        final Specification spec = getSpecification();
        Integer lhs = null;
        switch (leftHandSide.getScopeLevel()) {
        case SETTLEMENT:
            lhs = leftHandSide.getValue(settlement);
            lhs += spec.getInteger("model.option.settlementLimitModifier");
            break;
        case PLAYER:
            lhs = leftHandSide.getValue(settlement.getOwner());
            break;
        case GAME:
            lhs = leftHandSide.getValue(settlement.getGame());
            break;
        default:
            lhs = leftHandSide.getValue();
            break;
        }

        Integer rhs = null;
        switch (rightHandSide.getScopeLevel()) {
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
            break;
        }

        return evaluate(lhs, rhs);
    }

    /**
     * Check if at least one of the Operands has a given OperandType.
     *
     * @param type The <code>OperandType</code> to check for.
     * @return True if the type is present.
     */
    public boolean hasOperandType(OperandType type) {
        return leftHandSide.getOperandType() == type
            || rightHandSide.getOperandType() == type;
    }

    /**
     * Evaluate two integers using the limit operator.
     *
     * @param lhs The left hand side <code>Integer</code>.
     * @param rhs The right hand side <code>Integer</code>.
     * @return The result of the evaluation.
     */
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


    // Serialization

    private static final String LEFT_HAND_SIDE_TAG = "leftHandSide";
    private static final String OPERATOR_TAG = "operator";
    private static final String RIGHT_HAND_SIDE_TAG = "rightHandSide";

    /**
     * {@inheritDoc}
     */
    @Override
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        // Force operator to be an upper case string (do not use the
        // writeAttribute(... Enum) routine which downcases).
        writeAttribute(out, OPERATOR_TAG, operator.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        leftHandSide.toXML(out, LEFT_HAND_SIDE_TAG);
        rightHandSide.toXML(out, RIGHT_HAND_SIDE_TAG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        operator = getAttribute(in, OPERATOR_TAG,
                                Operator.class, (Operator)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final String tag = in.getLocalName();

        if (LEFT_HAND_SIDE_TAG.equals(tag)) {
            leftHandSide = new Operand();
            leftHandSide.readFromXML(in);

        } else if (RIGHT_HAND_SIDE_TAG.equals(tag)) {
            rightHandSide = new Operand();
            rightHandSide.readFromXML(in);

        } else {
            super.readChild(in);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(leftHandSide.toString());
        sb.append(" ").append(operator.toString())
            .append(" ").append(rightHandSide.toString());
        return sb.toString();
    }

    /**
     * Gets the XML tag name for this element.
     *
     * @return "limit".
     */
    public static String getXMLElementTagName() {
        return "limit";
    }
}
