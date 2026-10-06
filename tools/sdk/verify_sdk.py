"""SDK source dependency/public-symbol boundaries and staged publication metadata audit."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[2]
PROVIDER_MODULES = ("sdk/providers/tmdb", "sdk/providers/clientB")
HEADLESS_MODULES = ("sdk/model", "sdk/api", "sdk/runtime", *PROVIDER_MODULES)
PROVIDER_UI_MODULES = ("sdk/providers/tmdb/ui", "sdk/providers/clientB/ui")
OPTIONAL_UI_MODULES = ("sdk/ui", *PROVIDER_UI_MODULES)
MODULES = (*HEADLESS_MODULES, *OPTIONAL_UI_MODULES)
HEADLESS_ARTIFACTS = {"sdk-model", "sdk-api", "sdk-runtime", "provider-tmdb", "provider-clientb"}
OPTIONAL_UI_ARTIFACTS = {"sdk-ui", "provider-tmdb-ui", "provider-clientb-ui"}
UI_RESOURCE_PACKAGES = {
    "sdk-ui": "com.pampoukidis.streamcore.sdk.ui.generated.resources",
    "provider-tmdb-ui": "com.pampoukidis.streamcore.sdk.providers.tmdb.ui.generated.resources",
    "provider-clientb-ui": "com.pampoukidis.streamcore.sdk.providers.clientb.ui.generated.resources",
}
# Deliberate public provider-integration types colocated with internal runtime implementations.
RUNTIME_PROVIDER_CONTRACTS = {
    "auth.AuthProvider": "interface",
    "profile.ProfileProvider": "interface",
    "home.HomeProvider": "interface",
    "details.DetailsProvider": "interface",
    "search.SearchProvider": "interface",
    "playback.PlaybackProvider": "interface",
    "content.ContentPolicyProvider": "interface",
    "session.ProviderSessionFactory": "interface",
    "session.ProviderSessionServices": "class",
    "error.ProviderOperationException": "class",
}
PUBLIC_TOP_LEVEL = re.compile(
    r"^(?P<modifiers>(?:(?:public|internal|private|protected|open|abstract|sealed|data|enum|expect|actual|value|inline|suspend|fun|const|lateinit)\s+)*)"
    r"(?P<kind>class|interface|object|fun|val|var|typealias)\s+(?P<name>[\w<>.]+)", re.MULTILINE
)
FORBIDDEN_DEPENDENCY = re.compile(r"androidx\.(?:compose|tv|media3)|org\.jetbrains\.(?:compose|skiko)|io\.insert-koin|com\.google\.android\.exoplayer|shaka", re.I)
OPTIONAL_UI_DIRECT_FORBIDDEN_DEPENDENCY = re.compile(
    r"androidx\.(?:tv|media3)|(?:androidx|org\.jetbrains)\.compose\.material|"
    r"io\.(?:insert-koin|coil-kt)|com\.google\.android\.exoplayer|shaka|"
    r"^io\.ktor:|^(?:androidx|org\.jetbrains\.androidx)\.(?:datastore|lifecycle):",
    re.I,
)


def production_files(module: str):
    return sorted(path for path in (ROOT / module / "src").rglob("*.kt") if any(part.endswith("Main") for part in path.parts))


def without_comments(text: str) -> str:
    return re.sub(r"/\*.*?\*/|^[ \t]*//[^\n]*", "", text, flags=re.S | re.M)


def public_declarations(text: str):
    return [match for match in PUBLIC_TOP_LEVEL.finditer(without_comments(text)) if not re.search(r"\b(private|internal)\b", match["modifiers"])]


def boundaries() -> list[str]:
    errors = []
    for path in production_files("sdk/testing"):
        errors.append(f"Shared SDK test helper belongs in commonTest: {path.relative_to(ROOT)}")
    for module in MODULES:
        build = ROOT / module / "build.gradle.kts"
        if not build.is_file():
            errors.append(f"Missing SDK module build: {module}")
            continue
        declarations = build.read_text(encoding="utf-8-sig")
        production_declarations = re.sub(r"\w*Test\.dependencies\s*\{[^}]*\}", "", declarations, flags=re.S)
        optional_ui = module in OPTIONAL_UI_MODULES
        if optional_ui:
            project_dependencies = re.findall(r"projects\.([A-Za-z0-9_.]+)", production_declarations)
            if any(dependency not in ("sdk.model", "sdk.ui") for dependency in project_dependencies):
                errors.append(f"Non-leaf SDK UI project dependency in {module}")
            if re.search(r"project\([\"']:(?!sdk:(?:model|ui)[\"'])", production_declarations):
                errors.append(f"Non-leaf SDK UI project dependency in {module}")
            if re.search(r"libs\.(?:koin|coil|ktor|.*datastore|.*lifecycle|compose\.material|androidx\.(?:tv|media3))|(?:androidx|org\.jetbrains\.androidx)\.lifecycle:", production_declarations):
                errors.append(f"Forbidden optional SDK UI dependency in {module}")
        elif re.search(r"projects\.(feature|core\.ui|playback|sdk\.ui|sdk\.providers\.\w+\.ui)|libs\.(compose|koin|androidx\.media3)", production_declarations):
            errors.append(f"Application/rendering dependency in {module}")
        if re.search(r"(?:api|implementation)\(projects\.sdk\.testing\)", production_declarations):
            errors.append(f"Testing infrastructure is a production dependency in {module}")
        for path in production_files(module):
            text = path.read_text(encoding="utf-8-sig")
            package = re.search(r"^package\s+(\S+)", text, re.M)
            imports = re.findall(r"^import\s+(\S+)", text, re.M)
            if re.search(r"\bcom\.pampoukidis\.streamcore\.sdk\.testing\b", without_comments(text)):
                errors.append(f"Testing infrastructure in published SDK source: {path.relative_to(ROOT)}")
            if re.search(r"\bcom\.pampoukidis\.streamcoretv\b", without_comments(text)):
                errors.append(f"Legacy application namespace in published SDK source: {path.relative_to(ROOT)}")
            if not optional_ui and any(re.match(r"(?:androidx\.(?:compose|tv|media3)|org\.(?:koin|jetbrains\.compose)|com\.pampoukidis\.streamcoretv\.core\.ui|com\.pampoukidis\.streamcore\.sdk\.(?:ui|providers\.[^.]+\.ui))", item) for item in imports):
                errors.append(f"Non-headless source: {path.relative_to(ROOT)}")
            if optional_ui:
                forbidden_import = r"(?:org\.koin|coil\d?\.|io\.ktor|androidx\.(?:datastore|lifecycle|tv|media3|compose\.material)|com\.pampoukidis\.streamcoretv\.(?:(?:feature|playback|core\.ui)\.|client\.[^.]+\.data\.)|com\.pampoukidis\.streamcore\.sdk\.(?:api|runtime)\.|com\.pampoukidis\.streamcore\.sdk\.providers\.[^.]+\.(?!ui\.))"
                if any(re.match(forbidden_import, item) for item in imports):
                    errors.append(f"Application/infrastructure dependency in optional SDK UI: {path.relative_to(ROOT)}")
            if module in ("sdk/model", "sdk/api") and any(re.match(r"(?:android\.|androidx\.|io\.ktor\.|org\.koin\.)", item) for item in imports):
                errors.append(f"Platform/infrastructure type in consumer API: {path.relative_to(ROOT)}")
            if module == "sdk/model":
                for declaration in PUBLIC_TOP_LEVEL.finditer(without_comments(text)):
                    if declaration["kind"] not in ("class", "interface", "object", "typealias"):
                        continue
                    name = declaration["name"].partition("<")[0]
                    if not name.startswith("StreamCore") or name.endswith("Model"):
                        errors.append(f"SDK model type must use StreamCore without a Model suffix: {path.relative_to(ROOT)}: {name}")
            if module in PROVIDER_MODULES:
                provider_package = "com.pampoukidis.streamcore.sdk.providers." + module.rsplit("/", 1)[1].lower()
                if not package or not (package[1] == provider_package or package[1].startswith(provider_package + ".")):
                    errors.append(f"Noncanonical provider namespace: {path.relative_to(ROOT)}")
                if public_declarations(text) and (not package or package[1] != provider_package):
                    errors.append(f"Raw provider symbol is public: {path.relative_to(ROOT)}")
            if module == "sdk/runtime" and package:
                runtime_package = package[1].removeprefix("com.pampoukidis.streamcore.sdk.runtime.")
                if runtime_package == "integration" or runtime_package.startswith("integration."):
                    errors.append(f"Retired runtime integration package: {path.relative_to(ROOT)}")
                if re.match(r"(?:auth|profile|home|details|search|library|playback|content|session|error)(?:\.|$)", runtime_package):
                    for declaration in public_declarations(text):
                        contract = runtime_package + "." + declaration["name"]
                        if RUNTIME_PROVIDER_CONTRACTS.get(contract) != declaration["kind"]:
                            errors.append(f"Runtime implementation symbol is public: {path.relative_to(ROOT)}: {declaration['name']}")
    for application in ("feature", "app", "webApp"):
        for path in (ROOT / application).rglob("*.kt"):
            relative_parts = path.relative_to(ROOT / application).parts
            if "src" not in relative_parts:
                continue
            source_set = relative_parts[relative_parts.index("src") + 1]
            if "test" in source_set.lower():
                continue
            if re.search(r"^import\s+.*\.sdk\.runtime", path.read_text(encoding="utf-8-sig"), re.M):
                errors.append(f"Application/feature imports runtime implementation or SPI: {path.relative_to(ROOT)}")
    return errors


def staged_metadata(repository: Path) -> list[str]:
    errors = []
    roots = HEADLESS_ARTIFACTS | OPTIONAL_UI_ARTIFACTS
    expected = {root + suffix for root in roots for suffix in ("", "-android", "-wasm-js")}
    metadata_files = sorted(repository.rglob("*.module"))
    if not metadata_files:
        return [f"No published Gradle module metadata in {repository}"]
    observed = set()
    for path in metadata_files:
        metadata = json.loads(path.read_text(encoding="utf-8"))
        component = metadata.get("component", {})
        if component.get("group") != "com.pampoukidis.streamcore":
            errors.append(f"Incorrect published SDK group in {path.name}: {component.get('group')}")
        # KMP platform metadata's component names its root publication; its physical coordinate is the directory.
        module = path.parent.parent.name
        observed.add(module)
        if module not in expected:
            errors.append(f"Unexpected or colliding SDK publication: {module}")
        owner = next((root for root in roots if module in (root, root + "-android", root + "-wasm-js")), None)
        if owner and component.get("module") != owner:
            errors.append(f"Incorrect SDK publication owner {module} -> {component.get('module')}")
        for variant in metadata.get("variants", []):
            redirect = variant.get("available-at")
            if redirect and str(redirect.get("group", "")).startswith("com.pampoukidis.streamcore.internal."):
                errors.append(f"Internal project identity leaked into published variant in {path.name}")
            if redirect and redirect.get("group") == "com.pampoukidis.streamcore":
                destination = redirect["module"]
                if module in roots and destination not in (module + "-android", module + "-wasm-js"):
                    errors.append(f"Incorrect SDK variant redirect {module} -> {destination}")
                destination_file = repository / redirect["group"].replace(".", "/") / destination / redirect["version"] / f"{destination}-{redirect['version']}.module"
                if not destination_file.is_file():
                    errors.append(f"Missing published variant {destination}:{redirect['version']}")
            for dependency in variant.get("dependencies", []):
                coordinate = f"{dependency['group']}:{dependency['module']}"
                if dependency["group"].startswith("com.pampoukidis.streamcore.internal."):
                    errors.append(f"Internal project identity leaked into published dependency {coordinate}")
                if forbidden_published_dependency(owner, dependency["group"], dependency["module"]):
                    errors.append(f"Published {'optional UI' if owner in OPTIONAL_UI_ARTIFACTS else 'headless'} dependency violation {coordinate} in {path.name}")
                if dependency["group"] == "com.pampoukidis.streamcore":
                    version = dependency.get("version", {}).get("requires", "")
                    artifact = repository / dependency["group"].replace(".", "/") / dependency["module"] / version
                    if not artifact.is_dir():
                        errors.append(f"Missing published dependency {coordinate}:{version}")
    for missing in sorted(expected - observed):
        errors.append(f"Missing SDK publication: {missing}")
    for artifact, resource_package in UI_RESOURCE_PACKAGES.items():
        archives = list(repository.rglob(f"{artifact}-android-*.aar"))
        if not archives:
            errors.append(f"Missing optional UI Android archive: {artifact}")
        for path in archives:
            with zipfile.ZipFile(path) as archive:
                names = archive.namelist()
            prefix = f"assets/composeResources/{resource_package}/"
            owned_assets = [name for name in names if name.startswith(prefix)]
            if not owned_assets:
                errors.append(f"No published Compose resource assets in {path.name}: {resource_package}")
            if artifact != "provider-clientb-ui" and not any(name.endswith(".cvr") for name in owned_assets):
                errors.append(f"Missing published error string resources in {path.name}")
            if artifact.startswith("provider-") and not any("/drawable/" in name and name.endswith(".xml") for name in owned_assets):
                errors.append(f"Missing published avatar vectors in {path.name}")
    klib_owners = {}
    for path in sorted(repository.rglob("*.klib")):
        artifact = path.parent.parent.name
        with zipfile.ZipFile(path) as archive:
            manifest = archive.read("default/manifest").decode("utf-8")
        identity = next((line.partition("=")[2].replace("\\:", ":") for line in manifest.splitlines() if line.startswith("unique_name=")), "")
        if not identity:
            errors.append(f"Missing KLIB identity: {artifact}")
        elif identity in klib_owners and klib_owners[identity] != artifact:
            errors.append(f"Colliding KLIB identity {identity}: {klib_owners[identity]}, {artifact}")
        else:
            klib_owners[identity] = artifact
        if artifact.removesuffix("-wasm-js") not in identity:
            errors.append(f"KLIB identity does not identify its artifact: {artifact} -> {identity}")
    return errors


def forbidden_published_dependency(owner: str | None, group: str, module: str) -> bool:
    """Check direct SDK metadata edges; this does not inspect Compose's transitive resource closure."""
    coordinate = f"{group}:{module}"
    if group == "com.pampoukidis.streamcore" and module in ("sdk-testing", "sdk-testing-android", "sdk-testing-wasm-js"):
        return True
    if owner in OPTIONAL_UI_ARTIFACTS:
        if OPTIONAL_UI_DIRECT_FORBIDDEN_DEPENDENCY.search(coordinate):
            return True
        if group == "com.pampoukidis.streamcore":
            allowed = {"sdk-model", "sdk-ui", owner}
            return not any(module in (name, name + "-android", name + "-wasm-js") for name in allowed)
        return False
    if FORBIDDEN_DEPENDENCY.search(coordinate):
        return True
    return group == "com.pampoukidis.streamcore" and any(
        module in (name, name + "-android", name + "-wasm-js") for name in OPTIONAL_UI_ARTIFACTS
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", type=Path, help="Also audit staged metadata and internal dependency closure.")
    args = parser.parse_args()
    errors = boundaries()
    if args.repository:
        errors += staged_metadata(args.repository.resolve())
    if errors:
        print("\n".join(errors))
        return 1
    print("SDK source boundaries passed" + ("; staged metadata passed" if args.repository else ""))
    return 0


if __name__ == "__main__":
    sys.exit(main())
