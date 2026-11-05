import java.sql.*;

public class Deportes {

    private static final String URL  = "jdbc:mysql://localhost:3306/deportes?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = "root";
    private static final String SEP  = " | ";

    public static void main(String[] args) {
        try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {

            crearTablaClasificacion(con);
            insertarClasificacion(con);

            consulta(con, "JUGADORES DEL EQUIPO C",
                    "SELECT j.nombre_jugador, j.posicion, j.edad " +
                            "FROM jugadores j JOIN equipos e ON j.id_equipo = e.id_equipo " +
                            "WHERE e.nombre_equipo = 'Equipo C'");

            consulta(con, "RESULTADOS DE LOS PARTIDOS",
                    "SELECT el.nombre_equipo AS local, ev.nombre_equipo AS visitante, p.fecha_partido, p.resultado " +
                            "FROM partidos p " +
                            "JOIN equipos el ON p.equipo_local_id = el.id_equipo " +
                            "JOIN equipos ev ON p.equipo_visitante_id = ev.id_equipo");

            consulta(con, "CLASIFICACIÓN ACTUAL",
                    "SELECT c.posicion, e.nombre_equipo, c.puntos " +
                            "FROM clasificacion c JOIN equipos e ON c.id_equipo = e.id_equipo " +
                            "ORDER BY c.posicion");

        } catch (SQLException e) {
            System.err.println("Error de base de datos: " + e.getMessage());
        }
    }

    private static void crearTablaClasificacion(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("DROP TABLE IF EXISTS clasificacion");
            st.executeUpdate(
                    "CREATE TABLE clasificacion (" +
                            "id_clasificacion INT AUTO_INCREMENT PRIMARY KEY, " +
                            "id_equipo INT NOT NULL, " +
                            "posicion INT NOT NULL, " +
                            "puntos INT NOT NULL, " +
                            "FOREIGN KEY (id_equipo) REFERENCES equipos(id_equipo)" +
                            ")");
            System.out.println("Tabla clasificacion creada.\n");
        }
    }

    private static void insertarClasificacion(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            int filas = st.executeUpdate(
                    "INSERT INTO clasificacion (id_equipo, posicion, puntos) VALUES " +
                            "(1, 1, 20), (2, 2, 15), (3, 3, 12), (4, 4, 10), (5, 5, 8)");
            System.out.println(filas + " filas insertadas en clasificacion.\n");
        }
    }

    private static void consulta(Connection con, String titulo, String sql) throws SQLException {
        System.out.println(titulo);
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            int n = rs.getMetaData().getColumnCount();
            StringBuilder fila = new StringBuilder();
            for (int i = 1; i <= n; i++) {
                if (i > 1) fila.append(SEP);
                fila.append(rs.getMetaData().getColumnLabel(i));
            }
            System.out.println(fila);
            while (rs.next()) {
                fila.setLength(0);
                for (int i = 1; i <= n; i++) {
                    if (i > 1) fila.append(SEP);
                    fila.append(rs.getString(i));
                }
                System.out.println(fila);
            }
        }
        System.out.println();
    }
}