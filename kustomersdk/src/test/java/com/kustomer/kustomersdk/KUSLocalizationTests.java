package com.kustomer.kustomersdk;


import android.content.Context;

import com.kustomer.kustomersdk.Helpers.KUSLocalization;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class KUSLocalizationTests {

    private Context mContext;

    @Before
    public void setUp(){
        mContext = RuntimeEnvironment.application.getApplicationContext();
    }

    @Test
    public void testRTL(){

        boolean systemIsLRT= KUSLocalization.getSharedInstance().isLTR();
        KUSLocalization.getSharedInstance().setUserLocale(null);
        KUSLocalization.getSharedInstance().updateKustomerLocaleWithFallback(mContext);
        KUSLocalization.getSharedInstance().updateConfig(mContext);
        assertEquals(systemIsLRT, KUSLocalization.getSharedInstance().isLTR());

        KUSLocalization.getSharedInstance().setUserLocale( new Locale("en"));
        KUSLocalization.getSharedInstance().updateKustomerLocaleWithFallback(mContext);
        KUSLocalization.getSharedInstance().updateConfig(mContext);
        assertEquals(true, KUSLocalization.getSharedInstance().isLTR());

    }

    /* We have to use qualifiers in roboelectric as urdu is not supported directly in testing*/
    @Config(qualifiers="ur")
    @Test
    public void testUrduRTL(){
        assertEquals(false, KUSLocalization.getSharedInstance().isLTR());
    }

    @Test
    public void testLocale(){

        Locale expectedLocale = Locale.getDefault();
        KUSLocalization.getSharedInstance().setUserLocale(null);
        KUSLocalization.getSharedInstance().updateKustomerLocaleWithFallback(mContext);
        KUSLocalization.getSharedInstance().updateConfig(mContext);
        assertEquals(expectedLocale, Locale.getDefault());

        expectedLocale =new Locale("en");
        KUSLocalization.getSharedInstance().setUserLocale(expectedLocale);
        KUSLocalization.getSharedInstance().updateKustomerLocaleWithFallback(mContext);
        KUSLocalization.getSharedInstance().updateConfig(mContext);
        assertEquals(expectedLocale, Locale.getDefault());
    }

    /* We have to use qualifiers in roboelectric as urdu is not supported directly in testing*/
    @Config(qualifiers="ur")
    @Test
    public void testUrduLocale(){
        Locale expectedLocale = new Locale("ur");
        assertEquals(expectedLocale, Locale.getDefault());
    }


    @Test
    public void testLocalizedString(){

        KUSLocalization.getSharedInstance().setUserLocale(null);
        KUSLocalization.getSharedInstance().updateKustomerLocaleWithFallback(mContext);
        KUSLocalization.getSharedInstance().updateConfig(mContext);
        assertEquals(KUSLocalization.getSharedInstance().localizedString(mContext, "com_kustomer_attachment"), "Attachment");
        assertEquals(KUSLocalization.getSharedInstance().localizedString(mContext,"com_kustomer_just_now"), "Just now");
        assertEquals(KUSLocalization.getSharedInstance().localizedString(mContext,"com_kustomer_gallery"), "Gallery");
        assertEquals(KUSLocalization.getSharedInstance().localizedString(mContext,"com_kustomer_cancel"), "Cancel");
        assertEquals(KUSLocalization.getSharedInstance().localizedString(mContext,"com_kustomer_camera"), "Camera");

        KUSLocalization.getSharedInstance().setUserLocale(new Locale("en"));
        KUSLocalization.getSharedInstance().updateKustomerLocaleWithFallback(mContext);
        KUSLocalization.getSharedInstance().updateConfig(mContext);
        assertEquals(KUSLocalization.getSharedInstance().localizedString(mContext,"com_kustomer_attachment"), "Attachment");
        assertEquals(KUSLocalization.getSharedInstance().localizedString(mContext,"com_kustomer_just_now"), "Just now");
        assertEquals(KUSLocalization.getSharedInstance().localizedString(mContext,"com_kustomer_gallery"), "Gallery");
        assertEquals(KUSLocalization.getSharedInstance().localizedString(mContext,"com_kustomer_cancel"), "Cancel");
        assertEquals(KUSLocalization.getSharedInstance().localizedString(mContext,"com_kustomer_camera"), "Camera");

    }

    /* We have to use qualifiers in roboelectric as urdu is not supported directly in testing*/
    @Config(qualifiers="ur")
    @Test
    public void testLocalizedUrduString(){

        assertEquals(KUSLocalization.getSharedInstance().localizedString(mContext,"com_kustomer_cancel"), "منسوخ کریں");
        assertEquals(KUSLocalization.getSharedInstance().localizedString(mContext,"com_kustomer_attachment"), "منسلکہ");
        assertEquals(KUSLocalization.getSharedInstance().localizedString(mContext,"com_kustomer_just_now"), "ابھی ابھی");
        assertEquals(KUSLocalization.getSharedInstance().localizedString(mContext,"com_kustomer_camera"), "کیمرے");
        assertEquals(KUSLocalization.getSharedInstance().localizedString(mContext,"com_kustomer_gallery"), "نگارخانہ");

    }

}
