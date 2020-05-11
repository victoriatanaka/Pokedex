package edu.harvard.cs50.pokedex;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

public class PokemonActivity extends AppCompatActivity {
    private TextView nameTextView;
    private TextView numberTextView;
    private TextView type1TextView;
    private TextView type2TextView;
    private ImageView pokemonImage;
    private TextView descriptionTextView;
    private Button catchButton;
    private String url;
    private RequestQueue requestQueue;
    private boolean isCaught;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokemon);

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        url = getIntent().getStringExtra("url");
        nameTextView = findViewById(R.id.pokemon_name);
        numberTextView = findViewById(R.id.pokemon_number);
        type1TextView = findViewById(R.id.pokemon_type1);
        type2TextView = findViewById(R.id.pokemon_type2);
        catchButton = findViewById(R.id.button_catch);
        pokemonImage = findViewById(R.id.pokemon_image);
        descriptionTextView = findViewById(R.id.pokemon_description);
        load();
    }

    private class DownloadSpriteTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                URL url = new URL(strings[0]);
                return BitmapFactory.decodeStream(url.openStream());
            }
            catch (IOException e) {
                Log.e("cs50", "Download sprite error", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            pokemonImage.setImageBitmap(bitmap);
        }
    }

    public void load() {
        type1TextView.setText("");
        type2TextView.setText("");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String imageUrl = response.getJSONObject("sprites").getString("front_default");
                    String imageUrlShiny = response.getJSONObject("sprites").getString("front_shiny");
                    if ( !imageUrlShiny.isEmpty())
                        new DownloadSpriteTask().execute(imageUrlShiny);
                    else
                        new DownloadSpriteTask().execute(imageUrl);

                    nameTextView.setText(response.getString("name"));
                    numberTextView.setText(String.format("#%03d", response.getInt("id")));
                    loadPokemonDescription(response.getInt("id"));
                    JSONArray typeEntries = response.getJSONArray("types");
                    for (int i = 0; i < typeEntries.length(); i++) {
                        JSONObject typeEntry = typeEntries.getJSONObject(i);
                        int slot = typeEntry.getInt("slot");
                        String type = typeEntry.getJSONObject("type").getString("name");

                        if (slot == 1) {
                            type1TextView.setText(type);
                        }
                        else if (slot == 2) {
                            type2TextView.setText(type);
                        }
                        isCaught = getPreferences(Context.MODE_PRIVATE).getBoolean(nameTextView.getText().toString(), false);
                        catchButton.setText(isCaught ? "Release" : "Catch");
                    }
                } catch (JSONException e) {
                    Log.e("cs50", "Pokemon json error", e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("cs50", "Pokemon details error", error);
            }
        });

        requestQueue.add(request);
    }

    public void loadPokemonDescription(int id) {
        String url = "https://pokeapi.co/api/v2/pokemon-species/" + id;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray flavorTexts = response.getJSONArray("flavor_text_entries");
                    for (int i = 0, n = flavorTexts.length(); i < n; i++) {
                        JSONObject flavorText = flavorTexts.getJSONObject(i);
                        String text = flavorText.getString("flavor_text");
                        String languageName = flavorText.getJSONObject("language").getString("name");
                        if (languageName.equals("en")) {
                            descriptionTextView.setText(text.replace("\n", " "));
                            break;
                        }
                    }
                } catch (JSONException e) {
                    Log.e("cs50", "Json description error", e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("cs50", "Pokemon description error", error);
            }
        });

        requestQueue.add(request);
    }

    public void toggleCatch(View view) {
        if (!isCaught) {
            isCaught = true;
            catchButton.setText("Release");
            getPreferences(Context.MODE_PRIVATE).edit().putBoolean(nameTextView.getText().toString(), true).commit();
        }
        else {
            isCaught = false;
            catchButton.setText("Catch");
            getPreferences(Context.MODE_PRIVATE).edit().putBoolean(nameTextView.getText().toString(), false).commit();
        }
    }
}
