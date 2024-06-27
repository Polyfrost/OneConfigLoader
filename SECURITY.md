# Loader Security and Transparency

At Polyfrost, we make sure security is at the forefront of all our work. This is
very important for how we implement the loader and wrapper, since it downloads
and runs an executable jar automatically without user input. We understand the
concerns that this may raise and hope to make our systems as secure and comfortable
as possible.

## Security Policy

If you are looking for Polyfrost's security policy, please see our security page at
[the OneConfig repository](https://github.com/Polyfrost/OneConfig/security/policy).

If you have any other security-related concerns regarding the loader, please contact
nea89 or any other dev in [the Polyfrost discord server.](https://inv.wtf/polyfrost)

## Legacy Minecraft

In the legacy versions of Minecraft that this version of OneConfig currently supports,
it is generally accepted to use Mojang's launch wrapper system to load additional mods
and libraries, as seen with [the Essential mod](https://essential.gg). In this case,
we use [our API](https://api.polyfrost.org) and [our Maven Repository](https://repo.polyfrost.org)
to download and verify the hash of the OneConfig jar as outlined in the README of
[this repository](./README.md). We are working on improving the security of the Maven
repository so it can be seperated from the API. In the current version of our open
source API, we get the checksum of the latest artifact in the Maven repository,
which makes the checksum system less useful since if someone managed to access
the Maven repository, the API would also be compromised.

Of course, we implement strict security and sandboxing policies in our hosting
services and within those who have access to Maven repository keys. We are also
planning to improve the API overall at a later time. Even after these practices,
we are planning to renovate our systems soon to prevent supply-chain attacks.

## Modern Minecraft

When we migrate OneConfig to modern versions of Minecraft, we hope to also migrate to a more
security-focused loader, espically after the fracturiser incident spread across the modern
modding community unfortunately using a similar stage loader as OneConfig currently does.

We are planning to do research on a less intrusive and more secure way to load OneConfig
in modern Minecraft versions. We also plan to follow all advice given in the fracturiser
incident report and following meetings.
