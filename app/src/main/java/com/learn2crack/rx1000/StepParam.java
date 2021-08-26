package com.learn2crack.rx1000;

import android.util.Log;

import java.text.DecimalFormat;

import static android.content.ContentValues.TAG;

public class StepParam {

    public static int getstep0(double value, int min, int max){
        double formatvalue=0;
        int IntValue=0;
        formatvalue=value*100;
       if (formatvalue>=min && formatvalue<=max){
            IntValue = (int)Math.round(formatvalue);
        } else if (formatvalue<min || formatvalue>max ){
            IntValue=0;
            Log.d(TAG,"Mikro Not allowed ");
        }
        Log.d(TAG, "\nMikro round number is: "+ formatvalue );
        return IntValue;
    }

    public static int getstep1(double value, int min, int max){
        DecimalFormat df2 = new DecimalFormat("0.00");
        double formatvalue=0, remainder=0, result=0;
        int IntValue=0;
        formatvalue=value*100;
        formatvalue =Double.valueOf(df2.format(formatvalue));

        if (formatvalue>1000 && formatvalue<=max){
            remainder=formatvalue%50;
            double round = formatvalue%100;
            if (round == 50){
                result = formatvalue;
            } else if (round<50){
                result = formatvalue - remainder;
            } else if (round>50){
                result = formatvalue + (50-remainder);
            }
            IntValue = (int) result;
            Log.d(TAG,"Mikro Step 0.5: "+ IntValue);
        } else if (formatvalue>100 && formatvalue<1000){
            remainder=formatvalue%10;
            double round = formatvalue%10;
            if (round == 5){
                result = formatvalue;
            } else if (round<5){
                result = formatvalue - remainder;
            } else if (round>5){
                result = formatvalue + (10-remainder);
            }
            IntValue = (int) result;
            Log.d(TAG," Mikro Step 0.1: "+ IntValue);
        }else if (formatvalue>=min && formatvalue<=100 ){
            IntValue = (int) formatvalue;
            Log.d(TAG,"Mikro Step 0.01: "+ IntValue);
        } else if (formatvalue<min || formatvalue>max ){
            Log.d(TAG,"Mikro Not allowed ");
        }
        Log.d(TAG, "\nMikro round number is: "+ formatvalue + " "+ IntValue );
        return IntValue;
        //   String result=formatValue_current(value);
    }

    public static int getstep2(double value, int min, int max){
        DecimalFormat df2 = new DecimalFormat("0.00");
        double formatvalue=0, remainder=0, result=0;
        int IntValue=0;
        formatvalue=value*100;
        formatvalue =Double.valueOf(df2.format(formatvalue));
        Log.d(TAG,"Mikro: " + value + " "+ formatvalue);

        if (formatvalue>=min && formatvalue<1000){
            remainder=formatvalue%5;
            double round = formatvalue%10;
            round = Double.valueOf(df2.format(round));
           // Log.d(TAG,"Mikro: " + round + " "+ remainder);
            if (round == 5){
                result = formatvalue;
            } else if (round<5){
                result = formatvalue - remainder;
            } else if (round>5){
                result = formatvalue + (5-remainder);
            }
            IntValue = (int) result;
        } else if (formatvalue>=1000 && formatvalue<=max){
            IntValue = (int) formatvalue;
        } else if (formatvalue<min || formatvalue>max ){
            IntValue=0;
           Log.d(TAG,"Mikro Not allowed ");
        }
        Log.d(TAG, "\nMikro Step2 number is: "+ formatvalue );
        return IntValue;
    }

    public static int getstep3(double value, int min, int max){
        double formatvalue=0;
        int IntValue=0;
        formatvalue=value*100;
        if (formatvalue>=min && formatvalue<=max){
            IntValue = (int) Math.round(formatvalue);
            Log.d(TAG,"Mikro in loop: "+ IntValue);
        } else if (formatvalue<min || formatvalue>max ){
            IntValue=0;
            Log.d(TAG,"Mikro Not allowed ");
        }
        Log.d(TAG, "\nMikro round number is: "+ formatvalue );
        return IntValue;
    }
}
