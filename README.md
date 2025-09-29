
# Orebfuscator - Anti X-Ray
[![Release Status](https://github.com/Imprex-Development/Orebfuscator/workflows/Releases/badge.svg)](https://github.com/Imprex-Development/Orebfuscator/releases/latest) [![Build Status](https://github.com/Imprex-Development/Orebfuscator/workflows/Build/badge.svg)](https://github.com/Imprex-Development/Orebfuscator/actions?query=workflow%3ABuild)
<img align="right" src="https://user-images.githubusercontent.com/8127996/90168671-bb49c780-dd9d-11ea-989d-479f8c1f3ea3.png" height="200" width="200">

Orebfuscator empowers server owners to protect their server from X-Ray Clients and Texture Packs, all while offering a high degree of configurability. This is achieved through modifying network packets without altering your game world, guaranteeing a secure and reliable experience for users. With Orebfuscator, you can tailor the settings to suit your server's needs, ensuring precise control over the visibility of specific blocks. This means that not only does Orebfuscator safeguard your world's integrity, but it also empowers you to fine-tune your Anti-X-Ray measures for the best gameplay experience.

### Features
* Seamless Integration: Plug & Play functionality for effortless use.
* Extensive Configuration: Highly customizable settings to tailor the experience to your liking.
* Server Compatibility: Designed for Spigot-based servers 1.16 and newer.
* Block Obfuscation: Conceal non-visible blocks from players' view.
* Block-Entity Support: Hide block entities such as Chests and Furnaces.
* Dynamic Block Visibility: Adjust block visibility based on player proximity and distance.

### Requirements
* Java 17 or higher
* Spigot, Paper, Folia or compatible forks (1.16 or newer)
* [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997) 5.2.0 or later

### Installation
1. Download [ProtocolLib](https://github.com/dmulloy2/ProtocolLib/releases)
2. Download [Orebfuscator](https://github.com/Imprex-Development/Orebfuscator/releases)
3. Place both plugins in your _plugins_ directory
4. Start your server and [configure Orebfuscator](https://github.com/Imprex-Development/Orebfuscator/wiki/Config) to your liking

Still having trouble getting Orebfuscator to run check out our [common issues](https://github.com/Imprex-Development/Orebfuscator/wiki/Common-Issues).

### Legacy support
For compatibility with Java 11 or Minecraft 1.9.4 and later, you can use any legacy release prior to version 5.5.0. Please note that these legacy releases will no longer receive regular support or updates. However, they may receive critical security and vulnerability patches if necessary.

### Maven

To include the API in your Maven project, add the following configuration to your `pom.xml`:

```xml
<repositories>
  <repository>
    <id>codemc-repo</id>
    <url>https://repo.codemc.io/repository/maven-public/</url>
  </repository>
  <!-- Additional repositories can be added here if needed -->
</repositories>

<dependencies>
  <dependency>
    <groupId>net.imprex</groupId>
    <artifactId>orebfuscator-api</artifactId>
    <version>5.2.4</version>
  </dependency>
  <!-- Add other dependencies as required -->
</dependencies>
```

## License:

Completely rewritten by Imprex-Development to support v1.14 and higher Minecraft version's; these portions as permissible:
Copyright (C) 2020-2025 by Imprex-Development. All rights reserved.

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
