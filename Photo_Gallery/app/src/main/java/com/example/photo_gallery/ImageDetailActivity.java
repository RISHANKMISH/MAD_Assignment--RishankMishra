package com.example.photo_gallery;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageDetailActivity extends AppCompatActivity {

    private ImageView ivDetailImage;
    private TextView tvImageName;
    private TextView tvImagePath;
    private TextView tvImageSize;
    private TextView tvImageDate;
    private MaterialButton btnDelete;
    private MaterialToolbar toolbar;

    private String imagePath;
    private String imageName;
    private long imageSize;
    private long imageDateMillis;
    private int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ivDetailImage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        extractIntentData();
        initializeViews();
        setupToolbar();
        displayImageDetails();
        setupClickListeners();
    }

    private void extractIntentData() {
        Intent intent = getIntent();
        imagePath = intent.getStringExtra("imagePath");
        imageName = intent.getStringExtra("imageName");
        imageSize = intent.getLongExtra("imageSize", 0);
        imageDateMillis = intent.getLongExtra("imageDate", 0);
        position = intent.getIntExtra("position", -1);
    }

    private void initializeViews() {
        ivDetailImage = findViewById(R.id.ivDetailImage);
        tvImageName = findViewById(R.id.tvImageName);
        tvImagePath = findViewById(R.id.tvImagePath);
        tvImageSize = findViewById(R.id.tvImageSize);
        tvImageDate = findViewById(R.id.tvImageDate);
        btnDelete = findViewById(R.id.btnDelete);
        toolbar = findViewById(R.id.topAppBar);
    }

    private void setupToolbar() {
        toolbar.setTitle(imageName);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void displayImageDetails() {
        if (imagePath != null) {
            Object loadSource = imagePath.startsWith("content://") ? Uri.parse(imagePath) : new File(imagePath);
            Glide.with(this)
                    .load(loadSource)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.ic_gallery)
                    .error(R.drawable.ic_gallery)
                    .into(ivDetailImage);
        }

        tvImageName.setText(imageName != null ? imageName : "Unknown");
        tvImagePath.setText(imagePath != null ? imagePath : "Unknown");
        tvImageSize.setText(ImageUtils.formatFileSize(imageSize));

        if (imageDateMillis > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss", Locale.getDefault());
            String formattedDate = sdf.format(new Date(imageDateMillis));
            tvImageDate.setText(formattedDate);
        } else {
            tvImageDate.setText("Unknown");
        }
    }

    private void setupClickListeners() {
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_image)
                .setMessage(R.string.delete_confirmation)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteImage())
                .setNegativeButton(R.string.cancel, null)
                .setIcon(R.drawable.ic_delete)
                .show();
    }

    private void deleteImage() {
        if (imagePath == null) {
            Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean deleted = false;

        // Handle content URIs (SAF)
        if (imagePath.startsWith("content://")) {
            deleted = deleteDocument(Uri.parse(imagePath));
        } else {
            // Handle file paths
            File imageFile = new File(imagePath);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                deleted = deleteImageUsingMediaStore();
            }

            if (!deleted && imageFile.exists()) {
                deleted = imageFile.delete();
            }
        }

        if (deleted) {
            Toast.makeText(this, R.string.image_deleted, Toast.LENGTH_SHORT).show();
            setResult(Activity.RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean deleteDocument(Uri documentUri) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                return DocumentsContract.deleteDocument(getContentResolver(), documentUri);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean deleteImageUsingMediaStore() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                Uri contentUri = ImageUtils.getContentUriFromFile(this, imagePath);
                if (contentUri != null) {
                    int rowsDeleted = getContentResolver().delete(contentUri, null, null);
                    return rowsDeleted > 0;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
