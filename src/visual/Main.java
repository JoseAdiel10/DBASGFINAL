package visual;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    /**
     * Punto de entrada principal de la aplicación.
     * Configura el aspecto visual nativo de Windows y lanza la ventana de login
     * en el hilo de eventos de Swing (Event Dispatch Thread).
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}