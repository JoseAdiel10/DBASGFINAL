package visual;

import db.ConexionDB;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

public class PanelInforme extends JPanel {

    private final JComboBox<ComboItem> cmbPeriodo = new JComboBox<>();
    private final JComboBox<ComboItem> cmbEstudiante = new JComboBox<>();

    private final JLabel lblPeriodo = new JLabel("-");
    private final JLabel lblEstudiante = new JLabel("-");
    private final JLabel lblCarrera = new JLabel("-");
    private final JLabel lblTotalGrupos = new JLabel("Total de grupos inscritos: 0");
    private final JLabel lblTotalCreditos = new JLabel("Total de créditos: 0");

    private final DefaultTableModel modelo = UIUtils.crearModeloSoloLectura(
            new String[]{"ID Asignatura", "Número Grupo", "Nombre Asignatura", "Créditos", "Horario Condensado"});
    private final JTable tabla = new JTable(modelo);

    /**
     * Construye el informe de inscripción con encabezado (período, estudiante formateado y carrera),
     * tabla de grupos inscritos y pie de página con totales de grupos y créditos.
     */
    public PanelInforme() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controles.setBorder(new TitledBorder("Generar Informe de Inscripción"));
        controles.add(new JLabel("Período:"));
        controles.add(cmbPeriodo);
        controles.add(new JLabel("Estudiante:"));
        controles.add(cmbEstudiante);
        JButton btnGenerar = new JButton("Generar Informe");
        btnGenerar.addActionListener(e -> generarInforme());
        controles.add(btnGenerar);

        JPanel encabezado = new JPanel(new GridLayout(3, 2, 6, 6));
        encabezado.setBorder(new TitledBorder("Datos del Informe"));
        encabezado.add(new JLabel("Período Académico:"));
        encabezado.add(lblPeriodo);
        encabezado.add(new JLabel("ID y Nombre del Estudiante:"));
        encabezado.add(lblEstudiante);
        encabezado.add(new JLabel("Carrera:"));
        encabezado.add(lblCarrera);

        Font bold = lblEstudiante.getFont().deriveFont(Font.BOLD, 13f);
        lblPeriodo.setFont(bold);
        lblEstudiante.setFont(bold);
        lblCarrera.setFont(bold);

        JPanel norte = new JPanel(new BorderLayout(8, 8));
        norte.add(controles, BorderLayout.NORTH);
        norte.add(encabezado, BorderLayout.SOUTH);

        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 25, 8));
        pie.setBorder(new TitledBorder("Totales"));
        lblTotalGrupos.setFont(bold);
        lblTotalCreditos.setFont(bold);
        pie.add(lblTotalGrupos);
        pie.add(lblTotalCreditos);

        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);
    }

    /** Recarga las listas de períodos académicos y estudiantes disponibles. */
    public void recargarCatalogos() {
        UIUtils.cargarComboSimple(cmbPeriodo, "SELECT codigo, ISNULL(descripcion, codigo) FROM dbo.periodo_academico ORDER BY codigo");
        UIUtils.cargarComboSimple(cmbEstudiante, "SELECT id, ISNULL(nombre,'') + ' ' + ISNULL(apellido,'') FROM dbo.Estudiante ORDER BY id");
    }

    /**
     * Consulta los datos personales del estudiante, aplica el formato de nombre "Juan A. Luna P.",
     * lista todos los grupos inscritos en el período y calcula el total de grupos y créditos.
     */
    private void generarInforme() {
        ComboItem periodo = (ComboItem) cmbPeriodo.getSelectedItem();
        ComboItem estudiante = (ComboItem) cmbEstudiante.getSelectedItem();
        if (periodo == null || estudiante == null) {
            UIUtils.mostrarError(this, "Seleccione un período y un estudiante.");
            return;
        }

        modelo.setRowCount(0);
        lblPeriodo.setText(periodo.toString());

        String sqlCabecera =
                "SELECT e.id, e.nombre, e.apellido, ISNULL(c.nombre_carrera, 'No asignada') AS carrera " +
                "FROM dbo.Estudiante e " +
                "LEFT JOIN dbo.Carrera c ON e.id_carrera = c.idcarrera " +
                "WHERE e.id = ?";

        String sqlDetalle =
                "SELECT gi.codigo_asignatura, gi.numero_grupo, a.nombre, ISNULL(a.creditos,0) AS creditos, ISNULL(g.horario,'') AS horario " +
                "FROM dbo.grupos_inscritos gi " +
                "INNER JOIN dbo.grupo g " +
                "    ON gi.codigo_periodo = g.codigo_periodo " +
                "   AND gi.codigo_asignatura = g.codigo_asignatura " +
                "   AND gi.numero_grupo = g.numero_grupo " +
                "INNER JOIN dbo.Asignatura a ON gi.codigo_asignatura = a.codigo " +
                "WHERE gi.codigo_periodo = ? AND gi.id_estudiante = ? " +
                "ORDER BY gi.codigo_asignatura, gi.numero_grupo";

        try (Connection con = ConexionDB.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(sqlCabecera)) {
                ps.setString(1, estudiante.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String id = rs.getString("id");
                        String nombreFormateado = UIUtils.formatearNombreInforme(
                                rs.getString("nombre"), rs.getString("apellido"));
                        lblEstudiante.setText(id + " - " + nombreFormateado);
                        lblCarrera.setText(rs.getString("carrera"));
                    }
                }
            }

            int totalGrupos = 0;
            int totalCreditos = 0;
            try (PreparedStatement ps = con.prepareStatement(sqlDetalle)) {
                ps.setString(1, periodo.getId());
                ps.setString(2, estudiante.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int cred = rs.getInt("creditos");
                        modelo.addRow(new Object[]{
                                rs.getString("codigo_asignatura"),
                                rs.getString("numero_grupo"),
                                rs.getString("nombre"),
                                cred,
                                rs.getString("horario")
                        });
                        totalGrupos++;
                        totalCreditos += cred;
                    }
                }
            }

            lblTotalGrupos.setText("Total de grupos inscritos: " + totalGrupos);
            lblTotalCreditos.setText("Total de créditos: " + totalCreditos);

        } catch (SQLException e) {
            UIUtils.mostrarError(this, "Error generando el informe: " + e.getMessage());
        }
    }
}