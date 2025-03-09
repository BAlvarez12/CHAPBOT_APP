package com.example.minechapapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            String uid = user.getUid();
            Log.d(TAG, "Usuario autenticado con UID: " + uid);
            verificarUsuarioEnFirestore(uid);
        } else {
            Log.e(TAG, "No hay usuario autenticado. Redirigiendo a RegisterActivity...");
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
            finish();
        }
    }

    private void verificarUsuarioEnFirestore(String uid) {
        Log.d(TAG, "Verificando usuario en Firestore...");

        db.collection("usuarios").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Usuario encontrado en Firestore. Redirigiendo a Activity_Inicio...");
                        startActivity(new Intent(MainActivity.this, inicioActivity.class));
                        finish();
                    } else {
                        Log.e(TAG, "Usuario no encontrado en Firestore. Redirigiendo a RegisterActivity...");
                        startActivity(new Intent(MainActivity.this, RegisterActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al consultar Firestore", e);
                    Toast.makeText(MainActivity.this, "Error al obtener datos", Toast.LENGTH_SHORT).show();
                });
    }
}
