package nsu.security.demoapplication.repository;

import nsu.security.demoapplication.model.Order;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderJdbcRepository {

    private static final String URL = "jdbc:postgresql://db:5432/demodb";
    private static final String USER = "demo";
    private static final String PASSWORD = "demo";

    public List<Order> getOrders(String id) {

        Connection connection = null;
        Statement statement = null;
        PreparedStatement preparedStatement = null;
        ResultSet results = null;

        List<Order> orders = new ArrayList<>();

        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);

            // String sql = "SELECT * FROM orders WHERE id = " + id;
            // statement = connection.createStatement();
            // results = statement.executeQuery(sql);

            String sql = "SELECT * FROM orders WHERE id = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setLong(1, Long.parseLong(id));
            results = preparedStatement.executeQuery();

            while (results.next()) {
                Long orderId = results.getLong("id");
                String productType = results.getString("product_type");
                String quantity = results.getString("quantity");

                Order order = new Order(Order.ProductType.valueOf(productType), quantity);
                order.setId(orderId);
                orders.add(order);
            }

            return orders;

        } catch (SQLException e) {
            throw new RuntimeException("Не удалось получить заказы через JDBC", e);

        } finally {
            closeQuietly(results);
            closeQuietly(statement);
            closeQuietly(preparedStatement);
            closeQuietly(connection);
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                // намеренно игнорируем ошибку закрытия ресурса
            }
        }
    }
}