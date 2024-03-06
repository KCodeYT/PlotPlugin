/*
 * Copyright 2022 KCodeYT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ms.kevi.plotplugin.database;

import cn.nukkit.math.BlockVector3;
import com.google.gson.Gson;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import ms.kevi.plotplugin.manager.PlotManager;
import ms.kevi.plotplugin.util.Plot;
import ms.kevi.plotplugin.util.PlotId;

import java.sql.*;
import java.util.*;

/**
 * @author Kevims KCodeYT
 */
@Builder
@RequiredArgsConstructor
public final class Database {

    private static final Gson GSON = new Gson();

    private final String host;
    private final int port;
    private final List<String> parameters;
    private final String user;
    private final String password;
    private final String database;

    private Connection createConnection(boolean withDatabase) throws SQLException {
        final StringBuilder queryBuilder = new StringBuilder();
        if (this.parameters != null && !this.parameters.isEmpty()) {
            queryBuilder.append('?');

            final Iterator<String> parameterIterator = this.parameters.iterator();
            while (parameterIterator.hasNext()) {
                queryBuilder.append(parameterIterator.next());

                if (parameterIterator.hasNext())
                    queryBuilder.append("&");
            }
        }

        return DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + (withDatabase ? this.database : "") + queryBuilder, this.user, this.password);
    }

    public void executeActions(List<DatabaseAction> actions) {
        try (final Connection connection = this.createConnection(true)) {
            for (DatabaseAction action : actions)
                action.execute(connection);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void createDatabase() {
        try (final Connection connection = this.createConnection(false);
             final Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + this.database);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createPlotsTable(String levelName) {
        try (final Connection connection = this.createConnection(true);
             final PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + levelName + " (" +
                     "id BIGINT NOT NULL, " +
                     "x INT NOT NULL, " +
                     "z INT NOT NULL, " +
                     "owner VARCHAR(36), " +
                     "helpers TEXT, " +
                     "denied TEXT, " +
                     "config TEXT, " +
                     "merged TINYINT NOT NULL DEFAULT '0', " +
                     "home_position VARCHAR(100), " +
                     "origin BIGINT NOT NULL, " +
                     "PRIMARY KEY (id))")) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Plot> getPlots(PlotManager manager) {
        try (final Connection connection = this.createConnection(true);
             final PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + manager.getLevelName())) {
            final ResultSet resultSet = statement.executeQuery();
            final List<Plot> plots = new ArrayList<>();

            while (resultSet.next()) {
                final PlotId plotId = PlotId.fromLong(resultSet.getLong("id"));
                final String owner = resultSet.getString("owner");
                String helpers = resultSet.getString("helpers");
                helpers = helpers == null ? "[]" : helpers;
                String denied = resultSet.getString("denied");
                denied = denied == null ? "[]" : denied;
                String config = resultSet.getString("config");
                config = config == null ? "{}" : config;
                final byte mergedByte = resultSet.getByte("merged");
                final String homePosition = resultSet.getString("home_position");
                final PlotId origin = PlotId.fromLong(resultSet.getLong("origin"));

                final boolean[] mergedDirections = new boolean[4];
                for (int i = 0; i < 4; i++)
                    mergedDirections[i] = (mergedByte & (1 << i)) != 0;

                final BlockVector3 homePositionVector;
                if (homePosition != null) {
                    final String[] split = homePosition.split(";");
                    homePositionVector = new BlockVector3(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
                } else
                    homePositionVector = null;

                plots.add(Plot.builder(manager, plotId).
                        owner(owner == null ? null : UUID.fromString(owner)).
                        helpers(GSON.<List<String>>fromJson(helpers, List.class).stream().map(UUID::fromString).toList()).
                        deniedPlayers(GSON.<List<String>>fromJson(denied, List.class).stream().map(UUID::fromString).toList()).
                        config(GSON.<Map<String, Object>>fromJson(config, Map.class)).
                        mergedDirections(mergedDirections).
                        homePosition(homePositionVector).
                        originId(origin).
                        build());
            }

            return plots;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public DatabaseAction updatePlot(Plot plot) {
        return connection -> {
            final String levelName = plot.getManager().getLevel().getFolderName();

            try (final PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM " + levelName + " WHERE id = ?")) {
                selectStatement.setLong(1, plot.getId().asLong());

                final ResultSet resultSet = selectStatement.executeQuery();

                if (!resultSet.next()) {
                    if (!plot.hasOwner() && plot.isDefault()) return;

                    try (final PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO " + levelName + " (id, x, z, owner, helpers, denied, config, merged, home_position, origin) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                        insertStatement.setLong(1, plot.getId().asLong());
                        insertStatement.setInt(2, plot.getId().getX());
                        insertStatement.setInt(3, plot.getId().getZ());
                        insertStatement.setString(4, plot.getOwner() == null ? null : plot.getOwner().toString());
                        insertStatement.setString(5, GSON.toJson(plot.getHelpers().stream().map(UUID::toString).toList()));
                        insertStatement.setString(6, GSON.toJson(plot.getDeniedPlayers().stream().map(UUID::toString).toList()));
                        insertStatement.setString(7, GSON.toJson(plot.getConfig()));

                        byte mergedByte = 0;
                        for (int i = 0; i < 4; i++) if (plot.isMerged(i)) mergedByte |= 1 << i;

                        insertStatement.setByte(8, mergedByte);

                        final BlockVector3 homePosition = plot.getHomePosition();
                        if (homePosition != null)
                            insertStatement.setString(9, homePosition.getX() + ";" + homePosition.getY() + ";" + homePosition.getZ());
                        else
                            insertStatement.setString(9, null);

                        insertStatement.setLong(10, plot.getOriginId().asLong());
                        insertStatement.executeUpdate();
                    }
                } else {
                    if (!plot.hasOwner() && plot.isDefault()) {
                        try (final PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM " + levelName + " WHERE id = ?")) {
                            deleteStatement.setLong(1, plot.getId().asLong());
                            deleteStatement.executeUpdate();
                        }
                        return;
                    }

                    try (final PreparedStatement updateStatement = connection.prepareStatement("UPDATE " + levelName + " SET owner = ?, helpers = ?, denied = ?, config = ?, merged = ?, home_position = ?, origin = ? WHERE id = ?")) {
                        updateStatement.setString(1, plot.getOwner() == null ? null : plot.getOwner().toString());
                        updateStatement.setString(2, GSON.toJson(plot.getHelpers().stream().map(UUID::toString).toList()));
                        updateStatement.setString(3, GSON.toJson(plot.getDeniedPlayers().stream().map(UUID::toString).toList()));
                        updateStatement.setString(4, GSON.toJson(plot.getConfig()));

                        byte mergedByte = 0;
                        for (int i = 0; i < 4; i++) if (plot.isMerged(i)) mergedByte |= 1 << i;

                        updateStatement.setByte(5, mergedByte);

                        final BlockVector3 homePosition = plot.getHomePosition();
                        if (homePosition != null)
                            updateStatement.setString(6, homePosition.getX() + ";" + homePosition.getY() + ";" + homePosition.getZ());
                        else
                            updateStatement.setString(6, null);

                        updateStatement.setLong(7, plot.getOriginId().asLong());
                        updateStatement.setLong(8, plot.getId().asLong());
                        updateStatement.executeUpdate();
                    }
                }
            }
        };
    }

    public DatabaseAction insertPlot(String levelName, PlotId plotId, String owner, List<String> helpers, List<String> denied, Map<String, Object> config, boolean[] mergedDirections, BlockVector3 homePosition, PlotId originId) {
        return connection -> {
            try (final PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO " + levelName + " (id, x, z, owner, helpers, denied, config, merged, home_position, origin) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                insertStatement.setLong(1, plotId.asLong());
                insertStatement.setInt(2, plotId.getX());
                insertStatement.setInt(3, plotId.getZ());
                insertStatement.setString(4, owner);
                insertStatement.setString(5, GSON.toJson(helpers));
                insertStatement.setString(6, GSON.toJson(denied));
                insertStatement.setString(7, GSON.toJson(config));

                byte mergedByte = 0;
                for (int i = 0; i < 4; i++) if (mergedDirections[i]) mergedByte |= 1 << i;

                insertStatement.setByte(8, mergedByte);

                if (homePosition != null)
                    insertStatement.setString(9, homePosition.getX() + ";" + homePosition.getY() + ";" + homePosition.getZ());
                else
                    insertStatement.setString(9, null);

                insertStatement.setLong(10, originId.asLong());
                insertStatement.executeUpdate();
            }
        };
    }

    public DatabaseAction deletePlot(Plot plot) {
        return connection -> {
            final String levelName = plot.getManager().getLevel().getFolderName();

            try (final PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM " + levelName + " WHERE id = ?")) {
                deleteStatement.setLong(1, plot.getId().asLong());
                deleteStatement.executeUpdate();
            }
        };
    }

    public void createPlayersTable() {
        try (final Connection connection = this.createConnection(true);
             final PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS plot_players (" +
                     "id VARCHAR(36) NOT NULL, " +
                     "name VARCHAR(256) NOT NULL, " +
                     "PRIMARY KEY (id), " +
                     "UNIQUE (id))")) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<UUID, String> getPlayers() {
        try (final Connection connection = this.createConnection(true);
             final PreparedStatement statement = connection.prepareStatement("SELECT * FROM plot_players")) {
            final ResultSet resultSet = statement.executeQuery();

            final Map<UUID, String> names = new HashMap<>();

            while (resultSet.next()) {
                names.put(UUID.fromString(resultSet.getString("id")), resultSet.getString("name"));
            }

            return names;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public DatabaseAction addPlayer(UUID uniqueId, String name) {
        return connection -> {
            try (final PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO plot_players (id, name) VALUES (?, ?)")) {
                insertStatement.setString(1, uniqueId.toString());
                insertStatement.setString(2, name);
                insertStatement.executeUpdate();
            }
        };
    }

    public void addPlayers(Map<UUID, String> players) {
        try (final Connection connection = this.createConnection(true);
             final PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO plot_players (id, name) VALUES (?, ?)")) {
            for (Map.Entry<UUID, String> entry : players.entrySet()) {
                insertStatement.setString(1, entry.getKey().toString());
                insertStatement.setString(2, entry.getValue());
                insertStatement.addBatch();
            }

            insertStatement.executeLargeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public interface DatabaseAction {
        void execute(Connection connection) throws Exception;
    }

}
