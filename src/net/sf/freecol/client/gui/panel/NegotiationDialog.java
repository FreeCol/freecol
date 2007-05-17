package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTradeItem;
import net.sf.freecol.common.model.GoldTradeItem;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsTradeItem;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StanceTradeItem;
import net.sf.freecol.common.model.TradeItem;
import net.sf.freecol.common.model.Unit;
import cz.autel.dmi.HIGLayout;

/**
 * The panel that allows a user to purchase ships and artillery in Europe.
 */
public final class NegotiationDialog extends FreeColDialog implements ActionListener {
    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private static final int GOLD = 1, STANCE = 2;

    private static Logger logger = Logger.getLogger(NegotiationDialog.class.getName());

    private static FreeColClient freeColClient;

    private List<TradeItem> tradeItems;

    private Player otherPlayer;

    /**
     * The constructor to use.
     */
    public NegotiationDialog(Canvas parent, Player otherPlayer) {
        this(parent, otherPlayer, new ArrayList<TradeItem>());
    }

    public NegotiationDialog(Canvas parent, Player otherPlayer, List<TradeItem> items) {
        this.tradeItems = items;
        this.freeColClient = parent.getClient();
        this.otherPlayer = otherPlayer;

        JButton sendButton = new JButton(Messages.message("negotiationDialog.send"));
        sendButton.addActionListener(this);
        sendButton.setActionCommand("send");

        JButton acceptButton = new JButton(Messages.message("negotiationDialog.accept"));
        acceptButton.addActionListener(this);
        acceptButton.setActionCommand("accept");

        JButton cancelButton = new JButton(Messages.message("negotiationDialog.cancel"));
        cancelButton.addActionListener(this);
        cancelButton.setActionCommand("cancel");

        int[] widths = {0, 10, 0, 10, 0};
        int[] heights = {0, 0, 0, 0, 0, 10, 0};
        setLayout(new HIGLayout(widths, heights));

        int numberOfTradeItems = 5;
        int demandColumn = 1;
        int summaryColumn = 3;
        int offerColumn = 5;

        int row = 1;
        add(new StanceTradeItemPanel(this, freeColClient.getMyPlayer()),
            higConst.rc(row, offerColumn));
        row++;
        add(new GoldTradeItemPanel(this, otherPlayer),
            higConst.rc(row, demandColumn));
        add(new GoldTradeItemPanel(this, freeColClient.getMyPlayer()),
            higConst.rc(row, offerColumn));
        row++;
        /* TODO: GoodsTrade
        add(new GoodsTradeItemPanel(this, otherPlayer),
            higConst.rc(row, demandColumn));
        add(new GoodsTradeItemPanel(this, freeColClient.getMyPlayer()),
            higConst.rc(row, offerColumn));
        */
        row++;
        add(new ColonyTradeItemPanel(this, otherPlayer),
            higConst.rc(row, demandColumn));
        add(new ColonyTradeItemPanel(this, freeColClient.getMyPlayer()),
            higConst.rc(row, offerColumn));
        row++;
        /** TODO: UnitTrade
        add(new UnitTradeItemPanel(this, otherPlayer),
            higConst.rc(row, demandColumn));
        add(new UnitTradeItemPanel(this, freeColClient.getMyPlayer()),
            higConst.rc(row, offerColumn));
        */
        row += 2;
        add(sendButton, higConst.rc(row, demandColumn, ""));
        add(acceptButton, higConst.rc(row, summaryColumn, ""));
        add(cancelButton, higConst.rc(row, offerColumn, ""));
            
    }


    public void addTradeItem(Player source, Colony colony) {
        Player destination;
        if (source == otherPlayer) {
            destination = freeColClient.getMyPlayer();
        } else {
            destination = otherPlayer;
        }
        tradeItems.add(new ColonyTradeItem(freeColClient.getGame(), source, destination, colony));
    }

    public void addTradeItem(Player source, int amount) {
        Player destination;
        if (source == otherPlayer) {
            destination = freeColClient.getMyPlayer();
        } else {
            destination = otherPlayer;
        }
        tradeItems.add(new GoldTradeItem(freeColClient.getGame(), source, destination, amount));
    }

    public void setStance(int stance) {
        Iterator<TradeItem> tradeItemIterator = tradeItems.iterator();
        while (tradeItemIterator.hasNext()) {
            if (tradeItemIterator.next() instanceof StanceTradeItem) {
                tradeItemIterator.remove();
            }
        }
        tradeItems.add(new StanceTradeItem(freeColClient.getGame(), freeColClient.getMyPlayer(), otherPlayer, stance));
        tradeItems.add(new StanceTradeItem(freeColClient.getGame(), otherPlayer, freeColClient.getMyPlayer(), stance));
    }

    public void addTradeItem(Player source, Goods goods, Settlement settlement) {
        Player destination;
        if (source == otherPlayer) {
            destination = freeColClient.getMyPlayer();
        } else {
            destination = otherPlayer;
        }
        tradeItems.add(new GoodsTradeItem(freeColClient.getGame(), source, destination, goods, settlement));
    }

    public class ColonyTradeItemPanel extends JPanel implements ActionListener {

        private JComboBox colonyBox;
        private JButton addButton;
        private Player player;
        private NegotiationDialog negotiationDialog;

        public ColonyTradeItemPanel(NegotiationDialog parent, Player source) {
            this.player = source;
            this.negotiationDialog = parent;
            addButton = new JButton(Messages.message("negotiationDialog.add"));
            addButton.addActionListener(this);
            addButton.setActionCommand("add");
            colonyBox = new JComboBox();
            updateColonyBox();

            setLayout(new HIGLayout(new int[] {0}, new int[] {0, 0, 0}));
            add(new JLabel(Messages.message("negotiationDialog.colony")),
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
                negotiationDialog.addTradeItem(player, (Colony) colonyBox.getSelectedItem());
            }

        }
    }

    public class StanceTradeItemPanel extends JPanel implements ActionListener {

        private JComboBox stanceBox;
        private JButton addButton;
        private Player player;
        private NegotiationDialog negotiationDialog;

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

            setLayout(new HIGLayout(new int[] {0}, new int[] {0, 0, 0}));
            add(new JLabel(Messages.message("negotiationDialog.colony")),
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
                negotiationDialog.setStance(stanceBox.getSelectedIndex() - 2);
            }

        }
    }

    public class GoldTradeItemPanel extends JPanel implements ActionListener {

        private JSpinner spinner;
        private JButton addButton;
        private Player player;
        private NegotiationDialog negotiationDialog;

        public GoldTradeItemPanel(NegotiationDialog parent, Player source) {
            this.player = source;
            this.negotiationDialog = parent;
            addButton = new JButton(Messages.message("negotiationDialog.add"));
            addButton.addActionListener(this);
            addButton.setActionCommand("add");
            spinner= new JSpinner(new SpinnerNumberModel(0, 0, player.getGold(), 1));
            setLayout(new HIGLayout(new int[] {0}, new int[] {0, 0, 0}));
            add(new JLabel(Messages.message("negotiationDialog.colony")),
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
                negotiationDialog.addTradeItem(player, amount);
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
    }

}
