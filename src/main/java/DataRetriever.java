import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class DataRetriever {

    // ==========================================
    // i) SAUVEGARDE DE COMMANDE AVEC STOCKS
    // ==========================================
    public Order saveOrder(Order orderToSave) {
        DBConnection dbConnection = new DBConnection();
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false); // Début transaction

            // 1. VÉRIFICATION DES STOCKS
            for (DishOrder dishOrder : orderToSave.getDishOrderList()) {
                // On recharge le plat pour avoir ses ingrédients via l'ID
                Dish dish = findDishById(dishOrder.getDish().getId());

                for (DishIngredient di : dish.getDishIngredients()) {
                    double requiredQty = di.getQuantity() * dishOrder.getQuantity();
                    double currentStock = getCurrentStock(conn, di.getIngredient().getId());

                    if (currentStock < requiredQty) {
                        conn.rollback();
                        // Exception demandée : message contenant l'ingrédient manquant
                        throw new RuntimeException("Stock insuffisant pour l'ingrédient : " + di.getIngredient().getName());
                    }
                }
            }

            // 2. INSERTION DE LA COMMANDE (ENTÊTE)
            String insertOrderSql = "INSERT INTO \"order\" (id, reference, creation_datetime) VALUES (?, ?, ?)";
            int orderId = getNextSerialValue(conn, "order", "id");
            try (PreparedStatement psOrder = conn.prepareStatement(insertOrderSql)) {
                psOrder.setInt(1, orderId);
                psOrder.setString(2, orderToSave.getReference());
                psOrder.setTimestamp(3, Timestamp.from(orderToSave.getCreationDatetime()));
                psOrder.executeUpdate();
            }

            // 3. INSERTION DES DÉTAILS (TABLE DE LIAISON)
            String insertDetailSql = "INSERT INTO dish_order (id, id_order, id_dish, quantity) VALUES (?, ?, ?, ?)";
            try (PreparedStatement psDetail = conn.prepareStatement(insertDetailSql)) {
                for (DishOrder doItem : orderToSave.getDishOrderList()) {
                    psDetail.setInt(1, getNextSerialValue(conn, "dish_order", "id"));
                    psDetail.setInt(2, orderId);
                    psDetail.setInt(3, doItem.getDish().getId());
                    psDetail.setInt(4, doItem.getQuantity());
                    psDetail.addBatch();
                }
                psDetail.executeBatch();
            }

            conn.commit();
            orderToSave.setId(orderId);
            return orderToSave;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur SQL lors de la sauvegarde : " + e.getMessage(), e);
        }
    }

    // ==========================================
    // ii) RÉCUPÉRATION PAR RÉFÉRENCE
    // ==========================================
    public Order findOrderByReference(String reference) {
        DBConnection dbConnection = new DBConnection();
        try (Connection connection = dbConnection.getConnection()) {
            String sql = "SELECT id, reference, creation_datetime FROM \"order\" WHERE reference = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, reference);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        Order order = new Order();
                        Integer idOrder = resultSet.getInt("id");
                        order.setId(idOrder);
                        order.setReference(resultSet.getString("reference"));
                        order.setCreationDatetime(resultSet.getTimestamp("creation_datetime").toInstant());
                        order.setDishOrderList(findDishOrderByIdOrder(idOrder));
                        return order;
                    }
                }
            }
            // Exception demandée si la référence n'existe pas
            throw new RuntimeException("Commande introuvable pour la référence : " + reference);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ==========================================
    // MÉTHODES DE LECTURE (FINDERS)
    // ==========================================
    private List<DishOrder> findDishOrderByIdOrder(Integer idOrder) {
        DBConnection dbConnection = new DBConnection();
        List<DishOrder> dishOrders = new ArrayList<>();
        try (Connection connection = dbConnection.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT id, id_dish, quantity FROM dish_order WHERE id_order = ?");
            preparedStatement.setInt(1, idOrder);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                DishOrder dishOrder = new DishOrder();
                dishOrder.setId(resultSet.getInt("id"));
                dishOrder.setQuantity(resultSet.getInt("quantity"));
                dishOrder.setDish(findDishById(resultSet.getInt("id_dish")));
                dishOrders.add(dishOrder);
            }
            return dishOrders;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Dish findDishById(Integer id) {
        DBConnection dbConnection = new DBConnection();
        try (Connection connection = dbConnection.getConnection()) {
            String sql = "SELECT id, name, dish_type, selling_price FROM dish WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Dish dish = new Dish();
                        dish.setId(rs.getInt("id"));
                        dish.setName(rs.getString("name"));
                        dish.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type")));
                        dish.setPrice(rs.getDouble("selling_price"));
                        dish.setDishIngredients(findIngredientByDishId(id));
                        return dish;
                    }
                }
            }
            throw new RuntimeException("Dish not found " + id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<DishIngredient> findIngredientByDishId(Integer idDish) {
        DBConnection dbConnection = new DBConnection();
        List<DishIngredient> dishIngredients = new ArrayList<>();
        try (Connection connection = dbConnection.getConnection()) {
            String sql = """
                    SELECT i.id, i.name, i.price, i.category, di.required_quantity, di.unit
                    FROM ingredient i 
                    JOIN dish_ingredient di ON di.id_ingredient = i.id 
                    WHERE di.id_dish = ?""";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, idDish);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Ingredient ing = new Ingredient();
                ing.setId(rs.getInt("id"));
                ing.setName(rs.getString("name"));
                ing.setPrice(rs.getDouble("price"));
                ing.setCategory(CategoryEnum.valueOf(rs.getString("category")));

                DishIngredient di = new DishIngredient();
                di.setIngredient(ing);
                di.setQuantity(rs.getDouble("required_quantity"));
                di.setUnit(Unit.valueOf(rs.getString("unit")));
                dishIngredients.add(di);
            }
            return dishIngredients;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ==========================================
    // LOGIQUE DE STOCK ET SÉQUENCES
    // ==========================================
    private double getCurrentStock(Connection conn, int ingredientId) throws SQLException {
        String sql = "SELECT SUM(CASE WHEN type = 'E' THEN quantity ELSE -quantity END) FROM stock_movement WHERE id_ingredient = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return 0;
    }

    private int getNextSerialValue(Connection conn, String tableName, String columnName) throws SQLException {
        String sequenceName = getSerialSequenceName(conn, tableName, columnName);
        updateSequenceNextValue(conn, tableName, columnName, sequenceName);
        try (PreparedStatement ps = conn.prepareStatement("SELECT nextval(?)")) {
            ps.setString(1, sequenceName);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    private String getSerialSequenceName(Connection conn, String tableName, String columnName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT pg_get_serial_sequence(?, ?)")) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);
        }
        throw new RuntimeException("Sequence not found for " + tableName);
    }

    private void updateSequenceNextValue(Connection conn, String tableName, String columnName, String sequenceName) throws SQLException {
        String sql = String.format("SELECT setval('%s', (SELECT COALESCE(MAX(%s), 0) FROM %s))", sequenceName, columnName, tableName);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeQuery();
        }
    }
}