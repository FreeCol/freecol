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

package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

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
import net.sf.freecol.client.gui.Canvas;
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

import org.w3c.dom.Element;

import cz.autel.dmi.HIGLayout;

/**
 * The panel that allows negotiations between players.
 */
public final class NegotiationDialog extends FreeColDialog implements ActionListener {



    private static final String SEND = "send", ACCEPT = "accept", CANCEL = "cancel";

    private static Logger logger = Logger.getLogger(NegotiationDialog.class.getName());

    private FreeColClient freeColClient;

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
    private boolean canAccept;

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
     * @param agreement a <code>DiplomaticTrade</code> with the offer
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
        this.canAccept = agreement != null; // a new offer can't be accepted
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
                this.agreement.add(new StanceTradeItem(freeColClient.getGame(), player, otherPlayer, stance));
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

        int foreignGold = 0;
        Element report = getCanvas().getClient().getInGameController().getForeignAffairsReport();
        int number = report.getChildNodes().getLength();
        for (int i = 0; i < number; i++) {
            Element enemyElement = (Element) report.getChildNodes().item(i);
            Player enemy = (Player) getCanvas().getClient().getGame().getFreeColGameObject(enemyElement.getAttribute("nation"));
            if (enemy == otherPlayer) {
                foreignGold = Integer.parseInt(enemyElement.getAttribute("gold"));
                break;
            }
        }

        sendButton = new JButton(Messages.message("negotiationDialog.send"));
        sendButton.addActionListener(this);
        sendButton.setActionCommand(SEND);
        FreeColPanel.enterPressesWhenFocused(sendButton);

        acceptButton = new JButton(Messages.message("negotiationDialog.accept"));
        acceptButton.addActionListener(this);
        acceptButton.setActionCommand(ACCEPT);
        FreeColPanel.enterPressesWhenFocused(acceptButton);
        acceptButton.setEnabled(canAccept);

        cancelButton = new JButton(Messages.message("negotiationDialog.cancel"));
        cancelButton.addActionListener(this);
        cancelButton.setActionCommand(CANCEL);
        setCancelComponent(cancelButton);
        FreeColPanel.enterPressesWhenFocused(cancelButton);

        updateSummary();

        stance = new StanceTradeItemPanel(this, player, otherPlayer);
        goldDemand = new GoldTradeItemPanel(this, otherPlayer, foreignGold);
        goldOffer = new GoldTradeItemPanel(this, player, player.getGold());
        colonyDemand = new ColonyTradeItemPanel(this, otherPlayer);
        colonyOffer = new ColonyTradeItemPanel(this, player);
        /** TODO: UnitTrade
            unitDemand = new UnitTradeItemPanel(this, otherPlayer);
            unitOffer = new UnitTradeItemPanel(this, player);
        */

        int numberOfTradeItems = 4;
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
        if (unit.isCarrier()) {
            goodsDemand = new GoodsTradeItemPanel(this, otherPlayer, settlement.getGoodsContainer().getGoods());
            add(goodsDemand, higConst.rc(row, demandColumn));
            goodsOffer = new GoodsTradeItemPanel(this, player, unit.getGoodsContainer().getGoods());
            add(goodsOffer, higConst.rc(row, offerColumn));
        } else {
            add(colonyDemand, higConst.rc(row, demandColumn));
            add(colonyOffer, higConst.rc(row, offerColumn));
        }
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

            String input = Messages.message("negotiationDialog.summary");
            int start = input.indexOf('%');
            if (start == -1) {
                // no variables present
                insertText(input.substring(0));
                return;
            } else if (start > 0) {
                // output any string before the first occurence of '%'
                insertText(input.substring(0, start));
            }
            int end;

            loop: while ((end = input.indexOf('%', start + 1)) >= 0) {
                String var = input.substring(start, end + 1);
                if (var.equals("%nation%")) {
                    insertText(sender.getNationAsString());
                    start = end + 1;
                    continue loop;
                } else if (var.equals("%offers%")) {
                    insertOffers();
                    start = end + 1;
                    continue loop;
                } else if (var.equals("%demands%")) {
                    insertDemands();
                    start = end + 1;
                    continue loop;
                } else {
                    // found no variable to replace: either a single '%', or
                    // some unnecessary variable
                    insertText(input.substring(start, end));
                    start = end;
                }
            }

            // output any string after the last occurence of '%'
            if (start < input.length()) {
                insertText(input.substring(start));
            }
        } catch(Exception e) {
            logger.warning("Failed to update summary: " + e.toString());
        }
    }

    private void insertText(String text) throws Exception {
        StyledDocument document = summary.getStyledDocument();
        document.insertString(document.getLength(), text,
                              document.getStyle("regular"));
    }

    private void insertOffers() {
        insertTradeItemDescriptions(sender);
    }

    private void insertDemands() {
        insertTradeItemDescriptions(recipient);
    }

    private void insertTradeItemDescriptions(Player itemSource) {
        StyledDocument document = summary.getStyledDocument();
        List<TradeItem> items = agreement.getTradeItems();
        boolean foundItem = false;
        for (int index = 0; index < items.size(); index++) {
            TradeItem item = items.get(index);
            if (item.getSource() == itemSource) {
                foundItem = true;
                String description = "";
                if (item instanceof StanceTradeItem) {
                    description = Player.getStanceAsString(((StanceTradeItem) item).getStance());
                } else if (item instanceof GoldTradeItem) {
                    String gold = String.valueOf(((GoldTradeItem) item).getGold());
                    description = Messages.message("tradeItem.gold.long", "%amount%", gold);
                } else if (item instanceof ColonyTradeItem) {
                    description = Messages.message("tradeItem.colony.long", 
                            "%colony%", ((ColonyTradeItem) item).getColony().getName());
                } else if (item instanceof GoodsTradeItem) {
                    description = String.valueOf(((GoodsTradeItem) item).getGoods().getAmount()) + " " +
                        ((GoodsTradeItem) item).getGoods().getName();
                } else if (item instanceof UnitTradeItem) {
                    description = ((UnitTradeItem) item).getUnit().getName();
                }
                try {
                    JButton button = new JButton(description);
                    button.setMargin(new Insets(0,0,0,0));
                    button.setOpaque(false);
                    button.setForeground(LINK_COLOR);
                    button.setAlignmentY(0.8f);
                    button.setBorder(BorderFactory.createEmptyBorder());
                    button.addActionListener(this);
                    button.setActionCommand(String.valueOf(index));
                    StyleConstants.setComponent(document.getStyle("button"), button);
                    document.insertString(document.getLength(), " ", document.getStyle("button"));
                    if (index < items.size() - 1) {
                        document.insertString(document.getLength(), ", ", document.getStyle("regular"));
                    } else {
                        return;
                    }
                } catch(Exception e) {
                    logger.warning(e.toString());
                }
            }
        }

        if (!foundItem) {
            try {
                document.insertString(document.getLength(), Messages.message("negotiationDialog.nothing"),
                                      document.getStyle("regular"));
            } catch(Exception e) {
                logger.warning(e.toString());
            }
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
     * Sets the <code>stance</code> between the players.
     *
     * @param stance an <code>int</code> value
     */
    public void setStance(int stance) {
        agreement.add(new StanceTradeItem(freeColClient.getGame(), otherPlayer, player, stance));
    }


    /**
     * Returns the stance being offered, or Integer.MIN_VALUE if none
     * is being offered.
     *
     * @return an <code>int</code> value
     */
    public int getStance() {
        return agreement.getStance();
    }


    /**
     * Analyzes an event and calls the right external methods to take care of
     * the user's request.
     * 
     * @param event The incoming action event
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (command.equals(CANCEL)) {
            setResponse(null);
        } else if (command.equals(ACCEPT)) {
            agreement.setAccept(true);
            setResponse(agreement);
        } else if (command.equals(SEND)) {
            setResponse(agreement);
        } else {
            int index = Integer.parseInt(command);
            agreement.remove(index);
            initialize();
        }
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

            if (!player.isEuropean()) {
                return;
            }

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

        class StanceItem {
            private int value;
            StanceItem(int value) {
                this.value = value;
            }
            
            public String toString() {
                return Player.getStanceAsString(value);
            }
            
            int getValue() {
                return value;
            }
            
            public boolean equals(Object other) {
                if (other == null || !(other instanceof StanceItem)) {
                    return false;
                }
                return value == ((StanceItem) other).value;
            }
        }
        
        private JComboBox stanceBox;
        private JButton addButton;
        private NegotiationDialog negotiationDialog;

        /**
         * Creates a new <code>StanceTradeItemPanel</code> instance.
         *
         * @param parent a <code>NegotiationDialog</code> value
         * @param source a <code>Player</code> value
         */
        public StanceTradeItemPanel(NegotiationDialog parent, Player source, Player target) {
            this.negotiationDialog = parent;
            addButton = new JButton(Messages.message("negotiationDialog.add"));
            addButton.addActionListener(this);
            addButton.setActionCommand("add");
            stanceBox = new JComboBox();
            
            int stance = source.getStance(target);
            if (stance != Player.WAR) stanceBox.addItem(new StanceItem(Player.WAR));
            if (stance == Player.WAR) stanceBox.addItem(new StanceItem(Player.CEASE_FIRE));
            if (stance != Player.PEACE) stanceBox.addItem(new StanceItem(Player.PEACE));
            if (stance != Player.ALLIANCE) stanceBox.addItem(new StanceItem(Player.ALLIANCE));
            if (parent.hasPeaceOffer()) {
                stanceBox.setSelectedItem(new StanceItem(parent.getStance()));
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
                StanceItem stance = (StanceItem) stanceBox.getSelectedItem();
                negotiationDialog.setStance(stance.getValue());
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
        public GoldTradeItemPanel(NegotiationDialog parent, Player source, int gold) {
            this.player = source;
            this.negotiationDialog = parent;
            addButton = new JButton(Messages.message("negotiationDialog.add"));
            addButton.addActionListener(this);
            addButton.setActionCommand("add");
            spinner = new JSpinner(new SpinnerNumberModel(0, 0, gold, 1));

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

}
