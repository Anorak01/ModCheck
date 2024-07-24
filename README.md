One day I needed a mod that makes sure clients have exactly the versions of mods I need, so this is my solution

---
The server-side checks for modlist.txt file in the server root (next to files like server.properties) on startup.
- If it finds the file, it will compare the client mods against that
- If no file is present, the mods in server-side mods folder will be used

The modlist.txt file is generated using a small utility. [ModCheckUtil](https://github.com/Anorak01/ModCheckUtil)

The client-side sends its modlist with checksums when it joins the server.

There is also a handshake that happens between the server and client to make sure this mod is installed on client.

---

### Versions:
Currently, the only available version is for 1.20.1 Fabric.

I don't plan on compiling other versions any time soon.