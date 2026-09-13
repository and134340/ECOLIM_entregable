package com.example.ecolim;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;

public class ReportesFragment extends Fragment {
    private EditText desde, hasta, minimo, maximo;
    private Spinner tipo;
    private TextView resultado;
    private String reporte = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle estado) {
        View v = inflater.inflate(R.layout.fragment_reportes, container, false);
        desde = v.findViewById(R.id.txtDesde); hasta = v.findViewById(R.id.txtHasta);
        minimo = v.findViewById(R.id.txtMinimo); maximo = v.findViewById(R.id.txtMaximo);
        tipo = v.findViewById(R.id.spFiltroTipo); resultado = v.findViewById(R.id.txtResultado);
        desde.setText(Ayuda.hoy().substring(0, 7) + "-01"); hasta.setText(Ayuda.hoy());
        desde.setOnClickListener(b -> Ayuda.elegirFecha(desde));
        hasta.setOnClickListener(b -> Ayuda.elegirFecha(hasta));
        String[] opciones = new String[BaseDatos.TIPOS.length+1];
        opciones[0] = "Todos";
        for (int i=0; i<BaseDatos.TIPOS.length; i++) opciones[i+1]=BaseDatos.TIPOS[i];
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, opciones);
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tipo.setAdapter(adaptador);
        v.findViewById(R.id.btnGenerar).setOnClickListener(b -> generar());
        v.findViewById(R.id.btnCompartir).setOnClickListener(b -> {
            // Se vuelve a generar para no compartir filtros anteriores.
            if (generar()) {
                Intent envio = new Intent(Intent.ACTION_SEND);
                envio.setType("text/plain");
                envio.putExtra(Intent.EXTRA_SUBJECT, "Reporte ECOLIM");
                envio.putExtra(Intent.EXTRA_TEXT, reporte);
                startActivity(Intent.createChooser(envio, "Compartir reporte"));
            }
        });
        return v;
    }

    @Override
    public void onViewStateRestored(Bundle estado) {
        super.onViewStateRestored(estado);
        generar();
    }

    private boolean generar() {
        String inicio = desde.getText().toString(), fin = hasta.getText().toString();
        if (inicio.compareTo(fin)>0) return error("La fecha inicial no puede ser posterior a la final");
        Double menor=null, mayor=null;
        try {
            if (!minimo.getText().toString().trim().isEmpty()) menor=Ayuda.numero(minimo);
            if (!maximo.getText().toString().trim().isEmpty()) mayor=Ayuda.numero(maximo);
        } catch (NumberFormatException e) { return error("Revisa los límites de volumen"); }
        if ((menor!=null && menor<0) || (mayor!=null && mayor<0)) return error("El volumen no puede ser negativo");
        if (menor!=null && mayor!=null && menor>mayor) return error("El mínimo no puede superar al máximo");
        ArrayList<Residuo> filas;
        try (BaseDatos db = new BaseDatos(requireContext())) {
            filas = db.consultar(inicio, fin, tipo.getSelectedItemPosition(), menor, mayor, false);
        } catch (Exception e) { return error("No se pudo consultar la base de datos"); }
        double pesoTotal=0, volumenTotal=0;
        double[] pesos = new double[BaseDatos.TIPOS.length];
        double[] volumenes = new double[BaseDatos.TIPOS.length];
        int[] cantidades = new int[BaseDatos.TIPOS.length];
        StringBuilder detalle=new StringBuilder();
        for (Residuo r:filas) {
            pesoTotal+=r.peso; volumenTotal+=r.volumen;
            pesos[r.tipoId-1]+=r.peso; volumenes[r.tipoId-1]+=r.volumen; cantidades[r.tipoId-1]++;
            detalle.append("\n#").append(r.id).append(" | ").append(r.fecha).append(" | ").append(r.tipo)
                .append("\nTrabajador: ").append(r.trabajador).append(" · Área: ").append(r.area)
                .append("\n").append(Ayuda.cantidad(r.peso)).append(" kg · ")
                .append(Ayuda.cantidad(r.volumen)).append(" L\n");
        }
        StringBuilder texto=new StringBuilder("REPORTE ECOLIM\n");
        texto.append("Desde: ").append(inicio).append(" Hasta: ").append(fin)
            .append("\nTipo: ").append(tipo.getSelectedItem())
            .append("\nVolumen por registro: ").append(menor==null?"sin mínimo":Ayuda.cantidad(menor)+" L")
            .append(" / ").append(mayor==null?"sin máximo":Ayuda.cantidad(mayor)+" L")
            .append("\nRegistros: ").append(filas.size())
            .append("\nPeso total: ").append(Ayuda.cantidad(pesoTotal)).append(" kg")
            .append("\nVolumen total: ").append(Ayuda.cantidad(volumenTotal)).append(" L\n");
        for (int i=0;i<cantidades.length;i++) {
            if (cantidades[i]>0) texto.append("\n").append(BaseDatos.TIPOS[i]).append(": ")
                .append(cantidades[i]).append(" registros · ").append(Ayuda.cantidad(pesos[i]))
                .append(" kg · ").append(Ayuda.cantidad(volumenes[i])).append(" L");
        }
        if (filas.isEmpty()) texto.append("\nNo hay registros con estos filtros.");
        else texto.append("\n\nDETALLE\n").append(detalle);
        texto.append("\nReporte interno. Revisar antes de usar en una declaración oficial.");
        reporte=texto.toString(); resultado.setText(reporte); return true;
    }
    private boolean error(String texto) {
        reporte=""; resultado.setText(texto);
        Toast.makeText(requireContext(),texto,Toast.LENGTH_LONG).show(); return false;
    }
}
