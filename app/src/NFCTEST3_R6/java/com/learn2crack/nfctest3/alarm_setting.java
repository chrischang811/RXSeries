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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.STLog;
import com.st.st25sdk.TagHelper;
import com.st.st25sdk.type5.st25dv.ST25DVTag;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static android.os.SystemClock.sleep;
import static com.st.st25sdk.STException.STExceptionCode.CMD_FAILED;



public class alarm_setting extends BaseActivity implements TagDiscovery.onTagDiscoveryCompletedListener{

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

    static private NFCTag mftmTag;
    static private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private static final String TAG = alarm_setting.class.getSimpleName();
    CRC16checker crccheck=new CRC16checker();

    private EditText thd_volt_edit,thd_current_edit,Ucurrent_edit,Ocurrent_edit,Uvolt_edit,Ovolt_edit,freq_edit;
    private CheckBox thdvolt_output,thdcurrent_alarm,thdcurrent_output,ucurrent_alarm, ucurrent_output,ocurrent_alarm,ocurrent_output;
    private CheckBox thdvolt_alarm,counter_alarm,counter_output,timer_alarm,timer_output;
    private CheckBox uvolt_alarm,uvolt_output,ovolt_alarm,ovolt_output,cap_alarm,cap_output,ucom_alarm,ucom_output,ocom_alarm,ocom_output;
    private CheckBox step_alarm,step_output,novolt_alarm,novolt_output,ct_alarm,ct_output,clock_alarm,clock_output,eep_alarm,eep_output;
    private int thdV,thdA,overA,underV,overV;
    private double underA;
    private String[] set_alarm= new String[16];
    private String[] set_output=new String[16];
    private String thdV_L,thdV_H,thdA_L,thdA_H,underA_L,underA_H,overA_L,overA_H,underV_L,underV_H,overV_L,overV_H;
    private String SetAlarm_H,SetAlarm_L,SetOutput_H,SetOutput_L,crc_low,crc_high;

    Handler handler = new Handler();
    private Button writebtn, readbtn;
    private boolean readRun = false;
    private boolean writeRun = false;

    private boolean result = false;
    private boolean clicked = false;
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
       // setContentView(R.layout.alarm_setting);
        FrameLayout contentFrameLayout = (FrameLayout) findViewById(R.id.content_frame);
        getLayoutInflater().inflate(R.layout.alarm_setting, contentFrameLayout);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this); //check NFC hardware available
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        readbtn = findViewById(R.id.btn_read_alarm);
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

        //defines parameter
        thd_current_edit=(EditText) findViewById(R.id.thd_current_edit);
        thd_current_edit.setFilters(new InputFilter[]{new InputFilterMinMax("1", "300")});
        thd_volt_edit=(EditText) findViewById(R.id.thd_volt_edit);
        thd_volt_edit.setFilters(new InputFilter[]{new InputFilterMinMax("1", "300")});
        Ucurrent_edit=(EditText) findViewById(R.id.Ucurrent_edit);
       // Ucurrent_edit.setFilters(new InputFilter[]{new InputFilterMinMax("1", "140")});
        Uvolt_edit=(EditText) findViewById(R.id.Uvolt_edit);
        Uvolt_edit.setFilters(new InputFilter[]{new InputFilterMinMax("1", "395")});
        Ocurrent_edit=(EditText) findViewById(R.id.Ocurrent_edit);
        Ocurrent_edit.setFilters(new InputFilter[]{new InputFilterMinMax("1", "140")});
        Ovolt_edit=(EditText) findViewById(R.id.Ovolt_edit);
        Ovolt_edit.setFilters(new InputFilter[]{new InputFilterMinMax("1", "500")});


        thdvolt_alarm=(CheckBox)findViewById(R.id.THDvolt_alarm);
        thdvolt_alarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton ard0, boolean isChecked) {
                if (isChecked){
                    thdvolt_output.setClickable(true);
                    thdvolt_output.setChecked(true);
                } else {
                    thdvolt_output.setChecked(false);
                    thdvolt_output.setClickable(false);
                }
            }
        });
        thdvolt_output=(CheckBox)findViewById(R.id.THDvolt_output);

        thdcurrent_alarm=(CheckBox)findViewById(R.id.THDcurrent_alarm);
        thdcurrent_alarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton ard0, boolean isChecked) {
                if (isChecked){
                    thdcurrent_output.setClickable(true);
                    thdcurrent_output.setChecked(true);
                } else {
                    thdcurrent_output.setChecked(false);
                    thdcurrent_output.setClickable(false);
                }
            }
        });
        thdcurrent_output=(CheckBox)findViewById(R.id.THDcurrent_output);

        ucurrent_alarm=(CheckBox)findViewById(R.id.ucurrent_alarm);
        ucurrent_alarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton ard0, boolean isChecked) {
                if (isChecked){
                    ucurrent_output.setChecked(true);
                    ucurrent_output.setClickable(true);
                } else {
                    ucurrent_output.setChecked(false);
                    ucurrent_output.setClickable(false);
                }
            }
        });
        ucurrent_output=(CheckBox)findViewById(R.id.ucurrent_output);

        ocurrent_alarm=(CheckBox)findViewById(R.id.ocurrent_alarm);
        ocurrent_alarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton ard0, boolean isChecked) {
                if (isChecked){
                    ocurrent_output.setClickable(true);
                    ocurrent_output.setChecked(true);
                } else {
                    ocurrent_output.setChecked(false);
                    ocurrent_output.setClickable(false);
                }
            }
        });
        ocurrent_output=(CheckBox)findViewById(R.id.ocurrent_output);

        uvolt_alarm=(CheckBox)findViewById(R.id.uvolt_alarm);
        uvolt_alarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton ard0, boolean isChecked) {
                if (isChecked){
                    uvolt_output.setClickable(true);
                    uvolt_output.setChecked(true);
                } else {
                    uvolt_output.setChecked(false);
                    uvolt_output.setClickable(false);
                }
            }
        });
        uvolt_output=(CheckBox)findViewById(R.id.uvolt_output);

        ovolt_alarm=(CheckBox)findViewById(R.id.ovolt_alarm);
        ovolt_alarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton ard0, boolean isChecked) {
                if (isChecked){
                    ovolt_output.setClickable(true);
                    ovolt_output.setChecked(true);
                } else {
                    ovolt_output.setChecked(false);
                    ovolt_output.setClickable(false);
                }
            }
        });
        ovolt_output=(CheckBox)findViewById(R.id.ovolt_output);

        cap_alarm=(CheckBox)findViewById(R.id.cap_alarm);
        cap_alarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton ard0, boolean isChecked) {
                if (isChecked){
                    cap_output.setClickable(true);
                    cap_output.setChecked(true);
                } else {
                    cap_output.setChecked(false);
                    cap_output.setClickable(false);
                }
            }
        });
        cap_output=(CheckBox)findViewById(R.id.cap_output);

        ucom_alarm=(CheckBox)findViewById(R.id.ucom_alarm);
        ucom_alarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton ard0, boolean isChecked) {
                if (isChecked){
                    ucom_output.setClickable(true);
                    ucom_output.setChecked(true);
                } else {
                    ucom_output.setChecked(false);
                    ucom_output.setClickable(false);
                }
            }
        });
        ucom_output=(CheckBox)findViewById(R.id.ucom_output);

        ocom_alarm=(CheckBox)findViewById(R.id.ocom_alarm);
        ocom_alarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton ard0, boolean isChecked) {
                if (isChecked){
                    ocom_output.setClickable(true);
                    ocom_output.setChecked(true);
                } else {
                    ocom_output.setChecked(false);
                    ocom_output.setClickable(false);
                }
            }
        });
        ocom_output=(CheckBox)findViewById(R.id.ocom_output);

        step_alarm=(CheckBox)findViewById(R.id.stepError_alarm);
        step_alarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton ard0, boolean isChecked) {
                if (isChecked){
                    step_output.setClickable(true);
                    step_output.setChecked(true);
                } else {
                    step_output.setChecked(false);
                    step_output.setClickable(false);
                }
            }
        });
        step_output=(CheckBox)findViewById(R.id.stepError_output);

        novolt_alarm=(CheckBox)findViewById(R.id.novolt_alarm);
        novolt_alarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton ard0, boolean isChecked) {
                if (isChecked){
                    novolt_output.setClickable(true);
                    novolt_output.setChecked(true);
                } else {
                    novolt_output.setChecked(false);
                    novolt_output.setClickable(false);
                }
            }
        });
        novolt_output=(CheckBox)findViewById(R.id.noVolt_output);

        ct_alarm=(CheckBox)findViewById(R.id.CTError_alarm);
        ct_alarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton ard0, boolean isChecked) {
                if (isChecked){
                    ct_output.setClickable(true);
                    ct_output.setChecked(true);
                } else {
                    ct_output.setChecked(false);
                    ct_output.setClickable(false);
                }
            }
        });
        ct_output=(CheckBox)findViewById(R.id.CTError_output);

        clock_alarm=(CheckBox)findViewById(R.id.clock_alarm);
        clock_alarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton ard0, boolean isChecked) {
                if (isChecked){
                    clock_output.setClickable(true);
                    clock_output.setChecked(true);
                } else {
                    clock_output.setChecked(false);
                    clock_output.setClickable(false);
                }
            }
        });
        clock_output=(CheckBox)findViewById(R.id.clock_output);

        eep_alarm=(CheckBox)findViewById(R.id.EEPROM_alarm);
        eep_alarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton ard0, boolean isChecked) {
                if (isChecked){
                    eep_output.setClickable(true);
                    eep_output.setChecked(true);
                } else {
                    eep_output.setChecked(false);
                    eep_output.setClickable(false);
                }
            }
        });
        eep_output=(CheckBox)findViewById(R.id.EEPROM_output);

        timer_alarm=(CheckBox)findViewById(R.id.timer_alarm);
        timer_alarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton ard0, boolean isChecked) {
                if (isChecked){
                    timer_output.setClickable(true);
                    timer_output.setChecked(true);
                } else {
                    timer_output.setChecked(false);
                    timer_output.setClickable(false);
                }
            }
        });
        timer_output=(CheckBox)findViewById(R.id.timer_output);

        counter_alarm=(CheckBox)findViewById(R.id.counter_alarm);
        counter_alarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton ard0, boolean isChecked) {
                if (isChecked){
                    counter_output.setClickable(true);
                    counter_output.setChecked(true);
                } else {
                    counter_output.setChecked(false);
                    counter_output.setClickable(false);
                }
            }
        });
        counter_output=(CheckBox)findViewById(R.id.counter_output);

        writebtn = findViewById(R.id.btn_save_setting);
        writebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!writeRun) {
                    writeRun = true;
                    writebtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_green));

                String request="0110012a000912";
                //Edit thd voltage
                if(TextUtils.isEmpty(thd_volt_edit.getText().toString())){
                    thd_volt_edit.setError("THD voltage field cannot be empty");
                    return;
                } else {
                    thdV = Integer.valueOf(thd_volt_edit.getText().toString());
                    String thdvolt[] = hexStringToStringArray(Integer.toHexString(thdV));
                    thdV_H = thdvolt[0];
                    thdV_L = thdvolt[1];
                    request = request + thdV_H + thdV_L;
                    Log.d(TAG, "Mikro after thdvolt " + request);
                }

                //Edit THD  current
                if(TextUtils.isEmpty(thd_current_edit.getText().toString())){
                    thd_volt_edit.setError("THD current field cannot be empty");
                    return;
                } else {
                    thdA = Integer.valueOf(thd_current_edit.getText().toString());
                    String thdcurr[] = hexStringToStringArray(Integer.toHexString(thdA));
                    thdA_H = thdcurr[0];
                    thdA_L = thdcurr[1];
                    request = request + thdA_H + thdA_L;
                    Log.d(TAG, "Mikro after thd current " + request);
                }

                //edit undercurrent
                if(TextUtils.isEmpty(Ucurrent_edit.getText().toString())){
                    Ucurrent_edit.setError("Undercurrent field cannot be empty");
                    return;
                } else {
                    underA=Double.parseDouble((Ucurrent_edit.getText().toString()));
                    underA=underA*10;
                    int a=(int) Math.round(underA);
                    String undercurr[] = hexStringToStringArray(Integer.toHexString(a));
                    underA_H = undercurr[0];
                    underA_L = undercurr[1];
                    request = request + underA_H + underA_L;
                    Log.d(TAG, "Mikro after undercurrent " + request);
                }

                //edit overcurrent
                if(TextUtils.isEmpty(Ocurrent_edit.getText().toString())){
                    Ocurrent_edit.setError("Overcurrent field cannot be empty");
                    return;
                } else {
                    /*overA_H = "04";
                    overA_L = "4c";*/
                    overA = Integer.valueOf(Ocurrent_edit.getText().toString());
                    String overcurr[] = hexStringToStringArray(Integer.toHexString(overA));
                    overA_H = overcurr[0];
                    overA_L = overcurr[1];
                    request = request + overA_H + overA_L;
                    Log.d(TAG, "Mikro after overcurrent " + request);
                }


                //edit undervoltage
                if(TextUtils.isEmpty(Uvolt_edit.getText().toString())){
                    Uvolt_edit.setError("Undervoltage field cannot be empty");
                    return;
                } else {
                    underV = Integer.valueOf(Uvolt_edit.getText().toString());
                    String underVolt[] = hexStringToStringArray(Integer.toHexString(underV));
                    underV_H = underVolt[0];
                    underV_L = underVolt[1];
                    request = request + underV_H + underV_L;
                    Log.d(TAG, "Mikro after undervoltage " + request);
                }

                //edit overvoltage
                if(TextUtils.isEmpty(Ovolt_edit.getText().toString())){
                    Ovolt_edit.setError("Overvoltage field cannot be empty");
                    return;
                } else {
                    overV = Integer.valueOf(Ovolt_edit.getText().toString());
                    String overvolt[] = hexStringToStringArray(Integer.toHexString(overV));
                    overV_H = overvolt[0];
                    overV_L = overvolt[1];
                    request = request + overV_H + overV_L;
                    Log.d(TAG, "Mikro after Overvoltage " + request);
                }

                // reserve slot
                    request=request+"0000";
                    Log.d(TAG, "Mikro after reserve slot " + request);

                //Form the programmable alarm
                if(thdvolt_alarm.isChecked()){
                    set_alarm[15]="1";
                    //Log.d(TAG, "Mikro after frequency error " + request);
                } else{
                    set_alarm[15]="0";

                }

                if(thdcurrent_alarm.isChecked()) {
                    set_alarm[14] = "1";
                } else {
                    set_alarm[14]="0";
                }

                if(ucurrent_alarm.isChecked()) {
                    set_alarm[13] = "1";
                } else {
                    set_alarm[13]="0";
                }

                if(ocurrent_alarm.isChecked()) {
                    set_alarm[12] = "1";
                } else {
                    set_alarm[12]="0";
                }

                if(uvolt_alarm.isChecked()) {
                    set_alarm[11] = "1";
                } else {
                    set_alarm[11]="0";
                }

                if(ovolt_alarm.isChecked()) {
                    set_alarm[10] = "1";
                } else {
                    set_alarm[10]="0";
                }

                if(cap_alarm.isChecked()) {
                    set_alarm[9] = "1";
                } else {
                    set_alarm[9]="0";
                }

                if(ucom_alarm.isChecked()) {
                    set_alarm[8] = "1";
                } else {
                    set_alarm[8]="0";
                }

                if(ocom_alarm.isChecked()) {
                    set_alarm[7] = "1";
                } else {
                    set_alarm[7]="0";
                }

                if(step_alarm.isChecked()) {
                    set_alarm[6] = "1";
                } else {
                    set_alarm[6]="0";
                }

                if(novolt_alarm.isChecked()) {
                    set_alarm[5] = "1";
                } else {
                    set_alarm[5]="0";
                }

                if(ct_alarm.isChecked()) {
                    set_alarm[4] = "1";
                } else {
                    set_alarm[4]="0";
                }

                if(clock_alarm.isChecked()) {
                    set_alarm[3] = "1";
                } else {
                    set_alarm[3]="0";
                }

                if(eep_alarm.isChecked()) {
                    set_alarm[2] = "1";
                } else {
                    set_alarm[2]="0";
                }

                if(timer_alarm.isChecked()){
                    set_alarm[1]="1";
                    //Log.d(TAG, "Mikro after frequency error " + request);
                } else{
                    set_alarm[1]="0";
                }

                if(counter_alarm.isChecked()){
                    set_alarm[0]="1";
                    //Log.d(TAG, "Mikro after frequency error " + request);
                } else{
                    set_alarm[0]="0";
                }

                String alarm_setting=StringUtils.join(set_alarm, "");
                Log.d(TAG, "Mikro alarm setting in binary: " +alarm_setting);
                int myAlarmInt = Integer.parseInt(alarm_setting, 2);
                Log.d(TAG, "Mikro alarm setting in integer: " +myAlarmInt);
                String myAlarmSetting[] = hexStringToStringArray(Integer.toHexString(myAlarmInt));
                SetAlarm_H=myAlarmSetting[0];
                SetAlarm_L=myAlarmSetting[1];
                request = request +SetAlarm_H+SetAlarm_L;
                Log.d(TAG, "Mikro after alarm setting: " +request);

                //Form the programmable output
                if(thdvolt_output.isChecked()) {
                    set_output[15]="1";
                } else {
                    set_output[15]="0";
                }

                if(thdcurrent_output.isChecked()) {
                    set_output[14] = "1";
                } else {
                    set_output[14]="0";
                }

                if(ucurrent_output.isChecked()) {
                    set_output[13] = "1";
                } else {
                    set_output[13]="0";
                }

                if(ocurrent_output.isChecked()) {
                    set_output[12] = "1";
                } else {
                    set_output[12]="0";
                }

                if(uvolt_output.isChecked()) {
                    set_output[11] = "1";
                } else {
                    set_output[11]="0";
                }

                if(ovolt_output.isChecked()) {
                    set_output[10] = "1";
                } else {
                    set_output[10]="0";
                }

                if(cap_output.isChecked()) {
                    set_output[9] = "1";
                } else {
                    set_output[9]="0";
                }

                if(ucom_output.isChecked()) {
                    set_output[8] = "1";
                } else {
                    set_output[8]="0";
                }

                if(ocom_output.isChecked()) {
                    set_output[7] = "1";
                } else {
                    set_output[7]="0";
                }

                if(step_output.isChecked()) {
                    set_output[6] = "1";
                } else {
                    set_output[6]="0";
                }

                if(novolt_output.isChecked()) {
                    set_output[5] = "1";
                } else {
                    set_output[5]="0";
                }

                if(ct_output.isChecked()) {
                    set_output[4] = "1";
                } else {
                    set_output[4]="0";
                }

                if(clock_output.isChecked()) {
                    set_output[3] = "1";
                } else {
                    set_output[3]="0";
                }

                if(eep_output.isChecked()) {
                    set_output[2] = "1";
                } else {
                    set_output[2]="0";
                }

                if(timer_output.isChecked()){
                    set_output[1]="1";
                    //Log.d(TAG, "Mikro after frequency error " + request);
                } else{
                    set_output[1]="0";
                }

                if(counter_output.isChecked()){
                    set_output[0]="1";
                    //Log.d(TAG, "Mikro after frequency error " + request);
                } else{
                    set_output[0]="0";
                }

                String output_setting=StringUtils.join(set_output, "");
                Log.d(TAG, "Mikro output setting in binary: " +output_setting);
                int myOutputInt = Integer.parseInt(output_setting, 2);
                Log.d(TAG, "Mikro output setting in integer: " +myOutputInt);
                String myoutputSetting[] = hexStringToStringArray(Integer.toHexString(myOutputInt));
                SetOutput_H= myoutputSetting[0];
                SetOutput_L= myoutputSetting[1];
                request = request +SetOutput_H+SetOutput_L;
                Log.d(TAG, "Mikro after output setting: " +request);

                //request = request +"3d833d83";
                Log.d(TAG, "Mikro sent to CRC checker: " +request);
                //obtain CRC
                int[] ans= crccheck.getCRC(request);
                crc_low=Integer.toHexString(ans[0]);
                crc_high=Integer.toHexString(ans[1]);
                Log.d(TAG, "Mikro CRC: "+crc_high+" "+crc_low);


                writebtn.setBackground(getResources().getDrawable(R.drawable.rouded_button_green));
                //singleFTM();
                } else {
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
        mBuffer = new byte[27];
        mBuffer [0] = (byte)0x01;
        mBuffer [1] = (byte)0x10;
        mBuffer [2] = (byte)0x01;
        mBuffer [3] = (byte)0x2a;
        mBuffer [4] = (byte)0x00;
        mBuffer [5] = (byte)0x09;
        mBuffer [6] = (byte)0x12;
        mBuffer [7] = (byte)Integer.parseInt(thdV_H,16);
        mBuffer [8] = (byte)Integer.parseInt(thdV_L,16);
        mBuffer [9] = (byte)Integer.parseInt(thdA_H,16);
        mBuffer [10] = (byte)Integer.parseInt(thdA_L,16);
        mBuffer [11] = (byte)Integer.parseInt(underA_H,16);
        mBuffer [12] = (byte)Integer.parseInt(underA_L,16);
        mBuffer [13] = (byte)Integer.parseInt(overA_H,16);
        mBuffer [14] = (byte)Integer.parseInt(overA_L,16);
        mBuffer [15] = (byte)Integer.parseInt(underV_H,16);
        mBuffer [16] = (byte)Integer.parseInt(underV_L,16);
        mBuffer [17] = (byte)Integer.parseInt(overV_H,16);
        mBuffer [18] = (byte)Integer.parseInt(overV_L,16);
        mBuffer [19] = (byte)0x00;
        mBuffer [20] = (byte)0x00;
        mBuffer [21] = (byte)Integer.parseInt(SetAlarm_H,16);
        mBuffer [22] = (byte)Integer.parseInt(SetAlarm_L,16);
        mBuffer [23] = (byte)Integer.parseInt(SetOutput_H,16);
        mBuffer [24] = (byte)Integer.parseInt(SetOutput_L,16);
        mBuffer [25] = (byte)Integer.parseInt(crc_high,16);
        mBuffer [26] = (byte)Integer.parseInt(crc_low,16);
        Log.d(TAG, "Mikro: thdvoltage: " + mBuffer[7]+ mBuffer[8]);
        //mBuffer [27] = (byte)0x95;
        //mBuffer [28] = (byte)0x26 ;
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
        mBuffer [3] = (byte)0x2a;
        mBuffer [4] = (byte)0x00;
        mBuffer [5] = (byte)0x09;
        mBuffer [6] = (byte)0xa5;
        mBuffer [7] = (byte)0xf8;

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
                                    Toast.makeText(alarm_setting.this, "FTM OFF", Toast.LENGTH_SHORT).show();
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
                                    Toast.makeText(alarm_setting.this, "FTM OFF", Toast.LENGTH_SHORT).show();
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

                            Log.d(TAG, "Mikro: check mailbox passes");
                            //if mailbox available
                            if(((ST25DVTag)mftmTag).hasHostPutMsg(true))
                            {
                                Log.d(TAG, "Mikro: check host passed");
                                mFTMmsg = readMessage();
                                Log.d(TAG, "Mikro Return msg :" + mFTMmsg);

                                if(writeRun){
                                    Log.d(TAG, "Mikro: host passed, writing");
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

                                if(readRun) {

                                    //updateFtmMessage(mFTMmsg);
                                    Log.d(TAG, "Mikro: reading");
                                    String joined = updateFtmMessage(mFTMmsg);
                                    Log.d(TAG, "Mikro: Received " + joined);
                                    result = crccheck.crcChecker16(joined);
                                    Log.d(TAG, "Mikro: Result CRC " + result);
                                    if (!result) {
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                //  Toast.makeText(MainActivity.this, "FTM is OFF", Toast.LENGTH_SHORT).show();
                                                //Log.d(TAG, "Mikro: sync FTM is off");
                                                new AlertDialog.Builder(alarm_setting.this)
                                                        .setTitle(R.string.app_name)
                                                        .setIcon(R.mipmap.ic_launcher)
                                                        .setMessage("\nReading Error, please check devide and try again\n")
                                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                                Log.d(TAG, "Mikro: CRC Error ");
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

                            } else if(((ST25DVTag)mftmTag).hasHostPutMsg(true)==false) {
                                mFTMmsg = readMessage();
                                Log.d(TAG, "Mikro: writing");
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
                            } else
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
                                        Toast.makeText(alarm_setting.this, "FTM OFF", Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(alarm_setting.this, "FTM OFF", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(alarm_setting.this, "RF Mailbox Not Empty", Toast.LENGTH_SHORT).show();
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
        Log.d(TAG, "Mikro: update write msg " + no_message);
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
        String thdvolt="",thdcurrent="",undercurrent="",overcurrent="",undervolt="",overvolt="",cap_error="",alarmbit="",outputbit="";
        int val =0,alarmbitI=0,outputbitI=0;
        int [] tempAlarm = new int[0];
        int [] tempOutput = new int[0];
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
                    val = Integer.parseInt(element, 16);
                    thdvolt= Integer.toString(val);
                    Log.d(TAG, "Mikro alarm thdvolt:   " + thdvolt);
                    break;
                case 5:
                    val = Integer.parseInt(element, 16);
                    thdcurrent= Integer.toString(val);
                    break;
                case 7:
                    val = Integer.parseInt(element, 16);
                    double underA=val/10.00;
                    undercurrent=Double.toString(underA);
                    Log.d(TAG, "Mikro alarm undercurrent: " +undercurrent);
                    break;
                case 9:
                    val = Integer.parseInt(element, 16);
                    overcurrent= Integer.toString(val);
                    Log.d(TAG, "Mikro alarm overcurrent: " +overcurrent);
                   // overcurrent= Integer.toString(val);
                    break;
                case 11:
                    val = Integer.parseInt(element, 16);
                    undervolt= Integer.toString(val);
                    break;
                case 13:
                    val = Integer.parseInt(element, 16);
                    overvolt= Integer.toString(val);
                    break;
                case 15:
                    val = Integer.parseInt(element, 16);
                    cap_error= Integer.toString(val);
                    break;
                case 17:
                    Log.d(TAG, "Mikro alarm in Hex: " + element);
                    val = Integer.parseInt(element, 16);
                    alarmbitI = val;
                    alarmbit = Integer.toBinaryString(0x10000 | alarmbitI).substring(1);
                    Log.d(TAG, "Mikro FTM prgrammable alarm: " + alarmbit);
                    char ch [] = alarmbit.toCharArray();
                    tempAlarm = new int [ch.length];
                    for (int y=0;y<ch.length;y++){
                        int k=(ch.length-1)-y;
                        tempAlarm[k]=Integer.parseInt(""+ch[y]);
                        Log.d(TAG, "Mikro FTM prgrammable alarm by bit "+k+": " + tempAlarm[k]);
                    }
                    break;
                case 19:
                    Log.d(TAG, "Mikro output in Hex: " + element);
                    val = Integer.parseInt(element, 16);
                    outputbitI = val;
                    outputbit = Integer.toBinaryString(0x10000 | outputbitI).substring(1);
                    Log.d(TAG, "Mikro FTM prgrammable output: " + outputbit);
                    char ch1 [] = outputbit.toCharArray();
                    tempOutput = new int [ch1.length];
                    for (int y=0;y<ch1.length;y++){
                        int m=(ch1.length-1)-y;
                        tempOutput[m]=Integer.parseInt(""+ch1[y]);
                        Log.d(TAG, "Mikro FTM prgrammable output by bit "+m+": " + tempOutput[m]);
                    }

                    break;
            }
        }

        final String finalthdI=thdcurrent,finalthdV=thdvolt,finalUI=undercurrent,finalOI=overcurrent,finalUV=undervolt,finalOV=overvolt,finalCap=cap_error;
        final int[] finalTempAlarm = tempAlarm;
        final int[] finalTempOutput= tempOutput;

        runOnUiThread(new Runnable() {

            public void run() {
                thd_volt_edit.setText(finalthdV);
                thd_current_edit.setText(finalthdI);
                Ucurrent_edit.setText(finalUI);
                Ocurrent_edit.setText(finalOI);
                Uvolt_edit.setText(finalUV);
                Ovolt_edit.setText(finalOV);
                //freq_edit.setText(finalCap);

                //display output alarm buttons


                //display output alarm buttons
                if (finalTempAlarm[15] ==1) {
                    counter_alarm.setChecked(true);
                } else {
                    counter_alarm.setChecked(false);
                }

                if (finalTempAlarm[14] ==1) {
                    timer_alarm.setChecked(true);
                } else {
                    timer_alarm.setChecked(false);
                }

                if (finalTempAlarm[13] ==1) {
                    eep_alarm.setChecked(true);
                } else {
                    eep_alarm.setChecked(false);
                }

                if (finalTempAlarm[12] ==1) {
                    clock_alarm.setChecked(true);
                } else {
                    clock_alarm.setChecked(false);
                }

                if (finalTempAlarm[11] ==1) {
                    ct_alarm.setChecked(true);
                } else {
                    ct_alarm.setChecked(false);
                }

                if (finalTempAlarm[10] ==1) {
                    novolt_alarm.setChecked(true);
                } else {
                    novolt_alarm.setChecked(false);
                }

                if (finalTempAlarm[9] ==1) {
                    step_alarm.setChecked(true);
                } else {
                    step_alarm.setChecked(false);
                }

                if (finalTempAlarm[8] ==1) {
                    ocom_alarm.setChecked(true);
                } else {
                    ocom_alarm.setChecked(false);
                }

                if (finalTempAlarm[7] ==1) {
                    ucom_alarm.setChecked(true);
                } else {
                    ucom_alarm.setChecked(false);
                }

                if (finalTempAlarm[6] ==1) {
                    cap_alarm.setChecked(true);
                } else {
                    cap_alarm.setChecked(false);
                }

                if (finalTempAlarm[5] ==1) {
                    ovolt_alarm.setChecked(true);
                } else {
                    ovolt_alarm.setChecked(false);
                }

                if (finalTempAlarm[4] ==1) {
                    uvolt_alarm.setChecked(true);
                } else {
                    uvolt_alarm.setChecked(false);
                }

                if (finalTempAlarm[3] ==1) {
                    ocurrent_alarm.setChecked(true);
                } else {
                    ocurrent_alarm.setChecked(false);
                }

                if (finalTempAlarm[2] ==1) {
                    ucurrent_alarm.setChecked(true);
                } else {
                    ucurrent_alarm.setChecked(false);
                }

                if (finalTempAlarm[1] ==1) {
                    thdcurrent_alarm.setChecked(true);
                } else {
                    thdcurrent_alarm.setChecked(false);
                }

                if (finalTempAlarm[0] ==1) {
                    thdvolt_alarm.setChecked(true);
                } else {
                    thdvolt_alarm.setChecked(false);
                }

                //display output radio buttons

                if (finalTempOutput[15] ==1) {
                    counter_output.setChecked(true);
                } else {
                    counter_output.setChecked(false);
                }

                if (finalTempOutput[14] ==1) {
                    timer_output.setChecked(true);
                } else {
                    timer_output.setChecked(false);
                }

                if (finalTempOutput[13] ==1) {
                    eep_output.setChecked(true);
                } else {
                    eep_output.setChecked(false);
                }

                if (finalTempOutput[12] ==1) {
                    clock_output.setChecked(true);
                } else {
                    clock_output.setChecked(false);
                }

                if (finalTempOutput[11] ==1) {
                    ct_output.setChecked(true);
                } else {
                    ct_output.setChecked(false);
                }

                if (finalTempOutput[10] ==1) {
                    novolt_output.setChecked(true);
                } else {
                    novolt_output.setChecked(false);
                }

                if (finalTempOutput[9] ==1) {
                    step_output.setChecked(true);
                } else {
                    step_output.setChecked(false);
                }

                if (finalTempOutput[8] ==1) {
                    ocom_output.setChecked(true);
                } else {
                    ocom_output.setChecked(false);
                }

                if (finalTempOutput[7] ==1) {
                    ucom_output.setChecked(true);
                } else {
                    ucom_output.setChecked(false);
                }

                if (finalTempOutput[6] ==1) {
                    cap_output.setChecked(true);
                } else {
                    cap_output.setChecked(false);
                }

                if (finalTempOutput[5] ==1) {
                    ovolt_output.setChecked(true);
                } else {
                    ovolt_output.setChecked(false);
                }

                if (finalTempOutput[4] ==1) {
                    uvolt_output.setChecked(true);
                } else {
                    uvolt_output.setChecked(false);
                }

                if (finalTempOutput[3] ==1) {
                    ocurrent_output.setChecked(true);
                } else {
                    ocurrent_output.setChecked(false);
                }

                if (finalTempOutput[2] ==1) {
                    ucurrent_output.setChecked(true);
                } else {
                    ucurrent_output.setChecked(false);
                }

                if (finalTempOutput[1] ==1) {
                    thdcurrent_output.setChecked(true);
                } else {
                    thdcurrent_output.setChecked(false);
                }

                if (finalTempOutput[0] ==1) {
                    thdvolt_output.setChecked(true);
                } else {
                    thdvolt_output.setChecked(false);
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
