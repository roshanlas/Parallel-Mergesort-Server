package com.roshan.parallelserver;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class HomeActivity extends Activity {

    static TextView promptTxt;
    static ScrollView promptScroll;
    List<Integer> randomArray;
    int nRandom;
    static boolean arrayGenerated = false, parallelMode = false;
    long tStart, tEnd, tDelta;
    double elapsedSeconds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        promptTxt = (TextView) findViewById(R.id.txtPrompt);
        promptScroll = (ScrollView) findViewById(R.id.scrollPrompt);

        final RosChatServer chatServer = new RosChatServer(8888, getBaseContext());
        new Thread(new Runnable() {
            @Override
            public void run() {
                chatServer.runCommunicator();
            }
        }).start();

        Button generate = (Button) findViewById(R.id.btnGenerate);
        generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPromptText("Generating random array...");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String inputTemp = ((EditText) findViewById(R.id.nNumbers)).getText().toString();
                        inputTemp = inputTemp.equals("") ? "0" : inputTemp;
                        nRandom = Integer.parseInt(inputTemp);
                        if (nRandom <= 0) {
                            HomeActivity.setPromptText("Err: Enter a positive integer.");
                        } else {
                            randomArray = new ArrayList<Integer>(nRandom);
                            Random random = new Random();

                            for (int i = 0; i < nRandom; i++) {
                                randomArray.add(random.nextInt(1000));
                            }
                            arrayGenerated = true;
                            final String finalTemp = randomArray.toString();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    HomeActivity.setPromptText(finalTemp);
                                }
                            });
                        }
                    }
                }).start();
            }
        });

        Button execute = (Button) findViewById(R.id.btnExecute);
        execute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!arrayGenerated) {
                    setPromptText("Err: Array not generated.");
                } else {
                    if (parallelMode) {
                        int nDevices = RosChatServer.i;
                        setPromptText("Devices Connected: " + nDevices);
                        int i = (int) Math.floor(randomArray.size() / nDevices);
                        String[] part = new String[nDevices];

                        tStart = System.currentTimeMillis();

                        part[0] = TextUtils.join(",", randomArray.subList(0, i));
                        clientThread.inputData.offer(part[0]);
                        for (int j = 1; j < nDevices; j++) {
                            part[j] = TextUtils.join(",", randomArray.subList(j * i + 1, (j + 1) * i));
                            clientThread.inputData.offer(part[j]);
                        }

                        String temp1, temp2, result;
                        try {
                            temp1 = clientThread.outputData.take();
                            result = temp1;
                            for (int j = 1; j < nDevices; j++) {
                                temp2 = clientThread.outputData.take();
                                result = MergeSort(temp1, temp2);
                            }
                            tEnd = System.currentTimeMillis();
                            tDelta = tEnd - tStart;
                            elapsedSeconds = tDelta / 1000.0;

                            setPromptText("The Sorted Array:\n" + result);
                            setPromptText("Time Elapsed: " + elapsedSeconds + " sec");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private String MergeSort(String data1, String data2) {
        String[] strArray = data1.split(",");
        int[] intArray1 = new int[strArray.length];
        for (int i = 0; i < strArray.length; i++) {
            intArray1[i] = Integer.parseInt(strArray[i]);
        }
        strArray = data2.split(",");
        int[] intArray2 = new int[strArray.length];
        for (int i = 0; i < strArray.length; i++) {
            intArray2[i] = Integer.parseInt(strArray[i]);
        }
        intArray1 = merge(intArray1, intArray2);
        List<Integer> list = new ArrayList<Integer>();
        for (int index = 0; index < intArray1.length; index++) {
            list.add(intArray1[index]);
        }
        return TextUtils.join(",", list);
    }

    public static int[] merge(int[] a, int[] b) {
        int[] answer = new int[a.length + b.length];
        int i = a.length - 1, j = b.length - 1, k = answer.length;

        while (k > 0)
            answer[--k] = (j < 0 || (i >= 0 && a[i] >= b[j])) ? a[i--] : b[j--];
        return answer;
    }

    public static void setPromptText(final String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String temp = promptTxt.getText().toString();
                temp += "\n\n" + text;
                promptTxt.setText(temp);
                promptScroll.post(new Runnable() {
                    @Override
                    public void run() {
                        promptScroll.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        }).run();
    }
}
