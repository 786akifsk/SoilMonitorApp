package com.example.myapplication;

import android.content.Context;
import android.hardware.usb.*;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hoho.android.usbserial.driver.*;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import java.io.IOException;
import java.util.*;

public class ImageCardActivity extends AppCompatActivity {

    TextView tempValue, humidityValue, moistureValue;

    UsbManager usbManager;
    UsbSerialPort port;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.card_view);

        tempValue = findViewById(R.id.tempValue);
        humidityValue = findViewById(R.id.humidityValue);
        moistureValue = findViewById(R.id.moistureValue);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        setupSerial();
    }

    private void setupSerial() {
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (availableDrivers.isEmpty()) return;

        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());
        if (connection == null) return;

        port = driver.getPorts().get(0);
        try {
            port.open(connection);
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

            new SerialInputOutputManager(port, listener).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final SerialInputOutputManager.Listener listener = new SerialInputOutputManager.Listener() {
        @Override
        public void onNewData(byte[] data) {
            runOnUiThread(() -> {
                String received = new String(data);
                String[] values = received.trim().split(",");

                if (values.length == 3) {
                    tempValue.setText(values[0] + " °C");
                    humidityValue.setText(values[1] + " %");
                    moistureValue.setText(values[2] + " %");
                }
            });
        }

        @Override
        public void onRunError(Exception e) {
            e.printStackTrace();
        }
    };
}
