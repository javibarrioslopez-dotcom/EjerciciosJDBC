import java.sql.*;

public class Instituto {

    private static final String URL  = "jdbc:mysql://localhost:3306/Instituto?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = "root";
    private static final String SEP  = " | ";

    public static void main(String[] args) {
        try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {

            crearTablaNotasFinales(con);
            listarAlumnos(con);

        } catch (SQLException e) {
            System.err.println("Error de base de datos: " + e.getMessage());
        }
    }

    private static void crearTablaNotasFinales(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("DROP TABLE IF EXISTS notasfinales");
            st.executeUpdate(
                    "CREATE TABLE notasfinales (" +
                            "Mat VARCHAR(10) NOT NULL, " +
                            "Cod INT NOT NULL, " +
                            "NotaMedia DECIMAL(4,2) NOT NULL" +
                            ")");

            int filas = st.executeUpdate(
                    "INSERT INTO notasfinales (Mat, Cod, NotaMedia) " +
                            "SELECT Mat, COD, (NOTA1 + NOTA2 + NOTA3) / 3 FROM NOTAS");
            System.out.println("Tabla notasfinales creada con " + filas + " filas.\n");
        }
    }

    private static void listarAlumnos(Connection con) throws SQLException {
        String sql = "SELECT a.APEL_NOM AS NombreAlumno, a.POBLACION AS Localidad, s.NOMBRE AS Asignatura, " +
                "n.NOTA1 AS `Nota 1`, n.NOTA2 AS `Nota 2`, nf.NotaMedia AS NotaMedia " +
                "FROM ALUMNOS a " +
                "JOIN NOTAS n ON a.MAT = n.Mat " +
                "JOIN ASIGNATURAS s ON n.COD = s.COD " +
                "JOIN notasfinales nf ON nf.Mat = n.Mat AND nf.Cod = n.COD";
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            imprimir(rs);
        }
        System.out.println();
    }

    private static void imprimir(ResultSet rs) throws SQLException {
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
}