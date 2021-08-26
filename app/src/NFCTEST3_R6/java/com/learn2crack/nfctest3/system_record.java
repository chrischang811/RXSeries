package com.learn2crack.rx1000;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.os.SystemClock.sleep;
import static com.st.st25sdk.STException.STExceptionCode.CMD_FAILED;


public class system_record extends BaseActivity implements TagDiscovery.onTagDiscoveryCompletedListener {

    private final int ERROR     = -1;
    private final int TRY_AGAIN = 0;
    private final int OK        = 1;

    private final int DELAY_FTM = 50;
    //fast 50

    private ActionCode mAction;

    // To Do compute exactly and dynamically
    private int mMaxPayloadSizeTx = 220;
    private int mMaxPayloadSizeRx = 32;
    private int repeat,count,load=0;

    private int mOffset;
    LinearLayout linearLayout;

    private byte[] mFTMmsg;
    private byte[] mBuffer,mBuffer1;
    ProgressDialog dialog;

    static private NFCTag mftmTag;
    private static final String TAG = MainActivity.class.getSimpleName();
    CRC16checker crccheck=new CRC16checker();

    static private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private String crc_low,crc_high,reg_quantity;
    private int curr_reg,record_int=0;

    Handler handler = new Handler();
    private Button readbtn;

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
       // setContentView(R.layout.system_record);
        FrameLayout contentFrameLayout = (FrameLayout) findViewById(R.id.content_frame);
        getLayoutInflater().inflate(R.layout.system_record, contentFrameLayout);
        linearLayout = findViewById(R.id.linear_layout);

       /* Toolbar toolbar= findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);*/

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        Log.d(TAG, "Mikro: mNfcAdapter " + mNfcAdapter);
        // boolean status = checkMailboxActive();
        // Log.d(TAG, "Mikro: checkMailboxActive " + status);
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        readCode = new Runnable() {
            @Override

            public void run() {
                Log.d(TAG, "Mikro readcode");
                // Do something here on the main thread
                mAction = ActionCode.READ;
                fillView(mAction);

            }
        };

        syncCode = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Mikro syncode");
                // Do something here on the main thread
                syncFTM();

            }
        };

        runCode = new Runnable() {
            @Override

            public void run() {
                // Do something here on the main thread
                Log.d(TAG, "Mikro runcode");
                if(syncRun)
                {
                    if (repeat==0) {
                        String request = "010320";
                        reg_quantity="00";
                        request=request+reg_quantity+"0014";
                        Log.d(TAG, "Mikro request before: " + request);
                        int[] ans = crccheck.getCRC(request);
                        crc_low = Integer.toHexString(ans[0]);
                        crc_high = Integer.toHexString(ans[1]);
                        Log.d(TAG, "Mikro first CRC: " + crc_high + " " + crc_low);
                        repeat++;
                    }
                    syncFTM();
                }

            }
        };

        readbtn = findViewById(R.id.read_system_record);

        readbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();
                repeat = 0;
                count=0;
                curr_reg=0;
                load=1;
                if(syncRun)
                {
                    syncRun = false;
                    readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                    handler.removeCallbacks(syncCode);
                }
                else {

                    syncRun = true;
                    readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_green));
                    linearLayout.removeAllViews();
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
            if (load>0) {
                dialog = ProgressDialog.show(system_record.this, "",
                        "Loading data. Please don't remove your phone from device.", true);
            }

            handler.postDelayed(runCode, 100);  //try to read the message first

        }

    }


    public void syncFTM() {

        if (mftmTag == null) {
            return;
        }
        Log.d(TAG, "Mikro pumpcode");
        mBuffer = new byte[8];
        mBuffer[0] = (byte) 0x01;
        mBuffer[1] = (byte) 0x03;
        mBuffer[2] = (byte) 0x20;
        mBuffer[3] = (byte) Integer.parseInt(reg_quantity,16);
        mBuffer[4] = (byte) 0x00;
        mBuffer[5] = (byte) 0x14;
        mBuffer[6] = (byte) Integer.parseInt(crc_high,16);
        mBuffer[7] = (byte) Integer.parseInt(crc_low,16);
        Log.d(TAG, "Mikro byte crc: " + mBuffer[6]+mBuffer[7]);

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
                            //if mailbox available
                            if (sendSimpleData() == OK) {
                                Log.d(TAG, "Mikro: run Sync delay ");
                                handler.postDelayed(readCode, DELAY_FTM);
                            } else {
                                Log.d(TAG, "Mikro: Fail to sync");
                                resetFTM();
                                handler.postDelayed(syncCode, DELAY_FTM);

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
                        dialog.dismiss();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                new android.app.AlertDialog.Builder(system_record.this)
                                        .setTitle(R.string.app_name)
                                        .setIcon(R.mipmap.ic_launcher)
                                        .setMessage("\nNFC not detected by device, please try again.\n")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                //finish();
                                            }
                                        })
                                        .show();
                                readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                            }
                        });
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
                                if (joined.equals("false")){
                                    Log.d(TAG, "Mikro exit execution");
                                    repeat=0;
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));

                                        }
                                    });
                                    break;
                                }
                                result = crccheck.crcChecker16(joined);
                                Log.d(TAG, "Mikro: Result CRC " + result);

                                if (!result){
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            //  Toast.makeText(MainActivity.this, "FTM is OFF", Toast.LENGTH_SHORT).show();
                                            //Log.d(TAG, "Mikro: sync FTM is off");
                                            readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                                            new AlertDialog.Builder(system_record.this)
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
                                    if (repeat!=0) {
                                        String request = "010320";
                                        int reg_value = 20;
                                        curr_reg = curr_reg + reg_value;
                                        reg_quantity = Integer.toHexString(curr_reg);
                                        Log.d(TAG, "Mikro Register Quantity: " + reg_quantity);
                                        request = request + reg_quantity + "0014";
                                        Log.d(TAG, "Mikro request before: " + request);
                                        int[] ans = crccheck.getCRC(request);
                                        crc_low = Integer.toHexString(ans[0]);
                                        crc_high = Integer.toHexString(ans[1]);
                                        Log.d(TAG, "Mikro CRC: " + crc_high + " " + crc_low);
                                        //readRun = false;
                                        syncRun = false;
                                        repeat++;
                                        syncFTM();
                                        if (repeat == 6) {
                                            load=0;
                                            repeat = 0;
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                                                    dialog.dismiss();
                                                }
                                            });
                                        }
                                    }
                                }
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
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    //syncbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                                    Toast.makeText(system_record.this, "FTM OFF", Toast.LENGTH_SHORT).show();
                                }
                            });


                        }
                    } catch (STException e) {
                        Log.d(TAG, "Mikro: Error on read");
                        readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                        e.printStackTrace();
                        checkError(e);
                        dialog.dismiss();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                new android.app.AlertDialog.Builder(system_record.this)
                                        .setTitle(R.string.app_name)
                                        .setIcon(R.mipmap.ic_launcher)
                                        .setMessage("\nNFC not detected by device, please try again.\n")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                //finish();
                                            }
                                        })
                                        .show();
                                readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_red));
                            }
                        });
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
                        Toast.makeText(system_record.this, "RF Mailbox Not Empty", Toast.LENGTH_SHORT).show();
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

        byte response = 0;

        try {
            Log.d(TAG, "Mikro: send simple data(response) ");

            response = ((ST25DVTag) mftmTag).writeMailboxMessage(mBuffer.length, mBuffer);

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
        byte tempMsg = 0;
        String msg= "";
        String joined = null, append=null;
        DecimalFormat form = new DecimalFormat("0.00");


        HashMap<Integer,String> recDes= new HashMap<Integer, String>();
        recDes.put(0,"Setting change in Voltage System");
        recDes.put(1,"Setting change in Frequency System");
        recDes.put(2,"Setting change in System ID");
        recDes.put(3,"Setting change in Input Control");
        recDes.put(10,"Primary CT");
        recDes.put(11,"Setting change in Set Cos");
        recDes.put(12,"Setting change in Smallest Cap");
        recDes.put(13,"Setting change in Sensitivity");
        recDes.put(14,"Setting change in Reconnection Time");
        recDes.put(15,"Setting change in Switch Program");
        recDes.put(20,"Setting change in Rate Step 1");
        recDes.put(21,"Setting change in Rate Step 2");
        recDes.put(22,"Setting change in Rate Step 3");
        recDes.put(23,"Setting change in Rate Step 4");
        recDes.put(24,"Setting change in Rate Step 5");
        recDes.put(25,"Setting change in Rate Step 6");
        recDes.put(26,"Setting change in Rate Step 7");
        recDes.put(27,"Setting change in Rate Step 8");
        recDes.put(28,"Setting change in Rate Step 9");
        recDes.put(29,"Setting change in Rate Step 10");
        recDes.put(30,"Setting change in Rate Step 11");
        recDes.put(31,"Setting change in Rate Step 12");
        recDes.put(32,"Setting change in Rate Step 13");
        recDes.put(33,"Setting change in Rate Step 14");
        recDes.put(34,"Setting change in Rate Step 15");
        recDes.put(35,"Setting change in Rate Step 16");
        recDes.put(36,"Setting change in Rate Step 17");
        recDes.put(37,"Setting change in Rate Step 18");
        recDes.put(38,"Setting change in Rate Step 19");
        recDes.put(39,"Setting change in Rate Step 20");
        recDes.put(40,"Setting change in Rate Step 21");
        recDes.put(41,"Setting change in Rate Step 22");
        recDes.put(42,"Setting change in Rate Step 23");
        recDes.put(43,"Setting change in Rate Step 24");
        recDes.put(44,"Setting change in Rate Step 25");
        recDes.put(45,"Setting change in Rate Step 26");
        recDes.put(46,"Setting change in Rate Step 27");
        recDes.put(47,"Setting change in Rate Step 28");
        recDes.put(48,"Setting change in Rate Step 29");
        recDes.put(49,"Setting change in Rate Step 30");
        recDes.put(50,"Setting change in Rate Step 31");
        recDes.put(51,"Setting change in Rate Step 32");

        recDes.put(60,"Setting change in THD Voltage");
        recDes.put(61,"Setting change in THD Current");
        recDes.put(62,"Setting change in Undercurrent");
        recDes.put(63,"Setting change in Overcurrent");
        recDes.put(64,"Setting change in Undervoltage");
        recDes.put(65,"Setting change in Overvoltage");
        recDes.put(80,"Setting change in Communication");
        recDes.put(81,"Setting change in Remote Set");
        recDes.put(82,"Setting change in Communication Address");
        recDes.put(83,"Setting change in Baudrate");
        recDes.put(84,"Setting change in Parity");
        recDes.put(85,"Setting change in Number of Communication Stop Bit");
        recDes.put(90,"Setting change in Program Alarm");
        recDes.put(91,"Setting change in Program Output");

        recDes.put(92,"General Start");
        recDes.put(93,"Master Output Alarm");
        recDes.put(94,"Slave Output Alarm");
        recDes.put(95,"Master Output Fan");
        recDes.put(96,"Slave Output Fan");
        recDes.put(97,"Alarm Alert");
        recDes.put(98,"Setting Change through Communication");
        recDes.put(99,"Delete Step On Timer");
        recDes.put(100,"Delete Step on Counter");
        recDes.put(101,"Lock Setting Change");
        recDes.put(102,"Unlock Setting Change");
        recDes.put(103,"Default Setting");
        recDes.put(104,"Step Output auto Off");
        recDes.put(105,"Auto to manual mode");
        recDes.put(106,"Manual to auto mode");
        recDes.put(107,"Clear alarm");
        recDes.put(108,"Clear alarm step error");
        recDes.put(109,"Clear alarm cap error");
        recDes.put(110,"Clear alarm step timer");
        recDes.put(111,"Clear alarm step error");

        HashMap<Integer,String> desF2F18 = new HashMap<Integer, String>();
        desF2F18.put(0,"THD Voltage");
        desF2F18.put(1,"THD Current");
        desF2F18.put(2,"Undercurrent");
        desF2F18.put(3,"Overcurrent");
        desF2F18.put(4,"Undervoltage");
        desF2F18.put(5,"Overvoltage");
        desF2F18.put(6,"Capacitor Size Error");
        desF2F18.put(7,"Undercompensate");
        desF2F18.put(8,"Overcompensate");
        desF2F18.put(9,"Step Error");
        desF2F18.put(10,"No Voltage Release");
        desF2F18.put(11,"CT Polarity Error");
        desF2F18.put(12,"Clock Lost");
        desF2F18.put(13,"EEPROM Error");
        desF2F18.put(14,"Step Timer Alarm");
        desF2F18.put(15,"Step Counter Alarm");

        HashMap<Integer,String> desRate = new HashMap<Integer, String>();
        desRate.put(0,"Disable");
        desRate.put(1,"1");
        desRate.put(2,"2");
        desRate.put(3,"3");
        desRate.put(4,"4");
        desRate.put(5,"6");
        desRate.put(7,"8");
        desRate.put(8,"12");
        desRate.put(9,"16");
        desRate.put(10,"Alarm Output");
        desRate.put(11,"Fan Output");

        HashMap<Integer,String> desYN = new HashMap<Integer, String>();
        desYN.put(0,"No");
        desYN.put(1,"Yes");

        HashMap<Integer,String> desOnOff = new HashMap<Integer, String>();
        desOnOff.put(0,"Off");
        desOnOff.put(1,"On");

        HashMap<Integer,String> desRC2 = new HashMap<Integer, String>();
        desRC2.put(0,"Standalone unit");
        desRC2.put(1,"Master unit");
        desRC2.put(2,"Slave unit");

        HashMap<Integer,String> desRC15 = new HashMap<Integer, String>();
        desRC15.put(0,"Manual");
        desRC15.put(1,"Rational");
        desRC15.put(2,"Automatic");
        desRC15.put(3,"Four-quadrant");

        HashMap<Integer,String> desRC83 = new HashMap<Integer, String>();
        desRC83.put(0,"2400");
        desRC83.put(1,"4800");
        desRC83.put(2,"9600");
        desRC83.put(3,"19200");
        desRC83.put(4,"38400");

        HashMap<Integer,String> desRC84 = new HashMap<Integer, String>();
        desRC84.put(0,"None");
        desRC84.put(1,"Even");
        desRC84.put(2,"Odd");

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
            int y;
            for (y = 0; y < putftmpara.length; y++) {
                int value=Integer.parseInt(putftmpara[y],16);
                if (value<=15){
                    append= "0"+putftmpara[y];
                }else{
                    append=putftmpara[y];
                }
                a[y]=append;

            }
            joined = String.join("", a);
            Log.d(TAG, "Mikro: value: "+ y);
        }
        Log.d(TAG, "Mikro: Read CRC :"+joined);
        String first=joined.substring(6,(joined.length()-4));
        // Log.d(TAG, "Mikro subtring: "+ joined.length()+"   "+first);

        String [] record=splitString(first,16);
        Log.d(TAG, "Mikro record number :"+record.length);
        for (int x=0;x<record.length;x++) {
            Log.d(TAG, "Mikro split string "+ x +" "+ record[x]);
        }

        final String finalMsg = msg;
        //Log.d(TAG, "Mikro FTM finalMsg: " + finalMsg);

        String recordcode = null, year=null,month=null,date=null,hour=null,minute=null,recordvalue=null,recordvalue_tmp=null,symb=null;
        int val =0;
        double cos=0.00;

        for(int i = 0; i < record.length; i++)
        {
            count++;
            // Log.d(TAG, "Mikro words: "+i);
            for (int j=0;j<8;j++){
                if (record[i].equals("ffffffffffffffff")){
                    joined = "false";
                    return joined;
                }
                //record[0]="00140313100000ff";
               // record[1]="64ff03131000ffff";

                String [] words=splitString(record[i],2);
                String element = words[j];

                //  Log.d(TAG, "Mikro words "+j+" :" +words[j]);

                switch(j) {
                    case 0:
                        val = Integer.parseInt(element, 16);
                        record_int=val;
                        //Log.d(TAG, "Mikro case "+record_int);
                        //String recordcode_ini = Integer.toString(val) ;
                        recordcode=recDes.get(val);
                        if (val==12) {
                            symb= " VAR";
                        } else if(val==13){
                            symb = " Sec/step";
                        } else if(val==14){
                            symb = " Sec";
                        } else if(val>=60&&val<=63){
                            symb = " %";
                        }else if(val==65||val==64){
                            symb = " V";
                        } else {
                            symb=" ";
                        }
                        break;
                    case 1:
                        val = Integer.parseInt(element, 16);
                        int year_int = val + 2000;
                        year=Integer.toString(year_int);
                        break;
                    case 2:
                        val = Integer.parseInt(element, 16);
                        month = Integer.toString(val);
                        break;
                    case 3:
                        val = Integer.parseInt(element, 16);
                        date = Integer.toString(val) ;
                        break;
                    case 4:
                        val = Integer.parseInt(element, 16);
                        hour = Integer.toString(val);
                        break;
                    case 5:
                        val = Integer.parseInt(element, 16);
                        minute = Integer.toString(val);
                        if (val<10){
                            minute='0'+minute;
                        }
                        break;
                    case 6:
                        recordvalue_tmp= element;
                        break;
                    case 7:
                        //val = Integer.parseInt(element, 16);
                        String actual_recode=" ";
                        recordvalue = recordvalue_tmp + element;
                        val = Integer.parseInt(recordvalue, 16);
                        switch(record_int){
                            case 0:
                                if (val==1){
                                    actual_recode="L-L";
                                }else if(val==0){
                                    actual_recode="L-N";
                                }
                                break;
                            case 1:
                                if (val==1){
                                    actual_recode="60Hz";
                                }else if(val==0){
                                    actual_recode="50Hz";
                                }
                                break;
                            case 2:
                                actual_recode=desRC2.get(val);
                                break;
                            case 3: case 80: case 81:

                                actual_recode=desYN.get(val);
                                break;
                            case 10: case 13: case 14: case 60: case 61: case 62: case 63: case 65: case 82:
                                actual_recode=Integer.toString(val);
                                break;
                            case 11:
                                //round to 0.99-0.88 IND/CAP
                                val = Integer.parseInt(element, 16);
                                double cos_value=val;
                                if (val>=0&&val<=19){
                                    cos=(80+cos_value)/100;
                                    symb=" Ind";
                                } else if(val>=21&&val<=40){
                                    cos=(120-cos_value)/100;
                                    symb=" Cap";
                                } else if (val==20){
                                    cos=1;
                                }
                                actual_recode=Double.toString(cos);
                                break;
                            case 12:
                                if(val==0){
                                    actual_recode="Auto";
                                    symb= " ";
                                } else {
                                    val=val*100;
                                    actual_recode=Integer.toString(val);
                                }
                                break;
                            case 15:
                                actual_recode=desRC15.get(val);
                                break;
                            case 64:
                                val=val/10;
                                actual_recode=Integer.toString(val);
                                break;
                            case 83:
                                actual_recode=desRC83.get(val);
                                break;
                            case 84:
                                actual_recode=desRC84.get(val);
                                break;
                            case 85:
                                if (val==1){
                                    actual_recode="2 bits";
                                }else if(val==0){
                                    actual_recode="1 bits";
                                }
                                break;
                            case 90: case 91:
                                String outputbit="";
                                outputbit = Integer.toBinaryString(0x10000 | val ).substring(1);
                                Log.d(TAG, "Mikro FTM prgrammable output: " + outputbit);
                                char ch1 [] = outputbit.toCharArray();
                                int[] tempOutput = new int [ch1.length];
                                for (int y=0;y<ch1.length;y++){
                                    int m=(ch1.length-1)-y;
                                    tempOutput[m]=Integer.parseInt(""+ch1[y]);
                                    //Log.d(TAG, "Mikro FTM prgrammable output by bit "+m+": " + tempOutput[m]);
                                    if (tempOutput[m]==1){
                                        String temp_record=desF2F18.get(m);
                                        actual_recode=actual_recode+"\n"+temp_record;
                                     //   Log.d(TAG, "Mikro FTM alarm output: " + actual_recode);
                                    }
                                }
                                break;
                            case 93: case 94: case 95: case 96:
                                if (val%2==0){
                                    actual_recode="Off";
                                } else if(val%2==1){
                                    actual_recode="On";
                                }
                                //actual_recode=desOnOff.get(val);
                            case 97: case 107:
                                val = Integer.parseInt(element, 16);
                                actual_recode=desF2F18.get(val-1);
                                Log.d(TAG, "Mikro FTM alarm alert: " + actual_recode);
                                break;
                            case 98:
                                actual_recode="01"+recordvalue_tmp+" to 01"+element;
                                break;
                            case 99: case 100: case 108: case 110: case 111:
                                val = Integer.parseInt(element, 16);
                                actual_recode=Integer.toString(val);
                                if (val==255){
                                    actual_recode="All";
                                }
                                break;
                            case 109:
                                if (val==1){
                                    actual_recode="No Suitable step cap";
                                }else if(val==0){
                                    actual_recode="Smallest cap over size";
                                }
                                break;
                        }

                        if (record_int>=20 && record_int<=51){
                            actual_recode=desRate.get(val);
                        }

                        if ((record_int>=101 && record_int<=106) ||(record_int==92) ){
                            actual_recode=" ";
                        }


                        if (recordvalue.equals("ffff")){
                            recordvalue = " ";
                        }else {
                            recordvalue = actual_recode + symb;
                        }


                        // Log.d(TAG, "Mikro Record value "+recordvalue);
                        break;

                }
            }
            final String finalRecCode=recordcode,finalyear=year,finalmonth=month,finaldate=date,finalhour=hour,finalminute=minute,finalRecValue=recordvalue;
            final int finalnumber=count;
            runOnUiThread(new Runnable() {
                public void run() {
                    // for (int i = 1; i <= 50 ; i++) {
                    addTextViews(finalnumber,finalRecCode,finalyear,finalmonth,finaldate,finalhour,finalminute,finalRecValue);
                    //}
                }
            });
        }

        return joined;
    }

    public static String[] splitString(String str, int k) {
        if(str == null) return null;

        List<String> list = new ArrayList<String>();
        for(int i=0;i < str.length();i=i+k){
            int endindex = Math.min(i+k,str.length());
            list.add(str.substring(i, endindex));
            // Log.d(TAG, "Mikro Substring " +list);
        }
        return list.toArray(new String[list.size()]);
    }

    public void addTextViews(int count,String code,String year,String month,String date,String hour, String minute,String value) {
        //Adding a LinearLayout with HORIZONTAL orientation
        LinearLayout textLinearLayout = new LinearLayout(this);
        textLinearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(textLinearLayout);
        TextView textView = new TextView(this);
        if(date.equals("0")&& month.equals("0")){
            Log.d(TAG, "Mikro 1 Date not valid");
            textView.setText(count+". "+code+"   "+value);
        } else if(date.equals("255")|| month.equals("255")|| year.equals("2255")|| minute.equals("255") || hour.equals("255")){
            Log.d(TAG, "Mikro 2 Date not valid");
            if(value.equals("255")){
                textView.setText(count + ". " + code);
            } else {
                textView.setText(count + ". " + code + "   " + value);
            }
        } else if(value.equals("255")){
            Log.d(TAG, "Mikro value 255");
            textView.setText(count + ". " + date + "-" + month + "-" + year + "   " + hour + ":" + minute + "\n" + code);
        } else {
            textView.setText(count + ". " + date + "-" + month + "-" + year + "   " + hour + ":" + minute + "\n" + code + "    " + value);

        }
        setTextViewAttributes(textView);
        textLinearLayout.addView(textView);
    }

    public void setTextViewAttributes(TextView textView) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        params.setMargins(convertDpToPixel(16),
                convertDpToPixel(16),
                0, 0
        );

        textView.setTextColor(Color.WHITE);
        textView.setLayoutParams(params);
    }

    public int convertDpToPixel(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }

    public boolean checkMailboxActive()
    {

        //turning on FTM mode
        try {
            ((ST25DVTag)mftmTag).enableMailbox();
            {
                runOnUiThread(new Runnable() {

                    public void run() {
                        Toast.makeText(system_record.this, "NFC is OFF, now turning ON.", Toast.LENGTH_SHORT).show();

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


}