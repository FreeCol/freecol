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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Player.Stance;

import org.w3c.dom.Element;


/**
 * A summary of an enemy nation.
 */
public class NationSummary extends FreeColObject {

    /**
     * The number of settlements this player has.
     */
    private String numberOfSettlements;

    /**
     * The number of units this (European) player has.
     */
    private String numberOfUnits;

    /**
     * The military strength of this (European) player.
     */
    private String militaryStrength;

    /**
     * The naval strength of this (European) player.
     */
    private String navalStrength;

    /**
     * The stance of the player toward the requesting player.
     */
    private String stance;

    /**
     * The gold this (European) player has.
     */
    private String gold;

    /**
     * The (European) player SoL.
     */
    private String soL;

    /**
     * The number of founding fathers this (European) player has.
     */
    private String foundingFathers;

    /**
     * The tax rate of this (European) player.
     */
    private String tax;


    /**
     * Creates a nation summary for the specified player.
     *
     * @param player The <code>Player</code> the player to create the
     *     summary of.
     * @param requester The <code>Player</code> making the request.
     */
    public NationSummary(Player player, Player requester) {
        setId("");
        numberOfSettlements = Integer.toString(player.getSettlements().size());
        Stance sta = player.getStance(requester);
        stance = ((sta == Stance.UNCONTACTED) ? Stance.PEACE
                  : sta).toString();

        if (player.isEuropean()) {
            CombatModel cm = player.getGame().getCombatModel();
            int nUnits = 0, sMilitary = 0, sNaval = 0;
            for (Unit unit : player.getUnits()) {
                nUnits++;
                if (unit.isNaval()) {
                    sNaval += cm.getOffencePower(unit, null);
                } else {
                    sMilitary += cm.getOffencePower(unit, null);
                }
            }
            numberOfUnits = Integer.toString(nUnits);
            militaryStrength = Integer.toString(sMilitary);
            navalStrength = Integer.toString(sNaval);
            gold = Integer.toString(player.getGold());
            if (player == requester || requester
                .hasAbility("model.ability.betterForeignAffairsReport")) {
                soL = Integer.toString(player.getSoL());
                foundingFathers = Integer.toString(player.getFatherCount());
                tax = String.valueOf(player.getTax());
            } else {
                soL = null;
                foundingFathers = null;
                tax = null;
            }
        } else {
            numberOfUnits = militaryStrength = navalStrength = gold = "-1";
            soL = foundingFathers = tax = null;
        }
    }

    /**
     * Creates a new <code>NationSummary</code> instance.
     *
     * @param element An <code>Element</code> value.
     */
    public NationSummary(Element element) {
        readFromXMLElement(element);
    }


    // Trivial accessors
    public String getNumberOfSettlements() {
        return numberOfSettlements;
    }

    public String getNumberOfUnits() {
        return numberOfUnits;
    }

    public String getMilitaryStrength() {
        return militaryStrength;
    }

    public String getNavalStrength() {
        return navalStrength;
    }

    public Stance getStance() {
        return Enum.valueOf(Stance.class, stance);
    }

    public int getGold() {
        return Integer.parseInt(gold);
    }

    public String getFoundingFathers() {
        return foundingFathers;
    }

    public String getSoL() {
        return soL;
    }

    public String getTax() {
        return tax;
    }


    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *     to the stream.
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute("numberOfSettlements", numberOfSettlements);
        out.writeAttribute("numberOfUnits", numberOfUnits);
        out.writeAttribute("militaryStrength", militaryStrength);
        out.writeAttribute("navalStrength", navalStrength);
        out.writeAttribute("stance", stance);
        out.writeAttribute("gold", gold);
        if (soL != null) {
            out.writeAttribute("SoL", soL);
        }
        if (foundingFathers != null) {
            out.writeAttribute("foundingFathers", foundingFathers);
        }
        if (tax != null) {
            out.writeAttribute("tax", tax);
        }
    }

    /**
     * Reads the attributes of this object from an XML stream.
     *
     * @param in The XML input stream.
     * @throws XMLStreamException if a problem was encountered
     *     during parsing.
     */
    @Override
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        numberOfSettlements = getAttribute(in, "numberOfSettlements", "");
        numberOfUnits = getAttribute(in, "numberOfUnits", "");
        militaryStrength = getAttribute(in, "militaryStrength", "");
        navalStrength = getAttribute(in, "navalStrength", "");
        stance = getAttribute(in, "stance", "");
        gold = getAttribute(in, "gold", "");
        soL = in.getAttributeValue(null, "SoL");
        foundingFathers = in.getAttributeValue(null, "foundingFathers");
        tax = in.getAttributeValue(null, "tax");
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "nationSummary"
     */
    public static String getXMLElementTagName() {
        return "nationSummary";
    }
}
