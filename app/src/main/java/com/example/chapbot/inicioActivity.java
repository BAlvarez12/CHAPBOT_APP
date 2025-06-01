package com.example.chapbot;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.chapbot.ui.CarouselAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class inicioActivity extends AppCompatActivity {

    private static final String TAG = "inicioActivity";

    private ImageView btnSettings;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ViewPager2 viewPagerCarousel;
    private CarouselAdapter carouselAdapter;
    private List<String> listaImagenesBase64 = new ArrayList<>();
    private final Handler handler = new Handler();
    private Runnable runnableAutoScroll;
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnSettings = findViewById(R.id.btn_settings);
        btnSettings.bringToFront();
        btnSettings.setZ(100f);
        btnSettings.setOnClickListener(v -> showPopupMenu(v));

        viewPagerCarousel = findViewById(R.id.viewPagerCarousel);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ChatsFragmento())
                        .commit();
            }
            cargarImagenesDelCarrusel();
        } else {
            startActivity(new Intent(inicioActivity.this, Login.class));
            finish();
        }
    }

    private void cargarImagenesDelCarrusel() {
        db.collection("usuario_menu")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        listaImagenesBase64.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            if (doc.exists()) {
                                String b64 = doc.getString("imagenBase64");
                                if (b64 != null && !b64.isEmpty()) {
                                    listaImagenesBase64.add(b64);
                                }
                            }
                        }
                        if (listaImagenesBase64.isEmpty()) {
                            Log.w(TAG, "No se encontraron imágenes en usuario_menu");
                            return;
                        }
                        carouselAdapter = new CarouselAdapter(listaImagenesBase64);
                        viewPagerCarousel.setAdapter(carouselAdapter);
                        iniciarAutoScroll();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error al cargar documentos de usuario_menu: " + e.getMessage());
                    }
                });
    }

    private void iniciarAutoScroll() {
        final int delayMillis = 2000;
        runnableAutoScroll = new Runnable() {
            @Override
            public void run() {
                int itemCount = carouselAdapter.getItemCount();
                if (itemCount == 0) return;
                currentPage = (currentPage + 1) % itemCount;
                viewPagerCarousel.setCurrentItem(currentPage, true);
                handler.postDelayed(this, delayMillis);
            }
        };
        handler.postDelayed(runnableAutoScroll, delayMillis);
        viewPagerCarousel.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPage = position;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(runnableAutoScroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (listaImagenesBase64 != null && !listaImagenesBase64.isEmpty()) {
            handler.postDelayed(runnableAutoScroll, 2000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void showPopupMenu(View anchor) {
        try {
            PopupMenu popupMenu = new PopupMenu(inicioActivity.this, anchor);
            popupMenu.getMenuInflater().inflate(R.menu.menu_settings, popupMenu.getMenu());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                popupMenu.setForceShowIcon(true);
            } else {
                try {
                    Field[] fields = popupMenu.getClass().getDeclaredFields();
                    for (Field field : fields) {
                        if ("mPopup".equals(field.getName())) {
                            field.setAccessible(true);
                            Object menuPopupHelper = field.get(popupMenu);
                            Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                            Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                            setForceIcons.invoke(menuPopupHelper, true);
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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
                    startActivity(new Intent(inicioActivity.this, Login.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                    return true;
                }
                return false;
            });
            popupMenu.show();
        } catch (Exception ex) {
            Toast.makeText(this, "Error al mostrar menú: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}
