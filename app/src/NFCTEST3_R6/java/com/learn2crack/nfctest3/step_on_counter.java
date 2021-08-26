package com.learn2crack.rx1000;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
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
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.STLog;
import com.st.st25sdk.TagHelper;
import com.st.st25sdk.type5.st25dv.ST25DVTag;

import static android.os.SystemClock.sleep;
import static com.st.st25sdk.STException.STExceptionCode.CMD_FAILED;

public class step_on_counter extends BaseActivity implements TagDiscovery.onTagDiscoveryCompletedListener{

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

    private int totalreset=0,writeS=0;
    private CheckBox ratecount1,ratecount2,ratecount3,ratecount4,ratecount5,ratecount6,ratecount7,ratecount8,ratecount9;
    private CheckBox ratecount10,ratecount11,ratecount12,ratecount13,ratecount14,ratecount15,ratecount16;
    private int lowbyte,highbyte,write_flag;
    private String crc_low,crc_high,lowbyte_string,bits;
    private int[] step_count= new int[16];
    ProgressDialog dialog;

    static private NFCTag mftmTag;
    static private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private static final String TAG = step_on_counter.class.getSimpleName();
    CRC16checker crccheck=new CRC16checker();

    Handler handler = new Handler();
    private Button deleteSbtn, deleteAllbtn,readbtn;

    private TextView text7,text8,text9,text10,text11,text12,text13,text14;
    private TextView r1,r2, r3, r4,r5,r6,r7,r8,r9,r10,r11,r12,r13,r14,r15,r16;
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
        FrameLayout contentFrameLayout = (FrameLayout) findViewById(R.id.content_frame);
        getLayoutInflater().inflate(R.layout.function_setting, contentFrameLayout);
       // setContentView(R.layout.step_on_counter);
        Intent intent = getIntent();
        bits=intent.getStringExtra("device_bits");
        //bits="12";
        Log.d("Mikro ", "devices type: " +  bits);
        write_flag=0;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this); //check NFC hardware available
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        text7=(TextView)findViewById(R.id.rate7count_text);
        text8=(TextView)findViewById(R.id.rate8count_text);
        text9=(TextView)findViewById(R.id.rate9count_text);
        text10=(TextView)findViewById(R.id.rate10count_text);
        text11=(TextView)findViewById(R.id.rate11count_text);
        text12=(TextView)findViewById(R.id.rate12count_text);
        text13=(TextView)findViewById(R.id.rate13count_text);
        text14=(TextView)findViewById(R.id.rate14count_text);

        r1=(TextView)findViewById(R.id.countstep1);
        r2=(TextView)findViewById(R.id.countstep2);
        r3=(TextView)findViewById(R.id.countstep3);
        r4=(TextView)findViewById(R.id.countstep4);
        r5=(TextView)findViewById(R.id.countstep5);
        r6=(TextView)findViewById(R.id.countstep6);
        r7=(TextView)findViewById(R.id.countstep7);
        r8=(TextView)findViewById(R.id.countstep8);
        r9=(TextView)findViewById(R.id.countstep9);
        r10=(TextView)findViewById(R.id.countstep10);
        r11=(TextView)findViewById(R.id.countstep11);
        r12=(TextView)findViewById(R.id.countstep12);
        r13=(TextView)findViewById(R.id.countstep13);
        r14=(TextView)findViewById(R.id.countstep14);
        r15=(TextView)findViewById(R.id.countstep15);
        r16=(TextView)findViewById(R.id.countstep16);

        ratecount1=(CheckBox)findViewById(R.id.check_count_r1);
        ratecount2=(CheckBox)findViewById(R.id.check_count_r2);
        ratecount3=(CheckBox)findViewById(R.id.check_count_r3);
        ratecount4=(CheckBox)findViewById(R.id.check_count_r4);
        ratecount5=(CheckBox)findViewById(R.id.check_count_r5);
        ratecount6=(CheckBox)findViewById(R.id.check_count_r6);
        ratecount7=(CheckBox)findViewById(R.id.check_count_r7);
        ratecount8=(CheckBox)findViewById(R.id.check_count_r8);
        ratecount9=(CheckBox)findViewById(R.id.check_count_r9);
        ratecount10=(CheckBox)findViewById(R.id.check_count_r10);
        ratecount11=(CheckBox)findViewById(R.id.check_count_r11);
        ratecount12=(CheckBox)findViewById(R.id.check_count_r12);
        ratecount13=(CheckBox)findViewById(R.id.check_count_r13);
        ratecount14=(CheckBox)findViewById(R.id.check_count_r14);
        ratecount15=(CheckBox)findViewById(R.id.check_count_r15);
        ratecount16=(CheckBox)findViewById(R.id.check_count_r16);

        if(bits.equals("8")){
            ratecount7.setEnabled(false);
            text7.setBackgroundColor(Color.DKGRAY);

            ratecount8.setEnabled(false);
            text8.setBackgroundColor(Color.DKGRAY);

            ratecount9.setEnabled(false);
            text9.setBackgroundColor(Color.DKGRAY);

            ratecount10.setEnabled(false);
            text10.setBackgroundColor(Color.DKGRAY);

            ratecount11.setEnabled(false);
            text11.setBackgroundColor(Color.DKGRAY);

            ratecount12.setEnabled(false);
            text12.setBackgroundColor(Color.DKGRAY);

            ratecount13.setEnabled(false);
            text13.setBackgroundColor(Color.DKGRAY);

            ratecount14.setEnabled(false);
            text14.setBackgroundColor(Color.DKGRAY);
        }

        if(bits.equals("12")){
            ratecount11.setEnabled(false);
            text11.setBackgroundColor(Color.DKGRAY);

            ratecount12.setEnabled(false);
            text12.setBackgroundColor(Color.DKGRAY);

            ratecount13.setEnabled(false);
            text13.setBackgroundColor(Color.DKGRAY);

            ratecount14.setEnabled(false);
            text14.setBackgroundColor(Color.DKGRAY);
        }


        readbtn = findViewById(R.id.btn_read_count);
        readbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                write_flag=0;
                if (readRun){
                    readRun = false;
                    readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                } else {
                    readRun = true;
                    readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_green));
                }

            }
        });

        deleteSbtn = findViewById(R.id.btn_read_function);
        deleteSbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                write_flag=1;
                totalreset=0;
                writeS=0;
                if (!writeRun) {
                    if(ratecount1.isChecked()){
                        step_count[0]=1;
                        totalreset++;
                    }else{
                        step_count[0]=0;
                    }

                    if(ratecount2.isChecked()){
                        step_count[1]=1;
                        totalreset++;
                    }else{
                        step_count[1]=0;
                    }

                    if(ratecount3.isChecked()){
                        step_count[2]=1;
                        totalreset++;
                    }else{
                        step_count[2]=0;
                    }

                    if(ratecount4.isChecked()){
                        step_count[3]=1;
                        totalreset++;
                    }else{
                        step_count[3]=0;
                    }


                    if(ratecount5.isChecked()){
                        step_count[4]=1;
                        totalreset++;
                    }else{
                        step_count[4]=0;
                    }

                    if(ratecount6.isChecked()){
                        step_count[5]=1;
                        totalreset++;
                    }else{
                        step_count[5]=0;
                    }

                    if(ratecount7.isChecked()){
                        step_count[6]=1;
                        totalreset++;
                    }else{
                        step_count[6]=0;
                    }

                    if(ratecount8.isChecked()){
                        step_count[7]=1;
                        totalreset++;
                    }else{
                        step_count[7]=0;
                    }

                    if(ratecount9.isChecked()){
                        step_count[8]=1;
                        totalreset++;
                    }else{
                        step_count[8]=0;
                    }


                    if(ratecount10.isChecked()){
                        step_count[9]=1;
                        totalreset++;
                    }else{
                        step_count[9]=0;
                    }

                    if(ratecount11.isChecked()){
                        step_count[10]=1;
                        totalreset++;
                    }else{
                        step_count[10]=0;
                    }

                    if(ratecount12.isChecked()){
                        step_count[11]=1;
                        totalreset++;
                    }else{
                        step_count[11]=0;
                    }

                    if(ratecount13.isChecked()){
                        step_count[12]=1;
                        totalreset++;
                    }else{
                        step_count[12]=0;
                    }

                    if(ratecount14.isChecked()){
                        step_count[13]=1;
                        totalreset++;
                    }else{
                        step_count[13]=0;
                    }

                    if(ratecount15.isChecked()){
                        step_count[14]=1;
                        totalreset++;
                    }else{
                        step_count[14]=0;
                    }

                    if(ratecount16.isChecked()){
                        step_count[15]=1;
                        totalreset++;
                    }else{
                        step_count[15]=0;
                    }

                    writeRun = true;
                    deleteSbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_green));
                    Log.d(TAG, "Mikro total reset: " + totalreset);
                }else {
                    writeRun=false;
                    deleteSbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                }
            }
        });


        deleteAllbtn = findViewById(R.id.btn_save_function);
        deleteAllbtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                write_flag=0;
                if (!writeRun) {

                    String request = "010602000300";
                    lowbyte=0;
                    highbyte=3;

                    Log.d(TAG, "Mikro sent: " + request);
                    //obtain CRC
                    int[] ans= crccheck.getCRC(request);
                    crc_low = Integer.toHexString(ans[0]);
                    crc_high = Integer.toHexString(ans[1]);
                    Log.d(TAG, "Mikro CRC delete all: " + crc_high + " " + crc_low);

                    writeRun = true;
                    deleteAllbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_green));
                }else {
                    writeRun=false;
                    deleteAllbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                }
            }
        });

        readCode = new Runnable() {
            @Override
            public void run() {
                // Do something here on the main threadSZ

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
                    if (write_flag == 1) {
                        highbyte = 4;
                        lowbyte =0;
                        String request = "010602000400";
                        Log.d(TAG, "Mikro sent: " + request);
                        //obtain CRC
                        int[] ans = crccheck.getCRC(request);
                        crc_low = Integer.toHexString(ans[0]);
                        crc_high = Integer.toHexString(ans[1]);
                        Log.d(TAG, "Mikro CRC delete single: " + crc_high + " " + crc_low);
                        singleFTM();
                    } else{
                        singleFTM();
                    }
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

        if (mftmTag != null) {
            //write 3
            if (write_flag==1) {
                dialog = ProgressDialog.show(step_on_counter.this, "",
                        "Reseting. Please don't remove your phone from device...", true);
            }

            handler.postDelayed(runCode, 50);  //try to read the message first
        }

    }

    public void singleFTM() {
        //write 4
        if (mftmTag == null) {
            return;
        }

        // int i = 1;
        mBuffer = new byte[8];
        mBuffer [0] = (byte)0x01;
        mBuffer [1] = (byte)0x06;
        mBuffer [2] = (byte)0x02;
        mBuffer [3] = (byte)0x00;
        mBuffer [4] = (byte)highbyte;
        mBuffer [5] = (byte)lowbyte;
        mBuffer [6] = (byte)Integer.parseInt(crc_high,16);
        mBuffer [7] = (byte)Integer.parseInt(crc_low,16);

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
        mBuffer [2] = (byte)0x04;
        mBuffer [3] = (byte)0x00;
        mBuffer [4] = (byte)0x00;
        mBuffer [5] = (byte)0x20;
        mBuffer [6] = (byte)0x45;
        mBuffer [7] = (byte)0x22;

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

                                    handler.postDelayed(readCode, 500);
                                } else {
                                    Log.d(TAG, "Mikro: Fail on single");

                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            if(write_flag==1) {
                                                deleteSbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                                            }else {
                                                deleteAllbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                                            }
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
                                    if(write_flag==1) {
                                        deleteSbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                                    }else {
                                        deleteAllbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                                    }
                                    //writebtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                                    Toast.makeText(step_on_counter.this, "FTM OFF", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                    } catch (STException e) {
                        Log.d(TAG, "mikro: Error on Single");
                        e.printStackTrace();
                        dialog.dismiss();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                new android.app.AlertDialog.Builder(step_on_counter.this)
                                        .setTitle(R.string.app_name)
                                        .setIcon(R.mipmap.ic_launcher)
                                        .setMessage("\nNFC not detected by device, please try again.\n")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                //finish();
                                            }
                                        })
                                        .show();
                                if (write_flag == 1) {
                                    deleteSbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                                } else {
                                    deleteAllbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                                }
                            }
                        });
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
                                    //deleteSbtn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                    //deleteAllbtn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                    readbtn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                                }
                            });
                            readRun = false;
                            reset_ftm();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(step_on_counter.this, "FTM OFF", Toast.LENGTH_SHORT).show();
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
                                Log.d(TAG, "Mikro writerun:" +writeRun);
                                //updateFtmMessage(mFTMmsg);
                                if(writeRun)
                                {
                                    write_result=updateWriteResponse(mFTMmsg);
                                    Log.d(TAG, "Mikro write_result:" +write_result);
                                    if (write_result) {

                                        if (write_flag == 1) {
                                            highbyte = 4;
                                            if (step_count[0] == 1) {
                                                writeS++;
                                                String request = "010602000401";
                                                lowbyte =1;
                                                Log.d(TAG, "Mikro sent: " + request);
                                                //obtain CRC
                                                int[] ans = crccheck.getCRC(request);
                                                crc_low = Integer.toHexString(ans[0]);
                                                crc_high = Integer.toHexString(ans[1]);
                                                //Log.d(TAG, "Mikro CRC delete single: " + crc_high + " " + crc_low);
                                                singleFTM();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        ratecount1.setChecked(false);
                                                        r1.setText("0");
                                                    }
                                                });
                                                step_count[0] = 0;
                                            }else if (step_count[1] == 1) {
                                                writeS++;
                                                String request = "010602000402";
                                                lowbyte =2;
                                                Log.d(TAG, "Mikro sent: " + request);
                                                //obtain CRC
                                                int[] ans = crccheck.getCRC(request);
                                                crc_low = Integer.toHexString(ans[0]);
                                                crc_high = Integer.toHexString(ans[1]);
                                                //Log.d(TAG, "Mikro CRC delete single: " + crc_high + " " + crc_low);
                                                singleFTM();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        ratecount2.setChecked(false);
                                                        r2.setText("0");
                                                    }
                                                });
                                                step_count[1] = 0;
                                            }else if (step_count[2] == 1) {
                                                writeS++;
                                                String request = "010602000403";
                                                lowbyte =3;
                                                Log.d(TAG, "Mikro sent: " + request);
                                                //obtain CRC
                                                int[] ans = crccheck.getCRC(request);
                                                crc_low = Integer.toHexString(ans[0]);
                                                crc_high = Integer.toHexString(ans[1]);
                                                //Log.d(TAG, "Mikro CRC delete single: " + crc_high + " " + crc_low);
                                                singleFTM();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        ratecount3.setChecked(false);
                                                        r3.setText("0");
                                                    }
                                                });
                                                step_count[2] = 0;
                                            }else if (step_count[3] == 1) {
                                                writeS++;
                                                String request = "010602000404";
                                                lowbyte =4;
                                                Log.d(TAG, "Mikro sent: " + request);
                                                //obtain CRC
                                                int[] ans = crccheck.getCRC(request);
                                                crc_low = Integer.toHexString(ans[0]);
                                                crc_high = Integer.toHexString(ans[1]);
                                                //Log.d(TAG, "Mikro CRC delete single: " + crc_high + " " + crc_low);
                                                singleFTM();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        ratecount4.setChecked(false);
                                                        r4.setText("0");
                                                    }
                                                });
                                                step_count[3] = 0;
                                            }else if (step_count[4] == 1) {
                                                writeS++;
                                                String request = "010602000405";
                                                lowbyte = 5;
                                                Log.d(TAG, "Mikro sent: " + request);
                                                //obtain CRC
                                                int[] ans = crccheck.getCRC(request);
                                                crc_low = Integer.toHexString(ans[0]);
                                                crc_high = Integer.toHexString(ans[1]);
                                                //Log.d(TAG, "Mikro CRC delete single: " + crc_high + " " + crc_low);
                                                singleFTM();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        ratecount5.setChecked(false);
                                                        r5.setText("0");
                                                    }
                                                });
                                                step_count[4] = 0;
                                            }else if (step_count[5] == 1) {
                                                writeS++;
                                                String request = "010602000406";
                                                lowbyte = 6;
                                                Log.d(TAG, "Mikro sent: " + request);
                                                //obtain CRC
                                                int[] ans = crccheck.getCRC(request);
                                                crc_low = Integer.toHexString(ans[0]);
                                                crc_high = Integer.toHexString(ans[1]);
                                                //Log.d(TAG, "Mikro CRC delete single: " + crc_high + " " + crc_low);
                                                singleFTM();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        ratecount6.setChecked(false);
                                                        r6.setText("0");
                                                    }
                                                });
                                                step_count[5] = 0;
                                            }else if (step_count[6] == 1) {
                                                writeS++;
                                                String request = "010602000407";
                                                lowbyte = 7;
                                                Log.d(TAG, "Mikro sent: " + request);
                                                //obtain CRC
                                                int[] ans = crccheck.getCRC(request);
                                                crc_low = Integer.toHexString(ans[0]);
                                                crc_high = Integer.toHexString(ans[1]);
                                                //Log.d(TAG, "Mikro CRC delete single: " + crc_high + " " + crc_low);
                                                singleFTM();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        ratecount7.setChecked(false);
                                                        r7.setText("0");
                                                    }
                                                });
                                                step_count[6] = 0;
                                            }else if (step_count[7] == 1) {
                                                writeS++;
                                                String request = "010602000408";
                                                lowbyte = 8;
                                                Log.d(TAG, "Mikro sent: " + request);
                                                //obtain CRC
                                                int[] ans = crccheck.getCRC(request);
                                                crc_low = Integer.toHexString(ans[0]);
                                                crc_high = Integer.toHexString(ans[1]);
                                                //Log.d(TAG, "Mikro CRC delete single: " + crc_high + " " + crc_low);
                                                singleFTM();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        ratecount8.setChecked(false);
                                                        r8.setText("0");
                                                    }
                                                });
                                                step_count[7] = 0;
                                            }else if (step_count[8] == 1) {
                                                writeS++;
                                                String request = "010602000409";
                                                lowbyte = 9;
                                                Log.d(TAG, "Mikro sent: " + request);
                                                //obtain CRC
                                                int[] ans = crccheck.getCRC(request);
                                                crc_low = Integer.toHexString(ans[0]);
                                                crc_high = Integer.toHexString(ans[1]);
                                                //Log.d(TAG, "Mikro CRC delete single: " + crc_high + " " + crc_low);
                                                singleFTM();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        ratecount9.setChecked(false);
                                                        r9.setText("0");
                                                    }
                                                });
                                                step_count[8] = 0;
                                            }else if (step_count[9] == 1) {
                                                writeS++;
                                                String request = "01060200040a";
                                                lowbyte = Integer.parseInt(Integer.toHexString(10), 16);
                                                Log.d(TAG, "Mikro sent: " + request);
                                                //obtain CRC
                                                int[] ans = crccheck.getCRC(request);
                                                crc_low = Integer.toHexString(ans[0]);
                                                crc_high = Integer.toHexString(ans[1]);
                                                //Log.d(TAG, "Mikro CRC delete single: " + crc_high + " " + crc_low);
                                                singleFTM();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        ratecount10.setChecked(false);
                                                        r10.setText("0");
                                                    }
                                                });
                                                step_count[9] = 0;
                                            }else if (step_count[10] == 1) {
                                                writeS++;
                                                String request = "01060200040b";
                                                lowbyte = Integer.parseInt(Integer.toHexString(11), 16);
                                                Log.d(TAG, "Mikro sent: " + request);
                                                //obtain CRC
                                                int[] ans = crccheck.getCRC(request);
                                                crc_low = Integer.toHexString(ans[0]);
                                                crc_high = Integer.toHexString(ans[1]);
                                                //Log.d(TAG, "Mikro CRC delete single: " + crc_high + " " + crc_low);
                                                singleFTM();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        ratecount11.setChecked(false);
                                                        r11.setText("0");
                                                    }
                                                });
                                                step_count[10] = 0;
                                            }else if (step_count[11] == 1) {
                                                writeS++;
                                                String request = "01060200040c";
                                                lowbyte = Integer.parseInt(Integer.toHexString(12), 16);
                                                Log.d(TAG, "Mikro sent: " + request);
                                                //obtain CRC
                                                int[] ans = crccheck.getCRC(request);
                                                crc_low = Integer.toHexString(ans[0]);
                                                crc_high = Integer.toHexString(ans[1]);
                                                //Log.d(TAG, "Mikro CRC delete single: " + crc_high + " " + crc_low);
                                                singleFTM();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        ratecount12.setChecked(false);
                                                        r12.setText("0");
                                                    }
                                                });
                                                step_count[11] = 0;
                                            } else if (step_count[12] == 1) {
                                                writeS++;
                                                String request = "01060200040d";
                                                lowbyte = Integer.parseInt(Integer.toHexString(13), 16);
                                                Log.d(TAG, "Mikro sent: " + request);
                                                //obtain CRC
                                                int[] ans = crccheck.getCRC(request);
                                                crc_low = Integer.toHexString(ans[0]);
                                                crc_high = Integer.toHexString(ans[1]);
                                                //Log.d(TAG, "Mikro CRC delete single: " + crc_high + " " + crc_low);
                                                singleFTM();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        ratecount13.setChecked(false);
                                                        r13.setText("0");
                                                    }
                                                });
                                                step_count[12] = 0;
                                            }else if (step_count[13] == 1) {
                                                writeS++;
                                                String request = "01060200040e";
                                                lowbyte = Integer.parseInt(Integer.toHexString(14), 16);
                                                Log.d(TAG, "Mikro sent: " + request);
                                                //obtain CRC
                                                int[] ans = crccheck.getCRC(request);
                                                crc_low = Integer.toHexString(ans[0]);
                                                crc_high = Integer.toHexString(ans[1]);
                                                //Log.d(TAG, "Mikro CRC delete single: " + crc_high + " " + crc_low);
                                                singleFTM();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        ratecount14.setChecked(false);
                                                        r14.setText("0");
                                                    }
                                                });
                                                step_count[13] = 0;
                                            }else if (step_count[14] == 1) {
                                                writeS++;
                                                String request = "01060200040f";
                                                lowbyte = Integer.parseInt(Integer.toHexString(15), 16);
                                                Log.d(TAG, "Mikro sent: " + request);
                                                //obtain CRC
                                                int[] ans = crccheck.getCRC(request);
                                                crc_low = Integer.toHexString(ans[0]);
                                                crc_high = Integer.toHexString(ans[1]);
                                                //Log.d(TAG, "Mikro CRC delete single: " + crc_high + " " + crc_low);
                                                singleFTM();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        ratecount15.setChecked(false);
                                                        r15.setText("0");
                                                    }
                                                });
                                                step_count[14] = 0;
                                            }else if (step_count[15] == 1) {
                                                writeS++;
                                                String request = "010602000410";
                                                lowbyte = Integer.parseInt(Integer.toHexString(16), 16);
                                                Log.d(TAG, "Mikro sent: " + request);
                                                //obtain CRC
                                                int[] ans = crccheck.getCRC(request);
                                                crc_low = Integer.toHexString(ans[0]);
                                                crc_high = Integer.toHexString(ans[1]);
                                                //Log.d(TAG, "Mikro CRC delete single: " + crc_high + " " + crc_low);
                                                singleFTM();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        ratecount16.setChecked(false);
                                                        r16.setText("0");
                                                    }
                                                });
                                                step_count[15] = 0;
                                            }

                                            if (writeS == totalreset) {
                                                Log.d(TAG, "Mikro write single finished ");
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        deleteSbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                                                        dialog.dismiss();

                                                    }
                                                });
                                            }

                                        }else {
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    ratecount1.setChecked(false);
                                                    ratecount2.setChecked(false);
                                                    ratecount3.setChecked(false);
                                                    ratecount4.setChecked(false);
                                                    ratecount5.setChecked(false);
                                                    ratecount6.setChecked(false);
                                                    ratecount7.setChecked(false);
                                                    ratecount8.setChecked(false);
                                                    ratecount9.setChecked(false);
                                                    ratecount10.setChecked(false);
                                                    ratecount11.setChecked(false);
                                                    ratecount12.setChecked(false);
                                                    ratecount13.setChecked(false);
                                                    ratecount14.setChecked(false);
                                                    ratecount15.setChecked(false);
                                                    ratecount16.setChecked(false);

                                                    r1.setText("0");
                                                    r2.setText("0");
                                                    r3.setText("0");
                                                    r4.setText("0");
                                                    r5.setText("0");
                                                    r6.setText("0");
                                                    r7.setText("0");
                                                    r8.setText("0");
                                                    r9.setText("0");
                                                    r10.setText("0");
                                                    r11.setText("0");
                                                    r12.setText("0");
                                                    r13.setText("0");
                                                    r14.setText("0");
                                                    r15.setText("0");
                                                    r16.setText("0");
                                                    deleteAllbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                                                }
                                            });
                                        }

                                    }else{
                                        Log.d(TAG, "Mikro write failed ");
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                if(write_flag==1) {
                                                    deleteSbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                                                }else {
                                                    deleteAllbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                                                }
                                            }
                                        });
                                    }
                                    // writeRun = false;
                                }

                                if(readRun)
                                {
                                    String joined;
                                    joined = updateFtmMessage(mFTMmsg);
                                    Log.d(TAG, "Mikro: Received "+joined);
                                    result = crccheck.crcChecker16(joined);
                                    Log.d(TAG, "Mikro: Result CRC "+result);
                                    if (!result){
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                //  Toast.makeText(MainActivity.this, "FTM is OFF", Toast.LENGTH_SHORT).show();
                                                //Log.d(TAG, "Mikro: sync FTM is off");
                                                new AlertDialog.Builder(step_on_counter.this)
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
                                        Toast.makeText(step_on_counter.this, "FTM OFF", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            if(writeRun)
                            {
                                writeRun = false;
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        if(write_flag==1) {
                                            deleteSbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                                        }else {
                                            deleteAllbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                                        }
                                        Toast.makeText(step_on_counter.this, "FTM OFF", Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }
                            reset_ftm();
                        }
                    } catch (STException e) {
                        Log.d(TAG, "mikro: Error on read");
                        e.printStackTrace();
                        checkError(e);
                        dialog.dismiss();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                new android.app.AlertDialog.Builder(step_on_counter.this)
                                        .setTitle(R.string.app_name)
                                        .setIcon(R.mipmap.ic_launcher)
                                        .setMessage("\nNFC not detected by device, please try again.\n")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                //finish();
                                            }
                                        })
                                        .show();
                                if (write_flag == 1) {
                                    deleteSbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                                } else {
                                    deleteAllbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                                }
                            }
                        });
                     //   handler.postDelayed(readCode,DELAY_FTM);

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
                        Toast.makeText(step_on_counter.this, "RF Mailbox Not Empty", Toast.LENGTH_SHORT).show();
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
                    if (element.equals("86")&& lowbyte!=0){
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

        String step1=null, step2=null,step3=null,step4=null, step5=null, step6=null,step7=null,step8=null;
        String step9=null,step10=null,step11=null,step12=null,step13=null,step14=null,step15=null,step16=null;
        String val_temp=null;
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

            String step_temp=null;
            int st=0;
            switch (i) {
                case 3:
                    val_temp = element;
                    break;
                case 5:
                    step_temp =val_temp+element;
                    Log.d(TAG, "Mikro step 1:   " + step_temp);
                    st = Integer.parseInt(step_temp, 16);
                    Log.d(TAG, "Mikro step1:   " + st);
                    step1=Integer.toString(st);
                    break;
                case 7:
                    val_temp = element;
                    break;
                case 9:
                    step_temp =val_temp+element;
                    st = Integer.parseInt(step_temp, 16);
                    step2=Integer.toString(st);
                    break;
                case 11:
                    val_temp = element;

                    break;
                case 13:
                    step_temp =val_temp+element;
                    st = Integer.parseInt(step_temp, 16);
                    step3=Integer.toString(st) ;
                    break;
                case 15:
                    val_temp = element;
                    break;
                case 17:
                    step_temp =val_temp+element;
                    st = Integer.parseInt(step_temp, 16);
                    step4=Integer.toString(st) ;
                    break;
                case 19:
                    val_temp = element;
                    break;
                case 21:
                    step_temp =val_temp+element;
                    st = Integer.parseInt(step_temp, 16);
                    step5=Integer.toString(st) ;
                    break;
                case 23:
                    val_temp = element;
                    break;
                case 25:
                    step_temp =val_temp+element;
                    st = Integer.parseInt(step_temp, 16);
                    step6=Integer.toString(st);
                    break;
                case 27:
                    val_temp = element;
                    break;
                case 29:
                    step_temp =val_temp+element;
                    st = Integer.parseInt(step_temp, 16);
                    step7=Integer.toString(st) ;
                    break;
                case 31:
                    val_temp = element;
                    break;
                case 33:
                    step_temp =val_temp+element;
                    st = Integer.parseInt(step_temp, 16);
                    step8=Integer.toString(st);
                    break;
                case 35:
                    val_temp = element;
                    break;
                case 37:
                    step_temp =val_temp+element;
                    Log.d(TAG, "Mikro step 9:   " + step_temp);
                    st = Integer.parseInt(step_temp, 16);
                    Log.d(TAG, "Mikro step 9:   " + st);
                    step9=Integer.toString(st) ;
                    break;
                case 39:
                    val_temp = element;
                    break;
                case 41:
                    step_temp =val_temp+element;
                    st = Integer.parseInt(step_temp, 16);
                    step10=Integer.toString(st) ;
                    break;
                case 43:
                    val_temp = element;
                    break;
                case 45:
                    step_temp =val_temp+element;
                    st = Integer.parseInt(step_temp, 16);
                    step11=Integer.toString(st);
                    break;
                case 47:
                    val_temp = element;
                    break;
                case 49:
                    step_temp =val_temp+element;
                    st = Integer.parseInt(step_temp, 16);
                    step12=Integer.toString(st) ;
                    break;
                case 51:
                    val_temp = element;
                    break;
                case 53:
                    step_temp =val_temp+element;
                    st = Integer.parseInt(step_temp, 16);
                    step13=Integer.toString(st) ;
                    break;
                case 55:
                    val_temp = element;
                    break;
                case 57:
                    step_temp =val_temp+element;
                    st = Integer.parseInt(step_temp, 16);
                    step14=Integer.toString(st);
                    break;
                case 59:
                    val_temp = element;
                    break;
                case 61:
                    step_temp =val_temp+element;
                    st = Integer.parseInt(step_temp, 16);
                    step15=Integer.toString(st);
                    break;
                case 63:
                    val_temp = element;
                    break;
                case 65:
                    step_temp =val_temp+element;
                    st = Integer.parseInt(step_temp, 16);
                    step16=Integer.toString(st);
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
                r15.setText(finalstep15);
                r16.setText(finalstep16);
                if(bits!="8") {
                    r7.setText(finalstep7);
                    r8.setText(finalstep8);
                    r9.setText(finalstep9);
                    r10.setText(finalstep10);
                    if (bits!="12"){
                        r11.setText(finalstep11);
                        r12.setText(finalstep12);
                        r13.setText(finalstep13);
                        r14.setText(finalstep14);
                    }
                }
            }
        });

        return joined;
    }


}
