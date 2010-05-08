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

package net.sf.freecol.client.gui.action;

import java.util.Iterator;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.action.DisplayTileTextAction.DisplayText;
import net.sf.freecol.client.gui.panel.ColopediaPanel.PanelType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;

/**
 * Stores the actions.
 */
public class ActionManager extends OptionGroup {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ActionManager.class.getName());

    private FreeColClient freeColClient;

    /**
     * Creates a new <code>ActionManager</code>.
     * 
     * @param freeColClient The main client controller.
     */
    public ActionManager(FreeColClient freeColClient) {
        super("actionManager");

        this.freeColClient = freeColClient;

        freeColClient.getClientOptions().add(this);
        freeColClient.getClientOptions().addToMap(this);
    }


    public void initializeActions() {
        removeAll();
        // keep this list alphabetized.
        
        add(new AboutAction(freeColClient));
        add(new AssignTradeRouteAction(freeColClient));
        add(new BuildColonyAction(freeColClient));
        add(new CenterAction(freeColClient));
        add(new ChangeAction(freeColClient));
        add(new ChangeWindowedModeAction(freeColClient));
        add(new ChatAction(freeColClient));
        add(new ClearOrdersAction(freeColClient));
        for (PanelType type : PanelType.values()) {
            add(new ColopediaAction(freeColClient, type));
        }
        add(new DebugAction(freeColClient));
        add(new DeclareIndependenceAction(freeColClient));
        add(new DetermineHighSeasAction(freeColClient));
        add(new DisbandUnitAction(freeColClient));
        add(new DisplayBordersAction(freeColClient));
        add(new DisplayGridAction(freeColClient));
        for (DisplayText type : DisplayText.values()) {
            add(new DisplayTileTextAction(freeColClient, type));
        }
        add(new EndTurnAction(freeColClient));
        add(new EuropeAction(freeColClient));
        add(new ExecuteGotoOrdersAction(freeColClient));
        add(new FindSettlementAction(freeColClient));
        add(new FortifyAction(freeColClient));
        add(new GotoAction(freeColClient));
        add(new GotoTileAction(freeColClient));
        // Initialize ImprovementActions
        for (ImprovementActionType ia : FreeCol.getSpecification().getImprovementActionTypeList()) {
            add(new ImprovementAction(freeColClient, ia));
        }
        add(new LoadAction(freeColClient));
        add(new MapControlsAction(freeColClient));
        add(new MiniMapZoomInAction(freeColClient));
        add(new MiniMapZoomOutAction(freeColClient));
        for (Direction d : Direction.values()) {
            add(new MoveAction(freeColClient, d));
        }
        add(new NewAction(freeColClient));
        add(new NewEmptyMapAction(freeColClient));
        add(new OpenAction(freeColClient));
        add(new PreferencesAction(freeColClient));
        add(new SaveAndQuitAction(freeColClient));
        add(new QuitAction(freeColClient));
        add(new ReconnectAction(freeColClient));
        add(new RenameAction(freeColClient));
        add(new ReportCargoAction(freeColClient));
        add(new ReportContinentalCongressAction(freeColClient));
        add(new ReportColonyAction(freeColClient));
        add(new ReportExplorationAction(freeColClient));
        add(new ReportForeignAction(freeColClient));
        add(new ReportHighScoresAction(freeColClient));
        add(new ReportHistoryAction(freeColClient));
        add(new ReportIndianAction(freeColClient));
        add(new ReportLabourAction(freeColClient));
        add(new ReportMilitaryAction(freeColClient));
        add(new ReportNavalAction(freeColClient));
        add(new ReportProductionAction(freeColClient));
        add(new ReportReligionAction(freeColClient));
        add(new ReportRequirementsAction(freeColClient));
        add(new ReportTradeAction(freeColClient));
        add(new ReportTurnAction(freeColClient));
        add(new RetireAction(freeColClient));
        add(new SaveAction(freeColClient));
        add(new ScaleMapAction(freeColClient));
        add(new SentryAction(freeColClient));
        add(new ShowDifficultyAction(freeColClient));
        add(new ShowGameOptionsAction(freeColClient));
        add(new ShowMainAction(freeColClient));
        add(new ShowMapGeneratorOptionsAction(freeColClient));
        add(new SkipUnitAction(freeColClient));
        add(new TilePopupAction(freeColClient));
        add(new ToggleViewModeAction(freeColClient));
        add(new TradeRouteAction(freeColClient));
        add(new UnloadAction(freeColClient));
        add(new WaitAction(freeColClient));
        add(new ZoomInAction(freeColClient));
        add(new ZoomOutAction(freeColClient));
    }

    /**
     * Adds the given <code>FreeColAction</code>.
     * 
     * @param freeColAction The <code>FreeColAction</code> that should be
     *            added to this <code>ActionManager</code>.
     */
    public void add(FreeColAction freeColAction) {
        super.add(freeColAction);
    }

    /**
     * Gets the <code>FreeColAction</code> specified by the given
     * <code>id</code>.
     * 
     * @param id The string identifying the action.
     * @return The <code>FreeColAction</code>.
     */
    public FreeColAction getFreeColAction(String id) {
        Iterator<Option> it = iterator();
        while (it.hasNext()) {
            FreeColAction fa = (FreeColAction) it.next();
            if (fa.getId().equals(id)) {
                return fa;
            }
        }

        return null;
    }

    /**
     * Updates every <code>FreeColAction</code> this object keeps.
     * 
     * @see FreeColAction
     */
    public void update() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("update() should only be called from the " + 
                    "event dispatcher thread.");
        }
        Iterator<Option> it = iterator();
        while (it.hasNext()) {
            FreeColAction fa = (FreeColAction) it.next();
            fa.update();
        }
    }
}
