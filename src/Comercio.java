import java.sql.*;
import java.util.Scanner;

public class Comercio {

    private static final String URL  = "jdbc:mysql://localhost:3306/comercio?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
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
                    1. Crear pedido
                    2. Pedidos de un cliente
                    3. Detalles de un pedido
                    4. Stock de un producto
                    5. Añadir producto a un pedido
                    6. Cambiar cantidad de un artículo
                    7. Eliminar artículo de un pedido
                    8. Eliminar pedido
                    9. Salir""");
            System.out.print("Opción: ");
            try {
                opcion = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                opcion = 0;
            }

            try {
                switch (opcion) {
                    case 1 -> crearPedido(con, sc);
                    case 2 -> pedidosCliente(con, sc);
                    case 3 -> mostrarDetalle(con, leerInt(sc, "ID del pedido: "));
                    case 4 -> stockProducto(con, sc);
                    case 5 -> anadirProducto(con, sc);
                    case 6 -> cambiarCantidad(con, sc);
                    case 7 -> eliminarArticulo(con, sc);
                    case 8 -> eliminarPedido(con, sc);
                    case 9 -> System.out.println("Adiós.");
                    default -> System.out.println("Opción no válida.");
                }
            } catch (SQLException e) {
                System.err.println("Error SQL: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.err.println("Valor numérico no válido.");
            }
            System.out.println();
        } while (opcion != 9);
    }

    private static int leerInt(Scanner sc, String texto) {
        System.out.print(texto);
        return Integer.parseInt(sc.nextLine().trim());
    }

    private static int actualizar(Connection con, String sql, int... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setInt(i + 1, params[i]);
            }
            return ps.executeUpdate();
        }
    }

    private static void agregarLinea(Connection con, int pedido, int producto, int cantidad) throws SQLException {
        if (cantidad <= 0) throw new SQLException("Cantidad no válida.");
        if (actualizar(con, "UPDATE productos SET stock = stock - ? WHERE productoID = ? AND stock >= ?",
                cantidad, producto, cantidad) == 0) {
            throw new SQLException("El producto no existe o no hay stock suficiente.");
        }
        actualizar(con, "INSERT INTO detalle_pedidos (pedidoID, productoID, cantidad, precio_unidad) " +
                "SELECT ?, productoID, ?, precio FROM productos WHERE productoID = ?", pedido, cantidad, producto);
    }

    private static int cantidadActual(Connection con, int pedido, int producto) throws SQLException {
        String sql = "SELECT cantidad FROM detalle_pedidos WHERE pedidoID = ? AND productoID = ? FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pedido);
            ps.setInt(2, producto);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("El pedido no contiene ese producto.");
                return rs.getInt(1);
            }
        }
    }

    private static void crearPedido(Connection con, Scanner sc) throws SQLException {
        int cliente = leerInt(sc, "ID del cliente: ");
        int pedido;
        con.setAutoCommit(false);
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO pedidos (clienteID, fecha) VALUES (?, CURDATE())", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, cliente);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    pedido = rs.getInt(1);
                }
            }
            int lineas = 0;
            while (true) {
                int producto = leerInt(sc, "ID del producto (0 para terminar): ");
                if (producto == 0) break;
                int cantidad = leerInt(sc, "Cantidad: ");
                agregarLinea(con, pedido, producto, cantidad);
                lineas++;
            }
            if (lineas == 0) throw new SQLException("El pedido no tiene productos.");
            con.commit();
            System.out.println("Pedido " + pedido + " creado.");
            mostrarDetalle(con, pedido);
        } catch (SQLException | RuntimeException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    private static void pedidosCliente(Connection con, Scanner sc) throws SQLException {
        int cliente = leerInt(sc, "ID del cliente: ");
        try (PreparedStatement ps = con.prepareStatement("SELECT pedidoID, fecha FROM pedidos WHERE clienteID = ?")) {
            ps.setInt(1, cliente);
            try (ResultSet rs = ps.executeQuery()) {
                boolean hay = false;
                System.out.println("PEDIDO | FECHA");
                while (rs.next()) {
                    hay = true;
                    System.out.println(rs.getInt("pedidoID") + SEP + rs.getString("fecha"));
                }
                if (!hay) System.out.println("El cliente no tiene pedidos.");
            }
        }
    }

    private static void mostrarDetalle(Connection con, int pedido) throws SQLException {
        String sql = "SELECT p.nombre, d.cantidad, d.precio_unidad FROM detalle_pedidos d " +
                "JOIN productos p ON d.productoID = p.productoID WHERE d.pedidoID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pedido);
            try (ResultSet rs = ps.executeQuery()) {
                boolean hay = false;
                double total = 0;
                System.out.println("PRODUCTO | CANTIDAD | PRECIO | SUBTOTAL");
                while (rs.next()) {
                    hay = true;
                    double subtotal = rs.getInt("cantidad") * rs.getDouble("precio_unidad");
                    total += subtotal;
                    System.out.println(rs.getString("nombre") + SEP + rs.getInt("cantidad") + SEP +
                            String.format("%.2f", rs.getDouble("precio_unidad")) + SEP + String.format("%.2f", subtotal));
                }
                if (hay) {
                    System.out.println("TOTAL" + SEP + String.format("%.2f", total));
                } else {
                    System.out.println("No hay artículos para ese pedido.");
                }
            }
        }
    }

    private static void stockProducto(Connection con, Scanner sc) throws SQLException {
        int producto = leerInt(sc, "ID del producto: ");
        try (PreparedStatement ps = con.prepareStatement("SELECT nombre, stock FROM productos WHERE productoID = ?")) {
            ps.setInt(1, producto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println(rs.getString("nombre") + SEP + rs.getInt("stock"));
                } else {
                    System.out.println("No existe el producto.");
                }
            }
        }
    }

    private static void anadirProducto(Connection con, Scanner sc) throws SQLException {
        int pedido = leerInt(sc, "ID del pedido: ");
        int producto = leerInt(sc, "ID del producto: ");
        int cantidad = leerInt(sc, "Cantidad: ");
        con.setAutoCommit(false);
        try {
            agregarLinea(con, pedido, producto, cantidad);
            con.commit();
            System.out.println("Producto añadido.");
        } catch (SQLException | RuntimeException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    private static void cambiarCantidad(Connection con, Scanner sc) throws SQLException {
        int pedido = leerInt(sc, "ID del pedido: ");
        int producto = leerInt(sc, "ID del producto: ");
        int nueva = leerInt(sc, "Nueva cantidad: ");
        con.setAutoCommit(false);
        try {
            if (nueva <= 0) throw new SQLException("Cantidad no válida.");
            int diferencia = nueva - cantidadActual(con, pedido, producto);
            if (actualizar(con, "UPDATE productos SET stock = stock - ? WHERE productoID = ? AND stock >= ?",
                    diferencia, producto, diferencia) == 0) {
                throw new SQLException("No hay stock suficiente.");
            }
            actualizar(con, "UPDATE detalle_pedidos SET cantidad = ? WHERE pedidoID = ? AND productoID = ?",
                    nueva, pedido, producto);
            con.commit();
            System.out.println("Cantidad actualizada.");
        } catch (SQLException | RuntimeException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    private static void eliminarArticulo(Connection con, Scanner sc) throws SQLException {
        int pedido = leerInt(sc, "ID del pedido: ");
        int producto = leerInt(sc, "ID del producto: ");
        con.setAutoCommit(false);
        try {
            int actual = cantidadActual(con, pedido, producto);
            actualizar(con, "UPDATE productos SET stock = stock + ? WHERE productoID = ?", actual, producto);
            actualizar(con, "DELETE FROM detalle_pedidos WHERE pedidoID = ? AND productoID = ?", pedido, producto);
            con.commit();
            System.out.println("Artículo eliminado.");
        } catch (SQLException | RuntimeException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    private static void eliminarPedido(Connection con, Scanner sc) throws SQLException {
        int pedido = leerInt(sc, "ID del pedido: ");
        con.setAutoCommit(false);
        try {
            actualizar(con, "UPDATE productos p JOIN detalle_pedidos d ON p.productoID = d.productoID " +
                    "SET p.stock = p.stock + d.cantidad WHERE d.pedidoID = ?", pedido);
            actualizar(con, "DELETE FROM detalle_pedidos WHERE pedidoID = ?", pedido);
            if (actualizar(con, "DELETE FROM pedidos WHERE pedidoID = ?", pedido) == 0) {
                throw new SQLException("No existe el pedido.");
            }
            con.commit();
            System.out.println("Pedido eliminado.");
        } catch (SQLException | RuntimeException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }
}