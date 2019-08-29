/*
 * Copyright 2018 Picovoice Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.picovoice.porcupine.demo;


import android.Manifest;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.util.Locale;

import ai.picovoice.porcupinemanager.KeywordCallback;
import ai.picovoice.porcupinemanager.PorcupineManager;
import ai.picovoice.porcupinemanager.PorcupineManagerException;


public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{
    private PorcupineManager porcupineManager = null;
    private MediaPlayer notificationPlayer;
    private RelativeLayout layout;
    private ToggleButton recordButton;
    String text = "Hi Vipul Patel, How are you? I am just here and just gonna call you. Can you pickup the phone?";
    private TextToSpeech speech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_main);
        Utils.configurePorcupine(this);
        notificationPlayer = MediaPlayer.create(this, R.raw.notification);
        layout = findViewById(R.id.layout);
        recordButton = findViewById(R.id.record_button);

        // create the keyword spinner.
        configureKeywordSpinner();
        speech = new TextToSpeech(this, this);
        speech.setPitch(1f);
        speech.setSpeechRate(0.7f);
    }

    @Override
    public void onDestroy() {
        if (speech != null) {
            speech.stop();
            speech.shutdown();
        }
        super.onDestroy();
    }

    /**
     * Handler for the record button. Processes the audio and uses Porcupine library to detect the
     * keyword. It increments a counter to indicate the occurrence of a keyword.
     * @param view ToggleButton used for recording audio.
     */
    public void process(View view) {
        try {
            if (recordButton.isChecked()) {
                // check if record permission was given.
                if (Utils.hasRecordPermission(this)) {
                    porcupineManager = initPorcupine();
                    porcupineManager.start();

                } else {
                    Utils.showRecordPermission(this);
                }
            } else {
                porcupineManager.stop();
            }
        } catch (PorcupineManagerException e) {
            Utils.showErrorToast(this);
        }
    }

    public void unlockLockScreen(){
        DevicePolicyManager devicePolicyMngr= (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName compName=new ComponentName(this, DeviceAdminReceiver.class);
        if(!devicePolicyMngr.isAdminActive(compName))
            devicePolicyMngr.removeActiveAdmin(compName);
    }

    /**
     * Initialize the porcupineManager library.
     * @return Porcupine instance.
     */
    private PorcupineManager initPorcupine() throws PorcupineManagerException {
        //Spinner mySpinner= findViewById(R.id.keyword_spinner);
        String kwd = "Blueberry";//mySpinner.getSelectedItem().toString();
        // It is assumed that the file name is all lower-case and spaces are replaced with "_".
        String filename = kwd.toLowerCase().replaceAll("\\s+", "_");
        // get the keyword file and model parameter file from internal storage.
        String keywordFilePath = new File(this.getFilesDir(), filename + ".ppn")
                .getAbsolutePath();
        String modelFilePath = new File(this.getFilesDir(), "params.pv").getAbsolutePath();
        final int detectedBackgroundColor = getResources()
                .getColor(R.color.colorAccent);
        return new PorcupineManager(modelFilePath, keywordFilePath, 0.5f, new KeywordCallback() {
            @Override
            public void run(int keyword_index) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
                        boolean isScreenOn = false;
                        if (pm != null) {
                            isScreenOn = pm.isScreenOn();
                        }
                        Log.e("screen on", ""+isScreenOn);
                        if(!isScreenOn)
                        {
                            PowerManager.WakeLock wl = null;
                            if (pm != null) {
                                wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |PowerManager.ACQUIRE_CAUSES_WAKEUP |PowerManager.ON_AFTER_RELEASE,"digialminds:screenlock");
                            }
                            if (wl != null) {
                                wl.acquire(10000);
                            }
                            PowerManager.WakeLock wl_cpu = null;
                            if (pm != null) {
                                wl_cpu = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"digitalminds:cpulock");
                            }
                            if (wl_cpu != null) {
                                wl_cpu.acquire(10000);
                            }
                        }


                        unlockLockScreen();

                        if (!notificationPlayer.isPlaying()) {
                            notificationPlayer.start();
                        }
                        // change the background color for 1 second.
                        layout.setBackgroundColor(detectedBackgroundColor);
                        new CountDownTimer(1000, 100) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                if (!notificationPlayer.isPlaying()) {
                                    notificationPlayer.start();
                                }
                            }

                            @Override
                            public void onFinish() {
                                layout.setBackgroundColor(Color.TRANSPARENT);
                            }
                        }.start();

                        speakOut();
                    }
                });
            }
        });
    }

    /**
     * Check the result of the record permission request.
     * @param requestCode request code of the permission request.
     * @param permissions requested permissions.
     * @param grantResults results of the permission requests.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        // We only ask for record permission.
        if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED) {
            ToggleButton tbtn = findViewById(R.id.record_button);
            tbtn.toggle();
        } else {
            try {
                porcupineManager = initPorcupine();
                porcupineManager.start();
            } catch (PorcupineManagerException e) {
                Utils.showErrorToast(this);
            }
        }
    }

    /**
     * Configure the style and behaviour of the keyword spinner.
     */
    private void configureKeywordSpinner(){
        /*Spinner spinner = findViewById(R.id.keyword_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.keywords, R.layout.keyword_spinner_item);
        adapter.setDropDownViewResource(R.layout.keyword_spinner_item);
        spinner.setAdapter(adapter);

        // Make sure user stopped recording before changing the keyword.
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
                                       int position, long id) {
                if (recordButton.isChecked()) {
                    if (porcupineManager != null) {
                        try {
                            porcupineManager.stop();
                        } catch (PorcupineManagerException e) {
                            Utils.showErrorToast(getApplicationContext());
                        }
                    }
                    recordButton.toggle();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing.
            }
        });*/
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = speech.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(getApplicationContext(), "Language not supported", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(getApplicationContext(), "Init failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void calling(){
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:9052601154"));
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CALL_PHONE}, 111);
        } else {
            startActivity(intent);
        }
    }

    private void speakOut() {
        speech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
                final String keyword = s;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(getApplicationContext(), "Started" + keyword, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onDone(String s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        calling();
                        //Toast.makeText(getApplicationContext(), "Done ", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        calling();
                        //Toast.makeText(getApplicationContext(), "Error ", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "");
        speech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "Dummy String");
    }
}
