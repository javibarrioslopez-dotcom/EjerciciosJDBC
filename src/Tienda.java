import java.sql.*;
import java.util.Scanner;

public class Tienda {

    private static final String URL  = "jdbc:mysql://localhost:3306/tienda1?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = "root";
    private static final String SEP  = " | ";

    public static void main(String[] args) {
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             Scanner sc = new Scanner(System.in)) {

            crearTablaArtFab(con);
            listarArtFab(con);
            menu(con, sc);

        } catch (SQLException e) {
            System.err.println("Error de base de datos: " + e.getMessage());
        }
    }

    private static void crearTablaArtFab(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("DROP TABLE IF EXISTS ArtFab");
            st.executeUpdate(
                    "CREATE TABLE ArtFab (" +
                            "nombre_articulo VARCHAR(30) NOT NULL, " +
                            "nombre_fabricante VARCHAR(30) NOT NULL, " +
                            "precio INT NOT NULL, " +
                            "iva DECIMAL(10,2) NOT NULL" +
                            ")");

            int filas = st.executeUpdate(
                    "INSERT INTO ArtFab (nombre_articulo, nombre_fabricante, precio, iva) " +
                            "SELECT a.Nombre, f.Nombre, a.Precio, " +
                            "CASE " +
                            "WHEN a.Precio < 200 THEN a.Precio * 0.10 " +
                            "WHEN a.Precio < 500 THEN a.Precio * 0.08 " +
                            "WHEN a.Precio < 700 THEN a.Precio * 0.06 " +
                            "ELSE 0 " +
                            "END " +
                            "FROM Articulos a JOIN Fabricantes f ON a.CLFAB = f.CLFAB");
            System.out.println("Tabla ArtFab creada con " + filas + " filas.\n");
        }
    }

    private static void listarArtFab(Connection con) throws SQLException {
        System.out.println("ARTICULO" + SEP + "FABRICANTE" + SEP + "PRECIO" + SEP + "IVA");
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM ArtFab")) {
            while (rs.next()) {
                System.out.println(
                        rs.getString("nombre_articulo") + SEP +
                                rs.getString("nombre_fabricante") + SEP +
                                rs.getInt("precio") + SEP +
                                String.format("%.2f", rs.getDouble("iva")));
            }
        }
        System.out.println();
    }

    private static void menu(Connection con, Scanner sc) {
        int opcion;
        do {
            System.out.println("""
                    MENÚ
                    1. Listar artículos con precio menor que
                    2. Agregar un fabricante
                    3. Reducir precio de 'Disco Duro 300Gb'
                    4. Eliminar un artículo
                    5. Salir""");
            System.out.print("Opción: ");
            try {
                opcion = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                opcion = 0;
            }

            try {
                switch (opcion) {
                    case 1 -> listarMenoresQue(con, sc);
                    case 2 -> agregarFabricante(con, sc);
                    case 3 -> reducirPrecio(con, sc);
                    case 4 -> eliminarArticulo(con, sc);
                    case 5 -> System.out.println("Adiós.");
                    default -> System.out.println("Opción no válida.");
                }
            } catch (SQLException e) {
                System.err.println("Error SQL: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.err.println("Valor numérico no válido.");
            }
            System.out.println();
        } while (opcion != 5);
    }

    private static void listarMenoresQue(Connection con, Scanner sc) throws SQLException {
        System.out.print("Precio máximo (exclusivo): ");
        int max = Integer.parseInt(sc.nextLine().trim());
        String sql = "SELECT Nombre, Precio FROM Articulos WHERE Precio < ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, max);
            try (ResultSet rs = ps.executeQuery()) {
                boolean hay = false;
                System.out.println("ARTICULO" + SEP + "PRECIO");
                while (rs.next()) {
                    hay = true;
                    System.out.println(rs.getString("Nombre") + SEP + rs.getInt("Precio"));
                }
                if (!hay) System.out.println("No hay artículos con ese criterio.");
            }
        }
    }

    private static void agregarFabricante(Connection con, Scanner sc) throws SQLException {
        System.out.print("Nombre del fabricante: ");
        String nombre = sc.nextLine().trim();
        if (nombre.isEmpty() || nombre.length() > 30) {
            System.out.println("El nombre no puede estar vacío ni superar 30 caracteres.");
            return;
        }
        String sql = "INSERT INTO Fabricantes (CLFAB, Nombre) " +
                "SELECT COALESCE(MAX(CLFAB), 0) + 1, ? FROM Fabricantes";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            System.out.println(ps.executeUpdate() + " fabricante insertado.");
        }
    }

    private static void reducirPrecio(Connection con, Scanner sc) throws SQLException {
        System.out.print("Porcentaje de reducción (0-100): ");
        double pct = Double.parseDouble(sc.nextLine().trim().replace(',', '.'));
        if (pct < 0 || pct > 100) {
            System.out.println("El porcentaje debe estar entre 0 y 100.");
            return;
        }
        String sql = "UPDATE Articulos SET Precio = ROUND(Precio * (1 - ? / 100)) WHERE Nombre = 'Disco Duro 300Gb'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, pct);
            int n = ps.executeUpdate();
            System.out.println(n > 0 ? "Precio actualizado." : "No existe el artículo 'Disco Duro 300Gb'.");
        }
    }

    private static void eliminarArticulo(Connection con, Scanner sc) throws SQLException {
        System.out.print("Nombre del artículo a eliminar: ");
        String nombre = sc.nextLine().trim();
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM Articulos WHERE Nombre = ?")) {
            ps.setString(1, nombre);
            int n = ps.executeUpdate();
            System.out.println(n > 0 ? n + " artículo(s) eliminado(s)." : "No existe ese artículo.");
        }
    }
}