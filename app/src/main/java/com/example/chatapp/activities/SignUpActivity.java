package com.example.chatapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.databinding.ActivitySignUpBinding;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;
    private String encodedImageSTR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
    }

    private void setListeners() {
        binding.textSignIn.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.buttonSignUp.setOnClickListener(v ->{
            if (isValidSignUpDetails()){
                signUp();
            }
        });
        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
                }
        );
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void signUp(){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_NAME, binding.inputName.getText().toString());
        user.put(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
        user.put(Constants.KEY_PASSWORD, binding.inputPassword.getText().toString());
        user.put(Constants.KEY_IMAGE, encodedImageSTR);
        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReferences ->{
            loading(false);
            preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
            preferenceManager.putString(Constants.KEY_USER_ID, documentReferences.getId());
            preferenceManager.putString(Constants.KEY_NAME, binding.inputName.getText().toString());
            Log.d("CHECKIM_2", encodedImageSTR);
            preferenceManager.putString(Constants.KEY_IMAGE, encodedImageSTR);
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }).addOnFailureListener(exception -> {
            loading(false);
            showToast(exception.getMessage());
        });
    }

    private String encodedImage(Bitmap bitmap){
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK){
                    if (result.getData() != null){
                        Uri imageUri = result.getData().getData();
                        try{
//                            assert imageUri != null;
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            binding.textAddImage.setVisibility(View.GONE);
                            encodedImageSTR = encodedImage(bitmap);
                            Log.d("CHECKIM_1", encodedImageSTR);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private Boolean isValidSignUpDetails(){
        if (this.encodedImageSTR == null){
            showToast("Выберите изображение");
            return false;
        } else if (binding.inputName.getText().toString().trim().isEmpty()){
            showToast("Введите имя");
            return false;
        } else if (binding.inputEmail.getText().toString().trim().isEmpty()){
            showToast("Ввдеите эл. почту");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()){
            showToast("Введите корректную почту");
            return false;
        } else if (binding.inputPassword.getText().toString().trim().isEmpty()){
            showToast("Введите пароль");
            return false;
        } else if (binding.inputConfirmPassword.getText().toString().trim().isEmpty()){
            showToast("Подтвердите пароль");
            return false;
        } else if (!binding.inputPassword.getText().toString().equals(binding.inputConfirmPassword.getText().toString())){
            showToast("Пароли должны быть одинаковыми");
            return false;
        } else {
            return true;
        }

    }

    private void loading(Boolean isLoading){
        if (isLoading){
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignUp.setVisibility(View.VISIBLE);
        }
    }

}