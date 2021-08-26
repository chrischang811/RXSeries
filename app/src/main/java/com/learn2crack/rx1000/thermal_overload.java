package com.learn2crack.rx1000;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
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


public class thermal_overload extends BaseActivity implements TagDiscovery.onTagDiscoveryCompletedListener{

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
    private int get_Io,delay_spin_Io,get_tlo,get_ktlo, get_tlo2, get_Io2, delay_spin;
    double get_Io_d, get_tlo_d, get_ktIo_d, get_Io2_d, get_tlo2_d;
    private String Io_result[], tlo_result[],ktlo_result[], Io2_result[],tlo2_result[];
    private String Io_1,Io_0,tlo_1,tlo_0,ktlo_1,ktlo_0,Io2_1,Io2_0,tlo2_1,tlo2_0,crc_low,crc_high;

    static private NFCTag mftmTag;
    static private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private static final String TAG = thermal_overload.class.getSimpleName();
    CRC16checker crccheck=new CRC16checker();
    DecimalFormat df2 = new DecimalFormat("0.00");

    Handler handler = new Handler();
    private Button writebtn, readbtn;
    private Spinner spinner1,spinner2;
    private EditText editIo,editTlo,editKtlo,editIo2,editTlo2;
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
        FrameLayout contentFrameLayout = (FrameLayout) findViewById(R.id.content_frame);
        getLayoutInflater().inflate(R.layout.thermal, contentFrameLayout);
        thermal_overload.this.setTitle("THERMAL OVERLOAD");
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this); //check NFC hardware available
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        spinner1=(Spinner)findViewById(R.id.IƟ_spinner);
        spinner1.setOnItemSelectedListener(listener);
        /*
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

        //define spinner & editText
        editIo = (EditText) findViewById(R.id.edit_Io);

        spinner1=(Spinner)findViewById(R.id.Io_delay_option);
        spinner1.setOnItemSelectedListener(listener);

        editTlo = (EditText) findViewById(R.id.edit_tlo);

        editKtlo=(EditText)findViewById(R.id.edit_ktIo);

        spinner2=(Spinner)findViewById(R.id.Io2_op);
        spinner2.setOnItemSelectedListener(listener);

        editIo2 = (EditText) findViewById(R.id.edit_Io2);

        editTlo2 = (EditText) findViewById(R.id.edit_tIo2);

        writebtn = findViewById(R.id.btn_write_ef);
        writebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!writeRun){
                String request = "0110010800060c";

                Log.d(TAG, "Mikro sent: " + request);

                    //edit I> Setting
                    if (TextUtils.isEmpty(editIo.getText().toString())) {
                        editIo.setError("Io> setting cannot be empty");
                        return;
                    } else {
                        get_Io_d = Double.parseDouble((editIo.getText().toString()));
                        Log.d(TAG, "Mikro result Io> setting from apps before process " + get_Io_d);
                        get_Io = StepParam.getstep3(get_Io_d,10,1000);
                        Log.d(TAG, "Mikro result Io> setting from apps after process " + get_Io);
                        if (get_Io==0){
                            if (get_Io_d<=0.1) {
                                editIo.setError("I> setting cannot be less than 0.1");
                            }else if (get_Io_d>10){
                                editIo.setError("I> setting cannot be more than 10");
                            }
                            return;
                        }
                        String I1_display=df2.format(get_Io/100.00);
                        editIo.setText(I1_display);
                        Io_result= hexStringToStringArray(Integer.toHexString(get_Io));
                        Io_1 = Io_result[0];
                        Io_0= Io_result[1];
                        Log.d(TAG, "Mikro result Io in Hex " + Io_1 +Io_0);
                    }
                    request = request + Io_1 + Io_0;
                    Log.d(TAG, "Mikro after Io " + request);

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

                    //edit tIo> Setting
                    if (TextUtils.isEmpty(editTlo.getText().toString())) {
                        editTlo.setError("tIo> setting cannot be empty");
                        return;
                    } else {
                        get_tlo_d = Double.parseDouble((editTlo.getText().toString()));
                        Log.d(TAG, "Mikro result tIo> setting from apps before process " + get_tlo_d);
                        get_tlo = StepParam.getstep1(get_tlo_d,3,10000);
                        Log.d(TAG, "Mikro result tIo> setting from apps after process " + get_tlo);
                        if (get_tlo==0){
                            if (get_tlo_d<0.03) {
                                editTlo.setError("tIo> setting cannot be less than 0.03");
                            }else if (get_tlo_d>100){
                                editTlo.setError("tIo> setting cannot be more than 100");
                            }
                            return;
                        }
                        String Tlo_display=df2.format(get_tlo/100.00);
                        editTlo.setText(Tlo_display);
                        tlo_result= hexStringToStringArray(Integer.toHexString(get_tlo));
                        tlo_1 = tlo_result[0];
                        tlo_0= tlo_result[1];
                        Log.d(TAG, "Mikro result tIo> in Hex " + tlo_1 +tlo_0);
                    }
                    request = request + tlo_1 + tlo_0;
                    Log.d(TAG, "Mikro after tIo> " + request);

                    //edit ktIo> Setting
                    if (TextUtils.isEmpty(editKtlo.getText().toString())) {
                        editKtlo.setError("ktIo> setting cannot be empty");
                        return;
                    } else {
                        get_ktIo_d = Double.parseDouble((editKtlo.getText().toString()));
                        Log.d(TAG, "Mikro result ktIo> setting from apps before process " + get_ktIo_d);
                        get_ktlo = StepParam.getstep0(get_ktIo_d,1,100);
                        Log.d(TAG, "Mikro result ktIo> setting from apps after process " + get_ktlo);
                        if (get_ktlo==0){
                            if (get_ktIo_d<0.01) {
                                editKtlo.setError("ktIo> setting cannot be less than 0.01");
                            }else if (get_ktIo_d>1){
                                editKtlo.setError("ktIo> setting cannot be more than 1");
                            }
                            return;
                        }
                        ktlo_result= hexStringToStringArray(Integer.toHexString(get_ktlo));
                        ktlo_1 = ktlo_result[0];
                        ktlo_0= ktlo_result[1];
                        Log.d(TAG, "Mikro result ktIo> in Hex " + ktlo_1 +ktlo_0);
                    }
                    request = request + ktlo_1 + ktlo_0;
                    Log.d(TAG, "Mikro after ktIo> " + request);

                    //edit Io>> Setting
                    String spin2 = String.valueOf(spinner2.getSelectedItem());
                    if (spin2.equals("OFF")){
                        editIo2.setText("0");
                        Io2_1 = "00";
                        Io2_0= "00";
                    } else {
                        if (TextUtils.isEmpty(editIo2.getText().toString())) {
                            editIo2.setError("Io>> setting cannot be empty");
                            return;
                        } else {
                            get_Io2_d = Double.parseDouble((editIo2.getText().toString()));
                            Log.d(TAG, "Mikro result Io2 setting from apps before process " + get_Io2_d);

                            get_Io2 = StepParam.getstep2(get_Io2_d, 10, 10000);
                            Log.d(TAG, "Mikro result Io2 setting from apps after process " + get_Io2);
                            if (get_Io2 == 0) {
                                if (get_Io2_d < 0.1) {
                                    editIo2.setError("Io>> setting cannot be less than 0.5");
                                } else if (get_Io2_d > 100) {
                                    editIo2.setError("Io>> setting cannot be more than 100");
                                }
                                return;
                            }
                        }
                        String I2_display=df2.format(get_Io2/100.00);
                        editIo2.setText(I2_display);
                        Io2_result= hexStringToStringArray(Integer.toHexString(get_Io2));
                        Io2_1 = Io2_result[0];
                        Io2_0= Io2_result[1];
                        Log.d(TAG, "Mikro result Io2 in Hex " + Io2_1 +Io2_0);
                    }
                    request = request + Io2_1 + Io2_0;
                    Log.d(TAG, "Mikro after Io2 " + request);

                    //edit tIo>> Setting
                    if (TextUtils.isEmpty(editTlo2.getText().toString())) {
                        editTlo2.setError("tIo>> setting cannot be empty");
                        return;
                    } else {
                        get_tlo2_d = Double.parseDouble((editTlo2.getText().toString()));
                        Log.d(TAG, "Mikro result tIo>> setting from apps before process " + get_tlo2_d);
                        get_tlo2 = StepParam.getstep3(get_tlo2_d,3,10000);
                        Log.d(TAG, "Mikro result tIo>> setting from apps after process " + get_tlo2);
                        if (get_tlo2==0){
                            if (get_tlo2_d<0.03) {
                                editTlo2.setError("tIo>> setting cannot be less than 0.03");
                            }else if (get_tlo2_d>100){
                                editTlo2.setError("tIo>> setting cannot be more than 100");
                            }
                            return;
                        }
                        String TI2_display=df2.format(get_tlo2/100.00);
                        editTlo2.setText(TI2_display);
                        tlo2_result= hexStringToStringArray(Integer.toHexString(get_tlo2));
                        tlo2_1 = tlo2_result[0];
                        tlo2_0= tlo2_result[1];
                        Log.d(TAG, "Mikro result tIo>> in Hex " + tlo2_1 +tlo2_0);
                    }
                    request = request + tlo2_1 + tlo2_0;
                    Log.d(TAG, "Mikro after tIo>> " + request);

                //obtain CRC
                int[] ans = crccheck.getCRC(request);
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
*/
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

        Log.d(TAG, "Mikro: processIntent " + intent);

        Tag androidTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (androidTag != null) {
            // A tag has been taped
            Toast.makeText(this, "NFC detected on FTM!", Toast.LENGTH_SHORT).show();
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
        mBuffer = new byte[21];
        mBuffer [0] = (byte)0x01;
        mBuffer [1] = (byte)0x10;
        mBuffer [2] = (byte)0x01;
        mBuffer [3] = (byte)0x08;
        mBuffer [4] = (byte)0x00;
        mBuffer [5] = (byte)0x06;
        mBuffer [6] = (byte)0x0c;
        mBuffer [7] = (byte)Integer.parseInt(Io_1,16);
        mBuffer [8] = (byte)Integer.parseInt(Io_0,16);
        mBuffer [9] = (byte)0x00;
        mBuffer [10] = (byte)delay_spin;
        mBuffer [11] = (byte)Integer.parseInt(tlo_1,16);
        mBuffer [12] = (byte)Integer.parseInt(tlo_0,16);;
        mBuffer [13] = (byte)Integer.parseInt(ktlo_1,16);
        mBuffer [14] = (byte)Integer.parseInt(ktlo_0,16);
        mBuffer [15] = (byte)Integer.parseInt(Io2_1,16);
        mBuffer [16] = (byte)Integer.parseInt(Io2_0,16);
        mBuffer [17] = (byte)Integer.parseInt(tlo2_1,16);
        mBuffer [18] = (byte)Integer.parseInt(tlo2_0,16);
        mBuffer [19] = (byte)Integer.parseInt(crc_high,16);
        mBuffer [20] = (byte)Integer.parseInt(crc_low,16);

        Log.d(TAG, "Mikro: delay type " + mBuffer[10]);
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
        mBuffer [3] = (byte)0x08;
        mBuffer [4] = (byte)0x00;
        mBuffer [5] = (byte)0x06;
        //0xF645 when address start from 0108
      //  mBuffer [6] = (byte)0x45;
       // mBuffer [7] = (byte)0xf6;
        //0xB245 when address start from 0208
        mBuffer [6] = (byte)0x45;
        mBuffer [7] = (byte)0xb2;

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
                        }
                        else{

                            writeRun=false;
                            reset_ftm();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    writebtn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                    //writebtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                                    Toast.makeText(thermal_overload.this, "FTM OFF", Toast.LENGTH_SHORT).show();
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
                                    Toast.makeText(thermal_overload.this, "FTM OFF", Toast.LENGTH_SHORT).show();
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
                                                new AlertDialog.Builder(thermal_overload.this)
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
                                        Toast.makeText(thermal_overload.this, "FTM OFF", Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(thermal_overload.this, "FTM OFF", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(thermal_overload.this, "RF Mailbox Not Empty", Toast.LENGTH_SHORT).show();
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

        int Io_set=0, Io_delay=0,tlo_set=0,ktlo_set=0, Io2_opt=0, Io2_set=0, tlo2_set=0;
        double Io_set_double=0, tlo_set_double=0,ktlo_set_double=0,Io2_set_double=0, tlo2_set_double=0;
        String IoString="",tloString="",ktloString="",Io2String="",tlo2String="";
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

            Log.d(TAG, "Mikro putftmpara:   " + putftmpara1[i] + " " + putftmpara1[i + 1]);

            switch (i) {
                case 3:
                    Io_set= Integer.parseInt(element, 16);
                    Io_set_double=Io_set/100.00;
                    IoString=df2.format(Io_set_double);
                    Log.d(TAG, "Mikro Io> converted: :   " + IoString);
                    break;
                case 5:
                    Io_delay=Integer.parseInt(element, 16);
                    Log.d(TAG, "Mikro Io_delay:   " + Io_delay);
                    break;
                case 7:
                    tlo_set=Integer.parseInt(element, 16);
                    tlo_set_double=tlo_set/100.00;
                    tloString=df2.format(tlo_set_double);
                    Log.d(TAG, "Mikro tlo String:   " + tloString);
                    break;
                case 9:
                    ktlo_set=Integer.parseInt(element, 16);
                    ktlo_set_double=ktlo_set/100.00;
                    ktloString=df2.format(ktlo_set_double);
                    Log.d(TAG, "Mikro ktlo String:   " + ktloString);
                    break;
                case 11:
                    double Io2_set_d=Integer.parseInt(element,16);
                    Log.d(TAG, "Mikro FTM Io2_set:   " + Io2_set_d);
                    if (Io2_set_d>0){
                        Io2_opt =1;
                        Io2_set_double=Io2_set_d/100.00;
                        Io2String= df2.format(Io2_set_double);
                        //Log.d(TAG, "Mikro FTM Io2_set:   " + Io2String);
                    } else {
                        Io2_opt=0;
                    }
                    break;
                case 13:
                    tlo2_set=Integer.parseInt(element, 16);
                    tlo2_set_double=tlo2_set/100.00;
                    tlo2String=df2.format(tlo2_set_double);
                    Log.d(TAG, "Mikro FTM tlo2_set:   " + tlo2String);
                    break;

            }
        }

        final int  finalIodelay=Io_delay,finalIo2op=Io2_opt;
        final String  finalIo=IoString,finaltlo=tloString,finalktlo=ktloString,finalIo2=Io2String,finaltlo2=tlo2String;
        runOnUiThread(new Runnable() {
            public void run() {

                editIo.setText(finalIo);

                if (finalIodelay==0){
                    spinner1.setSelection(0);
                } else if (finalIodelay==1){
                    spinner1.setSelection(1);
                }else if (finalIodelay==2){
                    spinner1.setSelection(2);
                }else if (finalIodelay==3){
                    spinner1.setSelection(3);
                }else if (finalIodelay==4){
                    spinner1.setSelection(4);
                }else if (finalIodelay==5){
                    spinner1.setSelection(5);
                }

                editTlo.setText(finaltlo);
                editKtlo.setText(finalktlo);

                if (finalIo2op==0){
                    spinner2.setSelection(0);
                    editIo2.setText("0");
                } else if (finalIo2op==1){
                    spinner2.setSelection(1);
                    editIo2.setText(finalIo2);
                }

                editTlo2.setText(finaltlo2);

            }
        });
        return joined;
    }



    public static String[] hexStringToStringArray(String s) {
        int len = s.length();
        Log.d(TAG,"Mikro hex length:"+len);
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
      //  Log.d(TAG,"Mikro  hex: "+parts.toArray(new String[0]));
        return parts.toArray(new String[0]);
    }


}
