package com.example.minechapapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class inicioActivity extends AppCompatActivity {

    private ImageView btnSettings;
    private ImageView btnAdd;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);

        mAuth = FirebaseAuth.getInstance();

        btnSettings = findViewById(R.id.btn_settings);
        btnAdd = findViewById(R.id.btn_add);

        btnSettings.setOnClickListener(this::showPopupMenu);
        btnAdd.setOnClickListener(this::mostrarMenuAdd);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ChatsFragmento())
                .commit();
    }

    private void showPopupMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(inicioActivity.this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.menu_settings, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu_item_perfil) {
                Toast.makeText(inicioActivity.this, "Perfil seleccionado", Toast.LENGTH_SHORT).show();
                return true;

            } else if (itemId == R.id.menu_item_logout) {
                mAuth.signOut();
                Toast.makeText(inicioActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(inicioActivity.this, Login.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // ✅ Animación al cerrar sesión
                finish();
                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    private void mostrarMenuAdd(View anchor) {
        PopupMenu popupMenu = new PopupMenu(inicioActivity.this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.bottom_mas, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu_item_new_chat) {
                Intent intent = new Intent(inicioActivity.this, UsuariosActivos.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right); // ✅ Animación al abrir actividad
                return true;

            } else if (itemId == R.id.menu_item_chat_grupal) {
                Intent intent = new Intent(inicioActivity.this, GrupoChatActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right); // ✅ Animación al abrir actividad
                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right); // ✅ Animación al regresar
    }
}
