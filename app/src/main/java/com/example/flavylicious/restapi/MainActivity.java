package com.example.flavylicious.restapi;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    private static final String APP_ID= "59028fc6";
    private static final String API_KEY = "ad3e310307d7b2f8bf474c45e1efd01f";

    private static final String TAG = MainActivity.class.getSimpleName();

    private OkHttpClient okHttpClient;

    private EditText textInput;
    private Button submitButton;
    private TextView definitionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize ok http
        okHttpClient = new OkHttpClient();

        textInput = findViewById(R.id.textInput);
        submitButton = findViewById(R.id.submitButton);
        definitionView = findViewById(R.id.textMeaning);

        submitButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        findMeaningOfEnteredWord();
                    }
                });
    }

    private void findMeaningOfEnteredWord() {
        String word = textInput.getText().toString();
        if (word.isEmpty()) {
            Toast.makeText(this, "Nothing entered", Toast.LENGTH_SHORT).show();
            return;
        }

        // create url from the word
        String lowerCaseWord = word.toLowerCase();
        String httpRequestUrl = "https://od-api.oxforddictionaries.com:443/api/v1/entries/en/" + lowerCaseWord;
        // make request with REST url
        new RequestAsyncTask().execute(httpRequestUrl);
    }

    private class RequestAsyncTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            String requestUrl = params[0];
            Request request = new Request.Builder()
                    .url(requestUrl)
                    .addHeader("Accept", "application/json")
                    .addHeader("app_id", APP_ID)
                    .addHeader("app_key", API_KEY)
                    .build();

            Response response = null;
            try {
                response = okHttpClient.newCall(request).execute();
                return response.body().string();
            } catch (IOException ex) {
                Log.e(TAG, "caught error: " + ex.getMessage());
            }

            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject responseAsJson = new JSONObject(result);
                JSONArray results = responseAsJson.getJSONArray("results");
                SpannableStringBuilder outputStringBuilder = new SpannableStringBuilder();

                for (int i = 0; i < results.length(); i++) {
                    JSONObject resultJSONObject = results.getJSONObject(i);
                    JSONArray lexicalEntriesForResult = resultJSONObject.getJSONArray("lexicalEntries");
                    for (int j = 0; j < lexicalEntriesForResult.length(); j++) {
                        JSONObject entryJsonObject = lexicalEntriesForResult.getJSONObject(j);
                        JSONArray entries = entryJsonObject.getJSONArray("entries");
                        for (int k = 0; k < entries.length(); k++) {
                            JSONObject sensesJsonObject  = entries.getJSONObject(k);
                            JSONArray senses = sensesJsonObject.getJSONArray("senses");

                            for (int l = 0; l < senses.length(); l++) {
                                JSONObject senseJsonObject = senses.getJSONObject(l);
                                if (senseJsonObject.has("domains")) {
                                    String domain = senseJsonObject.getString("domains");
                                    SpannableString underlineableDomain = new SpannableString(domain);
                                    underlineableDomain.setSpan(new UnderlineSpan(), 0, domain.length(), 0);
                                    outputStringBuilder.append(underlineableDomain);
                                    outputStringBuilder.append("\n");
                                }

                                JSONArray definitions = senseJsonObject.getJSONArray("definitions");
                                for (int m = 0; m < definitions.length(); m++) {
                                    String definition = definitions.getString(m);

                                    outputStringBuilder.append(definition);
                                    // move to new line for next definition
                                    outputStringBuilder.append("\n");
                                    // move to new line again so there's an empty line between definitions
                                    outputStringBuilder.append("\n");
                                }
                            }
                        }
                    }
                }

                definitionView.setText(outputStringBuilder);
            } catch (Exception ex) {
                Log.e(TAG, "Exception during json parsing: " + ex.getMessage());
            }
        }
    }
}