package com.example.ecolim;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.fragment.app.Fragment;

public class RegistroFragment extends Fragment {
    private EditText trabajador, area, fecha, peso, volumen, observacion;
    private Spinner tipo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle estado) {
        View vista = inflater.inflate(R.layout.fragment_registro, container, false);
        trabajador = vista.findViewById(R.id.txtTrabajador);
        area = vista.findViewById(R.id.txtArea);
        fecha = vista.findViewById(R.id.txtFecha);
        peso = vista.findViewById(R.id.txtPeso);
        volumen = vista.findViewById(R.id.txtVolumen);
        observacion = vista.findViewById(R.id.txtObservacion);
        tipo = vista.findViewById(R.id.spTipo);
        String[] opciones = new String[BaseDatos.TIPOS.length + 1];
        opciones[0] = "Selecciona el tipo";
        for (int i = 0; i < BaseDatos.TIPOS.length; i++) opciones[i+1] = BaseDatos.TIPOS[i];
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, opciones);
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tipo.setAdapter(adaptador);
        fecha.setText(Ayuda.hoy());
        fecha.setOnClickListener(v -> Ayuda.elegirFecha(fecha));
        vista.findViewById(R.id.btnGuardar).setOnClickListener(v -> guardar());
        return vista;
    }

    private void guardar() {
        String nombre = trabajador.getText().toString().trim();
        String lugar = area.getText().toString().trim();
        if (nombre.isEmpty()) { trabajador.setError("Ingresa el trabajador"); return; }
        if (lugar.isEmpty()) { area.setError("Ingresa el área"); return; }
        if (tipo.getSelectedItemPosition() == 0) { mensaje("Selecciona un tipo de residuo"); return; }
        double kilos, litros;
        try { kilos = Ayuda.numero(peso); }
        catch (NumberFormatException e) { peso.setError("Ingresa un peso válido"); return; }
        try { litros = Ayuda.numero(volumen); }
        catch (NumberFormatException e) { volumen.setError("Ingresa un volumen válido"); return; }
        if (kilos <= 0 || kilos > 100000) { peso.setError("El peso debe ser mayor que 0 y hasta 100000 kg"); return; }
        if (litros <= 0 || litros > 1000000) { volumen.setError("El volumen debe ser mayor que 0 y hasta 1000000 L"); return; }
        try (BaseDatos db = new BaseDatos(requireContext())) {
            db.guardar(fecha.getText().toString(), nombre, lugar, tipo.getSelectedItemPosition(),
                    kilos, litros, observacion.getText().toString().trim());
            mensaje("Registro guardado en el teléfono");
            peso.setText(""); volumen.setText(""); observacion.setText(""); tipo.setSelection(0);
        } catch (Exception e) { mensaje("No se pudo guardar. Revisa el espacio del teléfono."); }
    }
    private void mensaje(String texto) { Toast.makeText(requireContext(), texto, Toast.LENGTH_LONG).show(); }
}
