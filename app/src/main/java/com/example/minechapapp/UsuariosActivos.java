package com.example.minechapapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
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

        // 1) Inicializamos RecyclerView
        recyclerView = findViewById(R.id.recyclerViewUsuarios);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 2) Preparamos la lista vacía y Firestore
        listaUsuarios = new ArrayList<>();
        db = FirebaseFirestore.getInstance();

        // 3) Configuramos el SearchView para que siempre esté desplegado
        SearchView searchView = findViewById(R.id.searchUsuarios);
        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);
        searchView.clearFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false; // no hacemos nada al “submit”
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (usuarioAdapter != null) {
                    usuarioAdapter.getFilter().filter(newText);
                }
                return true;
            }
        });

        // 4) Cargamos los usuarios y solo entonces creamos el adaptador
        cargarUsuariosDesdeFirestore();
    }

    private void cargarUsuariosDesdeFirestore() {
        db.collection("usuarios")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // 4.1) Llenamos la lista de usuarios
                    listaUsuarios.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String uid    = doc.getId();
                        String nombre = doc.getString("nombre");
                        String email  = doc.getString("email");
                        listaUsuarios.add(new Usuario(uid, nombre, email));
                    }

                    // 4.2) Creamos el adaptador con DATOS (no antes)
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

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar usuarios: ", e);
                    Toast.makeText(UsuariosActivos.this,
                            "Error al cargar usuarios", Toast.LENGTH_SHORT).show();
                });
    }
}
