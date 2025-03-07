package com.example.minechapapp;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";  // 💡 Este TAG servirá para filtrar en Logcat
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🔥 Verificar si Firebase está inicializado
        FirebaseApp.initializeApp(this);
        if (FirebaseApp.getApps(this).isEmpty()) {
            Log.e(TAG, "❌ Firebase NO se inicializó correctamente.");
        } else {
            Log.d(TAG, "✅ Firebase se inicializó correctamente.");
        }

        // 🔥 Conectar con Realtime Database
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://minechap-7166e-default-rtdb.firebaseio.com/");
        databaseReference = database.getReference("pruebaConexion");

        // Escribir y leer datos de Firebase
        escribirEnFirebase();
        leerDesdeFirebase();
    }

    private void escribirEnFirebase() {
        Log.d(TAG, "✏️ Intentando escribir en la base de datos...");
        databaseReference.setValue("Conexión exitosa!")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Datos escritos correctamente en Firebase.");
                    } else {
                        Log.e(TAG, "❌ Error al escribir en Firebase", task.getException());
                    }
                });
    }

    private void leerDesdeFirebase() {
        Log.d(TAG, "📖 Intentando leer desde Firebase...");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String mensaje = dataSnapshot.getValue(String.class);
                    Log.d(TAG, "📌 Mensaje recibido desde Firebase: " + mensaje);
                } else {
                    Log.e(TAG, "⚠️ No hay datos en esta referencia.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "❌ Error al leer datos desde Firebase", databaseError.toException());
            }
        });
    }
}
