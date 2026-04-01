package com.example.photo_gallery;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GalleryActivity extends AppCompatActivity implements ImageAdapter.OnImageClickListener {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private FloatingActionButton fabCamera;
    private MaterialToolbar toolbar;

    private String folderPath;
    private List<ImageItem> imageList;
    private ImageAdapter imageAdapter;

    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Intent> storageSettingsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gallery);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recyclerView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        folderPath = getIntent().getStringExtra("folderPath");
        if (folderPath == null) {
            Toast.makeText(this, "No folder selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupActivityResultLaunchers();
        setupRecyclerView();
        setupClickListeners();
        loadImages();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        emptyState = findViewById(R.id.emptyState);
        fabCamera = findViewById(R.id.fabCamera);
        toolbar = findViewById(R.id.topAppBar);

        File folder = new File(folderPath);
        toolbar.setTitle(folder.getName());
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupActivityResultLaunchers() {
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success) {
                        Toast.makeText(this, R.string.photo_saved, Toast.LENGTH_SHORT).show();
                        loadImages();
                    } else {
                        Toast.makeText(this, R.string.photo_save_failed, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCamera();
                    } else {
                        showPermissionDeniedDialog(getString(R.string.camera_permission_message));
                    }
                }
        );

        storageSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> checkStoragePermissionAndLaunch()
        );
    }

    private void setupRecyclerView() {
        imageList = new ArrayList<>();
        imageAdapter = new ImageAdapter(this, imageList, this);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(imageAdapter);
        recyclerView.setHasFixedSize(true);
    }

    private void setupClickListeners() {
        fabCamera.setOnClickListener(v -> checkCameraPermissionAndLaunch());
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, R.string.photo_save_failed, Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                Uri photoUri = androidx.core.content.FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider",
                        photoFile);
                takePictureLauncher.launch(photoUri);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + ".jpg";

        File storageDir = new File(folderPath);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        return new File(storageDir, imageFileName);
    }

    private void checkStoragePermissionAndLaunch() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                launchCamera();
            } else {
                requestAllFilesAccess();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void requestAllFilesAccess() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                storageSettingsLauncher.launch(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                storageSettingsLauncher.launch(intent);
            }
        }
    }

    private void loadImages() {
        imageList.clear();
        imageList.addAll(ImageUtils.getImagesInFolder(folderPath));
        imageAdapter.notifyDataSetChanged();

        if (imageList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onImageClick(ImageItem imageItem, int position) {
        Intent intent = new Intent(this, ImageDetailActivity.class);
        intent.putExtra("imagePath", imageItem.getPath());
        intent.putExtra("imageName", imageItem.getName());
        intent.putExtra("imageSize", imageItem.getSize());
        intent.putExtra("imageDate", imageItem.getDateTaken());
        intent.putExtra("position", position);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            loadImages();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadImages();
    }

    private void showPermissionDeniedDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }
}
