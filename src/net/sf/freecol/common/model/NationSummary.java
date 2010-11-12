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

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit;

import org.w3c.dom.Element;


public class NationSummary extends FreeColObject {

    private String playerId;

    private String numberOfColonies;

    private String numberOfUnits;

    private String militaryStrength;

    private String navalStrength;

    private String stance;

    private String gold;

    private String soL;

    private String foundingFathers;

    private String tax;


    /**
     * Creates a nation summary for the specified player.
     *
     * @param player The <code>Player</code> the player to create the
     *     summary of.
     * @param full Create full summary or not.
     * @param requester The <code>Player</code> making the request.
     */
    public NationSummary(Player player, boolean full, Player requester) {
        setId("");
        CombatModel cm = player.getGame().getCombatModel();
        playerId = player.getId();
        numberOfColonies = Integer.toString(player.getSettlements().size());
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
        Stance sta = player.getStance(requester);
        stance = ((sta == Stance.UNCONTACTED) ? Stance.PEACE : sta).toString();
        gold = Integer.toString(player.getGold());
        if (full) {
            soL = Integer.toString(player.getSoL());
            foundingFathers = Integer.toString(player.getFatherCount());
            tax = String.valueOf(player.getTax());
        } else {
            soL = null;
            foundingFathers = null;
            tax = null;
        }
    }

    /**
     * Creates a new <code>NationSummary</code> instance.
     *
     * @param element an <code>Element</code> value
     */
    public NationSummary(Element element) {
        readFromXMLElement(element);
    }


    // Trivial accessors

    public Player getPlayer(Game game) {
        FreeColGameObject fcgo = game.getFreeColGameObject(playerId);
        return (fcgo instanceof Player) ? (Player) fcgo : null;
    }

    public String getNumberOfColonies() {
        return numberOfColonies;
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

    public void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);
        out.writeAttribute("player", playerId);
        out.writeAttribute("numberOfColonies", numberOfColonies);
        out.writeAttribute("numberOfUnits", numberOfUnits);
        out.writeAttribute("militaryStrength", militaryStrength);
        out.writeAttribute("navalStrength", navalStrength);
        out.writeAttribute("stance", stance);
        out.writeAttribute("gold", gold);
        if (soL != null) out.writeAttribute("SoL", soL);
        if (foundingFathers != null) out.writeAttribute("foundingFathers", foundingFathers);
        if (tax != null) out.writeAttribute("tax", tax);
    }

    public void readAttributes(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, ID_ATTRIBUTE_TAG));
        playerId = getAttribute(in, "player", "");
        numberOfColonies = getAttribute(in, "numberOfColonies", "");
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
