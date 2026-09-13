package com.example.ecolim;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Evita que el contenido quede debajo de las barras del teléfono.
        View raiz = findViewById(R.id.raiz);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (vista, ventana) -> {
            Insets barras = ventana.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            vista.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return ventana;
        });
        findViewById(R.id.btnRegistrar).setOnClickListener(v -> abrir(new RegistroFragment()));
        findViewById(R.id.btnRegistros).setOnClickListener(v -> abrir(new ListaFragment()));
        findViewById(R.id.btnReportes).setOnClickListener(v -> abrir(new ReportesFragment()));
        if (savedInstanceState == null) abrir(new RegistroFragment());
    }
    private void abrir(Fragment pantalla) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contenedor, pantalla).commit();
    }
}
