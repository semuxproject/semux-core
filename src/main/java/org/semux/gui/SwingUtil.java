/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.text.DefaultEditorKit;

import org.semux.core.Transaction;
import org.semux.core.Unit;
import org.semux.core.state.Delegate;
import org.semux.core.state.DelegateState;
import org.semux.crypto.Hex;
import org.semux.gui.exception.QRCodeException;
import org.semux.message.GUIMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import io.netty.util.internal.StringUtil;

public class SwingUtil {

    private static final Logger logger = LoggerFactory.getLogger(SwingUtil.class);

    private SwingUtil() {
    }

    /**
     * Put a JFrame in the center of screen.
     * 
     * @param frame
     * @param width
     * @param height
     */
    public static void centerizeFrame(JFrame frame, int width, int height) {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int x = ((int) d.getWidth() - width) / 2;
        int y = ((int) d.getHeight() - height) / 2;
        frame.setLocation(x, y);
        frame.setBounds(x, y, width, height);
    }

    /**
     * Load an ImageIcon from resource, and rescale it.
     * 
     * @param imageName
     *            image name
     * @return an image icon if exists, otherwise null
     */
    public static ImageIcon loadImage(String imageName, int width, int height) {
        String imgLocation = imageName + ".png";
        URL imageURL = SwingUtil.class.getResource(imgLocation);

        if (imageURL == null) {
            logger.warn("Resource not found: {}", imgLocation);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            return new ImageIcon(image);
        } else {
            ImageIcon icon = new ImageIcon(imageURL);
            Image img = icon.getImage();
            Image img2 = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(img2);
        }
    }

    /**
     * Generate an empty image icon.
     * 
     * @param width
     * @param height
     * @return
     */
    public static ImageIcon emptyImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = image.createGraphics();
        graphics.setPaint(new Color(255, 255, 255));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

        return new ImageIcon(image);
    }

    /**
     * Set the preferred width of table columns.
     * 
     * @param table
     * @param total
     * @param widths
     */
    public static void setColumnWidths(JTable table, int total, double... widths) {
        TableColumnModel model = table.getColumnModel();
        for (int i = 0; i < widths.length; i++) {
            model.getColumn(i).setPreferredWidth((int) (total * widths[i]));
        }
    }

    /**
     * Set the alignments of table columns.
     * 
     * @param table
     * @param right
     */
    public static void setColumnAlignments(JTable table, boolean... right) {
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);

        TableColumnModel model = table.getColumnModel();
        for (int i = 0; i < right.length; i++) {
            if (right[i]) {
                model.getColumn(i).setCellRenderer(rightRenderer);
            }
        }
    }

    /**
     * Generate an QR image for the given text.
     * 
     * @param text
     * @param size
     * @return
     */
    public static BufferedImage generateQR(String text, int size) throws QRCodeException {
        try {
            Map<EncodeHintType, Object> hintMap = new EnumMap<>(EncodeHintType.class);
            hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hintMap.put(EncodeHintType.MARGIN, 2);
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hintMap);
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            image.createGraphics();

            Graphics2D graphics = (Graphics2D) image.getGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(Color.BLACK);

            for (int i = 0; i < width; i++) {
                for (int j = 0; j < width; j++) {
                    if (matrix.get(i, j)) {
                        graphics.fillRect(i, j, 1, 1);
                    }
                }
            }

            return image;
        } catch (WriterException e) {
            throw new QRCodeException(e);
        }
    }

    /**
     * Adds a copy-paste-cut popup to the given component.
     * 
     * @param comp
     */
    public static void addCopyPastePopup(JComponent comp) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem item = new JMenuItem(new DefaultEditorKit.CutAction());
        item.setText(GUIMessages.get("Cut"));
        popup.add(item);
        item = new JMenuItem(new DefaultEditorKit.CopyAction());
        item.setText(GUIMessages.get("Copy"));
        popup.add(item);
        item = new JMenuItem(new DefaultEditorKit.PasteAction());
        item.setText(GUIMessages.get("Paste"));
        popup.add(item);
        comp.setComponentPopupMenu(popup);
    }

    /**
     * Generates a text field with copy-paste-cut popup menu.
     * 
     * @return
     */
    public static JTextField textFieldWithCopyPastePopup() {
        JTextField textfield = new JTextField();
        addCopyPastePopup(textfield);
        return textfield;
    }

    /**
     * Generates a selectable text area.
     * 
     * @param txt
     * @return
     */
    public static JTextArea textAreaWithCopyPastePopup(String txt) {
        JTextArea c = new JTextArea(txt);
        c.setBackground(null);
        c.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        c.setEditable(false);

        addCopyPastePopup(c);
        return c;
    }

    /**
     * Convenience factory method for creating buttons
     * 
     * @param text
     * @param listener
     * @param action
     * @return
     */
    public static JButton createDefaultButton(String text, ActionListener listener, Action action) {
        JButton button = new JButton(text);
        button.setActionCommand(action.name());
        button.addActionListener(listener);
        return button;
    }

    /**
     * Parses a number from a localized string.
     * 
     * @param str
     * @return
     * @throws ParseException
     */
    public static Number parseNumber(String str) throws ParseException {
        NumberFormat format = NumberFormat.getInstance();
        ParsePosition position = new ParsePosition(0);
        Number number = format.parse(str, position);
        if (position.getIndex() != str.length()) {
            throw new ParseException("Failed to parse number: " + str, position.getIndex());
        }
        return number;
    }

    /**
     * Formats a number as a localized string.
     * 
     * @param number
     * @param decimals
     * @return
     */
    public static String formatNumber(Number number, int decimals) {
        NumberFormat format = NumberFormat.getInstance();
        format.setMinimumFractionDigits(decimals);
        format.setMaximumFractionDigits(decimals);

        return format.format(number);
    }

    /**
     * Format a number with zero decimals.
     * 
     * @param number
     * @return
     */
    public static String formatNumber(Number number) {
        return formatNumber(number, 0);
    }

    /**
     * Formats a Semux value.
     * 
     * @param nano
     * @param withUnit
     * @return
     */
    public static String formatValue(long nano, boolean withUnit) {
        return formatNumber(nano / (double) Unit.SEM, 2) + (withUnit ? " SEM" : "");
    }

    /**
     * Formats a Semux value.
     * 
     * @param nano
     * @return
     */
    public static String formatValue(long nano) {
        return formatValue(nano, true);
    }

    /**
     * Parses a Semux value.
     * 
     * @param str
     * @return
     * @throws ParseException
     */
    public static long parseValue(String str) throws ParseException {
        if (str.endsWith(" SEM")) {
            str = str.substring(0, str.length() - 4);
        }
        return (long) (parseNumber(str).doubleValue() * Unit.SEM);
    }

    /**
     * Formats a percentage
     * 
     * @param percentage
     * @return
     */
    public static String formatPercentage(double percentage) {
        return formatNumber(percentage, 1) + " %";
    }

    /**
     * Format a timestamp into date string.
     * 
     * @param timestamp
     * @return
     */
    public static String formatTimestamp(long timestamp) {
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

        return format.format(new Date(timestamp));
    }

    /**
     * Parse timestamp from its string representation.
     * 
     * @param timestamp
     * @return
     * @throws ParseException
     */
    public static long parseTimestamp(String timestamp) throws ParseException {
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

        return format.parse(timestamp).getTime();
    }

    /**
     * Formats a vote
     * 
     * @param vote
     * @return
     */
    public static String formatVote(long vote) {
        return formatNumber(vote / (double) Unit.SEM);
    }

    /**
     * Parses a percentage.
     * 
     * @param str
     * @return
     * @throws ParseException
     */
    public static double parsePercentage(String str) throws ParseException {
        return parseNumber(str.substring(0, str.length() - 2)).doubleValue();
    }

    /**
     * Number string comparator based on its value.
     * 
     * @exception
     */
    public static final Comparator<String> NUMBER_COMPARATOR = (o1, o2) -> {
        try {
            return Double.compare(parseNumber(o1).doubleValue(), parseNumber(o2).doubleValue());
        } catch (ParseException e) {
            throw new NumberFormatException("Invalid number strings: " + o1 + ", " + o2);
        }
    };

    /**
     * Value string comparator based on its value.
     * 
     * @exception
     */
    public static final Comparator<String> VALUE_COMPARATOR = (o1, o2) -> {
        try {
            return Double.compare(parseValue(o1), parseValue(o2));
        } catch (ParseException e) {
            throw new NumberFormatException("Invalid number strings: " + o1 + ", " + o2);
        }
    };

    /**
     * Percentage string comparator based on its value.
     * 
     * @exception
     */
    public static final Comparator<String> PERCENTAGE_COMPARATOR = (o1, o2) -> {
        try {
            return Double.compare(parsePercentage(o1), parsePercentage(o2));
        } catch (ParseException e) {
            throw new NumberFormatException("Invalid number strings: " + o1 + ", " + o2);
        }
    };

    /**
     * Timestamp/date string comparator based on its value.
     * 
     * @exception
     */
    public static final Comparator<String> TIMESTAMP_COMPARATOR = (o1, o2) -> {
        try {
            return Long.compare(parseTimestamp(o1), parseTimestamp(o2));
        } catch (ParseException e) {
            throw new NumberFormatException("Invalid number strings: " + o1 + ", " + o2);
        }
    };

    /**
     * Returns an description of an account.
     * 
     * @param tx
     * @return
     */
    public static String getTransactionDescription(SemuxGUI gui, Transaction tx) {
        switch (tx.getType()) {
        case COINBASE:
            return GUIMessages.get("BlockReward") + " => "
                    + getDelegateName(gui, tx.getTo()).orElse(GUIMessages.get("UnknownDelegate"));
        case VOTE:
        case UNVOTE:
        case TRANSFER:
            return getAddressAlias(gui, tx.getFrom()) + " => " + getAddressAlias(gui, tx.getTo());
        case DELEGATE:
            return GUIMessages.get("DelegateRegistration");
        default:
            return StringUtil.EMPTY_STRING;
        }
    }

    /**
     * Returns the name of an address.
     * 
     * @param m
     * @param address
     * @return
     */
    private static String getAddressAlias(SemuxGUI gui, byte[] address) {
        Optional<String> name = getDelegateName(gui, address);
        if (name.isPresent()) {
            return name.get();
        }

        int n = gui.getModel().getAccountNumber(address);
        return n == -1 ? Hex.encodeWithPrefix(address) : GUIMessages.get("AccountNum", n);
    }

    /**
     * Returns the name of the delegate that corresponds to the given address.
     * 
     * @param address
     * @return
     */
    public static Optional<String> getDelegateName(SemuxGUI gui, byte[] address) {
        DelegateState ds = gui.getKernel().getBlockchain().getDelegateState();
        Delegate d = ds.getDelegateByAddress(address);

        return d == null ? Optional.empty() : Optional.of(d.getNameString());
    }
}
