package com.example.ecolim;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListaFragment extends Fragment {
    private ListView lista;
    private ArrayList<Residuo> registros;
    private boolean enviando=false;
    private final ExecutorService trabajo = Executors.newSingleThreadExecutor();
    private final Handler pantalla = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle estado) {
        View v = inflater.inflate(R.layout.fragment_lista,container,false);
        lista=v.findViewById(R.id.listaRegistros);
        v.findViewById(R.id.btnSimular).setOnClickListener(b -> simular());
        v.findViewById(R.id.btnEnviar).setOnClickListener(b -> confirmarEnvio());
        lista.setOnItemClickListener((padre, fila, posicion, id) -> {
            if (registros.isEmpty()) return;
            Residuo r=registros.get(posicion);
            new AlertDialog.Builder(requireContext()).setTitle("Detalle #"+r.id)
                .setMessage(describir(r)+"\nObservación: "+r.observacion+
                    "\nGuardado: "+r.creado+"\nIdentificador: "+r.uuid)
                .setPositiveButton("Cerrar",null).show();
        });
        cargar();
        return v;
    }

    private String describir(Residuo r) {
        return "#"+r.id+" · "+r.fecha+" · "+r.tipo+"\n"+r.trabajador+" · "+r.area+
                "\n"+Ayuda.cantidad(r.peso)+" kg · "+Ayuda.cantidad(r.volumen)+" L\n"+
                (r.sincronizado?"Confirmado por API":"Pendiente de envío");
    }

    private void cargar() {
        try (BaseDatos db = new BaseDatos(requireContext())) {
            registros=db.consultar("","",0,null,null,false);
            ArrayList<String> textos=new ArrayList<>();
            for (Residuo r:registros) textos.add(describir(r));
            if (textos.isEmpty()) textos.add("Todavía no hay registros. Agrega uno en Registrar.");
            lista.setAdapter(new ArrayAdapter<>(requireContext(),android.R.layout.simple_list_item_1,textos));
        }
    }

    private ArrayList<Residuo> pendientes() {
        try (BaseDatos db = new BaseDatos(requireContext())) {
            return db.consultar("","",0,null,null,true);
        }
    }

    private void simular() {
        try {
            ArrayList<Residuo> filas=pendientes();
            if (filas.isEmpty()) { mensaje("No hay registros pendientes"); return; }
            // Muestra uno para que el ejemplo sea fácil de leer.
            ArrayList<Residuo> ejemplo=new ArrayList<>(); ejemplo.add(filas.get(0));
            new AlertDialog.Builder(requireContext()).setTitle("Simulación local, sin envío")
                .setMessage("Pendientes: "+filas.size()+"\nEjemplo del primer registro:\n"+
                    ApiCliente.preparar(ejemplo).toString(2)+
                    "\n\nNo se contactó ningún servidor. Los registros siguen pendientes.")
                .setPositiveButton("Cerrar",null).show();
        } catch (Exception e) { mensaje("No se pudo preparar la simulación"); }
    }

    private void confirmarEnvio() {
        if (ApiCliente.URL_API.isEmpty()) {
            mensaje("No hay servidor configurado. Puedes usar Simular envío a API."); return;
        }
        if (enviando) { mensaje("El envío sigue en proceso"); return; }
        new AlertDialog.Builder(requireContext()).setTitle("Enviar registros")
            .setMessage("Se enviarán los datos pendientes, incluido el nombre del trabajador, a la API configurada.")
            .setNegativeButton("Cancelar",null).setPositiveButton("Enviar",(d,b) -> enviar()).show();
    }

    private void enviar() {
        if (enviando) return;
        ArrayList<Residuo> filas=pendientes();
        if (filas.isEmpty()) { mensaje("No hay registros pendientes"); return; }
        Context contexto=requireContext().getApplicationContext();
        enviando=true; mensaje("Enviando registros...");
        // Internet se ejecuta aparte para que la pantalla no se congele.
        trabajo.execute(() -> {
            String resultado;
            try (BaseDatos db = new BaseDatos(contexto)) {
                ArrayList<String> confirmados=ApiCliente.enviar(filas);
                for (String uuid:confirmados) db.marcarEnviado(uuid);
                resultado="Confirmados: "+confirmados.size()+" de "+filas.size();
            } catch (Exception e) {
                resultado="No se completó el envío. Los registros no confirmados siguen pendientes.";
            }
            String mensajeFinal=resultado;
            pantalla.post(() -> {
                enviando=false;
                if (isAdded() && getView()!=null) { cargar(); mensaje(mensajeFinal); }
            });
        });
    }
    private void mensaje(String texto) { Toast.makeText(requireContext(),texto,Toast.LENGTH_LONG).show(); }
    @Override
    public void onDestroy() { trabajo.shutdown(); super.onDestroy(); }
}
