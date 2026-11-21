package visual;

import db.ConexionDB;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

public class PanelEstudiante extends JPanel {

    private final JTextField txtId = new JTextField(10);
    private final JTextField txtNombre = new JTextField(15);
    private final JTextField txtApellido = new JTextField(15);
    private final JTextField txtDireccion = new JTextField(25);
    private final JComboBox<ComboItem> cmbCarrera = new JComboBox<>();
    private final JComboBox<ComboItem> cmbCategoria = new JComboBox<>();
    private final JComboBox<ComboItem> cmbNacionalidad = new JComboBox<>();

    private final DefaultTableModel modelo = UIUtils.crearModeloSoloLectura(
            new String[]{"ID", "Nombre", "Apellido", "Dirección", "ID Carrera", "ID Cat. Pago", "ID Nac."});
    private final JTable tabla = new JTable(modelo);

    /**
     * Construye el formulario CRUD de la tabla Estudiante junto con la tabla
     * de visualización y los catálogos foráneos (Carrera, Categoría de Pago y Nacionalidad).
     */
    public PanelEstudiante() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(4, 4, 8, 8));
        form.setBorder(new TitledBorder("Datos del Estudiante"));
        form.add(new JLabel("ID (máx 8):"));
        form.add(txtId);
        form.add(new JLabel("Nombre:"));
        form.add(txtNombre);
        form.add(new JLabel("Apellido:"));
        form.add(txtApellido);
        form.add(new JLabel("Dirección:"));
        form.add(txtDireccion);
        form.add(new JLabel("Carrera:"));
        form.add(cmbCarrera);
        form.add(new JLabel("Categoría de Pago:"));
        form.add(cmbCategoria);
        form.add(new JLabel("Nacionalidad:"));
        form.add(cmbNacionalidad);
        form.add(new JLabel());
        form.add(new JLabel());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnNuevo = new JButton("Nuevo");
        JButton btnCrear = new JButton("Crear");
        JButton btnActualizar = new JButton("Actualizar");
        JButton btnEliminar = new JButton("Eliminar");
        JButton btnRefrescar = new JButton("Refrescar");

        btnNuevo.addActionListener(e -> limpiar());
        btnCrear.addActionListener(e -> crear());
        btnActualizar.addActionListener(e -> actualizar());
        btnEliminar.addActionListener(e -> eliminar());
        btnRefrescar.addActionListener(e -> recargarTodo());

        botones.add(btnNuevo);
        botones.add(btnCrear);
        botones.add(btnActualizar);
        botones.add(btnEliminar);
        botones.add(btnRefrescar);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(form, BorderLayout.CENTER);
        norte.add(botones, BorderLayout.SOUTH);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabla.getSelectedRow() != -1) {
                cargarFilaEnFormulario(tabla.getSelectedRow());
            }
        });

        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    /**
     * Recarga los catálogos desplegables de llaves foráneas y la tabla completa de estudiantes.
     */
    public void recargarTodo() {
        UIUtils.cargarComboSimple(cmbCarrera, "SELECT idcarrera, nombre_carrera FROM dbo.Carrera ORDER BY idcarrera");
        UIUtils.cargarComboSimple(cmbCategoria, "SELECT id_categoria_pago, descripcion FROM dbo.categoria_pago ORDER BY id_categoria_pago");
        UIUtils.cargarComboSimple(cmbNacionalidad, "SELECT id_nacionalidad, nombre FROM dbo.nacionalidad ORDER BY id_nacionalidad");
        cargarTabla();
    }

    /**
     * Consulta todos los registros de dbo.Estudiante y los muestra en el JTable.
     */
    private void cargarTabla() {
        modelo.setRowCount(0);
        String sql = "SELECT id, nombre, apellido, direccion, id_carrera, id_categoria_pago, id_nacionalidad " +
                "FROM dbo.Estudiante ORDER BY id";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                modelo.addRow(new Object[]{
                        rs.getString("id"),
                        rs.getString("nombre"),
                        rs.getString("apellido"),
                        rs.getString("direccion"),
                        rs.getString("id_carrera"),
                        rs.getString("id_categoria_pago"),
                        rs.getString("id_nacionalidad")
                });
            }
        } catch (SQLException e) {
            UIUtils.mostrarError(this, "Error cargando estudiantes: " + e.getMessage());
        }
    }

    /**
     * Transfiere los datos de la fila seleccionada en la tabla hacia los campos
     * del formulario para editar o eliminar el registro.
     */
    private void cargarFilaEnFormulario(int fila) {
        txtId.setText(UIUtils.valorStr(modelo.getValueAt(fila, 0)));
        txtId.setEnabled(false);
        txtNombre.setText(UIUtils.valorStr(modelo.getValueAt(fila, 1)));
        txtApellido.setText(UIUtils.valorStr(modelo.getValueAt(fila, 2)));
        txtDireccion.setText(UIUtils.valorStr(modelo.getValueAt(fila, 3)));
        UIUtils.seleccionarComboPorId(cmbCarrera, UIUtils.valorStr(modelo.getValueAt(fila, 4)));
        UIUtils.seleccionarComboPorId(cmbCategoria, UIUtils.valorStr(modelo.getValueAt(fila, 5)));
        UIUtils.seleccionarComboPorId(cmbNacionalidad, UIUtils.valorStr(modelo.getValueAt(fila, 6)));
    }

    /**
     * Limpia los campos de texto y vuelve a habilitar el ID para registrar un nuevo estudiante.
     */
    private void limpiar() {
        txtId.setText("");
        txtId.setEnabled(true);
        txtNombre.setText("");
        txtApellido.setText("");
        txtDireccion.setText("");
        tabla.clearSelection();
        txtId.requestFocus();
    }

    /**
     * Inserta un nuevo registro en dbo.Estudiante mediante PreparedStatement.
     */
    private void crear() {
        if (txtId.getText().trim().isEmpty()) {
            UIUtils.mostrarError(this, "El ID del estudiante es obligatorio.");
            return;
        }
        String sql = "INSERT INTO dbo.Estudiante (id, nombre, apellido, direccion, id_carrera, id_categoria_pago, id_nacionalidad) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, txtId.getText().trim());
            ps.setString(2, txtNombre.getText().trim());
            ps.setString(3, txtApellido.getText().trim());
            ps.setString(4, txtDireccion.getText().trim());
            ps.setString(5, UIUtils.obtenerIdCombo(cmbCarrera));
            ps.setString(6, UIUtils.obtenerIdCombo(cmbCategoria));
            ps.setString(7, UIUtils.obtenerIdCombo(cmbNacionalidad));
            ps.executeUpdate();

            UIUtils.mostrarInfo(this, "Estudiante creado correctamente.");
            limpiar();
            cargarTabla();
        } catch (SQLException e) {
            UIUtils.mostrarError(this, "Error al crear estudiante: " + e.getMessage());
        }
    }

    /**
     * Actualiza los datos de un estudiante existente identificado por su ID.
     */
    private void actualizar() {
        if (txtId.getText().trim().isEmpty()) {
            UIUtils.mostrarError(this, "Seleccione un estudiante de la tabla.");
            return;
        }
        String sql = "UPDATE dbo.Estudiante SET nombre=?, apellido=?, direccion=?, " +
                "id_carrera=?, id_categoria_pago=?, id_nacionalidad=? WHERE id=?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, txtNombre.getText().trim());
            ps.setString(2, txtApellido.getText().trim());
            ps.setString(3, txtDireccion.getText().trim());
            ps.setString(4, UIUtils.obtenerIdCombo(cmbCarrera));
            ps.setString(5, UIUtils.obtenerIdCombo(cmbCategoria));
            ps.setString(6, UIUtils.obtenerIdCombo(cmbNacionalidad));
            ps.setString(7, txtId.getText().trim());

            int filas = ps.executeUpdate();
            if (filas > 0) {
                UIUtils.mostrarInfo(this, "Estudiante actualizado.");
                limpiar();
                cargarTabla();
            } else {
                UIUtils.mostrarError(this, "No se encontró un estudiante con ese ID.");
            }
        } catch (SQLException e) {
            UIUtils.mostrarError(this, "Error al actualizar estudiante: " + e.getMessage());
        }
    }

    /**
     * Elimina de dbo.Estudiante el registro seleccionado previa confirmación del usuario.
     */
    private void eliminar() {
        String id = txtId.getText().trim();
        if (id.isEmpty()) {
            UIUtils.mostrarError(this, "Seleccione un estudiante para eliminar.");
            return;
        }
        int conf = JOptionPane.showConfirmDialog(this, "¿Eliminar al estudiante " + id + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM dbo.Estudiante WHERE id=?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();

            UIUtils.mostrarInfo(this, "Estudiante eliminado.");
            limpiar();
            cargarTabla();
        } catch (SQLException e) {
            UIUtils.mostrarError(this, "No se pudo eliminar (puede tener inscripciones asociadas):\n" + e.getMessage());
        }
    }
}