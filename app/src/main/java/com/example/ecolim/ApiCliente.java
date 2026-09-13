package com.example.ecolim;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ApiCliente {
    // Dejar vacío para el trabajo local. Completar solo con un servidor autorizado.
    public static final String URL_API = "";

    public static JSONObject preparar(ArrayList<Residuo> filas) throws Exception {
        JSONArray registros = new JSONArray();
        for (Residuo r : filas) {
            JSONObject fila = new JSONObject();
            fila.put("uuid", r.uuid); fila.put("fecha", r.fecha);
            fila.put("trabajador", r.trabajador); fila.put("area", r.area);
            fila.put("tipo_id", r.tipoId); fila.put("peso_kg", r.peso);
            fila.put("volumen_l", r.volumen); fila.put("observacion", r.observacion);
            fila.put("creado", r.creado);
            registros.put(fila);
        }
        JSONObject cuerpo = new JSONObject();
        cuerpo.put("registros", registros);
        return cuerpo;
    }

    public static ArrayList<String> enviar(ArrayList<Residuo> filas) throws Exception {
        if (URL_API.isEmpty()) throw new Exception("No hay una API configurada");
        URL url = new URL(URL_API);
        if (!url.getProtocol().equals("https")) throw new Exception("La API debe usar HTTPS");
        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
        try {
            conexion.setRequestMethod("POST");
            conexion.setConnectTimeout(10000); conexion.setReadTimeout(10000);
            conexion.setInstanceFollowRedirects(false);
            conexion.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conexion.setRequestProperty("Accept", "application/json");
            conexion.setDoOutput(true);
            byte[] datos = preparar(filas).toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream salida = conexion.getOutputStream()) { salida.write(datos); }
            int codigo = conexion.getResponseCode();
            if (codigo < 200 || codigo >= 300) throw new Exception("Respuesta HTTP " + codigo);
            ByteArrayOutputStream texto = new ByteArrayOutputStream();
            try (InputStream entrada = conexion.getInputStream()) {
                byte[] bloque = new byte[2048]; int cantidad;
                while ((cantidad=entrada.read(bloque)) != -1) {
                    if (texto.size()+cantidad>1000000) throw new Exception("Respuesta demasiado grande");
                    texto.write(bloque,0,cantidad);
                }
            }
            JSONObject respuesta = new JSONObject(new String(texto.toByteArray(),StandardCharsets.UTF_8));
            JSONArray confirmados = respuesta.getJSONArray("confirmados");
            ArrayList<String> enviados = new ArrayList<>();
            for (int i=0;i<confirmados.length();i++) {
                String uuid = confirmados.getString(i);
                // Solo se aceptan confirmaciones de registros del envío actual.
                for (Residuo r:filas) {
                    if (r.uuid.equals(uuid) && !enviados.contains(uuid)) enviados.add(uuid);
                }
            }
            return enviados;
        } finally { conexion.disconnect(); }
    }
}
