package com.learn2crack.rx1000;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.STLog;
import com.st.st25sdk.TagHelper;
import com.st.st25sdk.type5.st25dv.ST25DVTag;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static android.os.SystemClock.sleep;
import static com.st.st25sdk.STException.STExceptionCode.CMD_FAILED;

public class overcurrent extends BaseActivity implements TagDiscovery.onTagDiscoveryCompletedListener{

    private final int ERROR     = -1;
    private final int TRY_AGAIN = 0;
    private final int OK        = 1;

    private final int DELAY_FTM = 20;

    private ActionCode mAction;
    private byte[] mFTMmsg;
    private byte[] mBuffer;

    // To Do compute exactly and dynamically
    private int mMaxPayloadSizeTx = 220;
    private int mMaxPayloadSizeRx = 32;

    private int mOffset;
    private int get_I1,delay_spin,get_TI1,get_ktI, get_I2, get_TI2, get_I3,get_TI3;
    double get_I1_d, get_TI1_d, get_ktI_d, get_I2_d, get_TI2_d, get_I3_d,get_TI3_d;
    private String I1_result[], TI1_result[],ktI_result[], I2_result[],TI2_result[], I3_result[],TI3_result[];
    private String I1_1,I1_0,TI1_1,TI1_0,ktI_1,ktI_0,I2_1,I2_0,TI2_1,TI2_0,I3_1,I3_0,TI3_1,TI3_0,crc_low,crc_high;

    static private NFCTag mftmTag;
    static private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private static final String TAG = overcurrent.class.getSimpleName();
    CRC16checker crccheck=new CRC16checker();
    StepParam StepType=new StepParam();
    Handler handler = new Handler();
    DecimalFormat df2 = new DecimalFormat("0.00");

    private Button writebtn, readbtn;
    private Spinner spinner1, spinner2,spinner3;
    private EditText editTI2,editI2,editTI3,editI,editI3,editTI,editktI;
    private boolean readRun = false;
    private boolean writeRun = false;

    private boolean result = false;
    private boolean write_result = false;

    private Runnable readCode;
    private Runnable syncCode;
    private Runnable runCode;

    private AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            ((TextView) parent.getChildAt(0)).setTextColor(Color.BLACK);
            ((TextView) parent.getChildAt(0)).setTextSize(12);
            ((TextView) parent.getChildAt(0)).setTypeface(Typeface.DEFAULT_BOLD);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            ((TextView) parent.getChildAt(0)).setTextColor(Color.BLACK);
            ((TextView) parent.getChildAt(0)).setTextSize(12);
            ((TextView) parent.getChildAt(0)).setTypeface(Typeface.DEFAULT_BOLD);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.write_ftm);
        FrameLayout contentFrameLayout = (FrameLayout) findViewById(R.id.content_frame);
        getLayoutInflater().inflate(R.layout.overcurrent, contentFrameLayout);
        overcurrent.this.setTitle("OVERCURRENT SETTING");
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this); //check NFC hardware available
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        //define I> Setting
        editI= (EditText) findViewById(R.id.edit_I);

        //define I> delay type spinner
        spinner1 = (Spinner) findViewById(R.id.I_delay_option);
        spinner1.setOnItemSelectedListener(listener);

        //define tI> Setting
        editTI= (EditText) findViewById(R.id.edit_tI);

        //define ktI> Setting
        editktI= (EditText) findViewById(R.id.edit_ktI);

        //define I>> setting
        spinner2 = (Spinner) findViewById(R.id.I2_setting);
        spinner2.setOnItemSelectedListener(listener);
        editI2 = (EditText) findViewById(R.id.edit_I2);

        //define tI>> Setting
        editTI2 = (EditText) findViewById(R.id.edit_tI2);

        //define I>>> Setting
        spinner3= (Spinner) findViewById(R.id.I3_setting);
        spinner3.setOnItemSelectedListener(listener);
        editI3 = (EditText) findViewById(R.id.edit_I3);

        //define tI>>> Setting
        editTI3 = (EditText) findViewById(R.id.edit_tI3);

        //press read
        readbtn = findViewById(R.id.btn_read_ftm);
        readbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (readRun){
                    readRun = false;
                    readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                } else {
                    readRun = true;
                    readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_green));
                }
            }
        });

        //press save
        writebtn = findViewById(R.id.btn_write_ftm);
        writebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!writeRun) {
                    String request = "01100100000810";

                    //edit I> Setting
                    if (TextUtils.isEmpty(editI.getText().toString())) {
                        editI.setError("I> setting cannot be empty");
                        return;
                    } else {
                        get_I1_d = Double.parseDouble((editI.getText().toString()));
                        Log.d(TAG, "Mikro result I1 setting from apps before process " + get_I1_d);
                        get_I1 = StepParam.getstep2(get_I1_d,50,1250);
                        Log.d(TAG, "Mikro result I1 setting from apps after process " + get_I1);
                        if (get_I1==0){
                            if (get_I1_d<=0.5) {
                                editI.setError("I> setting cannot be less than 0.5");
                            }else if (get_I1_d>12.5){
                                editI.setError("I> setting cannot be more than 12.5");
                            }
                            return;
                        }
                        String I1_display=df2.format(get_I1/100.00);
                        editI.setText(I1_display);
                        I1_result= hexStringToStringArray(Integer.toHexString(get_I1));
                        I1_1 = I1_result[0];
                        I1_0= I1_result[1];
                        Log.d(TAG, "Mikro result I1 in Hex " + I1_1 +I1_0);
                    }
                    request = request + I1_1 + I1_0;
                    Log.d(TAG, "Mikro after I1 " + request);

                    //Delay Type
                    String delay = String.valueOf(spinner1.getSelectedItem());
                    Log.d(TAG, "Mikro: delay spinner: " + delay);
                    if (delay.equals("DT")) {
                        delay_spin = 0;
                        request = request + "0000";
                        Log.d(TAG, "Mikro: delay spinner " + request);
                    } else if (delay.equals("NI")) {
                        delay_spin = 1;
                        request = request + "0001";
                        Log.d(TAG, "Mikro: delay spinner " + request);
                    } else if (delay.equals("VI")) {
                        delay_spin = 2;
                        request = request + "0002";
                        Log.d(TAG, "Mikro: delay spinner " + request);
                    } else if (delay.equals("EI")) {
                        delay_spin = 3;
                        request = request + "0003";
                        Log.d(TAG, "Mikro: delay spinner " + request);
                    }else if (delay.equals("LTI")) {
                        delay_spin = 4;
                        request = request + "0004";
                        Log.d(TAG, "Mikro: delay spinner " + request);
                    } else if (delay.equals("NI1.3/10")) {
                        delay_spin = 5;
                        request = request + "0005";
                        Log.d(TAG, "Mikro: delay spinner " + request);
                    }

                //edit tI> Setting
                    if (TextUtils.isEmpty(editTI.getText().toString())) {
                        editTI.setError("tI> setting cannot be empty");
                        return;
                    } else {
                        get_TI1_d = Double.parseDouble((editTI.getText().toString()));
                        Log.d(TAG, "Mikro result tI> setting from apps before process " + get_TI1_d);
                        get_TI1 = StepParam.getstep1(get_TI1_d,3,10000);
                        Log.d(TAG, "Mikro result tI> setting from apps after process " + get_TI1);
                        if (get_TI1==0){
                            if (get_TI1_d<0.03) {
                                editTI.setError("tI> setting cannot be less than 0.03");
                            }else if (get_TI1_d>100){
                                editTI.setError("tI> setting cannot be more than 100");
                            }
                            return;
                        }
                        String TI1_display=df2.format(get_TI1/100.00);
                        editTI.setText(TI1_display);
                        TI1_result= hexStringToStringArray(Integer.toHexString(get_TI1));
                        TI1_1 = TI1_result[0];
                        TI1_0= TI1_result[1];
                        Log.d(TAG, "Mikro result tI> in Hex " + TI1_1 +TI1_0);
                    }
                    request = request + TI1_1 + TI1_0;
                    Log.d(TAG, "Mikro after tI> " + request);

                    //edit ktI> Setting
                    if (TextUtils.isEmpty(editktI.getText().toString())) {
                        editktI.setError("ktI> setting cannot be empty");
                        return;
                    } else {
                        get_ktI_d = Double.parseDouble((editktI.getText().toString()));
                        Log.d(TAG, "Mikro result ktI> setting from apps before process " + get_ktI_d);
                        get_ktI = StepParam.getstep0(get_ktI_d,1,100);
                        Log.d(TAG, "Mikro result ktI> setting from apps after process " + get_ktI);
                        if (get_ktI==0){
                            if (get_ktI_d<0.01) {
                                editktI.setError("ktI> setting cannot be less than 0.01");
                            }else if (get_ktI_d>1){
                                editktI.setError("ktI> setting cannot be more than 1");
                            }
                            return;
                        }
                        //String TI1_display=df2.format(get_TI1/100.00);
                        //editktI.setText(TI1_display);
                        ktI_result= hexStringToStringArray(Integer.toHexString(get_ktI));
                        ktI_1 = ktI_result[0];
                        ktI_0= ktI_result[1];
                        Log.d(TAG, "Mikro result ktI> in Hex " + ktI_1 +ktI_0);
                    }
                    request = request + ktI_1 + ktI_0;
                    Log.d(TAG, "Mikro after ktI> " + request);

                    //edit I>> Setting
                    String spin2 = String.valueOf(spinner2.getSelectedItem());
                    if (spin2.equals("OFF")){
                        editI2.setText("0");
                        I2_1 = "00";
                        I2_0= "00";
                    } else {
                        if (TextUtils.isEmpty(editI2.getText().toString())) {
                            editI2.setError("I>> setting cannot be empty");
                            return;
                        } else {
                            get_I2_d = Double.parseDouble((editI2.getText().toString()));
                            Log.d(TAG, "Mikro result I2 setting from apps before process " + get_I2_d);

                            get_I2 = StepParam.getstep2(get_I2_d, 50, 10000);
                            Log.d(TAG, "Mikro result I2 setting from apps after process " + get_I2);
                            if (get_I2 == 0) {
                                if (get_I2_d < 0.5) {
                                    editI2.setError("I>> setting cannot be less than 0.5");
                                } else if (get_I2_d > 100) {
                                    editI2.setError("I>> setting cannot be more than 100");
                                }
                                return;
                            }
                        }
                        String I2_display=df2.format(get_I2/100.00);
                        editI2.setText(I2_display);
                        I2_result= hexStringToStringArray(Integer.toHexString(get_I2));
                        I2_1 = I2_result[0];
                        I2_0= I2_result[1];
                        Log.d(TAG, "Mikro result I2 in Hex " + I2_1 +I2_0);
                    }
                    request = request + I2_1 + I2_0;
                    Log.d(TAG, "Mikro after I2 " + request);

                    //edit tI>> Setting
                    if (TextUtils.isEmpty(editTI2.getText().toString())) {
                        editTI.setError("tI>> setting cannot be empty");
                        return;
                    } else {
                        get_TI2_d = Double.parseDouble((editTI2.getText().toString()));
                        Log.d(TAG, "Mikro result tI>> setting from apps before process " + get_TI2_d);
                        get_TI2 = StepParam.getstep3(get_TI2_d,3,10000);
                        Log.d(TAG, "Mikro result tI>> setting from apps after process " + get_TI2);
                        if (get_TI2==0){
                            if (get_TI2_d<0.03) {
                                editTI2.setError("tI>> setting cannot be less than 0.03");
                            }else if (get_TI2_d>100){
                                editTI2.setError("tI>> setting cannot be more than 100");
                            }
                            return;
                        }
                        String TI2_display=df2.format(get_TI2/100.00);
                        editTI2.setText(TI2_display);
                        TI2_result= hexStringToStringArray(Integer.toHexString(get_TI2));
                        TI2_1 = TI2_result[0];
                        TI2_0= TI2_result[1];
                        Log.d(TAG, "Mikro result tI>> in Hex " + TI2_1 +TI2_0);
                    }
                    request = request + TI2_1 + TI2_0;
                    Log.d(TAG, "Mikro after tI>> " + request);

                    //edit I>>> Setting
                    String spin3 = String.valueOf(spinner3.getSelectedItem());
                    if (spin3.equals("OFF")){
                        editI3.setText("0");
                        I3_1 = "00";
                        I3_0= "00";
                    } else {
                        if (TextUtils.isEmpty(editI3.getText().toString())) {
                            editI3.setError("I>> setting cannot be empty");
                            return;
                        } else {
                            get_I3_d = Double.parseDouble((editI3.getText().toString()));
                            Log.d(TAG, "Mikro result I3 setting from apps before process " + get_I3_d);

                            get_I3 = StepParam.getstep2(get_I3_d, 50, 10000);
                            Log.d(TAG, "Mikro result I3 setting from apps after process " + get_I3);
                            if (get_I3 == 0) {
                                if (get_I3_d < 0.5) {
                                    editI3.setError("I>> setting cannot be less than 0.5");
                                } else if (get_I3_d > 100) {
                                    editI3.setError("I>> setting cannot be more than 100");
                                }
                                return;
                            }
                        }
                        String I3_display=df2.format(get_I3/100.00);
                        editI3.setText(I3_display);
                        I3_result= hexStringToStringArray(Integer.toHexString(get_I3));
                        I3_1 = I3_result[0];
                        I3_0= I3_result[1];
                        Log.d(TAG, "Mikro result I3 in Hex " + I3_1 +I3_0);
                    }
                    request = request + I3_1 + I3_0;
                    Log.d(TAG, "Mikro after I3 " + request);

                    //edit tI>>> Setting
                    if (TextUtils.isEmpty(editTI3.getText().toString())) {
                        editTI3.setError("tI>>> setting cannot be empty");
                        return;
                    } else {
                        get_TI3_d = Double.parseDouble((editTI3.getText().toString()));
                        Log.d(TAG, "Mikro result tI>>> setting from apps before process " + get_TI3_d);
                        get_TI3 = StepParam.getstep3(get_TI3_d,3,10000);
                        Log.d(TAG, "Mikro result tI>> setting from apps after process " + get_TI3);
                        if (get_TI3==0){
                            if (get_TI3_d<0.03) {
                                editTI3.setError("tI>>> setting cannot be less than 0.03");
                            }else if (get_TI3_d>100){
                                editTI3.setError("tI>>> setting cannot be more than 100");
                            }
                            return;
                        }
                        String TI3_display=df2.format(get_TI3/100.00);
                        editTI3.setText(TI3_display);
                        TI3_result= hexStringToStringArray(Integer.toHexString(get_TI3));
                        TI3_1 = TI3_result[0];
                        TI3_0= TI3_result[1];
                        Log.d(TAG, "Mikro result tI>>> in Hex " + TI3_1 +TI3_0);
                    }
                    request = request + TI3_1 + TI3_0;
                   // request="0110010000081000000000000000000000000000000000";
                    Log.d(TAG, "Mikro after tI>>> " + request);

                    Log.d(TAG, "Mikro final sent: " + request);

                    //obtain CRC
                    int[] ans= crccheck.getCRC(request);
                    crc_low = Integer.toHexString(ans[0]);
                    crc_high = Integer.toHexString(ans[1]);
                    Log.d(TAG, "Mikro CRC: " + crc_high + " " + crc_low);

                    writeRun = true;
                    writebtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_green));
                    //singleFTM();
                }else {
                    writeRun=false;
                    writebtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                }

            }
        });

        readCode = new Runnable() {
            @Override
            public void run() {
                // Do something here on the main thread

                mAction = ActionCode.READ;
                fillView(mAction);
                //  Log.d(TAG, "Mikro: readCode: read normal");
            }
        };

        syncCode = new Runnable() {
            @Override
            public void run() {
                // Do something here on the main thread
                syncFTM();
                Log.d(TAG, "Mikro: syncCode");

            }
        };

        runCode = new Runnable() {
            @Override
            public void run() {
                // Do something here on the main thread

                if(writeRun)
                {
                    Log.d(TAG, "Mikro: runCode: write");
                    singleFTM();
                }

                if(readRun)
                {
                    Log.d(TAG, "Mikro: runCode: read");
                    syncFTM();
                }

            }
        };

    }


    @Override
    public void onPause() {
        super.onPause();

        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }


    }

    @Override
    public void onResume() {
        Intent intent = getIntent();
        //Log.d(TAG, "Resume mainActivity intent: " + intent);
        // write 2

        super.onResume();


        if (mNfcAdapter != null)
        {
            Log.d(TAG, "Mikro: on Resume(NfcAdapter)");
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null /*nfcFiltersArray*/, null /*nfcTechLists*/);

            if(mNfcAdapter.isEnabled())
            {
                //if hardward nfc available
            }
            else
            {
                //if hardward nfc not available
            }
        }
        else
        {
            //if hardward nfc not available
        }

    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent (write 1)
        Log.d(TAG, "Mikro :onNewIntent " + intent);
        //setIntent(intent);

        Tag androidTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (androidTag != null) {
            // A tag has been taped
            // Toast.makeText(this, "NFC detected on FTM!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Mikro: NFC detected system setting.");
            // Perform tag discovery in an asynchronous task
            // onTagDiscoveryCompleted() will be called when the discovery is completed.
            new TagDiscovery(this).execute(androidTag);

            // This intent has been processed. Reset it to be sure that we don't process it again
            // if the MainActivity is resumed
            setIntent(null);
        }

    }


    @Override
    public void onTagDiscoveryCompleted(NFCTag nfcTag, TagHelper.ProductID productId, STException error) {
        //Toast.makeText(getApplication(), "onTagDiscoveryCompleted. productId:" + productId, Toast.LENGTH_LONG).show();
        if (error != null) {
            Log.i(TAG, error.toString());
            return;
        }

        mftmTag = nfcTag;

        if(mftmTag!=null)
        {
            //write 3
            handler.postDelayed(runCode,50);  //try to read the message first
        }

    }

    public void singleFTM() {
        //write 4
        if (mftmTag == null) {
            return;
        }

        // int i = 1;
        mBuffer = new byte[25];
        mBuffer [0] = (byte)0x01;
        mBuffer [1] = (byte)0x10;
        mBuffer [2] = (byte)0x01;
        mBuffer [3] = (byte)0x00;
        mBuffer [4] = (byte)0x00;
        mBuffer [5] = (byte)0x08;
        mBuffer [6] = (byte)0x10;
        mBuffer [7] = (byte)Integer.parseInt(I1_1,16);
        mBuffer [8] = (byte)Integer.parseInt(I1_0,16);
        mBuffer [9] = (byte)0x00;
        mBuffer [10] = (byte)delay_spin;
        mBuffer [11] = (byte)Integer.parseInt(TI1_1,16);
        mBuffer [12] = (byte)Integer.parseInt(TI1_0,16);
        mBuffer [13] = (byte)Integer.parseInt(ktI_1,16);
        mBuffer [14] = (byte)Integer.parseInt(ktI_0,16);
        mBuffer [15] = (byte)Integer.parseInt(I2_1,16);
        mBuffer [16] = (byte)Integer.parseInt(I2_0,16);
        mBuffer [17] = (byte)Integer.parseInt(TI2_1,16);
        mBuffer [18] = (byte)Integer.parseInt(TI2_0,16);
        mBuffer [19] = (byte)Integer.parseInt(I3_1,16);
        mBuffer [20] = (byte)Integer.parseInt(I3_0,16);
        mBuffer [21] = (byte)Integer.parseInt(TI3_1,16);
        mBuffer [22] = (byte)Integer.parseInt(TI3_0,16);
        mBuffer [23] = (byte)Integer.parseInt(crc_high,16);
        mBuffer [24] = (byte)Integer.parseInt(crc_low,16);
        //mBuffer [27] = (byte)0x95;
        //mBuffer [28] = (byte)0x26 ;
        Log.d(TAG, "Mikro byte tl>: " + mBuffer[11] + ","+ mBuffer[12]);
        Log.d(TAG, "Mikro byte tl>>>: " + mBuffer[21] + ","+ mBuffer[22]);
        Log.d(TAG, "Mikro byte CRC: " + mBuffer[23] + ","+ mBuffer[24]);
        mAction = ActionCode.SINGLE;
        fillView(mAction);
    }

    public void syncFTM() {

        if (mftmTag == null) {
            return;
        }

        mBuffer = new byte[8];
        mBuffer [0] = (byte)0x01;
        mBuffer [1] = (byte)0x03;
        mBuffer [2] = (byte)0x02;
        mBuffer [3] = (byte)0x00;
        mBuffer [4] = (byte)0x00;
        mBuffer [5] = (byte)0x08;
        //0xF045 when address start from 0100
       // mBuffer [6] = (byte)0x45;
       // mBuffer [7] = (byte)0xf0;
        //0xB445 when address start from 0200
        mBuffer [6] = (byte)0x45;
        mBuffer [7] = (byte)0xb4;

        mAction = ActionCode.SYNC;
        fillView(mAction);
    }

    public void resetFTM() {

        if (mftmTag == null) {
            return;
        }

        mAction = ActionCode.RESET;
        fillView(mAction);
    }

    public void fillView(ActionCode action) {

        new Thread(new ContentView(action)).start();
    }

    private enum ActionCode {
        SINGLE,
        SYNC,
        READ,
        RESET
    }

    class ContentView implements Runnable {

        ActionCode mAction;

        public ContentView(ActionCode action) {
            mAction = action;

        }

        public void run() {

            switch (mAction){
                case SINGLE:
                    try {
                        if(((ST25DVTag)mftmTag).isMailboxEnabled(false))
                        {
                            if(((ST25DVTag)mftmTag).hasHostPutMsg(true)==false) {
                                //if mailbox available
                                if (sendSimpleData() == OK) {
                                    // write 5
                                    Log.d(TAG, "Mikro: write single OK ");

                                    handler.postDelayed(readCode, DELAY_FTM);
                                } else {
                                    Log.d(TAG, "Mikro: Fail on single");
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            writebtn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                        }
                                    });
                                    writeRun = false;
                                }
                            } else{
                                Log.d(TAG, "Mikro: Fail on host msg full");
                                reset_ftm();
                                handler.postDelayed(runCode,DELAY_FTM);
                            }
                        } else{
                            Log.d(TAG, "Mikro: Fail on no reason");
                            writeRun=false;
                            reset_ftm();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    writebtn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                    //writebtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                                    Toast.makeText(overcurrent.this, "FTM OFF", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                    } catch (STException e) {
                        Log.d(TAG, "mikro: Error on Single");
                        e.printStackTrace();
                    }
                    break;
                case SYNC:
                    try {
                        if(((ST25DVTag)mftmTag).isMailboxEnabled(false))
                        {
                            Log.d(TAG, "Mikro: run Sync ");
                            //if mailbox available
                            if(sendSimpleData() == OK)
                            {
                                Log.d(TAG, "Mikro: run Sync delay ");
                                handler.postDelayed(readCode,DELAY_FTM);

                            }
                            else
                            {
                                Log.d(TAG, "Fail to sync");
                                reset_ftm();
                                handler.postDelayed(syncCode,DELAY_FTM);
                            }


                        }
                        else
                        {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    readbtn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                    //readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                                }
                            });
                            readRun = false;
                            reset_ftm();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(overcurrent.this, "FTM OFF", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (STException e) {
                        Log.d(TAG, "mikro: Error on sync");
                        e.printStackTrace();
                    }
                    break;
                case READ:
                    try {
                        if(((ST25DVTag)mftmTag).isMailboxEnabled(false))
                        {

                            //if mailbox available
                            if(((ST25DVTag)mftmTag).hasHostPutMsg(true))
                            {
                                mFTMmsg = readMessage();
                                Log.d(TAG, "Mikro: Read :" +mFTMmsg);
                                //updateFtmMessage(mFTMmsg);
                                if(writeRun)
                                {
                                    write_result=updateWriteResponse(mFTMmsg);
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            if (!write_result) {
                                                writebtn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                            } else {
                                                writebtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                                            }
                                        }
                                    });
                                    writeRun = false;
                                }

                                if(readRun)
                                {
                                    String joined=updateFtmMessage(mFTMmsg);
                                    Log.d(TAG, "Mikro: Received "+joined);
                                    result = crccheck.crcChecker16(joined);
                                    Log.d(TAG, "Mikro: Result CRC "+result);
                                    if (!result){
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                //  Toast.makeText(MainActivity.this, "FTM is OFF", Toast.LENGTH_SHORT).show();
                                                //Log.d(TAG, "Mikro: sync FTM is off");
                                                new AlertDialog.Builder(overcurrent.this)
                                                        .setTitle(R.string.app_name)
                                                        .setIcon(R.mipmap.ic_launcher)
                                                        .setMessage("\nReading Error, please check devide and try again\n")
                                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                                Log.d(TAG, "Mikro: CRC Error " );
                                                            }
                                                        })
                                                        .show();
                                            }
                                        });
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                                            }
                                        });

                                    } else {
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                                            }
                                        });
                                        //handler.postDelayed(syncCode,DELAY_FTM);  //wait another 100ms to send code
                                        readRun = false;
                                    }
                                }
                            }
                            else
                            {
                                Log.d(TAG, "Mikro: Read delay ");
                                reset_ftm();
                                //if not yet get any message, then wait another 100ms to read
                                handler.postDelayed(readCode,DELAY_FTM);
                            }
                        }
                        else
                        {
                            if(readRun)
                            {
                                readRun = false;
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        readbtn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                        //syncbtn.setBackgroundColor(getResources().getColor(R.color.colorGray));
                                        Toast.makeText(overcurrent.this, "FTM OFF", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            if(writeRun)
                            {
                                writeRun = false;
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        writebtn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                        //syncbtn.setBackgroundColor(getResources().getColor(R.color.colorGray));
                                        Toast.makeText(overcurrent.this, "FTM OFF", Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }
                            reset_ftm();
                        }
                    } catch (STException e) {
                        Log.d(TAG, "mikro: Error on read");
                        e.printStackTrace();
                        checkError(e);
                        handler.postDelayed(readCode,DELAY_FTM);

                    }
                    break;
                case RESET:
                    reset_ftm();
                    break;
            }
        }
    }

    private byte[] readMessage() throws STException {
        int length = ((ST25DVTag)mftmTag).readMailboxMessageLength();
        byte[] buffer;

        mBuffer = new byte[255];
        //write 8
        Log.d(TAG, "Mikro: Read msg ");
        if (length > 0)
            buffer = new byte[length];
        else
            throw new STException(CMD_FAILED);

        byte[] tmpBuffer;
        int offset = 0;

        int size = length;

        if (size <= 0)
            throw new STException(CMD_FAILED);


        //by using below method cannot read more than 253 bytes. If more than that, tag field error pop up
        //size = 254;
        //tmpBuffer = mST25DVTag.readMailboxMessage(offset, size);
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        while (offset < length) {
            size = ((length - offset) > mMaxPayloadSizeRx)
                    ? mMaxPayloadSizeRx
                    : length - offset;
            tmpBuffer = ((ST25DVTag)mftmTag).readMailboxMessage(offset, size);
            if (tmpBuffer.length < (size + 1) || tmpBuffer[0] != 0)
                throw new STException(CMD_FAILED);
            System.arraycopy(tmpBuffer, 1, buffer, offset,
                    tmpBuffer.length - 1);
            offset += tmpBuffer.length - 1;
            Log.d(TAG, "Mikro: Read offset "+offset);
        }

        return buffer;

    }


    private int sendSimpleData() {
        try {

            if (((ST25DVTag)mftmTag).hasRFPutMsg(true)) {
                Log.d(TAG, "Mikro:RF Mailbox Not Empty");
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(overcurrent.this, "RF Mailbox Not Empty", Toast.LENGTH_SHORT).show();
                    }
                });
                reset_ftm();
                handler.postDelayed(readCode,DELAY_FTM);  //try to read the message first
                return TRY_AGAIN;
            }
        } catch (final STException e) {
            Log.d(TAG, "Error on sendSimpleData");
            return checkError(e);
        }

        byte response;

        try {
            //write 6
            Log.d(TAG, "Mikro: sending simple data");
            response = ((ST25DVTag)mftmTag).writeMailboxMessage(mBuffer.length, mBuffer);

            if(response != 0x00){

                return ERROR;
            }
        } catch (final STException e) {
            Log.d(TAG, "Error on writeMailboxMessage");
            return checkError(e);
        }
        Log.d(TAG, "Mikro: end of loop");
        return OK;

    }

    private void reset_ftm(){
        Log.d(TAG, "Mikro: reseting FTM");
        if (mftmTag == null) {
            return;
        }

        try {
            ((ST25DVTag)mftmTag).disableMailbox();
            Log.d(TAG, "Mikro: Disable FTM");
        } catch (STException e) {
            Log.d(TAG, "Mikro: Fail disable FTM");
            switch (e.getError()) {
                case TAG_NOT_IN_THE_FIELD:
                    break;
                case CONFIG_PASSWORD_NEEDED:
                    break;
                //to do error message;
            }
        }

        //put a 10ms delay to wait, and then only enable ftm
        sleep(100);

        try {
            ((ST25DVTag)mftmTag).enableMailbox();
            Log.d(TAG, "Mikro: Enable FTM");
        } catch (STException e) {
            Log.d(TAG, "Mikro: Fail enable FTM");
            switch (e.getError()) {
                case TAG_NOT_IN_THE_FIELD:
                    break;
                case CONFIG_PASSWORD_NEEDED:
                    break;
                //to do error message;
            }
        }

        Log.d(TAG, "Mikro: finish reset");
    }

    private int checkError(STException e) {
        STException.STExceptionCode errorCode = e.getError();
        Log.d(TAG, "Error on checkError" + errorCode);
        if (errorCode == STException.STExceptionCode.CONNECTION_ERROR
                || errorCode == STException.STExceptionCode.TAG_NOT_IN_THE_FIELD
                || errorCode == STException.STExceptionCode.CMD_FAILED
                || errorCode == STException.STExceptionCode.CRC_ERROR) {
            STLog.i("Last cmd failed with error code " +  errorCode + ": Try again the same cmd");
            return TRY_AGAIN;
        } else {
            // Transfer failed
            STLog.e(e.getMessage());
            return ERROR;
        }
    }

    public Boolean updateWriteResponse (byte[] message){

        int no_message = message.length;
        String[] putftmpara = new String[message.length];
        String[] putftmpara1 = new String[message.length];
        byte tempMsg = 0;
        boolean status=false;
        String joined = null,append = null;

        String msg = "";
        //ftmMsgText = (TextView) findViewById(R.id.msgTextView);
        Log.d(TAG, "Mikro: update ftm msg " + no_message);
        for(int x = 0; x <no_message; x++){

            tempMsg = message[x];
            int hex_int= (int)tempMsg & 0xff;
            String hex_value = Integer.toHexString(hex_int);
            msg = msg + hex_value + ' ';
            putftmpara[x]=hex_value;

        }
        Log.d(TAG, "Mikro FTM Msg: " + msg);
        //to pass 2 bytes hex number to crc calculator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String[] a = new String[putftmpara.length];
            for (int y = 0; y < putftmpara.length; y++) {
                int value=Integer.parseInt(putftmpara[y],16);
                if (value<=15){
                    append= "0"+putftmpara[y];
                }else{
                    append=putftmpara[y];
                }
                a[y]=append;
                // Log.d(TAG, "Mikro: value: "+ a[y]);
            }
            joined = String.join("", a);
        }
        Log.d(TAG, "Mikro: Read joined "+joined);

        for(int i = 1; i < message.length; i+=1) {
            int value = Integer.parseInt(putftmpara[i], 16);
            if (value <= 15) {
                append = "0" + putftmpara[i];
            } else {
                append = putftmpara[i];
            }
            putftmpara1[i] = append;

            String element = putftmpara1[i];
            Log.d(TAG, "Mikro putftmpara:   " + putftmpara[i]);
            switch (i){
                case 1:
                    Log.d(TAG, "Mikro FTM response:   " + element);
                    if (element.equals("90")){
                        status=false;
                        Log.d(TAG, "Mikro FTM response: Error  ");
                    } else {
                        status=true;
                        Log.d(TAG, "Mikro FTM response: Write success ");
                    }
                    break;
            }
        }
        return status;
    }

    public String updateFtmMessage (byte[] message){
        int no_message = message.length;
        String[] putftmpara = new String[message.length];
        String[] putftmpara1 = new String[message.length];
        byte tempMsg = 0;
        String msg= "";
        String joined = null,append = null;

        // String msg = "FTM Message: Total byte is " + no_message +"\n";
        Log.d(TAG, "Mikro: update ftm msg " + no_message);
        for(int x = 0; x <no_message; x++){

            tempMsg = message[x];
            // Log.d(TAG, "Mikro FTM Message: tempMsg  " +x +" "+tempMsg);
            int hex_int= (int)tempMsg & 0xff;
            String hex_value = Integer.toHexString(hex_int);
            // Log.d(TAG, "Mikro FTM Message: hex_value  " +x +" "+ hex_value);
            msg = msg + hex_value;
            putftmpara[x]=hex_value;
        }

        //to pass 2 bytes hex number to crc calculator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String[] a = new String[putftmpara.length];
            for (int y = 0; y < putftmpara.length; y++) {
                int value=Integer.parseInt(putftmpara[y],16);
                if (value<=15){
                    append= "0"+putftmpara[y];
                }else{
                    append=putftmpara[y];
                }
                a[y]=append;
                // Log.d(TAG, "Mikro: value: "+ a[y]);
            }
            joined = String.join("", a);
        }

        final String finalMsg = msg;
        Log.d(TAG, "Mikro FTM finalMsg: " + finalMsg);

        int I_set=0, I_delay=0,TI_set=0,KTI_set=0, I2_set=0, I2_opt=0,TI2_set=0,I3_opt=0,I3_set=0,TI3_set=0;
        double I_set_double=0, TI_set_double=0,KTI_set_double=0,I2_set_double=0, TI2_set_double=0,I3_set_double=0,TI3_set_double=0;
        String I3String="",I2String="",TI2String="",TIString="",IString="", KTIString="",TI3String="";
        for(int i = 1; i < message.length; i+=2) {
            int value = Integer.parseInt(putftmpara[i], 16);
            if (value <= 15) {
                append = "0" + putftmpara[i];
            } else {
                append = putftmpara[i];
            }
            putftmpara1[i] = append;

            int value1 = Integer.parseInt(putftmpara[i + 1], 16);
            if (value1 <= 15) {
                append = "0" + putftmpara[i + 1];
            } else {
                append = putftmpara[i + 1];
            }
            putftmpara1[i + 1] = append;

            String element = putftmpara1[i] + putftmpara1[i + 1];

           // DecimalFormat df2 = new DecimalFormat("#.##");
            Log.d(TAG, "Mikro putftmpara:   " + putftmpara1[i] + " " + putftmpara1[i + 1]);

            switch (i) {
                case 3:
                    I_set= Integer.parseInt(element, 16);
                    I_set_double=I_set/100.00;
                    IString=df2.format(I_set_double);
                    Log.d(TAG, "Mikro I> converted:   " + IString);
                    break;
                case 5:
                    I_delay=Integer.parseInt(element, 16);
                    Log.d(TAG, "Mikro FTM I_delay:   " + I_delay);
                    break;
                case 7:
                    TI_set=Integer.parseInt(element, 16);
                    TI_set_double=TI_set/100.00;
                    TIString=df2.format(TI_set_double);
                    Log.d(TAG, "Mikro FTM TI_set:   " + TIString);
                    break;
                case 9:
                    KTI_set=Integer.parseInt(element, 16);
                    KTI_set_double=KTI_set/100.00;
                    KTIString=df2.format(KTI_set_double);
                    Log.d(TAG, "Mikro FTM KTI_set:   " + KTIString);
                    break;
                case 11:
                    double I2_set_d=Integer.parseInt(element,16);
                    Log.d(TAG, "Mikro FTM I2_set:   " + I2_set_d);
                    if (I2_set_d>0){
                        I2_opt =1;
                        I2_set_double=I2_set_d/100.00;
                        I2String= df2.format(I2_set_double);
                    } else {
                        I2_opt=0;
                    }
                    break;
                case 13:
                    TI2_set=Integer.parseInt(element, 16);
                    TI2_set_double=TI2_set/100.00;
                    TI2String=df2.format(TI2_set_double);
                    Log.d(TAG, "Mikro FTM TI2_set:   " + TI2String);
                    break;
                case 15:
                    I3_set=Integer.parseInt(element,16);
                    if (I3_set>0){
                        I3_opt =1;
                        I3_set_double=I3_set/100.00;
                        Log.d(TAG, "Mikro FTM I3_set:   " + I3_set);
                        I3String= df2.format(I3_set_double);
                    } else {
                        I3_opt=0;
                    }
                    break;
                case 17:
                    TI3_set=Integer.parseInt(element, 16);
                    TI3_set_double=TI3_set/100.00;
                    TI3String=df2.format(TI3_set_double);
                    Log.d(TAG, "Mikro FTM TI3_set:   " + TI3String);
                    break;
            }
        }

        final int  finalIdelay=I_delay,finalI2op=I2_opt,finalI3op=I3_opt;
        final String  finalTI2=TI2String,finalI3=I3String, finalTI3=TI3String,finalI2=I2String,finalTI=TIString,finalKTI=KTIString,finalI=IString;
        runOnUiThread(new Runnable() {
            public void run() {
                editI.setText(finalI);

                if (finalIdelay==0){
                    spinner1.setSelection(0);
                } else if (finalIdelay==1){
                    spinner1.setSelection(1);
                }else if (finalIdelay==2){
                    spinner1.setSelection(2);
                }else if (finalIdelay==3){
                    spinner1.setSelection(3);
                }else if (finalIdelay==4){
                    spinner1.setSelection(4);
                }else if (finalIdelay==5){
                    spinner1.setSelection(5);
                }

                editTI.setText(finalTI);
                editktI.setText(finalKTI);

                if (finalI2op==0){
                    spinner2.setSelection(0);
                    editI2.setText("0");
                } else if (finalI2op==1){
                    spinner2.setSelection(1);
                    editI2.setText(finalI2);
                }

                editTI2.setText(finalTI2);

                if (finalI3op==0){
                    spinner3.setSelection(0);
                    editI3.setText("0");
                } else if (finalI3op==1){
                    spinner3.setSelection(1);
                    editI3.setText(finalI3);
                }

                editTI3.setText(finalTI3);
            }
        });
        return joined;
    }

    public static String[] hexStringToStringArray(String s) {
        int len = s.length();
        //Log.d(TAG,"Mikro hex length:"+len);
        if (len<4){
            switch(len) {
                case 3:
                    s = "0" + s;
                    break;
                case 2:
                    s="00"+s;
                    break;
                case 1:
                    s="000"+s;
                    break;
            }
        }

        List<String> parts = new ArrayList<>();
        for (int i = 0; i < 4; i += 2) {
            parts.add(s.substring(i, Math.min(4, i + 2)));
        }
       // Log.d(TAG,"Mikro  hex: "+parts.toArray(new String[0]));
        return parts.toArray(new String[0]);
    }


}
