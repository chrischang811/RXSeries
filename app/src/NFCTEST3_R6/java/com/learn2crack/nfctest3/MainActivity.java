package com.learn2crack.rx1000;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import android.provider.Settings;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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


public class MainActivity extends AppCompatActivity implements TagDiscovery.onTagDiscoveryCompletedListener, NavigationView.OnNavigationItemSelectedListener {

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
    private String bits;

    private byte[] mFTMmsg;
    private byte[] mBuffer;

    static private NFCTag mftmTag;
    private static final String TAG = MainActivity.class.getSimpleName();
    CRC16checker crccheck=new CRC16checker();

    static private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;

    Handler handler = new Handler();
    private Button writebtn;
    private Button syncbtn;
    private FloatingActionButton fab;
    private TextView cosText,powerText,currentText,voltText,freqText,CTHDText,VTHDText,reqPowerText,actPowerText,reactPowerText,appPowerText;
    private TextView bit0, bit1, bit2,bit3,bit4,bit5,bit6,bit7,bit8,bit9,bit10,bit11,bit12,bit13,bit14,bit15;
    private TextView autoText, manualText,alarmText, capIn, capOut;
    private boolean enableftm = false;
    private boolean disableftm = false;
    private boolean readRun = false;
    private boolean syncRun = false;
    private boolean mMailboxEnabled = false;
    private boolean result = false;
    private boolean read_result = false;
    private Runnable readCode;
    private Runnable syncCode;
    private Runnable runCode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        bits=intent.getStringExtra("device_bits");
        Log.d("Mikro ", "devices type: " +  bits);

        //FrameLayout contentFrameLayout = (FrameLayout) findViewById(R.id.content_frame); //Remember this is the FrameLayout area within your activity_main.xml
        //getLayoutInflater().inflate(R.layout.read_ftm, contentFrameLayout);
        Toolbar toolbar= findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);


        //check NFC availability on device
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        Log.d(TAG, "Mikro: mNfcAdapter " + mNfcAdapter);
        Log.d(TAG, "Mikro: NFC status " + mNfcAdapter.isEnabled());

       // boolean status = checkMailboxActive();
       // Log.d(TAG, "Mikro: checkMailboxActive " + status);

        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);


        // Step output display
        bit0 = (TextView) findViewById(R.id.no1);
        bit1= (TextView) findViewById(R.id.no2);
        bit2= (TextView) findViewById(R.id.A0);
        bit3= (TextView) findViewById(R.id.no4);
        bit4= (TextView) findViewById(R.id.A1);
        bit5= (TextView) findViewById(R.id.no6);
        bit6= (TextView) findViewById(R.id.A2);
        bit7= (TextView) findViewById(R.id.no8);
        bit8= (TextView) findViewById(R.id.no9);
        bit9= (TextView) findViewById(R.id.no10);
        bit10= (TextView) findViewById(R.id.A3);
        bit11= (TextView) findViewById(R.id.no12);
        bit12= (TextView) findViewById(R.id.A4);
        bit13= (TextView) findViewById(R.id.no14);
        bit14= (TextView) findViewById(R.id.A5);
        bit15= (TextView) findViewById(R.id.no16);

        if(bits.equals("8")){
            bit6.setVisibility(View.INVISIBLE);
            bit7.setVisibility(View.INVISIBLE);
            bit8.setVisibility(View.INVISIBLE);
            bit9.setVisibility(View.INVISIBLE);
            bit10.setVisibility(View.INVISIBLE);
            bit11.setVisibility(View.INVISIBLE);
            bit12.setVisibility(View.INVISIBLE);
            bit13.setVisibility(View.INVISIBLE);
        }

        if(bits.equals("12")){
            bit10.setVisibility(View.INVISIBLE);
            bit11.setVisibility(View.INVISIBLE);
            bit12.setVisibility(View.INVISIBLE);
            bit13.setVisibility(View.INVISIBLE);
        }

        //LED Status
        autoText= (TextView) findViewById(R.id.auto);
        manualText= (TextView) findViewById(R.id.manual);
        capIn = (TextView) findViewById(R.id.arrowDown);
        capOut= (TextView) findViewById(R.id.arrowUp);
        alarmText= (TextView) findViewById(R.id.alarm);

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

        writebtn = findViewById(R.id.writeBtn);
        syncbtn = findViewById(R.id.syncBtn);

        writebtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                Intent intent = new Intent(MainActivity.this, write_ftm.class);
                startActivity(intent);
            }
        });

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();
                if(syncRun)
                {
                    syncRun = false;
                    //syncbtn.setBackgroundColor(getResources().getColor(R.color.colorGray));
                    //syncbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                    fab.setImageResource(R.drawable.rouded_button);
                    handler.removeCallbacks(syncCode);

                }
                else {
                    syncRun = true;
                   // syncbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_green));
                    fab.setImageResource(R.drawable.rouded_button_green);
                    // syncbtn.setBackgroundColor(getResources().getColor(R.color.colorGreen));

                }
            }
        });

        syncbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(syncRun)
                {
                    syncRun = false;
                    //syncbtn.setBackgroundColor(getResources().getColor(R.color.colorGray));
                    //syncbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                    fab.setImageResource(R.drawable.rouded_button);
                    handler.removeCallbacks(syncCode);

                }
                else {
                    syncRun = true;
                    fab.setBackground(getResources().getDrawable(R.drawable.rouded_button_green));
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
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        int id = menuItem.getItemId();

        if (id == R.id.nav_item_two) {
            Intent intent = new Intent(MainActivity.this, MainActivity.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        } else if (id == R.id.nav_item_three) {
            Intent intent = new Intent(MainActivity.this, read_harmonic.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        } else if (id == R.id.nav_item_four) {
            Intent intent = new Intent(MainActivity.this, write_ftm.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        } else if (id == R.id.nav_item_five) {
            Intent intent = new Intent(MainActivity.this, alarm_setting.class);
           intent.putExtra("device_bits",bits);
            startActivity(intent);
        } else if (id == R.id.nav_item_six) {
            Intent intent = new Intent(MainActivity.this, clock_setting.class);
           intent.putExtra("device_bits",bits);
            startActivity(intent);
        }else if (id == R.id.nav_item_seven) {
            Intent intent = new Intent(MainActivity.this, comm_setting.class);
           intent.putExtra("device_bits",bits);
            startActivity(intent);
        }else if (id == R.id.nav_item_eight) {
            Intent intent = new Intent(MainActivity.this, ratestep_setting.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        } else if (id == R.id.nav_item_nine) {
            Intent intent = new Intent(MainActivity.this, alarm_record.class);
           intent.putExtra("device_bits",bits);
            startActivity(intent);
        } else if (id == R.id.nav_item_ten) {
           Intent intent = new Intent(MainActivity.this, system_record.class);
           intent.putExtra("device_bits",bits);
           startActivity(intent);
       }else if (id == R.id.nav_item_eleven) {
           Intent intent = new Intent(MainActivity.this, step_record.class);
           intent.putExtra("device_bits",bits);
           startActivity(intent);
       } else if (id == R.id.nav_item_fourteen) {
           Intent intent = new Intent(MainActivity.this, step_on_setting.class);
           intent.putExtra("device_bits",bits);
           startActivity(intent);
       }else if (id == R.id.nav_item_twelve) {
           Intent intent = new Intent(MainActivity.this, step_on_timer.class);
           intent.putExtra("device_bits",bits);
           startActivity(intent);
       }else if (id == R.id.nav_item_thirteen) {
           Intent intent = new Intent(MainActivity.this, step_on_counter.class);
           intent.putExtra("device_bits",bits);
           startActivity(intent);
       }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
        mBuffer [3] = (byte)0x10;
        mBuffer [4] = (byte)0x00;
       // mBuffer [5] = (byte)0x20;
       // mBuffer [6] = (byte)0x45;
       // mBuffer [7] = (byte)0xd7;
        mBuffer [5] = (byte)0x21;
        mBuffer [6] = (byte)0x84;
        mBuffer [7] = (byte)0x17;

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
                                    //syncbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                                    fab.setImageResource(R.drawable.rouded_button_red);
                                }
                            });
                            syncRun = false;
                            checkMailboxActive();
                        }
                    } catch (STException e) {
                        Log.d(TAG, "Mikro: Error on sync");
                        fab.setImageResource(R.drawable.rouded_button_red);
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
                                read_result=updateWriteResponse(mFTMmsg);
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        if (!read_result) {
                                            fab.setImageResource(R.drawable.rouded_button_red);
                                        } else {
                                            fab.setImageResource(R.drawable.rouded_button_green);
                                        }
                                    }
                                });

                                String joined=updateFtmMessage(mFTMmsg);
                                result = crccheck.crcChecker16(joined);;
                                Log.d(TAG, "Mikro: Result CRC "+result);
                                if (!result){
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            //  Toast.makeText(MainActivity.this, "FTM is OFF", Toast.LENGTH_SHORT).show();
                                            //Log.d(TAG, "Mikro: sync FTM is off");
                                            fab.setImageResource(R.drawable.rouded_button_red);
                                            new AlertDialog.Builder(MainActivity.this)
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
                                            //syncbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                                            Log.d(TAG, "Mikro: CRC OK " );
                                            fab.setImageResource(R.drawable.rouded_button_green);
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
                                    Toast.makeText(MainActivity.this, "FTM OFF", Toast.LENGTH_SHORT).show();
                                }
                            });


                        }
                    } catch (STException e) {
                        Log.d(TAG, "Mikro: Error on read");
                        fab.setImageResource(R.drawable.rouded_button_red);
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
                        Toast.makeText(MainActivity.this, "RF Mailbox Not Empty", Toast.LENGTH_SHORT).show();
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

    public String formatValue_current(double value) {
        String arr[] = {"", "k", "M"};
        String formatvalue=null;
        double loopvalue=value/100, receivedValue =value/100;
        int index = 0;
        DecimalFormat decimalFormat1 = new DecimalFormat("0.00");
        DecimalFormat decimalFormat10 = new DecimalFormat("0.0");
        DecimalFormat decimalFormat100 = new DecimalFormat("0");

        if (value==0){
            return String.format("%s %s", decimalFormat100.format(value), arr[0]);
        }


        while ((loopvalue / 1000) >= 1) {
            loopvalue = loopvalue / 1000;
            index++;
        }

        //value =Math.round(value*100)/100;

        if (receivedValue>0 && receivedValue<10){
            formatvalue= decimalFormat1.format(receivedValue);
         //   Log.d(TAG, "Mikro FTM loop 1 if 1" );
        }  else if (receivedValue>10 && receivedValue<100){
            formatvalue= decimalFormat10.format(receivedValue);
         //   Log.d(TAG, "Mikro FTM loop 1 if 2" );
        } else if (receivedValue>100 && receivedValue<1000){
            formatvalue= decimalFormat100.format(receivedValue);
           // Log.d(TAG, "Mikro FTM loop 1 if 3" );
        }

        if (loopvalue>0 &&loopvalue<10){
            formatvalue= decimalFormat1.format(loopvalue);
           // Log.d(TAG, "Mikro FTM loop 2 if 1" );
        } else if (loopvalue>=10 &&loopvalue<100){
            formatvalue= decimalFormat10.format(loopvalue);
            //Log.d(TAG, "Mikro FTM loop 2 if 2" );
        } else if (loopvalue>=100 &&loopvalue<1000){
            formatvalue= decimalFormat100.format(loopvalue);
          //  Log.d(TAG, "Mikro FTM loop 2 if 3" );
        }
        Log.d(TAG, "Mikro FTM value: " + value + "   "+formatvalue+"   "+ index);
        return String.format("%s %s", formatvalue, arr[index]);
    }

    public String formatValue_power(double value) {
        String arr[] = {"", "k", "M"};
        String formatvalue=null;
        int index = 0;
        Log.d(TAG, "Mikro FTM received value: " + value);
        double receivedValue =value;
        DecimalFormat decimalFormat1 = new DecimalFormat("0");
        DecimalFormat decimalFormat10 = new DecimalFormat("0.00");
        DecimalFormat decimalFormat100 = new DecimalFormat("0.0");
        if (value==0){
            return String.format("%s %s", decimalFormat1.format(value), arr[0]);
        }
        while ((value / 1000) >= 1) {
            value = value / 1000;
            Log.d(TAG, "Mikro FTM loop value: " + value+ " " + index);
            index++;
        }

        //value =Math.round(value*100)/100;
        if (receivedValue>0 && receivedValue<1000){
            formatvalue= decimalFormat1.format(receivedValue);
        } else if (value>=1 &&value<9.95){
            formatvalue= decimalFormat10.format(value);
        } else if (value>=9.95 &&value<99.5){
            formatvalue= decimalFormat100.format(value);
        } else if (value>=99.5 &&value<1000){
            formatvalue= decimalFormat1.format(value);
        }
        Log.d(TAG, "Mikro FTM value: " + value + "   "+formatvalue+"   "+ index);
        return String.format("%s %s", formatvalue, arr[index]);
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
                    if (element.equals("83")||element.equals("84")){
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
            DecimalFormat form = new DecimalFormat("0.00");
            int no_message = message.length;
            String[] putftmpara = new String[message.length];
            String[] putftmpara1 = new String[message.length];
            int [] temp = new int[0];
            int [] tempLED = new int[0];
            byte tempMsg = 0;
            String msg= "";
            String joined = null,append = null;
            
           // String msg = "FTM Message: Total byte is " + no_message +"\n";
            //ftmMsgText = (TextView) findViewById(R.id.msgTextView);
            cosText = (TextView) findViewById(R.id.cos);
            voltText = (TextView) findViewById(R.id.IL1);
            currentText= (TextView) findViewById(R.id.IL2);
            freqText = (TextView) findViewById(R.id.IL3);
            CTHDText=(TextView) findViewById(R.id.CurrentTHD);
            VTHDText= (TextView) findViewById(R.id.ILo);
            reqPowerText = (TextView) findViewById(R.id.ReqPower);
            actPowerText=(TextView) findViewById(R.id.activePower);
            reactPowerText=(TextView) findViewById(R.id.reactivePower);
            appPowerText=(TextView) findViewById(R.id.appPower);
            powerText=(TextView)findViewById(R.id.powerfactor);

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
            Log.d(TAG, "Mikro CRC joined  "+joined);
            //crc check

            final String finalMsg = msg;
            Log.d(TAG, "Mikro FTM finalMsg: " + finalMsg);

            String readcos=null,freq = null, curr=null,pf=null,cp=null,voltage=null,vthd=null,cthd=null,reqp=null,actp=null,reap=null,appp=null,stepOutput=null,status=null, PFunit=null, pfsign=null;
            String val_temp=null,  fullPFString=null;
            int stpOut=0,val =0, volt=0;
            double current=0,appPower=0, reqPower=0,reactPower=0,actPower=0, powerFac=0, frequency=0,cospower=0;
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
                switch (i){
                    case 3:
                        val = Integer.parseInt(putftmpara1[i+1], 16);
                        cospower=val;
                        Log.d(TAG, "Mikro FTM pf value: " + cospower);
                        cp=form.format(cospower/100);
                        val = Integer.parseInt(element, 16);
                        status =  Integer.toBinaryString(0x1000 | val).substring(1);
                        Log.d(TAG, "Mikro FTM pf Status: " + status);
                        char ch2 [] = status.toCharArray();
                        tempLED = new int[ch2.length];
                        for (int y=0;y<ch2.length;y++){
                            tempLED[y]=Integer.parseInt(""+ch2[y]);
                          //  Log.d(TAG, "Mikro FTM cos Status by bit "+y+": " + tempLED[y]);
                        }

                        if (cospower==100){
                            PFunit=" ";
                        } else if (tempLED[3]==0){
                            PFunit="CAP";
                        } else if (tempLED[3]==1){
                            PFunit="IND";
                        } else if (tempLED[3]==0 &&tempLED[3]==0 ){
                            PFunit=" ";
                        } else {
                            PFunit ="null";
                        }

                       if(tempLED[2]==0 || cospower==100 ) {
                            pfsign="";
                        }  else if (tempLED[2]==1){
                            pfsign="-";
                        }

                        fullPFString = pfsign + cp + PFunit;
                        Log.d(TAG, "Mikro FTM pf full string: " + fullPFString);
                        break;
                    case 5:
                        val = Integer.parseInt(element, 16);
                        volt=val;
                        voltage=Integer.toString(volt)+ " V";
                        break;
                    case 7:
                        val_temp = element;
                        break;
                    case 9:
                        String curr_inbyte =val_temp+element;
                        Log.d(TAG, "Mikro FTM current:   " + curr_inbyte);
                        current = Integer.parseInt(curr_inbyte, 16);
                        Log.d(TAG, "Mikro FTM current:   " + current);
                        curr=formatValue_current(current) + "A";
                        break;
                    case 11:
                        val = Integer.parseInt(element, 16);
                        frequency=val;
                        freq=form.format(frequency/100)+ "Hz";
                        //Log.d(TAG, "Mikro FTM frequency: " + freq);
                        break;
                    case 13:
                        val = Integer.parseInt(element, 16);
                        int VTHD=val;
                        vthd=Integer.toString(VTHD) + " %";
                        break;
                    case 15:
                        val = Integer.parseInt(element, 16);
                        int CTHD=val;
                        cthd=Integer.toString(CTHD) + " %";
                        break;
                    case 17:
                        val_temp = element;
                        break;
                    case 19:
                        String reqPower_inbyte =val_temp+element;
                        reqPower = Integer.parseInt(reqPower_inbyte, 16);
                        reqp=formatValue_power(reqPower)+ "VAR";
                        Log.d(TAG, "Mikro FTM reqPower: " + reqPower);
                        break;
                    case 21:
                        val_temp = element;
                        break;
                    case 23:
                        String actPower_inbyte=val_temp+element;
                        actPower = Integer.parseInt(actPower_inbyte, 16);
                        actp=formatValue_power(actPower) + "W";
                        //Log.d(TAG, "Mikro FTM actPower: " + actPower);
                        break;
                    case 25:
                        val_temp = element;
                        break;
                    case 27:
                        String reactPower_inbyte = val_temp+element;
                        reactPower=Integer.parseInt(reactPower_inbyte, 16);
                        reap=formatValue_power(reactPower) + "VAR";
                        //Log.d(TAG, "Mikro FTM reactPower: " + reactPower);
                        break;
                    case 29:
                        val_temp = element;
                        break;
                    case 31:
                        String appPower_inbyte = val_temp+element;
                        appPower = Integer.parseInt(appPower_inbyte, 16);
                        appp=formatValue_power(appPower)+ "VA";
                        //Log.d(TAG, "Mikro FTM appPower: " + appPower);
                        break;
                    case 63:
                        Log.d(TAG, "Mikro FTM step output in Hex: " + element);
                        val = Integer.parseInt(element, 16);
                        stpOut = val;
                        stepOutput = Integer.toBinaryString(0x10000 | stpOut).substring(1);
                        Log.d(TAG, "Mikro FTM step output: " + stepOutput);
                        char ch [] = stepOutput.toCharArray();
                        temp = new int [ch.length];
                        for (int y=0;y<ch.length;y++){
                          temp[y]=Integer.parseInt(""+ch[y]);
                            //Log.d(TAG, "Mikro FTM step output by bit "+y+": " + temp[y]);
                        }
                        break;
                    case 65:
                        val = Integer.parseInt(element, 16);
                        int LEDst = val;
                        status =  Integer.toBinaryString(0x40 | LEDst).substring(1);
                        Log.d(TAG, "Mikro FTM LED Status: " + status);
                        char ch1 [] = status.toCharArray();
                        tempLED = new int[ch1.length];
                        for (int y=0;y<ch1.length;y++){
                            tempLED[y]=Integer.parseInt(""+ch1[y]);
                           // Log.d(TAG, "Mikro FTM LED Status by bit "+y+": " + tempLED[y]);
                        }
                        break;
                    case 67:
                        DecimalFormat decimalFormat10 = new DecimalFormat("0.00");
                        double val_tmp = Integer.parseInt(element, 16);
                        //val=0;
                        powerFac=val_tmp/100;
                        pf=decimalFormat10.format(powerFac);
                        Log.d(TAG, "Mikro FTM power factor  : " + powerFac + " "+pf );
                       // pf=Double.toString(powerFac);

                    break;
                };

               // Log.d(TAG, "Mikro FTM array "+i +", " +"hex: "+element+ ",digit:"+ val);
            }

            if (current==0 || volt==0 ){
                fullPFString = "--";
            }
            final String finalFrequency = freq, finalVoltage=voltage, finalcurrent=curr, finalcos =fullPFString, finalVTHD=vthd, finalCTHD=cthd;
            final String finalReqPower=reqp,finalActPower=actp, finalReactPower=reap, finalappPower=appp,finalpf=pf;
            final int[] finalTemp = temp;
            final int[] finalTempLED= tempLED;
            runOnUiThread(new Runnable() {
                public void run() {

                   // ftmMsgText.setText(finalMsg);
                    freqText.setText(finalFrequency);
                    voltText.setText(finalVoltage);
                    currentText.setText(finalcurrent);
                    cosText.setText(finalcos);
                    powerText.setText(finalpf);
                    CTHDText.setText(finalCTHD);
                    VTHDText.setText(finalVTHD);
                    reqPowerText.setText(finalReqPower);
                    actPowerText.setText(finalActPower);
                    reactPowerText.setText(finalReactPower);
                    appPowerText.setText(finalappPower);
                    if (finalTemp[15] ==1) {
                        bit0.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                    } else {
                        bit0.setTextColor(getResources().getColor(R.color.colorGray));
                    }

                    if (finalTemp[14] ==1){
                        bit1.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                    }else {
                        bit1.setTextColor(getResources().getColor(R.color.colorGray));
                    }

                    if (finalTemp[13] ==1){
                        bit2.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                    }else {
                        bit2.setTextColor(getResources().getColor(R.color.colorGray));
                    }

                    if (finalTemp[12] ==1){
                        bit3.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                    }else {
                        bit3.setTextColor(getResources().getColor(R.color.colorGray));
                    }

                    if (finalTemp[11] ==1){
                        bit4.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                    }else {
                        bit4.setTextColor(getResources().getColor(R.color.colorGray));
                    }

                    if (finalTemp[10] ==1){
                        bit5.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                    }else {
                        bit5.setTextColor(getResources().getColor(R.color.colorGray));
                    }

                    if (finalTemp[9] ==1){
                        bit6.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                    }else {
                        bit6.setTextColor(getResources().getColor(R.color.colorGray));
                    }

                    if (finalTemp[8] ==1){
                        bit7.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                    }else {
                        bit7.setTextColor(getResources().getColor(R.color.colorGray));
                    }

                    if (finalTemp[7] ==1){
                        bit8.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                    }else {
                        bit8.setTextColor(getResources().getColor(R.color.colorGray));
                    }

                    if (finalTemp[6] ==1){
                        bit9.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                    }else {
                        bit9.setTextColor(getResources().getColor(R.color.colorGray));
                    }

                    if (finalTemp[5] ==1){
                        bit10.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                    }else {
                        bit10.setTextColor(getResources().getColor(R.color.colorGray));
                    }

                    if (finalTemp[4] ==1){
                        bit11.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                    }else {
                        bit11.setTextColor(getResources().getColor(R.color.colorGray));
                    }

                    if (finalTemp[3] ==1){
                        bit12.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                    }else {
                        bit12.setTextColor(getResources().getColor(R.color.colorGray));
                    }
                    if (finalTemp[2] ==1){
                        bit13.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                    }else {
                        bit13.setTextColor(getResources().getColor(R.color.colorGray));
                    }

                    if (finalTemp[1] ==1){
                        bit14.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                    }else {
                        bit14.setTextColor(getResources().getColor(R.color.colorGray));
                    }

                    if (finalTemp[0] ==1){
                        bit15.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                    }else {
                        bit15.setTextColor(getResources().getColor(R.color.colorGray));
                    }

                    if (finalTempLED[5] ==1) {
                        autoText.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                        manualText.setTextColor(getResources().getColor(R.color.colorGray));
                    } else {
                        manualText.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                        autoText.setTextColor(getResources().getColor(R.color.colorGray));
                    }

                    if(finalTempLED[2]==1){
                        capOut.setTextColor(Color.BLACK);
                    }

                    if(finalTempLED[1]==1){
                        capIn.setTextColor(Color.BLACK);
                    }

                    if (finalTempLED[0]==1){
                        alarmText.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                    } else {
                        alarmText.setTextColor(getResources().getColor(R.color.colorGray));
                    }
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
                       /* new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.app_name)
                                .setIcon(R.mipmap.ic_launcher)
                                .setMessage("\nNFC is OFF, now turning ON. Please try again.\n")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Log.d(TAG, "Mikro: press switch on Mailbox " + enableftm);
                                    }

                                })
                                .show();*/
                        Toast.makeText(MainActivity.this, "NFC is OFF, now turning ON.", Toast.LENGTH_SHORT).show();

                    }

                });

            }

            Log.d(TAG, "Mikro: turning on FTM mode");
            syncRun = true;
            //syncbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_green));
            fab.setImageResource(R.drawable.rouded_button_green);
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