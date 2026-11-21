package visual;

import db.ConexionDB;
import java.awt.GridLayout;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class LoginFrame extends JFrame {

    private final JTextField txtServidor = new JTextField("localhost");
    private final JTextField txtPuerto = new JTextField("1433");
    private final JTextField txtBaseDatos = new JTextField("AcadDB(JAEU0001_KMBR0003)");
    private final JTextField txtUsuario = new JTextField("sa");
    private final JPasswordField txtPassword = new JPasswordField();

    /**
     * Inicializa la ventana de inicio de sesión y arma el formulario de conexión
     * a la base de datos académica.
     */
    public LoginFrame() {
        setTitle("Login - Sistema Académico");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(420, 270);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new GridLayout(6, 2, 8, 8));
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));

        panel.add(new JLabel("Servidor:"));
        panel.add(txtServidor);
        panel.add(new JLabel("Puerto:"));
        panel.add(txtPuerto);
        panel.add(new JLabel("Base de Datos:"));
        panel.add(txtBaseDatos);
        panel.add(new JLabel("Usuario SQL:"));
        panel.add(txtUsuario);
        panel.add(new JLabel("Contraseña:"));
        panel.add(txtPassword);

        JButton btnConectar = new JButton("Conectar");
        btnConectar.addActionListener(e -> conectar());
        getRootPane().setDefaultButton(btnConectar);

        panel.add(new JLabel());
        panel.add(btnConectar);

        add(panel);
    }

    /**
     * Lee los campos del formulario e intenta autenticar en SQL Server.
     * Si la conexión es exitosa, abre el menú principal y cierra el login.
     */
    private void conectar() {
        String srv = txtServidor.getText().trim();
        String port = txtPuerto.getText().trim();
        String db = txtBaseDatos.getText().trim();
        String user = txtUsuario.getText().trim();
        String pass = new String(txtPassword.getPassword());

        try {
            if (ConexionDB.autenticar(srv, port, db, user, pass)) {
                new MenuPrincipalFrame().setVisible(true);
                dispose();
            }
        } catch (SQLException ex) {
            UIUtils.mostrarError(this, "No se pudo conectar a SQL Server:\n" + ex.getMessage());
        }
    }
}