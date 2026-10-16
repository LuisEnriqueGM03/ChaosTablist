"""Validate production jars and reject accidentally mixed loader dependencies."""
from pathlib import Path
import hashlib
import json
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parent.parent
VERSION = next(line.split("=", 1)[1] for line in (ROOT / "gradle.properties").read_text().splitlines() if line.startswith("version="))

for loader in ("fabric", "forge", "neoforge"):
    jar = ROOT / loader / "build/libs" / f"chaostablist-{loader}-1.21.4-{VERSION}.jar"
    with zipfile.ZipFile(jar) as z:
        names = set(z.namelist())
        provider = z.read("META-INF/services/com.chaos.tablist.platform.Platform").decode().strip()
        assert provider == f"com.chaos.tablist.{loader}.{loader.title()}Platform"
        for other in {"fabric", "forge", "neoforge"} - {loader}:
            assert not any(n.startswith(f"com/chaos/tablist/{other}/") for n in names)
        for name in names:
            if name.endswith(".class") and name.startswith("com/chaos/tablist/"):
                content = z.read(name)
                assert int.from_bytes(content[6:8], "big") == 65, name
                for other, api in {"fabric": b"net/fabricmc/", "forge": b"net/minecraftforge/", "neoforge": b"net/neoforged/"}.items():
                    if other != loader:
                        assert api not in content, f"{other} dependency in {loader} jar: {name}"
        assert int.from_bytes(z.read("com/chaos/tablist/ChaosTablist.class")[6:8], "big") == 65
        assert "LICENSE" in names
        assert len(json.loads(z.read("assets/chaostablist/badge_meta.json"))["icons"]) == 162
        languages = [json.loads(z.read(f"assets/chaostablist/lang/{lang}.json")) for lang in
                     ("en_us", "es_es", "es_mx", "es_ar", "es_cl", "es_ec", "es_uy", "es_ve")]
        assert all(set(lang) == set(languages[0]) for lang in languages)
        mixins = json.loads(z.read("chaostablist.mixins.json"))
        assert mixins["required"] and len(mixins["mixins"]) == 1 and len(mixins["client"]) == 3
        if loader == "fabric":
            meta = json.loads(z.read("fabric.mod.json"))
            assert meta["id"] == "chaostablist" and meta["version"] == VERSION
            assert "fabric-api" in meta["depends"]
            assert "META-INF/mods.toml" not in names and "META-INF/neoforge.mods.toml" not in names
        else:
            metadata_path = "META-INF/neoforge.mods.toml" if loader == "neoforge" else "META-INF/mods.toml"
            meta = tomllib.loads(z.read(metadata_path).decode())
            assert meta["mods"][0]["modId"] == "chaostablist" and meta["mods"][0]["version"] == VERSION
            deps = {d["modId"]: d for d in meta["dependencies"]["chaostablist"]}
            assert deps[loader]["versionRange"] == ("[21.4.158,21.5)" if loader == "neoforge" else "[54.1.14,55)")
            assert deps["minecraft"]["versionRange"] == "[1.21.4,1.21.5)"
            assert "fabric.mod.json" not in names
            if loader == "forge":
                assert "MixinConfigs: chaostablist.mixins.json" in z.read("META-INF/MANIFEST.MF").decode()
                assert "META-INF/neoforge.mods.toml" not in names
            else:
                assert meta["mixins"][0]["config"] == "chaostablist.mixins.json"
                assert "META-INF/mods.toml" not in names
        print(json.dumps({"loader": loader, "file": jar.name, "sha256": hashlib.sha256(jar.read_bytes()).hexdigest(), "validation": "PASS"}))

# A shared source file must not import either loader's API.
for java in (ROOT / "common/src").rglob("*.java"):
    text = java.read_text(encoding="utf-8")
    assert "net.fabricmc." not in text and "net.minecraftforge." not in text and "net.neoforged." not in text, java
