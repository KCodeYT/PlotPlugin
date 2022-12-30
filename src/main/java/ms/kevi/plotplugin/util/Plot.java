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

package ms.kevi.plotplugin.util;

import cn.nukkit.math.BlockVector3;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import ms.kevi.plotplugin.manager.PlotManager;

import java.util.*;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
@Setter
@Getter
@EqualsAndHashCode(exclude = {"manager"})
public class Plot {

    public static final int DIRECTION_SELF = -1;

    public static final int DIRECTION_NORTH = 0;
    public static final int DIRECTION_EAST = 1;
    public static final int DIRECTION_SOUTH = 2;
    public static final int DIRECTION_WEST = 3;
    public static final int DIRECTION_NORTH_EAST = 4;
    public static final int DIRECTION_SOUTH_EAST = 5;
    public static final int DIRECTION_SOUTH_WEST = 6;
    public static final int DIRECTION_NORTH_WEST = 7;

    public static final int[] DIRECTION_OPPOSITES = new int[]{
            Plot.DIRECTION_SOUTH,
            Plot.DIRECTION_WEST,
            Plot.DIRECTION_NORTH,
            Plot.DIRECTION_EAST
    };

    private final PlotManager manager;
    private final PlotId id;

    private UUID owner;
    private final List<UUID> helpers;
    private final List<UUID> deniedPlayers;
    private final Map<String, Object> config;
    private BlockVector3 homePosition;

    private final boolean[] mergedDirections;
    private PlotId originId;

    public Plot(PlotManager manager, PlotId id) {
        this(manager, id, null);
    }

    public Plot(PlotManager manager, PlotId id, UUID owner) {
        this.manager = manager;
        this.id = id;
        this.owner = owner;
        this.helpers = new ArrayList<>();
        this.deniedPlayers = new ArrayList<>();
        this.config = new HashMap<>();
        Arrays.fill(this.mergedDirections = new boolean[4], false);
        this.originId = id;
    }

    public boolean hasOwner() {
        return this.owner != null;
    }

    public boolean isOwner(UUID playerId) {
        return this.owner != null && this.owner.equals(playerId);
    }

    public boolean addHelper(UUID playerId) {
        if(this.isHelper(playerId)) return false;

        this.manager.getConnectedPlots(this).forEach(plot -> plot.addHelper0(playerId));
        return true;
    }

    private void addHelper0(UUID playerId) {
        this.helpers.add(playerId);
    }

    public boolean removeHelper(UUID playerId) {
        final boolean wasHelper = this.isHelper(playerId);
        this.manager.getConnectedPlots(this).forEach(plot -> plot.removeHelper0(playerId));
        return wasHelper;
    }

    private void removeHelper0(UUID playerId) {
        this.helpers.remove(playerId);
    }

    public boolean isHelper(UUID playerId) {
        return this.helpers.contains(playerId);
    }

    public boolean isDenied(UUID playerId) {
        return this.deniedPlayers.contains(playerId);
    }

    public boolean denyPlayer(UUID playerId) {
        if(this.isDenied(playerId)) return false;

        this.manager.getConnectedPlots(this).forEach(plot -> plot.denyPlayer0(playerId));
        return true;
    }

    private void denyPlayer0(UUID playerId) {
        this.deniedPlayers.add(playerId);
    }

    public boolean unDenyPlayer(UUID playerId) {
        final boolean wasDenied = this.isDenied(playerId);
        this.manager.getConnectedPlots(this).forEach(plot -> plot.unDenyPlayer0(playerId));
        return wasDenied;
    }

    private void unDenyPlayer0(UUID playerId) {
        this.deniedPlayers.remove(playerId);
    }

    public Object getConfigValue(String name) {
        return this.config.get(name);
    }

    public void setConfigValue(String name, Object object) {
        this.manager.getConnectedPlots(this).forEach(plot -> plot.setConfigValue0(name, object));
    }

    private void setConfigValue0(String name, Object object) {
        this.config.put(name, object);
    }

    public boolean hasNoMerges() {
        for(boolean merged : this.mergedDirections)
            if(merged) return false;
        return true;
    }

    public boolean isFullyMerged() {
        for(boolean merged : this.mergedDirections)
            if(!merged) return false;
        return true;
    }

    public boolean isMerged(int direction) {
        if(direction < 0 || direction > 7) return false;

        if(direction < 4) return this.mergedDirections[direction];

        final int f = direction - 4;
        final int s = direction == DIRECTION_NORTH_WEST ? 0 : direction - 3;
        return this.isMerged(f) && this.isMerged(s) && this.manager.getPlotById(this.getRelative(f)).isMerged(s) && this.manager.getPlotById(this.getRelative(s)).isMerged(f);
    }

    public void setMerged(int direction, boolean bool) {
        this.mergedDirections[direction] = bool;
    }

    public PlotId getRelative(int direction) {
        return switch(direction) {
            case DIRECTION_NORTH -> this.id.subtract(0, 1);
            case DIRECTION_EAST -> this.id.add(1, 0);
            case DIRECTION_SOUTH -> this.id.add(0, 1);
            case DIRECTION_WEST -> this.id.subtract(1, 0);
            case DIRECTION_NORTH_EAST -> this.id.add(1, -1);
            case DIRECTION_SOUTH_EAST -> this.id.add(1, 1);
            case DIRECTION_SOUTH_WEST -> this.id.add(-1, 1);
            case DIRECTION_NORTH_WEST -> this.id.subtract(1, 1);
            default -> this.id;
        };
    }

    public int getRelativeDir(PlotId other) {
        final int x = this.id.getX() - other.getX();
        final int z = this.id.getZ() - other.getZ();

        if(x == 0 && z == 1) return DIRECTION_NORTH;
        if(x == -1 && z == 0) return DIRECTION_EAST;
        if(x == 0 && z == -1) return DIRECTION_SOUTH;
        if(x == 1 && z == 0) return DIRECTION_WEST;
        return -1;
    }

    public void recalculateOrigin() {
        if(this.hasNoMerges()) {
            this.originId = this.id;
            return;
        }

        final Set<Plot> connectedPlots = this.manager.getConnectedPlots(this);
        PlotId min = this.id;
        for(Plot plot : connectedPlots) {
            if(plot.id.getZ() < min.getZ() || plot.id.getZ() == min.getZ() && plot.id.getX() < min.getX())
                min = plot.id;
        }

        for(Plot plot : connectedPlots) plot.originId = min;
        this.originId = min;
    }

    public Plot getBasePlot() {
        if(this.originId != null) return this.manager.getPlotById(this.originId);
        this.recalculateOrigin();
        return this.manager.getPlotById(this.originId);
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    public boolean isDefault() {
        if(this.owner != null) return false;
        if(!this.helpers.isEmpty()) return false;
        if(!this.deniedPlayers.isEmpty()) return false;
        if(!this.config.isEmpty()) return false;
        for(boolean merged : this.mergedDirections)
            if(merged) return false;
        return true;
    }

    public static Plot.Builder builder(PlotManager manager, PlotId id) {
        return new Builder(manager, id);
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    @RequiredArgsConstructor
    public static final class Builder {

        private final PlotManager manager;
        private final PlotId id;

        private UUID owner;
        private List<UUID> helpers;
        private List<UUID> deniedPlayers;
        private Map<String, Object> config;
        private BlockVector3 homePosition;

        private boolean[] mergedDirections;
        private PlotId originId;

        public Plot build() {
            final Plot plot = new Plot(this.manager, this.id, this.owner);
            if(this.helpers != null && !this.helpers.isEmpty())
                plot.helpers.addAll(this.helpers);

            if(this.deniedPlayers != null && !this.deniedPlayers.isEmpty())
                plot.deniedPlayers.addAll(this.deniedPlayers);

            if(this.config != null && !this.config.isEmpty())
                plot.config.putAll(this.config);

            if(this.homePosition != null)
                plot.homePosition = homePosition;

            if(this.mergedDirections != null) {
                for(int i = 0; i < this.mergedDirections.length; i++)
                    plot.mergedDirections[i] = this.mergedDirections[i];
            }

            if(this.originId != null)
                plot.originId = this.originId;

            return plot;
        }

    }

}
