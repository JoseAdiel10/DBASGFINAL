package visual;

import java.util.Objects;

public class ComboItem {

    private final String id;
    private final String descripcion;

    /**
     * Construye un elemento para JComboBox separando su clave primaria (id)
     * del texto descriptivo visible para el usuario.
     */
    public ComboItem(String id, String descripcion) {
        this.id = id;
        this.descripcion = descripcion;
    }

    /** Devuelve la clave o código almacenado en la base de datos. */
    public String getId() {
        return id;
    }

    /** Devuelve la descripción legible del elemento. */
    public String getDescripcion() {
        return descripcion;
    }

    /** Define el formato con el que se muestra el elemento dentro del JComboBox. */
    @Override
    public String toString() {
        return id + " - " + descripcion;
    }

    /** Compara dos ComboItem por su ID para permitir selecciones automáticas. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComboItem)) return false;
        ComboItem that = (ComboItem) o;
        return Objects.equals(id, that.id);
    }

    /** Genera el código hash basado en el ID del elemento. */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
