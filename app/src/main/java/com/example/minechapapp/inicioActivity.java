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

        btnSettings.setOnClickListener(view -> showPopupMenu(view));
        btnAdd.setOnClickListener(view -> mostrarMenuAdd(view));

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
                startActivity(new Intent(inicioActivity.this, Login.class));
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
                startActivity(new Intent(inicioActivity.this, UsuariosActivos.class));
                return true;
            }
            return false;
        });
        popupMenu.show();
    }
}
