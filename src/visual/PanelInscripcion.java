package visual;

import db.ConexionDB;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
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
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

public class PanelInscripcion extends JPanel {

    private final JComboBox<ComboItem> cmbPeriodo = new JComboBox<>();
    private final JComboBox<ComboItem> cmbEstudiante = new JComboBox<>();

    private final DefaultTableModel modDisponibles = UIUtils.crearModeloSoloLectura(
            new String[]{"Asignatura", "Nombre", "Grupo", "Cupo", "Disp.", "Horario"});
    private final JTable tblDisponibles = new JTable(modDisponibles);

    private final DefaultTableModel modInscritos = UIUtils.crearModeloSoloLectura(
            new String[]{"Asignatura", "Nombre", "Grupo", "Créditos", "Horario"});
    private final JTable tblInscritos = new JTable(modInscritos);

    private final DefaultTableModel modDetalleHorario = UIUtils.crearModeloSoloLectura(
            new String[]{"Día", "Corto", "Hora Inicio", "Hora Fin"});
    private final JTable tblDetalleHorario = new JTable(modDetalleHorario);
    private final JLabel lblGrupoSeleccionado = new JLabel(
            "Seleccione un grupo disponible o inscrito para ver su horario.");

    private boolean cargando = false;

    /**
     * Construye el módulo de inscripción: selección de período y estudiante,
     * tabla de grupos disponibles con cupo, tabla de grupos inscritos y vista
     * inferior del horario detallado del grupo seleccionado.
     */
    public PanelInscripcion() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        top.setBorder(new TitledBorder("Selección de Período y Estudiante"));
        top.add(new JLabel("Período:"));
        top.add(cmbPeriodo);
        top.add(new JLabel("Estudiante:"));
        top.add(cmbEstudiante);
        JButton btnRefrescar = new JButton("Cargar Listas");
        btnRefrescar.addActionListener(e -> cargarListas());
        top.add(btnRefrescar);

        cmbPeriodo.addActionListener(e -> {
            if (!cargando) cargarListas();
        });
        cmbEstudiante.addActionListener(e -> {
            if (!cargando) cargarListas();
        });

        JPanel pIzq = new JPanel(new BorderLayout());
        pIzq.setBorder(new TitledBorder("Grupos Abiertos y Disponibles"));
        tblDisponibles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pIzq.add(new JScrollPane(tblDisponibles), BorderLayout.CENTER);

        JPanel pDer = new JPanel(new BorderLayout());
        pDer.setBorder(new TitledBorder("Grupos Inscritos del Estudiante"));
        tblInscritos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pDer.add(new JScrollPane(tblInscritos), BorderLayout.CENTER);

        JPanel pBotones = new JPanel(new GridBagLayout());
        JPanel cajaBotones = new JPanel(new GridLayout(1, 2, 12, 8));
        JButton btnInscribir = new JButton("Inscribir >>");
        JButton btnRetirar = new JButton("<< Eliminar");
        btnInscribir.addActionListener(e -> inscribirGrupo());
        btnRetirar.addActionListener(e -> eliminarGrupoInscrito());
        cajaBotones.add(btnInscribir);
        cajaBotones.add(btnRetirar);
        pBotones.add(cajaBotones);

        JPanel centro = new JPanel(new BorderLayout(8, 8));
        JPanel listas = new JPanel(new GridLayout(1, 2, 10, 10));
        listas.add(pIzq);
        listas.add(pDer);
        centro.add(listas, BorderLayout.CENTER);
        centro.add(pBotones, BorderLayout.SOUTH);

        JPanel pDetalle = new JPanel(new BorderLayout(5, 5));
        pDetalle.setBorder(new TitledBorder("Horario del Grupo Seleccionado (Tabla horariogrupo)"));
        pDetalle.setPreferredSize(new Dimension(100, 160));
        lblGrupoSeleccionado.setFont(lblGrupoSeleccionado.getFont().deriveFont(Font.BOLD));
        pDetalle.add(lblGrupoSeleccionado, BorderLayout.NORTH);
        pDetalle.add(new JScrollPane(tblDetalleHorario), BorderLayout.CENTER);

        tblDisponibles.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblDisponibles.getSelectedRow() != -1) {
                tblInscritos.clearSelection();
                int r = tblDisponibles.getSelectedRow();
                cargarDetalleHorario(modDisponibles.getValueAt(r, 0).toString(),
                        modDisponibles.getValueAt(r, 2).toString());
            }
        });

        tblInscritos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblInscritos.getSelectedRow() != -1) {
                tblDisponibles.clearSelection();
                int r = tblInscritos.getSelectedRow();
                cargarDetalleHorario(modInscritos.getValueAt(r, 0).toString(),
                        modInscritos.getValueAt(r, 2).toString());
            }
        });

        add(top, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(pDetalle, BorderLayout.SOUTH);
    }

    /** Recarga las listas de períodos y estudiantes almacenados en la base de datos. */
    public void recargarCatalogos() {
        cargando = true;
        UIUtils.cargarComboSimple(cmbPeriodo, "SELECT codigo, ISNULL(descripcion, codigo) FROM dbo.periodo_academico ORDER BY codigo");
        UIUtils.cargarComboSimple(cmbEstudiante, "SELECT id, ISNULL(nombre,'') + ' ' + ISNULL(apellido,'') FROM dbo.Estudiante ORDER BY id");
        cargando = false;
        cargarListas();
    }

    /**
     * Consulta los grupos abiertos con cupo disponible que el estudiante aún no ha inscrito
     * y los grupos que ya tiene inscritos en el período seleccionado.
     */
    private void cargarListas() {
        modDisponibles.setRowCount(0);
        modInscritos.setRowCount(0);
        modDetalleHorario.setRowCount(0);
        lblGrupoSeleccionado.setText("Seleccione un grupo disponible o inscrito para ver su horario.");

        ComboItem periodo = (ComboItem) cmbPeriodo.getSelectedItem();
        ComboItem estudiante = (ComboItem) cmbEstudiante.getSelectedItem();
        if (periodo == null || estudiante == null) return;

        String sqlDisp =
                "SELECT g.codigo_asignatura, a.nombre, g.numero_grupo, ISNULL(g.cupo,0) AS cupo, " +
                "       (ISNULL(g.cupo,0) - COUNT(gi.id_estudiante)) AS disponibles, ISNULL(g.horario,'') AS horario " +
                "FROM dbo.grupo g " +
                "INNER JOIN dbo.Asignatura a ON g.codigo_asignatura = a.codigo " +
                "LEFT JOIN dbo.grupos_inscritos gi " +
                "    ON g.codigo_periodo = gi.codigo_periodo " +
                "   AND g.codigo_asignatura = gi.codigo_asignatura " +
                "   AND g.numero_grupo = gi.numero_grupo " +
                "WHERE g.codigo_periodo = ? " +
                "  AND NOT EXISTS ( " +
                "      SELECT 1 FROM dbo.grupos_inscritos x " +
                "      WHERE x.codigo_periodo = g.codigo_periodo " +
                "        AND x.codigo_asignatura = g.codigo_asignatura " +
                "        AND x.id_estudiante = ? " +
                "  ) " +
                "GROUP BY g.codigo_asignatura, a.nombre, g.numero_grupo, g.cupo, g.horario " +
                "HAVING ISNULL(g.cupo,0) > COUNT(gi.id_estudiante) " +
                "ORDER BY g.codigo_asignatura, g.numero_grupo";

        String sqlInsc =
                "SELECT gi.codigo_asignatura, a.nombre, gi.numero_grupo, ISNULL(a.creditos,0) AS creditos, ISNULL(g.horario,'') AS horario " +
                "FROM dbo.grupos_inscritos gi " +
                "INNER JOIN dbo.grupo g " +
                "    ON gi.codigo_periodo = g.codigo_periodo " +
                "   AND gi.codigo_asignatura = g.codigo_asignatura " +
                "   AND gi.numero_grupo = g.numero_grupo " +
                "INNER JOIN dbo.Asignatura a ON gi.codigo_asignatura = a.codigo " +
                "WHERE gi.codigo_periodo = ? AND gi.id_estudiante = ? " +
                "ORDER BY gi.codigo_asignatura, gi.numero_grupo";

        try (Connection con = ConexionDB.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(sqlDisp)) {
                ps.setString(1, periodo.getId());
                ps.setString(2, estudiante.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        modDisponibles.addRow(new Object[]{
                                rs.getString("codigo_asignatura"),
                                rs.getString("nombre"),
                                rs.getString("numero_grupo"),
                                rs.getInt("cupo"),
                                rs.getInt("disponibles"),
                                rs.getString("horario")
                        });
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement(sqlInsc)) {
                ps.setString(1, periodo.getId());
                ps.setString(2, estudiante.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        modInscritos.addRow(new Object[]{
                                rs.getString("codigo_asignatura"),
                                rs.getString("nombre"),
                                rs.getString("numero_grupo"),
                                rs.getInt("creditos"),
                                rs.getString("horario")
                        });
                    }
                }
            }
        } catch (SQLException e) {
            UIUtils.mostrarError(this, "Error cargando grupos: " + e.getMessage());
        }
    }

    /**
     * Consulta en dbo.horariogrupo y dbo.diasemana los bloques de clase del grupo
     * seleccionado en cualquiera de las dos listas y los presenta en la tabla inferior.
     */
    private void cargarDetalleHorario(String codAsig, String numGrupo) {
        modDetalleHorario.setRowCount(0);
        ComboItem periodo = (ComboItem) cmbPeriodo.getSelectedItem();
        if (periodo == null) return;

        lblGrupoSeleccionado.setText("Horario de: " + codAsig + " - Grupo " + numGrupo + " (" + periodo.getId() + ")");
        String sql = "SELECT ds.descripcion, ds.nombrecorto, hg.horainicio, hg.horafin " +
                "FROM dbo.horariogrupo hg INNER JOIN dbo.diasemana ds ON hg.dia = ds.dia " +
                "WHERE hg.codigo_periodo=? AND hg.codigo_asignatura=? AND hg.numero_grupo=? " +
                "ORDER BY hg.dia, hg.horainicio";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, periodo.getId());
            ps.setString(2, codAsig);
            ps.setString(3, numGrupo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    modDetalleHorario.addRow(new Object[]{
                            rs.getString("descripcion"),
                            rs.getString("nombrecorto"),
                            rs.getTime("horainicio"),
                            rs.getTime("horafin")
                    });
                }
            }
        } catch (SQLException e) {
            UIUtils.mostrarError(this, "Error cargando detalle de horario: " + e.getMessage());
        }
    }

    /**
     * Inscribe el grupo seleccionado dentro de una transacción:
     * primero verifica/crea el encabezado en dbo.inscripcion para cumplir con la FK,
     * y luego inserta el grupo en dbo.grupos_inscritos.
     */
    private void inscribirGrupo() {
        int fila = tblDisponibles.getSelectedRow();
        if (fila == -1) {
            UIUtils.mostrarError(this, "Seleccione un grupo de la lista de disponibles.");
            return;
        }
        ComboItem periodo = (ComboItem) cmbPeriodo.getSelectedItem();
        ComboItem estudiante = (ComboItem) cmbEstudiante.getSelectedItem();
        if (periodo == null || estudiante == null) return;

        String codAsig = modDisponibles.getValueAt(fila, 0).toString();
        String numGrupo = modDisponibles.getValueAt(fila, 2).toString();

        String sqlExisteInsc = "SELECT 1 FROM dbo.inscripcion WHERE codigo_periodo=? AND id_estudiante=?";
        String sqlCrearInsc = "INSERT INTO dbo.inscripcion (codigo_periodo, id_estudiante, fecha_inscripcion) " +
                "VALUES (?, ?, CAST(GETDATE() AS DATE))";
        String sqlInsertGrupo = "INSERT INTO dbo.grupos_inscritos (codigo_periodo, id_estudiante, codigo_asignatura, numero_grupo) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection con = ConexionDB.getConnection()) {
            con.setAutoCommit(false);
            try {
                boolean existe = false;
                try (PreparedStatement ps = con.prepareStatement(sqlExisteInsc)) {
                    ps.setString(1, periodo.getId());
                    ps.setString(2, estudiante.getId());
                    try (ResultSet rs = ps.executeQuery()) {
                        existe = rs.next();
                    }
                }
                if (!existe) {
                    try (PreparedStatement ps = con.prepareStatement(sqlCrearInsc)) {
                        ps.setString(1, periodo.getId());
                        ps.setString(2, estudiante.getId());
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(sqlInsertGrupo)) {
                    ps.setString(1, periodo.getId());
                    ps.setString(2, estudiante.getId());
                    ps.setString(3, codAsig);
                    ps.setString(4, numGrupo);
                    ps.executeUpdate();
                }
                con.commit();
                cargarListas();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            UIUtils.mostrarError(this, "Error al inscribir grupo: " + e.getMessage());
        }
    }

    /**
     * Retira el grupo seleccionado de dbo.grupos_inscritos para el estudiante y período actuales.
     */
    private void eliminarGrupoInscrito() {
        int fila = tblInscritos.getSelectedRow();
        if (fila == -1) {
            UIUtils.mostrarError(this, "Seleccione un grupo de la lista de inscritos para eliminar.");
            return;
        }
        ComboItem periodo = (ComboItem) cmbPeriodo.getSelectedItem();
        ComboItem estudiante = (ComboItem) cmbEstudiante.getSelectedItem();
        if (periodo == null || estudiante == null) return;

        String codAsig = modInscritos.getValueAt(fila, 0).toString();
        String numGrupo = modInscritos.getValueAt(fila, 2).toString();

        String sql = "DELETE FROM dbo.grupos_inscritos " +
                "WHERE codigo_periodo=? AND id_estudiante=? AND codigo_asignatura=? AND numero_grupo=?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, periodo.getId());
            ps.setString(2, estudiante.getId());
            ps.setString(3, codAsig);
            ps.setString(4, numGrupo);
            ps.executeUpdate();
            cargarListas();
        } catch (SQLException e) {
            UIUtils.mostrarError(this, "Error al eliminar grupo inscrito: " + e.getMessage());
        }
    }
}