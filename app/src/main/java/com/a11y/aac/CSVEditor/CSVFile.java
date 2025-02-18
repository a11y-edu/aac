package com.a11y.aac.CSVEditor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class CSVFile {
    private File file;
    private List<String[]> csvData = new ArrayList<>();


    public CSVFile(String path) throws Exception {
        file = new File(path);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath())));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String[] row = line.split(",");
            csvData.add(row);
        }
    }

    public CSVFile(File file) throws Exception {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath())));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String[] row = line.split(",");
            csvData.add(row);
        }
    }

    public CSVFile(String[] header, String path) {
        file = new File(path);
        if (file.exists()) {
            throw new RuntimeException("File already exists");
        }

        csvData.add(header);
        save();
    }

    public int getRowCount() {
        return csvData.size();
    }

    public void printData(int row) {
        for (String cell : csvData.get(row)) {
            System.out.print(cell + " ");
        }
        System.out.println();
    }

    public void clearData() {
        csvData.clear();
    }

    public void addRow(String[] row) {
        csvData.add(row);
    }

    public String[] getHeader() {
        return csvData.get(0);
    }

    public boolean checkIfDataExistsInARow(String data) {
        for (String[] row : csvData) {
            for (String cell : row) {
                if (cell.equals(data)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkIfHeaderExists(String header) {
        for (String cell : csvData.get(0)) {
            if (cell.equals(header)) {
                return true;
            }
        }
        return false;
    }

    public boolean deleteRowWithData(String data) {
        for (String[] row : csvData) {
            for (String cell : row) {
                if (cell.equals(data)) {
                    csvData.remove(row);
                    return true;
                }
            }
        }
        return false;
    }

    public List<String[]> getCSVData() {
        return csvData;
    }

    public List<String> getColumnData(int column) {
        if (column >= csvData.get(0).length) {
            throw new RuntimeException("Column index out of bounds");
        }
        List<String> columnData = new ArrayList<>();
        for (int i = 1; i < csvData.size(); i++) {
            columnData.add(csvData.get(i)[column]);
        }
        return columnData;
    }

    public List<String> getColumnData(String header) {
        int column = -1;
        for (int i = 0; i < csvData.get(0).length; i++) {
            if (csvData.get(0)[i].equals(header)) {
                column = i;
                break;
            }
        }
        if (column == -1) {
            throw new RuntimeException("Header not found");
        }
        return getColumnData(column);
    }

    public List<String> getColumnData(String header, float start, float end) {
        List<String> timeData = getColumnData("Time");
        List<String> columnData = getColumnData(header);
        List<String> filteredData = new ArrayList<>();
        for (int i = 0; i < timeData.size(); i++) {
            float time = Float.parseFloat(timeData.get(i));
            if (time >= start && time <= end) {
                filteredData.add(columnData.get(i));
            }
        }
        return filteredData;
    }

    public boolean editRowWithData(String data, String[] newRow) {
        for (String[] row : csvData) {
            for (String cell : row) {
                if (cell.equals(data)) {
                    csvData.set(csvData.indexOf(row), newRow);
                    return true;
                }
            }
        }
        return false;
    }

    public void save() {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath())))) {
            for (String[] row : csvData) {
                StringBuilder rowString = new StringBuilder();
                for (String cell : row) {
                    rowString.append(cell).append(",");
                }
                writer.write(rowString.toString());
                writer.newLine();
            }
        } catch (Exception e) {
            throw new RuntimeException("I am inside the CSVFile class" + e);
        }
    }

    public void applyTime() {
        for (int i = 1; i < csvData.size(); i++) {
            String[] row = csvData.get(i);
            row = extendArray(row, row.length + 1);
            // Frequency of 52 Hz
            row[row.length - 1] = String.valueOf((double) i / 50.0);
            csvData.set(i, row);
        }

        String[] header = csvData.get(0);
        header = extendArray(header, header.length + 1);
        header[header.length - 1] = "Time";
        csvData.set(0, header);
    }

    public void trimData(int startTime, int endTime) {
        double startTimeInSeconds = (double) startTime / 1000.0;
        double endTimeInSeconds = (double) endTime / 1000.0;
        List<String[]> trimmedData = new ArrayList<>();
        for(int i = 1; i < csvData.size(); i++) {
            String[] row = csvData.get(i);
            double time = Double.parseDouble(row[row.length - 1]);
            if (time >= startTimeInSeconds && time <= endTimeInSeconds) {
                trimmedData.add(row);
            }
        }

        trimmedData.add(0, csvData.get(0));
        csvData = trimmedData;
    }

    public void applyMovingAverage() {
        int windowSize = 2;
        List<Double> accX = new ArrayList<>();
        List<Double> accY = new ArrayList<>();
        List<Double> accZ = new ArrayList<>();

        for(int i = 1; i < csvData.size(); i++) {
            String[] row = csvData.get(i);
            accX.add(Double.parseDouble(row[1]));
            accY.add(Double.parseDouble(row[2]));
            accZ.add(Double.parseDouble(row[3]));
        }

        List<Double> movingAvgX = calculateMovingAverage(accX, windowSize);
        List<Double> movingAvgY = calculateMovingAverage(accY, windowSize);
        List<Double> movingAvgZ = calculateMovingAverage(accZ, windowSize);

        for (int i = 1; i < csvData.size(); i++) {
            String[] row = csvData.get(i);
            row = extendArray(row, row.length + 3);
            row[row.length - 3] = movingAvgX.get(i - 1) != null ? movingAvgX.get(i - 1).toString() : accX.get(i - 1).toString();
            row[row.length - 2] = movingAvgY.get(i - 1) != null ? movingAvgY.get(i - 1).toString() : accY.get(i - 1).toString();
            row[row.length - 1] = movingAvgZ.get(i - 1) != null ? movingAvgZ.get(i - 1).toString() : accZ.get(i - 1).toString();
            csvData.set(i, row);
        }

        String[] header = csvData.get(0);
        header = extendArray(header, header.length + 3);
        header[header.length - 3] = "acc_ma_x";
        header[header.length - 2] = "acc_ma_y";
        header[header.length - 1] = "acc_ma_z";
        csvData.set(0, header);
    }

    private List<Double> calculateMovingAverage(List<Double> data, int windowSize) {
        List<Double> movingAvg = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            if (i < windowSize - 1) {
                movingAvg.add(null);
            } else {
                double sum = 0.0;
                for (int j = i; j > i - windowSize; j--) {
                    sum += data.get(j);
                }
                movingAvg.add(sum / windowSize);
            }
        }
        return movingAvg;
    }

    public void applyDifferentiation() {
        List<Double> accX = new ArrayList<>();
        List<Double> accY = new ArrayList<>();
        List<Double> accZ = new ArrayList<>();

        for (int i = 1; i < csvData.size(); i++) {
            String[] row = csvData.get(i);
            accX.add(Double.parseDouble(row[1]));
            accY.add(Double.parseDouble(row[2]));
            accZ.add(Double.parseDouble(row[3]));
        }

        List<Double> diffX = calculateDifferences(accX);
        List<Double> diffY = calculateDifferences(accY);
        List<Double> diffZ = calculateDifferences(accZ);

        for (int i = 1; i < csvData.size(); i++) {
            String[] row = csvData.get(i);
            row = extendArray(row, row.length + 3);
            row[row.length - 3] = diffX.get(i - 1) != null ? diffX.get(i - 1).toString() : accX.get(i - 1).toString();
            row[row.length - 2] = diffY.get(i - 1) != null ? diffY.get(i - 1).toString() : accY.get(i - 1).toString();
            row[row.length - 1] = diffZ.get(i - 1) != null ? diffZ.get(i - 1).toString() : accZ.get(i - 1).toString();
            csvData.set(i, row);
        }

        String[] header = csvData.get(0);
        header = extendArray(header, header.length + 3);
        header[header.length - 3] = "acc_diff_x";
        header[header.length - 2] = "acc_diff_y";
        header[header.length - 1] = "acc_diff_z";
        csvData.set(0, header);
    }

    private String[] extendArray(String[] original, int newLength) {
        String[] extended = new String[newLength];
        System.arraycopy(original, 0, extended, 0, original.length);
        return extended;
    }

    private List<Double> calculateDifferences(List<Double> data) {
        List<Double> differences = new ArrayList<>();
        differences.add(null); // No difference for the first element
        for (int i = 1; i < data.size(); i++) {
            differences.add(data.get(i) - data.get(i - 1));
        }
        return differences;
    }

    public String[] getRowWithData(String data) {
        for (String[] row : csvData) {
            for (String cell : row) {
                if (cell.equals(data)) {
                    return row;
                }
            }
        }
        return null;
    }
}
