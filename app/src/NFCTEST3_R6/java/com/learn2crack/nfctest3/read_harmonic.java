package com.learn2crack.rx1000;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.STLog;
import com.st.st25sdk.TagHelper;
import com.st.st25sdk.type5.st25dv.ST25DVTag;

import java.math.BigInteger;
import java.text.DecimalFormat;

import static android.os.SystemClock.sleep;
import static com.st.st25sdk.STException.STExceptionCode.CMD_FAILED;


public class read_harmonic extends BaseActivity implements TagDiscovery.onTagDiscoveryCompletedListener {

    private final int ERROR     = -1;
    private final int TRY_AGAIN = 0;
    private final int OK        = 1;

    private final int DELAY_FTM = 50;
    //fast 50

    private ActionCode mAction;

    // To Do compute exactly and dynamically
    private int mMaxPayloadSizeTx = 220;
    private int mMaxPayloadSizeRx = 32;

    private int mOffset;


    private byte[] mFTMmsg;
    private byte[] mBuffer;

    static private NFCTag mftmTag;
    private static final String TAG = MainActivity.class.getSimpleName();

    static private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;


    Handler handler = new Handler();
    private Button readbtn;

    private TextView h2, h3, h4,h5,h6,h7,h8,h9,h10,h11,h12,h13,h14,h15;
    private TextView autoText, manualText,alarmText;
    private boolean enableftm = false;
    private boolean disableftm = false;
    private boolean readRun = false;
    private boolean syncRun = false;
    private boolean mMailboxEnabled = false;
    private boolean result = false;
    private Runnable readCode;
    private Runnable syncCode;
    private Runnable runCode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout contentFrameLayout = (FrameLayout) findViewById(R.id.content_frame);
        getLayoutInflater().inflate(R.layout.harmonic_layout, contentFrameLayout);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        Log.d(TAG, "Mikro: mNfcAdapter " + mNfcAdapter);
        // boolean status = checkMailboxActive();
        // Log.d(TAG, "Mikro: checkMailboxActive " + status);
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // harmonic output display

        h2= (TextView) findViewById(R.id.hmn2);
        h3= (TextView) findViewById(R.id.hmn3);
        h4= (TextView) findViewById(R.id.hmn4);
        h5= (TextView) findViewById(R.id.hmn5);
        h6= (TextView) findViewById(R.id.hmn6);
        h7= (TextView) findViewById(R.id.hmn7);
        h8= (TextView) findViewById(R.id.hmn8);
        h9= (TextView) findViewById(R.id.hmn9);
        h10= (TextView) findViewById(R.id.hmn10);
        h11= (TextView) findViewById(R.id.hmn11);
        h12= (TextView) findViewById(R.id.hmn12);
        h13= (TextView) findViewById(R.id.hmn13);
        h14= (TextView) findViewById(R.id.hmn14);
        h15= (TextView) findViewById(R.id.hmn15);

        readCode = new Runnable() {
            @Override

            public void run() {
                // Do something here on the main thread
                mAction = ActionCode.READ;
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

                if(syncRun)
                {
                    resetFTM();
                    syncFTM();
                }

            }
        };

       readbtn = findViewById(R.id.btn_read_harmonic);

        readbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();
                if(syncRun)
                {
                    syncRun = false;
                    //syncbtn.setBackgroundColor(getResources().getColor(R.color.colorGray));
                    readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                    handler.removeCallbacks(syncCode);

                }
                else {
                    syncRun = true;
                    readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_green));
                    // syncbtn.setBackgroundColor(getResources().getColor(R.color.colorGreen));

                }
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



        // if (mNfcAdapter != null)
        // {
        Log.d(TAG, "Mikro: on Resume(NfcAdapter)");
        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null /*nfcFiltersArray*/, null /*nfcTechLists*/);

    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
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
        Log.d(TAG, "Mikro: on Resume(NfcAdapter): " + mftmTag);

        if(mftmTag!=null)
        {
            handler.postDelayed(runCode,10);  //try to read the message first
        }

    }


    public void syncFTM() {

        if (mftmTag == null) {
            return;
        }

        mBuffer = new byte[8];
        mBuffer [0] = (byte)0x01;
        mBuffer [1] = (byte)0x03;
        mBuffer [2] = (byte)0x00;
        mBuffer [3] = (byte)0x1f;
        mBuffer [4] = (byte)0x00;
        mBuffer [5] = (byte)0x0e;
        mBuffer [6] = (byte)0xf5;
        mBuffer [7] = (byte)0xc8;

        mAction = ActionCode.SYNC;
        fillView(mAction);
    }

    public void resetFTM() {
        Log.d(TAG, "Mikro: Reset FTM");
        if (mftmTag == null) {
            return;
        }

        try {
            ((ST25DVTag)mftmTag).disableMailbox();
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
            ((ST25DVTag)mftmTag).enableMailbox();
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


    public void fillView(ActionCode action) {

        new Thread(new ContentView(action)).start();
    }



    private enum ActionCode {
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
                                    readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));

                                }
                            });
                            syncRun = false;
                            checkMailboxActive();
                        }
                    } catch (STException e) {
                        Log.d(TAG, "Mikro: Error on sync");
                        readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
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
                                String joined=updateFtmMessage(mFTMmsg);
                                result = crcChecker16(joined);
                                Log.d(TAG, "Mikro: Result CRC "+result);
                                if (!result){
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            //  Toast.makeText(MainActivity.this, "FTM is OFF", Toast.LENGTH_SHORT).show();
                                            //Log.d(TAG, "Mikro: sync FTM is off");
                                            readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                                            new AlertDialog.Builder(read_harmonic.this)
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
                                            readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_green));
                                            Log.d(TAG, "Mikro: CRC OK " );

                                        }
                                    });
                                }

                                if(syncRun)
                                {
                                    handler.postDelayed(syncCode,DELAY_FTM);  //wait another 100ms to send code
                                }
                            }
                            else
                            {
                                Log.d(TAG, "Mikro: Read delay ");
                                //if not yet get any message, then wait another 100ms to read
                                resetFTM();
                                handler.postDelayed(syncCode,DELAY_FTM);
                            }
                        }
                        else
                        {

                            readRun = false;
                            syncRun = false;
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    //syncbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                                    Toast.makeText(read_harmonic.this, "FTM OFF", Toast.LENGTH_SHORT).show();
                                }
                            });


                        }
                    } catch (STException e) {
                        Log.d(TAG, "Mikro: Error on read");
                        readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                        e.printStackTrace();
                        checkError(e);
                        // handler.postDelayed(readCode,DELAY_FTM);

                    }
                    break;
                case RESET:
                    resetFTM();
                    break;


            }
        }
    }



    private byte[] readMessage() throws STException {
        int length = ((ST25DVTag)mftmTag).readMailboxMessageLength();
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
            tmpBuffer = ((ST25DVTag)mftmTag).readMailboxMessage(offset, size);
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
            if (((ST25DVTag)mftmTag).hasRFPutMsg(true)) {
                Log.d(TAG, "Mikro: RF has message");
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(read_harmonic.this, "RF Mailbox Not Empty", Toast.LENGTH_SHORT).show();
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
            response = ((ST25DVTag)mftmTag).writeMailboxMessage(mBuffer.length, mBuffer);
            if(response != 0x00){

                return ERROR;
            }
        } catch (final STException e) {
            Log.d(TAG, "Error on writeMailboxMessage");
            return checkError(e);
        }

        return OK;

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



    public String updateFtmMessage (byte[] message){
        int no_message = message.length;
        String[] putftmpara = new String[message.length];
        String[] putftmpara1 = new String[message.length];
        int [] temp = new int[0];
        int [] tempLED = new int[0];
        byte tempMsg = 0;
        String msg= "";
        String joined = null,append = null;


        //Log.d(TAG, "Mikro: update ftm msg " + no_message);
        for(int x = 0; x <no_message; x++){

            tempMsg = message[x];
            // Log.d(TAG, "Mikro FTM Message: tempMsg  " +x +" "+tempMsg);
            int hex_int= (int)tempMsg & 0xff;
            String hex_value = Integer.toHexString(hex_int);
            //   Log.d(TAG, "Mikro FTM Message: hex_value  " +x +" "+ hex_value);
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
        Log.d(TAG, "Mikro: Read CRC "+joined);
        //crc check


        final String finalMsg = msg;
        Log.d(TAG, "Mikro FTM finalMsg: " + finalMsg);

        String harmonic2 = null, harmonic3=null,harmonic4=null,harmonic5=null,harmonic6=null,harmonic7=null,harmonic8=null;
        String harmonic9=null,  harmonic10=null,harmonic11=null, harmonic12=null,harmonic13=null,harmonic14=null,harmonic15=null;
        int val =0;

        for(int i = 1; i < message.length; i+=2)
        {
            int value=Integer.parseInt(putftmpara[i],16);
            if (value<=15){
                append= "0"+putftmpara[i];
            }else{
                append=putftmpara[i];
            }
            putftmpara1[i]=append;

            int value1=Integer.parseInt(putftmpara[i+1],16);
            if (value1<=15){
                append= "0"+putftmpara[i+1];
            }else{
                append=putftmpara[i+1];
            }
            putftmpara1[i+1]=append;

            String element = putftmpara1[i]+putftmpara1[i+1];
            Log.d(TAG, "Mikro putftmpara:   " +putftmpara1[i] +" "+putftmpara1[i+1]);

            switch(i){
                case 3:
                    val = Integer.parseInt(element, 16);
                    harmonic2= val + " %";
                    break;
                case 5:
                    val = Integer.parseInt(element, 16);
                    harmonic3= val + " %";
                    break;
                case 7:
                    val = Integer.parseInt(element, 16);
                    harmonic4= val + " %";
                    break;
                case 9:
                    val = Integer.parseInt(element, 16);
                    harmonic5= val + " %";
                    break;
                case 11:
                    val = Integer.parseInt(element, 16);
                    harmonic6= val + " %";
                    break;
                case 13:
                    val = Integer.parseInt(element, 16);
                    harmonic7= val + " %";
                    break;
                case 15:
                    val = Integer.parseInt(element, 16);
                    harmonic8= val + " %";
                    break;
                case 17:
                    val = Integer.parseInt(element, 16);
                    harmonic9= val + " %";
                    break;
                case 19:
                    val = Integer.parseInt(element, 16);
                    harmonic10= val + " %";
                    break;
                case 21:
                    val = Integer.parseInt(element, 16);
                    harmonic11= val + " %";
                    break;
                case 23:
                    val = Integer.parseInt(element, 16);
                    harmonic12= val + " %";
                    break;
                case 25:
                    val = Integer.parseInt(element, 16);
                    harmonic13= val + " %";
                    break;
                case 27:
                    val = Integer.parseInt(element, 16);
                    harmonic14= val + " %";
                    break;
                case 29:
                    val = Integer.parseInt(element, 16);
                    Log.d(TAG, "Mikro FTM harmonic 15: " + val);
                    harmonic15= val + " %";
                    break;
                }

            }

        final String finalharmonic2= harmonic2,finalharmonic3= harmonic3,finalharmonic4= harmonic4,finalharmonic5= harmonic5,finalharmonic6= harmonic6,finalharmonic7= harmonic7, finalharmonic14= harmonic14;
        final String finalharmonic8= harmonic8,finalharmonic9= harmonic9,finalharmonic13= harmonic13,finalharmonic10= harmonic10,finalharmonic11= harmonic11,finalharmonic12= harmonic12, finalharmonic15= harmonic15;

        runOnUiThread(new Runnable() {
            public void run() {
                h2.setText(finalharmonic2);
                h3.setText(finalharmonic3);
                h4.setText(finalharmonic4);
                h5.setText(finalharmonic5);
                h6.setText(finalharmonic6);
                h7.setText(finalharmonic7);
                h8.setText(finalharmonic8);
                h9.setText(finalharmonic9);
                h10.setText(finalharmonic10);
                h11.setText(finalharmonic11);
                h12.setText(finalharmonic12);
                h13.setText(finalharmonic13);
                h14.setText(finalharmonic14);
                h15.setText(finalharmonic15);
            }
        });
        return joined;
    }

    public boolean checkMailboxActive()
    {

        //turning on FTM mode
        try {
            ((ST25DVTag)mftmTag).enableMailbox();
            {
                runOnUiThread(new Runnable() {

                    public void run() {
                        Toast.makeText(read_harmonic.this, "NFC is OFF, now turning ON.", Toast.LENGTH_SHORT).show();

                    }

                });

            }

            Log.d(TAG, "Mikro: turning on FTM mode");
            syncRun = true;
            readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_green));
            syncFTM();
        } catch (STException e) {
            e.printStackTrace();
        }

        //check mailbox after turn on
        try {
            mMailboxEnabled = ((ST25DVTag)mftmTag).isMailboxEnabled(false);
            runOnUiThread(new Runnable() {
                public void run() {
                    if(mMailboxEnabled)
                    {
                        Log.d(TAG, "Mikro Main: Success to turn ftm on");
                    }
                    else
                    {
                        Log.d(TAG, "Mikro Main: Failed to turn ftm ON");
                    }

                }

            });
        } catch (STException e) {
            e.printStackTrace();
            mMailboxEnabled=false;
        }
        return enableftm;
    }

    public boolean crcChecker16(String concatstring) {
        int reg = 0;
        boolean check = false;
        boolean bit = false;
        int poly = 0;
        int i = 0;
        int j = 0;
        int LoopCount = 0;
        byte temp = 0;

        Log.d(TAG, "\nMikro: Received is: "+ concatstring );
        BigInteger v = new BigInteger(concatstring, 16);
        byte[] array = v.toByteArray();
        for (byte d : array) {
            System.out.format("%x ", d);
        }

        LoopCount = array.length - 3;
        int crcHigh = array[array.length - 1] & 0xff;
        int crcLow = array[array.length - 2] & 0xff;
        //Log.d(TAG, "Mikro crcHigh of received: " + Integer.toHexString(crcHigh));
        //Log.d(TAG, "Mikro crcLow of received: " + Integer.toHexString(crcLow));

        reg = 0xFFFF;

        for (j = 0; (j < LoopCount + 1); j++) {
            temp = array[j];
            //System.out.println(temp + " ");
            poly = 0xA001;
            reg = reg ^ (temp & 0xff);

            for (i = 0; (i < 8); i++) {
                bit = (reg & 1) != 0; //check bit whether to xor
                reg = reg / 2; // shift byte to the right by 1 step
                if (bit == true) {
                    reg = reg ^ poly;
                    //System.out.println("reg: " + Integer.toHexString(reg) + " ");
                }
            }
        }

        int crcL = reg & 0xFF;
        int crcH = reg / 256;
        // Log.d(TAG, "Mikro crc high " + crcH);
        //Log.d(TAG, "Mikro crc low " + crcL);
        if (crcLow == crcL) {
            //System.out.println("True crcLow");
            if (crcHigh == crcH) {
                //System.out.println("True crcHigh");
                check = true;
            } else {
                //System.out.println("Wrong crcHigh");
            }
        } else {
            //System.out.println("Wrong crcLow " + crcLow + " " + Integer.toHexString(crcL));
        }

        return check;
    }

}