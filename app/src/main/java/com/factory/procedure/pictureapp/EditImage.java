package com.factory.procedure.pictureapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.factory.procedure.pictureapp.image_edit.PropertiesBSFragment;
import com.factory.procedure.pictureapp.image_edit.TextEditorDialogFragment;
import com.factory.procedure.pictureapp.tools.EditingToolsAdapter;
import com.factory.procedure.pictureapp.tools.ToolType;

import java.io.File;
import java.io.IOException;

import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.SaveSettings;

public class EditImage extends AppCompatActivity{


    private ProgressDialog mProgressDialog;
    public static final int READ_WRITE_STORAGE = 52;

    private PhotoEditorView mPhotoEditorView;
    private String imagePath;
    private TextView mTxtCurrentTool;
    private PhotoEditor mPhotoEditor;
    private PropertiesBSFragment mPropertiesBSFragment;
    private RecyclerView mRvTools, mRvFilters;
    private ConstraintLayout mRootView;
    private EditingToolsAdapter mEditingToolsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        makeFullScreen();
        setContentView(R.layout.edit_image);
        initViews();
        Bundle extras = getIntent().getExtras();
        imagePath = extras.getString(MediaDetails.IMAGE_DETAIL);

        mPropertiesBSFragment = new PropertiesBSFragment();
//        mEmojiBSFragment = new EmojiBSFragment();
//        mStickerBSFragment = new StickerBSFragment();
//        mStickerBSFragment.setStickerListener(this);
//        mEmojiBSFragment.setEmojiListener(this);
        mPropertiesBSFragment.setPropertiesChangeListener(properties);

        mPhotoEditorView = findViewById(R.id.photoEditorView);
        if (imagePath != null) {

            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
//        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            mPhotoEditorView.getSource().setImageBitmap(bitmap);
        }

        LinearLayoutManager llmTools = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mRvTools.setLayoutManager(llmTools);
        mRvTools.setAdapter(new EditingToolsAdapter(editingToolsAdapter));


        Typeface mTextRobotoTf = Typeface.createFromAsset(getAssets(),  "fonts/Roboto-Medium.ttf");
//        Typeface mTextRobotoTf = ResourcesCompat.getFont(this, R.font.roboto_medium);
        Typeface mEmojiTypeFace = Typeface.createFromAsset(getAssets(), "emojione-android.ttf");

        mPhotoEditor = new PhotoEditor.Builder(this, mPhotoEditorView)
                .setPinchTextScalable(true) // set flag to make text scalable when pinch
                .setDefaultTextTypeface(mTextRobotoTf)
                .setDefaultEmojiTypeface(mEmojiTypeFace)
                .build(); // build photo editor sdk


//        mPhotoEditor.setOnPhotoEditorListener(this);
    }


    @SuppressLint("MissingPermission")
    private void saveImage() {
        if (requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//            TODO translate
            showLoading(getString(R.string.saving));

//            TODO overwrite the file

            File file = new File(imagePath);
//            Delete and then write
            file.delete();

            try {
                file.createNewFile();

                SaveSettings saveSettings = new SaveSettings.Builder()
                        .setClearViewsEnabled(true)
                        .setTransparencyEnabled(true)
                        .build();

                mPhotoEditor.saveAsFile(file.getAbsolutePath(), saveSettings, new PhotoEditor.OnSaveListener() {
                    @Override
                    public void onSuccess(@NonNull String imagePath) {
                        hideLoading();
//                        TODO do translations
                        Toast.makeText(getApplicationContext(), R.string.image_edit_saved, Toast.LENGTH_SHORT).show();
                        mPhotoEditorView.getSource().setImageURI(Uri.fromFile(new File(imagePath)));
//                        TODO Leave close this page
                        finish();
                    }

                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        hideLoading();
                        Toast.makeText(getApplicationContext(), R.string.image_edit_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                hideLoading();
                showSnackbar(e.getMessage());
            }
        }
    }

    protected void showLoading(@NonNull String message) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(message);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    protected void hideLoading() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    protected void showSnackbar(@NonNull String message) {
        View view = findViewById(android.R.id.content);
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    public boolean requestPermission(String permission) {
        boolean isGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
        if (!isGranted) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{permission},
                    READ_WRITE_STORAGE);
        }
        return isGranted;
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.confirm_edit_exit));
        builder.setPositiveButton(getString(R.string.save_btn), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveImage();
            }
        });
        builder.setNegativeButton(R.string.cancel_btn, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setNeutralButton(getString(R.string.discard_btn), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.create().show();

    }



    EditingToolsAdapter.OnItemSelected editingToolsAdapter = new EditingToolsAdapter.OnItemSelected() {
        @Override
        public void onToolSelected(ToolType toolType) {
            switch (toolType) {
                case BRUSH:
                    mPhotoEditor.setBrushDrawingMode(true);
                    mTxtCurrentTool.setText(R.string.label_brush);
                    mPropertiesBSFragment.show(getSupportFragmentManager(), mPropertiesBSFragment.getTag());
                    break;
                case TEXT:
                    TextEditorDialogFragment textEditorDialogFragment = TextEditorDialogFragment.show(EditImage.this);
                    textEditorDialogFragment.setOnTextEditorListener(new TextEditorDialogFragment.TextEditor() {
                        @Override
                        public void onDone(String inputText, int colorCode) {
                            mPhotoEditor.addText(inputText, colorCode);
                            mTxtCurrentTool.setText(R.string.label_text);
                        }
                    });
                    break;
                case ERASER:
                    mPhotoEditor.brushEraser();
                    mTxtCurrentTool.setText(R.string.label_eraser);
                    break;
//                case FILTER:
//                    mTxtCurrentTool.setText(R.string.label_filter);
//                    showFilter(true);
//                    break;
//                case EMOJI:
//                    mEmojiBSFragment.show(getSupportFragmentManager(), mEmojiBSFragment.getTag());
//                    break;
//                case STICKER:
//                    mStickerBSFragment.show(getSupportFragmentManager(), mStickerBSFragment.getTag());
//                    break;
            }
        }
    };


    PropertiesBSFragment.Properties properties = new PropertiesBSFragment.Properties() {
        @Override
        public void onColorChanged(int colorCode) {
            mPhotoEditor.setBrushColor(colorCode);
            mTxtCurrentTool.setText(R.string.label_brush);
        }

        @Override
        public void onOpacityChanged(int opacity) {
            mPhotoEditor.setOpacity(opacity);
            mTxtCurrentTool.setText(R.string.label_brush);
        }

        @Override
        public void onBrushSizeChanged(int brushSize) {
            mPhotoEditor.setBrushSize(brushSize);
            mTxtCurrentTool.setText(R.string.label_brush);
        }
    };


    private void initViews() {
        ImageView imgUndo;
        ImageView imgRedo;
//        ImageView imgCamera;
//        ImageView imgGallery;
        ImageView imgSave;
        ImageView imgClose;

        mPhotoEditorView = findViewById(R.id.photoEditorView);
        mTxtCurrentTool = findViewById(R.id.txtCurrentTool);
        mRvTools = findViewById(R.id.rvConstraintTools);
        mRvFilters = findViewById(R.id.rvFilterView);
        mRootView = findViewById(R.id.rootView);

        imgUndo = findViewById(R.id.imgUndo);
        imgUndo.setOnClickListener(buttonListener);

        imgRedo = findViewById(R.id.imgRedo);
        imgRedo.setOnClickListener(buttonListener);

//        imgCamera = findViewById(R.id.imgCamera);
//        imgCamera.setOnClickListener(buttonListener);
//        imgGallery = findViewById(R.id.imgGallery);
//        imgGallery.setOnClickListener(buttonListener);

        imgSave = findViewById(R.id.imgSave);
        imgSave.setOnClickListener(buttonListener);

        imgClose = findViewById(R.id.imgClose);
        imgClose.setOnClickListener(buttonListener);
    }

    private View.OnClickListener buttonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {

                case R.id.imgUndo:
                    mPhotoEditor.undo();
                    break;

                case R.id.imgRedo:
                    mPhotoEditor.redo();
                    break;

                case R.id.imgSave:
                    saveImage();
                    break;

                case R.id.imgClose:
                    onBackPressed();
                    break;
            }
        }
    };

    @Override
    public void onBackPressed() {
//        if (mIsFilterVisible) {
//            showFilter(false);
//            mTxtCurrentTool.setText(R.string.app_name);
//        } else if (!mPhotoEditor.isCacheEmpty()) {
        if (!mPhotoEditor.isCacheEmpty()) {
            showSaveDialog();
        } else {
            super.onBackPressed();
        }
    }
}
