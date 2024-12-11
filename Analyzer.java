import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Analyzer {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("CSV Analyzer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);

            // Layout
            JPanel panel = new JPanel(new BorderLayout());
            JButton openButton = new JButton("Open CSV File");
            JTable table = new JTable();
            JScrollPane scrollPane = new JScrollPane(table);
            JButton analyzeButton = new JButton("Analyze Trends");

            // Add components
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.add(openButton, BorderLayout.WEST);
            topPanel.add(analyzeButton, BorderLayout.EAST);
            panel.add(topPanel, BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);
            frame.add(panel);

            // Open CSV button action
            openButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fileChooser = new JFileChooser();
                    int result = fileChooser.showOpenDialog(frame);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File file = fileChooser.getSelectedFile();
                        loadCSV(file, table);
                    }
                }
            });

            // Analyze button action
            analyzeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    analyzeTrends(table);
                }
            });

            frame.setVisible(true);
        });
    }

private static void loadCSV(File file, JTable table) {
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
        String line;
        String[] columns = null;
        DefaultTableModel model = new DefaultTableModel();

        boolean isFirstLine = true;
        List<Map<String, String>> data = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yy");

        while ((line = br.readLine()) != null) {
            // Use regex to handle cases with quotes around numbers with commas (e.g., "186,976")
            String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

            if (isFirstLine) {
                columns = new String[] {"Date", "Open", "High", "Low", "Close", "Adj Close", "Volume", "Dividend"};
                model.setColumnIdentifiers(columns);
                isFirstLine = false;
            } else {
                // Initialize an empty record map for each row
                Map<String, String> record = new HashMap<>();
                
                // Check if this line is a dividend-only line
                if (values.length == 7 && values[1].contains("Dividend")) {
                    // This line contains only a dividend, set values appropriately
                    record.put("Date", values[0]);
                    record.put("Open", "0");
                    record.put("High", "0");
                    record.put("Low", "0");
                    record.put("Close", "0");
                    record.put("Adj Close", "0");
                    record.put("Volume", "0");
                    
                    // Extract the numeric dividend value, removing any non-numeric characters
                    String dividendValue = values[1].replaceAll("[^0-9.]", "").trim();
                    record.put("Dividend", dividendValue.isEmpty() ? "0" : dividendValue);
                } else {
                    // Process normal rows with data in all columns
                    for (int i = 0; i < columns.length; i++) {
                        String value = i < values.length ? values[i] : "0";

                        // Handle "Volume" column with quoted values
                        if (columns[i].equals("Volume")) {
                            if (value.startsWith("\"") && value.endsWith("\"")) {
                                value = value.substring(1, value.length() - 1); // Remove both quotes
                            }
                            value = value.replace(",", ""); // Remove commas
                        }

                        // Handle "Dividend" column specifically
                        if (columns[i].equals("Dividend")) {
                            record.put(columns[i], value.isEmpty() ? "0" : value);
                        } else {
                            record.put(columns[i], value.isEmpty() ? "0" : value);
                        }
                    }
                }
                data.add(record);
            }
        }

        // Add the processed data to the table model
        for (Map<String, String> record : data) {
            Object[] rowData = new Object[columns.length];
            for (int i = 0; i < columns.length; i++) {
                rowData[i] = record.get(columns[i]);
            }
            model.addRow(rowData);
        }

        table.setModel(model);
    } catch (IOException e) {
        JOptionPane.showMessageDialog(null, "Error reading file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}

private static void analyzeTrends(JTable table) {
    DefaultTableModel model = (DefaultTableModel) table.getModel();
    int rowCount = model.getRowCount();
    int columnCount = model.getColumnCount();

    if (rowCount == 0 || columnCount < 7) {
        JOptionPane.showMessageDialog(null, "Insufficient data for analysis.", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    double totalVolume = 0;
    double totalClose = 0;
    double maxHigh = Double.MIN_VALUE;
    double minLow = Double.MAX_VALUE;
    double firstClose = 0;
    double lastClose = 0;
    int dataCount = 0;

    for (int i = 0; i < rowCount; i++) {
        try {
            String date = model.getValueAt(i, 0).toString();
            String closeStr = model.getValueAt(i, 4).toString();
            String highStr = model.getValueAt(i, 2).toString();
            String lowStr = model.getValueAt(i, 3).toString();
            String volumeStr = model.getValueAt(i, 6).toString();

            if (!date.isEmpty() && !closeStr.isEmpty() && !volumeStr.isEmpty()) {
                double close = Double.parseDouble(closeStr);
                double high = Double.parseDouble(highStr);
                double low = Double.parseDouble(lowStr);
                double volume = Double.parseDouble(volumeStr);

                totalClose += close;
                totalVolume += volume;
                maxHigh = Math.max(maxHigh, high);
                minLow = Math.min(minLow, low);

                if (dataCount == 0) {
                    firstClose = close;
                }
                lastClose = close;
                dataCount++;
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid data at row " + i + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error at row " + i + ": " + e.getMessage());
        }
    }

    if (dataCount == 0) {
        JOptionPane.showMessageDialog(null, "No valid data available for analysis.", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    String trend;
    double averageVolume = totalVolume / dataCount;

    if (lastClose > firstClose && averageVolume > 1000000) {
        trend = "Increasing in value (based on increasing close values and larger volume).";
    } else if (lastClose < firstClose && averageVolume > 1000000) {
        trend = "Decreasing in value (based on lower close value and volume).";
    } else if (averageVolume < 500000) {
        trend = "Stagnant (low volume).";
    } else {
        double volatility = maxHigh - minLow;
        if (volatility > (lastClose * 0.1)) {
            trend = "Volatile (discrepancies between high, low, and the open and close value).";
        } else {
            trend = "Stable.";
        }
    }

    JOptionPane.showMessageDialog(null, trend, "Trend Analysis", JOptionPane.INFORMATION_MESSAGE);
}
}
