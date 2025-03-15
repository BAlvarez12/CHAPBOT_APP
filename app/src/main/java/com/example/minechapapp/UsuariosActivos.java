package com.example.minechapapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class UsuariosActivos extends AppCompatActivity {

    private static final String TAG = "UsuariosActivos";

    private RecyclerView recyclerView;
    private UsuariosAdap usuarioAdapter;
    private List<Usuario> listaUsuarios;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuarios);

        recyclerView = findViewById(R.id.recyclerViewUsuarios);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        listaUsuarios = new ArrayList<>();
        usuarioAdapter = new UsuariosAdap(listaUsuarios, usuario -> {
            Toast.makeText(UsuariosActivos.this,
                    "Elegiste chatear con: " + usuario.getNombre(),
                    Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(UsuariosActivos.this, chatActivity.class);
            intent.putExtra("USER_ID", usuario.getUid());
            intent.putExtra("USER_NAME", usuario.getNombre());
            startActivity(intent);
        });
        recyclerView.setAdapter(usuarioAdapter);

        db = FirebaseFirestore.getInstance();

        cargarUsuariosDesdeFirestore();
    }

    private void cargarUsuariosDesdeFirestore() {
        db.collection("usuarios")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaUsuarios.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String uid = doc.getId();
                        String nombre = doc.getString("nombre");
                        String email = doc.getString("email");

                        Usuario usuario = new Usuario(uid, nombre, email);
                        listaUsuarios.add(usuario);
                    }
                    usuarioAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar usuarios: ", e);
                    Toast.makeText(UsuariosActivos.this,
                            "Error al cargar usuarios", Toast.LENGTH_SHORT).show();
                });
    }
}
