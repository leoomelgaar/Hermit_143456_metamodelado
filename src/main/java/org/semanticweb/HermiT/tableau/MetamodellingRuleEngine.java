package org.semanticweb.HermiT.tableau;

import java.util.ArrayList;
import java.util.List;

/**
 * Engine que coordina la ejecución de las reglas de metamodelado.
 * Permite agregar, configurar y ejecutar múltiples reglas de forma ordenada.
 */
public class MetamodellingRuleEngine {

    private List<MetamodellingRule> rules;

    public MetamodellingRuleEngine() {
        this.rules = new ArrayList<>();
    }

    /**
     * Agrega una regla al engine.
     *
     * @param rule La regla a agregar
     */
    public void addRule(MetamodellingRule rule) {
        if (rule != null) {
            rules.add(rule);
        }
    }

    /**
     * Ejecuta todas las reglas aplicables en el tableau.
     *
     * @param tableau El tableau sobre el cual aplicar las reglas
     * @return true si al menos una regla fue aplicada, false en caso contrario
     */
    public boolean applyAllRules(Tableau tableau) {
        boolean anyApplied = false;

        for (MetamodellingRule rule : rules) {
            if (rule.isApplicable(tableau)) {
                if (rule.apply(tableau)) {
                    anyApplied = true;
                }
            }
        }

        return anyApplied;
    }

    /**
     * Ejecuta una regla específica por nombre.
     *
     * @param ruleName El nombre de la regla a ejecutar
     * @param tableau El tableau sobre el cual aplicar la regla
     * @return true si la regla fue aplicada, false en caso contrario
     */
    public boolean applyRule(String ruleName, Tableau tableau) {
        for (MetamodellingRule rule : rules) {
            if (rule.getRuleName().equals(ruleName) && rule.isApplicable(tableau)) {
                return rule.apply(tableau);
            }
        }
        return false;
    }

    /**
     * Obtiene la lista de reglas registradas.
     *
     * @return Lista de reglas
     */
    public List<MetamodellingRule> getRules() {
        return new ArrayList<>(rules);
    }

    /**
     * Limpia todas las reglas del engine.
     */
    public void clearRules() {
        rules.clear();
    }

    /**
     * Obtiene el número de reglas registradas.
     *
     * @return Número de reglas
     */
    public int getRuleCount() {
        return rules.size();
    }
}
