
package net.sf.freecol.client.gui.panel;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;

/**
 * This panel is used to show information about a tile.
 */
public final class TilePanel extends FreeColDialog implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(TilePanel.class.getName());

    private static final int OK = 0;
    private static final int COLOPEDIA = 1;
    private final Canvas canvas;
    
    private final JPanel goodsPanel;
    private final JLabel tileNameLabel;
    private final JLabel ownerLabel;
    private final JButton okButton;
    private final JButton colopediaButton;

    private TileType tileType;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent panel.
     */
    public TilePanel(Canvas parent) {
        canvas = parent;

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        
        tileNameLabel = new JLabel("", JLabel.CENTER);
        tileNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(tileNameLabel);

        ownerLabel = new JLabel("", JLabel.CENTER);
        ownerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(ownerLabel);        
        
        goodsPanel = new JPanel();
        goodsPanel.setLayout(new FlowLayout());

        goodsPanel.setSize(goodsPanel.getPreferredSize());
        add(goodsPanel);

        colopediaButton = new JButton(Messages.message("menuBar.colopedia"));
        colopediaButton.setActionCommand(String.valueOf(COLOPEDIA));
        colopediaButton.addActionListener(this);
        enterPressesWhenFocused(colopediaButton);

        okButton = new JButton(Messages.message("ok"));
        okButton.setActionCommand(String.valueOf(OK));
        okButton.addActionListener(this);
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        enterPressesWhenFocused(okButton);

        // Use ESCAPE for closing the panel:
        InputMap inputMap = new ComponentInputMap(okButton);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "pressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true), "released");
        SwingUtilities.replaceUIInputMap(okButton, JComponent.WHEN_IN_FOCUSED_WINDOW, inputMap);        

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(colopediaButton);
        buttonPanel.add(okButton);
        add(buttonPanel);

    }


    public void requestFocus() {
        okButton.requestFocus();
    }


    /**
     * Initializes the information that is being displayed on this panel.
     * The information displayed will be based on the given tile.
     *
     * @param tile The Tile whose information should be displayed.
     */
    public void initialize(Tile tile) {
        this.tileType = tile.getType();
        tileNameLabel.setText(tile.getName() + " (" + tile.getX() + ", " +
                              tile.getY() + ")");
        if (tile.getNationOwner() == Player.NO_NATION) {
            ownerLabel.setText("");
        } else {
            String ownerName = Player.getNationAsString(tile.getNationOwner());
            if (ownerName.equals("INVALID")) {
                ownerLabel.setText("");
            } else {
                ownerLabel.setText(ownerName);
            }
        }
        
        goodsPanel.removeAll();
        List<GoodsType> production = tileType.getPotentialTypeList();
        for (GoodsType goodsType : production) {
            JLabel label = new JLabel(canvas.getImageProvider().getGoodsImageIcon(goodsType.getIndex()));
            label.setText(String.valueOf(tileType.getPotential(goodsType)));
            goodsPanel.add(label);
        }
        setSize(getPreferredSize());

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
                setResponse(new Boolean(true));
                break;
            case COLOPEDIA:
                int type = tileType.getIndex();
                setResponse(new Boolean(true));
                canvas.showColopediaPanel(ColopediaPanel.COLOPEDIA_TERRAIN, type);
                break;
            default:
                logger.warning("Invalid Actioncommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
}
