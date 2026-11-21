package visual;

import db.ConexionDB;
import java.awt.Component;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class UIUtils {

    /**
     * Crea un DefaultTableModel cuyas celdas no pueden editarse directamente
     * haciendo doble clic, evitando cambios accidentales en pantalla.
     */
    public static DefaultTableModel crearModeloSoloLectura(String[] columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /**
     * Busca dentro de un JComboBox el elemento cuyo ID coincida con el parámetro
     * y lo deja seleccionado.
     */
    public static void seleccionarComboPorId(JComboBox<ComboItem> combo, String id) {
        if (id == null) return;
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (id.equals(combo.getItemAt(i).getId())) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * Extrae de forma segura el ID del ComboItem actualmente seleccionado.
     * Retorna null si no hay ningún elemento seleccionado.
     */
    public static String obtenerIdCombo(JComboBox<ComboItem> combo) {
        ComboItem item = (ComboItem) combo.getSelectedItem();
        return item == null ? null : item.getId();
    }

    /**
     * Ejecuta una consulta SQL de dos columnas (ID y descripción) para poblar un JComboBox,
     * preservando la selección anterior si todavía existe tras recargar.
     */
    public static void cargarComboSimple(JComboBox<ComboItem> combo, String sql) {
        ComboItem anterior = (ComboItem) combo.getSelectedItem();
        combo.removeAllItems();
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                combo.addItem(new ComboItem(rs.getString(1), rs.getString(2)));
            }
            if (anterior != null) {
                seleccionarComboPorId(combo, anterior.getId());
            }
        } catch (SQLException e) {
            mostrarError(null, "Error al cargar catálogo: " + e.getMessage());
        }
    }

    /**
     * Convierte un texto en formato HH:mm o HH:mm:ss a un objeto java.sql.Time
     * listo para insertarse en columnas de tipo TIME en SQL Server.
     */
    public static Time parsearHoraSQL(String texto) {
        String limpio = texto.trim();
        String[] partes = limpio.split(":");
        if (partes.length == 2) {
            int h = Integer.parseInt(partes[0]);
            int m = Integer.parseInt(partes[1]);
            return Time.valueOf(String.format("%02d:%02d:00", h, m));
        } else if (partes.length == 3) {
            int h = Integer.parseInt(partes[0]);
            int m = Integer.parseInt(partes[1]);
            int s = Integer.parseInt(partes[2]);
            return Time.valueOf(String.format("%02d:%02d:%02d", h, m, s));
        }
        throw new IllegalArgumentException("Usa el formato HH:mm (ejemplo: 08:00).");
    }

    /**
     * Formatea el nombre completo del estudiante al estándar requerido por el informe:
     * Primer Nombre + Inicial Segundo Nombre + Primer Apellido + Inicial Segundo Apellido
     * (ejemplo: "Juan Antonio" y "Luna Perez" -> "Juan A. Luna P.").
     */
    public static String formatearNombreInforme(String nombre, String apellido) {
        return (abreviarSegundaPalabra(nombre) + " " + abreviarSegundaPalabra(apellido)).trim();
    }

    /**
     * Conserva la primera palabra intacta y reduce la segunda a su inicial con punto.
     * Si el texto tiene una sola palabra, la devuelve tal cual.
     */
    private static String abreviarSegundaPalabra(String texto) {
        if (texto == null || texto.trim().isEmpty()) return "";
        String[] partes = texto.trim().split("\\s+");
        if (partes.length == 1) return partes[0];
        return partes[0] + " " + partes[1].substring(0, 1).toUpperCase() + ".";
    }

    /** Convierte un valor de celda a String evitando mostrar la palabra "null". */
    public static String valorStr(Object o) {
        return o == null ? "" : o.toString();
    }

    /** Muestra un cuadro de diálogo de error estandarizado. */
    public static void mostrarError(Component parent, String mensaje) {
        JOptionPane.showMessageDialog(parent, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Muestra un cuadro de diálogo informativo estandarizado. */
    public static void mostrarInfo(Component parent, String mensaje) {
        JOptionPane.showMessageDialog(parent, mensaje, "Información", JOptionPane.INFORMATION_MESSAGE);
    }
}