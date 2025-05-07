package com.example.chapbot;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class Login extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private FrameLayout loadingOverlay;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (mAuth.getCurrentUser() != null) {
            goToInicioActivity();
            return;
        }
        setContentView(R.layout.activity_login);
        initUI();
        setListeners();
    }
    private void initUI() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        loadingOverlay = findViewById(R.id.loadingOverlay);
    }
    private void setListeners() {
        btnLogin.setOnClickListener(v -> {
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.dimencion_escala);
            v.startAnimation(anim);
            v.postDelayed(this::iniciarSesion, 150);
        });
        btnRegister.setOnClickListener(v -> {
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.dimencion_escala);
            v.startAnimation(anim);
            v.postDelayed(() -> {
                Intent intent = new Intent(Login.this, RegisterActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }, 150);
        });

    }
    private void iniciarSesion() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        btnLogin.setEnabled(false);
        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnLogin.setEnabled(true);
                    showLoading(false);

                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

                        if (uid != null) {
                            consultarFirestore(uid);
                        } else {
                            Toast.makeText(this, "Error al obtener UID del usuario", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
    private void consultarFirestore(String uid) {
        showLoading(true);

        db.collection("usuarios").document(uid).get()
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();

                        if (document.exists()) {
                            String nombre = document.getString("nombre");
                            Toast.makeText(this, "Bienvenido, " + nombre, Toast.LENGTH_SHORT).show();
                            actualizarTokenFCM(uid);
                            goToInicioActivity();
                        } else {
                            mAuth.signOut();
                            Toast.makeText(this, "Usuario no registrado en Firestore", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Error al consultar Firestore: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
    private void goToInicioActivity() {
        Intent intent = new Intent(Login.this, inicioActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void actualizarTokenFCM(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    db.collection("usuarios").document(userId).get()
                            .addOnSuccessListener(document -> {
                                String tokenGuardado = document.getString("token");

                                if (tokenGuardado == null || !tokenGuardado.equals(token)) {
                                    db.collection("usuarios").document(userId)
                                            .update("token", token)
                                            .addOnSuccessListener(aVoid -> Log.d("FCM_TOKEN", "Token actualizado"))
                                            .addOnFailureListener(e -> Log.e("FCM_TOKEN", "Error al actualizar token", e));
                                } else {
                                    Log.d("FCM_TOKEN", "Token ya estaba actualizado");
                                }
                            });
                });
    }
    private void guardarTokenEnFirestore(String userId, String token) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("usuarios").document(userId)
                .update("token", token)
                .addOnSuccessListener(aVoid -> Log.d("TOKEN", "Token guardado correctamente"))
                .addOnFailureListener(e -> Log.e("TOKEN", "Error al guardar token", e));
    }



}
