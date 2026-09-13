package com.example.ecolim;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class BaseDatos extends SQLiteOpenHelper {
    // Los ID de este catálogo son los mismos que guarda la tabla tipos.
    public static final String[] TIPOS = {"Papel y cartón", "Plástico", "Vidrio",
            "Metal", "Orgánico", "No aprovechable", "Peligroso"};

    public BaseDatos(Context context) { super(context, "ecolim.db", null, 1); }

    @Override
    public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tipos (id INTEGER PRIMARY KEY, nombre TEXT NOT NULL UNIQUE)");
        db.execSQL("CREATE TABLE recolecciones (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "uuid TEXT NOT NULL UNIQUE, fecha TEXT NOT NULL, trabajador TEXT NOT NULL, "
                + "area TEXT NOT NULL, tipo_id INTEGER NOT NULL, "
                + "peso REAL NOT NULL CHECK(peso > 0), volumen REAL NOT NULL CHECK(volumen > 0), "
                + "observacion TEXT NOT NULL, creado TEXT NOT NULL, "
                + "sincronizado INTEGER NOT NULL DEFAULT 0 CHECK(sincronizado IN (0,1)), "
                + "FOREIGN KEY(tipo_id) REFERENCES tipos(id))");
        db.execSQL("CREATE INDEX indice_fecha ON recolecciones(fecha)");
        for (int i = 0; i < TIPOS.length; i++) {
            ContentValues fila = new ContentValues();
            fila.put("id", i + 1);
            fila.put("nombre", TIPOS[i]);
            db.insertOrThrow("tipos", null, fila);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int anterior, int nueva) {
        // Versión inicial. Una futura versión debe agregar cambios sin borrar registros.
        throw new IllegalStateException("Falta preparar la actualización de la base de datos");
    }

    public long guardar(String fecha, String trabajador, String area, int tipo,
                        double peso, double volumen, String observacion) {
        ContentValues fila = new ContentValues();
        fila.put("uuid", UUID.randomUUID().toString());
        fila.put("fecha", fecha);
        fila.put("trabajador", trabajador);
        fila.put("area", area);
        fila.put("tipo_id", tipo);
        fila.put("peso", peso);
        fila.put("volumen", volumen);
        fila.put("observacion", observacion);
        fila.put("creado", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(new Date()));
        return getWritableDatabase().insertOrThrow("recolecciones", null, fila);
    }

    public ArrayList<Residuo> consultar(String desde, String hasta, int tipo,
                                      Double minimo, Double maximo, boolean pendientes) {
        String sql = "SELECT r.*, t.nombre AS tipo FROM recolecciones r "
                + "JOIN tipos t ON r.tipo_id=t.id WHERE 1=1";
        ArrayList<String> valores = new ArrayList<>();
        if (!desde.isEmpty()) { sql += " AND r.fecha >= ?"; valores.add(desde); }
        if (!hasta.isEmpty()) { sql += " AND r.fecha <= ?"; valores.add(hasta); }
        if (tipo > 0) { sql += " AND r.tipo_id = ?"; valores.add(String.valueOf(tipo)); }
        if (minimo != null) { sql += " AND r.volumen >= ?"; valores.add(String.valueOf(minimo)); }
        if (maximo != null) { sql += " AND r.volumen <= ?"; valores.add(String.valueOf(maximo)); }
        if (pendientes) sql += " AND r.sincronizado=0";
        sql += " ORDER BY r.fecha DESC, r.id DESC";
        ArrayList<Residuo> lista = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(sql, valores.toArray(new String[0]))) {
            while (c.moveToNext()) {
                Residuo r = new Residuo();
                r.id = c.getLong(c.getColumnIndexOrThrow("id"));
                r.tipoId = c.getInt(c.getColumnIndexOrThrow("tipo_id"));
                r.uuid = c.getString(c.getColumnIndexOrThrow("uuid"));
                r.fecha = c.getString(c.getColumnIndexOrThrow("fecha"));
                r.trabajador = c.getString(c.getColumnIndexOrThrow("trabajador"));
                r.area = c.getString(c.getColumnIndexOrThrow("area"));
                r.tipo = c.getString(c.getColumnIndexOrThrow("tipo"));
                r.peso = c.getDouble(c.getColumnIndexOrThrow("peso"));
                r.volumen = c.getDouble(c.getColumnIndexOrThrow("volumen"));
                r.observacion = c.getString(c.getColumnIndexOrThrow("observacion"));
                r.creado = c.getString(c.getColumnIndexOrThrow("creado"));
                r.sincronizado = c.getInt(c.getColumnIndexOrThrow("sincronizado")) == 1;
                lista.add(r);
            }
        }
        return lista;
    }

    public void marcarEnviado(String uuid) {
        ContentValues fila = new ContentValues();
        fila.put("sincronizado", 1);
        getWritableDatabase().update("recolecciones", fila, "uuid=?", new String[]{uuid});
    }
}
