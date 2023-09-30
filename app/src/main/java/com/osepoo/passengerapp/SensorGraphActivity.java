package com.osepoo.passengerapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.ValueFormatter;
import androidx.core.content.ContextCompat;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.*;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.EntryXComparator;

public class SensorGraphActivity extends AppCompatActivity {

    private LineChart chartX;
    private LineChart chartY;
    private LineChart chartZ;
    private DatabaseReference dataRef;
    private ArrayList<Entry> xValues;
    private ArrayList<Entry> yValues;
    private ArrayList<Entry> zValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_graph);
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an intent to navigate back to the EntryActivity
                Intent intent = new Intent(SensorGraphActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Optional: Finish the current activity
            }
        });

        chartX = findViewById(R.id.chartX);
        chartY = findViewById(R.id.chartY);
        chartZ = findViewById(R.id.chartZ);

        dataRef = FirebaseDatabase.getInstance().getReference("Recordings/0000");
        xValues = new ArrayList<>();
        yValues = new ArrayList<>();
        zValues = new ArrayList<>();

        ValueEventListener dataEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Iterate through timestamp nodes
                    for (DataSnapshot timestampSnapshot : dataSnapshot.getChildren()) {
                        // Retrieve the "x," "y," and "z" values for this timestamp
                        String xValueStr = timestampSnapshot.child("x").getValue(String.class);
                        String yValueStr = timestampSnapshot.child("y").getValue(String.class);
                        String zValueStr = timestampSnapshot.child("z").getValue(String.class);

                        float xValue = Float.parseFloat(xValueStr);
                        float yValue = Float.parseFloat(yValueStr);
                        float zValue = Float.parseFloat(zValueStr);

                        // Add the values to the respective lists
                        xValues.add(new Entry(xValues.size(), xValue));
                        yValues.add(new Entry(yValues.size(), yValue));
                        zValues.add(new Entry(zValues.size(), zValue));
                    }

                    // Update the charts
                    updateChart(chartX, xValues, "X Values");
                    updateChart(chartY, yValues, "Y Values");
                    updateChart(chartZ, zValues, "Z Values");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle any errors here
            }
        };

        // Add ValueEventListener to retrieve data
        dataRef.addListenerForSingleValueEvent(dataEventListener);
    }

    private void updateChart(LineChart chart, List<Entry> values, String label) {
        chart.getDescription().setEnabled(true);
        chart.getDescription().setText(label);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setBackgroundColor(ContextCompat.getColor(this, R.color.colorBackground));
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisLeft().setDrawAxisLine(true);
        chart.getAxisLeft().setTextSize(12f);
        chart.getAxisRight().setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(45);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return getTimeLabel((int) value);
            }
        });

        LineDataSet dataSet = new LineDataSet(values, label);
        dataSet.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
        dataSet.setLineWidth(2f);
        dataSet.setValueTextSize(12f);
        //dataSet.setCircleRadius(4f);
        dataSet.setDrawCircles(false);
        //dataSet.setCircleColor(ContextCompat.getColor(this, R.color.colorPrimary));

        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);
        LineData lineData = new LineData(dataSets);
        chart.setData(lineData);
        chart.invalidate();
    }

    private String getTimeLabel(int value) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -value);
        return sdf.format(calendar.getTime());
    }
}
