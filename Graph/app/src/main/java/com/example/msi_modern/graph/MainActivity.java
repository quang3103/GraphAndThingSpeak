package com.example.msi_modern.graph;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SerialInputOutputManager.Listener {

    GraphView graphTemperature, graphLightLevel, graphHumidity;
    TextView tvTitle;
    DataPoint[] tempList = new DataPoint[1];
    DataPoint[] lightList = new DataPoint[1];
    int time = 0;
    LineGraphSeries<DataPoint> seriesTemp;
    LineGraphSeries<DataPoint> seriesLight;

    UsbSerialPort port;
    private static final String ACTION_USB_PERMISSION = "com.android.recipes.USB_PERMISSION";
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";

    private void initUSBPort(){
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

        if (availableDrivers.isEmpty()) {
            Log.d("UART", "UART is not available");

        }else {
            Log.d("UART", "UART is available");

            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
            if (connection == null) {

                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
                manager.requestPermission(driver.getDevice(), usbPermissionIntent);

                manager.requestPermission(driver.getDevice(), PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0));

                return;
            } else {

                port = driver.getPorts().get(0);
                try {
                    Log.d("UART", "openned succesful");
                    port.open(connection);
                    port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                    //port.write("ABC#".getBytes(), 1000);

                    SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, this);
                    Executors.newSingleThreadExecutor().submit(usbIoManager);

                } catch (Exception e) {
                    Log.d("UART", "There is error");
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        graphTemperature = findViewById(R.id.graphTemperature);
        graphLightLevel = findViewById(R.id.graphLightLevel);
        graphHumidity = findViewById(R.id.graphHumidity);
        tvTitle = findViewById(R.id.graphTitle);

        //Toast.makeText(getApplicationContext(), "Graph", Toast.LENGTH_LONG).show();
        initUSBPort();
    }

    private void showDataOnGraph(LineGraphSeries<DataPoint> series, GraphView graph){
        if(graph.getSeries().size() > 0){
            graph.getSeries().remove(0);
        }
        graph.addSeries(series);
        series.setDrawDataPoints(true);
        series.setDataPointsRadius(10);
    }

    private String buffer = "";
    @Override
    public void onNewData(byte[] data) {
        buffer += new String(data);
        if (buffer.contains("#") && buffer.contains("!")) {
            int index_soc = buffer.indexOf("#");
            int index_eoc = buffer.indexOf("!");
            String value = buffer.substring(index_soc + 1, index_eoc);
            buffer = "";
            String[] numbers = value.split("\\s");
            if (time == 0) {
                tempList[0] = new DataPoint(time, Integer.valueOf(numbers[0]));
                seriesTemp = new LineGraphSeries<>(tempList);
                showDataOnGraph(seriesTemp, graphTemperature);

                lightList[0] = new DataPoint(time, Integer.valueOf(numbers[1]));
                seriesLight = new LineGraphSeries<>(lightList);
                showDataOnGraph(seriesLight, graphLightLevel);
                time += 1;
                sendDataToThingSpeak(numbers[0], numbers[1]);
                return;
            }
            DataPoint newPoint = new DataPoint(time, Integer.valueOf(numbers[0]));
            seriesTemp.appendData(newPoint, true, 20, true);
            showDataOnGraph(seriesTemp, graphTemperature);
            newPoint = new DataPoint(time, Integer.valueOf(numbers[1]));
            seriesLight.appendData(newPoint, true, 20, true);
            showDataOnGraph(seriesLight, graphLightLevel);
            sendDataToThingSpeak(numbers[0], numbers[1]);
            time += 1;
        }
    }

    @Override
    public void onRunError(Exception e) {

    }

    private void sendDataToThingSpeak(String ID, String value){
        OkHttpClient okHttpClient = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        //String apiURL = "https://api.thingspeak.com/update?api_key=0324U6WNIBX28W4G&field" + ID + "=" + value;
        String apiURL = "https://api.thingspeak.com/update?api_key=0324U6WNIBX28W4G&field1=" + ID + "&field2=" + value;
        Request request = builder.url(apiURL).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {

            }
        });
    }
}








































