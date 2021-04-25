package il.reportap;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.loginregister.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SentLab extends OptionsMenu {

    private RecyclerView recyclerView;
    private AdapterActivitySentLab adapter;
    private List<ModelActivitySentLab> modelActivitySentLabList;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sent_lab);
        Button btn = (Button)findViewById(R.id.sentBSL);
        btn.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.stroke));
        btn.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));

        recyclerView = (RecyclerView)findViewById(R.id.recyclerViewSentL);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration divider = new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL);
        divider.setDrawable(getDrawable(R.drawable.dividerbig));
        recyclerView.addItemDecoration(divider);
        modelActivitySentLabList = new ArrayList<>();
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                URLs.URL_SENTLAB,
                //lambda expression
                response -> {


                    try {
                        JSONObject repObj = new JSONObject(response);
                        JSONArray repArray = repObj.getJSONArray("report");

                        for(int i=0; i<repArray.length(); i++){

                            JSONObject jObg = new JSONObject();
                            jObg= repArray.getJSONObject(i);
                            ModelActivitySentLab modelActivitySentLab = new ModelActivitySentLab(jObg.getInt("id"),
                                    jObg.getString("sent_time"),
                                    jObg.getString("patient_id"),
                                    jObg.getString("name"),
                                    jObg.getInt("is_urgent"),
                                    jObg.getString("confirm_time"),
                                    jObg.getString("dept_name"));
                            modelActivitySentLabList.add(modelActivitySentLab);
                            System.out.println(modelActivitySentLabList.get(i).getId());
                        }




                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    adapter = new AdapterActivitySentLab(modelActivitySentLabList,getApplicationContext());
                    recyclerView.setAdapter(adapter);

                },
                //lambda expression
                error -> Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show())
        {
            @Nullable
            @Override
            protected Map<String,String> getParams() throws AuthFailureError {
                Map<String,String> params = new HashMap<>();
                params.put("department", String.valueOf(SharedPrefManager.getInstance(getApplicationContext()).getUser().getDepartment()));
                System.out.println(params.get("department"));
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);

        Button btnD= (Button)findViewById(R.id.doneBSL);
        btnD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(getApplicationContext(), DoneLab.class));
            }
        });
        Button btnI= (Button)findViewById(R.id.toDoBSL);
        btnI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(getApplicationContext(), InboxLab.class));
            }
        });
    }




}