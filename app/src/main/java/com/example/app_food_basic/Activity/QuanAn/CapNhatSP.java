package com.example.app_food_basic.Activity.QuanAn;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.example.app_food_basic.R;
import com.example.app_food_basic.databinding.ActivityCapNhatSpBinding;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class CapNhatSP extends AppCompatActivity {
    private ActivityCapNhatSpBinding binding;
    private String maSp;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    private double gia_goc, gia_giam, tiLePhanTram;
    private Uri image_uri;
    private static final String TAG = "Image";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCapNhatSpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        maSp = getIntent().getStringExtra("maSp");
        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this, R.style.CustomAlertDialog);
        progressDialog.setCanceledOnTouchOutside(false);
        loadSanPham(maSp);
        binding.coGiamGia.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                binding.giamCon.setVisibility(View.VISIBLE);
                binding.tiLe.setVisibility(View.VISIBLE);
            } else {
                binding.giamCon.setVisibility(View.GONE);
                binding.tiLe.setVisibility(View.GONE);
            }
        });
        binding.capNhat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkData();
            }
        });
        binding.hinhAnh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: ");
                option();
            }
        });
    }
    private void option() {
        Log.d(TAG, "showImagePickOption: ");
        PopupMenu popupMenu = new androidx.appcompat.widget.PopupMenu(CapNhatSP.this,binding.hinhAnh);
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "Camera");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "Gallery");

        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == 1) {
                    Log.d(TAG, "onMenuItemClick: Mở camera, check camera");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestCameraPemissions.launch(new String[]{Manifest.permission.CAMERA});
                    } else {
                        requestCameraPemissions.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE});
                    }
                } else if (itemId == 2) {
                    Log.d(TAG, "onMenuItemClick: Mở storage, check storage");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pickFromGallery();
                    } else {
                        requestStoragePemissions.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                }
                return false;
            }
        });

    }
    private void loadSanPham(String id) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("SanPham");
        reference.child(id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String hinhAnh = "" + snapshot.child("hinhAnh").getValue();
                String tenSp = "" + snapshot.child("tenSp").getValue();
                String giaGoc = "" + snapshot.child("giaGoc").getValue();
                String moTa = "" + snapshot.child("moTa").getValue();
                String coGiamGia = "" + snapshot.child("coGiamGia").getValue();
                String giaGiam = "" + snapshot.child("giaGiam").getValue();
                String tiLe = "" + snapshot.child("tiLeGiam").getValue();
                Picasso.get().load(hinhAnh).fit().centerCrop()
                        .placeholder(R.drawable.shopivhd)
                        .error(R.drawable.shopivhd)
                        .into(binding.hinhAnh);
                binding.tenSP.setText(tenSp);
                binding.giaCa.setText(giaGoc);
                binding.moTa.setText(moTa);
                if (coGiamGia.equals("true")) {
                    binding.coGiamGia.setChecked(true);
                    binding.giamCon.setText(giaGiam);
                    binding.tiLe.setText(tiLe);
                } else {
                    binding.giamCon.setVisibility(View.GONE);
                    binding.tiLe.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void capNhat(String id) {
        // cập nhật khi món ăn đã có sẵn ảnh.
        if (image_uri == null) {
            progressDialog.setMessage("Đang cập nhật...");
            progressDialog.show();
            final String timestamp = ""+System.currentTimeMillis();
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("tenSp", ""+tenSp);
            hashMap.put("moTa", ""+moTa);
            hashMap.put("giaGoc", ""+giaGoc);
            hashMap.put("giaGiam", ""+giamCon);
            hashMap.put("tiLeGiam", ""+tiLe);
            hashMap.put("coGiamGia", ""+coGiamGia);
            hashMap.put("timestamp", ""+timestamp);
            hashMap.put("uid", ""+firebaseAuth.getUid());
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("SanPham");
            reference.child(id).updateChildren(hashMap).addOnSuccessListener(unused -> {
                progressDialog.dismiss();
                new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                        .setMessage("Cập nhật thành công")
                        .setPositiveButton("OK", (dialogInterface, i) -> {
                            onBackPressed();
                            super.finish();
                        }).show();
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                        .setMessage("Cập nhật thất bại, lỗi: " + e.getMessage())
                        .setPositiveButton("OK", null).show();
            });
        } else capNhatHinhAnh(id);

    }
    private void capNhatHinhAnh(String id) {
        progressDialog.setMessage("Đang cập nhật...");
        progressDialog.show();
        final String timestamp = ""+System.currentTimeMillis();
        String path = "hinh_anh_mon_an/" + "" + id;
        StorageReference storageReference = FirebaseStorage.getInstance().getReference(path);
        storageReference.putFile(image_uri).addOnSuccessListener(taskSnapshot -> {
            Task<Uri> task = taskSnapshot.getStorage().getDownloadUrl();
            while (!task.isSuccessful());
            Uri downloadImageUri = task.getResult();
            if (task.isSuccessful()) {
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("tenSp", ""+tenSp);
                hashMap.put("moTa", ""+moTa);
                hashMap.put("hinhAnh", ""+downloadImageUri);
                hashMap.put("giaGoc", ""+giaGoc);
                hashMap.put("giaGiam", ""+giamCon);
                hashMap.put("tiLeGiam", ""+tiLe);
                hashMap.put("coGiamGia", ""+coGiamGia);
                hashMap.put("timestamp", ""+timestamp);
                hashMap.put("uid", ""+firebaseAuth.getUid());
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("SanPham");
                reference.child(id)
                        .updateChildren(hashMap)
                        .addOnSuccessListener(unused -> {
                            progressDialog.dismiss();
                            new AlertDialog.Builder(getApplicationContext(), R.style.CustomAlertDialog)
                                    .setMessage("Cập nhật thành công")
                                    .setPositiveButton("OK", (dialogInterface, i) -> {
                                        onBackPressed();
                                        super.finish();
                                    }).show();
                        }).addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            new AlertDialog.Builder(getApplicationContext(), R.style.CustomAlertDialog)
                                    .setMessage("Cập nhật thất bại, lỗi: " + e.getMessage())
                                    .setPositiveButton("OK", null).show();
                        });

            }
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            new AlertDialog.Builder(getApplicationContext(), R.style.CustomAlertDialog)
                    .setMessage("Cập nhật thất bại, lỗi: " + e.getMessage())
                    .setPositiveButton("OK", null).show();
        });
    }
    private String tenSp, giaGoc, moTa, tiLe, giamCon;
    protected boolean coGiamGia = false;
    private void checkData() {
        tenSp = binding.tenSP.getText().toString().trim();
        giaGoc = binding.giaCa.getText().toString().trim();
        gia_goc = Double.parseDouble(giaGoc);
        moTa = binding.moTa.getText().toString().trim();
        coGiamGia = binding.coGiamGia.isChecked();
        if (tenSp.isEmpty()) {
            binding.tenSP.setError("Bạn không được bỏ trống tên của món ăn");
            return;
        }
        if (giaGoc.isEmpty()) {
            binding.giaCa.setError("Bạn phải điền giá của món ăn");
            return;
        }
        if (moTa.isEmpty()) {
            binding.moTa.setError("Hãy mô tả món ăn của bạn");
            return;
        }
        if (coGiamGia) {
            giamCon = binding.giamCon.getText().toString().trim();
            gia_giam = Double.parseDouble(giamCon);
            // lấy tỉ lệ % giảm
            tiLePhanTram = (gia_goc - gia_giam) / gia_goc * 100;
            int lamTron = (int) Math.round(tiLePhanTram);
            binding.tiLe.setText("" + lamTron);
            tiLe = String.valueOf(lamTron);
            if (gia_giam >= gia_goc) {
                binding.giamCon.setError("Lỗi, giá giảm phải nhỏ hơn giá gốc");
                return;
            }
            if (giamCon.isEmpty()) {
                binding.giamCon.setError("Hãy nhập giá giảm món ăn này");
                return;
            }

        } else {
            giamCon = "0";
            tiLe = "";
        }
        capNhat(maSp);
    }



    // kiểm tra người dùng có cấp quyền truy cập camera hay chưa
    private ActivityResultLauncher<String[]> requestCameraPemissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    Log.d(TAG, "onActivityResult: " + result.toString());
                    boolean areAllGranted = true;
                    for (Boolean isGranted : result.values()) {
                        areAllGranted = areAllGranted && isGranted;
                    }
                    if (areAllGranted) {
                        Log.d(TAG, "onActivityResult: Tất cả quyền camera & storage");
                        pickFromCamera();
                    } else {
                        Log.d(TAG, "onActivityResult: Tất cả hoặc chỉ có một quyền");
                        Toast.makeText(CapNhatSP.this, "Quyền camera hoặc storage", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );
    // kiểm tra người dùng có cấp quyền truy cập bộ nhớ hay chưa
    private ActivityResultLauncher<String> requestStoragePemissions = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (isGranted) {
                        pickFromGallery();
                    } else {
                        Toast.makeText(CapNhatSP.this, "Quyền Storage chưa cấp quyền", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );


    // chụp hình
    private void pickFromCamera() {
        Log.d(TAG, "pickFromCamera: ");
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_Image Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp_Image Description");


        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        cameraActivityResultLaucher.launch(intent);

    }
    // lấy hình từ thư viện
    private void pickFromGallery() {
        Log.d(TAG, "pickFromGallery: ");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLaucher.launch(intent);
    }


    private ActivityResultLauncher<Intent> cameraActivityResultLaucher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d(TAG, "onActivityResult: Camera" + image_uri);
                        binding.hinhAnh.setImageURI(image_uri);

                    } else {
                        Toast.makeText(CapNhatSP.this, "Hủy", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );
    private ActivityResultLauncher<Intent> galleryActivityResultLaucher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        image_uri = data.getData();
                        Log.d(TAG, "onActivityResult: Hình ảnh thư viện: " + image_uri);
                        binding.hinhAnh.setImageURI(image_uri);

                    } else {
                        Log.d(TAG, "onActivityResult: "+image_uri);
                        Toast.makeText(CapNhatSP.this, "Hủy", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );
}