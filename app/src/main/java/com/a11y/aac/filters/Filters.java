package com.a11y.aac.filters;

import android.content.Context;
import android.graphics.Color;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.TooltipCompat;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.slider.Slider;
import com.skydoves.powerspinner.DefaultSpinnerAdapter;
import com.skydoves.powerspinner.OnSpinnerItemSelectedListener;
import com.skydoves.powerspinner.PowerSpinnerView;
import com.a11y.aac.CSVEditor.CSVFile;
import com.a11y.aac.KMeans.KMeans;
import com.a11y.aac.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Filters extends ConstraintLayout {
    Slider acc, accMa, gyro;
    EditText accEditText, accMAEditText, gyroEditText;
    TextView totalValue;
    Button saveModelButton;
    Button updateModelButton;
    Button deleteModelButton;
    Button similarityButton;
    Button heatmapButton;
    LinearLayout updateDeleteLayout;
    CardView heatmapCardView;
    PowerSpinnerView heatmapTypeSpinner;
    private float value;
    Spinner modelSpinner;
    GridLayout gridLayout;
    TableLayout heatmapTableLayout;
    TextView threshold;
    ArrayAdapter<String> modelAdapter;
    List<String> models = new ArrayList<>();
    String modelPath;
    String[] prefixes;
    double[] weights;
    String[] csvFileNames;
    HashMap<String, Slider> prefixToSlider = new HashMap<>();
    HashMap<Slider, String> sliderToPrefix = new HashMap<>();
    HashMap<String, String> prefixToWeight = new HashMap<>();
    String gesture1, gesture2;
    String heatmapType;
    double[][] heatmap;

    public Filters(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Filters(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.filters_card, this, true);

        acc = findViewById(R.id.accSlider);
        accMa = findViewById(R.id.accMASlider);
        gyro = findViewById(R.id.gyroSlider);
        accEditText = findViewById(R.id.accEditText);
        accMAEditText = findViewById(R.id.accMAEditText);
        gyroEditText = findViewById(R.id.gyroEditText);
        totalValue = findViewById(R.id.totalValue);
        modelSpinner = findViewById(R.id.modelSpinner);
        saveModelButton = findViewById(R.id.saveModelButton);
        updateModelButton = findViewById(R.id.updateModelButton);
        deleteModelButton = findViewById(R.id.deleteModelButton);
        updateDeleteLayout = findViewById(R.id.updateDeleteLayout);
        similarityButton = findViewById(R.id.similarityButton);
        heatmapButton = findViewById(R.id.heatmapButton);
        gridLayout = findViewById(R.id.gridLayout);
        heatmapTableLayout = findViewById(R.id.heatmapTableLayout);
        heatmapCardView = findViewById(R.id.heatmapCardView);
        heatmapTypeSpinner = findViewById(R.id.heatmapTypeSpinner);
        threshold = findViewById(R.id.thresholdTextView);

        updateDeleteLayout.setVisibility(View.INVISIBLE);
        saveModelButton.setVisibility(View.VISIBLE);

        value = 0;
        heatmapType = "Instance";

        File directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        modelPath = directory.getAbsolutePath() + "/models";

        setSlider(acc, accEditText);
        setSlider(accMa, accMAEditText);
        setSlider(gyro, gyroEditText);

        prefixToSlider.put("acc", acc);
        prefixToSlider.put("acc_ma", accMa);
        prefixToSlider.put("gyro", gyro);

        sliderToPrefix.put(acc, "acc");
        sliderToPrefix.put(accMa, "acc_ma");
        sliderToPrefix.put(gyro, "gyro");

        prefixToWeight.put("acc", "0");
        prefixToWeight.put("acc_ma", "0");
        prefixToWeight.put("gyro", "0");

        loadModels();
        initSpinners();

        updateModelButton.setOnClickListener(v -> updateModel());
        saveModelButton.setOnClickListener(v -> saveModel());
        deleteModelButton.setOnClickListener(v -> deleteModel());
        similarityButton.setOnClickListener(v -> getSimilarity());
        heatmapButton.setOnClickListener(v -> generateHeatmapOfType());

        initHeatmapTypeSpinner();

        // set heatmap to random values between 0 and 1
//        int val = 5;
//        heatmap = new double[val][val];
//        for (int i = 0; i < heatmap.length; i++) {
//            for (int j = 0; j < heatmap[0].length; j++) {
//                heatmap[i][j] = Math.random();
//            }
//        }

        heatmap = new double[][]{{1.00, 0.23, 0.78, 0.65, 0.49}, {0.54, 1.00, 0.31, 0.90, 0.72}, {0.87, 0.44, 1.00, 0.55, 0.92}, {0.12, 0.86, 0.77, 1.00, 0.68}, {0.33, 0.29, 0.48, 0.71, 1.00}};
    }

    // Load all the models from the models directory
    private void loadModels() {
        models.clear();
        models.add("Choose a model");
        File modelDir = new File(modelPath);
        if (modelDir.exists()) {
            File[] modelFiles = modelDir.listFiles();
            if (modelFiles == null) {
                return;
            }
            for (File modelFile : modelFiles) {
                String modelName = modelFile.getName();
                System.out.println(modelName);
                if (modelName.startsWith("tempModelCacheA11y")) {
                    continue;
                }

                if (modelName.endsWith(".csv")) {
                    modelName = modelName.replace(".csv", "");
                }
                models.add(modelName);
            }
        }

        modelAdapter = new ArrayAdapter<>(this.getContext(), android.R.layout.simple_spinner_item, models);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(modelAdapter);
    }

    public void setGesture1(String gesture1) {
        this.gesture1 = gesture1;
    }

    public void setGesture2(String gesture2) {
        this.gesture2 = gesture2;
    }

    // Reset all sliders to 0
    private void setSlidersToZero() {
        moveSlider(acc, 0);
        moveSlider(accMa, 0);
        moveSlider(gyro, 0);
    }

    // Initialize the spinner with the models
    private void initSpinners() {
        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedModel = (String) parent.getItemAtPosition(position);
                if (selectedModel.equals("Choose a model")) {
                    setSlidersToZero();
                    moveSlider(acc, (float) 100);
                    accEditText.setText("100");
                    updateTotal(acc);
                    saveModelButton.setVisibility(View.VISIBLE);
                    updateDeleteLayout.setVisibility(View.INVISIBLE);
                } else {
                    selectedModel = selectedModel + ".csv";
                    saveModelButton.setVisibility(View.INVISIBLE);
                    updateDeleteLayout.setVisibility(View.VISIBLE);
                    setPrefixesAndWeights(new File(modelPath + "/" + selectedModel));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void initHeatmapTypeSpinner() {
        DefaultSpinnerAdapter heatmapTypeSpinnerAdapter = new DefaultSpinnerAdapter(heatmapTypeSpinner);
        List<CharSequence> items = Arrays.asList("Instance", "Gesture");
        heatmapTypeSpinnerAdapter.setItems(items);
        heatmapTypeSpinner.setSpinnerAdapter(heatmapTypeSpinnerAdapter);
        heatmapTypeSpinner.selectItemByIndex(0);

        heatmapTypeSpinner.setOnSpinnerItemSelectedListener((OnSpinnerItemSelectedListener<String>) (oldIndex, oldItem, newIndex, newItem) -> {
            heatmapType = newItem;
        });
    }

    // Set sliders range, step size and init the Listener
    private void setSlider(Slider slider, EditText editText) {
        slider.setValueFrom(0);
        slider.setValueTo(100);
        slider.setStepSize(1);
        slider.setValue(0);
        editText.setText("0");

        slider.addOnChangeListener((slider1, value, fromUser) -> {
            editText.setText(String.valueOf((int) value));
            updateTotal(slider1);
        });

        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard(getContext(), editText);
                String text = editText.getText().toString();
                try {
                    int currValue = 0;
                    if (!text.isEmpty()) {
                        currValue = Integer.parseInt(text);
                        if (currValue < 0) {
                            currValue = 0;
                        } else if (currValue > 100) {
                            currValue = 100;
                        }
                    }
                    slider.setValue(currValue);
                    updateTotal(slider);

                    // This is purely to remove focus from editText
                    similarityButton.requestFocus();
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                return true;
            }
            return false;
        });

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String text = editText.getText().toString();
                try {
                    int value = Integer.parseInt(text);
                    if (value < 0) {
                        editText.setText("0");
                        moveSlider(slider, 0);
                    } else if (value > 100) {
                        editText.setText("100");
                        moveSlider(slider, 100);
                    } else {
                        moveSlider(slider, value);
                    }
                } catch (NumberFormatException e) {
                }
            }
        });
    }

    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    // Move the slider to the value
    private void moveSlider(Slider slider, double value) {
        slider.setValue((float) value);
    }

    // Set the prefixes and weights from the model file
    private void setPrefixesAndWeights(File model) {
        CSVFile modelCSV;
        try {
            modelCSV = new CSVFile(model.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        prefixes = new String[modelCSV.getCSVData().size() - 1];
        weights = new double[modelCSV.getCSVData().size() - 1];

        List<String[]> data = modelCSV.getCSVData();
        for (int i = 1; i < data.size(); i++) {
            this.prefixes[i - 1] = data.get(i)[0];
            this.weights[i - 1] = Double.parseDouble(data.get(i)[1]);
        }

        this.setSlidersToZero();
        for (int i = 0; i < this.prefixes.length; i++) {
            Slider slider = this.prefixToSlider.get(this.prefixes[i]);
            assert slider != null;
            double weight = this.weights[i] * 100;
            this.moveSlider(slider, weight);
        }
    }

    private void updateTotal(Slider slider) {
        // Setting the weight for Slider that changed
        String currFilter = sliderToPrefix.get(slider);
        double currWeight = slider.getValue();
        prefixToWeight.put(currFilter, String.valueOf(currWeight));

        value = acc.getValue() + accMa.getValue() + gyro.getValue();
        if (value == 100.0) {
            totalValue.setTextColor(ContextCompat.getColor(this.getContext(), R.color.green));

            if (!modelSpinner.getSelectedItem().toString().equals("Choose a model")) {
                boolean isModelChanged = false;
                for (int i = 0; i < this.prefixes.length; i++) {
                    Slider currSlider = this.prefixToSlider.get(this.prefixes[i]);
                    double weight = currSlider.getValue() / 100;
                    weight = Math.round(weight * 100.0) / 100.0;
                    if (weight != this.weights[i]) {
                        isModelChanged = true;
                    }
                }

                if (isModelChanged) {
                    updateModelButton.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.theme));
                    updateModelButton.setEnabled(true);
                } else {
                    updateModelButton.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.theme2));
                    updateModelButton.setEnabled(false);
                }
            } else {
                saveModelButton.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.theme));
                saveModelButton.setEnabled(true);
            }
        } else {
            totalValue.setTextColor(ContextCompat.getColor(this.getContext(), R.color.red));

            saveModelButton.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.theme2));
            updateModelButton.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.theme2));

            saveModelButton.setEnabled(false);
            updateModelButton.setEnabled(false);
        }
        totalValue.setText(String.valueOf(value));
    }

    private void saveModel() {
        View dialogView = LayoutInflater.from(this.getContext()).inflate(R.layout.custom_alert_dialog, null);

        TextView dialogTitle = dialogView.findViewById(R.id.alertTitle);
        EditText modelName = dialogView.findViewById(R.id.fileNameEditText);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button deleteButton = dialogView.findViewById(R.id.deleteButton);
        deleteButton.setVisibility(View.INVISIBLE);

        dialogTitle.setText("Enter the model name");
        modelName.setHint("Model name");

        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String name = modelName.getText().toString();
            if (name.isEmpty()) {
                Toast.makeText(this.getContext(), "Model name is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (models.contains(name + ".csv")) {
                Toast.makeText(this.getContext(), "Model with the same name already exists", Toast.LENGTH_SHORT).show();
                return;
            }
            createModelFile(name);
            alertDialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> {
            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    private void generateHeatmapOfType() {
        if (heatmapType.equals("Instance")) {
            generateHeatmap();
        } else {
            generateHeatmapGesture();
        }
        Toast.makeText(this.getContext(), "Heatmap generated", Toast.LENGTH_SHORT).show();
    }

    private void updateModel() {
        View dialogView = LayoutInflater.from(this.getContext()).inflate(R.layout.custom_alert_dialog, null);

        TextView dialogTitle = dialogView.findViewById(R.id.alertTitle);
        EditText modelName = dialogView.findViewById(R.id.fileNameEditText);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button deleteButton = dialogView.findViewById(R.id.deleteButton);

        deleteButton.setVisibility(View.GONE);
        saveButton.setText("Update");
        modelName.setVisibility(View.GONE);
        dialogTitle.setText("Overwrite the model?");
        dialogTitle.setPadding(0, 0, 0, 20);

        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();

        saveButton.setOnClickListener(v -> {
            try {
                String currModelName = modelSpinner.getSelectedItem().toString();
                deleteModel(true);
                createModelFile(currModelName);
            } catch (Exception e) {
                e.printStackTrace();
            }

            updateModelButton.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.theme2));
            updateModelButton.setEnabled(false);
            alertDialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> {
            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    private void deleteModel(boolean updateSig) {
        File model = new File(modelPath + "/" + modelSpinner.getSelectedItem().toString() + ".csv");
        if (updateSig) {
            if (model.delete()) {
                return;
            } else {
                Toast.makeText(this.getContext(), "Failed to update model", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deleteModel() {
        File model = new File(modelPath + "/" + modelSpinner.getSelectedItem().toString() + ".csv");

        if (model.delete()) {
            Toast.makeText(this.getContext(), "Model deleted", Toast.LENGTH_SHORT).show();
            loadModels();
        } else {
            Toast.makeText(this.getContext(), "Failed to delete model", Toast.LENGTH_SHORT).show();
        }
    }

    private void createModelFile(String name) {
        try {
            CSVFile modelCSV = new CSVFile(new String[]{"prefix", "weight"}, modelPath + "/" + name + ".csv");
            prefixToWeight.forEach((prefix, weight) -> {
                if (Double.parseDouble(weight) == 0.0) {
                    return;
                }
                double weightValue = Double.parseDouble(weight) / 100;
                modelCSV.addRow(new String[]{prefix, String.valueOf(weightValue)});
            });
            modelCSV.save();
            if (!name.equals("tempModelCacheA11y")) {
                loadModels();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTempModelFile() {
        File tempFile = new File(modelPath + "/tempModelCacheA11y.csv");
        if (tempFile.exists()) {
            if (tempFile.delete()) {
                System.out.println("Deleted temp model file");
            }
        }

        createModelFile("tempModelCacheA11y");
    }

    // Add these as class fields
    private double[][] cachedHeatmap;
    private String lastSelectedModel = "";
    private Map<String, Integer> gestureIndices = new HashMap<>();

    private void getSimilarity() {
        if (value != 100.0) {
            Toast.makeText(this.getContext(), "Total value should be 100", Toast.LENGTH_SHORT).show();
            threshold.setText("N/A");
            return;
        }

        String currentModel = modelSpinner.getSelectedItem().toString();
        if (currentModel.equals("Choose a model")) {
            createTempModelFile();
        }

        // Only regenerate heatmap if model changed
        if (!currentModel.equals(lastSelectedModel) || cachedHeatmap == null) {
            getHeatmapData();
            lastSelectedModel = currentModel;

            // Cache gesture indices
            gestureIndices.clear();
            for (int i = 0; i < csvFileNames.length; i++) {
                gestureIndices.put(csvFileNames[i], i);
            }
        }

        // Use cached indices for lookup
        String g1 = gesture1 + ".csv";
        String g2 = gesture2 + ".csv";

        Integer x = gestureIndices.get(g1);
        Integer y = gestureIndices.get(g2);

        if (x == null || y == null) {
            threshold.setText("N/A");
            return;
        }

        double similarity = heatmap[x][y];
        similarity = Math.round(similarity * 100.0);
        threshold.setText(similarity + "%");
    }
    private void generateHeatmap() {
        if (value != 100.0) {
            Toast.makeText(this.getContext(), "Total value should be 100", Toast.LENGTH_SHORT).show();
            return;
        }

        if (modelSpinner.getSelectedItem().toString().equals("Choose a model")) {
            createTempModelFile();
        }

        heatmapCardView.setVisibility(View.VISIBLE);
        gridLayout.removeAllViews();
        heatmapTableLayout.removeAllViews();

        getHeatmapData();
        int numRows = heatmap.length;
        int numCols = heatmap[0].length;

        gridLayout.setRowCount(numRows + 1);
        gridLayout.setColumnCount(numCols + 1);

        // Create the first row for indices
        heatmapTableLayout.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.red));
        heatmapTableLayout.setBackground(ContextCompat.getDrawable(this.getContext(), R.drawable.rounded_corner));
        heatmapTableLayout.setClipToOutline(true);

        TableRow row = new TableRow(this.getContext());
        heatmapTableLayout.addView(row);

        TableRow.LayoutParams params = new TableRow.LayoutParams();

        for (int i = 0; i < csvFileNames.length; i++) {
            TextView textView = new TextView(this.getContext());
            textView.setText(String.valueOf(i + 1));
            textView.setTextSize(18);
            textView.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.black));
            textView.setPadding(16, 25, 16, 25);

            params = new TableRow.LayoutParams();
            if (i != 0) {
                params.setMarginStart(8);
            }
            params.setMargins(0, 0, 0, 8);
            textView.setLayoutParams(params);
            row.addView(textView);
        }


        TableRow row2 = new TableRow(this.getContext());
        params = new TableRow.LayoutParams();
        row2.setLayoutParams(params);
        heatmapTableLayout.addView(row2);

        for (int i = 0; i < csvFileNames.length; i++) {
            TextView textView = new TextView(this.getContext());
            String name = csvFileNames[i].replace(".csv", "").replace("_", " ");
            textView.setText(name);
            textView.setTextSize(18);
            textView.setPadding(16, 25, 16, 25);
            textView.setGravity(Gravity.CENTER);
            textView.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.black));

            params = new TableRow.LayoutParams();
            if (i != 0) {
                params.setMarginStart(8);
            }
            textView.setLayoutParams(params);
            row2.addView(textView);
        }

        int cellSize = 150;  // Size in pixels for each cell

        for (int i = -1; i < numRows; i++) {
            for (int j = -1; j < numCols; j++) {
                TextView textView = new TextView(this.getContext());
                if (i == -1 && j == -1) {
                    textView.setText(String.valueOf(0));
                } else if (i == -1) {
                    textView.setText(String.valueOf(j + 1));
                } else if (j == -1) {
                    textView.setText(String.valueOf(i + 1));
                } else {
                    double rounded = Math.round(heatmap[i][j] * 100.0) / 100.0;
                    textView.setText(String.valueOf(rounded));

                    int finalI = i;
                    int finalJ = j;
                    textView.setOnClickListener(view -> {
                        String xName = csvFileNames[finalI].replace(".csv", "").replace("_", " ");
                        String yName = csvFileNames[finalJ].replace(".csv", "").replace("_", " ");

                        String tooltipText = "X: " + xName + ", Y: " + yName + "\nSimilarity: " + rounded;
                        TooltipCompat.setTooltipText(textView, tooltipText);
                        textView.performLongClick();
                    });
                }
                textView.setGravity(Gravity.CENTER);
                textView.setTextSize(18);
                if (i == -1 || j == -1) {
                    textView.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.green));
                } else {
                    textView.setBackgroundColor(getColorForValue((float) heatmap[i][j]));
                }
                GridLayout.LayoutParams gParams = new GridLayout.LayoutParams();
                gParams.width = cellSize;
                gParams.height = cellSize;
                textView.setLayoutParams(gParams);

                gridLayout.addView(textView);
            }
        }

        gridLayout.setBackground(ContextCompat.getDrawable(this.getContext(), R.drawable.rounded_corner));
        gridLayout.setClipToOutline(true);
    }

    public void generateHeatmapGesture() {
        if (value != 100.0) {
            Toast.makeText(this.getContext(), "Total value should be 100", Toast.LENGTH_SHORT).show();
            return;
        }

        if (modelSpinner.getSelectedItem().toString().equals("Choose a model")) {
            createTempModelFile();
        }

        heatmapCardView.setVisibility(View.VISIBLE);
        gridLayout.removeAllViews();
        heatmapTableLayout.removeAllViews();

        getHeatmapData();

        Map<String, List<Integer>> gestureGroups = new HashMap<>();

        for (int i = 0; i < csvFileNames.length; i++) {
            String gestureCategory = csvFileNames[i].substring(0, csvFileNames[i].lastIndexOf("_"));
            gestureGroups.computeIfAbsent(gestureCategory, k -> new ArrayList<>()).add(i);
        }

        int size = gestureGroups.size();
        double[][] gestureWiseMatrix = new double[size][size];

        int rowIndex = size - 1;
        for (String group1 : gestureGroups.keySet()) {
            int colIndex = size - 1;
            for (String group2 : gestureGroups.keySet()) {
                double sum = 0;
                int count = 0;

                List<Integer> indices1 = gestureGroups.get(group1);

                List<Integer> indices2 = gestureGroups.get(group2);

                for (int i : indices1) {
                    for (int j : indices2) {
                        if (i == j) {
                            continue;
                        }
                        double value = heatmap[i][j];
                        sum += heatmap[i][j];
                        count++;
                    }
                }

                gestureWiseMatrix[rowIndex][colIndex] = sum / count;
                colIndex--;
            }
            rowIndex--;
        }
        // Print the gesture-wise matrix
        // for (double[] row : gestureWiseMatrix) {
        //    for (double value : row) {
        //        System.out.printf("%.2f ", value);
        //    }
        //    System.out.println();
        // }

        int numRows = gestureWiseMatrix.length;
        int numCols = gestureWiseMatrix[0].length;

        gridLayout.setRowCount(numRows + 1);
        gridLayout.setColumnCount(numCols + 1);

        String[] gestureNames = gestureGroups.keySet().toArray(new String[0]);

        // Create the first row for indices
        heatmapTableLayout.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.red));
        heatmapTableLayout.setBackground(ContextCompat.getDrawable(this.getContext(), R.drawable.rounded_corner));
        heatmapTableLayout.setClipToOutline(true);

        TableRow row = new TableRow(this.getContext());
        heatmapTableLayout.addView(row);

        TableRow.LayoutParams params = new TableRow.LayoutParams();

        for (int i = 0; i < gestureNames.length; i++) {
            TextView textView = new TextView(this.getContext());
            textView.setText(String.valueOf(i + 1)); // Set indices as text
            textView.setTextSize(18);
            textView.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.black));
            textView.setPadding(16, 25, 16, 25);

            params = new TableRow.LayoutParams();
            if (i != 0) {
                params.setMarginStart(8);
            }
            params.setMargins(0, 0, 0, 8);
            textView.setLayoutParams(params);
            row.addView(textView);
        }


        TableRow row2 = new TableRow(this.getContext());
        params = new TableRow.LayoutParams();
        row2.setLayoutParams(params);
        heatmapTableLayout.addView(row2);

        for (int i = 0; i < gestureNames.length; i++) {
            TextView textView = new TextView(this.getContext());
            String name = gestureNames[i].replace("_", " ");
            textView.setText(name);
            textView.setTextSize(18);
            textView.setPadding(16, 25, 16, 25);
            textView.setGravity(Gravity.CENTER);
            textView.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.black));

            params = new TableRow.LayoutParams();
            if (i != 0) {
                params.setMarginStart(8);
            }
            textView.setLayoutParams(params);
            row2.addView(textView);
        }

        int cellSize = 150;

        for (int i = -1; i < numRows; i++) {
            for (int j = -1; j < numCols; j++) {
                TextView textView = new TextView(this.getContext());
                if (i == -1 && j == -1) {
                    textView.setText(String.valueOf(0));
                } else if (i == -1) {
                    textView.setText(String.valueOf(j + 1));
                } else if (j == -1) {
                    textView.setText(String.valueOf(i + 1));
                } else {
                    double rounded = Math.round(gestureWiseMatrix[i][j] * 100.0) / 100.0;
                    textView.setText(String.valueOf(rounded));
                    int finalI = i;
                    int finalJ = j;
                    textView.setOnClickListener(view -> {
                        String xName = gestureNames[finalI].replace("_", " ");
                        String yName = gestureNames[finalJ].replace("_", " ");

                        String tooltipText = "X: " + xName + ", Y: " + yName + "\nSimilarity: " + rounded;
                        TooltipCompat.setTooltipText(textView, tooltipText);
                        textView.performLongClick();
                    });
                }
                textView.setGravity(Gravity.CENTER);
                textView.setTextSize(18);
                if (i == -1 || j == -1) {
                    textView.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.green));
                } else {
                    textView.setBackgroundColor(getColorForValue((float) gestureWiseMatrix[i][j]));
                }
                GridLayout.LayoutParams gParams = new GridLayout.LayoutParams();
                gParams.width = cellSize;
                gParams.height = cellSize;
                // params.setMargins(8, 8, 8, 8);
                textView.setLayoutParams(gParams);

                gridLayout.addView(textView);
            }
        }

        gridLayout.setBackground(ContextCompat.getDrawable(this.getContext(), R.drawable.rounded_corner));
        gridLayout.setClipToOutline(true);
    }

    public int getColorForValue(float value) {
        value = Math.max(0, Math.min(1, value));

        int baseColor = Color.parseColor("#0057FF");
        int black = Color.rgb(0, 0, 0);

        int r = (int) (Color.red(black) * (1 - value) + Color.red(baseColor) * value);
        int g = (int) (Color.green(black) * (1 - value) + Color.green(baseColor) * value);
        int b = (int) (Color.blue(black) * (1 - value) + Color.blue(baseColor) * value);

        return Color.rgb(r, g, b);
    }

    private void getHeatmapData() {
        String currModel;
        if (modelSpinner.getSelectedItem().toString().equals("Choose a model")) {
            currModel = "tempModelCacheA11y.csv";
        } else {
            currModel = modelSpinner.getSelectedItem().toString();
        }

        KMeans kMeans = new KMeans(this.getContext());
        kMeans.setModel(currModel);

        heatmap = kMeans.performKMeans();
        for (double[] doubles : heatmap) {
            for (double aDouble : doubles) {
                System.out.print(aDouble + " ");
            }
            System.out.println();
        }
        csvFileNames = kMeans.getCSVFileNames();
    }
}
