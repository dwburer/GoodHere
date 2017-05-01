package com.danielburer.goodhere;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EstablishmentDetailActivity extends AppCompatActivity {

    private TextView name;
    private ArrayList<Product> productsPrepped;
    private ListView products;
    private ProductListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_establishment_detail);

        name = (TextView) findViewById(R.id.tv_estDetailName);

        productsPrepped = new ArrayList<>();
        products = (ListView) findViewById(R.id.lv_estDetailProducts);
        adapter = new ProductListAdapter(this, R.layout.atom_pay_list_item, productsPrepped);
        products.setAdapter(adapter);

        queryEstablishments();
    }

    public void queryEstablishments() {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String base_url= sharedPref.getString(getString(R.string.server_api_url), "");
        String query_url = String.format("%sestablishments/%d/", base_url, getIntent().getIntExtra("establishmentPK", 0));

        // Query establishment for details and products.
        JsonObjectRequest jsObjRequest = new JsonObjectRequest (Request.Method.GET, query_url, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    name.setText(response.getString("name"));

                    ArrayList<Product> newProducts = new ArrayList<>();
                    JSONArray responseProducts = response.getJSONArray("products");

                    for(int j = 0; j < responseProducts.length(); j++) {
                        JSONObject newProd = responseProducts.getJSONObject(j);
                        String name = newProd.getString("name");
                        String owner = newProd.getString("owner");
                        int pk = newProd.getInt("pk");
                        int votes = newProd.getInt("votes");
                        newProducts.add(new Product(name, owner, pk, votes));
                    }

                    String imageUrl = response.getString("brand_image");

                    try {
                        ImageView i = (ImageView)findViewById(R.id.brand_image);
                        Bitmap bitmap = BitmapFactory.decodeStream((InputStream)new URL(imageUrl).getContent());

//                        FrameLayout brandImageContainer = (FrameLayout)findViewById(R.id.brand_image_container);
//                        brandImageContainer.setBackground(new BitmapDrawable(getResources(), bitmap));
                        i.setImageBitmap(bitmap);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    productsPrepped.clear();
                    productsPrepped.addAll(newProducts);
                    adapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("volley", error.toString());
            }

        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                String token = sharedPref.getString(getString(R.string.client_saved_token_key), "");
                String auth = "Bearer " + token;
                params.put("Authorization", auth);
                return params;
            }
        };

        // Access the RequestQueue through our QueueSingleton class.
        QueueSingleton.getInstance(this).addToRequestQueue(jsObjRequest);
    }
}
