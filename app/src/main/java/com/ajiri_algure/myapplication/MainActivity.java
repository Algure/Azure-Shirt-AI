package com.ajiri_algure.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 11002;
    TextView classText;
    ImageView classImv,detImv;
    RelativeLayout clasPrompt;
    String classificationImageString,detectionImageString;
    Bitmap classificationBitmap,detectionBitmap;
    ProgressBar clasProgress;
    DataSnapshot clasSnapshot;
    private byte[] b;
    private JSONObject results;
    private String clasShirtType;
    private double largestProbability;
    private double PROBABILITY_THRESHOLD=0.1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        classText=findViewById(R.id.classtext);
        classImv=findViewById(R.id.classimv);
        clasPrompt=findViewById(R.id.class_prompt_layout);
        clasProgress=findViewById(R.id.clas_progress);
    }
    public void classifyImage(View view){
        showResultLayout(false);
       ClassifyTask ctask=new ClassifyTask();
       ctask.execute();
    }

    public void showResultLayout(boolean b){
        clasPrompt.setVisibility(b?View.VISIBLE:View.GONE);
    }
    public void showClasProgress(boolean b){
        if(b){
            clasPrompt.setVisibility(View.VISIBLE);
            clasProgress.setVisibility(View.VISIBLE);
            classText.setText("");
        }else {
            clasProgress.setVisibility(View.GONE);
        }
    }
    public void pictake(View v){
        showResultLayout(false);
        Intent pickintent=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        Intent imageintent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        Intent chooserintent=Intent.createChooser(pickintent,getResources().getString(R.string.choose));
        chooserintent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{imageintent});
        startActivityForResult(chooserintent,REQUEST_IMAGE_CAPTURE);
    }



    public Uri getImageUri(Bitmap bt, Context context, String title){
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        bt.compress(Bitmap.CompressFormat.JPEG,90,bytes);
        String path= MediaStore.Images.Media.insertImage(context.getContentResolver(),bt,title,null);
        return Uri.parse((path));
    }


    public String bitmapToString(Bitmap bitmap){
        ByteArrayOutputStream bas=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100,bas);
       b=bas.toByteArray();
        String temp= Base64.encodeToString(b,Base64.DEFAULT);
        return temp;
    }


    public Bitmap stringToBitmap(String bitmap){
        try {
            byte[] encodebyte = Base64.decode(bitmap, Base64.DEFAULT);
            Bitmap bm = BitmapFactory.decodeByteArray(encodebyte, 0, encodebyte.length);
            return bm;
        }catch (Exception e){
            return null;
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        boolean error = false;
        Bundle extras;
        String pic = "";
        Bitmap bitmap = null;
        try {
            extras = data.getExtras();
            if ((requestCode == REQUEST_IMAGE_CAPTURE  && resultCode == RESULT_OK) ){
            try {

                    Bitmap bt = (Bitmap) extras.get("data");
                    bitmap = bt;
                    pic = bitmapToString(bt);
                } catch (Exception E) {
                    Log.i("error1", E.toString());

                    try {
                        //set image uri and add to pics
                        Uri selectedimage = data.getData();
                        InputStream inps = getContentResolver().openInputStream(selectedimage);
                        Bitmap image = BitmapFactory.decodeStream(inps);
                        bitmap = image;

                        pic = bitmapToString(image);
                    } catch (Exception e) {
                        error=true;
                        Log.i("error2", e.toString());
                    }
                }
            }
            }catch(Exception e){
                 error=true;
                Toast.makeText(this, "Error !!!", Toast.LENGTH_SHORT).show();
            }
            if ( !error && !TextUtils.isEmpty(pic) && requestCode == REQUEST_IMAGE_CAPTURE) {
                classificationImageString = pic;
                classificationBitmap = bitmap;
                classImv.setImageBitmap(classificationBitmap);
            }
        }
        public class ClassifyTask extends AsyncTask<Void,Void,Void>{

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                results=null;
                clasShirtType="";
                largestProbability=0.0;
                showClasProgress(true);
            }

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    HttpClient client=new DefaultHttpClient();
                    URL req=new URL(getResources().getString(R.string.cvisionURL));
                     HttpPost httpPost = new HttpPost(String.valueOf(req));
                    int resultLength;

//            // Request headers - replace this example key with your valid Prediction-Key.
                    httpPost.addHeader("Prediction-Key", getResources().getString(R.string.prediction_key));
                    httpPost.setEntity(new ByteArrayEntity(b));
                    HttpResponse response=client.execute(httpPost);
                    Log.i("httpResponse",response.toString());

                    InputStream inputStream=response.getEntity().getContent();
                    InputStreamReader reader=new InputStreamReader(inputStream);
                    BufferedReader bufferedReader=new BufferedReader(reader);
                    StringBuilder builder=new StringBuilder();
                    String bufferedChunk=null;
                    while ((bufferedChunk=bufferedReader.readLine())!= null){
                        builder.append(bufferedChunk);
                    }
                    String result=builder.toString();
                     results=new JSONObject(result);
                    Log.i("jkzxil",builder.toString());

                    if(results!=null){
                        try {
                            JSONArray resultsArray= (JSONArray) results.get("predictions");
                            largestProbability=((JSONObject)resultsArray.get(0)).getDouble("probability");
                            clasShirtType =((JSONObject)resultsArray.get(0)).getString("tagName");
                            resultLength=resultsArray.length();
                            for(int i=0;i<resultLength;i++){
                                JSONObject obj= (JSONObject) resultsArray.get(i);
                                if(obj.getDouble("probability")>largestProbability){
                                    largestProbability=obj.getDouble("probability");
                                    clasShirtType =obj.getString("tagName");
                                }
                            }
                        } catch (JSONException e) {
                            Log.i("error1scda",e.toString());
                        }
                    }
                } catch (Exception e) {
                    Log.i("jkzxil",e.toString());
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                publishResult();
            }
        }

    private void publishResult() {

        showClasProgress(false);
        if(results!=null && !TextUtils.isEmpty(clasShirtType) && largestProbability> PROBABILITY_THRESHOLD ) {
            showResultLayout(true);
            classText.setText(clasShirtType);
        }else {
            showResultLayout(true);
            classText.setText("UNKNOWN");
        }
    }


}


