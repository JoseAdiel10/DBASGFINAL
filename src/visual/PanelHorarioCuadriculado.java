package visual;

import db.ConexionDB;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

public class PanelHorarioCuadriculado extends JPanel {

    private final JComboBox<ComboItem> cmbPeriodo = new JComboBox<>();
    private final JComboBox<ComboItem> cmbEstudiante = new JComboBox<>();
    private final JTextField txtNombreSP = new JTextField("dbo.sp_HorarioCuadriculado", 22);
    private final JTable tabla = new JTable();

    /**
     * Construye la pantalla que invoca el Stored Procedure del horario cuadriculado
     * y muestra sus columnas dinámicamente en una cuadrícula.
     */
    public PanelHorarioCuadriculado() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        top.setBorder(new TitledBorder("Parámetros del Stored Procedure"));
        top.add(new JLabel("Período:"));
        top.add(cmbPeriodo);
        top.add(new JLabel("Estudiante:"));
        top.add(cmbEstudiante);
        top.add(new JLabel("Procedimiento:"));
        top.add(txtNombreSP);

        JButton btnMostrar = new JButton("Mostrar Horario");
        btnMostrar.addActionListener(e -> ejecutarStoredProcedure());
        top.add(btnMostrar);

        tabla.setRowHeight(26);
        tabla.setGridColor(Color.LIGHT_GRAY);
        tabla.setShowGrid(true);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    /** Recarga los selectores de período académico y estudiante. */
    public void recargarCatalogos() {
        UIUtils.cargarComboSimple(cmbPeriodo, "SELECT codigo, ISNULL(descripcion, codigo) FROM dbo.periodo_academico ORDER BY codigo");
        UIUtils.cargarComboSimple(cmbEstudiante, "SELECT id, ISNULL(nombre,'') + ' ' + ISNULL(apellido,'') FROM dbo.Estudiante ORDER BY id");
    }

    /**
     * Invoca el Stored Procedure en SQL Server usando CallableStatement
     * y construye el modelo de tabla automáticamente a partir de los metadatos del ResultSet.
     */
    private void ejecutarStoredProcedure() {
        ComboItem periodo = (ComboItem) cmbPeriodo.getSelectedItem();
        ComboItem estudiante = (ComboItem) cmbEstudiante.getSelectedItem();
        String sp = txtNombreSP.getText().trim();

        if (periodo == null || estudiante == null) {
            UIUtils.mostrarError(this, "Seleccione un período y un estudiante.");
            return;
        }
        if (!sp.matches("[a-zA-Z0-9_.\\[\\]]+")) {
            UIUtils.mostrarError(this, "Nombre de procedimiento almacenado inválido.");
            return;
        }

        String llamada = "{call " + sp + "(?, ?)}";
        try (Connection con = ConexionDB.getConnection();
             CallableStatement cs = con.prepareCall(llamada)) {
            cs.setString(1, periodo.getId());
            cs.setString(2, estudiante.getId());

            try (ResultSet rs = cs.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                String[] columnas = new String[colCount];
                for (int i = 1; i <= colCount; i++) {
                    columnas[i - 1] = meta.getColumnLabel(i);
                }

                DefaultTableModel modelo = UIUtils.crearModeloSoloLectura(columnas);
                while (rs.next()) {
                    Object[] fila = new Object[colCount];
                    for (int i = 1; i <= colCount; i++) {
                        Object val = rs.getObject(i);
                        fila[i - 1] = val == null ? "" : val;
                    }
                    modelo.addRow(fila);
                }
                tabla.setModel(modelo);
            }
        } catch (SQLException e) {
            UIUtils.mostrarError(this, "Error ejecutando " + sp + ":\n" + e.getMessage());
        }
    }
}