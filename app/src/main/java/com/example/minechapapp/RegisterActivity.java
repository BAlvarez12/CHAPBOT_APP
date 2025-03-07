package com.example.minechapapp;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText etNombre, etEmail, etPassword;
    private Button btnRegister;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerUser();
            }
        });
    }


    private void registerUser() {
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (nombre.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {

                            String uid = user.getUid();
                            mDatabase.child("usuarios").child(uid).child("nombre").setValue(nombre)
                                    .addOnCompleteListener(dbTask -> {
                                        if (dbTask.isSuccessful()) {

                                            Toast.makeText(RegisterActivity.this,
                                                    "Registro exitoso",
                                                    Toast.LENGTH_SHORT).show();
                                            Log.d(TAG, "Nombre guardado en la DB con key: " + uid);

                                        } else {
                                            Toast.makeText(RegisterActivity.this,
                                                    "Error guardando nombre: " + dbTask.getException().getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                            Log.e(TAG, "Error al escribir en DB", dbTask.getException());
                                        }
                                    });
                        }
                    } else {
                        String errorMsg = (task.getException() != null) ?
                                task.getException().getMessage() : "Error desconocido";
                        Toast.makeText(RegisterActivity.this,
                                "Error al registrar: " + errorMsg,
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error al crear usuario", task.getException());
                    }
                });
    }
}
