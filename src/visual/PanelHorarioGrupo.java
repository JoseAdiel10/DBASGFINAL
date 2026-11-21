package visual;

import db.ConexionDB;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

public class PanelHorarioGrupo extends JPanel {

    private final JComboBox<ComboItem> cmbPeriodo = new JComboBox<>();
    private final JComboBox<ComboItem> cmbGrupo = new JComboBox<>();
    private final JComboBox<ComboItem> cmbDia = new JComboBox<>();
    private final JTextField txtHoraInicio = new JTextField("08:00", 6);
    private final JTextField txtHoraFin = new JTextField("10:00", 6);
    private final JLabel lblHorarioCondensado = new JLabel("(Sin horario)");

    private final DefaultTableModel modelo = UIUtils.crearModeloSoloLectura(
            new String[]{"ID Día", "Día", "Corto", "Hora Inicio", "Hora Fin"});
    private final JTable tabla = new JTable(modelo);
    private boolean cargando = false;

    /**
     * Construye el módulo para asignar o eliminar bloques en dbo.horariogrupo
     * y visualizar en tiempo real el horario condensado actualizado por el trigger en dbo.grupo.
     */
    public PanelHorarioGrupo() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel seleccion = new JPanel(new GridLayout(3, 2, 8, 8));
        seleccion.setBorder(new TitledBorder("Seleccionar Grupo"));
        seleccion.add(new JLabel("Período Académico:"));
        seleccion.add(cmbPeriodo);
        seleccion.add(new JLabel("Grupo (Asignatura - Número):"));
        seleccion.add(cmbGrupo);
        seleccion.add(new JLabel("Horario Condensado (actualizado por Trigger):"));
        lblHorarioCondensado.setFont(lblHorarioCondensado.getFont().deriveFont(Font.BOLD, 14f));
        lblHorarioCondensado.setForeground(new Color(0, 102, 153));
        seleccion.add(lblHorarioCondensado);

        JPanel agregarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        agregarPanel.setBorder(new TitledBorder("Agregar / Eliminar Bloque de Horario"));
        agregarPanel.add(new JLabel("Día:"));
        agregarPanel.add(cmbDia);
        agregarPanel.add(new JLabel("Hora Inicio (HH:mm):"));
        agregarPanel.add(txtHoraInicio);
        agregarPanel.add(new JLabel("Hora Fin (HH:mm):"));
        agregarPanel.add(txtHoraFin);

        JButton btnAgregar = new JButton("Asignar Horario");
        JButton btnEliminar = new JButton("Eliminar Bloque Seleccionado");
        agregarPanel.add(btnAgregar);
        agregarPanel.add(btnEliminar);

        cmbPeriodo.addActionListener(e -> {
            if (!cargando) cargarGruposPorPeriodo();
        });
        cmbGrupo.addActionListener(e -> {
            if (!cargando) cargarHorariosDelGrupo();
        });
        btnAgregar.addActionListener(e -> agregarHorario());
        btnEliminar.addActionListener(e -> eliminarHorario());

        JPanel norte = new JPanel(new BorderLayout(8, 8));
        norte.add(seleccion, BorderLayout.NORTH);
        norte.add(agregarPanel, BorderLayout.SOUTH);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    /**
     * Carga los períodos académicos y los días de la semana desde la base de datos,
     * y luego llena los grupos disponibles para el período activo.
     */
    public void recargarCatalogos() {
        cargando = true;
        UIUtils.cargarComboSimple(cmbPeriodo, "SELECT codigo, ISNULL(descripcion, codigo) FROM dbo.periodo_academico ORDER BY codigo");
        UIUtils.cargarComboSimple(cmbDia, "SELECT CAST(dia AS varchar(5)), descripcion + ' (' + ISNULL(nombrecorto,'') + ')' FROM dbo.diasemana ORDER BY dia");
        cargando = false;
        cargarGruposPorPeriodo();
    }

    /**
     * Consulta los grupos pertenecientes al período seleccionado y los muestra en cmbGrupo.
     */
    private void cargarGruposPorPeriodo() {
        cargando = true;
        cmbGrupo.removeAllItems();
        ComboItem periodo = (ComboItem) cmbPeriodo.getSelectedItem();
        if (periodo != null) {
            String sql = "SELECT g.codigo_asignatura + '|' + g.numero_grupo AS clave, " +
                    "g.codigo_asignatura + '-' + g.numero_grupo + ' (' + ISNULL(a.nombre,'') + ')' AS etiqueta " +
                    "FROM dbo.grupo g INNER JOIN dbo.Asignatura a ON g.codigo_asignatura = a.codigo " +
                    "WHERE g.codigo_periodo = ? ORDER BY g.codigo_asignatura, g.numero_grupo";
            try (Connection con = ConexionDB.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, periodo.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        cmbGrupo.addItem(new ComboItem(rs.getString("clave"), rs.getString("etiqueta")));
                    }
                }
            } catch (SQLException e) {
                UIUtils.mostrarError(this, "Error cargando grupos: " + e.getMessage());
            }
        }
        cargando = false;
        cargarHorariosDelGrupo();
    }

    /**
     * Lista los bloques actuales del grupo en dbo.horariogrupo y lee el valor
     * de la columna dbo.grupo.horario generado por el trigger.
     */
    private void cargarHorariosDelGrupo() {
        modelo.setRowCount(0);
        lblHorarioCondensado.setText("(Sin horario)");
        ComboItem periodo = (ComboItem) cmbPeriodo.getSelectedItem();
        ComboItem grupo = (ComboItem) cmbGrupo.getSelectedItem();
        if (periodo == null || grupo == null) return;

        String[] partes = grupo.getId().split("\\|");
        String codAsig = partes[0];
        String numGrupo = partes[1];

        String sqlHorarios = "SELECT hg.dia, ds.descripcion, ds.nombrecorto, hg.horainicio, hg.horafin " +
                "FROM dbo.horariogrupo hg INNER JOIN dbo.diasemana ds ON hg.dia = ds.dia " +
                "WHERE hg.codigo_periodo=? AND hg.codigo_asignatura=? AND hg.numero_grupo=? " +
                "ORDER BY hg.dia, hg.horainicio";

        String sqlCondensado = "SELECT ISNULL(horario, '(Vacío)') FROM dbo.grupo " +
                "WHERE codigo_periodo=? AND codigo_asignatura=? AND numero_grupo=?";

        try (Connection con = ConexionDB.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(sqlHorarios)) {
                ps.setString(1, periodo.getId());
                ps.setString(2, codAsig);
                ps.setString(3, numGrupo);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        modelo.addRow(new Object[]{
                                rs.getInt("dia"),
                                rs.getString("descripcion"),
                                rs.getString("nombrecorto"),
                                rs.getTime("horainicio"),
                                rs.getTime("horafin")
                        });
                    }
                }
            }
            try (PreparedStatement ps2 = con.prepareStatement(sqlCondensado)) {
                ps2.setString(1, periodo.getId());
                ps2.setString(2, codAsig);
                ps2.setString(3, numGrupo);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    if (rs2.next()) {
                        String h = rs2.getString(1);
                        lblHorarioCondensado.setText(h == null || h.trim().isEmpty() ? "(Sin horario)" : h);
                    }
                }
            }
        } catch (SQLException e) {
            UIUtils.mostrarError(this, "Error cargando horarios del grupo: " + e.getMessage());
        }
    }

    /**
     * Inserta un nuevo bloque de día/hora en dbo.horariogrupo, lo cual dispara
     * automáticamente el trigger trg_ActualizarHorarioCondensado en SQL Server.
     */
    private void agregarHorario() {
        ComboItem periodo = (ComboItem) cmbPeriodo.getSelectedItem();
        ComboItem grupo = (ComboItem) cmbGrupo.getSelectedItem();
        ComboItem dia = (ComboItem) cmbDia.getSelectedItem();
        if (periodo == null || grupo == null || dia == null) {
            UIUtils.mostrarError(this, "Seleccione período, grupo y día.");
            return;
        }
        try {
            Time hInicio = UIUtils.parsearHoraSQL(txtHoraInicio.getText());
            Time hFin = UIUtils.parsearHoraSQL(txtHoraFin.getText());
            if (!hFin.after(hInicio)) {
                UIUtils.mostrarError(this, "La hora de fin debe ser mayor que la hora de inicio.");
                return;
            }
            String[] partes = grupo.getId().split("\\|");
            String sql = "INSERT INTO dbo.horariogrupo (codigo_periodo, codigo_asignatura, numero_grupo, dia, horainicio, horafin) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection con = ConexionDB.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, periodo.getId());
                ps.setString(2, partes[0]);
                ps.setString(3, partes[1]);
                ps.setInt(4, Integer.parseInt(dia.getId()));
                ps.setTime(5, hInicio);
                ps.setTime(6, hFin);
                ps.executeUpdate();
            }
            cargarHorariosDelGrupo();
            UIUtils.mostrarInfo(this, "Horario asignado y horario condensado actualizado por el trigger.");
        } catch (IllegalArgumentException ex) {
            UIUtils.mostrarError(this, "Formato de hora inválido. Use HH:mm (ej. 08:00).");
        } catch (SQLException e) {
            UIUtils.mostrarError(this, "Error al asignar horario: " + e.getMessage());
        }
    }

    /**
     * Elimina el bloque de horario seleccionado de dbo.horariogrupo y refresca
     * el horario condensado recalculado por el trigger.
     */
    private void eliminarHorario() {
        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            UIUtils.mostrarError(this, "Seleccione un bloque de horario en la tabla para eliminar.");
            return;
        }
        ComboItem periodo = (ComboItem) cmbPeriodo.getSelectedItem();
        ComboItem grupo = (ComboItem) cmbGrupo.getSelectedItem();
        if (periodo == null || grupo == null) return;

        String[] partes = grupo.getId().split("\\|");
        int dia = Integer.parseInt(modelo.getValueAt(fila, 0).toString());
        Time hInicio = (Time) modelo.getValueAt(fila, 3);

        String sql = "DELETE FROM dbo.horariogrupo " +
                "WHERE codigo_periodo=? AND codigo_asignatura=? AND numero_grupo=? AND dia=? AND horainicio=?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, periodo.getId());
            ps.setString(2, partes[0]);
            ps.setString(3, partes[1]);
            ps.setInt(4, dia);
            ps.setTime(5, hInicio);
            ps.executeUpdate();

            cargarHorariosDelGrupo();
            UIUtils.mostrarInfo(this, "Bloque eliminado y horario condensado actualizado.");
        } catch (SQLException e) {
            UIUtils.mostrarError(this, "Error al eliminar bloque: " + e.getMessage());
        }
    }
}