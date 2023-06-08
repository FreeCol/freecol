/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

package net.sf.freecol.client.gui;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.SwingGUI.PopupPosition;
import net.sf.freecol.client.gui.dialog.CaptureGoodsDialog;
import net.sf.freecol.client.gui.dialog.ChooseFoundingFatherDialog;
import net.sf.freecol.client.gui.dialog.ConfirmDeclarationDialog;
import net.sf.freecol.client.gui.dialog.DifficultyDialog;
import net.sf.freecol.client.gui.dialog.DumpCargoDialog;
import net.sf.freecol.client.gui.dialog.EditOptionDialog;
import net.sf.freecol.client.gui.dialog.EditSettlementDialog;
import net.sf.freecol.client.gui.dialog.EmigrationDialog;
import net.sf.freecol.client.gui.dialog.EndTurnDialog;
import net.sf.freecol.client.gui.dialog.FirstContactDialog;
import net.sf.freecol.client.gui.dialog.FreeColChoiceDialog;
import net.sf.freecol.client.gui.dialog.FreeColConfirmDialog;
import net.sf.freecol.client.gui.dialog.FreeColDialog;
import net.sf.freecol.client.gui.dialog.FreeColStringInputDialog;
import net.sf.freecol.client.gui.dialog.GameOptionsDialog;
import net.sf.freecol.client.gui.dialog.LoadDialog;
import net.sf.freecol.client.gui.dialog.LoadingSavegameDialog;
import net.sf.freecol.client.gui.dialog.MapGeneratorOptionsDialog;
import net.sf.freecol.client.gui.dialog.MapSizeDialog;
import net.sf.freecol.client.gui.dialog.MonarchDialog;
import net.sf.freecol.client.gui.dialog.NativeDemandDialog;
import net.sf.freecol.client.gui.dialog.NegotiationDialog;
import net.sf.freecol.client.gui.dialog.Parameters;
import net.sf.freecol.client.gui.dialog.ParametersDialog;
import net.sf.freecol.client.gui.dialog.PreCombatDialog;
import net.sf.freecol.client.gui.dialog.RiverStyleDialog;
import net.sf.freecol.client.gui.dialog.SaveDialog;
import net.sf.freecol.client.gui.dialog.ScaleMapSizeDialog;
import net.sf.freecol.client.gui.dialog.SelectAmountDialog;
import net.sf.freecol.client.gui.dialog.SelectDestinationDialog;
import net.sf.freecol.client.gui.dialog.SelectTributeAmountDialog;
import net.sf.freecol.client.gui.dialog.VictoryDialog;
import net.sf.freecol.client.gui.dialog.WarehouseDialog;
import net.sf.freecol.client.gui.panel.AboutPanel;
import net.sf.freecol.client.gui.panel.BuildQueuePanel;
import net.sf.freecol.client.gui.panel.ChatPanel;
import net.sf.freecol.client.gui.panel.ColorChooserPanel;
import net.sf.freecol.client.gui.panel.DeclarationPanel;
import net.sf.freecol.client.gui.panel.ErrorPanel;
import net.sf.freecol.client.gui.panel.EuropePanel;
import net.sf.freecol.client.gui.panel.EventPanel;
import net.sf.freecol.client.gui.panel.FindSettlementPanel;
import net.sf.freecol.client.gui.panel.FreeColPanel;
import net.sf.freecol.client.gui.panel.IndianSettlementPanel;
import net.sf.freecol.client.gui.panel.InformationPanel;
import net.sf.freecol.client.gui.panel.NewPanel;
import net.sf.freecol.client.gui.panel.PurchasePanel;
import net.sf.freecol.client.gui.panel.RecruitPanel;
import net.sf.freecol.client.gui.panel.ServerListPanel;
import net.sf.freecol.client.gui.panel.StatisticsPanel;
import net.sf.freecol.client.gui.panel.TilePanel;
import net.sf.freecol.client.gui.panel.TradeRouteInputPanel;
import net.sf.freecol.client.gui.panel.TradeRoutePanel;
import net.sf.freecol.client.gui.panel.TrainPanel;
import net.sf.freecol.client.gui.panel.WorkProductionPanel;
import net.sf.freecol.client.gui.panel.colopedia.ColopediaPanel;
import net.sf.freecol.client.gui.panel.report.CompactLabourReport;
import net.sf.freecol.client.gui.panel.report.LabourData.UnitData;
import net.sf.freecol.client.gui.panel.report.ReportCargoPanel;
import net.sf.freecol.client.gui.panel.report.ReportClassicColonyPanel;
import net.sf.freecol.client.gui.panel.report.ReportCompactColonyPanel;
import net.sf.freecol.client.gui.panel.report.ReportContinentalCongressPanel;
import net.sf.freecol.client.gui.panel.report.ReportEducationPanel;
import net.sf.freecol.client.gui.panel.report.ReportExplorationPanel;
import net.sf.freecol.client.gui.panel.report.ReportForeignAffairPanel;
import net.sf.freecol.client.gui.panel.report.ReportHighScoresPanel;
import net.sf.freecol.client.gui.panel.report.ReportHistoryPanel;
import net.sf.freecol.client.gui.panel.report.ReportIndianPanel;
import net.sf.freecol.client.gui.panel.report.ReportLabourDetailPanel;
import net.sf.freecol.client.gui.panel.report.ReportLabourPanel;
import net.sf.freecol.client.gui.panel.report.ReportMilitaryPanel;
import net.sf.freecol.client.gui.panel.report.ReportNavalPanel;
import net.sf.freecol.client.gui.panel.report.ReportProductionPanel;
import net.sf.freecol.client.gui.panel.report.ReportReligiousPanel;
import net.sf.freecol.client.gui.panel.report.ReportRequirementsPanel;
import net.sf.freecol.client.gui.panel.report.ReportTradePanel;
import net.sf.freecol.client.gui.panel.report.ReportTurnPanel;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.metaserver.ServerInfo;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.util.Utils;


/**
 * Container for all the higher level dialogs and panels.
 * Moved here so that Canvas is more manageable.
 */
public final class Widgets {

    private static final Logger logger = Logger.getLogger(Widgets.class.getName());

    /** The game client. */
    private final FreeColClient freeColClient;

    /** The canvas to write to. */
    private final Canvas canvas;


    /** A wrapper class for non-modal dialogs. */
    private class DialogCallback<T> implements Runnable {
        
        /** The dialog to show. */
        private final FreeColDialog<T> fcd;

        /** A rough position for the dialog. */
        private final PopupPosition pos;

        /** The handler for the dialog response. */
        private final DialogHandler<T> handler;


        /**
         * Constructor.
         *
         * @param fcd The parent {@code FreeColDialog}.
         * @param pos A {@code PopupPosition} for the dialog.
         * @param handler The {@code DialogHandler} to call when run.
         */
        public DialogCallback(FreeColDialog<T> fcd, PopupPosition pos,
                              DialogHandler<T> handler) {
            this.fcd = fcd;
            this.pos = pos;
            this.handler = handler;
            SwingUtilities.invokeLater(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            Widgets.this.canvas.viewFreeColDialog(fcd, pos);
            // ...and use another thread to wait for a dialog response...
            new Thread(fcd.toString()) {
                @Override
                public void run() {
                    while (!fcd.responded()) {
                        Utils.delay(500, "Dialog interrupted.");
                    }
                    // ...before handling the result.
                    handler.handle(fcd.getResponse());
                }
            }.start();
        }
    };


    /**
     * Create this wrapper class.
     *
     * @param freeColClient The enclosing {@code FreeColClient}.
     * @param canvas The {@code Canvas} to call out to.
     */
    public Widgets(FreeColClient freeColClient, Canvas canvas) {
        this.freeColClient = freeColClient;
        this.canvas = canvas;
    }


    private FreeColFrame getFrame() {
        return this.canvas.getParentFrame();
    }
    

    // Generic dialogs

    /**
     * Show a modal dialog with a text and a ok/cancel option.
     *
     * @param tmpl A {@code StringTemplate} to explain the choice.
     * @param icon An optional icon to display.
     * @param okKey The text displayed on the "ok"-button.
     * @param cancelKey The text displayed on the "cancel"-button.
     * @param pos A {@code PopupPosition} for the dialog.
     * @return True if the user clicked the "ok"-button.
     */
    public boolean confirm(StringTemplate tmpl, ImageIcon icon,
                           String okKey, String cancelKey, PopupPosition pos) {
        FreeColConfirmDialog dialog
            = new FreeColConfirmDialog(this.freeColClient, getFrame(), true,
                                       tmpl, icon, okKey, cancelKey);
        return this.canvas.showFreeColDialog(dialog, pos);
    }

    /**
     * Show a modal dialog with text and a choice of options.
     *
     * @param <T> The type to be returned from the dialog.
     * @param tmpl A SringTemplate that explains the choice for the user.
     * @param icon An optional icon to display.
     * @param cancelKey Key for the text of the optional cancel button.
     * @param choices The {@code List} containing the ChoiceItems to
     *            create buttons for.
     * @param pos A {@code PopupPosition} for the dialog.
     * @return The corresponding member of the values array to the selected
     *     option, or null if no choices available.
     */
    public <T> T getChoice(StringTemplate tmpl, ImageIcon icon,
                           String cancelKey, List<ChoiceItem<T>> choices,
                           PopupPosition pos) {
        if (choices.isEmpty()) return null;
        FreeColChoiceDialog<T> dialog
            = new FreeColChoiceDialog<>(this.freeColClient, getFrame(), true,
                                        tmpl, icon, cancelKey, choices);
        return this.canvas.showFreeColDialog(dialog, pos);
    }

    /**
     * Show a modal dialog with a text field and a ok/cancel option.
     *
     * @param tmpl A {@code StringTemplate} that explains the
     *     action to the user.
     * @param defaultValue The default value appearing in the text field.
     * @param okKey A key displayed on the "ok"-button.
     * @param cancelKey A key displayed on the optional "cancel"-button.
     * @param pos A {@code PopupPosition} for the dialog.
     * @return The text the user entered, or null if cancelled.
     */
    public String getInput(StringTemplate tmpl, String defaultValue,
                           String okKey, String cancelKey,
                           PopupPosition pos) {
        FreeColStringInputDialog dialog
            = new FreeColStringInputDialog(this.freeColClient, getFrame(), true,
                                           tmpl, defaultValue,
                                           okKey, cancelKey);
        return this.canvas.showFreeColDialog(dialog, pos);
    }


    // Simple front ends to display specific dialogs and panels

    /**
     * Show the AboutPanel.
     *
     * @return The panel shown.
     */
    public FreeColPanel showAboutPanel() {
        AboutPanel panel
            = new AboutPanel(this.freeColClient);
        return this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED,
                                            false);
    }

    /**
     * Show the BuildQueuePanel for a given colony.
     *
     * @param colony The {@code Colony} to show the build queue of.
     * @return The panel shown.
     */
    public FreeColPanel showBuildQueuePanel(Colony colony) {
        BuildQueuePanel panel
            = this.canvas.getExistingFreeColPanel(BuildQueuePanel.class);
        if (panel == null || panel.getColony() != colony) {
            panel = new BuildQueuePanel(this.freeColClient, colony);
            return this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED,
                                                true);
        }
        return panel;
    }

    /**
     * Show the {@code CaptureGoodsDialog}.
     *
     * @param unit The {@code Unit} capturing goods.
     * @param gl The list of {@code Goods} to choose from.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showCaptureGoodsDialog(Unit unit, List<Goods> gl,
                                       DialogHandler<List<Goods>> handler) {
        new DialogCallback<>(new CaptureGoodsDialog(this.freeColClient,
                                                    getFrame(), unit, gl),
                             null, handler);
    }

    /**
     * Show the chat panel.
     *
     * @return The panel shown.
     */
    public FreeColPanel showChatPanel() {
        ChatPanel panel
            = new ChatPanel(this.freeColClient);
        this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, true);
        panel.requestFocus();
        return panel;
    }

    /**
     * Show the {@code ChooseFoundingFatherDialog}.
     *
     * @param ffs The {@code FoundingFather}s to choose from.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showChooseFoundingFatherDialog(List<FoundingFather> ffs,
                                               DialogHandler<FoundingFather> handler) {
        new DialogCallback<>(new ChooseFoundingFatherDialog(this.freeColClient,
                                                            getFrame(), ffs),
                             null, handler);
    }

    /**
     * Show the colopedia entry for a given node.
     *
     * @param nodeId The node identifier to display.
     * @return The panel shown.
     */
    public FreeColPanel showColopediaPanel(String nodeId) {
        ColopediaPanel panel
            = new ColopediaPanel(this.freeColClient, nodeId);
        return this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED,
                                            true);
    }

    /**
     * Show a {@code ColorChooserPanel}.
     *
     * @param al An {@code ActionListener} to handle panel button
     *     presses.
     * @return The panel shown.
     */
    public FreeColPanel showColorChooserPanel(ActionListener al) {
        FreeColPanel panel
            = new ColorChooserPanel(this.freeColClient, al);
        return this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED,
                                            false);
    }

    /**
     * Show the compact labour report.
     *
     * @return The panel shown.
     */
    public FreeColPanel showCompactLabourReport() {
        CompactLabourReport panel
            = new CompactLabourReport(this.freeColClient);
        panel.initialize();
        return this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED,
                                            false);

    }

    /**
     * Show the compact labour report for the specified unit data.
     *
     * @param unitData The {@code UnitData} to display.
     * @return The panel shown.
     */
    public FreeColPanel showCompactLabourReport(UnitData unitData) {
        CompactLabourReport panel
            = new CompactLabourReport(this.freeColClient, unitData);
        panel.initialize();
        return this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED,
                                            false);
    }

    /**
     * Show a dialog to confirm a declaration of independence.
     *
     * @return A list of names for a new nation.
     */
    public List<String> showConfirmDeclarationDialog() {
        ConfirmDeclarationDialog dialog
            = new ConfirmDeclarationDialog(this.freeColClient, getFrame());
        return this.canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Show a panel showing the declaration of independence with
     * animated signature.
     *
     * @param afterClosing A callback that is executed after the panel closes.
     */
    public void showDeclarationPanel(Runnable afterClosing) {
        final DeclarationPanel panel = new DeclarationPanel(this.freeColClient, afterClosing);
        this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, false);
    }

    /**
     * Show the difficulty dialog for a given group.
     *
     * @param spec The enclosing {@code Specification}.
     * @param group The {@code OptionGroup} containing the difficulty.
     * @param editable If the options should be editable.
     * @param dialogHandler Callback executed when the dialog gets closed.
     * @return The resulting {@code OptionGroup}.
     */
    public void showDifficultyDialog(Specification spec,
                                            OptionGroup group,
                                            boolean editable,
                                            DialogHandler<OptionGroup> dialogHandler) {
        final DifficultyDialog dialog = new DifficultyDialog(this.freeColClient, getFrame(), spec, group, editable);
        dialog.setDialogHandler(dialogHandler);
        this.canvas.showFreeColPanel(dialog, PopupPosition.CENTERED, true);
    }

    /**
     * Show the {@code DumpCargoDialog}.
     *
     * @param unit The {@code Unit} that is dumping.
     * @param pos A {@code PopupPosition} for the dialog.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showDumpCargoDialog(Unit unit, PopupPosition pos,
                                    DialogHandler<List<Goods>> handler) {
        new DialogCallback<>(new DumpCargoDialog(this.freeColClient,
                                                 getFrame(), unit),
                             pos, handler);
    }

    /**
     * Show the EditOptionDialog.
     *
     * @param op The {@code Option} to edit.
     * @return The response returned by the dialog.
     */
    public boolean showEditOptionDialog(Option op) {
        if (op == null) return false;
        EditOptionDialog dialog
            = new EditOptionDialog(this.freeColClient, getFrame(), op);
        return this.canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Show the EditSettlementDialog.
     *
     * @param is The {@code IndianSettlement} to edit.
     * @return The response returned by the dialog.
     */
    public IndianSettlement showEditSettlementDialog(IndianSettlement is) {
        EditSettlementDialog dialog
            = new EditSettlementDialog(this.freeColClient, getFrame(), is);
        return this.canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Show the panel that allows the user to choose which unit will
     * emigrate from Europe.
     *
     * @param player The {@code Player} whose unit is emigrating.
     * @param fountainOfYouth Is this dialog displayed as a result of a
     *     fountain of youth.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showEmigrationDialog(Player player, boolean fountainOfYouth,
                                     DialogHandler<Integer> handler) {
        new DialogCallback<>(new EmigrationDialog(this.freeColClient,
                                                  getFrame(),
                                                  player.getEurope(),
                                                  fountainOfYouth),
                             null, handler);
    }

    /**
     * Show the EndTurnDialog with given units that could still move.
     *
     * @param units A list of {@code Unit}s that could still move.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showEndTurnDialog(List<Unit> units,
                                  DialogHandler<Boolean> handler) {
        new DialogCallback<>(new EndTurnDialog(this.freeColClient,
                                               getFrame(), units),
                             null, handler);
    }

    /**
     * Show an error message.
     *
     * @param message The message to display.
     * @return The panel shown.
     */
    public FreeColPanel showErrorPanel(String message) {
        if (message == null) return null;
        ErrorPanel panel
            = new ErrorPanel(this.freeColClient, message);
        return this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED,
                                            true);
    }

    /**
     * Show the {@code EuropePanel}.
     *
     * @param callback An optional closing callback to add.
     * @return The panel shown.
     */
    public FreeColPanel showEuropePanel(Runnable callback) {
        if (this.freeColClient.getGame() == null) return null;
        EuropePanel panel
            = this.canvas.getExistingFreeColPanel(EuropePanel.class);
        if (panel == null) {
            panel = new EuropePanel(this.freeColClient,
                                    (this.canvas.getHeight() > 780));
            if (callback != null) panel.addClosingCallback(callback);
            this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, true);
        }
        return panel;
    }

    /**
     * Show an event panel.
     *
     * @param header The title.
     * @param image A resource key for the image to display.
     * @param footer Optional footer text.
     * @return The panel shown.
     */
    public FreeColPanel showEventPanel(String header, String image,
                                       String footer) {
        EventPanel panel
            = new EventPanel(this.freeColClient, header, image, footer);
        return this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED,
                                            false);
    }

    /**
     * Show the FindSettlementPanel.
     *
     * @return The panel shown.
     */
    public FreeColPanel showFindSettlementPanel() {
        FindSettlementPanel panel
            = new FindSettlementPanel(this.freeColClient);
        return this.canvas.showFreeColPanel(panel, PopupPosition.ORIGIN,
                                            true);
    }

    /**
     * Show the first contact dialog (which is really just a non-modal
     * confirm dialog).
     *
     * @param player The {@code Player} making contact.
     * @param other The {@code Player} to contact.
     * @param tile An optional {@code Tile} on offer.
     * @param settlementCount The number of settlements the other
     *     player has (from the server, other.getNumberOfSettlements()
     *     is wrong here!).
     * @param pos A {@code PopupPosition} for the dialog.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showFirstContactDialog(Player player, Player other,
                                       Tile tile, int settlementCount,
                                       PopupPosition pos,
                                       DialogHandler<Boolean> handler) {
        new DialogCallback<>(new FirstContactDialog(this.freeColClient,
                                                    getFrame(), player, other,
                                                    tile, settlementCount),
                             pos, handler);
    }

    /**
     * Show the GameOptionsDialog.
     *
     * @param editable Should the game options be editable?
     * @param dialogHandler A callback for handling the closing of the dialog.
     */
    public void showGameOptionsDialog(boolean editable, DialogHandler<OptionGroup> dialogHandler) {
        final GameOptionsDialog dialog = new GameOptionsDialog(this.freeColClient, getFrame(), editable);
        dialog.setDialogHandler(dialogHandler);
        this.canvas.showFreeColPanel(dialog, PopupPosition.CENTERED, true);
    }

    /**
     * Show the high scores panel.
     *
     * @param messageId An optional message to add to the high scores panel.
     * @param scores The list of {@code HighScore}s to display.
     * @return The panel shown.
     */
    public FreeColPanel showHighScoresPanel(String messageId,
                                            List<HighScore> scores) {
        ReportHighScoresPanel panel
            = new ReportHighScoresPanel(this.freeColClient, messageId, scores);
        return this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED,
                                            true);
    }

    /**
     * Show the panel of the given native settlement.
     *
     * @param is The {@code IndianSettlement} to display.
     * @param pos A {@code PopupPosition} for the panel.
     * @return The panel shown.
     */
    public FreeColPanel showIndianSettlementPanel(IndianSettlement is,
                                                  PopupPosition pos) {
        IndianSettlementPanel panel
            = new IndianSettlementPanel(this.freeColClient, is);
        return this.canvas.showFreeColPanel(panel, pos, true);
    }

    /**
     * Show a message with some information and an "OK"-button.
     *
     * @param displayObject Optional object for displaying.
     * @param pos A {@code PopupPosition} for the panel.
     * @param icon The icon to display for the object.
     * @param tmpl The {@code StringTemplate} to display.
     * @return The panel shown.
     */
    public FreeColPanel showInformationPanel(FreeColObject displayObject,
                                             PopupPosition pos, ImageIcon icon,
                                             StringTemplate tmpl) {
        InformationPanel panel
            = new InformationPanel(this.freeColClient,
                                   new String[] { Messages.message(tmpl) },
                                   new FreeColObject[] { displayObject },
                                   new ImageIcon[] { icon });
        return this.canvas.showFreeColPanel(panel, pos, false);
    }

    /**
     * Show a dialog where the user may choose a file.
     *
     * @param directory The directory containing the files.
     * @param filters {@code FileFilter}s for suitable files.
     * @return The selected {@code File}.
     */
    public File showLoadDialog(File directory, FileFilter[] filters) {
        LoadDialog dialog
            = new LoadDialog(this.freeColClient, getFrame(), directory,
                             filters);
        return this.canvas.showFreeColDialog(dialog, null);
    }        

    /**
     * Show a dialog for setting options when loading a savegame.
     *
     * The settings can be retrieved directly from
     * {@link LoadingSavegameDialog} after calling this method.
     *
     * @param pubSer Default value.
     * @param single Default value.
     * @return The {@code LoadingSavegameInfo} if the dialog was accepted,
     *     or null otherwise.
     */
    public LoadingSavegameInfo showLoadingSavegameDialog(boolean pubSer,
                                                         boolean single) {
        LoadingSavegameDialog dialog
            = new LoadingSavegameDialog(this.freeColClient, getFrame());
        return (this.canvas.showFreeColDialog(dialog, null)) ? dialog.getInfo()
            : null;
    }

    /**
     * Show a panel containing the log file.
     *
     * @return The panel shown.
     */
    public FreeColPanel showLogFilePanel() {
        ErrorPanel panel
            = new ErrorPanel(this.freeColClient);
        return this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED,
                                            true);
    }

    /**
     * Show the map generator options dialog.
     *
     * @param editable Should these options be editable.
     */
    public void showMapGeneratorOptionsDialog(boolean editable, DialogHandler<OptionGroup> dialogHandler) {
        final MapGeneratorOptionsDialog dialog = new MapGeneratorOptionsDialog(this.freeColClient, getFrame(), editable);
        dialog.setDialogHandler(dialogHandler);
        this.canvas.showFreeColPanel(dialog, PopupPosition.CENTERED, true);
    }

    /**
     * Show the map size dialog.
     * 
     * @return The response returned by the dialog.
     */
    public Dimension showMapSizeDialog() {
        MapSizeDialog dialog
            = new MapSizeDialog(this.freeColClient, getFrame());
        return this.canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Show the monarch dialog.
     *
     * @param action The {@code MonarchAction} underway.
     * @param tmpl The {@code StringTemplate} describing the
     *     situation.
     * @param monarchKey The resource key for the monarch image.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showMonarchDialog(MonarchAction action,
                                  StringTemplate tmpl, String monarchKey,
                                  DialogHandler<Boolean> handler) {
        new DialogCallback<>(new MonarchDialog(this.freeColClient,
                                               getFrame(), action,
                                               tmpl, monarchKey),
                             null, handler);
    }

    /**
     * Show a dialog to set a new name for something.
     *
     * @param tmpl A {@code StringTemplate} for the message
     *     to explain the dialog.
     * @param defaultName The default name.
     * @param pos A {@code PopupPosition} for the dialog.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showNamingDialog(StringTemplate tmpl, String defaultName,
                                 PopupPosition pos,
                                 DialogHandler<String> handler) {
        new DialogCallback<>(new FreeColStringInputDialog(this.freeColClient,
                                                          getFrame(), false,
                                                          tmpl, defaultName,
                                                          "ok", null),
                             pos, handler);
    }

    /**
     * Show a dialog to handle a native demand to a colony.
     *
     * @param unit The demanding {@code Unit}.
     * @param colony The {@code Colony} being demanded of.
     * @param type The {@code GoodsType} demanded (null for gold).
     * @param amount The amount of goods demanded.
     * @param pos A {@code PopupPosition} for the dialog.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showNativeDemandDialog(Unit unit, Colony colony,
                                       GoodsType type, int amount,
                                       PopupPosition pos,
                                       DialogHandler<Boolean> handler) {
        new DialogCallback<>(new NativeDemandDialog(this.freeColClient,
                                                    getFrame(), unit, colony,
                                                    type, amount),
                             pos, handler);
    }

    /**
     * Show the {@code NegotiationDialog}.
     *
     * @param our Our {@code FreeColGameObject} that is negotiating.
     * @param other The other {@code FreeColGameObject}.
     * @param agreement The current {@code DiplomaticTrade} agreement.
     * @param comment An optional {@code StringTemplate} containing a
     *     commentary message.
     * @param pos A {@code PopupPosition} for the dialog.
     * @return An updated agreement.
     */
    public DiplomaticTrade showNegotiationDialog(FreeColGameObject our,
                                                 FreeColGameObject other,
                                                 DiplomaticTrade agreement,
                                                 StringTemplate comment,
                                                 PopupPosition pos) {
        NegotiationDialog dialog
            = new NegotiationDialog(this.freeColClient, getFrame(),
                                    our, other, agreement, comment);
        return this.canvas.showFreeColDialog(dialog, pos);
    }

    /**
     * Show the NewPanel for a given optional specification.
     *
     * @param specification The {@code Specification} to use.
     * @return The panel shown.
     */
    public FreeColPanel showNewPanel(Specification specification) {
        NewPanel panel
            = new NewPanel(this.freeColClient, specification);
        return this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED,
                                            false);
    }

    /**
     * Show the parameters dialog.
     * 
     * @return The response returned by the dialog.
     */
    public Parameters showParametersDialog() {
        ParametersDialog dialog
            = new ParametersDialog(this.freeColClient, getFrame());
        return this.canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Show a dialog to confirm a combat.
     *
     * @param attacker The attacker {@code Unit}.
     * @param defender The defender.
     * @param pos A {@code PopupPosition} for the dialog.
     * @return True if the combat is to proceed.
     */
    public boolean showPreCombatDialog(Unit attacker,
                                       FreeColGameObject defender,
                                       PopupPosition pos) {
        PreCombatDialog dialog
            = new PreCombatDialog(this.freeColClient, getFrame(),
                                  attacker, defender);
        return this.canvas.showFreeColDialog(dialog, pos);
    }

    /**
     * Show the purchase panel.
     *
     * @return The panel shown.
     */
    public FreeColPanel showPurchasePanel() {
        PurchasePanel panel
            = this.canvas.getExistingFreeColPanel(PurchasePanel.class);
        if (panel == null) {
            panel = new PurchasePanel(this.freeColClient);
            panel.update();
            this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, false);
        }
        return panel;
    }

    /**
     * Show the recruit panel.
     *
     * @return The panel shown.
     */
    public FreeColPanel showRecruitPanel() {
        RecruitPanel panel
            = this.canvas.getExistingFreeColPanel(RecruitPanel.class);
        if (panel == null) {
            panel = new RecruitPanel(this.freeColClient);
            this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, false);
        }
        return panel;
    }

    /**
     * Show the labour detail panel.
     *
     * @param unitType The {@code UnitType} to display.
     * @param data The labour data.
     * @param unitCount A map of unit distribution.
     * @param colonies The list of player {@code Colony}s.
     * @return The panel shown.
     */
    public FreeColPanel showReportLabourDetailPanel(UnitType unitType,
        Map<UnitType, Map<Location, Integer>> data,
        TypeCountMap<UnitType> unitCount, List<Colony> colonies) {
        ReportLabourDetailPanel panel
            = new ReportLabourDetailPanel(this.freeColClient, unitType, data,
                                          unitCount, colonies);
        panel.initialize();
        return this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED,
                                            true);
    }

    /**
     * Show the river style dialog.
     *
     * @param styles The river styles a choice is made from.
     * @return The response returned by the dialog.
     */
    public String showRiverStyleDialog(List<String> styles) {
        RiverStyleDialog dialog
            = new RiverStyleDialog(this.freeColClient, getFrame(), styles);
        return this.canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Show a dialog where the user may choose a filename.
     *
     * @param directory The directory containing the files in which
     *     the user may overwrite.
     * @param filters {@code FileFilter}s for acceptable file names.
     * @param defaultName Default filename for the savegame.
     * @return The selected {@code File}.
     */
    public File showSaveDialog(File directory, FileFilter[] filters,
                               String defaultName) {
        SaveDialog dialog
            = new SaveDialog(this.freeColClient, getFrame(),
                             directory, filters, defaultName);
        return this.canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Show the scale map size dialog.
     * 
     * @return The response returned by the dialog.
     */
    public Dimension showScaleMapSizeDialog() {
        ScaleMapSizeDialog dialog
            = new ScaleMapSizeDialog(this.freeColClient, getFrame());
        return this.canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Show the select-amount dialog.
     *
     * @param goodsType The {@code GoodsType} to select an amount of.
     * @param available The amount of goods available.
     * @param defaultAmount The amount to select to start with.
     * @param needToPay If true, check the player has sufficient funds.
     * @return The amount selected.
     */
    public int showSelectAmountDialog(GoodsType goodsType, int available,
                                      int defaultAmount, boolean needToPay) {
        FreeColDialog<Integer> dialog
            = new SelectAmountDialog(this.freeColClient, getFrame(),
                                     goodsType, available,
                                     defaultAmount, needToPay);
        Integer result = this.canvas.showFreeColDialog(dialog, null);
        return (result == null) ? -1 : result;
    }

    /**
     * Show a dialog allowing the user to select a destination for
     * a given unit.
     *
     * @param unit The {@code Unit} to select a destination for.
     * @param pos A {@code PopupPosition} for the dialog.
     * @return A destination for the unit, or null.
     */
    public Location showSelectDestinationDialog(Unit unit, PopupPosition pos) {
        SelectDestinationDialog dialog
            = new SelectDestinationDialog(this.freeColClient, getFrame(),
                                          unit);
        return this.canvas.showFreeColDialog(dialog, pos);
    }

    /**
     * Show the {@code ServerListPanel}.
     *
     * @param serverList The list containing the servers retrieved from the
     *     metaserver.
     * @return The panel shown.
     */
    public FreeColPanel showServerListPanel(List<ServerInfo> serverList) {
        ServerListPanel panel
            = new ServerListPanel(this.freeColClient,
                                  this.freeColClient.getConnectController());
        panel.initialize(serverList);
        return this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED,
                                            true);
    }

    /**
     * Show the select-tribute-amount dialog.
     *
     * @param question a {@code stringtemplate} describing the
     *     amount of tribute to demand.
     * @param maximum The maximum amount available.
     * @return The amount selected.
     */
    public int showSelectTributeAmountDialog(StringTemplate question,
                                             int maximum) {
        FreeColDialog<Integer> dialog
            = new SelectTributeAmountDialog(this.freeColClient, getFrame(),
                                            question, maximum);
        Integer result = this.canvas.showFreeColDialog(dialog, null);
        return (result == null) ? -1 : result;
    }

    /**
     * Show the statistics panel.
     *
     * @param serverStats A map of server statistics key,value pairs.
     * @param clientStats A map of client statistics key,value pairs.
     * @return The panel shown.
     */
    public FreeColPanel showStatisticsPanel(Map<String, String> serverStats,
                                            Map<String, String> clientStats) {
        StatisticsPanel panel
            = new StatisticsPanel(this.freeColClient, serverStats,
                                  clientStats);
        return this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED,
                                            true);
    }

    /**
     * Show the tile panel for a given tile.
     *
     * @param tile The {@code Tile} to display.
     * @return The panel shown.
     */
    public FreeColPanel showTilePanel(Tile tile) {
        if (tile == null || !tile.isExplored()) return null;
        TilePanel panel
            = new TilePanel(this.freeColClient, tile);
        return this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED,
                                            false);
    }

    /**
     * Show the trade route input panel for a given trade route.
     *
     * @param newRoute The {@code TradeRoute} to display.
     * @return The panel shown.
     */
    public FreeColPanel showTradeRouteInputPanel(TradeRoute newRoute) {
        TradeRouteInputPanel panel
            = new TradeRouteInputPanel(this.freeColClient, newRoute);
        return this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED,
                                            true);
    }

    /**
     * Show a panel to select a trade route for a unit.
     *
     * @param unit An optional {@code Unit} to select a trade route for.
     * @param pos A {@code PopupPosition} for the panel.
     * @return The panel shown.
     */
    public FreeColPanel showTradeRoutePanel(Unit unit, PopupPosition pos) {
        TradeRoutePanel panel
            = new TradeRoutePanel(this.freeColClient, unit);
        return this.canvas.showFreeColPanel(panel, pos, true);
    }

    /**
     * Show the training panel.
     *
     * @return The panel shown.
     */
    public FreeColPanel showTrainPanel() {
        TrainPanel panel
            = this.canvas.getExistingFreeColPanel(TrainPanel.class);
        if (panel == null) {
            panel = new TrainPanel(this.freeColClient);
            panel.update();
            this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, false);
        }
        return panel;
    }

    /**
     * Show the victory dialog.
     *
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showVictoryDialog(DialogHandler<Boolean> handler) {
        new DialogCallback<>(new VictoryDialog(this.freeColClient, getFrame()),
                             null, handler);
    }

    /**
     * Show the warehouse dialog for a colony.
     *
     * Run out of ColonyPanel, so the tile is already displayed.
     *
     * @param colony The {@code Colony} to display.
     * @return The response returned by the dialog.
     */
    public void showWarehouseDialog(Colony colony, DialogHandler<Boolean> handler) {
        final WarehouseDialog dialog = new WarehouseDialog(this.freeColClient, colony, handler);
        this.canvas.showFreeColPanel(dialog, PopupPosition.CENTERED, true);
    }

    /**
     * Show the production of a unit.
     *
     * @param unit The {@code Unit} to display.
     * @return The panel shown.
     */
    public FreeColPanel showWorkProductionPanel(Unit unit) {
        WorkProductionPanel panel
            = new WorkProductionPanel(this.freeColClient, unit);
        return this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED,
                                            true);
    }

    
    // Singleton specialist reports

    public FreeColPanel showReportCargoPanel() {
        ReportCargoPanel panel
            = this.canvas.getExistingFreeColPanel(ReportCargoPanel.class);
        if (panel == null) {
            panel = new ReportCargoPanel(this.freeColClient);
            this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, true);
        }
        return panel;
    }

    public FreeColPanel showReportColonyPanel(boolean compact) {
        FreeColPanel panel;
        if (compact) {
            panel = this.canvas.getExistingFreeColPanel(ReportCompactColonyPanel.class);
            if (panel == null) {
                panel = new ReportCompactColonyPanel(this.freeColClient);
                this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED,
                                             true);
            }
        } else {
            panel = this.canvas.getExistingFreeColPanel(ReportClassicColonyPanel.class);
            if (panel == null) {
                panel = new ReportClassicColonyPanel(this.freeColClient);
                this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED,
                                             true);
            }
        }
        return panel;
    }

    public FreeColPanel showReportContinentalCongressPanel() {
        ReportContinentalCongressPanel panel
            = this.canvas.getExistingFreeColPanel(ReportContinentalCongressPanel.class);
        if (panel == null) {
            panel = new ReportContinentalCongressPanel(this.freeColClient);
            this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, true);
        }
        return panel;
    }

    public FreeColPanel showReportEducationPanel() {
        ReportEducationPanel panel
            = this.canvas.getExistingFreeColPanel(ReportEducationPanel.class);
        if (panel == null) {
            panel = new ReportEducationPanel(this.freeColClient);
            this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, true);
        }
        return panel;
    }

    public FreeColPanel showReportExplorationPanel() {
        ReportExplorationPanel panel
            = this.canvas.getExistingFreeColPanel(ReportExplorationPanel.class);
        if (panel == null) {
            panel = new ReportExplorationPanel(this.freeColClient);
            this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, true);
        }
        return panel;
    }

    public FreeColPanel showReportForeignAffairPanel() {
        ReportForeignAffairPanel panel
            = this.canvas.getExistingFreeColPanel(ReportForeignAffairPanel.class);
        if (panel == null) {
            panel = new ReportForeignAffairPanel(this.freeColClient);
            this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, true);
        }
        return panel;
    }

    public FreeColPanel showReportHistoryPanel() {
        ReportHistoryPanel panel
            = this.canvas.getExistingFreeColPanel(ReportHistoryPanel.class);
        if (panel == null) {
            panel = new ReportHistoryPanel(this.freeColClient);
            this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, true);
        }
        return panel;
    }

    public FreeColPanel showReportIndianPanel() {
        ReportIndianPanel panel
            = this.canvas.getExistingFreeColPanel(ReportIndianPanel.class);
        if (panel == null) {
            panel = new ReportIndianPanel(this.freeColClient);
            this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, true);
        }
        return panel;
    }

    public FreeColPanel showReportLabourPanel() {
        ReportLabourPanel panel
            = this.canvas.getExistingFreeColPanel(ReportLabourPanel.class);
        if (panel == null) {
            panel = new ReportLabourPanel(this.freeColClient);
            this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, true);
        }
        return panel;
    }

    public FreeColPanel showReportMilitaryPanel() {
        ReportMilitaryPanel panel
            = this.canvas.getExistingFreeColPanel(ReportMilitaryPanel.class);
        if (panel == null) {
            panel = new ReportMilitaryPanel(this.freeColClient);
            this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, true);
        }
        return panel;
    }

    public FreeColPanel showReportNavalPanel() {
        ReportNavalPanel panel
            = this.canvas.getExistingFreeColPanel(ReportNavalPanel.class);
        if (panel == null) {
            panel = new ReportNavalPanel(this.freeColClient);
            this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, true);
        }
        return panel;
    }

    public FreeColPanel showReportProductionPanel() {
        ReportProductionPanel panel
            = this.canvas.getExistingFreeColPanel(ReportProductionPanel.class);
        if (panel == null) {
            panel = new ReportProductionPanel(this.freeColClient);
            this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, true);
        }
        return panel;
    }

    public FreeColPanel showReportReligiousPanel() {
        ReportReligiousPanel panel
            = this.canvas.getExistingFreeColPanel(ReportReligiousPanel.class);
        if (panel == null) {
            panel = new ReportReligiousPanel(this.freeColClient);
            this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, true);
        }
        return panel;
    }

    public FreeColPanel showReportRequirementsPanel() {
        ReportRequirementsPanel panel
            = this.canvas.getExistingFreeColPanel(ReportRequirementsPanel.class);
        if (panel == null) {
            panel = new ReportRequirementsPanel(this.freeColClient);
            this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, true);
        }
        return panel;
    }

    public FreeColPanel showReportTradePanel() {
        ReportTradePanel panel
            = this.canvas.getExistingFreeColPanel(ReportTradePanel.class);
        if (panel == null) {
            panel = new ReportTradePanel(this.freeColClient);
            this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, true);
        }
        return panel;
    }

    /**
     * Show the turn report.
     *
     * @param messages The {@code ModelMessage}s to show.
     * @return The {@code FreeColPanel} displayed.
     */
    public FreeColPanel showReportTurnPanel(List<ModelMessage> messages) {
        ReportTurnPanel panel
            = this.canvas.getExistingFreeColPanel(ReportTurnPanel.class);
        if (panel == null) {
            panel = new ReportTurnPanel(this.freeColClient, messages);
            this.canvas.showFreeColPanel(panel, PopupPosition.CENTERED, true);
        } else {
            panel.setMessages(messages);
        }
        return panel;
    }
}
