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

import ms.kevi.plotplugin.manager.PlotManager;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Kevims KCodeYT
 */
@Setter
@Getter
@EqualsAndHashCode(exclude = {"manager", "origin"})
public class Plot {

    public static Plot fromConfig(PlotManager plotManager, Map<String, Object> plotMap) {
        final String ownerString = (String) plotMap.getOrDefault("owner", "null");
        final Plot plot = new Plot(plotManager, Plot.getPlotVectorFromConfig(plotMap), ownerString.equals("null") ? null : UUID.fromString(ownerString));
        plot.helpers.addAll((Plot.<Collection<? extends String>>getOrDefault(plotMap.get("helpers"), new ArrayList<>())).stream().map(UUID::fromString).toList());
        plot.deniedPlayers.addAll((Plot.<Collection<? extends String>>getOrDefault(plotMap.get("denied"), new ArrayList<>())).stream().map(UUID::fromString).toList());
        plot.config.putAll(Plot.<Map<String, Object>>getOrDefault(plotMap.get("config"), new HashMap<>()));
        for(int i = 0; i < plot.mergedPlots.length; i++)
            plot.mergedPlots[i] = (Boolean) ((List<?>) plotMap.getOrDefault("merges", new ArrayList<>())).get(i);
        return plot;
    }

    public static PlotId getPlotVectorFromConfig(Map<String, Object> plotMap) {
        return PlotId.of((int) plotMap.getOrDefault("x", 0), (int) plotMap.getOrDefault("z", 0));
    }

    @SuppressWarnings("unchecked")
    private static <C> C getOrDefault(Object o, C defaultValue) {
        return o == null ? defaultValue : (C) o;
    }

    private final PlotManager manager;
    private final PlotId id;

    private UUID owner;
    private final List<UUID> helpers;
    private final List<UUID> deniedPlayers;
    private final Map<String, Object> config;

    private final Boolean[] mergedPlots;
    private Plot origin;

    public Plot(PlotManager manager, PlotId id, UUID owner) {
        this.manager = manager;
        this.id = id;

        this.owner = owner;
        this.helpers = new ArrayList<>();
        this.deniedPlayers = new ArrayList<>();
        this.config = new HashMap<>();
        Arrays.fill(this.mergedPlots = new Boolean[4], false);
    }

    public boolean hasOwner() {
        return this.owner != null;
    }

    public boolean isOwner(UUID playerId) {
        return this.owner != null && this.owner.equals(playerId);
    }

    public boolean addHelper(UUID playerId) {
        if(!this.isHelper(playerId)) {
            this.manager.getConnectedPlots(this).forEach(plot -> plot.addHelper0(playerId));
            return true;
        }
        return false;
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
        if(!this.isDenied(playerId)) {
            this.manager.getConnectedPlots(this).forEach(plot -> plot.denyPlayer0(playerId));
            return true;
        }
        return false;
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
        for(boolean merged : this.mergedPlots)
            if(merged)
                return false;
        return true;
    }

    public boolean isMerged(int direction) {
        switch(direction) {
            case 0:
            case 1:
            case 2:
            case 3:
                return this.mergedPlots[direction];
            case 7:
                int i = direction - 4;
                int i2 = 0;
                return this.isMerged(i2) && this.isMerged(i) && this.manager.getPlotById(this.getRelative(i)).isMerged(i2) && this.manager.getPlotById(this.getRelative(i2)).isMerged(i);
            case 4:
            case 5:
            case 6:
                i = direction - 4;
                i2 = direction - 3;
                return this.isMerged(i2) && this.isMerged(i) && this.manager.getPlotById(this.getRelative(i)).isMerged(i2) && this.manager.getPlotById(this.getRelative(i2)).isMerged(i);
        }

        return false;
    }

    public void setMerged(int direction, boolean bool) {
        this.mergedPlots[direction] = bool;
    }

    public PlotId getRelative(int direction) {
        return switch(direction) {
            case 0 -> this.id.subtract(0, 1);
            case 1 -> this.id.add(1, 0);
            case 2 -> this.id.add(0, 1);
            case 3 -> this.id.subtract(1, 0);
            default -> this.id;
        };
    }

    public int getRelativeDir(PlotId other) {
        final int x = this.id.getX() - other.getX();
        final int z = this.id.getZ() - other.getZ();

        if(x == 0 && z == 1) return 0;
        if(x == -1 && z == 0) return 1;
        if(x == 0 && z == -1) return 2;
        if(x == 1 && z == 0) return 3;
        return -1;
    }

    public void recalculateOrigin() {
        if(this.hasNoMerges()) {
            this.origin = this;
            return;
        }

        final Set<Plot> connectedPlots = this.manager.getConnectedPlots(this);
        Plot min = this;
        for(Plot plot : connectedPlots) {
            if(plot.id.getZ() < min.id.getZ() || plot.id.getZ() == min.id.getZ() && plot.id.getX() < min.id.getX())
                min = plot;
        }

        for(Plot plot : connectedPlots) plot.origin = min;
        this.origin = min;
    }

    public Plot getBasePlot() {
        if(this.origin != null) return this.origin;
        return this.origin = this;
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
        for(boolean mergedPlot : this.mergedPlots)
            if(mergedPlot) return false;
        return true;
    }

    public Map<String, Object> toMap() {
        final Map<String, Object> map = new HashMap<>();

        map.put("x", this.id.getX());
        map.put("z", this.id.getZ());
        map.put("owner", this.owner == null ? "null" : this.owner.toString());
        map.put("helpers", this.helpers.stream().map(UUID::toString).collect(Collectors.toList()));
        map.put("denied", this.deniedPlayers.stream().map(UUID::toString).collect(Collectors.toList()));
        map.put("config", this.config);
        map.put("merges", this.mergedPlots);

        return map;
    }

}
