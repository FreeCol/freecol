package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTradeItem;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.GoldTradeItem;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsTradeItem;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StanceTradeItem;
import net.sf.freecol.common.model.TradeItem;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitTradeItem;
import cz.autel.dmi.HIGLayout;

/**
 * The panel that allows negotiations between players.
 */
public final class NegotiationDialog extends FreeColDialog implements ActionListener {
    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private static final int GOLD = 1, STANCE = 2;

    private static Logger logger = Logger.getLogger(NegotiationDialog.class.getName());

    private static FreeColClient freeColClient;

    private DiplomaticTrade agreement;

    private JButton acceptButton, cancelButton, sendButton;
    private StanceTradeItemPanel stance;
    private GoldTradeItemPanel goldOffer, goldDemand;
    private ColonyTradeItemPanel colonyOffer, colonyDemand;
    private GoodsTradeItemPanel goodsOffer, goodsDemand;
    //private UnitTradeItemPanel unitOffer, unitDemand;
    private JTextPane summary;

    private final Unit unit;
    private final Settlement settlement;
    private Player player;
    private Player otherPlayer;
    private Player sender;
    private Player recipient;

    /**
     * Creates a new <code>NegotiationDialog</code> instance.
     *
     * @param parent a <code>Canvas</code> value
     * @param unit an <code>Unit</code> value
     * @param settlement a <code>Settlement</code> value
     */
    public NegotiationDialog(Canvas parent, Unit unit, Settlement settlement) {
        this(parent, unit, settlement, null);
    }

    /**
     * Creates a new <code>NegotiationDialog</code> instance.
     *
     * @param parent a <code>Canvas</code> value
     * @param unit an <code>Unit</code> value
     * @param settlement a <code>Settlement</code> value
     * @param items a <code>List</code> of <code>TradeItem</code> values.
     */
    public NegotiationDialog(Canvas parent, Unit unit, Settlement settlement, DiplomaticTrade agreement) {
        super(parent);
        setFocusCycleRoot(true);

        this.unit = unit;
        this.settlement = settlement;
        this.freeColClient = parent.getClient();
        this.player = freeColClient.getMyPlayer();
        this.sender = unit.getOwner();
        this.recipient = settlement.getOwner();
        if (agreement == null) {
            this.agreement = new DiplomaticTrade(unit.getGame(), sender, recipient);
        } else {
            this.agreement = agreement;
        }
        if (sender == player) {
            this.otherPlayer = recipient;
        } else {
            this.otherPlayer = sender;
        }

        if (player.getStance(otherPlayer) == Player.WAR) {
            if (!hasPeaceOffer()) {
                int stance = Player.PEACE;
                agreement.add(new StanceTradeItem(freeColClient.getGame(), player, otherPlayer, stance));
                agreement.add(new StanceTradeItem(freeColClient.getGame(), otherPlayer, player, stance));
            }
        }

        summary = new JTextPane();
        summary.setOpaque(false);
        summary.setEditable(false);

        StyledDocument document = summary.getStyledDocument();
        //Initialize some styles.
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        
        Style regular = document.addStyle("regular", def);
        StyleConstants.setFontFamily(def, "Dialog");
        StyleConstants.setBold(def, true);
        StyleConstants.setFontSize(def, 12);

        Style buttonStyle = document.addStyle("button", regular);
        StyleConstants.setForeground(buttonStyle, LINK_COLOR);
    }

    /**
     * Set up the dialog.
     *
     */
    public void initialize() {
        sendButton = new JButton(Messages.message("negotiationDialog.send"));
        sendButton.addActionListener(this);
        sendButton.setActionCommand("send");
        FreeColPanel.enterPressesWhenFocused(sendButton);

        acceptButton = new JButton(Messages.message("negotiationDialog.accept"));
        acceptButton.addActionListener(this);
        acceptButton.setActionCommand("accept");
        FreeColPanel.enterPressesWhenFocused(acceptButton);
        // we can't accept an offer we are making
        if (sender == player) {
            acceptButton.setEnabled(false);
        }

        cancelButton = new JButton(Messages.message("negotiationDialog.cancel"));
        cancelButton.addActionListener(this);
        cancelButton.setActionCommand("cancel");
        setCancelComponent(cancelButton);
        FreeColPanel.enterPressesWhenFocused(cancelButton);

        updateSummary();

        List<Goods> tradeGoods = null;
        if (unit.isCarrier()) {
            tradeGoods = unit.getGoodsContainer().getGoods();
        }


        stance = new StanceTradeItemPanel(this, player);
        goldDemand = new GoldTradeItemPanel(this, otherPlayer);
        goldOffer = new GoldTradeItemPanel(this, player);
        goodsDemand = new GoodsTradeItemPanel(this, otherPlayer, tradeGoods);
        goodsOffer = new GoodsTradeItemPanel(this, player, tradeGoods);
        colonyDemand = new ColonyTradeItemPanel(this, otherPlayer);
        colonyOffer = new ColonyTradeItemPanel(this, player);
        /** TODO: UnitTrade
            unitDemand = new UnitTradeItemPanel(this, otherPlayer);
            unitOffer = new UnitTradeItemPanel(this, player);
        */

        int numberOfTradeItems = 5;
        int extraRows = 2; // headline and buttons

        int[] widths = {200, 10, 300, 10, 200};
        int[] heights = new int[2 * (numberOfTradeItems + extraRows) - 1];
        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = 10;
        }
        setLayout(new HIGLayout(widths, heights));

        int demandColumn = 1;
        int summaryColumn = 3;
        int offerColumn = 5;

        int row = 1;
        add(new JLabel(Messages.message("negotiationDialog.demand")),
            higConst.rc(row, demandColumn));
        add(new JLabel(Messages.message("negotiationDialog.offer")),
            higConst.rc(row, offerColumn));
        row += 2;
        add(stance, higConst.rc(row, offerColumn));
        row += 2;
        add(goldDemand, higConst.rc(row, demandColumn));
        add(goldOffer, higConst.rc(row, offerColumn));
        add(summary, higConst.rcwh(row, summaryColumn, 1, 5));
        row += 2;
        add(goodsDemand, higConst.rc(row, demandColumn));
        add(goodsOffer, higConst.rc(row, offerColumn));
        row += 2;
        add(colonyDemand, higConst.rc(row, demandColumn));
        add(colonyOffer, higConst.rc(row, offerColumn));
        row += 2;
        /** TODO: UnitTrade
            add(unitDemand, higConst.rc(row, demandColumn));
            add(unitOffer, higConst.rc(row, offerColumn));
        */
        row += 2;
        add(sendButton, higConst.rc(row, demandColumn, ""));
        add(acceptButton, higConst.rc(row, summaryColumn, ""));
        add(cancelButton, higConst.rc(row, offerColumn, ""));
            
    }


    private void updateSummary() {
        try {
            StyledDocument document = summary.getStyledDocument();
            document.remove(0, document.getLength());
        
            List<String> offers = new ArrayList<String>();
            List<String> demands = new ArrayList<String>();
            Iterator<TradeItem> itemIterator = agreement.iterator();
            while (itemIterator.hasNext()) {
                TradeItem item = itemIterator.next();
                String description = "";
                if (item instanceof StanceTradeItem) {
                    description = Player.getStanceAsString(((StanceTradeItem) item).getStance());
                } else if (item instanceof GoldTradeItem) {
                    String gold = String.valueOf(((GoldTradeItem) item).getGold());
                    description = Messages.message("tradeItem.gold.long",
                                                   new String[][] {{"%amount%", gold}});
                } else if (item instanceof ColonyTradeItem) {
                    description = Messages.message("tradeItem.colony.long",
                                                   new String[][] {
                                                       {"%colony%", ((ColonyTradeItem) item).getColony().getName()}});
                } else if (item instanceof GoodsTradeItem) {
                    description = ((GoodsTradeItem) item).getGoods().getName();
                } else if (item instanceof UnitTradeItem) {
                    description = ((UnitTradeItem) item).getUnit().getName();
                }
                if (item.getSource() == sender) {
                    offers.add(description);
                } else {
                    demands.add(description);
                }
            }

            String offerString;
            if (offers.isEmpty()) {
                offerString = Messages.message("negotiationDialog.nothing");
            } else {
                offerString = "";
                for (int index = 0; index < offers.size() - 1; index++) {
                    offerString += offers.get(index) + ", ";
                }
                offerString += offers.get(offers.size() - 1);
            }

            String demandString = "";
            if (demands.isEmpty()) {
                demandString = Messages.message("negotiationDialog.nothing");
            } else {
                demandString = "";
                for (int index = 0; index < demands.size() - 1; index++) {
                    demandString += demands.get(index) + ", ";
                }
                demandString += demands.get(demands.size() - 1);
            }

            document.insertString(document.getLength(), 
                                  Messages.message("negotiationDialog.summary",
                                                   new String[][] {
                                                       {"%nation%", sender.getNationAsString()},
                                                       {"%offers%", offerString},
                                                       {"%demands%", demandString}}),
                                  document.getStyle("regular"));

        } catch(Exception e) {
            logger.warning("Failed to update summary: " + e.toString());
        }
    }

    private boolean hasPeaceOffer() {
        return (getStance() > Integer.MIN_VALUE);
    }


    /**
     * Adds a <code>ColonyTradeItem</code> to the list of TradeItems.
     *
     * @param source a <code>Player</code> value
     * @param colony a <code>Colony</code> value
     */
    public void addColonyTradeItem(Player source, Colony colony) {
        Player destination;
        if (source == otherPlayer) {
            destination = player;
        } else {
            destination = otherPlayer;
        }
        agreement.add(new ColonyTradeItem(freeColClient.getGame(), source, destination, colony));
    }

    /**
     * Adds a <code>GoldTradeItem</code> to the list of TradeItems.
     *
     * @param source a <code>Player</code> value
     * @param amount an <code>int</code> value
     */
    public void addGoldTradeItem(Player source, int amount) {
        Player destination;
        if (source == otherPlayer) {
            destination = player;
        } else {
            destination = otherPlayer;
        }
        agreement.add(new GoldTradeItem(freeColClient.getGame(), source, destination, amount));
    }

    /**
     * Adds a <code>GoodsTradeItem</code> to the list of TradeItems.
     *
     * @param source a <code>Player</code> value
     * @param goods a <code>Goods</code> value
     * @param settlement a <code>Settlement</code> value
     */
    public void addGoodsTradeItem(Player source, Goods goods) {
        Player destination;
        if (source == otherPlayer) {
            destination = player;
        } else {
            destination = otherPlayer;
        }
        agreement.add(new GoodsTradeItem(freeColClient.getGame(), source, destination, goods, settlement));
    }


    /**
     * Returns the stance being offered, or Integer.MIN_VALUE if none
     * is being offered.
     *
     * @return an <code>int</code> value
     */
    public int getStance() {
        Iterator<TradeItem> itemIterator = agreement.iterator();
        while (itemIterator.hasNext()) {
            TradeItem item = itemIterator.next();
            if (item instanceof StanceTradeItem) {
                return ((StanceTradeItem) item).getStance();
            }
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Sets the <code>stance</code> between the players.
     *
     * @param stance an <code>int</code> value
     */
    public void setStance(int stance) {
        agreement.add(new StanceTradeItem(freeColClient.getGame(), otherPlayer, player, stance));
    }


    public class ColonyTradeItemPanel extends JPanel implements ActionListener {

        private JComboBox colonyBox;
        private JButton addButton;
        private Player player;
        private NegotiationDialog negotiationDialog;

        /**
         * Creates a new <code>ColonyTradeItemPanel</code> instance.
         *
         * @param parent a <code>NegotiationDialog</code> value
         * @param source a <code>Player</code> value
         */
        public ColonyTradeItemPanel(NegotiationDialog parent, Player source) {
            this.player = source;
            this.negotiationDialog = parent;
            addButton = new JButton(Messages.message("negotiationDialog.add"));
            addButton.addActionListener(this);
            addButton.setActionCommand("add");
            colonyBox = new JComboBox();
            updateColonyBox();

            setLayout(new HIGLayout(new int[] {0}, new int[] {0, 0, 0}));
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                         BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            add(new JLabel(Messages.message("tradeItem.colony")),
                higConst.rc(1, 1));
            add(colonyBox, higConst.rc(2, 1));
            add(addButton, higConst.rc(3, 1));
            
        }

        private void updateColonyBox() {

            // Remove all action listeners, so the update has no effect (except
            // updating the list).
            ActionListener[] listeners = colonyBox.getActionListeners();
            for (ActionListener al : listeners) {
                colonyBox.removeActionListener(al);
            }
            colonyBox.removeAllItems();
            List<Colony> colonies = player.getColonies();
            Collections.sort(colonies, freeColClient.getClientOptions().getColonyComparator());
            Iterator<Colony> colonyIterator = colonies.iterator();
            while (colonyIterator.hasNext()) {
                colonyBox.addItem(colonyIterator.next());
            }
            for(ActionListener al : listeners) {
                colonyBox.addActionListener(al);
            }
        }

        /**
         * Analyzes an event and calls the right external methods to take care of
         * the user's request.
         * 
         * @param event The incoming action event
         */
        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            if (command.equals("add")) {
                negotiationDialog.addColonyTradeItem(player, (Colony) colonyBox.getSelectedItem());
                updateSummary();
            }

        }
    }

    public class GoodsTradeItemPanel extends JPanel implements ActionListener {

        private JComboBox goodsBox;
        private JButton addButton;
        private Player player;
        private NegotiationDialog negotiationDialog;

        /**
         * Creates a new <code>GoodsTradeItemPanel</code> instance.
         *
         * @param parent a <code>NegotiationDialog</code> value
         * @param source a <code>Player</code> value
         * @param allGoods a <code>List</code> of <code>Goods</code> values
         */
        public GoodsTradeItemPanel(NegotiationDialog parent, Player source, List<Goods> allGoods) {
            this.player = source;
            this.negotiationDialog = parent;
            addButton = new JButton(Messages.message("negotiationDialog.add"));
            addButton.addActionListener(this);
            addButton.setActionCommand("add");
            goodsBox = new JComboBox();
            JLabel label = new JLabel(Messages.message("tradeItem.goods"));
            if (allGoods == null) {
                label.setEnabled(false);
                addButton.setEnabled(false);
                goodsBox.setEnabled(false);
            } else {
                updateGoodsBox(allGoods);
            }

            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                         BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            setLayout(new HIGLayout(new int[] {0}, new int[] {0, 0, 0}));
            add(label, higConst.rc(1, 1));
            add(goodsBox, higConst.rc(2, 1));
            add(addButton, higConst.rc(3, 1));
            setSize(getPreferredSize());
            
        }

        private void updateGoodsBox(List<Goods> allGoods) {

            // Remove all action listeners, so the update has no effect (except
            // updating the list).
            ActionListener[] listeners = goodsBox.getActionListeners();
            for (ActionListener al : listeners) {
                goodsBox.removeActionListener(al);
            }
            goodsBox.removeAllItems();
            Iterator<Goods> goodsIterator = allGoods.iterator();
            while (goodsIterator.hasNext()) {
                goodsBox.addItem(goodsIterator.next());
            }
            for(ActionListener al : listeners) {
                goodsBox.addActionListener(al);
            }
        }

        /**
         * Analyzes an event and calls the right external methods to take care of
         * the user's request.
         * 
         * @param event The incoming action event
         */
        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            if (command.equals("add")) {
                negotiationDialog.addGoodsTradeItem(player, (Goods) goodsBox.getSelectedItem());
                updateSummary();
            }

        }
    }

    public class StanceTradeItemPanel extends JPanel implements ActionListener {

        public static final int OFFSET = 2;
        private JComboBox stanceBox;
        private JButton addButton;
        private Player player;
        private NegotiationDialog negotiationDialog;

        /**
         * Creates a new <code>StanceTradeItemPanel</code> instance.
         *
         * @param parent a <code>NegotiationDialog</code> value
         * @param source a <code>Player</code> value
         */
        public StanceTradeItemPanel(NegotiationDialog parent, Player source) {
            this.player = source;
            this.negotiationDialog = parent;
            addButton = new JButton(Messages.message("negotiationDialog.add"));
            addButton.addActionListener(this);
            addButton.setActionCommand("add");
            stanceBox = new JComboBox();
            stanceBox.addItem(Messages.message("model.player.war"));
            stanceBox.addItem(Messages.message("model.player.ceaseFire"));
            stanceBox.addItem(Messages.message("model.player.peace"));
            stanceBox.addItem(Messages.message("model.player.alliance"));
            if (parent.hasPeaceOffer()) {
                stanceBox.setSelectedIndex(parent.getStance() + OFFSET);
            }

            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                         BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            setLayout(new HIGLayout(new int[] {0}, new int[] {0, 0, 0}));
            add(new JLabel(Messages.message("tradeItem.stance")),
                higConst.rc(1, 1));
            add(stanceBox, higConst.rc(2, 1));
            add(addButton, higConst.rc(3, 1));
            
        }

        /**
         * Analyzes an event and calls the right external methods to take care of
         * the user's request.
         * 
         * @param event The incoming action event
         */
        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            if (command.equals("add")) {
                negotiationDialog.setStance(stanceBox.getSelectedIndex() - OFFSET);
                updateSummary();
            }

        }
    }

    public class GoldTradeItemPanel extends JPanel implements ActionListener {

        private JSpinner spinner;
        private JButton addButton;
        private Player player;
        private NegotiationDialog negotiationDialog;

        /**
         * Creates a new <code>GoldTradeItemPanel</code> instance.
         *
         * @param parent a <code>NegotiationDialog</code> value
         * @param source a <code>Player</code> value
         */
        public GoldTradeItemPanel(NegotiationDialog parent, Player source) {
            this.player = source;
            this.negotiationDialog = parent;
            addButton = new JButton(Messages.message("negotiationDialog.add"));
            addButton.addActionListener(this);
            addButton.setActionCommand("add");
            int gold = Math.max(0, player.getGold());
            spinner= new JSpinner(new SpinnerNumberModel(0, 0, gold, 1));

            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                         BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            setLayout(new HIGLayout(new int[] {0}, new int[] {0, 0, 0}));
            add(new JLabel(Messages.message("tradeItem.gold")),
                higConst.rc(1, 1));
            add(spinner, higConst.rc(2, 1));
            add(addButton, higConst.rc(3, 1));
        }

        /**
         * Analyzes an event and calls the right external methods to take care of
         * the user's request.
         * 
         * @param event The incoming action event
         */
        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            if (command.equals("add")) {
                int amount = ((Integer) spinner.getValue()).intValue();
                negotiationDialog.addGoldTradeItem(player, amount);
                updateSummary();
            }

        }
    }

    /**
     * Analyzes an event and calls the right external methods to take care of
     * the user's request.
     * 
     * @param event The incoming action event
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        setResponse(command);
    }

}
