package edu.eci.arsw.blueprints;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import edu.eci.arsw.blueprints.config.AppConfig;
import edu.eci.arsw.blueprints.filters.BlueprintFilter;
import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintsPersistence;
import edu.eci.arsw.blueprints.services.BlueprintsServices;

public class Main {

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }

    private static String pointsOf(Blueprint bp) {
        var sb = new StringBuilder();
        sb.append(bp.toString()).append(" -> ");
        sb.append("[");
        var pts = bp.getPoints();
        for (int i = 0; i < pts.size(); i++) {
            Point p = pts.get(i);
            sb.append("(").append(p.getX()).append(",").append(p.getY()).append(")");
            if (i < pts.size() - 1) sb.append(", ");
        }
        sb.append("] (").append(pts.size()).append(" pts)");
        return sb.toString();
    }

    public static void main(String[] args) {
        try (var ctx = new AnnotationConfigApplicationContext(AppConfig.class)) {
            // eans base desde Spring
            BlueprintsServices svcDefault = ctx.getBean(BlueprintsServices.class); // usa el filtro @Primary (RedundancyFilter)
            BlueprintsPersistence persistence = ctx.getBean(BlueprintsPersistence.class);

            // Obtener el filtro de submuestreo y crear un servicio alterno con el mismo repositorio
            BlueprintFilter subsampling = ctx.getBean("subsampling", BlueprintFilter.class);
            BlueprintsServices svcSubsampling = new BlueprintsServices(persistence, subsampling);

            // Registrar datos de prueba (en la MISMA persistencia)
            // Plano con puntos consecutivos repetidos (ara ver el efecto del filtro de redundancias
            Point r = new Point(0, 0);
            Point s = new Point(5, 5);
            Blueprint bpDups = new Blueprint("john", "dups", new Point[]{ r, r, s, s, new Point(10,10) });

            // Plano con muchos puntos distintos (para ver claramente el submuestreo)
            Blueprint bpLong = new Blueprint("mike", "long",
                    new Point[]{
                            new Point(0,0), new Point(1,1), new Point(2,2),
                            new Point(3,3), new Point(4,4), new Point(5,5)
                    });

            // Otros dos planos normales
            Blueprint bp1 = new Blueprint("john", "house", new Point[]{ new Point(0,0), new Point(10,10) });
            Blueprint bp2 = new Blueprint("john", "car",   new Point[]{ new Point(5,5), new Point(15,15) });
            Blueprint bp3 = new Blueprint("anna", "garden", new Point[]{ new Point(3,3), new Point(7,7) });

            // Registrar usando el servicio por defecto igual quedan en la misma persistencia
            svcDefault.addNewBlueprint(bp1);
            svcDefault.addNewBlueprint(bp2);
            svcDefault.addNewBlueprint(bp3);
            svcDefault.addNewBlueprint(bpDups);
            svcDefault.addNewBlueprint(bpLong);

            // Mostrar con el FILTRO POR DEFECTO (RedundancyFilter @Primary)
            section("Filtro activo por defecto (RedundancyFilter)");
            System.out.println("-- TODOS --");
            svcDefault.getAllBlueprints().forEach(bp -> System.out.println(pointsOf(bp)));

            System.out.println("\n-- POR AUTOR: john --");
            svcDefault.getBlueprintsByAuthor("john").forEach(bp -> System.out.println(pointsOf(bp)));

            System.out.println("\n-- ESPECÍFICO: john:dups --");
            System.out.println(pointsOf(svcDefault.getBlueprint("john", "dups")));

            System.out.println("\n-- ESPECÍFICO: mike:long --");
            System.out.println(pointsOf(svcDefault.getBlueprint("mike", "long")));

            // Mostrar con el FILTRO DE SUBMUESTREO inyectado manualmente
            section("Filtro alterno (SubsamplingFilter)");
            System.out.println("-- TODOS --");
            svcSubsampling.getAllBlueprints().forEach(bp -> System.out.println(pointsOf(bp)));

            System.out.println("\n-- POR AUTOR: john --");
            svcSubsampling.getBlueprintsByAuthor("john").forEach(bp -> System.out.println(pointsOf(bp)));

            System.out.println("\n-- ESPECÍFICO: john:dups --");
            System.out.println(pointsOf(svcSubsampling.getBlueprint("john", "dups")));

            System.out.println("\n-- ESPECÍFICO: mike:long --");
            System.out.println(pointsOf(svcSubsampling.getBlueprint("mike", "long")));

            // Ejemplo de consulta inexistente
            try {
                svcDefault.getBlueprint("noone", "nothing");
            } catch (BlueprintNotFoundException ex) {
                System.out.println("\n[OK] No encontrado (esperado): " + ex.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
