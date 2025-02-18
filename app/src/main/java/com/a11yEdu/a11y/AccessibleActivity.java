package com.a11yEdu.a11y;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsSubscription;
import com.a11yEdu.a11y.CSVEditor.CSVFile;
import com.a11yEdu.a11y.KMeans.KMeans;
import com.a11yEdu.a11y.KMeans.KMeansObj;

import java.io.File;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


//Todo: Fix and optimize AAC activity
public class AccessibleActivity extends AppCompatActivity implements CardAdapter.OnItemClickListener, AccItem.OnItemRemovedListener {
    private ArrayList<AccItem> accItems = new ArrayList<>();
    private TextToSpeech textToSpeech;
    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";
    private static final String PATH = "/Meas/IMU6/";
    private static final String RATE = "52";
    private MdsSubscription mdsSubscription;
    private String connectedSerial;
    private LinearLayout scrollableLayout;
    private Spinner modelSpinner;
    private List<CardData> cardDataList;
    private RecyclerView recyclerView;
    private SwitchCompat toggleRecognition;
    Spinner voiceSpinner;
    TextView logText;
    RadioGroup radioGroup;
    EditText timeEditText;
    KMeans kMeans;
    CSVFile masterFile;
    boolean noModels = false;
    private List<Voice> voiceList = new ArrayList<>();
    private int duration = 0;
    LocalTime currentTime, prevTime;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accessible);

        voiceSpinner = findViewById(R.id.voiceSpinner);
        logText = findViewById(R.id.logText);
//        timeEditText = findViewById(R.id.timeEditText);
        radioGroup = findViewById(R.id.radioGroup);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Fetch available voices
                textToSpeech.setLanguage(Locale.ENGLISH);
                Set<Voice> voices = textToSpeech.getVoices();
                if (voices != null) {
                    voiceList.clear();
                    for (Voice voice : voices) {
                        if (voice.getLocale().getLanguage().startsWith("en")) {
                            voiceList.add(voice); // Add only English voices
                        }
                    }

                    // Populate spinner with voice names
                    /* List<String> voiceNames = new ArrayList<>();
                    for (Voice voice : voiceList) {
                        voiceNames.add(voice.getName());
                    } */

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(AccessibleActivity.this, android.R.layout.simple_spinner_item, Arrays.asList("Male", "Female"));

                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    voiceSpinner.setAdapter(adapter);

                    String maleVoice = "en-us-x-iol-local";
                    String femaleVoice = "en-US-language";

                    // Set listener for spinner selection
                    voiceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                            boolean found = false;
                            String selectedVoice = parent.getItemAtPosition(position).toString();
                            if (selectedVoice == "Male") {
                                for (Voice voice : voiceList) {
                                    if (voice.getName().equals(maleVoice)) {
                                        textToSpeech.setVoice(voice);
//                                        Toast.makeText(AccessibleActivity.this, "Male voice selected", Toast.LENGTH_SHORT).show();
                                        found = true;
                                        break;
                                    }
                                }
                            } else {
                                for (Voice voice : voiceList) {
                                    if (voice.getName().equals(femaleVoice)) {
                                        textToSpeech.setVoice(voice);
//                                        Toast.makeText(AccessibleActivity.this, "Female voice selected", Toast.LENGTH_SHORT).show();
                                        found = true;
                                        break;
                                    }
                                }
                            }
                            if (!found) {
                                Toast.makeText(AccessibleActivity.this, "Device doesn't support this voice. Switching to default", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            // Do nothing
                        }
                    });
                }
            } else {
                Toast.makeText(AccessibleActivity.this, "TTS initialization failed!", Toast.LENGTH_SHORT).show();
            }
        });

        File directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File master = new File(directory, "master.csv");
        if (!master.exists()) {
            Toast.makeText(this, "No gestures found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        try {
            // todo This cannot be done anymore because there is option to ignore
            masterFile = new CSVFile(master);
            int cnt = masterFile.getRowCount();
            if (cnt == 1) {
                Toast.makeText(this, "No gestures found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            if (cnt == 2) {
                Toast.makeText(this, "No active gestures found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            } else {
                int ignoreCount = 2;
                List<String[]> csvData = masterFile.getCSVData();
                for (String[] row : csvData) {
                    if (!row[0].equals("Rest") && row[4].equals("true")) {
                        ignoreCount++;
                    }
                }
                if (ignoreCount == cnt) {
                    Toast.makeText(this, "No active gesture found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        kMeans = new KMeans(this);

        modelSpinner = findViewById(R.id.modelSpinner);
        recyclerView = findViewById(R.id.recyclerView);
        toggleRecognition = findViewById(R.id.enableRecognitionSwitch);
        scrollableLayout = findViewById(R.id.scrollableLayout);

        connectedSerial = getConnectedSerial();
        cardDataList = new ArrayList<>();

        initModelSpinner();
        initSpeechTimer();
        initToggleRecognition();
        populateGridLayout();


//        for (int i = 0; i < gridLayout.getChildCount(); i++) {
//            gridLayout.getChildAt(i).setOnClickListener(this);
//        }
    }

    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private void initSpeechTimer() {
        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        int savedTimerValue = sharedPreferences.getInt("timer_value", 0);

        if (savedTimerValue > 0) {
            duration = savedTimerValue;
        } else {
            duration = 0;
        }

        switch (duration) {
            case 0:
                radioGroup.check(R.id.radioButton1);
                break;
            case 3:
                radioGroup.check(R.id.radioButton2);
                break;
            case 5:
                radioGroup.check(R.id.radioButton3);
                break;
            case 7:
                radioGroup.check(R.id.radioButton4);
                break;
        }

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioButton1) {
                duration = 0;
            } else if (checkedId == R.id.radioButton2) {
                duration = 3;
            } else if (checkedId == R.id.radioButton3) {
                duration = 5;
            } else if (checkedId == R.id.radioButton4) {
                duration = 7;
            }

            SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("timer_value", duration);
            editor.apply();
        });


        prevTime = LocalTime.now();
    }

    private void initToggleRecognition() {
        toggleRecognition.setChecked(false);
        toggleRecognition.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                String connectedSerial = getConnectedSerial();
                if (connectedSerial == null || BluetoothActivity.mMds == null) {
                    Toast.makeText(this, "Connect to a bluetooth device", Toast.LENGTH_SHORT).show();
                    toggleRecognition.setChecked(false);
                    return;
                }
                if (noModels) {
                    Toast.makeText(this, "No models found", Toast.LENGTH_SHORT).show();
                    toggleRecognition.setChecked(false);
                    return;
                }
                modelSpinner.setEnabled(true);
                subscribeToSensor(connectedSerial);
                prevTime = LocalTime.now();
            } else {
                modelSpinner.setEnabled(false);
                unsubscribe();
            }
        });
    }


    private void initModelSpinner() {
        modelSpinner = findViewById(R.id.modelSpinner);
        modelSpinner.setEnabled(false);
        List<String> modelNames = kMeans.getModelNames();

        // Remove the temporary cache model from the spinner
        int i;
        boolean found = false;
        for (i = 0; i < modelNames.size(); i++) {
            if (modelNames.get(i).startsWith("tempModelCacheA11y")) {
                found = true;
                break;
            }
        }
        if (found) {
            modelNames.remove(i);
        }

        if (modelNames.size() == 0) {
            noModels = true;
        }

        // Remove the .csv extension from the model names
        for (i = 0; i < modelNames.size(); i++) {
            modelNames.set(i, modelNames.get(i).replace(".csv", ""));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modelNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(adapter);
        final AtomicBoolean isLoading = new AtomicBoolean(false);

        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // If already loading, skip this request
                Toast.makeText(AccessibleActivity.this, "Loading model! This may take a moment", Toast.LENGTH_SHORT).show();
                if (isLoading.get()) {
                    return;
                }

                // Set loading flag
                isLoading.set(true);

                // Unsubscribe first
                unsubscribe();

                new Thread(() -> {
                    try {
                        String selectedModel = modelSpinner.getSelectedItem().toString() + ".csv";
                        kMeans.setModel(selectedModel);

                        runOnUiThread(() -> {
                            if (toggleRecognition.isChecked()) {
                                subscribeToSensor(connectedSerial);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(AccessibleActivity.this,
                                    "Error loading model", Toast.LENGTH_SHORT).show();
                        });
                    } finally {
                        // Reset loading flag
                        isLoading.set(false);
                    }
                }).start();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }

    private void populateGridLayout() {
        List<String[]> data = masterFile.getCSVData();
        boolean firstRow = true;
        for (String[] row : data) {
            if (firstRow) {
                firstRow = false;
                continue;
            }
            if (Objects.equals(row[0], "Rest")) {
                continue;
            }
            if (Objects.equals(row[4], "true")) {
                continue;
            }
            CardData cardData = new CardData();
            cardData.setName(row[0]);
            cardData.setTextToSpeak(row[1].replace("|", ","));
            cardDataList.add(cardData);
        }

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        CardAdapter adapter = new CardAdapter(cardDataList, (CardAdapter.OnItemClickListener) this);
        recyclerView.setAdapter(adapter);
    }

    private String getConnectedSerial() {
        if (MainActivity.connectedSerial == null) {
            return BluetoothActivity.connectedSerial;
        }
        return MainActivity.connectedSerial;
    }

    private void subscribeToSensor(String connectedSerial) {
        if (mdsSubscription != null) {
            unsubscribe();
        }

        String strContract = "{\"Uri\": \"" + connectedSerial + PATH + RATE + "\"}";

        mdsSubscription = this.getMds().builder().build(this).subscribe(URI_EVENTLISTENER, strContract, new MdsNotificationListener() {
            @Override
            public void onNotification(String data) {
                ImuModel imuModel = new Gson().fromJson(data, ImuModel.class);

                if (imuModel == null) {
                    return;
                }

                ImuModel.ArrayAcc[] arrAcc = imuModel.getBody().getArrayAcc();
                ImuModel.ArrayGyro[] arrGyro = imuModel.getBody().getArrayGyro();
                long timestamp = imuModel.getBody().getTimestamp();

                if (arrAcc.length == 0 || arrGyro.length == 0) {
                    return;
                }

                for (int i = 0; i < arrAcc.length; i++) {
                    String sensorMsgStr =
                            "x: " +
                                    Math.round(arrAcc[i].getX() * 100) / 100.0 +
                                    "  y: " +
                                    Math.round(arrAcc[i].getY() * 100) / 100.0 +
                                    "  z: " +
                                    Math.round(arrAcc[i].getZ() * 100) / 100.0;

                    KMeansObj res = kMeans.performKMeans(timestamp, arrAcc[i].getX(), arrAcc[i].getY(), arrAcc[i].getZ(), arrGyro[i].getX(), arrGyro[i].getY(), arrGyro[i].getZ());
                    if (res != null) {
//                        System.out.println(res);
                    }

                    if (res != null) {
                        String log = res.getBucket() + " Res: " + res.getRes() + " with probability " + res.getMaxConfidence();

                        if (res.getRes().equals(res.getBucket())) {
                            logText.setTextColor(getResources().getColor(R.color.green));
                        } else {
                            logText.setTextColor(getResources().getColor(R.color.red));
                        }
                        logText.setText(log);
                        addAccItem(res.getRes(), res.getMaxConfidence());
                    }
                }
            }

            @Override
            public void onError(MdsException error) {
                unsubscribe();
            }
        });
    }

    private void addAccItem(String res, double conf) {
        if (res.equals("Rest")) {
            return;
        }

        AccItem accItem = new AccItem(this);
        accItem.setOnItemRemovedListener(this);

        accItem.setText1(res);
        accItem.setText3(String.valueOf(conf));
        boolean isPresent = false;

        String textToSpeak = "";
        for (int i = 0; i < cardDataList.size(); i++) {
            if (cardDataList.get(i).getName().toLowerCase().equals(res.toLowerCase())) {
                accItem.setText2(cardDataList.get(i).getTextToSpeak());
                textToSpeak = cardDataList.get(i).getTextToSpeak();

                isPresent = true;
                break;
            }
        }

        if (accItems.size() > 0) {
            AccItem lastItem = accItems.get(accItems.size() - 1);
            if (lastItem.getText1().equals(res)) {
                lastItem.setText3(String.valueOf(conf));

                // Speak if not spoken in the last "duration" seconds
                currentTime = LocalTime.now();
                Duration timePassed = Duration.between(prevTime, currentTime);
                if (timePassed.getSeconds() > duration) {
                    textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                    prevTime = currentTime;
                }

                return;
            }
        }

        if (!isPresent) {
            return;
        }

        textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);

        scrollableLayout.addView(accItem);
        accItems.add(accItem);
    }

    /**
     * Unsubscribe from the sensor
     */
    private void unsubscribe() {
        if (mdsSubscription != null) {
            mdsSubscription.unsubscribe();
            mdsSubscription = null;
        }
    }

    /**
     * Get the MDS instance
     *
     * @return MDS instance
     */
    private Mds getMds() {
        return MainActivity.mMds;
    }


    public void playButtonClicked(View view) {
        speakText();

        scrollableLayout.removeAllViews();
        accItems.clear();
    }

    private void speakText() {
        StringBuilder text = new StringBuilder();
        for (AccItem accItem : accItems) {
            text.append(accItem.getText2()).append(" ");
        }

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();

        unsubscribe();
    }


    @Override
    public void onItemRemoved(AccItem accItem) {
        accItems.remove(accItem);
    }

    @Override
    public void onItemClick(View view, int position) {
        CardData cardData = cardDataList.get(position);
        String name = cardData.getName();
        String textToSpeak = cardData.getTextToSpeak();

        AccItem accItem = new AccItem(this);
        accItem.setOnItemRemovedListener(this);
        accItem.setText1(name);
        accItem.setText2(textToSpeak);

        LinearLayout scrollableLayout = findViewById(R.id.scrollableLayout);
        scrollableLayout.addView(accItem);
        accItems.add(accItem);
    }

    public void backButtonClicked(View view) {
        finish();
    }

    public void clearButtonClicked(View view) {
        scrollableLayout.removeAllViews();
        accItems.clear();
    }
}