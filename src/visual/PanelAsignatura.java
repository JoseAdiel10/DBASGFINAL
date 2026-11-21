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

public class PanelAsignatura extends JPanel {

    private final JTextField txtCodigo = new JTextField(10);
    private final JTextField txtNombre = new JTextField(25);
    private final JTextField txtCreditos = new JTextField(5);
    private final JTextField txtTeoricas = new JTextField(5);
    private final JTextField txtPracticas = new JTextField(5);

    private final DefaultTableModel modelo = UIUtils.crearModeloSoloLectura(
            new String[]{"Código", "Nombre", "Créditos", "Horas Teóricas", "Horas Prácticas"});
    private final JTable tabla = new JTable(modelo);

    /**
     * Construye el panel CRUD para gestionar los registros de la tabla dbo.Asignatura.
     */
    public PanelAsignatura() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(3, 4, 8, 8));
        form.setBorder(new TitledBorder("Datos de la Asignatura"));
        form.add(new JLabel("Código (máx 8):"));
        form.add(txtCodigo);
        form.add(new JLabel("Nombre:"));
        form.add(txtNombre);
        form.add(new JLabel("Créditos:"));
        form.add(txtCreditos);
        form.add(new JLabel("Horas Teóricas:"));
        form.add(txtTeoricas);
        form.add(new JLabel("Horas Prácticas:"));
        form.add(txtPracticas);
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
        btnRefrescar.addActionListener(e -> cargarTabla());

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
     * Consulta todas las asignaturas almacenadas en dbo.Asignatura y las carga en el JTable.
     */
    public void cargarTabla() {
        modelo.setRowCount(0);
        String sql = "SELECT codigo, nombre, creditos, Horas_teoricas, Horas_practicas " +
                "FROM dbo.Asignatura ORDER BY codigo";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                modelo.addRow(new Object[]{
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getObject("creditos"),
                        rs.getObject("Horas_teoricas"),
                        rs.getObject("Horas_practicas")
                });
            }
        } catch (SQLException e) {
            UIUtils.mostrarError(this, "Error cargando asignaturas: " + e.getMessage());
        }
    }

    /**
     * Copia los datos de la fila seleccionada al formulario y bloquea el código
     * (llave primaria) durante la edición.
     */
    private void cargarFilaEnFormulario(int fila) {
        txtCodigo.setText(UIUtils.valorStr(modelo.getValueAt(fila, 0)));
        txtCodigo.setEnabled(false);
        txtNombre.setText(UIUtils.valorStr(modelo.getValueAt(fila, 1)));
        txtCreditos.setText(UIUtils.valorStr(modelo.getValueAt(fila, 2)));
        txtTeoricas.setText(UIUtils.valorStr(modelo.getValueAt(fila, 3)));
        txtPracticas.setText(UIUtils.valorStr(modelo.getValueAt(fila, 4)));
    }

    /** Restablece el formulario para crear una nueva asignatura. */
    private void limpiar() {
        txtCodigo.setText("");
        txtCodigo.setEnabled(true);
        txtNombre.setText("");
        txtCreditos.setText("");
        txtTeoricas.setText("");
        txtPracticas.setText("");
        tabla.clearSelection();
        txtCodigo.requestFocus();
    }

    /** Inserta una nueva asignatura validando que créditos y horas sean números enteros. */
    private void crear() {
        if (txtCodigo.getText().trim().isEmpty()) {
            UIUtils.mostrarError(this, "El código es obligatorio.");
            return;
        }
        String sql = "INSERT INTO dbo.Asignatura (codigo, nombre, creditos, Horas_teoricas, Horas_practicas) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, txtCodigo.getText().trim());
            ps.setString(2, txtNombre.getText().trim());
            ps.setInt(3, Integer.parseInt(txtCreditos.getText().trim()));
            ps.setInt(4, Integer.parseInt(txtTeoricas.getText().trim()));
            ps.setInt(5, Integer.parseInt(txtPracticas.getText().trim()));
            ps.executeUpdate();

            UIUtils.mostrarInfo(this, "Asignatura creada.");
            limpiar();
            cargarTabla();
        } catch (NumberFormatException nfe) {
            UIUtils.mostrarError(this, "Créditos y horas deben ser números enteros.");
        } catch (SQLException e) {
            UIUtils.mostrarError(this, "Error al crear asignatura: " + e.getMessage());
        }
    }

    /** Actualiza nombre, créditos y horas de la asignatura seleccionada. */
    private void actualizar() {
        if (txtCodigo.getText().trim().isEmpty()) {
            UIUtils.mostrarError(this, "Seleccione una asignatura.");
            return;
        }
        String sql = "UPDATE dbo.Asignatura SET nombre=?, creditos=?, Horas_teoricas=?, Horas_practicas=? WHERE codigo=?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, txtNombre.getText().trim());
            ps.setInt(2, Integer.parseInt(txtCreditos.getText().trim()));
            ps.setInt(3, Integer.parseInt(txtTeoricas.getText().trim()));
            ps.setInt(4, Integer.parseInt(txtPracticas.getText().trim()));
            ps.setString(5, txtCodigo.getText().trim());
            ps.executeUpdate();

            UIUtils.mostrarInfo(this, "Asignatura actualizada.");
            limpiar();
            cargarTabla();
        } catch (NumberFormatException nfe) {
            UIUtils.mostrarError(this, "Créditos y horas deben ser números enteros.");
        } catch (SQLException e) {
            UIUtils.mostrarError(this, "Error al actualizar asignatura: " + e.getMessage());
        }
    }

    /** Elimina la asignatura seleccionada de dbo.Asignatura previa confirmación. */
    private void eliminar() {
        String cod = txtCodigo.getText().trim();
        if (cod.isEmpty()) {
            UIUtils.mostrarError(this, "Seleccione una asignatura para eliminar.");
            return;
        }
        int conf = JOptionPane.showConfirmDialog(this, "¿Eliminar la asignatura " + cod + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM dbo.Asignatura WHERE codigo=?";
        try (Connection con = ConexionDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cod);
            ps.executeUpdate();

            UIUtils.mostrarInfo(this, "Asignatura eliminada.");
            limpiar();
            cargarTabla();
        } catch (SQLException e) {
            UIUtils.mostrarError(this, "No se pudo eliminar (puede tener grupos asociados):\n" + e.getMessage());
        }
    }
}