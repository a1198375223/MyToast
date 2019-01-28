package com.example.mytoast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 实现一个自定义的Toast
 *
 * Toast提供了setView(View view) 接口来确保我们可以实现自定义的布局
 * 由于Toast只能支持在有looper环境下使用, 我们将封装它让它在非looper环境下也可以显示
 */
public class Toasty {
    // make toasty can show in thread which no looper
    private static Handler sHandler = new Handler(Looper.getMainLooper());

    public static final int CUSTOM_TOAST_DEFAULT_COLOR = Color.GRAY;
    public static final int CUSTOM_TOAST_SUCCESS_COLOR = Color.GREEN;
    public static final int CUSTOM_TOAST_ERROR_COLOR = Color.RED;
    public static final int CUSTOM_TOAST_WARNING_COLOR = Color.YELLOW;

    private static int sTextColor = Color.WHITE;
    private static int sTextSize = 14;
    private static Typeface sTypeface = Typeface.DEFAULT;

    private static int sCustomViewResource = -1;
    private static OnCustomViewInflate sListener;
    private static int sGravity = Gravity.BOTTOM;
    private static int Xoffset = 0;
    private static int Yoffset = 100;


    public static void showSuccess(CharSequence text) {
        showSuccess(text, Toast.LENGTH_SHORT);
    }

    public static void showSuccess(CharSequence text, int duration) {
        showSuccess(text, sTextColor, duration);
    }

    public static void showSuccess(CharSequence text, int textColor, int duration) {
        showSuccess(text, textColor, R.drawable.toast_success_while_icon, duration);
    }

    public static void showSuccess(CharSequence text, int textColor, int iconRes, int duration) {
        showSuccess(text, textColor, iconRes, CUSTOM_TOAST_SUCCESS_COLOR, duration);
    }

    public static void showSuccess(CharSequence text, int textColor, int iconRes, int tintColor, int duration) {
        showCustom(text, textColor, iconRes, tintColor, duration);
    }




    public static void showError(CharSequence text) {
        showError(text, Toast.LENGTH_SHORT);
    }

    public static void showError(CharSequence text, int duration) {
        showError(text, sTextColor, duration);
    }

    public static void showError(CharSequence text, int textColor, int duration) {
        showError(text, textColor, R.drawable.toast_error_while_icon, duration);
    }

    public static void showError(CharSequence text, int textColor, int iconRes, int duration) {
        showError(text, textColor, iconRes, CUSTOM_TOAST_ERROR_COLOR, duration);
    }

    public static void showError(CharSequence text, int textColor, int iconRes, int tintColor, int duration) {
        showCustom(text, textColor, iconRes, tintColor, duration);
    }


    public static void showWarning(CharSequence text) {
        showWarning(text, Toast.LENGTH_SHORT);
    }

    public static void showWarning(CharSequence text, int duration) {
        showWarning(text, sTextColor, duration);
    }

    public static void showWarning(CharSequence text, int textColor, int duration) {
        showWarning(text, textColor, R.drawable.toast_wran_while_icon, duration);
    }

    public static void showWarning(CharSequence text, int textColor, int iconRes, int duration) {
        showWarning(text, textColor, iconRes, CUSTOM_TOAST_WARNING_COLOR, duration);
    }

    public static void showWarning(CharSequence text, int textColor, int iconRes, int tintColor, int duration) {
        showCustom(text, textColor, iconRes, tintColor, duration);
    }





    public static void showCustom(final CharSequence text) {
        showCustom(text, Toast.LENGTH_SHORT);
    }

    public static void showCustom(CharSequence text, int duration) {
        showCustom(text, sTextColor, duration);
    }

    public static void showCustom(CharSequence text, int textColor, int duration) {
        showCustom(text, textColor, R.drawable.toast_normal_while_icon, duration);
    }

    public static void showCustom(CharSequence text, int textColor, int iconRes, int duration) {
        showCustom(text, textColor, iconRes, true, duration);
    }

    public static void showCustom(CharSequence text, int textColor, int iconRes, boolean withIcon, int duration) {
        show(AppUtils.getInstance().getTopActivityOrApp(), text, textColor, iconRes,
                CUSTOM_TOAST_DEFAULT_COLOR, true, withIcon, duration);
    }

    public static void showCustom(CharSequence text, int textColor, int iconRes, int tintCololr, int duration) {
        show(AppUtils.getInstance().getTopActivityOrApp(), text, textColor, iconRes, tintCololr, true, true, duration);
    }

    public static void showCustom(CharSequence text, int iconRes, boolean withIcon) {
        showCustom(text, iconRes, withIcon, Toast.LENGTH_SHORT);
    }

    public static void showCustom(CharSequence text, int iconRes, boolean withIcon, int duration) {
        show(AppUtils.getInstance().getTopActivityOrApp(), text, sTextColor, iconRes, CUSTOM_TOAST_DEFAULT_COLOR, true, withIcon, duration);
    }

    public static void showCustom(CharSequence text, boolean shouldTint, int tintColor) {
        showCustom(text, shouldTint, tintColor, Toast.LENGTH_SHORT);
    }

    public static void showCustom(CharSequence text, boolean shouldTint, int tintColor, int duration) {
        show(AppUtils.getInstance().getTopActivityOrApp(), text, sTextColor, R.drawable.toast_normal_while_icon, tintColor, shouldTint, true, duration);
    }

    private static void show(@NonNull final Context context, final CharSequence text, final int textColor,
                             final int iconRes, final int tintColor, final boolean shouldTint,
                             final boolean withIcon, final int duration) {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                custom(context, text, textColor, iconRes, tintColor, shouldTint, withIcon, duration).show();
            }
        });
    }



    @SuppressLint({"ShowToast", "InflateParams"})
    private static Toast custom(@NonNull Context context, CharSequence text, int textColor,
                                int iconRes, int tintColor, boolean shouldTint,
                                boolean withIcon, int duration) {
        Toast customToast = Toast.makeText(context, "", duration);

        TextView toastText;
        ImageView toastImage;


        View rootView;
         // set custom view or default view
        if (sCustomViewResource != -1) {
            // when use custom layout, you must at lease provide R.id.toast_image -> ImageView
            // and R.id.toast_text -> TextView
            rootView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(sCustomViewResource, null);
            customToast.setView(rootView);

            // if you want to change custom layout appearance
            // implement this interface
            if (sListener != null) {
                sListener.onCustomViewInflate(rootView);
            }

            toastImage = (ImageView) rootView.findViewById(R.id.toast_image);
            toastText = (TextView) rootView.findViewById(R.id.toast_text);
        } else {
            rootView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.toast_layout, null);
            toastText = (TextView) rootView.findViewById(R.id.toast_text);
            toastImage = (ImageView) rootView.findViewById(R.id.toast_image);
            customToast.setView(rootView);
        }

        // set main background
        Drawable mainBackground =ContextCompat.getDrawable(context, R.drawable.toast_frame);
        if (shouldTint && mainBackground != null) {
            mainBackground.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            rootView.setBackground(mainBackground);
        } else {
            rootView.setBackgroundDrawable(mainBackground);
        }


        // set gravity
        customToast.setGravity(sGravity, Xoffset, Yoffset);

        // set text
        //customToast.setText(text);
        toastText.setText(text);
        toastText.setTextColor(textColor);
        toastText.setTextSize(TypedValue.COMPLEX_UNIT_SP, sTextSize);
        toastText.setTypeface(sTypeface);


        // set image
        if (withIcon) {
            toastImage.setImageDrawable(ContextCompat.getDrawable(context, iconRes));
        } else {
            toastImage.setVisibility(View.GONE);
        }

        return customToast;
    }


    public interface OnCustomViewInflate {
        void onCustomViewInflate(View rootView);
    }

    public static Config Builder() {
        return new Config();
    }


    // set methods
    public static class Config {
        private int mCustomViewResource = -1;
        private OnCustomViewInflate mListener;
        private int mGravity = Gravity.BOTTOM;
        private int Xoffset = 0;
        private int Yoffset = 100;
        private int mTextColor = Color.WHITE;
        private int mTextSize = 14;
        private Typeface mTypeface = Typeface.DEFAULT;

        public Config setTextColor(int textColor) {
            this.mTextColor = textColor;
            return this;
        }

        public Config setTextSize(int textSize) {
            this.mTextSize = textSize;
            return this;
        }

        public Config setTypeface(Typeface typeface) {
            this.mTypeface = typeface;
            return this;
        }

        public Config setCustomViewResource(int customViewResource) {
            this.mCustomViewResource = customViewResource;
            return this;
        }

        public Config setListener(OnCustomViewInflate listener) {
            this.mListener = listener;
            return this;
        }

        public Config reset(){
            this.mCustomViewResource = -1;
            this.mGravity = Gravity.BOTTOM;
            this.Xoffset = 0;
            this.Yoffset = 100;
            this.mTextColor = Color.WHITE;
            this.mTextSize = 14;
            this.mTypeface = Typeface.DEFAULT;
            this.mListener = null;
            return this;
        }

        public Config setGravity(int gravity, int Xoffset, int Yoffset) {
            this.mGravity = gravity;
            this.Xoffset = Xoffset;
            this.Yoffset = Yoffset;
            return this;
        }

        public void apply() {
            Toasty.sCustomViewResource = this.mCustomViewResource;
            Toasty.sGravity = this.mGravity;
            Toasty.Xoffset = this.Xoffset;
            Toasty.Yoffset = this.Yoffset;
            Toasty.sTextColor = this.mTextColor;
            Toasty.sTextSize = this.mTextSize;
            Toasty.sTypeface = this.mTypeface;
            Toasty.sListener = this.mListener;
        }
    }



}
