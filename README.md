<img align="right" src="https://user-images.githubusercontent.com/8127996/90168671-bb49c780-dd9d-11ea-989d-479f8c1f3ea3.png" height="200" width="200">

# Orebfuscator - Anti X-Ray
[![Release Status](https://github.com/Imprex-Development/Orebfuscator/workflows/Releases/badge.svg)](https://github.com/Imprex-Development/Orebfuscator/releases/latest) [![Build Status](https://github.com/Imprex-Development/Orebfuscator/workflows/Build/badge.svg)](https://github.com/Imprex-Development/Orebfuscator/actions?query=workflow%3ABuild)

Orebfuscator is plugin for Spigot based Minecraft Servers that modifies packets in order to hide blocks of interest from X-Ray Clients and Texture Packs. Thus it doesn't modify your world and is safe to use.

### Features
* Plug & Play
* Highly configurable config
* Support for Spigot based servers 1.9.4+ (only tested on spigot)
* Obfuscate non-visible blocks
* Hide block entities like Chests and Furnaces
* Make blocks in a players proximity visible based on their distance an

### Requirements
- Java 17 or higher
- Spigot and (proably) any other fork of Spigot (1.9.4 or higher)
- [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997) 5.0.0 or higher

### Installation
1. Download [ProtocolLib](https://github.com/dmulloy2/ProtocolLib/releases)
2. Download [Orebfuscator](https://github.com/Imprex-Development/Orebfuscator/releases)
3. Put both in your *plugins* directory
4. Start your server and [configure](https://github.com/Imprex-Development/Orebfuscator/wiki/Config) orebfuscator to your liking

Still having trouble getting Orebfuscator to run check out our [common issues](https://github.com/Imprex-Development/Orebfuscator/wiki/Common-Issues).

### Maven
```maven
<repositories>
  <repository>
    <id>codemc-repo</id>
    <url>https://repo.codemc.io/repository/maven-public/</url>
  </repository>
  ...
</repositories>

<dependencies>
  <dependency>
    <groupId>net.imprex</groupId>
    <artifactId>orebfuscator-api</artifactId>
    <version>5.2.4</version>
  </dependency>
</dependencies>
```

## License:

Completely rewritten by Imprex-Development to support v1.14 and higher Minecraft version's; these portions as permissible:
Copyright (C) 2020-2023 by Imprex-Development. All rights reserved.

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
