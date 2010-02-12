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

package net.sf.freecol.client.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicInternalFrameUI;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.action.MapControlsAction;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChatPanel;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.client.gui.panel.ClientOptionsDialog;
import net.sf.freecol.client.gui.panel.ColonyPanel;
import net.sf.freecol.client.gui.panel.ColopediaPanel;
import net.sf.freecol.client.gui.panel.EmigrationPanel;
import net.sf.freecol.client.gui.panel.ErrorPanel;
import net.sf.freecol.client.gui.panel.EuropePanel;
import net.sf.freecol.client.gui.panel.EventPanel;
import net.sf.freecol.client.gui.panel.FreeColDialog;
import net.sf.freecol.client.gui.panel.FreeColImageBorder;
import net.sf.freecol.client.gui.panel.FreeColPanel;
import net.sf.freecol.client.gui.panel.IndianSettlementPanel;
import net.sf.freecol.client.gui.panel.InformationDialog;
import net.sf.freecol.client.gui.panel.LoadingSavegameDialog;
import net.sf.freecol.client.gui.panel.MainPanel;
import net.sf.freecol.client.gui.panel.MapControls;
import net.sf.freecol.client.gui.panel.MapGeneratorOptionsDialog;
import net.sf.freecol.client.gui.panel.NegotiationDialog;
import net.sf.freecol.client.gui.panel.NewPanel;
import net.sf.freecol.client.gui.panel.RecruitDialog;
import net.sf.freecol.client.gui.panel.ReportHighScoresPanel;
import net.sf.freecol.client.gui.panel.ServerListPanel;
import net.sf.freecol.client.gui.panel.StartGamePanel;
import net.sf.freecol.client.gui.panel.StatusPanel;
import net.sf.freecol.client.gui.panel.TilePanel;
import net.sf.freecol.client.gui.panel.TrainDialog;
import net.sf.freecol.client.gui.video.Video;
import net.sf.freecol.client.gui.video.VideoComponent;
import net.sf.freecol.client.gui.video.VideoListener;
import net.sf.freecol.common.ServerInfo;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.server.generator.MapGeneratorOptions;

/**
 * The main container for the other GUI components in FreeCol. This container is
 * where the panels, dialogs and menus are added. In addition, this is the
 * component in which the map graphics are displayed. <br>
 * <b>Displaying panels and a dialogs</b> <br>
 * <br>
 * <code>Canvas</code> contains methods to display various panels and dialogs.
 * Most of these methods use {@link net.sf.freecol.client.gui.i18n i18n} to get
 * localized text. Here is an example: <br>
 * 
 * <PRE>
 * 
 * if (canvas.showConfirmDialog("choice.text", "choice.yes", "choice.no")) { //
 * DO SOMETHING. }
 * 
 * </PRE>
 * 
 * <br>
 * where "choice.text", "choice.yes" and "choice.no" are keys for a localized
 * message. See {@link net.sf.freecol.client.gui.i18n i18n} for more
 * information. <br>
 * <b>The difference between a panel and a dialog</b> <br>
 * <br>
 * When displaying a dialog, using a <code>showXXXDialog</code>, the calling
 * thread will wait until that dialog is dismissed before returning. In
 * contrast, a <code>showXXXPanel</code>-method returns immediately.
 */
public final class Canvas extends JDesktopPane {

    private static final Logger logger = Logger.getLogger(Canvas.class.getName());

    public static enum PopupPosition {
        ORIGIN,
        CENTERED,
        CENTERED_LEFT,
        CENTERED_RIGHT,
    }

    public static enum ScoutAction {
        CANCEL,
        INDIAN_SETTLEMENT_SPEAK,
        INDIAN_SETTLEMENT_TRIBUTE,
        INDIAN_SETTLEMENT_ATTACK,
        FOREIGN_COLONY_NEGOTIATE,
        FOREIGN_COLONY_SPY,
        FOREIGN_COLONY_ATTACK
    }

    public static enum MissionaryAction {
        CANCEL,
        ESTABLISH_MISSION,
        DENOUNCE_HERESY,
        INCITE_INDIANS
    }

    public static enum BoycottAction {
        CANCEL,
        PAY_ARREARS,
        DUMP_CARGO
    }

    public static enum TradeAction {
        CANCEL,
        BUY,
        SELL,
        GIFT
    }


    private static final Integer MAIN_LAYER = JLayeredPane.DEFAULT_LAYER;

    private static final Integer STATUS_LAYER = JLayeredPane.POPUP_LAYER;

    /**
     * To save the most recently open dialog in Europe
     * (<code>RecruitDialog</code>, <code>PurchaseDialog</code>, <code>TrainDialog</code>)
     */
    private FreeColDialog<Integer> europeOpenDialog = null;

    private final FreeColClient freeColClient;

    private final MainPanel mainPanel;

    private final StartGamePanel startGamePanel;

    private final EuropePanel europePanel;

    private final StatusPanel statusPanel;

    private final ChatPanel chatPanel;
    
    private final GUI gui;

    private final ServerListPanel serverListPanel;

    private final ClientOptionsDialog clientOptionsDialog;

    private final LoadingSavegameDialog loadingSavegameDialog;
    
    /**
     * This is the GUI instance used to paint the colony tiles in the
     * ColonyPanel and other panels. It should not be scaled along
     * with the default GUI.
     */
    private final GUI colonyTileGUI;

    private boolean clientOptionsDialogShowing = false;

    private MapControls mapControls;

    /**
     * Variable used for detecting resizing.
     */
    private Dimension oldSize = null;
    
    private Dimension initialSize = null;

    /**
     * The constructor to use.
     * 
     * @param client main control class.
     * @param size The bounds of this <code>Canvas</code>.
     * @param gui The object responsible of drawing the map onto this component.
     */
    public Canvas(FreeColClient client, Dimension size, GUI gui) {
        this.freeColClient = client;
        this.gui = gui;

        colonyTileGUI = new GUI(client, size, freeColClient.getImageLibrary());

        initialSize = size;

        setLocation(0, 0);
        setSize(size);
        
        setDoubleBuffered(true);
        setOpaque(false);
        setLayout(null);

        mainPanel = new MainPanel(this);
        startGamePanel = new StartGamePanel(this);
        serverListPanel = new ServerListPanel(this, freeColClient.getConnectController());
        europePanel = new EuropePanel(this);
        statusPanel = new StatusPanel(this);
        chatPanel = new ChatPanel(this);
        clientOptionsDialog = new ClientOptionsDialog(this);
        loadingSavegameDialog = new LoadingSavegameDialog(this);

        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        // takeFocus();

        // chatDisplayThread = new ChatDisplayThread();
        // chatDisplayThread.start();

        // TODO: move shutdown hook from GUI to (say) client!
        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new Thread(FreeCol.CLIENT_THREAD+"Quitting Game") {
            @Override
            public void run() {
                freeColClient.getConnectController().quitGame(true);
            }
        });

        logger.info("Canvas created.");
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(640, 480);
    }

    @Override
    public Dimension getPreferredSize() {
        return initialSize;
    }
    
    public GUI getColonyTileGUI() {
        return colonyTileGUI;
    }

    /**
     * Returns the <code>ClientOptionsDialog</code>.
     * 
     * @return The <code>ClientOptionsDialog</code>
     * @see net.sf.freecol.client.ClientOptions
     */
    public ClientOptionsDialog getClientOptionsDialog() {
        return clientOptionsDialog;
    }

    public ImageLibrary getImageLibrary() {
        return freeColClient.getImageLibrary();
    }

    /**
     * Updates the label displaying the current amount of gold.
     */
    public void updateGoldLabel() {
        getClient().getFrame().getJMenuBar().repaint();
    }

    /**
     * Updates the sizes of the components on this Canvas.
     */
    public void updateSizes() {
        if (oldSize == null) {
            oldSize = getSize();
        }
        if (oldSize.width != getWidth() || oldSize.height != getHeight()) {
            MapControlsAction mca = (MapControlsAction) freeColClient
                .getActionManager().getFreeColAction(MapControlsAction.id);
            MapControls mc = mca.getMapControls();
            if (mc != null && mc.isShowing()) {
                mc.removeFromComponent(this);
                mc.addToComponent(this);
                mapControls = mc;
            }
            if (europePanel != null) {
                JInternalFrame f = getInternalFrame(europePanel);
                if (f != null) {
                    f.setSize(getWidth(), getHeight());
                    f.setLocation(0, 0);
                }
            }
            gui.setSize(getSize());
            gui.forceReposition();
            oldSize = getSize();
        }
    }
    
    /**
     * Paints this component. This method will use {@link GUI#display} to draw
     * the map/background on this component.
     * 
     * @param g The Graphics context in which to draw this component.
     * @see GUI#display
     */
    @Override
    public void paintComponent(Graphics g) {
        updateSizes();
        Graphics2D g2d = (Graphics2D) g;
        gui.display(g2d);
    }

    /**
     * Displays the <code>StartGamePanel</code>.
     * 
     * @param game The <code>Game</code> that is about to start.
     * @param player The <code>Player</code> using this client.
     * @param singlePlayerMode 'true' if the user wants to start a single player
     *            game, 'false' otherwise.
     * @see StartGamePanel
     */
    public void showStartGamePanel(Game game, Player player, boolean singlePlayerMode) {
        closeMenus();

        if (game != null && player != null) {
            startGamePanel.initialize(singlePlayerMode);
            showSubPanel(startGamePanel);
        } else {
            logger.warning("Tried to open 'StartGamePanel' without having 'game' and/or 'player' set.");
        }
    }

    /**
     * Displays the <code>ServerListPanel</code>.
     * 
     * @param username The username that should be used when connecting to one
     *            of the servers on the list.
     * @param serverList The list containing the servers retrieved from the
     *            metaserver.
     * @see ServerListPanel
     */
    public void showServerListPanel(String username, ArrayList<ServerInfo> serverList) {
        closeMenus();

        serverListPanel.initialize(username, serverList);
        showSubPanel(serverListPanel);
    }

    /**
     * Displays the <code>NegotiationDialog</code>.
     * 
     * @see NegotiationDialog
     */
    public DiplomaticTrade showNegotiationDialog(Unit unit, Settlement settlement, DiplomaticTrade agreement) {
        NegotiationDialog negotiationDialog = new NegotiationDialog(this, unit, settlement, agreement);
        negotiationDialog.initialize();
        return showFreeColDialog(negotiationDialog, unit.getTile());
    }

    /**
     * Displays a <code>FreeColPanel</code>.
     *
     * @param panel a <code>FreeColPanel</code> value
     */
    public void showPanel(FreeColPanel panel) {
        closeMenus();
        showSubPanel(panel);
    }

    /**
     * Displays a <code>FreeColPanel</code>.
     *
     * @param panel a <code>FreeColPanel</code> value
     * @param centered a <code>boolean</code> value
     */
    public void showPanel(FreeColPanel panel, boolean centered) {
        closeMenus();
        addAsFrame(panel, false, (centered) ? PopupPosition.CENTERED
                   : PopupPosition.ORIGIN);
        panel.requestFocus();
    }

    /**
     * Displays a <code>FreeColPanel</code>.
     * 
     */
    public void showSubPanel(FreeColPanel panel) {
        addAsFrame(panel);
        panel.requestFocus();
    }

    /**
     * Displays a <code>FreeColPanel</code> at a generalized position.
     *
     * @param popupPosition The generalized position to place the panel.
     */
    public void showSubPanel(FreeColPanel panel, PopupPosition popupPosition) {
        addAsFrame(panel, false, popupPosition);
        panel.requestFocus();
    }

    /**
     * Displays the <code>ChatPanel</code>.
     * 
     * @see ChatPanel
     */
    // TODO: does it have state, or can we create a new one?
    public void showChatPanel() {
    	
        // in single player, no chat available
        if (freeColClient.isSingleplayer()) {
            return;
        }
    	
        showPanel(chatPanel);
    }

    /**
     * Displays a number of ModelMessages.
     * 
     * @param modelMessages
     */
    public void showModelMessages(ModelMessage... modelMessages) {

        if (modelMessages.length == 1) {
            String id = modelMessages[0].getId();
            if (EventPanel.EventType.MEETING_NATIVES.toString().equals(id)) {
                showFreeColDialog(new EventPanel(this, EventPanel.EventType.MEETING_NATIVES));
                return;
            } else if (EventPanel.EventType.MEETING_AZTEC.toString().equals(id)) {
                showFreeColDialog(new EventPanel(this, EventPanel.EventType.MEETING_AZTEC));
                return;
            }
        }

        String okText = "ok";
        String cancelText = "display";
        String[] messageText = new String[modelMessages.length];
        ImageIcon[] messageIcon = new ImageIcon[modelMessages.length];
        try {
            okText = Messages.message(okText);
        } catch (MissingResourceException e) {
            logger.warning("could not find message with id: " + okText + ".");
        }
        try {
            cancelText = Messages.message(cancelText);
        } catch (MissingResourceException e) {
            logger.warning("could not find message with id: " + cancelText + ".");
        }
        for (int i = 0; i < modelMessages.length; i++) {
            try {
                messageText[i] = Messages.message(modelMessages[i].getId(), modelMessages[i].getData());
            } catch (MissingResourceException e) {
                logger.warning("could not find message with id: " + modelMessages[i].getId() + ".");
            }

            messageIcon[i] = getImageIcon(modelMessages[i].getDisplay(), false);
        }

        // source should be the same for all messages
        FreeColGameObject source = modelMessages[0].getSource();
        if ((source instanceof Europe && !europePanel.isShowing())
                || (source instanceof Colony || source instanceof WorkLocation)) {

            FreeColDialog<Boolean> confirmDialog = FreeColDialog.createConfirmDialog(messageText, messageIcon, okText,
                    cancelText);
            if (showFreeColDialog(confirmDialog)) {
                if (!isShowingSubPanel()) {
                    freeColClient.getInGameController().nextModelMessage();
                }
            } else {
                if (source instanceof Europe) {
                    showEuropePanel();
                } else if (source instanceof Colony) {
                    showColonyPanel((Colony) source);
                } else if (source instanceof WorkLocation) {
                    showColonyPanel(((WorkLocation) source).getColony());
                }
            }
        } else {
            showFreeColDialog(new InformationDialog(this,
                                                    messageText, messageIcon));
            if (!isShowingSubPanel()) {
                freeColClient.getInGameController().nextModelMessage();
            }
        }
    }

    /**
     * Returns the appropriate ImageIcon for Object.
     * 
     * @param display The Object to display.
     * @return The appropriate ImageIcon.
     */
    public ImageIcon getImageIcon(Object display, boolean small) {
        ImageLibrary imageLibrary = freeColClient.getImageLibrary();
        Image image = null;
        if (display == null) {
            return new ImageIcon();
        } else if (display instanceof GoodsType) {
            GoodsType goodsType = (GoodsType) display;
            try {
                image = imageLibrary.getGoodsImage(goodsType);
            } catch (Exception e) {
                logger.warning("could not find image for goods " + goodsType.getName());
            }
        } else if (display instanceof Unit) {
            Unit unit = (Unit) display;
            try {
                image = imageLibrary.getUnitImageIcon(unit.getType()).getImage();
            } catch (Exception e) {
                logger.warning("could not find image for unit " + unit.getName());
            }
        } else if (display instanceof UnitType) {
            UnitType unitType = (UnitType) display;
            try {
                image = imageLibrary.getUnitImageIcon(unitType).getImage();
            } catch (Exception e) {
                logger.warning("could not find image for unit " + unitType.getName());
            }
        } else if (display instanceof Settlement) {
            Settlement settlement = (Settlement) display;
            try {
                image = imageLibrary.getSettlementImage(settlement);
            } catch (Exception e) {
                logger.warning("could not find image for settlement " + settlement);
            }
        } else if (display instanceof LostCityRumour) {
            try {
                image = imageLibrary.getMiscImage(ImageLibrary.LOST_CITY_RUMOUR);
            } catch (Exception e) {
                logger.warning("could not find image for lost city rumour");
            }
        } else if (display instanceof Player) {
            image = imageLibrary.getCoatOfArmsImage(((Player) display).getNation());
        } 
        if (image != null && small) {
            return new ImageIcon(image.getScaledInstance((image.getWidth(null) / 3) * 2,
                                                         (image.getHeight(null) / 3) *2,
                                                         Image.SCALE_SMOOTH));
        } else {
            return (image != null) ? new ImageIcon(image) : null;
        }
    }

    /**
     * Displays the given dialog.
     * 
     * @param freeColDialog The dialog to be displayed
     * @return The {@link FreeColDialog#getResponse reponse} returned by
     *         the dialog.
     */
    public <T> T showFreeColDialog(FreeColDialog<T> freeColDialog) {
        return showFreeColDialog(freeColDialog, null);
    }

    /**
     * Given a tile to be made visible, determine a position to popup
     * a panel.
     *
     * @param tile A <code>Tile</code> to be made visible.
     * @return A <code>PopupPosition</code> for a panel to be displayed.
     */
    private PopupPosition getPopupPosition(Tile tile) {
        if (tile == null) return PopupPosition.CENTERED;
        int where = gui.setOffsetFocus(tile.getPosition());
        return (where > 0) ? PopupPosition.CENTERED_LEFT
            : (where < 0) ? PopupPosition.CENTERED_RIGHT
            : PopupPosition.CENTERED;
    }

    /**
     * Displays the given dialog, making sure a tile is visible.
     *
     * @param freeColDialog The dialog to be displayed
     * @param tile A <code>Tile</code> to make visible (not under the dialog!)
     * @return The {@link FreeColDialog#getResponse reponse} returned by
     *         the dialog.
     */
    public <T> T showFreeColDialog(FreeColDialog<T> freeColDialog, Tile tile) {
        PopupPosition popupPosition = getPopupPosition(tile);
        showSubPanel(freeColDialog, popupPosition);
        T response = freeColDialog.getResponse();
        remove(freeColDialog);
        return response;
    }

    /**
     * Displays a dialog with a text and a ok/cancel option.
     *
     * @param text The text that explains the choice for the user.
     * @param okText The text displayed on the "ok"-button.
     * @param cancelText The text displayed on the "cancel"-button.
     * @return <i>true</i> if the user clicked the "ok"-button and <i>false</i>
     *         otherwise.
     * @see FreeColDialog
     */
    public boolean showConfirmDialog(String text,
                                     String okText, String cancelText) {
        return showConfirmDialog(null, text, okText, cancelText);
    }

    /**
     * Displays a dialog with a text and a ok/cancel option.
     *
     * @param tile A <code>Tile</code> to make visible (not under the dialog!)
     * @param text The text that explains the choice for the user.
     * @param okText The text displayed on the "ok"-button.
     * @param cancelText The text displayed on the "cancel"-button.
     * @param replace An array of strings that will be inserted somewhere in the
     *            text.
     * @return <i>true</i> if the user clicked the "ok"-button and <i>false</i>
     *         otherwise.
     * @see FreeColDialog
     */
    public boolean showConfirmDialog(Tile tile, String text,
                                     String okText, String cancelText,
                                     String... replace) {
        try {
            text = Messages.message(text, replace);
            okText = Messages.message(okText);
            cancelText = Messages.message(cancelText);
        } catch (MissingResourceException e) {
            logger.warning("could not find message with id: " + text
                           + ", " + okText + " or " + cancelText + ".");
        }
        return showFreeColDialog(FreeColDialog.createConfirmDialog(text,
                                                                   okText,
                                                                   cancelText),
                                 tile);
    }

    /**
     * Displays a dialog with a text and a ok/cancel option.
     * 
     * @param messages The messages that explains the choice for the user.
     * @param okText The text displayed on the "ok"-button.
     * @param cancelText The text displayed on the "cancel"-button.
     * @return <i>true</i> if the user clicked the "ok"-button and <i>false</i>
     *         otherwise.
     * @see FreeColDialog
     */
    public boolean showConfirmDialog(ModelMessage[] messages, String okText, String cancelText) {
        try {
            okText = Messages.message(okText);
        } catch (MissingResourceException e) {
            logger.warning("could not find message with id: " + okText + ".");
        }
        try {
            cancelText = Messages.message(cancelText);
        } catch (MissingResourceException e) {
            logger.warning("could not find message with id: " + cancelText + ".");
        }

        String[] texts = new String[messages.length];
        ImageIcon[] images = new ImageIcon[messages.length];
        for (int i = 0; i < messages.length; i++) {
            String id = messages[i].getId();
            try {
                texts[i] = Messages.message(id, messages[i].getData());
            } catch (MissingResourceException e) {
                logger.warning("could not find message with id: " + id + ".");
            }
            images[i] = getImageIcon(messages[i].getDisplay(), false);
        }

        FreeColDialog<Boolean> confirmDialog = FreeColDialog.createConfirmDialog(texts, images, okText, cancelText);
        return showFreeColDialog(confirmDialog);
    }

    /**
     * Checks if the <code>ClientOptionsDialog</code> is visible.
     * 
     * @return <code>true</code> if no internal frames are open.
     */
    public boolean isClientOptionsDialogShowing() {
        return clientOptionsDialogShowing;
    }

    /**
     * Checks if mapboard actions should be enabled.
     * 
     * @return <code>true</code> if no internal frames are open.
     */
    public boolean isMapboardActionsEnabled() {
        return !isShowingSubPanel();
    }

    /**
     * Checks if this <code>Canvas</code> displaying another panel.
     * <p>
     * Note that the previous implementation could throw exceptions
     * in some cases, thus the change.
     * 
     * @return <code>true</code> if the <code>Canvas</code> is displaying an
     *         internal frame.
     */
    public boolean isShowingSubPanel() {
        return getShowingSubPanel() != null;
    }

    /**
     * The any panel this <code>Canvas</code> is displaying.
     *
     * @return A <code>Component</code> the <code>Canvas</code> is
     *         displaying, or null if none found.
     */
    public Component getShowingSubPanel() {
        Component[] components = getComponents();
        for (Component c : components) {
            if (c instanceof ToolBoxFrame) {
                continue;
            }
            if (c instanceof JInternalFrame) {
                return c;
            } else if (c instanceof JInternalFrame.JDesktopIcon) {
                return c;
            }
        }
        return null;
    }

    /**
     * Displays a dialog for setting game options.
     * 
     * @return <code>true</code> if the game options have been modified, and
     *         <code>false</code> otherwise.
     */

    /**
     * Gets the <code>LoadingSavegameDialog</code>.
     * 
     * @return The <code>LoadingSavegameDialog</code>.
     */
    public LoadingSavegameDialog getLoadingSavegameDialog() {
        return loadingSavegameDialog;
    }

    /**
     * Displays a dialog for setting options when loading a savegame. The
     * settings can be retrieved directly from {@link LoadingSavegameDialog}
     * after calling this method.
     * 
     * @param publicServer Default value.
     * @param singleplayer Default value.
     * @return <code>true</code> if the "ok"-button was pressed and
     *         <code>false</code> otherwise.
     */
    public boolean showLoadingSavegameDialog(boolean publicServer, boolean singleplayer) {
        loadingSavegameDialog.initialize(publicServer, singleplayer);
        return showFreeColDialog(loadingSavegameDialog);
    }

    /**
     * Displays a dialog for setting client options.
     * 
     * @return <code>true</code> if the client options have been modified, and
     *         <code>false</code> otherwise.
     */
    public boolean showClientOptionsDialog() {
        clientOptionsDialog.initialize();
        clientOptionsDialogShowing = true;
        boolean r = showFreeColDialog(clientOptionsDialog);
        clientOptionsDialogShowing = false;
        freeColClient.getActionManager().update();
        return r;
    }

    /**
     * Displays a dialog for setting the map generator options.
     * 
     * @param editable The options are only allowed to be changed if this
     *            variable is <code>true</code>.
     * @return <code>true</code> if the options have been modified, and
     *         <code>false</code> otherwise.
     */
    public boolean showMapGeneratorOptionsDialog(boolean editable) {
        final MapGeneratorOptions mgo = freeColClient.getPreGameController().getMapGeneratorOptions();
        return showMapGeneratorOptionsDialog(editable, mgo);
    }
    
    /**
     * Displays a dialog for setting the map generator options.
     * 
     * @param editable The options are only allowed to be changed if this
     *            variable is <code>true</code>.
     * @return <code>true</code> if the options have been modified, and
     *         <code>false</code> otherwise.
     */
    public boolean showMapGeneratorOptionsDialog(boolean editable, MapGeneratorOptions mgo) {
        MapGeneratorOptionsDialog mapGeneratorOptionsDialog = new MapGeneratorOptionsDialog(this);
        mapGeneratorOptionsDialog.initialize(editable, mgo);
        return showFreeColDialog(mapGeneratorOptionsDialog);
    }

    /**
     * Displays a dialog where the user may choose a file. This is the same as
     * calling:
     * 
     * <br>
     * <br>
     * <code>
     * showLoadDialog(directory, new FileFilter[] {FreeColDialog.getFSGFileFilter()});
     * </code>
     * 
     * @param directory The directory containing the files.
     * @return The <code>File</code>.
     * @see FreeColDialog
     */
    public File showLoadDialog(File directory) {
        return showLoadDialog(directory, new FileFilter[] { FreeColDialog.getFSGFileFilter() });
    }

    /**
     * Displays a dialog where the user may choose a file.
     * 
     * @param directory The directory containing the files.
     * @param fileFilters The file filters which the user can select in the
     *            dialog.
     * @return The <code>File</code>.
     * @see FreeColDialog
     */
    public File showLoadDialog(File directory, FileFilter[] fileFilters) {
        FreeColDialog<File> loadDialog = FreeColDialog.createLoadDialog(directory, fileFilters);

        File response = null;
        showSubPanel(loadDialog);
        for (;;) {
            response = (File) loadDialog.getResponse();
            if (response != null && response.isFile()) break;
            errorMessage("noSuchFile");
        }
        remove(loadDialog);

        return response;
    }

    /**
     * Displays a dialog where the user may choose a filename. This is the same
     * as calling:
     * 
     * <br>
     * <br>
     * <code>
     * showSaveDialog(directory, new FileFilter[] {FreeColDialog.getFSGFileFilter()}, defaultName);
     * </code>
     * 
     * @param directory The directory containing the files in which the user may
     *            overwrite.
     * @param defaultName Default filename for the savegame.
     * @return The <code>File</code>.
     * @see FreeColDialog
     */
    public File showSaveDialog(File directory, String defaultName) {
        return showSaveDialog(directory, ".fsg", new FileFilter[] { FreeColDialog.getFSGFileFilter() }, defaultName);
    }

    /**
     * Displays a dialog where the user may choose a filename.
     * 
     * @param directory The directory containing the files in which the user may
     *            overwrite.
     * @param standardName This extension will be added to the specified
     *            filename (if not added by the user).
     * @param fileFilters The available file filters in the dialog.
     * @param defaultName Default filename for the savegame.
     * @return The <code>File</code>.
     * @see FreeColDialog
     */
    public File showSaveDialog(File directory, String standardName, FileFilter[] fileFilters, String defaultName) {
        FreeColDialog<File> saveDialog = FreeColDialog.createSaveDialog(directory, standardName, fileFilters, defaultName);
        return showFreeColDialog(saveDialog);
    }

    /**
     * Displays a dialog that asks the user whether to pay arrears for
     * boycotted goods or to dump them instead.
     *
     * @param goods a <code>Goods</code> value
     * @param europe an <code>Europe</code> value
     * @return a <code>boolean</code> value
     */
    public BoycottAction showBoycottedGoodsDialog(Goods goods, Europe europe) {
        int arrears = europe.getOwner().getArrears(goods.getType());
        List<ChoiceItem<BoycottAction>> choices = new ArrayList<ChoiceItem<BoycottAction>>();
        choices.add(new ChoiceItem<BoycottAction>(Messages.message("boycottedGoods.payArrears"),
                                                  BoycottAction.PAY_ARREARS));
        choices.add(new ChoiceItem<BoycottAction>(Messages.message("boycottedGoods.dumpGoods"),
                                                  BoycottAction.DUMP_CARGO));
        choices.add(new ChoiceItem<BoycottAction>(Messages.message("cancel"),
                                                  BoycottAction.CANCEL));
        FreeColDialog<ChoiceItem<BoycottAction>> boycottedGoodsDialog = FreeColDialog
            .createChoiceDialog(Messages.message("boycottedGoods.text", 
                                                 "%goods%", goods.getName(), 
                                                 "%europe%", europe.getName(),
                                                 "%amount%", String.valueOf(arrears)),
                                null, choices);
        return showFreeColDialog(boycottedGoodsDialog).getObject();
    }

    /**
     * Displays a dialog that asks the user what he wants to do with his scout
     * in the indian settlement.
     * 
     * @param settlement The indian settlement that is being scouted.
     * 
     * @return ScoutAction.CANCEL if the action was cancelled,
     *         ScoutAction.INDIAN_SETTLEMENT_SPEAK if he wants to
     *         speak with the chief,
     *         ScoutAction.INDIAN_SETTLEMENT_TRIBUTE if he wants to
     *         demand tribute, ScoutAction.INDIAN_SETTLEMENT_ATTACK if
     *         he wants to attack the settlement.
     */
    public ScoutAction showScoutIndianSettlementDialog(IndianSettlement settlement) {
        StringBuilder text = new StringBuilder(400);
        text.append(Messages.message(settlement.getAlarmLevelMessage(freeColClient.getMyPlayer()),
                                     "%nation%", settlement.getOwner().getNationAsString()));
        text.append("\n\n");
        int number = settlement.getOwner().getNumberOfSettlements();
        text.append(Messages.message("scoutSettlement.greetings",
                                     "%nation%", settlement.getOwner().getNationAsString(),
                                     "%settlement%", settlement.getName(),
                                     "%number%", String.valueOf(number)));
        text.append(" ");
        if (settlement.getLearnableSkill() != null) {
            text.append(Messages.message("scoutSettlement.skill", "%skill%",
                                         settlement.getLearnableSkill().getName()));
            text.append(" ");
        }
        GoodsType[] wantedGoods = settlement.getWantedGoods();
        if (wantedGoods[0] != null) {
            text.append(Messages.message("scoutSettlement.trade",
                                         "%goods1%", wantedGoods[0].getName(),
                                         "%goods2%", wantedGoods[1].getName(),
                                         "%goods3%", wantedGoods[2].getName()));
            text.append("\n\n");
        }

        List<ChoiceItem<ScoutAction>> choices = new ArrayList<ChoiceItem<ScoutAction>>();
        choices.add(new ChoiceItem<ScoutAction>(Messages.message("scoutSettlement.speak"),
                                                ScoutAction.INDIAN_SETTLEMENT_SPEAK));
        choices.add(new ChoiceItem<ScoutAction>(Messages.message("scoutSettlement.tribute"),
                                                ScoutAction.INDIAN_SETTLEMENT_TRIBUTE));
        choices.add(new ChoiceItem<ScoutAction>(Messages.message("scoutSettlement.attack"),
                                                ScoutAction.INDIAN_SETTLEMENT_ATTACK));
        choices.add(new ChoiceItem<ScoutAction>(Messages.message("cancel"),
                                                ScoutAction.CANCEL));    
        FreeColDialog<ChoiceItem<ScoutAction>> scoutDialog
            = FreeColDialog.createChoiceDialog(text.toString(), null, choices);
        return showFreeColDialog(scoutDialog, settlement.getTile()).getObject();
    }

    /**
     * Displays a dialog that asks the user what he wants to do with his scout
     * in the foreign colony.
     * 
     * @param colony The foreign colony that is being scouted.
     * 
     * @return ScoutAction.CANCEL if the action was cancelled,
     *         ScoutAction.FOREIGN_COLONY_NEGOTIATE if he wants to
     *         negotiate with the foreign power,
     *         ScoutAction.FOREIGN_COLONY_SPY if he wants to spy the
     *         colony, ScoutAction.FOREIGN_COLONY_ATTACK if he wants
     *         to attack the colony.
     */
    public ScoutAction showScoutForeignColonyDialog(Colony colony, Unit unit) {
        String mainText = Messages.message("scoutColony.text", 
                                           "%unit%", unit.getName(), 
                                           "%colony%", colony.getName());
        List<ChoiceItem<ScoutAction>> choices = new ArrayList<ChoiceItem<ScoutAction>>();
        
        // We cannot negotiate with the REF
        boolean isPlayersREF = colony.getOwner() == unit.getOwner().getREFPlayer();
        if(!isPlayersREF){
            choices.add(new ChoiceItem<ScoutAction>(Messages.message("scoutColony.negotiate"), 
                                                ScoutAction.FOREIGN_COLONY_NEGOTIATE));
        }
        choices.add(new ChoiceItem<ScoutAction>(Messages.message("scoutColony.spy"),
                                                ScoutAction.FOREIGN_COLONY_SPY));
        choices.add(new ChoiceItem<ScoutAction>(Messages.message("scoutColony.attack"),
                                                ScoutAction.FOREIGN_COLONY_ATTACK));
        choices.add(new ChoiceItem<ScoutAction>(Messages.message("cancel"),
                                                ScoutAction.CANCEL));
        FreeColDialog<ChoiceItem<ScoutAction>> scoutDialog =
            FreeColDialog.createChoiceDialog(mainText, null, choices);
        return showFreeColDialog(scoutDialog, colony.getTile()).getObject();
    }

    /**
     * Displays a dialog that asks the user what he wants to do with his armed
     * unit in the indian settlement.
     * 
     * @param settlement The indian settlement that is going to be attacked or
     *            demanded.
     * 
     * @return ScoutAction.CANCEL if the action was
     *         cancelled, ScoutAction.INDIAN_SETTLEMENT_TRIBUTE if he
     *         wants to demand tribute,
     *         ScoutAction.INDIAN_SETTLEMENT_ATTACK if he wants to
     *         attack the settlement.
     */
    public ScoutAction showArmedUnitIndianSettlementDialog(IndianSettlement settlement) {
        String introText = Messages.message(settlement.getAlarmLevelMessage(freeColClient.getMyPlayer()),
                "%nation%", settlement.getOwner().getNationAsString());

        List<ChoiceItem<ScoutAction>> choices = new ArrayList<ChoiceItem<ScoutAction>>();
        choices.add(new ChoiceItem<ScoutAction>(Messages.message("scoutSettlement.tribute"),
                                                ScoutAction.INDIAN_SETTLEMENT_TRIBUTE));
        choices.add(new ChoiceItem<ScoutAction>(Messages.message("scoutSettlement.attack"),
                                                ScoutAction.INDIAN_SETTLEMENT_ATTACK));
        choices.add(new ChoiceItem<ScoutAction>(Messages.message("cancel"),
                                                ScoutAction.CANCEL));
        FreeColDialog<ChoiceItem<ScoutAction>> armedUnitDialog =
            FreeColDialog.createChoiceDialog(introText, null, choices);
        return showFreeColDialog(armedUnitDialog, settlement.getTile()).getObject();
    }

    /**
     * Displays a dialog that asks the user what he wants to do with his
     * missionary in the indian settlement.
     * 
     * @param settlement The indian settlement that is being visited.
     * 
     * @return ArrayList with an Integer and optionally a Player referencing the
     *         player to attack in case of "incite indians". Integer can be any
     *         of: FreeColDialog.MISSIONARY_ESTABLISH if he wants to establish a
     *         mission, FreeColDialog.MISSIONARY_DENOUNCE_AS_HERESY if he wants
     *         to denounce the existing (foreign) mission as heresy,
     *         FreeColDialog.MISSIONARY_INCITE_INDIANS if he wants to incite the
     *         indians (requests their support for war against another European
     *         power), FreeColDialog.MISSIONARY_CANCEL if the action was
     *         cancelled.
     */
    public List<Object> showUseMissionaryDialog(IndianSettlement settlement) {
        Player player = freeColClient.getMyPlayer();
        Unit missionary = settlement.getMissionary();
        StringBuilder introText
            = new StringBuilder(Messages.message(settlement.getAlarmLevelMessage(player),
                                                 "%nation%", settlement.getOwner().getNationAsString()));
        introText.append("\n\n");
        introText.append(Messages.message("missionarySettlement.question",
                                          "%settlement%", settlement.getName()));

        List<ChoiceItem<MissionaryAction>> choices = new ArrayList<ChoiceItem<MissionaryAction>>();
        if (missionary == null) {
            choices.add(new ChoiceItem<MissionaryAction>(Messages.message("missionarySettlement.establish"),
                                                         MissionaryAction.ESTABLISH_MISSION));
        }
        if (missionary != null && missionary.getOwner() != player) {
            choices.add(new ChoiceItem<MissionaryAction>(Messages.message("missionarySettlement.heresy"),
                                                         MissionaryAction.DENOUNCE_HERESY));
        }
        choices.add(new ChoiceItem<MissionaryAction>(Messages.message("missionarySettlement.incite"),
                                                     MissionaryAction.INCITE_INDIANS));
        choices.add(new ChoiceItem<MissionaryAction>(Messages.message("cancel"),
                                                     MissionaryAction.CANCEL));
        FreeColDialog<ChoiceItem<MissionaryAction>> missionaryDialog
            = FreeColDialog.createChoiceDialog(introText.toString(), null, choices);
        MissionaryAction response = showFreeColDialog(missionaryDialog, settlement.getTile()).getObject();
        ArrayList<Object> returnValue = new ArrayList<Object>();
        // TODO: Find a solution so that we can use a more specialized list.
        returnValue.add(response);

        if (MissionaryAction.INCITE_INDIANS.equals(response)) {
            List<Player> enemies = new ArrayList<Player>(freeColClient.getGame().getEuropeanPlayers());
            enemies.remove(freeColClient.getMyPlayer());
            List<ChoiceItem<Player>> enemyChoices = new ArrayList<ChoiceItem<Player>>();
            for (Player enemy : enemies) {
                enemyChoices.add(new ChoiceItem<Player>(enemy.getNationAsString(), enemy));
            }
            Player enemy = showChoiceDialog(Messages.message("missionarySettlement.inciteQuestion"),
                                            Messages.message("missionarySettlement.cancel"),
                                            enemyChoices);
            if (enemy == null) {
                returnValue.clear();
                returnValue.add(MissionaryAction.CANCEL);
            } else {
                returnValue.add(enemy);
            }

        }

        return returnValue;
    }

    /**
     * Displays a yes/no question to the user asking if he wants to pay the
     * given amount to an indian tribe in order to have them declare war on the
     * given player.
     * 
     * @param enemy The european player to attack.
     * @param amount The amount of gold to pay.
     * @param tile A tile to make visible (not under the dialog).
     * 
     * @return true if the players wants to pay, false otherwise.
     */
    public boolean showInciteDialog(Player enemy, int amount, Tile tile) {
        String text = Messages.message("missionarySettlement.inciteConfirm",
                                       "%player%", enemy.getName(),
                                       "%amount%", String.valueOf(amount));
        FreeColDialog<Boolean> confirmDialog
            = FreeColDialog.createConfirmDialog(text,
                                                Messages.message("yes"),
                                                Messages.message("no"));
        return showFreeColDialog(confirmDialog, tile);
    }

    /**
     * Displays a dialog with a text field and a ok/cancel option.
     * 
     * @param text The text that explains the action to the user.
     * @param defaultValue The default value appearing in the text field.
     * @param okText The text displayed on the "ok"-button.
     * @param cancelText The text displayed on the "cancel"-button. Use <i>null</i>
     *            to disable the cancel-option.
     * @return The text the user have entered or <i>null</i> if the user chose
     *         to cancel the action.
     * @see FreeColDialog
     */
    public String showInputDialog(String text, String defaultValue, String okText, String cancelText,
                                  String... data) {
        return showInputDialog(text, defaultValue, okText, cancelText, true, data);
    }

    /**
     * Displays a dialog with a text field and a ok/cancel option.
     * 
     * @param text The text that explains the action to the user.
     * @param defaultValue The default value appearing in the text field.
     * @param okText The text displayed on the "ok"-button.
     * @param cancelText The text displayed on the "cancel"-button. Use <i>null</i>
     *            to disable the cancel-option.
     * @param rejectEmptyString a <code>boolean</code> value
     * @return The text the user have entered or <i>null</i> if the user chose
     *         to cancel the action.
     * @see FreeColDialog
     */
    public String showInputDialog(String text, String defaultValue, String okText, String cancelText,
                                  boolean rejectEmptyString, String... data) {
        try {
            text = Messages.message(text, data);
            okText = Messages.message(okText);

            if (cancelText != null) {
                cancelText = Messages.message(cancelText);
            }
        } catch (MissingResourceException e) {
            logger.warning("could not find message with id: " + text + ", " + okText + " or " + cancelText + ".");
        }

        FreeColDialog<String> inputDialog = FreeColDialog.createInputDialog(text, defaultValue, okText, cancelText);
        showSubPanel(inputDialog);

        String response = (String) inputDialog.getResponse();

        // checks if the user entered some text.
        while (rejectEmptyString && (response != null) && (response.length() == 0)) {
            String okTxt = "ok";
            String txt = "enterSomeText";
            try {
                okTxt = Messages.message(okTxt);
                txt = Messages.message(txt);
            } catch (MissingResourceException e) {
                logger.warning("could not find message with id: " + txt + " or " + okTxt + ".");
            }
            remove(inputDialog);

            showFreeColDialog(new InformationDialog(this, txt, null));

            showSubPanel(inputDialog);
            response = (String) inputDialog.getResponse();
        }

        remove(inputDialog);

        return response;
    }

    /**
     * Displays a dialog with a text and a cancel-button, in addition to buttons
     * for each of the objects returned for the given <code>Iterator</code>.
     * 
     * @param text The text that explains the choice for the user.
     * @param cancelText The text displayed on the "cancel"-button.
     * @param choices The <code>List</code> containing the ChoiceItems to
     *            create buttons for.
     * @return The chosen object, or <i>null</i> for the cancel-button.
     */
    public <T> T showChoiceDialog(String text, String cancelText, List<ChoiceItem<T>> choices) {

        FreeColDialog<ChoiceItem<T>> choiceDialog = FreeColDialog.createChoiceDialog(text, cancelText, choices);
        if (choiceDialog.getHeight() > getHeight() / 3) {
            choiceDialog.setSize(choiceDialog.getWidth(), (getHeight() * 2) / 3);
        }
        ChoiceItem<T> response = showFreeColDialog(choiceDialog);
        return (response == null) ? null : response.getObject();
    }

    /**
     * Displays a dialog with a text and a cancel-button, in addition to buttons
     * for each of the objects in the array.
     * 
     * @param text The text that explains the choice for the user.
     * @param cancelText The text displayed on the "cancel"-button.
     * @param objects The List containing the objects to create buttons for.
     * @return The chosen object, or <i>null</i> for the cancel-button.
     */
    public <T> T showSimpleChoiceDialog(String text, String cancelText, List<T> objects) {

        List<ChoiceItem<T>> choices = new ArrayList<ChoiceItem<T>>();
        for (T object : objects) {
            choices.add(new ChoiceItem<T>(object));
        }

        return showChoiceDialog(text, cancelText, choices);
    }

    /**
     * Shows a status message that cannot be dismissed. The panel will be
     * removed when another component is added to this <code>Canvas</code>.
     * This includes all the <code>showXXX</code>-methods. In addition,
     * {@link #closeStatusPanel()} also removes this panel.
     * 
     * @param message The text message to display on the status panel.
     * @see StatusPanel
     */
    public void showStatusPanel(String message) {
        statusPanel.setStatusMessage(message);
        addCentered(statusPanel, STATUS_LAYER);
    }

    /**
     * Closes the <code>StatusPanel</code>.
     * 
     * @see #showStatusPanel
     */
    public void closeStatusPanel() {
        if (statusPanel.isVisible()) {
            remove(statusPanel);
        }
    }

    /**
     * Displays the <code>EuropePanel</code>.
     * 
     * @see EuropePanel
     */
    public void showEuropePanel() {
        closeMenus();

        if (freeColClient.getGame() == null) {
            errorMessage("europe.noGame");
        } else {
            europePanel.initialize(freeColClient.getMyPlayer().getEurope(), freeColClient.getGame());
            JInternalFrame f = addAsSimpleFrame(europePanel);
            f.setBorder(null);
            f.setSize(getWidth(), getHeight());
            f.setLocation(0, 0);
            f.moveToBack();
        }
    }

    /**
     * Displays one of the Europe Dialogs for Recruit, Purchase, Train.
     * Closes any currently open Dialogs. 
     * Does not return from this method before the panel is closed.
     *    
     * @param europeAction the type of panel to display
     * @return <code>FreeColDialog.getResponseInt</code>.
     */
    public int showEuropeDialog(EuropePanel.EuropeAction europeAction) {
        // Close any open Europe Dialog (Recruit, Purchase, Train)       
        try {
            if (europeOpenDialog != null) {
                europeOpenDialog.setResponse(new Integer(-1));
            }
        } catch (NumberFormatException e) {
            logger.warning("Canvas.showEuropeDialog: Invalid europeDialogType");
        }
        
        FreeColDialog<Integer> localDialog = null;

        // Open new Dialog
        switch (europeAction) {
        case EXIT:
        case UNLOAD:
        case SAIL:
            return -1;
        case RECRUIT:
            localDialog = new RecruitDialog(this);
            break;
        case PURCHASE:
        case TRAIN:
            localDialog = new TrainDialog(this, europeAction);
            break;
        }
        localDialog.initialize();
        europeOpenDialog = localDialog; // Set the open dialog to the class variable
        
        int response = showFreeColDialog(localDialog);

        if (europeOpenDialog == localDialog) {
            europeOpenDialog = null;    // Clear class variable when it's closed
        }
        
        return response;
    }

    /**
     * Displays the colony panel of the given <code>Colony</code>.
     * 
     * @param colony The colony whose panel needs to be displayed.
     * @see ColonyPanel
     */
    public void showColonyPanel(Colony colony) {
        ColonyPanel panel = new ColonyPanel(this, colony);
        showSubPanel(panel, getPopupPosition(colony.getTile()));
    }

    /**
     * Displays the panel of the given native settlement.
     *
     * @param indianSettlement The <code>IndianSettlement</code> to display.
     */
    public void showIndianSettlementPanel(IndianSettlement indianSettlement) {
        IndianSettlementPanel panel
            = new IndianSettlementPanel(this, indianSettlement);
        showSubPanel(panel, getPopupPosition(indianSettlement.getTile()));
    }

    /**
     * Displays the panel for trading with an <code>IndianSettlement</code>.
     * 
     * @param settlement The native settlement to trade with.
     * @param showBuy Show a "buy" option.
     * @param showSell Show a "sell" option.
     * @param showGift Show a "gift" option.
     */
    public TradeAction showIndianSettlementTradeDialog(Settlement settlement,
                                                       boolean showBuy, boolean showSell, boolean showGift) {
        ArrayList<ChoiceItem<TradeAction>> choices = new ArrayList<ChoiceItem<TradeAction>>();
        if (showBuy){
            choices.add(new ChoiceItem<TradeAction>(Messages.message("tradeProposition.toBuy"),
                                                    TradeAction.BUY));
        }
        if (showSell){
            choices.add(new ChoiceItem<TradeAction>(Messages.message("tradeProposition.toSell"),
                                                    TradeAction.SELL));
        }
        if (showGift){
            choices.add(new ChoiceItem<TradeAction>(Messages.message("tradeProposition.toGift"),
                                                    TradeAction.GIFT));
        }
        
        String prompt = Messages.message("tradeProposition.welcome",
                                         "%nation%", settlement.getOwner().getNationAsString(),
                                         "%settlement%", settlement.getName()); 
        TradeAction choice = showChoiceDialog(prompt,
                                              Messages.message("tradeProposition.cancel"), 
                                              choices);
        
        return choice;
    }

    /**
     * Shows the panel that allows the user to choose which unit will emigrate
     * from Europe. This method may only be called if the user has William
     * Brewster in congress.
     * 
     * @param fountainOfYouth a <code>boolean</code> value
     * @return The emigrant that was chosen by the user.
     */
    public int showEmigrationPanel(boolean fountainOfYouth) {
        EmigrationPanel emigrationPanel = new EmigrationPanel(this);
        emigrationPanel.initialize(freeColClient.getMyPlayer().getEurope(), fountainOfYouth);
        return showFreeColDialog(emigrationPanel);
    }

    /**
     * Removes the given component from this Container.
     * 
     * @param comp The component to remove from this Container.
     */
    @Override
    public void remove(Component comp) {
        remove(comp, true);
    }

    /**
     * Gets the internal frame for the given component.
     * 
     * @param c The component.
     * @return The given component if this is an internal frame or the first
     *         parent that is an internal frame. Returns <code>null</code> if
     *         no internal frame is found.
     */
    private JInternalFrame getInternalFrame(final Component c) {
        Component temp = c;

        while (temp != null && !(temp instanceof JInternalFrame)) {
            temp = temp.getParent();
        }
        return (JInternalFrame) temp;
    }

    /**
     * Removes the given component from this Container.
     * 
     * @param comp The component to remove from this Container.
     * @param update The <code>Canvas</code> will be enabled, the graphics
     *            repainted and both the menubar and the actions will be updated
     *            if this parameter is <code>true</code>.
     */
    public void remove(Component comp, boolean update) {
        if (comp == null) {
            return;
        } else if (comp instanceof FreeColPanel) {
            ((FreeColPanel) comp).setSavedSize(comp.getSize());
        }

        final Rectangle updateBounds = comp.getBounds();
        final JInternalFrame frame = getInternalFrame(comp);
        if (frame != null && frame != comp) {
            // updateBounds = frame.getBounds();
            frame.dispose();
        } else {
            super.remove(comp);
        }

        repaint(updateBounds.x, updateBounds.y, updateBounds.width, updateBounds.height);

        final boolean takeFocus = (comp != statusPanel);
        if (update) {
            getClient().updateMenuBar();
            freeColClient.getActionManager().update();
            if (takeFocus && !isShowingSubPanel()) {
                takeFocus();
            }
        }
    }

    /**
     * Adds a component to this Canvas.
     * 
     * @param comp The component to add
     * @return The component argument.
     */
    @Override
    public Component add(Component comp) {
        add(comp, null);
        return comp;
    }

    /**
     * Adds a component centered on this Canvas. Removes the statuspanel if
     * visible (and <code>comp != statusPanel</code>).
     * 
     * @param comp The component to add
     * @return The component argument.
     */
    public Component addCentered(Component comp) {
        addCentered(comp, null);
        return comp;
    }

    /**
     * Adds a component centered on this Canvas inside a frame. Removes the
     * statuspanel if visible (and <code>comp != statusPanel</code>).
     * 
     * @param comp The component to add to this JInternalFrame.
     * @return The <code>JInternalFrame</code> that was created and added.
     */
    public JInternalFrame addAsFrame(JComponent comp) {
        return addAsFrame(comp, false);
    }
    
    /**
     * Adds a component centered on this Canvas inside a frame.
     * The frame is considered as a tool box (not counted as a frame
     * by the methods deciding if a panel is being displayed).
     * 
     * <br><br>
     * 
     * Removes the statuspanel if visible (and
     * <code>comp != statusPanel</code>).
     * 
     * @param comp The component to add to this JInternalFrame.
     * @return The <code>JInternalFrame</code> that was created and added.
     */
    public JInternalFrame addAsToolBox(JComponent comp) {
        return addAsFrame(comp, true);
    }
    
    /**
     * Adds a component centered on this Canvas inside a frame. Removes the
     * statuspanel if visible (and <code>comp != statusPanel</code>).
     * 
     * @param comp The component to add to this ToEuropePanel.
     * @param toolBox Should be set to true if the resulting frame
     *      is used as a toolbox (that is: it should not be counted
     *      as a frame).
     * @return The <code>JInternalFrame</code> that was created and added.
     */
    private JInternalFrame addAsFrame(JComponent comp, boolean toolBox) {
        return addAsFrame(comp, toolBox, PopupPosition.CENTERED);
    }

    /**
     * Adds a component on this Canvas inside a frame. Removes the
     * statuspanel if visible (and <code>comp != statusPanel</code>).
     * 
     * @param comp The component to add to this ToEuropePanel.
     * @param toolBox Should be set to true if the resulting frame
     *      is used as a toolbox (that is: it should not be counted
     *      as a frame).
     * @param centered a <code>boolean</code> value
     * @return The <code>JInternalFrame</code> that was created and added.
     */
    private JInternalFrame addAsFrame(JComponent comp, boolean toolBox, PopupPosition popupPosition) {
        final int FRAME_EMPTY_SPACE = 60;

        final JInternalFrame f = (toolBox) ? new ToolBoxFrame() : new JInternalFrame();
        if (f.getContentPane() instanceof JComponent) {
            JComponent c = (JComponent) f.getContentPane();
            c.setOpaque(false);
            c.setBorder(null);
        }

        if (comp.getBorder() != null) {
            if (comp.getBorder() instanceof EmptyBorder) {
                f.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            } else {
                f.setBorder(comp.getBorder());
                comp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            }
        } else {
            f.setBorder(null);
        }

        final FrameMotionListener fml = new FrameMotionListener(f);
        comp.addMouseMotionListener(fml);
        comp.addMouseListener(fml);
        if (f.getUI() instanceof BasicInternalFrameUI) {
            BasicInternalFrameUI biu = (BasicInternalFrameUI) f.getUI();
            biu.setNorthPane(null);
            biu.setSouthPane(null);
            biu.setWestPane(null);
            biu.setEastPane(null);
        }

        f.getContentPane().add(comp);
        f.setOpaque(false);
        f.pack();
        int width = f.getWidth();
        int height = f.getHeight();
        if (width > getWidth() - FRAME_EMPTY_SPACE) {
            width = Math.min(width, getWidth());
        }
        if (height > getHeight() - FRAME_EMPTY_SPACE) {
            height = Math.min(height, getHeight());
        }
        f.setSize(width, height);
        switch (popupPosition) {
        case CENTERED:
            f.setLocation((getWidth() - f.getWidth()) / 2,
                         (getHeight() - f.getHeight()) / 2);
            break;
        case CENTERED_LEFT:
            f.setLocation((getWidth() - f.getWidth()) / 4,
                          (getHeight() - f.getHeight()) / 2);
            break;
        case CENTERED_RIGHT:
            f.setLocation(((getWidth() - f.getWidth()) * 3) / 4,
                          (getHeight() - f.getHeight()) / 2);
            break;
        case ORIGIN:
        default:
            f.setLocation(0, 0);
            break;
        }
        add(f, MODAL_LAYER);
        f.setName(comp.getClass().getSimpleName());

        f.setFrameIcon(null);
        f.setVisible(true);
        f.setResizable(true);
        try {
            f.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
        }

        return f;
    }

    /**
     * Adds a component centered on this Canvas inside a frame. Removes the
     * statuspanel if visible (and <code>comp != statusPanel</code>).
     * 
     * The frame cannot be moved or resized.
     * 
     * @param comp The component to add to this ToEuropePanel.
     * @return The <code>JInternalFrame</code> that was created and added.
     */
    public JInternalFrame addAsSimpleFrame(JComponent comp) {
        final JInternalFrame f = new JInternalFrame();
        JScrollPane scrollPane =
            new JScrollPane(comp, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, 
                            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        f.getContentPane().add(scrollPane);
        f.pack();
        addCentered(f);
        f.setName(comp.getClass().getSimpleName());

        if (f.getUI() instanceof BasicInternalFrameUI) {
            BasicInternalFrameUI biu = (BasicInternalFrameUI) f.getUI();
            biu.setNorthPane(null);
        }

        f.setFrameIcon(null);
        f.setVisible(true);
        f.setResizable(false);
        try {
            f.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
        }

        return f;
    }

    /**
     * Removes the mouse listeners for moving the frame of the given component.
     * 
     * @param c The component the listeners should be removed from.
     */
    public void deactivateMovable(JComponent c) {
        for (MouseListener ml : c.getMouseListeners()) {
            if (ml instanceof FrameMotionListener) {
                c.removeMouseListener(ml);
            }
        }
        for (MouseMotionListener ml : c.getMouseMotionListeners()) {
            if (ml instanceof FrameMotionListener) {
                c.removeMouseMotionListener(ml);
            }
        }
    }

    /**
     * Adds a component centered on this Canvas. Removes the statuspanel if
     * visible (and <code>comp != statusPanel</code>).
     * 
     * @param comp The component to add to this ToEuropePanel.
     * @param i The layer to add the component to (see JLayeredPane).
     */
    public void addCentered(Component comp, Integer i) {
        comp.setLocation((getWidth() - comp.getWidth()) / 2,
                         (getHeight() - comp.getHeight()) / 2);

        add(comp, i);
    }

    /**
     * Adds a component to this Canvas. Removes the statuspanel if visible (and
     * <code>comp != statusPanel</code>).
     * 
     * @param comp The component to add to this ToEuropePanel.
     * @param i The layer to add the component to (see JLayeredPane).
     */
    public void add(Component comp, Integer i) {
        add(comp, i, true);
    }

    /**
     * Adds a component to this Canvas. Removes the statuspanel if visible (and
     * <code>comp != statusPanel</code>).
     * 
     * @param comp The component to add to this ToEuropePanel.
     * @param i The layer to add the component to (see JLayeredPane).
     */
    public void add(Component comp, Integer i, boolean update) {
        
        if (comp != statusPanel && !(comp instanceof JMenuItem) && !(comp instanceof FreeColDialog)
                && statusPanel.isVisible()) {
            remove(statusPanel, false);
        }

        try {
            if (i == null) {
                super.add(comp);
            } else {
                super.add(comp, i);
            }
        } catch(Exception e) {
            logger.warning("add component failed with layer " + i);
        }

        if (update) {
            getClient().updateMenuBar();
            freeColClient.getActionManager().update();
        }
    }

    /**
     * Makes sure that this Canvas takes the focus. It will keep on trying for a
     * while even its request doesn't get granted immediately.
     */
    private void takeFocus() {
        if (!isShowingSubPanel()) {
            requestFocus();
        }
    }

    /**
     * Gets the <code>EuropePanel</code>.
     * 
     * @return The <code>EuropePanel</code>.
     */
    public EuropePanel getEuropePanel() {
        return europePanel;
    }

    /**
     * Returns the MapControls of this Canvas.
     *
     * @return a <code>MapControls</code> value
     */
    public MapControls getMapControls() {
        return mapControls;
    }

    /**
     * Shows the given popup at the given position on the screen.
     * 
     * @param popup The JPopupMenu to show.
     * @param x The x-coordinate at which to show the popup.
     * @param y The y-coordinate at which to show the popup.
     */
    public void showPopup(JPopupMenu popup, int x, int y) {
        // closeMenus();
        popup.show(this, x, y);
        popup.repaint();
    }

    /**
     * Shows a tile popup.
     * 
     * @param pos The coordinates of the Tile where the popup occurred.
     * @param x The x-coordinate on the screen where the popup needs to be
     *            placed.
     * @param y The y-coordinate on the screen where the popup needs to be
     *            placed.
     * @see TilePopup
     */
    public void showTilePopup(Map.Position pos, int x, int y) {
        if (pos != null) {
            Tile t = freeColClient.getGame().getMap().getTile(pos.getX(), pos.getY());

            if (t != null) {
                TilePopup tp = new TilePopup(t, freeColClient, this, getGUI());
                if (tp.hasItem()) {
                    showPopup(tp, x, y);
                } else if (t.isExplored()) {
                    showPanel(new TilePanel(this, t));
                }
            }
        }
    }

    /**
     * Displays an error message.
     * 
     * @param messageID The i18n-keyname of the error message to display.
     */
    public void errorMessage(String messageID) {
        errorMessage(messageID, "Unspecified error: " + messageID);
    }

    /**
     * Displays an error message.
     * 
     * @param messageID The i18n-keyname of the error message to display.
     * @param message An alternative message to display if the resource specified
     *            by <code>messageID</code> is unavailable.
     */
    public void errorMessage(String messageID, String message) {
        if (messageID != null) {
            try {
                message = Messages.message(messageID);
            } catch (MissingResourceException e) {
                logger.warning("could not find message with id: " + messageID);
            }
        }

        ErrorPanel errorPanel = new ErrorPanel(this);
        errorPanel.initialize(message);
        showFreeColDialog(errorPanel);
    }

    /**
     * Shows a message with some information and an "OK"-button.
     * 
     * @param messageId The messageId of the message to display.
     */
    public void showInformationMessage(String messageId) {
        showInformationMessage(messageId, null, new String[0]);
    }
    
    /**
     * Shows a message with some information and an "OK"-button.
     * 
     * @param messageId The messageId of the message to display.
     * @param displayObject Optional object for displaying an icon
     */
    public void showInformationMessage(String messageId, FreeColObject displayObject) {
        showInformationMessage(messageId, displayObject, new String[0]);
    }

    
    /**
     * Shows a message with some information and an "OK"-button.
     * 
     * <br>
     * <br>
     * <b>Example:</b> <br>
     * <code>canvas.showInformationMessage("noNeedForTheGoods", "%goods%", goods.getName());</code>
     * 
     * @param messageId The messageId of the message to display.
     * @param replace All occurrences of <code>replace[2x]</code> in the
     *            message gets replaced by <code>replace[2x+1]</code>.
     */
    public void showInformationMessage(String messageId, String... replace) {
        showInformationMessage(messageId, null, replace);
    }
    
    /**
     * Shows a message with some information and an "OK"-button.
     *
     * <b>Example:</b> <br>
     * <code>canvas.showInformationMessage("noNeedForTheGoods", "%goods%", goods.getName());</code>
     *
     * @param messageId The messageId of the message to display.
     * @param replace All occurrences of <code>replace[2x]</code> in the
     *            message gets replaced by <code>replace[2x+1]</code>.
     * @param displayObject Optional object for displaying an icon
     */
    public void showInformationMessage(String messageId, FreeColObject displayObject, String... replace) {
        String text;
        try {
            text = Messages.message(messageId, replace);
        } catch (MissingResourceException e) {
            text = messageId;
            logger.warning("Missing i18n resource: " + messageId);
        }
        ImageIcon icon = null;
        if (displayObject != null) {
            icon = getImageIcon(displayObject, false);
        }
        Tile tile = null;
        if (displayObject instanceof Tile) {
            tile = (Tile) displayObject;
        } else {
            try { // If the displayObject has a "getTile" method, invoke it.
                tile = (Tile) displayObject.getClass().getMethod("getTile")
                    .invoke(displayObject);
            } catch (Exception e) { /* Ignore failure */ }
        }
        showFreeColDialog(new InformationDialog(this, text, icon), tile);
    }

    /**
     * Refreshes this Canvas visually.
     */
    public void refresh() {
        gui.forceReposition();
        repaint(0, 0, getWidth(), getHeight());
    }

    /**
     * Refreshes the screen at the specified Tile.
     * 
     * @param x The x-coordinate of the Tile to refresh.
     * @param y The y-coordinate of the Tile to refresh.
     */
    public void refreshTile(int x, int y) {
        if (x >= 0 && y >= 0) {
            repaint(gui.getTileBounds(x, y));
        }
    }

    /**
     * Refreshes the screen at the specified Tile.
     * 
     * @param t The tile to refresh.
     */
    public void refreshTile(Tile t) {
        refreshTile(t.getX(), t.getY());
    }

    /**
     * Refreshes the screen at the specified Tile.
     * 
     * @param p The position of the tile to refresh.
     */
    public void refreshTile(Position p) {
        refreshTile(p.getX(), p.getY());
    }

    /**
     * Closes all the menus that are currently open.
     */
    public void closeMenus() {
        for (JInternalFrame frame : getAllFrames()) {
            frame.dispose();
        }
    }

    /**
     * Shows the <code>MainPanel</code>.
     * 
     * @see MainPanel
     */
    public void showMainPanel() {
        closeMenus();
        freeColClient.getFrame().setJMenuBar(null);
        addCentered(mainPanel, MAIN_LAYER);
        mainPanel.requestFocus();
    }
    
    /**
     * Shows the <code>VideoPanel</code>.
     */
    public void showOpeningVideoPanel() {
        closeMenus();
        final Video video = ResourceManager.getVideo("Opening.video");
        boolean muteAudio = !getClient().canPlayMusic();
        final VideoComponent vp = new VideoComponent(video, muteAudio);
        addCentered(vp, MAIN_LAYER);
        vp.play();
        
        final class AbortListener implements KeyListener, MouseListener, VideoListener {
            public void stopped() {
                execute();
            }
            
            public void keyPressed(KeyEvent e) {
                execute();
            }

            public void mouseClicked(MouseEvent e) {
                execute();
            }
            
            private void execute() {
                removeKeyListener(this);
                removeMouseListener(this);
                vp.removeMouseListener(this);
                vp.removeVideoListener(this);
                vp.stop();
                Canvas.this.remove(vp);
                showMainPanel();
                freeColClient.playMusic("intro");
            }

            public void keyReleased(KeyEvent e) {}
            public void keyTyped(KeyEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
        };
        final AbortListener l = new AbortListener();
        addMouseListener(l);
        addKeyListener(l);
        vp.addMouseListener(l);
        vp.addVideoListener(l);
    }

    /**
     * Gets the <code>MainPanel</code>.
     * 
     * @return The <code>MainPanel</code>.
     * @see MainPanel
     */
    public MainPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * Closes the {@link MainPanel}.
     */
    public void closeMainPanel() {
        remove(mainPanel);
    }

    /**
     * Shows the <code>OpenGamePanel</code>.
     */
    public void showOpenGamePanel() {
        errorMessage("openGame.unimplemented");
    }

    /**
     * Gets the <code>StartGamePanel</code> that lies in this container.
     * 
     * @return The <code>StartGamePanel</code>.
     * @see StartGamePanel
     */
    public StartGamePanel getStartGamePanel() {
        return startGamePanel;
    }

    /**
     * Tells the map controls that a chat message was received.
     * 
     * @param sender The player who sent the chat message to the server.
     * @param message The chat message.
     * @param privateChat 'true' if the message is a private one, 'false'
     *            otherwise.
     * @see GUIMessage
     */
    public void displayChatMessage(Player sender, String message, boolean privateChat) {
        gui.addMessage(new GUIMessage(sender.getName() + ": " + message, sender.getColor()));
    }

    /**
     * Displays a chat message originating from this client.
     * 
     * @param message The chat message.
     */
    public void displayChatMessage(String message) {
        displayChatMessage(freeColClient.getMyPlayer(), message, false);
    }

    /**
     * Quits the application. This method uses {@link #confirmQuitDialog()} in
     * order to get a "Are you sure"-confirmation from the user.
     */
    public void quit() {
        if (showConfirmDialog("quitDialog.areYouSure.text", "ok", "cancel")) {
            freeColClient.quit();
        }
    }

    /**
     * Quits the application. This method uses {@link #confirmQuitDialog()} in
     * order to get a "Are you sure"-confirmation from the user.
     */
    public void retire() {
        if (showConfirmDialog("retireDialog.areYouSure.text",
                              "ok", "cancel")) {
            if (freeColClient.retire()) {
                showPanel(new ReportHighScoresPanel(this));
            }
            // TODO: quit as soon as panel has been closed
            //freeColClient.quit();
            quit();
        }
    }

    /**
     * Closes all panels, changes the background and shows the main menu.
     */
    public void returnToTitle() {
        // TODO: check if the GUI object knows that we're not
        // inGame. (Retrieve value of GUI::inGame.)  If GUI thinks
        // we're still in the game then log an error because at this
        // point the GUI should have been informed.
        closeMenus();
        removeInGameComponents();
        showMainPanel();
        repaint();
    }

    /**
     * Removes components that is only used when in game.
     */
    public void removeInGameComponents() {
        // remove listeners, they will be added when launching the new game...
        KeyListener[] keyListeners = getKeyListeners();
        for (int i = 0; i < keyListeners.length; ++i) {
            removeKeyListener(keyListeners[i]);
        }

        MouseListener[] mouseListeners = getMouseListeners();
        for (int i = 0; i < mouseListeners.length; ++i) {
            removeMouseListener(mouseListeners[i]);
        }

        MouseMotionListener[] mouseMotionListeners = getMouseMotionListeners();
        for (int i = 0; i < mouseMotionListeners.length; ++i) {
            removeMouseMotionListener(mouseMotionListeners[i]);
        }
        
        // change to default view mode
        // Must be done before removing jMenuBar to prevent exception (crash)
        getGUI().getViewMode().changeViewMode(ViewMode.MOVE_UNITS_MODE);

        for (Component c : getComponents()) {
            remove(c, false);
        }
        
    }

    /**
     * Checks if this <code>Canvas</code> contains any ingame components.
     * 
     * @return <code>true</code> if there is a single ingame component.
     */
    public boolean containsInGameComponents() {
        KeyListener[] keyListeners = getKeyListeners();
        if (keyListeners.length > 0) {
            return true;
        }

        MouseListener[] mouseListeners = getMouseListeners();
        if (mouseListeners.length > 0) {
            return true;
        }

        MouseMotionListener[] mouseMotionListeners = getMouseMotionListeners();
        if (mouseMotionListeners.length > 0) {
            return true;
        }

        return false;
    }

    /**
     * Returns this <code>Canvas</code>'s <code>GUI</code>.
     * 
     * @return The <code>GUI</code>.
     */
    public GUI getGUI() {
        return gui;
    }

    /**
     * Returns the freeColClient.
     * 
     * @return The <code>freeColClient</code> associated with this
     *         <code>Canvas</code>.
     */
    public FreeColClient getClient() {
        return freeColClient;
    }

    /**
     * Displays a quit dialog and, if desired, logs out of the current game and
     * shows the new game panel.
     */
    public void newGame() {
        if (!showConfirmDialog("stopCurrentGame.text",
                               "stopCurrentGame.yes", "stopCurrentGame.no")) {
            return;
        }

        freeColClient.getConnectController().quitGame(true);
        removeInGameComponents();
        showPanel(new NewPanel(this));
    }

    /**
     * A class for frames being used as tool boxes.
     */
    class ToolBoxFrame extends JInternalFrame {
        
    }

    /**
     * Handles the moving of internal frames.
     */
    class FrameMotionListener extends MouseAdapter implements MouseMotionListener {

        private JInternalFrame f;

        private Point loc = null;


        FrameMotionListener(JInternalFrame f) {
            this.f = f;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (f.getDesktopPane() == null || f.getDesktopPane().getDesktopManager() == null) {
                return;
            }
            loc = SwingUtilities.convertPoint((Component) e.getSource(), e.getX(), e.getY(), null);
            f.getDesktopPane().getDesktopManager().beginDraggingFrame(f);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (loc == null || f.getDesktopPane() == null || f.getDesktopPane().getDesktopManager() == null) {
                return;
            }
            f.getDesktopPane().getDesktopManager().endDraggingFrame(f);
        }

        public void mouseDragged(MouseEvent e) {
            if (loc == null || f.getDesktopPane() == null || f.getDesktopPane().getDesktopManager() == null) {
                return;
            }

            Point p = SwingUtilities.convertPoint((Component) e.getSource(), e.getX(), e.getY(), null);
            int moveX = loc.x - p.x;
            int moveY = loc.y - p.y;
            f.getDesktopPane().getDesktopManager().dragFrame(f, f.getX() - moveX, f.getY() - moveY);
            loc = p;
        }

        public void mouseMoved(MouseEvent arg0) {
        }
    }
}
