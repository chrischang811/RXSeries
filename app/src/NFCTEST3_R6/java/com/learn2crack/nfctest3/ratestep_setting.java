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
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.STLog;
import com.st.st25sdk.TagHelper;
import com.st.st25sdk.type5.st25dv.ST25DVTag;

import java.util.ArrayList;
import java.util.List;

import static android.os.SystemClock.sleep;
import static com.st.st25sdk.STException.STExceptionCode.CMD_FAILED;

public class ratestep_setting extends BaseActivity implements TagDiscovery.onTagDiscoveryCompletedListener{

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
    private int steprate1,steprate2,steprate3,steprate4,steprate5,steprate6,steprate7,steprate8,steprate9,steprate10;
    private int steprate11,steprate12,steprate13,steprate14,steprate15,steprate16, spin15_pos;
    private String crc_low,crc_high,steprate15_high,steprate15_low,steprate16_high,steprate16_low;

    static private NFCTag mftmTag;
    static private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private static final String TAG = ratestep_setting.class.getSimpleName();
    CRC16checker crccheck=new CRC16checker();

    Handler handler = new Handler();
    private Button writebtn, readbtn;
    private Spinner spinner1, spinner2,spinner3,spinner4,spinner5,spinner6,spinner7,spinner8;
    private Spinner spinner9, spinner10,spinner11,spinner12,spinner13,spinner14,spinner15,spinner16;
    private TextView text7,text8,text9,text10,text11,text12,text13,text14;
    private TextView r1,r2, r3, r4,r5,r6,r7,r8,r9,r10,r11,r12,r13,r14,r15,r16;
    private boolean readRun = false;
    private boolean writeRun = false;
    final List<String> list= new ArrayList<>();


    private boolean result = false;
    private boolean write_result = false;
    private boolean addArray = false;
    private boolean readresult = false;
    private boolean ratiostep=false;
    private Runnable readCode;
    private Runnable syncCode;
    private Runnable runCode;

    private AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
            ((TextView) parent.getChildAt(0)).setTextSize(12);
            ((TextView) parent.getChildAt(0)).setTypeface(Typeface.DEFAULT_BOLD);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
            ((TextView) parent.getChildAt(0)).setTextSize(12);
            ((TextView) parent.getChildAt(0)).setTypeface(Typeface.DEFAULT_BOLD);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.ratestep_setting);
        FrameLayout contentFrameLayout = (FrameLayout) findViewById(R.id.content_frame);
        getLayoutInflater().inflate(R.layout.ratestep_setting, contentFrameLayout);
        Intent intent = getIntent();
        String bits=intent.getStringExtra("device_bits");
        //bits="8";
        Log.d("Mikro ", "devices type: " +  bits);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this); //check NFC hardware available
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        readbtn = findViewById(R.id.btn_read_ratestep);
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

        text7=(TextView)findViewById(R.id.rate7_text);
        text8=(TextView)findViewById(R.id.rate8_text);
        text9=(TextView)findViewById(R.id.rate9_text);
        text10=(TextView)findViewById(R.id.rate10_text);
        text11=(TextView)findViewById(R.id.rate11_text);
        text12=(TextView)findViewById(R.id.rate12_text);
        text13=(TextView)findViewById(R.id.rate13_text);
        text14=(TextView)findViewById(R.id.rate14_text);

        r1=(TextView)findViewById(R.id.ratiostep1);
        r2=(TextView)findViewById(R.id.ratiostep2);
        r3=(TextView)findViewById(R.id.ratiostep3);
        r4=(TextView)findViewById(R.id.ratiostep4);
        r5=(TextView)findViewById(R.id.ratiostep5);
        r6=(TextView)findViewById(R.id.ratiostep6);
        r7=(TextView)findViewById(R.id.ratiostep7);
        r8=(TextView)findViewById(R.id.ratiostep8);
        r9=(TextView)findViewById(R.id.ratiostep9);
        r10=(TextView)findViewById(R.id.ratiostep10);
        r11=(TextView)findViewById(R.id.ratiostep11);
        r12=(TextView)findViewById(R.id.ratiostep12);
        r13=(TextView)findViewById(R.id.ratiostep13);
        r14=(TextView)findViewById(R.id.ratiostep14);
        r15=(TextView)findViewById(R.id.ratiostep15);
        r16=(TextView)findViewById(R.id.ratiostep16);

        spinner1 = (Spinner) findViewById(R.id.rate1_option);
        spinner1.setOnItemSelectedListener(listener);
        spinner1.setSelection(1);
        spinner1.setEnabled(false);
        spinner1.setBackgroundColor(Color.DKGRAY);

        spinner2 = (Spinner) findViewById(R.id.rate2_option);
        spinner2.setOnItemSelectedListener(listener);

        spinner3 = (Spinner) findViewById(R.id.rate3_option);
        spinner3.setOnItemSelectedListener(listener);

        spinner4 = (Spinner) findViewById(R.id.rate4_option);
        spinner4.setOnItemSelectedListener(listener);

        spinner5 = (Spinner) findViewById(R.id.rate5_option);
        spinner5.setOnItemSelectedListener(listener);

        spinner6 = (Spinner) findViewById(R.id.rate6_option);
        spinner6.setOnItemSelectedListener(listener);

        spinner7 = (Spinner) findViewById(R.id.rate7_option);
        spinner7.setOnItemSelectedListener(listener);

        spinner8 = (Spinner) findViewById(R.id.rate8_option);
        spinner8.setOnItemSelectedListener(listener);

        spinner9 = (Spinner) findViewById(R.id.rate9_option);
        spinner9.setOnItemSelectedListener(listener);

        spinner10 = (Spinner) findViewById(R.id.rate10_option);
        spinner10.setOnItemSelectedListener(listener);

        spinner11 = (Spinner) findViewById(R.id.rate11_option);
        spinner11.setOnItemSelectedListener(listener);

        spinner12 = (Spinner) findViewById(R.id.rate12_option);
        spinner12.setOnItemSelectedListener(listener);

        spinner13 = (Spinner) findViewById(R.id.rate13_option);
        spinner13.setOnItemSelectedListener(listener);

        spinner14 = (Spinner) findViewById(R.id.rate14_option);
        spinner14.setOnItemSelectedListener(listener);

        spinner15 = (Spinner) findViewById(R.id.rate15_option);
        spinner15.setSelection(spin15_pos);
        Log.d(TAG, "Mikro spinner15: " + spin15_pos);

        //final List<String> list= new ArrayList<>();
        list.add("Disable");
        list.add("1 step");
        list.add("2 steps");
        list.add("3 steps");
        list.add("4 steps");
        list.add("6 steps");
        list.add("8 steps");
        list.add("12 steps");
        list.add("16 steps");
        final ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, R.layout.spinner,list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner15.setAdapter(dataAdapter);

        spinner15.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
                ((TextView) parent.getChildAt(0)).setTextSize(12);
                ((TextView) parent.getChildAt(0)).setTypeface(Typeface.DEFAULT_BOLD);
                spin15_pos=spinner15.getSelectedItemPosition();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                return;
            }
        });



        spinner16 = (Spinner) findViewById(R.id.rate16_option);
        spinner16.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
                ((TextView) parent.getChildAt(0)).setTextSize(12);
                ((TextView) parent.getChildAt(0)).setTypeface(Typeface.DEFAULT_BOLD);
                String spin16 = String.valueOf(spinner16.getSelectedItem());
                Log.d(TAG, "Mikro spinner16: " + spin16);
                if (spin16.equals("Alarm Output")&& !addArray){
                    if (readresult) {
                        list.remove("Fan Output");
                        Log.d(TAG, "Mikro FTM remove Fan Output");
                    }
                    list.add("Fan Output");
                    dataAdapter.notifyDataSetChanged();
                    addArray=true;
                } else {
                    Log.d(TAG, "Mikro spinner 16 beginning: not alarm");
                    if(addArray){
                        Log.d(TAG, "Mikro addArray remove Fan Output ");
                        list.remove("Fan Output");
                        spinner15.setSelection(0);
                        addArray=false;
                    }
                   /* if(readresult) {
                        Log.d(TAG, "Mikro readresult remove Fan Output ");
                        list.remove("Fan Output");
                        readresult = false;
                    }*/
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                return;
            }
        });

        if(bits.equals("8")){
            spinner7.setSelection(0);
            spinner7.setEnabled(false);
            spinner7.setBackgroundColor(Color.DKGRAY);
            text7.setBackgroundColor(Color.DKGRAY);
            r7.setBackgroundColor(Color.DKGRAY);

            spinner8.setSelection(0);
            spinner8.setEnabled(false);
            spinner8.setBackgroundColor(Color.DKGRAY);
            text8.setBackgroundColor(Color.DKGRAY);
            r8.setBackgroundColor(Color.DKGRAY);

            spinner9.setSelection(0);
            spinner9.setEnabled(false);
            spinner9.setBackgroundColor(Color.DKGRAY);
            text9.setBackgroundColor(Color.DKGRAY);
            r9.setBackgroundColor(Color.DKGRAY);

            spinner10.setSelection(0);
            spinner10.setEnabled(false);
            spinner10.setBackgroundColor(Color.DKGRAY);
            text10.setBackgroundColor(Color.DKGRAY);
            r10.setBackgroundColor(Color.DKGRAY);

            spinner11.setSelection(0);
            spinner11.setEnabled(false);
            spinner11.setBackgroundColor(Color.DKGRAY);
            text11.setBackgroundColor(Color.DKGRAY);
            r11.setBackgroundColor(Color.DKGRAY);

            spinner12.setSelection(0);
            spinner12.setEnabled(false);
            spinner12.setBackgroundColor(Color.DKGRAY);
            text12.setBackgroundColor(Color.DKGRAY);
            r12.setBackgroundColor(Color.DKGRAY);

            spinner13.setSelection(0);
            spinner13.setEnabled(false);
            spinner13.setBackgroundColor(Color.DKGRAY);
            text13.setBackgroundColor(Color.DKGRAY);
            r13.setBackgroundColor(Color.DKGRAY);

            spinner14.setSelection(0);
            spinner14.setEnabled(false);
            spinner14.setBackgroundColor(Color.DKGRAY);
            text14.setBackgroundColor(Color.DKGRAY);
            r14.setBackgroundColor(Color.DKGRAY);
        }

        if(bits.equals("12")){
            spinner11.setSelection(0);
            spinner11.setEnabled(false);
            spinner11.setBackgroundColor(Color.DKGRAY);
            text11.setBackgroundColor(Color.DKGRAY);
            r11.setBackgroundColor(Color.DKGRAY);

            spinner12.setSelection(0);
            spinner12.setEnabled(false);
            spinner12.setBackgroundColor(Color.DKGRAY);
            text12.setBackgroundColor(Color.DKGRAY);
            r12.setBackgroundColor(Color.DKGRAY);

            spinner13.setSelection(0);
            spinner13.setEnabled(false);
            spinner13.setBackgroundColor(Color.DKGRAY);
            text13.setBackgroundColor(Color.DKGRAY);
            r13.setBackgroundColor(Color.DKGRAY);

            spinner14.setSelection(0);
            spinner14.setEnabled(false);
            spinner14.setBackgroundColor(Color.DKGRAY);
            text14.setBackgroundColor(Color.DKGRAY);
            r14.setBackgroundColor(Color.DKGRAY);
        }

        writebtn = findViewById(R.id.btn_save_ratestep);
        writebtn.setOnClickListener(new View.OnClickListener() {
            public String getstep(String value){
                String spin =" ";
                if (value.equals("Disable")) {
                    spin = "0000";
                } else if (value.equals("1 step")) {
                    spin = "0001";
                } else if (value.equals("2 steps")) {
                    spin = "0002";
                } else if (value.equals("3 steps")) {
                    spin = "0003";
                } else if (value.equals("4 steps")) {
                    spin = "0004";
                }else if (value.equals("6 steps")) {
                    spin = "0005";
                }else if (value.equals("8 steps")) {
                    spin = "0007";
                }else if (value.equals("12 steps")) {
                    spin = "0008";
                }else if (value.equals("16 steps")) {
                    spin = "0009";
                }else if (value.equals("Alarm Output")) {
                    spin = "00fe";
                } else if (value.equals("Fan Output")) {
                    spin = "00ff";
                }
                //Log.d(TAG, "Mikro: spinner:" + spin);
                return spin;
            }

            @Override
            public void onClick(View v) {
                if (!writeRun) {
                    String request = "0110010a001020";

                    String spin1 = String.valueOf(spinner1.getSelectedItem());
                    String ratespin1=getstep(spin1);
                    steprate1=Integer.parseInt(ratespin1);
                    request = request + ratespin1;

                    String spin2 = String.valueOf(spinner2.getSelectedItem());
                    String ratespin2=getstep(spin2);
                    steprate2=Integer.parseInt(ratespin2);
                    request = request + ratespin2;

                    String spin3 = String.valueOf(spinner3.getSelectedItem());
                    String ratespin3=getstep(spin3);
                    steprate3=Integer.parseInt(ratespin3);
                    request = request + ratespin3;

                    String spin4 = String.valueOf(spinner4.getSelectedItem());
                    String ratespin4=getstep(spin4);
                    steprate4=Integer.parseInt(ratespin4);
                    request = request + ratespin4;

                    String spin5 = String.valueOf(spinner5.getSelectedItem());
                    String ratespin5=getstep(spin5);
                    steprate5=Integer.parseInt(ratespin5);
                    request = request + ratespin5;

                    String spin6 = String.valueOf(spinner6.getSelectedItem());
                    String ratespin6=getstep(spin6);
                    steprate6=Integer.parseInt(ratespin6);
                    request = request + ratespin6;

                    String spin7 = String.valueOf(spinner7.getSelectedItem());
                    String ratespin7=getstep(spin7);
                    steprate7=Integer.parseInt(ratespin7);
                    request = request + ratespin7;

                    String spin8 = String.valueOf(spinner8.getSelectedItem());
                    String ratespin8=getstep(spin8);
                    steprate8=Integer.parseInt(ratespin8);
                    request = request + ratespin8;

                    String spin9 = String.valueOf(spinner9.getSelectedItem());
                    String ratespin9=getstep(spin9);
                    steprate9=Integer.parseInt(ratespin9);
                    request = request + ratespin9;

                    String spin10 = String.valueOf(spinner10.getSelectedItem());
                    String ratespin10=getstep(spin10);
                    steprate10=Integer.parseInt(ratespin10);
                    request = request + ratespin10;

                    String spin11 = String.valueOf(spinner11.getSelectedItem());
                    String ratespin11=getstep(spin11);
                    steprate11=Integer.parseInt(ratespin11);
                    request = request + ratespin11;

                    String spin12 = String.valueOf(spinner12.getSelectedItem());
                    String ratespin12=getstep(spin12);
                    steprate12=Integer.parseInt(ratespin12);
                    request = request + ratespin12;

                    String spin13 = String.valueOf(spinner13.getSelectedItem());
                    String ratespin13=getstep(spin13);
                    steprate13=Integer.parseInt(ratespin13);
                    request = request + ratespin13;

                    String spin14 = String.valueOf(spinner14.getSelectedItem());
                    String ratespin14=getstep(spin14);
                    steprate14=Integer.parseInt(ratespin14);
                    request = request + ratespin14;

                    String spin15 = String.valueOf(spinner15.getSelectedItem());
                    String ratespin15=getstep(spin15);
                    steprate15=Integer.parseInt(ratespin15,16);
                    Log.d(TAG, "Mikro step15:" + ratespin15+" "+steprate15);
                    request = request +ratespin15;

                    String spin16 = String.valueOf(spinner16.getSelectedItem());
                    String ratespin16=getstep(spin16);
                    steprate16=Integer.parseInt(ratespin16,16);
                    Log.d(TAG, "Mikro step16:" + steprate16);
                    request = request +ratespin16;

                    Log.d(TAG, "Mikro sent: " + request);
                    //obtain CRC
                    int[] ans= crccheck.getCRC(request);
                    crc_low = Integer.toHexString(ans[0]);
                    crc_high = Integer.toHexString(ans[1]);
                    Log.d(TAG, "Mikro CRC: " + crc_high + " " + crc_low);

                    writeRun = true;
                    writebtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_green));
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
        mBuffer = new byte[41];
        mBuffer [0] = (byte)0x01;
        mBuffer [1] = (byte)0x10;
        mBuffer [2] = (byte)0x01;
        mBuffer [3] = (byte)0x0a;
        mBuffer [4] = (byte)0x00;
        mBuffer [5] = (byte)0x10;
        mBuffer [6] = (byte)0x20;
        mBuffer [7] = (byte)0x00;
        mBuffer [8] = (byte)steprate1;
        mBuffer [9] = (byte)0x00;
        mBuffer [10] = (byte)steprate2;
        mBuffer [11] = (byte)0x00;
        mBuffer [12] = (byte)steprate3;
        mBuffer [13] = (byte)0x00;
        mBuffer [14] = (byte)steprate4;
        mBuffer [15] = (byte)0x00;
        mBuffer [16] = (byte)steprate5;
        mBuffer [17] = (byte)0x00;
        mBuffer [18] = (byte)steprate6;
        mBuffer [19] = (byte)0x00;
        mBuffer [20] = (byte)steprate7;
        mBuffer [21] = (byte)0x00;
        mBuffer [22] = (byte)steprate8;
        mBuffer [23] = (byte)0x00;
        mBuffer [24] = (byte)steprate9;
        mBuffer [25] = (byte)0x00;
        mBuffer [26] = (byte)steprate10;
        mBuffer [27] = (byte)0x00;
        mBuffer [28] = (byte)steprate11;
        mBuffer [29] = (byte)0x00;
        mBuffer [30] = (byte)steprate12;
        mBuffer [31] = (byte)0x00;
        mBuffer [32] = (byte)steprate13;
        mBuffer [33] = (byte)0x00;
        mBuffer [34] = (byte)steprate14;
        mBuffer [35] = (byte)0x00;
        mBuffer [36] = (byte)steprate15;
        mBuffer [37] = (byte)0x00;
        mBuffer [38] = (byte)steprate16;
        mBuffer [39] = (byte)Integer.parseInt(crc_high,16);
        mBuffer [40] = (byte)Integer.parseInt(crc_low,16);

        //mBuffer [27] = (byte)0x95;
        //mBuffer [28] = (byte)0x26 ;
        //Log.d(TAG, "Mikro: byte ck: " + mBuffer[20]);
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
        mBuffer [2] = (byte)0x01;
        mBuffer [3] = (byte)0x0a;
        mBuffer [4] = (byte)0x00;
        mBuffer [5] = (byte)0x10;
        mBuffer [6] = (byte)0x65;
        mBuffer [7] = (byte)0xf8;

        mAction = ActionCode.SYNC;
        fillView(mAction);
    }

    public void syncFTM1() {

        if (mftmTag == null) {
            return;
        }
        Log.d(TAG, "Mikro ratio setting: " );
        mBuffer = new byte[8];
        mBuffer [0] = (byte)0x01;
        mBuffer [1] = (byte)0x03;
        mBuffer [2] = (byte)0x00;
        mBuffer [3] = (byte)0x31;
        mBuffer [4] = (byte)0x00;
        mBuffer [5] = (byte)0x10;
        mBuffer [6] = (byte)0x15;
        mBuffer [7] = (byte)0xc9;

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
                                    Toast.makeText(ratestep_setting.this, "FTM OFF", Toast.LENGTH_SHORT).show();
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
                                    Toast.makeText(ratestep_setting.this, "FTM OFF", Toast.LENGTH_SHORT).show();
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
                                    String joined;
                                    if (!ratiostep) {
                                        joined = updateFtmMessage(mFTMmsg);
                                    } else{
                                        Log.d(TAG, "Mikro update ratio msg loop");
                                        joined =updateratioMessage(mFTMmsg);
                                    }
                                    Log.d(TAG, "Mikro: Received "+joined);
                                    result = crccheck.crcChecker16(joined);
                                    Log.d(TAG, "Mikro: Result CRC "+result);
                                    if (!result){
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                //  Toast.makeText(MainActivity.this, "FTM is OFF", Toast.LENGTH_SHORT).show();
                                                //Log.d(TAG, "Mikro: sync FTM is off");
                                                new AlertDialog.Builder(ratestep_setting.this)
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
                                        if(ratiostep){
                                            Log.d(TAG, "Mikro:reading ratio steps");
                                            syncFTM1();
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
                                        Toast.makeText(ratestep_setting.this, "FTM OFF", Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(ratestep_setting.this, "FTM OFF", Toast.LENGTH_SHORT).show();
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
        Log.d(TAG, "Mikro: Read msg length: "+length);
        byte[] buffer;

        mBuffer = new byte[255];
        //write 8
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
                        Toast.makeText(ratestep_setting.this, "RF Mailbox Not Empty", Toast.LENGTH_SHORT).show();
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

    public int spin_location(int value){
        int spin =0;
        if (value==0) {
            spin = 0;
        } else if (value==1) {
            spin = 1;
        } else if (value==2) {
            spin = 2;
        } else if (value==3) {
            spin = 3;
        } else if (value==4) {
            spin = 4;
        }else if (value==5) {
            spin = 5;
        }else if (value==7) {
            spin = 6;
        }else if (value==8) {
            spin = 7;
        }else if (value==9) {
            spin = 8;
        }else if (value==254) {
            spin = 9;
        } else if (value==255) {
            spin = 10;
        }
        //Log.d(TAG, "Mikro: spinner:" + spin);
        return spin;
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

        int step1=0, step2=0,step3=0,step4=0, step5=0, step6=0,step7=0,step8=0,step9=0,step10=0,step11=0,step12=0,step13=0,step14=0,step15=0,step16=0;
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

            int step=0;
            switch (i) {
                case 3:
                    step= Integer.parseInt(element, 16);
                    step1=spin_location(step);
                    Log.d(TAG, "Mikro FTM rate step1:   " + step1);
                    break;
                case 5:
                    step= Integer.parseInt(element, 16);
                    step2=spin_location(step);
                    Log.d(TAG, "Mikro FTM rate step2:   " + step2);
                    break;
                case 7:
                    step= Integer.parseInt(element, 16);
                    step3=spin_location(step);
                    break;
                case 9:
                    step= Integer.parseInt(element, 16);
                    step4=spin_location(step);
                    break;
                case 11:
                    step= Integer.parseInt(element, 16);
                    step5=spin_location(step);
                    break;
                case 13:
                    step= Integer.parseInt(element, 16);
                    step6=spin_location(step);
                    break;
                case 15:
                    step= Integer.parseInt(element, 16);
                    step7=spin_location(step);
                    break;
                case 17:
                    step= Integer.parseInt(element, 16);
                    step8=spin_location(step);
                    break;
                case 19:
                    step= Integer.parseInt(element, 16);
                    step9=spin_location(step);
                    break;
                case 21:
                    step= Integer.parseInt(element, 16);
                    step10=spin_location(step);
                    break;
                case 23:
                    step= Integer.parseInt(element, 16);
                    step11=spin_location(step);
                    break;
                case 25:
                    step= Integer.parseInt(element, 16);
                    step12=spin_location(step);
                    break;
                case 27:
                    step= Integer.parseInt(element, 16);
                    step13=spin_location(step);
                    break;
                case 29:
                    step= Integer.parseInt(element, 16);
                    step14=spin_location(step);
                    break;
                case 31:
                    step= Integer.parseInt(element, 16);
                    step15=spin_location(step);
                    Log.d(TAG, "Mikro FTM rate step15:   " + step15);
                    break;
                case 33:
                    step= Integer.parseInt(element, 16);
                    step16=spin_location(step);
                    Log.d(TAG, "Mikro FTM rate step16:   " + step16);
                    break;

            }
        }

        final int finalstep1=step1,finalstep2=step2,finalstep3=step3,finalstep4=step4,finalstep5=step5,finalstep6=step6,finalstep7=step7,finalstep8=step8;
        final int finalstep9=step9,finalstep10=step10,finalstep11=step11,finalstep12=step12,finalstep13=step13,finalstep14=step14,finalstep15=step15,finalstep16=step16;
        runOnUiThread(new Runnable() {
            public void run() {
                for (int z=0;z<9;z++){
                    if (finalstep1 == z) {
                        spinner1.setSelection(z);
                    }
                    if (finalstep2 == z) {
                        spinner2.setSelection(z);
                    }
                    if (finalstep3 == z) {
                        spinner3.setSelection(z);
                    }
                    if (finalstep4 == z) {
                        spinner4.setSelection(z);
                    }
                    if (finalstep5 == z) {
                        spinner5.setSelection(z);
                    }
                    if (finalstep6 == z) {
                        spinner6.setSelection(z);
                    }
                    if (finalstep7 == z) {
                        spinner7.setSelection(z);
                    }
                    if (finalstep8 == z) {
                        spinner8.setSelection(z);
                    }
                    if (finalstep9 == z) {
                        spinner9.setSelection(z);
                    }
                    if (finalstep10 == z) {
                        spinner10.setSelection(z);
                    }
                    if (finalstep11 == z) {
                        spinner11.setSelection(z);
                    }
                    if (finalstep12 == z) {
                        spinner12.setSelection(z);
                    }
                    if (finalstep13 == z) {
                        spinner13.setSelection(z);
                    }

                    if (finalstep14 == z) {
                        spinner14.setSelection(z);
                    }
                    Log.d(TAG, "Mikro FTM rate step count:   " + z);
                }

                for (int z=0;z<11;z++) {
                    if (finalstep15 == z) {
                        if (finalstep15==10){
                            if (readresult) {
                                list.remove("Fan Output");
                                Log.d(TAG, "Mikro FTM remove Fan Output");
                            }
                            list.add("Fan Output");
                            final ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(ratestep_setting.this, R.layout.spinner, list);
                            dataAdapter.notifyDataSetChanged();
                            Log.d(TAG, "Mikro FTM rate step 15:fan out");
                            spinner15.setSelection(9);
                            readresult = true;
                        } else {
                            spinner15.setSelection(z);
                        }
                        Log.d(TAG, "Mikro FTM rate step 15:   " + z);
                    }
                }

                for (int z=0;z<11;z++) {
                    if (finalstep16 == z) {
                        spinner16.setSelection(z);
                        Log.d(TAG, "Mikro FTM rate step 16:   " + z);
                    }
                }
            }
        });
        ratiostep=true;
        return joined;
    }

    public String updateratioMessage (byte[] message){
        int no_message = message.length;
        String[] putftmpara = new String[message.length];
        String[] putftmpara1 = new String[message.length];
        byte tempMsg = 0;
        String msg= "";
        String joined = null,append = null;


        // String msg = "FTM Message: Total byte is " + no_message +"\n";
        Log.d(TAG, "Mikro: update ratio msg " + no_message);
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
        Log.d(TAG, "Mikro ratio finalMsg: " + finalMsg);
        String step1=null, step2=null,step3=null,step4=null, step5=null, step6=null,step7=null;
        String step8=null,step9=null,step10=null,step11=null,step12=null,step13=null,step14=null,step15=null,step16=null;
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

            int step=0;
            switch (i) {
                case 3:
                    step= Integer.parseInt(element, 16);
                    step1=Integer.toString(step);
                    Log.d(TAG, "Mikro ratio rate step1:   " + step1);
                    break;
                case 5:
                    step= Integer.parseInt(element, 16);
                    step2=Integer.toString(step);
                    Log.d(TAG, "Mikro ratio rate step2:   " + step2);
                    break;
                case 7:
                    step= Integer.parseInt(element, 16);
                    step3=Integer.toString(step);
                    break;
                case 9:
                    step= Integer.parseInt(element, 16);
                    step4=Integer.toString(step);
                    break;
                case 11:
                    step= Integer.parseInt(element, 16);
                    step5=Integer.toString(step);
                    break;
                case 13:
                    step= Integer.parseInt(element, 16);
                    step6=Integer.toString(step);
                    break;
                case 15:
                    step= Integer.parseInt(element, 16);
                    step7=Integer.toString(step);
                    break;
                case 17:
                    step= Integer.parseInt(element, 16);
                    step8=Integer.toString(step);
                    break;
                case 19:
                    step= Integer.parseInt(element, 16);
                    step9=Integer.toString(step);
                    break;
                case 21:
                    step= Integer.parseInt(element, 16);
                    step10=Integer.toString(step);
                    break;
                case 23:
                    step= Integer.parseInt(element, 16);
                    step11=Integer.toString(step);
                    break;
                case 25:
                    step= Integer.parseInt(element, 16);
                    step12=Integer.toString(step);
                    break;
                case 27:
                    step= Integer.parseInt(element, 16);
                    step13=Integer.toString(step);
                    break;
                case 29:
                    step= Integer.parseInt(element, 16);
                    step14=Integer.toString(step);
                    break;
                case 31:
                    step= Integer.parseInt(element, 16);
                    step15=Integer.toString(step);
                    Log.d(TAG, "Mikro ratio rate step15:   " + step15);
                    break;
                case 33:
                    step= Integer.parseInt(element, 16);
                    step16=Integer.toString(step);
                    Log.d(TAG, "Mikro ratio rate step16:   " + step16);
                    break;

            }
        }

        final String finalstep1=step1,finalstep2=step2,finalstep3=step3,finalstep4=step4,finalstep5=step5,finalstep6=step6,finalstep7=step7,finalstep8=step8;
        final String finalstep9=step9,finalstep10=step10,finalstep11=step11,finalstep12=step12,finalstep13=step13,finalstep14=step14,finalstep15=step15,finalstep16=step16;
        runOnUiThread(new Runnable() {
            public void run() {
                r1.setText(finalstep1);
                r2.setText(finalstep2);
                r3.setText(finalstep3);
                r4.setText(finalstep4);
                r5.setText(finalstep5);
                r6.setText(finalstep6);
                r7.setText(finalstep7);
                r8.setText(finalstep8);
                r9.setText(finalstep9);
                r10.setText(finalstep10);
                r11.setText(finalstep11);
                r12.setText(finalstep12);
                r13.setText(finalstep13);
                r14.setText(finalstep14);
                r15.setText(finalstep15);
                r16.setText(finalstep16);

            }
        });
        ratiostep=false;
        return joined;

    }

}
