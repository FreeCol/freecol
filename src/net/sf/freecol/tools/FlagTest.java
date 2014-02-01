/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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

package net.sf.freecol.tools;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JScrollPane;
import javax.swing.plaf.ColorUIResource;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.gui.panel.Flag;

public class FlagTest extends JFrame implements ActionListener, ItemListener {


    private static Flag SPANISH_FLAG, ENGLISH_FLAG, FRENCH_FLAG,
        PORTUGUESE_FLAG, DUTCH_FLAG, SWEDISH_FLAG, DANISH_FLAG,
        RUSSIAN_FLAG;

    static {
        // based on the flag of Venezuela (Colombia and Ecuador are
        // similar)
        SPANISH_FLAG = Flag.tricolore(Flag.Alignment.HORIZONTAL,
                                      new Color(0xcf, 0x14, 0x2b),
                                      new Color(0, 0x24, 0x7d),
                                      new Color(255, 204, 0));

        // based on the flag of Brazil, particularly the Provisional
        // Flag of Republic of the United States of Brazil (November
        // 15–19, 1889)
        PORTUGUESE_FLAG = Flag.starsAndStripes(Flag.Alignment.HORIZONTAL,
                                               new Color(62, 64, 149),
                                               new Color(0, 168, 89),
                                               new Color(255, 204, 41));

        // based on the current flag of the United States and its
        // various predecessors
        ENGLISH_FLAG = Flag.starsAndStripes(Flag.Alignment.HORIZONTAL,
                                            new Color(.234f, .233f, .430f),
                                            new Color(.698f, .132f, .203f),
                                            Color.WHITE);

        // based on the flag of Louisiana in 1861 and other similar
        // French colonial flags
        FRENCH_FLAG = Flag.tricolore(Flag.Alignment.VERTICAL,
                                     new Color(0, 0x23, 0x95),
                                     Color.WHITE,
                                     new Color(0xed, 0x29, 0x39));
        FRENCH_FLAG.setUnionPosition(Flag.UnionPosition.LEFT);

        // Dutch flag
        DUTCH_FLAG = Flag.tricolore(Flag.Alignment.HORIZONTAL,
                                    new Color(0xae, 0x1c, 0x28),
                                    Color.WHITE,
                                    new Color(0x21, 0x46, 0x6b));
        DUTCH_FLAG.setUnionPosition(Flag.UnionPosition.TOP);

        // Swedish flag
        SWEDISH_FLAG = Flag.quartered(new Color(0xFE, 0xCB, 0),
                                      new Color(0, 0x52, 0x93));

        // Danish flag
        DANISH_FLAG = Flag.quartered(Color.WHITE,
                                     new Color(0xC6, 0x0C, 0x30));

        // Russian flag
        RUSSIAN_FLAG = Flag.tricolore(Flag.Alignment.HORIZONTAL,
                                      Color.WHITE,
                                      new Color(0, 0x39, 0xa6),
                                      new Color(0xd5, 0x2b, 0x1e));

    }


    private final Flag[] FLAGS = new Flag[] {
        ENGLISH_FLAG, SPANISH_FLAG, FRENCH_FLAG, DUTCH_FLAG,
        PORTUGUESE_FLAG, SWEDISH_FLAG, DANISH_FLAG, RUSSIAN_FLAG, null
    };

    private final String[] FLAG_NAMES = new String[] {
        "England", "Spain", "France", "Netherlands",
        "Portugal", "Sweden", "Denmark", "Russia", "Custom"
    };

    private Flag flag;

    @SuppressWarnings("unchecked") // FIXME in Java7
    private JComboBox flags = new JComboBox(FLAG_NAMES);
    @SuppressWarnings("unchecked") // FIXME in Java7
    private JComboBox alignment = new JComboBox(Flag.Alignment.values());
    @SuppressWarnings("unchecked") // FIXME in Java7
    private JComboBox background = new JComboBox(Flag.Background.values());
    @SuppressWarnings("unchecked") // FIXME in Java7
    private JComboBox union = new JComboBox(Flag.UnionPosition.values());
    @SuppressWarnings("unchecked") // FIXME in Java7
    private JComboBox stars = new JComboBox(getNumbers(50));
    @SuppressWarnings("unchecked") // FIXME in Java7
    private JComboBox stripes = new JComboBox(getNumbers(13));

    private JButton unionColor = new JButton(" ");
    private JButton starColor = new JButton(" ");
    private JButton[] stripeColors = new JButton[] {
        new JButton(" "), new JButton(" "), new JButton(" ")
    };

    private Component[] customComponents = new Component[] {
        background, alignment, union, stripes,
        unionColor, starColor, stripeColors[0],
        stripeColors[1], stripeColors[2]
    };

    private JComboBox[] typeBoxes = new JComboBox[] {
        alignment, union
    };

    final JLabel label = new JLabel();

    @SuppressWarnings("unchecked")
    public FlagTest() {

        super("FlagTest");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new MigLayout("wrap 2", "[][fill]"));
        flags.addItemListener(this);
        add(new JLabel("predefined flags"));
        add(flags);

        background.addItemListener(this);
        add(new JLabel("background"));
        add(background);

        alignment.addItemListener(this);
        add(new JLabel("alignment"));
        add(alignment);

        union.addItemListener(this);
        add(new JLabel("union"));
        add(union);

        stars.setSelectedIndex(12);
        stars.addItemListener(this);
        add(new JLabel("number of stars"));
        add(stars);

        stripes.setSelectedIndex(12);
        stripes.addItemListener(this);
        add(new JLabel("number of stripes"));
        add(stripes);

        unionColor.addActionListener(this);
        add(new JLabel("union color"));
        add(unionColor);

        starColor.addActionListener(this);
        add(new JLabel("star color"));
        add(starColor);

        for (JButton button : stripeColors) {
            button.addActionListener(this);
            add(new JLabel("stripe color"));
            add(button);
        }

        add(label, "width 200, height 100");

        itemStateChanged(null);
    }

    public void itemStateChanged(ItemEvent e) {
        Flag.Background newType = (Flag.Background) background.getSelectedItem();
        Flag.Alignment newAlignment = (Flag.Alignment) alignment.getSelectedItem();
        Flag.UnionPosition newPosition = (Flag.UnionPosition) union.getSelectedItem();
        Flag newFlag = FLAGS[flags.getSelectedIndex()];
        if (e == null || e.getSource() == flags) {
            if (newFlag == null) {
                // custom
                enable(customComponents, true);
            } else {
                enable(customComponents, false);
                flag = newFlag;
                unionColor.setBackground(flag.getUnionColor());
                starColor.setBackground(flag.getStarColor());
                List<Color> colors = flag.getBackgroundColors();
                for (int index = 0; index < stripeColors.length; index++) {
                    Color color = (index < colors.size())
                        ? colors.get(index) : null;
                    stripeColors[index].setBackground(color);
                }
            }
        } else {
            if (newFlag == null) {
                if (e.getSource() == background) {
                    if (newType == Flag.Background.CROSS) {
                        enable(typeBoxes, false);
                        newAlignment = null;
                        newPosition = Flag.UnionPosition.INSET;
                    } else {
                        enable(typeBoxes, true);
                    }
                }
                flag = new Flag(newAlignment, newType, newPosition);
                flag.setStripes(stripes.getSelectedIndex() + 1);
                setColors();
            }
            flag.setStars(stars.getSelectedIndex() + 1);
        }

        label.setIcon(new ImageIcon(flag.getImage()));
    }

    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton) e.getSource();
        Color color = JColorChooser
            .showDialog(FlagTest.this,
                        label.getText(),
                        button.getBackground());
        button.setBackground(color);
        setColors();
        label.setIcon(new ImageIcon(flag.getImage()));
    }

    private void setColors() {
        flag.setUnionColor(unionColor.getBackground());
        flag.setStarColor(starColor.getBackground());
        List<Color> colors = new ArrayList<Color>();
        for (JButton button : stripeColors) {
            Color color = button.getBackground();
            if (!(color == null || color instanceof ColorUIResource)) {
                colors.add(color);
            }
            flag.setBackgroundColors(colors);
        }
    }

    public String[] getNumbers(int count) {
        String[] result = new String[count];
        for (int index = 0; index < count; index++) {
            result[index] = Integer.toString(index + 1);
        }
        return result;
    }

    private void enable(Component[] components, boolean value) {
        for (Component component : components) {
            component.setEnabled(value);
        }
    }


    public static void main(String[] args) {

        FlagTest frame = new FlagTest();

        // display the window
        frame.pack();
        frame.setVisible(true);


    }

}
