package com.example.quoctrong.smarthomeapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    MQTTHelper mqttHelper;
    ConstraintLayout Login, Home;
    //    Login
    EditText user, password, phone;
    Button signIn, signUp;
    SwitchCompat switchCompat;
    TextView textForgot, textPhone;
    ImageView imgPhone;
    CheckBox cbRemember;
    SharedPreferences sharedpreferences;
    String userLogin = "admin";
    String passLogin = "admin";
    public static final String MyPREFERENCES = "MyPrefs";
    public static final String USERNAME = "userNameKey";
    public static final String PASS = "passKey";
    public static final String REMEMBER = "remember";
    //    Login

    // Home
    Button logout;
    TextView data_temp, data_temp_check, data_humi, data_humi_check;
    SwitchCompat switch_led, switch_pump;
    ImageView led_image, pump_image;
    // Home

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        InitMapping();
        StartMQTT();
        //Start Login
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Editable username = user.getText();
                Editable pass = password.getText();
                if(username.length() == 0){
                    Toast.makeText(MainActivity.this, "Please enter your Username", Toast.LENGTH_SHORT).show();
                }
                else if(pass.length() == 0){
                    Toast.makeText(MainActivity.this, "Please enter your Password", Toast.LENGTH_SHORT).show();
                }
                else{
                    if(cbRemember.isChecked()){
                        saveLoginData(username.toString(), pass.toString());
                    }
                    else{
                        clearLoginData();
                    }
                    if(username.toString().equals(userLogin) && pass.toString().equals(passLogin)){
                        getLastValue_Temp();
                        getLastValue_Humi();
                        getLastValue_Led();
                        getLastValue_Pump();
                        Home.setVisibility(View.VISIBLE);
                        Login.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Logged in successfully", Toast.LENGTH_LONG).show();
                    }
                    else{
                        Toast.makeText(MainActivity.this, "Username or password is incorrect", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Editable username = user.getText();
                Editable pass = password.getText();
                Editable phoneNumber = phone.getText();
                if(username.length() <= 3){
                    Toast.makeText(MainActivity.this, "Please enter your Username above 3 character", Toast.LENGTH_SHORT).show();
                }
                else if(pass.length() <= 3){
                    Toast.makeText(MainActivity.this, "Please enter your Password above 3 character", Toast.LENGTH_SHORT).show();
                }
                else if(phoneNumber.length() != 10){
                    Toast.makeText(MainActivity.this, "Phone is not correct", Toast.LENGTH_SHORT).show();
                }
                else{
                    userLogin = username.toString();
                    passLogin = pass.toString();
                    Home.setVisibility(View.VISIBLE);
                    Login.setVisibility(View.GONE);
                }
            }
        });
        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked){
                user.setText("");
                password.setText("");
                cbRemember.setChecked(false);
                signIn.setVisibility(View.INVISIBLE);
                cbRemember.setVisibility(View.INVISIBLE);
                textForgot.setVisibility(View.INVISIBLE);
                imgPhone.setVisibility(View.VISIBLE);
                textPhone.setVisibility(View.VISIBLE);
                phone.setVisibility(View.VISIBLE);
                signUp.setVisibility(View.VISIBLE);
            } else{
                signIn.setVisibility(View.VISIBLE);
                cbRemember.setVisibility(View.VISIBLE);
                textForgot.setVisibility(View.VISIBLE);
                imgPhone.setVisibility(View.INVISIBLE);
                textPhone.setVisibility(View.INVISIBLE);
                phone.setVisibility(View.INVISIBLE);
                signUp.setVisibility(View.INVISIBLE);
            }
            }
        });
        //create shared preferences
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        // load username and password were saved
        loadLoginData();
        //End Login

        // Start Home
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Home.setVisibility(View.GONE);
                Login.setVisibility(View.VISIBLE);
            }
        });

        switch_led.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    mqttHelper.publishToTopic("iotg06/feeds/bk-iot-led", "1");
                    led_image.setImageDrawable(getDrawable(R.drawable.ic_led_on));
                }
                else{
                    mqttHelper.publishToTopic("iotg06/feeds/bk-iot-led", "0");
                    led_image.setImageDrawable(getDrawable(R.drawable.ic_led_off));
                }
            }
        });
        switch_pump.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    mqttHelper.publishToTopic("iotg06/feeds/bk-iot-pump", "1");
                    pump_image.setImageDrawable(getDrawable(R.drawable.ic_motor_on));
                }
                else{
                    mqttHelper.publishToTopic("iotg06/feeds/bk-iot-pump", "0");
                    pump_image.setImageDrawable(getDrawable(R.drawable.ic_motor_off));
                }
            }
        });
        // End Home
    }


    void InitMapping() {
        Login = findViewById(R.id.Login);
        Home = findViewById(R.id.Home);
        //Start Login
        user = findViewById(R.id.userName);
        password = findViewById(R.id.passWord);
        phone = findViewById(R.id.login_phoneNum);
        cbRemember = findViewById(R.id.cbRemember);
        textForgot = findViewById(R.id.text_forgot);
        signIn = findViewById(R.id.btnSubmit);
        switchCompat = findViewById(R.id.btnSign);
        signUp = findViewById(R.id.btnSub);
        textPhone = findViewById(R.id.text_phone);
        imgPhone = findViewById(R.id.image_phone);
        //End Login

        // Start Home
        logout = findViewById(R.id.logout_btn);
        data_temp = findViewById(R.id.data_temp);
        data_temp_check = findViewById(R.id.data_temp_check);
        data_humi = findViewById(R.id.data_humidity);
        data_humi_check = findViewById(R.id.data_humidity_check);
        switch_led = findViewById(R.id.switch_led);
        switch_pump = findViewById(R.id.switch_pump);
        led_image = findViewById(R.id.led_image);
        pump_image = findViewById(R.id.pump_image);
        // End Home
    }

    private void clearLoginData() {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.clear();
        editor.apply();
    }
    private void saveLoginData(String username, String Pass) {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(USERNAME, username);
        editor.putString(PASS, Pass);
        editor.putBoolean(REMEMBER,cbRemember.isChecked());
        editor.apply();
    }
    private void loadLoginData() {
        if(sharedpreferences.getBoolean(REMEMBER,false)) {
            user.setText(sharedpreferences.getString(USERNAME, ""));
            password.setText(sharedpreferences.getString(PASS, ""));
            cbRemember.setChecked(true);
        } else
            cbRemember.setChecked(false);
    }

    private void StartMQTT(){
        mqttHelper = new MQTTHelper(getApplicationContext(), "nqt");
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {}

            @Override
            public void connectionLost(Throwable cause) {}

            @Override
            public void messageArrived(String topic, MqttMessage message){
                if(topic.contains("bk-iot-temp")){
                    data_temp.setText(message.toString());
                    int temp = Integer.parseInt(message.toString());
                    if(temp > 28){
                        data_temp_check.setText("Hot");
                        data_temp_check.setTextColor(Color.RED);
                    }
                    else if(temp < 24){
                        data_temp_check.setText("Cold");
                        data_temp_check.setTextColor(Color.RED);
                    }else{
                        data_temp_check.setText("Normal");
                        data_temp_check.setTextColor(Color.BLACK);
                    }
                }

                if(topic.contains("bk-iot-humi")){
                    data_humi.setText(message.toString());
                    int humidity = Integer.parseInt(message.toString());
                    if(humidity > 70){
                        data_humi_check.setText("Humid air");
                        data_humi_check.setTextColor(Color.RED);
                    }
                    else if(humidity < 40){
                        data_humi_check.setText("Dry air");
                        data_humi_check.setTextColor(Color.RED);
                    }else{
                        data_humi_check.setText("Normal");
                        data_humi_check.setTextColor(Color.BLACK);
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {}
        });
    }

    private void getLastValue_Temp(){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        String url = "https://io.adafruit.com/api/v2/iotg06/feeds/bk-iot-temp/data?limit=1";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    JSONObject info = response.getJSONObject(0);
                    data_temp.setText(info.getString("value"));
                    int temp = Integer.parseInt(info.getString("value"));
                    if(temp > 28){
                        data_temp_check.setText("Hot");
                        data_temp_check.setTextColor(Color.RED);
                    }
                    else if(temp < 24){
                        data_temp_check.setText("Cold");
                        data_temp_check.setTextColor(Color.RED);
                    }else{
                        data_temp_check.setText("Normal");
                        data_temp_check.setTextColor(Color.BLACK);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(request);
    }

    private void getLastValue_Humi(){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        String url = "https://io.adafruit.com/api/v2/iotg06/feeds/bk-iot-humi/data?limit=1";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    JSONObject info = response.getJSONObject(0);
                    data_humi.setText(info.getString("value"));
                    int humidity = Integer.parseInt(info.getString("value"));
                    if(humidity > 70){
                        data_humi_check.setText("Humid air");
                        data_humi_check.setTextColor(Color.RED);
                    }
                    else if(humidity < 40){
                        data_humi_check.setText("Dry air");
                        data_humi_check.setTextColor(Color.RED);
                    }else{
                        data_humi_check.setText("Normal");
                        data_humi_check.setTextColor(Color.BLACK);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(request);
    }

    private void getLastValue_Led(){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        String url = "https://io.adafruit.com/api/v2/iotg06/feeds/bk-iot-led/data?limit=1";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    JSONObject info = response.getJSONObject(0);
                    if(info.getString("value").equals("1")){
                        switch_led.setChecked(true);
                        led_image.setImageDrawable(getDrawable(R.drawable.ic_led_on));
                    } else{
                        switch_led.setChecked(false);
                        led_image.setImageDrawable(getDrawable(R.drawable.ic_led_off));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(request);
    }

    private void getLastValue_Pump(){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        String url = "https://io.adafruit.com/api/v2/iotg06/feeds/bk-iot-pump/data?limit=1";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    JSONObject info = response.getJSONObject(0);
                    if(info.getString("value").equals("1")){
                        switch_pump.setChecked(true);
                        pump_image.setImageDrawable(getDrawable(R.drawable.ic_motor_on));
                    } else{
                        switch_pump.setChecked(false);
                        pump_image.setImageDrawable(getDrawable(R.drawable.ic_motor_off));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(request);
    }
}
