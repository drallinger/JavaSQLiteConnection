## Usage Example:
```java
import com.drallinger.sqlite.SQLFunction;
import com.drallinger.sqlite.SQLFunctions;
import com.drallinger.sqlite.SQLValue;
import com.drallinger.sqlite.SQLiteConnection;

import java.util.ArrayList;

record Person(int ID, String name, int age, String job, boolean enrolled) {}

public class TestConnection extends SQLiteConnection {
    private final SQLFunction<Person> buildPerson;

    public TestConnection() {
        super("test.db");
        buildPerson = rs -> new Person(
            rs.getInt(1),
            rs.getString(2),
            rs.getInt(3),
            rs.getString(4),
            toBoolean(rs, 5)
        );
    }

    @Override
    protected void initConnection(){
        createTable(
            "people",
            "name text not null",
            "age integer not null",
            "job text not null",
            "enrolled integer not null"
        );
        prepareStatement("addPerson", "insert into people (name,age,job,enrolled) values (?,?,?,?);");
        prepareStatement("personExists", "select exists(select 1 from people where name=?);");
        prepareStatement("getPerson", "select rowid,name,age,job,enrolled from people where name=?;");
        prepareStatement("getAllPeople", "select rowid,name,age,job,enrolled from people;");
        prepareStatement("getPersonID", "select rowid from people where name=?;");
        prepareStatement("getPersonName", "select name from people where rowid=?");
        prepareStatement("getPeopleByEnrolledStatus", "select rowid,name,age,job,enrolled from people where enrolled=?;");
    }

    public void addPerson(String name, int age, String job, boolean enrolled){
        executeUpdate(
            "addPerson",
            SQLValue.text(name),
            SQLValue.integer(age),
            SQLValue.text(job),
            SQLValue.bool(enrolled)
        );
    }

    public boolean personExists(String name){
        return executeExistsQuery(
            "personExists",
            SQLValue.text(name)
        );
    }

    public Person getPerson(String name){
        return executeSingleValueQuery(
            "getPerson",
            buildPerson,
            SQLValue.text(name)
        );
    }

    public ArrayList<Person> getAllPeople(){
        return executeMultiValueQuery(
            "getAllPeople",
            buildPerson
        );
    }

    public int getPersonID(String name){
        return executeSingleValueQuery(
            "getPersonID",
            SQLFunctions.singleInteger(),
            SQLValue.text(name)
        );
    }

    public String getPersonName(int ID){
        return executeSingleValueQuery(
            "getPersonName",
            SQLFunctions.singleString(),
            SQLValue.integer(ID)
        );
    }

    public ArrayList<Person> getPeopleByEnrolledStatus(boolean enrolled){
        return executeMultiValueQuery(
            "getPeopleByEnrolledStatus",
            buildPerson,
            SQLValue.bool(enrolled)
        );
    }

    public static void main(String[] args) {
        try(TestConnection connection = new TestConnection()){
            connection.addPerson("Fred", 25, "Programmer", true);
            connection.addPerson("Dave", 35, "Manager", true);
            connection.addPerson("Bob", 30, "Builder", false);

            String[] names = {
                "Fred",
                "Bob",
                "Rick"
            };
            for(String name : names){
                if(connection.personExists(name)){
                    System.out.printf("%s exists%n", name);
                }else{
                    System.out.printf("%s doesn't exist%n", name);
                }
            }

            Person bob = connection.getPerson("Bob");
            System.out.printf("ID: %s, Name: %s, Age: %s, Job: %s, Enrolled: %s%n", bob.ID(), bob.name(), bob.age(), bob.job(), bob.enrolled());

            System.out.println("All people:");
            connection.getAllPeople().forEach(p -> {
                System.out.println(p.name());
            });

            int fredID = connection.getPersonID("Fred");
            System.out.printf("Fred's ID: %s%n", fredID);

            String personName = connection.getPersonName(2);
            System.out.printf("Person2's name: %s%n", personName);

            System.out.println("All enrolled people:");
            connection.getPeopleByEnrolledStatus(true).forEach(p -> {
                System.out.println(p.name());
            });
        }
    }
}
```
