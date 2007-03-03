
package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;

import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TradeRoute.Stop;

import cz.autel.dmi.HIGLayout;

/**
 * Allows the user to edit trade routes.
 */
public final class TradeRouteInputDialog extends FreeColDialog implements ActionListener {
    private static final Logger logger = Logger.getLogger(TradeRouteInputDialog.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private static final int OK = 0, CANCEL = 1;

    private final Canvas parent;
    private TradeRoute originalRoute;

    private final JButton ok = new JButton(Messages.message("ok"));
    private final JButton cancel = new JButton(Messages.message("cancel"));

    private final JButton addStopButton = new JButton(Messages.message("traderouteDialog.addStop"));
    private final JButton removeStopButton = new JButton(Messages.message("traderouteDialog.removeStop"));

    private final JPanel tradeRoutePanel = new JPanel();
    private final JPanel buttonPanel = new JPanel();
    private final JPanel goodsPanel = new JPanel(new GridLayout(4, 4, margin, margin));
    private final JPanel cargoPanel = new JPanel();
    
    private final JComboBox destinationSelector = new JComboBox();
    private final JTextField tradeRouteName = new JTextField(Messages.message("traderouteDialog.newRoute"));

    private final DefaultListModel listModel = new DefaultListModel();
    private final JList stopList = new JList(listModel);
    private final JScrollPane tradeRouteView = new JScrollPane(stopList);
    private final JLabel nameLabel = new JLabel(Messages.message("traderouteDialog.nameLabel"));
    private final JLabel destinationLabel = new JLabel(Messages.message("traderouteDialog.destinationLabel"));


    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public TradeRouteInputDialog(final Canvas parent) {
        this.parent = parent;

        tradeRoutePanel.setOpaque(false);

        ok.setActionCommand(String.valueOf(OK));
        cancel.setActionCommand(String.valueOf(CANCEL));
        
        ok.addActionListener(this);
        cancel.addActionListener(this);

        ok.setMnemonic('y');
        cancel.setMnemonic('n');

        FreeColPanel.enterPressesWhenFocused(cancel);
        FreeColPanel.enterPressesWhenFocused(ok);

        setCancelComponent(cancel);

        stopList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateButtons();
            }
        });

        // button for adding new Stop
        addStopButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Stop stop = originalRoute.new Stop((Location) destinationSelector.getSelectedItem());
                    if (stopList.getSelectedIndex() == -1) {
                        listModel.addElement(stop);
                    } else {
                        listModel.add(stopList.getSelectedIndex(), stop);
                    }
                }
            });


        // button for deleting Stop
        removeStopButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    listModel.removeElement(stopList.getSelectedValue());
                }
            });



	stopList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	stopList.setDragEnabled(true);

        ImageLibrary library = (ImageLibrary) parent.getImageProvider();

        for (int goods = 0; goods < Goods.NUMBER_OF_TYPES; goods++) {
            ImageIcon icon = library.getGoodsImageIcon(goods);
            JLabel label = new JLabel(icon);
            //button.setHorizontalAlignment(SwingConstants.LEFT);
            //button.setActionCommand(String.valueOf(goods));
            //button.addActionListener(this);
            goodsPanel.add(label);
        }

        goodsPanel.setOpaque(false);
        goodsPanel.setBorder(BorderFactory.createTitledBorder(Messages.message("goods")));
        cargoPanel.setOpaque(false);
        cargoPanel.setBorder(BorderFactory.createTitledBorder(Messages.message("cargoOnShip")));


        int[] widths = {0};
        int[] heights = {0, margin, 0, margin, 0};
        setLayout(new HIGLayout(widths, heights));

        buttonPanel.add(ok);
        buttonPanel.add(cancel);
        buttonPanel.setOpaque(false);
        add(getDefaultHeader(Messages.message("traderouteDialog.editRoute")),
            higConst.rc(1, 1));
        add(tradeRoutePanel, higConst.rc(3, 1));
        add(buttonPanel, higConst.rc(5, 1));

    }

    public void initialize(TradeRoute tradeRoute) {
        originalRoute = tradeRoute;

        Player player = parent.getClient().getMyPlayer();

        tradeRoutePanel.removeAll();

        // combo box for selecting destination
        destinationSelector.removeAllItems();
        if (player.getEurope() != null) {
            destinationSelector.addItem(player.getEurope());
        }
        for (Settlement settlement : player.getSettlements()) {
            destinationSelector.addItem(settlement);
        }

        // reset stops 
        listModel.removeAllElements();
        for (Stop stop : tradeRoute.getStops()) {
            listModel.addElement(stop);
        }

        updateButtons();

        tradeRouteName.setText(tradeRoute.getName());

        int widths[] = {240, 3 * margin, 0, margin, 0};
        int heights[] = {0, 3 * margin, 0, margin, 0, margin, 80, margin, 0};
        int listColumn = 1;
        int labelColumn = 3;
        int valueColumn = 5;

        tradeRoutePanel.setLayout(new HIGLayout(widths, heights));

        int row = 1;
        tradeRoutePanel.add(tradeRouteView, higConst.rcwh(row, listColumn, 1, heights.length));
        tradeRoutePanel.add(nameLabel, higConst.rc(row, labelColumn));
        tradeRoutePanel.add(tradeRouteName, higConst.rc(row, valueColumn));
        row += 2;

        tradeRoutePanel.add(destinationLabel, higConst.rc(row, labelColumn));
        tradeRoutePanel.add(destinationSelector, higConst.rc(row, valueColumn));
        row += 2;

        tradeRoutePanel.add(goodsPanel, higConst.rcwh(row, labelColumn, 3, 1));
        row += 2;

        tradeRoutePanel.add(cargoPanel, higConst.rcwh(row, labelColumn, 3, 1));
        row += 2;

        tradeRoutePanel.add(addStopButton, higConst.rc(row, labelColumn));
        tradeRoutePanel.add(removeStopButton, higConst.rc(row, valueColumn));
        row += 2;

        


        /*
	JList locationList = new JList(locationListModel);

        JPanel tradeRoutePanel = new JPanel();
        tradeRoutePanel.setLayout(new BorderLayout());
        tradeRoutePanel.add(tradeRouteView, BorderLayout.CENTER);
        tradeRoutePanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        for (int goodsType = 0; goodsType < Goods.NUMBER_OF_TYPES; goodsType++) {
            traderouteDialog.add(new TradeRouteGoodsPanel(colony, goodsType));
        }
        */

        setSize(getPreferredSize());

    }

    public void updateButtons() {
        if (stopList.getSelectedIndex() == -1) {
            removeStopButton.setEnabled(false);
        } else {
            removeStopButton.setEnabled(true);
        }
    }

    
    public void requestFocus() {
        ok.requestFocus();
    }

    
    /**
    * This function analyses an event and calls the right methods to take
    * care of the user's requests.
    * @param event The incoming ActionEvent.
    */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            switch (Integer.valueOf(command).intValue()) {
            case OK:
                parent.remove(this);
                originalRoute.setName(tradeRouteName.getText());
                ArrayList<Stop> stops = new ArrayList<Stop>();
                for (int index = 0; index < listModel.getSize(); index++) {
                    stops.add((Stop) listModel.getElementAt(index));
                }
                originalRoute.setStops(stops);
                setResponse(new Boolean(true));
                break;
            case CANCEL:
                parent.remove(this);
                setResponse(new Boolean(false));
                break;
            default:
                logger.warning("Invalid ActionCommand: invalid number.");
            }
        }
        catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }

    /*
    public class StopPanel extends JPanel {
        
        private Location location;
        private ArrayList<Integer> goods = new ArrayList<Integer>();
        private final JComboBox destinationBox;
        private final JPanel goodsPanel;
        private final

        public void saveSettings() {
            if (export.isSelected() != colony.getExports(goodsType)) {
                parent.getClient().getInGameController().setExports(colony, goodsType, export.isSelected());
                colony.setExports(goodsType, export.isSelected());
            }
            colony.getLowLevel()[goodsType] = ((SpinnerNumberModel) lowLevel.getModel()).getNumber().intValue();
            colony.getHighLevel()[goodsType] = ((SpinnerNumberModel) highLevel.getModel()).getNumber().intValue();
            colony.getExportLevel()[goodsType] = ((SpinnerNumberModel) exportLevel.getModel()).getNumber().intValue();
    
        }
    }
    */
}
