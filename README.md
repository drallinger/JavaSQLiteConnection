## Usage Example:
```java
import com.drallinger.sqlite.SQLValue;
import com.drallinger.sqlite.SQLiteConnection;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class TestConnection extends SQLiteConnection {
    public TestConnection() {
        super("test.db");
    }

    @Override
    public void initConnection() throws SQLException {
        try(Statement statement = getStatement()){
            createTable(statement,
                "people",
                "name text not null",
                "age integer not null",
                "job text"
            );
        }
        prepareStatement("addPerson", "insert into people (name,age,job) values (?,?,?);");
        prepareStatement("personExists", "select exists(select 1 from people where name=?);");
        prepareStatement("getPerson", "select name,age,job from people where name = ?;");
        prepareStatement("getAllPeople", "select name,age,job from people;");
    }

    public void addPerson(String name, int age, String job){
        executeUpdate("addPerson",
            SQLValue.text(name),
            SQLValue.integer(age),
            SQLValue.text(job)
        );
    }

    public boolean personExists(String name){
        return executeExistsQuery("personExists", SQLValue.text(name));
    }

    public Person getPerson(String name){
        return executeSingleValueSelectQuery(
            "getPerson",
            rs -> new Person(rs.getString(1), rs.getInt(2), rs.getString(3)),
            SQLValue.text(name)
        );
    }

    public ArrayList<Person> getAllPeople(){
        return executeMultiValueSelectQuery(
            "getAllPeople",
            rs -> new Person(rs.getString(1), rs.getInt(2), rs.getString(3))
        );
    }
}
```