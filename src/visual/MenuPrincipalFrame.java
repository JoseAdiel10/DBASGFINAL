package visual;

import java.awt.Component;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

public class MenuPrincipalFrame extends JFrame {

    private final PanelEstudiante panelEstudiante = new PanelEstudiante();
    private final PanelAsignatura panelAsignatura = new PanelAsignatura();
    private final PanelHorarioGrupo panelHorarioGrupo = new PanelHorarioGrupo();
    private final PanelInscripcion panelInscripcion = new PanelInscripcion();
    private final PanelHorarioCuadriculado panelCuadriculado = new PanelHorarioCuadriculado();
    private final PanelInforme panelInforme = new PanelInforme();

    /**
     * Construye el contenedor principal con un JTabbedPane para navegar entre
     * todos los módulos del sistema y recarga los datos cada vez que se cambia de pestaña.
     */
    public MenuPrincipalFrame() {
        setTitle("Sistema Académico - Asignación Final");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1080, 680);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("1A. CRUD Estudiantes", panelEstudiante);
        tabs.addTab("1B. CRUD Asignaturas", panelAsignatura);
        tabs.addTab("2. Horarios de Grupo", panelHorarioGrupo);
        tabs.addTab("3. Inscripción de Grupos", panelInscripcion);
        tabs.addTab("4. Horario Cuadriculado", panelCuadriculado);
        tabs.addTab("5. Informe de Inscripción", panelInforme);

        tabs.addChangeListener(e -> recargarPestanaActiva(tabs.getSelectedComponent()));

        add(tabs);
        panelEstudiante.recargarTodo();
    }

    /**
     * Refresca los catálogos y tablas de la pestaña seleccionada para reflejar
     * inmediatamente cualquier cambio hecho en otros módulos.
     */
    private void recargarPestanaActiva(Component sel) {
        if (sel == panelEstudiante) {
            panelEstudiante.recargarTodo();
        } else if (sel == panelAsignatura) {
            panelAsignatura.cargarTabla();
        } else if (sel == panelHorarioGrupo) {
            panelHorarioGrupo.recargarCatalogos();
        } else if (sel == panelInscripcion) {
            panelInscripcion.recargarCatalogos();
        } else if (sel == panelCuadriculado) {
            panelCuadriculado.recargarCatalogos();
        } else if (sel == panelInforme) {
            panelInforme.recargarCatalogos();
        }
    }
}