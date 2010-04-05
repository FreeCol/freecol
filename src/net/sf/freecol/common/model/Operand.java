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

import java.lang.reflect.Method;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * The <code>Operand</code> class implements Operands to be used in
 * relations, such as the Limit class. The OperandType specifies which
 * types of objects will be considered, and the ScopeLevel specifies
 * the level at which these objects are to be selected. If the
 * ScopeLevel is PLAYER, for example, and the OperandType is UNITS,
 * then all units owned by a particular player will be considered.
 *
 * Since the class inherits from Scope, the choice of objects can be
 * further refined by specifying type, ability or method. However, the
 * return value of the method must be an Integer (or int), since this
 * value will be returned as the value of the Operand itself if the
 * OperandType is NONE.
 */
public class Operand extends Scope {

    private static final Logger logger = Logger.getLogger(Operand.class.getName());

    public static enum OperandType {
        UNITS, BUILDINGS, SETTLEMENTS, FOUNDING_FATHERS, NONE
    }

    public static enum ScopeLevel {
        SETTLEMENT, PLAYER, GAME, NONE
    }

    /**
     * Describe operandType here.
     */
    private OperandType operandType = OperandType.NONE;

    /**
     * Describe scopeLevel here.
     */
    private ScopeLevel scopeLevel = ScopeLevel.NONE;

    /**
     * A fixed rather than a dynamic Integer value.
     */
    private Integer value = null;


    /**
     * Creates a new <code>Operand</code> instance.
     *
     */
    public Operand() {
        // empty constructor
    }

    /**
     * Creates a new <code>Operand</code> instance.
     *
     * @param value an <code>int</code> value
     */
    public Operand(int value) {
        this.value = value;
    }

    /**
     * Creates a new <code>Operand</code> instance.
     *
     * @param operandType an <code>OperandType</code> value
     * @param scopeLevel a <code>ScopeLevel</code> value
     */
    public Operand(OperandType operandType, ScopeLevel scopeLevel) {
        this.operandType = operandType;
        this.scopeLevel = scopeLevel;
    }

    /**
     * Get the <code>OperandType</code> value.
     *
     * @return an <code>OperandType</code> value
     */
    public final OperandType getOperandType() {
        return operandType;
    }

    /**
     * Set the <code>OperandType</code> value.
     *
     * @param newOperandType The new OperandType value.
     */
    public final void setOperandType(final OperandType newOperandType) {
        this.operandType = newOperandType;
    }

    /**
     * Get the <code>ScopeLevel</code> value.
     *
     * @return a <code>ScopeLevel</code> value
     */
    public final ScopeLevel getScopeLevel() {
        return scopeLevel;
    }

    /**
     * Set the <code>ScopeLevel</code> value.
     *
     * @param newScopeLevel The new ScopeLevel value.
     */
    public final void setScopeLevel(final ScopeLevel newScopeLevel) {
        this.scopeLevel = newScopeLevel;
    }

    /**
     * Get the <code>Value</code> value.
     *
     * @return an <code>Integer</code> value
     */
    public final Integer getValue() {
        return value;
    }

    /**
     * Set the <code>Value</code> value.
     *
     * @param newValue The new Value value.
     */
    public final void setValue(final Integer newValue) {
        this.value = newValue;
    }

    /**
     * Returns an Integer value if this Operand is applicable to the
     * given Game, and <code>null</code> otherwise.
     *
     * @param game a <code>Game</code> value
     * @return an <code>Integer</code> value
     */
    public Integer getValue(Game game) {
        if (value == null) {
            if (scopeLevel == ScopeLevel.GAME) {
                List<FreeColObject> list = new LinkedList<FreeColObject>();
                if (operandType == OperandType.NONE
                    && getMethodName() != null) {
                    try {
                        Method method = game.getClass().getMethod(getMethodName());
                        if (method != null &&
                            Integer.class.isAssignableFrom(method.getReturnType())) {
                            return (Integer) method.invoke(game);
                        } else {
                            return null;
                        }
                    } catch(Exception e) {
                        logger.warning(e.toString());
                        return null;
                    }
                } else {
                    for (Player player : game.getPlayers()) {
                        switch(operandType) {
                        case UNITS:
                            list.addAll(player.getUnits());
                            break;
                        case BUILDINGS:
                            for (Colony colony : player.getColonies()) {
                                list.addAll(colony.getBuildings());
                            }
                            break;
                        case SETTLEMENTS:
                            list.addAll(player.getSettlements());
                            break;
                        case FOUNDING_FATHERS:
                            list.addAll(player.getFathers());
                            break;
                        default:
                            return null;
                        }
                    }
                    return count(list);
                }
            } else {
                return null;
            }
        } else {
            return value;
        }
    }

    /**
     * Returns an Integer value if this Operand is applicable to the
     * given Player, and <code>null</code> otherwise.
     *
     * @param player a <code>Player</code> value
     * @return an <code>Integer</code> value
     */
    public Integer getValue(Player player) {
        if (value == null) {
            if (scopeLevel == ScopeLevel.PLAYER) {
                List<FreeColObject> list = new LinkedList<FreeColObject>();
                switch(operandType) {
                case UNITS:
                    list.addAll(player.getUnits());
                    break;
                case BUILDINGS:
                    for (Colony colony : player.getColonies()) {
                        list.addAll(colony.getBuildings());
                    }
                    break;
                case SETTLEMENTS:
                    list.addAll(player.getSettlements());
                    break;
                case FOUNDING_FATHERS:
                    list.addAll(player.getFathers());
                    break;
                default:
                    if (getMethodName() != null) {
                        try {
                            Method method = player.getClass().getMethod(getMethodName());
                            if (method != null
                                && (int.class.equals(method.getReturnType())
                                    || Integer.class.equals(method.getReturnType()))) {
                                return (Integer) method.invoke(player);
                            }
                        } catch(Exception e) {
                            logger.warning(e.toString());
                            return null;
                        }
                    }
                    return null;
                }
                return count(list);
            } else {
                return null;
            }
        } else {
            return value;
        }
    }

    /**
     * Returns an Integer value if this Operand is applicable to the
     * given Settlement, and <code>null</code> otherwise. Currently,
     * this only works for Colonies.
     *
     * @param Settlement a <code>Settlement</code> value
     * @return an <code>Integer</code> value
     */
    public Integer getValue(Settlement settlement) {
        if (value == null) {
            if (scopeLevel == ScopeLevel.SETTLEMENT
                && settlement instanceof Colony) {
                Colony colony = (Colony) settlement;
                List<FreeColObject> list = new LinkedList<FreeColObject>();
                switch(operandType) {
                case UNITS:
                    list.addAll(colony.getUnitList());
                    break;
                case BUILDINGS:
                    list.addAll(colony.getBuildings());
                    break;
                default:
                    if (getMethodName() != null) {
                        try {
                            Method method = colony.getClass().getMethod(getMethodName());
                            if (method != null &&
                                Integer.class.isAssignableFrom(method.getReturnType())) {
                                return (Integer) method.invoke(colony);
                            }
                        } catch(Exception e) {
                            logger.warning(e.toString());
                            return null;
                        }
                    }
                    return null;
                }
                return count(list);
            } else {
                // in future, we might expand this to handle native
                // settlements
                return null;
            }
        } else {
            return value;
        }
    }


    /**
     * Describe <code>count</code> method here.
     *
     * @return an <code>int</code> value
     */
    private int count(List<FreeColObject> objects) {
        int result = 0;
        for (FreeColObject object : objects) {
            if (appliesTo(object)) {
                result++;
            }
        }
        return result;
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */    
    public void toXMLImpl(XMLStreamWriter out, String tag) throws XMLStreamException {
        out.writeStartElement(tag);
        writeAttributes(out);
        out.writeEndElement();
    }

    public void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);
        String attribute = in.getAttributeValue(null, "operandType");
        if (attribute != null) {
            operandType = Enum.valueOf(OperandType.class, attribute);
        }
        attribute = in.getAttributeValue(null, "scopeLevel");
        if (attribute != null) {
            scopeLevel = Enum.valueOf(ScopeLevel.class, attribute);
        }
        attribute = in.getAttributeValue(null, "value");
        if (attribute != null) {
            value = new Integer(attribute);
        }
    }
    
    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);
        out.writeAttribute("operandType", operandType.toString());
        out.writeAttribute("scopeLevel", scopeLevel.toString());
        if (value != null) {
            out.writeAttribute("value", value.toString());
        }
    }

    public String toString() {
        if (value == null) {
            return scopeLevel + "'s number of " + operandType + "s";
        } else {
            return Integer.toString(value);
        }
    }


}

