package com.kustomer.kustomersdk.Models;

import android.content.Intent;
import android.text.Html;

import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Utils.JsonHelper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Scanner;

/**
 * Created by Junaid on 1/20/2018.
 */

public class KUSTeam extends KUSModel {

    //region Properties
    private String emoji;
    public String displayName;
    public String icon;
    //endregion

    //region LifeCycleMethods
    public KUSTeam(JSONObject json) throws KUSInvalidJsonException {
        super(json);

        displayName = JsonHelper.stringFromKeyPath(json,"attributes.displayName");
        icon = JsonHelper.stringFromKeyPath(json, "attributes.icon");

        try {

            if(icon != null) {

                String [] unicodes = icon.split("-");
                StringBuilder text = new StringBuilder();
                byte [] bytes = null;

                long emojiInt = Long.parseLong(unicodes[0], 16);
                ByteBuffer b = ByteBuffer.allocate(8);
                b.order(ByteOrder.LITTLE_ENDIAN);
                b.putLong(emojiInt);

                bytes = b.array();

                if(unicodes.length > 1){
                    long emojiInt2 = Long.parseLong(unicodes[1], 16);
                    ByteBuffer b2 = ByteBuffer.allocate(8);
                    b2.order(ByteOrder.LITTLE_ENDIAN);
                    b2.putLong(emojiInt2);

                    bytes[4] = b2.array()[0];
                    bytes[5] = b2.array()[1];
                    bytes[6] = b2.array()[2];
                    bytes[7] = b2.array()[3];
                }

                text.append(new String(bytes,"UTF-32LE"));

                emoji = text.toString();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String fullDisplay(){
        if(emoji != null){
            return String.format("%s %s",emoji,displayName);
        }

        return displayName;
    }

    private static byte[] append(final byte[]... arrays) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (arrays != null) {
            for (final byte[] array : arrays) {
                if (array != null) {
                    out.write(array, 0, array.length);
                }
            }
        }
        return out.toByteArray();
    }
    //endregion

    //region Class methods
    public String modelType(){
        return null;
    }
    public boolean enforcesModelType(){
        return false;
    }
    //endregion
}
