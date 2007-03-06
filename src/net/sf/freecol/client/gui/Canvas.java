
package net.sf.freecol.client.gui;


import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileFilter;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChatPanel;
import net.sf.freecol.client.gui.panel.ChooseFoundingFatherDialog;
import net.sf.freecol.client.gui.panel.ClientOptionsDialog;
import net.sf.freecol.client.gui.panel.ColonyPanel;
import net.sf.freecol.client.gui.panel.ColopediaPanel;
import net.sf.freecol.client.gui.panel.DeclarationDialog;
import net.sf.freecol.client.gui.panel.EmigrationPanel;
import net.sf.freecol.client.gui.panel.ErrorPanel;
import net.sf.freecol.client.gui.panel.EuropePanel;
import net.sf.freecol.client.gui.panel.EventPanel;
import net.sf.freecol.client.gui.panel.FreeColDialog;
import net.sf.freecol.client.gui.panel.FreeColPanel;
import net.sf.freecol.client.gui.panel.GameOptionsDialog;
import net.sf.freecol.client.gui.panel.ImageProvider;
import net.sf.freecol.client.gui.panel.IndianSettlementPanel;
import net.sf.freecol.client.gui.panel.InfoPanel;
import net.sf.freecol.client.gui.panel.LoadingSavegameDialog;
import net.sf.freecol.client.gui.panel.MainPanel;
import net.sf.freecol.client.gui.panel.MapGeneratorOptionsDialog;
import net.sf.freecol.client.gui.panel.MonarchPanel;
import net.sf.freecol.client.gui.panel.NewPanel;
import net.sf.freecol.client.gui.panel.PurchaseDialog;
import net.sf.freecol.client.gui.panel.QuitDialog;
import net.sf.freecol.client.gui.panel.RecruitDialog;
import net.sf.freecol.client.gui.panel.ReportColonyPanel;
import net.sf.freecol.client.gui.panel.ReportContinentalCongressPanel;
import net.sf.freecol.client.gui.panel.ReportForeignAffairPanel;
import net.sf.freecol.client.gui.panel.ReportIndianPanel;
import net.sf.freecol.client.gui.panel.ReportLabourPanel;
import net.sf.freecol.client.gui.panel.ReportMilitaryPanel;
import net.sf.freecol.client.gui.panel.ReportNavalPanel;
import net.sf.freecol.client.gui.panel.ReportPanel;
import net.sf.freecol.client.gui.panel.ReportReligiousPanel;
import net.sf.freecol.client.gui.panel.ReportTradePanel;
import net.sf.freecol.client.gui.panel.ReportTurnPanel;
import net.sf.freecol.client.gui.panel.ServerListPanel;
import net.sf.freecol.client.gui.panel.StartGamePanel;
import net.sf.freecol.client.gui.panel.StatusPanel;
import net.sf.freecol.client.gui.panel.TilePanel;
import net.sf.freecol.client.gui.panel.TradeRouteDialog;
import net.sf.freecol.client.gui.panel.TradeRouteInputDialog;
import net.sf.freecol.client.gui.panel.TrainDialog;
import net.sf.freecol.client.gui.panel.VictoryPanel;
import net.sf.freecol.client.gui.panel.WarehouseDialog;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.model.Map.Position;


/**
* The main container for the other GUI components in FreeCol.
* This container is where the panels, dialogs and menus are added.
* In addition, this is the component in which the map graphics are displayed.
*
* <br><br>
*
* <br><b>Displaying panels and a dialogs</b>
* <br><br>
*
* <code>Canvas</code> contains  methods to display various panels
* and dialogs. Most of these methods use
* {@link net.sf.freecol.client.gui.i18n i18n} to get localized
* text. Here is an example:
*
* <br>
*
* <PRE>
* if (canvas.showConfirmDialog("choice.text", "choice.yes", "choice.no")) {
*     // DO SOMETHING.
* }
* </PRE>
*
* <br>
*
* where "choice.text", "choice.yes" and "choice.no" are keys for a localized
* message. See {@link net.sf.freecol.client.gui.i18n i18n} for more
* information.
*
* <br><br>
*
* <br><b>The difference between a panel and a dialog</b>
* <br><br>
*
* When displaying a dialog, using a <code>showXXXDialog</code>, the calling thread
* will wait until that dialog is dismissed before returning. In contrast, a
* <code>showXXXPanel</code>-method returns immediatly.
*/
public final class Canvas extends JLayeredPane {
    private static final Logger logger = Logger.getLogger(Canvas.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final Integer START_GAME_LAYER = DEFAULT_LAYER,
                                SERVER_LIST_LAYER = DEFAULT_LAYER,
                                VICTORY_LAYER = DEFAULT_LAYER,
                                CHAT_LAYER = DEFAULT_LAYER,
                                NEW_GAME_LAYER = DEFAULT_LAYER,
                                MODEL_MESSAGE_LAYER = new Integer(DEFAULT_LAYER.intValue() + 11),
                                CONFIRM_LAYER = new Integer(DEFAULT_LAYER.intValue() + 12),
                                GAME_OPTIONS_LAYER = new Integer(DEFAULT_LAYER.intValue() + 9),
                                CLIENT_OPTIONS_LAYER = new Integer(DEFAULT_LAYER.intValue() + 9),
                                LOADING_SAVEGAME_LAYER = new Integer(DEFAULT_LAYER.intValue() + 11),
                                LOAD_LAYER = new Integer(DEFAULT_LAYER.intValue() + 10),
                                SAVE_LAYER = new Integer(DEFAULT_LAYER.intValue() + 10),
                                ARMED_UNIT_INDIAN_SETTLEMENT_LAYER = new Integer(DEFAULT_LAYER.intValue() + 9),
                                SCOUT_INDIAN_SETTLEMENT_LAYER = new Integer(DEFAULT_LAYER.intValue() + 9),
                                USE_MISSIONARY_LAYER = new Integer(DEFAULT_LAYER.intValue() + 9),
                                INCITE_LAYER = new Integer(DEFAULT_LAYER.intValue() + 9),
                                INPUT_LAYER = new Integer(DEFAULT_LAYER.intValue() + 11),
                                CHOICE_LAYER = new Integer(DEFAULT_LAYER.intValue() + 11),
                                STATUS_LAYER = new Integer(DEFAULT_LAYER.intValue() + 7),
                                COLOPEDIA_LAYER = new Integer(DEFAULT_LAYER.intValue() + 20),
                                REPORT_LAYER = new Integer(DEFAULT_LAYER.intValue() + 1),
                                MAIN_LAYER = DEFAULT_LAYER,
                                QUIT_LAYER = new Integer(DEFAULT_LAYER.intValue() + 15),
                                INFORMATION_LAYER = new Integer(DEFAULT_LAYER.intValue() + 13),
                                ERROR_LAYER = new Integer(DEFAULT_LAYER.intValue() + 13),
                                EMIGRATION_LAYER = new Integer(DEFAULT_LAYER.intValue() + 1),
                                MONARCH_LAYER = new Integer(DEFAULT_LAYER.intValue() + 1),
                                TILE_LAYER = new Integer(DEFAULT_LAYER.intValue() + 1),
                                INDIAN_SETTLEMENT_LAYER = DEFAULT_LAYER,
                                COLONY_LAYER = new Integer(DEFAULT_LAYER.intValue() + 3),
                                WAREHOUSE_LAYER = new Integer(DEFAULT_LAYER.intValue() + 4),
                                EUROPE_LAYER = new Integer(DEFAULT_LAYER.intValue() + 3),
                                EVENT_LAYER = new Integer(DEFAULT_LAYER.intValue() + 1),
                                CHOOSE_FOUNDING_FATHER = new Integer(DEFAULT_LAYER.intValue() + 1);                                
    
    private final FreeColClient     freeColClient;

    private final MainPanel         mainPanel;
    private final NewPanel          newPanel;
    private final ErrorPanel        errorPanel;
    private final StartGamePanel    startGamePanel;
    private final QuitDialog        quitDialog;
    private final ColonyPanel       colonyPanel;
    private final IndianSettlementPanel indianSettlementPanel;
    private final TilePanel         tilePanel;
    private final MonarchPanel      monarchPanel;
    private final EuropePanel       europePanel;
    private final RecruitDialog     recruitDialog;
    private final PurchaseDialog    purchaseDialog;
    private final TrainDialog       trainDialog;
    private final TradeRouteDialog  tradeRouteDialog;
    private final TradeRouteInputDialog  tradeRouteInputDialog;
    private final StatusPanel       statusPanel;
    private final ChatPanel         chatPanel;
    private final GUI               gui;
    private final ChatDisplayThread chatDisplayThread;
    private final VictoryPanel      victoryPanel;
    private final WarehouseDialog   warehouseDialog;
    private final ChooseFoundingFatherDialog chooseFoundingFatherDialog;
    private final EventPanel        eventPanel;
    private final EmigrationPanel   emigrationPanel;
    private final ColopediaPanel    colopediaPanel;
    private final ReportReligiousPanel     reportReligiousPanel;
    private final ReportTradePanel         reportTradePanel;
    private final ReportTurnPanel          reportTurnPanel;
    private final ReportLabourPanel        reportLabourPanel;
    private final ReportColonyPanel        reportColonyPanel;
    private final ReportMilitaryPanel      reportMilitaryPanel;
    private final ReportNavalPanel         reportNavalPanel;
    private final ReportForeignAffairPanel reportForeignAffairPanel;
    private final ReportIndianPanel        reportIndianPanel;
    private final ReportContinentalCongressPanel reportContinentalCongressPanel;
    private final ServerListPanel   serverListPanel;
    private final GameOptionsDialog gameOptionsDialog;
    private final ClientOptionsDialog clientOptionsDialog;
    private final MapGeneratorOptionsDialog mapGeneratorOptionsDialog;
    private final DeclarationDialog declarationDialog;
    private final LoadingSavegameDialog loadingSavegameDialog;
    private TakeFocusThread         takeFocusThread;
    private JMenuBar                jMenuBar;


    /**
    * The constructor to use.
    *
    * @param client main control class.
    * @param bounds The bounds of this <code>Canvas</code>.
    * @param gui The object responsible of drawing the map onto this component.
    */
    public Canvas(FreeColClient client, Rectangle bounds, GUI gui) {
        this.freeColClient = client;
        this.gui = gui;

        setBounds(bounds);

        setOpaque(false);
        setLayout(null);

        takeFocusThread = null;

        mainPanel = new MainPanel(this, freeColClient);
        newPanel = new NewPanel(this, freeColClient.getConnectController());
        errorPanel = new ErrorPanel(this);
        startGamePanel = new StartGamePanel(this, freeColClient);
        serverListPanel = new ServerListPanel(this, freeColClient, freeColClient.getConnectController());
        quitDialog = new QuitDialog(this);
        colonyPanel = new ColonyPanel(this, freeColClient);
        indianSettlementPanel = new IndianSettlementPanel();
        tilePanel = new TilePanel(this);
        monarchPanel = new MonarchPanel(this);
        declarationDialog = new DeclarationDialog(this, freeColClient);

        europePanel = new EuropePanel(this, freeColClient, freeColClient.getInGameController());
        recruitDialog = new RecruitDialog(this);
        purchaseDialog = new PurchaseDialog(this);
        trainDialog = new TrainDialog(this);
        tradeRouteDialog = new TradeRouteDialog(this);
        tradeRouteInputDialog = new TradeRouteInputDialog(this);
        statusPanel = new StatusPanel(this);

        chatPanel = new ChatPanel(this, freeColClient);
        victoryPanel = new VictoryPanel(this, freeColClient);
        warehouseDialog = new WarehouseDialog(this);
        chooseFoundingFatherDialog = new ChooseFoundingFatherDialog(this);
        eventPanel = new EventPanel(this, freeColClient);
        emigrationPanel = new EmigrationPanel();
        colopediaPanel = new ColopediaPanel(this);
        reportReligiousPanel = new ReportReligiousPanel(this);
        reportTradePanel = new ReportTradePanel(this);
        reportTurnPanel = new ReportTurnPanel(this, freeColClient);
        reportLabourPanel = new ReportLabourPanel(this);
        reportColonyPanel = new ReportColonyPanel(this);
        reportMilitaryPanel = new ReportMilitaryPanel(this);
        reportNavalPanel = new ReportNavalPanel(this);
        reportForeignAffairPanel = new ReportForeignAffairPanel(this);
        reportIndianPanel = new ReportIndianPanel(this);
        reportContinentalCongressPanel = new ReportContinentalCongressPanel(this);
        gameOptionsDialog = new GameOptionsDialog(this, freeColClient);
        clientOptionsDialog = new ClientOptionsDialog(this, freeColClient);
        mapGeneratorOptionsDialog = new MapGeneratorOptionsDialog(this, freeColClient);
        loadingSavegameDialog = new LoadingSavegameDialog(this, freeColClient);

        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        //takeFocus();

        chatDisplayThread = new ChatDisplayThread();
        chatDisplayThread.start();

        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new Thread() {
            public void run() {
                freeColClient.getConnectController().quitGame(true);
            }
        });

        logger.info("Canvas created.");
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


    /**
    * Sets the menu bar. The menu bar will be resized to fit the width
    * of the gui and made visible.
    *
    * @param mb The menu bar.
    * @see FreeColMenuBar
    */
    public void setJMenuBar(JMenuBar mb) {
        if (jMenuBar != null) {
            remove(jMenuBar);
        }

        mb.setLocation(0, 0);
        mb.setSize(getWidth(), (int) mb.getPreferredSize().getHeight());
        add(mb);

        jMenuBar = mb;
    }


    /**
    * Gets the menu bar.
    * @return The menu bar.
    * @see FreeColMenuBar
    */
    public JMenuBar getJMenuBar() {
        return jMenuBar;
    }


    /**
    * Updates the label displaying the current amount of gold.
    */
    public void updateGoldLabel() {
        getJMenuBar().repaint();
    }


    /**
    * Paints this component. This method will use
    * {@link GUI#display} to draw the map/background on this component.
    *
    * @param g The Graphics context in which to draw this component.
    * @see GUI#display
    */
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        gui.display(g2d);
    }

    /**
     * Gets the height of the menu bar.
     * @return The menubar + any borders.
     */
    public int getMenuBarHeight() {
        if (jMenuBar == null) {
            return 0;
        } else if (jMenuBar instanceof FreeColMenuBar) {
        	return ((FreeColMenuBar) jMenuBar).getOpaqueHeight();
        } else {
            return jMenuBar.getHeight();
        }
    }

    
    /**
    * Displays the <code>StartGamePanel</code>.
    *
    * @param game The <code>Game</code> that is about to start.
    * @param player The <code>Player</code> using this client.
    * @param singlePlayerMode 'true' if the user wants to start a single player game,
    *        'false' otherwise.
    * @see StartGamePanel
    */
    public void showStartGamePanel(Game game, Player player, boolean singlePlayerMode) {
        closeMenus();

        if (game != null && player != null) {
            startGamePanel.initialize(singlePlayerMode);
            addCentered(startGamePanel, START_GAME_LAYER);
            startGamePanel.requestFocus();
        } else {
            logger.warning("Tried to open 'StartGamePanel' without having 'game' and/or 'player' set.");
        }
    }
    

    /**
    * Displays the <code>ServerListPanel</code>.
    *
    * @param username The username that should be used when connecting
    *        to one of the servers on the list.
    * @param serverList The list containing the servers retrived from the
    *        metaserver.
    * @see ServerListPanel
    */
    public void showServerListPanel(String username, ArrayList serverList) {
        closeMenus();

        serverListPanel.initialize(username, serverList);        
        addCentered(serverListPanel, SERVER_LIST_LAYER);
        serverListPanel.requestFocus();
    }


    /**
    * Displays the <code>VictoryPanel</code>.
    * @see VictoryPanel
    */
    public void showVictoryPanel() {
        closeMenus();
        setEnabled(false);
        addCentered(victoryPanel);
        victoryPanel.requestFocus();
    }

    
    /**
    * Displays the <code>WarehouseDialog</code>.
    * @see WarehouseDialog
    */
    public void showWarehouseDialog(Colony colony) {
        //closeMenus();
        setEnabled(false);
        warehouseDialog.initialize(colony);
        addCentered(warehouseDialog, WAREHOUSE_LAYER);
        warehouseDialog.requestFocus();
    }

    
    /**
    * Displays the <code>ChatPanel</code>.
    * @see ChatPanel
    */
    public void showChatPanel() {
        closeMenus();
        setEnabled(false);
        addCentered(chatPanel);
        chatPanel.requestFocus();
    }


    /**
    * Displays the <code>NewGamePanel</code>.
    * @see NewPanel
    */
    public void showNewGamePanel() {
        closeMenus();
        addCentered(newPanel);
        newPanel.requestFocus();
    }
    

    /**
    * Displays a <code>ModelMessage</code> in a modal dialog.
    * The message is displayed in this way:
    *
    * <ol>
    *   <li>The <code>messageID</code> is used to get the message from
    *       {@link net.sf.freecol.client.gui.i18n.Messages#message(String)}.
    *   <li>Every occuranse of <code>data[x][0]</code> is replaced with
    *       <code>data[x][1]</code> for every <code>x</code>.
    *   <li>The message is displayed using a modal dialog.
    * </ol>
    *
    * A specialized panel may be used. In this case the <code>messageID</code>
    * of the <code>ModelMessage</code> if used as a key for this panel.
    *
    * @param m The <code>ModelMessage</code> to be displayed.
    */
    /*
    public void oldshowModelMessage(ModelMessage m) {
        String okText = "ok";
        String cancelText = "display";
        String message = m.getMessageID();

        if (message.equals("EventPanel.MEETING_EUROPEANS")) {
            // Skip for now:
            //showEventDialog(EventPanel.MEETING_EUROPEANS);
            freeColClient.getInGameController().nextModelMessage();
        } else if (message.equals("EventPanel.MEETING_NATIVES")) {
            // Skip for now:
            //showEventDialog(EventPanel.MEETING_NATIVES);
            freeColClient.getInGameController().nextModelMessage();
        } else if (message.equals("EventPanel.MEETING_AZTEC")) {
            // Skip for now:
            //showEventDialog(EventPanel.MEETING_AZTEC);
            freeColClient.getInGameController().nextModelMessage();
        } else if (message.equals("EventPanel.MEETING_INCA")) {
            // Skip for now:
            //showEventDialog(EventPanel.MEETING_INCA);
            freeColClient.getInGameController().nextModelMessage();
        } else {
            try {
                okText = Messages.message(okText);
                cancelText = Messages.message(cancelText);
                message = Messages.message(message, m.getData());
            } catch (MissingResourceException e) {
                logger.warning("could not find message with id: " + okText + ".");
            }

            FreeColGameObject source = m.getSource();
            if (source instanceof Europe && !europePanel.isShowing() ||
                    (source instanceof Colony || source instanceof WorkLocation) && !colonyPanel.isShowing()) {

                FreeColDialog confirmDialog = FreeColDialog.createConfirmDialog(message, okText, cancelText);
                addCentered(confirmDialog, MODEL_MESSAGE_LAYER);
                confirmDialog.requestFocus();

                if (!confirmDialog.getResponseBoolean()) {
                    remove(confirmDialog);
                    if (source instanceof Europe) {
                        showEuropePanel();
                    } else if (source instanceof Colony) {
                        showColonyPanel((Colony) source);
                    } else if (source instanceof WorkLocation) {
                        showColonyPanel(((WorkLocation) source).getColony());
                    }
                } else {
                    remove(confirmDialog);
                    freeColClient.getInGameController().nextModelMessage();
                }
            } else {
                FreeColDialog informationDialog = null;
                if (m.getTypeOfGoods() < 0) {
                    informationDialog = FreeColDialog.createInformationDialog(message, okText);
                } else {
                    ImageIcon image = getImageProvider().getGoodsImageIcon(m.getTypeOfGoods());
                    informationDialog = FreeColDialog.createInformationDialog(message, okText, image);
                }
        
                addCentered(informationDialog, MODEL_MESSAGE_LAYER);
                informationDialog.requestFocus();

                informationDialog.getResponse();
                remove(informationDialog);

                freeColClient.getInGameController().nextModelMessage();
            }
        }
    }
    */

    /**
     * Displays all model messages for the current turn.
     *
     * @param messages A list of messages to display.
     */
    public void showTurnReport(ArrayList<ModelMessage> messages) {
        reportTurnPanel.initialize(messages);
        setEnabled(false);
        addCentered(reportTurnPanel, REPORT_LAYER);
        reportTurnPanel.requestFocus();
    }


    /**
     * Displays a single ModelMessage.
     *
     * @param modelMessage The message to display.
     */
    public void showModelMessage(ModelMessage modelMessage) {
        showModelMessages(new ModelMessage[] {modelMessage});
    }

    /**
     * Displays a number of ModelMessages.
     *
     * @param modelMessages
     */
    public void showModelMessages(ModelMessage[] modelMessages) {
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
                messageText[i] = Messages.message(modelMessages[i].getMessageID(),
                                                  modelMessages[i].getData());
            } catch (MissingResourceException e) {
                logger.warning("could not find message with id: " + modelMessages[i].getMessageID() + ".");
            }

            messageIcon[i] = getImageIcon(modelMessages[i].getDisplay());
        }

        // source should be the same for all messages
        FreeColGameObject source = modelMessages[0].getSource();
        if ((source instanceof Europe && !europePanel.isShowing()) ||
            (source instanceof Colony || source instanceof WorkLocation) && 
            !colonyPanel.isShowing()) {

            FreeColDialog confirmDialog = FreeColDialog.createConfirmDialog(messageText, messageIcon, okText, cancelText);
            addCentered(confirmDialog, MODEL_MESSAGE_LAYER);
            confirmDialog.requestFocus();

            if (!confirmDialog.getResponseBoolean()) {
                remove(confirmDialog);
                if (source instanceof Europe) {
                    showEuropePanel();
                } else if (source instanceof Colony) {
                    showColonyPanel((Colony) source);
                } else if (source instanceof WorkLocation) {
                    showColonyPanel(((WorkLocation) source).getColony());
                }
            } else {
                remove(confirmDialog);
                if (!getColonyPanel().isShowing() && !getEuropePanel().isShowing())
                    freeColClient.getInGameController().nextModelMessage();
            }
        } else {
            FreeColDialog informationDialog = FreeColDialog.createInformationDialog(messageText, messageIcon);
            addCentered(informationDialog, MODEL_MESSAGE_LAYER);
            informationDialog.requestFocus();

            informationDialog.getResponse();
            remove(informationDialog);

            if (!getColonyPanel().isShowing() && !getEuropePanel().isShowing())
                freeColClient.getInGameController().nextModelMessage();
        }
    }

    /*
     * Returns the appropriate ImageIcon for Object.
     *
     * @param display The Object to display.
     * @return The appropriate ImageIcon.
     */
    public ImageIcon getImageIcon(Object display) {
        ImageLibrary imageLibrary = (ImageLibrary) getImageProvider();
        ImageIcon imageIcon = null;
        if (display == null) {
            return imageIcon;
        } else if (display instanceof Goods) {
            Goods goods = (Goods) display;
            try {
                imageIcon = imageLibrary.getGoodsImageIcon(goods.getType());
            } catch(Exception e) {
                logger.warning("could not find image for goods " + goods.getName());
            }
        } else if (display instanceof Unit) {
            Unit unit = (Unit) display;
            try {
                int unitType = imageLibrary.getUnitGraphicsType(unit);
                imageIcon = imageLibrary.getUnitImageIcon(unitType);
            } catch(Exception e) {
                logger.warning("could not find image for unit " + unit.getName());
            }
        } else if (display instanceof Settlement) {
            Settlement settlement = (Settlement) display;
            try {                
                int settlementType = imageLibrary.getSettlementGraphicsType(settlement);
                imageIcon = new ImageIcon(imageLibrary.getColonyImage(settlementType));
            } catch(Exception e) {
                logger.warning("could not find image for settlement " + settlement);
            }
        } else if (display instanceof LostCityRumour) {
            try {
                imageIcon = new ImageIcon(imageLibrary.getMiscImage(ImageLibrary.LOST_CITY_RUMOUR));
            } catch(Exception e) {
                logger.warning("could not find image for lost city rumour");
            }
        }
        return imageIcon;
    }



    /**
     * Shows the <code>DeclarationDialog</code>.
     * @see DeclarationDialog
     */
     public void showDeclarationDialog() {
         addCentered(declarationDialog, CONFIRM_LAYER);
         declarationDialog.requestFocus();

         declarationDialog.initialize();
         boolean response = declarationDialog.getResponseBoolean();
         remove(declarationDialog);
     }

    /**
    * Displays a dialog with a text and a ok/cancel option.
    *
    * @param text The text that explains the choice for the user.
    * @param okText The text displayed on the "ok"-button.
    * @param cancelText The text displayed on the "cancel"-button.
    * @return <i>true</i> if the user clicked the "ok"-button
    *         and <i>false</i> otherwise.
    * @see FreeColDialog
    */
    public boolean showConfirmDialog(String text, String okText, String cancelText) {
        return showConfirmDialog(text, okText, cancelText, null);
    }


    /**
    * Displays a dialog with a text and a ok/cancel option.
    *
    * @param text The text that explains the choice for the user.
    * @param okText The text displayed on the "ok"-button.
    * @param cancelText The text displayed on the "cancel"-button.
    * @param replace An array of strings that will be inserted somewhere in the text.
    * @return <i>true</i> if the user clicked the "ok"-button
    *         and <i>false</i> otherwise.
    * @see FreeColDialog
    */
    public boolean showConfirmDialog(String text, String okText, String cancelText, String[][] replace) {
        try {
            text = Messages.message(text, replace);
            okText = Messages.message(okText);
            cancelText = Messages.message(cancelText);
        } catch (MissingResourceException e) {
            logger.warning("could not find message with id: " + text + ", " + okText + " or " + cancelText + ".");
        }

        FreeColDialog confirmDialog = FreeColDialog.createConfirmDialog(text, okText, cancelText);
        addCentered(confirmDialog, CONFIRM_LAYER);
        confirmDialog.requestFocus();

        boolean response = confirmDialog.getResponseBoolean();

        remove(confirmDialog);

        return response;
    }

    /**
     * Displays the <code>showPreCombatDialog</code>.
     *
     * @param attacker an <code>Unit</code> value
     * @param defender an <code>Unit</code> value
     * @param settlement a <code>Settlement</code> value
     * @return a <code>boolean</code> value
     */
    public boolean showPreCombatDialog(Unit attacker, Unit defender, Settlement settlement) {

        FreeColDialog preCombatDialog = FreeColDialog.createPreCombatDialog(attacker, defender, settlement, this);
        addCentered(preCombatDialog, CONFIRM_LAYER);
        preCombatDialog.requestFocus();

        boolean response = preCombatDialog.getResponseBoolean();

        remove(preCombatDialog);

        return response;
    }


    /**
    * Displays a dialog with a text and a ok/cancel option.
    *
    * @param messages The messages that explains the choice for the user.
    * @param okText The text displayed on the "ok"-button.
    * @param cancelText The text displayed on the "cancel"-button.
    * @return <i>true</i> if the user clicked the "ok"-button
    *         and <i>false</i> otherwise.
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
            String ID = messages[i].getMessageID();
            try {
                texts[i] = Messages.message(ID);
            } catch (MissingResourceException e) {
                logger.warning("could not find message with id: " + ID + ".");
            }
            images[i] = getImageIcon(messages[i].getDisplay());
        }            

        FreeColDialog confirmDialog = FreeColDialog.createConfirmDialog(texts, images, okText, cancelText);
        addCentered(confirmDialog, CONFIRM_LAYER);
        confirmDialog.requestFocus();

        boolean response = confirmDialog.getResponseBoolean();

        remove(confirmDialog);

        return response;
    }

    /**
    * Checks if this <code>Canvas</code> displaying another panel.
    * 
    * @return <code>true</code> if the <code>Canvas</code> is 
    *       displaying a sub panel other than
    *       {@link net.sf.freecol.client.gui.panel.InfoPanel} and
    *       {@link net.sf.freecol.client.gui.panel.MiniMap}.
    */
    public boolean isShowingSubPanel() {
        if (getColonyPanel().isShowing() || getEuropePanel().isShowing()) {
            return true;
        }
        
        Component[] comps = getComponents();
        for (int i=0; i < comps.length; i++) {
            if (comps[i] instanceof FreeColPanel && !(comps[i] instanceof InfoPanel)) {
                return true;
            }
        }

        return false;
    }


    /**
    * Displays a dialog for setting game options.
    * @return <code>true</code> if the game options have been modified,
    *         and <code>false</code> otherwise.
    */
    public boolean showGameOptionsDialog(boolean editable) {
        gameOptionsDialog.initialize(editable);

        addCentered(gameOptionsDialog, GAME_OPTIONS_LAYER);
        gameOptionsDialog.requestFocus();

        boolean r = gameOptionsDialog.getResponseBoolean();
        remove(gameOptionsDialog);

        return r;
    }

    /**
     * Gets the <code>LoadingSavegameDialog</code>.
     * @return The <code>LoadingSavegameDialog</code>.
     */
    public LoadingSavegameDialog getLoadingSavegameDialog() {
        return loadingSavegameDialog;
    }
    
    /**
     * Displays a dialog for setting options when loading a savegame.
     * The settings can be retrived directly from {@link LoadingSavegameDialog}
     * after calling this method. 
     * 
     * @param publicServer Default value.
     * @param singleplayer Default value. 
     * @return <code>true</code> if the "ok"-button was pressed and
     *      <code>false</code> otherwise.
     */
    public boolean showLoadingSavegameDialog(boolean publicServer, boolean singleplayer) {
        loadingSavegameDialog.initialize(publicServer, singleplayer);
        
        addCentered(loadingSavegameDialog, GAME_OPTIONS_LAYER);
        loadingSavegameDialog.requestFocus();
        
        boolean r = loadingSavegameDialog.getResponseBoolean();
        remove(loadingSavegameDialog);
        
        return r;
    }


    /**
    * Displays a dialog for setting client options.
    * @return <code>true</code> if the client options have been modified,
    *         and <code>false</code> otherwise.
    */
    public boolean showClientOptionsDialog() {
        clientOptionsDialog.initialize();

        addCentered(clientOptionsDialog, CLIENT_OPTIONS_LAYER);
        clientOptionsDialog.requestFocus();

        boolean r = clientOptionsDialog.getResponseBoolean();
        remove(clientOptionsDialog);

        return r;
    }
    
    /**
     * Displays a dialog for setting the map generator options.
     * 
     * @param editable The options are only allowed to be changed
     *      if this variable is <code>true</code>.
     * @return <code>true</code> if the options have been modified,
     *         and <code>false</code> otherwise.
     */
    public boolean showMapGeneratorOptionsDialog(boolean editable) {
        mapGeneratorOptionsDialog.initialize(editable);

        addCentered(mapGeneratorOptionsDialog, CLIENT_OPTIONS_LAYER);
        mapGeneratorOptionsDialog.requestFocus();

        boolean r = mapGeneratorOptionsDialog.getResponseBoolean();
        remove(mapGeneratorOptionsDialog);

        return r;
    }


    /**
    * Displays a dialog where the user may choose a file.
    * This is the same as calling:
    *
    * <br><br><code>
    * showLoadDialog(directory, new FileFilter[] {FreeColDialog.getFSGFileFilter()});
    * </code>
    *
    * @param directory The directory containing the files.
    * @return The <code>File</code>.
    * @see FreeColDialog
    */
    public File showLoadDialog(File directory) {
        return showLoadDialog(directory, new FileFilter[] {FreeColDialog.getFSGFileFilter()});
    }


    /**
    * Displays a dialog where the user may choose a file.
    *
    * @param directory The directory containing the files.
    * @param fileFilters The file filters which the user can
    *       select in the dialog.
    * @return The <code>File</code>.
    * @see FreeColDialog
    */
    public File showLoadDialog(File directory, FileFilter[] fileFilters) {
        FreeColDialog loadDialog = FreeColDialog.createLoadDialog(directory, fileFilters);
        addCentered(loadDialog, LOAD_LAYER);
        loadDialog.requestFocus();

        File response = (File) loadDialog.getResponse();
        
        while (response != null && !response.isFile()) {
            errorMessage("noSuchFile");
            response = (File) loadDialog.getResponse();
        }        

        remove(loadDialog);
        
        return response;
    }


    /**
    * Displays a dialog where the user may choose a filename.
    * This is the same as calling:
    *
    * <br><br><code>
    * showSaveDialog(directory, new FileFilter[] {FreeColDialog.getFSGFileFilter()}, defaultName);
    * </code>
    *
    * @param directory The directory containing the files in which the
    *                  user may overwrite.
    * @param defaultName Default filename for the savegame.
    * @return The <code>File</code>.
    * @see FreeColDialog
    */
    public File showSaveDialog(File directory, String defaultName) {
        return showSaveDialog(directory, ".fsg", new FileFilter[] {FreeColDialog.getFSGFileFilter()}, defaultName);
    }

    /**
    * Displays a dialog where the user may choose a filename.
    *
    * @param directory The directory containing the files 
    *       in which the user may overwrite.
    * @param standardName This extension will be added to the
    *       specified filename (if not added by the user). 
    * @param fileFilters The available file filters in the
    *       dialog.
    * @param defaultName Default filename for the savegame.
    * @return The <code>File</code>.
    * @see FreeColDialog
    */
    public File showSaveDialog(File directory, String standardName, FileFilter[] fileFilters, String defaultName) {
        FreeColDialog saveDialog = FreeColDialog.createSaveDialog(directory, standardName, fileFilters, defaultName);
        addCentered(saveDialog, SAVE_LAYER);
        saveDialog.requestFocus();

        File response = (File) saveDialog.getResponse();

        remove(saveDialog);

        return response;
    }

    /**
    * Displays a dialog that asks the user what he wants to do with his scout in the indian
    * settlement.
    *
    * @param settlement The indian settlement that is being scouted.
    *
    * @return FreeColDialog.SCOUT_INDIAN_SETTLEMENT_CANCEL if the action was cancelled,
    *         FreeColDialog.SCOUT_INDIAN_SETTLEMENT_SPEAK if he wants to speak with the chief,
    *         FreeColDialog.SCOUT_INDIAN_SETTLEMENT_TRIBUTE if he wants to demand tribute,
    *         FreeColDialog.SCOUT_INDIAN_SETTLEMENT_ATTACK if he wants to attack the settlement.
    */
    public int showScoutIndianSettlementDialog(IndianSettlement settlement) {
        FreeColDialog scoutDialog = FreeColDialog.createScoutIndianSettlementDialog(settlement, freeColClient.getMyPlayer());
        addCentered(scoutDialog, SCOUT_INDIAN_SETTLEMENT_LAYER);
        scoutDialog.requestFocus();

        int response = scoutDialog.getResponseInt();

        remove(scoutDialog);

        return response;
    }

    /**
    * Displays a dialog that asks the user what he wants to do with his armed unit
    * in the indian settlement.
    *
    * @param settlement The indian settlement that is going to be attacked or demanded.
    *
    * @return FreeColDialog.SCOUT_INDIAN_SETTLEMENT_CANCEL if the action was cancelled,
    *         FreeColDialog.SCOUT_INDIAN_SETTLEMENT_TRIBUTE if he wants to demand tribute,
    *         FreeColDialog.SCOUT_INDIAN_SETTLEMENT_ATTACK if he wants to attack the settlement.
    */
    public int showArmedUnitIndianSettlementDialog(IndianSettlement settlement) {
        FreeColDialog armedUnitDialog = FreeColDialog.createArmedUnitIndianSettlementDialog(settlement, freeColClient.getMyPlayer());
        addCentered(armedUnitDialog, ARMED_UNIT_INDIAN_SETTLEMENT_LAYER);
        armedUnitDialog.requestFocus();

        int response = armedUnitDialog.getResponseInt();

        remove(armedUnitDialog);

        return response;
    }


    /**
    * Displays a dialog that asks the user what he wants to do with his missionary in the indian
    * settlement.
    *
    * @param settlement The indian settlement that is being visited.
    *
    * @return ArrayList with an Integer and optionally a Player refencing the player to attack in case
    *         of "incite indians". Integer can be any of:
    *         FreeColDialog.MISSIONARY_ESTABLISH if he wants to establish a mission,
    *         FreeColDialog.MISSIONARY_DENOUNCE_AS_HERESY if he wants to denounce the existing
    *            (foreign) mission as heresy,
    *         FreeColDialog.MISSIONARY_INCITE_INDIANS if he wants to incite the indians
    *            (requests their support for war against another European power),
    *         FreeColDialog.MISSIONARY_CANCEL if the action was cancelled.
    */
    public List showUseMissionaryDialog(IndianSettlement settlement) {
        FreeColDialog missionaryDialog = FreeColDialog.createUseMissionaryDialog(settlement, freeColClient.getMyPlayer());
        addCentered(missionaryDialog, USE_MISSIONARY_LAYER);
        missionaryDialog.requestFocus();

        Integer response = (Integer)missionaryDialog.getResponse();
        ArrayList returnValue = new ArrayList();
        returnValue.add(response);

        remove(missionaryDialog);

        if (response.intValue() == FreeColDialog.MISSIONARY_INCITE_INDIANS) {
            FreeColDialog inciteDialog = FreeColDialog.createInciteDialog(freeColClient.getGame().getEuropeanPlayers(), freeColClient.getMyPlayer());
            addCentered(inciteDialog, USE_MISSIONARY_LAYER);
            inciteDialog.requestFocus();

            Player response2 = (Player)inciteDialog.getResponse();
            if (response2 != null) {
                returnValue.add(response2);
            }
            else {
                returnValue.clear();
                returnValue.add(new Integer(FreeColDialog.MISSIONARY_CANCEL));
            }

            remove(inciteDialog);
        }

        return returnValue;
    }


    /**
    * Displays a yes/no question to the user asking if he wants to pay the given amount to an
    * indian tribe in order to have them declare war on the given player.
    *
    * @param enemy The european player to attack.
    * @param amount The amount of gold to pay.
    *
    * @return true if the players wants to pay, false otherwise.
    */
    public boolean showInciteDialog(Player enemy, int amount) {
        String message = Messages.message("missionarySettlement.inciteConfirm");
        message = message.replaceAll("%player%", enemy.getName());
        message = message.replaceAll("%amount%", String.valueOf(amount));

        FreeColDialog confirmDialog = FreeColDialog.createConfirmDialog(message, Messages.message("yes"), Messages.message("no"));
        addCentered(confirmDialog, INCITE_LAYER);
        confirmDialog.requestFocus();
        boolean result = confirmDialog.getResponseBoolean();
        remove(confirmDialog);
        return result;
    }


    /**
    * Displays a dialog with a text field and a ok/cancel option.
    *
    * @param text The text that explains the action to the user.
    * @param defaultValue The default value appearing in the text field.
    * @param okText The text displayed on the "ok"-button.
    * @param cancelText The text displayed on the "cancel"-button.
    *                   Use <i>null</i> to disable the cancel-option.
    * @return The text the user have entered or <i>null</i> if the
    *         user chose to cancel the action.
    * @see FreeColDialog
    */
    public String showInputDialog(String text, String defaultValue, String okText, String cancelText) {
        try {
            text = Messages.message(text);
            okText = Messages.message(okText);

            if (cancelText != null) {
                cancelText = Messages.message(cancelText);
            }
        } catch (MissingResourceException e) {
            logger.warning("could not find message with id: " + text + ", " + okText + " or " + cancelText + ".");
        }

        FreeColDialog inputDialog = FreeColDialog.createInputDialog(text, defaultValue, okText, cancelText);
        addCentered(inputDialog, INPUT_LAYER);
        inputDialog.requestFocus();

        String response = (String) inputDialog.getResponse();

        // checks if the user entered some text.
        if((response != null) && (response.length() == 0)) {
            String okTxt = "ok";
            String txt = "enterSomeText";
            try {
                okTxt = Messages.message(okTxt);
                txt = Messages.message(txt);
            }
            catch(MissingResourceException e) {
                logger.warning("could not find message with id: " + txt + " or " + okTxt + ".");
            }

            FreeColDialog informationDialog = FreeColDialog.createInformationDialog(txt, okTxt);

            do {
                remove(inputDialog);
                addCentered(informationDialog, INPUT_LAYER);
                informationDialog.requestFocus();

                informationDialog.getResponse();
                remove(informationDialog);

                addCentered(inputDialog, INPUT_LAYER);
                inputDialog.requestFocus();

                response = (String) inputDialog.getResponse();
            } while((response != null) && (response.length() == 0));
        }

        remove(inputDialog);

        return response;
    }


    /**
    * Displays a dialog with a text and a cancel-button,
    * in addition to buttons for each of the objects returned for the given
    * <code>Iterator</code>.
    *
    * @param text The text that explains the choice for the user.
    * @param cancelText The text displayed on the "cancel"-button.
    * @param iterator The <code>Iterator</code> containing the objects to create
    *            buttons for.
    * @return The choosen object, or <i>null</i> for the cancel-button.
    */
    public Object showChoiceDialog(String text, String cancelText, Iterator iterator) {
        ArrayList a = new ArrayList();
        while (iterator.hasNext()) {
            a.add(iterator.next());
        }

        return showChoiceDialog(text, cancelText, a.toArray());
    }


    /**
    * Displays a dialog with a text and a cancel-button,
    * in addition to buttons for each of the objects in the array.
    *
    * @param text The text that explains the choice for the user.
    * @param cancelText The text displayed on the "cancel"-button.
    * @param objects The array containing the objects to create
    *            buttons for.
    * @return The choosen object, or <i>null</i> for the cancel-button.
    */
    public Object showChoiceDialog(String text, String cancelText, Object[] objects) {
        /*
        try {
            text = Messages.message(text);
            cancelText = Messages.message(cancelText);
        } catch (MissingResourceException e) {
            logger.warning("could not find message with id: " + text + " or " + cancelText + ".");
        }
        */

        FreeColDialog choiceDialog = FreeColDialog.createChoiceDialog(text, cancelText, objects);
        if (choiceDialog.getHeight() > getHeight() / 3) {
            choiceDialog.setSize(choiceDialog.getWidth(), (getHeight() * 2) / 3);
        }
        addCentered(choiceDialog, CHOICE_LAYER);
        choiceDialog.requestFocus();

        Object response = choiceDialog.getResponse();
        remove(choiceDialog);

        return response;
    }

    
    /**
    * Shows a status message that cannot be dismissed.
    * The panel will be removed when another component
    * is added to this <code>Canvas</code>. This includes
    * all the <code>showXXX</code>-methods. In addition,
    * {@link #closeStatusPanel()} and {@link #closeMenus()}
    * also removes this panel.
    *
    * @param message The text message to display on the
    *                status panel.
    * @see StatusPanel
    */
    public void showStatusPanel(String message) {
        statusPanel.setStatusMessage(message);
        addCentered(statusPanel, STATUS_LAYER);
    }
    

    /**
    * Closes the <code>StatusPanel</code>.
    * @see #showStatusPanel
    */
    public void closeStatusPanel() {
        remove(statusPanel);
    }


    /**
     * Shows a panel displaying Colopedia Information.
     * @param type The type of colopedia panel to display.
     */
    public void showColopediaPanel(int type) {
        colopediaPanel.initialize(type);
        setEnabled(false);
        addCentered(colopediaPanel, COLOPEDIA_LAYER);
        colopediaPanel.requestFocus();
    }


    /**
     * Shows a panel displaying Colopedia Information.
     * @param type The type of colopedia panel to display.
     * @param action The details to display.
     */
    public void showColopediaPanel(int type, int action) {
        colopediaPanel.initialize(type, action);
        setEnabled(false);
        addCentered(colopediaPanel, COLOPEDIA_LAYER);
        colopediaPanel.requestFocus();
    }


    /**
     * Shows a report panel.
     * @param classname The class name of the report panel
     *      to be displayed.
     */
    public void showReportPanel(String classname) {
        ReportPanel reportPanel = null;
        if ("net.sf.freecol.client.gui.panel.ReportReligiousPanel".equals(classname)) {
            reportPanel = reportReligiousPanel;
        } else if ("net.sf.freecol.client.gui.panel.ReportLabourPanel".equals(classname)) {
            reportPanel = reportLabourPanel;
        } else if ("net.sf.freecol.client.gui.panel.ReportForeignAffairPanel".equals(classname)) {
            reportPanel = reportForeignAffairPanel;
        } else if ("net.sf.freecol.client.gui.panel.ReportIndianPanel".equals(classname)) {
            reportPanel = reportIndianPanel;
        } else if ("net.sf.freecol.client.gui.panel.ReportTurnPanel".equals(classname)) {
            reportPanel = reportTurnPanel;
        } else if ("net.sf.freecol.client.gui.panel.ReportContinentalCongressPanel".equals(classname)) {
            reportPanel = reportContinentalCongressPanel;
        } else if ("net.sf.freecol.client.gui.panel.ReportTradePanel".equals(classname)) {
            reportPanel = reportTradePanel;
        } else if ("net.sf.freecol.client.gui.panel.ReportMilitaryPanel".equals(classname)) {
            reportPanel = reportMilitaryPanel;
        } else if ("net.sf.freecol.client.gui.panel.ReportNavalPanel".equals(classname)) {
            reportPanel = reportNavalPanel;
        } else if ("net.sf.freecol.client.gui.panel.ReportColonyPanel".equals(classname)) {
            reportPanel = reportColonyPanel;
        } else {
            logger.warning("Request for Report panel could not be processed.  Name="+ classname );
        }

        if (reportPanel != null) {
            reportPanel.initialize();
            setEnabled(false);
            addCentered(reportPanel, REPORT_LAYER);
            reportPanel.requestFocus();
        }
    }
    

    /**
    * Shows a panel where the player may choose the next founding father to recruit.
    * @param possibleFoundingFathers The different founding fathers the player may choose.
    * @return The founding father the player has chosen.
    * @see net.sf.freecol.common.model.FoundingFather
    */
    public int showChooseFoundingFatherDialog(int[] possibleFoundingFathers) {
        remove(statusPanel);
        
        chooseFoundingFatherDialog.initialize(possibleFoundingFathers);

        addCentered(chooseFoundingFatherDialog, CHOOSE_FOUNDING_FATHER);
        setEnabled(false);
        chooseFoundingFatherDialog.requestFocus();

        int response = chooseFoundingFatherDialog.getResponseInt();

        remove(chooseFoundingFatherDialog);
        setEnabled(true);

        return response;
    }
    

    /**
     * Gets the dialog which is used for choosing a founding father.
     * @return The dialog.
     */
    public ChooseFoundingFatherDialog getChooseFoundingFatherDialog() {
        return chooseFoundingFatherDialog;
    }


    /**
    * Shows the {@link EventPanel}.
    * @param eventID The type of <code>EventPanel</code> to be displayed.
    * @return <code>true</code>.
    */
    public boolean showEventDialog(int eventID) {
        eventPanel.initialize(eventID);

        addCentered(eventPanel, EVENT_LAYER);
        setEnabled(false);
        eventPanel.requestFocus();

        boolean response = eventPanel.getResponseBoolean();

        remove(eventPanel);
        setEnabled(true);

        return response;
    }


    /**
     * Gets the <code>EventPanel</code>.
     * @return The panel.
     */
    public EventPanel getEventPanel() {
        return eventPanel;
    }


    /**
    * Displays the <code>EuropePanel</code>.
    * @see EuropePanel
    */
    public void showEuropePanel() {
        closeMenus();

        if (freeColClient.getGame() == null) {
            errorMessage("europe.noGame");
        } else {
            europePanel.initialize(freeColClient.getMyPlayer().getEurope(), freeColClient.getGame());
            europePanel.setLocation(0, getMenuBarHeight());
            setEnabled(false);
            add(europePanel, EUROPE_LAYER);

            europePanel.requestFocus();
        }
    }

    /**
    * Displays the <code>RecruitDialog</code>. Does not return from this
    * method before the panel is closed.
    */
    public boolean showRecruitDialog() {
        recruitDialog.initialize();
        addCentered(recruitDialog, INPUT_LAYER);

        recruitDialog.requestFocus();

        boolean response = recruitDialog.getResponseBoolean();

        remove(recruitDialog);
        setEnabled(true);

        return response;
    }




    /**
    * Displays the <code>PurchaseDialog</code>. Does not return from this
    * method before the panel is closed.
    */
    public int showPurchaseDialog() {
        purchaseDialog.initialize();
        addCentered(purchaseDialog, INPUT_LAYER);

        purchaseDialog.requestFocus();

        int response = purchaseDialog.getResponseInt();

        remove(purchaseDialog);
        setEnabled(true);

        return response;

    }




    /**
    * Displays the <code>TrainDialog</code>. Does not return from this
    * method before the panel is closed.
    */
    public boolean showTrainDialog() {
        trainDialog.initialize();
        addCentered(trainDialog, INPUT_LAYER);

        trainDialog.requestFocus();

        boolean response = trainDialog.getResponseBoolean();

        remove(trainDialog);
        setEnabled(true);

        return response;
    }


    /**
     * Displays the <code>TradeRouteDialog</code>. Does not return
     * from this method before the panel is closed.
     */
    public boolean showTradeRouteDialog() {
        tradeRouteDialog.initialize();
        addCentered(tradeRouteDialog, INPUT_LAYER - 1);

        tradeRouteDialog.requestFocus();

        boolean response = tradeRouteDialog.getResponseBoolean();

        remove(tradeRouteDialog);
        setEnabled(true);

        return response;
    }

    /**
     * Displays the <code>TradeRouteInputDialog</code>. Does not
     * return from this method before the panel is closed.
     */
    public boolean showTradeRouteInputDialog(TradeRoute route) {
        tradeRouteInputDialog.initialize(route);
        addCentered(tradeRouteInputDialog, INPUT_LAYER);

        tradeRouteInputDialog.requestFocus();

        boolean response = tradeRouteInputDialog.getResponseBoolean();

        remove(tradeRouteInputDialog);
        setEnabled(true);

        return response;
    }



    /**
    * Displays the colony panel of the given <code>Colony</code>.
    * @param colony The colony whose panel needs to be displayed.
    * @see ColonyPanel
    */
    public void showColonyPanel(Colony colony) {
        closeMenus();                                               

        colonyPanel.initialize(colony, freeColClient.getGame());
        setEnabled(false);
        addCentered(colonyPanel, COLONY_LAYER);

        colonyPanel.requestFocus();
    }


    /**
    * Displays the indian settlement panel of the given <code>IndianSettlement</code>.
    * @param settlement The indian settlement whose panel needs to be displayed.
    * @see IndianSettlement
    */
    public void showIndianSettlementPanel(IndianSettlement settlement) {
        closeMenus();

        indianSettlementPanel.initialize(settlement);
        addCentered(indianSettlementPanel, INDIAN_SETTLEMENT_LAYER);
        indianSettlementPanel.requestFocus();
        indianSettlementPanel.getResponseBoolean();

        remove(indianSettlementPanel);
    }

    
    /**
    * Displays the tile panel of the given <code>Tile</code>.
    * @param tile The tile whose panel needs to be displayed.
    * @see Tile
    */
    public void showTilePanel(Tile tile) {
        closeMenus();

        tilePanel.initialize(tile);
        addCentered(tilePanel, TILE_LAYER);
        tilePanel.requestFocus();
        tilePanel.getResponseBoolean();

        remove(tilePanel);
    }

    
    /**
     * Displays the monarch action panel.
     * @param action The monarch action.
     * @param replace The replacement strings.
     * @return true or false
     * @see net.sf.freecol.common.model.Monarch
     */
    public boolean showMonarchPanel(int action, String [][] replace) {
        remove(statusPanel);

        setEnabled(false);
        monarchPanel.initialize(action, replace);
        addCentered(monarchPanel, MONARCH_LAYER);
        monarchPanel.requestFocus();
        boolean response = monarchPanel.getResponseBoolean();
        remove(monarchPanel);
        setEnabled(true);

        return response;
    }

    
    /**
    * Shows the panel that allows the user to choose which unit will emigrate
    * from Europe. This method may only be called if the user has William Brewster
    * in congress.
    * @return The emigrant that was chosen by the user (1, 2 or 3).
    */
    public int showEmigrationPanel() {
        emigrationPanel.initialize(freeColClient.getMyPlayer().getEurope());

        addCentered(emigrationPanel, EMIGRATION_LAYER);
        emigrationPanel.requestFocus();
        int response = emigrationPanel.getResponseInt();
        remove(emigrationPanel);

        return response;
    }


    /**
    * Updates the menu bar.
    */
    public void updateJMenuBar() {
        if (jMenuBar instanceof FreeColMenuBar) {
            ((FreeColMenuBar) jMenuBar).update();
        }
    }


    /**
    * Creates and sets a <code>FreeColMenuBar</code> on this <code>Canvas</code>.
    * @see FreeColMenuBar
    */
    public void resetFreeColMenuBar() {
        FreeColMenuBar freeColMenuBar = new FreeColMenuBar(freeColClient);
        setJMenuBar(freeColMenuBar);
    }

    /**
     * Removes the given component from this Container.
     * @param comp The component to remove from this Container.
     */
    public void remove(Component comp) {
       remove(comp, true); 
    }

    /**
     * Removes the given component from this Container.
     * @param comp The component to remove from this Container.
     * @param update The <code>Canvas</code> will be enabled,
     *      the graphics repainted and both the menubar and 
     *      the actions will be updated if this parameter is 
     *      <code>true</code>.
     */
    public void remove(Component comp, boolean update) {
        if (comp != null) {
            if (comp == jMenuBar) {
                jMenuBar = null;
            }
            
            boolean takeFocus = true;
            if (comp == statusPanel) {
                takeFocus = false;
            }

            Rectangle bounds = comp.getBounds();
            super.remove(comp);
            
            if (update) {
                setEnabled(true);
                updateJMenuBar();
                freeColClient.getActionManager().update();
                
                if (takeFocus && !isShowingSubPanel()) {
                    takeFocus();
                }

                repaint(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        }
    }


    /**
    * Adds a component to this Canvas.
    * @param comp The component to add to this ToEuropePanel.
    * @return The component argument.
    */
    public Component add(Component comp) {
        add(comp, null);
        return comp;
    }

    /**
     * Adds a component centered on this Canvas. Removes the statuspanel if visible
     * (and <code>comp != statusPanel</code>).
     * @param comp The component to add to this ToEuropePanel.
     * @return The component argument.
     */
    public Component addCentered(Component comp) { 
        addCentered(comp, null);
        return comp;
    }
    
    /**
     * Adds a component centered on this Canvas. Removes the statuspanel if visible
     * (and <code>comp != statusPanel</code>).
     * @param comp The component to add to this ToEuropePanel.
     * @param i The layer to add the component to (see JLayeredPane).
     */
    public void addCentered(Component comp, Integer i) { 
        comp.setLocation(getWidth() / 2 - comp.getWidth() / 2,
                (getHeight() + getMenuBarHeight()) / 2 - comp.getHeight() / 2);
        
        add(comp, i);
    }
    
    /**
    * Adds a component to this Canvas. Removes the statuspanel if visible
    * (and <code>comp != statusPanel</code>).
    * @param comp The component to add to this ToEuropePanel.
    * @param i The layer to add the component to (see JLayeredPane).
    */
    public void add(Component comp, Integer i) {
        if ((takeFocusThread != null) && (takeFocusThread.isAlive())) {
            takeFocusThread.stopWorking();
        }

        if (comp != statusPanel && !(comp instanceof JMenuItem) && !(comp instanceof FreeColDialog)) {
            remove(statusPanel, false);
        }

        if (i == null) {
            super.add(comp);
        } else {
            super.add(comp, i);
        }
        updateJMenuBar();
        freeColClient.getActionManager().update();
    }


    /**
    * Makes sure that this Canvas takes the focus. It will keep on trying for
    * a while even its request doesn't get granted immediately.
    */
    private void takeFocus() {
        JComponent c = this;

        if (startGamePanel.isShowing()) {
            c = startGamePanel;
        } else if (newPanel.isShowing()) {
            c = newPanel;
        } else if (mainPanel.isShowing()) {
            c = mainPanel;
        } else if (europePanel.isShowing()) {
            c = europePanel;
        } else if (colonyPanel.isShowing()) {
            c = colonyPanel;
        }

        if (takeFocusThread != null) {
            takeFocusThread.stopWorking();
        }

        /*if (c != this) {
            c.requestFocus();
        } else {
            takeFocusThread = new TakeFocusThread(c);
            takeFocusThread.start();
        }*/
        c.requestFocus();
    }


    /**
    * Gets the <code>ColonyPanel</code>.
    * @return The <code>ColonyPanel</code>
    */
    public ColonyPanel getColonyPanel() {
        return colonyPanel;
    }


    /**
    * Gets the <code>EuropePanel</code>.
    * @return The <code>EuropePanel</code>.
    */
    public EuropePanel getEuropePanel() {
        return europePanel;
    }


    /**
    * Enables or disables this component depending on the given argument.
    * @param b Must be set to 'true' if this component needs to be enabled
    * or to 'false' otherwise.
    */
    public void setEnabled(boolean b) {
        if (isEnabled() != b) {
            for (int i = 0; i < getComponentCount(); i++) {
                getComponent(i).setEnabled(b);              
            }
            
            /*
             if (jMenuBar != null) {
             jMenuBar.setEnabled(b);
             }
             */
            freeColClient.getActionManager().update();
            
            super.setEnabled(b);
        }
    }


    /**
    * Shows the given popup at the given position on the screen.
    *
    * @param popup The JPopupMenu to show.
    * @param x The x-coordinate at which to show the popup.
    * @param y The y-coordinate at which to show the popup.
    */
    public void showPopup(JPopupMenu popup, int x, int y) {
        closeMenus();
        popup.show(this, x, y);
    }


    /**
    * Shows a tile popup.
    *
    * @param pos The coordinates of the Tile where the popup occured.
    * @param x The x-coordinate on the screen where the popup needs to be placed.
    * @param y The y-coordinate on the screen where the popup needs to be placed.
    * @see TilePopup
    */
    public void showTilePopup(Map.Position pos, int x, int y) {
        if (pos != null) {
            Tile t = freeColClient.getGame().getMap().getTileOrNull(pos.getX(), pos.getY());

            if (t != null) {
                TilePopup tp = new TilePopup(t, freeColClient, this, getGUI());
                if (tp.hasItem()) {
                    showPopup(tp, x, y);
                } else if (t.getType() != Tile.UNEXPLORED) {
                    showTilePanel(t);
                }
            }
        }
    }


    /**
    * Displays an error message.
    * @param messageID The i18n-keyname of the error message to display.
    */
    public void errorMessage(String messageID) {
        errorMessage(messageID, "Unspecified error: " + messageID);
    }


    /**
    * Displays an error message.
    *
    * @param messageID The i18n-keyname of the error message to display.
    * @param message An alternativ message to display if the resource
    *                specified by <code>messageID</code> is unavailable.
    */
    public void errorMessage(String messageID, String message) {
        if (messageID != null) {
            try {
                message = Messages.message(messageID);
            } catch (MissingResourceException e) {
                logger.warning("could not find message with id: " + messageID);
            }
        }

        errorPanel.initialize(message);
        //setEnabled(false);
        addCentered(errorPanel, ERROR_LAYER);
        errorPanel.requestFocus();
        errorPanel.getResponse();
        closeErrorPanel();
    }


    /**
    * Shows a message with some information and an "OK"-button.
    * @param messageId The messageId of the message to display.
    */
    public void showInformationMessage(String messageId) {
        showInformationMessage(messageId, null);
    }


    /**
    * Shows a message with some information and an "OK"-button.
    * @param messageId The messageId of the message to display.
    * @param replaceString The string that we need to use to replace all occurences of %replace% in the
    *        message.
    */
    /*public void showInformationMessage(String messageId, String replaceString) {
        showInformationDialog(messageId, {{"%replace%", replaceString}});
    }*/

    /**
    * Shows a message with some information and an "OK"-button.
    *
    * <br><br><b>Example:</b>
    * <br><code>canvas.showInformationMessage("noNeedForTheGoods", new String[][] {{"%goods%", goods.getName()}});</code>
    * @param messageId The messageId of the message to display.
    * @param replace All occurances of <code>replace[i][0]</code> in
    *                the message gets replaced by <code>replace[i][1]</code>.
    */
    public void showInformationMessage(String messageId, String[][] replace) {
        String text;
        try {
            text = Messages.message(messageId, replace);
        } catch (MissingResourceException e) {
            text = messageId;
            logger.warning("Missing i18n resource: " + messageId);
        }
        FreeColDialog infoDialog = FreeColDialog.createInformationDialog(new String[] {text}, new ImageIcon[] {null});
        addCentered(infoDialog, INFORMATION_LAYER);
        infoDialog.requestFocus();

        infoDialog.getResponse();

        remove(infoDialog);
    }


    /**
    * Closes the <code>ErrorPanel</code>.
    */
    public void closeErrorPanel() {
        remove(errorPanel);
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
    * @param t The tile to refresh.
    */
    public void refreshTile(Tile t) {
        refreshTile(t.getX(), t.getY());
    }


    /**
    * Refreshes the screen at the specified Tile.
    * @param p The position of the tile to refresh.
    */
    public void refreshTile(Position p) {
        refreshTile(p.getX(), p.getY());
    }


    /**
    * Returns the image provider that is being used by this canvas.
    * @return The image provider that is being used by this canvas.
    */
    public ImageProvider getImageProvider() {
        return gui.getImageLibrary();
    }


    /**
    * Closes all the menus that are currently open.
    */
    public void closeMenus() {
        remove(newPanel, false);
        remove(startGamePanel, false);
        remove(serverListPanel, false);
        remove(colonyPanel, false);
        remove(europePanel, false);
        remove(statusPanel);
    }


    /**
    * Shows the <code>MainPanel</code>.
    * @see MainPanel
    */
    public void showMainPanel() {
        closeMenus();
        addCentered(mainPanel, MAIN_LAYER);
        mainPanel.requestFocus();
    }

    /**
     * Gets the <code>MainPanel</code>.
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
    * @return The <code>StartGamePanel</code>.
    * @see StartGamePanel
    */
    public StartGamePanel getStartGamePanel() {
        return startGamePanel;
    }


    /**
    * Tells the map controls that a chat message was recieved.
    * @param sender The player who sent the chat message to the server.
    * @param message The chat message.
    * @param privateChat 'true' if the message is a private one, 'false' otherwise.
    * @see GUIMessage
    */
    public void displayChatMessage(Player sender, String message, boolean privateChat) {
        gui.addMessage(new GUIMessage(sender.getName() + ": " + message, sender.getColor()));
    }


    /**
    * Displays a chat message originating from this client.
    * @param message The chat message.
    */
    public void displayChatMessage(String message) {
        displayChatMessage(freeColClient.getMyPlayer(), message, false);
    }


    /**
    * Quits the application. This method uses {@link #confirmQuitDialog()}
    * in order to get a "Are you sure"-confirmation from the user.
    */
    public void quit() {
        if (confirmQuitDialog()) {
            freeColClient.quit();
        }
    }


    /**
    * Closes all panels, changes the background and shows the main menu.
    */
    public void returnToTitle() {
        // TODO: check if the GUI object knows that we're not inGame. (Retrieve value
        //       of GUI::inGame.)
        //       If GUI thinks we're still in the game then log an error because at this
        //       point the GUI should have been informed.
        setEnabled(false);
        closeMenus();
        removeInGameComponents();
        showMainPanel();
    }


    /**
    * Removes components that is only used when in game.
    */
    public void removeInGameComponents() {
        // remove listeners, they will be added when launching the new game...
        KeyListener[] keyListeners = getKeyListeners();
        for(int i = 0; i < keyListeners.length; ++i) {
            removeKeyListener(keyListeners[i]);
        }

        MouseListener[] mouseListeners = getMouseListeners();
        for(int i = 0; i < mouseListeners.length; ++i) {
            removeMouseListener(mouseListeners[i]);
        }

        MouseMotionListener[] mouseMotionListeners = getMouseMotionListeners();
        for(int i = 0; i < mouseMotionListeners.length; ++i) {
            removeMouseMotionListener(mouseMotionListeners[i]);
        }

        if (jMenuBar != null) {
            remove(jMenuBar);            
        }
    }
    
    
    /**
     * Checks if this <code>Canvas</code> contains any
     * ingame components.
     * 
     * @return <code>true</code> if there is a single ingame
     *      component.
     */
    public boolean containsInGameComponents() {
        // remove listeners, they will be added when launching the new game...
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
    * Displays a "Are you sure you want to quit"-dialog
    * in which the user may choose to quit or cancel.
    *
    * @return <i>true</i> if the user desides to quit and
    *         <i>false</i> otherwise.
    */
    public boolean confirmQuitDialog() {
        addCentered(quitDialog, QUIT_LAYER);
        quitDialog.requestFocus();

        return quitDialog.getResponseBoolean();
    }
    

    /**
    * Returns this <code>Canvas</code>'s <code>GUI</code>.
    * @return The <code>GUI</code>.
    */
    public GUI getGUI() {
        return gui;
    }


    /** Returns the freeColClient.
    * @return The <code>freeColClient</code> associated with this <code>Canvas</code>.
    */
    public FreeColClient getClient() {
      return freeColClient;
    }


    /**
     * Displays a quit dialog and, if desired, logouts the current game and shows the new game panel.
     */
    public void newGame() {
        if(!showConfirmDialog("stopCurrentGame.text", "stopCurrentGame.yes", "stopCurrentGame.no")) {
            return;
        }

        freeColClient.getConnectController().quitGame(true);
        removeInGameComponents();
        showNewGamePanel();
    }

    /**
    * Makes sure that old chat messages are removed in time.
    */
    private final class ChatDisplayThread extends Thread {
        /**
        * The constructor to use.
        */
        public ChatDisplayThread() {
            super("ChatDisplayThread");
        }

        /**
        * Removes old chat messages regularly.
        */
        public void run() {
            for (;;) {
                if (gui.removeOldMessages()) {
                    refresh();
                }
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                }
            }
        }
    }




    /**
    * Makes sure that a given component takes the focus.
    */
    private final class TakeFocusThread extends Thread {
        private final JComponent component;
        private boolean doYourWork;

        /**
        * The constructor to use.
        * @param component The component that needs focus.
        */
        public TakeFocusThread(JComponent component) {
            super("TakeFocusThread");
            this.component = component;
            doYourWork = true;
        }

        /**
        * Makes sure that this thread stops working.
        */
        public void stopWorking() {
            doYourWork = false;
        }

        /**
        * Returns 'true' if this thread is going to keep on working, 'false' otherwise.
        * @return 'true' if this thread is going to keep on working, 'false' otherwise.
        */
        public boolean isStillWorking() {
            return doYourWork;
        }


        /**
         * Gets the component this thread is trying to take focus for.
         * @return The component.
         */
        public JComponent getComponent() {
            return component;
        }


        /**
        * Makes sure that the given component takes the focus.
        */
        public void run() {
            int count = 0;

            while ((!component.hasFocus()) && doYourWork) {
                component.requestFocus();

                try {
                    sleep(100);
                }
                catch (InterruptedException e) {
                }
                count++;
                if (count > 50) {
                    // We're already been trying for 5 seconds, there must be something wrong.
                    logger.warning("Component can't get focus: " + component.toString());
                }
            }
        }
    }
}
