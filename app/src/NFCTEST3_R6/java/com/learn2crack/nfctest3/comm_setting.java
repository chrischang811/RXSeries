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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static android.os.SystemClock.sleep;
import static com.st.st25sdk.STException.STExceptionCode.CMD_FAILED;



public class comm_setting extends BaseActivity implements TagDiscovery.onTagDiscoveryCompletedListener{

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
    private int baud_spin,parity_spin,bit_spin,edit_add;
    private String add1,add0,crc_low,crc_high;

    static private NFCTag mftmTag;
    static private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private static final String TAG = comm_setting.class.getSimpleName();
    CRC16checker crccheck=new CRC16checker();

    Handler handler = new Handler();
    private Button writebtn, readbtn;
    private Spinner spinner1, spinner2,spinner3;
    private EditText editaddress;
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
        getLayoutInflater().inflate(R.layout.thermal, contentFrameLayout);
       // setContentView(R.layout.comm_setting);
        //CRC16checker crccheck=new CRC16checker();
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this); //check NFC hardware available
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        readbtn = findViewById(R.id.btn_read_comm);
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
        editaddress = (EditText) findViewById(R.id.edit_address);
        editaddress.setFilters(new InputFilter[]{new InputFilterMinMax("1", "255")});

        spinner1=(Spinner)findViewById(R.id.baudrate_option);
        spinner1.setOnItemSelectedListener(listener);

        spinner2=(Spinner)findViewById(R.id.parity_option);
        spinner2.setOnItemSelectedListener(listener);

        spinner3=(Spinner)findViewById(R.id.stopbit_option);
        spinner3.setOnItemSelectedListener(listener);

        writebtn = findViewById(R.id.btn_write_comm);
        writebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!writeRun){
                String request = "01100133000408";

                Log.d(TAG, "Mikro sent: " + request);

                //Edit address
                if (TextUtils.isEmpty(editaddress.getText().toString())) {
                    editaddress.setError("CT Ratio field cannot be empty");
                    return;
                } else {
                    edit_add = Integer.valueOf(editaddress.getText().toString());
                    String address[] = hexStringToStringArray(Integer.toHexString(edit_add));
                    add1 = address[0];
                    add0 = address[1];
                    request = request + add1 + add0;
                    Log.d(TAG, "Mikro after address " + request);
                }

                //select baudrate
                String baud = String.valueOf(spinner1.getSelectedItem());
                Log.d(TAG, "Mikro: baudrate spinner: " + baud);
                if (baud.equals("2400")) {
                    baud_spin = 0;
                    request = request + "0000";
                    Log.d(TAG, "Mikro: baud spinner:0 " + request);
                } else if (baud.equals("4800")) {
                    baud_spin = 1;
                    request = request + "0001";
                    Log.d(TAG, "Mikro: baud spinner:1 " + request);
                } else if (baud.equals("9600")) {
                    baud_spin = 2;
                    request = request + "0002";
                    Log.d(TAG, "Mikro: baud spinner:2 " + request);
                } else if (baud.equals("19200")) {
                    baud_spin = 3;
                    request = request + "0003";
                    Log.d(TAG, "Mikro: baud spinner:3 " + request);
                } else if (baud.equals("38400")) {
                    baud_spin = 4;
                    request = request + "0004";
                    Log.d(TAG, "Mikro: baud spinner:4 " + request);
                } else if (baud.equals("57600")) {
                    baud_spin = 5;
                    request = request + "0005";
                    Log.d(TAG, "Mikro: baud spinner:5 " + request);
                }

                //parity setting
                String parity = String.valueOf(spinner2.getSelectedItem());
                Log.d(TAG, "Mikro: vs spinner: " + parity);
                if (parity.equals("Even")) {
                    parity_spin = 1;
                    request = request + "0001";
                    Log.d(TAG, "Mikro: parity spinner:1 " + request);
                } else if (parity.equals("Odd")) {
                    parity_spin = 2;
                    request = request + "0002";
                    Log.d(TAG, "Mikro: parity spinner:2 " + request);
                } else if (parity.equals("None")) {
                    parity_spin = 0;
                    request = request + "0000";
                    Log.d(TAG, "Mikro: parity spinner:0 " + request);
                }

                //Stop bit setting
                String st_bit = String.valueOf(spinner3.getSelectedItem());
                Log.d(TAG, "Mikro: st_bit spinner: " + st_bit);
                if (st_bit.equals("1 Stop Bit")) {
                    bit_spin = 0;
                    request = request + "0000";
                    Log.d(TAG, "Mikro: Stop Bits spinner: 0 " + request);
                } else if (st_bit.equals("2 Stop Bits")) {
                    bit_spin = 1;
                    request = request + "0001";
                    Log.d(TAG, "Mikro: Stop Bits spinner:1 " + request);
                }

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
        mBuffer = new byte[29];
        mBuffer [0] = (byte)0x01;
        mBuffer [1] = (byte)0x10;
        mBuffer [2] = (byte)0x01;
        mBuffer [3] = (byte)0x33;
        mBuffer [4] = (byte)0x00;
        mBuffer [5] = (byte)0x04;
        mBuffer [6] = (byte)0x08;
        mBuffer [7] = (byte)Integer.parseInt(add1,16);
        mBuffer [8] = (byte)Integer.parseInt(add0,16);
        mBuffer [9] = (byte)0x00;
        mBuffer [10] = (byte)baud_spin;
        mBuffer [11] = (byte)0x00;
        mBuffer [12] = (byte)parity_spin;
        mBuffer [13] = (byte)0x00;
        mBuffer [14] = (byte)bit_spin;
        mBuffer [15] = (byte)Integer.parseInt(crc_high,16);
        mBuffer [16] = (byte)Integer.parseInt(crc_low,16);

        Log.d(TAG, "Mikro: byte bit_spin: " + mBuffer[14]);
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
        mBuffer [3] = (byte)0x33;
        mBuffer [4] = (byte)0x00;
        mBuffer [5] = (byte)0x04;
        mBuffer [6] = (byte)0xb5;
        mBuffer [7] = (byte)0xfa;

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
                                    Toast.makeText(comm_setting.this, "FTM OFF", Toast.LENGTH_SHORT).show();
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
                                    Toast.makeText(comm_setting.this, "FTM OFF", Toast.LENGTH_SHORT).show();
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
                                                new AlertDialog.Builder(comm_setting.this)
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
                                        Toast.makeText(comm_setting.this, "FTM OFF", Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(comm_setting.this, "FTM OFF", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(comm_setting.this, "RF Mailbox Not Empty", Toast.LENGTH_SHORT).show();
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

        int add=0,parity=0,baudrate=0,stop_bit=0;
        String addString="";
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
                    add= Integer.parseInt(element, 16);
                    Log.d(TAG, "Mikro FTM add:   " + add);
                    addString=Integer.toString(add);
                    break;
                case 5:
                    baudrate=Integer.parseInt(element, 16);
                    Log.d(TAG, "Mikro FTM baudrate:   " + baudrate);
                    break;
                case 7:
                    parity=Integer.parseInt(element, 16);
                    Log.d(TAG, "Mikro FTM parity:   " + parity);
                    break;
                case 9:
                    stop_bit=Integer.parseInt(element, 16);
                    Log.d(TAG, "Mikro FTM stop_bit:   " + stop_bit);
                    break;

            }
        }

        final int finalbaudrate=baudrate, finalparity=parity, finalstop_bit=stop_bit;
        final String finaladd=addString;
        runOnUiThread(new Runnable() {
            public void run() {

                editaddress.setText(finaladd);
                if (finalbaudrate==0){
                    spinner1.setSelection(0);
                } else if (finalbaudrate==1){
                    spinner1.setSelection(1);
                }else if (finalbaudrate==2){
                    spinner1.setSelection(2);
                }else if (finalbaudrate==3){
                    spinner1.setSelection(3);
                }else if (finalbaudrate==4){
                    spinner1.setSelection(4);
                }else if (finalbaudrate==5){
                    spinner1.setSelection(5);
                }

                if (finalparity==0){
                    spinner2.setSelection(0);
                } else if (finalparity==1){
                    spinner2.setSelection(1);
                }else if (finalparity==2){
                    spinner2.setSelection(2);
                }

                if (finalstop_bit==0){
                    spinner3.setSelection(0);
                } else if (finalstop_bit==1){
                    spinner3.setSelection(1);
                }

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
        Log.d(TAG,"Mikro  hex: "+parts.toArray(new String[0]));
        return parts.toArray(new String[0]);
    }


}
