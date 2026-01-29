import org.postgresql.ds.PGSimpleDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DBConnection {
    public Connection getConnection() {
        try {
            PGSimpleDataSource ds = new PGSimpleDataSource();
            ds.setServerNames(new String[]{"localhost"});
            ds.setDatabaseName("mini_dish_db");
            ds.setUser("postgres");
            ds.setPassword("123456");
            return ds.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de se connecter à la base de données", e);
        }
    }
}