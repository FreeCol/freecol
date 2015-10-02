/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

package net.sf.freecol.common.model.mission;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.Unit;
import static net.sf.freecol.common.util.CollectionUtils.*;

import org.w3c.dom.Element;


/**
 * The ImprovementMission causes a Unit to add a TileImprovement to a
 * particular Tile.
 */
public class ImprovementMission extends AbstractMission {

    /**
     * The improvement of this Mission.
     */
    private TileImprovement improvement;


    /**
     * Creates a new <code>ImprovementMission</code> instance.
     *
     * @param game a <code>Game</code> value
     */
    public ImprovementMission(Game game) {
        super(game);
    }

    /**
     * Creates a new <code>ImprovementMission</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param xr a <code>FreeColXMLReader</code> value
     * @exception XMLStreamException if an error occurs
     */
    public ImprovementMission(Game game,
                              FreeColXMLReader xr) throws XMLStreamException {
        super(game, xr);
    }

    /**
     * Creates a new <code>ImprovementMission</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param e an <code>Element</code> value
     */
    public ImprovementMission(Game game, Element e) {
        super(game, e);
        readFromXMLElement(e);
    }

    /**
     * Creates a new <code>ImprovementMission</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param id The object identifier.
     */
    public ImprovementMission(Game game, String id) {
        super(game, id);
    }

    /**
     * Get the <code>Improvement</code> value.
     *
     * @return an <code>TileImprovement</code> value
     */
    public final TileImprovement getImprovement() {
        return improvement;
    }

    /**
     * Set the <code>Improvement</code> value.
     *
     * @param newImprovement The new Improvement value.
     */
    public final void setImprovement(final TileImprovement newImprovement) {
        this.improvement = newImprovement;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MissionState doMission() {
        // FIXME: get rid of magic numbers: either add a pioneerWork
        // attribute to UnitType, or introduce an expertRole ability
        // and add the work to the Role definition
        int work = getUnit().hasAbility(Ability.EXPERT_PIONEER) ? 2 : 1;
        setTurnCount(getTurnCount() - work);
        getUnit().setMovesLeft(0);
        return (getTurnCount() <= 0)
            ? MissionState.COMPLETED : MissionState.OK;
    }


    /**
     * Returns true if the mission is still valid.
     *
     * @return a <code>boolean</code> value
     */
    @Override
    public boolean isValid() {
        return super.isValid()
            && improvement != null
            && improvement.isWorkerAllowed(getUnit());
    }

    /**
     * Returns true if the given Unit is allowed to build at least one
     * TileImprovementType.
     *
     * @param unit an <code>Unit</code> value
     * @return false
     */
    public static boolean isValidFor(Unit unit) {
        final Specification spec = unit.getGame().getSpecification();
        return any(spec.getTileImprovementTypeList(),
            ti -> ti.isWorkerAllowed(unit));
    }


    // Serialization.

    private static final String IMPROVEMENT_TAG = "improvement";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(IMPROVEMENT_TAG, improvement);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        improvement = xr.makeFreeColGameObject(getGame(), IMPROVEMENT_TAG,
                                               TileImprovement.class, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "improvementMission"
     */
    public static String getXMLElementTagName() {
        return "improvementMission";
    }



}
