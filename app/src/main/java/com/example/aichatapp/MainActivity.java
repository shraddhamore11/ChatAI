package com.example.aichatapp;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.MediaType;
import retrofit2.Callback;
public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    EditText etMessage;
    Button btnSend;
    ImageButton btnVoice;

    ArrayList<Message> messages = new ArrayList<>();
    ChatAdapter adapter;

    static final int VOICE_REQ = 1;

    GeminiApiService apiService;

    // 🔴 PUT YOUR REAL API KEY HERE (starts with AIza)
    String API_KEY = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnVoice = findViewById(R.id.btnVoice);

        adapter = new ChatAdapter(messages, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        apiService = ApiClient.getClient().create(GeminiApiService.class);

        btnSend.setOnClickListener(v -> {
            String msg = etMessage.getText().toString().trim();

            if (!msg.isEmpty()) {
                messages.add(new Message(msg, true));
                adapter.notifyDataSetChanged();
                etMessage.setText("");

                sendToGemini(msg);
            }
        });

        btnVoice.setOnClickListener(v -> {
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            startActivityForResult(i, VOICE_REQ);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VOICE_REQ && resultCode == RESULT_OK && data != null) {
            ArrayList<String> res = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            if (res != null && !res.isEmpty()) {
                etMessage.setText(res.get(0));
            }
        }
    }

    private void sendToGemini(String userMsg) {

        try {
            // ---- Build JSON Body ----
            JSONObject part = new JSONObject();
            part.put("text", userMsg);

            JSONArray parts = new JSONArray();
            parts.put(part);

            JSONObject content = new JSONObject();
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            JSONObject bodyObj = new JSONObject();
            bodyObj.put("contents", contents);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    bodyObj.toString()
            );

            // ---- API Call ----
            apiService.getReply(API_KEY, body).enqueue(new Callback<ResponseBody>() {

                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                    try {
                        if (!response.isSuccessful()) {
                            String err = response.errorBody() != null
                                    ? response.errorBody().string()
                                    : "Unknown API Error";
                            messages.add(new Message("API Error: " + err, false));
                            adapter.notifyDataSetChanged();
                            return;
                        }

                        String res = response.body().string();
                        JSONObject json = new JSONObject(res);

                        JSONArray candidates = json.getJSONArray("candidates");
                        JSONObject content = candidates.getJSONObject(0)
                                .getJSONObject("content");
                        JSONArray parts = content.getJSONArray("parts");

                        String reply = parts.getJSONObject(0).getString("text");

                        messages.add(new Message(reply, false));
                        adapter.notifyDataSetChanged();

                    } catch (Exception e) {
                        messages.add(new Message("Parse Error: " + e.getMessage(), false));
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    messages.add(new Message("Network Error: " + t.getMessage(), false));
                    adapter.notifyDataSetChanged();
                }
            });

        } catch (Exception e) {
            messages.add(new Message("Request Error: " + e.getMessage(), false));
            adapter.notifyDataSetChanged();
        }
    }


}
