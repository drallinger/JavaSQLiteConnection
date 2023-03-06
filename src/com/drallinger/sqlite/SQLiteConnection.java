package com.drallinger.sqlite;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public abstract class SQLiteConnection implements AutoCloseable {
    private static final String BASE_CONNECTION_URL = "jdbc:sqlite:%s";
    private static final String DEFAULT_DATABASE_FILE = ":memory:";
    private final Connection connection;
    private final HashMap<String, PreparedStatement> preparedStatements;

    public SQLiteConnection(String databaseFile){
        connection = createConnection(databaseFile);
        preparedStatements = new HashMap<>();
        try{
            initConnection();
        }catch (SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
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

    public abstract void initConnection() throws SQLException;

    public void createTable(Statement statement, boolean ifNotExists, String tableName, String... columns){
        StringBuilder query = new StringBuilder("create table ");
        if(ifNotExists){
            query.append("if not exists ");
        }
        query.append(tableName).append("(");
        for(String column : columns){
            query.append(column).append(",");
        }
        query.replace(query.length() - 1, query.length(), ");");
        try{
            statement.executeUpdate(query.toString());
        }catch(SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void createTable(Statement statement, String tableName, String... columns){
        createTable(statement, true, tableName, columns);
    }

    public Statement getStatement(){
        Statement statement = null;
        try{
            statement = connection.createStatement();
        }catch(SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
        return statement;
    }

    public void prepareStatement(String queryName, String query, boolean returnKeys) throws SQLException{
        preparedStatements.put(queryName, connection.prepareStatement(query, returnKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS));
    }

    public void prepareStatement(String queryName, String query) throws SQLException{
        prepareStatement(queryName, query, false);
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

    public Optional<String> executeUpdate(String queryName, boolean returnKeys, SQLValue<?>... values){
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

    public Optional<String> executeUpdate(String queryName, SQLValue<?>... values){
        return executeUpdate(queryName, false, values);
    }

    public boolean executeExistsQuery(String queryName, SQLValue<?>... values){
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

    public <T> T executeSingleValueSelectQuery(String queryName, SQLFunction<T> function, SQLValue<?>... values){
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

    public <T> ArrayList<T> executeMultiValueSelectQuery(String queryName, SQLFunction<T> function, SQLValue<?>... values){
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
