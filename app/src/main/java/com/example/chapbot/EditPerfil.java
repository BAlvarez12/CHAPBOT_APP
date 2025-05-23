package com.example.chapbot;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EditPerfil extends AppCompatActivity {
    private ImageView imgPerfil;
    private Button btnCambiarFoto, btnGuardar, btnCargarMenu;
    private EditText etNombreUsuario;
    private Switch switchHasMenu;
    private EditText etMenuItem1, etMenuItem2, etMenuItem3;
    private EditText etMenuPrice1, etMenuPrice2, etMenuPrice3;
    private ScrollView scrollView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_MENU_IMAGE   = 2;

    private Uri imageUri;
    private String fotoActualBase64 = "";

    private Uri menuImageUri;
    private String menuImageBase64;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_perfil);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        scrollView      = findViewById(R.id.scrollView);
        imgPerfil       = findViewById(R.id.imgPerfil);
        btnCambiarFoto  = findViewById(R.id.btnCambiarFoto);
        btnGuardar      = findViewById(R.id.btnGuardar);
        etNombreUsuario = findViewById(R.id.etNombreUsuario);

        switchHasMenu   = findViewById(R.id.switchHasMenu);
        btnCargarMenu   = findViewById(R.id.btnCargarMenu);
        etMenuItem1     = findViewById(R.id.etMenuItem1);
        etMenuItem2     = findViewById(R.id.etMenuItem2);
        etMenuItem3     = findViewById(R.id.etMenuItem3);
        etMenuPrice1    = findViewById(R.id.etMenuPrice1);
        etMenuPrice2    = findViewById(R.id.etMenuPrice2);
        etMenuPrice3    = findViewById(R.id.etMenuPrice3);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        cargarDatosUsuario();

        btnCambiarFoto.setOnClickListener(v -> abrirGaleria(PICK_IMAGE_REQUEST));
        btnCargarMenu .setOnClickListener(v -> abrirGaleria(PICK_MENU_IMAGE));
        btnGuardar    .setOnClickListener(v -> guardarCambios());

        switchHasMenu.setOnCheckedChangeListener((btn, on) -> {
            int vis = on ? View.VISIBLE : View.GONE;
            btnCargarMenu.setVisibility(vis);
            etMenuItem1  .setVisibility(vis);
            etMenuItem2  .setVisibility(vis);
            etMenuItem3  .setVisibility(vis);
            etMenuPrice1 .setVisibility(vis);
            etMenuPrice2 .setVisibility(vis);
            etMenuPrice3 .setVisibility(vis);
        });

        View[] campos = new View[]{
                etMenuItem1, etMenuPrice1,
                etMenuItem2, etMenuPrice2,
                etMenuItem3, etMenuPrice3
        };
        for (View campo : campos) {
            campo.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    scrollView.post(() -> scrollView.smoothScrollTo(0, v.getTop()));
                }
            });
        }
    }

    private void cargarDatosUsuario() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    etNombreUsuario.setText(doc.getString("nombre"));
                    fotoActualBase64 = doc.getString("fotoBase64");
                    if (fotoActualBase64 != null && !fotoActualBase64.isEmpty()) {
                        imgPerfil.setImageBitmap(convertirBase64ABitmap(fotoActualBase64));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al cargar perfil", Toast.LENGTH_SHORT).show()
                );

        db.collection("Usuario_estado_menu").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    boolean hasMenu = doc.exists() && doc.getLong("estado") != null && doc.getLong("estado") == 1;
                    switchHasMenu.setChecked(hasMenu);
                    if (hasMenu) {
                        btnCargarMenu.setVisibility(View.VISIBLE);
                        etMenuItem1  .setVisibility(View.VISIBLE);
                        etMenuItem2  .setVisibility(View.VISIBLE);
                        etMenuItem3  .setVisibility(View.VISIBLE);
                        etMenuPrice1 .setVisibility(View.VISIBLE);
                        etMenuPrice2 .setVisibility(View.VISIBLE);
                        etMenuPrice3 .setVisibility(View.VISIBLE);

                        db.collection("usuario_menu").document(userId)
                                .get()
                                .addOnSuccessListener(m -> {
                                    if (!m.exists()) return;
                                    menuImageBase64 = m.getString("imagenBase64");
                                    etMenuItem1 .setText(m.getString("menu1"));
                                    etMenuItem2 .setText(m.getString("menu2"));
                                    etMenuItem3 .setText(m.getString("menu3"));
                                    etMenuPrice1.setText(String.valueOf(m.getDouble("precio1")));
                                    etMenuPrice2.setText(String.valueOf(m.getDouble("precio2")));
                                    etMenuPrice3.setText(String.valueOf(m.getDouble("precio3")));
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error cargando menú", Toast.LENGTH_SHORT).show()
                                );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error cargando estado menú", Toast.LENGTH_SHORT).show()
                );
    }

    private void abrirGaleria(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,@Nullable Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if (resultCode!=RESULT_OK || data==null) return;
        Uri uri = data.getData();
        if (requestCode==PICK_IMAGE_REQUEST) {
            imageUri = uri;
            imgPerfil.setImageURI(uri);
        } else if (requestCode==PICK_MENU_IMAGE) {
            menuImageUri = uri;
            Toast.makeText(this,"Imagen de menú seleccionada",Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarCambios() {
        String userId = mAuth.getCurrentUser().getUid();
        String nuevoNombre = etNombreUsuario.getText().toString().trim();
        if (nuevoNombre.isEmpty()) {
            Toast.makeText(this,"El nombre no puede quedar vacío",Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String,Object> updUsuario = new HashMap<>();
        updUsuario.put("nombre", nuevoNombre);
        if (imageUri!=null) {
            try {
                Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(),imageUri);
                String b64 = convertirBitmapABase64(bmp);
                if (!b64.equals(fotoActualBase64)) {
                    updUsuario.put("fotoBase64", b64);
                }
            } catch (IOException ex){
                Toast.makeText(this,"Error procesando foto",Toast.LENGTH_SHORT).show();
                return;
            }
        }
        db.collection("usuarios").document(userId).update(updUsuario);

        boolean hasMenu = switchHasMenu.isChecked();
        Map<String,Object> estado = new HashMap<>();
        estado.put("estado", hasMenu?1:0);
        db.collection("Usuario_estado_menu").document(userId).set(estado);

        if (hasMenu) {
            if (menuImageUri==null && (menuImageBase64==null||menuImageBase64.isEmpty())) {
                Toast.makeText(this,"Selecciona primero una imagen de menú",Toast.LENGTH_SHORT).show();
                return;
            }
            if (menuImageUri!=null) {
                try {
                    Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(),menuImageUri);
                    menuImageBase64 = convertirBitmapABase64(bmp);
                } catch (IOException ignored){}
            }
            String i1 = etMenuItem1.getText().toString().trim();
            String i2 = etMenuItem2.getText().toString().trim();
            String i3 = etMenuItem3.getText().toString().trim();
            String p1 = etMenuPrice1.getText().toString().trim();
            String p2 = etMenuPrice2.getText().toString().trim();
            String p3 = etMenuPrice3.getText().toString().trim();
            if (i1.isEmpty()||i2.isEmpty()||i3.isEmpty()
                    ||p1.isEmpty()||p2.isEmpty()||p3.isEmpty()) {
                Toast.makeText(this,"Completa todos los ítems y precios",Toast.LENGTH_SHORT).show();
                return;
            }
            double pr1,pr2,pr3;
            try {
                pr1=Double.parseDouble(p1);
                pr2=Double.parseDouble(p2);
                pr3=Double.parseDouble(p3);
            }catch(NumberFormatException ex){
                Toast.makeText(this,"Precios inválidos",Toast.LENGTH_SHORT).show();
                return;
            }
            double total = pr1+pr2+pr3;
            Map<String,Object> menuData = new HashMap<>();
            menuData.put("imagenBase64",menuImageBase64);
            menuData.put("menu1",i1);
            menuData.put("precio1",pr1);
            menuData.put("menu2",i2);
            menuData.put("precio2",pr2);
            menuData.put("menu3",i3);
            menuData.put("precio3",pr3);
            menuData.put("total",total);

            db.collection("usuario_menu").document(userId)
                    .set(menuData)
                    .addOnSuccessListener(a-> {
                        Toast.makeText(this,"¡Perfil y menú guardados!",Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e->
                            Toast.makeText(this,"Error guardando menú",Toast.LENGTH_LONG).show()
                    );
        } else {
            db.collection("usuario_menu").document(userId).delete();
            Toast.makeText(this,"Cambios guardados",Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private Bitmap convertirBase64ABitmap(String b64){
        byte[] dec = Base64.decode(b64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(dec,0,dec.length);
    }
    private String convertirBitmapABase64(Bitmap bmp){
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG,70,baos);
        return Base64.encodeToString(baos.toByteArray(),Base64.DEFAULT);
    }
}
