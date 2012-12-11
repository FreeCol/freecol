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

package net.sf.freecol.client.gui.action;

import java.util.Iterator;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.client.gui.action.DisplayTileTextAction.DisplayText;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;

/**
 * Stores all <code>FreeColActions</code> and retrieves them by ID.
 */
public class ActionManager extends OptionGroup {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ActionManager.class.getName());

    private FreeColClient freeColClient;

    private GUI gui;

    /**
     * Creates a new <code>ActionManager</code>.
     *
     * @param freeColClient The main client controller.
     */
    public ActionManager(FreeColClient freeColClient, GUI gui) {
        super("actionManager");
        this.freeColClient = freeColClient;
        this.gui = gui;
    }

    /**
     * This method adds all FreeColActions to the OptionGroup. If you
     * implement a new <code>FreeColAction</code>, then you need to
     * add it in this method.  Localization and a possible accelerator
     * need to be added to the strings file.
     */
    public void initializeActions(InGameController inGameController, ConnectController connectController) {
        // keep this list alphabetized.

        add(new AboutAction(freeColClient, gui));
        add(new AssignTradeRouteAction(freeColClient, inGameController, gui));
        add(new BuildColonyAction(freeColClient, inGameController, gui));
        add(new CenterAction(freeColClient, gui));
        add(new ChangeAction(freeColClient, gui));
        add(new ChangeWindowedModeAction(freeColClient, gui));
        add(new ChatAction(freeColClient, gui));
        add(new ClearOrdersAction(freeColClient, inGameController, gui));
        for (PanelType panelType : PanelType.values()) {
            add(new ColopediaAction(freeColClient, gui, panelType));
        }
        add(new DebugAction(freeColClient, inGameController, connectController, gui));
        add(new DeclareIndependenceAction(freeColClient, inGameController, gui));
        add(new DetermineHighSeasAction(freeColClient, gui));
        add(new DisbandUnitAction(freeColClient, inGameController, gui));
        add(new DisplayBordersAction(freeColClient, gui));
        add(new DisplayGridAction(freeColClient, gui));
        for (DisplayText type : DisplayText.values()) {
            add(new DisplayTileTextAction(freeColClient, gui, type));
        }
        add(new EndTurnAction(freeColClient, inGameController, gui));
        add(new EuropeAction(freeColClient, gui));
        add(new ExecuteGotoOrdersAction(freeColClient, inGameController, gui));
        add(new FindSettlementAction(freeColClient, gui));
        add(new FortifyAction(freeColClient, inGameController, gui));
        add(new GotoAction(freeColClient, inGameController, gui));
        add(new GotoTileAction(freeColClient, gui));
        add(new LoadAction(freeColClient, inGameController, gui));
        add(new MapControlsAction(freeColClient, gui));
        add(new MapEditorAction(freeColClient, gui));
        add(new MiniMapZoomInAction(freeColClient, gui));
        add(new MiniMapZoomInAction(freeColClient, gui, true));
        add(new MiniMapZoomOutAction(freeColClient, gui));
        add(new MiniMapZoomOutAction(freeColClient, gui, true));
        for (Direction d : Direction.values()) {
            add(new MoveAction(freeColClient, inGameController, gui, d));
            add(new MoveAction(freeColClient, inGameController, gui, d, true));
        }
        add(new NewAction(freeColClient, gui));
        add(new ContinueAction(freeColClient, inGameController, connectController, gui));
        add(new NewEmptyMapAction(freeColClient, gui));
        add(new OpenAction(freeColClient, inGameController, gui));
        add(new PreferencesAction(freeColClient, gui));
        add(new SaveAndQuitAction(freeColClient, inGameController, gui));
        add(new QuitAction(freeColClient, gui));
        add(new ReconnectAction(freeColClient, connectController, gui));
        add(new RenameAction(freeColClient, inGameController, gui));
        add(new ReportCargoAction(freeColClient, gui));
        add(new ReportContinentalCongressAction(freeColClient, gui));
        add(new ReportColonyAction(freeColClient, gui));
        add(new ReportEducationAction(freeColClient, gui));
        add(new ReportExplorationAction(freeColClient, gui));
        add(new ReportForeignAction(freeColClient, gui));
        add(new ReportHighScoresAction(freeColClient, gui));
        add(new ReportHistoryAction(freeColClient, gui));
        add(new ReportIndianAction(freeColClient, gui));
        add(new ReportLabourAction(freeColClient, gui));
        add(new ReportMilitaryAction(freeColClient, gui));
        add(new ReportNavalAction(freeColClient, gui));
        add(new ReportProductionAction(freeColClient, gui));
        add(new ReportReligionAction(freeColClient, gui));
        add(new ReportRequirementsAction(freeColClient, gui));
        add(new ReportTradeAction(freeColClient, gui));
        add(new ReportTurnAction(freeColClient, inGameController, gui));
        add(new RetireAction(freeColClient, gui));
        add(new SaveAction(freeColClient, inGameController, gui));
        add(new ScaleMapAction(freeColClient, gui));
        add(new SentryAction(freeColClient, inGameController, gui));
        add(new ShowDifficultyAction(freeColClient, gui));
        add(new ShowGameOptionsAction(freeColClient, gui));
        add(new ShowMainAction(freeColClient, connectController, gui));
        add(new ShowMapGeneratorOptionsAction(freeColClient, gui));
        add(new SkipUnitAction(freeColClient, inGameController, gui));
        add(new TilePopupAction(freeColClient, gui));
        add(new ToggleViewModeAction(freeColClient, gui));
        add(new TradeRouteAction(freeColClient, gui));
        add(new UnloadAction(freeColClient, inGameController, gui));
        add(new WaitAction(freeColClient, inGameController, gui));
        add(new ZoomInAction(freeColClient, gui));
        add(new ZoomOutAction(freeColClient, gui));
    }

    /**
     * Adds the <code>FreeColActions</code> that are provided by the
     * <code>Specification</code>. At the moment, this includes only
     * <code>TileImprovements</code>.
     *
     * @param specification a <code>Specification</code> value
     */
    public void addSpecificationActions(Specification specification) {
        // Initialize ImprovementActions
        for (TileImprovementType type : specification.getTileImprovementTypeList()) {
            if (!type.isNatural()) {
                add(new ImprovementAction(freeColClient, freeColClient.getInGameController(), gui, type));
            }
        }
    }

    /**
     * Gets the <code>FreeColAction</code> specified by the given
     * <code>id</code>.
     *
     * @param id The string identifying the action.
     * @return The <code>FreeColAction</code>.
     */
    public FreeColAction getFreeColAction(String id) {
        return (FreeColAction) super.getOption(id);
    }

    /**
     * Updates every <code>FreeColAction</code> this object keeps.
     *
     * @see FreeColAction
     */
    public void update() {
        Iterator<Option> it = iterator();
        while (it.hasNext()) {
            ((FreeColAction) it.next()).update();
        }
    }
}
