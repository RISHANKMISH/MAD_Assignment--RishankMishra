package com.example.photo_gallery;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private TextView tvCurrentFolder;
    private TextView tvImageCount;
    private MaterialCardView folderCard;
    private MaterialButton btnSelectFolder;
    private MaterialButton btnTakePhoto;
    private LinearLayout emptyState;
    private RecyclerView recyclerView;

    private String selectedFolderPath = null;
    private Uri selectedFolderUri = null;
    private Uri currentPhotoUri = null;

    private List<ImageItem> imageList;
    private ImageAdapter imageAdapter;

    private ActivityResultLauncher<Intent> folderPickerLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> storagePermissionLauncher;
    private ActivityResultLauncher<Intent> storageSettingsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupActivityResultLaunchers();
        setupClickListeners();
    }

    private void initializeViews() {
        tvCurrentFolder = findViewById(R.id.tvCurrentFolder);
        tvImageCount = findViewById(R.id.tvImageCount);
        folderCard = findViewById(R.id.folderCard);
        btnSelectFolder = findViewById(R.id.btnSelectFolder);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        emptyState = findViewById(R.id.emptyState);
        recyclerView = findViewById(R.id.recyclerView);

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        imageList = new ArrayList<>();
        imageAdapter = new ImageAdapter(this, imageList, this::onImageClick);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(imageAdapter);
        recyclerView.setHasFixedSize(true);
    }

    private void onImageClick(ImageItem imageItem, int position) {
        Intent intent = new Intent(this, ImageDetailActivity.class);
        intent.putExtra("imagePath", imageItem.getPath());
        intent.putExtra("imageName", imageItem.getName());
        intent.putExtra("imageSize", imageItem.getSize());
        intent.putExtra("imageDate", imageItem.getDateTaken());
        intent.putExtra("position", position);
        startActivity(intent);
    }

    private void setupActivityResultLaunchers() {
        folderPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            handleFolderSelection(uri);
                        }
                    }
                }
        );

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && currentPhotoUri != null) {
                        if (selectedFolderUri != null) {
                            boolean copied = copyImageToSelectedFolder();
                            if (copied) {
                                Toast.makeText(this, R.string.photo_saved, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, R.string.photo_save_failed, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, R.string.photo_saved, Toast.LENGTH_SHORT).show();
                        }
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

        storagePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openFolderPicker();
                    } else {
                        showStoragePermissionDialog();
                    }
                }
        );

        storageSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> checkStoragePermissionAndOpenPicker()
        );
    }

    private void setupClickListeners() {
        btnSelectFolder.setOnClickListener(v -> checkStoragePermissionAndOpenPicker());
        btnTakePhoto.setOnClickListener(v -> checkCameraPermissionAndLaunch());
    }

    private void checkStoragePermissionAndOpenPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                openFolderPicker();
            } else {
                requestAllFilesAccess();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                openFolderPicker();
            } else {
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void requestAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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

    private void showStoragePermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(R.string.storage_permission_message)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        requestAllFilesAccess();
                    } else {
                        storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        folderPickerLauncher.launch(intent);
    }

    private void handleFolderSelection(Uri uri) {
        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        getContentResolver().takePersistableUriPermission(uri, takeFlags);

        selectedFolderUri = uri;
        selectedFolderPath = FileUtils.getPathFromUri(this, uri);
        if (selectedFolderPath == null) {
            selectedFolderPath = uri.toString();
        }

        tvCurrentFolder.setText(selectedFolderPath);
        loadImages();
    }

    private void loadImages() {
        imageList.clear();

        if (selectedFolderUri != null) {
            // Try loading from SAF URI first
            imageList.addAll(ImageUtils.getImagesFromTreeUri(this, selectedFolderUri));
        }

        // Also try loading from file path if available
        if (selectedFolderPath != null && !selectedFolderPath.startsWith("content://")) {
            imageList.addAll(ImageUtils.getImagesInFolder(selectedFolderPath));
        }

        imageAdapter.notifyDataSetChanged();
        updateImageCount();
        updateGalleryVisibility();
    }

    private void updateGalleryVisibility() {
        if (imageList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateImageCount() {
        int count = imageList != null ? imageList.size() : 0;
        tvImageCount.setText(getString(R.string.images_count, count));
        tvImageCount.setVisibility(View.VISIBLE);
    }

    private void checkCameraPermissionAndLaunch() {
        if (selectedFolderPath == null) {
            Toast.makeText(this, "Please select a folder first", Toast.LENGTH_SHORT).show();
            return;
        }

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
                currentPhotoUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider",
                        photoFile);
                takePictureLauncher.launch(currentPhotoUri);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";

        File storageDir = getCacheDir();

        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private boolean copyImageToSelectedFolder() {
        if (selectedFolderUri == null || currentPhotoUri == null) {
            return false;
        }

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "IMG_" + timeStamp + ".jpg";

            android.content.ContentResolver resolver = getContentResolver();

            // Get the document ID for the selected folder tree
            String docId = android.provider.DocumentsContract.getTreeDocumentId(selectedFolderUri);
            // Build the document URI using the tree
            Uri treeUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(selectedFolderUri, docId);

            // Create the new document in the selected folder
            Uri destinationUri = android.provider.DocumentsContract.createDocument(
                    resolver, treeUri, "image/jpeg", imageFileName);

            if (destinationUri != null) {
                // Get the actual file path from the FileProvider URI
                // FileProvider URIs look like: content://authority/cache/IMG_...
                File cacheFile = new File(getCacheDir(), currentPhotoUri.getLastPathSegment());

                try (FileInputStream fis = new FileInputStream(cacheFile);
                     OutputStream os = resolver.openOutputStream(destinationUri)) {
                    if (os != null) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        os.flush();
                    }
                }

                // Clean up the temp file
                cacheFile.delete();

                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void showPermissionDeniedDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (selectedFolderUri != null || selectedFolderPath != null) {
            loadImages();
        }
    }
}
