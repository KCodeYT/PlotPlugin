![banner](./.github/images/banner.png)

What is this?
------------------------------

This project functions as a plugin for the powernukkitx environment.
It adds a plot system to the server, which supports changing borders, merging plots, customizing of the roads and much
more.

How can I download this plugin?
------------------------------

You can find the downloads on the [releases section](https://github.com/KCodeYT/PlotPlugin/releases) of this
github
page.
There you can find the LATEST jar file of this plugin.

How can I create a plot world?
------------------------------

    1. Open the chat in your client.
    2. Ensure you have the operator status on the server or 
       have the permission "plot.command.admin.generate", 
       otherwise you will not be able to use this command.
    3. Type /plot generate followed by the name of the plot world 
       and true or false if the world should be registered as default plot world.
    4. You are now in the plot world generation. 
       You have to answer the questions shown in the chat.
    5. After answering all questions, you are being teleported to the newly created plot world.

Commands and Permissions
------------------------------

| Command   | Sub command     | Permission                       | Aliases                       |
|-----------|-----------------|----------------------------------|-------------------------------|
| /plot     | ---             | ---                              | ---                           |
| ~         | addhelper       | ---                              | add, trust                    |
| ~         | auto            | ---                              | a                             |
| ~         | claim           | ---                              | c                             |
| ~         | clear           | ---                              | ---                           |
| ~         | delethome       | ---                              | delhome, removehome, rmhome   |
| ~         | deny            | ---                              | ---                           |
| ~         | dispose         | ---                              | reset                         |
| ~         | generate        | plot.command.admin.generate      | ---                           |
| ~         | home            | ---                              | h, visit, v                   |
| ~         | homes           | ---                              | ---                           |
| ~         | info            | ---                              | i                             |
| ~         | kick            | ---                              | ---                           |
| ~         | merge           | plot.command.merge               | ---                           |
| ~         | regenallroads   | plot.command.admin.regenallroads | ---                           |
| ~         | regenroad       | plot.command.admin.regenroad     | ---                           |
| ~         | reload          | plot.command.admin.reload        | ---                           |
| ~         | removehelper    | ---                              | remove, untrust               |
| ~         | sethome         | ---                              | ---                           |
| ~         | setowner        | plot.command.setowner            | ---                           |
| ~         | setroads        | plot.command.admin.setroads      | ---                           |
| ~         | setting         | ---                              | config                        |
| ~         | teleport        | plot.command.admin.teleport      | tp                            |
| ~         | undeny          | ---                              | ---                           |
| ~         | unlink          | plot.command.unlink              | ---                           |
| ~         | warp            | ---                              | w                             |

Commands that need to be enabled in config.yml
------------------------------

| Command | Sub command | Permission          | Aliases |
|---------|-------------|---------------------|---------|
| /plot   | border      | plot.command.border | b       |
| ~       | wall        | plot.command.wall   | ---     |

Other Permissions
------------------------------

| Permission                      | Description                                                                             |
|---------------------------------|-----------------------------------------------------------------------------------------|
| plot.command.admin.addhelper    | Bypasses the owner check when adding a helper to a plot.                                |
| plot.command.admin.border       | Bypasses the owner check when changing the plot border block.                           |
| plot.command.admin.clear        | Bypasses the owner check when clearing a plot.                                          |
| plot.command.admin.deletehome   | Bypasses the owner check when deleting the spawn point of a plot                        |
| plot.command.admin.deny         | Bypasses the owner check when denying a player from a plot.                             |
| plot.command.admin.dispose      | Bypasses the owner check when disposing a plot.                                         |
| plot.command.admin.info         | Bypasses the empty check when seeing the information of a plot.                         |
| plot.command.admin.kick         | Bypasses the owner check when kicking a player from a plot.                             |
| plot.command.admin.merge        | Bypasses the owner check when merging a plot.                                           |
| plot.command.admin.removehelper | Bypasses the owner check when removing a helper from a plot.                            |
| plot.command.admin.sethome      | Bypasses the owner check when setting the spawn point of a plot.                        |
| plot.command.admin.setowner     | Bypasses the owner check, self check and player limit when setting the owner of a plot. |
| plot.command.admin.config       | Bypasses the owner check when setting the configuration of a plot.                      |
| plot.command.admin.undeny       | Bypasses the owner check when undenying a player from a plot.                           |
| plot.command.admin.unlink       | Bypasses the owner check when unlinking a plot.                                         |
| plot.command.admin.wall         | Bypasses the owner check when changing the plot wall block.                             |
| plot.admin.bypass.deny          | Bypasses the denial of a player from a plot.                                            |
| plot.admin.bypass.kick          | Bypasses the kick of a player from a plot.                                              |
| plot.admin.interact             | Allows you to interact with blocks on the road.                                         |
| plot.admin.damage               | Allows you to damage players on roads or if pvp is disabled on the plot.                |
| plot.admin.bucket.fill          | Allows you to fill up buckets from the road.                                            |
| plot.admin.bucket.emtpy         | Allows you to empty buckets from the road.                                              |
| plot.admin.break                | Allows you to break blocks on the road.                                                 |
| plot.admin.place                | Allows you to place blocks on the road.                                                 |
| plot.merge.unlimited            | Allows you to merge unlimited plots.                                                    |
| plot.merge.limit.\<any number>  | Limits the player to only merge up to the given amount of plots.                        |
| plot.limit.\<any number>        | Limits the player to only claim up to the given amount of plots.                        |