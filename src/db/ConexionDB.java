package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {

    private static String url;
    private static String usuario;
    private static String password;

    /**
- Valida las credenciales de acceso contra SQL Server abriendo una conexión de prueba.
- Si la conexión tiene éxito, guarda la cadena URL y las credenciales en memoria
- para reutilizarlas en el resto de los módulos.
     */
    public static boolean autenticar(String servidor, String puerto, String baseDatos, String user, String pass)
            throws SQLException {
        String nuevaUrl = "jdbc:sqlserver://" + servidor + ":" + puerto + ";" +
                "databaseName=" + baseDatos + ";" +
                "encrypt=true;trustServerCertificate=true;";

        try (Connection con = DriverManager.getConnection(nuevaUrl, user, pass)) {
            url = nuevaUrl;
            usuario = user;
            password = pass;
            return con != null && !con.isClosed();
        }
    }

    /**
- Devuelve una nueva conexión activa a SQL Server utilizando los datos
- validados previamente en el login.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, usuario, password);
    }
}