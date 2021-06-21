<img align="right" src="https://user-images.githubusercontent.com/8127996/90168671-bb49c780-dd9d-11ea-989d-479f8c1f3ea3.png" height="200" width="200">

# Orebfuscator - Anti X-Ray
[![Release Status](https://github.com/Imprex-Development/Orebfuscator/workflows/Releases/badge.svg)](https://github.com/Imprex-Development/Orebfuscator/releases/latest) [![Build Status](https://github.com/Imprex-Development/Orebfuscator/workflows/Build/badge.svg)](https://github.com/Imprex-Development/Orebfuscator/actions?query=workflow%3ABuild)

### Description
Orebfuscator is plugin for Spigot based Minecraft Servers that modifies packets in order to hide blocks of interest from X-Ray Clients and Texture Packs. Thus it doesn't modify your world and is safe to use.

### Features:
* Plug & Play
* Highly configurable config
* Support for Spigot based servers 1.9.4+ (only tested on spigot)
* Obfuscate blocks based on their light level
* Hide block entities like Chests and Furnaces
* Make blocks in a players proximity visible based on their distance an

## IMPORTANT

### Requirements:
- Java 8 or higher
- Spigot and (proably) any other fork of Spigot (1.9.4 or higher)
- [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997) 4.0 or higher

### Configurate
1. Download [ProtocolLib](https://github.com/dmulloy2/ProtocolLib/releases)
2. Download [Orebfuscator](https://github.com/Imprex-Development/Orebfuscator/releases)
3. Put both in your *plugins* directory
4. Start your server and configure orebfuscator to your liking

## Developer

### Clone
1. Clone this repo "git clone https://github.com/Imprex-Development/Orebfuscator.git"
2. Open eclipse and right click on the "Project Explorer"
3. Click "Import..."
4. Maven -> Existing Maven Projects
5. Select the downloaded repo

## Maven
```maven
<repositories>
  <repository>
    <id>imprex-repo</id>
    <url>https://imprex.ingrim4.me/repository/maven-public/</url>
  </repository>
  ...
</repositories>

<dependencies>
  <dependency>
    <groupId>net.imprex</groupId>
    <artifactId>orebfuscator-api</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

## Build
1. Click right click on the Orebfuscator-repo folder and select "Run as" -> "Maven Build..."
2. Put into Goals this "clean compile package -pl Plugin -Dorebfuscator-version=5.0.0 --also-make"
3. Click Run
4. Your jar will be builded under the folder "target"

## Release a new version
1. git tag **version** -m "**description**"
2. git push origin **version**

## License:

Almost completely rewritten by Imprex-Development to support v1.14 and higher Minecraft version's; these portions as permissible:
Copyright (C) 2020-2021 by Imprex-Development. All rights reserved.

Released under the same license as original.

Significantly reworked by Aleksey_Terzi to support v1.9 Minecraft; these portions as permissible:
Copyright (C) 2016 by Aleksey_Terzi. All rights reserved.

Released under the same license as original.

#### Original Copyright and License:

Copyright (C) 2011-2015 lishid.  All rights reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation,  version 3.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.

See the LICENSE file.
