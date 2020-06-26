/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.awt.event.ActionListener;
import java.awt.Dimension;
import java.awt.Image;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileFilter;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.panel.AboutPanel;
import net.sf.freecol.client.gui.panel.BuildQueuePanel;
import net.sf.freecol.client.gui.dialog.CaptureGoodsDialog;
import net.sf.freecol.client.gui.panel.ChatPanel;
import net.sf.freecol.client.gui.dialog.ChooseFoundingFatherDialog;
import net.sf.freecol.client.gui.dialog.ClientOptionsDialog;
import net.sf.freecol.client.gui.panel.ColonyPanel;
import net.sf.freecol.client.gui.panel.colopedia.ColopediaPanel;
import net.sf.freecol.client.gui.panel.ColorChooserPanel;
import net.sf.freecol.client.gui.panel.report.CompactLabourReport;
import net.sf.freecol.client.gui.dialog.ConfirmDeclarationDialog;
import net.sf.freecol.client.gui.panel.DeclarationPanel;
import net.sf.freecol.client.gui.dialog.DifficultyDialog;
import net.sf.freecol.client.gui.dialog.DumpCargoDialog;
import net.sf.freecol.client.gui.dialog.EditOptionDialog;
import net.sf.freecol.client.gui.dialog.EditSettlementDialog;
import net.sf.freecol.client.gui.dialog.EmigrationDialog;
import net.sf.freecol.client.gui.dialog.EndTurnDialog;
import net.sf.freecol.client.gui.panel.ErrorPanel;
import net.sf.freecol.client.gui.panel.EuropePanel;
import net.sf.freecol.client.gui.panel.EventPanel;
import net.sf.freecol.client.gui.panel.FindSettlementPanel;
import net.sf.freecol.client.gui.dialog.FirstContactDialog;
import net.sf.freecol.client.gui.dialog.FreeColChoiceDialog;
import net.sf.freecol.client.gui.dialog.FreeColConfirmDialog;
import net.sf.freecol.client.gui.dialog.FreeColDialog;
import net.sf.freecol.client.gui.panel.FreeColPanel;
import net.sf.freecol.client.gui.dialog.FreeColStringInputDialog;
import net.sf.freecol.client.gui.dialog.GameOptionsDialog;
import net.sf.freecol.client.gui.panel.IndianSettlementPanel;
import net.sf.freecol.client.gui.panel.InformationPanel;
import net.sf.freecol.client.gui.panel.report.LabourData.UnitData;
import net.sf.freecol.client.gui.dialog.LoadDialog;
import net.sf.freecol.client.gui.dialog.LoadingSavegameDialog;
import net.sf.freecol.client.gui.panel.MainPanel;
import net.sf.freecol.client.gui.panel.MapEditorTransformPanel;
import net.sf.freecol.client.gui.dialog.MapGeneratorOptionsDialog;
import net.sf.freecol.client.gui.dialog.MapSizeDialog;
import net.sf.freecol.client.gui.dialog.MonarchDialog;
import net.sf.freecol.client.gui.dialog.NativeDemandDialog;
import net.sf.freecol.client.gui.dialog.NegotiationDialog;
import net.sf.freecol.client.gui.panel.NewPanel;
import net.sf.freecol.client.gui.dialog.Parameters;
import net.sf.freecol.client.gui.dialog.ParametersDialog;
import net.sf.freecol.client.gui.dialog.PreCombatDialog;
import net.sf.freecol.client.gui.panel.PurchasePanel;
import net.sf.freecol.client.gui.panel.RecruitPanel;
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
import net.sf.freecol.client.gui.panel.report.ReportPanel;
import net.sf.freecol.client.gui.panel.report.ReportProductionPanel;
import net.sf.freecol.client.gui.panel.report.ReportReligiousPanel;
import net.sf.freecol.client.gui.panel.report.ReportRequirementsPanel;
import net.sf.freecol.client.gui.panel.report.ReportTradePanel;
import net.sf.freecol.client.gui.panel.report.ReportTurnPanel;
import net.sf.freecol.client.gui.dialog.RiverStyleDialog;
import net.sf.freecol.client.gui.dialog.SaveDialog;
import net.sf.freecol.client.gui.dialog.ScaleMapSizeDialog;
import net.sf.freecol.client.gui.dialog.SelectAmountDialog;
import net.sf.freecol.client.gui.dialog.SelectDestinationDialog;
import net.sf.freecol.client.gui.dialog.SelectTributeAmountDialog;
import net.sf.freecol.client.gui.panel.ServerListPanel;
import net.sf.freecol.client.gui.panel.StartGamePanel;
import net.sf.freecol.client.gui.panel.StatisticsPanel;
import net.sf.freecol.client.gui.panel.StatusPanel;
import net.sf.freecol.client.gui.panel.TilePanel;
import net.sf.freecol.client.gui.panel.TradeRouteInputPanel;
import net.sf.freecol.client.gui.panel.TradeRoutePanel;
import net.sf.freecol.client.gui.panel.TrainPanel;
import net.sf.freecol.client.gui.dialog.VictoryDialog;
import net.sf.freecol.client.gui.dialog.WarehouseDialog;
import net.sf.freecol.client.gui.panel.WorkProductionPanel;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDataFile;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
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
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.Introspector;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.common.util.Utils;


/**
 * Container for all the higher level dialogs and panels.
 * Moved here so that Canvas is more manageable.
 */
public final class Widgets {

    private static final Logger logger = Logger.getLogger(Widgets.class.getName());

    private static final List<Class<? extends FreeColPanel>> EUROPE_CLASSES
        = makeUnmodifiableList(RecruitPanel.class,
            PurchasePanel.class,
            TrainPanel.class);

    /** The game client. */
    private final FreeColClient freeColClient;

    /** The image library to create icons etc with. */
    private final ImageLibrary imageLibrary;

    /** The canvas to write to. */
    private final Canvas canvas;


    /** A wrapper class for non-modal dialogs. */
    private class DialogCallback<T> implements Runnable {
        
        /** The dialog to show. */
        private final FreeColDialog<T> fcd;

        /** An optional tile to guide the dialog placement. */
        private final Tile tile;

        /** The handler for the dialog response. */
        private final DialogHandler<T> handler;


        /**
         * Constructor.
         *
         * @param fcd The parent {@code FreeColDialog}.
         * @param tile An optional {@code Tile} to display.
         * @param handler The {@code DialogHandler} to call when run.
         */
        public DialogCallback(FreeColDialog<T> fcd, Tile tile,
                              DialogHandler<T> handler) {
            this.fcd = fcd;
            this.tile = tile;
            this.handler = handler;
        }


        // Implement Runnable

        @Override
        public void run() {
            // Display the dialog...
            canvas.viewFreeColDialog(fcd, tile);
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
     * @param canvas The {@code Canvas} to call out to.
     */
    public Widgets(FreeColClient freeColClient, ImageLibrary imageLibrary,
                   Canvas canvas) {
        this.freeColClient = freeColClient;
        this.imageLibrary = imageLibrary;
        this.canvas = canvas;
    }


    // Simple utilities

    private FreeColFrame getFrame() {
        return this.canvas.getParentFrame();
    }
    
    /**
     * Make image icon from an image.
     * Use only if you know having null is possible!
     *
     * @param image The {@code Image} to create an icon for.
     * @return The {@code ImageIcon}.
     */
    private static ImageIcon createImageIcon(Image image) {
        return (image==null) ? null : new ImageIcon(image);
    }

    /**
     * Make image icon from an object.
     *
     * @param display The FreeColObject to find an icon for.
     * @return The {@code ImageIcon} found.
     */
    private ImageIcon createObjectImageIcon(FreeColObject display) {
        return (display == null) ? null
            : createImageIcon(this.imageLibrary.getObjectImage(display, 2f));
    }

    // Generic dialogs

    /**
     * Displays a modal dialog with text and a choice of options.
     *
     * @param <T> The type to be returned from the dialog.
     * @param tile An optional {@code Tile} to make visible (not
     *     under the dialog!)
     * @param obj An object that explains the choice for the user.
     * @param icon An optional icon to display.
     * @param cancelKey Key for the text of the optional cancel button.
     * @param choices The {@code List} containing the ChoiceItems to
     *            create buttons for.
     * @return The corresponding member of the values array to the selected
     *     option, or null if no choices available.
     */
    public <T> T showChoiceDialog(Tile tile, StringTemplate tmpl,
                                  ImageIcon icon, String cancelKey,
                                  List<ChoiceItem<T>> choices) {
        if (choices.isEmpty()) return null;
        FreeColChoiceDialog<T> dialog
            = new FreeColChoiceDialog<>(freeColClient, getFrame(), true,
                                        tmpl, icon, cancelKey, choices);
        return canvas.showFreeColDialog(dialog, tile);
    }

    /**
     * Displays a modal dialog with a text and a ok/cancel option.
     *
     * @param tile An optional {@code Tile} to make visible (not
     *     under the dialog!)
     * @param tmpl A {@code StringTemplate} to explain the choice.
     * @param icon An optional icon to display.
     * @param okKey The text displayed on the "ok"-button.
     * @param cancelKey The text displayed on the "cancel"-button.
     * @return True if the user clicked the "ok"-button.
     */
    public boolean showConfirmDialog(Tile tile, StringTemplate tmpl,
                                     ImageIcon icon,
                                     String okKey, String cancelKey) {
        FreeColConfirmDialog dialog
            = new FreeColConfirmDialog(freeColClient, getFrame(), true,
                                       tmpl, icon, okKey, cancelKey);
        return canvas.showFreeColDialog(dialog, tile);
    }

    /**
     * Displays a modal dialog with a text field and a ok/cancel option.
     *
     * @param tile An optional tile to make visible (not under the dialog).
     * @param tmpl A {@code StringTemplate} that explains the
     *     action to the user.
     * @param defaultValue The default value appearing in the text field.
     * @param okKey A key displayed on the "ok"-button.
     * @param cancelKey A key displayed on the optional "cancel"-button.
     * @return The text the user entered, or null if cancelled.
     */
    public String showInputDialog(Tile tile, StringTemplate tmpl,
                                  String defaultValue,
                                  String okKey, String cancelKey) {
        FreeColStringInputDialog dialog
            = new FreeColStringInputDialog(freeColClient, getFrame(), true,
                                           tmpl, defaultValue,
                                           okKey, cancelKey);
        return canvas.showFreeColDialog(dialog, tile);
    }


    // Simple front ends to display specific dialogs and panels

    /**
     * Cancel the current trade route in a TradeRouteInputPanel.
     */
    public void cancelTradeRouteInput() {
        TradeRouteInputPanel panel
            = canvas.getExistingFreeColPanel(TradeRouteInputPanel.class);
        if (panel != null) {
            panel.cancelTradeRoute();
        }
    }

    /**
     * Tells that a chat message was received.
     *
     * @param senderName The sender.
     * @param message The chat message.
     * @param privateChat True if this is a private message.
     */
    public void displayStartChat(String senderName, String message,
                                 boolean privateChat) {
        StartGamePanel panel
            = canvas.getExistingFreeColPanel(StartGamePanel.class);
        if (panel != null) {
            panel.displayChat(senderName, message, privateChat);
        }
    }

    /**
     * Is the client options dialog present?
     *
     * @return True if the client options dialog is found.
     */
    public boolean isClientOptionsDialogShowing() {
        return canvas.getExistingFreeColDialog(ClientOptionsDialog.class) != null;
    }

    /**
     * Display the AboutPanel.
     */
    public void showAboutPanel() {
        AboutPanel panel
            = new AboutPanel(freeColClient);
        canvas.showSubPanel(panel, false);
    }

    /**
     * Show the BuildQueuePanel for a given colony.
     *
     * @param colony The {@code Colony} to show the build queue of.
     * @return The {@code BuildQueuePanel}.
     */
    public BuildQueuePanel showBuildQueuePanel(Colony colony) {
        BuildQueuePanel panel
            = canvas.getExistingFreeColPanel(BuildQueuePanel.class);
        if (panel == null || panel.getColony() != colony) {
            panel = new BuildQueuePanel(freeColClient, colony);
            canvas.showSubPanel(panel, true);
        }
        return panel;
    }

    /**
     * Display the {@code CaptureGoodsDialog}.
     *
     * @param unit The {@code Unit} capturing goods.
     * @param gl The list of {@code Goods} to choose from.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showCaptureGoodsDialog(Unit unit, List<Goods> gl,
                                       DialogHandler<List<Goods>> handler) {
        CaptureGoodsDialog dialog
            = new CaptureGoodsDialog(freeColClient, getFrame(),
                                     unit, gl);
        SwingUtilities.invokeLater(
            new DialogCallback<>(dialog, null, handler));
    }

    /**
     * Displays the {@code ChatPanel}.
     */
    public void showChatPanel() {
        ChatPanel panel
            = new ChatPanel(freeColClient);
        canvas.showSubPanel(panel, true);
        panel.requestFocus();
    }

    /**
     * Displays the {@code ChooseFoundingFatherDialog}.
     *
     * @param ffs The {@code FoundingFather}s to choose from.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showChooseFoundingFatherDialog(List<FoundingFather> ffs,
                                               DialogHandler<FoundingFather> handler) {
        ChooseFoundingFatherDialog dialog
            = new ChooseFoundingFatherDialog(freeColClient, getFrame(),
                                             ffs);
        SwingUtilities.invokeLater(
            new DialogCallback<>(dialog, null, handler));
    }

    /**
     * Displays a dialog for setting client options.
     *
     * @return The modified {@code OptionGroup}, or null if not modified.
     */
    public OptionGroup showClientOptionsDialog() {
        ClientOptionsDialog dialog
            = new ClientOptionsDialog(freeColClient, getFrame());
        return canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Displays the colony panel of the given {@code Colony}.
     * Defends against duplicates as this can duplicate messages
     * generated by multiple property change listeners registered
     * against the same colony.
     *
     * @param colony The colony whose panel needs to be displayed.
     * @param unit An optional {@code Unit} to select within the panel.
     * @return The {@code ColonyPanel}.
     */
    public ColonyPanel showColonyPanel(Colony colony, Unit unit) {
        if (colony == null) return null;
        ColonyPanel panel = canvas.getColonyPanel(colony);
        if (panel == null) {
            try {
                panel = new ColonyPanel(freeColClient, colony);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception in ColonyPanel for "
                    + colony.getId(), e);
                return null;
            }
            canvas.showFreeColPanel(panel, colony.getTile(), true);
        } else {
            panel.requestFocus();
        }
        if (unit != null) panel.setSelectedUnit(unit);
        return panel;
    }

    /**
     * Show the colopedia entry for a given node.
     *
     * @param nodeId The node identifier to display.
     */
    public void showColopediaPanel(String nodeId) {
        ColopediaPanel panel
            = new ColopediaPanel(freeColClient, nodeId);
        canvas.showSubPanel(panel, true);
    }

    /**
     * Show a {@code ColorChooserPanel}.
     *
     * @param al An {@code ActionListener} to handle panel button
     *     presses.
     * @return The {@code ColorChooserPanel} created.
     */
    public ColorChooserPanel showColorChooserPanel(ActionListener al) {
        ColorChooserPanel panel
            = new ColorChooserPanel(freeColClient, al);
        canvas.showFreeColPanel(panel, null, false);
        return panel;
    }

    /**
     * Show the compact labour report.
     */
    public void showCompactLabourReport() {
        CompactLabourReport panel
            = new CompactLabourReport(freeColClient);
        panel.initialize();
        canvas.showSubPanel(panel, false);

    }

    /**
     * Show the compact labour report for the specified unit data.
     *
     * @param unitData The {@code UnitData} to display.
     */
    public void showCompactLabourReport(UnitData unitData) {
        CompactLabourReport panel
            = new CompactLabourReport(freeColClient, unitData);
        panel.initialize();
        canvas.showSubPanel(panel, false);
    }

    /**
     * Display a dialog to confirm a declaration of independence.
     *
     * @return A list of names for a new nation.
     */
    public List<String> showConfirmDeclarationDialog() {
        ConfirmDeclarationDialog dialog
            = new ConfirmDeclarationDialog(freeColClient, getFrame());
        return canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Display a panel showing the declaration of independence with
     * animated signature.
     */
    public void showDeclarationPanel() {
        DeclarationPanel panel
            = new DeclarationPanel(freeColClient);
        canvas.showSubPanel(panel, Canvas.PopupPosition.CENTERED, false);
    }

    /**
     * Display the difficulty dialog for a given group.
     *
     * @param spec The enclosing {@code Specification}.
     * @param group The {@code OptionGroup} containing the difficulty.
     * @param editable If the options should be editable.
     * @return The resulting {@code OptionGroup}.
     */
    public OptionGroup showDifficultyDialog(Specification spec,
                                            OptionGroup group, boolean editable) {
        DifficultyDialog dialog
            = new DifficultyDialog(freeColClient, getFrame(),
                                   spec, group, editable);
        OptionGroup ret = canvas.showFreeColDialog(dialog, null);
        if (ret != null) FreeCol.setDifficulty(ret);
        return ret;
    }

    /**
     * Displays the {@code DumpCargoDialog}.
     *
     * @param unit The {@code Unit} that is dumping.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showDumpCargoDialog(Unit unit, DialogHandler<List<Goods>> handler) {
        DumpCargoDialog dialog
            = new DumpCargoDialog(freeColClient, getFrame(), unit);
        SwingUtilities.invokeLater(
            new DialogCallback<>(dialog, unit.getTile(), handler));
    }

    /**
     * Display the EditOptionDialog.
     *
     * @param op The {@code Option} to edit.
     * @return The response returned by the dialog.
     */
    public boolean showEditOptionDialog(Option op) {
        if (op == null) return false;
        EditOptionDialog dialog
            = new EditOptionDialog(freeColClient, getFrame(), op);
        return canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Display the EditSettlementDialog.
     *
     * @param is The {@code IndianSettlement} to edit.
     * @return The response returned by the dialog.
     */
    public IndianSettlement showEditSettlementDialog(IndianSettlement is) {
        EditSettlementDialog dialog
            = new EditSettlementDialog(freeColClient, getFrame(), is);
        return canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Shows the panel that allows the user to choose which unit will emigrate
     * from Europe.
     *
     * @param player The {@code Player} whose unit is emigrating.
     * @param fountainOfYouth Is this dialog displayed as a result of a
     *     fountain of youth.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showEmigrationDialog(Player player, boolean fountainOfYouth,
                                     DialogHandler<Integer> handler) {
        EmigrationDialog dialog
            = new EmigrationDialog(freeColClient, getFrame(),
                                   player.getEurope(), fountainOfYouth);
        SwingUtilities.invokeLater(
            new DialogCallback<>(dialog, null, handler));
    }

    /**
     * Display the EndTurnDialog with given units that could still move.
     *
     * @param units A list of {@code Unit}s that could still move.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showEndTurnDialog(List<Unit> units,
                                  DialogHandler<Boolean> handler) {
        EndTurnDialog dialog
            = new EndTurnDialog(freeColClient, getFrame(),
                                units);
        SwingUtilities.invokeLater(
            new DialogCallback<>(dialog, null, handler));
    }

    /**
     * Displays an error message.
     *
     * @param message The message to display.
     * @param callback Optional routine to run when the error panel is closed.
     */
    public void showErrorMessage(String message, Runnable callback) {
        if (message != null) {
            ErrorPanel errorPanel = new ErrorPanel(freeColClient, message);
            if (callback != null) errorPanel.addClosingCallback(callback);
            canvas.showSubPanel(errorPanel, true);
        }
    }

    /**
     * Displays the {@code EuropePanel}.
     *
     * @see EuropePanel
     */
    public void showEuropePanel() {
        if (freeColClient.getGame() == null) return;
        EuropePanel panel
            = canvas.getExistingFreeColPanel(EuropePanel.class);
        if (panel == null) {
            panel = new EuropePanel(freeColClient, (canvas.getHeight() > 780));
            panel.addClosingCallback(() -> {
                    for (Class<? extends FreeColPanel> c: EUROPE_CLASSES) {
                        FreeColPanel p = canvas.getExistingFreeColPanel(c);
                        if (p != null) canvas.remove(p);
                    }
                });
            canvas.showSubPanel(panel, true);
        }
    }

    /**
     * Display an event panel.
     *
     * @param header The title.
     * @param image A resource key for the image to display.
     * @param footer Optional footer text.
     */
    public void showEventPanel(String header, String image, String footer) {
        EventPanel panel
            = new EventPanel(freeColClient, header, image, footer);
        canvas.showSubPanel(panel, Canvas.PopupPosition.CENTERED, false);
    }

    /**
     * Display the FindSettlementPanel.
     */
    public void showFindSettlementPanel() {
        FindSettlementPanel panel
            = new FindSettlementPanel(freeColClient);
        canvas.showSubPanel(panel, Canvas.PopupPosition.ORIGIN, true);
    }

    /**
     * Display the first contact dialog (which is really just a
     * non-modal confirm dialog).
     *
     * @param player The {@code Player} making contact.
     * @param other The {@code Player} to contact.
     * @param tile An optional {@code Tile} on offer.
     * @param settlementCount The number of settlements the other
     *     player has (from the server, other.getNumberOfSettlements()
     *     is wrong here!).
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showFirstContactDialog(Player player, Player other,
                                       Tile tile, int settlementCount,
                                       DialogHandler<Boolean> handler) {
        FirstContactDialog dialog
            = new FirstContactDialog(freeColClient, getFrame(),
                                     player, other, tile, settlementCount);
        SwingUtilities.invokeLater(
            new DialogCallback<>(dialog, tile, handler));
    }

    /**
     * Display the GameOptionsDialog.
     *
     * @param editable Should the game options be editable?
     * @return The {@code OptionGroup} selected.
     */
    public OptionGroup showGameOptionsDialog(boolean editable) {
        GameOptionsDialog dialog
            = new GameOptionsDialog(freeColClient, getFrame(), editable);
        return canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Displays the high scores panel.
     *
     * @param messageId An optional message to add to the high scores panel.
     * @param scores The list of {@code HighScore}s to display.
     */
    public void showHighScoresPanel(String messageId, List<HighScore> scores) {
        ReportHighScoresPanel panel
            = new ReportHighScoresPanel(freeColClient, messageId, scores);
        canvas.showSubPanel(panel, Canvas.PopupPosition.CENTERED, true);
    }

    /**
     * Displays the panel of the given native settlement.
     *
     * @param is The {@code IndianSettlement} to display.
     */
    public void showIndianSettlementPanel(IndianSettlement is) {
        IndianSettlementPanel panel
            = new IndianSettlementPanel(freeColClient, is);
        canvas.showFreeColPanel(panel, is.getTile(), true);
    }

    /**
     * Shows a message with some information and an "OK"-button.
     *
     * @param displayObject Optional object for displaying an icon.
     * @param tmpl The {@code StringTemplate} to display.
     * @return The {@code InformationPanel} that is displayed.
     */
    public InformationPanel showInformationPanel(FreeColObject displayObject,
                                                 StringTemplate tmpl) {
        ImageIcon icon = null;
        Tile tile = null;
        if (displayObject != null) {
            icon = createObjectImageIcon(displayObject);
            tile = (displayObject instanceof Location)
                ? ((Location)displayObject).getTile()
                : null;
        }
        return showInformationPanel(displayObject, tile, icon, tmpl);
    }

    /**
     * Shows a message with some information and an "OK"-button.
     *
     * @param displayObject Optional object for displaying.
     * @param tile The Tile the object is at.
     * @param icon The icon to display for the object.
     * @param tmpl The {@code StringTemplate} to display.
     * @return The {@code InformationPanel} that is displayed.
     */
    public InformationPanel showInformationPanel(FreeColObject displayObject,
                                                 Tile tile, ImageIcon icon,
                                                 StringTemplate tmpl) {
        String text = Messages.message(tmpl);
        InformationPanel panel = new InformationPanel(freeColClient, text, 
                                                      displayObject, icon);
        canvas.showFreeColPanel(panel, tile, true);
        return panel;
    }

    /**
     * Displays a dialog where the user may choose a file.
     *
     * @param directory The directory containing the files.
     * @param extension An extension to select with.
     * @return The selected {@code File}.
     */
    public File showLoadDialog(File directory, String extension) {
        FileFilter[] filters = new FileFilter[] {
            FreeColDataFile.getFileFilter(extension)
        };
        File file = null;
        LoadDialog dialog;
        for (;;) {
            dialog = new LoadDialog(freeColClient, getFrame(),
                                    directory, filters);
            file = canvas.showFreeColDialog(dialog, null);
            if (file == null || file.isFile()) break;
            showErrorMessage(Messages.message(FreeCol.badFile("error.noSuchFile", file)), null);
        }
        return file;
    }

    /**
     * Displays a dialog for setting options when loading a savegame.  The
     * settings can be retrieved directly from {@link LoadingSavegameDialog}
     * after calling this method.
     *
     * @param pubSer Default value.
     * @param single Default value.
     * @return The {@code LoadingSavegameInfo} if the dialog was accepted,
     *     or null otherwise.
     */
    public LoadingSavegameInfo showLoadingSavegameDialog(boolean pubSer,
                                                         boolean single) {
        LoadingSavegameDialog dialog
            = new LoadingSavegameDialog(freeColClient, getFrame());
        return (canvas.showFreeColDialog(dialog, null)) ? dialog.getInfo()
            : null;
    }

    /**
     * Show a panel containing the log file.
     */
    public void showLogFilePanel() {
        canvas.showSubPanel(new ErrorPanel(freeColClient), true);

    }

    /**
     * Display the map generator options dialog.
     *
     * @param editable Should these options be editable.
     * @return The {@code OptionGroup} as edited.
     */
    public OptionGroup showMapGeneratorOptionsDialog(boolean editable) {
        MapGeneratorOptionsDialog dialog
            = new MapGeneratorOptionsDialog(freeColClient, getFrame(),
                                            editable);
        return canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Display the map size dialog.
     * 
     * @return The response returned by the dialog.
     */
    public Dimension showMapSizeDialog() {
        MapSizeDialog dialog = new MapSizeDialog(freeColClient, getFrame());
        return canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Displays a number of ModelMessages.
     *
     * @param messages The {@code ModelMessage}s to display.
     */
    public void showModelMessages(List<ModelMessage> messages) {
        if (messages.isEmpty()) return;
        final Game game = freeColClient.getGame();
        int n = messages.size();
        String[] texts = new String[n];
        FreeColObject[] fcos = new FreeColObject[n];
        ImageIcon[] icons = new ImageIcon[n];
        Tile tile = null;
        for (int i = 0; i < n; i++) {
            ModelMessage m = messages.get(i);
            texts[i] = Messages.message(m);
            fcos[i] = game.getMessageSource(m);
            icons[i] = createObjectImageIcon(game.getMessageDisplay(m));
            if (tile == null && fcos[i] instanceof Location) {
                tile = ((Location)fcos[i]).getTile();
            }
        }

        InformationPanel panel
            = new InformationPanel(freeColClient, texts, fcos, icons);
        canvas.showFreeColPanel(panel, tile, true);
    }

    /**
     * Display the monarch dialog.
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
        MonarchDialog dialog
            = new MonarchDialog(freeColClient, getFrame(),
                                action, tmpl, monarchKey);
        SwingUtilities.invokeLater(
            new DialogCallback<>(dialog, null, handler));
    }

    /**
     * Display a dialog to set a new name for something.
     *
     * @param tmpl A {@code StringTemplate} for the message
     *     to explain the dialog.
     * @param defaultName The default name.
     * @param unit The {@code Unit} discovering it.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showNamingDialog(StringTemplate tmpl, String defaultName,
                                 Unit unit, DialogHandler<String> handler) {
        FreeColStringInputDialog dialog
            = new FreeColStringInputDialog(freeColClient, getFrame(), false,
                                           tmpl, defaultName, "ok", null);
        SwingUtilities.invokeLater(
            new DialogCallback<>(dialog, unit.getTile(), handler));
    }

    /**
     * Display a dialog to handle a native demand to a colony.
     *
     * @param unit The demanding {@code Unit}.
     * @param colony The {@code Colony} being demanded of.
     * @param type The {@code GoodsType} demanded (null for gold).
     * @param amount The amount of goods demanded.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showNativeDemandDialog(Unit unit, Colony colony,
                                       GoodsType type, int amount,
                                       DialogHandler<Boolean> handler) {
        NativeDemandDialog dialog
            = new NativeDemandDialog(freeColClient, getFrame(),
                                     unit, colony, type, amount);
        SwingUtilities.invokeLater(
            new DialogCallback<>(dialog, unit.getTile(), handler));
    }

    /**
     * Displays the {@code NegotiationDialog}.
     *
     * @param our Our {@code FreeColGameObject} that is negotiating.
     * @param other The other {@code FreeColGameObject}.
     * @param agreement The current {@code DiplomaticTrade} agreement.
     * @param comment An optional {@code StringTemplate} containing a
     *     commentary message.
     * @return An updated agreement.
     */
    public DiplomaticTrade showNegotiationDialog(FreeColGameObject our,
                                                 FreeColGameObject other,
                                                 DiplomaticTrade agreement,
                                                 StringTemplate comment) {
        if ((!(our instanceof Unit) && !(our instanceof Colony))
            || (!(other instanceof Unit) && !(other instanceof Colony))
            || (our instanceof Colony && other instanceof Colony)) {
            throw new RuntimeException("Bad DTD args: " + our + ", " + other);
        }
        NegotiationDialog dtd
            = new NegotiationDialog(freeColClient, getFrame(),
                                    our, other, agreement, comment);
        return canvas.showFreeColDialog(dtd, ((Location)our).getTile());
    }

    /**
     * Display the NewPanel for a given optional specification.
     *
     * @param specification The {@code Specification} to use.
     */
    public void showNewPanel(Specification specification) {
        NewPanel panel
            = new NewPanel(freeColClient, specification);
        canvas.showSubPanel(panel, false);
    }

    /**
     * Display the parameters dialog.
     * 
     * @return The response returned by the dialog.
     */
    public Parameters showParametersDialog() {
        ParametersDialog dialog
            = new ParametersDialog(freeColClient, getFrame());
        return canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Display a dialog to confirm a combat.
     *
     * @param attacker The attacker {@code Unit}.
     * @param defender The defender.
     * @param tile A {@code Tile} to make visible.
     * @return True if the combat is to proceed.
     */
    public boolean showPreCombatDialog(Unit attacker,
                                       FreeColGameObject defender,
                                       Tile tile) {
        PreCombatDialog dialog
            = new PreCombatDialog(freeColClient, getFrame(),
                                  attacker, defender);
        return canvas.showFreeColDialog(dialog, tile);
    }

    /**
     * Displays the purchase panel.
     */
    public void showPurchasePanel() {
        PurchasePanel panel
            = canvas.getExistingFreeColPanel(PurchasePanel.class);
        if (panel == null) {
            panel = new PurchasePanel(freeColClient);
            panel.update();
            canvas.showFreeColPanel(panel, null, false);
        }
    }

    /**
     * Displays the recruit panel.
     */
    public void showRecruitPanel() {
        RecruitPanel panel
            = canvas.getExistingFreeColPanel(RecruitPanel.class);
        if (panel == null) {
            panel = new RecruitPanel(freeColClient);
            canvas.showFreeColPanel(panel, null, false);
        }
    }

    /**
     * Display the labour detail panel.
     *
     * @param unitType The {@code UnitType} to display.
     * @param data The labour data.
     * @param unitCount A map of unit distribution.
     * @param colonies The list of player {@code Colony}s.
     */
    public void showReportLabourDetailPanel(UnitType unitType,
        Map<UnitType, Map<Location, Integer>> data,
        TypeCountMap<UnitType> unitCount, List<Colony> colonies) {
        ReportLabourDetailPanel panel
            = new ReportLabourDetailPanel(freeColClient, unitType, data,
                                          unitCount, colonies);
        panel.initialize();
        canvas.showSubPanel(panel, true);
    }

    /**
     * Display the river style dialog.
     *
     * @param styles The river styles a choice is made from.
     * @return The response returned by the dialog.
     */
    public String showRiverStyleDialog(List<String> styles) {
        RiverStyleDialog dialog
            = new RiverStyleDialog(freeColClient, getFrame(), styles);
        return canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Displays a dialog where the user may choose a filename.
     *
     * @param directory The directory containing the files in which
     *     the user may overwrite.
     * @param defaultName Default filename for the savegame.
     * @return The selected {@code File}.
     */
    public File showSaveDialog(File directory, String defaultName) {
        String extension = lastPart(defaultName, ".");
        FileFilter[] filters = new FileFilter[] {
            FreeColDataFile.getFileFilter(extension)
        };
        SaveDialog dialog
            = new SaveDialog(freeColClient, getFrame(),
                             directory, filters, defaultName);
        return canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Display the scale map size dialog.
     * 
     * @return The response returned by the dialog.
     */
    public Dimension showScaleMapSizeDialog() {
        ScaleMapSizeDialog dialog
            = new ScaleMapSizeDialog(freeColClient, getFrame());
        return canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Display the select-amount dialog.
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
            = new SelectAmountDialog(freeColClient, getFrame(),
                                     goodsType, available,
                                     defaultAmount, needToPay);
        Integer result = canvas.showFreeColDialog(dialog, null);
        return (result == null) ? -1 : result;
    }

    /**
     * Display a dialog allowing the user to select a destination for
     * a given unit.
     *
     * @param unit The {@code Unit} to select a destination for.
     * @return A destination for the unit, or null.
     */
    public Location showSelectDestinationDialog(Unit unit) {
        SelectDestinationDialog dialog
            = new SelectDestinationDialog(freeColClient, getFrame(),
                                          unit);
        return canvas.showFreeColDialog(dialog, unit.getTile());
    }

    /**
     * Display the select-tribute-amount dialog.
     *
     * @param question a {@code stringtemplate} describing the
     *     amount of tribute to demand.
     * @param maximum The maximum amount available.
     * @return The amount selected.
     */
    public int showSelectTributeAmountDialog(StringTemplate question,
                                             int maximum) {
        FreeColDialog<Integer> dialog
            = new SelectTributeAmountDialog(freeColClient, getFrame(),
                                            question, maximum);
        Integer result = canvas.showFreeColDialog(dialog, null);
        return (result == null) ? -1 : result;
    }

    /**
     * Display the statistics panel.
     *
     * @param serverStats A map of server statistics key,value pairs.
     * @param clientStats A map of client statistics key,value pairs.
     */
    public void showStatisticsPanel(Map<String, String> serverStats,
                                    Map<String, String> clientStats) {
        StatisticsPanel panel
            = new StatisticsPanel(freeColClient, serverStats, clientStats);
        canvas.showSubPanel(panel, true);
    }

    /**
     * Display the tile panel for a given tile.
     *
     * @param tile The {@code Tile} to display.
     */
    public void showTilePanel(Tile tile) {
        if (tile == null || !tile.isExplored()) return;
        TilePanel panel = new TilePanel(freeColClient, tile);
        canvas.showSubPanel(panel, false);
    }

    /**
     * Display the trade route input panel for a given trade route.
     *
     * @param newRoute The {@code TradeRoute} to display.
     * @return The {@code TradeRouteInputPanel}.
     */
    public TradeRouteInputPanel showTradeRouteInputPanel(TradeRoute newRoute) {
        TradeRouteInputPanel panel
            = new TradeRouteInputPanel(freeColClient, newRoute);
        canvas.showSubPanel(panel, null, true);
        return panel;
    }

    /**
     * Display a panel to select a trade route for a unit.
     *
     * @param unit An optional {@code Unit} to select a trade route for.
     */
    public void showTradeRoutePanel(Unit unit) {
        TradeRoutePanel panel = new TradeRoutePanel(freeColClient, unit);
        canvas.showFreeColPanel(panel, (unit == null) ? null : unit.getTile(), true);
    }

    /**
     * Displays the training panel.
     */
    public void showTrainPanel() {
        TrainPanel panel
            = canvas.getExistingFreeColPanel(TrainPanel.class);
        if (panel == null) {
            panel = new TrainPanel(freeColClient);
            panel.update();
            canvas.showFreeColPanel(panel, null, false);
        }
    }

    /**
     * Display the victory dialog.
     *
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showVictoryDialog(DialogHandler<Boolean> handler) {
        VictoryDialog dialog
            = new VictoryDialog(freeColClient, getFrame());
        SwingUtilities.invokeLater(
            new DialogCallback<>(dialog, null, handler));
    }

    /**
     * Display the warehouse dialog for a colony.
     *
     * Run out of ColonyPanel, so the tile is already displayed.
     *
     * @param colony The {@code Colony} to display.
     * @return The response returned by the dialog.
     */
    public boolean showWarehouseDialog(Colony colony) {
        WarehouseDialog dialog
            = new WarehouseDialog(freeColClient, getFrame(),
                                  colony);
        return canvas.showFreeColDialog(dialog, null);
    }

    /**
     * Display the production of a unit.
     *
     * @param unit The {@code Unit} to display.
     */
    public void showWorkProductionPanel(Unit unit) {
        WorkProductionPanel panel
            = new WorkProductionPanel(freeColClient, unit);
        canvas.showSubPanel(panel, true);
    }

    /**
     * Update all panels derived from the EuropePanel.
     */
    public void updateEuropeanSubpanels() {
        for (Class<? extends FreeColPanel> c: EUROPE_CLASSES) {
            FreeColPanel p = canvas.getExistingFreeColPanel(c);
            if (p != null) {
                // TODO: remember how to write generic code, avoid introspection
                try {
                    Introspector.invokeVoidMethod(p, "update");
                } catch (Exception e) {
                    ; // "can not happen"
                }
            }
        }
    }
    
    // Singleton specialist reports

    public void showReportCargoPanel() {
        ReportCargoPanel panel
            = canvas.getExistingFreeColPanel(ReportCargoPanel.class);
        if (panel == null) {
            panel = new ReportCargoPanel(freeColClient);
            canvas.showSubPanel(panel, true);
        }
    }

    public void showReportColonyPanel() {
        boolean compact;
        try {
            compact = freeColClient.getClientOptions()
                .getInteger(ClientOptions.COLONY_REPORT)
                == ClientOptions.COLONY_REPORT_COMPACT;
        } catch (Exception e) {
            compact = false;
        }
        ReportPanel panel = (compact)
            ? canvas.getExistingFreeColPanel(ReportCompactColonyPanel.class)
            : canvas.getExistingFreeColPanel(ReportClassicColonyPanel.class);
        if (panel == null) {
            panel = (compact)
                ? new ReportCompactColonyPanel(freeColClient)
                : new ReportClassicColonyPanel(freeColClient);
            canvas.showSubPanel(panel, true);
        }
    }

    public void showReportContinentalCongressPanel() {
        ReportContinentalCongressPanel panel
            = canvas.getExistingFreeColPanel(ReportContinentalCongressPanel.class);
        if (panel == null) {
            panel = new ReportContinentalCongressPanel(freeColClient);
            canvas.showSubPanel(panel, true);
        }
    }

    public void showReportEducationPanel() {
        ReportEducationPanel panel
            = canvas.getExistingFreeColPanel(ReportEducationPanel.class);
        if (panel == null) {
            panel = new ReportEducationPanel(freeColClient);
            canvas.showSubPanel(panel, true);
        }
    }

    public void showReportExplorationPanel() {
        ReportExplorationPanel panel
            = canvas.getExistingFreeColPanel(ReportExplorationPanel.class);
        if (panel == null) {
            panel = new ReportExplorationPanel(freeColClient);
            canvas.showSubPanel(panel, true);
        }
    }

    public void showReportForeignAffairPanel() {
        ReportForeignAffairPanel panel
            = canvas.getExistingFreeColPanel(ReportForeignAffairPanel.class);
        if (panel == null) {
            panel = new ReportForeignAffairPanel(freeColClient);
            canvas.showSubPanel(panel, true);
        }
    }

    public void showReportHistoryPanel() {
        ReportHistoryPanel panel
            = canvas.getExistingFreeColPanel(ReportHistoryPanel.class);
        if (panel == null) {
            panel = new ReportHistoryPanel(freeColClient);
            canvas.showSubPanel(panel, true);
        }
    }

    public void showReportIndianPanel() {
        ReportIndianPanel panel
            = canvas.getExistingFreeColPanel(ReportIndianPanel.class);
        if (panel == null) {
            panel = new ReportIndianPanel(freeColClient);
            canvas.showSubPanel(panel, true);
        }
    }

    public void showReportLabourPanel() {
        ReportLabourPanel panel
            = canvas.getExistingFreeColPanel(ReportLabourPanel.class);
        if (panel == null) {
            panel = new ReportLabourPanel(freeColClient);
            canvas.showSubPanel(panel, true);
        }
    }

    public void showReportMilitaryPanel() {
        ReportMilitaryPanel panel
            = canvas.getExistingFreeColPanel(ReportMilitaryPanel.class);
        if (panel == null) {
            panel = new ReportMilitaryPanel(freeColClient);
            canvas.showSubPanel(panel, true);
        }
    }

    public void showReportNavalPanel() {
        ReportNavalPanel panel
            = canvas.getExistingFreeColPanel(ReportNavalPanel.class);
        if (panel == null) {
            panel = new ReportNavalPanel(freeColClient);
            canvas.showSubPanel(panel, true);
        }
    }

    public void showReportProductionPanel() {
        ReportProductionPanel panel
            = canvas.getExistingFreeColPanel(ReportProductionPanel.class);
        if (panel == null) {
            panel = new ReportProductionPanel(freeColClient);
            canvas.showSubPanel(panel, true);
        }
    }

    public void showReportReligiousPanel() {
        ReportReligiousPanel panel
            = canvas.getExistingFreeColPanel(ReportReligiousPanel.class);
        if (panel == null) {
            panel = new ReportReligiousPanel(freeColClient);
            canvas.showSubPanel(panel, true);
        }
    }

    public void showReportRequirementsPanel() {
        ReportRequirementsPanel panel
            = canvas.getExistingFreeColPanel(ReportRequirementsPanel.class);
        if (panel == null) {
            panel = new ReportRequirementsPanel(freeColClient);
            canvas.showSubPanel(panel, true);
        }
    }

    public void showReportTradePanel() {
        ReportTradePanel panel
            = canvas.getExistingFreeColPanel(ReportTradePanel.class);
        if (panel == null) {
            panel = new ReportTradePanel(freeColClient);
            canvas.showSubPanel(panel, true);
        }
    }

    /**
     * Show the turn report.
     *
     * @param messages The {@code ModelMessage}s to show.
     */
    public void showReportTurnPanel(List<ModelMessage> messages) {
        ReportTurnPanel panel
            = canvas.getExistingFreeColPanel(ReportTurnPanel.class);
        if (panel == null) {
            panel = new ReportTurnPanel(freeColClient, messages);
            canvas.showSubPanel(panel, true);
        } else {
            panel.setMessages(messages);
        }
    }
}
