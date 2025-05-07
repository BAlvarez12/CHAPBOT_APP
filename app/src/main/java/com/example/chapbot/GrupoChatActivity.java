package com.example.chapbot;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GrupoChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UsuariosAdap usuarioAdapter;
    private List<Usuario> listaUsuarios;
    private Button btnContinuar;
    private FirebaseFirestore db;

    private final Set<String> usuariosSeleccionados = new HashSet<>();
    private static final int MAX_PARTICIPANTES = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grupo_chat);

        recyclerView = findViewById(R.id.recyclerViewUsuarios);
        btnContinuar = findViewById(R.id.btnContinuar);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        listaUsuarios = new ArrayList<>();

        usuarioAdapter = new UsuariosAdap(listaUsuarios, usuario -> {
            String uid = usuario.getUid();

            if (usuariosSeleccionados.contains(uid)) {
                usuariosSeleccionados.remove(uid);
                usuario.setSeleccionado(false);
            } else {
                if (usuariosSeleccionados.size() < MAX_PARTICIPANTES) {
                    usuariosSeleccionados.add(uid);
                    usuario.setSeleccionado(true);
                } else {
                    Toast.makeText(GrupoChatActivity.this, "Máximo " + MAX_PARTICIPANTES + " participantes permitidos", Toast.LENGTH_SHORT).show();
                }
            }

            usuarioAdapter.notifyDataSetChanged();
        });

        recyclerView.setAdapter(usuarioAdapter);

        db = FirebaseFirestore.getInstance();
        cargarUsuarios();

        btnContinuar.setOnClickListener(v -> {
            if (usuariosSeleccionados.isEmpty()) {
                Toast.makeText(GrupoChatActivity.this, "Selecciona al menos un usuario", Toast.LENGTH_SHORT).show();
            } else {
                Log.d("GrupoChatActivity", "Participantes seleccionados: " + usuariosSeleccionados);

                Intent intent = new Intent(GrupoChatActivity.this, NombreGrupoActivity.class);
                intent.putStringArrayListExtra("participantes", new ArrayList<>(usuariosSeleccionados));
                startActivity(intent);
                finish();
            }
        });
    }

    private void cargarUsuarios() {
        db.collection("usuarios")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaUsuarios.clear();
                    for (var doc : queryDocumentSnapshots) {
                        String uid = doc.getId();
                        String nombre = doc.getString("nombre");
                        String email = doc.getString("email");
                        listaUsuarios.add(new Usuario(uid, nombre, email));
                    }
                    usuarioAdapter.notifyDataSetChanged();
                    Log.d("GrupoChatActivity", "Usuarios cargados: " + listaUsuarios.size());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(GrupoChatActivity.this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show();
                    Log.e("GrupoChatActivity", "Error al cargar usuarios", e);
                });
    }
}
