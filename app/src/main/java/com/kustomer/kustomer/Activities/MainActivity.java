package com.kustomer.kustomer.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.kustomer.kustomer.BaseClasses.BaseActivity;
import com.kustomer.kustomer.R;
import com.kustomer.kustomersdk.Kustomer;
import com.kustomer.kustomersdk.Models.KUSCustomerDescription;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    //region Properties
    Button btnStartChat;
    Button btnResetTrackingToken;
    Button btnKnowledgeBase;
    ImageView ivSupport;
    //endregion

    //region LifeCycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setLayout(R.layout.activity_main,R.id.toolbar_main,getResources().getString(R.string.app_name),false);
        super.onCreate(savedInstanceState);

        initViews();
        setListeners();


        // Describing Customer
        KUSCustomerDescription customerDescription = new KUSCustomerDescription();
        customerDescription.setEmail("xasd@brainxtech.com");

        JSONObject customObject = new JSONObject();
        try {
            customObject.put("ageNum",19);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        customerDescription.setCustom(customObject);
        Kustomer.describeCustomer(customerDescription);

        // Describing Conversation
        JSONObject conversationObject = new JSONObject();
        try {
            conversationObject.put("companyStr","brainxTech");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Kustomer.describeConversation(conversationObject);

        Kustomer.identify("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHRlcm5hbElkIjoiMTEyMiIsImlhdCI6MTUyMTAzMTcyMX0.tOuT7041V4lV9LNtd6mEhQir-oQzCCkPEZoT2Qaq4ic");
    }
    //endregion

    //region Initializer
    private void initViews(){
        btnStartChat = findViewById(R.id.btnPresent);
        btnKnowledgeBase = findViewById(R.id.btnKnowledgeBase);
        ivSupport = findViewById(R.id.ivSupport);
        btnResetTrackingToken = findViewById(R.id.btnResetToken);
    }

    private void setListeners(){
        btnStartChat.setOnClickListener(this);
        btnResetTrackingToken.setOnClickListener(this);
        ivSupport.setOnClickListener(this);
        btnKnowledgeBase.setOnClickListener(this);
    }
    //endregion

    //region Click Listener
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.ivSupport:
            case R.id.btnPresent:
                Kustomer.showSupport(this);
                break;

            case R.id.btnResetToken:
                Kustomer.resetToken();
                break;

            case R.id.btnKnowledgeBase:
                Kustomer.presentKnowledgeBase(this);
                break;
        }
    }
    //endregion
}
