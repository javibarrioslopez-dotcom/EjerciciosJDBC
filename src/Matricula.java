import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Matricula {

    private static final String URL  = "jdbc:mysql://localhost:3306/matricula?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = "root";
    private static final String SEP  = " | ";

    public static void main(String[] args) {
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             Scanner sc = new Scanner(System.in)) {

            menu(con, sc);

        } catch (SQLException e) {
            System.err.println("Error de base de datos: " + e.getMessage());
        }
    }

    private static void menu(Connection con, Scanner sc) {
        int opcion;
        do {
            System.out.println("""
                    MENÚ
                    1. Crear alumno
                    2. Buscar alumno
                    3. Borrar alumno
                    4. Matricular alumno en asignaturas
                    5. Consultar asignaturas de un alumno
                    6. Salir""");
            opcion = leerOpcion(sc);

            try {
                switch (opcion) {
                    case 1 -> altaAlumno(con, leer(sc, "ID: "), leer(sc, "Apellidos: "), leer(sc, "Nombre: "),
                            leer(sc, "Curso: "), leer(sc, "Titulación: "));
                    case 2 -> mostrarAlumno(con, leerInt(sc, "ID del alumno: "));
                    case 3 -> eliminarAlumno(con, leerInt(sc, "ID del alumno: "));
                    case 4 -> matricular(con, leerInt(sc, "ID del alumno: "), leerAsignaturas(sc));
                    case 5 -> consultarAsignaturas(con, leerInt(sc, "ID del alumno: "));
                    case 6 -> System.out.println("Adiós.");
                    default -> System.out.println("Opción no válida.");
                }
            } catch (SQLException e) {
                System.err.println("Error SQL: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.err.println("Valor numérico no válido.");
            }
            System.out.println();
        } while (opcion != 6);
    }

    private static int leerOpcion(Scanner sc) {
        try {
            return Integer.parseInt(leer(sc, "Opción: "));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String leer(Scanner sc, String texto) {
        System.out.print(texto);
        return sc.nextLine().trim();
    }

    private static int leerInt(Scanner sc, String texto) {
        return Integer.parseInt(leer(sc, texto));
    }

    private static List<Integer> leerAsignaturas(Scanner sc) {
        List<Integer> lista = new ArrayList<>();
        for (String s : leer(sc, "IDs de asignaturas separados por comas: ").split(",")) {
            lista.add(Integer.parseInt(s.trim()));
        }
        return lista;
    }

    private static void altaAlumno(Connection con, String id, String apellidos, String nombre,
                                   String curso, String titulacion) throws SQLException {
        if (id.isEmpty() || apellidos.isEmpty() || nombre.isEmpty() || curso.isEmpty() || titulacion.isEmpty()) {
            System.out.println("Faltan datos. No se ha dado de alta el alumno.");
            return;
        }
        if (apellidos.length() > 24 || nombre.length() > 18) {
            System.out.println("Apellidos (máx. 24) o nombre (máx. 18) demasiado largos.");
            return;
        }
        int idAlumno = Integer.parseInt(id);
        int cursoAlumno = Integer.parseInt(curso);
        int titulacionAlumno = Integer.parseInt(titulacion);
        String sql = "INSERT INTO alumnos (id_alumno, apellidos, nombre, curso, titulacion) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idAlumno);
            ps.setString(2, apellidos);
            ps.setString(3, nombre);
            ps.setInt(4, cursoAlumno);
            ps.setInt(5, titulacionAlumno);
            ps.executeUpdate();
            System.out.println("Alumno dado de alta.");
        }
    }

    private static void eliminarAlumno(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM alumnos WHERE id_alumno = ?")) {
            ps.setInt(1, id);
            System.out.println(ps.executeUpdate() > 0 ? "Alumno eliminado." : "Error: no existe el alumno.");
        }
    }

    private static void matricular(Connection con, int idAlumno, List<Integer> asignaturas) throws SQLException {
        if (!existe(con, "SELECT 1 FROM alumnos WHERE id_alumno = ?", idAlumno)) {
            System.out.println("Error: no existe el alumno.");
            return;
        }
        String insertar = "INSERT INTO alumnos_asignaturas (id_alumno, id_asignatura, cursada) VALUES (?, ?, 1) " +
                "ON DUPLICATE KEY UPDATE cursada = 1";
        for (int asignatura : asignaturas) {
            if (!existe(con, "SELECT 1 FROM asignaturas WHERE id_asignatura = ?", asignatura)) {
                System.out.println("No existe la asignatura " + asignatura + ".");
            } else if (existe(con, "SELECT 1 FROM alumnos_asignaturas WHERE id_alumno = ? AND id_asignatura = ? AND cursada = 1",
                    idAlumno, asignatura)) {
                System.out.println("El alumno ya está matriculado en la asignatura " + asignatura + ".");
            } else {
                try (PreparedStatement ps = con.prepareStatement(insertar)) {
                    ps.setInt(1, idAlumno);
                    ps.setInt(2, asignatura);
                    ps.executeUpdate();
                    System.out.println("Matriculado en la asignatura " + asignatura + ".");
                }
            }
        }
    }

    private static void consultarAsignaturas(Connection con, int id) throws SQLException {
        if (!mostrarAlumno(con, id)) return;

        System.out.println("ASIGNATURAS QUE CURSA");
        listarAsignaturas(con, id,
                "SELECT a.id_asignatura, a.nombre FROM asignaturas a " +
                        "JOIN alumnos_asignaturas aa ON a.id_asignatura = aa.id_asignatura " +
                        "WHERE aa.id_alumno = ? AND aa.cursada = 1");

        System.out.println("ASIGNATURAS QUE NO CURSA");
        listarAsignaturas(con, id,
                "SELECT id_asignatura, nombre FROM asignaturas WHERE id_asignatura NOT IN " +
                        "(SELECT id_asignatura FROM alumnos_asignaturas WHERE id_alumno = ? AND cursada = 1)");
    }

    private static boolean mostrarAlumno(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM alumnos WHERE id_alumno = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Error: no existe el alumno.");
                    return false;
                }
                System.out.println("ID | APELLIDOS | NOMBRE | CURSO | TITULACION");
                System.out.println(rs.getInt("id_alumno") + SEP + rs.getString("apellidos") + SEP +
                        rs.getString("nombre") + SEP + rs.getInt("curso") + SEP + rs.getInt("titulacion"));
                return true;
            }
        }
    }

    private static void listarAsignaturas(Connection con, int id, String sql) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                boolean hay = false;
                while (rs.next()) {
                    hay = true;
                    System.out.println(rs.getInt("id_asignatura") + SEP + rs.getString("nombre"));
                }
                if (!hay) System.out.println("Ninguna.");
            }
        }
    }

    private static boolean existe(Connection con, String sql, int... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setInt(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}