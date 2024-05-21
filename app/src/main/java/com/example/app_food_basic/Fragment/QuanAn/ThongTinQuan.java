package com.example.app_food_basic.Fragment.QuanAn;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.app_food_basic.Activity.DangNhap;
import com.example.app_food_basic.R;
import com.example.app_food_basic.databinding.FragmentThongTinQuanBinding;
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
import java.util.Objects;

public class ThongTinQuan extends Fragment {
    private FragmentThongTinQuanBinding binding;
    private ProgressDialog progressDialog;

    private Uri image_uri;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference reference;
    public ThongTinQuan() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentThongTinQuanBinding.inflate(inflater, container, false);
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
                        String tenQuan = "" + snapshot.child("tenQuan").getValue();
                        String sdt = "" + snapshot.child("sdt").getValue();
                        String tinhTP = "" + snapshot.child("tinhTP").getValue();
                        String quanHuyen = "" + snapshot.child("quanHuyen").getValue();
                        String diaChi = "" + snapshot.child("diaChi").getValue();
                        String avatar = "" + snapshot.child("avatar").getValue();
                        // gán vào view
                        binding.tenQuan.setText(tenQuan);
                        binding.sdt.setText(sdt);
                        binding.tinhTP.setText(tinhTP);
                        binding.quanHuyen.setText(quanHuyen);
                        binding.diaChi.setText(diaChi);
                        try {
                            Picasso.get().load(avatar).fit().centerCrop()
                                    .placeholder(R.drawable.shopivhd)
                                    .error(R.drawable.shopivhd)
                                    .into(binding.avatar);
                        } catch (Exception e) {
                            binding.avatar.setImageResource(R.drawable.shopivhd);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    String tenQuan, sdt, tinhTP, quanHuyen, diaChi;
    private void capNhatTT() {
        tenQuan = binding.tenQuan.getText().toString().trim();
        sdt = binding.sdt.getText().toString().trim();
        tinhTP = binding.tinhTP.getText().toString().trim();
        quanHuyen = binding.quanHuyen.getText().toString().trim();
        diaChi = binding.diaChi.getText().toString().trim();
        if (tenQuan.isEmpty() || sdt.isEmpty() || tinhTP.isEmpty() || quanHuyen.isEmpty() || diaChi.isEmpty()) {
            dialog("Cập nhật thất bại", "Thông tin không được bỏ trống");
            return;
        }
        if (sdt.length() < 10) {
            dialog("Cập nhật thất bại", "Số điện thoại phải dài hơn hoặc bằng 10 ký tự");
            return;
        }
        xacNhan();
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
                hashMap.put("tenQuan", "" + tenQuan);
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
    private void xacNhan() {
        // cập nhật thông tin khi không có avatar
        if (image_uri == null) {
            progressDialog.show();
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("tenQuan", "" + tenQuan);
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
    private void dialog(String title, String message) {
        new AlertDialog.Builder(getActivity(), R.style.CustomAlertDialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
    // chọn hình từ camera hoặc thư viện
    private void option() {

    }
    // kiểm tra người dùng có cấp quyền truy cập camera hay chưa

}