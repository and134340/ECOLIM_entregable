package com.example.ecolim;

import android.app.DatePickerDialog;
import android.widget.EditText;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Ayuda {
    public static String hoy() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
    }
    public static void elegirFecha(EditText campo) {
        Calendar fecha = Calendar.getInstance();
        String[] partes = campo.getText().toString().split("-");
        if (partes.length == 3) {
            fecha.set(Integer.parseInt(partes[0]), Integer.parseInt(partes[1])-1, Integer.parseInt(partes[2]));
        }
        DatePickerDialog dialogo = new DatePickerDialog(campo.getContext(), (vista, ano, mes, dia) ->
                campo.setText(String.format(Locale.US, "%04d-%02d-%02d", ano, mes+1, dia)),
                fecha.get(Calendar.YEAR), fecha.get(Calendar.MONTH), fecha.get(Calendar.DAY_OF_MONTH));
        dialogo.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialogo.show();
    }
    public static double numero(EditText campo) {
        double n = Double.parseDouble(campo.getText().toString().trim().replace(',', '.'));
        if (Double.isNaN(n) || Double.isInfinite(n)) throw new NumberFormatException();
        return n;
    }
    public static String cantidad(double n) { return String.format(Locale.US, "%.2f", n); }
}
