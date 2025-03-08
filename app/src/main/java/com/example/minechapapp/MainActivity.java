package com.example.minechapapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
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
            obtenerDatosUsuario(uid);
        } else {
            Log.e(TAG, "No hay usuario autenticado. Redirigiendo a RegisterActivity...");
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
            finish();
        }
    }

    private void obtenerDatosUsuario(String uid) {
        Log.d(TAG, "Intentando obtener datos del usuario en Firestore...");

        db.collection("usuarios").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nombre = documentSnapshot.getString("nombre");
                        String email = documentSnapshot.getString("email");
                        Log.d(TAG, "Datos del usuario: Nombre: " + nombre + ", Email: " + email);
                        Toast.makeText(MainActivity.this, "Bienvenido, " + nombre, Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "No se encontraron datos para este usuario en Firestore.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener datos del usuario desde Firestore", e);
                });
    }
}
