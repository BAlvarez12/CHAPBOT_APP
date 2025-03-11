package com.example.minechapapp;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class chatActivity extends AppCompatActivity {

    private TextView tvNombreUsuario;
    private RecyclerView recyclerMensajes;
    private EditText editMensaje;
    private ImageButton btnEnviar;

    private ArrayList<String> listaMensajes = new ArrayList<>();
    private mensajeAdap mensajeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        recyclerMensajes = findViewById(R.id.recyclerMensajes);
        editMensaje = findViewById(R.id.editMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);

        String nombreUsuario = getIntent().getStringExtra("nombreUsuario");
        tvNombreUsuario.setText(nombreUsuario);

        recyclerMensajes.setLayoutManager(new LinearLayoutManager(this));
        mensajeAdapter = new mensajeAdap(listaMensajes);
        recyclerMensajes.setAdapter(mensajeAdapter);

        btnEnviar.setOnClickListener(v -> {
            String mensaje = editMensaje.getText().toString().trim();

            if (!mensaje.isEmpty()) {
                listaMensajes.add(mensaje);
                mensajeAdapter.notifyItemInserted(listaMensajes.size() - 1);
                recyclerMensajes.scrollToPosition(listaMensajes.size() - 1);

                editMensaje.setText("");
            } else {
                Toast.makeText(this, "Escribe un mensaje primero", Toast.LENGTH_SHORT).show();
            }
        });
        }
}
