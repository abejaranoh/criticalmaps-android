package de.stephanlindauer.criticalmaps.handler;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

import de.stephanlindauer.criticalmaps.R;
import de.stephanlindauer.criticalmaps.utils.ImageUtils;
import de.stephanlindauer.criticalmaps.vo.ResultType;

public class ProcessCameraResultHandler extends AsyncTask<Void, Void, ResultType> {

    private final Activity activity;
    private final File newCameraOutputFile;
    private File processedImageFile;
    private ProgressDialog progressDialog;

    public ProcessCameraResultHandler(Activity activity, File newCameraOutputFile) {
        this.activity = activity;
        this.newCameraOutputFile = newCameraOutputFile;
    }

    @Override
    protected void onPreExecute() {
        progressDialog = new ProgressDialog(activity);
        progressDialog.setIndeterminate(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(activity.getString(R.string.camera_processing_image));
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    @Override
    protected ResultType doInBackground(Void... params) {
        try {
            Bitmap rotatedBitmap = ImageUtils.rotateBitmap(newCameraOutputFile);
            Bitmap scaledBitmap = ImageUtils.resize(rotatedBitmap, 1024, 1024);
            if (scaledBitmap != rotatedBitmap) {
                rotatedBitmap.recycle();
            }

            processedImageFile = ImageUtils.getNewOutputImageFile();
            FileOutputStream fOut = new FileOutputStream(processedImageFile);
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fOut);
            fOut.flush();
            fOut.close();
            scaledBitmap.recycle();
        } catch (Exception e) {
            return ResultType.FAILED;
        }

        return ResultType.SUCCEEDED;
    }

    @Override
    protected void onPostExecute(ResultType resultType) {
        progressDialog.dismiss();

        if (resultType == ResultType.FAILED) {
            Toast.makeText(activity, R.string.something_went_wrong, Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View view = LayoutInflater.from(activity).inflate(R.layout.view_picture_upload, null);

        ImageView image = (ImageView) view.findViewById(R.id.picture_preview);

        image.setImageBitmap(BitmapFactory.decodeFile(processedImageFile.getPath(), new BitmapFactory.Options()));

        TextView text = (TextView) view.findViewById(R.id.picture_confirm_text);
        text.setLinksClickable(true);
        text.setText(Html.fromHtml(activity.getString(R.string.camera_confirm_image_upload)));

        builder.setView(view);

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        new ImageUploadHandler(processedImageFile, activity).execute();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        processedImageFile.delete();
                        break;
                }
            }
        };

        builder.setPositiveButton(R.string.camera_upload, dialogClickListener);
        builder.setNegativeButton(R.string.camera_discard, dialogClickListener);
        builder.setCancelable(false);
        builder.show();
    }
}
