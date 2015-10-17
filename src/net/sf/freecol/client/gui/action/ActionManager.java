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
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.action;

import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.client.gui.action.DisplayTileTextAction.DisplayText;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;


/**
 * Stores all <code>FreeColActions</code> and retrieves them by identifier.
 */
public class ActionManager extends OptionGroup {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ActionManager.class.getName());

    private final FreeColClient freeColClient;


    /**
     * Creates a new <code>ActionManager</code>.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ActionManager(FreeColClient freeColClient) {
        super("actionManager");

        this.freeColClient = freeColClient;
    }


    /**
     * This method adds all FreeColActions to the OptionGroup. If you
     * implement a new <code>FreeColAction</code>, then you need to
     * add it in this method.  Localization and a possible accelerator
     * need to be added to the strings file.
     *
     * @param inGameController The client <code>InGameController</code>.
     * @param connectController The client <code>ConnectController</code>.
     */
    public void initializeActions(InGameController inGameController,
                                  ConnectController connectController) {
        /**
         * Possible FIXME: should we put some of these, especially the
         * move and tile improvement actions, into OptionGroups of
         * their own? This would simplify the MapControls slightly.
         */

        // keep this list alphabetized.
        add(new AboutAction(freeColClient));
        add(new AssignTradeRouteAction(freeColClient));
        add(new BuildColonyAction(freeColClient));
        add(new CenterAction(freeColClient));
        add(new ChangeAction(freeColClient));
        add(new ChangeWindowedModeAction(freeColClient));
        add(new ChatAction(freeColClient));
        add(new ClearOrdersAction(freeColClient));
        for (PanelType panelType : PanelType.values()) {
            add(new ColopediaAction(freeColClient, panelType));
        }
        add(new ContinueAction(freeColClient));
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
        add(new LoadAction(freeColClient));
        add(new MapControlsAction(freeColClient));
        add(new MapEditorAction(freeColClient));
        add(new MiniMapToggleViewAction(freeColClient));
        add(new MiniMapToggleViewAction(freeColClient, true));
        add(new MiniMapToggleFogOfWarAction(freeColClient));
        add(new MiniMapToggleFogOfWarAction(freeColClient, true));
        add(new MiniMapZoomInAction(freeColClient));
        add(new MiniMapZoomInAction(freeColClient, true));
        add(new MiniMapZoomOutAction(freeColClient));
        add(new MiniMapZoomOutAction(freeColClient, true));
        for (Direction d : Direction.values()) {
            add(new MoveAction(freeColClient, d));
            add(new MoveAction(freeColClient, d, true));
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
        add(new ReportEducationAction(freeColClient));
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
     * Adds the <code>FreeColActions</code> that are provided by the
     * <code>Specification</code>.  At the moment, this includes only
     * <code>TileImprovements</code>.
     *
     * @param spec The <code>Specification</code> to refer to.
     */
    public void addSpecificationActions(Specification spec) {
        // Initialize ImprovementActions
        for (TileImprovementType type : spec.getTileImprovementTypeList()) {
            if (!type.isNatural()) {
                add(new ImprovementAction(freeColClient, type));
            }
        }
        update();
    }

    /**
     * Gets the <code>FreeColAction</code> specified by the given
     * identifier.
     *
     * @param id The object identifier.
     * @return The <code>FreeColAction</code>.
     */
    public FreeColAction getFreeColAction(String id) {
        return (FreeColAction)super.getOption(id);
    }

    /**
     * Updates every <code>FreeColAction</code> this object keeps.
     *
     * @see FreeColAction
     */
    public void update() {
        for (Option o : getOptions()) ((FreeColAction)o).update();
    }
}
