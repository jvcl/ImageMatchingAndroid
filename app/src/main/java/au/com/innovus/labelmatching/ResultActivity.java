package au.com.innovus.labelmatching;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class ResultActivity extends Activity {

    String filePath;
    private static final String TAG = MainActivity.class.getSimpleName();
    ProgressDialog progress;
    String upLoadServerUri = "http://107.191.56.252:8000/upload";
    int serverResponseCode = 0;

    private Item item = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        progress = new ProgressDialog(this);
        filePath = getIntent().getStringExtra("filePath");

        if (filePath != null) {
            previewImage(filePath);
        } else {
            Toast.makeText(getApplicationContext(),
                    "Sorry, file path is missing!", Toast.LENGTH_LONG).show();
        }
    }

    private void previewImage(String filePath){
        // Displaying the image
        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        BitmapFactory.Options options = new BitmapFactory.Options();

        // down sizing image as it throws OutOfMemory Exception for larger
        // images
        options.inSampleSize = 8;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        imageView.setImageBitmap(bitmap);
        progress.setTitle("Matching");
        progress.setMessage("Wait while loading...");
        progress.show();
        new UploadToServer().execute();
    }

    private class UploadToServer extends AsyncTask<Void, Integer, String>{

        @Override
        protected String doInBackground(Void... params) {
            try {
                return uploadImage();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @SuppressLint("LongLogTag")
        private String uploadImage() throws IOException {

            String fileName = filePath;
            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;
            File sourceFile = new File(filePath);
            Log.d(TAG, "Start decoding");
            Bitmap bitmap = decodeFile(sourceFile);
            Log.d(TAG, "got bitmap");

            try {

                // open a URL connection to the Servlet
                FileOutputStream out = null;
                try {

                    Log.d(TAG, "Writing to file");
                    out = new FileOutputStream(sourceFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    Log.d(TAG, bitmap.getWidth()+" "+bitmap.getHeight());
                    Log.d(TAG, "Finish Writing to file");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                FileInputStream fileInputStream = new FileInputStream(sourceFile);

                //imagePath = destination.getAbsolutePath();
                //Log.d("INFO", "PATH === " +imagePath);

                URL url = new URL(upLoadServerUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("file", "pic");

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"pic\";filename="+ fileName + "" + lineEnd);
                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "+ serverResponseMessage + ": " + serverResponseCode);

                if(serverResponseCode == 200){
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line+"\n");
                    }
                    br.close();
                    String ss = sb.toString();

                    Gson gson = new Gson();
                    item = gson.fromJson(ss,Item.class);
                    Log.d(TAG, item.toString());

                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {
                ex.printStackTrace();
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(ResultActivity.this, "MalformedURLException", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(ResultActivity.this, "Got Exception : see logcat ", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("Upload file to server Exception", "Exception : " + e.getMessage(), e);
            }
            return serverResponseCode +"";
        } // End else block


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
        }

        @Override
        protected void onPostExecute(String s) {
            progress.dismiss();

            if(serverResponseCode == 404){

                findViewById(R.id.linear_not_match).setVisibility(View.VISIBLE);
            }


            if (item != null){

                findViewById(R.id.linear_title).setVisibility(View.VISIBLE);
                findViewById(R.id.linear_description).setVisibility(View.VISIBLE);
                findViewById(R.id.linear_ingredients).setVisibility(View.VISIBLE);

                ((TextView) findViewById(R.id.textView_title)).setText(item.getTitle());
                ((TextView) findViewById(R.id.textView_category)).setText(item.getCategory());
                ((TextView) findViewById(R.id.textView_origin)).setText(item.getOrigin());
                ((TextView) findViewById(R.id.textView_description)).setText(item.getDescription());
                ((TextView) findViewById(R.id.textView_ingredients)).setText(item.getIngredients());
            }

            super.onPostExecute(s);

        }
    }

    private Bitmap decodeFile(File f) throws IOException {
        Bitmap b = null;

        //Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;

        FileInputStream fis = new FileInputStream(f);
        BitmapFactory.decodeStream(fis, null, o);
        fis.close();
        int IMAGE_MAX_SIZE = 680;

        int scale = 1;
        if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
            scale = (int)Math.pow(2, (int) Math.ceil(Math.log(IMAGE_MAX_SIZE /
                    (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
        }

        //Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        fis = new FileInputStream(f);
        b = BitmapFactory.decodeStream(fis, null, o2);
        fis.close();
        return b;
    }
}
