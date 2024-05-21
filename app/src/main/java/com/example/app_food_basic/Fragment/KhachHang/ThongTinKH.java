package com.example.app_food_basic.Fragment.KhachHang;

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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;

import com.example.app_food_basic.Activity.DangNhap;
import com.example.app_food_basic.R;
import com.example.app_food_basic.databinding.FragmentThongTinKhBinding;
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
import java.util.Objects;

public class ThongTinKH extends Fragment {
    private FragmentThongTinKhBinding binding;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference reference;
    private ProgressDialog progressDialog;

    private Uri image_uri;
    private static  final  String TAG = "image_profile_kh";
    public ThongTinKH() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentThongTinKhBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        progressDialog = new ProgressDialog(getActivity());

        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage("Đang cập nhật");
        firebaseAuth = FirebaseAuth.getInstance();
        reference = FirebaseDatabase.getInstance().getReference("TaiKhoan");
        loadUser();
        binding.avatar.setOnClickListener(view1 -> option());
        binding.xacNhan.setOnClickListener(view1 -> capNhatTT());
        binding.dangXuat.setOnClickListener(view1 -> dangXuat());
        return view;
    }
    private void dangXuat() {
        new AlertDialog.Builder(requireActivity(), R.style.CustomAlertDialog)
                .setTitle("Đăng xuất khỏi ứng dụng")
                .setMessage("Bạn có muốn đăng xuất không?")
                .setPositiveButton("Có", (dialogInterface, i) -> {
                    startActivity(new Intent(requireActivity(), DangNhap.class));
                    firebaseAuth.signOut();
                })
                .setNegativeButton("Không", null).show();
    }
    private void loadUser() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("TaiKhoan");
        reference.child(Objects.requireNonNull(firebaseAuth.getUid()))
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // lấy dữ liệu từ csdl
                        String hoTen = "" + snapshot.child("hoTen").getValue();
                        String sdt = "" + snapshot.child("sdt").getValue();
                        String tinhTP = "" + snapshot.child("tinhTP").getValue();
                        String quanHuyen = "" + snapshot.child("quanHuyen").getValue();
                        String diaChi = "" + snapshot.child("diaChi").getValue();
                        String avatar = "" + snapshot.child("avatar").getValue();
                        // gán vào view
                        binding.hoTen.setText(hoTen);
                        binding.sdt.setText(sdt);
                        binding.tinhTP.setText(tinhTP);
                        binding.quanHuyen.setText(quanHuyen);
                        binding.diaChi.setText(diaChi);
                        try {
                            Picasso.get().load(avatar).fit().centerCrop()
                                    .placeholder(R.drawable.fork)
                                    .error(R.drawable.fork)
                                    .into(binding.avatar);
                        } catch (Exception e) {
                            binding.avatar.setImageResource(R.drawable.fork);
                        }

                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    String hoTen, sdt, tinhTP, quanHuyen, diaChi;
    private void capNhatTT() {

        hoTen = binding.hoTen.getText().toString().trim();
        sdt = binding.sdt.getText().toString().trim();
        tinhTP = binding.tinhTP.getText().toString().trim();
        quanHuyen = binding.quanHuyen.getText().toString().trim();
        diaChi = binding.diaChi.getText().toString().trim();
        if (hoTen.isEmpty() || sdt.isEmpty() || tinhTP.isEmpty() || quanHuyen.isEmpty() || diaChi.isEmpty()) {
            dialog("Cập nhật thất bại", "Thông tin không được bỏ trống");
            return;
        }
        if (sdt.length() < 10) {
            dialog("Cập nhật thất bại", "Số điện thoại phải dài hơn hoặc bằng 10 ký tự");
            return;
        }
        xacNhan();
    }
    private void xacNhan() {
        // cập nhật thông tin khi không có avatar
        if (image_uri == null) {
            progressDialog.show();
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("hoTen", "" + hoTen);
            hashMap.put("sdt", "" + sdt);
            hashMap.put("tinhTP", "" + tinhTP);
            hashMap.put("quanHuyen", "" + quanHuyen);
            hashMap.put("diaChi", "" + diaChi);
            reference.child(Objects.requireNonNull(firebaseAuth.getUid()))
                    .updateChildren(hashMap)
                    .addOnSuccessListener(unused -> {
                        progressDialog.dismiss();
                        dialog("Cập nhật thông tin", "Cập nhật thành công");

                    }).addOnFailureListener(e -> {
                        dialog("Cập nhật thông tin", "Cập nhật thất bại, lý do: " + e.getMessage());

                    });
        } else capNhatAvatar();

    }
    private void capNhatAvatar() {
        progressDialog.show();
        String filePathAndName = "avatar/" + "" + firebaseAuth.getUid();
        StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
        storageReference.putFile(image_uri).addOnSuccessListener(taskSnapshot -> {
            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
            while (!uriTask.isSuccessful());
            Uri uRL = uriTask.getResult();
            if (uriTask.isSuccessful()) {
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("hoTen", "" + hoTen);
                hashMap.put("sdt", "" + sdt);
                hashMap.put("tinhTP", "" + tinhTP);
                hashMap.put("quanHuyen", "" + quanHuyen);
                hashMap.put("diaChi", "" + diaChi);
                hashMap.put("avatar", "" + uRL);
                reference.child(Objects.requireNonNull(firebaseAuth.getUid()))
                        .updateChildren(hashMap)
                        .addOnSuccessListener(unused -> {
                            progressDialog.dismiss();
                            dialog("Cập nhật thông tin", "Cập nhật thành công");
                        }).addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            dialog("Cập nhật thông tin", "Cập nhật thất bại, lý do: " + e.getMessage());
                        });
            }

        }).addOnFailureListener(e -> {

        });
    }
    private void dialog(String title, String message) {
        new AlertDialog.Builder(getActivity(), R.style.CustomAlertDialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // chọn hình từ camera hoặc thư viện
    private void option() {
        PopupMenu popupMenu = new PopupMenu(getContext(),binding.avatar);
        popupMenu.getMenu().add(Menu.NONE,1,1,"Camera");
        popupMenu.getMenu().add(Menu.NONE,2,2,"Gallery");

        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();
                if (itemId ==1){
                    Log.d(TAG, "onMenuItemClick: Mở camera, check camera");
                    if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.TIRAMISU){
                        requestCameraPemissions.launch(new String[]{Manifest.permission.CAMERA});
                    }else {
                        requestCameraPemissions.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE});
                    }
                }
                else if (itemId==2){
                    Log.d(TAG, "onMenuItemClick: Mở storage, check storage");
                    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
                        pickFromGallery1();
                    }else {
                        requestStoragePemissions.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                }
                return false;
            }
        });
    }

    private ActivityResultLauncher<String[]> requestCameraPemissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String,Boolean>>(){

                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    Log.d(TAG, "onActivityResult: "+result.toString());
                    boolean areAllGranted = true;
                    for (Boolean isGranted: result.values()){
                        areAllGranted = areAllGranted && isGranted;
                    }
                    if (areAllGranted){
                        Log.d(TAG, "onActivityResult: Tất cả quyền camera & storage");
                        pickFromCamera1();
                    }
                    else {
                        Log.d(TAG, "onActivityResult: Tất cả hoặc chỉ có một quyền");
                        Toast.makeText(getContext(), "Quyền camera hoặc storage", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private ActivityResultLauncher<String> requestStoragePemissions = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (isGranted){
                        pickFromGallery1();
                    }
                    else {
                        Toast.makeText(getContext(), "Quyền Storage chưa cấp quyền", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void pickFromGallery1() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLaucher.launch(intent);
    }
    private ActivityResultLauncher<Intent> galleryActivityResultLaucher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK){

                        Intent data = result.getData();
                        image_uri = data.getData();
                        Log.d(TAG, "onActivityResult: Hình ảnh thư viện: "+image_uri);
                        try {
                            Picasso.get().load(image_uri)
                                    .placeholder(R.drawable.shopivhd)
                                    .into(binding.avatar);
                        }catch (Exception e){
                            Log.d(TAG, "onActivityResult: "+e);
                            Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        Toast.makeText(getContext(), "Hủy", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );


    private void pickFromCamera1() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_Image Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp_Image Description");

        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        cameraActivityResultLaucher.launch(intent);

    }
    private ActivityResultLauncher<Intent> cameraActivityResultLaucher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK){
                        Log.d(TAG, "onActivityResult: Hình ảnh: "+image_uri);
                        try {
                            Picasso.get().load(image_uri)
                                    .placeholder(R.drawable.shopivhd)
                                    .into(binding.avatar);
                        }catch (Exception e){
                            Log.d(TAG, "onActivityResult: "+e);
                            Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        Toast.makeText(getContext(), "Hủy", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );
}