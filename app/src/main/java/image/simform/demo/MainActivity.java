package image.simform.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import image.simform.demo.simplefilechooser.Constants;
import image.simform.demo.simplefilechooser.ui.FileChooserActivity;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.btn_camera)
    Button btnCamera;
    @BindView(R.id.btn_gallery_single)
    Button btnGallerySingle;
    @BindView(R.id.btn_gallery_multi)
    Button btnGalleryMulti;
    @BindView(R.id.btn_Any_file)
    Button btn_Any_file;
    @BindView(R.id.fl_selectedPhoto)
    LinearLayout fl_selectedPhoto;

    private File file;
    private Uri uri;
    private Intent CamIntent, GalIntent;
    private RxPermissions rxPermissions;


    private int REQUEST_CAMERA = 3, SINGLE_SELECT_GALLERY = 2, MULTI_SELECT_GALLERY = 1, RESULT_CROP = 400,SELECT_FILE=4;
    private ArrayList<Uri> photoList = new ArrayList<>();
    private LinearLayout.LayoutParams lp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        rxPermissions = new RxPermissions(this);
    }

    @OnClick({R.id.btn_camera, R.id.btn_gallery_single, R.id.btn_gallery_multi, R.id.btn_Any_file})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_camera:  // Camera click
                rxPermissions.request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .subscribe(granted -> {
                            if (granted) {
                                ClickImageFromCamera();
                            } else {
                                Toast.makeText(this, R.string.permission_camera_denied, Toast.LENGTH_SHORT).show();

                            }
                        });
                break;
            case R.id.btn_gallery_single: //single image pick click
                rxPermissions
                        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .subscribe(granted -> {
                            if (granted) {

                                SingleGetImageFromGallery();
                            } else {
                                Toast.makeText(this, R.string.permission_camera_denied, Toast.LENGTH_SHORT).show();
                            }
                        });

                break;
            case R.id.btn_gallery_multi: // multi Images pick click
                rxPermissions
                        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .subscribe(granted -> {
                            if (granted) {
                                multiGetImageFromGallery();
                            } else {
                                Toast.makeText(this, R.string.permission_write_denied, Toast.LENGTH_SHORT).show();
                            }
                        });

                break;
            case R.id.btn_Any_file: //any Document pick click
                rxPermissions
                        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .subscribe(granted -> {
                            if (granted) { // Always true pre-M
                               fileGetFromStorage();
                            } else {
                                Toast.makeText(this, R.string.permission_write_denied, Toast.LENGTH_SHORT).show();
                            }
                        });

                break;
        }
    }



    // Camera function
    private void ClickImageFromCamera() {
        file = new File(Environment.getExternalStorageDirectory(), "file" + String.valueOf(System.currentTimeMillis()) + ".jpg");
        if (android.os.Build.VERSION.SDK_INT > 23) {
            uri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".provider", file);
        } else {
            uri = Uri.fromFile(file);
        }
        CamIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        CamIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri);
        CamIntent.putExtra("return-data", true);
        startActivityForResult(CamIntent, REQUEST_CAMERA);

    }

    // single image pick function
    private void SingleGetImageFromGallery() {
        file = new File(Environment.getExternalStorageDirectory(), "file" + String.valueOf(System.currentTimeMillis()) + ".jpg");
        if (android.os.Build.VERSION.SDK_INT > 23) {
            uri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".provider", file);
        } else {
            uri = Uri.fromFile(file);
        }
        GalIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(GalIntent, "Select Image From Gallery"), SINGLE_SELECT_GALLERY);
    }

    // multi image pick function
    private void multiGetImageFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), MULTI_SELECT_GALLERY);
    }

    //Any File pick function
    private void fileGetFromStorage() {
        Intent intent = new Intent(MainActivity.this, FileChooserActivity.class);
        startActivityForResult(intent, SELECT_FILE);
    }


    @SuppressLint("NewApi")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {

            if (requestCode == REQUEST_CAMERA) { //Camera result
                if (android.os.Build.VERSION.SDK_INT > 23) {
                    ImageCrop();
                } else {
                    ImageCropFunction();
                }
            } else if (requestCode == SINGLE_SELECT_GALLERY) {// One Image result
                uri = data.getData();
                String url = uri.toString();
                if (url.startsWith("content://com.google.android.apps.photos.content")) {
                    try {
                        InputStream is = this.getContentResolver().openInputStream(uri);
                        if (is != null) {
                            Bitmap pictureBitmap = BitmapFactory.decodeStream(is);
                            uri = getImageUri(this, pictureBitmap);
                            ImageCrop();
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    ImageCrop();
                }

            } else if (requestCode == MULTI_SELECT_GALLERY) { // Multi Image result
                photoList.clear();
                fl_selectedPhoto.removeAllViews();

                if (data.getClipData() != null) {

                    int count=data.getClipData().getItemCount();
                    Toast.makeText(this, count+" Image Selected", Toast.LENGTH_SHORT).show();

                    ClipData mClipData = data.getClipData();
                    for (int i = 0; i < mClipData.getItemCount(); i++) {
                        ClipData.Item item = mClipData.getItemAt(i);
                        uri = item.getUri();
                        photoList.add(uri);
                    }
                } else if (data.getClipData() == null) {
                    Toast.makeText(this, "1 Image Selected", Toast.LENGTH_SHORT).show();
                    uri = data.getData();
                    photoList.add(uri);
                }
                AddPhotoOnLayout();
            } else if (requestCode == RESULT_CROP) { //Crop Image result
                if (data != null) {
                    uri = getImageContentUri(this, file);
                    photoList.clear();
                    fl_selectedPhoto.removeAllViews();
                    photoList.add(uri);
                    AddPhotoOnLayout();
                }
            }
            else if (requestCode == SELECT_FILE) { //any file Result
                fl_selectedPhoto.removeAllViews();
                String fileSelected = data.getStringExtra(Constants.KEY_FILE_SELECTED);
                if(fileSelected.contains(".")) {
                    String type = fileSelected.substring(fileSelected.lastIndexOf("."));
                    switch (type) {
                        case ".pdf":
                            filetype(type, R.drawable.pdf);
                            break;
                        case ".jpg":
                            filetype(type, R.drawable.jpg);
                            break;
                        case ".jpeg":
                            filetype(type, R.drawable.jpg);
                            break;
                        case ".docx":
                            filetype(type, R.drawable.doc);
                            break;
                        case ".doc":
                            filetype(type, R.drawable.doc);
                            break;
                        case ".gif":
                            filetype(type, R.drawable.gif);
                            break;
                        case ".ppt":
                            filetype(type, R.drawable.ppt);
                            break;
                        case ".pptx":
                            filetype(type, R.drawable.ppt);
                            break;
                        case ".xls":
                            filetype(type, R.drawable.xls);
                            break;
                        case ".xlsx":
                            filetype(type, R.drawable.xls);
                            break;
                        case ".zip":
                            filetype(type, R.drawable.zip);
                            break;
                        case ".txt":
                            filetype(type, R.drawable.txt);
                            break;
                        default:
                            filetype(type, R.drawable.icon_media);
                            break;
                    }
                }
                else {
                    Toast.makeText(this, "file extension not showing", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void filetype(String type, int icon) {
        Toast.makeText(this, "File: "+type, Toast.LENGTH_SHORT).show();

        ImageView image=new ImageView(this);
        lp = new LinearLayout.LayoutParams(100, 130);
        image.setLayoutParams(lp);
        Picasso.with(this)
                .load(icon)
                .fit()
                .into(image);

        fl_selectedPhoto.addView(image);
    }

    private void AddPhotoOnLayout() {
        for (int i=0;i<=photoList.size()-1;i++) {
            ImageView image=new ImageView(this);
            image.setId(i);
            lp = new LinearLayout.LayoutParams(200, 200);
            lp.setMargins(210,0,0,0);
           /* lp.addRule(RelativeLayout.CENTER_IN_PARENT);*/
            image.setLayoutParams(lp);
            Picasso.with(this)
                    .load(photoList.get(i))
                    .fit()
                    .into(image);

            fl_selectedPhoto.addView(image);
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    private Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    //Scrop function for min SDK 23
    private void ImageCropFunction() {
        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        cropIntent.setDataAndType(uri, "image/*");
        cropIntent.putExtra("crop", "true");
        cropIntent.putExtra("aspectX", 1);
        cropIntent.putExtra("aspectY", 1);
        cropIntent.putExtra("outputX", 128);
        cropIntent.putExtra("outputY", 128);
        cropIntent.putExtra("return-data", true);
        startActivityForResult(cropIntent, RESULT_CROP);
    }

    //Scrop function for max SDK 23
    private void ImageCrop() {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 3);
        intent.putExtra("aspectY", 4);
        intent.putExtra("outputX", 180);
        intent.putExtra("outputY", 180);
        intent.putExtra("outputFormat", "JPEG");
        intent.putExtra("noFaceDetection", true);
        intent.putExtra("return-data", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
        startActivityForResult(intent, RESULT_CROP);
    }
}
