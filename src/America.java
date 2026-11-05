import java.sql.*;
import java.util.Scanner;

public class America {

    private static final String URL  = "jdbc:mysql://localhost:3306/America?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = "root";
    private static final String SEP  = " | ";

    public static void main(String[] args) {
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             Scanner sc = new Scanner(System.in)) {

            crearTablaPersonasPaises(con);
            actualizarEdades(con);
            listarPersonasPaises(con);
            menu(con, sc);

        } catch (SQLException e) {
            System.err.println("Error de base de datos: " + e.getMessage());
        }
    }

    private static void crearTablaPersonasPaises(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("DROP TABLE IF EXISTS PersonasPaises");
            st.executeUpdate(
                    "CREATE TABLE PersonasPaises (" +
                            "Id INT NOT NULL, " +
                            "Nombre VARCHAR(15), " +
                            "Apellido VARCHAR(15), " +
                            "Edad INT, " +
                            "NombrePais VARCHAR(15), " +
                            "Tamanio VARCHAR(15)" +
                            ")");

            int filas = st.executeUpdate(
                    "INSERT INTO PersonasPaises (Id, Nombre, Apellido, Edad, NombrePais, Tamanio) " +
                            "SELECT p.Id, p.Nombre, p.Apellido, p.Edad, c.Nombre, c.Tamanio " +
                            "FROM Persona p JOIN Pais c ON p.Pais = c.Id");
            System.out.println("Tabla PersonasPaises creada con " + filas + " filas.\n");
        }
    }

    private static void actualizarEdades(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            int filas = st.executeUpdate("UPDATE PersonasPaises SET Edad = Edad + 1 WHERE NombrePais = 'Costa Rica'");
            System.out.println(filas + " personas de Costa Rica actualizadas.\n");
        }
    }

    private static void listarPersonasPaises(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM PersonasPaises")) {
            imprimir(rs);
        }
        System.out.println();
    }

    private static void menu(Connection con, Scanner sc) {
        int opcion;
        do {
            System.out.println("""
                    MENÚ
                    1. Consultar la edad de una persona
                    2. Listar personas de una edad
                    3. Listar personas por orden alfabético
                    4. Insertar una persona
                    5. Salir""");
            System.out.print("Opción: ");
            try {
                opcion = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                opcion = 0;
            }

            try {
                switch (opcion) {
                    case 1 -> consultarEdad(con, sc);
                    case 2 -> listarPorEdad(con, sc);
                    case 3 -> listarAlfabetico(con);
                    case 4 -> insertarPersona(con, sc);
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

    private static void consultarEdad(Connection con, Scanner sc) throws SQLException {
        System.out.print("Nombre de la persona: ");
        String nombre = sc.nextLine().trim();
        try (PreparedStatement ps = con.prepareStatement("SELECT Nombre, Apellido, Edad FROM Persona WHERE Nombre = ?")) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (!imprimir(rs)) System.out.println("No existe esa persona.");
            }
        }
    }

    private static void listarPorEdad(Connection con, Scanner sc) throws SQLException {
        System.out.print("Edad: ");
        int edad = Integer.parseInt(sc.nextLine().trim());
        try (PreparedStatement ps = con.prepareStatement("SELECT Id, Nombre, Apellido, Edad FROM Persona WHERE Edad = ?")) {
            ps.setInt(1, edad);
            try (ResultSet rs = ps.executeQuery()) {
                if (!imprimir(rs)) System.out.println("No hay personas con esa edad.");
            }
        }
    }

    private static void listarAlfabetico(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT Id, Nombre, Apellido, Edad FROM Persona ORDER BY Nombre")) {
            imprimir(rs);
        }
    }

    private static void insertarPersona(Connection con, Scanner sc) throws SQLException {
        System.out.print("Id: ");
        int id = Integer.parseInt(sc.nextLine().trim());
        System.out.print("Nombre: ");
        String nombre = sc.nextLine().trim();
        System.out.print("Apellido: ");
        String apellido = sc.nextLine().trim();
        System.out.print("Edad: ");
        int edad = Integer.parseInt(sc.nextLine().trim());
        System.out.print("Id del país: ");
        int pais = Integer.parseInt(sc.nextLine().trim());
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO Persona (Id, Nombre, Apellido, Edad, Pais) VALUES (?, ?, ?, ?, ?)")) {
            ps.setInt(1, id);
            ps.setString(2, nombre);
            ps.setString(3, apellido);
            ps.setInt(4, edad);
            ps.setInt(5, pais);
            System.out.println(ps.executeUpdate() + " persona insertada.");
        }
    }

    private static boolean imprimir(ResultSet rs) throws SQLException {
        int n = rs.getMetaData().getColumnCount();
        StringBuilder fila = new StringBuilder();
        for (int i = 1; i <= n; i++) {
            if (i > 1) fila.append(SEP);
            fila.append(rs.getMetaData().getColumnLabel(i));
        }
        System.out.println(fila);
        boolean hay = false;
        while (rs.next()) {
            hay = true;
            fila.setLength(0);
            for (int i = 1; i <= n; i++) {
                if (i > 1) fila.append(SEP);
                fila.append(rs.getString(i));
            }
            System.out.println(fila);
        }
        return hay;
    }
}