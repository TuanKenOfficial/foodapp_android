package com.example.app_food_basic.Fragment.QuanAn;

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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.app_food_basic.Adapter.AdapterSanPham;
import com.example.app_food_basic.Model.ModelSanPham;
import com.example.app_food_basic.R;
import com.example.app_food_basic.databinding.FragmentTrangChuBinding;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TrangChu extends Fragment {
    private FragmentTrangChuBinding binding;

    private Uri image_uri;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private ArrayList<ModelSanPham> modelSanPhams = new ArrayList<>();
    private AdapterSanPham adapterSanPham;

    private GridLayoutManager gridLayoutManager;
    private static final String TAG = "Image_trangchu";
    public TrangChu() {
        // Required empty public constructor
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentTrangChuBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        firebaseAuth = FirebaseAuth.getInstance();
        binding.themMoi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogThemMoi();
            }
        });

        progressDialog = new ProgressDialog(getActivity(), R.style.CustomAlertDialog);
        progressDialog.setCanceledOnTouchOutside(false);
        loadSanPham();
        binding.timKiem.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    adapterSanPham.getFilter().filter(charSequence);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        return view;
    }
    private void loadSanPham() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("SanPham");
        reference.orderByChild("uid")
                .equalTo(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        modelSanPhams.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            ModelSanPham sanPham = ds.getValue(ModelSanPham.class);
                            modelSanPhams.add(sanPham);
                        }
                        if (modelSanPhams.size() == 0) {
                            binding.relativeLayout.setVisibility(View.GONE);
                            binding.empty.setVisibility(View.VISIBLE);
                        } else {
                            binding.relativeLayout.setVisibility(View.VISIBLE);
                            binding.empty.setVisibility(View.GONE);
                            adapterSanPham = new AdapterSanPham(getActivity(), modelSanPhams);
                            gridLayoutManager = new GridLayoutManager(getActivity(), 2);
                            binding.sanPhamRv.setLayoutManager(gridLayoutManager);
                            binding.sanPhamRv.setAdapter(adapterSanPham);
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    // dialog thêm món mới
    private EditText tenSpEt, giaGocEt, moTaEt, giaGiamEt;
    private TextView tiLeTv;
    private String tenSp, giaGoc, moTa, tiLe, giamCon;
    private ImageView hinhSP;
    private boolean coGiamGia = false;
    private SwitchCompat giamGiaSwitch;
    private AlertDialog dialog;
    private double giaGocDouble, giaGiamDouble, tiLeDouble;
    private void dialogThemMoi() {
        // gán view vào dialog
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_them_moi, null);
        tenSpEt = view.findViewById(R.id.tenSP);
        giaGocEt = view.findViewById(R.id.giaCa);
        moTaEt = view.findViewById(R.id.moTa);
        giaGiamEt = view.findViewById(R.id.giamCon);
        tiLeTv = view.findViewById(R.id.tiLe);
        giamGiaSwitch = view.findViewById(R.id.coGiamGia);
        hinhSP = view.findViewById(R.id.hinhSP);
        giaGiamEt.setVisibility(View.GONE);
        tiLeTv.setVisibility(View.GONE);
        // hiển thị phần trăm giảm giá nếu có giảm
        giamGiaSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                giaGiamEt.setVisibility(View.VISIBLE);
                tiLeTv.setVisibility(View.VISIBLE);
            } else {
                giaGiamEt.setVisibility(View.GONE);
                tiLeTv.setVisibility(View.GONE);
            }
        });
        hinhSP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (image_uri == null){
                    option();
                }

            }
        });
        Button themSp = view.findViewById(R.id.themSp);
        // khởi tạo dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.CustomAlertDialog);
        builder.setView(view);
        dialog = builder.create();
        // hiển thị dialog
        dialog.show();
        themSp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkData();
            }
        });

    }
    // kiểm tra dl nhập vào
    private void checkData() {
        tenSp = tenSpEt.getText().toString().trim();
        giaGoc = giaGocEt.getText().toString().trim();
        moTa = moTaEt.getText().toString().trim();
        coGiamGia = giamGiaSwitch.isChecked();
        giaGocDouble = Double.parseDouble(giaGoc);

        if (image_uri == null) {
            new AlertDialog.Builder(getActivity(), R.style.CustomAlertDialog)
                    .setTitle("Thêm thất bại")
                    .setMessage("Món ăn phải có hình ảnh mới được chứ bạn")
                    .setPositiveButton("OK", null).show();
            return;
        }
        if (tenSp.isEmpty()) {
            tenSpEt.setError("Bạn không được bỏ trống tên của món ăn");
            return;
        }
        if (giaGoc.isEmpty()) {
            giaGocEt.setError("Bạn phải điền giá của món ăn");
            return;
        }
        if (moTa.isEmpty()) {
            moTaEt.setError("Hãy mô tả món ăn của bạn");
            return;
        }
        if (coGiamGia) {
            giamCon = giaGiamEt.getText().toString().trim();
            giaGiamDouble = Double.parseDouble(giamCon);
            tiLeDouble = (giaGocDouble - giaGiamDouble) / giaGocDouble * 100;
            int lamTron = (int) Math.round(tiLeDouble);
            tiLeTv.setText("" + lamTron);
            tiLe = String.valueOf(lamTron);
            if (giamCon.isEmpty()) {
                giaGiamEt.setError("Hãy nhập giá giảm món ăn này");
                return;
            }
            if (giaGiamDouble >= giaGocDouble) {
                new AlertDialog.Builder(getContext(), R.style.CustomAlertDialog)
                        .setTitle("Lỗi")
                        .setMessage("Giá giảm phải nhỏ hơn giá gốc của món ăn")
                        .setPositiveButton("OK", null).show();
                return;
            }

        } else {
            giamCon = "0";
            tiLe = "";
        }

        themMoi();
    }
    // tiến hành thêm món ăn sau khi kiểm tra
    private void themMoi() {
        progressDialog.setMessage("Đang thêm món ăn");
        progressDialog.show();
        final String timestamp = ""+System.currentTimeMillis();
        String path = "hinh_anh_mon_an/" + "" + timestamp;
        StorageReference storageReference = FirebaseStorage.getInstance().getReference(path);
        storageReference.putFile(image_uri).addOnSuccessListener(taskSnapshot -> {
            Task<Uri> task = taskSnapshot.getStorage().getDownloadUrl();
            while (!task.isSuccessful());
            Uri downloadImageUri = task.getResult();
            if (task.isSuccessful()) {
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("maSp", ""+timestamp);
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
                reference.child(timestamp)
                        .updateChildren(hashMap)
                        .addOnSuccessListener(unused -> {
                            progressDialog.dismiss();
                            new AlertDialog.Builder(getActivity(), R.style.CustomAlertDialog)
                                    .setMessage("Thêm thành công")
                                    .setPositiveButton("OK", (dialogInterface, i) -> dialog.dismiss()).show();
                        }).addOnFailureListener(e -> {
                            new AlertDialog.Builder(getActivity(), R.style.CustomAlertDialog)
                                    .setMessage("Thêm thất bại, lỗi: " + e.getMessage())
                                    .setPositiveButton("OK", null).show();
                        });

            }
        }).addOnFailureListener(e -> {

        });

    }

    // chọn hình từ camera hoặc thư viện
    private void option() {

        PopupMenu popupMenu = new androidx.appcompat.widget.PopupMenu(getContext(),hinhSP);
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "Camera");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "Thư viện ảnh");

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
                        Toast.makeText(getContext(), "Quyền camera hoặc storage", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(getContext(), "Quyền Storage chưa cấp quyền", Toast.LENGTH_SHORT).show();
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


        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

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
                        hinhSP.setImageURI(image_uri);

                    } else {
                        Toast.makeText(getContext(), "Hủy", Toast.LENGTH_SHORT).show();
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
                        hinhSP.setImageURI(image_uri);
                        Log.d(TAG, "onActivityResult: Hình ảnh thư viện: " + image_uri);

                    } else {
                        Log.d(TAG, "onActivityResult: "+image_uri);
                        Toast.makeText(getContext(), "Hủy", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

}