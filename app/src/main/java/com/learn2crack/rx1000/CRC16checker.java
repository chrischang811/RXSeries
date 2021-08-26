package com.learn2crack.rx1000;

import android.util.Log;

import java.math.BigInteger;

public class CRC16checker {
    private static final String TAG = CRC16checker.class.getSimpleName();

    public  int[] getCRC(String concatstring) {
        int reg = 0;
        boolean check = false;
        boolean bit = false;
        int poly = 0;
        int i = 0;
        int j = 0;
        int LoopCount = 0;
        byte temp = 0;
        int crc[]= new int[2];

        Log.d(TAG, "\nMikro CRC: Received is: "+ concatstring );

        BigInteger v = new BigInteger(concatstring, 16);
        byte[] array = v.toByteArray();
        for (byte d : array) {
            System.out.format("%x ", d);
        }

        LoopCount = array.length;

        reg = 0xFFFF;

        for (j = 0; (j < LoopCount); j++) {
            temp = array[j];
            System.out.println(temp + " ");
            poly = 0xA001;
            reg = reg ^ (temp & 0xff);

            for (i = 0; (i < 8); i++) {
                bit = (reg & 1) != 0; //check bit whether to xor
                reg = reg / 2; // shift byte to the right by 1 step
                if (bit == true) {
                    reg = reg ^ poly;
                    // System.out.println("reg: " + Integer.toHexString(reg) + " ");
                }
            }
        }

        crc[1] = reg & 0xFF;
        crc[0] = reg / 256;
        Log.d(TAG,"\nMikro crc high " + Integer.toHexString(crc[1]));
        Log.d(TAG,"\nMikro crc low " + Integer.toHexString(crc[0]));

        return crc;

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

        Log.d(TAG, "\nMikro CRC: Received is: "+ concatstring );
        BigInteger v = new BigInteger(concatstring, 16);
        byte[] array = v.toByteArray();
        for (byte d : array) {
            System.out.format(" %x ", d);
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
                }
            }
        }

        int crcL = reg & 0xFF;
        int crcH = reg / 256;

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