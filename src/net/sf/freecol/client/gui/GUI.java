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


package net.sf.freecol.client.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas.BoycottAction;
import net.sf.freecol.client.gui.Canvas.BuyAction;
import net.sf.freecol.client.gui.Canvas.ClaimAction;
import net.sf.freecol.client.gui.Canvas.EventType;
import net.sf.freecol.client.gui.Canvas.MissionaryAction;
import net.sf.freecol.client.gui.Canvas.ScoutColonyAction;
import net.sf.freecol.client.gui.Canvas.ScoutIndianSettlementAction;
import net.sf.freecol.client.gui.Canvas.SellAction;
import net.sf.freecol.client.gui.Canvas.TradeAction;
import net.sf.freecol.client.gui.animation.Animations;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.menu.FreeColMenuBar;
import net.sf.freecol.client.gui.menu.InGameMenuBar;
import net.sf.freecol.client.gui.menu.MapEditorMenuBar;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.client.gui.panel.ColonyPanel;
import net.sf.freecol.client.gui.panel.CornerMapControls;
import net.sf.freecol.client.gui.panel.EuropePanel;
import net.sf.freecol.client.gui.panel.MiniMap;
import net.sf.freecol.client.gui.panel.LabourData.UnitData;
import net.sf.freecol.client.gui.panel.LoadingSavegameDialog;
import net.sf.freecol.client.gui.panel.MapControls;
import net.sf.freecol.client.gui.panel.MapSize;
import net.sf.freecol.client.gui.panel.Parameters;
import net.sf.freecol.client.gui.sound.SoundPlayer;
import net.sf.freecol.common.ServerInfo;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.LanguageOption;
import net.sf.freecol.common.option.LanguageOption.Language;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.PercentageOption;
import net.sf.freecol.common.resources.ResourceManager;

public class GUI {


    private static final Logger logger = Logger.getLogger(GUI.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2002-2012   The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";


    /**
     * The space not being used in windowed mode.
     */
    private static final int DEFAULT_WINDOW_SPACE = 50;

    

    public static final int MOVE_UNITS_MODE = 0;

    public static final int VIEW_TERRAIN_MODE = 1;


    private FreeColClient freeColClient;


    private GraphicsDevice gd;

    private FreeColFrame frame;

    private Canvas canvas;

    private MapViewer mapViewer;
    
    private MapControls mapControls;

    /**
     * This is the MapViewer instance used to paint the colony tiles in the
     * ColonyPanel and other panels. It should not be scaled along
     * with the default MapViewer.
     */
    private MapViewer colonyTileGUI;

    private ImageLibrary imageLibrary;

    private SoundPlayer soundPlayer;

    private boolean windowed;

    private Rectangle windowBounds;

    private JWindow splash;

    public GUI(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
        this.imageLibrary = new ImageLibrary();
    }

    public void activateGotoPath() {
        Unit unit = getActiveUnit();

        // Action should be disabled if there is no active unit, but make sure
        if (unit == null)
            return;

        // Enter "goto mode" if not already activated; otherwise cancel it
        if (mapViewer.isGotoStarted()) {
            mapViewer.stopGoto();
        } else {
            mapViewer.startGoto();

            // Draw the path to the current mouse position, if the
            // mouse is over the screen; see also
            // CanvaseMouseMotionListener
            Point pt = canvas.getMousePosition();
            if (pt != null) {
                Tile tile = mapViewer.convertToMapTile(pt.x, pt.y);
                if (tile != null && unit.getTile() != tile) {
                    PathNode dragPath = unit.findPath(tile);
                    mapViewer.setGotoPath(dragPath);
                }
            }
        }

    }

    /**
     * Verifies if the client can play sounds.
     * @return boolean <b>true</b> if and only if client sound player has an instance
     */
    public boolean canPlaySound() {
        return soundPlayer != null;
    }

    public void centerActiveUnit() {
        mapViewer.centerActiveUnit();
    }

    /**
     * Change the windowed mode.
     * @param windowed Use <code>true</code> for windowed mode
     *      and <code>false</code> for fullscreen mode.
     */
    public void changeWindowedMode(boolean windowed) {
        JMenuBar menuBar = null;
        if (frame != null) {
            menuBar = frame.getJMenuBar();
            if (frame instanceof WindowedFrame) {
                this.windowBounds = frame.getBounds();
            }
            frame.setVisible(false);
            frame.dispose();
        }
        setWindowed(windowed);
        
        this.frame = FreeColFrame.createFreeColFrame(freeColClient, canvas, gd, windowed);
        
        frame.setJMenuBar(menuBar);
        frame.setCanvas(canvas);
        frame.updateBounds(getWindowBounds());

        mapViewer.forceReposition();
        canvas.updateSizes();
        frame.setVisible(true);
    }

    public void closeMainPanel() {
        canvas.closeMainPanel();
    }

    public void closeMenus() {
        canvas.closeMenus();
    }

    public void closeStatusPanel() {
        canvas.closeStatusPanel();
    }

    public boolean containsInGameComponents() {
        return canvas.containsInGameComponents();
    }

    public Dimension determineWindowSize() {
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getMaximumWindowBounds();
        Dimension size = new Dimension(bounds.width - DEFAULT_WINDOW_SPACE,
                                       bounds.height - DEFAULT_WINDOW_SPACE);
        return size;
    }

    public void displayChat(String senderNme, String message, boolean privateChat) {
        canvas.displayChat(senderNme, message, privateChat);

    }


    /**
     * Tells the map controls that a chat message was received.
     *
     * @param player The player who sent the chat message.
     * @param message The chat message.
     * @param privateChat 'true' if the message is a private one, 'false'
     *            otherwise.
     * @see GUIMessage
     */
    public void displayChatMessage(Player player, String message,
                                   boolean privateChat) {
        mapViewer.addMessage(new GUIMessage(player.getName() + ": " + message,
                imageLibrary.getColor(player)));

        canvas.repaint(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    public void displaySpashScreen(final String splashFilename) {
        splash = null;
        if (splashFilename != null) {
            try {
                Image im = Toolkit.getDefaultToolkit()
                    .getImage(splashFilename);
                splash = new JWindow();
                splash.getContentPane().add(new JLabel(new ImageIcon(im)));
                splash.pack();
                Point center = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getCenterPoint();
                splash.setLocation(center.x - splash.getWidth() / 2,
                                   center.y - splash.getHeight() / 2);
                splash.setVisible(true);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Splash fail", e);
                splash = null;
            }
        }
    }

    public void errorMessage(String messageId) {
        canvas.errorMessage(messageId);
    }

    public void errorMessage(String messageID, String message) {
        canvas.errorMessage(messageID, message);

    }

    public void executeWithUnitOutForAnimation(final Unit unit,
            final Tile sourceTile,
            final OutForAnimationCallback r) {
        mapViewer.executeWithUnitOutForAnimation(unit, sourceTile, r);
    }

    public Unit getActiveUnit() {
        if (mapViewer == null)
            return null;
        return mapViewer.getActiveUnit();
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public MapViewer getColonyTileGUI() {
        return colonyTileGUI;
    }

    public int getCurrentViewMode() {
        return mapViewer.getView();
    }

    public Tile getFocus() {
        if (mapViewer == null)
            return null;
        return mapViewer.getFocus();
    }


    public ImageIcon getImageIcon(Object display, boolean small) {
        return imageLibrary.getImageIcon(display, small);
    }

    public ImageLibrary getImageLibrary() {
        return imageLibrary;
    }

    public LoadingSavegameDialog getLoadingSavegameDialog() {
        return canvas.getLoadingSavegameDialog();
    }


    public float getMapScale() {
        return mapViewer.getMapScale();
    }

    public MapViewer getMapViewer() {
        return mapViewer;
    }

    public Tile getSelectedTile() {
        return mapViewer.getSelectedTile();
    }

    public SoundPlayer getSoundPlayer() {
        return soundPlayer;
    }

    public Rectangle getTileBounds(Tile tile) {
        return mapViewer.getTileBounds(tile);
    }

    public Point getTilePosition(Tile tile) {
        return mapViewer.getTilePosition(tile);
    }

    public Rectangle getWindowBounds() {
        return windowBounds;
    }

    public void hideSplashScreen() {
        if (splash != null) {
            splash.setVisible(false);
            splash.dispose();
        }
    }

    public boolean isClientOptionsDialogShowing() {
        return canvas != null && !canvas.isClientOptionsDialogShowing();
    }


    public boolean isMapboardActionsEnabled() {
        return canvas != null && canvas.isMapboardActionsEnabled();
    }

    public boolean isShowingSubPanel() {
        return canvas.isShowingSubPanel();
    }

    public boolean isWindowed() {
        return windowed;
    }

 
    public boolean onScreen(Tile tileToCheck) {
        return mapViewer.onScreen(tileToCheck);
    }

    public void paintImmediatelyCanvasIn(Rectangle rectangle) {
        canvas.paintImmediately(rectangle);
    }

    public void paintImmediatelyCanvasInItsBounds() {
        canvas.paintImmediately(canvas.getBounds());
    }


    /**
     * Plays some sound. Parameter == null stops playing a sound.
     *
     * @param sound The sound resource to play or <b>null</b>
     */
    public void playSound(String sound) {
        if (canPlaySound()) {
            if (sound == null) {
               soundPlayer.stop();
            } else {
               File file = ResourceManager.getAudio(sound);
               if (file != null) {
                   soundPlayer.playOnce(file);
               }
               logger.finest(((file == null) ? "Could not load" : "Playing")
                             + " sound: " + sound);
            }
        }
    }


    public void quit() throws Exception {
        if (!isWindowed()) {
                gd.setFullScreenWindow(null);
        }
    }

    public void refresh() {
        mapViewer.forceReposition();
        canvas.refresh();
    }

    public void refreshPlayersTable() {
        canvas.refreshPlayersTable();
    }

    /**
     * Refreshes the screen at the specified Tile.
     *
     * @param t The tile to refresh.
     */
    public void refreshTile(Tile t) {
        if (t.getX() >= 0 && t.getY() >= 0) {
            canvas.repaint(mapViewer.getTileBounds(t));
        }
    }

    public void removeFromCanvas(Component component) {
        canvas.remove(component);
    }

    public void removeInGameComponents() {
        canvas.removeInGameComponents();
    }

    public void requestFocusForSubPanel() {
        canvas.getShowingSubPanel().requestFocus();
    }

    public boolean requestFocusInWindow() {
        return canvas.requestFocusInWindow();
    }

    public void resetMenuBar() {
        JMenuBar menuBar = frame.getJMenuBar();
        if (menuBar != null) {
            ((FreeColMenuBar) menuBar).reset();
        }
    }

    public void returnToTitle() {
        canvas.returnToTitle();
    }

    public void scaleMap(float delta) {
        mapViewer.scaleMap(delta);
        refresh();
    }

    public void setActiveUnit(Unit unitToActivate) {
        mapViewer.setActiveUnit(unitToActivate);
    }

    public void setFocus(Tile tileToFocus) {
        mapViewer.setFocus(tileToFocus);
    }

    public void setFocusImmediately(Tile tileToFocus) {
        mapViewer.setFocusImmediately(tileToFocus);
    }

    public boolean setSelectedTile(Tile newTileToSelect, boolean clearGoToOrders) {
        return mapViewer.setSelectedTile(newTileToSelect, clearGoToOrders);
    }

    public void setupInGameMenuBar() {
        frame.setJMenuBar(new InGameMenuBar(freeColClient, this));
        frame.paintAll(canvas.getGraphics());
    }

    public void setupMenuBarToNull() {
        frame.setJMenuBar(null);
    }

    public void setUpMouseListenersForCanvas(){
        canvas.addMouseListener(new CanvasMouseListener(freeColClient, canvas, mapViewer));
        canvas.addMouseMotionListener(new CanvasMouseMotionListener(freeColClient, mapViewer));
    }

    public void setWindowed(boolean windowed) {
        this.windowed = windowed;

    }

    public void showAboutPanel() {
        canvas.showAboutPanel();
    }

    public ScoutIndianSettlementAction showArmedUnitIndianSettlementDialog(IndianSettlement settlement) {
        return canvas.showArmedUnitIndianSettlementDialog(settlement);
    }

    public BoycottAction showBoycottedGoodsDialog(Goods goods, Europe europe) {
        return canvas.showBoycottedGoodsDialog(goods, europe);
    }

    public void showBuildQueuePanel(Colony colony) {
        canvas.showBuildQueuePanel(colony);
    }

    public void showBuildQueuePanel(Colony colony, Runnable callBack) {
        canvas.showBuildQueuePanel(colony, callBack);
    }

    public BuyAction showBuyDialog(Unit unit, Settlement settlement,
            Goods goods, int gold, boolean canBuy) {
        return canvas.showBuyDialog(unit, settlement, goods, gold, canBuy);
    }

    public List<Goods> showCaptureGoodsDialog(Unit winner, List<Goods> loot) {
        return canvas.showCaptureGoodsDialog(winner, loot);
    }

    public void showChatPanel() {
        canvas.showChatPanel();
    }

    public <T> T showChoiceDialog(Tile tile, String text, String cancelText,
            List<ChoiceItem<T>> choices) {
        return canvas.showChoiceDialog(tile, text, cancelText, choices);
    }

    public MonarchAction showChoiceMonarchActionDialog(String monarchTitle, List<ChoiceItem<MonarchAction>> actions) {
        return canvas.showChoiceMonarchActionDialog(monarchTitle, actions);
    }

    public FoundingFather showChooseFoundingFatherDialog(List<ChoiceItem<FoundingFather>> fathers, String fatherTitle) {
        return canvas.showChooseFoundingFatherDialog(fathers, fatherTitle);
    }

    public FoundingFather showChooseFoundingFatherDialog(List<FoundingFather> ffs) {
        return canvas.showChooseFoundingFatherDialog(ffs);
    }

    public ClaimAction showClaimDialog(Tile tile, Player player, int price,
            Player owner, boolean canAccept) {
        return canvas.showClaimDialog(tile, player, price, owner, canAccept);
    }

    public OptionGroup showClientOptionsDialog() {
        return canvas.showClientOptionsDialog();
    }

    public ColonyPanel showColonyPanel(Colony colony) {
        return canvas.showColonyPanel(colony);
    }

    public void showColonyPanel(Colony colony, Runnable callback) {
        canvas.showColonyPanel(colony, callback);
    }

    public void showColopediaPanel(String nodeId) {
        canvas.showColopediaPanel(nodeId);
    }

    public void showCompactLabourReport() {
        canvas.showCompactLabourReport();
    }

    public void showCompactLabourReport(UnitData unitData) {
        canvas.showCompactLabourReport(unitData);
    }

    public List<String> showConfirmDeclarationDialog() {
        return canvas.showConfirmDeclarationDialog();
    }

    public boolean showConfirmDialog(String text, String okText, String cancelText) {
        return canvas.showConfirmDialog(text, okText, cancelText);
    }

    public boolean showConfirmDialog(Tile tile, ModelMessage[] messages,
            String okText, String cancelText) {
        return canvas.showConfirmDialog(tile, messages, okText, cancelText);
    }

    public boolean showConfirmDialog(Tile tile, StringTemplate text,
            String okText, String cancelText) {
        return canvas.showConfirmDialog(tile, text, okText, cancelText);
    }

    public void showDeclarationDialog() {
        canvas.showDeclarationDialog();
    }

    public void showDifficultyDialog(boolean editable) {
        canvas.showDifficultyDialog(editable);
    }

    public OptionGroup showDifficultyDialog(Specification specification) {
        return canvas.showDifficultyDialog(specification);
    }

    public List<Goods> showDumpCargoDialog(Unit unit) {
        return canvas.showDumpCargoDialog(unit);
    }


    public boolean showEditOptionDialog(Option option) {
        return canvas.showEditOptionDialog(option);
    }

    public int showEmigrationPanel(boolean fountainOfYouth) {
        return canvas.showEmigrationPanel(fountainOfYouth);
    }

    public boolean showEndTurnDialog(List<Unit> units) {
        return canvas.showEndTurnDialog(units);
    }

    public int showEuropeDialog(EuropePanel.EuropeAction europeAction) {
        return canvas.showEuropeDialog(europeAction);
    }

    public void showEuropePanel() {
        canvas.showEuropePanel();
    }

    public void showEventPanel(EventType type) {
        canvas.showEventPanel(type);
    }

    public void showFindSettlementDialog() {
        canvas.showFindSettlementDialog();
    }

    public void showGameOptionsDialog(boolean editable, boolean loadCustomOptions) {
        canvas.showGameOptionsDialog(editable, loadCustomOptions);
    }

    public void showHighScoresPanel(String messageId) {
        canvas.showHighScoresPanel(messageId);
    }

    public void showIndianSettlementPanel(IndianSettlement indianSettlement) {
        canvas.showIndianSettlementPanel(indianSettlement);
    }


    public TradeAction showIndianSettlementTradeDialog(Settlement settlement,
            boolean canBuy,
            boolean canSell,
            boolean canGift) {
        return canvas.showIndianSettlementTradeDialog(settlement, canBuy, canSell, canGift);
    }

    public void showInformationMessage(FreeColObject displayObject, String messageId) {
        canvas.showInformationMessage(displayObject, messageId);
    }

    public void showInformationMessage(FreeColObject displayObject, StringTemplate template) {
        canvas.showInformationMessage(displayObject, template);
    }

    public void showInformationMessage(ModelMessage message) {
        canvas.showInformationMessage(message);
    }

    public void showInformationMessage(String messageId) {
        canvas.showInformationMessage(messageId);
    }

    public void showInformationMessage(StringTemplate template) {
        canvas.showInformationMessage(template);
    }

    public String showInputDialog(Tile tile, StringTemplate text, String defaultValue,
            String okText, String cancelText,
            boolean rejectEmptyString) {
        return canvas.showInputDialog(tile, text, defaultValue, okText, cancelText, rejectEmptyString);
    }

    public File showLoadDialog(File directory) {
        return canvas.showLoadDialog(directory);
    }

    public File showLoadDialog(File directory, FileFilter[] fileFilters) {
        return canvas.showLoadDialog(directory, fileFilters);
    }

    public boolean showLoadingSavegameDialog(boolean publicServer, boolean singlePlayer) {
        return canvas.showLoadingSavegameDialog(publicServer, singlePlayer);
    }


    public void showLogFilePanel() {
        canvas.showLogFilePanel();
    }

    public void showMainPanel() {
        canvas.showMainPanel();
    }

    public OptionGroup showMapGeneratorOptionsDialog(OptionGroup mgo, boolean editable, boolean loadCustomOptions){
        return canvas.showMapGeneratorOptionsDialog(mgo, editable, loadCustomOptions);
    }

    public Dimension showMapSizeDialog() {
        return canvas.showMapSizeDialog();
    }

    public void showModelMessages(ModelMessage... modelMessages) {
        canvas.showModelMessages(modelMessages);
    }

    public boolean showMonarchPanelDialog(MonarchAction action, StringTemplate replace) {
        return canvas.showMonarchPanelDialog(action, replace);
    }

    public DiplomaticTrade showNegotiationDialog(Unit unit, Settlement settlement, DiplomaticTrade agreement) {
        return canvas.showNegotiationDialog(unit, settlement, agreement);
    }

    public void showNewPanel() {
        canvas.showNewPanel();
    }

    public void showNewPanel(Specification specification) {
        canvas.showNewPanel(specification);
    }

    public boolean showPreCombatDialog(FreeColGameObject attacker,
            FreeColGameObject defender,
            Tile tile) {
        return canvas.showPreCombatDialog(attacker, defender, tile);
    }

    public void showReportCargoPanel() {
        canvas.showReportCargoPanel();
    }

    public void showReportColonyPanel() {
        canvas.showReportColonyPanel();
    }

    public void showReportContinentalCongressPanel() {
        canvas.showReportContinentalCongressPanel();
    }

    public void showReportEducationPanel() {
        canvas.showReportEducationPanel();
    }

    public void showReportExplorationPanel() {
        canvas.showReportExplorationPanel();
    }

    public void showReportForeignAffairPanel() {
        canvas.showReportForeignAffairPanel();
    }

    public void showReportHistoryPanel() {
        canvas.showReportHistoryPanel();
    }

    public void showReportIndianPanel() {
        canvas.showReportIndianPanel();
    }

    public void showReportLabourDetailPanel(UnitType unitType, Map<UnitType, Map<Location, Integer>> data,
            TypeCountMap<UnitType> unitCount, List<Colony> colonies) {
        canvas.showReportLabourDetailPanel(unitType, data, unitCount, colonies);
        
    }


    public void showReportLabourPanel() {
        canvas.showReportLabourPanel();
    }

    public void showReportMilitaryPanel() {
        canvas.showReportMilitaryPanel();
    }

    public void showReportNavalPanel() {
        canvas.showReportNavalPanel();
    }

    public void showReportProductionPanel() {
        canvas.showReportProductionPanel();
    }

    public void showReportReligiousPanel() {
        canvas.showReportReligiousPanel();
    }

    public void showReportRequirementsPanel() {
        canvas.showReportRequirementsPanel();
    }

    public void showReportTradePanel() {
        canvas.showReportTradePanel();
    }

    public void showReportTurnPanel(ModelMessage... messages) {
        canvas.showReportTurnPanel(messages);
    }

    public File showSaveDialog(File directory, String defaultName) {
        return canvas.showSaveDialog(directory, defaultName);
    }

    public File showSaveDialog(File directory, String standardName, FileFilter[] fileFilters, String defaultName) {
        return canvas.showSaveDialog(directory, standardName, fileFilters, defaultName);
    }

    public ScoutColonyAction showScoutForeignColonyDialog(Colony colony,
            Unit unit,
            boolean canNegotiate) {
        return canvas.showScoutForeignColonyDialog(colony, unit, canNegotiate);
    }




    public ScoutIndianSettlementAction showScoutIndianSettlementDialog(IndianSettlement settlement, String number) {
        return canvas.showScoutIndianSettlementDialog(settlement, number);
    }

    public int showSelectAmountDialog(GoodsType goodsType, int available, int defaultAmount, boolean needToPay) {
        return canvas.showSelectAmountDialog(goodsType, available, defaultAmount, needToPay);
    }

    public Location showSelectDestinationDialog(Unit unit) {
        return canvas.showSelectDestinationDialog(unit);
    }

    public SellAction showSellDialog(Unit unit, Settlement settlement,
            Goods goods, int gold) {
        return canvas.showSellDialog(unit, settlement, goods, gold);
    }

    public void showServerListPanel(String username, List<ServerInfo> serverList) {
        canvas.showServerListPanel(username, serverList);

    }

    public <T> T showSimpleChoiceDialog(Tile tile,
            String text, String cancelText,
            List<T> objects) {
        return canvas.showSimpleChoiceDialog(tile, text, cancelText, objects);
    }

    public void showStartGamePanel(Game game, Player player, boolean singlePlayerMode) {
        canvas.showStartGamePanel(game, player, singlePlayerMode);
    }


    public void showStatisticsPanel() {
        canvas.showStatisticsPanel();
    }

    public void showStatusPanel(String message) {
        canvas.showStatusPanel(message);
    }

    public void showTilePanel(Tile tile) {
        canvas.showTilePanel(tile);
    }

    public void showTilePopUpAtSelectedTile() {
        canvas.showTilePopup(getSelectedTile(),
                mapViewer.getCursor().getCanvasX(),
                mapViewer.getCursor().getCanvasY());
    }

    public boolean showTradeRouteDialog(Unit unit) {
        return canvas.showTradeRouteDialog(unit);
    }

    public boolean showTradeRouteInputDialog(TradeRoute newRoute) {
        return canvas.showTradeRouteInputDialog(newRoute);
    }

    public MissionaryAction showUseMissionaryDialog(Unit unit,
            IndianSettlement settlement,
            boolean canEstablish,
            boolean canDenounce) {
        return canvas.showUseMissionaryDialog(unit, settlement, canEstablish, canDenounce);
    }

    public void showVictoryPanel() {
        canvas.showVictoryPanel();
    }

    public boolean showWarehouseDialog(Colony colony) {
        return canvas.showWarehouseDialog(colony);
    }

    public void showWorkProductionPanel(Unit unit) {
        canvas.showWorkProductionPanel(unit);
    }

    /**
     * Starts the GUI by creating and displaying the GUI-objects.
     */
    public void startGUI(Dimension innerWindowSize,
                         final boolean sound,
                         final boolean showOpeningVideo,
                         final boolean loadGame) {
        final ClientOptions opts = freeColClient.getClientOptions();
        // Prepare the sound system.
        if (sound) {
            final AudioMixerOption amo
                = (AudioMixerOption) opts.getOption(ClientOptions.AUDIO_MIXER);
            final PercentageOption volume
                = (PercentageOption) opts.getOption(ClientOptions.AUDIO_VOLUME);
            try {
                this.soundPlayer = new SoundPlayer(amo, volume);
            } catch (Exception e) {
                // #3168279 reports an undocumented NPE thrown by
                // AudioSystem.getMixer(null).  Workaround this and other
                // such failures by just disabling sound.
                this.soundPlayer = null;
                logger.log(Level.WARNING, "Sound disabled", e);
            }
        } else {
            this.soundPlayer = null;
        }

        if (GraphicsEnvironment.isHeadless()) {
            logger.info("It seems that the GraphicsEnvironment is headless!");
        }
        this.gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (!isWindowed()) {
            if (!gd.isFullScreenSupported()) {
                String fullscreenNotSupported =
                   "\nIt seems that full screen mode is not fully supported for this" +
                   "\nGraphicsDevice. Please try the \"--windowed\" option if you\nexperience" +
                   "any graphical problems while running FreeCol.";
                logger.info(fullscreenNotSupported);
                System.out.println(fullscreenNotSupported);
                /*
                 * We might want this behavior later: logger.warning("It seems
                 * that full screen mode is not supported for this
                 * GraphicsDevice! Using windowed mode instead."); windowed =
                 * true; setWindowed(true); frame = new
                 * WindowedFrame(size);
                 */
            }
            Rectangle bounds = gd.getDefaultConfiguration().getBounds();
            innerWindowSize = new Dimension(bounds.width - bounds.x, bounds.height - bounds.y);
        }

        // Work around a Java 2D bug that seems to be X11 specific.
        // According to:
        //   http://www.oracle.com/technetwork/java/javase/index-142560.html
        //
        //   ``The use of pixmaps typically results in better
        //     performance. However, in certain cases, the opposite is true.''
        //
        // The standard workaround is to use -Dsun.java2d.pmoffscreen=false,
        // but this is too hard for some users, so provide an option to
        // do it easily.  However respect the initial value if present.
        //
        // Remove this if Java 2D is ever fixed.  DHYB.
        //
        final String pmoffscreen = "sun.java2d.pmoffscreen";
        BooleanOption usePixmaps
            = (BooleanOption) opts.getOption(ClientOptions.USE_PIXMAPS);
        String pmoffscreenValue = System.getProperty(pmoffscreen);
        if (pmoffscreenValue == null) {
            System.setProperty(pmoffscreen, usePixmaps.getValue().toString());
            logger.info(pmoffscreen + " using client option: "
                + usePixmaps.getValue().toString());
        } else {
            usePixmaps.setValue(new Boolean(pmoffscreenValue));
            logger.info(pmoffscreen + " overrides client option: "
                + pmoffscreenValue);
        }
        usePixmaps.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) {
                    String newValue = e.getNewValue().toString();
                    System.setProperty(pmoffscreen, newValue);
                    logger.info("Set " + pmoffscreen + " to: " + newValue);
                }
            });

        this.mapViewer = new MapViewer(freeColClient, this, innerWindowSize, imageLibrary);
        this.canvas = new Canvas(freeColClient, this, innerWindowSize, mapViewer);
        this.colonyTileGUI = new MapViewer(freeColClient, this, innerWindowSize, imageLibrary);

        changeWindowedMode(isWindowed());
        frame.setIconImage(ResourceManager.getImage("FrameIcon.image"));

        // Now that there is a canvas, prepare for language changes.
        LanguageOption o = (LanguageOption) freeColClient.getClientOptions().getOption(ClientOptions.LANGUAGE);
        if (o != null) {
            o.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent e) {
                        if (((Language) e.getNewValue()).getKey().equals(LanguageOption.AUTO)) {
                            showInformationMessage("autodetectLanguageSelected");
                        } else {
                            Locale l = ((Language) e.getNewValue()).getLocale();
                            Messages.setMessageBundle(l);
                            showInformationMessage(StringTemplate.template("newLanguageSelected")
                                .addName("%language%", l.getDisplayName()));
                        }
                    }
                });
        }

        // run opening video or main panel
        if (showOpeningVideo && !loadGame) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    canvas.showOpeningVideoPanel();
                }
            });
        } else {
            if (!loadGame) {
                showMainPanel();
            }
            playSound("sound.intro.general");
        }
        mapViewer.startCursorBlinking();
    }

    public void startMapEditorGUI() {
        // We may need to reset the zoom value to the default value
        scaleMap(2f);

        setupMapEditorMenuBar();
        canvas.showMapEditorTransformPanel();

        setupMouseListenerForMapEditor();
    }

    public void toggleViewMode() {
        mapViewer.toggleViewMode();
    }

    /**
     * Try to remove a unit from a colony.
     * - Check for population limit.
     * - Query if education should be abandoned.
     *
     * @param unit The <code>Unit</code> that is leaving the colony.
     * @return True if the unit is allowed to leave.
     */
    public boolean tryLeaveColony(Unit unit) {
        Colony colony = unit.getColony();
        String message = colony.getReducePopulationMessage();
        if (message != null) {
            showInformationMessage(message);
            return false;
        }
        return confirmAbandonEducation(unit, true);
    }

    /**
     * Confirms that a unit should move somewhere where it would have
     * to abandon its participation in education (if any).
     *
     * @param unit The <code>Unit</code> to check.
     * @param checkStudent Should we check for student movements.
     * @return True if the unit should proceed to move.
     */
    public boolean confirmAbandonEducation(Unit unit, boolean checkStudent) {
        StringTemplate message = unit.getAbandonEducationMessage(checkStudent);
        return message == null
            || showConfirmDialog(unit.getTile(), message,
                "abandonEducation.yes", "abandonEducation.no");
    }

    public void updateGameOptions() {
        canvas.updateGameOptions();
    }

    public void updateMapGeneratorOptions() {
        canvas.updateMapGeneratorOptions();
    }

    public void updateMenuBar() {
        if (frame != null && frame.getJMenuBar() != null) {
            ((FreeColMenuBar) frame.getJMenuBar()).update();
        }
    }


    private void setupMapEditorMenuBar() {
        frame.setJMenuBar(new MapEditorMenuBar(freeColClient, this));
    }

    private void setupMouseListenerForMapEditor() {
        CanvasMapEditorMouseListener listener = new CanvasMapEditorMouseListener(freeColClient, this, canvas);
        canvas.addMouseListener(listener);
        canvas.addMouseMotionListener(listener);
    }

    public Parameters showParametersDialog() {
        return canvas.showParametersDialog(); 
    }

    public MapSize showScaleMapSizeDialog() {
        return canvas.showScaleMapSizeDialog();
    }

    
    

    public void showMapControls(boolean value) {
        if (value && freeColClient.isInGame()) {
            if (mapControls == null) {
                try {
                    String className = freeColClient.getClientOptions()
                        .getString(ClientOptions.MAP_CONTROLS);
                    Class<?> controls = Class.forName("net.sf.freecol.client.gui.panel." + className);
                    mapControls = (MapControls) controls.getConstructor(FreeColClient.class, GUI.class)
                        .newInstance(freeColClient, this);
                } catch(Exception e) {
                    mapControls = new CornerMapControls(freeColClient, this);
                }
            }
            mapControls.update();
        }
        if (mapControls != null) {
            if (value) {
                if (!mapControls.isShowing()) {
                    mapControls.addToComponent(canvas);
                }
                mapControls.update();
            } else {
                if (mapControls.isShowing()) {
                    mapControls.removeFromComponent(canvas);
                }
            }
        }
    }
    
    public void updateMapControls() {
        if (mapControls != null) 
            mapControls.update();
    }
    
    public void updateMapControlsInCanvas() {
        if (mapControls != null && mapControls.isShowing()) {
            mapControls.removeFromComponent(canvas);
            mapControls.addToComponent(canvas);
        }
    }

    

    public void zoomInMapControls() {
        mapControls.zoomIn();
    }
    
    public void zoomOutMapControls() {
        mapControls.zoomOut();
    }
    
    public boolean canZoomInMapControls() {
        return mapControls != null && mapControls.canZoomInMapControls();
    }

    public boolean canZoomOutMapControls() {
        return mapControls != null && mapControls.canZoomOutMapControls();
    }
    
    
    /**
     * Common utility routine to retrieve animation speed.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param unit The <code>Unit</code> to be animated.
     * @return The animation speed.
     */
    public int getAnimationSpeed(Unit unit) {
        String key = (freeColClient.getMyPlayer() == unit.getOwner())
            ? ClientOptions.MOVE_ANIMATION_SPEED
            : ClientOptions.ENEMY_MOVE_ANIMATION_SPEED;
        return freeColClient.getClientOptions().getInteger(key);
    }

    public void animateUnitAttack(Unit unit, Unit defender, boolean success) {
        Animations.unitAttack(this, unit, defender, success);
        
    }

    public void animateUnitMove(Unit unit, Tile sourceTile, Tile destinationTile) {
        Animations.unitMove(this, unit, sourceTile, destinationTile);
        
    }
    
    public BufferedImage createMiniMapThumbNail() {
        MiniMap miniMap = new MiniMap(freeColClient, this);
        miniMap.setTileSize(MiniMap.MAX_TILE_SIZE);
        int width = freeColClient.getGame().getMap().getWidth()
            * MiniMap.MAX_TILE_SIZE + MiniMap.MAX_TILE_SIZE/2;
        int height = freeColClient.getGame().getMap().getHeight()
            * MiniMap.MAX_TILE_SIZE / 4;
        BufferedImage image = new BufferedImage(width, height,
                                                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        miniMap.paintMap(g2d);

        // TODO: this can probably done more efficiently
        // by applying a suitable AffineTransform to the
        // Graphics2D
        double scaledWidth = Math.min((64 * width) / height, 128);
        BufferedImage scaledImage = new BufferedImage((int) scaledWidth, 64,
                                                      BufferedImage.TYPE_INT_ARGB);
        scaledImage.createGraphics().drawImage(image, 0, 0, (int) scaledWidth, 64, null);
        return scaledImage;
    }
    
}
