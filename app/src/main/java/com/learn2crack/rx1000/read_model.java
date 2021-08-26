package com.learn2crack.rx1000;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.STLog;
import com.st.st25sdk.TagHelper;
import com.st.st25sdk.type5.st25dv.ST25DVTag;

import static android.os.SystemClock.sleep;
import static com.st.st25sdk.STException.STExceptionCode.CMD_FAILED;


public class read_model extends AppCompatActivity implements TagDiscovery.onTagDiscoveryCompletedListener {
    private final int ERROR     = -1;
    private final int TRY_AGAIN = 0;
    private final int OK        = 1;

    private final int DELAY_FTM = 50;

    Handler handler = new Handler();
    SharedPreferences preferences;

    private Button readbtn;
    private boolean mMailboxEnabled = false;
    private boolean result = false;
    private boolean mRFMsg = false;
    private boolean mHostMsg = false;
    private boolean enableftm;

    private byte[] mFTMmsg;
    private byte[] mBuffer;

    //private boolean enableftm = false;
    private boolean disableftm = false;
    private boolean readRun = false;
    private boolean syncRun = false;

    private int mMaxPayloadSizeTx = 220;
    private int mMaxPayloadSizeRx = 32;
    private MainActivity1 mAction;

    //Important part for NFC/////////////
    private static final String TAG = read_model.class.getSimpleName();
    CRC16checker crccheck=new CRC16checker();

    static private NFCTag mTag;
    static private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private Runnable readCode;
    private Runnable syncCode;
    private Runnable runCode;
    private TextView device_type_text,vers_number;
    private Button next_step;
    private int device_bits;

    //END///////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.read_model);

        final Intent queryIntent = getIntent();
        final String queryAction = queryIntent.getAction();
        Intent intent = getIntent();
        String action = intent.getAction();
        Log.d("Mikro ", "queryAction " + queryAction);

        //testing
      mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        Log.d(TAG, "Mikro: mNfcAdapter " + mNfcAdapter);

        if (mNfcAdapter == null) {
            new android.app.AlertDialog.Builder(read_model.this)
                    .setTitle(R.string.app_name)
                    .setIcon(R.mipmap.ic_launcher)
                    .setMessage("\nNFC feature is not found in this device, please try with other device.\n")
                    .setPositiveButton("OK",  new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish();
                        }
                    })
                    .show();
        }

        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        //testing
        //check status of NFC feature on device
        if (!mNfcAdapter.isEnabled()) {
            new AlertDialog.Builder(read_model.this)
                    .setTitle(R.string.app_name)
                    .setIcon(R.mipmap.ic_launcher)
                    .setMessage("\nPlease enable NFC before using the app.\n")
                    .setPositiveButton("OK",  new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                                startActivity(intent);
                            } else {
                                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                                startActivity(intent);
                            }
                            //finish();
                        }
                    })
                    .show();

        }

        device_type_text=(TextView)findViewById(R.id.deviceInfo);
        vers_number=(TextView)findViewById(R.id.deviceVers);
        next_step=(Button)findViewById(R.id.setting);
        device_bits=0;

        readCode = new Runnable() {
            @Override

            public void run() {
                // Do something here on the main thread
                mAction = MainActivity1.READ;
                fillView(mAction);

            }
        };

        syncCode = new Runnable() {
            @Override
            public void run() {
                // Do something here on the main thread
                syncFTM();

            }
        };

        runCode = new Runnable() {
            @Override
            public void run() {
                // Do something here on the main thread
                if(mTag == null)return;
                rfMsg();
                hostMsg();
                enableFTMStatus();

                if(readRun)
                {
                    Log.d(TAG, "Mikro: runCode: read");
                    syncFTM();
                }


            }

        };

        if (Intent.ACTION_SEND.equals(action)) {
            String ftmstatus = intent.getStringExtra(Intent.EXTRA_TEXT);
            Log.d("Mikro ", "ftmstatus:" +  ftmstatus);
        }

       next_step.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
               //testing
               // device_bits=16;
                Intent intent = new Intent(read_model.this, MainActivity.class);
                intent.putExtra("device_bits",Integer.toString(device_bits));
                startActivity(intent);
            }
        });


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
        super.onResume();


        if (mNfcAdapter != null)
        {
            Log.d(TAG, "Mikro ftmstatus : on Resume(NfcAdapter)");

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
        // onResume gets called after this to handle the intent
        Log.d(TAG, "onNewIntent " + intent);
        //setIntent(intent);

        Log.d(TAG, "processIntent " + intent);

        Tag androidTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (androidTag != null) {
            // A tag has been taped
            Toast.makeText(this, "NFC Tag detected!", Toast.LENGTH_SHORT).show();
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

        Log.d(TAG, "Mikro onTagDiscoveryCompleted ");
        mTag = nfcTag;
        if (readRun){
            readRun = false;
           // readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
        } else {
            readRun = true;
           // readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_green));
        }

        if(mTag!=null)
        {
            handler.postDelayed(runCode,50);  //try to read the message first
        }


    }

    public void syncFTM() {

        if (mTag == null) {
            return;
        }

        mBuffer = new byte[8];
        mBuffer [0] = (byte)0x01;
        mBuffer [1] = (byte)0x03;
        mBuffer [2] = (byte)0x00;
        mBuffer [3] = (byte)0x00;
        mBuffer [4] = (byte)0x00;
        mBuffer [5] = (byte)0x08;
        mBuffer [6] = (byte)0x44;
        mBuffer [7] = (byte)0x0c;

        mAction = MainActivity1.SYNC;
        fillView(mAction);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
       // getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //if (id == R.id.action_settings) {
        //    return true;
        //}

        return super.onOptionsItemSelected(item);
    }

    //////////////////////////////////////////////
    //For Runnable part
    //Must use runnable method to run ST25DV FTM Function, if not it will STOP the entire system
    /////////////////////////////////////////////
    public void enableFTMStatus() {
        mAction = MainActivity1.ENABLE;
        fillView(mAction);
    }

    public void rfMsg() {
        mAction = MainActivity1.RFMSG;
        fillView(mAction);
    }
    public void hostMsg() {
        mAction = MainActivity1.HOSTMSG;
        fillView(mAction);
    }

    public void resetFTM() {
        Log.d(TAG, "Mikro: Reset FTM");
        if (mTag == null) {
            return;
        }

        try {
            ((ST25DVTag)mTag).disableMailbox();
        } catch (STException e) {
            switch (e.getError()) {
                case TAG_NOT_IN_THE_FIELD:
                    break;
                case CONFIG_PASSWORD_NEEDED:
                    break;
                //to do error message;
            }
        }

        //put a 10ms delay to wait, and then only enable ftm
        sleep(10);

        try {
            ((ST25DVTag)mTag).enableMailbox();
        } catch (STException e) {
            switch (e.getError()) {
                case TAG_NOT_IN_THE_FIELD:
                    break;
                case CONFIG_PASSWORD_NEEDED:
                    break;
                //to do error message;
            }
        }
        /*if (mftmTag == null) {
            return;
        }

        mAction = ActionCode.RESET;
        fillView(mAction);*/
    }
    public void fillView(MainActivity1 action) {

        new Thread(new ContentView(action)).start();
    }



    private enum MainActivity1 {ENABLE,
        DISABLE,
        RFMSG,
        HOSTMSG,
        SYNC,
        READ,
        RESET
    }

    class ContentView implements Runnable {

        MainActivity1 mAction;


        public ContentView(MainActivity1 action) {
            mAction = action;

        }

        public void run() {

            switch (mAction){
                case SYNC:
                    try {
                        if(((ST25DVTag)mTag).isMailboxEnabled(false))
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
                                Log.d(TAG, "Mikro: Fail to sync");
                                resetFTM();
                                handler.postDelayed(syncCode,DELAY_FTM);

                            }
                        }
                        else
                        {
                            Log.d(TAG, "Mikro: cant run Sync ");
                            runOnUiThread(new Runnable() {
                                public void run() {
                             //       readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));

                                }
                            });
                            readRun = false;
                            reset_ftm();
                            checkMailboxActive();
                        }
                    } catch (STException e) {
                        Log.d(TAG, "Mikro: Error on sync");
                        //readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                        e.printStackTrace();
                    }
                    break;
                case READ:
                    try {
                        if(((ST25DVTag)mTag).isMailboxEnabled(false))
                        {

                            //if mailbox available
                            if(((ST25DVTag)mTag).hasHostPutMsg(true))
                            {

                                mFTMmsg = readMessage();
                                String joined=updateFtmMessage(mFTMmsg);
                                result = crccheck.crcChecker16(joined);
                                Log.d(TAG, "Mikro: Result CRC "+result);
                                if (!result){
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            //  Toast.makeText(MainActivity.this, "FTM is OFF", Toast.LENGTH_SHORT).show();
                                            //Log.d(TAG, "Mikro: sync FTM is off");
                                          // readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                                            new AlertDialog.Builder(read_model.this)
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
                                } else {
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            //readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_green));
                                            next_step.setVisibility(View.VISIBLE);
                                        }
                                    });
                                   // readRun = false;
                                }

                              /*  if(syncRun)
                                {
                                    handler.postDelayed(syncCode,DELAY_FTM);  //wait another 100ms to send code
                                }*/
                            }
                            else
                            {
                                Log.d(TAG, "Mikro: Read delay ");
                                //if not yet get any message, then wait another 100ms to read
                                resetFTM();
                                handler.postDelayed(readCode,DELAY_FTM);
                            }
                        }
                        else
                        {
                            readRun = false;
                            syncRun = false;
                            Log.d(TAG, "Mikro: FTM off ");
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    //syncbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                                    //Toast.makeText(ftmStatus.this, "FTM OFF", Toast.LENGTH_SHORT).show();
                                }
                            });

                        }
                       // reset_ftm();
                    } catch (STException e) {
                        Log.d(TAG, "Mikro: Error on read");
                       // readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                        e.printStackTrace();
                        checkError(e);
                        // handler.postDelayed(readCode,DELAY_FTM);

                    }
                    break;
                case ENABLE:
                    try {
                        ((ST25DVTag)mTag).enableMailbox();
                        checkMailboxActive();
                       // Log.d(TAG, "Mikro: 1 action enable Mailbox ");
                        /*if(mMailboxEnabled){
                            enableftm = false;
                            Log.d(TAG, "Mikro: 1 action disable Mailbox ");
                        }*/

                    } catch (STException e) {
                        e.printStackTrace();
                    }
                    break;
                case DISABLE:
                    try {
                        ((ST25DVTag)mTag).disableMailbox();
                        checkMailboxActive();
                      //  Log.d(TAG, "Mikro: 2 action disable Mailbox ");
                     /*   if(mMailboxEnabled == false){
                            disableftm = false;
                            Log.d(TAG, "Mikro: 2 action enable Mailbox ");
                        }*/

                    } catch (STException e) {
                        e.printStackTrace();
                    }
                    break;

                case RFMSG:
                    try {
                        if(((ST25DVTag)mTag).isMailboxEnabled(false) == true)
                        {
                            Log.d(TAG, "Mikro: RF msg false ");
                            mMailboxEnabled = true;
                            //((ST25DVTag)mTag).refreshMailboxStatus();

                        }
                        else
                        {
                            Log.d(TAG, "Mikro: RF msg true ");
                            mMailboxEnabled = false;
                            mRFMsg = false;
                        }

                        updateFtmStatus();
                    } catch (STException e) {
                        e.printStackTrace();
                    }
                    break;

                case HOSTMSG:
                    try {
                        if(((ST25DVTag)mTag).isMailboxEnabled(false) == true)
                        {
                            mMailboxEnabled = true;
                            //((ST25DVTag)mTag).refreshMailboxStatus();
                            Log.d(TAG, "Mikro: Host msg false ");

                        }
                        else
                        {
                            Log.d(TAG, "Mikro: host msg true ");
                            mMailboxEnabled = false;
                            mHostMsg = false;
                        }

                        updateFtmStatus();

                    } catch (STException e) {
                        e.printStackTrace();
                    }
                    break;
                case RESET:
                    reset_ftm();
                    break;
            }

        }

    }

    private void reset_ftm(){
        //Log.d(TAG, "Mikro: reseting FTM");
        if (mTag == null) {
            return;
        }

        try {
            ((ST25DVTag)mTag).disableMailbox();
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
            ((ST25DVTag)mTag).enableMailbox();
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
            STLog.i("Last cmd failed with error code " + errorCode + ": Try again the same cmd");

            return TRY_AGAIN;
        } else {
            // Transfer failed
            STLog.e(e.getMessage());
            return ERROR;
        }
    }

    private void updateFtmStatus() throws STException {

        mRFMsg = ((ST25DVTag)mTag).hasRFPutMsg(false);
        mHostMsg = ((ST25DVTag)mTag).hasHostPutMsg(false);
        Log.d(TAG, "Mikro: ftm status: "+ mRFMsg+ " " +mHostMsg);
        runOnUiThread(new Runnable() {
            public void run() {
                if(mMailboxEnabled){
                    Log.d(TAG, "Mikro: Already enabled Mailbox ");
                    //enableMailRadioButton.setChecked(true);
                }
                else
                {
                    Log.d(TAG, "Mikro: Already disabled Mailbox ");
                    //enableMailRadioButton.setChecked(false);
                    mRFMsg = false;
                    mHostMsg = false;
                }

            }
        });


    }

    public String updateFtmMessage (byte[] message){
        int no_message = message.length;
        String[] putftmpara = new String[message.length];
        String[] putftmpara1 = new String[message.length];
        int [] temp = new int[0];
        int [] tempLED = new int[0];
        byte tempMsg = 0;
        String msg= "";
        String joined = null,append = null;


        Log.d(TAG, "Mikro: update ftm msg " + no_message);
        for(int x = 0; x <no_message; x++){

            tempMsg = message[x];
        //     Log.d(TAG, "Mikro FTM Message: tempMsg  " +x +" "+tempMsg);
            int hex_int= (int)tempMsg & 0xff;
            String hex_value = Integer.toHexString(hex_int);
         //      Log.d(TAG, "Mikro FTM Message: hex_value  " +x +" "+ hex_value);
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
                 Log.d(TAG, "Mikro: value: "+ a[y]);
            }
            joined = String.join("", a);
        }
        Log.d(TAG, "Mikro: Read CRC "+joined);
        //crc check


        final String finalMsg = msg;
        Log.d(TAG, "Mikro FTM finalMsg: " + finalMsg);

        String device_type = null,ver_num_main=null,ver_num_sub=null;
        int val =0;

        for(int i = 1; i < message.length; i++)
        {
            int value=Integer.parseInt(putftmpara[i],16);
            if (value<=15){
                append= "0"+putftmpara[i];
            }else{
                append=putftmpara[i];
            }
            putftmpara1[i]=append;

            String element = putftmpara1[i];
            Log.d(TAG, "Mikro putftmpara:   " +putftmpara1[i]);

            switch(i){
                case 6:
                    int device_type_no1=Integer.parseInt(element)-30;
                    device_type=Integer.toString(device_type_no1);
                    Log.d(TAG, "Mikro device type " + device_type);
                    break;
                case 8:
                    int device_type_no2=Integer.parseInt(element)-30;
                    Log.d(TAG, "Mikro device_type_no2:" + device_type_no2);
                    device_type=device_type+Integer.toString(device_type_no2);
                    Log.d(TAG, "Mikro device type: " + device_type);

                    break;
                case 12:
                    int device_vers_1=Integer.parseInt(element)-30;
                    ver_num_main=Integer.toString(device_vers_1);
                    Log.d(TAG, "Mikro version number 1:" + ver_num_main);
                    break;
                case 14:
                    int device_vers_2=Integer.parseInt(element)-30;
                    ver_num_main=Integer.toString(device_vers_2)+ver_num_main;
                    Log.d(TAG, "Mikro version number 2:" + ver_num_main);
                    break;
                case 16:
                    int device_vers_sub_1=Integer.parseInt(element)-30;
                    ver_num_sub=Integer.toString(device_vers_sub_1);
                    Log.d(TAG, "Mikro sub version number 1:" + ver_num_sub);
                    break;
                case 18:
                    int device_vers_sub_2=Integer.parseInt(element)-30;
                    ver_num_sub=Integer.toString(device_vers_sub_2)+ver_num_sub;
                    Log.d(TAG, "Mikro sub version number 2:" + ver_num_sub);
                    break;
            }
        }

        if (device_type.equals("25")){
            device_type="RX1000";
        }else if (device_type.equals("40")){
            device_type="RX233";
        }else if (device_type.equals("51")){
            device_type="RX232";
        }
        device_type="Device Type: "+device_type;
        ver_num_main="Device Version: Ver"+ver_num_main+"."+ver_num_sub;
        final String finaldevicetype=device_type,finalversno=ver_num_main;

        runOnUiThread(new Runnable() {
            public void run() {

                device_type_text.setText(finaldevicetype);
                vers_number.setText(finalversno);

            }
        });
        return joined;
    }

    private byte[] readMessage() throws STException {
        int length = ((ST25DVTag)mTag).readMailboxMessageLength();
        byte[] buffer;

        mBuffer = new byte[255];
        Log.d(TAG, "Mikro: Read msg length "+length);
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
            Log.d(TAG, "Mikro: Read msg size "+size);
            tmpBuffer = ((ST25DVTag)mTag).readMailboxMessage(offset, size);
            Log.d(TAG, "Mikro: Read msg tmpBuffer "+tmpBuffer.length);
            if (tmpBuffer.length < (size + 1) || tmpBuffer[0] != 0)
                throw new STException(CMD_FAILED);
            System.arraycopy(tmpBuffer, 1, buffer, offset,
                    tmpBuffer.length - 1);
            offset += tmpBuffer.length - 1;
            Log.d(TAG, "Mikro: Read offset "+offset);
        }

        Log.d(TAG, "Mikro: Read buffer "+buffer);
        return buffer;

    }

    private int sendSimpleData() {

        try {
            Log.d(TAG, "Mikro: check simpleData");
            if (((ST25DVTag)mTag).hasRFPutMsg(true)) {
                Log.d(TAG, "Mikro: RF has message");
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(read_model.this, "RF Mailbox Not Empty", Toast.LENGTH_SHORT).show();
                    }
                });
                resetFTM();
                handler.postDelayed(readCode,DELAY_FTM);  //try to read the message first
                return TRY_AGAIN;
            }
        } catch (final STException e) {
            Log.d(TAG, "Error on sendSimpleData");
            return checkError(e);
        }

        byte response;

        try {
            Log.d(TAG, "Mikro: send simple data(response) ");
            response = ((ST25DVTag)mTag).writeMailboxMessage(mBuffer.length, mBuffer);
            Log.d(TAG, "Mikro: send simple data(response) "+response);
            if(response != 0x00){

                return ERROR;
            }
        } catch (final STException e) {
            Log.d(TAG, "Error on writeMailboxMessage");
            return checkError(e);
        }

        return OK;

    }

    void checkMailboxActive()
    {
        //enableMailRadioButton = findViewById(R.id.enableMailRB);

        try {
            mMailboxEnabled = ((ST25DVTag)mTag).isMailboxEnabled(false);
            runOnUiThread(new Runnable() {
                public void run() {
                    if(mMailboxEnabled)
                    {
                        Toast.makeText(read_model.this, "FTM is OFF, now turning on", Toast.LENGTH_SHORT).show();
                         Log.d(TAG, "Mikro ftm: FTM is on");
                         readRun = true;
                         syncFTM();
                    }
                }
            });
        } catch (STException e) {
            e.printStackTrace();
            mMailboxEnabled=false;
        }



    }
}

