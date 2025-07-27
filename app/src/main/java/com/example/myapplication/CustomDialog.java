package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;

public class CustomDialog {

    public interface DialogCallback {
        void onConfirm();
        void onCancel();
    }

    // ✅ Default version using your predefined colors
    public static void showDialog(Context context,
                                  String title,
                                  String message,
                                  String confirmButtonText,
                                  String cancelButtonText,
                                  int iconResId,
                                  DialogCallback callback) {
        // Call the overloaded version with default colors
        showDialog(context, title, message, confirmButtonText, cancelButtonText, iconResId,
                R.color.confirmBtn, R.color.cancelBtn, callback);
    }

    // ✅ Overloaded version that allows custom button colors
    public static void showDialog(Context context,
                                  String title,
                                  String message,
                                  String confirmButtonText,
                                  String cancelButtonText,
                                  int lottieRawResId,
                                  int confirmColorResId,
                                  int cancelColorResId,
                                  DialogCallback callback) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_custom_layout, null);

//        ImageView icon = dialogView.findViewById(R.id.icon);
//        if (icon != null) {
//            icon.setImageResource(iconResId);
//        }

        LottieAnimationView lottieIcon = dialogView.findViewById(R.id.icon);
        if (lottieIcon != null) {
            lottieIcon.setAnimation(lottieRawResId);
            lottieIcon.playAnimation();
        }

        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        TextView messageText = dialogView.findViewById(R.id.dialog_message);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        dialogTitle.setText(title);
        messageText.setText(message);
        btnConfirm.setText(confirmButtonText);

        if (cancelButtonText != null && !cancelButtonText.isEmpty()) {
            btnCancel.setText(cancelButtonText);
            btnCancel.setVisibility(View.VISIBLE);
        } else {
            btnCancel.setVisibility(View.GONE);
        }

        // 🔴 Apply dynamic colors here
        btnConfirm.setBackgroundTintList(ContextCompat.getColorStateList(context, confirmColorResId));
        btnCancel.setBackgroundTintList(ContextCompat.getColorStateList(context, cancelColorResId));

        builder.setView(dialogView);
        AlertDialog dialog = builder.setCancelable(false).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnConfirm.setOnClickListener(v -> {
            if (callback != null) callback.onConfirm();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            if (callback != null) callback.onCancel();
            dialog.dismiss();
        });

        dialog.show();
    }
}
