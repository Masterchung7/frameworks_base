
package com.android.systemui.statusbar.policy;

import java.lang.ref.WeakReference;

import com.android.internal.telephony.PhoneStateIntentReceiver;

import android.R.integer;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class SignalText extends TextView {

    private static final String TAG = "SignalText";
    int dBm = 0;
    int ASU = 0;
    private SignalStrength signal;
    private boolean mAttached;
    private boolean mInAirplaneMode;
    private static final int STYLE_HIDE = 0;
    private static final int STYLE_SHOW = 1;
    private static final int STYLE_SHOW_DBM = 2;
    private static final int EVENT_SIGNAL_STRENGTH_CHANGED = 200;
    private int style;
    private Handler mHandler;
    private Context mContext;
    protected int mSignalColor = com.android.internal.R.color.holo_blue_light;
    private PhoneStateIntentReceiver mPhoneStateReceiver;
    
    private SignalText mSignalText;
    
    private static class MyHandler extends Handler {
    	private WeakReference<SignalText> mSignalText;

         public MyHandler(SignalText activity) {
             mSignalText = new WeakReference<SignalText>(activity);
         } 
       
        @Override
        public void handleMessage(Message msg) {
        	SignalText st = mSignalText.get();
            if (st == null) {
                return;
            }
            if (msg.what == EVENT_SIGNAL_STRENGTH_CHANGED) {
            	st.updateSignalStrength();
            }
        }
    }

    public SignalText(Context context) {
        this(context, null);
    }

    public SignalText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignalText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
        	Log.d("db","attached to Window");
            mAttached = true;
           // ((TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE)).listen(
           //         mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE
           //                 | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

            mHandler = new MyHandler(this);
            SettingsObserver settingsObserver = new SettingsObserver(mHandler);
            settingsObserver.observe();
            mPhoneStateReceiver = new PhoneStateIntentReceiver(mContext, mHandler);
            mPhoneStateReceiver.notifySignalStrength(EVENT_SIGNAL_STRENGTH_CHANGED);
            mPhoneStateReceiver.registerIntent();
            updateSettings();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mAttached = false;
            mPhoneStateReceiver.unregisterIntent();
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUSBAR_SIGNAL_TEXT), false,
                    this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUSBAR_SIGNAL_TEXT_COLOR), false,
                    this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    private void updateSettings() {
        ContentResolver resolver = getContext().getContentResolver();
        mSignalColor = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_SIGNAL_TEXT_COLOR,
                0xFF33B5E5);
        if (mSignalColor == Integer.MIN_VALUE) {
            // flag to reset the color
            mSignalColor = 0xFF33B5E5;
        }
        setTextColor(mSignalColor);
        updateSignalText();
    }
    
    public void updateSignalStrength(){
    	dBm = mPhoneStateReceiver.getSignalStrengthDbm();

        if (-1 == dBm) dBm = 0;

        ASU = mPhoneStateReceiver.getSignalStrengthLevelAsu();

        if (-1 == ASU) ASU = 0;
        Log.d("db","updateSignalStrength - "+ dBm);
        updateSignalText();
    }

    private void updateSignalText() {
    	Log.d("db","updating signal text");
        style = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.STATUSBAR_SIGNAL_TEXT, STYLE_HIDE);

        if (style == STYLE_SHOW) {
            String result = Integer.toString(dBm);

            setText(result + " ");
        } else if (style == STYLE_SHOW_DBM) {
            String result = Integer.toString(dBm) + " dBm ";

            SpannableStringBuilder formatted = new SpannableStringBuilder(result);
            int start = result.indexOf("d");

            CharacterStyle style = new RelativeSizeSpan(0.7f);
            formatted.setSpan(style, start, start + 3, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);

            setText(formatted);
        } 
    }

    /*
     * Phone listener to update signal information
     */
/*    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            signal = signalStrength;

            if (signal != null) {
                if (signal.isGsm()) {
                    ASU = signal.getGsmSignalStrength();
                    if (ASU != -1)
                        dBm = -113 + (2 * ASU);
                    else
                        dBm = 0;
                } else {
                    dBm = signal.getCdmaDbm();
                }
            } else {
                ASU = 0;
                dBm = 0;
            }

            // update text if it's visible
            if (mAttached)
                updateSignalText();
        }

        public void onServiceStateChanged(ServiceState serviceState) {
            mInAirplaneMode = serviceState.getState() == ServiceState.STATE_POWER_OFF;
            if (mAttached)
            	updateSignalText();
        }

    }; */

}
