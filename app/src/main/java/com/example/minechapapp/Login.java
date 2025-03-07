package com.example.minechapapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class Login extends AppCompatActivity {

    private FirebaseAuth mAuth; // Instancia de FirebaseAuth
    private EditText etEmail, etPassword; // Campos de correo y contraseña
    private Button btnLogin; // Botón de iniciar sesión

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // referencias a los campos y botones
        etEmail = findViewById(R.id.etEmail); // Asegúrate de que el ID coincida con tu XML
        etPassword = findViewById(R.id.etPassword); // Asegúrate de que el ID coincida con tu XML
        btnLogin = findViewById(R.id.btnLogin); // Asegúrate de que el ID coincida con tu XML

        // Configurar el clic en "Iniciar Sesión"
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Obtener el correo y la contraseña ingresados
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                // Validar que los campos no estén vacíos
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(Login.this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Iniciar sesión con Firebase
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Inicio de sesión exitoso
                                    Toast.makeText(Login.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();

                                    // Redirigir a la actividad principal (MainActivity)
                                    Intent intent = new Intent(Login.this, MainActivity.class);
                                    startActivity(intent);
                                    finish(); // Cerrar la actividad actual
                                } else {
                                    // Si el inicio de sesión falla, mostrar un mensaje de error
                                    Toast.makeText(Login.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        // Obtener referencia al Button de "Registrarse"
        Button btnRegister = findViewById(R.id.btnRegister);

        // Configurar el clic en "Registrarse"
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Crear un Intent para abrir la actividad Registers
                Intent intent = new Intent(Login.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}