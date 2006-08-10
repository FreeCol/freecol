package net.sf.freecol.client.gui.panel;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Unit;

/**
 * Panel for chosing the goods to capture.
 *
 * @see Unit#attack(Unit, int, int)
 */
public final class CaptureGoodsDialog extends FreeColDialog implements ActionListener {
    private static final Logger logger = Logger.getLogger(CaptureGoodsDialog.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private final Canvas parent;
    private JButton allButton;
    private JButton noneButton;
    private JButton acceptButton;
    private JList goodsList;
//    private JLabel spaceLeft;

    private int maxCargo;

    public CaptureGoodsDialog(Canvas parent) {
        this.parent = parent;

        setBorder(null);
        setOpaque(false);

        allButton = new JButton(Messages.message("all"));
        noneButton = new JButton(Messages.message("none"));
        acceptButton = new JButton(Messages.message("accept"));
//        spaceLeft = new JLabel("");

        goodsList = new JList();
        goodsList.setCellRenderer(new CheckBoxRenderer());

        goodsList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                int selectedIndex = goodsList.locationToIndex(me.getPoint());
                if (selectedIndex < 0)
                    return;
                GoodsItem item = (GoodsItem) goodsList.getModel().getElementAt(selectedIndex);
                if (item.isEnabled())
                    item.setSelected(!item.isSelected());
                updateComponents();
            }
        });

        JScrollPane goodsListScroll = new JScrollPane(goodsList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        goodsListScroll.setSize(140, 150);
        goodsListScroll.setLocation(10, 10);

        allButton.setLocation(10, 170);
        noneButton.setLocation(10 + 60 + 15, 170);
        acceptButton.setLocation(35, 10 + 140 + 10 + 20 + 20);

//        spaceLeft.setSize(140,25);
//        spaceLeft.setLocation(40, 170+20);

        allButton.setSize(65, 20);
        noneButton.setSize(64, 20);
        acceptButton.setSize(80, 20);

        add(goodsListScroll);
        add(allButton);
        add(noneButton);
        add(acceptButton);
//        add(spaceLeft);
        this.setLayout(null);
//        this.setSize(200,300);

        allButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < goodsList.getModel().getSize() && i < maxCargo; i++) {
                    GoodsItem gi = (GoodsItem) goodsList.getModel().getElementAt(i);
                    gi.setSelected(true);
                    updateComponents();
                }
            }
        });
        noneButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < goodsList.getModel().getSize(); i++) {
                    GoodsItem gi = (GoodsItem) goodsList.getModel().getElementAt(i);
                    gi.setSelected(false);
                    updateComponents();
                }
            }
        });
        noneButton.setMnemonic('n');
        allButton.setMnemonic('l');
        acceptButton.setMnemonic('a');
    }

    public void requestFocus() {
        acceptButton.requestFocus();
    }

    private void updateComponents() {
        int selectedCount = 0;
        for (int i = 0; i < goodsList.getModel().getSize(); i++) {
            GoodsItem gi = (GoodsItem) goodsList.getModel().getElementAt(i);
            if (gi.isSelected())
                selectedCount++;
        }

        if (selectedCount >= maxCargo) {
            allButton.setEnabled(false);
            for (int i = 0; i < goodsList.getModel().getSize(); i++) {
                GoodsItem gi = (GoodsItem) goodsList.getModel().getElementAt(i);
                if (!gi.isSelected())
                    gi.setEnabled(false);
            }
        }
        else {
            allButton.setEnabled(true);
            for (int i = 0; i < goodsList.getModel().getSize(); i++) {
                GoodsItem gi = (GoodsItem) goodsList.getModel().getElementAt(i);
                if (!gi.isSelected())
                    gi.setEnabled(true);
            }
        }

        goodsList.repaint();
    }

    /**
     * Inits the dialog.
     *
     * @param capturedUnit
     * @param capturingUnit
     */
    public void initialize(Unit capturedUnit, Unit capturingUnit) {
        maxCargo = capturingUnit.getInitialSpaceLeft();
        GoodsItem[] goods = new GoodsItem[capturedUnit.getGoodsCount()];
        if (goods.length > 0) {
            Iterator iter = capturedUnit.getGoodsIterator();
            for (int i = 0; iter.hasNext(); i++) {
                Goods g = (Goods) iter.next();
                goods[i] = new GoodsItem(g);
            }
        }
        goodsList.setListData(goods);

//            spaceLeft.setText(capturingUnit.getSpaceLeft() + " free");
    }

    /**
    * This function analyses an event and calls the right methods to take
    * care of the user's requests. The response is an ArrayList of Goods.
    * @param e The incoming ActionEvent.
    */
    public void actionPerformed(ActionEvent e) {
        ArrayList list = new ArrayList(4);

        for (int i = 0; i < goodsList.getModel().getSize(); i++) {
            GoodsItem gi = (GoodsItem) goodsList.getModel().getElementAt(i);
            if(gi.isSelected())
                list.add(gi.getGoods());
        }
        setResponse(list);
    }

    private class CheckBoxRenderer extends JCheckBox implements ListCellRenderer {

        public CheckBoxRenderer() {
            setBackground(UIManager.getColor("List.textBackground"));
            setForeground(UIManager.getColor("List.textForeground"));
        }

        public Component getListCellRendererComponent(JList listBox, Object obj, int currentindex,
                                                      boolean isChecked, boolean hasFocus) {
            setSelected(((GoodsItem) obj).isSelected());
            setText(((GoodsItem) obj).toString());
            setEnabled(((GoodsItem) obj).isEnabled());
            return this;
        }
    }

    class GoodsItem extends JCheckBox {
        private boolean isChecked;
        private Goods good;

        public GoodsItem(Goods good) {
            isChecked = false;
            this.good = good;
        }

        public Goods getGoods() {
            return good;
        }

        public String toString() {
            return good.getAmount() + " " + Goods.getName(good.getType());
        }
    }
}
