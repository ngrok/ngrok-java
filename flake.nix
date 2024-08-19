{
  description = "ngrok agent library in Java";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixpkgs-unstable";

    # Note: fenix packages are cached via cachix:
    #       cachix use nix-community
    fenix-flake = {
      url = "github:nix-community/fenix";
      inputs.nixpkgs.follows = "nixpkgs";
    };

    flake-utils = {
      url = "github:numtide/flake-utils";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, fenix-flake, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config.android_sdk.accept_license = true;
          overlays = [
            fenix-flake.overlays.default
          ];
        };
        rust-toolchain = with pkgs.fenix; combine [
          (complete.withComponents [
            "cargo"
            "clippy"
            "rust-src"
            "rustc"
            "rustfmt"
            "rust-analyzer"
          ])
          targets.aarch64-linux-android.latest.rust-std
          targets.armv7-linux-androideabi.latest.rust-std
          targets.arm-linux-androideabi.latest.rust-std
        ];
        ANDROID_NDK = with pkgs.androidenv.androidPkgs; "${ndk-bundle}/libexec/android-sdk/ndk/${ndk-bundle.version}";
        ndk-path = "${ANDROID_NDK}/toolchains/llvm/prebuilt/linux-x86_64/bin";
        java-toolchain = with pkgs; [
          openjdk17_headless
          openjdk11_headless
          maven
        ];
        fix-n-fmt = pkgs.writeShellScriptBin "fix-n-fmt" ''
          set -euf -o pipefail
          ${rust-toolchain}/bin/cargo clippy --fix --allow-staged --allow-dirty --all-targets --all-features
          ${rust-toolchain}/bin/cargo fmt
        '';
        pre-commit = pkgs.writeShellScript "pre-commit" ''
          cargo clippy --workspace --all-targets --all-features -- -D warnings
          result=$?

          if [[ ''${result} -ne 0 ]] ; then
              cat <<\EOF
          There are some linting issues, try `fix-n-fmt` to fix.
          EOF
              exit 1
          fi

          # Use a dedicated sub-target-dir for udeps. For some reason, it fights with clippy over the cache.
          CARGO_TARGET_DIR=$(git rev-parse --show-toplevel)/target/udeps cargo udeps --workspace --all-targets --all-features
          result=$?

          if [[ ''${result} -ne 0 ]] ; then
              cat <<\EOF
          There are some unused dependencies.
          EOF
              exit 1
          fi

          diff=$(cargo fmt -- --check)
          result=$?

          if [[ ''${result} -ne 0 ]] ; then
              cat <<\EOF
          There are some code style issues, run `fix-n-fmt` first.
          EOF
              exit 1
          fi

          exit 0
        '';
        setup-hooks = pkgs.writeShellScriptBin "setup-hooks" ''
          repo_root=$(git rev-parse --git-dir)

          ${toString (map (h: ''
            ln -sf ${h} ''${repo_root}/hooks/${h.name}
          '') [
            pre-commit
          ])}
        '';
        # Make sure that cargo semver-checks uses the stable toolchain rather
        # than the nightly one that we normally develop with.
        semver-checks = with pkgs; symlinkJoin {
          name = "cargo-semver-checks";
          paths = [ cargo-semver-checks ];
          buildInputs = [ makeWrapper ];
          postBuild = ''
            wrapProgram $out/bin/cargo-semver-checks \
              --prefix PATH : ${rustc}/bin \
              --prefix PATH : ${cargo}/bin
          '';
        };
        extract-version = with pkgs; writeShellScriptBin "extract-crate-version" ''
          ${cargo}/bin/cargo metadata --format-version 1 --no-deps | \
            ${jq}/bin/jq -r ".packages[] | select(.name == \"$1\") | .version"
        '';
      in
      {
        legacyPackages = pkgs;
        devShell = pkgs.mkShell {
          inherit ANDROID_NDK;
          CHALK_OVERFLOW_DEPTH = 3000;
          CHALK_SOLVER_MAX_SIZE = 1500;
          OPENSSL_LIB_DIR = "${pkgs.openssl.out}/lib";
          OPENSSL_INCLUDE_DIR = "${pkgs.openssl.dev}/include";
          RUSTC_WRAPPER = "${pkgs.sccache}/bin/sccache";
          JAVA_11_HOME = "${pkgs.openjdk11_headless}";
          JAVA_17_HOME = "${pkgs.openjdk17_headless}";
          CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER = "${ndk-path}/aarch64-linux-android21-clang";
          CC_aarch64_linux_android = "${ndk-path}/aarch64-linux-android21-clang";
          AR_aarch64_linux_android = "${ndk-path}/llvm-ar";
          RUSTFLAGS = "-C link-arg=-Wl,-soname,libngrok_java.so";
          buildInputs = with pkgs; [
            rust-toolchain
            java-toolchain
            fix-n-fmt
            setup-hooks
            cargo-udeps
            semver-checks
            extract-version
            jdt-language-server
          ] ++ lib.optionals stdenv.isDarwin [
            # nix darwin stdenv has broken libiconv: https://github.com/NixOS/nixpkgs/issues/158331
            libiconv
            pkgs.darwin.apple_sdk.frameworks.Security
          ];
        };
      });
}
