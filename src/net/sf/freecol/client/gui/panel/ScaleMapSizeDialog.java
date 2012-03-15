package net.sf.freecol.client.gui.panel;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Map;



public class ScaleMapSizeDialog extends FreeColDialog<MapSize> {

    public static final String COPYRIGHT = "Copyright (C) 2003-2012 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private static final int COLUMNS = 5;

    private Map oldMap;

    final JTextField inputWidth = new JTextField(Integer.toString(oldMap.getWidth()), COLUMNS);

    final JTextField inputHeight = new JTextField(Integer.toString(oldMap.getHeight()), COLUMNS);


    public ScaleMapSizeDialog(FreeColClient freeColClient, final GUI gui) {
        super(freeColClient, gui);
        oldMap = freeColClient.getGame().getMap();
        /*
         * TODO: Extend this dialog. It should be possible to specify the sizes
         * using percentages.
         * 
         * Add a panel containing information about the scaling (old size, new
         * size etc).
         */

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);

        final ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    int width = Integer.parseInt(inputWidth.getText());
                    int height = Integer.parseInt(inputHeight.getText());
                    if (width <= 0 || height <= 0) {
                        throw new NumberFormatException();
                    }
                    setResponse(new MapSize(width, height));
                } catch (NumberFormatException nfe) {
                    gui.errorMessage("integerAboveZero");
                }
            }
        };
        JButton okButton = new JButton(Messages.message("ok"));
        buttons.add(okButton);

        JButton cancelButton = new JButton(Messages.message("cancel"));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                setResponse(null);
            }
        });
        buttons.add(cancelButton);
        setCancelComponent(cancelButton);

        okButton.addActionListener(al);
        inputWidth.addActionListener(al);
        inputHeight.addActionListener(al);

        JLabel widthLabel = new JLabel(Messages.message("width"));
        widthLabel.setLabelFor(inputWidth);
        JLabel heightLabel = new JLabel(Messages.message("height"));
        heightLabel.setLabelFor(inputHeight);

        JPanel widthPanel = new JPanel(new FlowLayout());
        widthPanel.setOpaque(false);
        widthPanel.add(widthLabel);
        widthPanel.add(inputWidth);
        JPanel heightPanel = new JPanel(new FlowLayout());
        heightPanel.setOpaque(false);
        heightPanel.add(heightLabel);
        heightPanel.add(inputHeight);

        add(widthPanel);
        add(heightPanel);
        add(buttons);

        setSize(getPreferredSize());
    }

    @Override
    public void requestFocus() {
        inputWidth.requestFocus();
    }

}
