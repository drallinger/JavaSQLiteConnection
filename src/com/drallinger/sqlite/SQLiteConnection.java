package com.drallinger.sqlite;

import com.drallinger.sqlite.blueprints.PreparedStatementBlueprint;
import com.drallinger.sqlite.blueprints.TableBlueprint;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public abstract class SQLiteConnection implements AutoCloseable {
    private static final String BASE_CONNECTION_URL = "jdbc:sqlite:%s";
    private static final String DEFAULT_DATABASE_FILE = ":memory:";
    private final Connection connection;
    private final HashMap<String, PreparedStatement> preparedStatements;
    private final ArrayList<TableBlueprint> tableBlueprints;
    private final ArrayList<PreparedStatementBlueprint> preparedStatementBlueprints;

    public SQLiteConnection(String databaseFile){
        connection = createConnection(databaseFile);
        preparedStatements = new HashMap<>();
        tableBlueprints = new ArrayList<>();
        preparedStatementBlueprints = new ArrayList<>();

        initConnection();
        createTables();
        createPreparedStatements();
    }

    public SQLiteConnection(){
        this(DEFAULT_DATABASE_FILE);
    }

    private Connection createConnection(String databaseFile){
        Connection connection = null;
        try{
            connection = DriverManager.getConnection(String.format(BASE_CONNECTION_URL, databaseFile));
        }catch(SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
        return connection;
    }

    private void setStatementValues(PreparedStatement statement, SQLValue<?>[] values) throws SQLException{
        for(int i = 0; i < values.length; i++){
            SQLValue<?> value = values[i];
            switch (value.getType()){
                case INTEGER -> statement.setInt(i+1, (int) value.getValue());
                case REAL -> statement.setDouble(i+1, (double) value.getValue());
                case TEXT -> statement.setString(i+1, (String) value.getValue());
            }
        }
    }

    protected abstract void initConnection();

    @Deprecated
    protected void createTable(Statement statement, boolean ifNotExists, String tableName, String... columns){
        try{
            statement.executeUpdate(createTableQuery(new TableBlueprint(tableName, ifNotExists, columns)));
        }catch(SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Deprecated
    protected void createTable(Statement statement, String tableName, String... columns){
        createTable(statement, true, tableName, columns);
    }

    protected void createTable(String tableName, boolean ifNotExists, String... columns){
        tableBlueprints.add(new TableBlueprint(tableName, ifNotExists, columns));
    }

    protected void createTable(String tableName, String... columns){
        createTable(tableName, true, columns);
    }

    private String createTableQuery(TableBlueprint tableBlueprint){
        StringBuilder query = new StringBuilder("create table ");
        if(tableBlueprint.ifNotExists()){
            query.append("if not exists ");
        }
        query.append(tableBlueprint.tableName()).append("(");
        for(String column : tableBlueprint.columns()){
            query.append(column).append(",");
        }
        query.replace(query.length() - 1, query.length(), ");");
        return query.toString();
    }

    private void createTables(){
        try(Statement statement = connection.createStatement()){
            for(TableBlueprint tableBlueprint : tableBlueprints){
                statement.executeUpdate(createTableQuery(tableBlueprint));
            }
        }catch(SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    protected Statement getStatement(){
        Statement statement = null;
        try{
            statement = connection.createStatement();
        }catch(SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
        return statement;
    }

    protected void prepareStatement(String queryName, String query, boolean returnKeys){
        preparedStatementBlueprints.add(new PreparedStatementBlueprint(queryName, query, returnKeys));
    }

    protected void prepareStatement(String queryName, String query){
        prepareStatement(queryName, query, false);
    }

    private void createPreparedStatements(){
        try{
            for(PreparedStatementBlueprint preparedStatementBlueprint : preparedStatementBlueprints){
                preparedStatements.put(
                    preparedStatementBlueprint.queryName(),
                    connection.prepareStatement(
                        preparedStatementBlueprint.query(),
                        preparedStatementBlueprint.returnKeys() ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS
                    )
                );
            }
        }catch(SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    protected Optional<String> executeUpdate(String queryName, boolean returnKeys, SQLValue<?>... values){
        Optional<String> optional = Optional.empty();
        try{
            PreparedStatement statement = preparedStatements.get(queryName);
            setStatementValues(statement, values);
            statement.executeUpdate();
            if(returnKeys){
                try(ResultSet resultSet = statement.getGeneratedKeys()){
                    if(resultSet.next()){
                        optional = Optional.of(resultSet.getString(1));
                    }
                }
            }
        }catch (SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
        return optional;
    }

    protected Optional<String> executeUpdate(String queryName, SQLValue<?>... values){
        return executeUpdate(queryName, false, values);
    }

    protected boolean executeExistsQuery(String queryName, SQLValue<?>... values){
        boolean result = false;
        try{
            PreparedStatement statement = preparedStatements.get(queryName);
            setStatementValues(statement, values);
            try(ResultSet resultSet = statement.executeQuery()){
                if(resultSet.next()){
                    result = resultSet.getInt(1) == 1;
                }
            }
        }catch (SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
        return result;
    }

    protected <T> T executeSingleValueQuery(String queryName, SQLFunction<T> function, SQLValue<?>... values){
        T result = null;
        try{
            PreparedStatement statement = preparedStatements.get(queryName);
            setStatementValues(statement, values);
            try(ResultSet resultSet = statement.executeQuery()){
                if(resultSet.next()){
                    result = function.execute(resultSet);
                }
            }
        }catch (SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
        return result;
    }

    protected <T> ArrayList<T> executeMultiValueQuery(String queryName, SQLFunction<T> function, SQLValue<?>... values){
        ArrayList<T> arrayList = new ArrayList<>();
        try{
            PreparedStatement statement = preparedStatements.get(queryName);
            setStatementValues(statement, values);
            try(ResultSet resultSet = statement.executeQuery()){
                while(resultSet.next()){
                    arrayList.add(function.execute(resultSet));
                }
            }
        }catch (SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
        return arrayList;
    }

    protected boolean toBoolean(ResultSet rs, int index) throws SQLException{
        return rs.getInt(index) == 1;
    }

    public void setAutoCommit(boolean autoCommit){
        try{
            connection.setAutoCommit(autoCommit);
        }catch(SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void commit(){
        try{
            connection.commit();
        }catch(SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void rollback(){
        try{
            connection.rollback();
        }catch(SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void close(){
        try{
            if(connection != null && !connection.isClosed()){
                connection.close();
            }
        }catch(SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
    }
}
