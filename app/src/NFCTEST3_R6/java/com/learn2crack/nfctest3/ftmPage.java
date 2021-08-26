package com.learn2crack.rx1000;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.st.st25sdk.Crc;
import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.STLog;
import com.st.st25sdk.type5.st25dv.ST25DVTag;

import static com.st.st25sdk.STException.STExceptionCode.CMD_FAILED;

public class ftmPage extends AppCompatActivity {

    private ActionCode mAction;

    // To Do compute exactly and dynamically
    private int mMaxPayloadSizeTx = 220;
    private int mMaxPayloadSizeRx = 32;

    private int mOffset;


    private byte[] mFTMmsg;
    private byte[] mBuffer;

    private final int FAST_TRANSFER_COMMAND = 0x00;
    private final int FAST_TRANSFER_ANSWER  = 0x01;
    private final int FAST_TRANSFER_ACK     = 0x02;

    private final int FAST_TRANSFER_OK      = 0x00;
    private final int FAST_TRANSFER_ERROR   = 0x01;

    private final int ERROR     = -1;
    private final int TRY_AGAIN = 0;
    private final int OK        = 1;

    private final int CHAINED_HEADER_SIZE = 13;
    private final int SIMPLE_HEADER_SIZE = 5;

    public ST25DVTag mST25DVTag;

    private Button ftmReadButton;
    private Button ftmWriteButton;
    private Button ftmWriteButton2;
    private Button ftmWriteButton3;
    private Button ftmWriteButton4;
    private Button ftmCheckButton;

    private EditText ftmWriteText;
    private TextView ftmMsgText;


    private PendingIntent mPendingIntent;


    private Runnable runnableCode;
    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.read_index);

        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        ftmReadButton = (Button) findViewById(R.id.btn_write_ftm);
        ftmWriteButton = (Button) findViewById(R.id.btn_write_ftm);
        ftmWriteButton2 = (Button) findViewById(R.id.btn_write_ftm2);
        ftmWriteButton3 = (Button) findViewById(R.id.btn_write_ftm3);
        ftmWriteButton4 = (Button) findViewById(R.id.btn_write_ftm4);
        ftmCheckButton = (Button) findViewById(R.id.btn_check_ftm);
        ftmWriteText = (EditText) findViewById(R.id.editText_write );


    //PS commented
       /* mST25DVTag = (ST25DVTag) MainActivity.getTag();
        ftmReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mST25DVTag == null) {
                    return;
                }
                readFTM();
            }
        });*/

        ftmWriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mST25DVTag == null) {
                    return;
                }

                mBuffer = new byte[8];
                mBuffer [0] = (byte)0x01;   ///
                mBuffer [1] = (byte)0x03;
                mBuffer [2] = (byte)0x00;
                mBuffer [3] = (byte)0x10;
                mBuffer [4] = (byte)0x00;
                mBuffer [5] = (byte)0x0c;
                mBuffer [6] = (byte)0x44;
                mBuffer [7] = (byte)0x0a;

                writeFTM();
            }
        });


        ftmWriteButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mST25DVTag == null) {
                    return;
                }

                mBuffer = new byte[8];
                mBuffer [0] = (byte)0x01;
                mBuffer [1] = (byte)0x03;
                mBuffer [2] = (byte)0x00;
                mBuffer [3] = (byte)0x10;
                mBuffer [4] = (byte)0x00;
                mBuffer [5] = (byte)0x01;
                mBuffer [6] = (byte)0x85;
                mBuffer [7] = (byte)0xcf;

                writeFTM();
            }
        });

        ftmWriteButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mST25DVTag == null) {
                    return;
                }

                mBuffer = new byte[8];
                mBuffer [0] = (byte)0x01;
                mBuffer [1] = (byte)0x06;
                mBuffer [2] = (byte)0x01;
                mBuffer [3] = (byte)0x04;
                mBuffer [4] = (byte)0x1f;
                mBuffer [5] = (byte)0x40;
                mBuffer [6] = (byte)0xc0;
                mBuffer [7] = (byte)0x37;

                writeFTM();
            }
        });

        ftmWriteButton4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mST25DVTag == null) {
                    return;
                }

                mBuffer = new byte[8];
                mBuffer [0] = (byte)0x01;
                mBuffer [1] = (byte)0x06;
                mBuffer [2] = (byte)0x01;
                mBuffer [3] = (byte)0x04;
                mBuffer [4] = (byte)0x00;
                mBuffer [5] = (byte)0x05;
                mBuffer [6] = (byte)0x09;
                mBuffer [7] = (byte)0xf4;

                writeFTM();
            }
        });

        ftmCheckButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.postDelayed(runnableCode,200);
                /*String text;
                int textLength;
                text = ftmWriteText.getText().toString();
                textLength=text.length();
                Toast.makeText(ftmPage.this, textLength, Toast.LENGTH_LONG).show();
                */
            }
        });

         runnableCode = new Runnable() {
            @Override
            public void run() {
                // Do something here on the main thread
                checkHOST();

                Log.d("Handlers", "Called check host message");
            }
        };

    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        //PS commented
     //if (MainActivity.getNfcAdater() != null) {
     //      MainActivity.getNfcAdater().enableForegroundDispatch(this, mPendingIntent, null /*nfcFiltersArray*/, null /*nfcTechLists*/);
      //}

    }
    @Override
    public void onPause() {
        super.onPause();

      //  if (MainActivity.getNfcAdater() != null) {
      //      MainActivity.getNfcAdater().disableForegroundDispatch(this);
      //  }

    }


    public void readFTM() {
        mAction = ActionCode.READ;
        fillView(mAction);
    }

    public void writeFTM() {
        mAction = ActionCode.WRITE;
        fillView(mAction);
    }

    public void checkHOST(){
        mAction = ActionCode.CHECK;
        fillView(mAction);
    }

    private enum ActionCode { READ,
        WRITE,
        CHECK}

    public void fillView(ActionCode action) {

        new Thread(new ContentView(action)).start();
    }

    class ContentView implements Runnable {

        ActionCode mAction;

        public ContentView(ActionCode action) {
            mAction = action;

        }

        public void run() {

            switch(mAction){
                case READ:
                    try {
                        if(mST25DVTag.isMailboxEnabled(false)==false)
                        {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(ftmPage.this, "FTM MODE OFF", Toast.LENGTH_LONG).show();
                                }
                            });
                            break;
                        }
                        mFTMmsg = readMessage();
                        update_ftm_text_view(mFTMmsg);
                    } catch (final STException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(ftmPage.this, e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });

                    }
                    break;
                case WRITE:
                    sendSimpleData();
                    break;
                case CHECK:
                    checkHostMsg();
                    break;


            }

        }
    }

    public void update_ftm_text_view (byte[] message){

        int no_message = message.length;
        byte tempMsg = 0;

        String msg = "FTM Message: Total byte is " + no_message +"\n";
        ftmMsgText = (TextView) findViewById(R.id.txtFTMMsg);

        for(int x = 0; x <no_message; x++){

            tempMsg = message[x];
            int hex_int= (int)tempMsg & 0xff;
            String hex_value = Integer.toHexString(hex_int);
            msg = msg + hex_value + ' ';

        }
        final String finalMsg = msg;
        runOnUiThread(new Runnable() {
                          public void run() {
                              ftmMsgText.setText(finalMsg);
                              Toast.makeText(ftmPage.this, "Read Successful!", Toast.LENGTH_LONG).show();
                          }
                      });

    }

    public NFCTag getTag() {
        return mST25DVTag;
    }


    private int checkError(STException e) {
        STException.STExceptionCode errorCode = e.getError();
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


    private byte[] readMessage() throws STException {
        int length = mST25DVTag.readMailboxMessageLength();
        byte[] buffer;

        mBuffer = new byte[255];

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
            tmpBuffer = mST25DVTag.readMailboxMessage(offset, size);
            if (tmpBuffer.length < (size + 1) || tmpBuffer[0] != 0)
                throw new STException(CMD_FAILED);
            System.arraycopy(tmpBuffer, 1, buffer, offset,
                    tmpBuffer.length - 1);
            offset += tmpBuffer.length - 1;
        }

        return buffer;

    }

    private int sendSimpleData() {

        try {
            if (mST25DVTag.hasRFPutMsg(true)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(ftmPage.this, "RF Mailbox Not Empty", Toast.LENGTH_LONG).show();
                    }
                });
                return TRY_AGAIN;
            }
        } catch (STException e) {
            return checkError(e);
        }

        byte response;
        //mBuffer = new byte[240];
        //Arrays.fill(mBuffer, (byte) 2);

        try {
            response = mST25DVTag.writeMailboxMessage(mBuffer.length, mBuffer);
            if(response != 0x00){
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(ftmPage.this, "Write Fail", Toast.LENGTH_LONG).show();
                    }
                });

                return ERROR;
            }
        } catch (STException e) {
            return checkError(e);
        }

            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(ftmPage.this, "Write Successful!", Toast.LENGTH_LONG).show();
                }
            });

        handler.postDelayed(runnableCode,100);
        return OK;

    }


    private int checkHostMsg()
    {

        try {
            if (mST25DVTag.hasHostPutMsg(true)) {
                readFTM();
            }
            else
            {
                handler.postDelayed(runnableCode, 100);
            }
        } catch (STException e) {
            return checkError(e);
        }
        return OK;
    }

    private long computeCrc() {
        try {
            return Crc.CRC(mFTMmsg);
        } catch (Exception e) {
            return ERROR;
        }
    }

    /**
     * This function is typically used when a critical issue is detected (ex: nfcTag is null).
     * It goes back to the MainActivity.
     */
    protected void goBackToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);

        // Set the flags to flush the activity stack history
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
    }
}
