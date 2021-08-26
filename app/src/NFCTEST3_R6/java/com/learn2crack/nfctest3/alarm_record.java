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


public class alarm_record extends BaseActivity implements TagDiscovery.onTagDiscoveryCompletedListener {

    private final int ERROR     = -1;
    private final int TRY_AGAIN = 0;
    private final int OK        = 1;

    private final int DELAY_FTM = 10;
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
    private int curr_reg;

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
        FrameLayout contentFrameLayout = (FrameLayout) findViewById(R.id.content_frame);
        getLayoutInflater().inflate(R.layout.alarm_record, contentFrameLayout);
        //setContentView(R.layout.alarm_record);
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
                        String request = "010310";
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

        readbtn = findViewById(R.id.read_alarm_record);

        readbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();
                count=0;
                repeat=0;
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
                dialog = ProgressDialog.show(alarm_record.this, "",
                        "Loading data. Please don't remove your phone from device.", true);
            }

            handler.postDelayed(runCode, 100);  //try to read the message first

        }

    }


    public void syncFTM() {

        if (mftmTag == null) {
            return;
        }

        mBuffer = new byte[8];
        mBuffer[0] = (byte) 0x01;
        mBuffer[1] = (byte) 0x03;
        mBuffer[2] = (byte) 0x10;
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
                                Log.d(TAG, "Mikro: run Sync ");
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
                                new android.app.AlertDialog.Builder(alarm_record.this)
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
                                            new AlertDialog.Builder(alarm_record.this)
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
                                        String request = "010310";
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
                                        if (repeat == 3) {
                                            repeat = 0;
                                            load=0;
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    readbtn.setBackground(getResources().getDrawable(R.drawable.rouded_button));
                                                    //Log.d(TAG, "Mikro Turn button gray ");
                                                    dialog.dismiss();
                                                }
                                            });
                                        }
                                    }

                                    //Log.d(TAG, "Mikro Turn readrun false ");

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
                                    Toast.makeText(alarm_record.this, "FTM OFF", Toast.LENGTH_SHORT).show();
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
                                new android.app.AlertDialog.Builder(alarm_record.this)
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
                        Toast.makeText(alarm_record.this, "RF Mailbox Not Empty", Toast.LENGTH_SHORT).show();
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
        recDes.put(1,"THD Voltage Alarm");
        recDes.put(2,"THD Current Alarm");
        recDes.put(3,"Undercurrent Alarm");
        recDes.put(4,"Overcurrent Alarm");
        recDes.put(5,"Undervoltage Alarm");
        recDes.put(6,"Overvoltage Alarm");
        recDes.put(7,"Capacitor Size Error Alarm");
        recDes.put(8,"Undercompensate Alarm");
        recDes.put(9,"Overcompensate Alarm");
        recDes.put(10,"Step Error Alarm");
        recDes.put(11,"No Voltage Release Alarm");
        recDes.put(12,"CT Polarity Error Alarm");
        recDes.put(13,"Clock Loss Alarm");
        recDes.put(14,"EEPROM Error Alarm");


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

        String recordcode = null, year=null,month=null,date=null,hour=null,minute=null,recordvalue=null,symb=null;
        int val =0,record_int=0;

        for(int i = 0; i < record.length; i++)
        {
            count++;
            // Log.d(TAG, "Mikro words: "+i);
            for (int j=0;j<8;j++){
                if (record[i].equals("ffffffffffffffff")){
                    joined = "false";
                    return joined;
                }
                String [] words=splitString(record[i],2);
                String element = words[j];
                //  Log.d(TAG, "Mikro words "+j+" :" +words[j]);

                switch(j) {
                    case 0:
                        val = Integer.parseInt(element, 16);
                        record_int=val;
                        //String recordcode_ini = Integer.toString(val) ;
                        recordcode=recDes.get(val);
                        if (val<=4) {
                            symb= " %";
                        } else if(val==5||val==6||val==11){
                            symb = " V";
                        } else{
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
                        recordvalue= element;
                        break;
                    case 7:
                        //val = Integer.parseInt(element, 16);
                        recordvalue = recordvalue + element;
                        val = Integer.parseInt(recordvalue, 16);
                         if(record_int==3){
                            recordvalue= form.format(val/10);
                        } else{
                            recordvalue=Integer.toString(val);
                        }

                         if (recordvalue.equals("255")){
                             recordvalue="";
                        }else{
                            recordvalue=recordvalue+symb;
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
        } else if(date.equals("255")|| month.equals("255")|| year.equals("2255")|| minute.equals("255") || hour.equals("255")) {
            Log.d(TAG, "Mikro 2 Date not valid");
            textView.setText(count + ". " + code + "   " + value);
        } else {
            textView.setText(count + ". " + date + "-" + month + "-" + year + "   " + hour + ":" + minute + "\n" + code + "   " + value);
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
                        Toast.makeText(alarm_record.this, "NFC is OFF, now turning ON.", Toast.LENGTH_SHORT).show();

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