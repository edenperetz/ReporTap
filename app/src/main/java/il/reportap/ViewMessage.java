package il.reportap;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.loginregister.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ViewMessage extends ButtonsOptions {

    private Integer messageID, senderID, senderDept;
    private TextView sentTime, senderName, patientId, patientName, testName, componentName, measuredAmountValue, measurementUnit, boolValue, comments;
    private boolean isTestValueBool, wasRead;
    private ImageView isUrgent;
    private ImageButton wasReadButton, replyButton, forwardButton;
    private EditText editTextReplyText;
    private Spinner spinnerForwardTo;

    private HashMap<String, Integer> deptMap;                     //Translates department name to it's corresponding ID
    private HashMap<String, Pair<Integer, String>> testTypeMap;   //Translates test type ID to it's corresponding name + result type (in this form: [name, [ID, resultType]] )
    private ProgressDialog progressDialog;

    //TODO Define back-button to refresh the inbox rather then just go back to it's old state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_message);

        this.progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("נא להמתין לטעינת הדיווח...");
        progressDialog.show();

        this.sentTime = findViewById(R.id.textViewDateTime);
        this.senderName = findViewById(R.id.textViewSenderName_Dept);
        this.patientName = findViewById(R.id.textViewPatientName);
        this.patientId = findViewById(R.id.textViewPatientID);
        this.testName = findViewById(R.id.textViewTestName);
        this.componentName = findViewById(R.id.textViewComponentName);
        this.measuredAmountValue = findViewById(R.id.textViewMeasuredAmount);
        this.measurementUnit = findViewById(R.id.textViewMeasurementUnit);
        this.boolValue = findViewById(R.id.textViewBoolResult);
        this.comments = findViewById(R.id.textViewComments);
        this.isUrgent = findViewById(R.id.imageViewUrgent);

        wasReadButton =  findViewById(R.id.imageButtonRead);
        replyButton = findViewById(R.id.imageButtonReply);
        forwardButton = findViewById(R.id.imageButtonForward);
        this.editTextReplyText = findViewById(R.id.EditTextReplyText);
        this.spinnerForwardTo = findViewById(R.id.SpinnerForwardTo);

        if (getIntent().getExtras() != null) {  // Get the message ID from the previous screen - inbox
            messageID = getIntent().getIntExtra("MESSAGE_ID", 0);
            getMessage(this.messageID);         // Update all message fields from the DB according to the received message ID
        }
        else {
            Toast.makeText(getApplicationContext(), "שגיאה בקבלת מספר הדיווח.\nאנא נסו שוב מאוחר יותר.", Toast.LENGTH_LONG).show();
        }

        //TODO add other test results for the same patient (nice to have for version #1)

        // this.isTestValueBool = ((Pair)testTypeMap.get(this.testName.getText())).second.equals("boolean"); //TODO skip this hashmap? isTestValueBool gets value in getMessage() already

        // Setting up the navigation buttons
        findViewById(R.id.toDoB).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(getApplicationContext(), InboxDoctor.class));
            }
        });
        findViewById(R.id.sentB).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(getApplicationContext(), SentDoctor.class));
            }
        });
        findViewById(R.id.doneB).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(getApplicationContext(), DoneDoctor.class));
            }
        });
        progressDialog.hide();
    }

    public void populateHashmaps(){
        StringRequest stringRequest = new StringRequest(Request.Method.GET, URLs.URL_GET_DEPTS_N_TESTS, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                deptMap = new HashMap<String, Integer>();
                testTypeMap = new HashMap<String, Pair<Integer, String>>();

                try{
                    JSONObject entireResponse = new JSONObject(response);
                    JSONArray deptsArray = entireResponse.getJSONArray("departments");
                    JSONArray testTypesArray = entireResponse.getJSONArray("testTypes");
                    for (int i=0; i<deptsArray.length(); i++) {
                        JSONObject dept = deptsArray.getJSONObject(i);
                        deptMap.put(dept.getString("deptName"), dept.getInt("deptID"));
                    }
                    for (int i=0; i<testTypesArray.length(); i++){
                        JSONObject testType = testTypesArray.getJSONObject(i);
                        testTypeMap.put(testType.getString("testName"), new Pair<>(testType.getInt("testID"), testType.getString("resultType")));
                    }
                    inflateForwardList();
                }
                catch (JSONException e){
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    public void inflateForwardList(){
        //Define recipient list for forwarding

        ArrayList<String> departments = new ArrayList<>();
        try {
            for (String dept : this.deptMap.keySet())
                if (this.deptMap.get(dept) != SharedPrefManager.getInstance(getApplicationContext()).getUser().getDepartment())  // Add all possible departments except the user's department, to whom he can't forward
                    departments.add(dept);
        } catch (NullPointerException e) {
            Toast.makeText(getApplicationContext(), "חלה שגיאה בהצגת המחלקות. אנא נסו שוב מאוחר יותר.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, departments);
        spinnerForwardTo.setAdapter(adapter);
        //TODO enforce only valid options from the list are selectable
    }

    public void getMessage(Integer messageID){
        progressDialog.setMessage("ההודעה בטעינה,\nנא להמתין");
        progressDialog.show();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URLs.URL_GETMESSAGE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if (jsonObject.getBoolean("error")) { // If there was any error along the way
                        Toast.makeText(getApplicationContext(), jsonObject.getString("message"), Toast.LENGTH_LONG).show();
                    } else {
                        JSONObject requestedMessage = jsonObject.getJSONObject("requestedMessage");
                        patientName.setText(requestedMessage.getString("patientName"));
                        sentTime.setText(requestedMessage.getString("sentTime"));
                        senderName.setText(requestedMessage.getString("senderName") + " | " + requestedMessage.getString("senderDeptName"));
                        senderID = requestedMessage.getInt("sender");       // Keep sender ID for use later (though not presented on screen)
                        senderDept = requestedMessage.getInt("senderDept"); // Keep sending department ID for use later (though not presented on screen)
                        patientId.setText(requestedMessage.getString("patientId"));
                        patientName.setText(requestedMessage.getString("patientName"));
                        testName.setText(requestedMessage.getString("testName"));
                        componentName.setText(requestedMessage.getString("componentName"));
                        isTestValueBool = requestedMessage.getString("isValueBool").equals("1");    //The 'equals' check is performed (here and for the next boolean variables) since the value returns as a string rather than boolean
                        if (isTestValueBool){
                            boolValue.setText((requestedMessage.getString("testResultValue")).equals("1") ? "חיובית" : "שלילית");
                        } else {
                            measuredAmountValue.setText(requestedMessage.getString("testResultValue"));
                            measurementUnit.setText(requestedMessage.getString("measurementUnit"));
                        }
                        comments.setText(requestedMessage.getString("comments")); //TODO resize the textview to fit all the text
                        wasRead = !requestedMessage.getString("confirmTime").equals("null");
                        if (wasRead){
                            wasReadButton.setImageResource(R.drawable.eyecheck2_bmp);
                            wasReadButton.setClickable(false);
                        }
                        if (requestedMessage.getString("isUrgent").equals("1")){
                            isUrgent.setImageResource(R.drawable.redexclamation_trans);
                            ((TextView)findViewById(R.id.textViewUrgent)).setText("דחוף");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                isUrgent.setTooltipText("דחוף");
                            }
                        } else {
                            isUrgent.setImageResource(R.drawable.greyexclamation_trans);
                            ((TextView)findViewById(R.id.textViewUrgent)).setText("לא דחוף");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                isUrgent.setTooltipText("לא דחוף");
                            }
                        }
                        updateFields();
                    }
                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }) {
            @Nullable
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("messageID", messageID.toString());
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
        progressDialog.hide();
    }

    public void updateFields(){
        if (isTestValueBool){
            findViewById(R.id.linearLayoutBoolResult).setVisibility(View.VISIBLE);
            findViewById(R.id.linearLayoutMeasuredAmount).setVisibility(View.GONE);
        } else {
            findViewById(R.id.linearLayoutBoolResult).setVisibility(View.GONE);
            findViewById(R.id.linearLayoutMeasuredAmount).setVisibility(View.VISIBLE);
        }

        String userID = SharedPrefManager.getInstance(getApplicationContext()).getUser().getEmployeeNumber();

        wasReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markAsRead(messageID, userID);
            }
        });
        replyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO validate input
                reply(messageID, userID, senderDept);

            }
        });
        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO validate input
                forward(messageID, userID);
            }
        });
    }

    public void markAsRead(Integer messageID, String userID){ // This method doesn't return a value (impossible to return inside an onClick() method) but changes the global variable successMarkRead according to success.
        DialogInterface.OnClickListener confirmedMarkAsReadListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                progressDialog.setMessage("מעדכן שההודעה נקראה...");
                progressDialog.show();
                StringRequest stringRequest = new StringRequest(Request.Method.POST, URLs.URL_MARK_AS_READ, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Boolean successMarkRead = false;
                            JSONObject jsonObject = new JSONObject(response);
                            successMarkRead = !jsonObject.getBoolean("error");
                            if (!successMarkRead) {
                                Toast.makeText(getApplicationContext(), jsonObject.getString("message"), Toast.LENGTH_LONG).show(); // Show a message whether the operation succeeded or the error message
                                if (jsonObject.getBoolean("alreadyMarked")) {    // If the message had already been marked by another user, the function will also mark success TRUE and disable the button to mark again
                                    successMarkRead = true;
                                    wasRead = true;
                                    wasReadButton.setImageResource(R.drawable.eyecheck2_bmp);
                                    wasReadButton.setClickable(false);
                                }
                            } else {
                                Toast.makeText(getApplicationContext(), "ההודעה אושרה בהצלחה!", Toast.LENGTH_LONG).show();
                                wasReadButton.setImageResource(R.drawable.eyecheck2_bmp);
                                wasReadButton.setClickable(false);
                            }
                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }
                },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }) {
                    @Nullable
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<>();
                        params.put("messageID",messageID.toString());
                        params.put("userID",userID);
                        params.put("isResponse", "false");
                        return params;
                    }
                };
                RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
                requestQueue.add(stringRequest);
                progressDialog.hide();
            }
        };
        confirmMarkAsRead(messageID, userID, confirmedMarkAsReadListener);  // Show a confirmation dialog prior to marking the message as read
    }

    public void confirmMarkAsRead(Integer messageID, String userID, DialogInterface.OnClickListener confirmedMarkAsReadListener){
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogRTL);
        builder.setTitle("אישור קריאה");
        builder.setMessage(Html.fromHtml("האם לסמן שהדיווח נקרא?" + "<br/><b>" + "לא ניתן לבטל טיפול בדיווח" + "</b>"));
        builder.setPositiveButton("כן", (DialogInterface.OnClickListener) confirmedMarkAsReadListener); // User confirmed
        builder.setNegativeButton("ביטול", null);                                               // User cancelled
        AlertDialog dialog = builder.show();
        dialog.show();
    }

    public void reply(Integer messageID, String userID, Integer dept){
        findViewById(R.id.replyPopupDialog).setVisibility(View.VISIBLE);    // Show popup dialog
        findViewById(R.id.ButtonCancelReply).setOnClickListener(v -> {      // User canceled reply
            findViewById(R.id.replyPopupDialog).setVisibility(View.INVISIBLE);
            editTextReplyText.setText("");
        });
        findViewById(R.id.ButtonSendReply).setOnClickListener(v -> {        // User sent reply
            progressDialog.setMessage("התגובה שלך נשלחת...");
            progressDialog.show();

            //Defining constant values for use in the parameters map
            final String MessageID = messageID.toString();
            final String UserID = userID;
            final String Dept = dept.toString();
            final String ReplyText = editTextReplyText.getText().toString();

            StringRequest stringRequest = new StringRequest(Request.Method.POST, URLs.URL_NEWRESPONSE, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.getBoolean("error")) {
                            Toast.makeText(getApplicationContext(), jsonObject.getString("message"), Toast.LENGTH_LONG).show();
                        } else {
                            //TODO mark as read
                            Toast.makeText(getApplicationContext(), "התגובה נשלחה בהצלחה!", Toast.LENGTH_LONG).show();
                            wasReadButton.setImageResource(R.drawable.eyecheck2_bmp);
                            wasReadButton.setClickable(false);
                        }
                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }
            },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
            {
                @Nullable
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("messageId", MessageID);
                    params.put("sender", UserID);
                    params.put("department", Dept);
                    params.put("text", ReplyText);
                    return params;
                }
            };
            findViewById(R.id.replyPopupDialog).setVisibility(View.INVISIBLE);
            ((EditText)findViewById(R.id.EditTextReplyText)).setText("");
            progressDialog.hide();
            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
            requestQueue.add(stringRequest);
        });

    }

    public void forward(Integer messageID, String userID){
        populateHashmaps(); //TODO move this to inside forward() ?
        findViewById(R.id.forwardPopupDialog).setVisibility(View.VISIBLE);    // Show popup dialog
        findViewById(R.id.ButtonCancelForward).setOnClickListener(v -> {      // User canceled forward
            findViewById(R.id.forwardPopupDialog).setVisibility(View.INVISIBLE);
            spinnerForwardTo.setSelection(0);
        });
        findViewById(R.id.ButtonSendForward).setOnClickListener(v -> {        // User forwarded message
            progressDialog.setMessage("ההודעה מועברת...");
            progressDialog.show();

            //Defining constant values for use in the parameters map
            final String MessageID = messageID.toString();
            final String UserID = userID;
            final String DeptID = this.deptMap.get(spinnerForwardTo.getSelectedItem().toString()).toString(); //Getting the department ID from the selected department name

            StringRequest stringRequest = new StringRequest(Request.Method.POST, URLs.URL_FORWARDMESSAGE, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.getBoolean("error")) {
                            Toast.makeText(getApplicationContext(), jsonObject.getString("message"), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "ההודעה הועברה בהצלחה!", Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }
            },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }) {
                @Nullable
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("messageID", MessageID);
                    params.put("sender", UserID);
                    params.put("department", DeptID);
                    return params;
                }
            };
            findViewById(R.id.forwardPopupDialog).setVisibility(View.INVISIBLE);
            spinnerForwardTo.setSelection(0);
            progressDialog.hide();
            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
            requestQueue.add(stringRequest);
        });
        //TODO After forwarding: Exit back to inbox?/Grey out all the buttons?
    }
}