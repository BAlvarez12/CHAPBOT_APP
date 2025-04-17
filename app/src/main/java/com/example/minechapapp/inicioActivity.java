package com.example.minechapapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ChatsFragmento())
                    .commit();
        } else {
            Intent intent = new Intent(inicioActivity.this, Login.class);
            startActivity(intent);
            finish();
        }
    }
    private void showPopupMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(inicioActivity.this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.menu_settings, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_item_perfil) {
                Toast.makeText(inicioActivity.this, "Perfil seleccionado", Toast.LENGTH_SHORT).show();
                try {
                    Intent intent = new Intent(inicioActivity.this, EditPerfil.class);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                } catch (Exception e) {
                    Toast.makeText(inicioActivity.this, "Error al abrir perfil: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                return true;
            } else if (itemId == R.id.menu_item_logout) {
                mAuth.signOut();
                Toast.makeText(inicioActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(inicioActivity.this, Login.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                return true;
            } else if (itemId == R.id.menu_item_chat_grupal) {
                Intent intent = new Intent(inicioActivity.this, GrupoChatActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}
