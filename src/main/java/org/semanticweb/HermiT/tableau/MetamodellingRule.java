package org.semanticweb.HermiT.tableau;

/**
 * Interfaz para las reglas de metamodelado.
 * Cada regla implementa la lógica específica para un tipo de inferencia de metamodelado.
 */
public interface MetamodellingRule {

    /**
     * Aplica la regla de metamodelado al tableau.
     *
     * @param tableau El tableau sobre el cual aplicar la regla
     * @return true si la regla fue aplicada exitosamente, false en caso contrario
     */
    boolean apply(Tableau tableau);

    /**
     * Obtiene el nombre descriptivo de la regla.
     *
     * @return El nombre de la regla
     */
    String getRuleName();

    /**
     * Verifica si la regla es aplicable en el estado actual del tableau.
     *
     * @param tableau El tableau a verificar
     * @return true si la regla es aplicable, false en caso contrario
     */
    boolean isApplicable(Tableau tableau);
}
