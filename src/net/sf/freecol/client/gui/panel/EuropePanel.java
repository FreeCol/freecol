

package net.sf.freecol.client.gui.panel;


import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.logging.Logger;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;

import net.sf.freecol.common.FreeColException;

import net.sf.freecol.client.gui.Canvas;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Goods;


/**
 * This is a panel for the Europe display. It shows the ships in Europe and
 * allows the user to send them back.
 */
public final class EuropePanel extends JLayeredPane implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(EuropePanel.class.getName());

    private static final int    EXIT = 0,
                                RECRUIT = 1,
                                RECRUIT_CANCEL = 2,
                                RECRUIT_1 = 3,
                                RECRUIT_2 = 4,
                                RECRUIT_3 = 5,
                                PURCHASE = 6,
                                PURCHASE_CANCEL = 7,
                                PURCHASE_ARTILLERY = 8,
                                PURCHASE_CARAVEL = 9,
                                PURCHASE_MERCHANTMAN = 10,
                                PURCHASE_GALLEON = 11,
                                PURCHASE_PRIVATEER = 12,
                                PURCHASE_FRIGATE = 13,
                                TRAIN = 14,
                                TRAIN_CANCEL = 15,
                                TRAIN_EXPERT_ORE_MINER = 16,
                                TRAIN_EXPERT_LUMBER_JACK = 17,
                                TRAIN_MASTER_GUNSMITH = 18,
                                TRAIN_EXPERT_SILVER_MINER = 19,
                                TRAIN_MASTER_FUR_TRADER = 20,
                                TRAIN_MASTER_CARPENTER = 21,
                                TRAIN_EXPERT_FISHERMAN = 22,
                                TRAIN_MASTER_BLACKSMITH = 23,
                                TRAIN_EXPERT_FARMER = 24,
                                TRAIN_MASTER_DISTILLER = 25,
                                TRAIN_HARDY_PIONEER = 26,
                                TRAIN_MASTER_TOBACCONIST = 27,
                                TRAIN_MASTER_WEAVER = 28,
                                TRAIN_JESUIT_MISSIONARY = 29,
                                TRAIN_FIREBRAND_PREACHER = 30,
                                TRAIN_ELDER_STATESMAN = 31,
                                TRAIN_VETERAN_SOLDIER = 32;


    private final Canvas  parent;
    private final FreeColClient freeColClient;
    private InGameController inGameController;

    private final JLabel                    cargoLabel;
    private final JLabel                    goldLabel;
    private final ToAmericaPanel            toAmericaPanel;
    private final ToEuropePanel             toEuropePanel;
    private final InPortPanel               inPortPanel;
    private final DocksPanel                docksPanel;
    private final CargoPanel                cargoPanel;
    private final MarketPanel               marketPanel;
    private final RecruitPanel              recruitPanel;
    private final PurchasePanel             purchasePanel;
    private final TrainPanel                trainPanel;
    private final DefaultTransferHandler    defaultTransferHandler;
    private final MouseListener             pressListener;

    private Europe      europe;
    private Game        game;
    private UnitLabel   selectedUnit;

    private JButton exitButton = new JButton("Close");




    /**
     * The constructor for the panel.
     * @param parent The parent of this panel
     */
    public EuropePanel(Canvas parent, FreeColClient freeColClient, InGameController inGameController) {
        this.parent = parent;
        this.freeColClient = freeColClient;
        this.inGameController = inGameController;

        toAmericaPanel = new ToAmericaPanel(this);
        toEuropePanel = new ToEuropePanel(this);
        inPortPanel = new InPortPanel();
        docksPanel = new DocksPanel(this);
        cargoPanel = new CargoPanel(this);
        marketPanel = new MarketPanel(this);
        recruitPanel = new RecruitPanel(this);
        purchasePanel = new PurchasePanel(this);
        trainPanel = new TrainPanel(this);

        toAmericaPanel.setBackground(Color.WHITE);
        toEuropePanel.setBackground(Color.WHITE);
        inPortPanel.setBackground(Color.WHITE);
        docksPanel.setBackground(Color.WHITE);
        cargoPanel.setBackground(Color.WHITE);

        defaultTransferHandler = new DefaultTransferHandler(this);
        toAmericaPanel.setTransferHandler(defaultTransferHandler);
        toEuropePanel.setTransferHandler(defaultTransferHandler);
        inPortPanel.setTransferHandler(defaultTransferHandler);
        docksPanel.setTransferHandler(defaultTransferHandler);
        cargoPanel.setTransferHandler(defaultTransferHandler);
        marketPanel.setTransferHandler(defaultTransferHandler);

        pressListener = new DragListener(this);
        MouseListener releaseListener = new DropListener();
        toAmericaPanel.addMouseListener(releaseListener);
        toEuropePanel.addMouseListener(releaseListener);
        inPortPanel.addMouseListener(releaseListener);
        docksPanel.addMouseListener(releaseListener);
        cargoPanel.addMouseListener(releaseListener);
        marketPanel.addMouseListener(releaseListener);

        toAmericaPanel.setLayout(new GridLayout(0 , 2));
        toEuropePanel.setLayout(new GridLayout(0 , 2));
        inPortPanel.setLayout(new GridLayout(0 , 2));
        docksPanel.setLayout(new GridLayout(0 , 2));
        cargoPanel.setLayout(new GridLayout(1 , 0));

        cargoLabel = new JLabel("<html><strike>Cargo</strike></html>");
        goldLabel = new JLabel("Gold: 0");

        JButton recruitButton = new JButton("Recruit"),
                purchaseButton = new JButton("Purchase"),
                trainButton = new JButton("Train");
        JScrollPane toAmericaScroll = new JScrollPane(toAmericaPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
                    toEuropeScroll = new JScrollPane(toEuropePanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
                    inPortScroll = new JScrollPane(inPortPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
                    docksScroll = new JScrollPane(docksPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
                    cargoScroll = new JScrollPane(cargoPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                    marketScroll = new JScrollPane(marketPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JLabel  toAmericaLabel = new JLabel("Going to America"),
                toEuropeLabel = new JLabel("Going to Europe"),
                inPortLabel = new JLabel("In port"),
                docksLabel = new JLabel("Docks");

        exitButton.setSize(80, 20);
        recruitButton.setSize(100, 20);
        purchaseButton.setSize(100, 20);
        trainButton.setSize(100, 20);
        toAmericaScroll.setSize(200, 300);
        toEuropeScroll.setSize(200, 300);
        inPortScroll.setSize(200, 300);
        docksScroll.setSize(200, 300);
        cargoScroll.setSize(410, 96);
        marketScroll.setSize(620, 114);
        toAmericaLabel.setSize(200, 20);
        toEuropeLabel.setSize(200, 20);
        inPortLabel.setSize(200, 20);
        docksLabel.setSize(200, 20);
        cargoLabel.setSize(410, 20);
        goldLabel.setSize(100, 20);

        exitButton.setLocation(760, 570);
        recruitButton.setLocation(690, 90);
        purchaseButton.setLocation(690, 120);
        trainButton.setLocation(690, 150);
        toAmericaScroll.setLocation(10, 35);
        toEuropeScroll.setLocation(220, 35);
        inPortScroll.setLocation(430, 35);
        docksScroll.setLocation(640, 250);
        cargoScroll.setLocation(220, 370);
        marketScroll.setLocation(10, 476);
        toAmericaLabel.setLocation(10, 10);
        toEuropeLabel.setLocation(220, 10);
        inPortLabel.setLocation(430, 10);
        docksLabel.setLocation(640, 225);
        cargoLabel.setLocation(220, 345);
        goldLabel.setLocation(15, 345);

        setLayout(null);

        exitButton.setActionCommand(String.valueOf(EXIT));
        recruitButton.setActionCommand(String.valueOf(RECRUIT));
        purchaseButton.setActionCommand(String.valueOf(PURCHASE));
        trainButton.setActionCommand(String.valueOf(TRAIN));

        exitButton.addActionListener(this);
        recruitButton.addActionListener(this);
        purchaseButton.addActionListener(this);
        trainButton.addActionListener(this);

        add(exitButton);
        add(recruitButton);
        add(purchaseButton);
        add(trainButton);
        add(toAmericaScroll);
        add(toEuropeScroll);
        add(inPortScroll);
        add(docksScroll);
        add(cargoScroll);
        add(marketScroll);
        add(toAmericaLabel);
        add(toEuropeLabel);
        add(inPortLabel);
        add(docksLabel);
        add(cargoLabel);
        add(goldLabel);

        try {
            BevelBorder border = new BevelBorder(BevelBorder.RAISED);
            setBorder(border);
        } catch(Exception e) {}

        setSize(850, 600);

        selectedUnit = null;
    }

    
    public void requestFocus() {
        exitButton.requestFocus();
    }
    
    

    /**
    * Refreshes this panel.
    */
    public void refresh() {
        repaint(0, 0, getWidth(), getHeight());
    }


    /**
    * Refreshes the components on this panel that need to be refreshed after the user
    * has recruited a new unit.
    */
    public void refreshBuyRecruit() {
        docksPanel.removeAll();

        Iterator unitIterator = europe.getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit unit = (Unit) unitIterator.next();

            if (((unit.getState() == Unit.ACTIVE) || (unit.getState() == Unit.SENTRY)) && (!unit.isNaval())) {
                UnitLabel unitLabel = new UnitLabel(unit, parent);
                unitLabel.setTransferHandler(defaultTransferHandler);
                unitLabel.addMouseListener(pressListener);

                docksPanel.add(unitLabel, false);
            }
        }

        goldLabel.setText("Gold: " + freeColClient.getMyPlayer().getGold());

        // Only two components will be repainted!
        goldLabel.repaint(0, 0, goldLabel.getWidth(), goldLabel.getHeight());
        docksPanel.repaint(0, 0, docksPanel.getWidth(), docksPanel.getHeight());
    }


    /**
    * Refreshes the components on this panel that need to be refreshed after the user
    * has purchased a new unit.
    *
    * @param type The type of unit that was just purchased. This is needed to know which
    *             component needs to be refreshed.
    */
    public void refreshBuyPurchase(int type) {
        if (type == Unit.ARTILLERY) {
            refreshBuyRecruit();
        } else {
            inPortPanel.removeAll();

            Iterator unitIterator = europe.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit unit = (Unit) unitIterator.next();

                if ((unit.getState() == Unit.ACTIVE) && (unit.isNaval())) {
                    UnitLabel unitLabel = new UnitLabel(unit, parent);
                    unitLabel.setTransferHandler(defaultTransferHandler);
                    unitLabel.addMouseListener(pressListener);

                    inPortPanel.add(unitLabel);
                }
            }

            goldLabel.setText("Gold: " + freeColClient.getMyPlayer().getGold());

            // Only two components will be repainted!
            goldLabel.repaint(0, 0, goldLabel.getWidth(), goldLabel.getHeight());
            inPortPanel.repaint(0, 0, inPortPanel.getWidth(), inPortPanel.getHeight());
        }
    }


    /**
     * Initialize the data on the window.
     */
    public void initialize(Europe europe, Game game) {
        this.europe = europe;
        this.game = game;

        //
        // Remove the old components from the panels.
        //

        toAmericaPanel.removeAll();
        toEuropePanel.removeAll();
        inPortPanel.removeAll();
        cargoPanel.removeAll();
        marketPanel.removeAll();
        docksPanel.removeAll();

        //
        // Place new components on the panels.
        //

        UnitLabel carrier = null;
        Iterator unitIterator = europe.getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit unit = (Unit) unitIterator.next();

            UnitLabel unitLabel = new UnitLabel(unit, parent);
            unitLabel.setTransferHandler(defaultTransferHandler);
            unitLabel.addMouseListener(pressListener);

            if (((unit.getState() == Unit.ACTIVE) || (unit.getState() == Unit.SENTRY)) && (!unit.isNaval())) {
                docksPanel.add(unitLabel, false);
            } else if (unit.getState() == Unit.ACTIVE) {
                carrier = unitLabel;
                inPortPanel.add(unitLabel);
            } else if (unit.getState() == Unit.TO_EUROPE) {
                toEuropePanel.add(unitLabel, false);
            } else if (unit.getState() == Unit.TO_AMERICA) {
                toAmericaPanel.add(unitLabel, false);
            }
        }

        for (int i = 0; i < Goods.NUMBER_OF_TYPES; i++) {
            MarketLabel marketLabel = new MarketLabel(i, game.getMarket(), parent);
            marketLabel.setTransferHandler(defaultTransferHandler);
            marketLabel.addMouseListener(pressListener);
            ((JPanel)marketPanel).add(marketLabel);
        }

        setSelectedUnit(carrier);
        updateGoldLabel();
    }
    
    
    /**
    * Updates the gold label.
    */
    public void updateGoldLabel() {
        goldLabel.setText("Gold: " + freeColClient.getMyPlayer().getGold());
    }


    /**
    * Reinitializes the panel, but keeps the currently selected unit.
    */
    public void reinitialize() {
        final UnitLabel selectedUnit = this.selectedUnit;
        initialize(europe, game);
        setSelectedUnit(selectedUnit);
    }

    /**
    * Selects a unit that is located somewhere on this panel.
    *
    * @param unit The unit that is being selected.
    */
    public void setSelectedUnit(UnitLabel unitLabel) {
        if (selectedUnit != unitLabel) {
            if (selectedUnit != null) {
                selectedUnit.setSelected(false);
            }
            cargoPanel.removeAll();
            selectedUnit = unitLabel;

            if (selectedUnit != null) {
                selectedUnit.setSelected(true);
                Unit selUnit = selectedUnit.getUnit();

                Iterator unitIterator = selUnit.getUnitIterator();
                while (unitIterator.hasNext()) {
                    Unit unit = (Unit) unitIterator.next();

                    UnitLabel label = new UnitLabel(unit, parent);
                    label.setTransferHandler(defaultTransferHandler);
                    label.addMouseListener(pressListener);

                    cargoPanel.add(label, false);
                }

                Iterator goodsIterator = selUnit.getGoodsIterator();
                while (goodsIterator.hasNext()) {
                    Goods g = (Goods) goodsIterator.next();

                    GoodsLabel label = new GoodsLabel(g, parent);
                    label.setTransferHandler(defaultTransferHandler);
                    label.addMouseListener(pressListener);

                    cargoPanel.add(label, false);
                }
            }

            updateCargoLabel();
        }
        cargoPanel.revalidate();
        refresh();
    }


    /**
    * Updates the label that is placed above the cargo panel. It shows the name
    * of the unit whose cargo is displayed and the amount of space left on that unit.
    */
    private void updateCargoLabel() {
        if (selectedUnit != null) {
            cargoLabel.setText("Cargo (" + selectedUnit.getUnit().getName() + ") space left: " + selectedUnit.getUnit().getSpaceLeft());
        } else {
            cargoLabel.setText("<html><strike>Cargo</strike></html>");
        }
    }


    /**
    * Returns the currently select unit.
    *
    * @return The currently select unit.
    */
    public Unit getSelectedUnit() {
        return selectedUnit.getUnit();
    }


    /**
     * Analyzes an event and calls the right external methods to take
     * care of the user's request.
     *
     * @param event The incoming action event
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            switch (Integer.valueOf(command).intValue()) {
                case EXIT:
                    parent.remove(this);
                    parent.showMapControls();
                    freeColClient.getInGameController().nextModelMessage();
                    break;
                case RECRUIT:
                    recruitPanel.initialize();
                    recruitPanel.setLocation(getWidth() / 2 - recruitPanel.getWidth() / 2, getHeight() / 2 - recruitPanel.getHeight() / 2);
                    add(recruitPanel, JLayeredPane.PALETTE_LAYER);
                    break;
                case RECRUIT_CANCEL:
                    remove(recruitPanel);
                    revalidate();
                    repaint(recruitPanel.getX(), recruitPanel.getY(), recruitPanel.getWidth(), recruitPanel.getHeight());
                    break;
                case RECRUIT_1:
                    inGameController.recruitUnitInEurope(1);
                    remove(recruitPanel);
                    revalidate();
                    repaint(recruitPanel.getX(), recruitPanel.getY(), recruitPanel.getWidth(), recruitPanel.getHeight());
                    refreshBuyRecruit();
                    break;
                case RECRUIT_2:
                    inGameController.recruitUnitInEurope(2);
                    remove(recruitPanel);
                    revalidate();
                    repaint(recruitPanel.getX(), recruitPanel.getY(), recruitPanel.getWidth(), recruitPanel.getHeight());
                    refreshBuyRecruit();
                    break;
                case RECRUIT_3:
                    inGameController.recruitUnitInEurope(3);
                    remove(recruitPanel);
                    revalidate();
                    repaint(recruitPanel.getX(), recruitPanel.getY(), recruitPanel.getWidth(), recruitPanel.getHeight());
                    refreshBuyRecruit();                    
                    break;
                case PURCHASE:
                    purchasePanel.initialize();
                    purchasePanel.setLocation(getWidth() / 2 - purchasePanel.getWidth() / 2, getHeight() / 2 - purchasePanel.getHeight() / 2);
                    add(purchasePanel, JLayeredPane.PALETTE_LAYER);
                    break;
                case PURCHASE_CANCEL:
                    remove(purchasePanel);
                    revalidate();
                    repaint(purchasePanel.getX(), purchasePanel.getY(), purchasePanel.getWidth(), purchasePanel.getHeight());
                    break;
                case PURCHASE_ARTILLERY:
                    inGameController.purchaseUnitFromEurope(Unit.ARTILLERY);
                    remove(purchasePanel);
                    revalidate();
                    repaint(purchasePanel.getX(), purchasePanel.getY(), purchasePanel.getWidth(), purchasePanel.getHeight());
                    refreshBuyPurchase(Unit.ARTILLERY);
                    break;
                case PURCHASE_CARAVEL:
                    inGameController.purchaseUnitFromEurope(Unit.CARAVEL);
                    remove(purchasePanel);
                    revalidate();
                    repaint(purchasePanel.getX(), purchasePanel.getY(), purchasePanel.getWidth(), purchasePanel.getHeight());
                    refreshBuyPurchase(Unit.CARAVEL);
                    break;
                case PURCHASE_MERCHANTMAN:
                    inGameController.purchaseUnitFromEurope(Unit.MERCHANTMAN);
                    remove(purchasePanel);
                    revalidate();
                    repaint(purchasePanel.getX(), purchasePanel.getY(), purchasePanel.getWidth(), purchasePanel.getHeight());
                    refreshBuyPurchase(Unit.MERCHANTMAN);
                    break;
                case PURCHASE_GALLEON:
                    inGameController.purchaseUnitFromEurope(Unit.GALLEON);
                    remove(purchasePanel);
                    revalidate();
                    repaint(purchasePanel.getX(), purchasePanel.getY(), purchasePanel.getWidth(), purchasePanel.getHeight());
                    refreshBuyPurchase(Unit.GALLEON);
                    break;
                case PURCHASE_PRIVATEER:
                    inGameController.purchaseUnitFromEurope(Unit.PRIVATEER);
                    remove(purchasePanel);
                    revalidate();
                    repaint(purchasePanel.getX(), purchasePanel.getY(), purchasePanel.getWidth(), purchasePanel.getHeight());
                    refreshBuyPurchase(Unit.PRIVATEER);
                    break;
                case PURCHASE_FRIGATE:
                    inGameController.purchaseUnitFromEurope(Unit.FRIGATE);
                    remove(purchasePanel);
                    revalidate();
                    repaint(purchasePanel.getX(), purchasePanel.getY(), purchasePanel.getWidth(), purchasePanel.getHeight());
                    refreshBuyPurchase(Unit.FRIGATE);
                    break;
                case TRAIN:
                    trainPanel.initialize();
                    trainPanel.setLocation(getWidth() / 2 - trainPanel.getWidth() / 2, getHeight() / 2 - trainPanel.getHeight() / 2);
                    add(trainPanel, JLayeredPane.PALETTE_LAYER);
                    break;
                case TRAIN_CANCEL:
                    remove(trainPanel);
                    revalidate();
                    repaint(trainPanel.getX(), trainPanel.getY(), trainPanel.getWidth(), trainPanel.getHeight());
                    break;
                case TRAIN_EXPERT_ORE_MINER:
                    inGameController.trainUnitInEurope(Unit.EXPERT_ORE_MINER);
                    remove(trainPanel);
                    revalidate();
                    repaint(trainPanel.getX(), trainPanel.getY(), trainPanel.getWidth(), trainPanel.getHeight());
                    refreshBuyRecruit();
                    break;
                case TRAIN_EXPERT_LUMBER_JACK:
                    inGameController.trainUnitInEurope(Unit.EXPERT_LUMBER_JACK);
                    remove(trainPanel);
                    revalidate();
                    repaint(trainPanel.getX(), trainPanel.getY(), trainPanel.getWidth(), trainPanel.getHeight());
                    refreshBuyRecruit();
                    break;
                case TRAIN_MASTER_GUNSMITH:
                    inGameController.trainUnitInEurope(Unit.MASTER_GUNSMITH);
                    remove(trainPanel);
                    revalidate();
                    repaint(trainPanel.getX(), trainPanel.getY(), trainPanel.getWidth(), trainPanel.getHeight());
                    refreshBuyRecruit();
                    break;
                case TRAIN_EXPERT_SILVER_MINER:
                    inGameController.trainUnitInEurope(Unit.EXPERT_SILVER_MINER);
                    remove(trainPanel);
                    revalidate();
                    repaint(trainPanel.getX(), trainPanel.getY(), trainPanel.getWidth(), trainPanel.getHeight());
                    refreshBuyRecruit();
                    break;
                case TRAIN_MASTER_FUR_TRADER:
                    inGameController.trainUnitInEurope(Unit.MASTER_FUR_TRADER);
                    remove(trainPanel);
                    revalidate();
                    repaint(trainPanel.getX(), trainPanel.getY(), trainPanel.getWidth(), trainPanel.getHeight());
                    refreshBuyRecruit();
                    break;
                case TRAIN_MASTER_CARPENTER:
                    inGameController.trainUnitInEurope(Unit.MASTER_CARPENTER);
                    remove(trainPanel);
                    revalidate();
                    repaint(trainPanel.getX(), trainPanel.getY(), trainPanel.getWidth(), trainPanel.getHeight());
                    refreshBuyRecruit();
                    break;
                case TRAIN_EXPERT_FISHERMAN:
                    inGameController.trainUnitInEurope(Unit.EXPERT_FISHERMAN);
                    remove(trainPanel);
                    revalidate();
                    repaint(trainPanel.getX(), trainPanel.getY(), trainPanel.getWidth(), trainPanel.getHeight());
                    refreshBuyRecruit();
                    break;
                case TRAIN_MASTER_BLACKSMITH:
                    inGameController.trainUnitInEurope(Unit.MASTER_BLACKSMITH);
                    remove(trainPanel);
                    revalidate();
                    repaint(trainPanel.getX(), trainPanel.getY(), trainPanel.getWidth(), trainPanel.getHeight());
                    refreshBuyRecruit();
                    break;
                case TRAIN_EXPERT_FARMER:
                    inGameController.trainUnitInEurope(Unit.EXPERT_FARMER);
                    remove(trainPanel);
                    revalidate();
                    repaint(trainPanel.getX(), trainPanel.getY(), trainPanel.getWidth(), trainPanel.getHeight());
                    refreshBuyRecruit();
                    break;
                case TRAIN_MASTER_DISTILLER:
                    inGameController.trainUnitInEurope(Unit.MASTER_DISTILLER);
                    remove(trainPanel);
                    revalidate();
                    repaint(trainPanel.getX(), trainPanel.getY(), trainPanel.getWidth(), trainPanel.getHeight());
                    refreshBuyRecruit();
                    break;
                case TRAIN_HARDY_PIONEER:
                    inGameController.trainUnitInEurope(Unit.HARDY_PIONEER);
                    remove(trainPanel);
                    revalidate();
                    repaint(trainPanel.getX(), trainPanel.getY(), trainPanel.getWidth(), trainPanel.getHeight());
                    refreshBuyRecruit();
                    break;
                case TRAIN_MASTER_TOBACCONIST:
                    inGameController.trainUnitInEurope(Unit.MASTER_TOBACCONIST);
                    remove(trainPanel);
                    revalidate();
                    repaint(trainPanel.getX(), trainPanel.getY(), trainPanel.getWidth(), trainPanel.getHeight());
                    refreshBuyRecruit();
                    break;
                case TRAIN_MASTER_WEAVER:
                    inGameController.trainUnitInEurope(Unit.MASTER_WEAVER);
                    remove(trainPanel);
                    revalidate();
                    repaint(trainPanel.getX(), trainPanel.getY(), trainPanel.getWidth(), trainPanel.getHeight());
                    refreshBuyRecruit();
                    break;
                case TRAIN_JESUIT_MISSIONARY:
                    inGameController.trainUnitInEurope(Unit.JESUIT_MISSIONARY);
                    remove(trainPanel);
                    revalidate();
                    repaint(trainPanel.getX(), trainPanel.getY(), trainPanel.getWidth(), trainPanel.getHeight());
                    refreshBuyRecruit();
                    break;
                case TRAIN_FIREBRAND_PREACHER:
                    inGameController.trainUnitInEurope(Unit.FIREBRAND_PREACHER);
                    remove(trainPanel);
                    revalidate();
                    repaint(trainPanel.getX(), trainPanel.getY(), trainPanel.getWidth(), trainPanel.getHeight());
                    refreshBuyRecruit();
                    break;
                case TRAIN_ELDER_STATESMAN:
                    inGameController.trainUnitInEurope(Unit.ELDER_STATESMAN);
                    remove(trainPanel);
                    revalidate();
                    repaint(trainPanel.getX(), trainPanel.getY(), trainPanel.getWidth(), trainPanel.getHeight());
                    refreshBuyRecruit();
                    break;
                case TRAIN_VETERAN_SOLDIER:
                    inGameController.trainUnitInEurope(Unit.VETERAN_SOLDIER);
                    remove(trainPanel);
                    revalidate();
                    repaint(trainPanel.getX(), trainPanel.getY(), trainPanel.getWidth(), trainPanel.getHeight());
                    refreshBuyRecruit();
                    break;
                default:
                    logger.warning("Invalid action");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid action number");
        }
    }
    

    /**
    * Paints this component.
    * @param g The graphics context in which to paint.
    */
    public void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
    }
    
    
    
    
    

    /**
    * A panel that holds UnitsLabels that represent Units that are
    * going to America.
    */
    public final class ToAmericaPanel extends JPanel {
        private final EuropePanel europePanel;

        
        
        /**
        * Creates this ToAmericaPanel.
        * @param europePanel The panel that holds this ToAmericaPanel.
        */
        public ToAmericaPanel(EuropePanel europePanel) {
            this.europePanel = europePanel;
        }

        
        

        /**
        * Adds a component to this ToAmericaPanel and makes sure that the unit
        * that the component represents gets modified so that it will sail to
        * America.
        * @param comp The component to add to this ToAmericaPanel.
        * @param editState Must be set to 'true' if the state of the component
        * that is added (which should be a dropped component representing a Unit)
        * should be changed so that the underlying unit is now sailing to America.
        * @return The component argument.
        */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (comp instanceof UnitLabel) {
                    comp.getParent().remove(comp);
                    Unit unit = ((UnitLabel)comp).getUnit();
                    inGameController.moveToAmerica(unit);
                } else {
                    logger.warning("An invalid component got dropped on this ToAmericaPanel.");
                    return null;
                }
            }
            setSelectedUnit(null);
            Component c = add(comp);
            europePanel.refresh();
            return c;
        }
        
    }

    
    
    

    /**
    * A panel that holds UnitsLabels that represent Units that are
    * going to Europe.
    */
    public final class ToEuropePanel extends JPanel {
        private final EuropePanel europePanel;

        

        /**
        * Creates this ToEuropePanel.
        * @param europePanel The panel that holds this ToEuropePanel.
        */
        public ToEuropePanel(EuropePanel europePanel) {
            this.europePanel = europePanel;
        }

        

        /**
        * Adds a component to this ToEuropePanel and makes sure that the unit
        * that the component represents gets modified so that it will sail to
        * Europe.
        * @param comp The component to add to this ToEuropePanel.
        * @param editState Must be set to 'true' if the state of the component
        * that is added (which should be a dropped component representing a Unit)
        * should be changed so that the underlying unit is now sailing to Europe.
        * @return The component argument.
        */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (comp instanceof UnitLabel) {
                    comp.getParent().remove(comp);
                    Unit unit = ((UnitLabel)comp).getUnit();
                    inGameController.moveToEurope(unit);
                } else {
                    logger.warning("An invalid component got dropped on this ToEuropePanel.");
                    return null;
                }
            }
            setSelectedUnit(null);
            Component c = add(comp);
            europePanel.refresh();
            return c;
        }
    }

    
    
    

    /**
    * A panel that holds UnitsLabels that represent naval Units that are
    * waiting in Europe.
    */
    public final class InPortPanel extends JPanel {
        
        
        /**
        * Adds a component to this InPortPanel.
        * @param comp The component to add to this InPortPanel.
        * @return The component argument.
        */
        public Component add(Component comp) {
            return super.add(comp);
        }
    }

    
    
    

    /**
    * A panel that holds UnitsLabels that represent Units that are
    * waiting on the docks in Europe.
    */
    public final class DocksPanel extends JPanel {
        private final EuropePanel europePanel;



        /**
        * Creates this DocksPanel.
        * @param europePanel The panel that holds this DocksPanel.
        */
        public DocksPanel(EuropePanel europePanel) {
            this.europePanel = europePanel;
        }



        /**
        * Adds a component to this DocksPanel and makes sure that the unit
        * that the component represents gets modified so that it will wait
        * on the docks in Europe.
        *
        * @param comp The component to add to this DocksPanel.
        * @param editState Must be set to 'true' if the state of the component
        *                  that is added (which should be a dropped component
        *                  representing a Unit) should be changed so that the
        *                  underlying unit will wait on the docks in Europe.
        * @return The component argument.
        */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (comp instanceof UnitLabel) {
                    comp.getParent().remove(comp);
                    Unit unit = ((UnitLabel)comp).getUnit();
                    inGameController.leaveShip(unit);
                } else {
                    logger.warning("An invalid component got dropped on this DocksPanel.");
                    return null;
                }
            }
            updateCargoLabel();
            Component c = add(comp);
            europePanel.refresh();
            return c;
        }
    }





    /**
    * A panel that holds units and goods that represent Units and cargo that are
    * on board the currently selected ship.
    */
    public final class CargoPanel extends JPanel {
        private final EuropePanel europePanel;



        /**
        * Creates this CargoPanel.
        * @param europePanel The panel that holds this CargoPanel.
        */
        public CargoPanel(EuropePanel europePanel) {
            this.europePanel = europePanel;
        }



        /**
        * Adds a component to this CargoPanel and makes sure that the unit
        * or good that the component represents gets modified so that it is
        * on board the currently selected ship.
        * @param comp The component to add to this CargoPanel.
        * @param editState Must be set to 'true' if the state of the component
        * that is added (which should be a dropped component representing a Unit or
        * good) should be changed so that the underlying unit or goods are
        * on board the currently selected ship.
        * @return The component argument.
        */
        public Component add(Component comp, boolean editState) {
            if (selectedUnit == null) {
                return null;
            }

            if (editState) {
                if (comp instanceof UnitLabel) {
                    Unit unit = ((UnitLabel)comp).getUnit();
                    inGameController.boardShip(unit, selectedUnit.getUnit());
                    comp.getParent().remove(comp);                  
                } else if (comp instanceof MarketLabel) {
                    if ((freeColClient.getMyPlayer().getGold() >= (game.getMarket().costToBuy(((MarketLabel)comp).getType()) * 100))) {
                        inGameController.buyGoods(((MarketLabel)comp).getType(), 100, selectedUnit.getUnit());
                    }
                    updateCargoLabel();
                    goldLabel.setText("Gold: " + freeColClient.getMyPlayer().getGold());
                    goldLabel.repaint(0, 0, goldLabel.getWidth(), goldLabel.getHeight());

                    // TODO: Make this look prettier :-)
                    UnitLabel t = selectedUnit;
                    selectedUnit = null;
                    setSelectedUnit(t);

                    europePanel.getMarketPanel().revalidate();
                    revalidate();
                    europePanel.refresh();
                    return comp;
                } else {
                    logger.warning("An invalid component got dropped on this CargoPanel.");
                    return null;
                }
            }

            updateCargoLabel();
            Component c = add(comp);
            europePanel.refresh();
            return c;
        }

        public boolean isActive() {
            return (getSelectedUnit() != null);
        }        
    }




    /**
    * A panel that shows goods available for purchase in Europe.
    */
    public final class MarketPanel extends JPanel {
        private final EuropePanel europePanel;


        /**
        * Creates this MarketPanel.
        * @param europePanel The panel that holds this CargoPanel.
        */
        public MarketPanel(EuropePanel europePanel) {
            this.europePanel = europePanel;
            setLayout(new GridLayout(2,8));
        }


        /**
        * If a GoodsLabel is dropped here, sell the goods.
        * @param comp The component to add to this MarketPanel.
        * @param editState Must be set to 'true' if the state of the component
        * that is added (which should be a dropped component representing goods)
        * should be sold.
        * @return The component argument.
        */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (comp instanceof GoodsLabel) {
                    comp.getParent().remove(comp);
                    inGameController.sellGoods(((GoodsLabel)comp).getGoods());
                    updateCargoLabel();
                    goldLabel.setText("Gold: " + freeColClient.getMyPlayer().getGold());
                    goldLabel.repaint(0, 0, goldLabel.getWidth(), goldLabel.getHeight());
                    europePanel.getCargoPanel().revalidate();
                    revalidate();
                    europePanel.refresh();
                    return comp;
                } else {
                    logger.warning("An invalid component got dropped on this MarketPanel.");
                    return null;
                }
            }
            europePanel.refresh();
            return comp;
        }
        public void remove(Component comp) {
          // Don't remove the marketLabel.
        }
    }


    /**
    * Returns a pointer to the <code>cargoPanel</code>-object in use.
    */
    public final CargoPanel getCargoPanel() {
        return cargoPanel;
    }

    /**
    * Returns a pointer to the <code>marketPanel</code>-object in use.
    */
    public final MarketPanel getMarketPanel() {
        return marketPanel;
    }    
    

    /**
    * The panel that allows a user to recruit people in Europe.
    */
    public final class RecruitPanel extends JPanel {
        private final JLabel    price;
        private final JButton   person1,
                                person2,
                                person3;

        /**
        * The constructor to use.
        * @param actionListener The ActionListener for this panel's buttons.
        */
        public RecruitPanel(ActionListener actionListener) {
            JLabel  question = new JLabel("Click one of the following individuals to"),
                    question2 = new JLabel("recruit them."),
                    priceLabel = new JLabel("Their price:");
            JButton cancel = new JButton("Cancel");

            price = new JLabel();
            person1 = new JButton();
            person2 = new JButton();
            person3 = new JButton();

            initialize();

            question.setSize(300, 20);
            question2.setSize(300, 20);
            priceLabel.setSize(80, 20);
            price.setSize(60, 20);
            person1.setSize(200, 20);
            person2.setSize(200, 20);
            person3.setSize(200, 20);
            cancel.setSize(80, 20);

            question.setLocation(10, 10);
            question2.setLocation(10, 30);
            priceLabel.setLocation(10, 55);
            price.setLocation(95, 55);
            person1.setLocation(60, 90);
            person2.setLocation(60, 115);
            person3.setLocation(60, 140);
            cancel.setLocation(120, 175);

            setLayout(null);

            person1.setActionCommand(String.valueOf(RECRUIT_1));
            person2.setActionCommand(String.valueOf(RECRUIT_2));
            person3.setActionCommand(String.valueOf(RECRUIT_3));
            cancel.setActionCommand(String.valueOf(RECRUIT_CANCEL));

            person1.addActionListener(actionListener);
            person2.addActionListener(actionListener);
            person3.addActionListener(actionListener);
            cancel.addActionListener(actionListener);

            add(question);
            add(question2);
            add(priceLabel);
            add(price);
            add(person1);
            add(person2);
            add(person3);
            add(cancel);

            try {
                BevelBorder border = new BevelBorder(BevelBorder.RAISED);
                setBorder(border);
            }
            catch(Exception e) {
            }

            setSize(320, 205);
        }


        /**
        * Updates this panel's labels so that the information it displays is up to date.
        */
        public void initialize() {
            if ((game != null) && (freeColClient.getMyPlayer() != null)) {
                price.setText(Integer.toString(freeColClient.getMyPlayer().getRecruitPrice()) + " gold");

                person1.setText(Unit.getName(europe.getRecruitable(1)));
                person2.setText(Unit.getName(europe.getRecruitable(2)));
                person3.setText(Unit.getName(europe.getRecruitable(3)));


                if (freeColClient.getMyPlayer().getRecruitPrice() > freeColClient.getMyPlayer().getGold()) {
                    person1.setEnabled(false);
                    person2.setEnabled(false);
                    person3.setEnabled(false);
                } else {
                    person1.setEnabled(true);
                    person2.setEnabled(true);
                    person3.setEnabled(true);
                }
            }
        }
    }

    
    
    

    /**
    * The panel that allows a user to purchase ships and artillery in Europe.
    */
    public final class PurchasePanel extends JPanel {
        private JButton   artilleryButton,
                                caravelButton,
                                merchantmanButton,
                                galleonButton,
                                privateerButton,
                                frigateButton;
        private JLabel artilleryLabel = new JLabel("?");

        /**
        * The constructor to use.
        * @param actionListener The ActionListener for this panel's buttons.
        */
        public PurchasePanel(ActionListener actionListener) {
            JLabel  question = new JLabel("Click one of the following items to"),
                    question2 = new JLabel("purchase them."),
                    caravelLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.CARAVEL))),
                    merchantmanLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.MERCHANTMAN))),
                    galleonLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.GALLEON))),
                    privateerLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.PRIVATEER))),
                    frigateLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.FRIGATE)));
            JButton cancel = new JButton("Cancel");

            artilleryButton = new JButton(Unit.getName(Unit.ARTILLERY));
            caravelButton = new JButton(Unit.getName(Unit.CARAVEL));
            merchantmanButton = new JButton(Unit.getName(Unit.MERCHANTMAN));
            galleonButton = new JButton(Unit.getName(Unit.GALLEON));
            privateerButton = new JButton(Unit.getName(Unit.PRIVATEER));
            frigateButton = new JButton(Unit.getName(Unit.FRIGATE));

            question.setSize(300, 20);
            question2.setSize(300, 20);
            artilleryLabel.setSize(40, 20);
            caravelLabel.setSize(40, 20);
            merchantmanLabel.setSize(40, 20);
            galleonLabel.setSize(40, 20);
            privateerLabel.setSize(40, 20);
            frigateLabel.setSize(40, 20);
            cancel.setSize(80, 20);
            artilleryButton.setSize(200, 20);
            caravelButton.setSize(200, 20);
            merchantmanButton.setSize(200, 20);
            galleonButton.setSize(200, 20);
            privateerButton.setSize(200, 20);
            frigateButton.setSize(200, 20);

            question.setLocation(10, 10);
            question2.setLocation(10, 30);
            artilleryLabel.setLocation(245, 65);
            caravelLabel.setLocation(245, 90);
            merchantmanLabel.setLocation(245, 115);
            galleonLabel.setLocation(245, 140);
            privateerLabel.setLocation(245, 165);
            frigateLabel.setLocation(245, 190);
            cancel.setLocation(120, 225);
            artilleryButton.setLocation(35, 65);
            caravelButton.setLocation(35, 90);
            merchantmanButton.setLocation(35, 115);
            galleonButton.setLocation(35, 140);
            privateerButton.setLocation(35, 165);
            frigateButton.setLocation(35, 190);

            setLayout(null);

            cancel.setActionCommand(String.valueOf(PURCHASE_CANCEL));
            artilleryButton.setActionCommand(String.valueOf(PURCHASE_ARTILLERY));
            caravelButton.setActionCommand(String.valueOf(PURCHASE_CARAVEL));
            merchantmanButton.setActionCommand(String.valueOf(PURCHASE_MERCHANTMAN));
            galleonButton.setActionCommand(String.valueOf(PURCHASE_GALLEON));
            privateerButton.setActionCommand(String.valueOf(PURCHASE_PRIVATEER));
            frigateButton.setActionCommand(String.valueOf(PURCHASE_FRIGATE));

            cancel.addActionListener(actionListener);
            artilleryButton.addActionListener(actionListener);
            caravelButton.addActionListener(actionListener);
            merchantmanButton.addActionListener(actionListener);
            galleonButton.addActionListener(actionListener);
            privateerButton.addActionListener(actionListener);
            frigateButton.addActionListener(actionListener);

            add(question);
            add(question2);
            add(artilleryLabel);
            add(caravelLabel);
            add(merchantmanLabel);
            add(galleonLabel);
            add(privateerLabel);
            add(frigateLabel);
            add(cancel);
            add(artilleryButton);
            add(caravelButton);
            add(merchantmanButton);
            add(galleonButton);
            add(privateerButton);
            add(frigateButton);

            try {
                BevelBorder border = new BevelBorder(BevelBorder.RAISED);
                setBorder(border);
            } catch(Exception e) {}

            setSize(320, 255);
        }


        /**
        * Updates this panel's labels so that the information it displays is up to date.
        */
        public void initialize() {
            if ((game != null) && (freeColClient.getMyPlayer() != null)) {
                Player gameOwner = freeColClient.getMyPlayer();

                artilleryLabel.setText(Integer.toString(europe.getArtilleryPrice()));

                if (europe.getArtilleryPrice() > gameOwner.getGold()) {
                    artilleryButton.setEnabled(false);
                } else {
                    artilleryButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.CARAVEL) > gameOwner.getGold()) {
                    caravelButton.setEnabled(false);
                } else {
                    caravelButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.MERCHANTMAN) > gameOwner.getGold()) {
                    merchantmanButton.setEnabled(false);
                } else {
                    merchantmanButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.GALLEON) > gameOwner.getGold()) {
                    galleonButton.setEnabled(false);
                } else {
                    galleonButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.PRIVATEER) > gameOwner.getGold()) {
                    privateerButton.setEnabled(false);
                } else {
                    privateerButton.setEnabled(true);
                }
                
                if (Unit.getPrice(Unit.FRIGATE) > gameOwner.getGold()) {
                    frigateButton.setEnabled(false);
                } else {
                    frigateButton.setEnabled(true);
                }
            }
        }
    }

    
    
    

    /**
    * The panel that allows a user to train people in Europe.
    */
    public final class TrainPanel extends JPanel {
        private JButton   expertOreMinerButton,
                          expertLumberJackButton,
                          masterGunsmithButton,
                          expertSilverMinerButton,
                          masterFurTraderButton,
                          masterCarpenterButton,
                          expertFishermanButton,
                          masterBlacksmithButton,
                          expertFarmerButton,
                          masterDistillerButton,
                          hardyPioneerButton,
                          masterTobacconistButton,
                          masterWeaverButton,
                          jesuitMissionaryButton,
                          firebrandPreacherButton,
                          elderStatesmanButton,
                          veteranSoldierButton;
                          
                          

        /**
        * The constructor to use.
        * @param actionListener The ActionListener for this panel's buttons.
        */
        public TrainPanel(ActionListener actionListener) {
            JLabel  question = new JLabel("Click one of the following individuals to"),
                    question2 = new JLabel("train them."),
                    expertOreMinerLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.EXPERT_ORE_MINER))),
                    expertLumberJackLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.EXPERT_LUMBER_JACK))),
                    masterGunsmithLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.MASTER_GUNSMITH))),
                    expertSilverMinerLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.EXPERT_SILVER_MINER))),
                    masterFurTraderLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.MASTER_FUR_TRADER))),
                    masterCarpenterLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.MASTER_CARPENTER))),
                    expertFishermanLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.EXPERT_FISHERMAN))),
                    masterBlacksmithLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.MASTER_BLACKSMITH))),
                    expertFarmerLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.EXPERT_FARMER))),
                    masterDistillerLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.MASTER_DISTILLER))),
                    hardyPioneerLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.HARDY_PIONEER))),
                    masterTobacconistLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.MASTER_TOBACCONIST))),
                    masterWeaverLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.MASTER_WEAVER))),
                    jesuitMissionaryLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.JESUIT_MISSIONARY))),
                    firebrandPreacherLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.FIREBRAND_PREACHER))),
                    elderStatesmanLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.ELDER_STATESMAN))),
                    veteranSoldierLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.VETERAN_SOLDIER)));
            JButton cancel = new JButton("Cancel");

            expertOreMinerButton = new JButton(Unit.getName(Unit.EXPERT_ORE_MINER));
            expertLumberJackButton = new JButton(Unit.getName(Unit.EXPERT_LUMBER_JACK));
            masterGunsmithButton = new JButton(Unit.getName(Unit.MASTER_GUNSMITH));
            expertSilverMinerButton = new JButton(Unit.getName(Unit.EXPERT_SILVER_MINER));
            masterFurTraderButton = new JButton(Unit.getName(Unit.MASTER_FUR_TRADER));
            masterCarpenterButton = new JButton(Unit.getName(Unit.MASTER_CARPENTER));
            expertFishermanButton = new JButton(Unit.getName(Unit.EXPERT_FISHERMAN));
            masterBlacksmithButton = new JButton(Unit.getName(Unit.MASTER_BLACKSMITH));
            expertFarmerButton = new JButton(Unit.getName(Unit.EXPERT_FARMER));
            masterDistillerButton = new JButton(Unit.getName(Unit.MASTER_DISTILLER));
            hardyPioneerButton = new JButton(Unit.getName(Unit.HARDY_PIONEER));
            masterTobacconistButton = new JButton(Unit.getName(Unit.MASTER_TOBACCONIST));
            masterWeaverButton = new JButton(Unit.getName(Unit.MASTER_WEAVER));
            jesuitMissionaryButton = new JButton(Unit.getName(Unit.JESUIT_MISSIONARY));
            firebrandPreacherButton = new JButton(Unit.getName(Unit.FIREBRAND_PREACHER));
            elderStatesmanButton = new JButton(Unit.getName(Unit.ELDER_STATESMAN));
            veteranSoldierButton = new JButton(Unit.getName(Unit.VETERAN_SOLDIER));

            question.setSize(300, 20);
            question2.setSize(300, 20);
            expertOreMinerLabel.setSize(40, 20);
            expertLumberJackLabel.setSize(40, 20);
            masterGunsmithLabel.setSize(40, 20);
            expertSilverMinerLabel.setSize(40, 20);
            masterFurTraderLabel.setSize(40, 20);
            masterCarpenterLabel.setSize(40, 20);
            expertFishermanLabel.setSize(40, 20);
            masterBlacksmithLabel.setSize(40, 20);
            expertFarmerLabel.setSize(40, 20);
            masterDistillerLabel.setSize(40, 20);
            hardyPioneerLabel.setSize(40, 20);
            masterTobacconistLabel.setSize(40, 20);
            masterWeaverLabel.setSize(40, 20);
            jesuitMissionaryLabel.setSize(40, 20);
            firebrandPreacherLabel.setSize(40, 20);
            elderStatesmanLabel.setSize(40, 20);
            veteranSoldierLabel.setSize(40, 20);
            cancel.setSize(80, 20);
            expertOreMinerButton.setSize(200, 20);
            expertLumberJackButton.setSize(200, 20);
            masterGunsmithButton.setSize(200, 20);
            expertSilverMinerButton.setSize(200, 20);
            masterFurTraderButton.setSize(200, 20);
            masterCarpenterButton.setSize(200, 20);
            expertFishermanButton.setSize(200, 20);
            masterBlacksmithButton.setSize(200, 20);
            expertFarmerButton.setSize(200, 20);
            masterDistillerButton.setSize(200, 20);
            hardyPioneerButton.setSize(200, 20);
            masterTobacconistButton.setSize(200, 20);
            masterWeaverButton.setSize(200, 20);
            jesuitMissionaryButton.setSize(200, 20);
            firebrandPreacherButton.setSize(200, 20);
            elderStatesmanButton.setSize(200, 20);
            veteranSoldierButton.setSize(200, 20);
            
            question.setLocation(10, 10);
            question2.setLocation(10, 30);
            expertOreMinerLabel.setLocation(245, 65);
            expertLumberJackLabel.setLocation(245, 90);
            masterGunsmithLabel.setLocation(245, 115);
            expertSilverMinerLabel.setLocation(245, 140);
            masterFurTraderLabel.setLocation(245, 165);
            masterCarpenterLabel.setLocation(245, 190);
            expertFishermanLabel.setLocation(245, 215);
            masterBlacksmithLabel.setLocation(245, 240);
            expertFarmerLabel.setLocation(245, 265);
            masterDistillerLabel.setLocation(245, 290);
            hardyPioneerLabel.setLocation(245, 315);
            masterTobacconistLabel.setLocation(245, 340);
            masterWeaverLabel.setLocation(245, 365);
            jesuitMissionaryLabel.setLocation(245, 390);
            firebrandPreacherLabel.setLocation(245, 415);
            elderStatesmanLabel.setLocation(245, 440);
            veteranSoldierLabel.setLocation(245, 465);
            cancel.setLocation(120, 500);
            expertOreMinerButton.setLocation(35, 65);
            expertLumberJackButton.setLocation(35, 90);
            masterGunsmithButton.setLocation(35, 115);
            expertSilverMinerButton.setLocation(35, 140);
            masterFurTraderButton.setLocation(35, 165);
            masterCarpenterButton.setLocation(35, 190);
            expertFishermanButton.setLocation(35, 215);
            masterBlacksmithButton.setLocation(35, 240);
            expertFarmerButton.setLocation(35, 265);
            masterDistillerButton.setLocation(35, 290);
            hardyPioneerButton.setLocation(35, 315);
            masterTobacconistButton.setLocation(35, 340);
            masterWeaverButton.setLocation(35, 365);
            jesuitMissionaryButton.setLocation(35, 390);
            firebrandPreacherButton.setLocation(35, 415);
            elderStatesmanButton.setLocation(35, 440);
            veteranSoldierButton.setLocation(35, 465);
            
            setLayout(null);

            cancel.setActionCommand(String.valueOf(TRAIN_CANCEL));
            expertOreMinerButton.setActionCommand(String.valueOf(TRAIN_EXPERT_ORE_MINER));
            expertLumberJackButton.setActionCommand(String.valueOf(TRAIN_EXPERT_LUMBER_JACK));
            masterGunsmithButton.setActionCommand(String.valueOf(TRAIN_MASTER_GUNSMITH));
            expertSilverMinerButton.setActionCommand(String.valueOf(TRAIN_EXPERT_SILVER_MINER));
            masterFurTraderButton.setActionCommand(String.valueOf(TRAIN_MASTER_FUR_TRADER));
            masterCarpenterButton.setActionCommand(String.valueOf(TRAIN_MASTER_CARPENTER));
            expertFishermanButton.setActionCommand(String.valueOf(TRAIN_EXPERT_FISHERMAN));
            masterBlacksmithButton.setActionCommand(String.valueOf(TRAIN_MASTER_BLACKSMITH));
            expertFarmerButton.setActionCommand(String.valueOf(TRAIN_EXPERT_FARMER));
            masterDistillerButton.setActionCommand(String.valueOf(TRAIN_MASTER_DISTILLER));
            hardyPioneerButton.setActionCommand(String.valueOf(TRAIN_HARDY_PIONEER));
            masterTobacconistButton.setActionCommand(String.valueOf(TRAIN_MASTER_TOBACCONIST));
            masterWeaverButton.setActionCommand(String.valueOf(TRAIN_MASTER_WEAVER));
            jesuitMissionaryButton.setActionCommand(String.valueOf(TRAIN_JESUIT_MISSIONARY));
            firebrandPreacherButton.setActionCommand(String.valueOf(TRAIN_FIREBRAND_PREACHER));
            elderStatesmanButton.setActionCommand(String.valueOf(TRAIN_ELDER_STATESMAN));
            veteranSoldierButton.setActionCommand(String.valueOf(TRAIN_VETERAN_SOLDIER));
            
            cancel.addActionListener(actionListener);
            expertOreMinerButton.addActionListener(actionListener);
            expertLumberJackButton.addActionListener(actionListener);
            masterGunsmithButton.addActionListener(actionListener);
            expertSilverMinerButton.addActionListener(actionListener);
            masterFurTraderButton.addActionListener(actionListener);
            masterCarpenterButton.addActionListener(actionListener);
            expertFishermanButton.addActionListener(actionListener);
            masterBlacksmithButton.addActionListener(actionListener);
            expertFarmerButton.addActionListener(actionListener);
            masterDistillerButton.addActionListener(actionListener);
            hardyPioneerButton.addActionListener(actionListener);
            masterTobacconistButton.addActionListener(actionListener);
            masterWeaverButton.addActionListener(actionListener);
            jesuitMissionaryButton.addActionListener(actionListener);
            firebrandPreacherButton.addActionListener(actionListener);
            elderStatesmanButton.addActionListener(actionListener);
            veteranSoldierButton.addActionListener(actionListener);
            
            add(question);
            add(question2);
            add(expertOreMinerLabel);
            add(expertLumberJackLabel);
            add(masterGunsmithLabel);
            add(expertSilverMinerLabel);
            add(masterFurTraderLabel);
            add(masterCarpenterLabel);
            add(expertFishermanLabel);
            add(masterBlacksmithLabel);
            add(expertFarmerLabel);
            add(masterDistillerLabel);
            add(hardyPioneerLabel);
            add(masterTobacconistLabel);
            add(masterWeaverLabel);
            add(jesuitMissionaryLabel);
            add(firebrandPreacherLabel);
            add(elderStatesmanLabel);
            add(veteranSoldierLabel);
            add(cancel);
            add(expertOreMinerButton);
            add(expertLumberJackButton);
            add(masterGunsmithButton);
            add(expertSilverMinerButton);
            add(masterFurTraderButton);
            add(masterCarpenterButton);
            add(expertFishermanButton);
            add(masterBlacksmithButton);
            add(expertFarmerButton);
            add(masterDistillerButton);
            add(hardyPioneerButton);
            add(masterTobacconistButton);
            add(masterWeaverButton);
            add(jesuitMissionaryButton);
            add(firebrandPreacherButton);
            add(elderStatesmanButton);
            add(veteranSoldierButton);
            
            try {
                BevelBorder border = new BevelBorder(BevelBorder.RAISED);
                setBorder(border);
            } catch(Exception e) {}
            
            setSize(320, 530);
        }
        

        /**
        * Updates this panel's labels so that the information it displays is up to date.
        */
        public void initialize() {
            if ((game != null) && (freeColClient.getMyPlayer() != null)) {
                Player gameOwner = freeColClient.getMyPlayer();

                if (Unit.getPrice(Unit.EXPERT_ORE_MINER) > gameOwner.getGold()) {
                    expertOreMinerButton.setEnabled(false);
                }
                else {
                    expertOreMinerButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.EXPERT_LUMBER_JACK) > gameOwner.getGold()) {
                    expertLumberJackButton.setEnabled(false);
                }
                else {
                    expertLumberJackButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.MASTER_GUNSMITH) > gameOwner.getGold()) {
                    masterGunsmithButton.setEnabled(false);
                }
                else {
                    masterGunsmithButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.EXPERT_SILVER_MINER) > gameOwner.getGold()) {
                    expertSilverMinerButton.setEnabled(false);
                }
                else {
                    expertSilverMinerButton.setEnabled(true);
                }
                
                if (Unit.getPrice(Unit.MASTER_FUR_TRADER) > gameOwner.getGold()) {
                    masterFurTraderButton.setEnabled(false);
                }
                else {
                    masterFurTraderButton.setEnabled(true);
                }
                
                if (Unit.getPrice(Unit.MASTER_CARPENTER) > gameOwner.getGold()) {
                    masterCarpenterButton.setEnabled(false);
                }
                else {
                    masterCarpenterButton.setEnabled(true);
                }
                
                if (Unit.getPrice(Unit.EXPERT_FISHERMAN) > gameOwner.getGold()) {
                    expertFishermanButton.setEnabled(false);
                }
                else {
                    expertFishermanButton.setEnabled(true);
                }
                
                if (Unit.getPrice(Unit.MASTER_BLACKSMITH) > gameOwner.getGold()) {
                    masterBlacksmithButton.setEnabled(false);
                }
                else {
                    masterBlacksmithButton.setEnabled(true);
                }
                
                if (Unit.getPrice(Unit.EXPERT_FARMER) > gameOwner.getGold()) {
                    expertFarmerButton.setEnabled(false);
                }
                else {
                    expertFarmerButton.setEnabled(true);
                }
                
                if (Unit.getPrice(Unit.MASTER_DISTILLER) > gameOwner.getGold()) {
                    masterDistillerButton.setEnabled(false);
                }
                else {
                    masterDistillerButton.setEnabled(true);
                }
                
                if (Unit.getPrice(Unit.HARDY_PIONEER) > gameOwner.getGold()) {
                    hardyPioneerButton.setEnabled(false);
                }
                else {
                    hardyPioneerButton.setEnabled(true);
                }
                
                if (Unit.getPrice(Unit.MASTER_TOBACCONIST) > gameOwner.getGold()) {
                    masterTobacconistButton.setEnabled(false);
                }
                else {
                    masterTobacconistButton.setEnabled(true);
                }
                
                if (Unit.getPrice(Unit.MASTER_WEAVER) > gameOwner.getGold()) {
                    masterWeaverButton.setEnabled(false);
                }
                else {
                    masterWeaverButton.setEnabled(true);
                }
                
                if (Unit.getPrice(Unit.JESUIT_MISSIONARY) > gameOwner.getGold()) {
                    jesuitMissionaryButton.setEnabled(false);
                }
                else {
                    jesuitMissionaryButton.setEnabled(true);
                }
                
                if (Unit.getPrice(Unit.FIREBRAND_PREACHER) > gameOwner.getGold()) {
                    firebrandPreacherButton.setEnabled(false);
                }
                else {
                    firebrandPreacherButton.setEnabled(true);
                }
                
                if (Unit.getPrice(Unit.ELDER_STATESMAN) > gameOwner.getGold()) {
                    elderStatesmanButton.setEnabled(false);
                }
                else {
                    elderStatesmanButton.setEnabled(true);
                }
                
                if (Unit.getPrice(Unit.VETERAN_SOLDIER) > gameOwner.getGold()) {
                    veteranSoldierButton.setEnabled(false);
                }
                else {
                    veteranSoldierButton.setEnabled(true);
                }
            }
        }
    }
    


}
